/**
 * Copyright 2013-2014 Ronald W Hoffman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.BitcoinCore;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.net.InetAddress;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>An 'addr' message is sent to inform a node about peers on the network.</p>
 *
 * <p>Address Message</p>
 * <pre>
 *   Size       Field           Description
 *   ====       =====           ===========
 *   VarInt     Count           The number of addresses
 *   Variable   Addresses       One or more network addresses
 * </pre>
 *
 * <p>Network Address</p>
 * <pre>
 *   Size       Field           Description
 *   ====       =====           ===========
 *   4 bytes    Time            Timestamp in seconds since the epoch
 *   8 bytes    Services        Services provided by the node
 *  16 bytes    Address         IPv6 address (IPv4 addresses are encoded as IPv6 addresses)
 *   2 bytes    Port            Port (network byte order)
 * </pre>
 */
public class AddressMessage {

    /** IPv6-encoded IPv4 address prefix */
    public static final byte[] IPV6_PREFIX = new byte[] {
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff
    };

    /**
     * Build an 'addr' message
     *
     * We will include all peers that we have seen within the last hour
     *
     * @param       peer            The destination peer or null for a broadcast message
     * @param       addressList     Peer address list
     * @return                      Message to be sent to the peer
     */
    public static Message buildAddressMessage(Peer peer, List<PeerAddress> addressList) {
        //
        // Create an address list containing peers that we have seen within the past hour.
        // The maximum length of the list is 250 entries.  Static addresses are not included
        // in the list.
        //
        long oldestTime = System.currentTimeMillis()/1000 - (15*60);
        List<PeerAddress> addresses = new ArrayList<>(addressList.size());
        for (PeerAddress address : addressList) {
            if (addresses.size() == 250)
                break;
            if (address.getTimeStamp() >= oldestTime && !address.isStatic())
                addresses.add(address);
        }
        //
        // Build the message payload
        //
        byte[] varCount = VarInt.encode(addresses.size());
        byte[] msgData = new byte[addresses.size()*30+varCount.length];
        System.arraycopy(varCount, 0, msgData, 0, varCount.length);
        int offset = varCount.length;
        for (PeerAddress address : addresses) {
            Utils.uint32ToByteArrayLE(address.getTimeStamp(), msgData, offset);
            Utils.uint64ToByteArrayLE(address.getServices(), msgData, offset+4);
            offset += 12;
            byte[] addrBytes = address.getAddress().getAddress();
            if (addrBytes.length == 16) {
                System.arraycopy(addrBytes, 0, msgData, offset, 16);
            } else {
                System.arraycopy(IPV6_PREFIX, 0, msgData, offset, 12);
                System.arraycopy(addrBytes, 0, msgData, offset+12, 4);
            }
            offset += 16;
            int port = address.getPort();
            msgData[offset] = (byte)(port>>8);
            msgData[offset+1] = (byte)port;
            offset += 2;
        }
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("addr", msgData);
        return new Message(buffer, peer, MessageHeader.ADDR_CMD);
    }

    /**
     * Process an 'addr' message and return the address list
     *
     * @param       msg                     Message
     * @param       inStream                Message data stream
     * @param       invHandler              Inventory handler
     * @return                              Peer address list
     * @throws      EOFException            Serialized byte stream is too short
     * @throws      IOException             Error reading from input stream
     * @throws      VerificationException   Message contains more than 1000 entries
     */
    public static List<PeerAddress> processAddressMessage(Message msg, ByteArrayInputStream inStream,
                                            InventoryHandler invHandler)
                                            throws EOFException, IOException, VerificationException {
        byte[] bytes = new byte[30];
        byte[] addr4Bytes = new byte[4];
        byte[] addr6Bytes = new byte[16];
        long oldestTime = System.currentTimeMillis()/1000 - (15*60);
        //
        // Get the address count
        //
        int addrCount = new VarInt(inStream).toInt();
        if (addrCount < 0 || addrCount > 1000)
            throw new VerificationException("More than 1000 addresses in 'addr' message");
        List<PeerAddress> addresses = new ArrayList<>(addrCount);
        //
        // Process the addresses and keep any addresses that have been seen within the
        // past hour and provide network node services
        //
        for (int i=0; i<addrCount; i++) {
            int count = inStream.read(bytes);
            if (count < 30)
                throw new EOFException("End-of-data on 'addr' message");
            long timeSeen = Utils.readUint32LE(bytes, 0);
            if (timeSeen < oldestTime)
                continue;
            long services = Utils.readUint64LE(bytes, 4);
            if ((services&NetParams.NODE_NETWORK) == 0)
                continue;
            boolean ipv4 = true;
            for (int j=0; j<12; j++) {
                if (bytes[j+12] != IPV6_PREFIX[j]) {
                    ipv4 = false;
                    break;
                }
            }
            InetAddress address;
            if (ipv4) {
                System.arraycopy(bytes, 24, addr4Bytes, 0, 4);
                address = InetAddress.getByAddress(addr4Bytes);
            } else {
                System.arraycopy(bytes, 12, addr6Bytes, 0, 16);
                address = InetAddress.getByAddress(addr6Bytes);
            }
            int port = (((int)bytes[28]&0xff)<<8) | ((int)bytes[29]&0xff);
            PeerAddress peerAddress = new PeerAddress(address, port, timeSeen);
            peerAddress.setServices(services);
            addresses.add(peerAddress);
        }
        return addresses;
    }
}
