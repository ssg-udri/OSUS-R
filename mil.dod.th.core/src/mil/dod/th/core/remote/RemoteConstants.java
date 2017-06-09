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

import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage.Version;

/**
 * Contains constants for the remote interface.
 * 
 * @author Dave Humeniuk
 *
 */
public final class RemoteConstants
{
    /**
     * Version of the remote interface.  Each {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} will 
     * contain this version.  See remote interface control document (ICD) for further information.
     */
    final public static Version SPEC_VERSION = Version.newBuilder().setMajor(10).setMinor(3).build(); //NOCHECKSTYLE:
                                                                               //magic number, literal version number.
    /** Event topic prefix to use for all topics in RemoteConstants. */
    final public static String TOPIC_PREFIX = "mil/dod/th/core/remote/RemoteConstants/";
    
    /**
     * Topic used when a remote message is received.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link #EVENT_PROP_MESSAGE} - {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that was 
     * received
     * <li>{@link #EVENT_PROP_SOURCE_ID} - source of message
     * <li>{@link #EVENT_PROP_DEST_ID} - destination of message
     * <li>{@link #EVENT_PROP_PAYLOAD} - includes payload containing namespace and namespace message
     * <li>{@link #EVENT_PROP_NAMESPACE} - namespace of the received message
     * <li>{@link #EVENT_PROP_NAMESPACE_MESSAGE} - namespace message within the message
     * <li>{@link #EVENT_PROP_DATA_MESSAGE} - data message within the namespace message, null if no data
     * <li>{@link #EVENT_PROP_MESSAGE_TYPE} - message type of the received message
     * <li>{@link #EVENT_PROP_MESSAGE_RESPONSE} - represents if this message is a response
     * <li>{@link #EVENT_PROP_CHANNEL} - channel the message was received on
     * </ul>
     */
    final public static String TOPIC_MESSAGE_RECEIVED = TOPIC_PREFIX + "MESSAGE_RECEIVED";
    
    /**
     * Topic used when a remote message is received, message's destination ID is different from that of the 
     * receiving system, and the receiving system does not have a way to send the message to its intended destination.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link #EVENT_PROP_MESSAGE} - {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that was 
     * received
     * <li>{@link #EVENT_PROP_SOURCE_ID} - source of message
     * <li>{@link #EVENT_PROP_DEST_ID} - destination of message
     * </ul>
     */
    final public static String TOPIC_MESSAGE_RECEIVED_UNREACHABLE_DEST = 
            TOPIC_PREFIX + "MESSAGE_RECEIVED_UNREACHABLE_DEST";
    
    /**
     * Topic used when sending a remote message and the sending system does not have a way to deliver the message to
     * its intended destination.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link #EVENT_PROP_MESSAGE} - {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that is 
     * being sent
     * <li>{@link #EVENT_PROP_SOURCE_ID} - source of message
     * <li>{@link #EVENT_PROP_DEST_ID} - destination of message
     * </ul>
     */
    final public static String TOPIC_MESSAGE_SEND_UNREACHABLE_DEST = TOPIC_PREFIX + "MESSAGE_SEND_UNREACHABLE_DEST";
    
    /**
     * Topic used when a message is received by a channel and the source ID has changed from previous messages
     * or is a new id.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link #EVENT_PROP_CHANNEL} - channel that has an updated/new system id
     * <li>{@link #EVENT_PROP_SYS_ID} - new system id
     * </ul>
     */
    final public static String TOPIC_NEW_OR_CHANGED_CHANNEL_ID = TOPIC_PREFIX + "NEW_CHANGED_ID";
    
    /**
     * Topic used when a channel has been closed and must be removed from {@link RemoteChannelLookup}.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link #EVENT_PROP_CHANNEL} - channel that should be removed
     * </ul>
     */
    final public static String TOPIC_REMOVE_CHANNEL = TOPIC_PREFIX + "REMOVE_CHANNEL";
    
    /** Event property key for the entire Terra Harvest Message itself. */
    final public static String EVENT_PROP_MESSAGE = "message";

    /** Event property key for the source id of the message. */
    final public static String EVENT_PROP_SOURCE_ID = "source.id";
    
    /** Event property key for the destination id of the message. */
    final public static String EVENT_PROP_DEST_ID = "dest.id";
    
    /** 
     * Event Property key for the payload of the message.
     * The Terra Harvest Payload is defined in the remote-base Terra Harvest Message.
     * The payload is parsed from the {@link #EVENT_PROP_MESSAGE} and contains the
     * {@link #EVENT_PROP_NAMESPACE} and {@link #EVENT_PROP_NAMESPACE_MESSAGE} values. 
     */
    final public static String EVENT_PROP_PAYLOAD = "message.payload";
    
    /** 
     * Event property key for the namespace of the message, this key is represented as the string equivalent 
     * of the enum.
     * The {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace} enumeration returned should be one of 
     * the namespaces defined in the remote-base Terra Harvest Message.
     */
    final public static String EVENT_PROP_NAMESPACE = "namespace";
    
    /** 
     * Event property key for the namespace message object. 
     * The namespace specific message is parsed from the {@link #EVENT_PROP_MESSAGE},
     * and is of the type described by {@link #EVENT_PROP_NAMESPACE}.
     */
    final public static String EVENT_PROP_NAMESPACE_MESSAGE = "namespace.message";
    
    /** 
     * Event property key for the type of the inner message, this key is represented as the string equivalent 
     * of the enum. 
     * Expected message types are defined by the {@link #EVENT_PROP_NAMESPACE} from which this message originated.
     */
    final public static String EVENT_PROP_MESSAGE_TYPE = "message.type";
    
    /** 
     * Event property key for the data message object.
     * The data message is the message parsed from the namespace message.
     * The {@link #EVENT_PROP_MESSAGE_TYPE} can be used to get the type of this message.
     */
    final public static String EVENT_PROP_DATA_MESSAGE = "message.data";
    
    /** 
     * Event property key that represents if the message is a response. 
     * The property will return a boolean value, true represents the message
     * represented by the event is a response message.
     */
    final public static String EVENT_PROP_MESSAGE_RESPONSE = "message.response";
    
    /** Event property key for a channel. */
    final public static String EVENT_PROP_CHANNEL = "channel";
    
    /** 
     * Event property key for the system id associated with the socket.
     * Should be a valid controller(system) id.
     */
    final public static String EVENT_PROP_SYS_ID = "system.id";
    
    /**
     * Event property key to denote whether an event is a remote event received from a remote client.  
     * If set at all, then event came from a remote interface and will be used to filter remote events. 
     */
    final public static String REMOTE_EVENT_PROP = "remote";
      
    /**
     * Suffix used to make an event topic remote.
     * An event with this suffix will also contain
     * <ul>
     * <li>{@link #REMOTE_EVENT_PROP_CONTROLLER_ID} - the controller ID where the event if from
     * </ul>
     */
    final public static String REMOTE_TOPIC_SUFFIX = "_REMOTE";

    /**
     * Event property key for the controller id associated with a remote event.
     */
    final public static String REMOTE_EVENT_PROP_CONTROLLER_ID = "controller.id";

    /**
     * Defined to prevent instantiation.
     */
    private RemoteConstants()
    {
        
    }
    
}
