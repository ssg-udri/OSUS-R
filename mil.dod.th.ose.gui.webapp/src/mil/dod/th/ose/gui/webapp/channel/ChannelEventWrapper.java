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
package mil.dod.th.ose.gui.webapp.channel;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This event handler will listen for events that pertain to channels and produce
 * push related events.
 * @author nickmarcucci
 *
 */
@Startup
@Singleton
public class ChannelEventWrapper implements EventHandler
{
    /**
     * Topic prefix for channel topics.
     */
    public final static String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/channels/ChannelConstants/";
    
    /**
     * Topic for when a channel has been updated.
     */
    public final static String TOPIC_CHANNEL_UPDATED = TOPIC_PREFIX + "CHANNEL_UPDATED";
    
    /**
     * Topic for when a channel has been removed.
     */
    public final static String TOPIC_CHANNEL_REMOVED = TOPIC_PREFIX + "CHANNEL_REMOVED";
    
    /**
     * The service registration for this event listener.
     */
    @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
    private ServiceRegistration m_Registration;
    
    /**
     * Service that retrieves the bundle context of this bundle.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Inject the EventAdmin service for posting events.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * Set the Bundle Context utility service.
     * @param bundleUtil
     *     the bundle context utility service to use
     */
    public void setBundleContextUtility(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    /**
     * Set the {@link EventAdmin} service.
     * @param eventAdmin
     *      the event admin service to use.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Register for channel updated and removed events.
     */
    @PostConstruct
    public void init()
    {
        final BundleContext context = m_BundleUtil.getBundleContext();
        // register to listen to the Base:SystemInfo event
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        final String[] channelAddRemove = {RemoteChannelLookup.TOPIC_CHANNEL_UPDATED, 
            RemoteChannelLookup.TOPIC_CHANNEL_REMOVED};
        props.put(EventConstants.EVENT_TOPIC, channelAddRemove);
        
        //register the event handler that listens for system info responses
        m_Registration = context.registerService(EventHandler.class, this, props);
    }
    
    /**
     * Unregister the event listener.
     */
    @PreDestroy
    public void cleanup()
    {
        m_Registration.unregister();
    }
    
    /* (non-Javadoc)
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event)
    {
        final String topic = event.getTopic();
        
        if (topic.equals(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED))
        {
            final Event updated = new Event(ChannelEventWrapper.TOPIC_CHANNEL_UPDATED, new HashMap<String, Object>());
            m_EventAdmin.postEvent(updated);
        }
        else if (topic.equals(RemoteChannelLookup.TOPIC_CHANNEL_REMOVED))
        {
            final Event removed = new Event(ChannelEventWrapper.TOPIC_CHANNEL_REMOVED, new HashMap<String, Object>());
            m_EventAdmin.postEvent(removed);
        }
        
    }
    
}
