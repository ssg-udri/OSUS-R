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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.concurrent.TimeUnit;
import mil.dod.th.ose.gui.integration.helpers.AssetCommandHelper;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper.AssetTabConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper.ExpandCollapse;

import org.junit.After;
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
 * Integration test class to test asynchronous sending of asset responses.
 */
public class TestAssetAsyncCommandResponse
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void beforeClass() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        NavigationHelper.collapseSideBars(m_Driver);
    }
    
    @After
    public void teardown() throws InterruptedException
    {           
        //Remove any left over assets. Cleanup in case of test failure.
        AssetHelper.removeAllAssets(m_Driver);
    }
    
    /**
     * Verify that the get command tab is update for update response events.
     */
    @Test
    public void testUpdateCommandResponseEvent() throws InterruptedException
    {
        //unique name for the asset created for this particular test
        final String assetName = "updateCommandResponseEventAsset";
        AssetHelper.createAsset(m_Driver, "ExampleUpdateAsset", assetName);
        
        // must go to command tab to register the response event handler
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.COMMAND_TAB);
        
        // now that we are listening for the event, we can activate the asset to make the plug-in post the response
        AssetHelper.activateAsset(m_Driver, assetName);
        
        //---choose the command tab ---
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.COMMAND_TAB);
        
        AssetCommandHelper.clickAccordionPanel(m_Driver, assetName, "Get Commands", ExpandCollapse.EXPAND);
        
        WebElement assetCommandPanel = AssetCommandHelper.getAssetCommandPanel(m_Driver, assetName);
        WebElement versionTab = assetCommandPanel.findElement(By.linkText("Version"));
        assertThat(versionTab, notNullValue());
        
        versionTab.click();
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 15);
        wait.until(ExpectedConditions.visibilityOf(assetCommandPanel.findElement(
                       By.cssSelector("div[id*='getCommandTab'][role='tabpanel'][aria-hidden='false']"))));
        
        // refresh once
        m_Driver.navigate().refresh();
        Wait<WebDriver> fWait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).pollingEvery(
                    2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, NoSuchElementException.class);
        fWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement assetCommandPanel = AssetCommandHelper.getAssetCommandPanel(m_Driver, assetName);
                WebElement div = assetCommandPanel.findElement(
                        By.cssSelector("div[id*='getCommandTab'][role='tabpanel'][aria-hidden='false']"));

                WebElement textArea = div.findElement(
                        By.cssSelector("textarea[id$='responseData'][aria-disabled='false']"));
                return textArea.getText().contains("currentVersion=V activate-version");
            }
        });
        assetCommandPanel = AssetCommandHelper.getAssetCommandPanel(m_Driver, assetName);
        WebElement tabDiv  = assetCommandPanel.findElement(By.cssSelector(
                "div[id*='getCommandTab'][role='tabpanel'][aria-hidden='false']"));
        
        WebElement time = tabDiv.findElement(By.cssSelector("span[id$='timeData']"));
        assertThat(time, notNullValue());
        
        assertThat(time.getText(), not(equalTo("Time Received: N/A")));
        AssetHelper.deactivateAsset(m_Driver, assetName);
    }
}
