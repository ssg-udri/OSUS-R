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
package mil.dod.th.ose.gui.webapp.utils.push;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;


/**
 * This class is the central "hub" for distributing THOSE GUI events via PrimeFaces 
 * Push to browser clients.
 * 
 * @author nickmarcucci
 *
 */
@Startup
@Singleton
public class PushChannelsGenericEventHandler implements EventHandler
{
    /**
     * Service registration for the listener service. Saved for unregistering the service when the bean is 
     * destroyed.
     */
    @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
    private ServiceRegistration m_Registration;
    
    /**
     * Utility class used to retrieve the bundle context.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Utility service that is used to push messages.
     */
    @Inject
    private PushChannelMessageManager m_PushManager;
    
    /**
     * Sets the bundle context utility to use.
     * 
     * @param bundleUtil
     *          Bundle context utility to set.
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    /**
     * Set the push message manager to use.
     * @param manager
     *  the push message manager service to use
     */
    public void setPushManager(final PushChannelMessageManager manager)
    {
        m_PushManager = manager;
    }
    
    /**
     * Register event handlers to subscribe to events that 
     * can cause PrimeFaces Push notification.
     */
    @PostConstruct
    public void setup()
    {
        registerAllPushChannelEvents();
    }
    
    /**
     * Unregisters event handler on destroy of the bean.
     */
    @PreDestroy
    public void cleanup()
    {
        unregisterListener();
    }
    
    @Override
    public void handleEvent(final Event event)
    {
        final String topic = event.getTopic();
        
        final Map<String, Object> props = new HashMap<>();
        for (String key : event.getPropertyNames())
        {
            props.put(key, event.getProperty(key));
        }
        
        //push over the generic event channel
        final PushEventMessage msg = new PushEventMessage(topic, props);
        
        m_PushManager.addMessage(msg);
    }
    
    /**
     * Method that registers configuration events to listen for.
     */
    private void registerAllPushChannelEvents()
    {
        final BundleContext context = m_BundleUtil.getBundleContext();
        
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, "mil/dod/th/ose/gui/webapp/*");
        
        m_Registration = context.registerService(EventHandler.class, this, props);
    }
    
    /**
     * Method unregisters event registration.
     */
    private void unregisterListener()
    {
        m_Registration.unregister();
    }

}
