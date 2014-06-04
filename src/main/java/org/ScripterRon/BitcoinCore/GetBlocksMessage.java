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
 *  32 bytes    Stop                Hash of the last desired block or zero to get as many as possible
 * </pre>
 */
public class GetBlocksMessage {

    /**
     * Build a 'getblocks' message
     *
     * @param       peer            Destination peer
     * @param       invList         Block hash list
     * @return                      Message to send to the peer
     */
    public static Message buildGetBlocksMessage(Peer peer, List<Sha256Hash> invList) {
        //
        // Build the message payload
        //
        // The protocol version will be set to the lesser of our version and the peer version
        // The stop locator will be set to zero since we don't know the network chain head.
        //
        int varCount = invList.size();
        byte[] varBytes = VarInt.encode(varCount);
        byte[] msgData = new byte[4+varBytes.length+varCount*32+32];
        Utils.uint32ToByteArrayLE(Math.min(NetParams.PROTOCOL_VERSION, peer.getVersion()), msgData, 0);
        System.arraycopy(varBytes, 0, msgData, 4, varBytes.length);
        int offset = 4+varBytes.length;
        for (Sha256Hash blockHash : invList) {
            System.arraycopy(Utils.reverseBytes(blockHash.getBytes()), 0, msgData, offset, 32);
            offset+=32;
        }
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("getblocks", msgData);
        return new Message(buffer, peer, MessageHeader.GETBLOCKS_CMD);
    }
}
