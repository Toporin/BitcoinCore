/*
 * Copyright 2014 Ronald W Hoffman.
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
import java.util.LinkedList;
import java.util.List;

/**
 * <p>The 'merkleblock' message is sent in response to a 'getdata' block request
 * and the requesting peer has set a Bloom filter.  In this case, the response is just the
 * block header and a Merkle branch representing the matching transactions.</p>
 *
 * <p>MerkleBlock Message</p>
 * <pre>
 *   Size       Field           Description
 *   ====       =====           ===========
 *   4 bytes    Version         The block version number
 *   32 bytes   PrevBlockHash   The hash of the preceding block in the chain
 *   32 byte    MerkleRoot      The Merkle root for the transactions in the block
 *   4 bytes    Time            The time the block was mined
 *   4 bytes    Difficulty      The target difficulty
 *   4 bytes    Nonce           The nonce used to generate the required hash
 *   4 bytes    txCount         Number of transactions in the block
 *   VarInt     hashCount       Number of hashes
 *   Variable   hashes          Hashes in depth-first order
 *   VarInt     flagCount       Number of bytes of flag bits
 *   Variable   flagBits        Flag bits packed 8 per byte, least significant bit first
 * </pre>
 */
public class MerkleBlockMessage {

    /**
     * Builds the 'merkleblock' message
     *
     * @param       peer            Destination peer
     * @param       block           Block to be sent to the peer
     * @param       indexList       List of matching transaction indexes
     * @return                      'merkleblock' message
     */
    public static Message buildMerkleBlockMessage(Peer peer, Block block, List<Integer> indexList) {
        //
        // Create the Merkle branch
        //
        List<Transaction> txList = block.getTransactions();
        List<byte[]> merkleTree = block.getMerkleTree();
        MerkleBranch branch = new MerkleBranch(txList.size(), indexList, merkleTree);
        //
        // Create the message data
        //
        SerializedBuffer msgBuffer = new SerializedBuffer(BlockHeader.HEADER_SIZE+txList.size()*32+12);
        block.getHeaderBytes(msgBuffer);
        branch.getBytes(msgBuffer);
        //
        // Create the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("merkleblock", msgBuffer);
        return new Message(buffer, peer, MessageHeader.MessageCommand.MERKLEBLOCK);
    }
    /**
     * Processes the 'merkleblock' message
     *
     * @param       msg                     Message
     * @param       inBuffer                Input buffer
     * @param       msgListener             Message listener
     * @throws      EOFException            End-of-data processing input stream
     * @throws      VerificationException   Verification error
     */
    public static void processMerkleBlockMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {
        //
        // Get the block header
        //
        BlockHeader blockHeader = new BlockHeader(inBuffer, true);
        //
        // Get the matching transactions from the Merkle branch
        //
        MerkleBranch merkleBranch = new MerkleBranch(inBuffer);
        List<Sha256Hash> matches = new LinkedList<>();
        Sha256Hash merkleRoot = merkleBranch.calculateMerkleRoot(matches);
        if (!merkleRoot.equals(blockHeader.getMerkleRoot()))
            throw new VerificationException("Merkle root is incorrect", RejectMessage.REJECT_INVALID,
                                            blockHeader.getHash());
        blockHeader.setMatches(matches);
        //
        // Notify the message listener that a block is ready for processing
        //
        msgListener.processMerkleBlock(msg, blockHeader);
    }
}
