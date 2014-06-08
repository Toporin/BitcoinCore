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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * <p>The 'version' message is exchanged when two nodes connect.  It identifies
 * the services provided by the nodes and the latest block each has seen.  A node
 * responds with a 'verack' message if it accepts the connection, otherwise the
 * node will close the connection.</p>
 *
 * <p>Version Message:</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   4 bytes    Version             Protocol version
 *   8 bytes    Services            Supported services (bit field)
 *   8 bytes    Timestamp           Time in seconds since the epoch
 *  26 bytes    RemoteAddress       Remote node address
 *  26 bytes    LocalAddress        Local node address
 *   8 bytes    Nonce               Random value to identify sending node
 *  VarString   UserAgent           Identification string
 *   4 bytes    BlockHeight         Last block received by sending node
 *   1 byte     TxRelay             TRUE if remote peer should relay transactions
 * </pre>
 *
 * <p>Network Address:</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   8 bytes    Services            Supported services (same as 'version' message)
 *  16 bytes    NetworkAddress      IPv6 address (IPv4 address encoded as IPv6 address)
 *   2 bytes    Port                Port (network byte order)
 * </pre>
 */
public class VersionMessage {

    /** Node identifier for this peer execution */
    public static final long NODE_ID = Double.doubleToRawLongBits(Double.valueOf(Math.random()));

    /**
     * Builds a 'version' message
     *
     * @param       peer                The remote peer
     * @param       localAddress        Local listen address or null if not accepting inbound connections
     * @param       chainHeight         Current chain height
     * @return                          Message to send to remote peer
     */
    public static Message buildVersionMessage(Peer peer, PeerAddress localAddress, int chainHeight) {
        //
        // Set the protocol version, supported services and current time
        //
        SerializedBuffer msgBuffer = new SerializedBuffer();
        msgBuffer.putInt(NetParams.PROTOCOL_VERSION)
                 .putLong(NetParams.SUPPORTED_SERVICES)
                 .putLong(System.currentTimeMillis()/1000);
        //
        // Set the destination address
        //
        PeerAddress peerAddress = peer.getAddress();
        byte[] dstAddress = peerAddress.getAddress().getAddress();
        msgBuffer.skip(8);
        if (dstAddress.length == 16) {
            msgBuffer.putBytes(dstAddress);
        } else {
            msgBuffer.putBytes(PeerAddress.IPV6_PREFIX);
            msgBuffer.putBytes(dstAddress);
        }
        msgBuffer.putUnsignedShort(peerAddress.getPort());
        //
        // Set the source address
        //
        msgBuffer.putLong(NetParams.SUPPORTED_SERVICES);
        if (localAddress != null) {
            byte[] srcAddress = localAddress.getAddress().getAddress();
            if (srcAddress.length == 16) {
                msgBuffer.putBytes(srcAddress);
            } else {
                msgBuffer.putBytes(PeerAddress.IPV6_PREFIX)
                         .putBytes(srcAddress);
            }
            msgBuffer.putUnsignedShort(localAddress.getPort());
        } else {
            msgBuffer.skip(16+2);
        }
        //
        // Set the agent name
        //
        try {
        String agentName = String.format("/%s/%s/", NetParams.APPLICATION_NAME, NetParams.LIBRARY_NAME);
            byte[] agentBytes = agentName.getBytes("UTF-8");
            msgBuffer.putByte((byte)agentBytes.length)
                     .putBytes(agentBytes);
        } catch (UnsupportedEncodingException exc) {
            throw new RuntimeException("Unable to convert string to UTF-8: "+exc.getMessage());
        }
        //
        // Set the chain height and transaction relay flag
        //
        msgBuffer.putInt(chainHeight);
        msgBuffer.putByte((NetParams.SUPPORTED_SERVICES&NetParams.NODE_NETWORK)!=0 ? (byte)1 : (byte)0);
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("version", msgBuffer);
        return new Message(buffer, peer, MessageHeader.VERSION_CMD);
    }

    /**
     * Processes a 'version' message
     *
     * @param       msg                     Message
     * @param       inBuffer                Input buffer
     * @param       msgListener             Message listener
     * @throws      EOFException            End-of-data processing message data
     * @throws      VerificationException   Message verification failed
     */
    public static void processVersionMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {

        Peer peer = msg.getPeer();
        //
        // Validate the protocol level
        //
        int version = inBuffer.getInt();
        if (version < NetParams.MIN_PROTOCOL_VERSION)
            throw new VerificationException(String.format("Protocol version %d is not supported", version),
                                            NetParams.REJECT_OBSOLETE);
        peer.setVersion(version);
        //
        // Get the peer services
        //
        peer.setServices(inBuffer.getLong());
        if ((peer.getServices()&NetParams.NODE_NETWORK) == 0)
            throw new VerificationException("Peer does not provide network services",
                                            NetParams.REJECT_NONSTANDARD);
        //
        // Get our address as seen by the peer
        //
        inBuffer.skip(8+8);
        byte[] addrBytes = inBuffer.getBytes(16);
        InetAddress addr;
        try {
            boolean ipv4 = true;
            for (int j=0; j<12; j++) {
                if (addrBytes[j] != PeerAddress.IPV6_PREFIX[j]) {
                    ipv4 = false;
                    break;
                }
            }
            if (ipv4)
                addr = InetAddress.getByAddress(Arrays.copyOfRange(addrBytes, 12, 16));
            else
                addr = InetAddress.getByAddress(addrBytes);
        } catch (UnknownHostException exc) {
            throw new VerificationException("Destination address is not valid: "+exc.getMessage());
        }
        PeerAddress localAddress = new PeerAddress(addr, inBuffer.getUnsignedShort());
        //
        // Get the user agent
        //
        inBuffer.skip(26+8);
        try {
            int length = inBuffer.getByte();
            if (length < 0 || length > 255)
                throw new VerificationException("Agent length is greater than 255 characters");
            byte[] agentBytes = inBuffer.getBytes(length);
            peer.setUserAgent(new String(agentBytes, "UTF-8"));
        } catch (UnsupportedEncodingException exc) {
            throw new VerificationException("Agent name is not valid: "+exc.getMessage());
        }
        //
        // Get the chain height and transaction relay flag (the transaction relay flag is
        // not included in earlier protocol versions)
        //
        peer.setHeight(inBuffer.getInt());
        peer.setTxRelay(inBuffer.available()>0 && inBuffer.getByte()!=0);
        //
        // Notify the message listener
        //
        msgListener.processVersion(peer, localAddress);
    }
}
