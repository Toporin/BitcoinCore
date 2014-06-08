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
 * <p>A 'notfound' message is returned when one or more items in a 'getdata' request
 * were not found.</p>
 *
 * <p>NotFound Message:</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   VarInt     Count               Number of inventory items
 *   Variable   InvItems            One or more inventory items
 * </pre>
 */
public class NotFoundMessage {

    /**
     * Build a 'notfound' message
     *
     * @param       peer                    Destination peer
     * @param       itemList                Inventory item list
     * @return                              Message to send to the peer
     */
    public static Message buildNotFoundMessage(Peer peer, List<InventoryItem> itemList) {
        //
        // Build the message data
        //
        SerializedBuffer msgBuffer = new SerializedBuffer(4+itemList.size()*36);
        msgBuffer.putVarInt(itemList.size());
        itemList.stream().forEach((item) -> item.getBytes(msgBuffer));
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("notfound", msgBuffer);
        return new Message(buffer, peer, MessageHeader.NOTFOUND_CMD);
    }

    /**
     * Process a 'notfound' message
     *
     * @param       msg                     Message
     * @param       inBuffer                Input buffer
     * @param       msgListener             Message listener
     * @throws      EOFException            End-of-data processing input stream
     * @throws      VerificationException   Verification error
     */
    public static void processNotFoundMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {
        //
        // Build the inventory item list
        //
        int count = inBuffer.getInt();
        if (count < 0 || count > 1000)
            throw new VerificationException("More than 1000 entries in 'notfound' message");
        List<InventoryItem> itemList = new ArrayList<>(count);
        for (int i=0; i<count; i++)
            itemList.add(new InventoryItem(inBuffer));
        //
        // Notify the message listener
        //
        msgListener.requestNotFound(msg.getPeer(), itemList);
    }
}
