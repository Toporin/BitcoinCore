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
import java.io.InputStream;
import java.io.IOException;

/**
 * <p>The 'headers' message is returned in response to a 'getheaders' message.
 * Note that the returned header includes the block header (80 bytes) plus
 * the transaction count (although the count is set to zero)</p>
 *
 * <p>Headers Message</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   VarInt     Count               Number of headers
 *   Variable   Entries             Header entries
 * </pre>
 */
public class HeadersMessage {

    /**
     * Process the 'headers' message
     *
     * @param       msg                     Message
     * @param       inStream                Input stream
     * @param       invHandler              Inventory handler
     * @throws      EOFException            End-of-data encountered while processing input stream
     * @throws      IOException             Unable to read the input stream
     * @throws      VerificationException   Verification error
     */
    public static void processHeadersMessage(Message msg, InputStream inStream,
                                            InventoryHandler invHandler)
                                            throws EOFException, IOException, VerificationException {
        //
        // Get the header count
        //
        int hdrCount = new VarInt(inStream).toInt();
        if (hdrCount < 0 || hdrCount > 2000)
            throw new VerificationException("More than 2000 headers", NetParams.REJECT_INVALID);
        //
        // Process each block header
        //
        byte[] hdrData = new byte[BlockHeader.HEADER_SIZE];
        for (int i=0; i<hdrCount; i++) {
            int count = inStream.read(hdrData);
            if (count != BlockHeader.HEADER_SIZE)
                throw new EOFException("End-of-data processing headers");
            int txCount = new VarInt(inStream).toInt();
            if (txCount != 0)
                throw new VerificationException("Transaction count is non-zero",
                                                NetParams.REJECT_INVALID);
            invHandler.processBlockHeader(new BlockHeader(hdrData));
        }
    }
}
