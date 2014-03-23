/**
 * Copyright 2011 Google Inc.
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * A PeerAddress holds an IP address and port number representing the network location of
 * a peer in the Bitcoin Peer-to-Peer network.
 */
public class PeerAddress {

    /** The IP address */
    private InetAddress address;

    /** The IP port */
    private int port;

    /** Time seen */
    private long timeSeen;

    /** Peer services */
    private long services;

    /** Peer connected */
    private boolean connected;

    /** Static address */
    private boolean staticAddress;

    /**
     * Constructs a peer address from the given IP address and port
     *
     * @param       address         IP address
     * @param       port            IP port
     */
    public PeerAddress(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        timeSeen = System.currentTimeMillis()/1000;
    }

    /**
     * Constructs a peer address from the given IP address and port
     *
     * @param       address         IP address
     * @param       port            IP port
     * @param       timeSeen        Latest time peer was seen
     */
    public PeerAddress(InetAddress address, int port, long timeSeen) {
        this.address = address;
        this.port = port;
        this.timeSeen = timeSeen;
    }

    /**
     * Constructs a peer address from a network socket
     *
     * @param       socket          Network socket
     */
    public PeerAddress(InetSocketAddress socket) {
        this(socket.getAddress(), socket.getPort());
    }

    /**
     * Constructs a peer address from a string in the format "[address]:port" where
     * address can be "nnn.nnn.nnn.nnn" for IPv4 or "xxxx:xxxx:xxxx;xxxx:xxxx:xxxx:xxxx:xxxx"
     * for IPv6.
     *
     * @param       peerString              Address string
     * @throws      UnknownHostException    Incorrect address format
     */
    public PeerAddress(String peerString) throws UnknownHostException {
        //
        // Separate the address and the port
        //
        int addrSep = peerString.lastIndexOf(']');
        int portSep = peerString.lastIndexOf(':');
        if (peerString.charAt(0) != '[' || addrSep < 0 ||
                                portSep < addrSep || portSep == peerString.length()-1)
                throw new UnknownHostException("Incorrect [address]:port format");
        String addrString = peerString.substring(1, addrSep);
        String portString = peerString.substring(portSep+1);
        //
        // Decode the address
        //
        byte[] addrBytes;
        if (addrString.indexOf('.') >= 0) {
            String[] addrParts = addrString.split("\\D");
            if (addrParts.length != 4)
                throw new UnknownHostException("Incorrect IPv4 address format");
            addrBytes = new byte[4];
            for (int j=0; j<4; j++)
                addrBytes[j] = (byte)Integer.parseInt(addrParts[j]);
        } else if (addrString.indexOf(':') >= 0) {
            String[] addrParts = addrString.split(":");
            if (addrParts.length != 8)
                throw new UnknownHostException("Incorrect IPv6 address format");
            addrBytes = new byte[16];
            int offset = 0;
            for (int j=0; j<8; j++) {
                if (addrParts[j].length() == 0) {
                    offset += 2;
                } else {
                    int nibble = Integer.parseInt(addrParts[j], 16);
                    addrBytes[offset++] = (byte)(nibble>>8);
                    addrBytes[offset++] = (byte)nibble;
                }
            }
        } else {
            throw new UnknownHostException("Incorrect [address]:port format");
        }
        //
        // Create the address and port values
        //
        address = InetAddress.getByAddress(addrBytes);
        port = Integer.parseInt(portString);
        timeSeen = System.currentTimeMillis()/1000;
    }

    /**
     * Returns the IP address
     *
     * @return                      IP address
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Sets the IP address
     *
     * @param       address         IP address
     */
    public void setAddress(InetAddress address) {
        this.address = address;
    }

    /**
     * Returns the IP port
     *
     * @return                      IP port
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the IP port
     *
     * @param       port            IP port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the timestamp for this peer
     *
     * @return      Timestamp in seconds since the epoch
     */
    public long getTimeStamp() {
        return timeSeen;
    }

    /**
     * Sets the timestamp for this peer
     *
     * @param       timeSeen        Time peer was seen in seconds since the epoch
     */
    public void setTimeStamp(long timeSeen) {
        this.timeSeen = timeSeen;
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
     * Checks if this peer is connected
     *
     * @return      TRUE if the peer is connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Sets the peer connection status
     *
     * @param       isConnected     TRUE if the peer is connected
     */
    public void setConnected(boolean isConnected) {
        connected = isConnected;
    }

    /**
     * Check if this is a static address
     *
     * @return      TRUE if this is a static address
     */
    public boolean isStatic() {
        return staticAddress;
    }

    /**
     * Set the address type
     *
     * @param       isStatic        TRUE if this is a static address
     */
    public void setStatic(boolean isStatic) {
        staticAddress = isStatic;
    }

    /**
     * Return a socket address for our IP address and port
     *
     * @return                      Socket address
     */
    public InetSocketAddress toSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    /**
     * Returns a string representation of the IP address and port
     *
     * @return                      String representation
     */
    @Override
    public String toString() {
        return String.format("[%s]:%d", address.getHostAddress(), port);
    }

    /**
     * Checks if the supplied address is equal to this address
     *
     * @param       obj             Address to check
     * @return                      TRUE if the addresses are equal
     */
    @Override
    public boolean equals(Object obj) {
        boolean areEqual = false;
        if (obj != null && (obj instanceof PeerAddress)) {
            PeerAddress other = (PeerAddress)obj;
            areEqual = (address.equals(other.address) && port == other.port);
        }

        return areEqual;
    }

    /**
     * Returns the hash code for this object
     *
     * @return                      The hash code
     */
    @Override
    public int hashCode() {
        return (address.hashCode()^port);
    }
}
