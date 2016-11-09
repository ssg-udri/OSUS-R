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

/**
 * MessageResponseWrapper wraps a partial {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} 
 * with a reference to {@link mil.dod.th.core.remote.messaging.MessageSender} and manages the transportation of
 * the message. This interface is used only for response messages.
 * 
 * The queue() method allows a message to be queued for later transmission if the endpoint is not available.  
 * If the endpoint is available, the message will be sent immediately.
 * 
 * @author cashioka
 *
 */
@ProviderType
public interface MessageResponseWrapper 
{ 
    /**
     * Queue the wrapped message where the message id matches the request id and the message is sent to the source 
     * of the request. If the endpoint is not available, the message will sit in a local queue and will be sent later
     * when the endpoint becomes available.
     * 
     * @param channel
     *      request came in this channel and will be used for sending the response
     * @return 
     *      true if the response message could be queued (or sent immediately), 
     *      false if not (the message will not be sent as the queue has reached a limit)
     */
    boolean queue(RemoteChannel channel); 
} 
