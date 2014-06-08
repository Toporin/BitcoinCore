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
 * <p>The 'getdata' message is used to request one or more blocks and transactions.
 * Blocks are returned as 'block' messages and transactions are returned as 'tx'
 * messages.  Any entries that are not found are returned as a 'notfound' response.</p>
 *
 * <p>GetData Message:</p>
 * <pre>
 *   Size       Field               Definition
 *   ====       =====               ==========
 *   VarInt     Count               Number of inventory items
 *   Variable   InvItems            One or more inventory items
 * </pre>
 */
public class GetDataMessage {

    /**
     * Create a 'getdata' message
     *
     * @param       peer            Destination peer
     * @param       invList         Inventory item list
     * @return                      Message to be sent to the peer
     */
    public static Message buildGetDataMessage(Peer peer, List<InventoryItem> invList) {
        SerializedBuffer msgBuffer = new SerializedBuffer(invList.size()*36+4);
        msgBuffer.putVarInt(invList.size());
        invList.stream().forEach((item) -> item.getBytes(msgBuffer));
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("getdata", msgBuffer);
        return new Message(buffer, peer, MessageHeader.GETDATA_CMD);
    }

    /**
     * Process a 'getdata' message
     *
     * @param       msg                     Message
     * @param       inBuffer                Input buffer
     * @param       msgListener             Message listener
     * @throws      EOFException            End-of-data while processing message data
     * @throws      VerificationException   Data verification failed
     */
    public static void processGetDataMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {
        //
        // Build the request list
        //
        int count = inBuffer.getVarInt();
        if (count < 0 || count > 50000)
            throw new VerificationException("More than 50,000 inventory entries in 'getdata' message");
        List<InventoryItem> itemList = new ArrayList<>(count);
        for (int i=0; i<count; i++)
            itemList.add(new InventoryItem(inBuffer));
        //
        // Notify the message listener
        //
        msgListener.sendInventory(msg.getPeer(), itemList);
    }
}
