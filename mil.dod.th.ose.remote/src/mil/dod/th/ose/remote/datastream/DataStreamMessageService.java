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
package mil.dod.th.ose.remote.datastream;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileFactory;
import mil.dod.th.core.datastream.capability.StreamProfileCapabilities;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace.DataStreamServiceMessageType;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DisableStreamProfileRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.EnableStreamProfileRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesRequestData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.GetStreamProfilesResponseData.Builder;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.datastream.capability.StreamProfileCapabilitiesGen;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * This class is responsible for receiving and responding to messages from the DataStreamService 
 * namespace using the remote interface.
 * 
 * @author jmiller
 *
 */
@Component(immediate = true, provide = { })
public class DataStreamMessageService implements MessageService
{
    
    /**
     * Constant error message sent remotely after the occurrence of an exception.
     */
    final private static String GENERIC_ERR_MSG = "Cannot complete request. ";

    /**
     * Used for logging messages.
     */
    private LoggingService m_Logging;

    /**
     * Reference to the event admin service.  Used for local messages within event admin service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Local service for managing StreamProfile instances.
     */
    private DataStreamService m_DataStreamService;
    
    /**
     * Local service for managing assets.
     */
    private AssetDirectoryService m_AssetDirectoryService;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;
    
    /**
     * Service that assists in converting instances of {@link StreamProfileCapabilities}s from
     * JAXB objects to proto messages.
     */
    private JaxbProtoObjectConverter m_Converter;
    
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
    
    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      service used to post events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Bind the DataStreamService.
     * 
     * @param dataStreamService
     *      service for managing StreamProfile instances locally
     */
    @Reference(optional = true, dynamic = true)
    public void setDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = dataStreamService;
    }

    /**
     * Unbind the DataStreamService.
     * 
     * @param dataStreamService
     *      service for managing StreamProfile instances locally
     */
    public void unsetDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = null; // NOPMD: NullAssignment, Must assign to null if no longer available
    }

    /**
     * Bind the AssetDirectoryService.
     * 
     * @param assetDirectoryService
     *      service for managing assets locally
     */
    @Reference
    public void setAssetDirectoryService(final AssetDirectoryService assetDirectoryService)
    {
        m_AssetDirectoryService = assetDirectoryService;
    }
    
    /**
     * Bind to the service for creating remote messages.
     * 
     * @param messageFactory
     *      service that create messages
     */
    @Reference
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /**
     * Bind a message router to register.
     * 
     * @param messageRouter
     *      router that handles incoming messages
     */
    @Reference
    public void setMessageRouter(final MessageRouterInternal messageRouter)
    {
        m_MessageRouter = messageRouter;
    }
    
    /**
     * Set the {@link mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter}.
     * 
     * @param converter
     *     the service responsible for converting between proto and JAXB objects.
     */
    @Reference
    public void setJaxbProtoObjectConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;        
    }
    
    /**
     * Activate method to bind this service to the message router.
     */
    @Activate
    public void activate()
    {
        m_MessageRouter.bindMessageService(this);
    }
    
    /**
     * Deactivate component by unbinding the service from the message router.
     */
    @Deactivate
    public void deactivate()
    {
        m_MessageRouter.unbindMessageService(this);
    }
    
    @Override
    public Namespace getNamespace()
    {
        return Namespace.DataStreamService;
    }
    
    @Override
    public void handleMessage(final TerraHarvestMessage message,
            final TerraHarvestPayload payload, final RemoteChannel channel) throws IOException
    {
        //parse event message
        final DataStreamServiceNamespace serviceMessage = DataStreamServiceNamespace.
                parseFrom(payload.getNamespaceMessage());
        
        Message dataMessage = null;
        
        switch (serviceMessage.getType())
        {
            case GetStreamProfilesRequest:
                getStreamProfiles(serviceMessage, message, channel);
                break;
            case GetStreamProfilesResponse:
                dataMessage = GetStreamProfilesResponseData.parseFrom(serviceMessage.getData());
                break;
            case GetCapabilitiesRequest:
                dataMessage = getCapabilities(serviceMessage, message, channel);
                break;
            case GetCapabilitiesResponse:
                dataMessage = GetCapabilitiesResponseData.parseFrom(serviceMessage.getData());
                break;
            case EnableStreamProfileRequest:
                dataMessage = enableStreamProfile(serviceMessage, message, channel);
                break;
            case EnableStreamProfileResponse:
                break;
            case DisableStreamProfileRequest:
                dataMessage = disableStreamProfile(serviceMessage, message, channel);
                break;
            case DisableStreamProfileResponse:
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the DataStreamMessageService namespace.", serviceMessage.getType()));
        }

        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, serviceMessage, 
                serviceMessage.getType(), dataMessage, channel);

        m_EventAdmin.postEvent(event);
    }
    
    /**
     * Method responsible for handling a remote request to enable a stream profile based on that
     * stream profile's UUID.
     * 
     * @param message
     *      EnableStreamProfileRequest message containing the UUID of the stream profile to be enabled
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed
     */
    private Message enableStreamProfile(final DataStreamServiceNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws IOException
    {
        final EnableStreamProfileRequestData enableRequest = 
                EnableStreamProfileRequestData.parseFrom(message.getData());

        try
        {
            final StreamProfile streamProfile = m_DataStreamService.getStreamProfile(
                    SharedMessageUtils.convertProtoUUIDtoUUID(enableRequest.getUuid()));
            streamProfile.setEnabled(true);
            m_MessageFactory.createDataStreamServiceResponseMessage(request, 
                    DataStreamServiceMessageType.EnableStreamProfileResponse, null).queue(channel);
        }
        catch (final IllegalArgumentException e)
        {
            m_Logging.error(e, "Failed to enable stream profile.");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE,
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }

        return enableRequest;
        
    }

    /**
     * Method responsible for handling a remote request to disable a stream profile based on that
     * stream profile's UUID.
     * 
     * @param message
     *      DisableStreamProfileRequest message containing the UUID of the stream profile to be disabled
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed
     */
    private Message disableStreamProfile(final DataStreamServiceNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws IOException
    {
        final DisableStreamProfileRequestData disableRequest = 
                DisableStreamProfileRequestData.parseFrom(message.getData());
        
        try
        {
            final StreamProfile streamProfile = m_DataStreamService.getStreamProfile(
                    SharedMessageUtils.convertProtoUUIDtoUUID(disableRequest.getUuid()));
            streamProfile.setEnabled(false);
            m_MessageFactory.createDataStreamServiceResponseMessage(request,
                    DataStreamServiceMessageType.DisableStreamProfileResponse, null).queue(channel);
        }
        catch (final IllegalStateException e)
        {
            m_Logging.error(e, "Failed to disable stream profile");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE,
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        
        return disableRequest;
        
    }

    /**
     * Method responsible for handling a request to get a stream profile's capabilities based
     * on the stream profile UUID contained in the request message.
     * 
     * @param message
     *      GetCapabilitiesRequest message containing the UUID of the stream profile
     * @param request
     *      entire remote message for the request
     * @param channel
     *      the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message getCapabilities(final DataStreamServiceNamespace message, final TerraHarvestMessage request,
            final RemoteChannel channel) throws IOException
    {
        final GetCapabilitiesRequestData capabilitiesRequest = GetCapabilitiesRequestData.parseFrom(message.getData());
        final String productType = capabilitiesRequest.getProductType();
        final Set<StreamProfileFactory> streamProfileFactories = m_DataStreamService.getStreamProfileFactories();
        
        StreamProfileFactory factory = null;
        
        //get the factory by the sream profile name
        for (StreamProfileFactory tempFactory : streamProfileFactories)
        {
            if (productType.equals(tempFactory.getProductType()))
            {
                factory = tempFactory;
                break;
            }
        }
        
        if (factory == null)
        {
            m_Logging.error("Cannot get capabilities, no factory found for type %s.", productType);
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE, 
                    GENERIC_ERR_MSG + "Factory not found.").queue(channel);                
            return capabilitiesRequest;
        }
        
        //Get capabilities from factory
        try
        {
            final StreamProfileCapabilities capabilities = factory.getStreamProfileCapabilities();
            final StreamProfileCapabilitiesGen.StreamProfileCapabilities capGen =
                    (StreamProfileCapabilitiesGen.StreamProfileCapabilities)m_Converter.
                    convertToProto(capabilities);
            final GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                    setProductType(productType).
                    setCapabilities(capGen).build();
            
            m_MessageFactory.createDataStreamServiceResponseMessage(request,
                    DataStreamServiceMessageType.GetCapabilitiesResponse,
                    response).queue(channel);
        }
        catch (final ObjectConverterException e)
        {
            m_Logging.error(e, "Cannot convert observation to proto message for "
                    + "stream profile %s in get capabilities.", productType);
            m_MessageFactory.createBaseErrorMessage(request,  ErrorCode.CONVERTER_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        
        return capabilitiesRequest;
    }

    /**
     * Method responsible for handling a remote request to get a list of the stream profiles known to the service.
     * 
     * @param message
     *      GetStreamProfilesRequest message which may optionally contain an asset UUID, such that only those
     *      StreamProfile instances associated with that asset are returned
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      if message cannot be parsed
     */
    private void getStreamProfiles(final DataStreamServiceNamespace message, 
            final TerraHarvestMessage request, final RemoteChannel channel) throws IOException
    {
        
        final GetStreamProfilesRequestData data = GetStreamProfilesRequestData.parseFrom(message.getData());
        final Set<StreamProfile> streamProfiles;
        
        //Check if request has an Asset UUID specified, which returns only those StreamProfile instances associated
        //with that Asset.  Otherwise return all StreamProfile instances.
        if (data.hasAssetUuid())
        {
            final UUID assetUuid = SharedMessageUtils.convertProtoUUIDtoUUID(data.getAssetUuid());
            final Asset asset = m_AssetDirectoryService.getAssetByUuid(assetUuid);
            streamProfiles = m_DataStreamService.getStreamProfiles(asset);            
        }
        else
        {
            streamProfiles = m_DataStreamService.getStreamProfiles();
        }
        
        final Builder builder = GetStreamProfilesResponseData.newBuilder();
        
        for (StreamProfile profile : streamProfiles)
        {
            final DataStreamServiceMessages.StreamProfile.Builder streamProfileBuilder = 
                    DataStreamServiceMessages.StreamProfile.newBuilder();
            
            streamProfileBuilder.setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(profile.getAsset().getUuid()))
                .setBitrateKbps(profile.getBitrate())
                .setFormat(profile.getFormat())
                .setInfo(SharedMessageUtils.createFactoryObjectInfoMessage(profile))
                .setIsEnabled(profile.isEnabled())
                .setSensorId(profile.getSensorId())
                .setStreamPort(profile.getStreamPort().toString());

            builder.addStreamProfile(streamProfileBuilder.build());
        }
        
        m_MessageFactory.createDataStreamServiceResponseMessage(request,
                DataStreamServiceMessageType.GetStreamProfilesResponse, builder.build()).queue(channel);
        
        
        
    }
    
    

}
