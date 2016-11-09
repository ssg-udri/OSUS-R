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
package example.config;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * An example definition of configuration properties used by the integration tests.
 * 
 * @author dhumeniuk
 *
 */
@OCD
public interface ExampleConfig
{
    @AD(deflt = "8", description = "Example integer property")
    int someInt();
    
    @AD(deflt = "Value1", description = "Example choice enum property")
    SomeEnum someEnum();
    
    @AD(deflt = "false", description = "Example boolean property")
    boolean someBool();
    
    enum SomeEnum
    {
        Value1,
        Value2,
        Value3
    }
}
