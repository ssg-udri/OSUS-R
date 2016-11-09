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
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsInUseRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsInUseResponseData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsOpenRequestData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.IsOpenResponseData;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace.PhysicalLinkMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This class is responsible for managing Physical Link messages received through the remote interface and 
 * sending proper responses according to different incoming Physical Link request messages.  
 * @author matt
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { })
public class PhysicalLinkMessageService implements MessageService
{
    /**
     * Local service for managing custom comms messages.
     */
    private CustomCommsService m_CommsService;
    
    /**
     * Reference to the event admin service.  Used for local messages within physical link message service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;

    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;
    
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
        return Namespace.PhysicalLink;
    }

    @Override
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
        final RemoteChannel channel) throws IOException
    {
        final PhysicalLinkNamespace physicalLinkMessage = PhysicalLinkNamespace.
              parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;

        switch (physicalLinkMessage.getType())
        {
            case IsOpenRequest:
                dataMessage = isOpen(physicalLinkMessage, message, channel);
                break;
            case IsOpenResponse:
                dataMessage = IsOpenResponseData.parseFrom(physicalLinkMessage.getData());
                break;
            case IsInUseRequest:
                dataMessage = isPhysicalLinkInUse(physicalLinkMessage, message, channel);
                break;
            case IsInUseResponse:
                dataMessage = IsInUseResponseData.parseFrom(physicalLinkMessage.getData());
                break;
            case DeleteRequest:
                dataMessage = removePhysicalLink(physicalLinkMessage, message, channel);
                break;
            case DeleteResponse:
                dataMessage = null;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the PhysicalLinkMessageService namespace.", physicalLinkMessage.getType()));
        }
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, physicalLinkMessage, 
                physicalLinkMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }

    /**
     * Check to see if the physical link that the user passed in with the message is opened or not.
     * @param physicalLinkMessage
     *      request message containing the specific physical link the user wants to see if it is open.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws InvalidProtocolBufferException
     *      thrown if a protocol message being parsed is invalid in some way.
     */
    private Message isOpen(final PhysicalLinkNamespace physicalLinkMessage, final TerraHarvestMessage request, 
            final RemoteChannel channel) throws InvalidProtocolBufferException
    {
        final IsOpenRequestData isOpenRequest = IsOpenRequestData.parseFrom(physicalLinkMessage.getData());
        final UUID pLinkUuid = SharedMessageUtils.convertProtoUUIDtoUUID(isOpenRequest.getUuid());

        final String pLinkName = m_CommsService.getPhysicalLinkName(pLinkUuid);
        final IsOpenResponseData response = IsOpenResponseData.newBuilder().setIsOpen(
                m_CommsService.isPhysicalLinkOpen(pLinkName)).build();

        m_MessageFactory.createPhysicalLinkResponseMessage(request, PhysicalLinkMessageType.IsOpenResponse, 
                response).queue(channel);

        return isOpenRequest;
    }
    
    /**
     * Method to remove a {@link mil.dod.th.core.ccomm.physical.PhysicalLink}, given a UUID from the ccommsMessage.
     * @param physicalLinkMessage
     *      request message containing the UUID of the physical link to remove.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message removePhysicalLink(final PhysicalLinkNamespace physicalLinkMessage, 
        final TerraHarvestMessage request, final RemoteChannel channel) throws IOException
    {
        final DeleteRequestData removePhysicalLink = DeleteRequestData.parseFrom(physicalLinkMessage.getData());

        final UUID physicalUuid = SharedMessageUtils.convertProtoUUIDtoUUID(
                removePhysicalLink.getPhysicalLinkUuid());
        
        final String errorMessage = "RemovePhysicalLink: Cannot remove physical link. ";
        try
        {
            final String pLinkName = m_CommsService.getPhysicalLinkName(physicalUuid);
            m_CommsService.deletePhysicalLink(pLinkName);
            m_MessageFactory.createPhysicalLinkResponseMessage(request, 
                PhysicalLinkMessageType.DeleteResponse, null).queue(channel);
        }
        catch (final CCommException e)
        {
            m_Logging.error(e, errorMessage);
            
            m_MessageFactory.createBaseErrorMessage(request, 
                    ErrorCode.CCOMM_ERROR, errorMessage + e.getMessage()).queue(channel);
        }
        return removePhysicalLink;
    }
    
    /**
     * Method to see if a {@link mil.dod.th.core.ccomm.physical.PhysicalLink} is in use or not.
     * @param physicalLinkMessage
     *      request message containing the UUID of the physical link to find if it is in use.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed or response message cannot be sent
     */
    private Message isPhysicalLinkInUse(final PhysicalLinkNamespace physicalLinkMessage, 
            final TerraHarvestMessage request, final RemoteChannel channel) throws IOException
    {
        final IsInUseRequestData isPhysicalLinkInUse = IsInUseRequestData.parseFrom(physicalLinkMessage.getData());
        
        final UUID physicalUuid = SharedMessageUtils.convertProtoUUIDtoUUID(
                isPhysicalLinkInUse.getPhysicalLinkUuid());
        
        final IsInUseResponseData response = IsInUseResponseData.newBuilder().
                setIsInUse(m_CommsService.isPhysicalLinkInUse(
                        m_CommsService.getPhysicalLinkName(physicalUuid))).build();
        
        m_MessageFactory.createPhysicalLinkResponseMessage(request, PhysicalLinkMessageType.IsInUseResponse, 
                response).queue(channel);
        
        return isPhysicalLinkInUse;
    }
}
