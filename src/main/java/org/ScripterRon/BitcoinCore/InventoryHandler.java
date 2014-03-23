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
 * It is called during the processing of a 'getdata', 'headers' or 'inv' message.
 */
public interface InventoryHandler {

    /**
     * Sends the requested inventory item to the requesting peer
     *
     * @param       peer            Peer requesting the inventory item
     * @param       type            Type of inventory item (INV_BLOCK or INV_TX)
     * @param       hash            Item hash
     * @return                      TRUE if the item was sent, FALSE if it was not sent
     */
    public boolean sendInventory(Peer peer, int type, Sha256Hash hash);

    /**
     * Request an inventory item if desired
     *
     * @param       peer            Peer broadcasting inventory item
     * @param       type            Type of inventory item (INV_BLOCK or INV_TX)
     * @param       hash            Item hash
     */
    public void requestInventory(Peer peer, int type, Sha256Hash hash);

    /**
     * Process a block header received from a remote peer
     *
     * @param       blockHeader     Block header
     */
    public void processBlockHeader(BlockHeader blockHeader);
}
