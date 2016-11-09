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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.SetNameRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.CommandResponseEnumConverter;
import mil.dod.th.remote.converter.CommandTypeEnumConverter;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This class is responsible for managing OSGi Assets messages received through the remote interface and sending proper
 * responses according to different incoming Asset request messages.  
 * @author bachmakm
 *
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) // NOCHECKSTYLE - Reached max class fan out complexity
                                            // needed to implement all asset messages
public class AssetMessageService implements MessageService
{
    /**
     * Constant error message sent remotely after the occurrence of an exception.
     */
    final private static String GENERIC_ERR_MSG = "Cannot complete request. "; 
    
    /**
     * Used to log messages.
     */
    private LoggingService m_Logging;

    /**
     * Reference to the event admin service.  Used for local messages within asset message service.
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
     * Service that assists in converting instances of {@link Observation}s from 
     * JAXB objects to proto messages.
     */
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;
    
    /**
     * The service which provides command converting utilities.
     */
    private CommandConverter m_CommandConverterUtility;
    
    /**
     * {@link ExecutorService} which runs threads which process asset commands.
     */
    private ExecutorService m_CommandExecutorService;

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
     * Bind a message route to register.
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
     * Set the CommandConverter service.
     * @param commandConverter
     *  the command converter to use.
     */
    @Reference
    public void setCommandConverter(final CommandConverter commandConverter)
    {
        m_CommandConverterUtility = commandConverter;
    }
    
    /**
     * Activate method creates the {@link JaxbProtoObjectConverter} instances and binds the service to the message 
     * router. 
     */
    @Activate
    public void activate() 
    {
        m_CommandExecutorService = Executors.newCachedThreadPool();
        
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
        return Namespace.Asset;
    }

    @Override //NOCHECKSTYLE - Reached max cyclomatic complexity - needed to implement all asset messages
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload,
        final RemoteChannel channel) throws IOException
    {
        final AssetNamespace assetMessage = AssetNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;

        switch (assetMessage.getType())
        {
            case GetLastStatusRequest:
                dataMessage = getStatus(assetMessage, message, channel);
                break;
            case GetLastStatusResponse:
                dataMessage = GetLastStatusResponseData.parseFrom(assetMessage.getData());
                break;
            case PerformBitRequest:
                dataMessage = performBit(assetMessage, message, channel);
                break;
            case PerformBitResponse:
                dataMessage = PerformBitResponseData.parseFrom(assetMessage.getData());
                break;
            case CaptureDataRequest:
                dataMessage = captureData(assetMessage, message, channel);
                break;
            case CaptureDataResponse:
                dataMessage = CaptureDataResponseData.parseFrom(assetMessage.getData());
                break;
            case ExecuteCommandRequest:
                dataMessage = executeCommand(assetMessage, message, channel);
                break;
            case ExecuteCommandResponse:
                dataMessage = ExecuteCommandResponseData.parseFrom(assetMessage.getData());
                break;
            case ActivateRequest:
                dataMessage = activateAsset(assetMessage, message, channel);
                break;
            case ActivateResponse:
                dataMessage = null;
                break;
            case DeactivateRequest:
                dataMessage = deactiveAsset(assetMessage, message, channel);
                break;
            case DeactivateResponse:
                dataMessage = null;
                break;
            case GetNameRequest:
                dataMessage = getAssetName(assetMessage, message, channel);
                break;
            case GetNameResponse:
                dataMessage = GetNameResponseData.parseFrom(assetMessage.getData());
                break;
            case GetActiveStatusRequest:
                dataMessage = getAssetStatus(assetMessage, message, channel);
                break;
            case GetActiveStatusResponse:
                dataMessage = GetActiveStatusResponseData.parseFrom(assetMessage.getData());
                break;
            case DeleteRequest:
                dataMessage = removeAsset(assetMessage, message, channel);
                break;
            case DeleteResponse:
                dataMessage = null;
                break;
            case SetNameRequest: 
                dataMessage = setAssetName(assetMessage, message, channel);
                break;
            case SetNameResponse:
                dataMessage = null;
                break;
            case SetPropertyRequest:
                dataMessage = setAssetProperty(assetMessage, message, channel);
                break;
            case SetPropertyResponse:
                dataMessage = null;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the AssetMessageService namespace.", assetMessage.getType()));
        }
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, assetMessage, 
                assetMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }
 
    /**
     * Method that is responsible for responding with the status of an Asset based on the UUID contained in the 
     * received request message.
     * @param message
     *      get status request message containing the UUID of the asset status to be queried
     * @param request
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message getStatus(final AssetNamespace message, final TerraHarvestMessage request,
        final RemoteChannel channel) throws IOException
    {
        final GetLastStatusRequestData statusRequest = GetLastStatusRequestData.parseFrom(message.getData());
        
        if (statusRequest.getStatusObservationFormat() != RemoteTypesGen.LexiconFormat.Enum.NATIVE)
        {
            throw new UnsupportedOperationException(
                    String.format("Lexicon format %s is not valid for last status observations", 
                            statusRequest.getStatusObservationFormat())); 
        }
        
        final Asset asset = m_AssetDirectoryService.getAssetByUuid(SharedMessageUtils.
                    convertProtoUUIDtoUUID(statusRequest.getUuid()));
        final Observation statusObs = asset.getLastStatus();

        final GetLastStatusResponseData.Builder response = GetLastStatusResponseData.newBuilder().
                setAssetUuid(statusRequest.getUuid());
        if (statusObs != null)
        {
            try
            {
                response.setStatusObservationNative(
                            (ObservationGen.Observation)m_Converter.convertToProto(statusObs)).build();
            }
            catch (final ObjectConverterException e)
            {
                m_Logging.error(e, "Cannot convert the status of asset [%s] to a proto message.", 
                    asset.getName());
                m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CONVERTER_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
                //return
                return statusRequest;
            }
        }
        m_MessageFactory.createAssetResponseMessage(request, AssetMessageType.GetLastStatusResponse, 
                response.build()).queue(channel);
        return statusRequest;
    }
    
    /**
     * Method responsible for performing a Built-In Test (BIT) on an asset based on the asset UUID contained
     * in the request message.  The summary of the BIT is contained in the response message.   
     * @param message
     *      perform BIT request message containing the UUID of the asset status to be queried
     * @param request
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message performBit(final AssetNamespace message, final TerraHarvestMessage request, 
        final RemoteChannel channel) throws IOException
    {
        final PerformBitRequestData bitRequest = PerformBitRequestData.parseFrom(message.getData());
        
        if (bitRequest.getStatusObservationFormat() != RemoteTypesGen.LexiconFormat.Enum.NATIVE)
        {
            throw new UnsupportedOperationException(
                    String.format("Lexicon format %s is not valid for perform BIT status observations", 
                            bitRequest.getStatusObservationFormat())); 
        }
        
        final Asset asset = m_AssetDirectoryService.getAssetByUuid(SharedMessageUtils.
                    convertProtoUUIDtoUUID(bitRequest.getUuid()));
 
        final Observation statusObs = asset.performBit();
        try
        {
            final PerformBitResponseData response = PerformBitResponseData.newBuilder()
                .setAssetUuid(bitRequest.getUuid())
                .setStatusObservationNative(
                        (ObservationGen.Observation)m_Converter.convertToProto(statusObs))
                .build();

            m_MessageFactory.createAssetResponseMessage(request, AssetMessageType.PerformBitResponse,
                response).queue(channel);
        }
        catch (final ObjectConverterException e)
        {
            m_Logging.error(e, "Cannot convert the status to a proto message after performing BIT for asset [%s].", 
                    asset.getName());
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CONVERTER_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        } 
        return bitRequest;
    }
    
    /**
     * Method responsible for setting property(ies) on an asset based on the asset UUID.
     * @param message
     *  the set asset property request message containing the UUID of the asset and the 
     *  properties that are to be set on the asset
     * @param request
     *  the entire remote message for the request
     * @param channel
     *  the channel that was used to send the request
     * @return
     *  the data message for this request
     * @throws IOException
     *  if message cannot be parsed or if response message cannot be sent
     */
    private Message setAssetProperty(final AssetNamespace message, 
            final TerraHarvestMessage request, final RemoteChannel channel) throws IOException
    {
        final SetPropertyRequestData propRequest = SetPropertyRequestData.parseFrom(message.getData());
        final Asset asset = m_AssetDirectoryService.getAssetByUuid(
                SharedMessageUtils.convertProtoUUIDtoUUID(propRequest.getUuid()));
        
        try
        {
            asset.setProperties(SharedMessageUtils.convertListSimpleTypesMapEntrytoMapStringObject(
                    propRequest.getPropertiesList()));
        }
        catch (final FactoryException exception)
        {
            m_Logging.error(exception, "Failed to set properties for asset %s.", asset.getName());
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ASSET_ERROR, 
                    GENERIC_ERR_MSG + exception.getMessage()).queue(channel);
        }
        
        m_MessageFactory.createAssetResponseMessage(request, 
                AssetMessageType.SetPropertyResponse, null).queue(channel);
        
        return propRequest;
    }
    
    /**
     * Method that is responsible for remotely requesting an asset (specified by a UUID contained in the request
     * message) to capture data.
     * @param message
     *      capture data request message containing the UUID of the asset status to be queried
     * @param request
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or if response message cannot be sent
     */
    private Message captureData(final AssetNamespace message, final TerraHarvestMessage request, final RemoteChannel 
        channel) throws IOException
    {
        final CaptureDataRequestData captureRequest = CaptureDataRequestData.parseFrom(message.getData());
        final Asset asset = m_AssetDirectoryService.getAssetByUuid(SharedMessageUtils.
                    convertProtoUUIDtoUUID(captureRequest.getUuid()));

        
        try
        {
            final Observation observation = asset.captureData();
            final CaptureDataResponseData.Builder responseBuilder = CaptureDataResponseData.newBuilder();
            responseBuilder.setAssetUuid(captureRequest.getUuid());

            switch (captureRequest.getObservationFormat())
            {
                case NATIVE:
                    //Respond with observation data if the getObservationResponse flag was set to true 
                    //Otherwise respond with the UUID of the observation data
                    final ObservationGen.Observation obsGen = (ObservationGen.Observation)m_Converter.
                            convertToProto(observation);
                    responseBuilder.setObservationNative(obsGen);
                    break;

                case UUID_ONLY:
                    final SharedMessages.UUID uuid = SharedMessageUtils.convertUUIDToProtoUUID(observation.getUuid());
                    responseBuilder.setObservationUuid(uuid);
                    break;
                    
                default:
                    throw new UnsupportedOperationException(
                            String.format("Asset capture data request does not support the %s format", 
                                    captureRequest.getObservationFormat()));
            }

            m_MessageFactory.createAssetResponseMessage(request, AssetMessageType.CaptureDataResponse, 
                    responseBuilder.build()).queue(channel);
        }
        catch (final AssetException e)
        {
            m_Logging.error(e, "Failed to get observation from asset %s during capture data.", asset.getName());
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ASSET_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        catch (final ObjectConverterException e)
        {
            m_Logging.error(e, "Cannot convert observation to proto message for asset %s in capture data.", 
                    asset.getName());
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CONVERTER_ERROR, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }       
        return captureRequest;
    }
    
    /**
     * Method responsible for handling a request for an asset to execute a specific command per specifications
     * contained in the request message.  
     * @param message
     *      message containing the UUID of the asset that a command is executed on
     * @param request
     *      entire remote message for the request
     * @param channel
     *      the channel that was used to send the request
     * @return
     *      the data message for this request
     * @throws InvalidProtocolBufferException 
     *      thrown when a protocol message being parsed is invalid 
     */
    private Message executeCommand(final AssetNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException 
    {    
        final ExecuteCommandRequestData executeCommandRequestData = 
              ExecuteCommandRequestData.parseFrom(message.getData()); 
        final Asset asset = m_AssetDirectoryService.getAssetByUuid(
                SharedMessageUtils.convertProtoUUIDtoUUID(executeCommandRequestData.getUuid())); 
        
        final CommandTypeEnum executeCommandEnum = 
                CommandTypeEnumConverter.convertProtoEnumToJava(executeCommandRequestData.getCommandType());
        
        final CommandResponseEnum responseType = 
                m_CommandConverterUtility.getResponseTypeFromCommandType(executeCommandEnum);
        
        try
        {
            final Command command = m_CommandConverterUtility.
                    getJavaCommandType(executeCommandRequestData.getCommand().toByteArray(), executeCommandEnum);
            
            //start a runnable instance to execute the command.
            m_CommandExecutorService.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        final Response responseJaxb = asset.executeCommand(command);
                        
                        final Message responseProto = m_Converter.convertToProto(responseJaxb);
                        final ExecuteCommandResponseData responseData = ExecuteCommandResponseData.newBuilder().
                                        setUuid(SharedMessageUtils.convertUUIDToProtoUUID(asset.getUuid())).
                                        setResponseType(CommandResponseEnumConverter
                                                .convertJavaEnumToProto(responseType)).
                                        setResponse(responseProto.toByteString()).build();
                        m_MessageFactory.createAssetResponseMessage(request, AssetMessageType.ExecuteCommandResponse, 
                                responseData).queue(channel);
                    }
                    catch (final CommandExecutionException exception)
                    {
                        m_MessageFactory.createBaseErrorResponse(request, exception, 
                                String.format("Unable to execute command %s for asset %s", 
                                        executeCommandEnum, asset.getName())).queue(channel);
                    }
                    catch (final ObjectConverterException exception)
                    {
                        m_MessageFactory.createBaseErrorResponse(request, exception, 
                                String.format("Unable to convert command response to proto message for " 
                                        + "asset %s when executing command %s",
                                        asset.getName(), executeCommandEnum)).queue(channel);
                    }
                    catch (final InterruptedException exception)
                    {
                        m_MessageFactory.createBaseErrorResponse(request, exception, 
                                String.format("Execution of command %s for asset %s was interrupted.",
                                        executeCommandEnum, asset.getName())).queue(channel);
                    }
                    catch (final Exception exception)
                    {
                        m_MessageFactory.createBaseErrorResponse(request, exception, String.format(
                                "An exception occurred for asset %s while processing command %s.", 
                                asset.getName(), executeCommandEnum)).queue(channel);
                    }
                }
            });
        }
        catch (final ObjectConverterException exception)
        {
            m_MessageFactory.createBaseErrorResponse(request, exception, String.format(
                    "Unable to retrieve java command from execute request for asset %s. Cannot execute command %s.", 
                    asset.getName(), executeCommandEnum)).queue(channel);
        }
        
        return executeCommandRequestData;
    }    

    /**
     * Method responsible for handling a remote request to remove an asset from the directory.   
     * @param message
     *      remove asset request message containing the UUID of the asset to be removed from the directory
     * @param request
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed  
     */
    private Message removeAsset(final AssetNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws IOException
    {
        final DeleteRequestData removeRequest = DeleteRequestData.parseFrom(message.getData());

        final Asset asset = m_AssetDirectoryService.getAssetByUuid(
                SharedMessageUtils.convertProtoUUIDtoUUID(removeRequest.getUuid()));
        asset.delete();
        m_MessageFactory.createAssetResponseMessage(request, 
                    AssetMessageType.DeleteResponse, null).queue(channel);
        return removeRequest;
    }
    
    /**
     * Method responsible for handling a remote request to activate a specific asset based on that asset's UUID.
     * @param message
     *      activate asset request message containing the UUID of the asset to be activated
     * @param request
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed   
     */
    private Message activateAsset(final AssetNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws IOException
    {
        final ActivateRequestData activateRequest = ActivateRequestData.parseFrom(message.getData());
        try
        {
            final Asset asset = m_AssetDirectoryService.getAssetByUuid(
                SharedMessageUtils.convertProtoUUIDtoUUID(activateRequest.getUuid()));
            asset.activateAsync();
            m_MessageFactory.createAssetResponseMessage(request, 
                AssetMessageType.ActivateResponse, null).queue(channel);
        }
        catch (final IllegalStateException e)
        {
            m_Logging.error(e, "Failed to activate asset.");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        return activateRequest;
    }

    /**
     * Method responsible for handling a remote request to deactivate a specific asset based on that asset's UUID.
     * @param message
     *      deactivate asset request message containing the UUID of the asset to be activated
     * @param request
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed  
     */
    private Message deactiveAsset(final AssetNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws IOException
    {
        final DeactivateRequestData deactivateRequest = DeactivateRequestData.parseFrom(message.getData());
        try
        {
            final Asset asset = m_AssetDirectoryService.getAssetByUuid(
                SharedMessageUtils.convertProtoUUIDtoUUID(deactivateRequest.getUuid()));
            asset.deactivateAsync();
            m_MessageFactory.createAssetResponseMessage(request, 
                AssetMessageType.DeactivateResponse, null).queue(channel);
        }
        catch (final IllegalStateException e)
        {
            m_Logging.error(e, "Failed to deactivate asset.");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE, 
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }

        return deactivateRequest;
    }

    /**
     * Method responsible for handling a remote request to get the status of a specific asset 
     * based on that asset's UUID.
     * @param message
     *      get asset status request message containing the UUID of the asset to be queried
     * @param request
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed 
     */
    private Message getAssetStatus(final AssetNamespace message, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws IOException
    {
        final GetActiveStatusRequestData statusRequest = GetActiveStatusRequestData.parseFrom(message.getData());

        final AssetActiveStatus status = m_AssetDirectoryService.getAssetByUuid(SharedMessageUtils.
                convertProtoUUIDtoUUID(statusRequest.getUuid())).getActiveStatus();
        final GetActiveStatusResponseData response = GetActiveStatusResponseData.newBuilder().
                setStatus(EnumConverter.convertJavaAssetActiveStatusToProto(status)).
                setUuid(statusRequest.getUuid()).build(); 
        m_MessageFactory.createAssetResponseMessage(request, 
                AssetMessageType.GetActiveStatusResponse, response).queue(channel);
        return statusRequest;
    }
    
    /**
     * Handle setting the given asset's name.
     * @param serviceMessage
     *      set asset name request message containing the UUID of the asset to be queried 
     * @param message
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed  
     */
    private Message setAssetName(final AssetNamespace serviceMessage,
        final TerraHarvestMessage message, final RemoteChannel channel) throws IOException
    {
        //parse the set asset name request
        final SetNameRequestData request = SetNameRequestData.parseFrom(serviceMessage.getData());

        final UUID uuid = SharedMessageUtils.convertProtoUUIDtoUUID(request.getUuid());
        try
        {
            final Asset asset = m_AssetDirectoryService.getAssetByUuid(uuid);
            asset.setName(request.getAssetName());
        }
        catch (final IllegalArgumentException e)
        {
            final String errorDesc = String.format("Request to set the asset's name with "
                + "UUID %s was null, overlapped with another asset's name, or the asset was not found. %s", 
                uuid.toString(), e.getMessage());
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, errorDesc).queue(channel);
            m_Logging.error(e, errorDesc);
            return request;
        }
        catch (final FactoryException e)
        {
            final String errorDesc = String.format("Unable to set the name for asset with UUID %s. %s", uuid.toString(),
                e.getMessage());
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.ASSET_ERROR, errorDesc).queue(channel);
            m_Logging.error(e, errorDesc);
            return request;
        }
        //send response
        m_MessageFactory.createAssetResponseMessage(
             message, AssetMessageType.SetNameResponse, null).queue(channel);
        return request;
    }

    /**
     * Handle getting the given asset's name.
     * @param serviceMessage
     *      get asset name request message containing the UUID of the asset to be queried 
     * @param message
     *      entire remote message for the request
     * @param channel 
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException 
     *      if message cannot be parsed  
     */
    private Message getAssetName(final AssetNamespace serviceMessage, 
        final TerraHarvestMessage message, final RemoteChannel channel) throws IOException
    {
        //parse the set asset name request
        final GetNameRequestData request = GetNameRequestData.parseFrom(serviceMessage.getData());

        final UUID uuid = SharedMessageUtils.convertProtoUUIDtoUUID(request.getUuid());
        final Asset asset = m_AssetDirectoryService.getAssetByUuid(uuid);
        final String name = asset.getName();

        //send response
        final GetNameResponseData response = GetNameResponseData.newBuilder().
             setAssetName(name).
             setUuid(request.getUuid()).build();
        m_MessageFactory.createAssetResponseMessage(
             message, AssetMessageType.GetNameResponse, response).queue(channel);
        return request;
    }
}
