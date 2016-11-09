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
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;

/**
 * MessageWrapper wraps a partial {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} with a reference
 * to {@link mil.dod.th.core.remote.messaging.MessageSender} and manages the transportation of the message.
 * 
 * The queue() method allows a message to be queued for later transmission if the endpoint is not available.  
 * If the endpoint is available, the message will be sent immediately.
 * 
 * The trySend() method does not support a response handler. If a response handler is needed for the handling 
 * of a response to a request, the queue() must be called. 
 * 
 * @author cashioka
 *
 */
@ProviderType
public interface MessageWrapper extends MessageWrapperAutoEncryption
{ 

    /**
     * Try sending the wrapped message to the desired destination.  Will only send the message if a channel
     * is available to the destination.
     * 
     * @param destId
     *      {@link mil.dod.th.core.system.TerraHarvestSystem} id of the destination system
     * @param encryptType
     *      the encryption mode to be used
     * @return
     *      true if the message could be sent, false if the message could not because no channels are currently 
     *      available
     * @throws IllegalArgumentException
     *      if the given destination id cannot be found in the lookup
     */
    boolean trySend(int destId, EncryptType encryptType) 
            throws IllegalArgumentException; 
    
    /**
     * Queue the wrapped message to the desired destination.  If the endpoint is not available, the message
     * will sit in a local queue and will be sent later when the endpoint becomes available.
     * 
     * @param destId
     *      {@link mil.dod.th.core.system.TerraHarvestSystem} id of the destination system
     * @param encryptType
     *      the encryption mode to be used
     * @param handler
     *      the handler to be called when a response is received for the request message sent via the calling
     *      of this method
     * @return
     *      true if the message could be queued (or sent immediately), 
     *      false if not (the message will not be sent as the queue has reached a limit)
     * @throws IllegalArgumentException
     *      if the given destination id cannot be found in the lookup
     */
    boolean queue(int destId, EncryptType encryptType, ResponseHandler handler) 
            throws IllegalArgumentException;

    /**
     * Queue the wrapped message to the desired destination.  If the endpoint is not available, the message
     * will sit in a local queue and will be sent later when the endpoint becomes available. This method should only
     * be called when an instance of {@link mil.dod.th.core.remote.RemoteSystemEncryption} is 
     * available. If this service is not available the message will not be able to be sent.
     * 
     * @param channel
     *      {@link RemoteChannel} the message is to be sent over
     * @param encryptType
     *      the encryption mode to be used
     * @param handler
     *      the handler to be called when a response is received for the request message sent via the calling
     *      of this method
     * @return
     *      true if the message could be queued (or sent immediately), 
     *      false if not (the message will not be sent as the queue has reached a limit)
     */
    boolean queue(RemoteChannel channel, EncryptType encryptType, ResponseHandler handler);
} 
