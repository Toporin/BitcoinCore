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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import java.util.LinkedList;
import java.util.List;

/**
 * The bitcoin network consists of peer nodes which establish communication links
 * between themselves.  The nodes exchange blocks and transactions which are used
 * to create new blocks which are then added to the block chain.
 *
 * The Peer object contains the information needed to handle communications with a
 * remote peer.
 */
public class Peer {

    /** Peer address */
    private PeerAddress address;

    /** Socket channel */
    private SocketChannel channel;

    /** Selection key */
    private SelectionKey key;

    /** Current input buffer */
    private ByteBuffer inputBuffer;

    /** Output message list */
    private final List<Message> outputList = new LinkedList<>();

    /** Current output buffer */
    private ByteBuffer outputBuffer;

    /** Disconnect peer */
    private boolean disconnectPeer;

    /** Connected status */
    private boolean connected;

    /** Peer protocol version */
    private int version;

    /** Peer services */
    private long services;

    /** User agent */
    private String userAgent;

    /** Peer chain height */
    private int chainHeight;

    /** Version handshake count */
    private int versionCount;

    /** Current ban score */
    private int banScore;

    /** Ping sent */
    private boolean pingSent;

    /**
     * Creates a new peer
     *
     * @param       address         The network address for this peer
     * @param       channel         The socket channel for this peer
     * @param       key             The selection key for this peer
     */
    public Peer(PeerAddress address, SocketChannel channel, SelectionKey key) {
        this.address = address;
        this.channel = channel;
        this.key = key;
    }

    /**
     * Returns the peer address
     *
     * @return      Peer address
     */
    public PeerAddress getAddress() {
        return address;
    }

    /**
     * Returns the socket channel
     *
     * @return      Socket channel
     */
    public SocketChannel getChannel() {
        return channel;
    }

    /**
     * Returns the selection key
     *
     * @return      Selection key
     */
    public SelectionKey getKey() {
        return key;
    }

    /**
     * Returns the current input buffer
     *
     * @return      Input buffer
     */
    public ByteBuffer getInputBuffer() {
        return inputBuffer;
    }

    /**
     * Sets the current input buffer
     *
     * @param       buffer              The new input buffer
     */
    public void setInputBuffer(ByteBuffer buffer) {
        this.inputBuffer = buffer;
    }

    /**
     * Returns the output message list
     *
     * @return      Output message list
     */
    public List<Message> getOutputList() {
        return outputList;
    }

    /**
     * Returns the current output buffer
     *
     * @return      Output buffer
     */
    public ByteBuffer getOutputBuffer() {
        return outputBuffer;
    }

    /**
     * Sets the current output buffer
     *
     * @param       buffer              The new output buffer
     */
    public void setOutputBuffer(ByteBuffer buffer) {
        this.outputBuffer = buffer;
    }

    /**
     * Set connected state
     *
     * @param       connected           TRUE if the peer is connected
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    /** Checks if this peer is connected
     *
     * @return      TRUE if the peer is connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Checks if we should disconnect the peer
     *
     * @return      TRUE if peer should be disconnected
     */
    public boolean shouldDisconnect() {
        return disconnectPeer;
    }

    /**
     * Sets the peer disconnect status
     *
     * @param       disconnect          TRUE to disconnect the peer
     */
    public void setDisconnect(boolean disconnect) {
        this.disconnectPeer = disconnect;
    }

    /**
     * Sets the peer version
     *
     * @param       version             Peer protocol version
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Gets the peer version
     *
     * @return      Peer version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets the ban score for this peer
     *
     * @param       banScore            New ban score
     */
    public void setBanScore(int banScore) {
        this.banScore = banScore;
    }

    /**
     * Returns the current ban score for this peer
     *
     * @return      Current ban score
     */
    public int getBanScore() {
        return banScore;
    }

    /**
     * Sets the peer services
     *
     * @param       services            Peer services
     */
    public void setServices(long services) {
        this.services = services;
    }

    /**
     * Returns the peer services
     *
     * @return      Peer services
     */
    public long getServices() {
        return services;
    }

    /**
     * Sets the user agent
     *
     * @param       userAgent           User agent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Returns the user agent
     *
     * @return      User agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the chain height
     *
     * @param       chainHeight         Chain height
     */
    public void setHeight(int chainHeight) {
        this.chainHeight = chainHeight;
    }

    /**
     * Returns the chain height
     *
     * @return      Chain height
     */
    public int getHeight() {
        return chainHeight;
    }

    /**
     * Returns the version handshake count
     *
     * @return      Version handshake count
     */
    public int getVersionCount() {
        return versionCount;
    }

    /**
     * Increments the version handshake count
     */
    public void incVersionCount() {
        versionCount++;
    }

    /**
     * Checks if a 'ping' message has been sent to this peer
     *
     * @return      TRUE if a ping has been sent
     */
    public boolean wasPingSent() {
        return pingSent;
    }

    /**
     * Sets the ping status for this peer
     *
     * @param       pingSent        TRUE if a ping has been sent
     */
    public void setPing(boolean pingSent) {
        this.pingSent = pingSent;
    }
}
