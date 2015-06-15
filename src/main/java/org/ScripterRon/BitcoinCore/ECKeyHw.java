/*
 * java API for the SatoChip Bitcoin Hardware Wallet
 * (c) 2015 by Toporin - 16DMCk4WUaHofchAhpMaQS4UPm4urcy2dN
 * Sources available on https://github.com/Toporin
 * 
 * Copyright 2015 by Toporin (https://github.com/Toporin)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.BitcoinCore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Base64;
import org.satochip.satochipclient.CardConnector;
import org.satochip.satochipclient.CardConnectorException;
import org.satochip.satochipclient.CardDataParser;
import org.satochip.satochipclient.JCconstants;

public class ECKeyHw extends ECKey{
    
    /** constants*/
    private static final byte HW_TYPE_BIP32=0x00;
    private static final byte HW_TYPE_STD=0x01;
    private static final boolean DOUBLESHA256=true;
    private static final boolean SINGLESHA256=false;
    
    /** CardConnector object for javacard wallet*/
    private static CardConnector cardConnector;
    
    /** Bip32 public authentication key derived uniquely from seed*/
    private static byte[] authentikey= null;
    
    /** Type of private key storage: HD with BIP32 or standard single key*/
    private byte hw_type;
    
    /** Bip32 HDkey path */
    private List<Integer> bip32path=null;
    
    /** Standard (single) key referenced by a 1-byte key number*/
    private byte keynbr=(byte)0xff;

    /**
     * Creates an ECKey with the private key secured in a hardware dongle (SatoChip).  
     * The private key is defined from the BIP32 path provided as a list of Integer.
     * The public key will be generated in the dongle from the private key. 
     *
     * @param       bip32path           BIP32 derivation path from HD master key to derived key
     */
    public ECKeyHw(List<Integer> bip32path, byte[] pubkey) {
        this.bip32path=bip32path;
        this.pubKey= pubkey;
        this.hw_type= HW_TYPE_BIP32;
        this.isCompressed= (pubkey.length==33);
    }
        
    /**
     * Creates an ECKey with the private key secured in a hardware dongle (SatoChip).  
     * The private key is defined from the BIP32 path provided as a list of Integer.
     * The public key will be generated in the dongle from the private key. 
     *
     * @param       bip32path           BIP32 derivation path from HD master key to derived key
     * @throws org.ScripterRon.BitcoinCore.ECException
     */
    public ECKeyHw(List<Integer> bip32path) throws ECException, CardConnectorException {
        if (bip32path.size()>10)
            throw new IllegalArgumentException("BIP32 path is too long");
        
        this.bip32path = bip32path;
        this.pubKey= getBip32ExtendedKey(bip32path);
        this.hw_type= HW_TYPE_BIP32;
        this.creationTime= System.currentTimeMillis()/1000;
        this.isCompressed= (this.pubKey.length==33);
    }
    
    /**
     * Creates an ECKey with the private key secured in a hardware dongle (SatoChip).  
     * The private key is defined from the BIP32 path provided as a list of Integer.
     * The public key will be generated in the dongle from the private key. 
     *
     * @param   keynbr
     * @param   pubkey
     */
    public ECKeyHw(byte keynbr, byte[] pubkey) {
        this.keynbr=keynbr;
        this.pubKey= pubkey;
        this.hw_type= HW_TYPE_STD;
        this.isCompressed= (pubkey.length==33);
    }

     /**
     * Creates an ECKey with the private key secured in a hardware dongle (SatoChip).  
     * The private key is provided as a BigInteger and imported in the dongle.
     * The public key will be regenerated from the dongle with the private key. 
     *
     * @param       keynbr           key slot in the dongle (<MAX_NUM_KEY)
     * @param       privKey         EC private key value 
     * @throws org.ScripterRon.BitcoinCore.ECException 
     * @throws musclecardclient.CardConnectorException 
     */
    public ECKeyHw(byte keynbr, byte[] privKey, int val) throws ECException, CardConnectorException {
        this.keynbr=keynbr;
        this.pubKey= importStdKey(keynbr, privKey);
        this.hw_type= HW_TYPE_STD;
        this.creationTime= System.currentTimeMillis()/1000;
        this.isCompressed= (this.pubKey.length==33);
    }
    
    public ECKeyHw(byte keynbr) throws ECException, CardConnectorException{
        this.keynbr=keynbr;
        this.pubKey= getStandardKey(keynbr);
        this.hw_type= HW_TYPE_STD;
        this.creationTime= System.currentTimeMillis()/1000;
        this.isCompressed= (this.pubKey.length==33);
    }
    
    public static void setCardConnector(CardConnector cc){
        cardConnector= cc; 
    }
    
    public byte[] getKeypath(){
        byte[] keypath;
        if (this.hw_type==HW_TYPE_BIP32){
            keypath= new byte[this.bip32path.size()*4];
            for (int i=0; i<bip32path.size(); i++){
                long val= Integer.toUnsignedLong(bip32path.get(i));
                keypath[4*i]=  (byte)((val>>24) & 0xff);
                keypath[4*i+1]=  (byte)((val>>16) & 0xff);
                keypath[4*i+2]=  (byte)((val>>8) & 0xff);
                keypath[4*i+3]=  (byte)(val & 0xff);
            }
        }else{
            keypath= new byte[1];
            keypath[0]=this.keynbr;
        }
        return keypath;
    }
    
    public List<Integer> getBip32path(){
        if (this.hw_type==HW_TYPE_BIP32)
            return this.bip32path;
        else
            return null;
    }
    
    public boolean isBIP32(){
        if (this.hw_type==HW_TYPE_BIP32)
            return true;
        else
            return false;
    }
    
    private static byte[] recoverFromSignature(int recID, byte[] msg, byte[] sig, boolean doublehash) throws ECException{
        
        //return CardConnector.recoverPublicKeyFromSig(recID, msg, sig, doublehash);
        
        byte[] digest= new byte[32];
        SHA256Digest sha256= new SHA256Digest();
        sha256.reset();
        sha256.update(msg, 0, msg.length);
        sha256.doFinal(digest, 0);
        if (doublehash){
            sha256.reset();
            sha256.update(digest, 0, digest.length);
            sha256.doFinal(digest, 0);
        }
        BigInteger bi= new BigInteger(1,digest);
        ECDSASignature ecdsaSig= new ECDSASignature(sig);
        ECKey k= ECKey.recoverFromSignature(recID, ecdsaSig, bi, true);
        
        if (k!=null)
            return k.getPubKey();
        else
            return null;
        
    }
    
    
    /**
     * Signs a message using the private key
     *
     * @param       message             Message to be signed
     * @return                          Base64-encoded signature string
     * @throws      ECException         Unable to sign the message
     */
    @Override
    public String signMessage(String message) throws ECException {
        String encodedSignature = null;
        if ((hw_type==HW_TYPE_BIP32 && bip32path==null) || (hw_type==HW_TYPE_STD && keynbr==0xff) )
            throw new IllegalStateException("No private key available");
        if (cardConnector==null)
            throw new ECException("No cardConnector object found");
        
        String debug2 = null;
        try {
            // Format the message for signing => done inside the card!!
            byte[] contents;
            byte[] paddedcontents;
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(message.length()*2)) {
                byte[] headerBytes = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes("UTF-8");
                outStream.write(VarInt.encode(headerBytes.length));
                outStream.write(headerBytes);
                byte[] messageBytes = message.getBytes("UTF-8");
                outStream.write(VarInt.encode(messageBytes.length));
                outStream.write(messageBytes);
                paddedcontents = outStream.toByteArray();
                contents = message.getBytes("UTF-8");
            }
            
            // prepare the private key in dongle
            byte[] pubkey=null;
            if (this.hw_type==HW_TYPE_BIP32)
                pubkey= getBip32ExtendedKey(this.bip32path);
            else
                pubkey= getStandardKey(this.keynbr);
               
            // check public key consistency
            if (!Arrays.equals(pubkey, this.pubKey))
                throw new ECException("Public key mismatch");

            // Create the signature
            byte[] response;
            if (contents.length>140)
                response= cardConnector.cardSignMessage((this.hw_type==HW_TYPE_BIP32)?(byte)0xff:this.keynbr, contents);
            else
                response= cardConnector.cardSignShortMessage((this.hw_type==HW_TYPE_BIP32)?(byte)0xff:this.keynbr, contents);
            
            //debug2
            debug2="response:"+CardConnector.toString(response)+"\n";
            debug2+="contents:"+CardConnector.toString(contents)+"\n";
            debug2+="paddedcontents:"+CardConnector.toString(paddedcontents)+"\n";
            byte[] digest= new byte[32];
            SHA256Digest sha256= new SHA256Digest();
            sha256.reset();
            sha256.update(paddedcontents, 0, paddedcontents.length);
            sha256.doFinal(digest, 0);
            sha256.reset();
            sha256.update(digest, 0, digest.length);
            sha256.doFinal(digest, 0);
            debug2+="hash:"+CardConnector.toString(digest)+"\n";
            //endbug
            
            // recover the public key from the signature
            int recID = -1;
            for (int i=0; i<4; i++) { //debug:<4
                pubkey = recoverFromSignature(i, paddedcontents, response, DOUBLESHA256);
                if (pubkey != null && Arrays.equals(pubkey, this.pubKey)) {
                    recID = i;
                    break; //debug
                }
                if (pubkey != null)
                    debug2+="recID:"+Integer.toString(i)+" pubkey:"+CardConnector.toString(pubkey)+"\n";
            }
            if (recID == -1)
                throw new ECException("Unable to recover public key from signature"+debug2);
            
            // The message signature consists of a header byte followed by the R and S values
            byte[] sigData = CardDataParser.PubKeyData.parseToCompactSig(response, recID, this.isCompressed());
            encodedSignature = new String(Base64.encode(sigData), "UTF-8");
            debug2+="compress:"+this.isCompressed+"\n sig:"+CardConnector.toString(sigData)+ "\n encoded sig:"+encodedSignature+"\n";
            
            // verify signature in software...
            boolean verif=verifyMessage(this.toAddress().toString(), message, encodedSignature);
            if (!verif)
                throw new ECException("ECException during message signing: signature verification failed \n"+debug2);
            
        } catch (IOException exc) {
            throw new IllegalStateException("Unexpected IOException", exc);
        } catch (CardConnectorException ex) {
            Logger.getLogger(ECKeyHw.class.getName()).log(Level.SEVERE, null, ex);
            throw new ECException("ECException during message signing: ins:"+ex.getIns()
                    +" sw12:"+ex.getSW12()
                    +" p1:"+ex.getCommand().getP1()
                    +" p2:"+ex.getCommand().getP2()
                    +" c:"+CardConnector.toString(ex.getCommand().getData()));
        } catch (SignatureException ex) {
            Logger.getLogger(ECKeyHw.class.getName()).log(Level.SEVERE, null, ex);
            throw new ECException("ECException during message signing: ins:"+this.toAddress().toString()+debug2);
        } catch (CardDataParser.CardDataParserException ex) {
            Logger.getLogger(ECKeyHw.class.getName()).log(Level.SEVERE, null, ex);
        }
        return encodedSignature;
    }
    
    /**
     * Creates a signature for the supplied contents using the private key stored in the dongle
     *
     * @param       contents                Contents to be signed
     * @return                              ECDSA signature
     * @throws      ECException             Unable to create signature
     */
    @Override
    public ECDSASignature createSignature(byte[] contents) throws ECException {
        
        try {
            if ((hw_type==HW_TYPE_BIP32 && bip32path==null) || (hw_type==HW_TYPE_STD && keynbr==0xff) )
                throw new IllegalStateException("No private key available in dongle");
            if (cardConnector==null)
                throw new ECException("No cardConnector object found");
            
            // derive private key in dongle and recover public key
            byte[] pubkey=null;
            byte keyref=0;
            if (this.hw_type==HW_TYPE_BIP32){
                pubkey= getBip32ExtendedKey(this.bip32path);
                keyref=(byte)0xff;
            }else{
                pubkey= getStandardKey(this.keynbr);
                keyref= this.keynbr;
            }
            // check public key consistency
            if (!Arrays.equals(pubkey, this.pubKey))
                throw new ECException("Public key mismatch");
            Logger.getLogger(Transaction.class.getName()).log(Level.INFO, 
                    "getpubkey:"+CardConnector.toString(pubkey));
            
            // parse the transaction
            byte[] swHash = Utils.doubleDigest(contents);
            byte[] response = cardConnector.cardParseTransaction(contents);
            CardDataParser.PubKeyData parser= new CardDataParser.PubKeyData(authentikey);
            byte[] hwHash= parser.parseTxHash(response).data;//new byte[32];
            Logger.getLogger(Transaction.class.getName()).log(Level.INFO, "recovered authentikey:{0}", CardConnector.toString(parser.authentikey));
            if (!Arrays.equals(hwHash, swHash)){
                Logger.getLogger(Transaction.class.getName()).log(Level.SEVERE, 
                    "Error during transaction parsing: txhashes are not consistent!"
                    +"\n\t rawtx:"+CardConnector.toString(contents)
                    +"\n\t hwHash:"+CardConnector.toString(hwHash)
                    +"\n\t swHash:"+CardConnector.toString(swHash));
                throw new ECException("Error computing tx hash");
            }
            Logger.getLogger(Transaction.class.getName()).log(Level.INFO, 
                    "\n\t parsetx:"+CardConnector.toString(response)
                    +"\n\t hwHash:"+CardConnector.toString(hwHash));
            
            // Create the signature
            byte[] signature = cardConnector.cardSignTransaction(keyref, hwHash, null);
            Logger.getLogger(Transaction.class.getName()).log(Level.FINE, "signature:{0}", CardConnector.toString(signature));
            
            // recover pub key from signature
            //byte[] signature= new byte[(int)response[1]+2];
            //System.arraycopy(response, 0, signature, 0, signature.length);
            int recID = -1;
            for (int i=0; i<4; i++) {
                pubkey = recoverFromSignature(i, contents, signature, DOUBLESHA256);
                if (pubkey != null && Arrays.equals(pubkey, this.pubKey)) {
                    recID = i;
                    break;
                }
            }
            if (recID == -1)
                throw new ECException("Unable to recover public key from signature");
            Logger.getLogger(Transaction.class.getName()).log(Level.FINE, "createsign: getkey:"+CardConnector.toString(pubkey));
            
            // to do: canonical signature??
            return new ECDSASignature(signature);
           
        } catch (CardConnectorException ex) {
            Logger.getLogger(ECKeyHw.class.getName()).log(Level.SEVERE, 
                            "CardConnectorException:\n ins:"+ex.getIns()
                            +"\n sw12:"+ex.getSW12()
                            +"\n command:"+CardConnector.toString(ex.getCommand().getData())
                            +"\n response:"+CardConnector.toString(ex.getResponse().getData()));
            return null;
        } catch (org.toporin.bitcoincore.ECException ex) {
            Logger.getLogger(ECKeyHw.class.getName()).log(Level.SEVERE, "Uneble to verify hwhash signature", ex);
            return null;
        }
    }
    
    public static byte[] importBip32Seed(byte[] keyACL, byte[] seed) throws ECException, CardConnectorException {    
        byte[] response= cardConnector.cardBip32ImportSeed(keyACL, seed);
        return getBip32AuthentiKey();
    }
    
    public static byte[] importStdKey(byte keynbr, byte[] privKey) throws ECException, CardConnectorException {
        
        if (keynbr<0 || keynbr>=JCconstants.MAX_NUM_KEYS)
            throw new IllegalArgumentException("Wrong key number");
        
        // import private key in dongle
        if (privKey!=null){
            byte[] key_ACL= {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            byte[] keyblob= new byte[2+privKey.length];
            keyblob[0]= (byte)((privKey.length>>8) & 0xff);
            keyblob[1]= (byte)((privKey.length) & 0xff);
            System.arraycopy(privKey, 0, keyblob, 2, privKey.length);

            byte[] response= cardConnector.cardImportKey( keynbr, key_ACL, JCconstants.BLOB_ENC_PLAIN, 
                    JCconstants.TYPE_EC_FP_PRIVATE, JCconstants.LENGTH_EC_FP_256, keyblob);
        }
        
        return getStandardKey(keynbr);
        
    }

    public static byte[] getBip32AuthentiKey() throws ECException, CardConnectorException{
        
        byte[] response= cardConnector.cardBip32GetAuthentiKey();
        
        // extract msg & sig from response
        int coordx_size = ((int)(response[0] & 0xff)<<8) + ((int)(response[1] & 0xff));
        byte[] msg= new byte[2+coordx_size]; 
        System.arraycopy(response, 0, msg, 0, coordx_size+2);
        byte[] coordx= new byte[coordx_size]; 
        System.arraycopy(response, 2, coordx, 0, coordx_size);
        int sigsize = ((int)(response[2+coordx_size] & 0xff)<<8) + ((int)(response[2+coordx_size+1] & 0xff));  
        byte[] signature= new byte[sigsize]; 
        System.arraycopy(response, coordx_size+4, signature, 0, sigsize);
        //recover pubkey
        int recID =-1;
        for (int i=0; i<4; i++){
            byte[] pubkey= recoverFromSignature(i, msg, signature, SINGLESHA256);
            if (pubkey!=null){
                byte[] coordxkey= new byte[coordx_size];
                System.arraycopy(pubkey, 1, coordxkey, 0, coordx_size);
                if (Arrays.equals(coordx,coordxkey)){
                    recID=i;
                    authentikey= pubkey; // better to set it after?
                    break;
                }
            }
        }
        if (recID == -1)
            throw new ECException("Unable to recover public key from signature");        
        
        return authentikey;
    }    
    
    public static byte[] getBip32ExtendedKey(List<Integer> bip32path) throws ECException, CardConnectorException{
        
        byte[] pubkey=null;
        
        if (bip32path.size()>10)
            throw new IllegalArgumentException("BIP32 path is too long");
        
        // convert List<Integer> to byte[]
        byte[] bytepath= new byte[4*bip32path.size()];
        for (int i=0; i<bip32path.size(); i++){
            long val= Integer.toUnsignedLong(bip32path.get(i));
            bytepath[4*i]=  (byte)((val>>24) & 0xff);
            bytepath[4*i+1]=  (byte)((val>>16) & 0xff);
            bytepath[4*i+2]=  (byte)((val>>8) & 0xff);
            bytepath[4*i+3]=  (byte)(val & 0xff);
        }
        
        // send apdu command
        byte[] response= cardConnector.cardBip32GetExtendedKey(bytepath);
        
        // extract msg & sig from response
        int coordx_size = ((int)(response[0] & 0xff)<<8) + ((int)(response[1] & 0xff));
        byte[] msg= new byte[2+coordx_size]; 
        System.arraycopy(response, 0, msg, 0, coordx_size+2);
        byte[] coordx= new byte[coordx_size]; 
        System.arraycopy(response, 2, coordx, 0, coordx_size);
        int sig_size = ((int)(response[2+coordx_size] & 0xff)<<8) + ((int)(response[2+coordx_size+1] & 0xff));  
        byte[] signature= new byte[sig_size]; 
        System.arraycopy(response, coordx_size+4, signature, 0, sig_size);
        //debug2
        String debug="response:"+CardConnector.toString(response)+"\n";
        debug+="authkey:"+CardConnector.toString(authentikey)+"\n";
        debug+="msg:"+CardConnector.toString(msg)+"\n";
        debug+="sig:"+CardConnector.toString(signature)+"\n";
        byte[] digest= new byte[32];
        SHA256Digest sha256= new SHA256Digest();
        sha256.reset();
        sha256.update(msg, 0, msg.length);
        sha256.doFinal(digest, 0);
        debug+="hash:"+CardConnector.toString(digest)+"\n";
        //endbug
        //recover pubkey
        int recID=-1;
        for (int i=0; i<4; i++){
            pubkey= recoverFromSignature(i, msg, signature, SINGLESHA256);
            if (pubkey!=null){
                byte[] coordxkey= new byte[coordx_size];
                System.arraycopy(pubkey, 1, coordxkey, 0, coordx_size);
                if (Arrays.equals(coordx,coordxkey)){
                    recID=i;
                    break;
                }
                debug+="recID:"+Integer.toString(i)+" pubkey:"+CardConnector.toString(pubkey)+"\n";
            }
        }
        if (recID == -1)
            throw new ECException("Unable to recover public key from signature"+debug);
        
        // recover authentikey
        byte[] msg2= new byte[2+coordx_size+2+sig_size];
        System.arraycopy(response, 0, msg2, 0, 2+coordx_size+2+sig_size);
        int sig_size2 = ((int)(response[2+coordx_size+2+sig_size] & 0xff)<<8) + ((int)(response[2+coordx_size+2+sig_size+1] & 0xff));  
        byte[] signature2= new byte[sig_size2]; 
        System.arraycopy(response, 2+coordx_size+2+sig_size+2, signature2, 0, sig_size2);
        //debug2
        String debug2="response:"+CardConnector.toString(response)+"\n";
        debug2+="authkey:"+CardConnector.toString(authentikey)+"\n";
        debug2+="msg2:"+CardConnector.toString(msg2)+"\n";
        debug2+="sig2:"+CardConnector.toString(signature2)+"\n";
        digest= new byte[32];
        sha256= new SHA256Digest();
        sha256.reset();
        sha256.update(msg2, 0, msg2.length);
        sha256.doFinal(digest, 0);
        debug2+="hash:"+CardConnector.toString(digest)+"\n";
        //endbug
        recID=-1;
        for (int i=0; i<4; i++){
            byte[] authkey= recoverFromSignature(i, msg2, signature2, SINGLESHA256);
            if (authkey!=null && Arrays.equals(authkey, authentikey)){
                recID=i;
                break;
            }
            if (authkey!=null)
                debug2+="recID:"+Integer.toString(i)+" pubkey:"+CardConnector.toString(authkey)+"\n";
        }
        if (recID == -1)
            throw new ECException("Unable to recover authentikey from signature"+debug2);
        
        return pubkey;
    }
    
    public static byte[] getStandardKey(byte keynbr) throws ECException, CardConnectorException{
      
        if (keynbr<0 || keynbr>=JCconstants.MAX_NUM_KEYS)
            throw new IllegalArgumentException("Wrong key number");
        
        byte[] pubkey=null;
       
        // get public key from private key
        byte[] response= cardConnector.cardGetPublicKeyFromPrivate(keynbr);
        // extract msg & sig from response
        int coordx_size = ((int)(response[0] & 0xff)<<8) + ((int)(response[1] & 0xff));
        byte[] msg= new byte[2+coordx_size]; 
        System.arraycopy(response, 0, msg, 0, coordx_size+2);
        byte[] coordx= new byte[coordx_size]; 
        System.arraycopy(response, 2, coordx, 0, coordx_size);
        int sigsize = ((int)(response[2+coordx_size] & 0xff)<<8) + ((int)(response[2+coordx_size+1] & 0xff));  
        byte[] signature= new byte[sigsize]; 
        System.arraycopy(response, coordx_size+4, signature, 0, sigsize);
        //recover pubkey
        int recID =-1;
        for (int i=0; i<4; i++){
            pubkey= recoverFromSignature(i, msg, signature, SINGLESHA256);
            if (pubkey!=null){
                byte[] coordxkey= new byte[coordx_size];
                System.arraycopy(pubkey, 1, coordxkey, 0, coordx_size);
                if (Arrays.equals(coordx,coordxkey)){
                    recID=i;
                    break;
                }
            }
        }
        if (recID == -1)
            throw new ECException("Unable to recover public key from signature");
        
        return pubkey;
    }
    
//    /* convert a DER encoded signature to compact 65-byte format
//        input is hex string in DER format
//        output is hex string in compact 65-byteformat
//        http://bitcoin.stackexchange.com/questions/12554/why-the-signature-is-always-65-13232-bytes-long
//        https://bitcointalk.org/index.php?topic=215205.0            
//    */
//    private static byte[] toCompactSig(byte[] sigin, int recid, boolean compressed) {
//
//        byte[] sigout= new byte[65];
//        // parse input 
//        byte first= sigin[0];
//        if (first!= 0x30){
//            System.out.println("Wrong first byte!");
//            return new byte[0];
//        }
//        byte lt= sigin[1];
//        byte check= sigin[2];
//        if (check!= 0x02){
//            System.out.println("Check byte should be 0x02");
//            return new byte[0];
//        }
//        // extract r
//        byte lr= sigin[3];
//        for (int i= 0; i<=31; i++){
//            byte tmp= sigin[4+lr-1-i];
//            if (lr>=(i+1)) {
//                sigout[32-i]= tmp;
//            } else{ 
//                sigout[32-i]=0;  
//            }
//        }
//        // extract s
//        check= sigin[4+lr];
//        if (check!= 0x02){
//            System.out.println("Second check byte should be 0x02");
//            return new byte[0];
//        }
//        byte ls= sigin[5+lr];
//        if (lt != (lr+ls+4)){
//            System.out.println("Wrong lt value");
//            return new byte[0];
//        }
//        for (int i= 0; i<=31; i++){
//            byte tmp= sigin[5+lr+ls-i];
//            if (ls>=(i+1)) {
//                sigout[64-i]= tmp;
//            } else{ 
//                sigout[32-i]=0;  
//            }
//        }
//
//        // 1 byte header
//        if (recid>3 || recid<0){
//            System.out.println("Wrong recid value");
//            return new byte[0];
//        }
//        if (compressed){
//            sigout[0]= (byte)(27 + recid + 4 );
//        }else{
//            sigout[0]= (byte)(27 + recid);                
//        }
//
//        return sigout;
//    }
}
