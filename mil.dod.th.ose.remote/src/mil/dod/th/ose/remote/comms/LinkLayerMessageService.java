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
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.CustomCommsTypes;
import mil.dod.th.core.remote.proto.LinkLayerMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetMtuRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetMtuResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetPhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetPhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsAvailableRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsAvailableResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.PerformBITRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.PerformBITResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This class is responsible for managing Link Layer messages received through the remote interface and 
 * sending proper responses according to different incoming Link Layer request messages.  
 * @author matt
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) // NOCHECKSTYLE - Reached max class fan out complexity
                                            // needed to implement all link layer messages
public class LinkLayerMessageService implements MessageService
{
    /**
     * Used to log messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Local service for managing custom comms messages.
     */
    private CustomCommsService m_CommsService;
    
    /**
     * Reference to the event admin service.  Used for local messages within asset message service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Service to get the address object based on a given address name.
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
     * Bind the CustomCommsService.
     * @param customCommsService
     *      service for managing custom comms locally
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService customCommsService)
    {
        m_CommsService = customCommsService;
    }
    
    /**
     * Binds the {@link AddressManagerService} service.  
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
        return Namespace.LinkLayer;
    }

    @Override //NOCHECKSTYLE - Reached max cyclomatic complexity - needed to implement all link layer messages
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
              final RemoteChannel channel) throws IOException
    {
        final LinkLayerNamespace linkLayerMessage = LinkLayerNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;

        switch (linkLayerMessage.getType())
        {
            case ActivateRequest:
                dataMessage = activate(linkLayerMessage, message, channel);
                break;
            case ActivateResponse:
                dataMessage = null;
                break;
            case DeactivateRequest:
                dataMessage = deactivate(linkLayerMessage, message, channel);
                break;
            case DeactivateResponse:
                dataMessage = null;
                break;
            case GetStatusRequest:
                dataMessage = getStatus(linkLayerMessage, message, channel);
                break;
            case GetStatusResponse:
                dataMessage = GetStatusResponseData.parseFrom(linkLayerMessage.getData());
                break;
            case IsActivatedRequest:
                dataMessage = isActivated(linkLayerMessage, message, channel);
                break;
            case IsActivatedResponse:
                dataMessage = IsActivatedResponseData.parseFrom(linkLayerMessage.getData());
                break;
            case GetMtuRequest:
                dataMessage = getMtu(linkLayerMessage, message, channel);
                break;
            case GetMtuResponse:
                dataMessage = GetMtuResponseData.parseFrom(linkLayerMessage.getData());
                break;
            case IsAvailableRequest:
                dataMessage = isAvailable(linkLayerMessage, message, channel);
                break;
            case IsAvailableResponse:
                dataMessage = IsAvailableResponseData.parseFrom(linkLayerMessage.getData());
                break;
            case GetPhysicalLinkRequest:
                dataMessage = getPhysicalLink(linkLayerMessage, message, channel);
                break;
            case GetPhysicalLinkResponse:
                dataMessage = GetPhysicalLinkResponseData.parseFrom(linkLayerMessage.getData());
                break;
            case PerformBITRequest:
                dataMessage = performBIT(linkLayerMessage, message, channel);
                break;
            case PerformBITResponse:
                dataMessage = PerformBITResponseData.parseFrom(linkLayerMessage.getData());
                break;
            case DeleteRequest:
                dataMessage = deleteLinkLayer(linkLayerMessage, message, channel);
                break;
            case DeleteResponse:
                dataMessage = null;
                break;
            case SetPropertyRequest:
                dataMessage = setLinkLayerProperty(linkLayerMessage, message, channel);
                break;
            case SetPropertyResponse:
                dataMessage = null;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the LinkLayerMessageService namespace.", linkLayerMessage.getType()));
        }
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, linkLayerMessage, 
                linkLayerMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }

    /**
     * Get the physical link associated with the link layer.
     * @param linkLayerMessage
     *      request message containing the specific link layer the user wants.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      message being parsed is invalid in some way
     */
    private Message getPhysicalLink(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage request,
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final GetPhysicalLinkRequestData getPhysicalLinkRequest = GetPhysicalLinkRequestData.parseFrom(
                linkLayerMessage.getData());

        final LinkLayer requestedLinkLayer = CustomCommUtility.getLinkLayerByUuid(m_CommsService,
                getPhysicalLinkRequest.getUuid());

        final GetPhysicalLinkResponseData.Builder response = GetPhysicalLinkResponseData.newBuilder();
        
        final PhysicalLink pLink = requestedLinkLayer.getPhysicalLink();
        if (pLink != null)
        {
            response.setPhysicalLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(pLink.getUuid()));
        }
        
        m_MessageFactory.createLinkLayerResponseMessage(request, 
                LinkLayerMessageType.GetPhysicalLinkResponse, 
                response.build()).queue(channel);

        return getPhysicalLinkRequest;
    }

    /**
     * Check to see if an address is reachable from the specified link layer.
     * @param linkLayerMessage
     *      request message containing the specific link layer the user wants.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      message being parsed is invalid in some way
     */
    private Message isAvailable(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final IsAvailableRequestData isAvailableRequest = IsAvailableRequestData.parseFrom(linkLayerMessage.getData());
        
        final String address = isAvailableRequest.getAddress();
        
        try
        {
            final LinkLayer requestedLinkLayer = CustomCommUtility.getLinkLayerByUuid(
                    m_CommsService, isAvailableRequest.getUuid());
            
            final Address linkAddress = m_AddressManagerService.getOrCreateAddress(address);
            
            final IsAvailableResponseData response = IsAvailableResponseData.newBuilder().
                setAvailable(requestedLinkLayer.isAvailable(linkAddress)).
                setUuid(isAvailableRequest.getUuid()).build();
        
            m_MessageFactory.createLinkLayerResponseMessage(request, LinkLayerMessageType.IsAvailableResponse, 
                    response).queue(channel);
        }
        catch (final CCommException e)
        {
            m_Logging.error(e, "IsAvailable: Cannot get or create address.");
            
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CCOMM_ERROR, 
                   "Custom comms error when checking if address was available: " + e.getMessage()).queue(channel);
        }
        
        return isAvailableRequest;
    }
    
    /**
     * Get the maximum transmission unit that the link layer can send.
     * @param linkLayerMessage
     *      request message containing the specific link layer the user wants.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      message being parsed is invalid in some way
     */
    private Message getMtu(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final GetMtuRequestData getMtuRequest = GetMtuRequestData.parseFrom(linkLayerMessage.getData());

        final LinkLayer requestedLinkLayer = CustomCommUtility.getLinkLayerByUuid(
                m_CommsService, getMtuRequest.getUuid());

        final GetMtuResponseData response = GetMtuResponseData.newBuilder().setMtu(requestedLinkLayer.getMtu()).build();

        m_MessageFactory.createLinkLayerResponseMessage(request, LinkLayerMessageType.GetMtuResponse, response)
            .queue(channel);
        
        return getMtuRequest;
    }

    /**
     * Check whether the link layer specified is activated.
     * @param linkLayerMessage
     *      request message containing the specific link layer the user wants.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      message being parsed is invalid in some way
     */
    private Message isActivated(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final IsActivatedRequestData isActivatedRequest = IsActivatedRequestData.parseFrom(linkLayerMessage.getData());

        final LinkLayer requestedLinkLayer = CustomCommUtility.getLinkLayerByUuid(m_CommsService,
                isActivatedRequest.getUuid());

        final IsActivatedResponseData response = IsActivatedResponseData.newBuilder().setIsActivated(
                requestedLinkLayer.isActivated()).setUuid(isActivatedRequest.getUuid()).build();

        m_MessageFactory.createLinkLayerResponseMessage(request, LinkLayerMessageType.IsActivatedResponse, 
                response).queue(channel);
        
        return isActivatedRequest;
    }

    /**
     * Get the status of the specified link layer.
     * @param linkLayerMessage
     *      request message containing the specific link layer the user wants.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      message being parsed is invalid in some way
     */
    private Message getStatus(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final GetStatusRequestData getStatusRequest = GetStatusRequestData.parseFrom(linkLayerMessage.getData());

        final LinkLayer requestedLinkLayer = CustomCommUtility.getLinkLayerByUuid(
                m_CommsService, getStatusRequest.getUuid());

        final CustomCommsTypes.LinkStatus status = 
                EnumConverter.convertJavaLinkStatusToProto(requestedLinkLayer.getLinkStatus());

        final GetStatusResponseData response = GetStatusResponseData.newBuilder().setLinkStatus(status).setUuid(
                getStatusRequest.getUuid()).build();

        m_MessageFactory.createLinkLayerResponseMessage(request, LinkLayerMessageType.GetStatusResponse, 
                response).queue(channel);

        
        return getStatusRequest;
    }

    /**
     * Deactivate the specified link layer.
     * @param linkLayerMessage
     *      request message containing the specific link layer the user wants.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      message being parsed is invalid in some way 
     */
    private Message deactivate(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final DeactivateRequestData deactivateRequest = DeactivateRequestData.parseFrom(linkLayerMessage.getData());

        final LinkLayer lLayer = CustomCommUtility.getLinkLayerByUuid(m_CommsService, deactivateRequest.getUuid());
        lLayer.deactivateLayer();

        m_MessageFactory.createLinkLayerResponseMessage(request, LinkLayerMessageType.DeactivateResponse, null).
            queue(channel);

        
        return deactivateRequest;
    }

    /**
     * Activate the specified link layer.
     * @param linkLayerMessage
     *      request message containing the specific link layer the user wants.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request 
     * @throws InvalidProtocolBufferException
     *      message being parsed is invalid in some way 
     */
    private Message activate(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final ActivateRequestData activateRequest = ActivateRequestData.parseFrom(linkLayerMessage.getData());

        final LinkLayer lLayer = CustomCommUtility.getLinkLayerByUuid(m_CommsService, activateRequest.getUuid());
        lLayer.activateLayer();

        m_MessageFactory.createLinkLayerResponseMessage(request, LinkLayerMessageType.ActivateResponse, null).
            queue(channel);
        
        return activateRequest;
    }

    /**
     * Handle the request for a link layer to perform a BIT.
     * @param linkLayerMessage
     *     the message that contains the information needed to complete the perform BIT request
     * @param message
     *     the message that the request was contained within
     * @param channel
     *     the channel that the message came through
     * @return
     *     the parsed link layer request
     * @throws InvalidProtocolBufferException 
     *     message being parsed is invalid in some way 
     */
    private Message performBIT(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage message, 
        final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        //parse request
        final PerformBITRequestData request = PerformBITRequestData.parseFrom(linkLayerMessage.getData());
        
        //response
        final PerformBITResponseData.Builder response = PerformBITResponseData.newBuilder();

        //get the link layer
        try
        {
            final LinkLayer link = CustomCommUtility.getLinkLayerByUuid(m_CommsService, request.getLinkUuid());
            
            //request link to perform its BIT
            final CustomCommsTypes.LinkStatus status = 
                    EnumConverter.convertJavaLinkStatusToProto(link.performBit());
            response.setLinkUuid(request.getLinkUuid()).setPerformBitStatus(status);
        }
        catch (final CCommException e)
        {
            final String errorDesc = String.format("A link layer with UUID [%s] could not perform a BIT because: %s.",
                SharedMessageUtils.convertProtoUUIDtoUUID(request.getLinkUuid()), e.getMessage());
            m_MessageFactory.createBaseErrorMessage(message, ErrorCode.CCOMM_ERROR, errorDesc).queue(channel);

            //log error
            m_Logging.error(e, errorDesc);
            //exit method
            return request;
        }
        //send response
        m_MessageFactory.createLinkLayerResponseMessage(message, LinkLayerMessageType.PerformBITResponse, 
            response.build()).queue(channel);
        
        return request;
    }

    /**
     * Method to delete a {@link LinkLayer}, given a UUID.
     * @param linkLayerMessage
     *      request message containing the UUID of the link layer to delete.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent 
     */
    private Message deleteLinkLayer(final LinkLayerNamespace linkLayerMessage, final TerraHarvestMessage request,
        final RemoteChannel channel) throws IOException
    {
        final DeleteRequestData deleteLinkLayer = DeleteRequestData.parseFrom(
                linkLayerMessage.getData());
        
        final LinkLayer lLayer = CustomCommUtility.getLinkLayerByUuid(m_CommsService, 
                deleteLinkLayer.getLinkLayerUuid());
        lLayer.delete();
        
        m_MessageFactory.createLinkLayerResponseMessage(request, 
                LinkLayerMessageType.DeleteResponse, 
                null).queue(channel);
        return deleteLinkLayer;
    }

    /**
     * Method responsible for setting property(ies) on a link layer based on the UUID.
     * 
     * @param message
     *  the set link layer property request message containing the UUID of the link layer and the 
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
    private Message setLinkLayerProperty(final LinkLayerNamespace message, 
            final TerraHarvestMessage request, final RemoteChannel channel) throws IOException
    {
        final SetPropertyRequestData propRequest = SetPropertyRequestData.parseFrom(message.getData());
        final LinkLayer linkLayer = CustomCommUtility.getLinkLayerByUuid(m_CommsService, propRequest.getUuid());
        try
        {
            linkLayer.setProperties(SharedMessageUtils.convertListSimpleTypesMapEntrytoMapStringObject(
                                    propRequest.getPropertiesList()));
        }
        catch (final FactoryException exception)
        {
            final String errorDesc =
                    String.format("A link layer with UUID [%s] could not update properties because: %s.",
                    SharedMessageUtils.convertProtoUUIDtoUUID(propRequest.getUuid()), exception.getMessage());
            m_Logging.error(exception, "Failed to set properties for link layer %s.", linkLayer.getName());
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.CCOMM_ERROR, errorDesc).queue(channel);
        }

        m_MessageFactory.createLinkLayerResponseMessage(request, 
                LinkLayerMessageType.SetPropertyResponse, null).queue(channel);

        return propRequest;
    }
}
