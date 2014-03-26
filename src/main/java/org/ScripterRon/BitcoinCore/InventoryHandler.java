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

/**
 * An InventoryHandler is responsible for handling inventory requests.
 * It is called during message processing to handle application-specific tasks.
 */
public interface InventoryHandler {

    /**
     * Sends the requested inventory item to the requesting peer.  This method
     * is called when a 'getdata' message is processed.
     *
     * @param       peer            Peer requesting the inventory item
     * @param       type            Type of inventory item (INV_BLOCK or INV_TX)
     * @param       hash            Item hash
     * @return                      TRUE if the item was sent, FALSE if it was not sent
     */
    public boolean sendInventory(Peer peer, int type, Sha256Hash hash);

    /**
     * Requests an available inventory item if desired.  This method is
     * called when an 'inv' message is processed.
     *
     * @param       peer            Peer announcing inventory item
     * @param       type            Type of inventory item (INV_BLOCK or INV_TX)
     * @param       hash            Item hash
     */
    public void requestInventory(Peer peer, int type, Sha256Hash hash);

    /**
     * Processes a completed request.  This method is called when a 'merkleblock'
     *  or 'tx' message is processed.
     *
     * @param       peer            Peer sending the response
     * @param       type            Type of inventory item (INV_FILTERED_BLOCK or INV_TX)
     * @param       hash            Item hash
     */
    public void requestCompleted(Peer peer, int type, Sha256Hash hash);
    
    /**
     * Processes a request that was returned by the peer because the inventory item was 
     * not found.  The request can be discarded or retried by sending it to a different
     * peer.  This method is called when a 'notfound' message is processed.
     * 
     * @param       peer            Peer sending the response
     * @param       type            Type of inventory item (INV_BLOCK or INV_TX)
     * @param       hash            Item hash
     */
    public void requestNotFound(Peer peer, int type, Sha256Hash hash);

    /**
     * Processes a block header received from a remote peer.  This method is
     * called when a 'headers' or 'merkleblock' message is processed.
     *
     * @param       blockHeader     Block header
     */
    public void processBlockHeader(BlockHeader blockHeader);
    
    /**
     * Processes a transaction received from a remote peer.  This method is
     * called when a 'tx' message is processed
     * 
     * @param       tx              Transaction
     */
    public void processTransaction(Transaction tx);
    
    /**
     * Processes a rejection from a peer.  This method is called when a 'reject'
     * message is processed.
     * 
     * @param       peer            Peer sending the message
     * @param       cmd             Failing message command
     * @param       reasonCode      Failure reason code
     * @param       description     Description of the failure
     * @param       hash            Item hash
     */
    public void processReject(Peer peer, String cmd, int reasonCode, String description, Sha256Hash hash);
}
