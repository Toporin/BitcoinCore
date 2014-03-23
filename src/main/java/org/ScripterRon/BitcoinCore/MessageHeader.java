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

import java.nio.ByteBuffer;

import java.util.Map;
import java.util.HashMap;

/**
 * <p>Each message on the network consists of the message header followed by an optional payload.</p>
 *
 * <p>Message Header:</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   4 bytes    Magic               Magic number
 *  12 bytes    Command             Null-terminated ASCII command
 *   4 bytes    Length              Payload length
 *   4 bytes    Checksum            First 4 bytes of the SHA-256 double digest of the payload
 * </pre>
*/
public class MessageHeader {

    /** Message header length */
    public static final int HEADER_LENGTH = 24;

    /** Checksum for zero-length payload */
    public static final byte[] ZERO_LENGTH_CHECKSUM = new byte[] {
        (byte)0x5d, (byte)0xf6, (byte)0xe0, (byte)0xe2
    };

    /** Message commands */
    public static final int ADDR_CMD = 1;
    public static final int ALERT_CMD = 2;
    public static final int BLOCK_CMD = 3;
    public static final int FILTERADD_CMD = 4;
    public static final int FILTERCLEAR_CMD = 5;
    public static final int FILTERLOAD_CMD = 6;
    public static final int GETADDR_CMD = 7;
    public static final int GETBLOCKS_CMD = 8;
    public static final int GETDATA_CMD = 9;
    public static final int GETHEADERS_CMD = 10;
    public static final int HEADERS_CMD = 11;
    public static final int INV_CMD = 12;
    public static final int INVBLOCK_CMD = 13;
    public static final int INVTX_CMD = 14;
    public static final int MEMPOOL_CMD = 15;
    public static final int MERKLEBLOCK_CMD = 16;
    public static final int NOTFOUND_CMD = 17;
    public static final int PING_CMD = 18;
    public static final int PONG_CMD = 19;
    public static final int REJECT_CMD = 20;
    public static final int TX_CMD = 21;
    public static final int VERACK_CMD = 22;
    public static final int VERSION_CMD = 23;

    /** Message command map */
    public static final Map<String, Integer> cmdMap = new HashMap<>(20);
    static {
        cmdMap.put("addr", ADDR_CMD);
        cmdMap.put("alert", ALERT_CMD);
        cmdMap.put("block", BLOCK_CMD);
        cmdMap.put("filteradd", FILTERADD_CMD);
        cmdMap.put("filterclear", FILTERCLEAR_CMD);
        cmdMap.put("filterload", FILTERLOAD_CMD);
        cmdMap.put("getaddr", GETADDR_CMD);
        cmdMap.put("getblocks", GETBLOCKS_CMD);
        cmdMap.put("getdata", GETDATA_CMD);
        cmdMap.put("getheaders", GETHEADERS_CMD);
        cmdMap.put("headers", HEADERS_CMD);
        cmdMap.put("inv", INV_CMD);
        cmdMap.put("mempool", MEMPOOL_CMD);
        cmdMap.put("merkleblock", MERKLEBLOCK_CMD);
        cmdMap.put("notfound", NOTFOUND_CMD);
        cmdMap.put("ping", PING_CMD);
        cmdMap.put("pong", PONG_CMD);
        cmdMap.put("reject", REJECT_CMD);
        cmdMap.put("tx", TX_CMD);
        cmdMap.put("verack", VERACK_CMD);
        cmdMap.put("version", VERSION_CMD);
    }

    /**
     * Builds the message header and then constructs a buffer containing the message header
     * and the message data
     *
     * @param       cmd             Message command
     * @param       msgData         Message data
     * @return      Message buffer
     */
    public static ByteBuffer buildMessage(String cmd, byte[] msgData) {
        byte[] bytes = new byte[HEADER_LENGTH+msgData.length];
        //
        // Set the magic number
        //
        Utils.uint32ToByteArrayLE(NetParams.MAGIC_NUMBER, bytes, 0);
        //
        // Set the command name
        //
        for (int i=0; i<cmd.length(); i++)
            bytes[4+i] = (byte)cmd.codePointAt(i);
        //
        // Set the payload length
        //
        Utils.uint32ToByteArrayLE(msgData.length, bytes, 16);
        //
        // Compute the payload checksum
        //
        // The message header contains a fixed checksum value when there is no payload
        //
        if (msgData.length == 0) {
            System.arraycopy(ZERO_LENGTH_CHECKSUM, 0, bytes, 20, 4);
        } else {
            byte[] digest = Utils.doubleDigest(msgData);
            System.arraycopy(digest, 0, bytes, 20, 4);
            System.arraycopy(msgData, 0, bytes, 24, msgData.length);
        }
        return ByteBuffer.wrap(bytes);
    }

    /**
     * Processes the message header and returns the message command.  A VerificationException
     * is thrown if the message header is incomplete, has an incorrect magic value, or the
     * checksum is not correct.
     *
     * @param       inStream            Message data stream
     * @param       msgBytes            Message bytes
     * @return      Message command
     * @throws      EOFException
     * @throws      IOException
     * @throws      VerificationException
     */
    public static String processMessage(ByteArrayInputStream inStream, byte[] msgBytes)
                                        throws EOFException, IOException, VerificationException {
        if (inStream.available() < HEADER_LENGTH)
            throw new EOFException("End-of-data while processing message header");
        inStream.skip(HEADER_LENGTH);
        //
        // Verify the magic number
        //
        long magic = Utils.readUint32LE(msgBytes, 0);
        if (magic != NetParams.MAGIC_NUMBER)
            throw new VerificationException(String.format("Message header magic number %d is invalid", magic));
        //
        // Verify the payload checksum
        //
        if (msgBytes.length > HEADER_LENGTH) {
            byte[] digest = Utils.doubleDigest(msgBytes, HEADER_LENGTH, msgBytes.length-HEADER_LENGTH);
            if (digest[0] != msgBytes[20] || digest[1] != msgBytes[21] ||
                                             digest[2] != msgBytes[22] || digest[3] != msgBytes[23])
                throw new VerificationException("Message checksum incorrect");
        }
        //
        // Build the command name
        //
        StringBuilder cmdString = new StringBuilder(16);
        for (int i=4; i<16; i++) {
            if (msgBytes[i] == 0)
                break;

            cmdString.appendCodePoint(((int)msgBytes[i])&0xff);
        }
        return cmdString.toString();
    }
}
