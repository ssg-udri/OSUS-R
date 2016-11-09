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
package mil.dod.th.ose.gui.integration.webapp.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.ose.gui.integration.helpers.ChannelsHelper;
import mil.dod.th.ose.gui.integration.helpers.ControllerHelper;
import mil.dod.th.ose.gui.integration.helpers.GeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.ImageHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selenium test for the controller sidebar. Testing multiple controllers, the ability to set active controllers, and 
 * the ability to filter the controllers.
 * 
 * @author matt
 */
public class TestControllerSideBar
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        //Navigate to the observation tab to ensure that the observation count is zero before continuing.
        WebDriverWait wait = new WebDriverWait(m_Driver, 15);
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("a[href*='assetObsTab']")));
        WebElement observationTab = m_Driver.findElement(By.cssSelector("a[href*='assetObsTab']"));
        observationTab.click();
        
        //use the gui as another controller for testing
        ControllerHelper.setGuiNameAndIdThenCreateChannel(m_Driver);
    }
    
    @Before
    public void setup() throws InterruptedException
    {
        NavigationHelper.expandRightSideBarOnly(m_Driver);        
    }
    
    @AfterClass
    public static void cleanup() throws InterruptedException, ExecutionException, TimeoutException
    {
        //Insures that the default controller has been added back and that all other controllers have been removed.
        ControllerHelper.cleanupControllerCheck(m_Driver);
    }
    
    /**
     * Verify multiple controllers show up correctly in the controller sidebar.
     */
    @Test
    public void testMulitipleControllers() throws Exception
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 15);
        
        ControllerHelper.ensureTwoControllers(m_Driver);
        
        //verify that both controllers are in the sidebar
        assertThat(m_Driver.findElements(By.cssSelector("table[class='controller']")).size(), is(2));
        
        //---verify correct information is given for the controllers---
        WebElement defltGui = ControllerHelper.getControllerListElement(m_Driver, ControllerHelper.DEFAULT_GUI_NAME);
        assertThat("Controller with name " + ControllerHelper.DEFAULT_GUI_NAME + " could not be found.", 
                defltGui, is(notNullValue()));
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("span[class='controllerObsCnt']")));
        WebElement controllerObsQueued = defltGui.findElement(By.cssSelector("span[class='controllerObsCnt']"));
        
        //verify that the obs count is displayed initially as zero
        assertThat(controllerObsQueued.getText().contains("0"), is(true));
        
        //---get the other controller and test the information---
        WebElement defltCont = ControllerHelper.getControllerListElement(m_Driver, 
            ControllerHelper.DEFAULT_CONTROLLER_NAME);
        assertThat("Controller with name " + ControllerHelper.DEFAULT_CONTROLLER_NAME + " could not be found.", 
                defltCont, is(notNullValue()));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("span[class='controllerObsCnt']")));
        controllerObsQueued = defltCont.findElement(By.cssSelector("span[class='controllerObsCnt']"));

        //verify that the obs count is displayed initially as zero
        assertThat(controllerObsQueued.getText().contains("0"), is(true));
    }
    
    /**
     * Verify that the controller images are the correct images.
     */
    @Test
    public void testControllerImages()
    {
        List<WebElement> controllers = m_Driver.findElements(By.cssSelector("table[class='controller']"));
        
        assertThat(controllers.size(), greaterThan(0));
        
        for (WebElement webElement : controllers)
        {
            WebElement controllerImageBox = webElement.findElement(By.cssSelector("table[class='controllerImageBox']"));
            WebElement controllerImage = controllerImageBox.findElement(By.tagName("img"));
            ImageHelper.getPictureOrIconNameAndVerify(controllerImage, "controller.png", ".png");
        }
    }
    
    /**
     * Verify that if any controller that is added that can be chosen to be the active controller and that
     * the other controller will be made inactive.
     */
    @Test
    public void testActiveController() throws InterruptedException, NumberFormatException, ExecutionException, 
        TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
               
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.xpath("//div[contains(@id, 'controllerList')]")));

        ControllerHelper.ensureTwoControllers(m_Driver);
        
        //verify the active controller information
        String nameOfActiveCont = ControllerHelper.getNameOfActiveContoller(m_Driver);
        WebElement controllerId = m_Driver.findElement(By.xpath("//span[contains(@id, 'controllerId')]"));
        if (nameOfActiveCont.equals(ControllerHelper.DEFAULT_GUI_NAME))
        {
            //verify the active controller information
            assertThat(controllerId.getText(), containsString(ControllerHelper.DEFAULT_GUI_ID));
        }
        else
        {
            assertThat(nameOfActiveCont, is(ControllerHelper.DEFAULT_CONTROLLER_NAME));
            assertThat(controllerId.getText(), containsString(ControllerHelper.DEFAULT_CONTROLLER_ID));
        }

        //---Make the other controller the active controller---
        WebElement inactiveButton = m_Driver.findElements(By.cssSelector("button[class*='inactiveButton']")).get(0);
        
        //---Make the other controller the active controller. should be only inactive controller now---
        inactiveButton = m_Driver.findElement(By.cssSelector("button[class*='inactiveButton']"));
        //grab index of inactive controller
        String buttonIndex = GeneralHelper.getElementIndex(inactiveButton.getAttribute("id")); 
        inactiveButton.click();
        
        //wait until inactive controller becomes active controller 
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='" + buttonIndex 
                + ":active']")));
        
        //get the name of the active controller
        String nameOfOtherActiveCont = ControllerHelper.getNameOfActiveContoller(m_Driver);
        //verify the same controller isn't still active
        assertThat(nameOfActiveCont, is(not(nameOfOtherActiveCont)));
        
        //verify the active controller information
        controllerId = m_Driver.findElement(By.xpath("//span[contains(@id, 'controllerId')]"));
        if (nameOfOtherActiveCont.equals(ControllerHelper.DEFAULT_GUI_NAME))
        {
            assertThat(controllerId.getText(), containsString(ControllerHelper.DEFAULT_GUI_ID));
        }
        else if (nameOfOtherActiveCont.equals(ControllerHelper.DEFAULT_CONTROLLER_NAME))
        {
            //verify the active controller information
            assertThat(nameOfActiveCont, is(ControllerHelper.DEFAULT_CONTROLLER_NAME));
            assertThat(controllerId.getText(), containsString(ControllerHelper.DEFAULT_CONTROLLER_ID));
        }
    }
    
    /**
     * Verify removing and adding a controller.
     * Verify that if there is only one controller known that it is made the 'active' controller automatically.
     */
    @Test
    public void testRemoveThenAddController() throws NumberFormatException, InterruptedException, ExecutionException, 
        TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        //Checks which controller is active. This is important since the active controller will cause an additional
        //growl message to appear. So it is easier if the same controller is active every time. In this case the 
        //gui is always made the active controller if it is not already.
        String activeControllerName = ControllerHelper.getNameOfActiveContoller(m_Driver);
        if (activeControllerName != null && activeControllerName.equals("THarvest"))
        {
            ControllerHelper.setActiveControllerByName(m_Driver, ControllerHelper.DEFAULT_GUI_NAME);
        }
        
        NavigationHelper.collapseRightSideBarOnly(m_Driver);
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        wait.until(ExpectedConditions.presenceOfElementLocated(ChannelsHelper.SOCKET_TABLE_SELECTOR));
        
        //---disconnect from controllers---
        ChannelsHelper.removeSocketChannel(m_Driver, ControllerHelper.DEFAULT_CONTROLLER_ID,
                ControllerHelper.DEFAULT_CONTROLLER_NAME, ChannelsHelper.HOST_NAME, "4000", false);
        
        ChannelsHelper.removeSocketChannel(m_Driver, ControllerHelper.DEFAULT_GUI_ID,
                ControllerHelper.DEFAULT_GUI_NAME, ChannelsHelper.HOST_NAME, "4001", true);
        
        //verify that if not controller is set that the upper region 'active' controller 
        //area displays the correct information
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("span[id$='noController']")));
        WebElement noController = m_Driver.findElement(By.cssSelector("span[id$='noController']"));
        assertThat(noController.getText(), is("No Controller"));
        
        //---reconnect a controller---
        NavigationHelper.expandRightSideBarOnly(m_Driver);
        ControllerHelper.createController(m_Driver);
        
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("table[class='controller']")));
        assertThat(m_Driver.findElements(By.cssSelector("table[class='controller']")).size(), is(1));
       
        //verify again that if a controller is added that the newly added controller is automatically set
        //as the active controller
        activeControllerName = ControllerHelper.getNameOfActiveContoller(m_Driver);
        assertThat(activeControllerName, is(ControllerHelper.DEFAULT_CONTROLLER_NAME));
    }
    
    /**
     * Verify the Controller's OperationalMode can be changed.
     */
    @Test
    public void testControllerMode() throws InterruptedException, ExecutionException, TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        
        ControllerHelper.findControllerMenu(m_Driver);
        
        //Expand the right side bar so that the test can access the controller mode.
        NavigationHelper.expandRightSideBarOnly(m_Driver);

        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("button[id*='controllerList']")));
        
        //Change the mode 2 times (change, put back)
        ControllerHelper.changeControllerOpMode(m_Driver, "THarvest", OperationMode.OPERATIONAL_MODE, true);
        ControllerHelper.changeControllerOpMode(m_Driver, "THarvest", OperationMode.TEST_MODE, true);
    }

    /**
     * Verify the Specs for a Controller exist.
     */
    @Test
    public void testControllerSpecs() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        
        ControllerHelper.displayControllerInfo(m_Driver, ControllerHelper.DEFAULT_CONTROLLER_NAME);
        
        //open controller specs
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='specsButton']")));
        WebElement specButton = m_Driver.findElement(By.cssSelector("button[id*='specsButton']"));
        specButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id*='ctlrCapabilitiesHeader']")));
        WebElement capabilHeader = m_Driver.findElement(By.cssSelector("div[id*='ctlrCapabilitiesHeader']"));
        WebElement header = capabilHeader.findElement(By.cssSelector("span[class='ui-dialog-title']"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("BODY"), "Capabilities Document"));
        assertThat(header.getText(), is("Capabilities Document"));
        
        //--verify capabilities are displayed---
        capabilHeader = m_Driver.findElement(By.cssSelector("div[id*='ctlrCapabilitiesHeader']"));
        WebElement assetCapabils = capabilHeader.findElement(By.cssSelector("div[id*='ctlrTreeTable']"));
        assertThat(assetCapabils.getText().isEmpty(), is(false));
        
        //--verify specific item to capabilities
        //TODO: TH-1274 Improve testing.
        WebElement caps = capabilHeader.findElement(By.cssSelector("div[id*='TreeTable']"));
        assertThat(caps.getText().isEmpty(), is(false));
        assertThat(caps.getText(), containsString("Example Controller"));
        
        //---close the specs dialog---
        WebElement closeDialog = capabilHeader.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        assertThat(closeDialog.isDisplayed(), is(true));
        closeDialog.click();
        
        WebElement infoHeader = m_Driver.findElement(By.cssSelector("div[id$='controllerInfoDlg']"));
        
        //---close the info dialog---
        closeDialog = infoHeader.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        assertThat(closeDialog.isDisplayed(), is(true));
        closeDialog.click();
    }
    
    /**
     * Verify the build information for a Controller exist.
     */
    @Test
    public void testControllerBuildInfo() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        
        ControllerHelper.displayControllerInfo(m_Driver, ControllerHelper.DEFAULT_CONTROLLER_NAME);
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                "div[class='ui-tabs-panel ui-widget-content ui-corner-bottom']")));

        WebElement buildProprsHeader = m_Driver.findElement(
                By.xpath("//span[contains(@id, 'controllerBuildInfoHeader')]"));
        assertThat(buildProprsHeader.getText(), is("System Build Information"));
        
        //Retrieves a list of all the build property table elements
        List<WebElement> buildRows = 
                m_Driver.findElements(By.cssSelector("div[class='ui-dt-c']"));
        
        //verify build property information
        ControllerHelper.verifyBuildProperties(buildRows);
        
        //---close the info dialog---
        WebElement infoHeader = m_Driver.findElement(By.cssSelector("div[id$='controllerInfoDlg']"));

        WebElement closeDialog = infoHeader.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        assertThat(closeDialog.isDisplayed(), is(true));
        closeDialog.click();
    }
}