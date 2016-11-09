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
package mil.dod.th.ose.archiver.vlc;

import mil.dod.th.ose.utils.CoverageIgnore;

import uk.co.caprica.vlcj.discovery.NativeDiscovery;


/**
 * Utility class to load VLC platform-specific libraries.
 * 
 * @author jmiller
 *
 */
public final class NativeLibraryLoader
{
    
    /**
     * Private constructor to prevent initialization.
     */
    private NativeLibraryLoader()
    {
        
    }
    
    /**
     * Make native VLC libraries available to bundle.
     */
    @CoverageIgnore
    public static void load()
    {
        new NativeDiscovery().discover();
    }

}