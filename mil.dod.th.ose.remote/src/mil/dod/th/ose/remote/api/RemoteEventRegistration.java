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
package mil.dod.th.ose.remote.api;

import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventHandler;

/**
 * Data type for remote event registrations.
 * 
 * @author Dave Humeniuk
 *
 */
public class RemoteEventRegistration
{
    /**
     * System id of the system that has registered for the remote event.
     */
    private final int m_SystemId;
    
    /**
     * Service registration for the event.
     */
    private final ServiceRegistration<EventHandler> m_ServiceReg;
    
    /**
     * The data message.
     */
    private final EventRegistrationRequestData m_RequestData;

    /**
     * Default constructor.
     * 
     * @param systemId
     *      system id of the system that has registered for the event remotely
     * @param reg
     *      remote event service registration
     * @param data
     *      the request data message
     *
     */
    public RemoteEventRegistration(final int systemId, final ServiceRegistration<EventHandler> reg,
            final EventRegistrationRequestData data)
    {
        m_SystemId = systemId;
        m_ServiceReg = reg;
        m_RequestData = data;
    }

    @Override
    public String toString()
    {
        return String.format("SystemId: %d%nRemote Registration Data:%n%s%n", m_SystemId, m_RequestData);
    }
    
    /**
     * Get the OSGi service registration for the event registration.
     * @return
     *      the service registration
     */
    public ServiceRegistration<EventHandler> getServiceRegistration()
    {
        return m_ServiceReg;
    }

    /**
     * Get the system id of the system that registered for the remote event.
     * @return
     *      Terra Harvest system id
     */
    public Integer getSystemId()
    {
        return m_SystemId;
    }

    /**
     * Get the original event registration request message.
     * @return
     *      the message
     */
    public EventRegistrationRequestData getEventRegistrationRequestData()
    {
        return m_RequestData.toBuilder().build();
    }
}
