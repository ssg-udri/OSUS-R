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
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.ChannelsHelper;
import mil.dod.th.ose.gui.integration.helpers.ConfigurationHelper;
import mil.dod.th.ose.gui.integration.helpers.ControllerHelper;
import mil.dod.th.ose.gui.integration.helpers.GeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.ImageHelper;
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
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selenium test for the asset page. Test creating an asset, and various operations on the asset that can be performed
 * through the gui.
 * 
 * @author matt
 */
public class TestAssetPage
{
    private static WebDriver m_Driver;
    private static WebDriverWait wait;
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        wait = new WebDriverWait(m_Driver, 15);
        
        //Navigate to asset page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        NavigationHelper.collapseSideBars(m_Driver);
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
     * Assuming default controller is the active controller, assuming example asset and example platform 
     * bundles are in the controller. Verify the example exception asset and example asset show up in the asset type 
     * list.. verify we can create a new asset with the example asset type.
     */
    @Test
    public void testCreateAssetDialog()
    {
        WebElement addAssetButton = m_Driver.findElement(By.cssSelector("button[id*='addAsset']"));
        addAssetButton.click();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id*='addAssetDialog']")));
        
        //refresh the asset types 
        WebElement refreshTypesButton = m_Driver.findElement(By.cssSelector("button[id*='refreshAssetTypes']"));
        refreshTypesButton.click();
        
        WebElement createAssetPanel = m_Driver.findElement(
                By.cssSelector("span[id='createAssetForm:createAssetPanel']"));
        
        //Asset panel may or may not go stale due to being updated by the refresh types button being clicked. This is
        //due to the speed at which the web driver may move. Therefore the wait may timeout if it does not go stale.
        try
        {
            wait.until(ExpectedConditions.stalenessOf(createAssetPanel));
        }
        catch (final TimeoutException exception)
        {
            //Do nothing
        }
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        //Monitor the asset types list for the specified asset type.
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return AssetHelper.retrieveAssetType(driver, "ExampleAsset") != null;
            }
        });
        
        WebElement exampleAsset = AssetHelper.retrieveAssetType(m_Driver, "ExampleAsset");
        
        assertThat(exampleAsset, is(notNullValue()));
        exampleAsset.click();
        
        //make sure text changes
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(
                "span[id='createAssetForm:factoryName']"), "ExampleAsset"));
        
        //---click on the spec button verify information is correct---
        GeneralHelper.safeClickBySelector(By.cssSelector("button[id*='assetSpecsButton']"));
        
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement capabilHeader = driver.findElement(By.cssSelector("div[id*='assetCapabilitiesHeader']"));
                WebElement header = capabilHeader.findElement(By.cssSelector("span[class='ui-dialog-title']"));
                return header.getText().equals("Capabilities Document");
            }
        });

        //--verify capabilities are displayed---
        WebElement capabilHeader = m_Driver.findElement(By.cssSelector("div[id*='assetCapabilitiesHeader']"));
        WebElement assetCapabils = capabilHeader.findElement(By.cssSelector("div[id*='assetTreeTable']"));
        assertThat(assetCapabils.getText().isEmpty(), is(false));
        
        //--verify specific item to capabilities
        //TODO: TH-1274 Improve testing.
        WebElement caps = capabilHeader.findElement(By.cssSelector("div[id*='TreeTable']"));
        assertThat(caps.getText().isEmpty(), is(false));
        assertThat(caps.getText(), containsString("ExampleAsset"));
        
        //---close the dialog---
        WebElement closeDialog = capabilHeader.findElement(
                By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        assertThat(closeDialog.isDisplayed(), is(true));
        closeDialog.click();
        
        WebElement exampleAssetElement = null;
        
        //make sure that an exampleAsset type exists
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id*='assetTypeList_content']")));
        for (WebElement assetType : m_Driver.findElements(By.cssSelector("a[id*='assetTypeList']")))
        {
            if (assetType.getText().contains("ExampleAsset"))
            {
                exampleAssetElement = assetType;
                break;
            }
        }
        
        assertThat(exampleAssetElement, is(notNullValue()));
        exampleAssetElement.click();
        
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("span[id*='factoryName']"), 
                "ExampleAsset"));
        
        //---close the dialog---
        WebElement addAssetDialog = m_Driver.findElement(By.cssSelector("div[id*='addAssetDialog']"));
        closeDialog = addAssetDialog.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        assertThat(closeDialog.isDisplayed(), is(true));
        closeDialog.click();
    }
    
    /**
     * Verify the correct images shows up for different asset types in the add asset dialog.
     */
    @Test
    public void testCreateAssetDialogImage()
    {
        WebElement addAssetButton = m_Driver.findElement(By.cssSelector("button[id*='addAsset']"));
        addAssetButton.click();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id*='addAssetDialog']")));
        
        //refresh the asset types 
        WebElement refreshTypesButton = m_Driver.findElement(By.cssSelector("button[id*='refreshAssetTypes']"));
        refreshTypesButton.click();
        
        WebElement createAssetPanel = m_Driver.findElement(
                By.cssSelector("span[id='createAssetForm:createAssetPanel']"));
        
        //Asset panel may or may not go stale due to being updated by the refresh types button being clicked. This is
        //due to the speed at which the web driver may move. Therefore the wait may timeout if it does not go stale.
        try
        {
            wait.until(ExpectedConditions.stalenessOf(createAssetPanel));
        }
        catch (final TimeoutException exception)
        {
            //Do nothing
        }
        
        int assetTypeCount = m_Driver.findElements(By.cssSelector("a[id*='assetTypeList']")).size();
        assertThat(assetTypeCount, greaterThan(0));

        Map<String, String> assetTypeImageMap = new HashMap<String, String>();
        for (int assetTypeIdx = 0; assetTypeIdx < assetTypeCount; assetTypeIdx++)
        {
            WebElement assetTypeLink = GeneralHelper.scrollElementIntoView(
                    By.cssSelector(String.format("a[id*='assetTypeList:%d']", assetTypeIdx)));
            
            List<WebElement> assetTypeTables = 
                    m_Driver.findElements(By.cssSelector("div[id='createAssetForm:assetTypeList_content'] > table"));
            WebElement assetIcon = assetTypeTables.get(assetTypeIdx).findElement(By.tagName("img"));
            
            if (assetTypeLink.getText().contains("ExampleObservationsAsset"))
            {
                assertThat(assetIcon.isDisplayed(), is(true));
                String source = assetIcon.getAttribute("src");
                String img = ImageHelper.getPictureOrIconName(source, ".png");
                
                assetTypeImageMap.put("ExampleObservationsAsset", img);
            }
            else if (assetTypeLink.getText().contains("ExampleAsset"))
            {
                assertThat(assetIcon.isDisplayed(), is(true));
                String source = assetIcon.getAttribute("src");
                String img = ImageHelper.getPictureOrIconName(source, ".png");
                
                assetTypeImageMap.put("ExampleAsset", img);
            }
        }
        
        assertThat(assetTypeImageMap, hasEntry("ExampleObservationsAsset","acoustic.png"));
        assertThat(assetTypeImageMap, hasEntry("ExampleAsset","magnetic.png"));
        
        //---close the dialog---
        WebElement addAssetDialog = m_Driver.findElement(By.cssSelector("div[id*='addAssetDialog']"));
        WebElement closeDialog = addAssetDialog.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        assertThat(closeDialog.isDisplayed(), is(true));
        closeDialog.click();
    }
    
    /**
     * Verify the correct images shows up for assets on the configuration page.
     */
    @Test
    public void testAssetConfigurationImage() throws InterruptedException
    {
        //---verify ExampleAsset images are correct in the configuration section and asset list section---
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        WebElement configDiv = m_Driver.findElement(By.cssSelector("div[id*='assetConfig']"));
        
        WebElement assetConfigImg = configDiv.findElement(By.tagName("img"));
        
        ImageHelper.getPictureOrIconNameAndVerify(assetConfigImg, "magnetic.png", ".png");
       
        WebElement dataList = m_Driver.findElement(By.cssSelector("tbody[id*='dataList_data']"));
        
        assetConfigImg = dataList.findElement(By.tagName("img"));
        ImageHelper.getPictureOrIconNameAndVerify(assetConfigImg, "magnetic.png", ".png");
        
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
        
        //---verify ExampleObservationsAsset images are correct in the configuration section and asset list section---
        AssetHelper.createAsset(m_Driver, "ExampleObservationsAsset", "testExampleObservationsAsset");
        
        configDiv = m_Driver.findElement(By.cssSelector("div[id*='assetConfig']"));
        
        assetConfigImg = configDiv.findElement(By.tagName("img"));
        ImageHelper.getPictureOrIconNameAndVerify(assetConfigImg, "acoustic.png", ".png");
        
        dataList = m_Driver.findElement(By.cssSelector("tbody[id*='dataList_data']"));
        
        assetConfigImg = dataList.findElement(By.tagName("img"));
        ImageHelper.getPictureOrIconNameAndVerify(assetConfigImg, "acoustic.png", ".png");
        
        AssetHelper.removeAsset(m_Driver, "testExampleObservationsAsset");
    }
    
    /**
     * Verify that the asset name can be edited, verify the cancel button works properly.
     */
    @Test
    public void testEditAssetName() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        //---edit asset name---
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("img[id$='assetNameImg']")));
        WebElement editImg = m_Driver.findElement(By.cssSelector("img[id$='assetNameImg']"));
        editImg.click();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[id$='assetName']")));
        
        WebElement assetName = m_Driver.findElement(By.cssSelector("input[id$='assetName']"));
        assetName.sendKeys("testAsset");
        
        WebElement editPanel = m_Driver.findElement(By.cssSelector("span[id$='editPanel']"));
        
        //verify confirm and cancel buttons show up
        WebElement cancelButton = editPanel.findElement(By.cssSelector("button[title='Cancel']"));
        
        WebElement saveButton = editPanel.findElement(By.cssSelector("button[title='Save']"));
        
        saveButton.click();
        
        //wait until asset updates
        wait.until(ExpectedConditions.stalenessOf(saveButton));
        
        //verify name changed
        wait.until(ExpectedConditions.textToBePresentInElementValue(By.cssSelector("input[id$='assetName']"), 
                "testAsset"));
        
        editPanel = m_Driver.findElement(By.cssSelector("span[id$='editPanel']"));
        
        //---begin to edit asset name and cancel---
        //Wait needed since the edit button may be stale due to the name changing being changed. At the point the edit
        //button is clicked ajax may not be finished updating the asset panel.
        Wait<WebDriver> editWait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).pollingEvery(2,
                TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, NoSuchElementException.class);
        editWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                driver.findElement(By.cssSelector("img[id$='assetNameImg']")).click();
                return true;
            }
        });
        
        editWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return driver.findElement(By.cssSelector("input[id$='assetName']")).isDisplayed();
            }
        });
        
        assetName = m_Driver.findElement(By.cssSelector("input[id$='assetName']"));
        assetName.clear();
        assetName.sendKeys("garbage");
        
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[title='Cancel']")));
        //Thread sleep required due to stale element since the cancel button may not be finished updating when it
        //is clicked.
        GeneralHelper.sleepWithText(3, "Waiting for cancel button to be finished updating.");
        editPanel = m_Driver.findElement(By.cssSelector("span[id$='editPanel']"));
        cancelButton = editPanel.findElement(By.cssSelector("button[title='Cancel']"));
        cancelButton.click();
        
        //html will appear in text if a sleep is not used here
        GeneralHelper.sleepWithText(2, "html will appear in text if a sleep is not used here");
        
        //---verify asset name didn't change---
        
        assertThat(editPanel.getText(), is("testAsset"));
        
        AssetHelper.removeAsset(m_Driver, "testAsset");
    }
    
    /**
     * Verify the asset's BIT can be run and the status updates appropriately.
     */
    @Test
    public void testAssetBITStatusChanged() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        //---verify asset status is unknown and it is deactivated---
        WebElement assetElement = AssetHelper.retrieveAsset(m_Driver, "testExampleAsset");
        assertThat(assetElement, is(notNullValue()));
        WebElement assetStatusDesc = assetElement.findElement(By.cssSelector("span[id*='assetStatusDescription']"));
        assertThat(assetStatusDesc.getText(), is("A status has not been established."));
        WebElement assetActiveStatus = assetElement.findElement(By.cssSelector("span[id*='assetActiveStatus']"));
        assertThat(assetActiveStatus.getText(), is("DEACTIVATED"));
        
        //---perform BIT---
        WebElement performBit = m_Driver.findElement(By.cssSelector("button[id*='performBit']"));
        GrowlVerifier.verifyAndWaitToDisappear(20, performBit, "Asset completed BIT test.");
        
        //---verify status changed to GOOD---
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("span[id$='assetStatusDescription']"), "BIT Passed"));
        assetElement = AssetHelper.retrieveAsset(m_Driver, "testExampleAsset");
        assertThat(assetElement, is(notNullValue()));
        //verify status light
        WebElement statusLed = assetElement.findElement(By.cssSelector("div[id*='assetStatusLed']"));
        assertThat(statusLed.isDisplayed(), is(true));
        //the status led class expected
        WebElement statusDiv = assetElement.findElement(By.cssSelector("div[class='led-GOOD']"));
        assertThat(statusDiv.isDisplayed(), is(true));
        
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
    }
    
    /**
     * Verify the asset can be activated and the status updates appropriately.
     * Verify the asset can be deactivated and the status updates appropriately.
     */
    @Test
    public void testActivateDeactivateAsset() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        AssetHelper.activateAsset(m_Driver, "testExampleAsset");
        
        WebElement assetPanel = AssetHelper.retrieveAsset(m_Driver, "testExampleAsset");
        WebElement statusDiv = assetPanel.findElement(By.cssSelector("div[class='led-GOOD']"));
        assertThat(statusDiv.isDisplayed(), is(true));
        
        AssetHelper.deactivateAsset(m_Driver, "testExampleAsset");
        
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
    }
    
    /**
     * Assuming an Asset was created using the exampleAsset type. Verify the example exception asset and example asset 
     * show up in the asset type list.. verify we can cancel removing an asset and verify the asset can be removed.
     */
    @Test
    public void testRemoveAsset() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        //remove asset
        WebElement removeButton = m_Driver.findElement(By.cssSelector("button[id*='removeButton']"));
        removeButton.click();
        
        //cancel removing the asset and verify the asset is still present
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[id*='cancelRemoveAsset']")));
        WebElement cancelRemove = m_Driver.findElement(By.cssSelector("button[id*='cancelRemoveAsset']"));
        cancelRemove.click();
        
        //verify asset is still present
        WebElement editPanel = m_Driver.findElement(By.cssSelector("span[id*='editPanel']"));
        assertThat(editPanel.getText(), is("testExampleAsset"));
        
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
        
        //---verify asset is gone---
        WebElement tabView = m_Driver.findElement(By.cssSelector("div[id*='tabView']"));
        WebElement noRecords = tabView.findElement(By.cssSelector("div[id*='assetObjData']"));
        assertThat(noRecords.getText(), is("No records found."));
    }
    
    /**
     * Verify the asset's location can be set and synced.
     */
    @Test
    public void testAssetLocation() throws InterruptedException, ExecutionException, 
        java.util.concurrent.TimeoutException
    {
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "locationAsset");
        
        //---verify asset has been created ---
        WebElement assetElement = AssetHelper.retrieveAsset(m_Driver, "locationAsset");
        assertThat(assetElement, is(notNullValue()));
        
        //---click the edit location button---
        WebElement locationEdit = m_Driver.findElement(By.cssSelector("button[id*='editLocationButton']"));
        locationEdit.click();
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[id*='updateLocationButton']")));
        
        //update the values
        WebElement lat = m_Driver.findElement(By.cssSelector("input[id$='assetLatLocation']"));
        GeneralHelper.retrySendKeys(m_Driver, lat, "4", 10);
        WebElement longitude = m_Driver.findElement(By.cssSelector("input[id$='assetLongLocation']"));
        GeneralHelper.retrySendKeys(m_Driver, longitude, "3.5", 10);
        WebElement alt = m_Driver.findElement(By.cssSelector("input[id$='assetAltitudeLocation']"));
        GeneralHelper.retrySendKeys(m_Driver, alt, "3.9", 10);
        WebElement head = m_Driver.findElement(By.cssSelector("input[id$='assetHeadingLocation']"));
        GeneralHelper.retrySendKeys(m_Driver, head, "6", 10);
        WebElement ele = m_Driver.findElement(By.cssSelector("input[id$='assetElevationLocation']"));
        GeneralHelper.retrySendKeys(m_Driver, ele, "9.9", 10);
        WebElement bank = m_Driver.findElement(By.cssSelector("input[id$='assetBankLocation']"));
        GeneralHelper.retrySendKeys(m_Driver, bank, "0.8", 10);

        WebElement updateLocation = m_Driver.findElement(By.cssSelector("button[id*='updateLocationButton']"));
        updateLocation.click();
        
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("button[id*='editLocationButton']"), "Edit Location"));

        //now disconnect from the controller so we can be sure the values can be requested
        ChannelsHelper.removeSocketChannel(m_Driver, "localhost", "4000", true);
        //..reconnect
        ControllerHelper.createController(m_Driver);
        
        //Navigate to asset page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        
        //grab the asset
        assetElement = AssetHelper.retrieveAsset(m_Driver, "locationAsset");
        assertThat(assetElement, is(notNullValue()));
        
        //request the previously set values to be synced
        WebElement syncPosition = m_Driver.findElement(By.cssSelector("button[id*='syncLocationButton']"));
        syncPosition.click();
        
        //can't wait for text to appear because of the boxes being disabled
        GeneralHelper.sleepWithText(5, "Waiting for command to execute and update values.");
        
        //verify values, need to get attribute because boxes are disabled
        WebElement syncedLat = m_Driver.findElement(By.cssSelector("input[id$='assetLatLocation']"));
        assertThat(syncedLat.getAttribute("value"), is("4.000000"));
        WebElement syncedLong = m_Driver.findElement(By.cssSelector("input[id$='assetLongLocation']"));
        assertThat(syncedLong.getAttribute("value"), is("3.500000"));
        WebElement syncedAlt = m_Driver.findElement(By.cssSelector("input[id$='assetAltitudeLocation']"));
        assertThat(syncedAlt.getAttribute("value"), is("3.900000"));
        WebElement syncedHead = m_Driver.findElement(By.cssSelector("input[id$='assetHeadingLocation']"));
        assertThat(syncedHead.getAttribute("value"), is("6.00"));
        WebElement syncedEle = m_Driver.findElement(By.cssSelector("input[id$='assetElevationLocation']"));
        assertThat(syncedEle.getAttribute("value"), is("9.90"));
        WebElement syncedBank = m_Driver.findElement(By.cssSelector("input[id$='assetBankLocation']"));
        assertThat(syncedBank.getAttribute("value"), is("0.80"));
        
        AssetHelper.removeAsset(m_Driver, "locationAsset");
    }
    
    /**
     * Verify that an asset with required configuration properties can be created through the GUI and that the 
     * appropriate configuration values are actually set.
     */
    @Test
    public void testCreateAssetWithRequiredProps() throws InterruptedException
    {
        final Map<String, Object> props = new HashMap<>();
        props.put("Activate on startup", true);
        props.put("Plugin overrides position", true);
        props.put("hostname", "127.0.0.1");
        props.put("portNumber", "1337");
        
        //---Create asset that has required properties---
        AssetHelper.createAsset(m_Driver, "ExampleRequiredPropAsset", "reqPropAsset", props);
        
        //---Verified asset was actually created---
        final WebElement asset = AssetHelper.retrieveAsset(m_Driver, "reqPropAsset");
        assertThat(asset, is(notNullValue()));
        
        //---bring up configuration dialog---
        asset.findElement(By.cssSelector("button[id*='configurationButton']")).click();
        
        //---verify header is visible---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("assetEditConfiguration")));     
        
        //---retrieve the configuration table used to verify appropriate configuration values are being used---
        WebElement configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        WebElement configTable = configDialog.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        //---verify values were persisted---
        assertThat(ConfigurationHelper.getBooleanButtonValue(configTable, "Activate on startup"), is(true));
        assertThat(ConfigurationHelper.getBooleanButtonValue(configTable, "Plugin overrides position"), is(true));
        assertThat(ConfigurationHelper.getInputBoxValue(configTable, "hostname"), is("127.0.0.1"));
        assertThat(ConfigurationHelper.getInputBoxValue(configTable, "portNumber"), is("1337"));
        
        //---close the configuration dialog after verifying properties---
        configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        WebElement cancelConfig = configDialog.findElement(By.cssSelector("button[id*='cancelConfig']"));
        cancelConfig.click();
        
        //---verify dialog is closed---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='configurationButton']")));
        
        //---deactivate asset since it should automatically start due to the configuration properties---
        AssetHelper.deactivateAsset(m_Driver, "reqPropAsset");
        AssetHelper.removeAsset(m_Driver, "reqPropAsset");
    }
}

