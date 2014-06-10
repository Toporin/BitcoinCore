/**
 * Copyright 2014 Ronald W Hoffman
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

/**
 * The 'filterclear' message clear the bloom filter for the client.
 *
 * The message consists of just the message header.
 */
public class FilterClearMessage {

    /**
     * Build a 'filterclear' message
     *
     * @param       peer            Destination peer
     * @return                      'filterclear' message
     */
    public static Message buildFilterClearMessage(Peer peer) {
        ByteBuffer buffer = MessageHeader.buildMessage("filterclear", new byte[0]);
        return new Message(buffer, peer, MessageHeader.FILTERCLEAR_CMD);
    }

    /**
     * Process a 'filterclear' message
     *
     * The existing Bloom filter will be cleared
     *
     * @param       msg             Message
     * @param       inBuffer        Input buffer
     * @param       msgListener     Message listener
     */
    public static void processFilterClearMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener) {
        Peer peer = msg.getPeer();
        //
        // Clear the current Bloom filter
        //
        BloomFilter oldFilter = peer.getBloomFilter();
        peer.setBloomFilter(null);
        //
        // Notify the message listener
        //
        msgListener.processFilterClear(peer, oldFilter);
    }
}
