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
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Handles sending a given message to the desired system. Will automatically lookup remote channel given the system id.
 * Will accept a partial {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} and then construct the 
 * full {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} based on current system (source) and other 
 * parameters.
 * 
 * queue() methods allow a message to be queued for later transmission if the endpoint is not available.  If the 
 * endpoint is available, the message will be sent immediately.
 * 
 * trySend() methods do not support a response handler. If a response handler is needed for the handling of a 
 * response to a request, the queue() must be called. 
 * 
 * @author cashioka
 */
@ProviderType
public interface MessageSender
{
    /**
     * Try sending a TerraHarvestMessage to the desired destination.  Will only send the message if a channel
     * is available to the destination.
     * 
     * @param destId
     *      {@link mil.dod.th.core.system.TerraHarvestSystem} id of the destination system
     * @param payload
     *      the message payload
     * @param encryptType
     *      the encryption mode to be used
     * @return
     *      true if the message could be sent, false if the message could not because no channels are currently 
     *      available
     * @throws IllegalArgumentException
     *      if the given destination id cannot be found in the lookup
     */
    boolean trySendMessage(int destId, TerraHarvestPayload payload, EncryptType encryptType) 
            throws IllegalArgumentException;

    /**
     * Try sending a TerraHarvestMessage to the desired destination.  Will only send the message if a channel
     * is available to the destination. This method should only be called when an instance of 
     * {@link mil.dod.th.core.remote.RemoteSystemEncryption} is available. If this 
     * service is not available the message will not be able to be sent.
     * 
     * @param destId
     *      {@link mil.dod.th.core.system.TerraHarvestSystem} id of the destination system
     * @param payload
     *      the message payload
     * @return
     *      true if the message could be sent, false if the message could not because no channels are currently 
     *      available
     * @throws IllegalArgumentException
     *      if the given destination id cannot be found in the lookup
     * @throws IllegalStateException
     *      if there is no {@link mil.dod.th.core.remote.RemoteSystemEncryption} service available
     */
    boolean trySendMessage(int destId, TerraHarvestPayload payload) throws IllegalArgumentException, 
            IllegalStateException;
    
    /**
     * Queue a TerraHarvestMessage to the desired destination.  If the endpoint is not available, the message
     * will sit in a local queue and will be sent later when the endpoint becomes available.
     * 
     * @param destId
     *      {@link mil.dod.th.core.system.TerraHarvestSystem} id of the destination system
     * @param payload
     *      the message payload
     * @param encryptType
     *      the encryption mode to be used
     * @param handler
     *      the handler to be called when a response is received for the request message sent via the calling
     *      of this method, may be null if there is no response handler associated with the message
     * @return
     *      true if the message could be queued (or sent immediately), 
     *      false if not (the message will not be sent as the queue has reached a limit)
     * @throws IllegalArgumentException
     *      if the given destination id cannot be found in the lookup
     */
    boolean queueMessage(int destId, TerraHarvestPayload payload, EncryptType encryptType, ResponseHandler handler)
            throws IllegalArgumentException;
    
    /**
     * Queue a TerraHarvestMessage to the desired destination.  If the endpoint is not available, the message
     * will sit in a local queue and will be sent later when the endpoint becomes available. This method should only
     * be called when an instance of {@link mil.dod.th.core.remote.RemoteSystemEncryption} is 
     * available. If this service is not available the message will not be able to be sent.
     * 
     * @param destId
     *      {@link mil.dod.th.core.system.TerraHarvestSystem} id of the destination system
     * @param payload
     *      the message payload
     * @param handler
     *      the handler to be called when a response is received for the request message sent via the calling
     *      of this method, may be null if there is no response handler associated with the message
     * @return
     *      true if the message could be queued (or sent immediately), 
     *      false if not (the message will not be sent as the queue has reached a limit)
     * @throws IllegalArgumentException
     *      if the given destination id cannot be found in the lookup
     * @throws IllegalStateException
     *      if there is no {@link mil.dod.th.core.remote.RemoteSystemEncryption} service available
     */
    boolean queueMessage(int destId, TerraHarvestPayload payload, ResponseHandler handler) 
            throws IllegalArgumentException, IllegalStateException;
    
    /**
     * Queue a message response where the message id matches the request id and the message is sent to the source 
     * of the request. If the endpoint is not available, the message will sit in a local queue and will be sent later
     * when the endpoint becomes available.
     * 
     * @param request 
     *      request message that this response is being sent for
     * @param payload
     *      the message payload
     * @param channel
     *      request came in this channel and will be used for sending the response
     * @return 
     *      true if the response message could be queued (or sent immediately), 
     *      false if not (the message will not be sent as the queue has reached a limit)
     * @throws IllegalStateException
     *      if there is no {@link mil.dod.th.core.remote.RemoteSystemEncryption} service available     
     */
    boolean queueMessageResponse(TerraHarvestMessage request, TerraHarvestPayload payload, RemoteChannel channel)
            throws IllegalStateException;
    
    /**
     * Queue a TerraHarvestMessage to the desired destination using the specified channel.  If the endpoint is not 
     * available, the message will sit in a local queue and will be sent later when the endpoint becomes available. 
     * 
     * @param channel
     *      the {@link RemoteChannel} that will send the message
     * @param payload
     *      the payload message
     * @param encryptType
     *      the encryption mode to be used
     * @param handler
     *      the handler to be called when a response is received for the request message sent via the calling
     *      of this method, may be null if there is no response handler associated with the message
     * @return
     *      true if the response message could be queued (or sent immediately), 
     *      false if not (the message will not be sent as the queue has reached a limit)
     */
    boolean queueMessage(RemoteChannel channel, TerraHarvestPayload payload, EncryptType encryptType, 
            ResponseHandler handler);

    /**
     * Queue a TerraHarvestMessage to the desired destination using the specified channel.  If the endpoint is not 
     * available, the message will sit in a local queue and will be sent later when the endpoint becomes available. 
     * This method should only be called when an instance of {@link mil.dod.th.core.remote.RemoteSystemEncryption} is 
     * available. If this service is not available the message will not be able to be sent.
     * 
     *  @param channel
     *      the {@link RemoteChannel} that will send the message
     * @param payload
     *      the payload message
     * @param handler
     *      the handler to be called when a response is received for the request message sent via the calling
     *      of this method, may be null if there is no response handler associated with the message
     * @return
     *      true if the response message could be queued (or sent immediately), 
     *      false if not (the message will not be sent as the queue has reached a limit)
     */
    boolean queueMessage(RemoteChannel channel, TerraHarvestPayload payload, ResponseHandler handler);
    
    /**
     * Get the number of response handlers currently known to this service.
     * @return
     *     the number of registered response handlers known
     */
    int getResponseHandleRegCount();
}
