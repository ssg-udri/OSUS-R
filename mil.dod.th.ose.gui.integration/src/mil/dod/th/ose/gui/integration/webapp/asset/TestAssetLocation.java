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
import mil.dod.th.ose.gui.integration.helpers.ConfigurationHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

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
 * Test asset location functionality.
 * @author matt
 *
 */
public class TestAssetLocation
{
    private static WebDriver m_Driver;
    
    private static final String LAT_INPUT_ID = "assetLatLocation";
    private static final String LON_INPUT_ID = "assetLongLocation";
    private static final String ALT_INPUT_ID = "assetAltitudeLocation";
    private static final String BANK_INPUT_ID = "assetBankLocation";
    private static final String HEAD_INPUT_ID = "assetHeadingLocation";
    private static final String ELV_INPUT_ID = "assetElevationLocation";
    
    private static final String EDIT_LOC_BUT_ID = "editLocationButton";
    private static final String SYNC_LOC_BUT_ID = "syncLocationButton";
    private static final String UPDATE_LOC_BUT_ID = "updateLocationButton";
    
    
    @BeforeClass
    public static void beforeSetup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        NavigationHelper.expandSideBars(m_Driver);
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='assetButton']")));
        
        //Navigate to asset page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        NavigationHelper.collapseSideBars(m_Driver);
    }
    
    @After
    public void cleanup() throws InterruptedException
    {        
        //Cleanup in case of test failure.
        AssetHelper.removeAllAssets(m_Driver);
    }
    
    /**
     * Test the defaults of the ExampleAsset asset location handling
     * ExampleAsset: has getPosition capability, does not have setPosition capability
     * Asset position handling config property: not set yet so the flag is not registered
     * Verify default values are shown in the location fields, verify the edit location button does not
     * show up yet.
     */
    @Test
    public void testGetPositionCapabilsDefaults() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        WebElement latitude = m_Driver.findElement(By.cssSelector("input[id$='" + LAT_INPUT_ID + "']"));
        assertThat(latitude.getAttribute("value"), is(""));
        assertThat(latitude.isEnabled(), is(false));
        
        WebElement longitude = m_Driver.findElement(By.cssSelector("input[id$='" + LON_INPUT_ID +"']"));
        assertThat(longitude.getAttribute("value"), is(""));
        assertThat(longitude.isEnabled(), is(false));
        
        WebElement altitude = m_Driver.findElement(By.cssSelector("input[id$='" + ALT_INPUT_ID + "']"));
        assertThat(altitude.getAttribute("value"), is(""));
        assertThat(altitude.isEnabled(), is(false));
        
        WebElement bank = m_Driver.findElement(By.cssSelector("input[id$='" + BANK_INPUT_ID + "']"));
        assertThat(bank.getAttribute("value"), is(""));
        assertThat(bank.isEnabled(), is(false));
        
        WebElement heading = m_Driver.findElement(By.cssSelector("input[id$='" + HEAD_INPUT_ID + "']"));
        assertThat(heading.getAttribute("value"), is(""));
        assertThat(heading.isEnabled(), is(false));
        
        WebElement elevation = m_Driver.findElement(By.cssSelector("input[id$='" + ELV_INPUT_ID + "']"));
        assertThat(elevation.getAttribute("value"), is(""));
        assertThat(elevation.isEnabled(), is(false));
        
        //verify edit location button shows up
        assertThat(m_Driver.findElements(By.cssSelector("button[id$='" + EDIT_LOC_BUT_ID + "']")).size(), is(1));
        
        //verify sync button shows up
        assertThat(m_Driver.findElements(By.cssSelector("button[id$='" + SYNC_LOC_BUT_ID + "']")).size(), is(1));
        
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
    }
    
    /**
     * ExampleAsset: has getPosition capability, does not have setPosition capability
     * Asset position handling config property: defaults to true
     * Bring up the configuration dialog so the default Asset position handling configuration property of true, updates
     * the flag. Verify that the edit button shows up and we can successfully edit and sync the asset location.
     */
    @Test
    public void testGetPositionCapabilsConfigHandlePositionTrue() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        WebElement syncButton = m_Driver.findElement(By.cssSelector("button[id$='" + SYNC_LOC_BUT_ID + "']"));
        assertThat(syncButton.isEnabled(), is(true));
        
        //used to check if the edit location button is visible and clickable, and to make sure that inputs
        //for the location fields are editable
        Wait<WebDriver> locationEditWait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        //wait until the edit location button shows up
        locationEditWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebDriverWait applyWait = new WebDriverWait(driver, 10);
                applyWait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("button[id$='" + EDIT_LOC_BUT_ID + "']")));
                
                return true;
            }
        });
        
        //there is an update to the button right after it is visible, this just makes sure that
        //that staleness is accounted for and avoided..
        locationEditWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement editLocation = m_Driver.findElement(By.cssSelector("button[id$='" + EDIT_LOC_BUT_ID + "']"));
                editLocation.click();
                
                return true;
            }
        });
        
        //wait until the inputs are editable
        boolean result = locationEditWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement latitude = driver.findElement(By.cssSelector("input[id$='" + LAT_INPUT_ID + "']"));
                
                return latitude.isEnabled();
            }
        });
        
        assertThat(result, is(true));
        
        //Thread sleep needed to ensure that the location panel is done updating otherwise a stale element might occur.
        Thread.sleep(5000);
        
        WebElement latitude = m_Driver.findElement(By.cssSelector("input[id$='" + LAT_INPUT_ID +"']"));
        WebElement longitude = m_Driver.findElement(By.cssSelector("input[id$='" + LON_INPUT_ID + "']"));
        WebElement altitude = m_Driver.findElement(By.cssSelector("input[id$='" + ALT_INPUT_ID + "']"));
        WebElement bank = m_Driver.findElement(By.cssSelector("input[id$='" + BANK_INPUT_ID + "']"));
        WebElement heading = m_Driver.findElement(By.cssSelector("input[id$='" + HEAD_INPUT_ID + "']"));
        WebElement elevation = m_Driver.findElement(By.cssSelector("input[id$='" + ELV_INPUT_ID + "']"));
        
        //verify all inputs are now enabled
        assertThat(latitude.isEnabled(), is(true));
        assertThat(longitude.isEnabled(), is(true));
        assertThat(altitude.isEnabled(), is(true));
        assertThat(bank.isEnabled(), is(true));
        assertThat(heading.isEnabled(), is(true));
        assertThat(elevation.isEnabled(), is(true)); 
        
        latitude.clear();
        latitude.sendKeys("1");
        
        longitude.clear();
        longitude.sendKeys("2");
        
        altitude.clear();
        altitude.sendKeys("3");
        
        bank.clear();
        bank.sendKeys("4");
        
        elevation.clear();
        elevation.sendKeys("5");
        
        heading.clear();
        heading.sendKeys("6");
        
        //Update the location.
        m_Driver.findElement(By.cssSelector("button[id$='" + UPDATE_LOC_BUT_ID + "']")).click();
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        
        wait.until(ExpectedConditions.stalenessOf(latitude));
        wait.until(ExpectedConditions.stalenessOf(longitude));
        wait.until(ExpectedConditions.stalenessOf(altitude));
        wait.until(ExpectedConditions.stalenessOf(bank));
        wait.until(ExpectedConditions.stalenessOf(heading));
        wait.until(ExpectedConditions.stalenessOf(elevation));
        
        latitude = m_Driver.findElement(By.cssSelector("input[id$='" + LAT_INPUT_ID +"']"));
        longitude = m_Driver.findElement(By.cssSelector("input[id$='" + LON_INPUT_ID + "']"));
        altitude = m_Driver.findElement(By.cssSelector("input[id$='" + ALT_INPUT_ID + "']"));
        bank = m_Driver.findElement(By.cssSelector("input[id$='" + BANK_INPUT_ID + "']"));
        heading = m_Driver.findElement(By.cssSelector("input[id$='" + HEAD_INPUT_ID + "']"));
        elevation = m_Driver.findElement(By.cssSelector("input[id$='" + ELV_INPUT_ID + "']"));
         
        //verify all inputs are now not enabled
        assertThat(latitude.isEnabled(), is(false));
        assertThat(longitude.isEnabled(), is(false));
        assertThat(altitude.isEnabled(), is(false));
        assertThat(bank.isEnabled(), is(false));
        assertThat(heading.isEnabled(), is(false));
        assertThat(elevation.isEnabled(), is(false));
        
        assertThat(latitude.getAttribute("value"), is("1.000000"));
        assertThat(longitude.getAttribute("value"), is("2.000000"));
        assertThat(altitude.getAttribute("value"), is("3.000000"));
        assertThat(bank.getAttribute("value"), is("4.00"));
        assertThat(elevation.getAttribute("value"), is("5.00"));
        assertThat(heading.getAttribute("value"), is("6.00"));
        
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
    }
    
    /**
     * ExampleAsset: has getPosition capability, does not have setPosition capability
     * Asset position handling config property: set to false
     * Bring up the configuration dialog so the default Asset position handling configuration property of true, updates
     * the flag. Verify that the edit button shows up and we can successfully edit and sync the asset location.
     */
    @Test
    public void testGetPositionCapabilsConfigHandlePositionFalse() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        //used to check the location inputs are enabled and the default values are correct while avoiding
        //stale element references
        Wait<WebDriver> locationInputWait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        locationInputWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement latitude = m_Driver.findElement(By.cssSelector("input[id$='" + LAT_INPUT_ID +"']"));
                assertThat(latitude.getAttribute("value"), is(""));
                assertThat(latitude.isEnabled(), is(false));
                return true;
            }
        });
        
        locationInputWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement longitude = m_Driver.findElement(By.cssSelector("input[id$='" + LON_INPUT_ID + "']"));
                assertThat(longitude.getAttribute("value"), is(""));
                assertThat(longitude.isEnabled(), is(false));
                return true;
            }
        });
        
        locationInputWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement altitude = m_Driver.findElement(By.cssSelector("input[id$='" + ALT_INPUT_ID + "']"));
                assertThat(altitude.getAttribute("value"), is(""));
                assertThat(altitude.isEnabled(), is(false));
                return true;
            }
        });
        
        locationInputWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement bank = m_Driver.findElement(By.cssSelector("input[id$='" + BANK_INPUT_ID + "']"));
                assertThat(bank.getAttribute("value"), is(""));
                assertThat(bank.isEnabled(), is(false));
                return true;
            }
        });
        
        locationInputWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement heading = m_Driver.findElement(By.cssSelector("input[id$='" + HEAD_INPUT_ID + "']"));
                assertThat(heading.getAttribute("value"), is(""));
                assertThat(heading.isEnabled(), is(false));
                return true;
            }
        });
        
        locationInputWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement elevation = m_Driver.findElement(By.cssSelector("input[id$='" + ELV_INPUT_ID + "']"));
                assertThat(elevation.getAttribute("value"), is(""));
                assertThat(elevation.isEnabled(), is(false));
                return true;
            }
        });
        
        //verify edit location button shows up
        assertThat(m_Driver.findElements(By.cssSelector("button[id$='" + EDIT_LOC_BUT_ID + "']")).size(), is(1));
        
        //verify sync button shows up
        assertThat(m_Driver.findElements(By.cssSelector("button[id$='" + SYNC_LOC_BUT_ID + "']")).size(), is(1));
        
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
    }
    
    /**
     * ExampleObservationsAsset: does not have getPosition capability, or setPosition capability
     * Asset position handling config property: set to true
     * Verify that the text the position is not available is displayed and the edit location button does not
     * show up and the sync button does not show up.
     */
    @Test
    public void testNoGetLocationCapabilsConfigHandlePositionTrue() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, "ExampleObservationsAsset", "testExampleAsset");
        
        //set config property to true to use the asset plugin
        WebElement configButton = m_Driver.findElement(By.cssSelector("button[id*='configurationButton']"));
        configButton.click();
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("assetEditConfiguration")));
        
        WebElement configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        WebElement configTable = configDialog.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        ConfigurationHelper.updateValue(configTable, "Plugin overrides position", true);
        
        configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        WebElement saveConfig = configDialog.findElement(By.cssSelector("button[id*='saveConfig']"));
        saveConfig.click();
        
        //TODO TH-1189: Alter to verify save requested has been completed.
        //Sleep needed. Ajax may not be finished updating the page at the point an attempt is made to remove the 
        //asset.
        Thread.sleep(7000);
        
        //---refresh is required in order for asset location display to be updated---
        m_Driver.get(m_Driver.getCurrentUrl());
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("span[id$='positionNotAvailable']")));
        
        //verify edit location button doesn't show up
        assertThat(m_Driver.findElements(By.cssSelector("button[id$='" + EDIT_LOC_BUT_ID + "']")).size(), is(0));
        
        //verify sync button doesn't show up
        assertThat(m_Driver.findElements(By.cssSelector("button[id$='" + SYNC_LOC_BUT_ID + "']")).size(), is(0));
        
        //verify that the asset location is not available text shows up
        assertThat(m_Driver.findElements(By.cssSelector("span[id$='positionNotAvailable']")).size(), is(1));
        assertThat(m_Driver.findElement(By.cssSelector("span[id$='positionNotAvailable']")).getText(), 
                is("Position is not available"));
 
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
    }
}
