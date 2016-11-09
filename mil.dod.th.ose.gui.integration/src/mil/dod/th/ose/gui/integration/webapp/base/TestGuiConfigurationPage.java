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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.ChannelsHelper;
import mil.dod.th.ose.gui.integration.helpers.ControllerHelper;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Class that tests the functionality of the GUI configuration page.
 * 
 * @author cweisenborn
 */
public class TestGuiConfigurationPage
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
    }
    
    /**
     * Test that the build properties for the GUI are displayed.
     * Verify that at least a select number of build properties are displayed. The total number may vary depending on
     * the build.
     */
    @Test
    public void testGuiBuildInfo() throws InterruptedException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_GUI_CONFIG);
        
        WebElement propsTable = m_Driver.findElement(By.cssSelector("div[id$='buildProps']"));
        List<WebElement> buildRows = 
                propsTable.findElements(By.cssSelector("div[class='ui-dt-c']"));
        
        //Verify that the build properties table contains at least build type and time.
        ControllerHelper.verifyBuildProperties(buildRows);
    }
    
    /**
     * Test that the GUI name and ID can be altered.
     * Verify that the name and ID are updated appropriately by connecting the GUI to itself.
     */
    @Test
    public void testChangeNameAndId() throws InterruptedException, ExecutionException, TimeoutException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_GUI_CONFIG);
        //Set the new id of the GUI.
        WebElement nameInput = m_Driver.findElement(By.cssSelector("input[id$='systemname']"));
        nameInput.clear();
        nameInput.sendKeys("gui-test");
        
        //Set the new name of the GUI.
        WebElement idInput = m_Driver.findElement(By.cssSelector("input[id$='sysId']"));
        idInput.clear();
        idInput.sendKeys("0000006D");
        
        //Save the changes to the GUI name and id.
        WebElement saveButton = m_Driver.findElement(By.cssSelector("button[id$='save']"));
        GrowlVerifier.verifyNoWait(10, saveButton, "Updated System Information");
        
        //Connect the GUI to itself.
        ChannelsHelper.createSocketChannel(m_Driver, "localhost", "4001");
        NavigationHelper.expandRightSideBarOnly(m_Driver);
        
        //Find the name of all controllers.
        List<String> controllerNames = new ArrayList<String>();
        for (WebElement nameElement: m_Driver.findElements(By.cssSelector("span[id$='controllerName']")))
        {
            controllerNames.add(nameElement.getText());
        }
        //Verify that the new GUI name is among the list of controller names.
        assertThat(controllerNames, hasItems("gui-test"));
        
        //Set the GUI as the active controller.
        ControllerHelper.setActiveControllerByName(m_Driver, "gui-test");
        
        //Verify that the active controller display displays the correct information.
        WebElement activeController = m_Driver.findElement(By.cssSelector("span[id='activeControllerPanel']"));
        WebElement activeControllerName = activeController.findElement(By.cssSelector("span[id$='controllerName']"));
        assertThat(activeControllerName.getText(), containsString("gui-test"));
        WebElement activeControllerId = activeController.findElement(By.cssSelector("span[id$='controllerId']"));
        assertThat(activeControllerId.getText(), containsString("0x0000006D"));
        
        //Set the controller back to the default.
        ControllerHelper.setActiveControllerByName(m_Driver, "THarvest");
        
        //Verify that the active controller display shows the default controller is selected.
        activeController = m_Driver.findElement(By.cssSelector("span[id='activeControllerPanel']"));
        activeControllerName = activeController.findElement(By.cssSelector("span[id$='controllerName']"));
        assertThat(activeControllerName.getText(), containsString("THarvest"));
        activeControllerId = activeController.findElement(By.cssSelector("span[id$='controllerId']"));        
        assertThat(activeControllerId.getText(), containsString("0x0000007D"));
        
        //Remove the channel that connects the GUI to itself.
        ChannelsHelper.removeSocketChannel(m_Driver, "0x0000006D", "gui-test", "localhost", "4001", false);
        
        //Navigate back to the GUI configuration page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_GUI_CONFIG);
        
        //Set the GUI name back to the default.
        nameInput = m_Driver.findElement(By.cssSelector("input[id$='systemname']"));
        nameInput.clear();
        nameInput.sendKeys("gui-generic");
        
        //Set the GUI ID back to the default.
        idInput = m_Driver.findElement(By.cssSelector("input[id$='sysId']"));
        idInput.clear();
        idInput.sendKeys("00000001");
        
        //Save the changes to the GUI name and ID.
        saveButton = m_Driver.findElement(By.cssSelector("button[id$='save']"));
        GrowlVerifier.verifyAndWaitToDisappear(20, saveButton, "Updated System Information");
    }
    
    /**
     * Test that the GUI Configuration page is the page displayed for the base THOSE URL.
     * Verify that the appropriate fields for the GUI configuration page are displayed.
     */
    @Test
    public void testOpenBasePage() throws InterruptedException
    {
        //Navigate to the GUI Configuration page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_GUI_CONFIG);
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        //Verify that the GUI Configuration page fields are displayed.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[id*='save']")));
        WebElement header = m_Driver.findElement(By.cssSelector("span[id*='sysNameHeader']"));
        assertThat(header.getText(), is("System Name"));
        header = m_Driver.findElement(By.cssSelector("span[id*='sysIdHeader']")); 
        assertThat(header.getText(), is("System ID"));
        header = m_Driver.findElement(By.cssSelector("span[id*='sysVersionHeader']")); 
        assertThat(header.getText(), is("System Version"));
        header = m_Driver.findElement(By.cssSelector("span[id$='sysBuildInfoHeader']"));
        assertThat(header.getText(), is("System Build Information"));
        
        WebElement input = m_Driver.findElement(By.cssSelector("input[id$='systemname']"));
        assertThat(input.getAttribute("value"), is("gui-generic"));
        input = m_Driver.findElement(By.cssSelector("input[id$='sysId']"));
        assertThat(input.getAttribute("value"), is("0x00000001"));
        
        WebElement output = m_Driver.findElement(By.cssSelector("div[id$='buildProps']"));
        assertThat(output, is(notNullValue()));
        WebElement button = m_Driver.findElement(By.cssSelector("button[id$='save']"));
        assertThat(button, is(notNullValue()));
    }
}
