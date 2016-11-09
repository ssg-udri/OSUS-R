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
package example.message;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageRouter;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Thread that listens on a socket for {@link TerraHarvestMessage}s and injects them into the core using methods in 
 * {@link MessageRouter}.
 * 
 * @author jlatham
 *
 */
@Component(factory = SocketListener.FACTORY)
public class SocketListener implements Runnable
{
    /**
     * String for the socket listener factory.
     */
    public static final String FACTORY = "example.message.SocketListener";
    
    /**
     * String for the socket initialization property.
     */
    public static final String PROP_SOCKET = "prop.socket";
    
    /**
     * String for the system id initialization property.
     */
    public static final String PROP_SYSTEMID = "prop.system.id";
    
    private AtomicBoolean m_Running;
    private Socket m_Socket;
    private long m_SystemId;
    private MessageRouter m_MessageRouter;
    private RemoteChannelLookup m_RemoteChannelLookup;
    private LoggingService m_Logger;
    private Map<Integer, List<Integer>> m_ResponseMap;
    private UnreachableEventHandler m_SendHandler;
    
    /**
     * Socket listener activate method.
     * 
     * @param context
     *      Bundle context the socket listener is associated with.
     * @param props
     *      Map of properties to be passed to the socket listener upon startup.
     */
    @Activate
    public void activate(final BundleContext context, final Map<String, Object> props)
    {
        m_Running = new AtomicBoolean(false);
        m_ResponseMap = new HashMap<>();
        m_Socket = (Socket)props.get(PROP_SOCKET);
        m_SendHandler = new UnreachableEventHandler();
        m_SendHandler.registerForEvents(context);
        
        final byte[] id = new byte[Long.BYTES];
        try
        {
            m_Socket.getInputStream().read(id, 0, Long.BYTES);
        }
        catch (final IOException ex)
        {
            m_Logger.error(ex, "Unable to read in system ID");
            return;
        }
        final ByteBuffer buffer = ByteBuffer.wrap(id);
        m_SystemId = buffer.getLong();
        
        m_Logger.debug("Received system ID from: %s", m_SystemId);
    }
    
    @Reference
    public void setMessageRouter(final MessageRouter router)
    {
        m_MessageRouter = router;
    }
    
    @Reference
    public void setRemoteChannelLookup(final RemoteChannelLookup lookup)
    {
        m_RemoteChannelLookup = lookup;
    }
    
    @Reference
    public void setLoggingService(final LoggingService logger)
    {
        m_Logger = logger;
    }

    public long getSystemId()
    {
        return m_SystemId;
    }
    
    @Override
    public void run()
    {
        try
        {
            m_Running.set(true);

            while (m_Running.get())
            {
                final TerraHarvestMessage thmessage;
                try
                {
                    thmessage = TerraHarvestMessage.parseDelimitedFrom(m_Socket.getInputStream());
                }
                catch (final IOException ex)
                {
                    continue;
                }
                
                if (thmessage == null)
                {
                    continue;
                }
                
                final int destId = thmessage.getDestId();
                
                m_Logger.debug("Received message from system ID [%s] for system ID [%s]. Message type: [%s]", 
                        thmessage.getSourceId(), 
                        destId, 
                        TerraHarvestPayload.parseFrom(thmessage.getTerraHarvestPayload(
                                )).getNamespace());                

                try
                {
                    m_Logger.debug("Looking up remote channel for destId [%s] ", destId);
                    final RemoteChannel channel = m_RemoteChannelLookup.getChannel(destId);
                    m_MessageRouter.handleMessage(thmessage, channel);          
                }
                catch (final IllegalArgumentException ex)
                {
                    // Could not find remote channel
                    m_Logger.debug("Could not find a remote channel for destId [%s]", destId);
                    if (!thmessage.getIsResponse() && m_SystemId != thmessage.getSourceId())
                    {
                        m_Logger.debug("Adding system ID [%s] and message ID [%s] to response map", 
                                thmessage.getSourceId(), thmessage.getMessageId());
                        if (!m_ResponseMap.containsKey(thmessage.getSourceId()))
                        {
                            m_ResponseMap.put(thmessage.getSourceId(), new ArrayList<>());
                        }
                        m_ResponseMap.get(thmessage.getSourceId()).add(thmessage.getMessageId());
                    }
                    m_MessageRouter.handleMessage(thmessage, null);          
                }                    
                     
            }

        }
        catch (final Exception ex)
        {
            m_Logger.error(ex, "Exception while listening for remote message");
        }
        finally
        {
            try
            {
                m_Socket.close();
            }
            catch (final IOException ex)
            {
                m_Logger.error(ex, "Error closing client socket");
            }
        }

        m_Logger.debug("Exiting socket listener thread [%s] for socket: %s", Thread.currentThread().getName(), 
                m_Socket.getInetAddress());
    }
    
    /**
     * Method that shuts down the socket listener.
     */
    public void shutdown()
    {
        m_Running.set(false);
    }
    
    /**
     * Method that sends the specified message.
     * 
     * @param message
     *      Message to be sent by the socket listener.
     * @throws IOException
     *      If an exception occurs sending the message.
     */
    private void sendMessage(final TerraHarvestMessage message) throws IOException
    {
        message.writeDelimitedTo(m_Socket.getOutputStream());
    } 

    /**
     * Event handler that listens for message send/receive unreachable events.
     */
    class UnreachableEventHandler implements EventHandler
    {
        private ServiceRegistration<EventHandler> m_Registration;
        
        /**
         * Method that registers the event handler for send/receive unreachable events.
         * 
         * @param context
         *      Bundle context to register the event handler with.
         */
        public void registerForEvents(final BundleContext context)
        {
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] topics = {RemoteConstants.TOPIC_MESSAGE_SEND_UNREACHABLE_DEST, 
                RemoteConstants.TOPIC_MESSAGE_RECEIVED_UNREACHABLE_DEST };
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            m_Logger.debug("Received event. Topic: %s", event.getTopic());
            final TerraHarvestMessage thmessage = 
                    (TerraHarvestMessage)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE);
            final int destId = thmessage.getDestId();
            final int messageId = thmessage.getMessageId();
            m_Logger.debug("Message destination: [%s], message ID: [%s], socket system ID: [%s], response map: [%s]", 
                    destId, messageId, m_SystemId, m_ResponseMap.toString());
            final String sendError = "Error sending response message to remote system with ID: [%s]";
            if (destId == m_SystemId)
            {
                try
                {
                    sendMessage(thmessage);
                }
                catch (final IOException ex)
                {
                    m_Logger.error(ex, sendError, destId);
                }
            }
            else if (thmessage.getIsResponse() 
                    && m_ResponseMap.containsKey(destId) 
                    && m_ResponseMap.get(destId).contains(messageId))
            {
                try
                {
                    sendMessage(thmessage);
                    m_ResponseMap.get(destId).remove(Integer.valueOf(messageId));
                }
                catch (final IOException ex)
                {
                    m_Logger.error(ex, sendError, destId);
                }
            }
        }
        
        /**
         * Method that unregisters the event handler.
         */
        public void unregisterForEvents()
        {
            if (m_Registration != null)
            {
                m_Registration.unregister();
            }
        }
    }
}
