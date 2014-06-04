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
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>A 'reject' message is sent when the receiver rejects a message.  The message
 * contains a reason code and text description for the rejection.  There is no
 * response to the message - it is merely a diagnostic aid.  However, the sender
 * may disconnect if too many rejections occur.</p>
 *
 * <p>Reject Message</p>
 * <pre>
 *   Size       Field           Description
 *   ====       =====           ===========
 *   VarString  Command         The failing command
 *   1 byte     Reason          The reason code
 *   VarString  Description     Descriptive text
 *   32 bytes   Hash            Block hash ('block') or transaction hash ('tx'), omitted otherwise
 * </pre>
 */
public class RejectMessage {

    /**
     * Builds a 'reject' message to be sent to the destination peer
     *
     * @param       peer            Destination peer
     * @param       cmd             Failing command
     * @param       reason          Reason code
     * @param       description     Descriptive text
     * @return                      Message to be sent to the peer
     */
    public static Message buildRejectMessage(Peer peer, String cmd, int reason, String description) {
        return buildRejectMessage(peer, cmd, reason, description, Sha256Hash.ZERO_HASH);
    }

    /**
     * Builds a 'reject' message to be sent to the destination peer
     *
     * @param       peer            Destination peer
     * @param       cmd             Failing command
     * @param       reason          Reason code
     * @param       desc            Descriptive text
     * @param       hash            Block or transaction hash
     * @return                      Message to be sent to the peer
     */
    public static Message buildRejectMessage(Peer peer, String cmd, int reason, String desc, Sha256Hash hash) {
        byte[] cmdLength = VarInt.encode(cmd.length());
        byte[] descLength = VarInt.encode(desc.length());
        byte[] msgData = null;
        //
        // Build the message payload
        //
        try {
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(3+cmd.length()+desc.length()+32)) {
                outStream.write(cmdLength);
                for (int i=0; i<cmd.length(); i++)
                    outStream.write(cmd.codePointAt(i));
                outStream.write(reason);
                outStream.write(descLength);
                for (int i=0; i<desc.length(); i++)
                    outStream.write(desc.codePointAt(i));
                if (!hash.equals(Sha256Hash.ZERO_HASH))
                    outStream.write(hash.getBytes());
                msgData = outStream.toByteArray();
            }
        } catch (IOException exc) {
            // Should never happen
        }
        //
        // Build the message
        //
        ByteBuffer buffer = MessageHeader.buildMessage("reject", msgData);
        return new Message(buffer, peer, MessageHeader.REJECT_CMD);
    }

    /**
     * Processes a 'reject' message
     *
     * @param       msg             Message
     * @param       inStream        Message data stream
     * @param       invHandler      Inventory handler
     * @throws      EOFException    Serialized byte stream is too short
     * @throws      IOException     Error reading from input stream
     */
    public static void processRejectMessage(Message msg, ByteArrayInputStream inStream,
                                    InventoryHandler invHandler)
                                    throws EOFException, IOException {
        //
        // Get the command name
        //
        int cmdLength = new VarInt(inStream).toInt();
        if (cmdLength < 0 || cmdLength > inStream.available())
            throw new EOFException("End-of-data processing 'reject' message");
        StringBuilder cmdString = new StringBuilder(cmdLength);
        for (int i=0; i<cmdLength; i++)
            cmdString.appendCodePoint(inStream.read());
        String cmd = cmdString.toString();
        //
        // Get the reason code
        //
        int reasonCode = inStream.read();
        if (reasonCode < 0)
            throw new EOFException("End-of-data processing 'reject' message");
        //
        // Get the description
        //
        int descLength = new VarInt(inStream).toInt();
        if (descLength < 0 || descLength > inStream.available())
            throw new EOFException("End-of-data processing 'reject' message");
        StringBuilder descString = new StringBuilder(descLength);
        for (int i=0; i<descLength; i++)
            descString.appendCodePoint(inStream.read());
        String desc = descString.toString();
        //
        // Get the hash
        //
        Sha256Hash hash = Sha256Hash.ZERO_HASH;
        if (inStream.available() >= 32) {
            byte[] bytes = new byte[32];
            inStream.read(bytes);
            hash = new Sha256Hash(Utils.reverseBytes(bytes));
        }
        //
        // Notify the inventory handler
        //
        invHandler.processReject(msg.getPeer(), cmd, reasonCode, desc, hash);
    }
}
