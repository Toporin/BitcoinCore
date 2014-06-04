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
import java.util.ArrayList;
import java.util.List;

/**
 * <p>An alert is sent out by the development team to notify all peers in the network
 * about a problem.  The alert is displayed in the user interface and written to the
 * log.  It is also sent each time a node connects to another node until the relay time
 * is exceeded or the alert is canceled.</p>
 *
 * <p>Alert Payload</p>
 * <pre>
 *   Size       Field           Description
 *   ====       =====           ===========
 *   4 bytes    Version         Alert version
 *   8 bytes    RelayUntil      Relay the alert until this time (seconds)
 *   8 bytes    Expires         Alert expires at this time (seconds)
 *   4 bytes    AlertID         Unique identifier for this alert
 *   4 bytes    CancelID        Cancel the alert with this identifier
 *    IntSet    CancelSet       Set of alert identifiers to cancel
 *   4 bytes    MinVersion      Minimum applicable protocol version
 *   4 bytes    MaxVersion      Maximum applicable protocol version
 *    StrSet    SubVersionSet   Applicable subversions
 *   4 bytes    Priority        Relative priority
 *   String     Comment         Comment about the alert
 *   String     Status          Alert message to display and log
 *   String     Reserved        Reserved for future use
 * </pre>
 */
public class Alert {

    /** Alert payload */
    private final byte[] payload;

    /** Alert signature */
    private final byte[] signature;

    /** Alert version */
    private int version;

    /** Relay until time */
    private long relayTime;

    /** Expiration time */
    private long expireTime;

    /** Alert ID */
    private int alertID;

    /** Cancel ID */
    private int cancelID;

    /** Cancel set */
    private List<Integer> cancelSet;

    /** Min applicable version */
    private int minVersion;

    /** Max applicable version */
    private int maxVersion;

    /** Subversion */
    private List<String> subVersions;

    /** Priority */
    private int priority;

    /** Comment */
    private String comment;

    /** Message */
    private String message;

    /** Cancel status */
    private boolean isCanceled;

    /**
     * Creates an alert from the serialized message data
     *
     * @param       payload             Alert payload
     * @param       signature           Alert signature
     * @throws      EOFException        End-of-data while processing payload
     * @throws      IOException         Error while reading serialized data
     */
    public Alert(byte[] payload, byte[] signature) throws EOFException, IOException {
        this.payload = payload;
        this.signature = signature;
        //
        // Decode the payload
        //
        try (ByteArrayInputStream inStream = new ByteArrayInputStream(payload)) {
            byte[] bytes = new byte[28];
            //
            // Get version, relayTime, expireTime, alertID and cancelID
            //
            int count = inStream.read(bytes);
            if (count != 28)
                throw new EOFException("End-of-data processing payload");
            version = (int)Utils.readUint32LE(bytes, 0);
            relayTime = Utils.readUint64LE(bytes, 4);
            expireTime = Utils.readUint64LE(bytes, 12);
            alertID = (int)Utils.readUint32LE(bytes, 20);
            cancelID = (int)Utils.readUint32LE(bytes, 24);
            //
            // Get the cancel set
            //
            int setCount = new VarInt(inStream).toInt();
            if (setCount > 0) {
                cancelSet = new ArrayList<>(setCount);
                for (int i=0; i<setCount; i++) {
                    count = inStream.read(bytes, 0, 4);
                    if (count != 4)
                        throw new EOFException("End-of-data processing payload");
                    cancelSet.add((int)Utils.readUint32LE(bytes, 0));
                }
            } else {
                cancelSet = new ArrayList<>(1);
            }
            //
            // Get minVersion and maxVersion
            //
            count = inStream.read(bytes, 0, 8);
            if (count != 8)
                throw new EOFException("End-of-data processing payload");
            minVersion = (int)Utils.readUint32LE(bytes, 0);
            maxVersion = (int)Utils.readUint32LE(bytes, 4);
            //
            // Get the subversions
            //
            int subCount = new VarInt(inStream).toInt();
            if (subCount == 0) {
                subVersions = new ArrayList<>(1);
            } else {
                subVersions = new ArrayList<>(subCount);
                for (int i=0; i<subCount; i++) {
                    int strLength = new VarInt(inStream).toInt();
                    StringBuilder subString = new StringBuilder(strLength);
                    for (int j=0; j<strLength; j++) {
                        int codePoint = inStream.read();
                        if (codePoint < 0)
                            throw new EOFException("End-of-data processing payload");
                        subString.appendCodePoint(codePoint);
                    }
                    subVersions.add(subString.toString());
                }
            }
            //
            // Get the priority
            //
            count = inStream.read(bytes, 0, 4);
            if (count != 4)
                throw new EOFException("End-of-data processing payload");
            priority = (int)Utils.readUint32LE(bytes, 0);
            //
            // Get the comment
            //
            int strLength = new VarInt(inStream).toInt();
            if (strLength == 0) {
                comment = "";
            } else {
                StringBuilder subString = new StringBuilder(strLength);
                for (int i=0; i<strLength; i++) {
                    int codePoint = inStream.read();
                    if (codePoint < 0)
                        throw new EOFException("End-of-data processing payload");
                    subString.appendCodePoint(codePoint);
                }
                comment = subString.toString();
            }
            //
            // Get the alert message
            //
            strLength = new VarInt(inStream).toInt();
            if (strLength == 0) {
                message = "";
            } else {
                StringBuilder subString = new StringBuilder(strLength);
                for (int i=0; i<strLength; i++) {
                    int codePoint = inStream.read();
                    if (codePoint < 0)
                        throw new EOFException("End-of-data processing payload");
                    subString.appendCodePoint(codePoint);
                }
                message = subString.toString();
            }
        }
    }

    /**
     * Returns the payload
     *
     * @return      Alert payload
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Returns the signature
     *
     * @return      Alert signature
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Returns the alert ID
     *
     * @return      Alert ID
     */
    public int getID() {
        return alertID;
    }

    /**
     * Returns the cancel ID
     *
     * @return      Cancel ID
     */
    public int getCancelID() {
        return cancelID;
    }

    /**
     * Returns the cancel set
     *
     * @return      Cancel set
     */
    public List<Integer> getCancelSet() {
        return cancelSet;
    }

    /**
     * Returns the alert message
     *
     * @return      Alert message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the relay until time
     *
     * @return      Relay until time
     */
    public long getRelayTime() {
        return relayTime;
    }

    /**
     * Returns the expiration time
     *
     * @return      Expiration time
     */
    public long getExpireTime() {
        return expireTime;
    }

    /**
     * Returns the alert priority
     *
     * @return      Alert priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Returns the message version
     *
     * @return      Alert message version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Returns the minimum protocol version
     *
     * @return      Minimum protocol version
     */
    public int getMinVersion() {
        return minVersion;
    }

    /**
     * Returns the maximum protocol version
     *
     * @return      Maximum protocol version
     */
    public int getMaxVersion() {
        return maxVersion;
    }

    /**
     * Returns the user agent (subVersion) list
     *
     * @return      User agent list
     */
    public List<String> getSubVersions() {
        return subVersions;
    }

    /**
     * Returns the developer comment
     *
     * @return      Developer comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Checks if the alert has been canceled
     *
     * @return      TRUE if the alert has been canceled
     */
    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Sets the alert cancel status
     *
     * @param       isCanceled      TRUE if the alert has been canceled
     */
    public void setCancel(boolean isCanceled) {
        this.isCanceled = isCanceled;
    }
}
