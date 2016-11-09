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
package mil.dod.th.ose.gui.integration;

import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.Test;
import org.openqa.selenium.WebDriver;

/**
 * This is a special test class that is run last to stop the controller software.  This test will verify the controller
 * can be shutdown using the message, but is also necessary to stop the controller software so the testing can finish.
 * This test all shutdown the web driver used to run the tests and closes all browser windows created by the driver.
 * 
 * @author Dave Humeniuk
 *
 */
public class ShutdownTasks
{    
    /**
     * Shutdown the web driver and all browser windows associated with it.
     */
    @Test
    public void testShutdownDriver()
    {
        final WebDriver driver = WebDriverFactory.retrieveWebDriver();
        //Close all the browser windows and shutdown the associated driver.
        driver.quit();
    }
}
