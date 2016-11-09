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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.ControllerHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Special test class that connects a controller to the web gui.
 * 
 * @author cweisenborn
 */
public class ConnectController
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void setup()
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
    }
    
    /**
     * Connects a controller to the GUI for later use in testing. 
     */
    @Test
    public void testConnectController() throws InterruptedException, ExecutionException, TimeoutException
    {
        ControllerHelper.createController(m_Driver);
        
        WebElement tHarvestController = ControllerHelper.getControllerListElement(m_Driver, 
            ControllerHelper.DEFAULT_CONTROLLER_NAME);
        
        assertThat(tHarvestController, is(notNullValue()));
    }
}
