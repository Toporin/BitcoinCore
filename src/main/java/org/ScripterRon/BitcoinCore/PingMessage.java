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
 */
public class PingMessage {

    /**
     * Send a 'ping' message to a peer
     *
     * @param       peer                Destination peer
     * @return                          'ping' message
     */
    public static Message buildPingMessage(Peer peer) {
        //
        // We will use the current time as the nonce
        //
        SerializedBuffer msgBuffer = new SerializedBuffer(8).putLong(System.currentTimeMillis());
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("ping", msgBuffer);
        return new Message(buffer, peer, MessageHeader.MessageCommand.PING);
    }

    /**
     * Process a 'ping' message
     *
     * @param       msg                 Message
     * @param       inBuffer            Input buffer
     * @param       msgListener         Message listener
     * @throws      EOFException        End-of-data while processing input stream
     */
    public static void processPingMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                        throws EOFException {
        //
        // Get the nonce from the 'ping' message
        //
        long nonce = inBuffer.getLong();
        //
        // Notify the message listener
        //
        msgListener.processPing(msg, nonce);
    }
}
