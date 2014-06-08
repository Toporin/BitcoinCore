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

/**
 * <p>A 'ping' message is sent to test network connectivity to a node.  Upon receiving a ping,
 * the node responds with a pong.</p>
 *
 * <p>Ping Message</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   8 bytes    Nonce               Random value
 * </pre>
 *
 * <p>Pong Message</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   8 bytes    Nonce               Random value from ping message
 * </pre> *
 */
public class PingMessage {

    /**
     * Send a 'ping' message to a peer
     *
     * @param       peer                Destination peer
     * @return      Message to be sent
     */
    public static Message buildPingMessage(Peer peer) {
        //
        // We will use the current time as the nonce
        //
        byte[] msgData = new byte[8];
        Utils.uint64ToByteArrayLE(System.currentTimeMillis(), msgData, 0);
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("ping", msgData);
        return new Message(buffer, peer, MessageHeader.PING_CMD);
    }

    /**
     * Process a 'ping' message and return a 'pong' response
     *
     * @param       msg                 Message
     * @param       inStream            Message data stream
     * @param       invHandler          Inventory handler
     * @throws      EOFException        End-of-data while processing input stream
     * @throws      IOException         Unable to read input stream
     */
    public static void processPingMessage(Message msg, ByteArrayInputStream inStream,
                                        MessageListener invHandler)
                                        throws EOFException, IOException {
        byte[] bytes = new byte[8];
        //
        // Get the nonce from the 'ping' message
        //
        int count = inStream.read(bytes);
        if (count < 8)
            throw new EOFException("End-of-data while processing 'ping' message");
        //
        // Build the 'pong' response
        //
        ByteBuffer buffer = MessageHeader.buildMessage("pong", bytes);
        msg.setBuffer(buffer);
        msg.setCommand(MessageHeader.PONG_CMD);
    }
}
