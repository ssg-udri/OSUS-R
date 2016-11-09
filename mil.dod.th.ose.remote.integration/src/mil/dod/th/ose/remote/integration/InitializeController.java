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
package mil.dod.th.ose.remote.integration;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import mil.dod.th.ose.remote.api.RemoteSettings;

import org.junit.Test;

/**
 * This is a special test class that is run first to enable remote interface debug logging.
 * 
 * @author Dave Humeniuk
 *
 */
public class InitializeController
{
    /**
     * Enable remote interface debug logging.
     */
    @Test
    public void testInitialize() throws UnknownHostException, IOException
    {
        try (Socket socket = SocketHostHelper.connectToController())
        {
            ConfigNamespaceUtils.setConfigProperty(RemoteSettings.PID, 
                    RemoteSettings.KEY_LOG_REMOTE_MESSAGES, true, socket);      
        }
    }
}
