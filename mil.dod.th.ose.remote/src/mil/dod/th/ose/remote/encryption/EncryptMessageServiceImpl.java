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
package mil.dod.th.ose.remote.encryption;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec; 
import javax.xml.bind.DatatypeConverter;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptInfo;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.api.EncryptionUtility;
import mil.dod.th.ose.shared.SystemConfigurationConstants;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.DSADigestSigner;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Implementation of EncryptMessage Service.
 * 
 * @author powarniu
 */
@Component // NOCHECKSTYLE: Class Fan-Out Complexity: Class works with a lot of other things.
public class EncryptMessageServiceImpl implements EncryptMessageService 
{
    /**
     *  The public keys are stored in this file.
    */
    private static final String PROP_FILE_NAME_PUBLIC = "th.encryption.public.properties";    
    
    /**
     *  The private keys are stored in this file.
    */
    private static final String PROP_FILE_NAME_PRIVATE = "th.encryption.private.properties";
    
    /**
     *  The private keys are keyed in using private.key.
    */
    private static final String PRIVATE_KEY = "private.key";
    
    /**
     *  The authorized encryption keys (public keys) are stored in this file.
    */
    private static final String PROP_FILE_NAME_AUTHORIZED = "th.encryption.authorizedKeys.properties";    
    
    /**
     *  The authorized encryption keys are keyed in with system.id.key.
    */
    private static final String SYSTEM_ID_KEY = "system.id.key";
 
    /**
     * Constant value used for encryption algorithm enabling the padding and GCM.
     */
    private static final String FULL_ENCRYPT_ALGORITHM = "AES/GCM/NoPadding"; 
    
    /**
     * Constant value used for AES encryption algorithm for just initializing AES.
     */
    private static final String ENCRYPT_ALGORITHM_AES = "AES"; 
    
    /**
     * Constant value used for Bouncy Castle Provider.
     */
    private static final String BOUNCY_CASTLE_PROVIDER = "BC"; 

    /**
     * Utility used to log messages.
     */
    private LoggingService m_Logging;

    /**
     * File to hold public static keys.     
     */
    private File m_PropFilePublic;
    
    /**
     * Properties map for the Public static local keys.
     */
    private Properties m_PropPublic;
    
    /**
     * File to hold private static keys.     
     */
    private File m_PropFilePrivate;
    
    /**
     * Properties map for the Private static local keys.
     */
    private Properties m_PropPrivate;

     /**
     * File to hold Authorized public keys.     
     */
    private File m_PropFileAuthorized;
    
    /**
     * Properties map for the authorized public keys.
     */
    private Properties m_PropAuthorized;
    
    /**
     * Hexadecimal radix.
     */
    private static final int hexaDecimalRadix = 16;//NOCHECKSTYLE 16 is used as radix
    
    /**
     * Initialization vector length.
     */
    private static final int m_IvLength = 12;//NOCHECKSTYLE 12 is used as initialization vector length
   
    /**
     * Cipher input for encrypting data.
     */
    private Cipher m_Input;
    
    /**
     * Cipher output for decrypting data.
     */
    private Cipher m_Output;
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Activate the encryption component.
     * <p>
     * We have three files for each system. one file is for saving all static public key,
     * second file is used for saving all static private key and third file is used for saving
     * all the public remote keys for all systems. 
     * 
     * @param context
     *      this bundle's context
     * @throws IOException 
     *         Component is not activated if there is an IOException
     * @throws NoSuchPaddingException 
     *         Incorrect Padding argument, should be NoPadding.
     * @throws NoSuchProviderException 
     *         Incorrect name of the provider, should be BC.
     * @throws NoSuchAlgorithmException 
     *         Incorrect name of the algorithm, should be AES.
     */
    @Activate
    public void activate(final BundleContext context)
            throws IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
    {
        // Bouncy castle provider is added during execution
        Security.addProvider(new BouncyCastleProvider());
        m_Input = Cipher.getInstance(FULL_ENCRYPT_ALGORITHM, BOUNCY_CASTLE_PROVIDER);
        m_Output = Cipher.getInstance(FULL_ENCRYPT_ALGORITHM, BOUNCY_CASTLE_PROVIDER);
        final String dataDir = context.getProperty(SystemConfigurationConstants.DATA_DIR_PROPERTY);
        final String dir = dataDir + File.separator + "encrypt-conf";
        
        //open up the files
        m_PropFileAuthorized = new File(dir, PROP_FILE_NAME_AUTHORIZED);
        m_PropAuthorized = new Properties();
        m_PropFilePublic = new File(dir, PROP_FILE_NAME_PUBLIC);
        m_PropPublic = new Properties();
        m_PropFilePrivate = new File(dir, PROP_FILE_NAME_PRIVATE);
        m_PropPrivate = new Properties();
        
        try (FileInputStream inputStreamAuthorized = new FileInputStream(m_PropFileAuthorized))
        {
            m_PropAuthorized.load(inputStreamAuthorized);
        }
        
        try (FileInputStream inputStreamPublic = new FileInputStream(m_PropFilePublic))
        {
            m_PropPublic.load(inputStreamPublic);
        }
        
        try (FileInputStream inputStreamPrivate = new FileInputStream(m_PropFilePrivate))
        {
            m_PropPrivate.load(inputStreamPrivate);
        }
    }

    /**
     * Encrypt the payload information from the message.
     * 
     * @return bytes
     *       Encrypted payload
     * @param message
     *      TerraHarvestMessage before encryption
     * @param payload
     *      TerraHarvestpayload before encryption     
     */
    @Override
    public TerraHarvestMessage encryptMessage(final TerraHarvestMessage.Builder message,
                                              final TerraHarvestPayload payload)
         
    {
        if (message.getEncryptType().equals(EncryptType.NONE))
        {
            return message.setTerraHarvestPayload(payload.toByteString()).build();
        }

        final int remoteSystemId = message.getDestId();
        final int localSystemId = message.getSourceId();
        
        // One Pass Diffie-Hellman key exchange per "Suite B Implementers' Guide to NIST SP 800-56A" section 3.2
        // for store and forward applications. Basically, we generate an ephemeral (temporary) key pair for each
        // message and then use ECDH with the ephemeral private key and the recipient's static (preloaded) public
        // key to generate an AES key that is used to encrypt the message. The ephemeral public key is sent along
        // with the message, and is signed with the sender's static private key to allow the recipient to authenticate
        // the sender. The receiver uses ECDH with its static private key and the ephemeral public key received with the
        // message to generate the AES key used to decrypt the message as previously described. This is basically the
        // technique used for S/MIME email.
        //
        //create a ephemeral key pair for the source
        final AsymmetricCipherKeyPair ephemeralKeyPair = EncryptionUtility.createKeyPair();
        final byte[] ephemeralPublicKey = 
                ((ECPublicKeyParameters)ephemeralKeyPair.getPublic()).getQ().getEncoded(false);
        
        //Now sign the ephemeral public key with source private static key.
        // To follow the guidance exactly, we should use a separate private key for the signing operation and the
        // encryption operation (see the Suite B Implementer's Guide section 8.2 as well as NIST SP 800-57 section 5.2.)
        // We don't do this right now for simplicity. We may want to change this later 
        //(which would mean we'd need to load two sets of keys for each System.)
        
        final DSADigestSigner dsaSigner = new DSADigestSigner(new ECDSASigner(), new SHA256Digest()); 
        dsaSigner.init(true, getPrivateStaticKey());
        
        //If we realize it here, we don't use the whole message, instead just use the Ephemeral public key from source
        //and sign it using Astatic priv key. This is done because whole message signature would take a long time,
        //just signing the ephemeral public key with Source static private key will save time. 
        
        dsaSigner.update(ephemeralPublicKey, 0, ephemeralPublicKey.length);
        final byte[] ephemeralSignature = dsaSigner.generateSignature();
        
        //generate the shared KDF key
        final byte[] sharedKeyBytesEncrypt = EncryptionUtility.sharedEcdhKeyCreate(
                    (ECPrivateKeyParameters)ephemeralKeyPair.getPrivate(), getRemotePublicKey(remoteSystemId), 
                    localSystemId, remoteSystemId);
        final Key key = new SecretKeySpec(sharedKeyBytesEncrypt, ENCRYPT_ALGORITHM_AES);
        //create the initialization vector for length =12
        //final SecureRandom rngBC1 = new SecureRandom();
        final byte[] ivBytes = new byte[m_IvLength];
        EncryptionUtility.RANDOM_BC.nextBytes(ivBytes);
        final IvParameterSpec ivBytes_New = new IvParameterSpec(ivBytes);
        
        final byte[] encrypt;
        try
        {
            m_Input.init(Cipher.ENCRYPT_MODE, key, ivBytes_New);
            encrypt = m_Input.doFinal(payload.toByteArray());
        } 
        catch (final InvalidKeyException e) 
        {
            throw new IllegalArgumentException(" Key returned by KDF does not meet specification", e);
        } 
        catch (final InvalidAlgorithmParameterException e)
        {
            throw new IllegalArgumentException(" Inappropriate Algorithm parameters", e);
        }
        catch (final IllegalBlockSizeException e)
        {
            throw new IllegalArgumentException(" Data and block size are not aligned", e);
        }
        catch (final BadPaddingException e)
        {
            throw new IllegalArgumentException(" Data is not padded appropriately for the padding mechanism", e);
        }

        //build EncryptInfo
        final EncryptInfo encryptInfo = EncryptInfo.newBuilder().
                setEphemeralKey(ByteString.copyFrom(ephemeralPublicKey)).
                setEphemeralKeySignature(ByteString.copyFrom(ephemeralSignature)).
                setInitializationVector(ByteString.copyFrom(ivBytes)).
                build();
  
        //build the message containing encrypted payload information
        return message.setTerraHarvestPayload(ByteString.copyFrom(encrypt)).setEncryptInfo(encryptInfo).build();
        
    }

     /**
     * Decrypt the payload inside the TerraHarvestMessage.
     * 
     * @return
     *     returns a decrypted (original) payload
     * @param message
     *        TerraHarvestMessage contains the encrypted payload bytes 
     * @throws InvalidProtocolBufferException
     *        thrown when a protocol message being parsed is invalid
     * @throws InvalidKeySignatureException 
     *        thrown when key signatures can not be verified during decryption
     */
    @Override
    public TerraHarvestPayload decryptRemoteMessage(final TerraHarvestMessage message) 
            throws InvalidProtocolBufferException, InvalidKeySignatureException  
    {
        if (message.getEncryptType().equals(EncryptType.NONE))
        {
            return TerraHarvestPayload.parseFrom(message.getTerraHarvestPayload());
        }
        final byte[] payload = message.getTerraHarvestPayload().toByteArray();
        final EncryptInfo encryptInfo = message.getEncryptInfo();
        final int localSystemId = message.getDestId();
        final int remoteSystemId = message.getSourceId();
        //parse the initialization vector from The TerraHarvestMessage
        final IvParameterSpec ivBytes_New = new IvParameterSpec(encryptInfo.getInitializationVector().toByteArray());
        
        //we will verify the signature using sender public key and the ephemeral key received with
        //the message and the ephemeral signature itself
        final DSADigestSigner dsaSigner1 = new DSADigestSigner(new ECDSASigner(), new SHA256Digest()); 
        dsaSigner1.init(false, getRemotePublicKey(remoteSystemId));
        final byte[] ephemeralKeyTemp = encryptInfo.getEphemeralKey().toByteArray();
        dsaSigner1.update(ephemeralKeyTemp, 0, ephemeralKeyTemp.length);
        final boolean sigOK = dsaSigner1.verifySignature(encryptInfo.getEphemeralKeySignature().toByteArray());
        
        if (sigOK)
        {
            m_Logging.info("Message key signature verification passed");
        }
        else
        {
            throw new InvalidKeySignatureException("Message key signature verification failed");
        }
        //generate the shared key 
        //first get the ephemeral public key from the message and source private key
        final ECPoint qPoint = EncryptionUtility.CURVEPARAMS.getCurve().
                                           decodePoint(ephemeralKeyTemp);
        final ECPublicKeyParameters ephemeralPublicKey = new ECPublicKeyParameters(
                                                            qPoint, EncryptionUtility.CURVEPARAMS);
        //local and remote system id gets flipped for decrypt.   
        final byte[] sharedKeyBytesDecrypt = EncryptionUtility.sharedEcdhKeyCreate(getPrivateStaticKey(),
            ephemeralPublicKey, remoteSystemId, localSystemId);
       

        final Key key = new SecretKeySpec(sharedKeyBytesDecrypt, ENCRYPT_ALGORITHM_AES);
        
        final byte[] decrypt;
        //Create a CIpher object to perform the decryption
        try
        {
            m_Output.init(Cipher.DECRYPT_MODE, key, ivBytes_New);
            decrypt = m_Output.doFinal(payload);
        }
        catch (final InvalidKeyException e) 
        {
            throw new IllegalArgumentException("Key returned by KDF does not meet specification", e);
        } 
        catch (final InvalidAlgorithmParameterException e)
        {
            throw new IllegalArgumentException("Inappropriate Algorithm parameters", e);
        }
        catch (final IllegalBlockSizeException e)
        {
            throw new IllegalArgumentException("Data and block size are not aligned", e);
        }
        catch (final BadPaddingException e)
        {
            throw new IllegalArgumentException("Data is not padded appropriately for the padding mechanism", e);
        }

        return TerraHarvestPayload.parseFrom(decrypt);
    }
    
    /**
     * Get the remote public key from the "th.encryption.authorized.properties" file.
     * @param remoteSystemId
     *        Remote system Id for a specific system       
     * @return
     *        remote public key for the system 
     */
    public ECPublicKeyParameters getRemotePublicKey(final int remoteSystemId) 
    {
        final String property = m_PropAuthorized.getProperty(SYSTEM_ID_KEY +  ((Integer)remoteSystemId).toString());
        if (property == null || property.equals(""))
        {
            throw new IllegalArgumentException("Unable to get 'remote public key' not set or unavailable");
        }
        //Convert string to byte array
        final byte[] remotePublicKeybytes = DatatypeConverter.parseHexBinary(property);
        final ECPublicKeyParameters remotePublicKey = new ECPublicKeyParameters(
            EncryptionUtility.CURVEPARAMS.getCurve().decodePoint(remotePublicKeybytes),
            EncryptionUtility.CURVEPARAMS);
        return remotePublicKey;
    }
    
    /**
     * Get the private static key from the "th.encryption.private.properties" file. 
     * @return
     *        private static key value string 
     */    
    public ECPrivateKeyParameters getPrivateStaticKey() 
    {
        final String property = m_PropPrivate.getProperty(PRIVATE_KEY);
        if (property == null || property.equals(""))
        {
            throw new IllegalArgumentException("Unable to get 'private key' it is either not set or unavailable");
        }
        final BigInteger privateStaticKeyBytes = new BigInteger(property, hexaDecimalRadix);
        final ECPrivateKeyParameters privateStaticKey = new ECPrivateKeyParameters(privateStaticKeyBytes,
                                                               EncryptionUtility.CURVEPARAMS);
        return privateStaticKey;
    }

    /**
     * Save the private static key in the "th.encryption.private.properties" file.
     * Save the public static key in the "th.encryption.public.properties" file 
     * @param staticPublicKeyString
     *         This is the public static key encoded in hexadecimal string
     * @param staticPrivateKeyString
     *         This is the private static key string encoded in hexadecimal string
     *        
     */    
    public void saveStaticKeyPair(final String staticPublicKeyString, final String staticPrivateKeyString)
    {

        //Save the string in hexadecimal format in "th.encryption.public.properties"
        m_PropPublic.setProperty("public.key", staticPublicKeyString);
        try (FileOutputStream outputStream = new FileOutputStream(m_PropFilePublic))
        {
            m_PropPublic.store(outputStream, "Encryption Properties , last property edited was 'publicStaticKey'");
        }
        catch (final IOException ex)
        {
            m_Logging.log(LogService.LOG_ERROR, "Unable to write public key to file: " + ex.getMessage());
        }
        

        //Save the string in hexadecimal format in "th.encryption.private.properties"
        m_PropPrivate.setProperty(PRIVATE_KEY, staticPrivateKeyString);
        try (FileOutputStream outputStream = new FileOutputStream(m_PropFilePrivate))
        {
            m_PropPrivate.store(outputStream, "Encryption Properties , last property edited was 'privateStaticKey'");
        }
        catch (final IOException ex)
        {
            m_Logging.log(LogService.LOG_ERROR, "Unable to write private key to file: " + ex.getMessage());
        }
    }
    
    /**
     * Save the local public static key in the "th.encryption.authorized.properties" file 
     * for the specific system. This is only used for testing purposes.
     * @param systemId
     *         SystemId for a specific system
     * @param authorizedPublicKey
     *         This is the public static key encoded in hexadecimal string
     *        
     */    
    public void saveAuthorizedKey(final int systemId, final String authorizedPublicKey)
    {

        m_PropAuthorized.setProperty(SYSTEM_ID_KEY + ((Integer)systemId).toString(), authorizedPublicKey);
        try (FileOutputStream outputStream = new FileOutputStream(m_PropFileAuthorized))
        {
            m_PropAuthorized.store(outputStream, "Encryption Properties , last edited ((Integer)systemId).toString() ");
        }
        catch (final IOException ex)
        {
            m_Logging.log(LogService.LOG_ERROR, "Unable to write authorized public key to file: " + ex.getMessage());
        }
    }
}

