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

import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>The 'getdata' message is used to request one or more blocks and transactions.
 * Blocks are returned as 'block' messages and transactions are returned as 'tx'
 * messages.  Any entries that are not found are returned as a 'notfound' response.</p>
 *
 * <p>GetData Message:</p>
 * <pre>
 *   Size       Field               Definition
 *   ====       =====               ==========
 *   VarInt     Count               Number of inventory vectors
 *   Variable   InvVectors          One or more inventory vectors
 * </pre>
 *
 * <p>Inventory Vector:</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   4 bytes    Type                0=Error, 1=Transaction, 2=Block, 3=Filtered block
 *  32 bytes    Hash                Object hash
 * </pre>
 */
public class GetDataMessage {

    /**
     * Create a 'getdata' message
     *
     * @param       peer            Peer node
     * @param       type            Request type (INV_TX or INV_FILTERED_BLOCK)
     * @param       hashList        Hash list
     * @return      Message
     */
    public static Message buildGetDataMessage(Peer peer, int type, List<Sha256Hash> hashList) {
        int varCount = hashList.size();
        byte[] varBytes = VarInt.encode(varCount);
        byte[] msgData = new byte[varBytes.length+varCount*36];
        //
        // Build the message payload
        //
        System.arraycopy(varBytes, 0, msgData, 0, varBytes.length);
        int offset = varBytes.length;
        for (int i=0; i<varCount; i++) {
            Sha256Hash hash = hashList.get(i);
            Utils.uint32ToByteArrayLE(type, msgData, offset);
            System.arraycopy(Utils.reverseBytes(hash.getBytes()), 0, msgData, offset+4, 32);
            offset+=36;
        }
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("getdata", msgData);
        return new Message(buffer, peer,
                (type==NetParams.INV_FILTERED_BLOCK?MessageHeader.INVBLOCK_CMD:MessageHeader.INVTX_CMD));
    }

    /**
     * Process a 'getdata' message
     *
     * @param       msg                     Message
     * @param       inStream                Message data stream
     * @param       invHandler              Inventory handler
     * @throws      EOFException            End-of-data while processing message data
     * @throws      IOException             Unable to read message data
     * @throws      VerificationException   Data verification failed
     */
    public static void processGetDataMessage(Message msg, ByteArrayInputStream inStream,
                                            InventoryHandler invHandler)
                                            throws EOFException, IOException, VerificationException {
        Peer peer = msg.getPeer();
        //
        // Get the number of inventory entries
        //
        int varCount = new VarInt(inStream).toInt();
        if (varCount < 0 || varCount > 50000)
            throw new VerificationException("More than 50,000 inventory entries in 'getdata' message");
        //
        // Process each request
        //
        List<byte[]> notFound = new ArrayList<>(50);
        byte[] invBytes = new byte[36];
        for (int i=0; i<varCount; i++) {
            int count = inStream.read(invBytes);
            if (count < 36)
                throw new EOFException("End-of-data while processing 'getdata' message");
            int invType = (int)Utils.readUint32LE(invBytes, 0);
            Sha256Hash hash = new Sha256Hash(Utils.reverseBytes(invBytes, 4, 32));
            if (!invHandler.sendInventory(peer, invType, hash))
                notFound.add(Arrays.copyOf(invBytes, 36));
        }
        //
        // Create a 'notfound' response if we didn't find all of the requested items
        //
        if (!notFound.isEmpty()) {
            varCount = notFound.size();
            byte[] varBytes = VarInt.encode(varCount);
            byte[] msgData = new byte[varCount*36+varBytes.length];
            System.arraycopy(varBytes, 0, msgData, 0, varBytes.length);
            int offset = varBytes.length;
            for (byte[] invItem : notFound) {
                System.arraycopy(invItem, 0, msgData, offset, 36);
                offset += 36;
            }
            ByteBuffer buffer = MessageHeader.buildMessage("notfound", msgData);
            msg.setBuffer(buffer);
            msg.setCommand(MessageHeader.NOTFOUND_CMD);
        }
    }
}
