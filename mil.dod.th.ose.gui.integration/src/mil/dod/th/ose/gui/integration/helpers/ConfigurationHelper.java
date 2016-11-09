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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
 * Helper class for dealing with the configuration tab on the system configuration page.
 * 
 * @author cweisenborn
 */
public class ConfigurationHelper
{
    /**
     * ID for the configuration tab on the system configuration page.
     */
    public static final String CONFIGURATION_TAB_ID = "configTab";
    
    /**
     * Method used to find a configuration or factory in either the configuration or factory configuration table on the
     * configuration tab of the system configuration page.
     * 
     * @param configPid
     *          PID of the configuration or factory to be found.
     * @param isFactory
     *          Boolean used to determine if a factory configuration is being searched for.
     * @return
     *          The web element that represents the configuration or factory.
     */
    public static WebElement findConfigurationPanel(final String configPid, final boolean isFactory)
    {
        WebDriver driver = WebDriverFactory.retrieveWebDriver();
        
        if (isFactory)
        {
            WebElement mainFactoryConfigTable = 
                    driver.findElement(By.cssSelector("tbody[id*='factoryConfigTable_data']"));
            for (WebElement factory : mainFactoryConfigTable.findElements(By.cssSelector("div[id$='factoryPanel']")))
            {
                WebElement factoryHeader = factory.findElement(By.cssSelector("span[id$='factoryHeader']"));
                if (factoryHeader.getText().equals(configPid))
                {
                    return factory;
                }
            }
        }
        else
        {
            WebElement mainConfigTable = driver.findElement(By.cssSelector("tbody[id*='configTable_data']"));
            for (WebElement config : mainConfigTable.findElements(By.cssSelector("div[id$='configPanel']")))
            {
                WebElement configHeader = config.findElement(By.cssSelector("span[id$='configHeader']"));
                if (configHeader.getText().equals(configPid))
                {
                    return config;
                }
            }
        }
        
        throw new IllegalStateException("Unable to find panel with pid: " + configPid);
    }
    
    /**
     * This method will navigate to the configuration tab when on the system configuration page.
     * 
     * @param driver
     *          Current web driver.
     */
    public static void handleNavigateToConfigTab(final WebDriver driver)
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);
        
        //Wait for the tabs on the system configuration page to be available.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[id*='systemConfigTab']")));
        WebElement tabPanel = driver.findElement(By.cssSelector("div[id*='systemConfigTab']"));
        
        //Navigate to the configurations tab on the system configuration page.
        WebElement configTab = tabPanel.findElements(By.tagName("a")).get(1);
        assertThat(configTab.getText(), is("Configurations"));
        configTab.click();
        
        //Wait until the configuration table is available.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tbody[id*='configTable_data']")));
    }
    
    /**
     * Method that handles the delete configuration dialog the pops up when deleting a configuration on the 
     * configuration tab.
     * 
     * @param deletePid
     *          PID of the configuration being deleted. Used to verify that the appropriate configuration is being
     *          deleted.
     * @param removeConfig
     *          Used to determine whether or not confirm or cancel the deletion.
     */
    public static void handleDeleteConfigDialog(final WebDriver driver, final String deletePid, 
            final boolean removeConfig)
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);
        
        //Find the delete dialog.
        WebElement deleteDialog = driver.findElement(By.cssSelector("div[id*='removeDialog']"));
        wait.until(ExpectedConditions.visibilityOf(deleteDialog));
        assertThat(deleteDialog.isDisplayed(), is(true));
        
        //Verify that the event admin config is being deleted.
        WebElement configPid = deleteDialog.findElement(By.cssSelector("span[class='confirmDialogTextImportant']"));
        assertThat(configPid.getText(), is(deletePid));
        
        //Confirm or cancel deleting the configuration.
        if (removeConfig)
        {
            WebElement confirmDelete = deleteDialog.findElement(By.cssSelector("button[id*='removeButton']"));
            confirmDelete.click();
        }
        else
        {
            WebElement cancelDelete = deleteDialog.findElement(By.cssSelector("button[id*='cancelButton']"));
            cancelDelete.click();
        }
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[id*='removeDialog']")));
        assertThat(deleteDialog.isDisplayed(), is(false));
    }
    
    /**
     * Get the current value of the configuration property that is a select one list box.
     */
    public static String getInputBoxValue(String configName)
    {
        return getInputBoxValue(null, configName);
    }
    
    /**
     * Get the current value of the configuration property that is a select one list box.
     */
    public static String getInputBoxValue(WebElement table, String configName)
    {
        WebElement input = findInputBox(findConfigPropRow(table, configName));
        return input.getAttribute("value");
    }
    
    /**
     * Get the current value of the configuration property that is a boolean button.
     */
    public static boolean getBooleanButtonValue(String configName)
    {
        return getBooleanButtonValue(null, configName);
    }
    
    /**
     * Get the current value of the configuration property that is a boolean button.
     */
    public static boolean getBooleanButtonValue(WebElement table, String configName)
    {
        WebElement buttonDiv = findBooleanButton(findConfigPropRow(table, configName));
        
        String boolStr = buttonDiv.findElement(By.cssSelector("span")).getText();
        return (boolStr.equals("Yes")) ? true : false;
    }
    
    /**
     * Get the current value of the configuration property that is a select one list box.
     */
    public static String getSelectOneListBoxValue(String configName)
    {
        return getSelectOneListBoxValue(null, configName);
    }
    
    /**
     * Get the current value of the configuration property that is a select one list box.
     */
    public static String getSelectOneListBoxValue(WebElement table, String configName)
    {
        WebElement buttonDiv = findSelectOneListBox(findConfigPropRow(table, configName));
        
        return buttonDiv.findElement(By.cssSelector("li[class~='ui-state-active']")).getText();
    }
    
    /**
     * Update the configuration property value for the one table on the page.
     */
    public static void updateValue(String configName, Object value)
    {
        updateValue(null, configName, value);
    }

    /**
     * Update the configuration property value for the given table.
     */
    public static void updateValue(WebElement table, String configName, Object value)
    {
        WebElement inputRow = findConfigPropRow(table, configName);
        
        if (!inputRow.findElements(By.cssSelector("div[id$='compositeConfigBoolean']")).isEmpty())
        {
            boolean boolVal = (boolean)value;
            updateBooleanButton(inputRow, boolVal);
        }
        else
        {
            String strVal = (String)value;
            
            if (!inputRow.findElements(By.cssSelector("div[id$='compositeConfigSelectOne']")).isEmpty())
            {
                updateSelectOneListBox(inputRow, strVal);
            }
            else
            {
                updateInputBox(inputRow, strVal);
            }
        }
    }
    
    /**
     * Iterates through list of configuration property names, types, and descriptions to ensure
     * that the values that appear in the create configuration dialog match the given expected values.
     * @param driver
     *      web driver that is currently running the test.
     * @param configDialogSelector
     *      selector corresponding to the create configuration dialog.
     * @param expectedName
     *      expected value from the Name column in the create configuration dialog.
     * @param expectedDescription
     *      expected description from the given configuration dialog.  
     */
    public static void verifyDialogValues(final WebDriver driver, final By configDialogSelector, 
            final String expectedName, final String expectedDescription)
    {
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(10, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement configDialog = driver.findElement(configDialogSelector);
                //list of all names of the properties in top-down order
                List<WebElement> propertyNames = configDialog.findElements(
                        By.cssSelector("span[id*='compositeConfigName']"));
                
                //list of all descriptions of the properties in top-down order
                List<WebElement> propertyDescriptions = configDialog.findElements(
                        By.cssSelector("span[id*='compositeConfigDescription']"));
                
                /*
                 * Index corresponds to a row value in each of the property lists.  In other words, the
                 * name found at index 0 in the propertyNames array corresponds to the type found at index 0 in the
                 * propertyTypes array and the description found at index 0 in the propertyDescriptions
                 * array. Loop finds the expected name and ensures that the corresponding type and description
                 * match the expected values. 
                 */
                boolean nameFound = false;
                for (int i = 0; i < propertyNames.size(); i++)
                {            
                    if (propertyNames.get(i).getText().trim().equals(expectedName))
                    {
                        assertThat(propertyDescriptions.get(i).getText().trim(), is(expectedDescription));
                        nameFound = true;
                        break;
                    }
                }
                return nameFound;
            }
        });
    }
    
    /**
     * Method which expands a specified factory configuration and will optionally open its associated
     * create configuration dialog.  
     * @param configurationName
     *      name of the factory configuration desired
     * @param openDialog
     *      Should be <code>true</code> if the create configuration dialog should be opened. <code>false</code>
     *      if the create configuration dialog should remain closed.  
     * @return
     *      String representation of the row index pointing to the factory configuration. Used to find elements
     *      external to this method that belong to a particular factory configuration.  
     */
    public static String createConfigDialogHelper(String configurationName, boolean openDialog)
    {
        WebElement desiredConfig = ConfigurationHelper.findConfigurationPanel(configurationName, true);
        
        try
        {
            //try to expand the configuration so that the properties are available
            WebElement expandConfig = desiredConfig.findElement(By.
                    cssSelector("span[class='ui-icon ui-icon-plusthick']"));
            expandConfig.click();
        }
        catch (NoSuchElementException e)
        {
            //do nothing; configuration is already expanded
        }
            
        //get the row index of the configuration being tested.
        String rowNum = GeneralHelper.getElementIndex(desiredConfig.getAttribute("id"));
        if (openDialog)
        {
            //open create configuration dialog
            WebElement createConfigButton = desiredConfig.findElement(By.cssSelector("button[id$='" + rowNum 
                    + ":createConfigButton']"));

            WebDriverWait wait = new WebDriverWait(WebDriverFactory.retrieveWebDriver(), 5);
            wait.until(ExpectedConditions.visibilityOf(createConfigButton));
            createConfigButton.click();
        }        
        return rowNum;
    }
    
    /**
     * Returns a list of created configurations based on a given row from the list of factory configurations.
     * @param rowIndex
     *      index of the desired factory configuration 
     * @param configTable
     *      WebElement containing the list of all factory configurations
     * @return
     *      list of configurations created from the factory configuration at the specified row index
     */
    public static List<WebElement> getConfigurationsList(String rowIndex, WebElement configTable)
    {
        WebElement rowParent = configTable.findElement(By.cssSelector("tbody[id*='factoryConfigTable:" 
                + rowIndex + "']"));
        List<WebElement> rowList = rowParent.findElements(By.cssSelector("tr[class*='ui-datatable']"));
        
        List<WebElement> configList = new ArrayList<WebElement>();
        for(WebElement row: rowList) //iterate through all rows belonging to configuration parent
        {
            try
            {
                row.findElement(By.cssSelector("span[class='textHeader']")); //find rows with a config header
                configList.add(row);
            }
            catch (NoSuchElementException e)
            {
                //do nothing - row did not correspond to configuration properties table and should not be returned
            }
        }
        return configList;
    }
    
    /**
     * Method used to create a new factory configuration with the specified property values.
     * 
     * @param driver
     *          Instance of the {@link WebDriver} to be used.
     * @param factoryPid
     *          PID of the factory to create a configuration for.
     * @param valuesMap
     *          Map containing the values to be set for the factory configuration. The key is the name of the property
     *          and the value is the string representation of the value to be set for the property.
     */
    public static void createFactoryConfig(final WebDriver driver, final String factoryPid, 
            final Map<String, Object> valuesMap) throws InterruptedException
    {
        final WebDriverWait wait = new  WebDriverWait(driver, 10);
        
        //Verify current page and tab before continuing.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG, 
                CONFIGURATION_TAB_ID);
        
        WebElement configurationPanel = findConfigurationPanel(factoryPid, true);
        if (!configurationPanel.findElement(By.cssSelector("button[id$='createConfigButton']")).isDisplayed())
        {
            configurationPanel.findElement(By.cssSelector("span[class='ui-icon ui-icon-plusthick']")).click();
        }
        
        //Open the create config dialog.
        configurationPanel.findElement(By.cssSelector("button[id$='createConfigButton']")).click();
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        WebElement dialogPropTable = fwait.until(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                WebElement propTable = null;
                for (WebElement table: driver.findElements(By.cssSelector("tbody[id$='compositeConfigTable_data']")))
                {
                    if (table.isDisplayed())
                    {
                        propTable = table;
                    }
                }
                wait.until(ExpectedConditions.visibilityOf(propTable));
                return propTable;
            }
        });
       
        for (String key : valuesMap.keySet())
        {
            updateValue(dialogPropTable, key, valuesMap.get(key));
        }
        
        //Create the configuration.
        driver.findElement(By.cssSelector("button[id='createFactoryForm:createConfig']")).click();
        
        GeneralHelper.sleepWithText(5, "Wait for AJAX to update after submitting new configuration");
    }
    
    /**
     * Update the configuration property that has an input box entry. 
     */
    private static void updateInputBox(WebElement inputRow, String value)
    {
        WebElement input = findInputBox(inputRow);
        GeneralHelper.retrySendKeys(WebDriverFactory.retrieveWebDriver(), input, value, 10);
    }
    
    /**
     * Update the configuration property that has a boolean button.
     */
    private static void updateBooleanButton(WebElement inputRow, boolean value)
    {
        WebElement buttonDiv = findBooleanButton(inputRow);
        
        // only click button to change to opposite value if not current value
        String boolStr = value ? "Yes" : "No";
        if (!buttonDiv.findElement(By.cssSelector("span")).getText().equals(boolStr))
        {
            buttonDiv.click();
        }
    }
    
    /**
     * Update the configuration property that has a select one list box.
     */
    private static void updateSelectOneListBox(WebElement inputRow, String value)
    {
        WebElement buttonDiv = findSelectOneListBox(inputRow);
        
        // only click button to change to opposite value if not current value
        List<WebElement> listItems = buttonDiv.findElements(By.cssSelector("li"));
        for (WebElement listItem : listItems)
        {
            if (listItem.getText().equals(value))
            {
                listItem.click();
                return;
            }
        }
        
        throw new IllegalArgumentException(
                String.format("Unable to set value to [%s]", value));
    }
    
    /**
     * Get the element representing the input box for the given config property.
     */
    private static WebElement findInputBox(WebElement inputRow)
    {
        
        return inputRow.findElement(By.cssSelector("input[id$='compositeConfigInput']"));
    }
    
    /**
     * Get the boolean configuration button given the configuration name.
     */
    private static WebElement findBooleanButton(WebElement inputRow)
    {
        return inputRow.findElement(By.cssSelector("div[id$='compositeConfigBoolean']"));
    }
    
    /**
     * Update the configuration property that has a select one list.
     */
    private static WebElement findSelectOneListBox(WebElement inputRow)
    {
        return inputRow.findElement(By.cssSelector("div[id$='compositeConfigSelectOne']"));
    }
    
    /**
     * Find the property row for the given table or pass null if only one table.
     */
    private static WebElement findConfigPropRow(WebElement table, String configName)
    {
        WebElement configInputTable = table;
        
        if (table == null)
        {
            configInputTable = WebDriverFactory.retrieveWebDriver()
                .findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        }
        
        List<WebElement> configInputRows = configInputTable.findElements(By.tagName("tr"));
        for (WebElement inputRow : configInputRows)
        {
            WebElement inputBox = inputRow.findElement(By.cssSelector("span[id$='compositeConfigName']"));
            if (inputBox.getText().equals(configName))
            {
                return inputRow;
            }
        }
        
        throw new IllegalArgumentException("The property could not be found with the given name [" + configName +"]");
    }
}
