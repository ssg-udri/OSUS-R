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
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * This helper performs functions on missions on the missions page.
 * 
 * @author nickmarcucci
 *
 */
public class MissionHelper 
{
    /**
     * Function searches through the displayed missions for the correct mission card and returns that 
     * WebElement. If the mission cannot be found, then null will be returned.
     * @param driver
     *  the webdriver that is to be used
     * @param missionName
     *  the name of the mission that should be used to find the correct card
     * @return
     *  the webelement that contains the information for the mission specified by the mission name if found.
     *  returns false otherwise.
     */
    public static WebElement findCorrectRunningMissionCard(final WebDriver driver, final String missionName)
    {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table[class='ui-datagrid-data']")));
        
        List<WebElement> allMissionCards = driver.findElements(By.cssSelector(
                "div[id*='missionPanel'][class*='ui-panel ui-widget']"));
        
        for (WebElement missionCard : allMissionCards)
        {
            WebElement missionNameHeader = missionCard.findElement(By.cssSelector("span[id*='mName']"));
            
            if (missionNameHeader.getText().equals(missionName))
            {
                return missionCard;
            }
        }
        
        return null;
    }
    
    /**
     * Function to start a mission by specified name.
     * @param driver
     *  the web driver that is to be used
     * @param missionName
     *  the name of the mission that is to be started
     */
    public static void executeStartOnMission(final WebDriver driver, final String missionName) throws 
        InterruptedException, ExecutionException, TimeoutException
    {
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(driver, missionName);
        assertThat(theMissionCard, notNullValue());
        
        WebElement executeButton = theMissionCard.findElement(By.cssSelector("button[id*='execButton']"));
        assertThat(executeButton, notNullValue());
        
        GrowlVerifier.verifyAndWaitToDisappear(20, executeButton, "Mission Status Updated", "Mission Executed.");
    }
    
    /**
     * Function to execute a test for a mission specified by name.
     * @param driver
     *  the web driver that is to be used
     * @param missionName
     *  the name of the mission that the test should be executed on
     */
    public static void executeTestOnMission(final WebDriver driver, final String missionName)
    {
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(driver, missionName);
        assertThat(theMissionCard, notNullValue());
        
        WebElement testButton = theMissionCard.findElement(By.cssSelector("button[id*='testButton']"));
        assertThat(testButton, notNullValue());
        
        GrowlVerifier.verifyAndWaitToDisappear(20, testButton, "Mission Status Updated", "Mission Test");
    }
    
    /**
     * Function to find and cancel a mission based on mission name.
     * @param driver
     *  the web driver that is to be used
     * @param missionName
     *  the name of the mission that is to be canceled
     */
    public static void executeCancelOnMission(final WebDriver driver, final String missionName) throws 
        InterruptedException, ExecutionException, TimeoutException
    {
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(driver, missionName);
        assertThat(theMissionCard, notNullValue());
        
        WebElement cancelButton = theMissionCard.findElement(By.cssSelector("button[id*='cancelButton']"));
        assertThat(cancelButton, notNullValue());
        
        //verify messages
        GrowlVerifier.verifyAndWaitToDisappear(20, cancelButton, "Mission Status Updated");
    }
    
    /**
     * Function to find and remove a mission based on a mission name.
     * @param driver
     *  the web driver that is to be used
     * @param missionName
     *  the name of the mission that is to be removed
     */
    public static void executeRemoveOnMission(final WebDriver driver, final String missionName) throws 
        InterruptedException, ExecutionException, TimeoutException
    {
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(driver, missionName);
        assertThat(theMissionCard, notNullValue());
        
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='removeButton']")));
        WebElement removeButton = theMissionCard.findElement(By.cssSelector("button[id*='removeButton']"));
        assertThat(removeButton, notNullValue());
        
        //verify removed mission message
        GrowlVerifier.verifyAndWaitToDisappear(20, removeButton, "Mission Removed!");
    }
    
    /**
     * Function to find and stop a mission based on a mission name.
     * @param driver
     *  the web driver that is to be used
     * @param missionName
     *  the name of the mission that is to be removed
     */
    public static void executeStopOnMission(final WebDriver driver, final String missionName) throws 
        InterruptedException, ExecutionException, TimeoutException
    {
        WebElement theMissionCard = MissionHelper.findCorrectRunningMissionCard(driver, missionName);
        assertThat(theMissionCard, notNullValue());
        
        WebElement stopButton = theMissionCard.findElement(By.cssSelector("button[id*='stopButton']"));
        
        assertThat(stopButton, notNullValue());
        
        //stop mission
        GrowlVerifier.verifyAndWaitToDisappear(20, stopButton, "Mission Status Updated", "Mission Shutdown");
    }
}
