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
package mil.dod.th.ose.junit4xmltestrunner;

import org.osgi.util.tracker.ServiceTracker;

/**
 * Immutable data type class to hold info for a service reference.
 * 
 * @author Dave Humeniuk
 *
 */
public class ServiceReferenceInfo
{
    /**
     * Type of service.
     */
    private final Class<?> m_Class;
    
    /**
     * Whether the service is optional.
     */
    private final boolean m_Optional;
    
    /**
     * Tracker to use for the service.
     */
    private final ServiceTracker<?, ?> m_ServiceTracker;

    /**
     * Create an object with the give class and custom filter.
     * 
     * @param clazz
     *      what class the service needs to provide
     * @param optional
     *      whether the service is optional
     * @param serviceTracker
     *      service tracker to look up the needed service
     */
    public ServiceReferenceInfo(final Class<?> clazz, final boolean optional, final ServiceTracker<?, ?> serviceTracker)
    {
        m_Class = clazz;
        m_Optional = optional;
        m_ServiceTracker = serviceTracker;
    }

    /**
     * Get the class this service reference is looking for.
     * 
     * @return
     *      type of service
     */
    public Class<?> getServiceClass()
    {
        return m_Class;
    }

    /**
     * Get the tracker for this service reference.
     * 
     * @return
     *      tracker of the service
     */
    public ServiceTracker<?, ?> getTracker()
    {
        return m_ServiceTracker;
    }

    /**
     * Get whether the reference is optional.
     * 
     * @return
     *      true if the service is considered optional, false if not
     */
    public boolean isOptional()
    {
        return m_Optional;
    }

}
