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
package mil.dod.th.ose.remote.integration.encryption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionErrorCode;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoErrorResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;
import mil.dod.th.ose.remote.integration.EncryptionUtil;
import mil.dod.th.ose.remote.integration.MatchCount;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.integration.MessageListener.MessageDetails;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestEncryptedSystem
{
    private Socket m_Socket;
    
    /**
     * Sets up the socket that connects remote interface to the controller
     */
    @Before
    public void setUp() throws Exception
    {
        m_Socket = SocketHostHelper.connectToController();  
    }

    /**
     * Closes the socket.  
     */
    @After
    public void tearDown() throws UnknownHostException, IOException
    {       
        m_Socket.close();
    }
    
    /**
     * Verify that an encryption info error response is sent when an unencrypted message is sent to an encrypted system.
     */
    @Test
    public void testEncryptionInfoErrorResponse() throws IOException, InvalidKeyException, IllegalBlockSizeException, 
        BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, 
        NoSuchPaddingException, InterruptedException
    {
        //Create the message to alter encryption level property.
        TerraHarvestMessage message = EncryptionUtil.createSetEncryptionLevelMessage(EncryptionMode.AES_ECDH_ECDSA);
        
        //Send message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for response
        MessageListener listener = new MessageListener(m_Socket);
        
        listener.waitForMessage(Namespace.ConfigAdmin,  ConfigAdminMessageType.SetPropertyResponse, 800);
       
        //Send unencrypted message that should cause an encryption error.
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //Verify only one encryption error response is received.
        listener = new MessageListener(m_Socket);
        
        listener.waitForMessage(Namespace.EncryptionInfo, 
                EncryptionInfoMessageType.EncryptionInfoErrorResponse, 1000);
        
        //Create message to return encryption type to none.
        message = EncryptionUtil.createSetEncryptionLevelMessage(EncryptionMode.NONE);
        TerraHarvestMessage encryptedMessage = EncryptionUtil.encryptMessageWithECDH(message);
        
        //Send message
        encryptedMessage.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for response
        listener.waitForMessage(Namespace.ConfigAdmin, ConfigAdminMessageType.SetPropertyResponse, 1000);
        
        //Create message
        message = EncryptionUtil.createEncryptionInfoMessage(
                EncryptionInfoMessageType.GetEncryptionTypeRequest, null);
        
        //Send request
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for response
        final EncryptionInfoNamespace namespace = (EncryptionInfoNamespace)listener.waitForMessage(
                Namespace.EncryptionInfo, EncryptionInfoMessageType.GetEncryptionTypeResponse, 500);
        
        //Verify response
        final GetEncryptionTypeResponseData data = GetEncryptionTypeResponseData.parseFrom(namespace.getData());
        
        assertThat(data.getType(), is(EncryptType.NONE));
    }
    
    /**
     * Verify that the encrypted system accepts an unencrypted encryption info error response and sends no error 
     * response. This ensures that an infinite loop condition won't happen by sending back and forth encrypted 
     * generic error responses.
     */
    @Test
    public void testNoInfiniteLoop() throws IOException, InterruptedException, InvalidKeyException, 
        IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, 
        NoSuchProviderException, NoSuchPaddingException
    {
        //Create the message to alter encryption level property.
        TerraHarvestMessage message = EncryptionUtil.createSetEncryptionLevelMessage(EncryptionMode.AES_ECDH_ECDSA);
        
        //Send message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for response
        final MessageListener listener = new MessageListener(m_Socket);
        listener.waitForMessage(Namespace.ConfigAdmin, ConfigAdminMessageType.SetPropertyResponse, 800);
        
        EncryptionInfoErrorResponseData encryptErrorResponse = EncryptionInfoErrorResponseData.newBuilder().setError(
                EncryptionErrorCode.INVALID_ENCRYPTION_LEVEL).setErrorDescription("Test error!").setType(
                        EncryptType.NONE).build();
        TerraHarvestMessage encryptErrorMessage = EncryptionUtil.createEncryptionInfoMessage(
                EncryptionInfoMessageType.EncryptionInfoErrorResponse, encryptErrorResponse);
        
        //Send message
        encryptErrorMessage.writeDelimitedTo(m_Socket.getOutputStream());
        
        //Verify that the error message was accepted and no other error message was returned. If an error message is
        //returned then there may be an infinite loop condition present.
        listener.waitForMessages(1200, new BasicMessageMatcher(Namespace.EncryptionInfo, 
                EncryptionInfoMessageType.EncryptionInfoErrorResponse, MatchCount.times(0)),
                new BasicMessageMatcher(Namespace.Base, BaseMessageType.GenericErrorResponse,  MatchCount.times(0)));
        
        //Create message to return encryption type to none.
        message = EncryptionUtil.createSetEncryptionLevelMessage(EncryptionMode.NONE);
        TerraHarvestMessage encryptedMessage = EncryptionUtil.encryptMessageWithECDH(message);
        
        //Send message
        encryptedMessage.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for response
        listener.waitForMessage(Namespace.ConfigAdmin, ConfigAdminMessageType.SetPropertyResponse, 1000);
        
        //Create message
        message = EncryptionUtil.createEncryptionInfoMessage(
                EncryptionInfoMessageType.GetEncryptionTypeRequest, null);
        
        //Send request
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for response
        final EncryptionInfoNamespace namespace = (EncryptionInfoNamespace)listener.waitForMessage(
                Namespace.EncryptionInfo, EncryptionInfoMessageType.GetEncryptionTypeResponse, 500);
        
        //Verify response
        final GetEncryptionTypeResponseData data = GetEncryptionTypeResponseData.parseFrom(namespace.getData());
        
        assertThat(data.getType(), is(EncryptType.NONE));
    }
    
    /**
     * Verify the ability to send and receive encrypted messages using ECDH with ECDSA.
     * This test requires that the public and private keys are already stored on the controller that is being connected
     * to.
     */
    @Test
    public void testEncryptedResponse() throws IOException, InvalidKeyException, InvalidAlgorithmParameterException, 
        IllegalBlockSizeException, BadPaddingException, IllegalArgumentException, NoSuchAlgorithmException, 
        NoSuchProviderException, NoSuchPaddingException, InterruptedException
    {
        //message listener that will listen for response messages.
        MessageListener listener = new MessageListener(m_Socket);
        
        //create a request for system information
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createRequestControllerInfoMsg();
        //need to set the encryption information so put the message to a builder
        TerraHarvestMessage.Builder builder = thMessage.toBuilder();
        builder.setEncryptType(EncryptType.AES_ECDH_ECDSA);
  
        //build the message containing encrypted payload information
        TerraHarvestMessage encryptedMessage = EncryptionUtil.encryptMessageWithECDH(thMessage);
        //send
        encryptedMessage.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for a response
        listener.waitForMessage(Namespace.Base, BaseMessageType.ControllerInfo, 800);
        
        //verify
        List<MessageDetails> messages = listener.getCapturedTerraHarvestMessages();
        assertThat(messages.size(), is(1));
        //get the response
        TerraHarvestMessage responseMessage = messages.get(0).getMessage();
        assertThat(responseMessage.getEncryptType(), is(EncryptType.AES_ECDH_ECDSA));

        //parse the decrypted bytes
        TerraHarvestPayload payloadDecrypt = EncryptionUtil.decryptECDHMessage(responseMessage);
        
        //verify information in the response
        assertThat(payloadDecrypt.getNamespace(), is(Namespace.Base));
        
        //Get the base namespace message
        BaseNamespace namespaceData = BaseNamespace.parseFrom(payloadDecrypt.getNamespaceMessage());
        assertThat(namespaceData.getType(), is(BaseMessageType.ControllerInfo));
        
        //system info message
        ControllerInfoData dataMessage = ControllerInfoData.parseFrom(namespaceData.getData());
        assertThat(dataMessage.getBuildInfoCount(), greaterThan(0));
        assertThat(dataMessage.getName().length(), greaterThan(1));
    }
}
