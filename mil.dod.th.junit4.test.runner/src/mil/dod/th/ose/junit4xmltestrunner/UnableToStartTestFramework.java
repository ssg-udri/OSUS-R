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

import static org.junit.Assert.fail; // NOCHECKSTYLE: Static import required for JUnit

/**
 * Dummy test class that will always fail.
 * 
 * @author dhumeniuk
 *
 */
public class UnableToStartTestFramework
{
    /**
     * Dummy test method that will always fail.  Needed to report issue with starting the JUnit4 framework
     * or other critical error.
     */
    @Test
    public final void testInitialization()
    {
        fail("Unable to start integration framework.  Error not detected by framework, no test results.  " 
                + "See console output.");
    }
}
