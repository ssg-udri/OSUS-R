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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageSender;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.ose.remote.EventChannel;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.encryption.EncryptMessageService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

/**
 * Implementation of the {@link MessageSender}.
 * @author allenchl
 *
 */
@Component
public class MessageSenderImpl implements MessageSender
{
    /**
     * Service to lookup remote channels.
     */
    private RemoteChannelLookup m_RemoteChannelLookup;

    /**
     * Service for this instance of a Terra Harvest system.
     */
    private TerraHarvestSystem m_TerraHarvestSystem;
    
    /**
     * Next message id to use for requests.  Responses will use the same message id as the request. It will be unique
     * for each route attempted to send the message.  For a single message, the message id will be different when 
     * attempted to send (trySendX) versus if the message is queued (queueX).
     */
    private int m_NextRequestMessageId;
    
    /**
     * Map of message ids, and {@link ResponseHandler}s that are to be called when the response message id 
     * matches the request id.
     */
    private final Map<Integer, ResponseHandler> m_Handlers = 
        Collections.synchronizedMap(new LinkedHashMap<Integer, ResponseHandler>());

    /**
     * Class to handle response messages that have a defined handler from the request.
     */
    private ResponseEventHandler m_ResponseEventHandler;
    
    /**
     * Used for logging messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Encryption service.
     */
    private EncryptMessageService m_EncryptService;
    
    /**
     * Remote system encryption service, optionally utilized to store the encryption level of remote systems.
     */
    private RemoteSystemEncryption m_RemoteSystemEncryption;
    
    /**
     * Remote settings service.
     */
    private RemoteSettings m_RemoteSettings;
    private EventAdmin m_EventAdmin;
    
    /**
     * Bind the service to lookup remote channels.
     * 
     * @param remoteChannelLookup
     *      service to use for looking up channels
     */
    @Reference
    public void setRemoteChannelLookup(final RemoteChannelLookup remoteChannelLookup)
    {
        m_RemoteChannelLookup = remoteChannelLookup;
    }

    /**
     * Bind the service.
     * 
     * @param terraHarvestSystem
     *      service to use for querying about the system
     */
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem terraHarvestSystem)
    {
        m_TerraHarvestSystem = terraHarvestSystem;
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
     * Bind the encryption service.
     * 
     * @param encryptionService
     *      service to use for unencrypting messages
     */
    @Reference
    public void setEncryptMessageService(final EncryptMessageService encryptionService)
    {
        m_EncryptService = encryptionService;
    }
    
    /**
     * Binds the remote system encryption service to be used by this component.
     * 
     * @param remoteSystemEncryption
     *      the remote system encryption service to use, if available
     */
    @Reference(dynamic = true, optional = true)
    public void setRemoteSystemEncryption(final RemoteSystemEncryption remoteSystemEncryption)
    {
        m_RemoteSystemEncryption = remoteSystemEncryption;
    }
    
    /**
     * Binds the event admin service to be used by this component.
     * 
     * @param eventAdmin
     *      the event admin service to use
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Unbinds the remote system encryption service to be used by this component.
     * 
     * @param remoteSystemEncryption
     *      parameter not used, must match binding method signature
     */
    public void unsetRemoteSystemEncryption(final RemoteSystemEncryption remoteSystemEncryption)
    {
        m_RemoteSystemEncryption = null; //NOPMD setting to null because the reference is no longer valid
    }
    
    /**
     * Binds the remote settings service to be used.
     * 
     * @param remoteSettings
     *      the remote settings service to use
     */
    @Reference
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        m_RemoteSettings = remoteSettings;
    }
    
    /**
     * Register the event handler.
     * @param context
     *     the bundle context used to register the event handler with the framework
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_ResponseEventHandler = new ResponseEventHandler(context);
        m_ResponseEventHandler.registerMessageReceivedEvent();
    }

    /**
     * Unregister the event handler.
     */
    @Deactivate
    public void deactivate()
    {
        m_ResponseEventHandler.unregisterServiceRegistration();
    }

    @Override
    public boolean trySendMessage(final int destId, final TerraHarvestPayload payload, final EncryptType encryptType)
            throws IllegalArgumentException
    {
        final TerraHarvestMessage builtMessage = 
            createCompleteMessage(destId, payload, encryptType, false, m_NextRequestMessageId++);
        final List<RemoteChannel> channels = m_RemoteChannelLookup.getChannels(builtMessage.getDestId());
        if (channels.isEmpty())
        {
            final EventChannel evnetChannel = new EventChannel(destId, m_EventAdmin);
            evnetChannel.trySendMessage(builtMessage);
            throw new IllegalArgumentException(String.format("Invalid destination id 0x%08X, channel not found", 
                    builtMessage.getDestId())); 
        }
        for (RemoteChannel channel : channels)
        {
            if (channel.trySendMessage(builtMessage))
            {
                // successfully sent, exit out
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean trySendMessage(final int destId, final TerraHarvestPayload payload) throws IllegalArgumentException,
            IllegalStateException
    {
        final EncryptType encryptionLevel = determineEncryptionLevel(destId, payload);
        return trySendMessage(destId, payload, encryptionLevel);
    }

    @Override
    public boolean queueMessage(final int destId, final TerraHarvestPayload payload, final EncryptType encryptType,
        final ResponseHandler handler) throws IllegalArgumentException
    {
        //capture the message ID
        int messageIdForHandler = m_NextRequestMessageId;
        if (handler != null)
        {
            regResponseHandler(messageIdForHandler, handler, payload.getNamespace());
        }
        if (trySendMessage(destId, payload, encryptType))
        {
            // successfully sent message
            return true;
        }
        m_Handlers.remove(messageIdForHandler);
        m_Logging.debug("Handler removed, failed 'try send', for namespace %s, message id: %d", 
                payload.getNamespace(), messageIdForHandler);
        //recapture the message ID
        messageIdForHandler = m_NextRequestMessageId;
        if (handler != null)
        {
            regResponseHandler(messageIdForHandler, handler, payload.getNamespace());
        }
        final TerraHarvestMessage builtMessage = 
                createCompleteMessage(destId, payload, encryptType, false, m_NextRequestMessageId++);
        // send failed, so queue instead
        final RemoteChannel channel = m_RemoteChannelLookup.getChannel(destId);
        if (!channel.queueMessage(builtMessage))
        {
            //queue failed remove the handler
            removeResponseHandler(messageIdForHandler, payload.getNamespace());
            return false;
        }
        //queue worked
        return true;
    }
    
    @Override
    public boolean queueMessage(final int destId, final TerraHarvestPayload payload, final ResponseHandler handler) 
            throws IllegalArgumentException, IllegalStateException
    {
        final EncryptType encryptionLevel = determineEncryptionLevel(destId, payload);
        return queueMessage(destId, payload, encryptionLevel, handler);
    }
    
    @Override
    public boolean queueMessage(final RemoteChannel channel, final TerraHarvestPayload payload, 
            final EncryptType encryptType, final ResponseHandler handler)
    {
        final int messageIdForHandler = m_NextRequestMessageId;
        if (handler != null)
        {
            regResponseHandler(messageIdForHandler, handler, payload.getNamespace());
        }
        
        final int destId;
        RemoteChannel destChannel = channel;
        if (channel instanceof EventChannel)
        {
            destId = ((EventChannel)channel).getRemoteSystemId();
            //Verify that there is no remote channel to the destination system.
            if (!m_RemoteChannelLookup.getChannels(destId).isEmpty())
            {
                destChannel = m_RemoteChannelLookup.getChannel(destId);
            }
        }
        else
        {
            destId = m_RemoteChannelLookup.getChannelSystemId(channel);
        }
        
        final TerraHarvestMessage builtMessage = 
                createCompleteMessage(destId, payload, encryptType, false, 
                        m_NextRequestMessageId++);
        if (!destChannel.queueMessage(builtMessage))
        {
            //queue failed remove the handler
            removeResponseHandler(messageIdForHandler, payload.getNamespace());
            return false;
        }
        //queue worked
        return true;
    }
    
    @Override
    public boolean queueMessage(final RemoteChannel channel, final TerraHarvestPayload payload, 
            final ResponseHandler handler) throws IllegalStateException
    {
        final EncryptType encryptionLevel = determineEncryptionLevel(m_RemoteChannelLookup.getChannelSystemId(channel), 
                payload);
        return queueMessage(channel, payload, encryptionLevel, handler);
    }

    @Override
    public boolean queueMessageResponse(final TerraHarvestMessage request, final TerraHarvestPayload payload, 
        final RemoteChannel channel)
    {
        final int destId;
        RemoteChannel destChannel = channel;
        if (channel instanceof EventChannel)
        {
            destId = ((EventChannel)channel).getRemoteSystemId();
            //Verify that there is no remote channel to the destination system.
            if (!m_RemoteChannelLookup.getChannels(destId).isEmpty())
            {
                destChannel = m_RemoteChannelLookup.getChannel(destId);
            }
        }
        else
        {
            final int channelId = m_RemoteChannelLookup.getChannelSystemId(channel);
            if (channelId == request.getSourceId())
            {
                destId = channelId;
            }
            else
            {
                destId = request.getSourceId();
            }
        }
        
        // send failed, so queue instead
        final TerraHarvestMessage builtMessage = 
            createCompleteMessage(destId, payload, request.getEncryptType(), true, 
                request.getMessageId());
        // queue message
        return destChannel.queueMessage(builtMessage);
    }

    /**
     * Get the number of response handlers currently known to this service.
     * @return
     *     the number of registered response handlers known
     */
    @Override
    public int getResponseHandleRegCount()
    {
        return m_Handlers.size();
    }
    
    /**
     * Method that checks the encryption level of the remote system the message being sent to versus the local system 
     * and returns the highest encryption level of the two or none if the message belongs to the 
     * {@link Namespace#EncryptionInfo}.
     * 
     * @param destId
     *      ID of the system the message is being sent to.
     * @param payload
     *      {@link TerraHarvestPayload} that the message is sending. Used to determine if the namespace of the message
     *      is of the encryption info type.
     * @return
     *      {@link EncryptType} that represents the local system encryption level.
     */
    private EncryptType determineEncryptionLevel(final int destId, final TerraHarvestPayload payload)
    {
        final Namespace namespace = payload.getNamespace();
        if (m_RemoteSystemEncryption == null && namespace != Namespace.EncryptionInfo)
        {
            throw new IllegalStateException(String.format("The message for Namespace %s cannot be queued without the " 
                    + "encryption type included because there is not a RemoteSystemEncryption service available.", 
                        namespace));
        }
        else if (namespace == Namespace.EncryptionInfo)
        {
            return EncryptType.NONE;
        }
        
        final EncryptType sysEncryptType = 
                EnumConverter.convertEncryptionModeToEncryptType(m_RemoteSettings.getEncryptionMode());
        final EncryptType remoteEncryptType = m_RemoteSystemEncryption.getEncryptType(destId);
        if (remoteEncryptType == null)
        {
            throw new IllegalArgumentException(String.format("Encryption type is not known for system with id 0x%08X", 
                    destId));
        }
        
        if (sysEncryptType.getNumber() < remoteEncryptType.getNumber())
        {
            return remoteEncryptType;
        }

        return sysEncryptType;
    }
    
    /**
     * Construct a complete {@link TerraHarvestMessage}.
     * @param destId
     *     the system ID of the destination system
     * @param payload
     *     the payload message
     * @param encryptType
     *     the type of encryption that the message should be processed with
     * @param isResponse
     *     denotes that this message is a response
     * @param messageId
     *     the message ID to assign to the message
     * @return message
     *     built message with all required data.
     */
    private TerraHarvestMessage createCompleteMessage(final int destId, final TerraHarvestPayload payload, 
        final EncryptType encryptType, final boolean isResponse, final int messageId)
    {
        // build a terra harvest message
        final TerraHarvestMessage.Builder builderMessage = TerraHarvestMessage.newBuilder().
                setVersion(RemoteConstants.SPEC_VERSION).
                setSourceId(m_TerraHarvestSystem.getId()).
                setDestId(destId).
                setMessageId(messageId).
                setEncryptType(encryptType);
        if (isResponse)
        {
            builderMessage.setIsResponse(true);
        }
        return m_EncryptService.encryptMessage(builderMessage, payload);
    }
    
    /**
     * Store a {@link ResponseHandler}.
     * @param messageId
     *     the message ID that will be used for later lookup
     * @param handler
     *     the actual response handler to store
     * @param namespace
     *     the namespace for which the handler will be called for
     */
    private void regResponseHandler(final int messageId, final ResponseHandler handler, final Namespace namespace)
    {
        m_Handlers.put(messageId, handler);
        m_Logging.debug("Handler registered for namespace %s, message id: %d", namespace, 
            messageId);
    }
    
    /**
     * Remove a stored {@link ResponseHandler}.
     * @param messageId
     *      ID of the message the response handler belongs to
     * @param namespace
     *      The namespace for which the handler belongs to
     */
    private void removeResponseHandler(final int messageId, final Namespace namespace)
    {
        m_Handlers.remove(messageId);
        m_Logging.debug("Handler removed, failed 'queue' for namespace %s, message id: %d", 
            namespace, messageId);
    }
    
    /**
     * Inner event listener used to handle {@link RemoteConstants#TOPIC_MESSAGE_RECEIVED} events where the
     * request registered a handler.
     */
    class ResponseEventHandler implements EventHandler
    {
        /**
         * The service registration object for the registered event.
         */
        private ServiceRegistration<EventHandler> m_ServiceReg;
        
        /**
         * The OSGi bundle context for this bundle.
         */
        private final BundleContext m_Context;
        
        /**
         * Public constructor for the event handler.
         * @param context
         *   the bundle context used to register the event handler   
         */
        ResponseEventHandler(final BundleContext context)
        {
            m_Context = context;
        }

        @Override
        public void handleEvent(final Event event)
        {
            final TerraHarvestMessage thMessage = (TerraHarvestMessage)event.
                getProperty(RemoteConstants.EVENT_PROP_MESSAGE);
            
            final TerraHarvestPayload payload = (TerraHarvestPayload)event.
                getProperty(RemoteConstants.EVENT_PROP_PAYLOAD);
            
            final int messageId = thMessage.getMessageId();
            final ResponseHandler handler = m_Handlers.remove(messageId);
            if (handler != null)
            {
                m_Logging.log(LogService.LOG_DEBUG, "Response handler called for a %s namespace message, with message"
                    + " id %d", payload.getNamespace(), messageId);
                final Message namespaceMessage = (Message)event.getProperty(
                    RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE);
                final Message data = (Message)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
                //call the handlers handle method
                handler.handleResponse(thMessage, payload, namespaceMessage, data);
            }
        }
        
        /**
         * Method to register this event handler for the message received event.
         */
        public void registerMessageReceivedEvent()
        {
            //dictionary of properties for the event handler for TOPIC_MESSAGE_RECEIVED
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            props.put(EventConstants.EVENT_FILTER, String.format("(%s=%s)", 
                    RemoteConstants.EVENT_PROP_MESSAGE_RESPONSE, true));
            
            //register the event handler
            m_ServiceReg = m_Context.registerService(EventHandler.class, this, props);
        }
        
        /**
         * Method to unregister the service registration for the registered event.
         */
        public void unregisterServiceRegistration()
        {
            m_ServiceReg.unregister();
        }
    }
}
