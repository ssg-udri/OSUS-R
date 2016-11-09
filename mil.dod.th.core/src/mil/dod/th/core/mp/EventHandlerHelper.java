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
package mil.dod.th.core.mp;

import aQute.bnd.annotation.ProviderType;

import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventHandler;

/**
 * <p>
 * OSGi provided service that aids in registering services from a JavaScript.  For example:
 * 
 * <pre>
 * importPackage(org.osgi.service.event);
 * 
 * obj =
 * {
 *    handleEvent: function(event)
 *    {
 *        pds.persist(event.getTopic().getClass(), uuid, "test", event.getTopic());
 *    }
 * };
 * 
 * handler = new EventHandler(obj);
 * 
 * ehh.registerHandler(handler, "mil/dod/th/core/asset/Asset/DATA_CAPTURED");
 * </pre>
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface EventHandlerHelper
{
    /**
     * Registers an {@link EventHandler} with OSGi to subscribe to events with the given topic.
     * 
     * @param handler
     *      handler that will be called when an event occurs with the given topic
     * @param topic
     *      {@link org.osgi.service.event.EventConstants#EVENT_TOPIC} to filter on for events
     * @return
     *      reference to the handler service, call {@link #unregisterHandler(ServiceReference)} when done with handler
     */
    ServiceReference<EventHandler> registerHandler(EventHandler handler, String topic);
    
    /**
     * Registers an {@link EventHandler} with OSGi to subscribe to events with the given topic.
     * 
     * @param handler
     *      handler that will be called when an event occurs with the given topic
     * @param topic
     *      {@link org.osgi.service.event.EventConstants#EVENT_TOPIC} to filter on for events
     * @param filter
     *      LDAP {@link org.osgi.service.event.EventConstants#EVENT_FILTER} for the event
     * @return
     *      reference to the handler service, call {@link #unregisterHandler(ServiceReference)} when done with handler
     */
    ServiceReference<EventHandler> registerHandler(EventHandler handler, String topic, String filter);
    
    /**
     * Unregister a handler for the given reference.
     * 
     * @param handlerReference
     *      service reference to the registered event handler
     */
    void unregisterHandler(ServiceReference<EventHandler>  handlerReference);
    
    /**
     * Unregister all previously registered handlers.
     */
    void unregisterAllHandlers();
}
