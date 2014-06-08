/*
 * Copyright 2013-2014 Ronald Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * The 'block' message consists of a single serialized block.
 */
public class BlockMessage {

    /**
     * Build a 'block' message
     *
     * @param       peer                The destination peer or null for a broadcast message
     * @param       block               Block to be sent to the peer
     * @return                          'block' message
     */
    public static Message buildBlockMessage(Peer peer, Block block) {
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("block", block.getBytes());
        return new Message(buffer, peer, MessageHeader.BLOCK_CMD);
    }

    /**
     * Process a 'block' message
     *
     * @param       msg                     Message
     * @param       inBuffer                Input buffer
     * @param       msgListener             Message listener
     * @throws      EOFException            End-of-data while processing stream
     * @throws      VerificationException   Block verification failed
     */
    public static void processBlockMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {
        //
        // Get the block
        //
        Block block = new Block(inBuffer, true);
        //
        // Notify the message listener
        //
        msgListener.processBlock(msg.getPeer(), block);
    }
}
