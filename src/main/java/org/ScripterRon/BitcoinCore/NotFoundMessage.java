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
import java.util.Iterator;

/**
 * <p>A 'notfound' message is returned when one or more items in a 'getdata' request
 * were not found.<p>
 *
 * <p>NotFound Message:</p>
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
public class NotFoundMessage {

    /**
     * Process a 'notfound' message
     *
     * @param       msg                     Message
     * @param       inStream                Message data stream
     * @param       invHandler              Inventory handler
     * @throws      EOFException            End-of-data processing input stream
     * @throws      IOException             Unable to read input stream
     * @throws      VerificationException   Verification error
     */
    public static void processNotFoundMessage(Message msg, ByteArrayInputStream inStream,
                                            InventoryHandler invHandler)
                                            throws EOFException, IOException, VerificationException {
        byte[] bytes = new byte[36];
        //
        // Get the number of inventory vectors
        //
        int invCount = new VarInt(inStream).toInt();
        if (invCount < 0 || invCount > 50000)
            throw new VerificationException("More than 50,000 entries in 'notfound' message");
        //
        // Process the inventory vectors
        //
        for (int i=0; i<invCount; i++) {
            int count = inStream.read(bytes);
            if (count < 36)
                throw new EOFException("'inv' message is too short");
            int type = (int)Utils.readUint32LE(bytes, 0);
            Sha256Hash hash = new Sha256Hash(Utils.reverseBytes(bytes, 4, 32));
            invHandler.requestNotFound(msg.getPeer(), type, hash);
        }
    }
}
