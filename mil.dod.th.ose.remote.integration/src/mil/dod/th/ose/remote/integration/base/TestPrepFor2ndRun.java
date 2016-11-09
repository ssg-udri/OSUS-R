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
package mil.dod.th.ose.remote.integration.base;

import java.io.IOException;
import java.net.Socket;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.ose.remote.integration.RemoteEventRegistration;
import mil.dod.th.ose.remote.integration.SocketHostHelper;

import org.junit.Test;

/**
 * Preparation ran before shutting down the controller on the first run. 
 * @author callen
 *
 */
public class TestPrepFor2ndRun
{
    /**
     * Preparation for restart. Create an event admin event registration.
     */
    @Test
    public void testPrepEventRegistration() throws IOException, InterruptedException
    {
        try (Socket socket = SocketHostHelper.connectToController())
        {
            //will only register to the default system credentials
            RemoteEventRegistration.regRemoteEventMessagesAlternavtiveSystem(
                    socket, new String[]{Asset.TOPIC_DATA_CAPTURED}, null);
        }
    }
}
