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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * Asset helper to create a new asset with assumptions that an active controller is set that has the example asset 
 * and example platform bundles.
 * 
 * @author matt
 *
 */
public class AssetHelper
{
    /**
     * ID for the configuration tab on the assets page.
     */
    public static final String ASSET_CONFIG_TAB_ID = "assetConfig";
    
    /**
     * ID for the observation tab on the assets page
     */
    public static final String ASSET_OBERSVATION_TAB_ID = "assetObsTab";
    
    /**
     * ID for the command and control tab on the assets page.
     */
    public static final String ASSET_COMMAND_TAB_ID = "assetCommandTab";
    
    private static final Logger LOG = Logger.getLogger("asset.helper");
    
    /**
     * Assuming default controller is the active controller, assuming the correct example asset and example platform 
     * bundles are in the controller. Create a new asset type specified by assetTypeName. Will wait until asset is 
     * created will still be on the asset page.
     */
    public static void createAsset(final WebDriver driver, final String assetTypeName, final String desiredName) 
            throws InterruptedException 
    {
        createAsset(driver, assetTypeName, desiredName, null);
    }
    
    /**
     * Create a new asset with the specified name, types, and configuration property values. Will wait until asset is 
     * created and will remain on the asset page. This method assumes an active controller the the correct asset
     * bundles are on the controller.
     */
    public static void createAsset(final WebDriver driver, final String assetTypeName, final String desiredName,
             Map<String, Object> props) 
        throws InterruptedException
    {
        //Instantiate web driver wait and fluent wait.
        WebDriverWait wait = new WebDriverWait(driver, 10);
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(20, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        //Initial page/tab check.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, ASSET_CONFIG_TAB_ID);
        
        //Bring up the add asset dialog.
        driver.findElement(By.cssSelector("button[id*='addAsset']")).click();
        
        //Wait till the add asset dialog is visible.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[id*='assetTypeList']")));

        ///refresh the asset types 
        driver.findElement(By.cssSelector("button[id*='refreshAssetTypes']")).click();

        //Retrieve the specified asset type once it is verified that it exists.
        WebElement desiredAssetTypeElement = retrieveAssetType(driver, assetTypeName);
        
        //Select the specified asset type.
        assertThat(desiredAssetTypeElement, is(notNullValue()));
        desiredAssetTypeElement.click();
        
        //Wait till the dialog reflects that the asset type has been selected.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span[id*='factoryName']")));
        
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("span[id*='factoryName']"), 
                assetTypeName));
        
        //create asset with name specified.
        WebElement assetExampleName = driver.findElement(By.cssSelector("input[id*='aName']"));
        assetExampleName.clear();
        assetExampleName.sendKeys(desiredName);
        
        //click ok to create new asset.
        driver.findElement(By.cssSelector("button[id$='configureAsset']")).click();
        
        //Wait till the configure new asset dialog is visible.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("span[id*='newAssetconfigPanel']")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("tbody[id$='compositeConfigTable_data']")));
        
        //Alter asset configuration properties.
        if (props != null && !props.isEmpty())
        {
            for (String key: props.keySet())
            {
                final WebElement assetConfigPanel = driver.findElement(
                        By.cssSelector("span[id$='newAssetconfigPanel']"));
                final WebElement configTable = assetConfigPanel.findElement(
                        By.cssSelector("tbody[id$='compositeConfigTable_data']"));
                ConfigurationHelper.updateValue(configTable, key, props.get(key));
            }
        }
        
        //Wait till the dialog add button is visible to click it.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id$='newAssetCreate']")));
        
        //click add to create new asset.
        GrowlVerifier.verifyAndWaitToDisappear(20, driver.findElement(By.cssSelector("button[id$='newAssetCreate']")), 
                "Asset created.");

        
        //Sleep for a second to ensure that AJAX has completed.
        Thread.sleep(1000);
        
        //Wait for the created asset to appear in the assets list.
        Boolean result = fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return retrieveAsset(driver, desiredName) != null;
            }
        });
        
        assertThat(result, is(true));
    }
    
    /**
     * Assuming an asset with the given name is already created, activate the asset.
     */
    public static void activateAsset(final WebDriver driver, final String assetName) throws InterruptedException
    {
        //Initial page/tab check.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, ASSET_CONFIG_TAB_ID);
        
        //Retrieve the asset to be activated.
        WebElement asset = retrieveAsset(driver, assetName);
        if (asset != null)
        {
            //--activate the asset
            asset.findElement(By.cssSelector("button[id*='activationButton']")).click();
            
            Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                    pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                            StaleElementReferenceException.class);
            
            //Wait till the status of the asset is activated.
            Boolean result = fwait.until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    WebElement asset = retrieveAsset(driver, assetName);
                    WebElement status = asset.findElement(By.cssSelector("span[id*='assetActiveStatus']"));
                    return status.getText().matches("ACTIVATED");
                }
            });
            // TODO: TH-3085: Re-enable verification of activation status
            //assertThat(result, is(true));
            LOG.info("Asset activated: " + result);
            return;
        }
        throw new IllegalArgumentException("No asset found with name: " + assetName);
    }
    
    /**
     * Assuming an asset with the given name is already created and activated, remove the asset.
     */
    public static void deactivateAsset(final WebDriver driver, final String assetName) throws InterruptedException
    {
        //Initial page/tab check.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, ASSET_CONFIG_TAB_ID);
        
        //Retrieve the asset to be deactivated.
        WebElement asset = retrieveAsset(driver, assetName);
        if (asset != null)
        {
            //--deactivate the asset
            asset.findElement(By.cssSelector("button[id*='deactivationButton']")).click();
            
            Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                    pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                            StaleElementReferenceException.class);
            
            //Wait till the status of the asset is deactivated.
            Boolean result = fwait.until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    WebElement asset = retrieveAsset(driver, assetName);
                    WebElement status = asset.findElement(By.cssSelector("span[id*='assetActiveStatus']"));
                    return status.getText().matches("DEACTIVATED");
                }
            });
            // TODO: TH-3085: Re-enable verification of deactivation status
            //assertThat(result, is(true));
            LOG.info("Asset deactivated: " + result);
            return;
        }
        throw new IllegalArgumentException("No asset found with name: " + assetName);
    }
    
    /**
     * Assuming an asset with the given name is already created and deactivated, remove the asset.
     */
    public static void removeAsset(final WebDriver driver, final String assetName) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        
        //Initial page/tab check.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, ASSET_CONFIG_TAB_ID);
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(60, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        //Use a fluent wait to insure that the remove asset button actually gets clicked before continuing.
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                //---remove the asset---
                final WebElement asset = retrieveAsset(driver, assetName);
                if (asset != null)
                {
                    asset.findElement(By.cssSelector("button[id$='removeButton']")).click();
                    return true;
                }
                return false;
            }
        });
        
        //confirm we want to remove the asset
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='confirmDialog']")));
        driver.findElement(By.cssSelector("button[id$='removeAsset']")).click();
        
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return retrieveAsset(driver, assetName) == null;
            }
        });
    }
    
    /**
     * Function to capture data for a specified asset. If repeatInterval is greater than 0 then capture data
     * is clicked for the amount of times specified. Function assumes asset page is the current page. 
     * Method may return before all data has been returned to web GUI
     * 
     * @param driver
     *  the web driver that is to be used.
     * @param assetName
     *  the name of the asset that the action is to be performed on.
     * @param repeatInterval
     *  the number of times that capture data is to be clicked.
     */
    public static void assetCaptureData(final WebDriver driver, final String assetName, int repeatInterval) 
        throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, ASSET_CONFIG_TAB_ID);
        
        final WebElement assetPanel = retrieveAsset(driver, assetName);
        final WebElement captureDataButton = assetPanel.findElement(By.cssSelector("button[id$='captureData']"));
        
        //Click capture data equal to the repeat interval specified.
        for (int i = 0; i < repeatInterval; i++)
        {
            // must wait as the verifier doesn't distinguish between each capture growl message, if you don't wait, it
            // will count the very first capture growl message for each button click
            GrowlVerifier.verifyAndWaitToDisappear(30, captureDataButton, "Asset captured data!");
        }
           
    }
    
    /**
     * Function performs the BIT test for a specified asset. Function assumes asset page is the current page.
     * @param driver
     *  the web driver that is to be used.
     * @param assetName
     *  the name of the asset that the action is to be performed on.
     */
    public static void assetPerformBIT(final WebDriver driver, final String assetName) 
        throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, ASSET_CONFIG_TAB_ID);
        
        WebElement assetPanel = retrieveAsset(driver, assetName);
        assertThat(assetPanel, notNullValue());
        
        WebElement performBit = assetPanel.findElement(By.cssSelector("button[id$='performBit']"));
        
        GrowlVerifier.verifyAndWaitToDisappear(20, performBit, "Asset completed BIT test.");
    }
    
    /**
     * Method that finds and returns the WebElement that represents the asset. This method returns null if the
     * specified asset cannot be found.
     * 
     * @param driver
     *      Instance of the {@link WebDriver} the asset will be retrieved from.
     * @param assetName
     *      Name of the asset to be retrieved.
     * @return
     *      {@link WebElement} that represents the specified asset or <code>null</code> if it cannot be found.
     */
    public static WebElement retrieveAsset(final WebDriver driver, final String assetName)
    {      
        //Search the list for the asset with the matching name.
        for (final WebElement asset : driver.findElements(By.cssSelector("table[id$='assetTable']")))
        {
            if (asset.findElement(By.cssSelector("span[id*='editPanel']")).getText().equals(assetName))
            {
                return asset;
            }
        } 
        return null;
    }
    
    /**
     * Method that finds and returns the WebElement that represents the specified asset type on the add asset dialog.
     * This method returns null if the specified asset type cannot be found.
     * 
     * @param driver
     *      Instance if the {@link WebDriver} the asset type will be retrieved from.
     * @param assetTypeName
     *      Name of the asset type to be retrieved. 
     * @return
     *      {@link WebElement} that represents the specified asset type or <code>null</code> if it cannot be found.
     */
    public static WebElement retrieveAssetType(final WebDriver driver, final String assetTypeName)
    {
        //Retrieve the list of all asset types and search for an asset type with the specified name.
        int assetTypeCount = -1;
        int tempCount = 0;
        while (assetTypeCount != (tempCount = driver.findElements(By.cssSelector("a[id*='assetTypeList']")).size()))
        {
            assetTypeCount = tempCount;

            try
            {
                // Allow a small amount of time for the asset list to update and then check size again
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new IllegalStateException(e);
            }
        }

        for (int assetTypeIdx = 0; assetTypeIdx < assetTypeCount; assetTypeIdx++)
        {
            WebElement assetType = GeneralHelper.scrollElementIntoView(
                    By.cssSelector(String.format("a[id*='assetTypeList:%d']", assetTypeIdx)));

            if (assetType.getText().contains(assetTypeName))
            {
                return assetType;
            }
        }
        return null;
    }
    
    /**
     * Method that removes all assets found on the asset page.
     * @param driver
     *      Instance of the {@link WebDriver} to be used.
     */
    public static void removeAllAssets(final WebDriver driver) throws InterruptedException
    {
        //Initial page/tab check.
        checkAndCloseAddAssetDialog();
        
        //Create a list of names for all the assets.
        List<String> assetNamesList = new ArrayList<String>();
        for (WebElement asset: driver.findElements(By.cssSelector("table[id$='assetTable']")))
        {
            assetNamesList.add(asset.findElement(By.cssSelector("span[id*='editPanel']")).getText());
        }
        
        //Retrieve a list of all assets and remove them.
        for (final String assetName: assetNamesList)
        {
            WebElement assetToRetrieve = retrieveAsset(driver, assetName);
            
            if (assetToRetrieve != null)
            {
                final String status = 
                        assetToRetrieve.findElement(By.cssSelector("span[id*='assetActiveStatus']")).getText();
                // Check if asset is not deactivated, try to deactivate so it can be removed. If 
                // activating/deactivating, try anyways just in case.
                if (!status.equals("DEACTIVATED"))
                {
                    AssetHelper.deactivateAsset(driver, assetName);
                }
                //Attempt to remove specified asset.
                AssetHelper.removeAsset(driver, assetName);
            }
            else
            {
                LOG.log(Level.INFO, String.format(
                        "Cannot find asset [%s] when trying to remove it from the list.", assetName));
            }
        }
    }
    
    /**
     * Function navigates to a specified tab on the asset page. This function assumes that 
     * the asset page has been navigated to.
     * @param driver
     *  the web driver to use
     * @param constants
     *  the {@link AssetTabConstants} which identifies which tab to navigate to.
     */
    public static void chooseAssetTab(final WebDriver driver, final AssetTabConstants constants)
    {
        String assetTabAnchor = "";
        String assetTabDiv = "";
        if (constants.equals(AssetTabConstants.CONFIGURATION_TAB))
        {
            assetTabAnchor = "a[href*='assetConfig']";
            assetTabDiv = "div[id$='assetConfig']";
        }
        else if (constants.equals(AssetTabConstants.OBSERVATION_TAB))
        {
            assetTabAnchor = "a[href*='assetObsTab']";
            assetTabDiv = "div[id$='assetObsTab']";
        }
        else if (constants.equals(AssetTabConstants.COMMAND_TAB))
        {
            assetTabAnchor = "a[href*='assetCommandTab']";
            assetTabDiv = "div[id$='assetCommandTab']";
        }
        
        if (!driver.findElement(By.cssSelector(assetTabDiv)).isDisplayed())
        {
            LOG.log(Level.INFO, "Clicking asset tab [{0}]", assetTabAnchor);
            GeneralHelper.safeClickBySelector(By.cssSelector(assetTabAnchor));
            
            WebDriverWait wait = new WebDriverWait(driver, 30);
            wait.ignoring(NoSuchElementException.class);
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(assetTabDiv)));
        }
    }
    
    /**
     * Function that ensures that add asset dialog is closed.
     */
    public static void checkAndCloseAddAssetDialog() throws InterruptedException
    {
        final WebDriver driver = WebDriverFactory.retrieveWebDriver();
        
        //Initial page/tab check.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, ASSET_CONFIG_TAB_ID);
        
        final By addAssetDialogSelector = By.cssSelector("div[id='addAssetDialog']");
        final WebElement addAssetDialog = driver.findElement(addAssetDialogSelector);
        
        if (addAssetDialog.isDisplayed())
        {
            final WebElement closeDialogButton = 
                    addAssetDialog.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
            closeDialogButton.click();
            
            final WebDriverWait wait = new WebDriverWait(driver, 30);
            wait.ignoring(NoSuchElementException.class, StaleElementReferenceException.class);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(addAssetDialogSelector));
            
            //Wait just to ensure that the dialog box has closed and won't obstruct any other actions.
            Thread.sleep(500);
        }
    }
    
    /**
     * Enumeration which identifies which asset tab is needed.
     * Tabs are located on the asset page.
     * @author nickmarcucci
     *
     */
    public enum AssetTabConstants
    {
        CONFIGURATION_TAB,
        OBSERVATION_TAB,
        COMMAND_TAB
    };
}
