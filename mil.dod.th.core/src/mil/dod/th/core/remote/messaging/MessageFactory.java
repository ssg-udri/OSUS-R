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

import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace.DataStreamServiceMessageType;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace.DataStreamStoreMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace.MetaTypeMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace.PhysicalLinkMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace.
    RemoteChannelLookupMessageType;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;

/**
 * MessageFactory handles the creation of partial {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage}s
 * for all namespaces. The constructed messages are wrapped in {@link mil.dod.th.core.remote.messaging.MessageWrapper}
 * or {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} instances. The partially constructed messages
 * are not encrypted.
 * 
 * @author cashioka
 *
 */
@ProviderType
public interface MessageFactory
{
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#Base} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the Base namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createBaseMessage(final BaseMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#Base} namespace and is not encrypted. 
     * 
     * @param request
     *     request message that this error response is being sent for
     * @param errorCode
     *     represents the occurrence of a generic error defined by the TerraHarvestMessage ErrorCode enum type
     * @param errorDescription
     *     field can optionally describe the TerraHarvestMessage error in more detail
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createBaseErrorMessage(final TerraHarvestMessage request, final ErrorCode errorCode,
            final String errorDescription);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#Base} namespace and is not encrypted. 
     * 
     * NOTE: The error will be logged as well, no need to log message.
     * 
     * Error code and error description fields will be filled out based on the exception.
     * 
     * @param request
     *     request message that this error response is being sent for
     * @param exception
     *     the exception that occurred
     * @param errorDescription
     *      description to go with the error response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createBaseErrorResponse(final TerraHarvestMessage request, final Exception exception, 
            final String errorDescription);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#Base} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the Base namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createBaseMessageResponse(final TerraHarvestMessage request, 
         final BaseMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#MissionProgramming} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the MissionProgramming namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createMissionProgrammingMessage(final MissionProgrammingMessageType messageType, 
        final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#MissionProgramming} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the MissionProgramming namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createMissionProgrammingResponseMessage(final TerraHarvestMessage request, 
            final MissionProgrammingMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#EventAdmin} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the EventAdmin namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createEventAdminMessage(final EventAdminMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#EventAdmin} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the EventAdmin namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createEventAdminResponseMessage(final TerraHarvestMessage request, 
            final EventAdminMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#ConfigAdmin} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the ConfigAdmin namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createConfigAdminMessage(final ConfigAdminMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#ConfigAdmin} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the ConfigAdmin namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createConfigAdminResponseMessage(final TerraHarvestMessage request, 
            final ConfigAdminMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#MetaType} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the MetaType namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createMetaTypeMessage(final MetaTypeMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#MetaType} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the MetaType namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createMetaTypeResponseMessage(final TerraHarvestMessage request, 
            final MetaTypeMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#Asset} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the Asset namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createAssetMessage(final AssetMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#Asset} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the Asset namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createAssetResponseMessage(final TerraHarvestMessage request, 
            final AssetMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#AssetDirectoryService} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the AssetDirectoryService namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createAssetDirectoryServiceMessage(final AssetDirectoryServiceMessageType messageType, 
        final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#AssetDirectoryService} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the AssetDirectoryService namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createAssetDirectoryServiceResponseMessage(final TerraHarvestMessage request, 
            final AssetDirectoryServiceMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#Bundle} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the Bundle namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createBundleMessage(final BundleMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#Bundle} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the Bundle namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createBundleResponseMessage(final TerraHarvestMessage request, 
            final BundleMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#CustomComms} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the CustomComms namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createCustomCommsMessage(final CustomCommsMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#CustomComms} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the CustomComms namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createCustomCommsResponseMessage(final TerraHarvestMessage request, 
            final CustomCommsMessageType messageType, final Message dataMessage);
    
    
    /**
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#DataStreamService} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the DataStreamService namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */
    MessageWrapper createDataStreamServiceMessage(final DataStreamServiceMessageType messageType, 
            final Message dataMessage);
    
    /**
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#DataStreamService} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the DataStreamService namespace response message
     * @param dataMessage
     *      message containing the data of the response message; null if no data associated with response
     * @return
     *      a wrapped partial TerraHarvestMessage
     */
    MessageResponseWrapper createDataStreamServiceResponseMessage(final TerraHarvestMessage request, 
            final DataStreamServiceMessageType messageType, final Message dataMessage);
    
    /**
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#DataStreamStore} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the DataStreamStore namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */
    MessageWrapper createDataStreamStoreMessage(final DataStreamStoreMessageType messageType,
            final Message dataMessage);
    
    /**
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#DataStreamStore} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the DataStreamStore namespace response message
     * @param dataMessage
     *      message containing the data of the response message; null if no data associated with response
     * @return
     *      a wrapped partial TerraHarvestMessage
     */
    MessageResponseWrapper createDataStreamStoreResponseMessage(final TerraHarvestMessage request,
            final DataStreamStoreMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#PhysicalLink} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the PhysicalLink namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createPhysicalLinkMessage(final PhysicalLinkMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#PhysicalLink} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the PhysicalLink namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createPhysicalLinkResponseMessage(final TerraHarvestMessage request, 
            final PhysicalLinkMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#LinkLayer} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the LinkLayer namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createLinkLayerMessage(final LinkLayerMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#LinkLayer} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the LinkLayer namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createLinkLayerResponseMessage(final TerraHarvestMessage request, 
            final LinkLayerMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#TransportLayer} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the TransportLayer namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createTransportLayerMessage(final TransportLayerMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#TransportLayer} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the TransportLayer namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createTransportLayerResponseMessage(final TerraHarvestMessage request, 
            final TransportLayerMessageType messageType, final Message dataMessage);

    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#ObservationStore} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the ObservationStore namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createObservationStoreMessage(final ObservationStoreMessageType messageType, 
        final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#ObservationStore} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the ObservationStore namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createObservationStoreResponseMessage(final TerraHarvestMessage request, 
            final ObservationStoreMessageType messageType, final Message dataMessage);
   
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#RemoteChannelLookup} namespace and is not encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the RemoteChannelLookup namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageWrapper createRemoteChannelLookupMessage(final RemoteChannelLookupMessageType messageType, 
        final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#RemoteChannelLookup} namespace and is not encrypted. 
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the RemoteChannelLookup namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */ 
    MessageResponseWrapper createRemoteChannelLookupResponseMessage(final TerraHarvestMessage request, 
            final RemoteChannelLookupMessageType messageType, final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#EncryptionInfo} namespace and should never be encrypted.
     *
     * @param messageType 
     *      the type of message, expected to be of a type from the EncryptInfo namespace 
     * @param dataMessage 
     *      the message data, the namespace message will be constructed from this and the message type 
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */
    MessageWrapperAutoEncryption createEncryptionInfoMessage(final EncryptionInfoMessageType messageType, 
            final Message dataMessage);
    
    /** 
     * Create a {@link mil.dod.th.core.remote.messaging.MessageResponseWrapper} containing a partial 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage} that can send the message through the 
     * {@link mil.dod.th.core.remote.messaging.MessageSender}. The wrapped message is from the 
     * {@link mil.dod.th.core.remote.proto.RemoteBase.Namespace#EncryptionInfo} namespace and should never be encrypted.
     * 
     * @param request
     *      request message that this response is being sent for
     * @param messageType
     *      type of the EncryptInfo namespace response message
     * @param dataMessage
     *      message containing the data of the response message, null if no data associated with response
     * @return 
     *      a wrapped partial TerraHarvestMessage
     */
    MessageResponseWrapper createEncryptionInfoResponseMessage(final TerraHarvestMessage request,
            final EncryptionInfoMessageType messageType, final Message dataMessage);
}
