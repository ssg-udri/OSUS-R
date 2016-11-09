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
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.TransportLayerMessages.GetLinkLayerRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.GetLinkLayerResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsAvailableRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsAvailableResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsReceivingRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsReceivingResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsTransmittingRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsTransmittingResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.ShutdownRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.ose.remote.integration.CustomCommsNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.RemoteEventRegistration;
import mil.dod.th.ose.remote.integration.RemoteLinkLayerUtils;
import mil.dod.th.ose.remote.integration.RemotePhysicalLinkUtils;
import mil.dod.th.ose.remote.integration.RemoteTransportLayerUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

import example.ccomms.EchoTransport;
import example.ccomms.ExampleLinkLayer;

/**
 * @author matt
 */
public class TestTransportLayerNamespace
{
    private SharedMessages.UUID transportUuid;
    private SharedMessages.UUID pLinkUuid;
    private SharedMessages.UUID linkUuid;
    
    private Socket socket;
    
    private int listenerTimeout = 1000;
    
    /**
     * Create a physical link, link layer, and transport layer so that the transport layer can be used for unit
     * testing. Also set up the socket.
     */
    @Before
    public void setUp() throws IOException
    {
        socket = SocketHostHelper.connectToController();
        
        //------Create physical link--------
        // construct new custom comms message type
        CreatePhysicalLinkRequestData createPLinkRequest = CreatePhysicalLinkRequestData.newBuilder().
                setPhysicalLinkType(CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C).
                setPhysicalLinkName("food").
                build();
        
        CustomCommsNamespace.Builder customCommsMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.CreatePhysicalLinkRequest).
                setData(createPLinkRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.CustomComms, customCommsMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // read in response
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        CustomCommsNamespace namespaceResponse = CustomCommsNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        
        CreatePhysicalLinkResponseData createPhysicalLinkResponse = CreatePhysicalLinkResponseData.parseFrom(
                namespaceResponse.getData());
        
        pLinkUuid = createPhysicalLinkResponse.getInfo().getUuid();
        
        //------Create link layer--------
        // construct new custom comms message type
        CreateLinkLayerRequestData createLLayerRequest = CreateLinkLayerRequestData.newBuilder().
                setLinkLayerProductType(ExampleLinkLayer.class.getCanonicalName()).
                setLinkLayerName("bear").
                setPhysicalLinkUuid(pLinkUuid).
                build();
        
        customCommsMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.CreateLinkLayerRequest).
                setData(createLLayerRequest.toByteString());

        // create terra harvest message
        message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.CustomComms, customCommsMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
        payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        namespaceResponse = CustomCommsNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        
        CreateLinkLayerResponseData createLinkResponse = CreateLinkLayerResponseData.parseFrom(
                namespaceResponse.getData());
        
        linkUuid = createLinkResponse.getInfo().getUuid();
        
        //------Create transport layer--------
        // construct new custom comms message type
        CreateTransportLayerRequestData createTLayerResquest = CreateTransportLayerRequestData.newBuilder().
                setTransportLayerName("annie").
                setLinkLayerUuid(linkUuid).
                setTransportLayerProductType(EchoTransport.class.getCanonicalName()).
                build();
        
        customCommsMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.CreateTransportLayerRequest).
                setData(createTLayerResquest.toByteString());
        
        // create terra harvest message
        message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.CustomComms, customCommsMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
        payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        namespaceResponse = CustomCommsNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        
        CreateTransportLayerResponseData createTransportResponse = CreateTransportLayerResponseData.parseFrom(
                namespaceResponse.getData());
        
        transportUuid = createTransportResponse.getInfo().getUuid();
    }
    
    /**
     * Remove the physical link, link layer, and transport layer that were created in setup, verify they are gone.
     */
    @After
    public void tearDown() throws IOException, InterruptedException
    {
        try
        {
            //------Remove transport layer--------
            RemoteTransportLayerUtils.removeTransportLayer(transportUuid, socket);
            
            //------Remove link layer--------
            RemoteLinkLayerUtils.removeLink(linkUuid, socket);
            
            //------Remove physical link--------
            RemotePhysicalLinkUtils.tryRemovePhysicalLink(pLinkUuid, socket);

            // verify physical link, link layer, and transport layer are gone
            List<SharedMessages.UUID> uuidList = CustomCommsNamespaceUtils.getLayerUuidsByType(socket, 
                    CommType.PhysicalLink);
            assertThat(uuidList.contains(pLinkUuid), is(false));
            
            uuidList = CustomCommsNamespaceUtils.getLayerUuidsByType(socket, CommType.Linklayer);
            assertThat(uuidList.contains(linkUuid), is(false));
            
            uuidList = CustomCommsNamespaceUtils.getLayerUuidsByType(socket, CommType.TransportLayer);
            assertThat(uuidList.contains(transportUuid), is(false));
        }
        finally
        {
            socket.close();
        }
    }
    
    /**
     * Assuming that a transport was created in setup, echo transport will always return true for any address,
     * verify that true is returned.
     */
    @Test
    public void testIsAvailable() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        IsAvailableRequestData isReceivingRequest = IsAvailableRequestData.newBuilder().
                setUuid(transportUuid).
                setAddress("Example:5").
                build();
        
        TransportLayerNamespace.Builder transportLayerMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.IsAvailableRequest).
                setData(isReceivingRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.TransportLayer, 
                transportLayerMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.TransportLayer,
                TransportLayerMessageType.IsAvailableResponse, listenerTimeout);

        TransportLayerNamespace response = (TransportLayerNamespace)responseRcvd;
        
        IsAvailableResponseData dataResponse = IsAvailableResponseData.parseFrom(
                response.getData());
        
        assertThat(dataResponse.getIsAvailable(), is(true));
        assertThat(dataResponse.getUuid(), is(transportUuid));
    }
    
    /**
     * Assuming a transport layer was created in setup, verify that the default of a newly created transport layer
     * receiving property is set to false.
     */
    @Test
    public void testIsReceiving() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        IsReceivingRequestData isReceivingRequest = IsReceivingRequestData.newBuilder().
                setUuid(transportUuid).
                build();
        
        TransportLayerNamespace.Builder transportLayerMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.IsReceivingRequest).
                setData(isReceivingRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.TransportLayer, 
                transportLayerMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.TransportLayer,
                TransportLayerMessageType.IsReceivingResponse, listenerTimeout);

        TransportLayerNamespace response = (TransportLayerNamespace)responseRcvd;
        
        IsReceivingResponseData dataResponse = IsReceivingResponseData.parseFrom(
                response.getData());
        
        assertThat(dataResponse.getIsReceiving(), is(false));
    }
    
    /**
     * Assuming a transport layer was created in setup, verify that the default of a newly created transport layer's
     * transmitting property is set to false.
     */
    @Test
    public void testIsTransmitting() throws IOException
    {
        IsTransmittingRequestData isTransmittingRequest = IsTransmittingRequestData.newBuilder().
                setUuid(transportUuid).
                build();
        
        TransportLayerNamespace.Builder transportLayerMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.IsTransmittingRequest).
                setData(isTransmittingRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.TransportLayer, 
                transportLayerMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // read in response
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        TransportLayerNamespace namespaceResponse = TransportLayerNamespace.
            parseFrom(payLoadTest.getNamespaceMessage());
        
        IsTransmittingResponseData getLayersResponse = IsTransmittingResponseData.parseFrom(
                namespaceResponse.getData());
        
        assertThat(getLayersResponse.getIsTransmitting(), is(false));
    }
    
    /**
     * Assuming that a transport layer was created in setup with an associated link layer, verify that the correct 
     * link layer is returned when requesting to get the link layer for the transport layer.
     */
    @Test
    public void testGetLinkLayer() throws IOException
    {
        GetLinkLayerRequestData getLinkLayerRequest = GetLinkLayerRequestData.newBuilder().
                setUuid(transportUuid).
                build();
        
        TransportLayerNamespace.Builder transportLayerMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.GetLinkLayerRequest).
                setData(getLinkLayerRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.TransportLayer, 
                transportLayerMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // read in response
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        TransportLayerNamespace namespaceResponse = TransportLayerNamespace.
              parseFrom(payLoadTest.getNamespaceMessage());
        
        GetLinkLayerResponseData getLinkLayerResponse = GetLinkLayerResponseData.parseFrom(
                namespaceResponse.getData());
        
        assertThat(getLinkLayerResponse.getUuid(), is(linkUuid));
    }
    
    /**
     * Assuming a transport layer was created in setup with an associated link layer, verify that the transport layer 
     * can be successfully shutdown. This means activating the link layer and calling the shutdown method on the 
     * transport layer and verifying that the link layer was deactivated.
     */
    @Test
    public void testShutdown() throws IOException, InterruptedException
    {
        //---Activate the link layer associated with the transport layer---
        MessageListener listener = new MessageListener(socket);
        
        //register for event so that the test does not move forward before activation is complete
        RemoteEventRegistration.regRemoteEventMessages(socket, LinkLayer.TOPIC_ACTIVATED);
        
        // construct new link layer message type
        ActivateRequestData activateRequest = ActivateRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.ActivateRequest).
                setData(activateRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        listener.waitForMessages(3000,
                new BasicMessageMatcher(Namespace.LinkLayer, LinkLayerMessageType.ActivateResponse),
                new BasicMessageMatcher(Namespace.EventAdmin, EventAdminMessageType.SendEvent));
        
        MessageListener.unregisterEvent(socket);
        
        //---Verify the link layer was activated---
        // construct new link layer message type
        IsActivatedRequestData isActivatedRequest = IsActivatedRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.IsActivatedRequest).
                setData(isActivatedRequest.toByteString());
        
        message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.IsActivatedResponse, listenerTimeout);
        LinkLayerNamespace response = (LinkLayerNamespace)responseRcvd;
        IsActivatedResponseData dataResponse = IsActivatedResponseData.parseFrom(response.getData());
        
        assertThat(dataResponse.getIsActivated(), is(true));
        
        //register to listen for when the link layer deactivates
        RemoteEventRegistration.regRemoteEventMessages(socket, LinkLayer.TOPIC_DEACTIVATED);
        
        //---Shutdown the transport layer---
        ShutdownRequestData shutdownRequest = ShutdownRequestData.newBuilder().
                setUuid(transportUuid).
                build();
        
        TransportLayerNamespace.Builder transportLayerMessage = TransportLayerNamespace.newBuilder().
                setType(TransportLayerMessageType.ShutdownRequest).
                setData(shutdownRequest.toByteString());
        
        message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.TransportLayer, 
                transportLayerMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        listener.waitForMessages(3000,
                new BasicMessageMatcher(Namespace.TransportLayer, TransportLayerMessageType.ShutdownResponse),
                new BasicMessageMatcher(Namespace.EventAdmin, EventAdminMessageType.SendEvent));
        
        //---Verify that the link layer associated with the transport layer is deactivated---
        isActivatedRequest = IsActivatedRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.IsActivatedRequest).
                setData(isActivatedRequest.toByteString());
        
        message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());

        responseRcvd = listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.IsActivatedResponse, listenerTimeout);
        response = (LinkLayerNamespace)responseRcvd;
        dataResponse = IsActivatedResponseData.parseFrom(response.getData());
        
        assertThat(dataResponse.getIsActivated(), is(false));
    }
    
    /**
     * Verifies the ability of the system to get a transport layer's capabilities remotely.
     */
    @Test
    public final void testGetCapabilities() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
    
        GetCapabilitiesRequestData transportLayerCapsRequest = GetCapabilitiesRequestData.newBuilder().
                setCommType(CommType.TransportLayer).setProductType(EchoTransport.class.getName()).build();
    
        TerraHarvestMessage message = 
                createTransportLayerMessage(CustomCommsMessageType.GetCapabilitiesRequest, transportLayerCapsRequest);
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        Message responseRcvd = listener.waitForMessage(Namespace.CustomComms,
                CustomCommsMessageType.GetCapabilitiesResponse, 1000);
        CustomCommsNamespace response = (CustomCommsNamespace)responseRcvd;
        GetCapabilitiesResponseData dataResponse = GetCapabilitiesResponseData.parseFrom(response.getData());
    
        //test various capabilities
        assertThat(dataResponse.getTransportCapabilities(), is(notNullValue()));
        assertThat(dataResponse.getTransportCapabilities().getBase().getDescription(), 
                is("An example transport layer that echoes messages back through the same transport layer"));
        assertThat(dataResponse.getTransportCapabilities().getBase().getManufacturer(), is("ExampleManufacturer"));
        assertThat(dataResponse.getTransportCapabilities().getBase().getProductName(), is("Echo Transport"));
    }
    
    /**
     * Helper method for creating transport layer messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createTransportLayerMessage(final CustomCommsMessageType type, 
            final Message message)
    {
        CustomCommsNamespace.Builder commsMessageBuilder = CustomCommsNamespace.newBuilder().
                setType(type).
                setData(message.toByteString());

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.CustomComms, commsMessageBuilder);
        return thMessage;
    }
}
