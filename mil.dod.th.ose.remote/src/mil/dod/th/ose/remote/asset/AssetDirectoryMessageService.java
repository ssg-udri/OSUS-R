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
package mil.dod.th.ose.remote.asset;

import java.io.IOException;
import java.util.Set;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace
    .AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.CreateAssetResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetAssetsResponseData.Builder;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetScannableAssetTypesResponseData;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.ScanForNewAssetsRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.asset.capability.AssetCapabilitiesGen;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This class is responsible for managing asset directory service messages received through the remote 
 * interface and sending proper responses according to different incoming AssetDirectoryService request messages.  
 * @author Admin
 *
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) // NOCHECKSTYLE - Reached max class fan out complexity
                                            // Needed to implement all service messages
public class AssetDirectoryMessageService implements MessageService
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
     * Service that assists in converting instances of {@link AssetCapabilities}s from 
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
        return Namespace.AssetDirectoryService;
    }

    @Override //NOCHECKSTYLE - Reached max cyclomatic complexity - needed to implement all service messages
    public void handleMessage(final TerraHarvestMessage message, //NOPMD: Method length, TODO: TH-2801: could fix this
            final TerraHarvestPayload payload, final RemoteChannel channel) throws IOException
    {
        //parse event message
        final AssetDirectoryServiceNamespace serviceMessage = AssetDirectoryServiceNamespace.
                parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;

        switch (serviceMessage.getType())
        {
            case GetAssetTypesRequest:
                dataMessage = null;
                getAssetTypes(message, channel);
                break;
            case GetAssetTypesResponse:
                dataMessage = GetAssetTypesResponseData.parseFrom(serviceMessage.getData());
                break;
            case ScanForNewAssetsRequest:
                dataMessage = scanForNewAssets(serviceMessage, message, channel);
                break;
            case ScanForNewAssetsResponse:
                dataMessage = null;
                break;
            case CreateAssetRequest:
                dataMessage = createAsset(serviceMessage, message, channel);
                break;
            case CreateAssetResponse:
                dataMessage = CreateAssetResponseData.parseFrom(serviceMessage.getData());
                break;
            case GetAssetsRequest:
                dataMessage = null;
                getAssets(message, channel);
                break;
            case GetAssetsResponse:
                dataMessage = GetAssetsResponseData.parseFrom(serviceMessage.getData());
                break;
            case GetScannableAssetTypesRequest:
                dataMessage = null;
                getScannableAssetTypes(message, channel);
                break;
            case GetScannableAssetTypesResponse:
                dataMessage = GetScannableAssetTypesResponseData.parseFrom(serviceMessage.getData());
                break;
            case GetCapabilitiesRequest:
                dataMessage = getCapabilities(serviceMessage, message, channel);
                break;      
            case GetCapabilitiesResponse:
                dataMessage = GetCapabilitiesResponseData.parseFrom(serviceMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the AssetDirectoryMessageService namespace.", serviceMessage.getType()));
        }

        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, serviceMessage, 
                serviceMessage.getType(), dataMessage, channel);

        m_EventAdmin.postEvent(event);
    }

    /**
     * Method responsible for getting all of the asset factory names and types known to the asset directory service. 
     * @param request
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     */
    private void getAssetTypes(final TerraHarvestMessage request, final RemoteChannel channel)
    {
        final GetAssetTypesResponseData.Builder assetTypesResponseBuilder = GetAssetTypesResponseData.newBuilder();

        for (AssetFactory factory : m_AssetDirectoryService.getAssetFactories())
        {
            assetTypesResponseBuilder.addProductName(factory.getProductName());
            assetTypesResponseBuilder.addProductType(factory.getProductType());
        }

        final GetAssetTypesResponseData response = assetTypesResponseBuilder.build();
        m_MessageFactory.createAssetDirectoryServiceResponseMessage(request, 
                AssetDirectoryServiceMessageType.GetAssetTypesResponse, response).queue(channel);
    }

    /**
     * Method responsible for remotely requesting the asset service to scan the system for new assets. If the asset
     * type is attached to the request message, the service only scans for new assets of the specified type. If a
     * type is not specified in the request message, the service scans for new assets of all types.  
     * @param message
     *      "scan for new assets" request message which optionally contains the specific type of asset for 
     *      which to scan 
     * @param request
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message scanForNewAssets(final AssetDirectoryServiceNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws IOException
    {
        final ScanForNewAssetsRequestData scanRequest = ScanForNewAssetsRequestData.parseFrom(message.getData());
        final String assetType = scanRequest.getProductType();
        try
        {
            if (assetType.isEmpty())
            {
                m_AssetDirectoryService.scanForNewAssets();
            }
            else
            {
                m_AssetDirectoryService.scanForNewAssets(assetType);
            }
            m_MessageFactory.createAssetDirectoryServiceResponseMessage(request, 
                    AssetDirectoryServiceMessageType.ScanForNewAssetsResponse, null).queue(channel);
        }
        catch (final IllegalArgumentException e)
        {
            m_Logging.error(e, "Failed to scan assets of type %s.", assetType);
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ASSET_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        } 
        return scanRequest;
    }

    /**
     * Method responsible for handling a remote request for creating and adding an asset to the directory.
     * @param message
     *      create asset request message containing the fully qualified class name of the asset to be created
     * @param request
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response  
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed 
     */
    private Message createAsset(final AssetDirectoryServiceNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel)throws IOException
    {
        final CreateAssetRequestData createRequest = CreateAssetRequestData.parseFrom(message.getData());
        final String productType = createRequest.getProductType();
        final String assetName = createRequest.getName();

        try
        {
            final Asset newAsset;
            //If create asset request contains configuration properties then create the asset with the specified
            //properties in the message.
            if (createRequest.getPropertiesCount() > 0)
            {
                newAsset = m_AssetDirectoryService.createAsset(productType, assetName, SharedMessageUtils
                        .convertListSimpleTypesMapEntrytoMapStringObject(createRequest.getPropertiesList())); 
            }
            else
            {
                newAsset = m_AssetDirectoryService.createAsset(productType, assetName);
            }
            
            final CreateAssetResponseData response = CreateAssetResponseData.newBuilder().
                setInfo(SharedMessageUtils.createFactoryObjectInfoMessage(newAsset)).build();

            m_MessageFactory.createAssetDirectoryServiceResponseMessage(request, 
                    AssetDirectoryServiceMessageType.CreateAssetResponse, response).queue(channel);
        }
        catch (final AssetException e)
        {
            m_Logging.error(e, "Cannot add asset of type %s to directory.", productType);
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ASSET_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        return createRequest;
    }

    /**
     * Method responsible for handling a remote request to get a list of the assets known to the service.
     * @param request
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     * @throws IOException 
     *      if message cannot be parsed  
     */
    private void getAssets(final TerraHarvestMessage request, final RemoteChannel channel) 
            throws IOException
    {
        final Set<Asset> assets = this.m_AssetDirectoryService.getAssets();
        final Builder builder = GetAssetsResponseData.newBuilder();
        for (Asset asset: assets)
        {
            final FactoryObjectInfo assetInfo = SharedMessageUtils.createFactoryObjectInfoMessage(asset);
            builder.addAssetInfo(assetInfo);
        }
        m_MessageFactory.createAssetDirectoryServiceResponseMessage(request, 
                AssetDirectoryServiceMessageType.GetAssetsResponse, builder.build()).queue(channel);
    }
    
    /**
     * Handle a request for getting available asset types that are scannable.
     * @param request
     *      the message received containing the request
     * @param channel
     *      the channel from which the message came
     */
    private void getScannableAssetTypes(final TerraHarvestMessage request, final RemoteChannel channel)
    {
        //send response
        final GetScannableAssetTypesResponseData.Builder response = GetScannableAssetTypesResponseData.newBuilder();
        
        //fetch scannable types
        for (String assetType : m_AssetDirectoryService.getScannableAssetTypes())
        {
            response.addScannableAssetType(assetType);
        }
        
        m_MessageFactory.createAssetDirectoryServiceResponseMessage(request, 
                AssetDirectoryServiceMessageType.GetScannableAssetTypesResponse, response.build()).queue(channel);
    }

    /**
     * Method responsible for handling a request to get an asset's capabilities based on 
     * the asset UUID contained in the request message. 
     * @param message
     *      get capabilities request message containing the UUID of the asset
     * @param request
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message getCapabilities(final AssetDirectoryServiceNamespace message, final TerraHarvestMessage request,
        final RemoteChannel channel) throws IOException
    {
        final GetCapabilitiesRequestData capabilitiesRequest = GetCapabilitiesRequestData.parseFrom(message.getData());
        final String productType = capabilitiesRequest.getProductType();
        final Set<AssetFactory> deviceFactories = m_AssetDirectoryService.getAssetFactories();
        
        AssetFactory factory = null;
        
        //get the factory by asset name
        for (AssetFactory tempFactory : deviceFactories)
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
            final AssetCapabilities capabilities = factory.getAssetCapabilities();
            final AssetCapabilitiesGen.AssetCapabilities capGen = 
                    (AssetCapabilitiesGen.AssetCapabilities)m_Converter.
                    convertToProto(capabilities);
            final GetCapabilitiesResponseData response = GetCapabilitiesResponseData.newBuilder().
                    setProductType(productType).
                    setCapabilities(capGen).build();
           
            m_MessageFactory.createAssetDirectoryServiceResponseMessage(request, 
                AssetDirectoryServiceMessageType.GetCapabilitiesResponse, 
                    response).queue(channel);
        }
        catch (final ObjectConverterException e)
        {
            m_Logging.error(e, "Cannot convert observation to proto message for asset %s in get capabilities.", 
                    productType);
            m_MessageFactory.createBaseErrorMessage(request,  ErrorCode.CONVERTER_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }

        return capabilitiesRequest;
    }
}
