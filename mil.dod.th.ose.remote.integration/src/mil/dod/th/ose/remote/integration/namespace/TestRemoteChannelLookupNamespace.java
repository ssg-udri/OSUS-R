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

import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace.
    RemoteChannelLookupMessageType;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.SyncTransportChannelRequestData;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.SyncTransportChannelResponseData;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.RemoteLinkLayerUtils;
import mil.dod.th.ose.remote.integration.RemotePhysicalLinkUtils;
import mil.dod.th.ose.remote.integration.RemoteTransportLayerUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

import example.ccomms.EchoTransport;
import example.ccomms.ExampleLinkLayer;

/**
 * Test the remote channel lookup service
 * @author callen
 *
 */
public class TestRemoteChannelLookupNamespace
{
    private Socket socket;
    
    /**
     * Physical Link UUID.
     */
    private UUID pLinkUuid;
    
    /**
     * Link Layer UUID.
     */
    private UUID linkUuid;
    
    /**
     * Transport Layer UUID.
     */
    private UUID transportUuid;
    
    @Before
    public void setUp() throws UnknownHostException, IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
        //physical link
        pLinkUuid = RemotePhysicalLinkUtils.createPhysicalLink(
                CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, "phys-12", socket);

        //link layer
        linkUuid = RemoteLinkLayerUtils.createLinkLayer("link-12", ExampleLinkLayer.class.getName(), 
                pLinkUuid, socket);
        
        transportUuid = RemoteTransportLayerUtils.createTransportLayer("trans-12", EchoTransport.class.getName(),
                linkUuid, socket);
    }
    
    /**
     * Remove both physical links created in setup, verify they are removed.
     */
    @After
    public void tearDown() throws Exception
    {
        try
        {
            Exception theFirstException = null;
            try
            {
                // cleanup
                RemoteTransportLayerUtils.removeTransportLayer(transportUuid, socket);
            }
            catch (Exception e)
            {
                if (theFirstException == null)
                {
                    theFirstException = e;
                }
            }
            try
            {
                RemoteLinkLayerUtils.removeLink(linkUuid, socket);
            }
            catch (Exception e)
            {
                if (theFirstException == null)
                {
                    theFirstException = e;
                }
            }

            RemotePhysicalLinkUtils.tryRemovePhysicalLink(pLinkUuid, socket);

            try
            {
                MessageListener.unregisterEvent(socket); 
            }
            catch (Exception e)
            {
                if (theFirstException == null)
                {
                    theFirstException = e;
                }
            }
            if (theFirstException != null)
            {
                throw theFirstException;
            }
        }
        finally
        {
            socket.close();
        }
    }
    
    /**
     * Verify ability to remotely sync a transport layer.
     */
    @Test
    public void testSyncTransportLayer() throws IOException, InterruptedException
    {
        //request the syncing of the channel
        MessageListener listener = new MessageListener(socket);
        
        SyncTransportChannelRequestData request = SyncTransportChannelRequestData.newBuilder().
                setDestSystemAddress("remote").
                setRemoteSystemAddress("local").
                setTransportLayerName("trans-12").
                setRemoteSystemId(7).build();
        
        TerraHarvestMessage thMessage = 
            createRemoteChannelLookupMessage(RemoteChannelLookupMessageType.SyncTransportChannelRequest, request);
        
        thMessage.writeDelimitedTo(socket.getOutputStream());

        //get namespace message
        RemoteChannelLookupNamespace namespaceMessage = (RemoteChannelLookupNamespace)listener.waitForMessage(
            Namespace.RemoteChannelLookup, RemoteChannelLookupMessageType.SyncTransportChannelResponse, 500);
        //create response message
        SyncTransportChannelResponseData responseMessage = 
            SyncTransportChannelResponseData.parseFrom(namespaceMessage.getData());
        
        assertThat(responseMessage.getRemoteSystemId(), is(7));
        assertThat(responseMessage.getTransportLayerName(), is("trans-12"));
        assertThat(responseMessage.getRemoteSystemAddress(), is("local"));
        assertThat(responseMessage.getSourceSystemAddress(), is("remote"));
    }
    
    /**
     * Helper method for creating RemoteChannelLookup messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createRemoteChannelLookupMessage(final RemoteChannelLookupMessageType type, 
            final Message message)
    {
        RemoteChannelLookupNamespace.Builder ccommMessageBuilder = RemoteChannelLookupNamespace.
                newBuilder().
                setType(type);

        if (message != null)
        {
            ccommMessageBuilder.setData(message. toByteString());
        }

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.RemoteChannelLookup, ccommMessageBuilder);
        return thMessage;
    }
}
