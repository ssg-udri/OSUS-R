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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;

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
 * Function to grab the controller information that is set in the ant build file.
 * 
 * @author nickmarcucci
 *
 */
public class ControllerHelper
{
    /**
     * System property that holds the name of the controller that has been started for testing.
     */
    public static final String CONTROLLER_PROP_NAME = "mil.dod.th.ose.gui.integration.controller.name";
    
    /**
     * System property that holds the id of the controller that has been started for testing.
     */
    public static final String CONTROLLER_PROP_ID = "mil.dod.th.ose.gui.integration.controller.id";
    
    /**
     * Default string to use for the controller name property.
     */
    public static final String DEFAULT_CONTROLLER_NAME = "THarvest";
    
    /**
     * Default string to use for the controller id property.
     */
    public static final String DEFAULT_CONTROLLER_ID = "0x0000007D";
    
    /**
     * Default string to use for the gui id.
     */
    public static final String DEFAULT_GUI_ID = "0x00000001";
    
    /**
     * Default string to use for the gui name.
     */
    public static final String DEFAULT_GUI_NAME = "gui-generic";
    
    /**
     * Function to get the running controller's name.  Defaults to {@value #DEFAULT_CONTROLLER_NAME}.
     * @return
     *  the running controller's name as it is specified in the ant build file
     */
    public static String getControllerName()
    {
        String name = System.getProperty(CONTROLLER_PROP_NAME);

        if(name == null)
        {
            name = DEFAULT_CONTROLLER_NAME;
        }
        
        return name;
    }
    
    /**
     * Function to get the running controller's id in a hex string format.  Defaults to {@value #DEFAULT_CONTROLLER_ID}.
     * @return
     *  the running controller's id in a hex string format
     */
    public static String getControllerId()
    {
        String id = System.getProperty(CONTROLLER_PROP_ID);
        
        if (id == null)
        {
            id = DEFAULT_CONTROLLER_ID;
        }
        
        return id;
    }
    
    /**
     * Function to get the running controller's id in integer format. 
     * @return
     *  the running controller's id in integer format
     */
    public static int getControllerIdAsInteger()
    {
        String id = getControllerId();
        
        return (int)Long.parseLong(id.substring(2), 16);
    }

    /**
     * Setup a channel and set controller as active. This uses the default controller information.
     * @param driver
     *     the web driver to use
     */
    public static void createAndSetActiveController(final WebDriver driver) throws InterruptedException, 
        ExecutionException, TimeoutException
    {
        //create channel
        ChannelsHelper.createSocketChannel(driver);
        
        NavigationHelper.expandRightSideBarOnly(driver);
        
        // wait for the controller to respond
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException exception)
        {
            exception.printStackTrace();
        }
        
        //set active controller
        setActiveControllerByName(driver, DEFAULT_CONTROLLER_NAME);
    }
    
    /**
     * Setup a channel for creation of a controller. This uses the default controller information.
     * @param driver
     *     the web driver to use 
     */
    public static void createController(final WebDriver driver) throws InterruptedException, ExecutionException, 
        TimeoutException
    {
        //create channel
        ChannelsHelper.createSocketChannel(driver);
        
        NavigationHelper.expandRightSideBarOnly(driver);
        
        // wait for the controller to respond
        try
        {
            Thread.sleep(3000);
        }
        catch (InterruptedException exception)
        {
            exception.printStackTrace();
        }
    }
    
    /**
     * Sets the controller with specified name as active. This method assume that the controller side bar is expanded.
     * 
     * @param driver
     * the web driver to use.
     * @param controllerName
     * name of the controller to set active.
     * @throws InterruptedException
     *      Thrown if the sleep used to wait for the right side bar to expand is interrupted.
     */
    public static void setActiveControllerByName(final WebDriver driver, final String controllerName) 
        throws InterruptedException
    {
        //Make right side bar is expanded.
        NavigationHelper.expandRightSideBarOnly(driver);
        
        //Check to make sure the controller that is being made active is not already the active controller.
        String activeControllerName = getNameOfActiveContoller(driver);
        if (activeControllerName != null && activeControllerName.equals(controllerName))
        {
            return;
        }

        WebElement tharvestController = getControllerListElement(driver, controllerName);
        assertThat("Controller list element was found", tharvestController, is(notNullValue()));
        WebElement activateButton = tharvestController.findElement(By.cssSelector("button[id*='inactive']"));

        activateButton.click();
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        //Wait till controller becomes active before continuing.
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                final String activeControllerName = getNameOfActiveContoller(driver);
                if (activeControllerName != null)
                {
                    return activeControllerName.equals(controllerName);
                }
                else
                {
                    return false;
                }
            }
        });
    }
    
    /**
     * Set the name and id of the gui then connect to it for testing another controller.
     * @param driver
     * the web driver to use
     */
    public static void setGuiNameAndIdThenCreateChannel(final WebDriver driver) 
        throws NumberFormatException, InterruptedException, ExecutionException, TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);
        
        //navigate to the gui config page
        NavigationHelper.navigateToPage(driver, NavigationButtonNameConstants.NAVBUT_PROP_GUI_CONFIG);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[contains(@id, 'systemname')]")));
        
        //collapse sidebars
        NavigationHelper.collapseSideBars(driver);
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.xpath("//button[contains(@id, 'save')]"))));
        
        WebElement guiName = driver.findElement(By.xpath("//input[contains(@id, 'systemname')]"));
        WebElement guiId = driver.findElement(By.xpath("//input[contains(@id, 'sysId')]"));
        WebElement save = driver.findElement(By.xpath("//button[contains(@id, 'save')]"));
        
        assertThat(guiName, is(notNullValue()));
        assertThat(guiId, is(notNullValue()));
        assertThat(save, is(notNullValue()));
        
        //set GUI name
        guiName.clear();
        guiName.sendKeys(DEFAULT_GUI_NAME);
        
        //set GUI id
        guiId.clear();
        guiId.sendKeys(DEFAULT_GUI_ID.replace("0x", "")); // 0x prefix as text entry will always have that part
        
        //save changes
        save.click();
        
        // TD: only question is why???
        GeneralHelper.sleepWithText(4, "Waiting after clicking save");
        
        //expand sidebars
        NavigationHelper.expandSideBars(driver);
        NavigationHelper.openBaseUrl();
        
        //create gui controller
        ChannelsHelper.createSocketChannel(driver, ChannelsHelper.HOST_NAME, "4001");
        
        NavigationHelper.openBaseUrl();
    }
    
    /**
     * Method used to find the name of the currently active controller.
     * 
     * @param driver
     *          Current web driver.
     * @return
     *          The name of the currently active controller. Returns null if there is no active controller.
     */
    public static String getNameOfActiveContoller(final WebDriver driver)
    {
        List<WebElement> controllerList = driver.findElements(By.cssSelector("table[class='controller']"));
        for (WebElement controller: controllerList)
        {
            try
            {
                WebElement active = controller.findElement(By.cssSelector("img[class='activeButton']"));
                assertThat(active, is(notNullValue()));
                WebElement controllerName = controller.findElement(By.cssSelector("span[id*='controllerName']"));
                return controllerName.getText();
            }
            catch (Exception e)
            {
                
            }
        }
        
        return null;
    }
    
    /**
     * Get a Controller {@link WebElement} by name. Will return null if the Controller cannot be found.
     * @param driver
     *          Current web driver.
     * @param name
     *          String that represents the name of the controller element to be retrieved.
     * @throws InterruptedException
     *      Thrown if the sleep used to wait for the right side bar to expand is interrupted.
     */
    public static WebElement getControllerListElement(final WebDriver driver, final String name) 
        throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 10);
       
        //make sure the sidebar is open
        NavigationHelper.expandRightSideBarOnly(driver);
        //wait for controller list to be present
        wait.until(ExpectedConditions.
                presenceOfElementLocated(By.cssSelector("div[id*='controllerList_content']")));
        
        //get list of controllers
        List<WebElement> controllerList = driver.findElements(By.cssSelector("table[class='controller']"));
        WebElement foundController = null;
        for (WebElement controller : controllerList)
        {
            WebElement nameFromList;
            try
            {
                //May throw no such element found exception if a connection to a second controller was attempted but
                //not successful.
                nameFromList = controller.findElement(By.cssSelector("span[id*='controllerName']"));
            }
            catch (final NoSuchElementException exception)
            {
                continue;
            }
            
            //check if the controller element matches the desired element
            if (nameFromList.getText().equals(name))
            {
                foundController = controller;
            }
        }
        return foundController;
    }
    
    /**
     * Find the controller menu button in the controller sidebar and collapse the left sidebar once found.  Return
     * the controller menu button element. 
     * @param driver
     *      current web driver.
     * @return
     *      Controller menu button web element.
     */
    public static WebElement findControllerMenu(final WebDriver driver) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[id$='controllerMenu_button']")));
        NavigationHelper.collapseLeftBarOnly(driver);
        
        WebElement controllerMenuButton = driver.findElement(By.
                cssSelector("button[id$='controllerMenu_button']"));        
        return controllerMenuButton;
    }
    
    /**
     * Method that will display the controller information dialog for the specified controller.
     * 
     * @param driver
     *      WebDriver currently being used.
     * @param controllerName
     *      Name of the controller to display the information dialog for.
     * @throws InterruptedException
     *      Thrown if the sleep used to wait for the right side bar to expand is interrupted.
     */
    public static void displayControllerInfo(final WebDriver driver, final String controllerName) 
        throws InterruptedException
    {
        WebElement controller = getControllerListElement(driver, controllerName);
        WebElement controllerMenuButton = controller.findElement(By.cssSelector("button[id$='controllerMenu_button']"));
        controllerMenuButton.click();
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(10, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        WebElement controllerInfo = fwait.until(new ExpectedCondition<WebElement>()
        {
            @Override
            public WebElement apply(WebDriver driver)
            {
                List<WebElement> infoButtonList = driver.findElements(By.cssSelector("a[id$='controllerInfo']"));
                WebElement controllerInfo = null;
                for (WebElement infoButton: infoButtonList)
                {
                    if (infoButton.isDisplayed())
                    {
                        controllerInfo = infoButton;
                    }
                }
                
                if (controllerInfo != null)
                {
                    return controllerInfo;
                }
                throw new NoSuchElementException("Could not find a visible element with id 'controllerInfo'!");
            }
        });
        controllerInfo.click();
        fwait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='controllerInfoTab']")));
    }
    
    /**
     * Set a Controller's Operational Mode. Not if the mode is 
     * {@link mil.dod.th.core.controller.TerraHarvestController.OperationMode#TEST_MODE} it can only be
     * changed to {@link mil.dod.th.core.controller.TerraHarvestController.OperationMode#OPERATIONAL_MODE}. 
     * @param driver
     *          Current web driver.
     * @param name
     *          the name of the Controller to change the operational mode of
     * @param verifyGrowl
     *          if any the mode changed message should be searched for
     * @return
     *     if the mode actually changed, false means that the mode was already set to the desired value
     */
    public static boolean changeControllerOpMode(final WebDriver driver, final String name, 
        final OperationMode desiredMode, final boolean verifyGrowl) throws InterruptedException, ExecutionException, 
            TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        
        final WebElement controller = getControllerListElement(driver, name);
        
        //open controller menu
        final WebElement controllerMenu = controller.
                findElement(By.cssSelector("button[id$='controllerMenu_button']"));
        controllerMenu.click();
        
        //get the row index associated with the desired controller from the list
        final String controllerIndex = GeneralHelper.getElementIndex(controllerMenu.getAttribute("id"));
        
        //Bring up the controller mode selection dialog for the desired controller
        final WebElement controllerMode = driver.findElement(By.cssSelector("a[id$='"+ controllerIndex
                +":controllerMode']"));
        wait.until(ExpectedConditions.visibilityOf(controllerMode));   
        controllerMode.click();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("span[id$='controllerModePanel']")));

        boolean modeChanged = false;
        //check the mode. 
        boolean opMode = driver.findElement(By.cssSelector("button[id$='opModeButton']")).isEnabled();
        if (opMode && desiredMode == OperationMode.OPERATIONAL_MODE)
        {
            if (verifyGrowl)
            {
                GrowlVerifier.verifyAndWaitToDisappear(30, 
                    driver.findElement(By.cssSelector("button[id$='opModeButton']")), "Controller Status Updated");
            }
            else
            {
                WebElement opModeElement = driver.findElement(By.cssSelector("button[id$='opModeButton']"));
                opModeElement.click();
            }
            modeChanged = true;
        }
        else if (!opMode && desiredMode == OperationMode.TEST_MODE)
        {
            if (verifyGrowl)
            {
                GrowlVerifier.verifyAndWaitToDisappear(30,
                    driver.findElement(By.cssSelector("button[id$='testModeButton']")), "Controller Status Updated");
            }
            else
            {
                WebElement testModeElement = driver.findElement(By.cssSelector("button[id$='testModeButton']"));
                testModeElement.click();
            }
            modeChanged = true;
        }
        else
        {
            //---close the dialog--- so it isn't in the way
            WebElement dialogElement = driver.findElement(
                By.cssSelector("div[id$='controllerModeDialog']"));
            WebElement closeDialog = dialogElement.findElement(
                    By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
            closeDialog.click();
            modeChanged = false;
        }

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("span[id$='controllerModePanel']")));
        return modeChanged;
    }
    
    /**
     * Verify system build properties exist in a list of web elements.
     * @param buildPropElements
     *     the list of web elements expected to contain the build properties
     */
    public static void verifyBuildProperties(final List<WebElement> buildPropElements)
    {
        //list to hold the text of the web elements
        List<String> elementText = new ArrayList<String>();
        //Verify there are more than 8 text items. There should be three build properties (and their values) 
        //that are always assigned despite the build type, along the table column headers. Depending on the build 
        //type upon which these tests are ran against there maybe more properties.
        assertThat(buildPropElements.size(), greaterThan(8));
        
        //gather all the text
        for (WebElement element: buildPropElements)
        {
            elementText.add(element.getText());
        }
        assertThat(elementText, 
                hasItems("Build Property Key", "Build Property Value", "build.type", "build.time"));
    }
    
    /**
     * Method that removes any additional controllers that have been added and insures that the default controller has
     * a channel. This method should be used in the {@link org.junit.AfterClass} method to verify the correct 
     * controller/channel settings before continuing testing.
     * 
     * @param driver
     *      Current web driver.
     */
    public static void cleanupControllerCheck(final WebDriver driver) throws InterruptedException, ExecutionException, 
        TimeoutException
    {
        //Add and set active the default controller if it isn't already available.
        if (getControllerListElement(driver, DEFAULT_CONTROLLER_NAME) == null)
        {
            createAndSetActiveController(driver);
        }
        else if (!getNameOfActiveContoller(driver).equals(DEFAULT_CONTROLLER_NAME))
        {
            setActiveControllerByName(driver, DEFAULT_CONTROLLER_NAME);
        }
        
        //Must retrieve the list every single time to receive updated size information. 
        while (ChannelsHelper.retrieveChannelsList(driver, ChannelsHelper.SOCKET_TABLE_SELECTOR).size() > 1)
        {
            final List<WebElement> channelsList = 
                    ChannelsHelper.retrieveChannelsList(driver, ChannelsHelper.SOCKET_TABLE_SELECTOR);
            for (WebElement socket: channelsList)
            {
                final List<WebElement> cells = socket.findElements(By.tagName("td"));
                if(ChannelsHelper.channelRowContainsProperty(cells, DEFAULT_CONTROLLER_NAME + " - " 
                        + DEFAULT_CONTROLLER_ID))
                {
                    continue;
                }
                else
                {
                    final WebElement removeButton = 
                            socket.findElement(By.cssSelector("button[id$='removeSocketButton']"));
                    GrowlVerifier.verifyAndWaitToDisappear(20, removeButton, "Channel Removed");
                    //Break required! Cannot remove more than one channel per iteration over the sockets due to the page
                    //being refreshed by AJAX when a channel is removed. If it is not done this way a stale element
                    //exception will occur.
                    break;
                }
            }
        }
    }
    
    /**
     * Ensure that there are two Controllers.
     * @param driver
     *      Current web driver.
     */
    public static void ensureTwoControllers(final WebDriver driver) throws NumberFormatException, InterruptedException, 
        ExecutionException, TimeoutException
    {
        //verify that there are two controllers
        List<WebElement> controllerList = driver.findElements(By.cssSelector("table[class='controller']"));
        String nameOfActiveCont = ControllerHelper.getNameOfActiveContoller(driver);
        if (controllerList.size() <2)
        {
            if (nameOfActiveCont.equals(ControllerHelper.DEFAULT_CONTROLLER_NAME))
            {
                ControllerHelper.setGuiNameAndIdThenCreateChannel(driver);
                NavigationHelper.expandRightSideBarOnly(driver);        
            }
            else
            {
                ControllerHelper.createController(driver);
            }
        }
    }
}
