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
package example.metatype.XML;

import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * The example configurable interface used in the {@link ExampleClass}.
 * @author callen
 *
 */
@OCD
public interface ExampleClassConfig
{ 
    String CONFIG_PROP_EXAMPLE_CONFIG_VALUE = "example.config.value";
    String CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING = "example.config.value.string";
    String CONFIG_PROP_EXAMPLE_CONFIG_STRING_VALUE_AGAIN = "example.config.string.value.again";
    String CONFIG_PROP_EXAMPLE_CONFIG_REQUIRED_STRING_VALUE = "example.config.required.string.value";
    
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
             optionLabels = {"Label1", "Label2", "Label3", "Label4" }, 
             optionValues = {"1", "2", "3", "4" },
             id = CONFIG_PROP_EXAMPLE_CONFIG_VALUE,
             name = "Example Config Value",
             description = "This is an example configuration from an example bundle, changing this value will not "
             + " affect anything in the system.")
    int exampleConfigValue();
    
    /**
     * Holds an example string value
     * 
     * @return
     *    a string value
     */
    @Meta.AD(required = false, deflt = "hello", id = CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING, 
            name = "Example Config String Value")
    String exampleConfigValueString();
    
    /**
     * Holds an example string value
     * 
     * @return
     *    a string value
     */
    @Meta.AD(required = false, deflt = "goodbye", id = CONFIG_PROP_EXAMPLE_CONFIG_STRING_VALUE_AGAIN,
            name = "Another Config String Value")
    String exampleConfigStringValueAgain();
    
    /**
     * Holds an example string value
     * 
     * @return
     *    a string value
     */
    @Meta.AD(required = true, id = CONFIG_PROP_EXAMPLE_CONFIG_REQUIRED_STRING_VALUE,
            name = "Required Config String Value")
    String exampleRequiredProperty();
}
