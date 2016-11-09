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
package mil.dod.th.ose.remote.comms;

import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.shared.SharedMessageUtils;

/**
 * Custom comms utility class for various operations on the custom comms service.
 * @author matt
 */
public final class CustomCommUtility
{
    
    /**
     * Constructor is declared to prevent instantiation of the class.
     */
    private CustomCommUtility()
    {
        
    }
    
    /**
     * Get a {@link mil.dod.th.core.ccomm.link.LinkLayer} from the {@link mil.dod.th.core.ccomm.CustomCommsService}.
     * @param ccommService
     *      custom comms service to find the link layer from
     * @param uuid
     *      protocol buffer UUID of the link layer to find
     * @return
     *      link layer that was found in the service
     * @throws IllegalArgumentException
     *      if link layer cannot be found with given UUID
     */
    public static LinkLayer getLinkLayerByUuid(final CustomCommsService ccommService, final UUID uuid) throws 
            IllegalArgumentException
    {
        final java.util.UUID linkUuid = SharedMessageUtils.convertProtoUUIDtoUUID(uuid);
        
        for (LinkLayer linkLayer : ccommService.getLinkLayers())
        {
            if (linkLayer.getUuid().equals(linkUuid))
            {
                return linkLayer;
            }
        }
        throw new IllegalArgumentException(String.format("Cannot find the Link Layer with uuid: [%s] ", linkUuid));
    }
    
    /**
     * Get a {@link mil.dod.th.core.ccomm.transport.TransportLayer} from the 
     * {@link mil.dod.th.core.ccomm.CustomCommsService}.
     * @param ccommService
     *      custom comms service to find the transport layer from
     * @param uuid
     *      protocol buffer UUID of the transport layer to find
     * @return
     *      transport layer that was found in the service
     * @throws IllegalArgumentException
     *      if transport layer cannot be found with given UUID
     */
    public static TransportLayer getTransportLayerByUuid(final CustomCommsService ccommService, final UUID uuid) throws
            IllegalArgumentException
    {
        final java.util.UUID transportUuid = SharedMessageUtils.convertProtoUUIDtoUUID(uuid);
        
        for (TransportLayer transport : ccommService.getTransportLayers())
        {
            if (transport.getUuid().equals(transportUuid))
            {
                return transport;
            }
        }
        throw new IllegalArgumentException(String.format("Cannot find the Transport Layer with uuid: [%s]", 
                transportUuid));
    }
}
