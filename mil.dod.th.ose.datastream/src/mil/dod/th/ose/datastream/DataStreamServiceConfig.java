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
package mil.dod.th.ose.datastream;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Defines metadata of properties for the {@link mil.dod.th.core.datastream.DataStreamService} 
 * reference implementation.
 * 
 * @author jmiller
 *
 */
@OCD
public interface DataStreamServiceConfig
{
    /** Configuration property key for  {@link #multicastHost()}. */
    String CONFIG_PROP_MULTICAST_HOST = "multicast.host";
    
    /** Configuration property key for {@link #startPort()}. */
    String CONFIG_PROP_START_PORT = "start.port";
    
    /**
     * Configuration property for the multicast host address.
     * 
     * @return
     *      multicast address as a String
     */
    @AD(required = true, id = CONFIG_PROP_MULTICAST_HOST, 
            description = "Multicast host address for data stream output")
    String multicastHost();
    
    /**
     * Configuration property for the starting port number.
     * 
     * @return
     *      port number as an int
     */
    @AD(required = false, deflt = "5004", id = CONFIG_PROP_START_PORT, 
            description =  "Starting port number for service to assign to stream profile instances.")
    int startPort();
    
    
    
}
