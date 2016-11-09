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
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper.ExpandCollapse;

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

import com.google.common.base.Joiner;

/**
 * Command helper class to aid in filling out and executing commands.
 * 
 * @author nickmarcucci
 *
 */
public class AssetCommandHelper
{
    public static final String ADD = "<ADD BUTTON>";
    
    private static final Logger LOG = Logger.getLogger("command.helper");
    
    /**
     * Function to find the desired panel, the desired asset, that is to be used when executing 
     * commands.
     * @param driver
     *  the web driver to use
     * @param assetName
     *  the name of the asset for which the desired panel is associated
     * @return
     *  the WebElement which is the panel for the specified asset or null if not found.
     */
    public static WebElement getAssetCommandPanel(final WebDriver driver, final String assetName)
    {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("tbody[id='tabView:assetCommandControl_data']")));
        List<WebElement> assetCommandPanels = driver.findElements(
                By.cssSelector("tbody[id='tabView:assetCommandControl_data']"));
        
        assertThat(assetCommandPanels.size(), greaterThan(0));
        
        for (WebElement commandPanel : assetCommandPanels)
        {
            try
            {
                WebElement neededSpanContainer = commandPanel.findElement(By.cssSelector("tr>td>div>span"));
                WebElement assetId = neededSpanContainer.findElement(By.cssSelector("span[id$='assetName']"));
            
                if (assetId != null && assetId.getText().equals(assetName))
                {
                    return commandPanel;
                }
            }
            catch (NoSuchElementException exception)
            {
                
            }   
        }  
        return null;
    }
    
    /**
     * Function that will click the specified command panel expander.
     * @param driver
     *  the driver to use
     * @param assetName
     *  the name of the asset that the command pertains to.
     * @param command
     *  the name of the command that identifies the panel to be clicked
     * @param choice 
     *  whether to expand or collapse the panel
     */
    public static void clickAccordionPanel(final WebDriver driver, final String assetName, final String command, 
            ExpandCollapse choice) 
        throws InterruptedException
    {
        final WebElement assetCommandPanel = getAssetCommandPanel(driver, assetName);
        
        GeneralHelper.toggleAccordion(assetCommandPanel, command, choice);
    }
    
    /**
     * Function takes a list of string input values and iterates through the list and fills out 
     * the command on the respective panel. Function will expand the command drop down based on the 
     * supplied string.
     * @param driver
     *  the web driver to use
     * @param assetName
     *  the asset on which the command is to be filled in and executed.
     * @param command
     *  the command which is to be expanded and filled in.
     * @param params
     *  the parameters to edit where the key is the parameter name and value is the parameter value to use
     */
    public static void inputAllParameters(final WebDriver driver, final String assetName, final String command, 
            final Map<String, String> params) throws InterruptedException
    {
        WebElement assetCommandPanel = getAssetCommandPanel(driver, assetName);
        
        clickAccordionPanel(driver, assetName, command, ExpandCollapse.EXPAND);
        
        WebElement accordion = assetCommandPanel.findElement(By.cssSelector(
                "div[class*='ui-accordion-content ui-helper-reset ui-widget-content']" +
                "[aria-hidden='false'][id*='tabView:assetCommandControl']"));
        WebElement table = accordion.findElement(By.tagName("tbody"));
        
        List<WebElement> rows = table.findElements(By.tagName("tr"));
        
        assertThat(rows.size(), greaterThan(0));
        
        int numberOfRows = rows.size();
        
        int lastTreeLevel = 0;
        Stack<String> path = new Stack<>(); // current path from the root of the tree to the current node
        for (int i = 0; i < numberOfRows; i++)
        {
            WebElement row = rows.get(i);
            
            List<WebElement> paramNameCell = row.findElements(By.cssSelector("td:nth-child(1)"));
            List<WebElement> valueCell = row.findElements(By.cssSelector("td:nth-child(3)"));
            
            if (valueCell.size() == 0)
            {
                continue;
            }
            
            // id will be something like xxxxxxx_node_0_0_0 where the numbers at the end will be the index within the 
            // tree, use the tree level to figure out where we are on the path of the tree
            String nodeLabel = row.getAttribute("id").split("_node_")[1];
            int treeLevel = nodeLabel.split("_").length;
            if (treeLevel == lastTreeLevel)
            {
                // at same level, pop last node
                path.pop();
            }
            else if (treeLevel < lastTreeLevel)
            {
                // at a lower level, pop last 2 nodes
                path.pop(); // pop last visited node
                path.pop(); // pop the parent too
            }
            lastTreeLevel = treeLevel;
            
            path.push(paramNameCell.get(0).getText());
            String paramName = Joiner.on(".").join(path);
            
            LOG.info("found param name = " + paramName);
            String desiredValue = params.remove(paramName); // take parameter from map so we don't try to use again
            if (desiredValue == null)
            {
                // do not want to set parameter, not in map of things to set
                continue;
            }
            
            //find what it contains. used the findElements method to avoid the NoSuchElementException
            //so lists should always be a size of one or empty.
            List<WebElement> buttons = valueCell.get(0).findElements(By.cssSelector("button"));
            List<WebElement> inputs = valueCell.get(0).findElements(By.cssSelector("input"));
            List<WebElement> dropDowns = valueCell.get(0).findElements(By.cssSelector("div[id$='enumDropDown']"));
            
            if (buttons.size() > 0)
            {
                // assume add button
                WebElement button = buttons.get(0);
                
                if (button.getText().equals("Add") && desiredValue.equals(ADD))
                {
                    button.click();
                    
                    GeneralHelper.sleepWithText(5, "Waiting for accordion to expand and update the DOM");
                    
                    WebDriverWait wait = new WebDriverWait(driver, 20);
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                            "div[class*='ui-accordion-content ui-helper-reset ui-widget-content']" +
                            "[aria-hidden='false'][id*='tabView:assetCommandControl']")));
                    
                    table = assetCommandPanel.findElement(By.cssSelector(
                            "div[class*='ui-accordion-content ui-helper-reset ui-widget-content']" +
                            "[aria-hidden='false'][id*='tabView:assetCommandControl']"));
                    
                    rows = table.findElements(By.tagName("tr"));
                    numberOfRows = rows.size();
                }
            }
            else if (inputs.size() > 0)
            {
                // no button, but we have input, assume text entry
                WebElement textInput = inputs.get(0);
                
                if (!textInput.getText().equals(desiredValue))
                {
                    GeneralHelper.retrySendKeys(driver, textInput, desiredValue, 5);
                }
            }
            else if (dropDowns.size() > 0)
            {
                // no button, no input, we have drop down
                WebElement dropDown = dropDowns.get(0);
                
                WebElement dropDownButton = dropDown.findElement(
                        By.cssSelector("label[class='ui-selectonemenu-label ui-inputfield ui-corner-all']"));
                dropDownButton.click();
                
                WebDriverWait wait = new WebDriverWait(driver, 20);
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div[id*='enumDropDown_panel'][style*='block;']")));
                WebElement dropDownList = driver.findElement(
                        By.cssSelector("div[id*='enumDropDown_panel'][style*='block;']"));
                
                List<WebElement> dropDownValues = dropDownList.findElements(By.tagName("li"));
                
                for (WebElement dropDownValue : dropDownValues)
                {
                    if (dropDownValue.getText().equals(desiredValue))
                    {
                        dropDownValue.click();
                        break;
                    }
                }
            }
        } 
        
        assertThat("Not all desired parameters where changed: " + params, params.size(), is(0));
    }
    
    /**
     * Function finds the correct button on the supplied asset command panel.
     * @param driver
     *  the web driver to use
     * @param assetName
     *  the name of the asset that the command is to be executed on.
     * @param buttonText
     *  the text of the button that needs to be found
     * @return
     *  null if the button cannot be found else the found button
     */
    public static WebElement findCorrectSendButton(final WebDriver driver, final String assetName, 
            final String buttonText, final String buttonId)
    {
        WebElement assetCommandPanel = getAssetCommandPanel(driver, assetName);
        
        List<WebElement> buttons = assetCommandPanel.findElements(By.cssSelector(
                "button[role='button'][id$='" + buttonId + "'][aria-disabled='false']"));
        
        for (WebElement button : buttons)
        {
            if (button.getText().equals(buttonText))
            {
                return button;
            }
        }
        
        return null;
    }
    
    /**
     * Function to execute a sync command for a specified command on a specified asset.
     * @param driver
     *  the web driver to use
     * @param assetName
     *  the name of the asset that the command is to be synced on
     * @param command
     *  the name of the command that is going to be synced
     * @param buttonText
     *  the text of the sync button that is to be selected. 
     */
    public static void executeSyncCommand(final WebDriver driver, final String assetName,
            final String command, final String buttonText) throws InterruptedException, 
            ExecutionException, TimeoutException
    {
        clickAccordionPanel(driver, assetName, command, ExpandCollapse.EXPAND);
        
        WebElement button = findCorrectSendButton(driver, assetName, buttonText, "syncCommand");
        assertThat(button, notNullValue());
        button.click();
        
        //verify that the growl message appeared there maybe other messages on the page, 
        //just search for the command one
        GrowlChecker.findGrowlMessage(driver, "Asset Command Synchronized:");
        
        GeneralHelper.sleepWithText(10, "Waiting for synced values to update the DOM");
    }
    
    /**
     * Function to execute a sync all command on a specified asset.
     * 
     * @param driver
     *  the web driver to use
     * @param assetName
     *  the name of the asset on which the sync all command is to be performed
     */
    public static void executeSyncAll(final WebDriver driver, final String assetName) 
        throws InterruptedException, ExecutionException, TimeoutException
    {
        WebElement button = findCorrectSendButton(driver, assetName, "Sync All", "syncAll");
        assertThat(button, notNullValue());
        button.click();
        GrowlChecker.findGrowlMessage(driver, "Asset Command Synchronized:");
        
        GeneralHelper.sleepWithText(10, "Waiting for synced values to update the DOM");
    }
    
    /**
     * Executes a get command for a specified asset and verifies the received data.
     * @param driver
     *  the web driver to use
     * @param assetName
     *  the name of the asset on which the get command is to be performed
     * @param command
     *  the name of the command that is to be performed. In this case, command corresponds to the tab shown 
     *  within the "Get Commands" accordion panel.
     *  @param textToVerify
     *  the text output that is to be verified as having shown up when get command is executed. (Text comparison 
     *  performed will be a contains check)
     */
    public static void executeGetCommand(final WebDriver driver, final String assetName, 
            final String command, final String textToVerify) throws InterruptedException
    {
        clickAccordionPanel(driver, assetName, "Get Commands", ExpandCollapse.EXPAND);
        
        final WebElement assetCommandPanel = getAssetCommandPanel(driver, assetName);
        WebElement commandTab = assetCommandPanel.findElement(By.linkText(command));
        assertThat(commandTab, notNullValue());
        
        commandTab.click();
        
        //wait to give time for tab to change. 
        Thread.sleep(2000);
        
        WebElement tabDiv = assetCommandPanel.findElement(By.cssSelector(
                "div[id*='getCommandTab'][role='tabpanel'][aria-hidden='false']"));
        
        WebElement executeButton = tabDiv.findElement(By.cssSelector("button[id$='executeButton']"));
        assertThat(executeButton, notNullValue());
        executeButton.click();
        
        GrowlChecker.findGrowlMessage(driver, "Asset Command Executed:");
        
        Wait<WebDriver> fWait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        fWait.until(new ExpectedCondition<Boolean>(){
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement div = assetCommandPanel.findElement(By.cssSelector(
                        "div[id*='getCommandTab'][role='tabpanel'][aria-hidden='false']"));
                
                WebElement textArea = div.findElement(By.cssSelector(
                        "textarea[id$='responseData'][aria-disabled='false']"));
                
                return textArea.getText().contains(textToVerify);
            }
        });
        
        tabDiv = assetCommandPanel.findElement(By.cssSelector(
                "div[id*='getCommandTab'][role='tabpanel'][aria-hidden='false']"));
        
        WebElement time = tabDiv.findElement(By.cssSelector("span[id$='timeData']"));
        assertThat(time, notNullValue());
        
        assertThat(time.getText(), not(equalTo("Time Received: N/A")));
    }
}
