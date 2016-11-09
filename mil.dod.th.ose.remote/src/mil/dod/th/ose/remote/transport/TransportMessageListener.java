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
package mil.dod.th.ose.remote.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportPacket;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.messaging.MessageRouter;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.api.RemoteSettings;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Reads in messages from a transport channel.  Will pass received message to a {@link MessageRouter} and sync with the
 * {@link mil.dod.th.core.remote.RemoteChannelLookup}.
 * @author bachmakm
 *
 */
@Component(factory = TransportMessageListener.FACTORY_NAME, provide = { })
public class TransportMessageListener implements EventHandler, Runnable
{
    /**
     * Name of the OSGi component factory, used for filtering.
     */
    public final static String FACTORY_NAME = "mil.dod.th.ose.remote.transport.TransportMessageListener";

    /**
     * Component property key containing the {@link TransportChannel} associated with this listener. 
     */
    public static final String CHANNEL_PROP_KEY = "channel";

    /**
     * Routes incoming messages.
     */
    private MessageRouter m_MessageRouter;
    
    /**
     * Whether the runner should continue.
     */
    private boolean m_Running = true;
    
    /** 
     * Queue to pass packets from the event handler to the read thread. 
     */
    private final LinkedBlockingQueue<TerraHarvestMessage> m_ReadPacketQueue = 
            new LinkedBlockingQueue<TerraHarvestMessage>();
    
    /**
     * Wrapper service for logging.
     */
    private LoggingService m_Logging;
    
    /**
     * Used to register/unregister an event handler during listener updates.  
     */
    private ServiceRegistration<EventHandler> m_ServiceRegistration;
    
    /**
     * Channel this class listens on.
     */
    private TransportChannel m_Channel;

    /**
     * OSGi Service used to post events.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * {@link mil.dod.th.core.system.TerraHarvestSystem} id of the remote system this message listener is listening to.
     */
    private Integer m_RemoteId;
    
    /**
     * Service contains current settings from config admin.
     */
    private RemoteSettings m_RemoteSettings;
    
    /**
     * Bind a message router to handle message read in from transport layer.
     * 
     * @param messageRouter
     *      router to handle incoming messages
     */
    @Reference
    public void setMessageRouter(final MessageRouter messageRouter)
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
     * Bind the service for posting events.
     * 
     * @param eventAdmin
     *      the service to bind for later use
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Bind the settings for the remote interface.
     * 
     * @param remoteSettings
     *      interface containing remote settings from config admin
     */
    @Reference
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        m_RemoteSettings = remoteSettings;
    }
    
    /**
     * Activate the listener.
     * 
     * @param props
     *      properties of the component including the string representations
     *      of the {@link Address} and {@link TransportLayer} 
     *      from which the message is being received
     * @param context
     *      bundle context object used to register {@link EventHandler}
     */
    @Activate
    public void activate(final Map<String, Object> props, final BundleContext context) 
    {
        m_Channel = (TransportChannel)props.get(CHANNEL_PROP_KEY);
        this.registerEventHandler(context);
    }
    
    /**
     * Deactivates component by unregistering event handler.  
     */
    @Deactivate
    public void deactivate()
    {
        this.m_Running = false;
        this.m_ServiceRegistration.unregister();
    }
    
    @Override
    public void handleEvent(final Event event)
    {
        
        TerraHarvestMessage message = null;
        try
        {
            final TransportPacket packet = (TransportPacket)event.getProperty(TransportLayer.EVENT_PROP_PACKET);
            if (packet.getPayload().capacity() > m_RemoteSettings.getMaxMessageSize())
            {
                final String msg = String.format("Message over Max Size: %s > %s",
                        packet.getPayload().capacity(), m_RemoteSettings.getMaxMessageSize());
                m_Logging.error(msg);
                                
                //Stop processing.  This should propagate to remove the channel and stop this thread.
                final Map<String, Object> properties = new HashMap<String, Object>();
                properties.put(RemoteConstants.EVENT_PROP_CHANNEL, m_Channel);
                m_EventAdmin.postEvent(new Event(RemoteConstants.TOPIC_REMOVE_CHANNEL, properties));                
            }
            else
            {
                final byte[] transportPacket = packet.getPayload().array(); 
                final ByteArrayInputStream bais = new ByteArrayInputStream(transportPacket);
                message = TerraHarvestMessage.parseDelimitedFrom(bais); //throws IOException
                this.m_ReadPacketQueue.put(message); //throws InterruptedException           
            }
        }
        catch (final IOException e) 
        {
            m_Logging.error(e, "Failed to parse incoming message.");
        }
        catch (final InterruptedException e) 
        {
            m_Logging.error(e, "Failed to add incoming message to queue.");
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public void run()
    {
        while (m_Running)
        {           
            try
            {
                //remove message at head of queue - method will block until there is a message in the queue
                final TerraHarvestMessage message = this.m_ReadPacketQueue.take(); //throws InterruptedException
                
                if (m_RemoteSettings.isLogRemoteMessagesEnabled())
                {
                    m_Logging.debug("Transport channel %s received remote message%n%s", m_Channel, 
                            message);
                }

                // send message on to router if parsed
                m_MessageRouter.handleMessage(message, m_Channel);

                // post about a new/updated id if never set or changed
                if (m_RemoteId == null || m_RemoteId == Integer.MAX_VALUE)
                {
                    m_Logging.debug("Message for transport listener is coming from a new id (%d), old was: %d", 
                            message.getSourceId(), m_RemoteId);
                    
                    m_RemoteId = message.getSourceId();
                    final Map<String, Object> properties = new HashMap<String, Object>();
                    properties.put(RemoteConstants.EVENT_PROP_CHANNEL, m_Channel);
                    properties.put(RemoteConstants.EVENT_PROP_SYS_ID, m_RemoteId);
                    m_EventAdmin.postEvent(new Event(RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID, properties));
                }
            }
            catch (final InterruptedException e) 
            {
                m_Logging.error(e, "Failed to remove message from head of queue.");
                return;
            }
        }
    }

    /**
     * Helper method for registering an event handler that is fired when a new message comes in through the remote 
     * channel.  
     * 
     * @param context
     *      context of bundles used to register {@link EventHandler}
     *      
     */
    private void registerEventHandler(final BundleContext context)
    {
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EventConstants.EVENT_TOPIC, TransportLayer.TOPIC_PACKET_RECEIVED);
        properties.put(EventConstants.EVENT_FILTER, 
                String.format("(&(%s=%s)(%s=%s)(%s=%s))",
                        // only get packet from one transport
                        FactoryDescriptor.EVENT_PROP_OBJ_NAME, 
                        m_Channel.getTransportLayerName(), 
                        // AND only get packet for one source address (remote address)
                        Address.EVENT_PROP_SOURCE_ADDRESS_PREFIX + Address.EVENT_PROP_MESSAGE_ADDRESS_SUFFIX, 
                        m_Channel.getRemoteMessageAddress(),
                        // AND only get packet for one destination address (local address)
                        Address.EVENT_PROP_DEST_ADDRESS_PREFIX + Address.EVENT_PROP_MESSAGE_ADDRESS_SUFFIX, 
                        m_Channel.getLocalMessageAddress())); 
        this.m_ServiceRegistration = context.registerService(EventHandler.class, this, properties);
    }
}
