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
import static mil.dod.th.ose.test.matchers.Matchers.*;

import java.io.File;
import java.util.List;

import mil.dod.th.ose.gui.integration.helpers.MissionSetupHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.util.ResourceLocatorUtil;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selenium test for the add mission app setup page.
 * 
 * @author cweisenborn
 */
public class TestAddMissionAppSetup 
{
    private static WebDriver m_Driver;
    
    /** Mission variable used for setting up the test mission. **/
    private final static Integer TEST_INT = 125;
    private final static Double TEST_DOUBLE = 10.25;
    private final static String TEST_STRING = "WOOHOO!";
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SETUP_MIS);
        NavigationHelper.collapseSideBars(m_Driver);
    }
    
    @After
    public void teardown() throws InterruptedException
    {
        //the tests will run in any order, therefore we choose the mission tab and during the test
        //get the mission to the proper state for every test. 
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SETUP_MIS);
    }
    
    /**
     * Tests the mission selection tab on the mission setup page. 
     * Verify that the test mission shows up, the information dialog box appears with appropriate information, and that 
     * a mission can be selected. 
     */
    @Test
    public void testChooseMissionTab() throws InterruptedException
    {
        //Wait to assure the page has loaded.
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[contains(@id, 'selectInfo')]")));
        
        //Retrieve the test mission.
        WebElement testMission = MissionSetupHelper.retrieveMissionByName(m_Driver, "test-mission");
        
        //Brings up the information dialog for the integration test mission.
        WebElement infoButton = testMission.findElement(By.cssSelector("span[class='ui-panel-title']")).findElement(
                By.tagName("button"));
        assertThat(infoButton, is(notNullValue()));
        infoButton.click();
        
        //Wait needed to insure the information dialog has opened.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@id, 'infoBox')]")));
        
        //Select and click on the information dialog box.
        WebElement infoDialogBox = m_Driver.findElement(By.xpath("//div[contains(@id, 'infoBox')]"));
        assertThat(infoDialogBox, is(notNullValue()));
        infoDialogBox.click();
        
        //Verify that the mission name on the information dialog is appropriate.
        WebElement infoMissionName = m_Driver.findElement(By.xpath("//div[contains(@id, 'mInfo')]")).findElement(
                By.tagName("span"));
        assertThat(infoMissionName.getText(), is("test-mission"));
        
        //Gets a list of all parameters displayed on the information dialog.
        List<WebElement> infoParameters = m_Driver.findElement(
                By.cssSelector("ul[class='ui-datalist-data']")).findElements(By.tagName("li"));
        assertThat(infoParameters.size(), is(4));
        
        //Verify the first parameter displayed on the information dialog.
        WebElement parameter = infoParameters.get(0).findElement(By.tagName("span"));
        assertThat(parameter.getText(), is("testInt"));
        
        //Verify the second parameter displayed on the information dialog.
        parameter = infoParameters.get(1).findElement(By.tagName("span"));
        assertThat(parameter.getText(), is("testDouble"));
        
        //Verify the third parameter displayed on the information dialog.
        parameter = infoParameters.get(2).findElement(By.tagName("span"));
        assertThat(parameter.getText(), is("testString"));
        
        //Verify the fourth parameter displayed on the information dialog.
        parameter = infoParameters.get(3).findElement(By.tagName("span"));
        assertThat(parameter.getText(), is("testDropDown"));
        
        //Select and click the close button for information dialog box.
        WebElement closeDialogButton = infoDialogBox.findElement(
                By.cssSelector("a[class='ui-dialog-titlebar-icon ui-dialog-titlebar-close ui-corner-all']"));
        assertThat(closeDialogButton, is(notNullValue()));
        closeDialogButton.click();
        
        //Select and click the select mission button to proceed to the parameters tab.
        WebElement selectMissionButton = testMission.findElement(
                By.cssSelector("div[class='ui-panel-content ui-widget-content']")).findElement(By.tagName("button"));
        assertThat(selectMissionButton, is(notNullValue()));
        selectMissionButton.click();
    }
    
    /**
     * Tests the variables tab on the mission setup page. 
     * Verify that value can be set for each variable in the selected mission and that advancing to the next tab works.
     * @throws InterruptedException
     *          When thread is sleeping and another thread interrupts.
     */
    @Test
    public void testParametersTab() throws InterruptedException
    {
        //Retrieve the test mission.
        WebElement testMission = MissionSetupHelper.retrieveMissionByName(m_Driver, "test-mission");
        //Select and click the select mission button to proceed to the parameters tab.
        WebElement selectMissionButton = testMission.findElement(
                By.cssSelector("div[class='ui-panel-content ui-widget-content']")).findElement(By.tagName("button"));
        assertThat(selectMissionButton, is(notNullValue()));
        selectMissionButton.click();
        
        MissionSetupHelper.setupMissionParameters(TEST_INT, TEST_DOUBLE, TEST_STRING, m_Driver);
    }
    
    /**
     * Tests the schedule tab of the mission setup page. 
     * Verify that a start and stop time can be selected for the mission and that advancing to the next tab works 
     * appropriately.
     */
    @Test
    public void testScheduleTab() throws InterruptedException
    {
        //Retrieve the test mission.
        WebElement testMission = MissionSetupHelper.retrieveMissionByName(m_Driver, "test-mission");
        //Select and click the select mission button to proceed to the parameters tab.
        WebElement selectMissionButton = testMission.findElement(
                By.cssSelector("div[class='ui-panel-content ui-widget-content']")).findElement(By.tagName("button"));
        assertThat(selectMissionButton, is(notNullValue()));
        selectMissionButton.click();
        
        //setup params
        MissionSetupHelper.setupMissionParameters(TEST_INT, TEST_DOUBLE, TEST_STRING, m_Driver);
        
        //set schedule
        MissionSetupHelper.setupTimedMission(m_Driver, System.currentTimeMillis() + 30 * 1000,
                System.currentTimeMillis() + 60 * 1000); 
    }
    
    /**
     * Tests the finish tab of the mission setup page. 
     * Verify that the values set for the variables and schedule appear appropriately and finally test the clear mission
     * button.
     */
    @Test
    public void testFinishTab() throws InterruptedException
    {
        //Retrieve the test mission.
        WebElement testMission = MissionSetupHelper.retrieveMissionByName(m_Driver, "test-mission");
        //Select and click the select mission button to proceed to the parameters tab.
        WebElement selectMissionButton = testMission.findElement(
                By.cssSelector("div[class='ui-panel-content ui-widget-content']")).findElement(By.tagName("button"));
        assertThat(selectMissionButton, is(notNullValue()));
        selectMissionButton.click();
        
        //setup params
        MissionSetupHelper.setupMissionParameters(TEST_INT, TEST_DOUBLE, TEST_STRING, m_Driver);
        
        //set schedule
        MissionSetupHelper.setupTimedMission(m_Driver, System.currentTimeMillis() + 30 * 1000,
                System.currentTimeMillis() + 60 * 1000); 
        
        //Wait needed to assure that the elements for the tab has loaded.
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[contains(@id, 'clearMission')]")));
        
        //Selects the table which displays the parameters for the mission and then creates a list of the rows within
        //the table.
        WebElement parametersTable = m_Driver.findElement(By.cssSelector("div[class='ui-datatable-scrollable-body']"));
        List<WebElement> tableRows = parametersTable.findElements(By.tagName("tr"));
        assertThat(tableRows.size(), is(4));
        
        //Loops through the rows of the table that displays the parameters and verifies the name and value of each
        //parameter.
        for (int i = 0; i < 4; i++)
        {
            WebElement parameterName = tableRows.get(i).findElements(By.tagName("td")).get(0);
            WebElement parameterValue = tableRows.get(i).findElements(By.tagName("td")).get(1);
            
            //Verifies the name and value depending on which row is being checked.
            switch(i)
            {
                case 0:
                    assertThat(parameterName.getText(), is("testInt"));
                    assertThat(parameterValue.getText(), is(TEST_INT.toString()));
                    break;
                case 1:
                    assertThat(parameterName.getText(), is("testDouble"));
                    assertThat(parameterValue.getText(), is(TEST_DOUBLE.toString()));
                    break;
                case 2:
                    assertThat(parameterName.getText(), is("testString"));
                    assertThat(parameterValue.getText(), is(TEST_STRING.toString()));
                    break;
                case 3:
                    assertThat(parameterName.getText(), is("testDropDown"));
                    assertThat(parameterValue.getText(), is("Option 2"));
                    break;
                default:
                    break;
            }
        }
        
        //Verify that the appropriate mission schedule is displayed
        WebElement schedule = m_Driver.findElement(By.cssSelector("div[class='ui-fieldset-content']")).findElement(
                By.tagName("tr")).findElements(By.tagName("td")).get(1).findElement(By.tagName("span"));
        assertThat(schedule.getText(), is("Timed"));
        
        //Select and click the clear mission button to make the clear mission dialog appear.
        WebElement clearMissionButton = m_Driver.findElement(By.xpath("//button[contains(@id, 'clearMission')]"));
        assertThat(clearMissionButton, is(notNullValue()));
        clearMissionButton.click();
        
        //Select and click the proceed button on the clear mission dialog window to clear the mission.
        WebElement proceedWithClear = m_Driver.findElement(By.xpath("//button[contains(@id, 'proceedClear')]"));
        assertThat(proceedWithClear, is(notNullValue()));
        proceedWithClear.click();
    }
    
    /**
     * Test ability to successfully import mission.
     */
    @Test
    public void testImportMission()
    {
        File simpleTemplate = new File(ResourceLocatorUtil.getBaseIntegrationPath(),
                "/testMissionTemplates/simple-template.xml");
        assertThat(simpleTemplate, isFile());
        String expectedGrowlMessage = "Mission template successfully imported!";
        
        MissionSetupHelper.importMissionAndVerify(m_Driver, simpleTemplate.getAbsolutePath(), expectedGrowlMessage);
    }
}
