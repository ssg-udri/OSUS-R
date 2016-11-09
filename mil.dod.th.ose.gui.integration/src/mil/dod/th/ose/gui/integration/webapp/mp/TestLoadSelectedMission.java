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
package mil.dod.th.ose.gui.integration.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.ChannelsHelper;
import mil.dod.th.ose.gui.integration.helpers.ControllerHelper;
import mil.dod.th.ose.gui.integration.helpers.MissionHelper;
import mil.dod.th.ose.gui.integration.helpers.MissionSetupHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Test the load selected mission backing bean. 
 * 
 * @author callen
 *
 */
public class TestLoadSelectedMission 
{

    private static WebDriver m_Driver;

    @BeforeClass
    public static void setup()
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
    }
    
    @AfterClass
    public static void cleanup() throws InterruptedException, ExecutionException, TimeoutException
    {
        //Insures that the default controller has been added back and that all other controllers have been removed.
        ControllerHelper.cleanupControllerCheck(m_Driver);
    }

    /**
     * Setup a mission and click load mission.
     * 
     * Verify unable to load dialog is presented.
     */
    @Test
    public void testSelectedMissionNoController() throws InterruptedException, ExecutionException, TimeoutException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        ChannelsHelper.removeSocketChannel(m_Driver, "localhost", "4000", true);
        
        MissionSetupHelper.createMissionSetupWithTestMission(m_Driver, false, false);

        //Wait needed to assure that the elements for the tab has loaded.
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[contains(@id, 'loadMission')]")));

        //Selects the table which displays the parameters for the mission and then creates a list of the rows within
        //the table.
        WebElement parametersTable = m_Driver.findElement(By.cssSelector("div[class='ui-datatable-scrollable-body']"));
        List<WebElement> tableRows = parametersTable.findElements(By.tagName("tr"));
        assertThat(tableRows.size(), is(4));

        //Select and click the load mission button
        WebElement loadMissionButton = m_Driver.findElement(By.xpath("//button[contains(@id, 'loadMission')]"));
        assertThat(loadMissionButton, is(notNullValue()));
        loadMissionButton.click();
        
        //Wait needed to insure the unable to load dialog has opened.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@id, 'unableLoadDlg')]")));
        
        //Select and click unable to load dialog
        WebElement closeDialog = m_Driver.findElement(By.xpath("//button[contains(@id, 'closeDialog')]"));
        assertThat(closeDialog, is(notNullValue()));
        closeDialog.click();
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        ControllerHelper.createController(m_Driver);
    }
    
    /**
     * Setup a mission and click load mission.
     */
    @Test
    public void testSelectedMissionWithController() throws InterruptedException, ExecutionException, TimeoutException
    {
        //open left side bar
        NavigationHelper.expandSideBars(m_Driver);

        //create a channel and set an active controller
        ControllerHelper.setActiveControllerByName(m_Driver, "THarvest");

        Thread.sleep(1000);

        //create the mission to load
        MissionSetupHelper.createMissionSetupWithTestMission(m_Driver, false, false);

        //Wait needed to assure that the elements have loaded.
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[contains(@id, 'loadMission')]")));

        //Selects the table which displays the parameters for the mission and then creates a list of the rows within
        //the table.
        WebElement parametersTable = m_Driver.findElement(By.cssSelector("div[class='ui-datatable-scrollable-body']"));
        List<WebElement> tableRows = parametersTable.findElements(By.tagName("tr"));
        //Verify that the four properties of the test mission are shown appropriately.
        assertThat(tableRows.size(), is(4));
        
        MissionSetupHelper.loadMissionToController(m_Driver, "missionSelectedTest");
        
        //Verify that the page navigates back to the first page of the mission wizard.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(
                "div[id$='missionGrid_content']")));
        WebElement header = m_Driver.findElement(By.cssSelector(
                "li[class='ui-wizard-step-title ui-state-default ui-corner-all ui-state-highlight']"));
        assertThat(header.getText(), is("Choose A Mission"));
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_MISSIONS);
        
        MissionHelper.executeStopOnMission(m_Driver, "missionSelectedTest");
        MissionHelper.executeRemoveOnMission(m_Driver, "missionSelectedTest");
    }
}
