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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigInteger;
import java.security.Security;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

/**
 * @author powarniu
 *
 */
public class TestEncryptionUtility
{
     
    @Before
    public final void setup()
    {
        //just added the bouncy castle provider so that it can run the message digest hashing algo.
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /** 
     * Validates creating shared key Basically we create two systems system A and system B. We create 
     * AsymmetricKey pair for both the systems Then we use private key for A and public key for B and 
     * create the shared secret key which could be used during encryption.
     * Latter we use private key for B and public key for A and create the shared secret key which could be used
     * during decryption.
     * Both the shared secret key during encryption and during decryption should match for proper encrypt-decrypt 
     * mechanism
     */
    @Test
    public final void testsharedEcdhKeyCreate() 
    {
        final AsymmetricCipherKeyPair keyPairA = EncryptionUtility.createKeyPair();
        //Extract public key bytes
        final byte[] staticPublicKeyBytesA = ((ECPublicKeyParameters)keyPairA.getPublic()).getQ().getEncoded(false);
        //First convert the key to hexadecimal format string 
        final String staticPublicKeyStringA = EncryptionHelper.toHex(staticPublicKeyBytesA);
        
        //Extract private key bytes
        final BigInteger staticPrivateKeyBytesA = ((ECPrivateKeyParameters)keyPairA.getPrivate()).getD();
        //First convert the key to hexadecimal format string 
        final String staticPrivateKeyStringA = staticPrivateKeyBytesA.toString(16);
        
        final AsymmetricCipherKeyPair keyPairB = EncryptionUtility.createKeyPair();
        //Extract public key bytes
        final byte[] staticPublicKeyBytesB = ((ECPublicKeyParameters)keyPairB.getPublic()).getQ().getEncoded(false);
        //First convert the key to hexadecimal format string 
        final String staticPublicKeyStringB = EncryptionHelper.toHex(staticPublicKeyBytesB);
        
        //Extract private key bytes
        final BigInteger staticPrivateKeyBytesB = ((ECPrivateKeyParameters)keyPairB.getPrivate()).getD();
        //First convert the key to hexadecimal format string 
        final String staticPrivateKeyStringB = staticPrivateKeyBytesB.toString(16);
      
        //Extracting local private key for system A
        final BigInteger privateStaticKeyBytesA = new BigInteger(staticPrivateKeyStringA, 16);
        final ECPrivateKeyParameters localPrivateKeyA = new ECPrivateKeyParameters(privateStaticKeyBytesA,
                                                               EncryptionUtility.CURVEPARAMS);
        //Extracting remote public key for system B
        final byte[] remotePublicKeybytesB = DatatypeConverter.parseHexBinary(staticPublicKeyStringB);
        final ECPublicKeyParameters remotePublicKeyB = new ECPublicKeyParameters(EncryptionUtility.CURVEPARAMS.
                getCurve().decodePoint(remotePublicKeybytesB), EncryptionUtility.CURVEPARAMS);

        //Creating a shared secret key using KDF for localprivateKeyA and remotePublicKeyB with localSystemId=1,
        //remoteSystemId = 2, while encryption.
        byte[] resultSharedSecretKey1 = EncryptionUtility.sharedEcdhKeyCreate(localPrivateKeyA, remotePublicKeyB, 1, 2);
        
        //Extracting local private key for system B
        final BigInteger privateStaticKeyBytesB = new BigInteger(staticPrivateKeyStringB, 16);
        final ECPrivateKeyParameters localPrivateKeyB = new ECPrivateKeyParameters(privateStaticKeyBytesB,
                                                               EncryptionUtility.CURVEPARAMS);
        //Extracting remote public key for system A
        final byte[] remotePublicKeybytesA = DatatypeConverter.parseHexBinary(staticPublicKeyStringA);
        final ECPublicKeyParameters remotePublicKeyA = new ECPublicKeyParameters(EncryptionUtility.CURVEPARAMS.
                getCurve().decodePoint(remotePublicKeybytesA), EncryptionUtility.CURVEPARAMS);

        //Creating a shared secret key using KDF for localprivateKeyB and remotePublicKeyA with localSystemId=1,
        //remoteSystemId = 2, while decryption.
        byte[] resultSharedSecretKey2 = EncryptionUtility.sharedEcdhKeyCreate(localPrivateKeyB, remotePublicKeyA, 1, 2);
        
        System.out.printf("Shared Secret Key in encryption:  %s\n", EncryptionHelper.toHex(resultSharedSecretKey1));
        System.out.printf("Shared Secret Key in decryption:  %s\n", EncryptionHelper.toHex(resultSharedSecretKey2));
        
        assertThat(resultSharedSecretKey1, is(resultSharedSecretKey2));
    }
    /**
     * Testing the createkeypair function to check if the public key created is of certain length.
     */
    @Test
    public final void testcreateKeyPair()
    {
        final AsymmetricCipherKeyPair keyPairA = EncryptionUtility.createKeyPair();
        final byte[] staticPublicKeyBytesA = ((ECPublicKeyParameters)keyPairA.getPublic()).getQ().getEncoded(false);
        assertThat(staticPublicKeyBytesA.length, is(65));     
    }    
}
