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

    /**
     * SerializedInputStream keeps tracks of the bytes consumed when deserializing an object/
     * This byte stream is then returned for use later when the transaction needs to be
     * re-serialized.  This is important because hash values are computed using the
     * serialized byte stream and it is possible to have different representations of the
     * same byte stream due to differences in VarInt processing.
     */
public class SerializedInputStream extends ByteArrayInputStream {

    /** Starting position within the byte array */
    private int startPos;

    /**
     * Creates a serialized input stream for a byte array
     *
     * @param       serializedData      Serialized input stream
     */
    public SerializedInputStream(byte[] serializedData) {
            super(serializedData);
    }

    /**
     * Creates a serialized input stream for a byte array
     *
     * @param       serializedData      Serialized input stream
     * @param       offset              Starting offset within the array
     * @param       count               Number of input bytes
     */
    public SerializedInputStream(byte[] serializedData, int offset, int count) {
        super(serializedData, offset, count);
    }

    /**
     * Sets the starting position to the current position within the byte array
     */
    public void setStart() {
        startPos = pos;
    }

    /**
     * Return a byte array representing the bytes consumed from the start
     * position to the current position
     *
     * @return      Byte array
     */
    public byte[] getBytes() {
        return getBytes(startPos);
    }

    /**
     * Returns a byte array representing the bytes consumed from the specified
     * start position to the current position
     *
     * @param       startPos            The starting position
     * @return      Byte array
     */
    public byte[] getBytes(int startPos) {
        if (startPos > pos)
            throw new IllegalArgumentException("Start position greater than current position");
        int length = pos - startPos;
        byte[] dataBytes = new byte[length];
        System.arraycopy(buf, startPos, dataBytes, 0, length);
        return dataBytes;
    }
}
