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
import java.net.UnknownHostException;

import org.junit.Test;

import terra.harvest.standalone.demo.RemoteInterfaceStandaloneDemo;

/**
 * @author dhumeniuk
 *
 */
public class TestStandAloneDemo
{
    /**
     * Verify the demo can be run.
     */
    @Test
    public final void testIt() throws UnknownHostException, IOException, InterruptedException
    {
        RemoteInterfaceStandaloneDemo.main(null);
    }
}
