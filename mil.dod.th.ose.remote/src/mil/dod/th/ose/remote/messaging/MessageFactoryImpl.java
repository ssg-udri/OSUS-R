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
package mil.dod.th.ose.remote.messaging;

import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.messaging.MessageSender;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.messaging.MessageWrapperAutoEncryption;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace.DataStreamServiceMessageType;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace.DataStreamStoreMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace.MetaTypeMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace.PhysicalLinkMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace.
    RemoteChannelLookupMessageType;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;

/**
 * Implementation of {@link MessageFactory}.
 * @author allenchl
 *
 */
@Component //NOCHECKSTYLE: High class fan-out, need to handle requests/responses for every namespace
public class MessageFactoryImpl implements MessageFactory
{
    /**
     * Maps exception classes to error codes.
     */
    final static ImmutableMap<Class<? extends Exception>, ErrorCode> EXCEPTION_MAP = 
            ImmutableMap.<Class<? extends Exception>, ErrorCode>builder()
                .put(AssetException.class, ErrorCode.ASSET_ERROR)
                .put(CCommException.class, ErrorCode.CCOMM_ERROR)
                .put(ObjectConverterException.class, ErrorCode.CONVERTER_ERROR)
                .put(IllegalStateException.class, ErrorCode.ILLEGAL_STATE)
                .put(IllegalArgumentException.class, ErrorCode.INVALID_VALUE)
                .put(MarshalException.class, ErrorCode.JAXB_ERROR)
                .put(UnmarshalException.class, ErrorCode.JAXB_ERROR)
                .put(PersistenceFailedException.class, ErrorCode.PERSIST_ERROR)
                .build();
    
   /**
     * Service used to send {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage}s.
     */
    private MessageSender m_MessageSender;

    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Bind the service used to send messages.
     * @param messageSender
     *     the service to use
     */
    @Reference
    public void setMessageSender(final MessageSender messageSender)
    {
        m_MessageSender = messageSender;
    }
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    @Override
    public MessageWrapper createBaseMessage(final BaseMessageType messageType, final Message dataMessage)
    {
        final BaseNamespace.Builder namespaceMessage = BaseNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.Base, namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createBaseErrorMessage(final TerraHarvestMessage request, final ErrorCode errorCode,
            final String errorDescription)
    {
        final GenericErrorResponseData error = GenericErrorResponseData.newBuilder().
                setError(errorCode).
                setErrorDescription(errorDescription).build();
        final BaseNamespace namespaceMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GenericErrorResponse).
                setData(error.toByteString()).build();
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.Base, namespaceMessage), 
                m_MessageSender);
    }
    
    @Override
    public MessageResponseWrapper createBaseErrorResponse(final TerraHarvestMessage request, final Exception exception, 
            final String errorDescription)
    {
        // try to get error code from exception type, default to internal error 
        final ErrorCode errorCode = 
                Objects.firstNonNull(EXCEPTION_MAP.get(exception.getClass()), ErrorCode.INTERNAL_ERROR);
        
        final String delimeter = ": ";
        final StringBuilder errorMsgBuilder = new StringBuilder();
        errorMsgBuilder.append(errorDescription).append(delimeter);
        if (errorCode == ErrorCode.INTERNAL_ERROR)
        {
            errorMsgBuilder.append(exception.getClass().getName()).append(delimeter);
        }
        errorMsgBuilder.append(exception.getMessage());
        
        m_Logging.error(exception, errorDescription);
        
        final GenericErrorResponseData errorData = GenericErrorResponseData.newBuilder().
                setError(errorCode).
                setErrorDescription(errorMsgBuilder.toString()).build();
        final BaseNamespace namespaceMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GenericErrorResponse).
                setData(errorData.toByteString()).build();
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.Base, namespaceMessage), 
                m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createBaseMessageResponse(final TerraHarvestMessage request, 
            final BaseMessageType messageType, final Message dataMessage)
    {
        final BaseNamespace.Builder namespaceMessage = BaseNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.Base, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageWrapper createMissionProgrammingMessage(final MissionProgrammingMessageType messageType,
            final Message dataMessage)
    {
        final MissionProgrammingNamespace.Builder namespaceMessage = MissionProgrammingNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.MissionProgramming, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createMissionProgrammingResponseMessage(final TerraHarvestMessage request,
            final MissionProgrammingMessageType messageType, final Message dataMessage)
    {
        final MissionProgrammingNamespace.Builder namespaceMessage = MissionProgrammingNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.MissionProgramming, 
                namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageWrapper createEventAdminMessage(final EventAdminMessageType messageType, final Message dataMessage)
    {
        final EventAdminNamespace.Builder namespaceMessage = EventAdminNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.EventAdmin, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createEventAdminResponseMessage(final TerraHarvestMessage request,
            final EventAdminMessageType messageType, final Message dataMessage)
    {
        final EventAdminNamespace.Builder namespaceMessage = EventAdminNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.EventAdmin, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageWrapper createConfigAdminMessage(final ConfigAdminMessageType messageType, 
            final Message dataMessage)
    {
        final ConfigAdminNamespace.Builder namespaceMessage = ConfigAdminNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.ConfigAdmin, namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createConfigAdminResponseMessage(final TerraHarvestMessage request,
            final ConfigAdminMessageType messageType, final Message dataMessage)
    {
        final ConfigAdminNamespace.Builder namespaceMessage = ConfigAdminNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.ConfigAdmin, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageWrapper createMetaTypeMessage(final MetaTypeMessageType messageType, final Message dataMessage)
    {
        final MetaTypeNamespace.Builder namespaceMessage = MetaTypeNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.MetaType, namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createMetaTypeResponseMessage(final TerraHarvestMessage request,
            final MetaTypeMessageType messageType, final Message dataMessage)
    {
        final MetaTypeNamespace.Builder namespaceMessage = MetaTypeNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.MetaType, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageWrapper createAssetMessage(final AssetMessageType messageType, final Message dataMessage)
    {
        final AssetNamespace.Builder namespaceMessage = AssetNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.Asset, namespaceMessage.build()),  m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createAssetResponseMessage(final TerraHarvestMessage request, 
            final AssetMessageType messageType, final Message dataMessage)
    {
        final AssetNamespace.Builder namespaceMessage = AssetNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.Asset, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageWrapper createAssetDirectoryServiceMessage(final AssetDirectoryServiceMessageType messageType,
            final Message dataMessage)
    {
        final AssetDirectoryServiceNamespace.Builder namespaceMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.AssetDirectoryService, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createAssetDirectoryServiceResponseMessage(final TerraHarvestMessage request,
            final AssetDirectoryServiceMessageType messageType, final Message dataMessage)
    {
        final AssetDirectoryServiceNamespace.Builder namespaceMessage = AssetDirectoryServiceNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, 
                createPayload(Namespace.AssetDirectoryService, namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageWrapper createBundleMessage(final BundleMessageType messageType, final Message dataMessage)
    {
        final BundleNamespace.Builder namespaceMessage = BundleNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.Bundle, namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createBundleResponseMessage(final TerraHarvestMessage request,
            final BundleMessageType messageType, final Message dataMessage)
    {
        final BundleNamespace.Builder namespaceMessage = BundleNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.Bundle, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageWrapper createCustomCommsMessage(final CustomCommsMessageType messageType, final Message dataMessage)
    {
        final CustomCommsNamespace.Builder namespaceMessage = CustomCommsNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.CustomComms, namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createCustomCommsResponseMessage(final TerraHarvestMessage request,
            final CustomCommsMessageType messageType, final Message dataMessage)
    {
        final CustomCommsNamespace.Builder namespaceMessage = CustomCommsNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.CustomComms, namespaceMessage.build()), 
                m_MessageSender);
    }
    
    @Override
    public MessageWrapper createDataStreamServiceMessage(final DataStreamServiceMessageType messageType, 
            final Message dataMessage)
    {
        final DataStreamServiceNamespace.Builder namespaceMessage = DataStreamServiceNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.DataStreamService, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createDataStreamServiceResponseMessage(final TerraHarvestMessage request,
            final DataStreamServiceMessageType messageType, final Message dataMessage)
    {
        final DataStreamServiceNamespace.Builder namespaceMessage = DataStreamServiceNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.DataStreamService, 
                namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageWrapper createDataStreamStoreMessage(final DataStreamStoreMessageType messageType,
            final Message dataMessage)
    {
        final DataStreamStoreNamespace.Builder namespaceMessage = DataStreamStoreNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.DataStreamStore, namespaceMessage.build()),
                m_MessageSender);
    }
    
    @Override
    public MessageResponseWrapper createDataStreamStoreResponseMessage(final TerraHarvestMessage request,
            final DataStreamStoreMessageType messageType, final Message dataMessage)
    {
        final DataStreamStoreNamespace.Builder namespaceMessage = DataStreamStoreNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.DataStreamStore,
                namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageWrapper createPhysicalLinkMessage(final PhysicalLinkMessageType messageType, 
            final Message dataMessage)
    {
        final PhysicalLinkNamespace.Builder namespaceMessage = PhysicalLinkNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.PhysicalLink, namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createPhysicalLinkResponseMessage(final TerraHarvestMessage request,
            final PhysicalLinkMessageType messageType, final Message dataMessage)
    {
        final PhysicalLinkNamespace.Builder namespaceMessage = PhysicalLinkNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.PhysicalLink, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageWrapper createLinkLayerMessage(final LinkLayerMessageType messageType, final Message dataMessage)
    {
        final LinkLayerNamespace.Builder namespaceMessage = LinkLayerNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.LinkLayer, namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createLinkLayerResponseMessage(final TerraHarvestMessage request,
            final LinkLayerMessageType messageType, final Message dataMessage)
    {
        final LinkLayerNamespace.Builder namespaceMessage = LinkLayerNamespace.newBuilder().setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.LinkLayer, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageWrapper createTransportLayerMessage(final TransportLayerMessageType messageType, 
            final Message dataMessage)
    {
        final TransportLayerNamespace.Builder namespaceMessage = TransportLayerNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.TransportLayer, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createTransportLayerResponseMessage(final TerraHarvestMessage request,
            final TransportLayerMessageType messageType, final Message dataMessage)
    {
        final TransportLayerNamespace.Builder namespaceMessage = TransportLayerNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.TransportLayer, 
                namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageWrapper createObservationStoreMessage(final ObservationStoreMessageType messageType, 
            final Message dataMessage)
    {
        final ObservationStoreNamespace.Builder namespaceMessage = ObservationStoreNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.ObservationStore, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createObservationStoreResponseMessage(final TerraHarvestMessage request,
            final ObservationStoreMessageType messageType, final Message dataMessage)
    {
        final ObservationStoreNamespace.Builder namespaceMessage = ObservationStoreNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.ObservationStore, 
                namespaceMessage.build()), m_MessageSender);
    }

    @Override
    public MessageWrapper createRemoteChannelLookupMessage(final RemoteChannelLookupMessageType messageType,
            final Message dataMessage)
    {
        final RemoteChannelLookupNamespace.Builder namespaceMessage = RemoteChannelLookupNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.RemoteChannelLookup, namespaceMessage.build()), 
                m_MessageSender);
    }

    @Override
    public MessageResponseWrapper createRemoteChannelLookupResponseMessage(final TerraHarvestMessage request,
            final RemoteChannelLookupMessageType messageType, final Message dataMessage)
    {
        final RemoteChannelLookupNamespace.Builder namespaceMessage = RemoteChannelLookupNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.RemoteChannelLookup, 
                namespaceMessage.build()), m_MessageSender);
    }
    
    @Override
    public MessageWrapperAutoEncryption createEncryptionInfoMessage(final EncryptionInfoMessageType messageType, 
            final Message dataMessage)
    {
        final EncryptionInfoNamespace.Builder namespaceMessage = EncryptionInfoNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageWrapperImpl(createPayload(Namespace.EncryptionInfo, namespaceMessage.build()), 
                m_MessageSender);
    }
    
    @Override
    public MessageResponseWrapper createEncryptionInfoResponseMessage(final TerraHarvestMessage request,
            final EncryptionInfoMessageType messageType, final Message dataMessage)
    {
        final EncryptionInfoNamespace.Builder namespaceMessage = EncryptionInfoNamespace.newBuilder().
                setType(messageType);
        if (dataMessage != null)
        {
            namespaceMessage.setData(dataMessage.toByteString());
        }
        return new MessageResponseWrapperImpl(request, createPayload(Namespace.EncryptionInfo, 
                namespaceMessage.build()), m_MessageSender);
    }

    /**
     * Create a {@link TerraHarvestPayload} instance.
     * @param namespace
     *     the namespace enumeration
     * @param namespaceMessage
     *     the namespace defined message
     * @return
     *     the completed payload
     */
    private TerraHarvestPayload createPayload(final Namespace namespace, final Message namespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
            setNamespace(namespace).
            setNamespaceMessage(namespaceMessage.toByteString()).build();
    }
}
