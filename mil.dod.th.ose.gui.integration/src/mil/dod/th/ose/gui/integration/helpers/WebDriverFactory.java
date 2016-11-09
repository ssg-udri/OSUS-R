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
package mil.dod.th.ose.gui.integration.helpers;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import mil.dod.th.ose.gui.integration.util.ResourceLocatorUtil;
import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;

import com.google.common.base.Strings;

/**
 * Class is used to create web drivers for testing.
 * 
 * @author Dave Humeniuk
 *
 */
public final class WebDriverFactory
{
    /**
     * System property that defines the browser to use.
     */
    public final static String BROWSER_TYPE_PROP_NAME = "mil.dod.th.ose.gui.integration.browser";
    
    /**
     * Static string that represents firefox.
     */
    public final static String FIREFOX_BROWSER = "firefox";
    
    /**
     * Static string that represents internet explorer.
     */
    public final static String IE_BROWSER = "ie";
    
    private static final Logger LOG = Logger.getLogger("web.driver.factory");
    
    /**
     * System property that defines the binary path to Firefox. If not specified, Selenium will try to find it. Only
     * need to set if the default version is not sufficient.
     */
    private static final String FIREFOX_BINARY = "mil.dod.th.ose.gui.integration.firefox.binary";
    
    /**
     * Variable used to store the web driver once created. This is the web driver that is used to interact with the 
     * browser during testing.
     */
    private static WebDriver m_Driver;
    
    /**
     * Retrieve the web driver to be used for testing.  All test must retrieve web driver using this method.
     * 
     * @return
     *      Retrieve the web driver to be used for testing.
     */
    public static WebDriver retrieveWebDriver()
    {
        if (m_Driver == null)
        {
            m_Driver = createWebDriver();
            m_Driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
            m_Driver.manage().window().maximize();
        }
        return m_Driver;
    }
    
    /**
     * Create a web driver based on the system property {@link #BROWSER_TYPE_PROP_NAME}.
     * 
     * @return
     *      Create the web driver for the desired browser
     */
    private static WebDriver createWebDriver()
    {
        String desiredBrowser = System.getProperty(BROWSER_TYPE_PROP_NAME);
        if (desiredBrowser == null)
        {
            // use Firefox by default
            return createFirefoxDriverWithProfile();
        }
        else if (desiredBrowser.equals(FIREFOX_BROWSER))
        {
            return createFirefoxDriverWithProfile();
        }
        else if (desiredBrowser.equals(IE_BROWSER))
        {
            //IEDriverServer.exe needed for selenium tests to be ran if firefox is not the target. Must set the path 
            //where the exe is located as an environment variable.
            final File driverServer = new File(ResourceLocatorUtil.getWorkspacePath(), 
                    "deps/selenium-2.42.2/IEDriverServer.exe");
            System.setProperty("webdriver.ie.driver", driverServer.getAbsolutePath());
            return new InternetExplorerDriver();
        }
        else
        {
            throw new IllegalArgumentException(BROWSER_TYPE_PROP_NAME + " is not set to a valid browser name");
        }
    }
    
    /**
     * Creates a Firefox driver with a custom profile. This is used to ensure no dialog windows pop up and that Firefox
     * will occupy the full screen. Mainly needed for running integration tests on a server where there will be no user
     * interaction to assist the test.
     * 
     * @return
     *      {@link FirefoxDriver} with the specified user profile.
     */
    private static FirefoxDriver createFirefoxDriverWithProfile()
    {
        File profileDir = new File(ResourceLocatorUtil.getGuiIntegrationPath(), "/firefox_profile");
        FirefoxProfile profile = new FirefoxProfile(profileDir);
        profile.setEnableNativeEvents(true);
        try
        {
            JavaScriptError.addExtension(profile);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to install Firefox Javascript error extension.", e);
        }
        LOG.info("Using Firefox profile in: " + profileDir.getAbsolutePath());
        
        String firefoxBinaryPath = System.getProperty(FIREFOX_BINARY);
        if (Strings.isNullOrEmpty(firefoxBinaryPath))
        {
            LOG.info("Using standard Firefox driver");
            return new FirefoxDriver(profile);
        }
        else
        {
            File firefoxBinary = new File(firefoxBinaryPath);
            LOG.info("Using Firefox driver with binary at: " + firefoxBinary.getAbsolutePath());
            return new FirefoxDriver(new FirefoxBinary(firefoxBinary), profile);
        }
    }
}
