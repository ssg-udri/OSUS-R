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
package mil.dod.th.ose.core.impl.mp;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.mp.EventHandlerHelper;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import org.apache.felix.service.command.Descriptor;

/**
 * Implementation of the {@link EventHandlerHelper} interface.
 * 
 * @author dhumeniuk
 * 
 */
@Component
public class EventHandlerHelperImpl implements EventHandlerHelper
{
    /**
     * Context for the bundle containing this component.
     */
    private BundleContext m_Context;

    /**
     * Map of all active registrations.
     */
    private final Map<ServiceReference<EventHandler>, ServiceRegistration<EventHandler>> m_Registrations = 
        Collections.synchronizedMap(new HashMap<ServiceReference<EventHandler>, ServiceRegistration<EventHandler>>());
    
    /**
     * Activate this component.
     * 
     * @param context
     *            context for the bundle containing this component
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
    }
    

    /**
     * Deactivate the component by unregister all event handlers.
     */
    @Deactivate
    public void deactivate()
    {
        for (ServiceRegistration<?> reg : m_Registrations.values())
        {
            reg.unregister();
        }
        
        m_Registrations.clear();
    }

    @Override
    public ServiceReference<EventHandler> registerHandler(final EventHandler handler, final String topic)
    {
        return registerHandler(handler, topic, null);
    }

    @Override
    public ServiceReference<EventHandler> registerHandler(final EventHandler handler, final String topic, 
        final String filter)
    {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, topic);
        if (filter != null)
        {
            props.put(EventConstants.EVENT_FILTER, filter);
        }
        final ServiceRegistration<EventHandler> reg = m_Context.registerService(EventHandler.class, handler, props);
        final ServiceReference<EventHandler> ref = reg.getReference();
        m_Registrations.put(ref, reg);
        Logging.log(LogService.LOG_DEBUG, "Event handler helper registered for topic [%s] and filter [%s]", topic, 
                filter);
        return ref;
    }
    
    @Override
    public void unregisterHandler(final ServiceReference<EventHandler> handlerReference)
    {
        final ServiceRegistration<EventHandler> reg = m_Registrations.remove(handlerReference);
        if (reg == null)
        {
            throw new IllegalArgumentException("Registration not found, likely already unregistered");
        }
        reg.unregister();
    }

    @Override
    @Descriptor("Unregister all previously registered handlers")
    public void unregisterAllHandlers()
    {
        synchronized (m_Registrations)
        {
            for (ServiceRegistration<EventHandler> reg : m_Registrations.values())
            {
                reg.unregister();
            }
            m_Registrations.clear();
        }
    }
}
