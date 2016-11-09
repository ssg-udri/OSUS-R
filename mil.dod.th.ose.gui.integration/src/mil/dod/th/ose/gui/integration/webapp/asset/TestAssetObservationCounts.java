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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.ControllerHelper;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper.AssetTabConstants;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selenium test for the asset observation counts. Setup a mission to capture data every 5 seconds
 * and make sure observation counts are incremented. Go to the observations tab of the asset page 
 * and verify that the observation counts are decremented to 0.
 * 
 * @author matt
 */
public class TestAssetObservationCounts
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
    }
    
    @Before
    public void beforeEach() throws InterruptedException
    {
        //Add testAsset
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testAsset");
        // go to obs page and refresh to zero out the observation count
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB);
        m_Driver.navigate().refresh();   
        // then go back
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.CONFIGURATION_TAB);
    }
    
    @After
    public void afterEach() throws InterruptedException
    {
        AssetHelper.removeAllAssets(m_Driver);
        
        ObservationHelper.deleteAllObservations(m_Driver);
    }
    
    /**
     * Verify the observation count on the sidebar of the mainscreen template increments the count of observations
     * correctly and verify that it resets correctly when going to the observations page.
     */
    @Test
    public void testSideBarObservationCount() throws InterruptedException, ExecutionException, TimeoutException
    {
        //Wait to assure the page has loaded.
        Wait<WebDriver> waitForText = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        NavigationHelper.expandRightSideBarOnly(m_Driver);
        
        waitForText.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[class='controllerObsCnt']")).getText().equals("0"))
                {
                    return true;
                }
                return false;
            }
        });
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[class='ui-growl-item']")));
        
        //capture data from asset
        GrowlVerifier.verifyAndWaitToDisappear(30, 
                m_Driver.findElement(By.cssSelector("button[id$='captureData']")), "Asset captured data!");
        //Wait till growl message disappears to continue.
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[id='growl-timed_container']")));
        
        waitForText.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[class='controllerObsCnt']")).getText().equals("1"))
                {
                    return true;
                }
                return false;
            }
        });
        
        //capture data 4 more times and check at 5
        for (int i = 0; i < 4; i++)
        {
            GrowlVerifier.verifyAndWaitToDisappear(30, 
                    m_Driver.findElement(By.cssSelector("button[id$='captureData']")), "Asset captured data!");
            //Wait till growl message disappears to continue.
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div[id='growl-timed_container']")));
        }
        
        waitForText.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[class='controllerObsCnt']")).getText().equals("5"))
                {
                    return true;
                }
                return false;
            }
        });
        
        //capture data 5 more times and check at 10
        for (int i = 0; i < 5; i++)
        {
            GrowlVerifier.verifyAndWaitToDisappear(30, 
                    m_Driver.findElement(By.cssSelector("button[id$='captureData']")), "Asset captured data!");
            //Wait till growl message disappears to continue.
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div[id='growl-timed_container']")));
        }
        
        waitForText.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[class='controllerObsCnt']")).getText().equals("10"))
                {
                    return true;
                }
                return false;
            }
        });
        
        //Need to refresh the page since now the observation isn't reset if you are on the assets page and 
        //go to the observation tab
        m_Driver.navigate().refresh();
        
        //click on observation tab
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("a[href*='assetObsTab']")));
        WebElement observationTab = m_Driver.findElement(By.cssSelector("a[href*='assetObsTab']"));
        observationTab.click();
        
        //verify count is decremented
        waitForText.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[class='controllerObsCnt']")).getText().equals("0"))
                {
                    return true;
                }
                return false;
            }
        });
    }
    
    /**
     * Verify the observation count on the header of the mainscreen template increments the count of observations
     * correctly and verify that it resets correctly when going to the observations page.
     */
    @Test
    public void testHeaderObservationCount() throws InterruptedException, ExecutionException, TimeoutException
    {
        final int controllerId = ControllerHelper.getControllerIdAsInteger();
        
        //Wait to assure the page has loaded.
        Wait<WebDriver> waitForObsCount = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        waitForObsCount.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[id$='activeControllerObsCnt_" + controllerId + "']"))
                        .getText().equals("0"))
                {
                    return true;
                }
                return false;
            }
        });
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[class='ui-growl-item']")));
        
        //capture data from asset
        GrowlVerifier.verifyAndWaitToDisappear(30, 
                m_Driver.findElement(By.cssSelector("button[id$='captureData']")), "Asset captured data!");
        //Wait till growl message disappears to continue.
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div[id='growl-timed_container']")));
        
        waitForObsCount.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[id$='activeControllerObsCnt_" + controllerId + "']"))
                        .getText().equals("1"))
                {
                    return true;
                }
                return false;
            }
        });
        
        //capture data 4 more times and check at 5
        for (int i = 0; i < 4; i++)
        {
            GrowlVerifier.verifyAndWaitToDisappear(30, 
                    m_Driver.findElement(By.cssSelector("button[id$='captureData']")), "Asset captured data!");
            //Wait till growl message disappears to continue.
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div[id='growl-timed_container']")));
        }
        
        waitForObsCount.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[id$='activeControllerObsCnt_" + controllerId + "']"))
                        .getText().equals("5"))
                {
                    return true;
                }
                return false;
            }
        });
        
        //capture data 5 more times and check at 10
        for (int i = 0; i < 5; i++)
        {
            GrowlVerifier.verifyAndWaitToDisappear(30, 
                    m_Driver.findElement(By.cssSelector("button[id$='captureData']")), "Asset captured data!");
            //Wait till growl message disappears to continue.
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div[id='growl-timed_container']")));
        }
        
        waitForObsCount.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[id$='activeControllerObsCnt_" + controllerId + "']"))
                        .getText().equals("10"))
                {
                    return true;
                }
                return false;
            }
        });
        
        //Need to refresh the page since now the observation isn't reset if you are on the assets page and 
        //go to the observation tab
        m_Driver.navigate().refresh();
        
        //click on observation tab
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("a[href*='assetObsTab']")));
        WebElement observationTab = m_Driver.findElement(By.cssSelector("a[href*='assetObsTab']"));
        observationTab.click();
        
        //verify count is decremented to 0
        waitForObsCount.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                if (driver.findElement(By.cssSelector("span[id$='activeControllerObsCnt_" + controllerId + "']"))
                        .getText().equals("0"))
                {
                    return true;
                }
                return false;
            }
        });
    }
}
