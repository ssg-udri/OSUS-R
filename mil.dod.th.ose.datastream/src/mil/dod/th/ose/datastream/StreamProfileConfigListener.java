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
package mil.dod.th.ose.datastream;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Service that listens for stream profile configuration to be created and will create a stream profile object if none
 * is associated with the configuration.
 * 
 * @author cweisenborn
 */
@Component(provide = StreamProfileConfigListener.class)
public class StreamProfileConfigListener
{
    /**
     * Topic that is posted whenever a stream profile configuration is updated.
     * 
     * Contains the same properties as the topic {@link ConfigurationEventConstants#TOPIC_CONFIGURATION_UPDATED_EVENT}.
     */
    public static final String TOPIC_STREAM_PROFILE_CONFIG_UPDATED = 
            "mil/dod/th/ose/datastream/StreamProfileConfigListener/STREAM_PROFILE_CONFIG_UPDATED";
    
    private EventAdmin m_EventAdmin;
    private BundleContext m_Context;
    
    /**
     * Map of registered configuration listeners. The key is the factory PID the configuration listener is registered
     * to listen for and the value is the configuration listener.
     */
    private Map<String, ConfigurationListener> m_ConfigListenerMap;
    
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Service activate method.
     * 
     * @param context
     *      Bundle context the service is associated with.
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
        m_ConfigListenerMap = new HashMap<>();
    }
    
    /**
     * Deactivation method the unregisters all registered configuration listeners.
     */
    @Deactivate
    public void deactivate()
    {
        for (ConfigurationListener configListener : m_ConfigListenerMap.values())
        {
            configListener.unregsiterEvents();
        }
    }
    
    /**
     * Registers a configuration listeners for the stream profile factory with the specified PID.
     * 
     * @param factoryPid
     *      PID of the stream profile factory to create a configuration listener for.
     */
    public void registerConfigListener(final String factoryPid)
    {
        if (!m_ConfigListenerMap.containsKey(factoryPid))
        {
            final ConfigurationListener configListener = new ConfigurationListener(factoryPid);
            m_ConfigListenerMap.put(factoryPid, configListener);
            configListener.registerEvents();
        }
    }
    
    /**
     * Unregisters a configuration listener for the stream profile factory with the specified PID.
     * 
     * @param factoryPid
     *      PID of the stream profile factory to remove a configuration listener for.
     */
    public void unregisterConfigListener(final String factoryPid)
    {
        final ConfigurationListener configListener = m_ConfigListenerMap.remove(factoryPid);
        if (configListener != null)
        {
            configListener.unregsiterEvents();
        }
    }
    
    /**
     * Event handler that listens for the configuration updated events to be posted for the specified stream profile
     * factory.
     */
    class ConfigurationListener implements EventHandler
    {
        /**
         * The service registration reference for this handler class.
         */
        private ServiceRegistration<EventHandler> m_ServiceRegistration;
        
        private String m_FactoryPid;
        
        /**
         * Constructor that accepts the PID of the factory to listen for updated configurations events.
         * 
         * @param factoryPid
         *      PID of factory to register to listen for configuration updated events.
         */
        ConfigurationListener(final String factoryPid)
        {
            m_FactoryPid = factoryPid;
        }
        
        /**
         * Method used to register this handler class for configuration events.
         */
        public void registerEvents()
        {
            final Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(EventConstants.EVENT_TOPIC, ConfigurationEventConstants.TOPIC_CONFIGURATION_UPDATED_EVENT);
            final String filterString = String.format("(%s=%s)", ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, 
                    m_FactoryPid);
            properties.put(EventConstants.EVENT_FILTER, filterString);
            m_ServiceRegistration = m_Context.registerService(EventHandler.class, this, properties);
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final Map<String, Object> props = new HashMap<>();
            for (String propName: event.getPropertyNames())
            {
                props.put(propName, event.getProperty(propName));
            }
            final Event streamConfigEvent = new Event(TOPIC_STREAM_PROFILE_CONFIG_UPDATED, props);
            m_EventAdmin.postEvent(streamConfigEvent);
        }
        
       /**
        * Method used to unregister this handler class.
        */
        public void unregsiterEvents()
        {
            m_ServiceRegistration.unregister();
        }
    }
}
