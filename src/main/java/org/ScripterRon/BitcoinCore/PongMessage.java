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
 * <p>Pong Message</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   8 bytes    Nonce               Random value from ping message
 * </pre> *
 */
public class PongMessage {

    /**
     * Send a 'pong' message to a peer
     *
     * @param       peer                Destination peer
     * @param       nonce               Nonce from the 'ping' message
     * @return                          'pong' message
     */
    public static Message buildPongMessage(Peer peer, long nonce) {
        //
        // Build the message data
        //
        SerializedBuffer msgBuffer = new SerializedBuffer(8).putLong(nonce);
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("pong", msgBuffer);
        return new Message(buffer, peer, MessageHeader.MessageCommand.PONG);
    }

    /**
     * Process a 'pong'
     *
     * @param       msg                 Message
     * @param       inBuffer            Input buffer
     * @param       msgListener         Message listener
     * @throws      EOFException        End-of-data while processing input stream
     */
    public static void processPongMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                        throws EOFException {
        //
        // Get the nonce from the 'pong' message
        //
        long nonce = inBuffer.getLong();
        //
        // Notify the message listener
        //
        msgListener.processPong(msg.getPeer(), nonce);
    }
}
