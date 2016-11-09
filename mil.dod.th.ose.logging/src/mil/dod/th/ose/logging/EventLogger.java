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
package mil.dod.th.ose.logging;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.shared.OSGiEventConstants;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Component will log {@link org.osgi.service.event.EventAdmin} activity.
 * 
 * @author Dave Humeniuk
 *
 */
@Component
public class EventLogger
{
    /**
     * Name of the OSGi framework property containing a flag of whether to enable the event logger.
     */
    public static final String LOG_EVENTS_ENABLED_PROPERTY = "mil.dod.th.ose.logging.logEvents";
    
    /**
     * Used for logging events.
     */
    private LoggingService m_Logging;
    
    /**
     * Registration for the event logging event handler.
     */
    private ServiceRegistration<EventHandler> m_EventHandlerReg;

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
     * Activate the component.
     * 
     * @param context
     *      context for the bundle containing this component
     */
    @Activate
    public void activate(final BundleContext context)
    {
        final boolean enabled = Boolean.parseBoolean(context.getProperty(LOG_EVENTS_ENABLED_PROPERTY));
        
        if (enabled)
        {
            final Dictionary<String, Object> eventHandlerProps = new Hashtable<String, Object>();
            eventHandlerProps.put(EventConstants.EVENT_TOPIC, "*");
            m_EventHandlerReg = 
                    context.registerService(EventHandler.class, new EventLoggerHandler(), eventHandlerProps);
        }
    }
    
    /**
     * Deactivate the component.
     */
    public void deactivate()
    {
        if (m_EventHandlerReg != null)
        {
            m_EventHandlerReg.unregister();
        }
    }
    
    /**
     * Logs all received events.
     */
    class EventLoggerHandler implements EventHandler
    {
        @Override
        public void handleEvent(final Event event)
        {
            if (event.getTopic().startsWith(OSGiEventConstants.TOPIC_PREFIX_LOGGING_EVENTS))
            {
                return; // ignore any log events, would cause an infinite loop of logging
            }
            
            final StringBuilder builder = new StringBuilder();
            builder.append("Event: topic=[").append(event.getTopic()).append("] {");
            
            final List<String> props = new ArrayList<String>();
            for (String key : event.getPropertyNames())
            {
                if (key.equals(EventConstants.EVENT_TOPIC))
                {
                    continue; // don't log event topic itself, already in string
                }
                props.add(key + "=" + event.getProperty(key));
            }

            builder.append(StringUtils.join(props, ", "));

            builder.append("}");
            
            m_Logging.debug(builder.toString());
        }
    }
}
