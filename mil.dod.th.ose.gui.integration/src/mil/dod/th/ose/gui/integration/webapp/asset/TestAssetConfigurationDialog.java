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

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.ConfigurationHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Test asset configuration dialog functionality.
 * @author matt
 *
 */
public class TestAssetConfigurationDialog
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void beforeClass() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
    }
    
    @AfterClass
    public static void afterClass() throws InterruptedException
    {   
        AssetHelper.removeAllAssets(m_Driver);
    }
    
    /**
     * Verify configuration dialog can correctly set configuration values for all properties for the asset. 
     * Verify that canceling the dialog will clear out all changed values.
     * *Not testing uuid because there seems to be a problem with changing a uuid*
     */
    @Test
    public void testAssetConfiguration() throws InterruptedException
    {
        NavigationHelper.collapseSideBars(m_Driver);
        AssetHelper.createAsset(m_Driver, "ExampleAsset", "testExampleAsset");
        
        NavigationHelper.collapseSideBars(m_Driver);
        
        //---bring up configuration dialog---
        WebElement configButton = m_Driver.findElement(By.cssSelector("button[id*='configurationButton']"));
        configButton.click();
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 30);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("assetEditConfiguration")));
        
        WebElement configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        
        //---verify header is visible---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("assetEditConfiguration")));
        
        WebElement configDialogHeader = configDialog.findElement(By.cssSelector("span[class='ui-dialog-title']"));
        assertThat(configDialogHeader.getText(), is("Asset Configuration"));
        
        //---verify configuration values section is correct---
        //verify values in dialog match expected values
        By selector = By.id("assetEditConfiguration");
        ConfigurationHelper.verifyDialogValues(m_Driver, selector, "Activate on startup", 
                "Determines whether the asset should be activated during startup (Boolean)");
        ConfigurationHelper.verifyDialogValues(m_Driver, selector, "Plugin overrides position",
                "When this property is false, position is managed by the core for the asset instance. When true, "
              + "position will be managed by the plug-in. (Boolean)");
        
        ConfigurationHelper.verifyDialogValues(m_Driver, selector, "Device Power Toggle Name",
                "Name of device power toggle to use (String)");
        
        WebElement configTable = configDialog.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        // verify default values
        assertThat(ConfigurationHelper.getInputBoxValue(configTable, "Device Power Toggle Name"),
                    is("invalid, update this value!"));

        // update config values
        ConfigurationHelper.updateValue(configTable, "Activate on startup", true);
        ConfigurationHelper.updateValue(configTable, "Plugin overrides position", true);
        ConfigurationHelper.updateValue(configTable, "Device Power Toggle Name", "powerName");
        ConfigurationHelper.updateValue(configTable, "Example choice", "Option b");
        
        //---save the configuration and verify that the values persist---
        configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        WebElement saveConfig = configDialog.findElement(By.cssSelector("button[id*='saveConfig']"));
        saveConfig.click();
        
        //verify dialog is closed
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("button[id*='saveConfig']")));
        
        //TODO TH-1265 problem with clicking this element right away
        Thread.sleep(5000);
        //---bring up configuration dialog---
        m_Driver.findElement(By.cssSelector("button[id*='configurationButton']")).click();
        
        //---verify header is visible---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("assetEditConfiguration")));     
        
        configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        configTable = configDialog.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        // verify value persisted before
        assertThat(ConfigurationHelper.getBooleanButtonValue(configTable, "Activate on startup"), is(true));
        assertThat(ConfigurationHelper.getBooleanButtonValue(configTable, "Plugin overrides position"), is(true));
        assertThat(ConfigurationHelper.getInputBoxValue(configTable, "Device Power Toggle Name"), is("powerName"));
        assertThat(ConfigurationHelper.getSelectOneListBoxValue(configTable, "Example choice"), is("Option b"));
        
        // set to new value
        ConfigurationHelper.updateValue(configTable, "Activate on startup", false);
        ConfigurationHelper.updateValue(configTable, "Plugin overrides position", false);
        ConfigurationHelper.updateValue(configTable, "Device Power Toggle Name", "failText");
        ConfigurationHelper.updateValue(configTable, "Example choice", "Option c");
        
        //---verify cancellation of config does not persist new values---
        configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        WebElement cancelConfig = configDialog.findElement(By.cssSelector("button[id*='cancelConfig']"));
        cancelConfig.click();
        
        //verify dialog is closed
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='configurationButton']")));
        
        //used to bring up the configuration dialog again after saving configuration due to staleness
        //of elements caused from updates
        Wait<WebDriver> configUpdate = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        configUpdate.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                //---bring up configuration dialog---
                WebElement configButton = m_Driver.findElement(By.cssSelector("button[id*='configurationButton']"));
                configButton.click();
                
                return true;
            }
        });
        
        //---verify header is visible---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("assetEditConfiguration")));
        
        configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        configTable = configDialog.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        // verify value persisted from before
        assertThat(ConfigurationHelper.getBooleanButtonValue(configTable, "Activate on startup"), is(true));
        assertThat(ConfigurationHelper.getBooleanButtonValue(configTable, "Plugin overrides position"), is(true));
        assertThat(ConfigurationHelper.getInputBoxValue(configTable, "Device Power Toggle Name"), is("powerName"));
        assertThat(ConfigurationHelper.getSelectOneListBoxValue(configTable, "Example choice"), is("Option b"));

        configDialog = m_Driver.findElement(By.id("assetEditConfiguration"));
        cancelConfig = configDialog.findElement(By.cssSelector("button[id*='cancelConfig']"));
        cancelConfig.click();
    }
}
