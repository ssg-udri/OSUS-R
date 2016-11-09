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
package mil.dod.th.core.ccomm.physical;


import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Defines metadata of properties that are available to all {@link PhysicalLink}s. Retrieve properties using {@link 
 * PhysicalLink#getConfig()}.
 * 
 * @author dhumeniuk
 *
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface PhysicalLinkAttributes
{
    /** Configuration property key for {@link #dataBits()}. */
    String CONFIG_PROP_DATA_BITS = FactoryObject.TH_PROP_PREFIX + ".databits";

    /** 
      * Configuration property key for {@link #readTimeoutMs()}.
      */
    String CONFIG_PROP_READ_TIMEOUT_MS = FactoryObject.TH_PROP_PREFIX + ".read.timeout.ms";

    /** 
     * Configuration property for the number of bits in each byte read from the link.
     * 
     * @return data bits
     */
    @AD(required = false, deflt = "8", id = CONFIG_PROP_DATA_BITS,
        description = "How many bits of data in each byte read from the link")
    int dataBits();
    
    /** 
     * Configuration property for the read timeout in milliseconds.
     * 
     * @return timeout in milliseconds, a negative number will cause the read to wait indefinitely
     */
    @AD(required = false, deflt = "-1", id = CONFIG_PROP_READ_TIMEOUT_MS,
        description = "Timeout value for read calls in milliseconds")
    int readTimeoutMs();
}
