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

/**
 * The 'getaddr' message is sent to a peer to request a list of known peers.  The response
 * is an 'addr' message (the application should call AddressMessage.buildAddressMessage()
 * when it receives a 'getaddr' message)
 */
public class GetAddressMessage {

    /**
     * Build the 'getaddr' message
     *
     * @param       peer            The remote peer
     * @return                      'getaddr' message
     */
    public static Message buildGetAddressMessage(Peer peer) {
        //
        // The 'getaddr' message consists of just the message header
        //
        ByteBuffer buffer = MessageHeader.buildMessage("getaddr", new byte[0]);
        return new Message(buffer, peer, MessageHeader.GETADDR_CMD);
    }
}
