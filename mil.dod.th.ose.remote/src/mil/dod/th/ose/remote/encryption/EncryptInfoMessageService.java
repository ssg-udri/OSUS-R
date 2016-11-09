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
package mil.dod.th.ose.remote.encryption;

import java.io.IOException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoErrorResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This message service is responsible for handling messages that contain information specific to the encryption type
 * being used for remote communication.
 * 
 * @author cweisenborn
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { })
public class EncryptInfoMessageService implements MessageService
{
    /**
     * Reference to the event admin service.  Used for local messages within encryption information message service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Service that contains remote interface properties.
     */
    private RemoteSettings m_RemoteSettings;
    
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
     * Bind the service containing the remote interface properties.
     * 
     * @param remoteSettings
     *      service that contains remote interface properties.
     */
    @Reference
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        m_RemoteSettings = remoteSettings;
    }
    
    /**
     * Activate method binds the service to the message router. 
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
        return Namespace.EncryptionInfo;
    }

    @Override
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
            final RemoteChannel channel) throws IOException
    {
        final EncryptionInfoNamespace encryptInfoMessage = 
                EncryptionInfoNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
        
        switch (encryptInfoMessage.getType())
        {
            case GetEncryptionTypeRequest:
                dataMessage = null;
                sendEncryptionTypeResponse(message, channel);
                break;
            case GetEncryptionTypeResponse:
                dataMessage = GetEncryptionTypeResponseData.parseFrom(encryptInfoMessage.getData());
                break;
            case EncryptionInfoErrorResponse:
                dataMessage = EncryptionInfoErrorResponseData.parseFrom(encryptInfoMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the EncryptionInfo namespace.", encryptInfoMessage.getType()));
        }
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, encryptInfoMessage, 
                encryptInfoMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }

    /**
     * Method that is responsible for responding with the encryption level of the system.
     * 
     * @param request
     *      entire remote message for the request
     * @param channel
     *    the channel that was used to send request 
     */
    private void sendEncryptionTypeResponse(final TerraHarvestMessage request, final RemoteChannel channel)
    {       
        final EncryptType encryptionType = 
                EnumConverter.convertEncryptionModeToEncryptType(m_RemoteSettings.getEncryptionMode());
        final GetEncryptionTypeResponseData response = 
                GetEncryptionTypeResponseData.newBuilder().setType(encryptionType).build();
        m_MessageFactory.createEncryptionInfoResponseMessage(request, 
                EncryptionInfoMessageType.GetEncryptionTypeResponse, response).queue(channel);
    }
}
