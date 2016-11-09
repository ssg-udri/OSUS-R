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
package mil.dod.th.ose.remote.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.Socket;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.ose.remote.integration.MessageMatchers.EventMessageMatcher;
import mil.dod.th.ose.remote.integration.namespace.TestCustomCommsNamespace;

/**
 * @author allenchl
 *
 */
public final class RemoteLinkLayerUtils
{
    /**
     * Timeout for LinkLayer interactions.
     */
    final static int TIME_OUT = 1200;
    
    /**
     * Hidden constructor.
     */
    private RemoteLinkLayerUtils()
    {
        
    }
    
    /**
     * Create a LinkLayer.
     * @param linkName
     *      the name to be assigned to the LinkLayer
     * @param linkClass
     *      the type of the LinkLayer that should be created
     * @param phyUuid
     *      the UUID of the PhysicalLink that will belong to the LinkLayer
     * @param socket
     *      the socket to use for communications to the controller
     * @return
     *      the UUID of the LinkLayer created
     */
    public static UUID createLinkLayer(final String linkName, final String linkClass, 
            final UUID phyUuid, final Socket socket) throws IOException, InterruptedException
    {
        UUID linkUuid = createLinkCommLayer(linkName, linkClass, phyUuid, socket).getInfo().getUuid();

        assertThat(linkUuid, is(notNullValue()));
        return linkUuid;
    }
    
    /**
     * Activate a LinkLayer.
     * @param linkUuid
     *      the UUID of the LinkLayer to activate
     * @param socket
     *      the socket to use for communications to the controller
     */
    public static void activateLink(final UUID linkUuid, final Socket socket) throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
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
        listener.waitForMessages(TIME_OUT, 
                new BasicMessageMatcher(Namespace.LinkLayer, LinkLayerMessageType.ActivateResponse),
                new EventMessageMatcher(LinkLayer.TOPIC_ACTIVATED));
    }
    
    /**
     * Deactivate a LinkLayer.
     * @param linkUuid
     *      the UUID of the LinkLayer to deactivate
     * @param socket
     *      the socket to use for communications to the controller
     */
    public static void deactivateLink(final UUID linkUuid, final Socket socket) throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //register for event so that the test does not move forward before deactivation is complete
        RemoteEventRegistration.regRemoteEventMessages(socket, LinkLayer.TOPIC_DEACTIVATED);
        
        // construct new link layer message type
        DeactivateRequestData deactivateRequest = DeactivateRequestData.newBuilder().
                setUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.DeactivateRequest).
                setData(deactivateRequest.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());

        // listen for messages for a specific time interval
        listener.waitForMessages(TIME_OUT,
                new BasicMessageMatcher(Namespace.LinkLayer, LinkLayerMessageType.DeactivateResponse),
                new EventMessageMatcher(LinkLayer.TOPIC_DEACTIVATED));
    }
    
    /**
     * Delete a LinkLayer.
     * @param linkUuid
     *      the UUID of the LinkLayer to remove
     * @param socket
     *      the socket to use for communications to the controller
     */
    public static void removeLink(final UUID linkUuid, final Socket socket) throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // construct new link layer message type
        DeleteRequestData createLLayerRequest = DeleteRequestData.newBuilder().
                setLinkLayerUuid(linkUuid).
                build();
        
        LinkLayerNamespace.Builder linkLayerMessage = LinkLayerNamespace.newBuilder().
                setType(LinkLayerMessageType.DeleteRequest).
                setData(createLLayerRequest.toByteString());

        // create terra harvest message
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.LinkLayer, 
                linkLayerMessage);
        
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());

        listener.waitForMessage(Namespace.LinkLayer, LinkLayerMessageType.DeleteResponse, TIME_OUT);
    }
    
    
    /**
     * Create a link comms layer.
     * @param linkName
     *      the name to be assigned to the LinkLayer
     * @param linkClass
     *      the type of the LinkLayer that should be created
     * @param phyUuid
     *      the UUID of the PhysicalLink that will belong to the LinkLayer
     * @param socket
     *      the socket to use for communications to the controller
     * @return
     *     the response message from creating the layer
     */
    public static CreateLinkLayerResponseData createLinkCommLayer(final String linkName, final String linkClass, 
        final UUID phyUuid, final Socket socket) throws IOException, InterruptedException
    {
        //listener
        MessageListener listener = new MessageListener(socket);
        
        //---Create a link layer associated with the physical link---
        // request data to create a new link layer
        CreateLinkLayerRequestData.Builder createLinkLayerRequestMessage = CreateLinkLayerRequestData.newBuilder().
                setLinkLayerProductType(linkClass).
                setLinkLayerName(linkName);
                
        if (phyUuid != null)
        {
            createLinkLayerRequestMessage.setPhysicalLinkUuid(phyUuid);
        }
        
        // create terra harvest message
        TerraHarvestMessage createLinkLayerMessage = TestCustomCommsNamespace.createCustomCommsMessage(
                CustomCommsMessageType.CreateLinkLayerRequest, createLinkLayerRequestMessage.build());

        // send out message
        createLinkLayerMessage.writeDelimitedTo(socket.getOutputStream());

        CustomCommsNamespace createLinkLayerResponse = (CustomCommsNamespace)listener.waitForMessage(
                Namespace.CustomComms, CustomCommsMessageType.CreateLinkLayerResponse, TIME_OUT);
        return CreateLinkLayerResponseData.parseFrom(createLinkLayerResponse.getData());
    }
}
