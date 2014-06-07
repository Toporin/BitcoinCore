/**
 * Copyright 2013 Ronald W Hoffman
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
import java.nio.ByteBuffer;

/**
 * <p>The 'filteradd' message is sent to add an additional element to an existing Bloom
 * filter.</p>
 *
 * <p>FilterAdd Message</p>
 * <pre>
 *   Size       Field           Description
 *   ====       =====           ===========
 *   VarInt     Count           Number of bytes in the filter element (maximum of 520)
 *   Variable   Element         Filter element
 * </pre>
 */
public class FilterAddMessage {

    /**
     * Build a 'filteradd' message
     *
     * @param       peer                    The destination peer
     * @param       elem                    The filter element
     * @return                              Message to send to peer
     */
    public static Message buildFilterAddMessage(Peer peer, byte[] elem) {
        SerializedBuffer outBuffer = new SerializedBuffer();
        outBuffer.putVarInt(elem.length)
                 .putBytes(elem);
        ByteBuffer buffer = MessageHeader.buildMessage("filteradd", outBuffer);
        return new Message(buffer, peer, MessageHeader.FILTERADD_CMD);
    }

    /**
     * Processes a 'filteradd' message
     *
     * @param       msg                     Message
     * @param       inBuffer                Input buffer
     * @param       msgListener             Message listener
     * @throws      EOFException            End-of-data while processing stream
     * @throws      VerificationException   Message verification failed
     */
    public static void processFilterAddMessage(Message msg, SerializedBuffer inBuffer, MessageListener msgListener)
                                            throws EOFException, VerificationException {
        //
        // Get the filter element
        //
        byte[] filterData = inBuffer.getBytes();
        if (filterData.length > 520)
            throw new VerificationException("Filter element length is greater than 520 bytes");
        //
        // Add the element to the existing filter (the 'filteradd' request will be ignored
        // if a 'filterload' request has not been done)
        //
        Peer peer = msg.getPeer();
        synchronized(peer) {
            BloomFilter filter = peer.getBloomFilter();
            if (filter != null)
                filter.insert(filterData);
        }
    }
}
