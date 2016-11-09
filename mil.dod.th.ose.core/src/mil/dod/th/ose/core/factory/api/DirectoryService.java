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
package mil.dod.th.ose.core.factory.api;

import java.util.Map;

import mil.dod.th.core.log.LoggingService;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * Common abstract class for all services that work with FactoryDescriptors to provide instances of items. Allows
 * factory descriptor registration to be shared.
 *
 * @author dhumeniuk
 */
public abstract class DirectoryService
{
    /**
     * Utility used to log messages.
     */
    protected LoggingService m_Logging;

    /**
     * Reference to the EventAdmin service that all events will be posted to.
     */
    protected EventAdmin m_EventAdmin;
    
    /**
     * Binds the logging service for logging messages.  This method must be called by all classes which extends this 
     * class.
     * 
     * @param logging
     *      Logging service object
     */
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    /**
     * Binds the EventAdmin service to this component.
     * 
     * 1..1 Cardinality
     * 
     * @param eventAdmin
     *      EventAdmin service to use for posting events
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    } 
    
    /**
     * Post an event to the EventAdmin.
     * 
     * @param topic
     *            Topic to use for the event
     * @param props
     *            Properties to associate with the event
     */
    // TODO: TH-2763 move to FactoryServiceContext
    protected void postEvent(final String topic, final Map<String, Object> props) 
    {
        m_EventAdmin.postEvent(new Event(topic, props));

        m_Logging.log(LogService.LOG_DEBUG, "%s posted event %s", this.getClass().getSimpleName(), topic);
    }

    /**
     * Post an event about factory related actions.
     * 
     * @param <FO>
     *  the base factory object internal type, for example {@link mil.dod.th.ose.core.impl.asset.AssetInternal}
     * @param topic
     *            Topic of the event as defined by {@link mil.dod.th.core.factory.FactoryDescriptor} interface.
     * @param factoryObject
     *            Object that the event is about
     */
    protected <FO extends FactoryObjectInternal> void postFactoryObjectEvent(final String topic, 
        final FO factoryObject) // TODO: TH-2763 move to FactoryServiceContext
    {
        final Map<String, Object> props = FactoryServiceUtils.getFactoryObjectBaseEventProps(factoryObject);
        
        postEvent(topic, props);
    }   
}
