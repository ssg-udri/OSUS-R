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
package mil.dod.th.ose.remote.integration.namespace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.integration.EncryptionUtil;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for the {@link EncryptionInfoNamespace}.
 * 
 * @author cweisenborn
 */
public class TestEncryptionInfoNamespace
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
     * Verify that a valid encryption info response is sent when the controller receives an encryption info request.
     */
    @Test
    public void testGetEncryptionTypeRequest() throws IOException, InvalidKeyException, NoSuchAlgorithmException, 
        NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, 
        InvalidAlgorithmParameterException, InterruptedException
    {
        //Create message
        final TerraHarvestMessage message = EncryptionUtil.createEncryptionInfoMessage(
                EncryptionInfoMessageType.GetEncryptionTypeRequest, null);
        
        //Send request
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        //wait for response
        final MessageListener listener = new MessageListener(m_Socket);
        
        //Verify response
        final EncryptionInfoNamespace namespace = (EncryptionInfoNamespace)listener.waitForMessage(
                Namespace.EncryptionInfo, EncryptionInfoMessageType.GetEncryptionTypeResponse, 500);
        
        final GetEncryptionTypeResponseData data = GetEncryptionTypeResponseData.parseFrom(namespace.getData());
        
        assertThat(data.getType(), is(EncryptType.NONE));
    }
}
