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

package mil.dod.th.core.remote;

import java.util.Map;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;

/**
 * A remote channel is a way of sending a message to a single remote endpoint.  The method for sending depends on the 
 * implementation (socket, transport layer, etc.).
 * 
 * Each remote channel type shall register as an OSGi service that can be created using a {@link 
 * org.osgi.service.component.ComponentFactory}.
 * 
 * @author Dave Humeniuk
 */
@ProviderType
public interface RemoteChannel 
{

    /**
     * Try to send a remote message out through the channel to the endpoint.  Use message serialization method when 
     * sending out message.  This message must be thread safe to allow multiple threads calling this method at the same
     * time.
     * 
     * @param message
     *          the message to send.
     * @return
     *          true if the message was successfully sent, false if the endpoint is not available or otherwise unable to
     *          send message
     */
    boolean trySendMessage(TerraHarvestMessage message);
    
    /**
     * Send a remote message out through the channel to the endpoint if available. If the endpoint is not available, 
     * queue the message for later.  Use message serialization method when sending out message.  Messages should only be
     * queued directly with the channel using this method if wanting to send out the message through a certain channel. 
     * Otherwise, use the {@link mil.dod.th.core.remote.messaging.MessageFactory} to queue the message so the first 
     * available channel can be used to send messages to the same endpoint.
     * 
     * @param message
     *          the message to send
     * @return
     *          true if the message could be queued or sent, false if message could not be sent or queued and will never
     *          be sent
     */
    boolean queueMessage(TerraHarvestMessage message);
    
    /**
     * Check if the channel has the same properties as those passed in.
     * 
     * @param properties
     *      properties of the remote channel, properties are defined by implementations of this interface 
     * @return
     *      true if the properties match with those of the remote channel
     */
    boolean matches(Map<String, Object> properties);
    
    /**
     * Get the status of the channel. This status represents whether the channel is active, unavailable, or unknown.
     * @return
     *    the current status of the channel
     */
    ChannelStatus getStatus();
    
    /**
     * Get the type of the channel.
     * @return
     *    the type of the channel
     */
    RemoteChannelTypeEnum getChannelType();
    
    /**
     * Get the number of messages currently queued for sending.
     * 
     * @return
     *      number of messages already queued
     */
    int getQueuedMessageCount();
    
    /**
     * Get the running count of byte transmitted on this channel.  Value is only reset when channel is recreated (system
     * restart).  Does not include the octet count varint sent before each message.
     * 
     * @return
     *      number of bytes transmitted minus octet count
     */
    long getBytesTransmitted();
    
    /**
     * Get the running count of byte received on this channel.  Value is only reset when channel is recreated (system
     * restart).  Does not include the octet count varint sent before each message.
     * 
     * @return
     *      number of bytes received minus octet count
     */
    long getBytesReceived();

    /**
     * Clear the queue of messages for this channel. Any data in the queue will be permanently lost.
     */
    void clearQueuedMessages();
}
