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
package mil.dod.th.ose.linux.loader;

import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;

/**
 * Class to allow the native libraries accessible from the mil.dod.th.ose.linux.gnu package to be loaded on demand.
 * 
 * @author dhumeniuk
 *
 */
public final class LinuxNativeLibraryLoader
{
    /**
     * Private constructor to prevent instantiation.
     */
    private LinuxNativeLibraryLoader()
    {
        
    }
    
    /**
     * Load the native libraries used by the mil.dod.th.ose.linux.gnu package.  Must be called if using the package 
     * directly. Not necessary if using serial ports as the library will be loaded before ports will be available.
     */
    public static void load()
    {
        Logging.log(LogService.LOG_INFO, "Loading the Native Linux Serial Port Library");
        System.loadLibrary("serialport");
    }
}
