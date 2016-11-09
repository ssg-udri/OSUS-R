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
package mil.dod.th.ose.remote.remoteconfiguration; 

import java.io.IOException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace.
    RemoteChannelLookupMessageType;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.SyncTransportChannelRequestData;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.SyncTransportChannelResponseData;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * This service handles messages used for setting and configuring transport layer channel remotely.
 * 
 * @author Pinar French
 *
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) 
public class RemoteChannelLookupMessageService implements MessageService 
{
    /**
     * Service to use for posting events by this component.
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
     * Lookup table used to get the transport layer channels to update or to add new channels to.
     */
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      event admin service to use to post events
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
     * Set the remote channel lookup to sync transport channels.
     * @param remoteChannelLookup
     *      used to sync transport channels
     */
    @Reference
    public void setRemoteChannelLookup(final RemoteChannelLookup remoteChannelLookup)
    {
        m_RemoteChannelLookup = remoteChannelLookup;
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
        return Namespace.RemoteChannelLookup;
    }
    
    @Override
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
        final RemoteChannel channel) throws IOException
    {
        final RemoteChannelLookupNamespace remoteChannelMessage =
                RemoteChannelLookupNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
     
        // message specific handling
        switch (remoteChannelMessage.getType())
        {
            case SyncTransportChannelRequest:
                dataMessage = syncTransportChannel(message, remoteChannelMessage, channel);
                break;
            case SyncTransportChannelResponse:
                dataMessage = SyncTransportChannelResponseData.parseFrom(remoteChannelMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the RemoteChannelLookupMessageService namespace.", remoteChannelMessage.getType()));
        }
        
        // post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, remoteChannelMessage, 
                remoteChannelMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }

    /**
     * Handle syncing a transport channel.
     * @param message
     *     the message containing the information needed to process the request
     * @param namespaceMessage
     *     the namespace message that contains the actual request message data
     * @param channel
     *     the channel that the request come through
     * @return
     *     the request message
     * @throws InvalidProtocolBufferException
     *      if there is a failure while sending the response message
     *     Terra Harvest controller capabilities
     */
    private Message syncTransportChannel(final TerraHarvestMessage message, 
            final RemoteChannelLookupNamespace namespaceMessage, final RemoteChannel channel) 
            throws InvalidProtocolBufferException
    {
        final SyncTransportChannelRequestData requestData = 
                SyncTransportChannelRequestData.parseFrom(namespaceMessage.getData());
        
        final TransportChannel newChannel = m_RemoteChannelLookup.syncTransportChannel(
                requestData.getTransportLayerName(), requestData.getDestSystemAddress(), 
                requestData.getRemoteSystemAddress(), requestData.getRemoteSystemId());
        
        final SyncTransportChannelResponseData responseData = SyncTransportChannelResponseData.newBuilder().
                setTransportLayerName(newChannel.getTransportLayerName()).
                setSourceSystemAddress(newChannel.getLocalMessageAddress()).// local address is source for response msg
                setRemoteSystemAddress(newChannel.getRemoteMessageAddress()).
                setRemoteSystemId(requestData.getRemoteSystemId()).
                build();
        
        m_MessageFactory.createRemoteChannelLookupResponseMessage(message,
                RemoteChannelLookupMessageType.SyncTransportChannelResponse, responseData).queue(channel);
        
        return requestData;
    }

}
