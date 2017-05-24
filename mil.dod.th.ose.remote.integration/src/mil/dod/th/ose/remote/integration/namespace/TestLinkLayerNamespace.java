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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsTypes;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetMtuRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetMtuResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetPhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetPhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsAvailableRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsAvailableResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.PerformBITRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.PerformBITResponseData;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.ose.remote.integration.CustomCommsNamespaceUtils;
import mil.dod.th.ose.remote.integration.RemoteEventRegistration;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.RemoteLinkLayerUtils;
import mil.dod.th.ose.remote.integration.RemotePhysicalLinkUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

import example.ccomms.ExampleLinkLayer;

/**
 * Tests the interaction of the remote interface with the {@link LinkLayerNamespace}.  Specifically, 
 * the class tests that LinkLayer messages are properly sent and that appropriate responses are
 * received.
 * @author matt
 */
public class TestLinkLayerNamespace
{
    private SharedMessages.UUID pLinkUuid;
    private SharedMessages.UUID linkUuid;
    
    private Socket socket;
    
    private int listenerTimeout = 600;
    
    /**
     * Setup and connect the socket to the controller.
     * Create a physical link, and link layer for use with unit tests that need one. 
     * The physical link and link layer will be removed in the tear down method.
     */
    @Before
    public void setUp() throws IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
        
        //---Create physical link---
        pLinkUuid = RemotePhysicalLinkUtils.createPhysicalLink(
                CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, "cat", socket);
        
        //---Create link layer---
        linkUuid = RemoteLinkLayerUtils.createLinkLayer(
                "bear", ExampleLinkLayer.class.getName(), pLinkUuid, socket); 
    }
    
    /**
     * Remove the physical link, and link layer created in setup, verify they are removed.
     */
    @After
    public void tearDown() throws IOException, InterruptedException
    {
        try
        {
            // ---Remove link layer---
            RemoteLinkLayerUtils.removeLink(linkUuid, socket);

            // ---Remove physical link---
            RemotePhysicalLinkUtils.tryRemovePhysicalLink(pLinkUuid, socket);
            
            // verify physical link and link layer are gone
            List<SharedMessages.UUID> uuidList = CustomCommsNamespaceUtils.getLayerUuidsByType(socket,
                    CommType.PhysicalLink);
            assertThat(uuidList.contains(pLinkUuid), is(false));

            uuidList = CustomCommsNamespaceUtils.getLayerUuidsByType(socket, CommType.Linklayer);
            assertThat(uuidList.contains(linkUuid), is(false));
            MessageListener.unregisterEvent(socket);           
        }
        finally
        {
            socket.close();
        }
    }
    
    /**
     * Assuming that a link layer was created in setup, verify that the physical link associated with the link layer 
     * is the physical link that was created first in the setup.
     */
    @Test
    public void testGetPhysicalLink() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // construct new link layer message type
        GetPhysicalLinkRequestData getPhysicalLinkRequest = GetPhysicalLinkRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.GetPhysicalLinkRequest).
                setData(getPhysicalLinkRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        LinkLayerNamespace response = (LinkLayerNamespace)listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.GetPhysicalLinkResponse, listenerTimeout);
        
        GetPhysicalLinkResponseData dataResponse = GetPhysicalLinkResponseData.parseFrom(
                response.getData());
        
        assertThat(dataResponse.getPhysicalLinkUuid(), is(pLinkUuid));
    }
    
    /**
     * Assuming that a link layer was created in setup, make sure that a valid address is available to the link layer.
     */
    @Test
    public void testIsAvailable() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //---Verify a valid address returns that it is available---
        // construct new link layer message type
        IsAvailableRequestData isAvailableRequest = IsAvailableRequestData.newBuilder().
                setUuid(linkUuid).
                setAddress("Example:5").
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.IsAvailableRequest).
                setData(isAvailableRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        LinkLayerNamespace response = (LinkLayerNamespace)listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.IsAvailableResponse, listenerTimeout);
       
        IsAvailableResponseData dataResponse = IsAvailableResponseData.parseFrom(
                response.getData());
        
        assertThat(dataResponse.getAvailable(), is(true));
        assertThat(dataResponse.getUuid(), is(linkUuid));
        
        //---Verify an invalid address returns an error message---
        // construct new link layer message type
        isAvailableRequest = IsAvailableRequestData.newBuilder().
                setUuid(linkUuid).
                setAddress("giraffe").
                build();
        
        linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.IsAvailableRequest).
                setData(isAvailableRequest.toByteString());
        
        message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        BaseNamespace errorResponse = (BaseNamespace)listener.waitForMessage(Namespace.Base, 
                BaseMessageType.GenericErrorResponse, listenerTimeout);
        
        GenericErrorResponseData genericErrorResponseTest = GenericErrorResponseData.parseFrom(errorResponse.getData());
        
        assertThat(genericErrorResponseTest.getError(), is(ErrorCode.INVALID_VALUE));
    }
    
    /**
     * Assuming that a link layer was created in setup, verify the correct MTU can be received.
     */
    @Test
    public void testGetMTU() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // construct new link layer message type
        GetMtuRequestData getMtuRequest = GetMtuRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.GetMtuRequest).
                setData(getMtuRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        LinkLayerNamespace response = (LinkLayerNamespace)listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.GetMtuResponse, listenerTimeout);
        
        GetMtuResponseData dataResponse = GetMtuResponseData.parseFrom(response.getData());
        
        // verify dynamic value returned by proxy is returned through remote interface
        assertThat(dataResponse.getMtu(), is(1000));
    }
    
    /**
     * Assuming link layer was created in the setup, verify that it defaults to deactivated.
     */
    @Test
    public void testIsActivated() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // construct new link layer message type
        IsActivatedRequestData isActivatedRequest = IsActivatedRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.IsActivatedRequest).
                setData(isActivatedRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        LinkLayerNamespace response = (LinkLayerNamespace)listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.IsActivatedResponse, listenerTimeout);
       
        IsActivatedResponseData dataResponse = IsActivatedResponseData.parseFrom(response.getData());
        
        assertThat(dataResponse.getIsActivated(), is(false));
    }
    
    /**
     * Assuming a link layer was created in setup.
     * Activate the link layer and verify we receive that it is active.
     */
    @Test
    public void testActivate() throws IOException, InterruptedException
    {
        //---Activate the link layer---
        MessageListener listener = new MessageListener(socket);

        // activate the layer
        RemoteLinkLayerUtils.activateLink(linkUuid, socket);
        
        //---Verify the link layer was activated---
        // construct new link layer message type
        IsActivatedRequestData isActivatedRequest = IsActivatedRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.IsActivatedRequest).
                setData(isActivatedRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        LinkLayerNamespace response = (LinkLayerNamespace)listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.IsActivatedResponse, listenerTimeout);
        
        IsActivatedResponseData dataResponse = IsActivatedResponseData.parseFrom(response.getData());
        
        assertThat(dataResponse.getIsActivated(), is(true));
        
        //deactivate the layer
        RemoteLinkLayerUtils.deactivateLink(linkUuid, socket);
    }
    
    /**
     * Assuming a link layer was created in setup.
     * Deactivate the link layer and verify we receive that it is deactivated.
     */
    @Test
    public void testDeactivate() throws IOException, InterruptedException
    {
        // activate the layer
        RemoteLinkLayerUtils.activateLink(linkUuid, socket);
        
        //---Deactivate the link layer---
        RemoteLinkLayerUtils.deactivateLink(linkUuid, socket);
        
        MessageListener listener = new MessageListener(socket);

        //---Verify the link layer was deactivated---
        // construct new link layer message type
        IsActivatedRequestData isActivatedRequest = IsActivatedRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.IsActivatedRequest).
                setData(isActivatedRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        LinkLayerNamespace response = (LinkLayerNamespace)listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.IsActivatedResponse, listenerTimeout);
        
        IsActivatedResponseData dataResponse = IsActivatedResponseData.parseFrom(response.getData());
        
        assertThat(dataResponse.getIsActivated(), is(false));
    }
    
    /**
     * Assuming link layer was created in setup.
     * Verify that a LinkLayer's status can be retrieved.
     * Default value is implementation specific, just verify that the status can be retrieved.
     */
    @Test
    public void testGetStatus() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // construct new link layer message type
        GetStatusRequestData getStatusRequest = GetStatusRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.GetStatusRequest).
                setData(getStatusRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        LinkLayerNamespace response = (LinkLayerNamespace)listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.GetStatusResponse, listenerTimeout);
       
        GetStatusResponseData dataResponse = GetStatusResponseData.parseFrom(response.getData());
        
        //example link layer will always return lost
        assertThat(dataResponse.getLinkStatus(), is(CustomCommsTypes.LinkStatus.LOST));
        assertThat(dataResponse.getUuid(), is(linkUuid));
    }

    /**
     * Assuming link layer was created in setup. Make sure its status returned from performing BIT is ok.
     */
    @Test
    public void testPerformBIT() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // construct new link layer message type
        PerformBITRequestData getStatusRequest = PerformBITRequestData.newBuilder().
                setLinkUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.PerformBITRequest).
                setData(getStatusRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        LinkLayerNamespace response = (LinkLayerNamespace)listener.waitForMessage(Namespace.LinkLayer,
                LinkLayerMessageType.PerformBITResponse, listenerTimeout);
       
        PerformBITResponseData dataResponse = PerformBITResponseData.parseFrom(response.getData());
        
        //example link layer will always return ok for the perform bit status
        assertThat(dataResponse.getPerformBitStatus(), is(CustomCommsTypes.LinkStatus.OK));
        assertThat(dataResponse.getLinkUuid(), is(linkUuid));   
    }

    /**
     * Assuming link layer was created in setup. Make sure its status returned from performing BIT is ok.
     */
    @Test
    public void testPerformBITEvent() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // construct new link layer message type
        PerformBITRequestData getStatusRequest = PerformBITRequestData.newBuilder().
                setLinkUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.PerformBITRequest).
                setData(getStatusRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        RemoteEventRegistration.regRemoteEventMessages(socket, LinkLayer.TOPIC_PREFIX + "*");
        message.writeDelimitedTo(socket.getOutputStream());
        
        
        // listen for messages for a specific time interval
        EventAdminNamespace response = (EventAdminNamespace)listener.
                waitForRemoteEvent(LinkLayer.TOPIC_STATUS_CHANGED, listenerTimeout);
        
        SendEventData event = SendEventData.parseFrom(response.getData());

        //check event properties
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        for (ComplexTypesMapEntry entry : event.getPropertyList())
        {
            if (entry.getKey().equals(LinkLayer.EVENT_PROP_LINK_STATUS))
            {
                propertyMap.put(entry.getKey(), entry.getLinkLayerStatus());
            }
        }
        assertThat((CustomCommsTypes.LinkStatus)propertyMap.get(LinkLayer.EVENT_PROP_LINK_STATUS), 
                is(CustomCommsTypes.LinkStatus.OK));
    }
    
    /**
     * Verifies the ability of the system to get a link layer's capabilities remotely.
     */
    @Test
    public final void testGetCapabilities() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
    
        GetCapabilitiesRequestData linkLayerCapsRequest = GetCapabilitiesRequestData.newBuilder().
                setCommType(CommType.Linklayer).setProductType(ExampleLinkLayer.class.getName()).build();
    
        TerraHarvestMessage message= createLinkLayerMessage(CustomCommsMessageType.GetCapabilitiesRequest,
                linkLayerCapsRequest);
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        CustomCommsNamespace response = (CustomCommsNamespace)listener.waitForMessage(Namespace.CustomComms,
                CustomCommsMessageType.GetCapabilitiesResponse, 1500);
        
        //method call will only return messages of type GetCapabilitiesResponse
        GetCapabilitiesResponseData dataResponse = GetCapabilitiesResponseData.parseFrom(response.getData());
    
        //test various capabilities
        assertThat(dataResponse.getLinkCapabilities(), is(notNullValue()));
        assertThat(dataResponse.getLinkCapabilities().getBase().getDescription(), is("Example Link Layer"));
        assertThat(dataResponse.getLinkCapabilities().getBase().getManufacturer(), is("Example Manufacturer"));
        assertThat(dataResponse.getLinkCapabilities().getModality(), 
                is(CustomCommTypesGen.LinkLayerType.Enum.LINE_OF_SIGHT));
        assertThat(dataResponse.getLinkCapabilities().getStaticMtu(), is(false));
        assertThat(dataResponse.getLinkCapabilities().getPerformBITSupported(), is(true));
        assertThat(dataResponse.getLinkCapabilities().getPhysicalLinkRequired(), is(true));
        assertThat(dataResponse.getLinkCapabilities().getPhysicalLinksSupportedList().get(0),
                is(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT));
        assertThat(dataResponse.getLinkCapabilities().getBase().getProductName(), is("ExampleLinkLayer"));
    }
    
    /**
     * Helper method for creating link layer messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createLinkLayerMessage(final CustomCommsMessageType type, 
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
