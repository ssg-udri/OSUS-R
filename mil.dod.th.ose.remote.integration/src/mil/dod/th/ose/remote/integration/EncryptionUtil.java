//==============================================================================
// This software is part of the Open Standard for Unattended Sensors (OSUS)
// reference implementation (OSUS-R).
//
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.
//
// You should have received a copy of the CC0 Public Domain Dedication along
// with this software. If not, see
// <http://creativecommons.org/publicdomain/zero/1.0/>.
//==============================================================================
package mil.dod.th.ose.remote.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.DSADigestSigner;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptInfo;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.ose.remote.api.EncryptionUtility;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;
import mil.dod.th.ose.shared.SharedMessageUtils;

/**
 * Utility used to encrypt and decrypt messages for the remote interface integration tests.
 * 
 * @author cweisenborn
 */
public class EncryptionUtil
{    
    /**
     * Encrypt a message with ECDH.
     */
    public static TerraHarvestMessage encryptMessageWithECDH(TerraHarvestMessage thMessage) 
        throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, 
        NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
    {
        //Retrieve the cipher to be used for encrypting messages.
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        
        //need to set the encryption information so put the message to a builder
        TerraHarvestMessage.Builder builder = thMessage.toBuilder();
        builder.setEncryptType(EncryptType.AES_ECDH_ECDSA);
        
        //destination and source ids
        int remoteSystemId = TerraHarvestMessageHelper.getControllerId();
        int localSystemId = TerraHarvestMessageHelper.getSourceId();
        
        //asymmetrical key generator
        AsymmetricCipherKeyPair ephemeralKeyPair = EncryptionUtility.createKeyPair();
        byte[] ephemeralPublicKey = ((ECPublicKeyParameters)ephemeralKeyPair.getPublic()).getQ().getEncoded(false);
        
        //digital signer will 'digest' or take parts of the ephemeral key and this systems
        //private key and create a signature that will be used when the message is received to verify the sender
        DSADigestSigner dsaSigner = new DSADigestSigner(new ECDSASigner(), new SHA256Digest()); 
        dsaSigner.init(true, getPrivateStaticKey());
        dsaSigner.update(ephemeralPublicKey, 0, ephemeralPublicKey.length);
        byte[] ephemeralSignature = dsaSigner.generateSignature();
        
        //create the shared key
        byte [] sharedKeyBytesEncrypt = EncryptionUtility.sharedEcdhKeyCreate(
                    (ECPrivateKeyParameters)ephemeralKeyPair.getPrivate(), getRemotePublicKey(remoteSystemId), 
                    localSystemId, remoteSystemId);
        Key key = new SecretKeySpec(sharedKeyBytesEncrypt, "AES");

        //initialization vector creation, these are random bytes, 
        byte[] ivBytes = new byte[12];
        EncryptionUtility.RANDOM_BC.nextBytes(ivBytes);
        IvParameterSpec ivBytes_New = new IvParameterSpec(ivBytes);
        
        //initialize the cipher, and encrypt the message
        cipher.init(Cipher.ENCRYPT_MODE, key, ivBytes_New);
        byte[] encrypt = cipher.doFinal(thMessage.getTerraHarvestPayload().toByteArray());
        
        //build EncryptInfo
        EncryptInfo encryptInfo = EncryptInfo.newBuilder().
                setEphemeralKey(ByteString.copyFrom(ephemeralPublicKey)).
                setEphemeralKeySignature(ByteString.copyFrom(ephemeralSignature)).
                setInitializationVector(ByteString.copyFrom(ivBytes)).
                build();
  
        //build the message containing encrypted payload information
        TerraHarvestMessage encryptedMessage = builder.setTerraHarvestPayload(ByteString.copyFrom(encrypt)).
                setEncryptInfo(encryptInfo).build();
        
        return encryptedMessage;
    }
    
    /**
     * Decrypt a message encrypted with ECDH.
     */
    public static TerraHarvestPayload decryptECDHMessage(TerraHarvestMessage message) throws NoSuchAlgorithmException, 
        NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, 
        InvalidKeyException, InvalidAlgorithmParameterException, InvalidProtocolBufferException
    {
        //Retrieve the cipher to be used for decrypting messages.
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        
        //get the encrypted payload
        byte[] payload = message.getTerraHarvestPayload().toByteArray();
        EncryptInfo responseEncryptInfo = message.getEncryptInfo();
        
        //system ids
        int responseLocalSystemId = message.getDestId();
        int responseRemoteSystemId = message.getSourceId();
        
        //parse the initialization vector from The TerraHarvestMessage
        IvParameterSpec responseIvBytes_New = 
                new IvParameterSpec(responseEncryptInfo.getInitializationVector().toByteArray());
        
        //we will verify the signature using sender public key and the ephemeral key received with
        //the message and the ephemeral signature itself
        DSADigestSigner dsaSigner1 = new DSADigestSigner(new ECDSASigner(), new SHA256Digest()); 
        dsaSigner1.init(false, getRemotePublicKey(responseRemoteSystemId));
        byte [] ephemeralKeyTemp = responseEncryptInfo.getEphemeralKey().toByteArray();
        dsaSigner1.update(ephemeralKeyTemp, 0, ephemeralKeyTemp.length);
        
        //process signature
        boolean sigOK = dsaSigner1.verifySignature(responseEncryptInfo.getEphemeralKeySignature().toByteArray());
        assertThat(sigOK, is(true));
        
        //first get the ephemeral public key from the message and source private key
        ECPoint qPoint = EncryptionUtility.CURVEPARAMS.getCurve().decodePoint(ephemeralKeyTemp);
        ECPublicKeyParameters ephemeralPublicKeyParam = new ECPublicKeyParameters(qPoint, 
                EncryptionUtility.CURVEPARAMS);
        
        //create a shared key 
        byte [] sharedKeyBytesDecrypt = EncryptionUtility.sharedEcdhKeyCreate(getPrivateStaticKey(),
                ephemeralPublicKeyParam, responseRemoteSystemId, responseLocalSystemId);
       
        //key
        Key responsekey = new SecretKeySpec(sharedKeyBytesDecrypt, "AES");
        
        //Create a CIpher object to perform the decryption
        cipher.init(Cipher.DECRYPT_MODE, responsekey, responseIvBytes_New);
        byte[] decrypt = cipher.doFinal(payload);

        //parse the decrypted bytes
        TerraHarvestPayload payloadDecrypt = TerraHarvestPayload.parseFrom(decrypt);
        
        return payloadDecrypt;
    }
    
    /**
     * Get the private key for this mocked system.
     * @return
     *     the private static key for this system
     */
    private static ECPrivateKeyParameters getPrivateStaticKey()
    {
        BigInteger privateStaticKeyBytes = 
            new BigInteger("ba54a96c13034cff23c258d574d84e6db6f0d7b7b611ce29848900c63607c9d5", 16);
        return new ECPrivateKeyParameters(privateStaticKeyBytes, EncryptionUtility.CURVEPARAMS);
    }
    
    /**
     * Get the remote public key.
     * @param remoteSystemId
     *        Remote system Id for a specific system       
     * @return
     *        remote public key for the system 
     */
    private static ECPublicKeyParameters getRemotePublicKey(final int remoteSystemId) throws IllegalArgumentException 
    {
        byte[] remotePublicKeybytes;
        switch(remoteSystemId)
        {
            case 1:
              //Convert string to byte array
                remotePublicKeybytes = 
                        DatatypeConverter.parseHexBinary("04050422b181c748aa3fb24fc7a821d230401eaef75c50dc8a581e5e64ec"
                            + "0aba82acabb2274f73a1d4edcba3de3b60f3392bd3e2e5cd02b9cad26adddd03afad73");
                break;
            case 0:
                //Convert string to byte array
                remotePublicKeybytes = 
                    DatatypeConverter.parseHexBinary("047428d301984d3a37751e5db315f5ce1f6c3b0fd9ddaf89b4a24a19d284"
                            + "cee827bd02081f5d4c060f8b14dbf02775798bbf09787257e539d3b140bb745ff7dda0");
                break;
            default:
                throw new IllegalArgumentException(String.format("The system id %d is not known.", remoteSystemId));
        }
        return new ECPublicKeyParameters(
                EncryptionUtility.CURVEPARAMS.getCurve().decodePoint(remotePublicKeybytes),
                EncryptionUtility.CURVEPARAMS);  
    }
    
    /**
     * Method that creates a {@link TerraHarvestMessage} that can be sent to change the encryption level of the 
     * controller.
     * 
     * @param encryptionMode
     *      encryption mode to set the controller to.
     * @return
     *      {@link TerraHarvestMessage} containing the needed information to change the encryption level of the 
     *      controller.
     */
    public static TerraHarvestMessage createSetEncryptionLevelMessage(EncryptionMode encryptionMode)
    {
        Multitype multitypeValue = SharedMessageUtils.convertObjectToMultitype(encryptionMode.toString());
        
        SimpleTypesMapEntry prop = SimpleTypesMapEntry.newBuilder()
                .setKey(RemoteSettings.KEY_ENCRYPTION_MODE)
                .setValue(multitypeValue).build();
        
        SetPropertyRequestData setPropRequest = SetPropertyRequestData.newBuilder()
                .setPid(RemoteSettings.PID)
                .addProperties(prop).build();
        
        ConfigAdminNamespace.Builder configNamespaceBuilder = ConfigAdminNamespace.newBuilder()
                .setType(ConfigAdminMessageType.SetPropertyRequest)
                .setData(setPropRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(
                Namespace.ConfigAdmin, configNamespaceBuilder);
        return message;
    }
    
    /**
     * Build a terra harvest message with the specified encryption info message.
     * 
     * @param type
     *      {@link EncryptionInfoMessageType}
     * @param message
     *      EncryptionInfo message to be added to the terra harvest message.
     * @return
     *      Return the complete terra harvest message containing the encryption info message.
     */
    public static TerraHarvestMessage createEncryptionInfoMessage(final EncryptionInfoMessageType type, 
            final Message message)
    {       
        EncryptionInfoNamespace.Builder namespaceMsg = EncryptionInfoNamespace.newBuilder().setType(type);
        
        if (message != null)
        {
            namespaceMsg.setData(message.toByteString());
        }
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.EncryptionInfo, namespaceMsg);
        
        return thMessage;
    }
}
