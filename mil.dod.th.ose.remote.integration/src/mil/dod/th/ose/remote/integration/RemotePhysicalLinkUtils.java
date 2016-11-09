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

import org.osgi.service.log.LogService;

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace.PhysicalLinkMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

/**
 * @author allenchl
 *
 */
public final class RemotePhysicalLinkUtils
{
    /**
     * Timeout for phys interactions.
     */
    final static int TIME_OUT = 800;
    
    /**
     * Hidden constructor.
     */
    private RemotePhysicalLinkUtils()
    {
        
    }
    
    /**
     * Create a PhysicalLink.
     * @param type
     *      type to create
     * @param physName
     *      the name to be assigned to the PhysicalLink
     * @param socket
     *      the socket to use for communications to the controller
     */
    public static UUID createPhysicalLink(final CustomCommTypesGen.PhysicalLinkType.Enum type, final String physName, 
            final Socket socket) throws IOException
    {
        
        UUID linkUuid = createPhysicalCommLayer(type, physName, socket).getInfo().getUuid();
        
        assertThat(linkUuid, is(notNullValue()));
        return linkUuid;
    }
    
    /**
     * Try to remove a PhysicalLink.
     * @param physUuid
     *      the UUID of the physical link to remove or null if no UUID (will not try to remove in this case)
     */
    public static void tryRemovePhysicalLink(final UUID physUuid, final Socket socket)
    {
        if (physUuid != null)
        {
            try
            {
                removePhysicalLink(physUuid, socket);
            }
            catch (Exception e)
            {
                Logging.log(LogService.LOG_ERROR, e, "Unable to remove physical link with UUID=%s", physUuid);
            }
        }
    }
    
    /**
     * Remove a PhysicalLink.
     * @param physUuid
     *      the UUID of the physical link to remove
     */
    public static void removePhysicalLink(final UUID physUuid, final Socket socket) throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        // request data to remove the created physical link
        DeleteRequestData request = DeleteRequestData.newBuilder().
                setPhysicalLinkUuid(physUuid).
                build();
        
        PhysicalLinkNamespace.Builder physMessage = PhysicalLinkNamespace.
                newBuilder().
                setType(PhysicalLinkMessageType.DeleteRequest).setData(request.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.PhysicalLink, 
                physMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        listener.waitForMessage(Namespace.PhysicalLink, PhysicalLinkMessageType.DeleteResponse, TIME_OUT);
    }
      
    /**
     * Create a physical link comms layer.
     * @param type
     *     type of physical link to create
     * @param name
     *     the name to set for the layer
     * @param socket
     *     the socket to use for the request
     * @return
     *     the response message from creating the layer
     */
    public static CreatePhysicalLinkResponseData createPhysicalCommLayer(
            final CustomCommTypesGen.PhysicalLinkType.Enum type, 
            final String name, final Socket socket) throws IOException
    {
        //listener
        MessageListener listener = new MessageListener(socket);
        
        // request data to create a new physical link
        CreatePhysicalLinkRequestData request = CreatePhysicalLinkRequestData.newBuilder().
                setPhysicalLinkType(type).
                setPhysicalLinkName(name).
                build();
        
        CustomCommsNamespace.Builder customCommsMessage = CustomCommsNamespace.newBuilder().
                setType(CustomCommsMessageType.CreatePhysicalLinkRequest).
                setData(request.toByteString());
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.CustomComms, customCommsMessage);
        
        message.writeDelimitedTo(socket.getOutputStream());
        
        // listen for messages for a specific time interval        
        CustomCommsNamespace namespaceResponse = (CustomCommsNamespace)listener.waitForMessage(Namespace.CustomComms, 
                        CustomCommsMessageType.CreatePhysicalLinkResponse, TIME_OUT);
        return CreatePhysicalLinkResponseData.parseFrom(namespaceResponse.getData());
    }
}
