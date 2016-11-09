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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

import example.ccomms.ExampleLinkLayer;
import example.ccomms.ExamplePhysicalLink;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsInUseRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsInUseResponseData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsOpenRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsOpenResponseData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace.PhysicalLinkMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.ose.remote.integration.CustomCommsNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.RemoteLinkLayerUtils;
import mil.dod.th.ose.remote.integration.RemotePhysicalLinkUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

/**
 * Tests the interaction of the remote interface with the {@link PhysicalLinkNamespace}.  Specifically, 
 * the class tests that PhysicalLink messages are properly sent and that appropriate responses are received.
 * @author matt
 */
public class TestPhysicalLinkNamespace
{
    private SharedMessages.UUID testUuid;
    
    private Socket socket;
    
    private int listenerTimeout = 600;
    
    /**
     * Setup and connect the socket to the controller.
     * Create a physical link for use with unit tests that need one. The physical link will be removed in the tear down
     * method.
     */
    @Before
    public void setUp() throws IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
        
        testUuid = RemotePhysicalLinkUtils.createPhysicalLink(
                CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, "rawr", socket);
    }
    
    /**
     * Assuming a link layer was created and activated that is associated with the physical link created in setup.
     * Deactivate and remove the link layer. Remove the physical link and verify it is removed.
     */
    @After
    public void tearDown() throws IOException, InterruptedException
    {
        try
        {
            RemotePhysicalLinkUtils.tryRemovePhysicalLink(testUuid, socket);
            // verify physical link is gone
            List<SharedMessages.UUID> uuidList = CustomCommsNamespaceUtils.getLayerUuidsByType(socket, 
                    CommType.PhysicalLink);
            assertThat(uuidList.contains(testUuid), is(false));
            MessageListener.unregisterEvent(socket);
        }
        finally
        {
            socket.close();
        }
    }
    
    /**
     * Verify the default physical link isOpen property can be received. Create a link layer associated with the 
     * physical link, activate the link layer and verify that the physical link is now open. Deactivate and remove 
     * the link layer.
     */
    @Test
    public void testIsOpen() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //---Verify physical link is not open---
        // request data see if physical link is open
        IsOpenRequestData requestMessage = IsOpenRequestData.newBuilder()
                .setUuid(testUuid)
                .build();
        
        PhysicalLinkNamespace.Builder pLinkMessage = PhysicalLinkNamespace.newBuilder().
                setType(PhysicalLinkMessageType.IsOpenRequest).
                setData(requestMessage.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(
                Namespace.PhysicalLink, pLinkMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        PhysicalLinkNamespace response = (PhysicalLinkNamespace)listener.waitForMessage(Namespace.PhysicalLink,
                PhysicalLinkMessageType.IsOpenResponse, listenerTimeout);

        IsOpenResponseData dataResponse = IsOpenResponseData.parseFrom(response.getData());
        
        assertThat(dataResponse.getIsOpen(), is(false));
        
        //---Create a link layer associated with the physical link---
        SharedMessages.UUID linkUuid = RemoteLinkLayerUtils.createLinkLayer(
              "yarrr", ExampleLinkLayer.class.getName(), testUuid, socket);
        
        //---Activate link layer---
        RemoteLinkLayerUtils.activateLink(linkUuid, socket);
        
        //---Verify physical link was opened---
        // request data see if physical link is open
        requestMessage = IsOpenRequestData.newBuilder()
                .setUuid(testUuid)
                .build();
        
        pLinkMessage = PhysicalLinkNamespace.newBuilder().
                setType(PhysicalLinkMessageType.IsOpenRequest).
                setData(requestMessage.toByteString());
        
        message = TerraHarvestMessageHelper.createTerraHarvestMsg(
                Namespace.PhysicalLink, pLinkMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        response = (PhysicalLinkNamespace)listener.waitForMessage(Namespace.PhysicalLink, 
                PhysicalLinkMessageType.IsOpenResponse, listenerTimeout);

        dataResponse = IsOpenResponseData.parseFrom(response.getData());
        
        assertThat(dataResponse.getIsOpen(), is(true));
        
        //---Deactivate the link layer---
        RemoteLinkLayerUtils.deactivateLink(linkUuid, socket);

        //---Remove link layer---
        RemoteLinkLayerUtils.removeLink(linkUuid, socket);

        // verify link layer is gone
        List<UUID> linkLayers = CustomCommsNamespaceUtils.getLayerUuidsByType(socket, CommType.Linklayer);
        assertThat(linkLayers.contains(linkUuid), is(false));
    }
    
    /**
     * Verifies the ability of the system to get a physical link's capabilities remotely.
     */
    @Test
    public final void testGetCapabilities() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
    
        GetCapabilitiesRequestData physicalLinkCapsRequest = GetCapabilitiesRequestData.newBuilder().
                setCommType(CommType.PhysicalLink).setProductType(ExamplePhysicalLink.class.getName()).build();
    
        TerraHarvestMessage message= createCustomCommsMessage(CustomCommsMessageType.GetCapabilitiesRequest, 
                physicalLinkCapsRequest);
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for messages for a specific time interval
        CustomCommsNamespace response = (CustomCommsNamespace)listener.waitForMessage(Namespace.CustomComms,
                CustomCommsMessageType.GetCapabilitiesResponse, 1000);

        GetCapabilitiesResponseData dataResponse = GetCapabilitiesResponseData.parseFrom(response.getData());
    
        //test various capabilities
        assertThat(dataResponse.getPhysicalCapabilities(), is(notNullValue()));
        assertThat(dataResponse.getPhysicalCapabilities().getBase().getDescription(), is("An example physical link"));
        assertThat(dataResponse.getPhysicalCapabilities().getLinkType(), 
                is(CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C));
        assertThat(dataResponse.getPhysicalCapabilities().getBase().getManufacturer(), is("ExampleManufacturer"));
        assertThat(dataResponse.getPhysicalCapabilities().getBase().getProductName(), is("ExamplePhysicalLink"));
    }
    
    /**
     * Verify that the system can remotely see if a physical link is in use or not. Creating a new physical link, then
     * verifying that it is not in use, then we create a link layer that is associated with the physical link, then 
     * verify that the physical link is now in use.. then remove both newly created layers.
     */
    @Test
    public void testIsPhysicalLinkInUse() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);

        // request data to see if a physical link is in use
        IsInUseRequestData requestMessage = IsInUseRequestData.newBuilder().
                setPhysicalLinkUuid(testUuid).
                build();
        
        // create terra harvest message
        TerraHarvestMessage message = createPhysicalLinkMessage(PhysicalLinkMessageType.IsInUseRequest,
                requestMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        PhysicalLinkNamespace isInUseResponse = (PhysicalLinkNamespace)listener.waitForMessage(Namespace.PhysicalLink,
                PhysicalLinkMessageType.IsInUseResponse, 600);
        IsInUseResponseData dataResponse = IsInUseResponseData.parseFrom(
                isInUseResponse.getData());
        
        assertThat(dataResponse.getIsInUse(), is(false));
        
        //---Create a new link layer and verify physical link becomes in use---
        // request data to create a new link layer
        SharedMessages.UUID localLinkLayerUuid = RemoteLinkLayerUtils.createLinkLayer(
            "water", ExampleLinkLayer.class.getName(), testUuid, socket);
        
        //---Verify the physical link is now in use---
        // request data to see if a physical link is in use
        requestMessage = IsInUseRequestData.newBuilder().
                setPhysicalLinkUuid(testUuid).
                build();
        
        // create terra harvest message
        message = createPhysicalLinkMessage(PhysicalLinkMessageType.IsInUseRequest, 
                requestMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval
        isInUseResponse = (PhysicalLinkNamespace)listener.waitForMessage(Namespace.PhysicalLink, 
                PhysicalLinkMessageType.IsInUseResponse, 600);
        dataResponse = IsInUseResponseData.parseFrom(isInUseResponse.getData());
        
        assertThat(dataResponse.getIsInUse(), is(true));
        
        //---Remove newly created link layer and physical link---
        // request data to remove the newly created link layer
        RemoteLinkLayerUtils.removeLink(localLinkLayerUuid, socket);
        
        // verify newly created link layer is gone
        List<SharedMessages.UUID> uuidList = CustomCommsNamespaceUtils.getLayerUuidsByType(socket, CommType.Linklayer);
        assertThat(uuidList.contains(localLinkLayerUuid), is(false));
    }
    
    /**
     * Helper method for creating Custom comms messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createCustomCommsMessage(final CustomCommsMessageType type, 
            final Message message)
    {
        CustomCommsNamespace.Builder commsMessageBuilder = CustomCommsNamespace.newBuilder().
                setType(type).
                setData(message.toByteString());

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.CustomComms, commsMessageBuilder);
        return thMessage;
    }
    
    /**
     * Helper method for creating physical link messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createPhysicalLinkMessage(final PhysicalLinkMessageType type, 
            final Message message)
    {
        PhysicalLinkNamespace.Builder physNamespaceMessage = PhysicalLinkNamespace.newBuilder().
                setType(type).
                setData(message.toByteString());

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.PhysicalLink, physNamespaceMessage);
        return thMessage;
    }
}
