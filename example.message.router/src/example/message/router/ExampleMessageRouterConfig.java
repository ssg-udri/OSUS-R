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
package example.message.router;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Defines the metadata for the properties available to the {@link example.message.router.ExampleMessageRouter}.
 * 
 * @author Josh
 */
@OCD
public interface ExampleMessageRouterConfig
{
    /**
     * Constant for the server socket port configuration property.
     */
    String CONFIG_PROP_PORT = "port";
    
    /**
     * Server socket port number configuration property.
     * 
     * @return
     *      The server socket port number to be used.
     */
    @AD(id = ExampleMessageRouterConfig.CONFIG_PROP_PORT, name = "Server Socket Port", 
            description = "Server Socket Port", required = false, deflt = "4242")
    int port();
}
