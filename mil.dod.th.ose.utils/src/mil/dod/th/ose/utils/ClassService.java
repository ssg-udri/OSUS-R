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

import java.net.URL;

/**
 * OSGi service for classes to allow for injection.
 * 
 * @author dhumeniuk
 *
 */
public interface ClassService
{
    /**
     * Calls {@link java.lang.Class#getResource(String)}.
     * 
     * @param clazz
     *      will use the class loader of the given class to find the resource.
     * @param name
     *      name of the desired resource relative to the location of the class
     * @return
     *      URL for the resource or null if not found
     */
    URL getResource(Class<?> clazz, String name);
    
    /**
     * Calls {@link Class#getClassLoader()}.
     * 
     * @param clazz
     *      class to get the class loader for
     * @return
     *      class loader for the given class
     */
    ClassLoader getClassLoader(Class<?> clazz);
}
