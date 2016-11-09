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
package mil.dod.th.ose.controller.integration.api;

/**
 * Contains properties that can be accessed by running tests, different test runs can set properties as needed.
 * 
 * @author dhumeniuk
 *
 */
public class IntegrationProperties
{
    /**
     * Whether the tests that shutdown the core bundle should be skipped.
     */
    public static boolean skipCoreShutdown()
    {
        final String skipCoreShutdownString = 
            System.getProperty("mil.dod.th.ose.controller.integration.skipCoreShutdown");
        
        if (skipCoreShutdownString == null)
        {
            return false;
        }
        else
        {
            return Boolean.parseBoolean(skipCoreShutdownString);
        }
    }
}
