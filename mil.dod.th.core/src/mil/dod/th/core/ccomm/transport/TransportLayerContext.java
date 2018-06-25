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
package mil.dod.th.core.ccomm.transport;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.factory.FactoryObjectContext;

/**
 * This is the context of the {@link TransportLayer} that is made available to implementors of {@link 
 * TransportLayerProxy}. Each instance of a {@link TransportLayer} will have a matching context to allow the plug-in to 
 * interact with the rest of the system.
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface TransportLayerContext extends TransportLayer, FactoryObjectContext
{
    /**
     * Call when the plug-in starts receiving data.
     */
    void beginReceiving();
    
    /**
     * Call when the plug-in receives a complete {@link TransportPacket}. Will post the {@link 
     * TransportLayer#TOPIC_PACKET_RECEIVED} event.
     * 
     * @param pkt
     *      package received
     * @param sourceAddress
     *      Source address of the packet
     * @param destAddress
     *      Destination address of the packet
     */
    void endReceiving(TransportPacket pkt, Address sourceAddress, Address destAddress);
}
