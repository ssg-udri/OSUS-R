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
package mil.dod.th.ose.mp.runtime;

/**
 * OSGi service definition to retrieve the class loader that should be used by mission programs as this loader is 
 * capable of dynamically loading all packages.
 * 
 * @author dhumeniuk
 *
 */
public interface MissionProgramRuntime
{
    /**
     * Get the class loader to use for mission programs.
     * 
     * @return
     *      class loader capable of dynamically loading all packages within an OSGi environment
     */
    ClassLoader getClassLoader();
}
