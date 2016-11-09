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
package mil.dod.th.ose.remote.util;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

import org.osgi.service.event.Event;

/**
 * Utility class for the remote interface.
 * @author bachmakm
 *
 */
public final class RemoteInterfaceUtilities
{
    /**
     * Utility class, no need to instantiate it.
     */
    private RemoteInterfaceUtilities()
    {
        
    }
    
    /**
     * Get the remote topic for the given local topic.  Basically just appends _REMOTE to the existing topic, but allows
     * for the conversion to be done in one place.
     * 
     * @param eventTopic
     *      Local event topic to convert
     * @return
     *      Remote event topic generated from the input, properties for remote topics will be different than the local
     */
    public static String getRemoteEventTopic(final String eventTopic)
    {
        return eventTopic + RemoteConstants.REMOTE_TOPIC_SUFFIX;
    }

    /**
     * Create a {@link mil.dod.th.core.remote.RemoteConstants#TOPIC_MESSAGE_RECEIVED} event for the given remote 
     * message.
     * 
     * @param message
     *      message that was received
     * @param payload
     *      payload that was received
     * @param namespaceMessage
     *      namespace message object parsed from the given message based on the namespace of the message
     * @param messageType
     *      specific type of message, each {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace} has an enum of 
     *      message types
     * @param dataMessage
     *      data message object parsed from the given namespace message based on the message type
     * @param channel
     *      channel the message was received on
     * @return
     *      event for the message received
     * @throws InvalidProtocolBufferException
     *      unable to parse the payload 
     */
    public static Event createMessageReceivedEvent(final TerraHarvestMessage message, 
            final TerraHarvestPayload payload, final Message namespaceMessage, 
            final ProtocolMessageEnum messageType, final Message dataMessage,
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final Map<String, Object> props = createBaseMessageProps(message);
        props.put(RemoteConstants.EVENT_PROP_PAYLOAD, payload);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, payload.getNamespace().toString());
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE, namespaceMessage);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, messageType.toString());
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMessage);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_RESPONSE, message.getIsResponse());
        props.put(RemoteConstants.EVENT_PROP_CHANNEL, channel);
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Create a {@link mil.dod.th.core.remote.RemoteConstants#TOPIC_MESSAGE_SEND_UNREACHABLE_DEST} event for the given 
     * remote message.
     * 
     * @param message
     *      message that could not be sent due to an unreachable destination
     * @return
     *      event for the message that could not be sent
     */
    public static Event createMessageUnreachableSendEvent(final TerraHarvestMessage message)
    {
        final Map<String, Object> props = createBaseMessageProps(message);
        return new Event(RemoteConstants.TOPIC_MESSAGE_SEND_UNREACHABLE_DEST, props);
    }
    
    /**
     * Create a {@link mil.dod.th.core.remote.RemoteConstants#TOPIC_MESSAGE_RECEIVED_UNREACHABLE_DEST} event for the 
     * given remote message.
     * 
     * @param message
     *      message that was received but was for a destination that is not reachable from the current system
     * @return
     *      event for the message that was received
     */
    public static Event createMessageUnreachableReceivedEvent(final TerraHarvestMessage message)
    {
        final Map<String, Object> props = createBaseMessageProps(message);
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED_UNREACHABLE_DEST, props);
    }
    
    /**
     * Creates a map of basic properties for {@link mil.dod.th.core.remote.RemoteConstants} event topics.
     * 
     * @param message
     *      message to create basic set of event properties for
     * @return
     *      the map of basic event properties
     */
    private static Map<String, Object> createBaseMessageProps(final TerraHarvestMessage message)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_MESSAGE, message);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, message.getSourceId());
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, message.getDestId());
        return props;
    }
}
