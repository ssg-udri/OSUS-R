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
package mil.dod.th.ose.gui.integration.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;

import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper.AssetTabConstants;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationHelper;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

/**
 * Integration test to verify scan for asset functionality.
 * 
 * @author cweisenborn
 */
public class TestScanForAssets
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        //Navigate to asset page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        NavigationHelper.collapseSideBars(m_Driver);
        
        //Navigate to the asset configuration tab.
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.CONFIGURATION_TAB);
    }
    
    @After
    public void cleanup() throws InterruptedException
    {
        //Cleanup in case of test failure.
        AssetHelper.removeAllAssets(m_Driver);
        
        //Remove all observations.
        ObservationHelper.deleteAllObservations(m_Driver);
    }
    
    /**
     * Verify that the scan for assets buttons updates the asset page for any assets found when scanning the system.
     */
    @Test
    public void testScanForAssetsButton() throws InterruptedException
    {
        //Click scan for assets button.
        GrowlVerifier.verifyAndWaitToDisappear(20, m_Driver.findElement(
                By.cssSelector("button[id$='scanForAssets']")), "Asset created.");

        
        //Sleep for a second to ensure that AJAX has completed.
        Thread.sleep(1000);
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(20, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        //Wait for the created asset to appear in the assets list.
        Boolean result = fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return AssetHelper.retrieveAsset(driver, "FoundExAsset") != null;
            }
        });
        
        assertThat(result, is(true));
    }
}
