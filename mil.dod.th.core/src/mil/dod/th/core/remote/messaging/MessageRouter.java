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

package mil.dod.th.core.remote.messaging;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;

/**
 * MessageRouter handles routing {@link TerraHarvestMessage}s received by the system to the appropriate service that 
 * will handle parsing the message's content. 
 * 
 * @author Dave Humeniuk
 */
@ProviderType
public interface MessageRouter
{
    /**
     * Routes the given {@link TerraHarvestMessage} to the appropriate service that will handle the message. This 
     * method can be used when the message is received through a means that is not associated with a 
     * {@link RemoteChannel}.
     *  
     * @param message
     *      message that has been received
     */
    void handleMessage(TerraHarvestMessage message);
    
    /**
     * Routes the given {@link TerraHarvestMessage} to the appropriate service that will handle the message.
     *  
     * @param message
     *      message that has been received
     * @param channel
     *      channel the message was received on or null if the message is was not received through a channel
     */
    void handleMessage(TerraHarvestMessage message, RemoteChannel channel);
}
