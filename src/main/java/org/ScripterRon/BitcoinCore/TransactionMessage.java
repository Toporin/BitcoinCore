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
 * <p>The 'tx' message contains a transaction which is not yet in a block.  The transaction
 * will be held in the memory pool for a period of time to allow other peers to request
 * the transaction.</p>
 *
 * <p>Transaction Message</p>
 * <pre>
 *   Size           Field               Description
 *   ====           =====               ===========
 *   4 bytes        Version             Transaction version
 *   VarInt         InputCount          Number of inputs
 *   Variable       InputList           Inputs
 *   VarInt         OutputCount         Number of outputs
 *   Variable       OutputList          Outputs
 *   4 bytes        LockTime            Transaction lock time
 * </pre>
 */
public class TransactionMessage {

    /**
     * Build a 'tx' message
     *
     * @param       peer                The destination peer or null for a broadcast message
     * @param       tx                  Transaction to be sent
     * @return                          'tx' message
     */
    public static Message buildTransactionMessage(Peer peer, Transaction tx) {
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("tx", tx.getBytes());
        return new Message(buffer, peer, MessageHeader.TX_CMD);
    }

    /**
     * Processes a 'tx' message
     *
     * @param       msg                     Message
     * @param       inBuffer                Input buffer
     * @param       msgListener             Message listener
     * @throws      EOFException            Serialized data is too short
     * @throws      VerificationException   Transaction verification failed
     */
    public static void processTransactionMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {
        //
        // Get the transaction
        //
        Transaction tx = new Transaction(inBuffer);
        //
        // Notify the message listener that a transaction is ready for processing
        //
        msgListener.processTransaction(msg.getPeer(), tx);
    }
}
