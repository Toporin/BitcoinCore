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

import java.io.EOFException;
import java.nio.ByteBuffer;
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

    /**
     * Build an 'addr' message
     *
     * We will include all peers that we have seen within the past 15 minutes as well as
     * our own external listen address.  A maximum of 250 addresses will be included in
     * the address message.
     *
     * @param       peer                The destination peer or null for a broadcast message
     * @param       addressList         Peer address list
     * @param       localAddress        Local address or null if not accepting inbound connections
     * @return                          Message to be sent to the peer
     */
    public static Message buildAddressMessage(Peer peer, List<PeerAddress> addressList, PeerAddress localAddress) {
        //
        // Create an address list containing peers that we have seen within the past 15 minutes.
        // The maximum length of the list is 250 entries.  Static addresses are not included
        // in the list.  We will include our own address with a current timestamp if it is available.
        //
        long oldestTime = System.currentTimeMillis()/1000 - (15*60);
        List<PeerAddress> addresses = new ArrayList<>(250);
        if (localAddress != null) {
            localAddress.setTimeStamp(oldestTime);
            addresses.add(localAddress);
        }
        for (PeerAddress address : addressList) {
            if (addresses.size() >= 250)
                break;
            if (address.getTimeStamp() >= oldestTime && !address.isStatic())
                addresses.add(address);
        }
        //
        // Build the message payload
        //
        int bufferLength = addresses.size()*PeerAddress.PEER_ADDRESS_SIZE + 5;
        SerializedBuffer msgBuffer = new SerializedBuffer(bufferLength);
        msgBuffer.putVarInt(addresses.size())
                 .putBytes(addresses);
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("addr", msgBuffer);
        return new Message(buffer, peer, MessageHeader.ADDR_CMD);
    }

    /**
     * Process an 'addr' message
     *
     * Addresses that were seen within the previous 15 minutes will be included in the address
     * list.  The processAddresses() inventory handler will then be notified that new addresses
     * have been received.
     *
     * @param       msg                     Message
     * @param       inBuffer                Message buffer
     * @param       msgListener             Message listener or null
     * @throws      EOFException            Serialized byte stream is too short
     * @throws      VerificationException   Message contains more than 1000 entries
     */
    public static void processAddressMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {
        //
        // Get the address count
        //
        int addrCount = inBuffer.getVarInt();
        if (addrCount < 0 || addrCount > 1000)
            throw new VerificationException("More than 1000 addresses in 'addr' message");
        //
        // Build the address list.  We will not include addresses that were not seen within
        // the previous 15 minutes.
        //
        long oldestTime = System.currentTimeMillis()/1000 - (15*60);
        List<PeerAddress> addresses = new ArrayList<>(addrCount);
        for (int i=0; i<addrCount; i++) {
            PeerAddress address = new PeerAddress(inBuffer);
            if (address.getTimeStamp() > oldestTime)
                addresses.add(new PeerAddress(inBuffer));
        }
        //
        // Notify the application message listener
        //
        if (!addresses.isEmpty() && msgListener != null)
            msgListener.processAddresses(msg.getPeer(), addresses);
    }
}
