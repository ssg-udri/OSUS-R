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
package mil.dod.th.ose.remote.integration.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;

import org.junit.Test;

import com.google.protobuf.ByteString;

/**
 * Class verifies basic handling of messages for a socket connection only.
 * 
 * @author Dave Humeniuk
 *
 */
public class TestSocketChannel
{
    /**
     * Verify connection can be made to the default port for sockets.
     * Verify multiple connections can be made one after another
     */
    @Test
    public void testSequentialConnect() throws UnknownHostException, IOException
    {
        // want to try to connect several times, just an arbitrary high number
        for (int i=0; i < 30; i++)
        {
            Socket socket = SocketHostHelper.connectToController();
            socket.close();
        }
    }

    /**
     * Verify connection can be made to the default port for sockets.
     * Verify multiple connections can be made at once.
     */
    @Test
    public void testMultiConnect() throws UnknownHostException, IOException
    {
        // verify can make connection
        try (Socket socket = SocketHostHelper.connectToController())
        {
            // verify can make multiple connections
            try (Socket socket2 = SocketHostHelper.connectToController())
            {
                try (Socket socket3 = SocketHostHelper.connectToController())
                {
                    // verify each socket is actually a different connection
                    assertThat(socket.getLocalPort(), is(not(socket2.getLocalPort())));
                    assertThat(socket.getLocalPort(), is(not(socket3.getLocalPort())));
                    assertThat(socket2.getLocalPort(), is(not(socket3.getLocalPort())));
                }
            }
        }
    }
    
    /**
     * Verify a simple message can be sent and a response is received.
     */
    @Test
    public void testMessage() throws IOException
    {
        try (Socket socket = SocketHostHelper.connectToController())
        {
            // send out message
            TerraHarvestMessage request = TerraHarvestMessageHelper.createRequestControllerInfoMsg();
            request.writeDelimitedTo(socket.getOutputStream());
            
            // read in response
            TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
            TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
            assertThat(response, is(notNullValue()));
            assertThat(payLoadTest.getNamespace(), is(Namespace.Base));
    
            BaseNamespace namespaceResponse = BaseNamespace.parseFrom(payLoadTest.getNamespaceMessage());
            assertThat(namespaceResponse.getType(), is(BaseMessageType.ControllerInfo));
            
            ControllerInfoData systemInfo = ControllerInfoData.parseFrom(namespaceResponse.getData());
            assertThat(systemInfo.isInitialized(), is(true));
        }
    }
    
    /**
     * Confirm that sending a message over the max size will close the connection.
     */
    @Test(expected = SocketException.class)
    public void testBigMessage() throws IOException
    {
        try (Socket socket = SocketHostHelper.connectToController())
        {
            // send out message with the payload being the max size
            byte[] bytes = new byte[16777216];        
            ByteString bs = ByteString.copyFrom(bytes);
            TerraHarvestMessage msg = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.Base,
                    BaseNamespace.newBuilder().setType(BaseMessageType.ControllerInfo).setData(bs));
            msg.writeDelimitedTo(socket.getOutputStream());
        }
    }
    
    /**
     * Verify sending garbage data won't trip up the channel.
     */
    @Test
    public void testGarbage() throws UnknownHostException, IOException
    {
        try (Socket socket = SocketHostHelper.connectToController())
        {
            // complete garbage message, first byte is the varint for the octet count to follow (so total size is 4)
            socket.getOutputStream().write(new byte[] {0x3, 0x67, 0x11, 0x22});
            
            // incomplete message
            TerraHarvestMessage message = TerraHarvestMessage.getDefaultInstance();
            message.writeDelimitedTo(socket.getOutputStream());
            
            // send out real message to ensure it is still accepted
            TerraHarvestMessage request = TerraHarvestMessageHelper.createRequestControllerInfoMsg();
            request.writeDelimitedTo(socket.getOutputStream());
            
            // read in response
            TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
            TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
            assertThat(response, is(notNullValue()));
            assertThat(payLoadTest.getNamespace(), is(Namespace.Base));
        }
    }
}
