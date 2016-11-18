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
package mil.dod.th.ose.controller.integration.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage.Version;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;
import example.ccomms.QueueTransport;

/**
 * Tests loading of configuration information from an .xml file for all remote channel types.
 * 
 * @author cweisenborn
 */
public class TestRemoteChannelConfigs
{
    /**
     * Verify that the remote channels defined in the configs.xml are added to the {@link RemoteChannelLookup}.
     */
    @Test
    public void testRemoteChannelConfigLoading()
    {
        RemoteChannelLookup remoteChannelLookup = IntegrationTestRunner.getService(RemoteChannelLookup.class);
        
        assertThat(remoteChannelLookup.getChannels(300).size(), is(1));
        assertThat(remoteChannelLookup.getChannels(2).size(), is(1));
        
        TransportChannel transportChannel = (TransportChannel)remoteChannelLookup.getChannels(300).get(0);
        assertThat(transportChannel.getTransportLayerName(), is("xmlConfigTransportLayer"));
        assertThat(transportChannel.getLocalMessageAddress(), is("Example:1"));
        assertThat(transportChannel.getRemoteMessageAddress(), is("Example:2"));
        
        SocketChannel socketChannel = (SocketChannel)remoteChannelLookup.getChannels(2).get(0);
        assertThat(socketChannel.getHost(), is("localhost"));
        assertThat(socketChannel.getPort(), is(4001));
    }
    
    /**
     * Verify that the XML created transport channel can send a message using the specified transport layer.
     */
    @Test
    public void testTransportChannelTrySend()
    {
        RemoteChannelLookup remoteChannelLookup = IntegrationTestRunner.getService(RemoteChannelLookup.class);
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        
        //Verify that the transport layer needed by the channel already exists.
        TransportLayer tLayer = customCommsService.getTransportLayer("xmlConfigTransportLayer");
        assertThat(tLayer, is(notNullValue()));
        
        //Retrieve the transport channel that should be used to send a message.
        TransportChannel transportChannel = (TransportChannel)remoteChannelLookup.getChannels(300).get(0);
        TerraHarvestMessage tMessage = createMessage();
        
        //Verify that the channel can be used to send a message.
        boolean sent = transportChannel.trySendMessage(tMessage);
        assertThat(sent, is(true));
    }
    
    /**
     * Verify that the XML created transport channel can send a message through a transport layer that is created after
     * the channel.
     */
    @Test
    public void testTransportChannelCreateTrySend() throws CCommException
    {
        RemoteChannelLookup remoteChannelLookup = IntegrationTestRunner.getService(RemoteChannelLookup.class);
              
        TransportChannel transportChannel = (TransportChannel)remoteChannelLookup.getChannels(250).get(0);
        TerraHarvestMessage tMessage = createMessage();
        //Try sending a message before the transport layer has been created. Message should fail to send.
        boolean sent = transportChannel.trySendMessage(tMessage);
        assertThat(sent, is(false));
        
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        //Create the transport layer needed by the channel.
        TransportLayer tLayer = customCommsService.createTransportLayer("example.ccomms.EchoTransport", 
                transportChannel.getTransportLayerName(), (String)null);
        assertThat(tLayer, is(notNullValue()));
        
        //Try sending a message now that the transport layer exists. Message should send successfully.
        sent = transportChannel.trySendMessage(tMessage);
        assertThat(sent, is(true));
    }

    
    @Test
    public void testEventQueueing() throws CCommException, IllegalArgumentException 
    {
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        RemoteChannelLookup remoteChannelLookup = IntegrationTestRunner.getService(RemoteChannelLookup.class);
        int queueChannel = 150;
        //Create a remote channel to send messages over, and reset its associated transport layer
        TransportChannel tChannel = (TransportChannel)remoteChannelLookup.getChannel(queueChannel);
        //Create a fake Transport Layer that will return false for the isAvailable call
        TransportLayer tLayer = customCommsService.
                createTransportLayer(QueueTransport.class.getName(), 
                        tChannel.getTransportLayerName(), (String)null);
        assertThat(tLayer, is(notNullValue()));
        //clear queue and make sure that no messages are currently queued
        tChannel.clearQueuedMessages();
        assertThat(tChannel.getQueuedMessageCount(), is(0));
        //Create and send a Message over the fake transport layer
        TerraHarvestMessage thMessage =  createMessage();
        boolean queue = tChannel.queueMessage(thMessage);
        assertThat(queue, is(true));
        //Make sure that the message gets properly queued
        assertThat(tChannel.getQueuedMessageCount(), is(1));
        //clear Queued Messages
        tChannel.clearQueuedMessages();
        assertThat(tChannel.getQueuedMessageCount(), is(0));
    }
    
    /**
     * Create a message used for testing.
     */
    private TerraHarvestMessage createMessage()
    {
        BaseNamespace namespaceMsg = BaseNamespace.newBuilder().setType(BaseMessageType.GetOperationModeRequest)
                .build();
        TerraHarvestPayload tPayload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.Base)
                .setNamespaceMessage(namespaceMsg.toByteString()).build();
        TerraHarvestMessage tMessage = TerraHarvestMessage.newBuilder().setDestId(200).setSourceId(1).setMessageId(5000)
                .setVersion(Version.newBuilder().setMajor(1).setMinor(1).build())
                .setTerraHarvestPayload(tPayload.toByteString()).build();
        return tMessage;
    }
}
