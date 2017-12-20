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

import java.text.ParseException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.MissionHelper;
import mil.dod.th.ose.gui.integration.helpers.MissionSetupHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.TimeUtil;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Tests the functionality of the mission app status page. 
 * 
 * @author nickmarcucci
 */
public class TestMissionAppStatusPage
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
    }
    
    /**
     * Creates a mission with no time interval and verifies that it executes immediately.
     * Removes the mission at the end.
     */
    @Test
    public void testCreateAndExecuteImmediateTestMission() throws 
        InterruptedException, ExecutionException, TimeoutException
    {
        MissionSetupHelper.createMissionAndLoadToController(m_Driver, "Mission 1", false, false);
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_MISSIONS);
        
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 1");
        assertThat(theMissionCard, notNullValue());
        
        verifyCommonHeaders(theMissionCard, "RUNNING", "test-mission");
        
        //since only default times are used should be showing Stop: Indefinite
        WebElement stopIndef = theMissionCard.findElement(By.cssSelector("td[id*='mStopIndefinite']"));
        
        assertThat(stopIndef, notNullValue());
        assertThat(stopIndef.getText(), is("Indefinite"));
        
        //verify button stop is showing
        MissionHelper.executeStopOnMission(m_Driver, "Mission 1");
        
        //need to regrab the mission card since it has been updated
        theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 1");
        assertThat(theMissionCard, notNullValue());
        
        //verify mission state is shutdown and click shutdown button
        WebElement status2 = theMissionCard.findElement(By.cssSelector("td[id*='mStatus']"));
        
        assertThat(status2, notNullValue());
        assertThat(status2.getText(), is("SHUTDOWN"));
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='removeButton']")));
       
        //remove the mission
        MissionHelper.executeRemoveOnMission(m_Driver, "Mission 1");
    }
    
    /**
     * Creates a timed mission, verifies that it shows up as scheduled, executes it, 
     * stops it, and removes the mission.
     */
    @Test
    public void testCreateAndExecuteTimedTestMission() throws 
        InterruptedException, ExecutionException, TimeoutException, ParseException
    {
        String start = MissionSetupHelper.createMissionAndLoadToController(m_Driver, "Mission 2", true, true);
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_MISSIONS);
        
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 2");
        assertThat(theMissionCard, notNullValue());
        
        verifyCommonHeaders(theMissionCard, "SCHEDULED", "test-mission");
        
        WebElement startTime = theMissionCard.findElement(By.cssSelector("span[id*='mStartTime']"));
        assertThat(startTime, notNullValue());
        assertThat(startTime.getText(), is(start + "Z"));
        
        //execute a test
        WebElement testButton = theMissionCard.findElement(By.cssSelector("button[id*='testButton']"));
        assertThat(testButton, notNullValue());
        
        //execute mission
        WebElement executeButton = theMissionCard.findElement(By.cssSelector("button[id*='execButton']"));
        assertThat(executeButton, notNullValue());
        
        //cancel mission
        WebElement cancelButton = theMissionCard.findElement(By.cssSelector("button[id*='cancelButton']"));
        assertThat(cancelButton, notNullValue());
        
        //execute mission 
        MissionHelper.executeStartOnMission(m_Driver, "Mission 2");
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span[id*='mStopTime']")));
        
        //verify stop time is correct. first regrab the mission card
        theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 2");
        assertThat(theMissionCard, notNullValue());
        
        WebElement stopTime = theMissionCard.findElement(By.cssSelector("span[id*='mStopTime']"));
        assertThat(stopTime, notNullValue());
        
        Calendar startCalendar = TimeUtil.getCalendarFromFormattedDate(start);
        startCalendar.add(Calendar.MINUTE, 10);
        
        assertThat(stopTime.getText(), is(TimeUtil.getFormattedTime(startCalendar) + "Z"));
        
        //verify button stop is showing
        MissionHelper.executeStopOnMission(m_Driver, "Mission 2");
        
        //grab card again
        theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 2");
        assertThat(theMissionCard, notNullValue());
        
        //verify mission state is shutdown and click shutdown button
        WebElement status2 = theMissionCard.findElement(By.cssSelector("td[id*='mStatus']"));
        
        assertThat(status2, notNullValue());
        assertThat(status2.getText(), is("SHUTDOWN"));
        
        //remove the mission
        MissionHelper.executeRemoveOnMission(m_Driver, "Mission 2");
    }
    
    /**
     * Creates a timed mission and then cancels it. Removes the mission at the end.
     */
    @Test
    public void testCreateAndCancelTestMission() throws 
        InterruptedException, ExecutionException, TimeoutException
    {
        String start = MissionSetupHelper.createMissionAndLoadToController(m_Driver, "Mission 3", true, false);
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_MISSIONS);
        
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 3");
        assertThat(theMissionCard, notNullValue());
        
        verifyCommonHeaders(theMissionCard, "SCHEDULED", "test-mission");
        
        WebElement startTime = theMissionCard.findElement(By.cssSelector("span[id*='mStartTime']"));
        assertThat(startTime, notNullValue());
        assertThat(startTime.getText(), is(start + "Z"));
        
        //execute a test
        WebElement testButton = theMissionCard.findElement(By.cssSelector("button[id*='testButton']"));
        assertThat(testButton, notNullValue());
        
        //execute mission
        WebElement executeButton = theMissionCard.findElement(By.cssSelector("button[id*='execButton']"));
        assertThat(executeButton, notNullValue());
        
        //cancel mission
        WebElement cancelButton = theMissionCard.findElement(By.cssSelector("button[id*='cancelButton']"));
        assertThat(cancelButton, notNullValue());
        
        //cancel the mission
        MissionHelper.executeCancelOnMission(m_Driver, "Mission 3");
        
        theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 3");
        assertThat(theMissionCard, notNullValue());
        
        //verify mission state is shutdown and click shutdown button
        WebElement status2 = theMissionCard.findElement(By.cssSelector("td[id*='mStatus']"));
        
        assertThat(status2, notNullValue());
        assertThat(status2.getText(), is("CANCELED"));
        
        //remove 
        MissionHelper.executeRemoveOnMission(m_Driver, "Mission 3");
    }
    
    /**
     * Creates a mission and verifies that it can be tested. Mission is then canceled and removed.
     */
    @Test
    public void testCreateAndTestTestMission() throws InterruptedException, ExecutionException, TimeoutException
    {
        String start = MissionSetupHelper.createMissionAndLoadToController(m_Driver, "Mission 4", true, false);
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_MISSIONS);
        
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 4");
        assertThat(theMissionCard, notNullValue());
        
        verifyCommonHeaders(theMissionCard, "SCHEDULED", "test-mission");
        
        WebElement startTime = theMissionCard.findElement(By.cssSelector("span[id*='mStartTime']"));
        assertThat(startTime, notNullValue());
        assertThat(startTime.getText(), is(start + "Z"));
        
        //execute a test
        MissionHelper.executeTestOnMission(m_Driver, "Mission 4");
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("td[id='mLastResult']")));
        
        theMissionCard = MissionHelper.findCorrectRunningMissionCard(m_Driver, "Mission 4");
        assertThat(theMissionCard, notNullValue());
        
        WebElement lastTestResult = theMissionCard.findElement(By.cssSelector("td[id='mLastResult']"));
        assertThat(lastTestResult.getText(), is("PASSED"));
        
        //cancel mission
        MissionHelper.executeCancelOnMission(m_Driver, "Mission 4");
        
        //remove
        MissionHelper.executeRemoveOnMission(m_Driver, "Mission 4");
    }
    
    /**
     * Function to verify the common headers of a scheduled/running/shutdown mission on the missions page.
     * @param missionCard
     *  the webelement that holds all the information on the mission
     * @param statusTxt
     *  the status of the mission that is expected to be present
     * @param templateNameTxt
     *  the name of the template of the mission that is expected to be present
     */
    private void verifyCommonHeaders(final WebElement missionCard, final String statusTxt, final String templateNameTxt)
    {
        //wait for mission to show up
        WebElement status = missionCard.findElement(By.cssSelector("td[id*='mStatus']"));
        assertThat(status, notNullValue());
        assertThat(status.getText(), is(statusTxt));
        
        WebElement templateName = missionCard.findElement(By.cssSelector("span[id*='templateName']"));
        assertThat(templateName, notNullValue());
        assertThat(templateName.getText(), is(templateNameTxt));
    }
}
