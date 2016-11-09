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
package example.message.client;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Configuration properties for {@link ExampleMessageClient}.
 * 
 * @author jlatham
 */
@OCD
public interface ExampleMessageClientConfig
{
    /**
     * Constant for the server IP configuration property.
     */
    String CONFIG_PROP_SERVER_IP = "server.ip";
    
    /**
     * Constant for the server port configuration property.
     */
    String CONFIG_PROP_SERVER_PORT = "server.port";
    
    /**
     * Configuration property for the IP of server the client will connect to when a specific IP is not
     * specified.
     * 
     * @return
     *      IP of the server.
     */
    @AD(id = ExampleMessageClientConfig.CONFIG_PROP_SERVER_IP, name = "Server IP", 
            description = "Server IP to connect to", required = false, deflt = "127.0.0.1")
    String ipAdress();
    
    /**
     * Configuration property for the port of the server the client will connect to when specific port is not 
     * specified.
     * 
     * @return
     *      Port of the server.
     */
    @AD(id = ExampleMessageClientConfig.CONFIG_PROP_SERVER_PORT, name = "Server Port", description = "Server Port", 
            required = false, deflt = "4242")
    int port();
}
