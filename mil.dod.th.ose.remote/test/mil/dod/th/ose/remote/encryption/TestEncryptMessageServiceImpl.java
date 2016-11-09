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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.NoSuchPaddingException;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.RemoteBase;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.api.EncryptionHelper;
import mil.dod.th.ose.remote.api.EncryptionUtility;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author powarniu
 * verify the encrypted payload can be successfully decrypted and compare the original payload
 * with decrypted payload.
 *
 */
public class TestEncryptMessageServiceImpl 
{
    private File m_Folder;
    private File m_Temp;
    private File m_Temp_pub;
    private File m_Temp_priv;
    private EncryptMessageServiceImpl m_SUT;
    @Before
    public final void setup() throws IOException
    {
        // this is the normal set-up
        m_Folder = new File(".", "encrypt-conf");
        m_Folder.mkdir();
        m_Temp = new File("encrypt-conf","th.encryption.authorizedKeys.properties");  
        FileOutputStream outAuthorize = new FileOutputStream(m_Temp, false); 
        outAuthorize.close();
        m_Temp_pub = new File("encrypt-conf","th.encryption.public.properties");  
        FileOutputStream outPublic = new FileOutputStream(m_Temp_pub, false); 
        outPublic.close();
        m_Temp_priv = new File("encrypt-conf","th.encryption.private.properties");  
        FileOutputStream outPrivate = new FileOutputStream(m_Temp_priv, false); 
        outPrivate.close();

        m_SUT = new EncryptMessageServiceImpl();
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());      
    }
    @After
    public void tearDown() throws Exception
    {
        m_Temp.delete();
        m_Temp_pub.delete();
        m_Temp_priv.delete();
        m_Temp_pub.delete();
        m_Folder.delete();
    }

    /**
     * This function encrypts a TerraHarvest payload which is inside a TerraHarvest message.
     * On the other end it is decrypted at the destination controller. The decrypted message is compared
     * to the unencrypted payload to make sure they are the same
     * System 1, Key A are all related to Source or sender controller
     * System 2, Key B are all related to Destination or recipient controller
     *  
     *  
     */   
    @Test
    public final void testEncryptDecryptPayload() 
        throws IOException, NoSuchAlgorithmException, NoSuchProviderException, 
        NoSuchPaddingException, InvalidKeySignatureException 
    {
        m_SUT.activate();
        // First generate keys for the controllers. In reality this would be done in advance and the keys
        // loaded into a config file on each controller.
        // In our case source is controller A with controller id =1 and destination is controller B with id = 2
        
        final AsymmetricCipherKeyPair keyPairA = EncryptionUtility.createKeyPair();
        //Extract public key bytes
        final byte[] staticPublicKeyBytesA = ((ECPublicKeyParameters)keyPairA.getPublic()).getQ().getEncoded(false);
        //First convert the key to hexadecimal format string to save it into the file
        final String staticPublicKeyStringA = EncryptionHelper.toHex(staticPublicKeyBytesA);
        
        //Extract private key bytes
        final BigInteger staticPrivateKeyBytesA = ((ECPrivateKeyParameters)keyPairA.getPrivate()).getD();
        //First convert the key to hexadecimal format string to save it into the file
        final String staticPrivateKeyStringA = staticPrivateKeyBytesA.toString(16);
        
        m_SUT.saveStaticKeyPair(staticPublicKeyStringA, staticPrivateKeyStringA);
        m_SUT.saveAuthorizedKey(1, staticPublicKeyStringA);
   
        final AsymmetricCipherKeyPair keyPairB = EncryptionUtility.createKeyPair();
        //Extract public key bytes
        final byte[] staticPublicKeyBytesB = ((ECPublicKeyParameters)keyPairB.getPublic()).getQ().getEncoded(false);
        //First convert the key to hexadecimal format string to save it into the file
        final String staticPublicKeyStringB = EncryptionHelper.toHex(staticPublicKeyBytesB);
        
        //Extract private key bytes
        final BigInteger staticPrivateKeyBytesB = ((ECPrivateKeyParameters)keyPairB.getPrivate()).getD();
        //First convert the key to hexadecimal format string to save it into the file
        final String staticPrivateKeyStringB = staticPrivateKeyBytesB.toString(16);
        

        //set the private static keys for both the controller 2 which is the destination controller
        //In normal circumstances you don't have to do this step

        m_SUT.saveAuthorizedKey(2, staticPublicKeyStringB);
 
        // construct a single message containing data within a namespace message
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder()
                .setName("test-name")
                .build();
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .setData(systemInfoData.toByteString())
                .build();
        //build payload
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(baseNamespaceMessage.toByteString()).
                build();
        // build a terra harvest message
        final TerraHarvestMessage.Builder fullMessage = TerraHarvestMessage.newBuilder().
                setVersion(RemoteConstants.SPEC_VERSION).
                setSourceId(1).
                // send message back to source
                setDestId(2).
                setMessageId(100).
                setEncryptType(RemoteBase.EncryptType.AES_ECDH_ECDSA);
                
        //Encrypt payload
        TerraHarvestMessage fullEncryptedMessage = m_SUT.encryptMessage(fullMessage, payload); 
        
        //Each controller will have to do it independently. but for testing purposes I also set the
        //pub-priv key for controller 2
        m_SUT.saveStaticKeyPair(staticPublicKeyStringB, staticPrivateKeyStringB);
        
        //Decrypt payload
        TerraHarvestPayload payloadDecrypted = m_SUT.decryptRemoteMessage(fullEncryptedMessage);
        System.out.printf("Original payload:  %s\n", payload);
        System.out.printf("After Encryption: %s\n", fullEncryptedMessage);
        System.out.printf("After Decryption: %s\n", payloadDecrypted);
        
        assertThat(payloadDecrypted, is(payload));        
    }
}
