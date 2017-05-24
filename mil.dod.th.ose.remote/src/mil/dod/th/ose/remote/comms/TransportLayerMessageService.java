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
package mil.dod.th.ose.remote.comms;

import java.io.IOException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.TransportLayerMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.GetLinkLayerRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.GetLinkLayerResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsAvailableRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsAvailableResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsReceivingRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsReceivingResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsTransmittingRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.IsTransmittingResponseData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.ShutdownRequestData;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This class is responsible for managing Transport Layer messages received through the remote interface and 
 * sending proper responses according to different incoming Transport Layer request messages.  
 * @author matt
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { })
public class TransportLayerMessageService implements MessageService
{
    /**
     * Used to log messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Local service for managing custom comms.
     */
    private CustomCommsService m_CommsService;
    
    /**
     * Reference to the event admin service.  Used for local messages within the transport layer message service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Service for managing addresses.
     */
    private AddressManagerService m_AddressManagerService;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;

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
     * Bind the event admin.
     * @param eventAdmin
     *      service used to post events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
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
     * Bind the custom comms service.
     * @param customCommsService
     *      service for managing custom comms locally
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService customCommsService)
    {
        m_CommsService = customCommsService;
    }
    
    /**
     * Binds the address manager service.  
     * @param addressManager
     *      service to use for obtaining address objects
     */
    @Reference
    public void setAddressManagerService(final AddressManagerService addressManager)
    {
        m_AddressManagerService = addressManager;
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
        return Namespace.TransportLayer;
    }

    @Override //NOCHECKSTYLE: High cyclomatic complexity, need to handle all messages
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload,
        final RemoteChannel channel) throws IOException
    {
        final TransportLayerNamespace transportLayerMessage = TransportLayerNamespace.
              parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;

        switch (transportLayerMessage.getType())
        {
            case IsReceivingRequest:
                dataMessage = isReceiving(transportLayerMessage, message, channel);
                break;
            case IsReceivingResponse:
                dataMessage = IsReceivingResponseData.parseFrom(transportLayerMessage.getData());
                break;
            case IsTransmittingRequest:
                dataMessage = isTransmitting(transportLayerMessage, message, channel);
                break;
            case IsTransmittingResponse:
                dataMessage = IsTransmittingResponseData.parseFrom(transportLayerMessage.getData());
                break;
            case IsAvailableRequest:
                dataMessage = isAvailable(transportLayerMessage, message, channel);
                break;
            case IsAvailableResponse:
                dataMessage = IsAvailableResponseData.parseFrom(transportLayerMessage.getData());
                break;
            case GetLinkLayerRequest:
                dataMessage = getLinkLayer(transportLayerMessage, message, channel);
                break;
            case GetLinkLayerResponse:
                dataMessage = GetLinkLayerResponseData.parseFrom(transportLayerMessage.getData());
                break;
            case ShutdownRequest:
                dataMessage = shutdown(transportLayerMessage, message, channel);
                break;
            case ShutdownResponse:
                dataMessage = null;
                break;
            case DeleteRequest:
                dataMessage = removeTransportLayer(transportLayerMessage, message, channel);
                break;
            case DeleteResponse:
                dataMessage = null;
                break;
            case SetPropertyRequest:
                dataMessage = setTransportLayerProperty(transportLayerMessage, message, channel);
                break;
            case SetPropertyResponse:
                dataMessage = null;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the TransportLayerMessageService namespace.", transportLayerMessage.getType()));
        }
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, 
             transportLayerMessage, transportLayerMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);        
    }

    /**
     * Shutdown a {@link TransportLayer}.
     * @param transportLayerMessage
     *      request message containing the specific transport layer to shutdown.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      when a message being parsed is invalid in some way.
     */
    private Message shutdown(final TransportLayerNamespace transportLayerMessage, final TerraHarvestMessage request,
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final ShutdownRequestData shutdownRequest = ShutdownRequestData.parseFrom(transportLayerMessage.getData());

        final TransportLayer tLayer = CustomCommUtility.getTransportLayerByUuid(m_CommsService,
                shutdownRequest.getUuid());
        tLayer.shutdown();

        m_MessageFactory.createTransportLayerResponseMessage(request, 
                TransportLayerMessageType.ShutdownResponse,
                null).queue(channel);
        
        return shutdownRequest;
    }

    /**
     * Retrieve the {@link mil.dod.th.core.ccomm.link.LinkLayer} associated with the specified {@link TransportLayer}.
     * @param transportLayerMessage
     *      request message containing the specific transport layer to get the associated link layer.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      thrown when a message being parsed is invalid in some way
     */
    private Message getLinkLayer(final TransportLayerNamespace transportLayerMessage, final TerraHarvestMessage request,
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final GetLinkLayerRequestData getLinkLayer = GetLinkLayerRequestData.parseFrom(transportLayerMessage.getData());

        final TransportLayer transportLayer = CustomCommUtility.getTransportLayerByUuid(m_CommsService,
                getLinkLayer.getUuid());

        final GetLinkLayerResponseData response = GetLinkLayerResponseData.newBuilder().setUuid(
                SharedMessageUtils.convertUUIDToProtoUUID(transportLayer.getLinkLayer().getUuid())).build();

        m_MessageFactory.createTransportLayerResponseMessage(request, TransportLayerMessageType.GetLinkLayerResponse,
                response).queue(channel);
        
        return getLinkLayer;
    }

    /**
     * Get whether or not an {@link Address} can be reached by the specified {@link TransportLayer}.
     * @param transportLayerMessage
     *      request message containing the specific transport layer to see if an address is available to it.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      thrown when a message being parsed is invalid in some way
     */
    private Message isAvailable(final TransportLayerNamespace transportLayerMessage, final TerraHarvestMessage request,
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final IsAvailableRequestData isAvailableRequest = IsAvailableRequestData.parseFrom(
                transportLayerMessage.getData());
        final String address = isAvailableRequest.getAddress();
        
        try
        {
            final TransportLayer requestedTransportLayer = CustomCommUtility.getTransportLayerByUuid(m_CommsService, 
                    isAvailableRequest.getUuid());
            
            final Address transportAddress = m_AddressManagerService.getOrCreateAddress(address);
            
            final IsAvailableResponseData response = IsAvailableResponseData.newBuilder().
                setIsAvailable(requestedTransportLayer.isAvailable(transportAddress)).
                setUuid(isAvailableRequest.getUuid()).build();
            
            m_MessageFactory.createTransportLayerResponseMessage(request, 
                    TransportLayerMessageType.IsAvailableResponse, response).queue(channel);
        }
        catch (final CCommException e)
        {
            m_Logging.error(e, "IsAvailable: Cannot get or create address.");
            
            m_MessageFactory.createBaseErrorMessage(request, 
                            ErrorCode.CCOMM_ERROR, 
                            "Custom comms error when checking if address was available" + e.getMessage()).
                            queue(channel);
        }
        
        return isAvailableRequest;
    }

    /**
     * Get whether or not the specified {@link TransportLayer} is transmitting data.
     * @param transportLayerMessage
     *      request message containing the specific transport layer to see if it is transmitting.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      thrown when a message being parsed is invalid in some way
     */
    private Message isTransmitting(final TransportLayerNamespace transportLayerMessage, 
            final TerraHarvestMessage request, final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final IsTransmittingRequestData isTransmittingRequest = IsTransmittingRequestData.parseFrom(
                transportLayerMessage.getData());
        final TransportLayer requestedTransportLayer = CustomCommUtility.getTransportLayerByUuid(m_CommsService,
                isTransmittingRequest.getUuid());

        final IsTransmittingResponseData response = IsTransmittingResponseData.newBuilder().setIsTransmitting(
                requestedTransportLayer.isTransmitting()).build();

        m_MessageFactory.createTransportLayerResponseMessage(request, TransportLayerMessageType.IsTransmittingResponse,
                response).queue(channel);

        
        return isTransmittingRequest;
    }

    /**
     * Get whether or not the specified {@link TransportLayer} is receiving data.
     * @param transportLayerMessage
     *      request message containing the specific transport layer to see if it is transmitting.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      thrown when a message being parsed is invalid in some way 
     */
    private Message isReceiving(final TransportLayerNamespace transportLayerMessage, final TerraHarvestMessage request,
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final IsReceivingRequestData isReceivingRequest = IsReceivingRequestData.parseFrom(
                transportLayerMessage.getData());

        final TransportLayer requestedTransportLayer = CustomCommUtility.getTransportLayerByUuid(m_CommsService,
                isReceivingRequest.getUuid());

        final IsReceivingResponseData response = IsReceivingResponseData.newBuilder().setIsReceiving(
                requestedTransportLayer.isReceiving()).build();

        m_MessageFactory.createTransportLayerResponseMessage(request, TransportLayerMessageType.IsReceivingResponse,
                response).queue(channel);
        
        return isReceivingRequest;
    }
    
    /**
     * Method to remove a {@link TransportLayer}, given a UUID from the ccommsMessage.
     * @param transMessage
     *      request message containing the UUID of the transport layer to remove.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message removeTransportLayer(final TransportLayerNamespace transMessage, final TerraHarvestMessage request,
            final RemoteChannel channel) throws IOException
    {
        final DeleteRequestData removeTransportLayer = DeleteRequestData.parseFrom(transMessage.getData());

        final TransportLayer tLayer = CustomCommUtility.getTransportLayerByUuid(m_CommsService, 
                removeTransportLayer.getTransportLayerUuid());  
        tLayer.delete();
        
        m_MessageFactory.createTransportLayerResponseMessage(request, 
                TransportLayerMessageType.DeleteResponse, null).queue(channel);

        
        return removeTransportLayer;
    }

    /**
     * Method responsible for setting property(ies) on a transport layer based on the UUID.
     * 
     * @param message
     *  the set transport layer property request message containing the UUID of the transport layer and the 
     *  properties that are to be set
     * @param request
     *  the entire remote message for the request
     * @param channel
     *  the channel that was used to send the request
     * @return
     *  the data message for this request
     * @throws IOException
     *  if message cannot be parsed or if response message cannot be sent
     */
    private Message setTransportLayerProperty(final TransportLayerNamespace message, 
            final TerraHarvestMessage request, final RemoteChannel channel) throws IOException
    {
        final SetPropertyRequestData propRequest = SetPropertyRequestData.parseFrom(message.getData());
        final TransportLayer transportLayer =
                CustomCommUtility.getTransportLayerByUuid(m_CommsService, propRequest.getUuid());
        try
        {
            transportLayer.setProperties(SharedMessageUtils.convertListSimpleTypesMapEntrytoMapStringObject(
                                    propRequest.getPropertiesList()));
        }
        catch (final FactoryException exception)
        {
            final String errorDesc =
                    String.format("A transport layer with UUID [%s] could not update properties because: %s.",
                    SharedMessageUtils.convertProtoUUIDtoUUID(propRequest.getUuid()), exception.getMessage());
            m_Logging.error(exception, "Failed to set properties for transport layer %s.", transportLayer.getName());
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CCOMM_ERROR, errorDesc).queue(channel);
        }

        m_MessageFactory.createTransportLayerResponseMessage(request, 
                TransportLayerMessageType.SetPropertyResponse, null).queue(channel);

        return propRequest;
    }
}
