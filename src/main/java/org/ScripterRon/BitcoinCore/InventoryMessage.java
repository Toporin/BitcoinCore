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

import java.util.List;

/**
 * <p>The 'inv' message is sent by a remote peer to advertise blocks and transactions
 * that are available.  This message can be unsolicited or in response to a 'getblocks'
 * request.</p>
 *
 * <p>We will add items that we don't have to the 'pendingRequests' queue.  This will cause
 * the network handler to send 'getdata' requests to get the missing items.</p>
 *
 * <p>Inventory Message:</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   VarInt     Count               Number of inventory vectors
 *   Variable   InvVector           One or more inventory vectors
 * </pre>
 *
 * <p>Inventory Vector:</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   4 bytes    Type                0=Error, 1=Transaction, 2=Block
 *  32 bytes    Hash                Object hash
 * </pre>
 */
public class InventoryMessage {

    /**
     * Build an 'inv' message
     *
     * @param       peer            Destination peer
     * @param       type            Inventory type (INV_TX or INV_BLOCK)
     * @param       hashList        Inventory hash list
     * @return                      Message to send to the peer
     */
    public static Message buildInventoryMessage(Peer peer, int type, List<Sha256Hash> hashList) {
        byte[] varCount = VarInt.encode(hashList.size());
        byte[] msgData = new byte[hashList.size()*36+varCount.length];
        //
        // Build the message payload
        //
        System.arraycopy(varCount, 0, msgData, 0, varCount.length);
        int offset = varCount.length;
        for (Sha256Hash hash : hashList) {
            Utils.uint32ToByteArrayLE(type, msgData, offset);
            System.arraycopy(Utils.reverseBytes(hash.getBytes()), 0, msgData, offset+4, 32);
            offset += 36;
        }
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("inv", msgData);
        return new Message(buffer, peer,
                (type==NetParams.INV_BLOCK?MessageHeader.INVBLOCK_CMD:MessageHeader.INVTX_CMD));
    }

    /**
     * Process an 'inv' message.
     *
     * @param       msg                     Message
     * @param       inStream                Message data stream
     * @param       invHandler              Inventory handler
     * @throws      EOFException            End-of-data while processing input stream
     * @throws      IOException             Unable to read input stream
     * @throws      VerificationException   Verification failed
     */
    public static void processInventoryMessage(Message msg, ByteArrayInputStream inStream,
                                            InventoryHandler invHandler)
                                            throws EOFException, IOException, VerificationException {
        byte[] bytes = new byte[36];
        Peer peer = msg.getPeer();
        //
        // Get the number of inventory vectors (maximum of 1000 entries)
        //
        int invCount = new VarInt(inStream).toInt();
        if (invCount < 0 || invCount > 1000)
            throw new VerificationException("More than 1000 entries in 'inv' message",
                                            NetParams.REJECT_INVALID);
        //
        // Process the inventory vectors
        //
        for (int i=0; i<invCount; i++) {
            int count = inStream.read(bytes);
            if (count < 36)
                throw new EOFException("'inv' message is too short");
            int type = (int)Utils.readUint32LE(bytes, 0);
            Sha256Hash hash = new Sha256Hash(Utils.reverseBytes(bytes, 4, 32));
            invHandler.requestInventory(peer, type, hash);
        }
    }
}
