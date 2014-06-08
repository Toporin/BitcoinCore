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
 * <p>The 'getblocks' message is sent by a peer when it does not have the latest block chain
 * and needs a list of the blocks required to get to the latest block.</p>
 *
 * <p>GetBlocks Message</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   4 bytes    Version             Negotiated protocol version
 *   VarInt     Count               Number of locator hash entries
 *   Variable   Entries             Locator hash entries
 *   32 bytes   Stop                Hash of the last desired block or zero to get as many as possible
 * </pre>
 */
public class GetBlocksMessage {

    /**
     * Build a 'getblocks' message
     *
     * @param       peer            Destination peer
     * @param       blockList       Block hash list
     * @param       stopBlock       Stop block hash (Sha256Hash.ZERO_HASH to get all blocks)
     * @return                      'getblocks' message
     */
    public static Message buildGetBlocksMessage(Peer peer, List<Sha256Hash> blockList, Sha256Hash stopBlock) {
        //
        // Build the message payload
        //
        // The protocol version will be set to the lesser of our version and the peer version
        //
        SerializedBuffer msgBuffer = new SerializedBuffer(blockList.size()*32+40);
        msgBuffer.putInt(Math.min(peer.getVersion(), NetParams.PROTOCOL_VERSION))
                 .putVarInt(blockList.size());
        blockList.stream().forEach((hash) -> msgBuffer.putBytes(Utils.reverseBytes(hash.getBytes())));
        msgBuffer.putBytes(Utils.reverseBytes(stopBlock.getBytes()));
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("getblocks", msgBuffer);
        return new Message(buffer, peer, MessageHeader.GETBLOCKS_CMD);
    }

    /**
     * Process the 'getblocks' message and return an 'inv' message
     *
     * @param       msg                     Message
     * @param       inBuffer                Input buffer
     * @param       msgListener             Message listener
     * @throws      EOFException            End-of-data processing stream
     * @throws      VerificationException   Message verification failed
     */
    public static void processGetBlocksMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {
        //
        // Process the message
        //
        int version = inBuffer.getInt();
        if (version < NetParams.MIN_PROTOCOL_VERSION)
            throw new VerificationException(String.format("Protocol version %d is not supported", version));
        int count = inBuffer.getVarInt();
        if (count < 0 || count > 500)
            throw new VerificationException("More than 500 locator entries in 'getblocks' message");
        List<Sha256Hash> blockList = new ArrayList<>(count);
        for (int i=0; i<count; i++)
            blockList.add(new Sha256Hash(Utils.reverseBytes(inBuffer.getBytes(32))));
        Sha256Hash stopBlock = new Sha256Hash(Utils.reverseBytes(inBuffer.getBytes(32)));
        //
        // Notify the message listener
        //
        msgListener.processGetBlocks(msg.getPeer(), version, blockList, stopBlock);
    }
}
