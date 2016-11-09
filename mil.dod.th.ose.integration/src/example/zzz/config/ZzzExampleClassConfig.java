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
package example.zzz.config;

import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * The example configurable interface used in the {@link ZzzExampleClass}.
 */
@OCD
public interface ZzzExampleClassConfig
{ 
    String CONFIG_PROP_EXAMPLE_CONFIG_VALUE = "example.config.value";
    
    /**
     * Holds an example configuration value.
     * 
     * @return
     *    an integer value
     */
    @Meta.AD(required = false, 
             deflt = "1", 
             min = "1", 
             max = "100", 
             id = CONFIG_PROP_EXAMPLE_CONFIG_VALUE,
             name = "Example Config Value",
             description = "This is an example configuration from an example bundle, changing this value will not "
             + " affect anything in the system.")
    int exampleConfigValue();
}
