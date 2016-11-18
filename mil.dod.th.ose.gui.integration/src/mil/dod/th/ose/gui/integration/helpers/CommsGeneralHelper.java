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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.util.ResourceLocatorUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Comms helper to test various stack operations with assumptions that an active controller with the example platform 
 * bundle.
 * @author bachmakm
 *
 */
public class CommsGeneralHelper
{ 
    /**
     * Static variable for maintaining the state of whether or not a bundle has been installed.
     * Used in the event a test fails after a bundle has been uninstalled.  Bundles needs to be
     * reinstalled at the end of the test. 
     */
    private static boolean bundleInstalled = true;
    
    /**
     * Assuming default controller is the active controller, assuming the example platform bundle is loaded
     * on the controller. Create a new comms stack type specified by the given params.
     */
    public static void createStack(final WebDriver driver, final String physicalName, final String linkName, 
        final String linkType, final String transName, final String transType) 
        throws InterruptedException
    {            
        //Initial page check
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);
        
        //this needs to be 2 seconds
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);

        fwait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[id*='addCommsButton']")));
        
        //Bring up the add comms dialog
        WebElement addCommsButton = driver.findElement(By.cssSelector("button[id*='addCommsButton']"));
        addCommsButton.click();
        
        //Instantiate web driver wait and fluent wait.
        WebDriverWait wait = new WebDriverWait(driver, 5);

        //Wait till the add comms dialog is visible.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id*='addAccordion']")));

        /**
         * Select a link layer type and pass the given link name to the input field 
         */
        CommsAddDialogHelper.layerSelectHelper(driver, "Link Layer", linkType).click();
        
        //Wait until the dialog reflects that the link type has been selected       
        wait.until(ExpectedConditions.textToBePresentInElementValue(By.
                cssSelector("input[name$='linkData_selection']"), linkType));
        
        //enter the layer name
        WebElement input = driver.findElement(By.cssSelector("input[id$='linkName']"));
        GeneralHelper.retrySendKeys(driver, input, linkName, 5);
        
        /**
         * Select a physical layer 
         */
        CommsAddDialogHelper.layerSelectHelper(driver, "Physical Link", physicalName).click();
        
        //Wait until the dialog reflects that the physical link has been selected     
        wait.until(ExpectedConditions.textToBePresentInElementValue(By.
                cssSelector("input[name$='physicalData_selection']"), physicalName));
        
        /**
         * Select a transport layer and pass the name values, if applicable 
         */
        if (transType != null)
        {
            // Check to see if DirectTransport is visable
            WebElement layer = CommsAddDialogHelper.layerSelectHelper(driver, "Transport Type",
                    transType);
            // If there are more then 3 in the list, select next page, then check again
            if (layer == null)
            {
                CommsAddDialogHelper.selectNextCommsLayerPage(driver);
                layer = CommsAddDialogHelper.layerSelectHelper(driver, "Transport Type",
                        transType);
                if (layer == null)
                {
                    CommsAddDialogHelper.selectBackCommsLayerPage(driver);
                    layer = CommsAddDialogHelper.layerSelectHelper(driver, "Transport Type", transType);
                }

            }

            CommsAddDialogHelper.layerSelectHelper(driver, "Transport Type", transType).click();
            
            //Wait till the dialog reflects that the link type has been selected        
            wait.until(ExpectedConditions.textToBePresentInElementValue(By.
                    cssSelector("input[name$='transportData_selection']"), transType));
            
            //Sleep needed  to allow AJAX update to finish after a layer is selected.
            Thread.sleep(2000);
            
            //enter trans name
            input = driver.findElement(By.cssSelector("input[id$='transName']"));
            GeneralHelper.retrySendKeys(driver, input, transName, 5);
        }
        
        //click confirm stack to create new stack
        WebElement confirmStackButton = driver.findElement(By.cssSelector("button[id$='confirmStackButton']"));
        confirmStackButton.click();
        
        //wait until the confirm stack table is shown
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span[id$='confirmStack']")));
        
        //create list of entered values for created stack
        List<String> givenParams = new ArrayList<String>();
        givenParams.add(physicalName);
        givenParams.add(linkName);
        givenParams.add(linkType);
        if (transType != null)
        {
            //add transport layer params
            givenParams.add(transName);
            givenParams.add(transType);
        }
        
        //all entered values should appear in confirm dialog
        List<WebElement> confirmData = driver.findElements(By.cssSelector("tr[class='ui-widget-content']>td")); 

        assertThat(givenParams.size(), is(confirmData.size() - 2));
        
        //submit the form
        WebElement submitButton = driver.findElement(By.cssSelector("button[id$='confirmSubmitButton']"));
        submitButton.click();
        
        //Wait for the created stack to appear in the stacks list.
        Boolean result = fwait.until(new ExpectedCondition<Boolean>()
        { 
            @Override
            public Boolean apply(WebDriver driver)
            {
                //pass in either the trans name or link depending on which one is topmost
                return CommsGeneralHelper.retrieveStack(driver, transName == null ? linkName : transName) != null;
            }
        });
        assertThat(result, is(true));
        
        //Thread sleep needed. This is due to the fact that the stack may have been created and appear in the list
        //but the list may not be done rendering. This means that any attempt to use the stack directly after this 
        //method may encounter a stale element since rendering is not complete.
        Thread.sleep(7000);
    }
    
    /**
     * Assuming an stack with the given name is already created, activate the stack.
     */
    public static void activateStack(final WebDriver driver, final String stackName) throws InterruptedException, 
        ExecutionException, TimeoutException
    {
        //Initial page/tab check.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);
        
        //Retrieve the stack to be activated.
        WebElement stack = retrieveStack(driver, stackName);
        if (stack == null)
        {
            throw new IllegalArgumentException("No stack found with name: " + stackName);
        }
        
        //--activate the stack
        WebElement activateButton = stack.findElement(By.cssSelector("button[id$='activateButton']"));
        WebElement removeButton = stack.findElement(By.cssSelector("button[id$='removeButton']"));
        
        //assert that activate and remove buttons are displayed
        assertThat(activateButton.isDisplayed(), is(true));
        assertThat(removeButton.isDisplayed(), is(true));
        
        //click the activate button
        GrowlVerifier.verifyAndWaitToDisappear(20, activateButton, "Link Activated");
        
        //this needs to be 2 seconds
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        //Wait till the status of the stack is activated.
        fwait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[id$='deactivationButton']")));
        
        //ensure activate and remove buttons are invisible
        fwait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("button[id$='activateButton']")));
        fwait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("button[id$='removeButton']")));
        
        //refresh DOM instance - since buttons changed, using old stack object will cause StaleElementException
        stack = retrieveStack(driver, stackName);
        
        //find deactivate button
        WebElement deactivateButton = stack.findElement(By.cssSelector("button[id$='deactivationButton']"));

        //ensure deactivate button is displayed
        assertThat(deactivateButton.isDisplayed(), is(true));     
    }
    
    /**
     * Assuming an stack with the given name is already created and activated, remove the stack.
     */
    public static void deactivateStack(final WebDriver driver, final String stackName) throws InterruptedException, 
        ExecutionException, TimeoutException
    {
        //Initial page/tab check.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);
        
        //Retrieve the stack to be deactivated.
        WebElement stack = retrieveStack(driver, stackName);
        if (stack == null)
        {
            throw new IllegalArgumentException("No stack found with name: " + stackName);
        }
        
        //this needs to be 2 seconds
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        //--deactivate the stack
        WebElement deactivateButton = stack.findElement(By.cssSelector("button[id*='deactivationButton']"));
        GrowlVerifier.verifyAndWaitToDisappear(20, deactivateButton, "Link Deactivated");
        
        //Wait till the status of the stack is deactivated.
        fwait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[id$='activateButton']")));
        fwait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[id$='removeButton']")));
        
        //ensure that deactivate button becomes invisible
        fwait.until(ExpectedConditions.invisibilityOfElementLocated(By.
                cssSelector("button[id*='deactivationButton']")));
        
        //refresh DOM instance - since buttons changed, using old stack object will cause StaleElementException
        stack = retrieveStack(driver, stackName);
        
        //find button elements
        WebElement activateButton = stack.findElement(By.cssSelector("button[id*='activateButton']"));
        WebElement removeButton = stack.findElement(By.cssSelector("button[id$='removeButton']"));
        
        //enusre that activate and remove buttons are displayed
        assertThat(activateButton.isDisplayed(), is(true));
        assertThat(removeButton.isDisplayed(), is(true));
    }
    
    /**
     * Assuming a stack with the given name is already created and deactivated, remove the stack.
     */
    public static void removeStack(final WebDriver driver, final String stackName, 
            final String ...growlTitles) throws InterruptedException, ExecutionException, TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(driver, 15);
        
        //Initial page/tab check.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);
        
        //Retrieve the stack to be removed.
        WebElement stack = retrieveStack(driver, stackName);
        
        //---remove the stack---
        List<WebElement> removeButtonList = stack.findElements(By.cssSelector("button[id$='removeButton']"));
        
        if (removeButtonList.size() > 0)
        {
            removeButtonList.get(0).click();
        
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='removeStackDialog']")));
            
            //confirm we want to remove the stack
            WebElement confirmRemove = driver.findElement(By.cssSelector("button[id$='confirmDeleteButton']"));
            if (growlTitles.length > 0)
            {
                GrowlVerifier.verifyAndWaitToDisappear(30, confirmRemove, growlTitles);
            }
            else
            {
                confirmRemove.click();
            }
            
            //this needs to be 2 seconds
            Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                    pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                            NoSuchElementException.class);
            
            Boolean result = fwait.until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    return retrieveStack(driver, stackName) == null;
                }
            });        
            assertThat(result, is(true));
            
            //Thread sleep needed. This is due to the fact that the stack may 
            //have been deleted and disappear from the list
            //but the list may not be updated. This means that any attempt to use the stack directly after this 
            //method may encounter a stale element since rendering is not complete.
            Thread.sleep(2000);
        }
    }
    
    /**
     * Method that finds and returns the WebElement that represents the stack. This method returns null if the
     * specified stack cannot be found.
     * 
     * @param driver
     *      Instance of the {@link WebDriver} the stack will be retrieved from.
     * @param stackName
     *      Name of the stack to be retrieved.
     * @return
     *      {@link WebElement} that represents the specified stack or <code>null</code> if it cannot be found.
     */
    public static WebElement retrieveStack(final WebDriver driver, final String stackName)
    {   
        List<WebElement> commsPanels = driver.findElements(By.cssSelector("table[class='commsInfoPanel']"));
        
        for(WebElement element : commsPanels)
        {
            List<WebElement> spans = element.findElements(By.tagName("span"));
            
            for(WebElement span : spans)
            {
                if (span.getText().equals(stackName))
                {
                    return element;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Method that finds and returns the WebElement that represents the specified comms layer type on the add comms
     * stack dialog.
     * This method returns null if the specified comms layer type cannot be found.
     * 
     * @param driver
     *      Instance of the {@link WebDriver} the comm type will be retrieved from.
     * @param layerTypeName
     *      Name of the layer type to be retrieved. 
     * @return
     *      {@link WebElement} that represents the specified layer type or <code>null</code> if it cannot be found.
     */
    public static WebElement retrieveCommsLayerType(final WebDriver driver, final String layerTypeName)
    {
        //Retrieve the list of all layer types and search for an layer type with the specified name.
        //tbody contains the <tr>s of layer types, the second <td> child of parent <tr> should contain
        //the layer type.
        for (WebElement layerType : driver.findElements(By.
                cssSelector("tbody[class='ui-datatable-data ui-widget-content']>tr>td:nth-child(2)")))
        {
            if (layerType.getText().contains(layerTypeName))
            {
                return layerType;
            }
        }
        return null;
    }
    
    /**
     * The selenium getText() method only retrieves text values if the element is visible.  This method
     * forces the retrieval of text contained within a given element even if it is invisible. 
     * @param driver
     *      Instance of the {@link WebDriver} for the comms page test
     * @param element
     *      WebElement (either visible or invisible) containing text
     * @return
     *      String representation of the text contained within the given WebElement 
     */
    public static String getHiddenText(WebDriver driver, WebElement element)
    {
        JavascriptExecutor executor = (JavascriptExecutor)driver;
        return executor.executeScript("return arguments[0].innerHTML", element).toString();
    }
    
    /**
     * Used to navigate to comms page from system configurations page.  
     */
    public static void commsTestSetupHelper(WebDriver driver) throws InterruptedException
    {        
        //Navigate to comms page
        NavigationHelper.navigateToPage(driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS);

        NavigationHelper.collapseSideBars(driver);
    }
    
    /**
     * Used to ensure that the add comms dialog is closed and that all non-physical link stacks are removed.
     */
    public static void commsTestCleanUpHelper(WebDriver driver) throws InterruptedException, ExecutionException, 
        TimeoutException
    {
        WebElement addCommsDlg = driver.findElement(By.cssSelector("div[id$='addCommsDialog']"));
        
        if (addCommsDlg.isDisplayed())
        {
            //rather than just closing dialog, refresh entire page to ensure closure of all open dialogs
            //and resetting of input fields and validation state.
            driver.navigate().refresh();            
        }
        
        List<WebElement> stacksList = driver.findElements(By.cssSelector("a[id*='dataList']"));
        //stack list should not contain more than 1 "All" stack, 2 dev physical links, 
        //and 1 physical link that was force-added
        if (stacksList.size() > 4)
        {
            for (WebElement element : driver.findElements(By.cssSelector("a[id*='dataList']")))
            {
                String topLayerName = element.getText().trim();
                if (!topLayerName.equals("All") && !topLayerName.contains("dev") 
                        && !topLayerName.matches("^[0-9]+$"))
                {
                    CommsGeneralHelper.removeStack(driver, topLayerName);
                }
            }
        }
        
        if(!bundleInstalled)
        {
            BundleManagerUtil.installBundle(driver, ResourceLocatorUtil.getExampleCustomCommPluginFile(), 
                    NavigationButtonNameConstants.NAVBUT_PROP_COMMS);
            bundleInstalled = true;
            
            //open up add comms dialog and refresh the types again so they are available for the next test
            WebElement addCommsButton = driver.findElement(By.cssSelector("button[id*='addCommsButton']"));
            
            WebDriverWait wait = new WebDriverWait(driver, 5);
            wait.until(ExpectedConditions.visibilityOf(addCommsButton));
            addCommsButton.click();
            
            WebElement refreshTypesButton = driver.findElement(By.cssSelector("button[id*='refreshCommsTypes']"));
            wait.until(ExpectedConditions.visibilityOf(refreshTypesButton));
            refreshTypesButton.click();
            
            wait.until(ExpectedConditions.refreshed(ExpectedConditions.
                    elementToBeClickable(By.cssSelector("button[id*='cancelStackButton']"))));
            
            WebElement closeButton = driver.findElement(By.cssSelector("button[id*='cancelStackButton']"));
            closeButton.click();
        }
    }
    
    /**
     * Helper method which checks if there's an available physical link for use.  If not, it force-adds a 
     * physical link and returns the name of the newly created physical link.  
     */
    public static String getAvailablePhysicalLink(WebDriver driver) throws InterruptedException
    {
        String physName = CommsAddDialogHelper.getTopPhysicalLinkName(driver);
        
        if(physName == null)
        {
            CommsAddDialogHelper.forceAddPhysicalLink(driver, "testPlink");
        }
        
        return CommsAddDialogHelper.getTopPhysicalLinkName(driver);
    }
    
    /**
     * Returns <code>true</code> if a bundle is installed.  <code>false</code> otherwise. 
     */
    public boolean isBundleInstalled()
    {
        return bundleInstalled;
    }
    
    /**
     * Method for setting the value of the bundleInstalled variable.
     */
    public static void setBundleInstalled(boolean isBundleInstalled)
    {
        bundleInstalled = isBundleInstalled;
    }
}
