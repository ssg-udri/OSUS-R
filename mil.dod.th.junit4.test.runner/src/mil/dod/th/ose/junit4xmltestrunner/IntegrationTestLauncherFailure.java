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
package mil.dod.th.ose.junit4xmltestrunner;

import org.junit.Test;

/**
 * Dummy test class that will always fail.
 * 
 * @author dhumeniuk
 *
 */
public class IntegrationTestLauncherFailure
{
    /**
     * Dummy test method that will always fail.  Will throw exception from test launcher to be used by the XML reporter.
     * 
     * SEE stack trace for what really caused this failure.
     * 
     * @throws Exception
     *      will be thrown by test to show up as failure in report
     *      
     */
    @Test
    public final void testInitialization() throws Exception //NOPMD avoid throwing java.lang.Exception 
    {                                                       // (generic method to support all exception types)
        throw IntegrationTestLauncher.getTestException();
    }
}
