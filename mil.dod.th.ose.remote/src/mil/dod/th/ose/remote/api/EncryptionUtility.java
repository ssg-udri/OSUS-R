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
package mil.dod.th.ose.remote.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.util.BigIntegers;

/**
 * Encryption Utility class provides methods to generate the shared key using ECDH and 
 * also implements key derivation function (KDF) as documented in section 5.8.1 of NIST's 
 * publication: Recommendation for Pair-Wise Key Establishment Schemes Using Discrete 
 * Logarithm Cryptography.
 * @author powarniu
 *
 */
final public class EncryptionUtility
{
    /**
     * ECDH/ECDSA using P256 curve parameters is initialized.
    */
    public static final ASN1ObjectIdentifier CURVEID = org.bouncycastle.asn1.sec.SECObjectIdentifiers.secp256r1;
    
    /**
     * ASN.1 definition for Elliptic-Curve Parameters structure.
    */
    public static final org.bouncycastle.asn1.x9.X9ECParameters ECP = 
        org.bouncycastle.asn1.sec.SECNamedCurves.getByOID(CURVEID);
    
    /**
     * ECDomain parameters are mostly derived from ECConstants.
    */
    public static final ECDomainParameters CURVEPARAMS = new ECDomainParameters(ECP.getCurve(), ECP.getG(), 
             ECP.getN(), ECP.getH(), ECP.getSeed());

    /**
     * Random number generator instance.
     */
    public static final SecureRandom RANDOM_BC = new SecureRandom();
    
    /**
     * Utility class, no need to instantiate it.
     */
    private EncryptionUtility()
    {
        
    }

    /**
     * Creating cipher key pair (public and private keys). Keys can be used for both static and ephemeral keys.
     * 
     * @return 
     *      The Asymmetric key pair contains a private and public key used for encryption
     */
    public static AsymmetricCipherKeyPair createKeyPair()
    {
        final ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(CURVEPARAMS, RANDOM_BC);
        final ECKeyPairGenerator ecKG = new ECKeyPairGenerator();
        ecKG.init(keygenParams);
        return ecKG.generateKeyPair();
    }
    
    /**
     * NIST Concatenation Key Derivation function (KDF) per "Suite B Implementers' Guide to NIST SP 800-56A" section 5.
     * An approved Key Derivation function (KDF) shall be used to derive secret keying material from shared secret key.
     *   
     * @param hashAlg
     *      Type of hash Algorithm used. The following java.security.messagedigest API discusses several hash algorithms
     *      http://docs.oracle.com/javase/7/docs/api/java/security/MessageDigest.html
     * @param sharedSecret
     *      A shared secret key is a key that has been computed using a key agreement scheme, 
     *      in our case we use ECDH key agreement.
     * @param keyDataLen
     *      An integer that indicates the length (in bits) of the secret keying material to be generated;
     * @param algorithmID
     *      A bit string indicating how the derived keying material will be parsed
     *      and for which algorithms the derived secret keying material will be used. For e.g. AlgorithmID might 
     *      indicate that bits 1-80 are to be used as an 80-bit HMAC key and that bits 81-208 are to be used as
     *      a 128-bit AES key, This is set to (int)1 and converted to 32 bit integer value as an array of bytes.
     * @param partyUInfo
     *      A bit string containing public information that is required by the application
     *      using this KDF to be contributed by party U to the key derivation process. At minimum, 
     *      partyUInfo shall include srcAddress. In our case, we end up setting the partyUInfo with
     *      sourceId instead and convert it to a 32 bit integer value as an array of bytes.
     * @param partyVInfo
     *      A bit string containing public information that is required by the application
     *      using this KDF to be contributed by party V to the key derivation process. At minimum, 
     *      partyVInfo shall include dstAddress. In our case, we end up setting the partyVInfo with 
     *      destId instead and convert it to a 32 bit integer value as an array of bytes.
     * @return
     *      Shared Secret key bytes array or also termed as DerivedKeyingMaterial generated after 
     *      Key Derivation function is a bit string of length keyDataLen
     */
    private static byte[] concatKDF(final String hashAlg, final byte[] sharedSecret, final int keyDataLen, 
          final byte[] algorithmID, final byte[] partyUInfo, final byte[] partyVInfo) 
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try 
        {
            baos.write(algorithmID);
            baos.write(partyUInfo);
            baos.write(partyVInfo);
        }
        catch (final IOException e)
        {
            throw new IllegalStateException(e);
        }
        //Each of the three fields: algorithmId, PartyUInfo and PartyVInfo shall be concatenated
        //in a fixed length sequence of substrings termed as otherInfo
        final byte[] otherInfo = baos.toByteArray();
        final byte[] key = new byte[keyDataLen];
        final MessageDigest messageDigest;
        try 
        {
            messageDigest = MessageDigest.getInstance(hashAlg);
        } 
        catch (final NoSuchAlgorithmException e) 
        {
            throw new IllegalStateException("No such hashing algorithm");
        } 
        final int hashLen = messageDigest.getDigestLength(); 
        int reps = keyDataLen / hashLen;
        if (keyDataLen % hashLen != 0)
        {
            reps = reps + 1;
        }
        for (int i = 1; i <= reps; i++)
        {
            messageDigest.update(ByteBuffer.allocate(4).putInt(i).array());//new buffer capacity is 4 bytes NOCHECKSTYLE
            messageDigest.update(sharedSecret);
            messageDigest.update(otherInfo);
            final byte[] hash = messageDigest.digest();
            if (i < reps)
            {
                System.arraycopy(hash, 0, key, hashLen * (i - 1), hashLen);
            }
            else
            {
                if (keyDataLen % hashLen == 0)
                {
                    System.arraycopy(hash, 0, key, hashLen * (i - 1), hashLen);
                }
                else
                {
                    System.arraycopy(hash, 0, key, hashLen * (i - 1), keyDataLen % hashLen);
                }
            }
        }
        return key;
    }
 

    /**
     * utility function for creating shared secret kdf key based on private and public key 
     * from local and remote systems.
     * @param localPrivateKey
     *       local private key
     * @param remotePublicKey
     *      Remote public key
     * @param localSystemId
     *      local system Id
     * @param remoteSystemId
     *     remote system Id
     * @return byte[]
     *      The shared secret key formed after kdf.
     */
    public static byte[] sharedEcdhKeyCreate(final ECPrivateKeyParameters localPrivateKey, 
                      final ECPublicKeyParameters remotePublicKey, final int localSystemId, final int remoteSystemId)
    {
        //set up basic key agreement
        final ECDHBasicAgreement basicAgreement = new ECDHBasicAgreement();
        basicAgreement.init(localPrivateKey);
        final BigInteger secret = basicAgreement.calculateAgreement(remotePublicKey);
        final byte[] sharedSecret = BigIntegers.asUnsignedByteArray(secret);
        final String hashAlg = "SHA256";
        //Normally this should have been (256/8 = 32bytes),however, as per Nist
        //this is truncated to 16 bytes.
        final int keyDataLen = 16;
        //buffer capacity in bytes(4)
        final int bufferCapacity = 4;
        //few things like algorithm id = 1 are hard coded.
        final byte[] algorithmID = ByteBuffer.allocate(bufferCapacity).putInt(1).array();
        final byte[] partyUInfo = ByteBuffer.allocate(bufferCapacity).putInt(localSystemId).array();
        final byte[] partyVInfo = ByteBuffer.allocate(bufferCapacity).putInt(remoteSystemId).array();
        return concatKDF(hashAlg, sharedSecret, keyDataLen, algorithmID, partyUInfo, partyVInfo);
    }    
}
