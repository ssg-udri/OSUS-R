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
package mil.dod.th.ose.config.event;

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
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * The Felix implementation of the configuration admin does not post configuration events to the event admin service,
 * therefore this class bridges configuration events to the event admin that way services can listen to the event admin 
 * service for configuration events rather than having to implement a configuration listener and register with the 
 * configuration admin to receive configuration events.
 * 
 * @author cweisenborn
 */
@Component
public class ConfigurationAdminEventBridge
{
    /**
     * Reference to the event admin service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Reference to the local implementation of a {@link ConfigurationListener}.
     */
    private ConfigListener m_ConfigListener;
    
    /**
     * Method that sets the event admin service to be used.
     * 
     * @param eventAdmin
     *          the {@link EventAdmin} service to be set.
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Method that activates the configuration event to event admin bridge service. Registers the local configuration
     * listener to listen for configuration events.
     * 
     * @param context
     *          bundle context used to register services with the OSGi framework.
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_ConfigListener = new ConfigListener();
        
        m_ConfigListener.registerConfigurationEvents(context);
    }
    
    /**
     * Method that deactivates the configuration event to event admin bridge service. Unregisters the local 
     * configuration listener used to receive configuration events and post them to the event admin.
     */
    @Deactivate
    public void deactivate()
    {
        m_ConfigListener.unregisterListener();
    }
    
    /**
     * Method that takes a configuration event, converts it to an event admin compatible event, and then posts that
     * event to the event admin service.
     * 
     * @param configEvent
     *          the {@link ConfigurationEvent} to be converted and posted to the {@link EventAdmin} service.
     */
    private void postConfigurationEvent(final ConfigurationEvent configEvent)
    {
        //Create the properties map to be posted with the event.
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConfigurationEventConstants.EVENT_PROP_PID, configEvent.getPid());
        props.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, configEvent.getFactoryPid());
        //Retrieve inner service reference properties.
        final ServiceReference<ConfigurationAdmin> serviceReference = configEvent.getReference();
        props.put(ConfigurationEventConstants.EVENT_PROP_SERVICE_REFERENCE, serviceReference);
        props.put(ConfigurationEventConstants.EVENT_PROP_SERVICE_ID, 
                serviceReference.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_ID));
        props.put(ConfigurationEventConstants.EVENT_PROP_SERVICE_OBJECT_CLASS, 
                serviceReference.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_OBJECT_CLASS));
        props.put(ConfigurationEventConstants.EVENT_PROP_SERVICE_PID, 
                serviceReference.getProperty(ConfigurationEventConstants.EVENT_PROP_SERVICE_PID));
        
        final int eventType = configEvent.getType();
        String topic = null;
        //Convert the enumeration integer value to a string topic that can be used in a standard event.
        switch (eventType)
        {
            case ConfigurationEvent.CM_DELETED:
                topic = ConfigurationEventConstants.TOPIC_CONFIGURATION_DELETED_EVENT;
                break;
            case ConfigurationEvent.CM_UPDATED:
                topic = ConfigurationEventConstants.TOPIC_CONFIGURATION_UPDATED_EVENT;
                break;
            case ConfigurationEvent.CM_LOCATION_CHANGED:
                topic = ConfigurationEventConstants.TOPIC_CONFIGURATION_LOCATION_EVENT;
                break;
            default:
                throw new IllegalArgumentException("Invalid configuration event topic!");
        }

        //Post the event to the event admin service. 
        m_EventAdmin.postEvent(new Event(topic, props));
    }
    
    
    /**
     * The inner configuration listener class that is used to receive all configuration events so that they may be
     * posted to the event admin service.
     */
    class ConfigListener implements ConfigurationListener
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the service is 
         * deactivated.
         */
        private ServiceRegistration<ConfigurationListener> m_Registration;
        
        /**
         * Method to register this configuration listener to listen for all configuration events.
         * 
         * @param context
         *          The bundle context used to register the listener.
         */
        public void registerConfigurationEvents(final BundleContext context)
        {
            final String configEvents = ConfigurationEventConstants.TOPIC_ALL_CONFIGURATION_EVENTS;
            // register to listen for all configuration events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, configEvents);
            
            //register the configuration listener that listens for configuration events.
            m_Registration = context.registerService(ConfigurationListener.class, this, props);
        }
        
        /* (non-Javadoc)
         * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
         */
        @Override
        public void configurationEvent(final ConfigurationEvent Event)
        {
            postConfigurationEvent(Event);
        }
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }
}
