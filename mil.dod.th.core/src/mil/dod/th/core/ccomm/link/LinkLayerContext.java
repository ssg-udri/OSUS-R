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
package mil.dod.th.core.ccomm.link;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.factory.FactoryObjectContext;

/**
 * This is the context of the {@link LinkLayer} that is made available to implementors of {@link LinkLayerProxy}. Each 
 * instance of a {@link LinkLayer} will have a matching context to allow the plug-in to interact with the rest of the 
 * system.
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface LinkLayerContext extends LinkLayer, FactoryObjectContext
{
    /**
     * Update the current link layer status. Will post an OSGi event with the topic {@link 
     * LinkLayer#TOPIC_STATUS_CHANGED}.
     * 
     * @param status
     *      New status
     */
    void setStatus(LinkStatus status);
    
    /**
     * Post the {@link LinkLayer#TOPIC_DATA_RECEIVED} topic to the EventAdmin service.
     * 
     * @param sourceAddress
     *      The source of the received data 
     * @param destAddress
     *      The destination of the received data
     * @param frame
     *      Link frame associated with the event
     */
    void postReceiveEvent(Address sourceAddress, Address destAddress, LinkFrame frame);
}
