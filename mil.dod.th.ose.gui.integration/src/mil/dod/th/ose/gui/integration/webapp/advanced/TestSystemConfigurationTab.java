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
package mil.dod.th.ose.gui.integration.webapp.advanced;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.ConfigurationHelper;
import mil.dod.th.ose.gui.integration.helpers.GeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

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
 * Integration test for the configurations tab of the system configurations page.
 * 
 * @author cweisenborn
 */
public class TestSystemConfigurationTab
{
    private static WebDriver m_Driver;
    
    private static String m_PidToChange = "example.config.ExampleConfigComp";
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG);
        NavigationHelper.collapseSideBars(m_Driver);
        ConfigurationHelper.handleNavigateToConfigTab(m_Driver);
    }
    
    /**
     * Tests that test changing a configurations values and then deleting a configuration. This test method calls both
     * the change config test and delete config test due to the fact that the delete configuration test relies on the 
     * change config to to alter the values of a configuration. A configuration will not be able to be deleted until one
     * is created which does not happen until its values are altered.
     */
    @Test
    public void testChangeDeleteConfig() throws InterruptedException, ExecutionException, TimeoutException
    {
        WebDriver driver = WebDriverFactory.retrieveWebDriver();
        
        NavigationHelper.navigateToPage(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG);
        
        NavigationHelper.collapseSideBars(driver);

        ConfigurationHelper.handleNavigateToConfigTab(driver);
        
        assertChangeConfig();
        assertDeleteConfig();
    }
    
    /**
     * Test changing the values of a configuration.
     * Verify that the values are changed.
     */
    public void assertChangeConfig() throws InterruptedException, ExecutionException, TimeoutException
    {
        WebElement configurationPanel = ConfigurationHelper.findConfigurationPanel(m_PidToChange, false);
        
        //Expand the configuration so that the properties are available.
        WebElement expandConfig = configurationPanel.findElement(
                By.cssSelector("span[class='ui-icon ui-icon-plusthick']"));
        expandConfig.click();
        
        //Find the property table for the configuration.
        WebElement configPropsTable = configurationPanel.findElement(
                By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        WebDriverWait wait = new WebDriverWait(WebDriverFactory.retrieveWebDriver(), 5);
        wait.until(ExpectedConditions.visibilityOf(configPropsTable));
        
        ConfigurationHelper.updateValue(configPropsTable, "Some int", "75");
        ConfigurationHelper.updateValue(configPropsTable, "Some enum", "Value3");
        ConfigurationHelper.updateValue(configPropsTable, "Some bool", true);
        
        //Save changes to the configuration.
        WebElement saveConfig = configurationPanel.findElement(By.cssSelector("button[id*='saveConfig']"));
        
        wait.until(ExpectedConditions.visibilityOf(saveConfig));
        GrowlVerifier.verifyAndWaitToDisappear(20, saveConfig, "Properties Accepted:");
        
        GeneralHelper.sleepWithText(5, "Wait for AJAX to update panel after saving");
        
        configurationPanel = ConfigurationHelper.findConfigurationPanel(m_PidToChange, false);
        configPropsTable = configurationPanel.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        wait.until(ExpectedConditions.visibilityOf(configPropsTable));

        assertThat(ConfigurationHelper.getInputBoxValue(configPropsTable, "Some int"), is("75"));
        assertThat(ConfigurationHelper.getSelectOneListBoxValue(configPropsTable, "Some enum"), is("Value3"));
        assertThat(ConfigurationHelper.getBooleanButtonValue(configPropsTable, "Some bool"), is(true));
    }
    
    /**
     * Test deleting a configuration.
     * Verify that the values are returned to the default.
     * This test depends on the {@link #assertChangeConfig()} test to alter the values of the configuration first. 
     * If that test fails then this test will also fail since there will be no configuration to delete.
     */
    public void assertDeleteConfig() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        //Wait until the configuration table is available.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tbody[id*='configTable_data']")));

        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        //Delete the configuration.
        WebElement deleteButton = fwait.until(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                WebElement configPanel = ConfigurationHelper.findConfigurationPanel(m_PidToChange, false);
                return configPanel.findElement(By.cssSelector("button[id*='deleteConfig']"));
            }
        });
        assertThat(deleteButton, is (notNullValue()));
        deleteButton.click();
        ConfigurationHelper.handleDeleteConfigDialog(m_Driver, m_PidToChange, true);
        
        //Sleep need so controller has time to delete configuration and send response.
        Thread.sleep(2000);
        
        //Verify that config is new or unbound.
        Boolean unbound = fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement configPanel = ConfigurationHelper.findConfigurationPanel(m_PidToChange, false);
                WebElement unbound = configPanel.findElement(By.cssSelector("span[id*='unboundConfig']"));
                return unbound.getText().equals("Unbound or new configuration.");
            }
        });
        assertThat(unbound, is(true));
        
        // make sure values are defaults again
        WebElement configurationPanel = ConfigurationHelper.findConfigurationPanel(m_PidToChange, false);
        WebElement configPropsTable = 
                configurationPanel.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        wait.until(ExpectedConditions.visibilityOf(configPropsTable));
        
        assertThat(ConfigurationHelper.getInputBoxValue(configPropsTable, "Some int"), is("8"));
        assertThat(ConfigurationHelper.getSelectOneListBoxValue(configPropsTable, "Some enum"), is("Value1"));
        assertThat(ConfigurationHelper.getBooleanButtonValue(configPropsTable, "Some bool"), is(false));
        
        //Close the config panel.
        WebElement collapseConfig = configurationPanel.findElement(
                By.cssSelector("span[class='ui-icon ui-icon-minusthick']"));
        collapseConfig.click();
        
        //Wait for config panel to collapse.
        Thread.sleep(1000);
        assertThat(configPropsTable.isDisplayed(), is(false));
    }
    
    /**
     * Test deleting factory configurations.
     * Verify the configurations are actually removed.
     */
    @Test
    public void testDeleteFactoryConfigs() throws InterruptedException
    {
        final String factoryPid = "example.asset.ExampleAssetConfig";
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("Activate on startup", true);
        valuesMap.put("Plugin overrides position", true);
        valuesMap.put("Device Power Toggle Name", "Bob");
        ConfigurationHelper.createFactoryConfig(m_Driver, factoryPid, valuesMap);

        WebElement exampleAssetFactory = ConfigurationHelper.findConfigurationPanel(factoryPid, true);
        exampleAssetFactory.findElement(By.cssSelector("span[class='ui-icon ui-icon-plusthick']")).click();

        //Retrieve a list of all factory configs. There should be two.
        List<WebElement> factoryConfigs = exampleAssetFactory.findElements(
                By.cssSelector("div[class='ui-panel ui-widget ui-widget-content ui-corner-all ui-hidden-container']"));
        wait.until(ExpectedConditions.visibilityOf(factoryConfigs.get(0)));
        assertThat(factoryConfigs.size(), is(1));
        
        //Verify that the factory configs are actually serial port factory configs.
        for (WebElement config: factoryConfigs)
        {
            WebElement configHeader = config.findElement(By.cssSelector("span[class='ui-panel-title']"));
            assertThat(configHeader.getText().contains(factoryPid), is(true));
        }
        
        //Retrieve the first serial port config.
        WebElement asset1 = factoryConfigs.get(0);
        
        //Retrieve the PID of the first serial port.
        WebElement configHeader = asset1.findElement(By.cssSelector("span[class='ui-panel-title']"));
        String asset1PID = configHeader.getText();
        
        //Delete the first serial port config.
        WebElement deleteConfigButton = asset1.findElement(
                By.cssSelector("button[id*='deleteFactoryConfig']"));
        deleteConfigButton.click();
        
        //Verify canceling the delete.
        ConfigurationHelper.handleDeleteConfigDialog(m_Driver, asset1PID, false);
        
        deleteConfigButton.click();
        
        //Actually delete the configuration this time.
        ConfigurationHelper.handleDeleteConfigDialog(m_Driver, asset1PID, true);
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        fwait.until(new ExpectedCondition<Boolean>()
        {

            @Override
            public Boolean apply(final WebDriver driver)
            {
                //Find the event example asset factory.
                WebElement exampleAssetFactory = ConfigurationHelper.findConfigurationPanel(factoryPid, true);
                //Retrieve a list of all example asset configs, verify there are none.
                List<WebElement> factoryConfigs = exampleAssetFactory.findElements(
                        By.cssSelector(
                                "div[class='ui-panel ui-widget ui-widget-content ui-corner-all ui-hidden-container']"));
                return factoryConfigs.size() == 0;
            }
        });
        
        //Retrieve the example asset factory so that it might be collapsed.
        exampleAssetFactory = ConfigurationHelper.findConfigurationPanel(factoryPid, true);
        
        //Collapse the example asset factory so that the factory configurations can not be accessed.
        WebElement expandFactory = exampleAssetFactory.findElement(
                By.cssSelector("span[class='ui-icon ui-icon-minusthick']"));
        expandFactory.click();
    }
    
    /**
     * Verify correct dialog values. 
     * Verify the cancel button on the create configuration closes the dialog.
     * Verify the cancel button will not create a new configuration. 
     * 
     */
    @Test
    public void testCreateConfigDialog() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        //open create configuration dialog and record count of existing configurations
        String rowNum = ConfigurationHelper.createConfigDialogHelper("example.ccomms.ExamplePhysicalLinkConfig", true);
        WebElement configTable = m_Driver.findElement(By.cssSelector("tbody[id*='factoryConfigTable_data']"));
        int originalConfigCount = ConfigurationHelper.getConfigurationsList(rowNum, configTable).size();
        
        //wait until dialog is visible before interacting with it
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id='createFactoryConfig']")));
        
        //verify actual values in dialog match expected values
        By configDialogSelector = By.cssSelector("div[id='createFactoryConfig']");
        ConfigurationHelper.verifyDialogValues(m_Driver, configDialogSelector, "Read timeout ms",
                "Timeout value for read calls in milliseconds (Integer)");
        ConfigurationHelper.verifyDialogValues(m_Driver, configDialogSelector, "Data bits", 
                "How many bits of data in each byte read from the link (Integer)");
        
        //press the cancel button
        WebElement configDialog = m_Driver.findElement(configDialogSelector);
        WebElement cancelButton = configDialog.findElement(By.
                cssSelector("button[id='createFactoryForm:cancelConfig']"));
        wait.until(ExpectedConditions.visibilityOf(cancelButton));
        cancelButton.click();
        
        //verify no new configurations were created
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[id='createFactoryConfig']")));
        configTable = m_Driver.findElement(By.cssSelector("tbody[id*='factoryConfigTable_data']"));
        int newCount = ConfigurationHelper.getConfigurationsList(rowNum, configTable).size();
        assertThat(originalConfigCount, is(newCount)); //indicates no change in the number of configurations 
        
        WebElement config = ConfigurationHelper.findConfigurationPanel(
                "example.ccomms.ExamplePhysicalLinkConfig", true);
        WebElement configPropsTable = 
                config.findElement(By.cssSelector("tbody[class='ui-datatable-data ui-widget-content']"));
        WebElement collapse = config.findElement(By.cssSelector("span[class='ui-icon ui-icon-minusthick']"));
        collapse.click();
        
        //Wait for physical link config panel to collapse.
        Thread.sleep(1000);
        assertThat(configPropsTable.isDisplayed(), is(false));
    }
    
    /**
     * Verify inputting incorrect values according to the type specified results
     * in a validation error. 
     */
    @Test
    public void testCreateConfigValidation() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        //open create configuration dialog and record count of existing configurations
        By configTableSelector = By.cssSelector("tbody[id*='factoryConfigTable_data']");
        String rowNum = ConfigurationHelper.createConfigDialogHelper("example.ccomms.ExamplePhysicalLinkConfig", true);
        WebElement configTable = m_Driver.findElement(configTableSelector);
        int originalConfigCount = ConfigurationHelper.getConfigurationsList(rowNum, configTable).size();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id='createFactoryConfig']")));
        WebElement configDialog = m_Driver.findElement(By.cssSelector("div[id='createFactoryConfig']"));
        
        WebElement propsTable = m_Driver.findElement(By.
            cssSelector("form[id='createFactoryForm']>span>div>div>table>tbody"));
        //input for read timeout
        ConfigurationHelper.updateValue(propsTable, "Read timeout ms", "Droids");
        
        //input for data bits
        ConfigurationHelper.updateValue(propsTable, "Data bits", "Looking");
        
        //try to create a new configuration
        WebElement createButton = configDialog.findElement(By.
                cssSelector("button[id='createFactoryForm:createConfig']"));
        createButton.click();
        
        //Wait till error messages are present to continue.
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("span[class='ui-message-error-detail']")));
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        Boolean errorsFound = fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement configDialog = m_Driver.findElement(By.cssSelector("div[id='createFactoryConfig']"));
                List<WebElement> errorList = configDialog.findElements(
                        By.cssSelector("span[class='ui-message-error-detail']"));
                if (errorList.size() == 2 
                        && errorList.get(0).getText().equals("Value Invalid Type!")
                        && errorList.get(1).getText().equals("Value Invalid Type!"))
                {
                    return true;
                }
                return false;
            }
        });
        assertThat(errorsFound, is(true));
        
        //close create config dialog
        WebElement cancelButton = configDialog.findElement(By.
                cssSelector("button[id='createFactoryForm:cancelConfig']"));
        wait.until(ExpectedConditions.visibilityOf(cancelButton));
        cancelButton.click();       
        
        //verify no new configurations were created
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[id='createFactoryConfig']")));
        configTable = m_Driver.findElement(configTableSelector);
        int newCount = ConfigurationHelper.getConfigurationsList(rowNum, configTable).size();
        assertThat(originalConfigCount, is(newCount)); //indicates no change in the number of configurations 
        
        WebElement config = ConfigurationHelper.findConfigurationPanel(
                "example.ccomms.ExamplePhysicalLinkConfig", true);
        WebElement configPropsTable = 
                config.findElement(By.cssSelector("tbody[class='ui-datatable-data ui-widget-content']"));
        WebElement collapse = config.findElement(By.cssSelector("span[class='ui-icon ui-icon-minusthick']"));
        collapse.click();
        
        //Wait for physical link config panel to collapse.
        Thread.sleep(1000);
        assertThat(configPropsTable.isDisplayed(), is(false));
    }
    
    /**
     * Verify a new configuration can be created from an existing factory configuration. 
     */
    @Test
    public void testCreateConfig() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        //open create configuration dialog and get count of existing configurations
        By configTableSelector = By.cssSelector("tbody[id*='factoryConfigTable_data']");
        String rowNum = ConfigurationHelper.createConfigDialogHelper("example.ccomms.ExamplePhysicalLinkConfig", true);
        WebElement configTable = m_Driver.findElement(configTableSelector);
        int originalConfigCount = ConfigurationHelper.getConfigurationsList(rowNum, configTable).size();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id='createFactoryConfig']")));
        WebElement configDialog = m_Driver.findElement(By.cssSelector("div[id='createFactoryConfig']"));
        
        WebElement propsBody = m_Driver.findElement(By.
            cssSelector("form[id='createFactoryForm']>span>div>div>table>tbody"));
        
        // set values
        ConfigurationHelper.updateValue(propsBody, "Read timeout ms", "1999");
        ConfigurationHelper.updateValue(propsBody, "Data bits", "8675309");
        
        //create configuration
        WebElement createButton = configDialog.findElement(By.
                cssSelector("button[id='createFactoryForm:createConfig']"));
        createButton.click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[id='createFactoryConfig']")));
        
        //Sleep needed so controller has time to add configuration and send response.
        Thread.sleep(1000);
        
        //get updated element containing factory configuration        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        rowNum = fwait.until(new ExpectedCondition<String>()
        {
            @Override
            public String apply(WebDriver driver)
            {
                return ConfigurationHelper.createConfigDialogHelper("example.ccomms.ExamplePhysicalLinkConfig", false);
            }
        });
        
        //get list of existing configurations
        configTable = m_Driver.findElement(By.cssSelector("tbody[id*='factoryConfigTable_data']"));
        List<WebElement> configList = ConfigurationHelper.getConfigurationsList(rowNum, configTable);
        int newConfigCount = configList.size();
        
        //ensure that there is 1 new configuration
        assertThat(newConfigCount, is(originalConfigCount+1));
        
        //verify property values are identical to given values
        WebElement newConfig = configList.get(0);
        propsBody = configTable.findElement(By.cssSelector("form[id*='factoryConfigTable:"+ rowNum 
                + "']>div>table>tbody"));
        assertThat(ConfigurationHelper.getInputBoxValue(propsBody, "Read timeout ms"), is("1999"));
        assertThat(ConfigurationHelper.getInputBoxValue(propsBody, "Data bits"), is("8675309"));      
        
        //delete new configuration
        WebElement propsTable = configTable.findElement(By.cssSelector("form[id*='factoryConfigTable:"+ rowNum 
                + "']"));
        WebElement deleteButton = propsTable.findElement(By.cssSelector("button[id$='deleteFactoryConfig']"));
        deleteButton.click();
        
        //pid of new configuration is contained in first token delimited by the newline character
        ConfigurationHelper.handleDeleteConfigDialog(m_Driver, newConfig.getText().split("\n")[0].trim(), true);
        
        //Sleep need so controller has time to delete configuration and send response.
        Thread.sleep(1000);
        
        //Retrieve factory so it can be closed.
        WebElement config = fwait.until(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                return ConfigurationHelper.findConfigurationPanel("example.ccomms.ExamplePhysicalLinkConfig", true);
            }
        });
        
        WebElement configPropsTable = 
                config.findElement(By.cssSelector("tbody[class='ui-datatable-data ui-widget-content']"));
        WebElement collapse = config.findElement(By.cssSelector("span[class='ui-icon ui-icon-minusthick']"));
        collapse.click();
        
        //Wait for physical link config panel to collapse.
        Thread.sleep(1000);
        assertThat(configPropsTable.isDisplayed(), is(false));
    }   
}
