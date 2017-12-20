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
package mil.dod.th.ose.utils;

import aQute.bnd.annotation.ProviderType;

import org.osgi.framework.Bundle;

/**
 * OSGi service to get information about bundles. Allows service interface to be injected instead of having to call
 * static methods.
 * 
 * @author dhumeniuk
 */
@ProviderType
public interface BundleService
{
    /**
     * Calls {@link org.osgi.framework.FrameworkUtil#getBundle(Class)}.
     * 
     * @param clazz
     *      bundle class to look up
     * @return
     *      bundle that contains the given class or null if unable to find the bundle for the given class
     */
    Bundle getBundle(Class<?> clazz);
}
