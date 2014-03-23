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
 * A message is associated with a peer node and is sent or received using
 * the socket channel assigned to that peer.
 */
public class Message {

    /** The message buffer */
    private ByteBuffer buffer;

    /** The associated peer */
    private Peer peer;

    /** The message command */
    private int command;

    /**
     * Creates a new message
     *
     * @param       buffer          Message buffer
     * @param       peer            Associated peer or null for a broadcast message
     * @param       cmd             Message command
     */
    public Message(ByteBuffer buffer, Peer peer, int cmd) {
        this.buffer = buffer;
        this.peer = peer;
        this.command = cmd;
    }

    /**
     * Returns the peer associated with this message or null if this is a broadcast message
     *
     * @return      Peer
     */
    public Peer getPeer() {
        return peer;
    }

    /**
     * Returns the message buffer
     *
     * @return      Message buffer
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    /**
     * Sets the message buffer
     *
     * @param       buffer          Message buffer
     */
    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Returns the message command
     *
     * @return      Message command
     */
    public int getCommand() {
        return command;
    }

    /**
     * Sets the message command
     *
     * @param       cmd             Message command
     */
    public void setCommand(int cmd) {
        command = cmd;
    }

    /**
     * Creates a copy of this message for another peer
     *
     * @param       peer            The target peer
     * @return                      Cloned message
     *
     * A new ByteBuffer is created using the same byte array.  This allows multiple
     * channels to process the data at the same time.
     */
    public Message clone(Peer peer) {
        ByteBuffer newBuffer = null;
        if (buffer != null)
            newBuffer = ByteBuffer.wrap(buffer.array());
        return new Message(newBuffer, peer, command);
    }
}
