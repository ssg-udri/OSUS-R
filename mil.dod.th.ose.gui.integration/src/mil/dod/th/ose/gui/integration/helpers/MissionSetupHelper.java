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
import static org.hamcrest.Matchers.notNullValue;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
 * This helper will run the mission setup with the test, template.
 * @author callen
 *
 */
public class MissionSetupHelper
{
    /**
     * Setup a mission using the test template. Includes intervals for mission if start and stop intervals
     * are true. Then loads the mission with the name provided to the currently connected controller.
     * Assumes controller is already connected.
     * @param driver
     *  the web driver to use
     * @param missionName
     *  the mission name that should be used to identify the mission
     * @param hasStartTimeInterval
     *  whether a start time interval is to be set for the mission being created.
     *  start time will be 10 minutes after the current time.
     * @param hasStopTimeInterval
     *  whether a stop time interval is to be set for the mission being created.
     *  stop time will be 20 minutes after the current time.
     * @return
     *  the String version of the start time if the start interval flag has been
     *  set to true. Otherwise, returns empty string.
     */
    public static String createMissionAndLoadToController(final WebDriver driver, final String missionName, 
            final boolean hasStartTimeInterval, final boolean hasStopTimeInterval) 
        throws InterruptedException, ExecutionException, TimeoutException
    {
        String startTime = createMissionSetupWithTestMission(driver, hasStartTimeInterval, hasStopTimeInterval);
        loadMissionToController(driver, missionName);
        
        return startTime;
    }
    
    /**
     * Method for importing mission templates and verifying the growl message title resulting from the type
     * of template imported.
     * @param driver
     *         the web driver performing the test
     * @param templatePath
     *         the path of the template to be uploaded to the web GUI
     * @param expectedMessage
     *         the expected title of the growl message as a result of the type of template uploaded
     */
    public static void importMissionAndVerify (final WebDriver driver, String templatePath, String expectedMessage)
    {
        //wait until import button becomes visible and then click it
        GeneralHelper.safeClickBySelector(By.cssSelector("button[id*='importButton']"));

        //Verify that the install bundle dialog is displayed.
        WebElement installDialog = driver.findElement(By.cssSelector("div[id*='importMissionDlg']"));
        GeneralHelper.safeWaitUntil(ExpectedConditions.visibilityOf(installDialog));
        
        //Enter the location of the template to be uploaded.
        WebElement fileUpload = installDialog.findElement(By.cssSelector("input[type='file']"));
        fileUpload.sendKeys(templatePath);
        
        //Ensure that the selected file shows up in dialog
        WebElement files = driver.findElement(By.cssSelector("table[class=files]"));
        GeneralHelper.safeWaitUntil(ExpectedConditions.visibilityOf(files));
        
        //Upload file and verify growl message matches expected message
        WebElement uploadButton = driver.findElement(By.cssSelector(".fileupload-buttonbar>button:first-of-type"));
        GrowlVerifier.verifyNoWait(2, uploadButton, expectedMessage);
    }
    
    /**
     * Setup a mission. The setup stops at the mission finish page, where the mission can be clear, loaded to a 
     * controller, saved, etc.
     * @param driver
     *  the web driver performing the test
     * @param hasStartTimeInterval
     *  if a start time interval is to be set for the mission being created. 
     *  Start time will be 10 minutes after the current time.
     * @param hasStopTimeInterval
     *  if a stop time interval is to be set for the mission being created.
     *  Stop time will be 20 minutes after the current time.
     * @return
     *  the String version of the start time that the mission was set to if the 
     *  start time interval flag is set to true. If not set, 
     */
    public static String createMissionSetupWithTestMission(final WebDriver driver, 
            final boolean hasStartTimeInterval, final boolean hasStopTimeInterval) throws InterruptedException
    {
        NavigationHelper.navigateToPage(driver, NavigationButtonNameConstants.NAVBUT_PROP_SETUP_MIS);
        NavigationHelper.collapseSideBars(driver);
        
        //Wait to assure the page has loaded.
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[contains(@id, 'selectInfo')]")));
        
        WebElement testMission = retrieveMissionByName(driver, "test-mission");
        //Verifies that the test mission was found.
        assertThat(testMission, is(notNullValue()));

        //Select and click the select mission button to proceed to the parameters tab.
        WebElement selectMissionButton = testMission.findElement(By.cssSelector("button[id*='selectMission']"));
        assertThat(selectMissionButton, is(notNullValue()));
        selectMissionButton.click();
        //Wait needed to assure that the elements for the tab has loaded.
        wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("span[class='ui-icon ui-icon-pencil']")));


        //Select and click the next button once all variables have been set.
        GeneralHelper.safeClickBySelector(By.xpath("//button[contains(@id, 'missionWiz_next')]"));
        
        //Wait needed to assure that the elements for the tab has loaded.
        wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@id, 'tChangerStart')]")));

        //get current time
        long currentTime = System.currentTimeMillis();
        String startTime = "";
        
        //if start time desired
        if (hasStartTimeInterval)
        {
            Calendar calendar = TimeUtil.getCalendarForTime(currentTime);
            calendar.add(Calendar.MINUTE, 10);
            startTime = TimeUtil.getFormattedTime(calendar);
            
            //get the web element
            GeneralHelper.safeClickBySelector(By.cssSelector("div[id='wizForm:tChangerStart']>div:nth-child(2)>span"));
            
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[id='wizForm:timeChoose:startTimeSelect_input']")));
            
            WebElement startTimeInput = driver.findElement(
                    By.cssSelector("input[id='wizForm:timeChoose:startTimeSelect_input']"));
            
            assertThat(startTimeInput, notNullValue());

            //input start time 
            startTimeInput.clear();
            startTimeInput.sendKeys(startTime);
            
            //find the done button to close the calendar
            GeneralHelper.safeClickBySelector(By.cssSelector("button[class='ui-datepicker-close" +
                            " ui-state-default ui-priority-primary ui-corner-all']"));
        }
        
        //if stop time desired
        if (hasStopTimeInterval)
        {
            GeneralHelper.safeClickBySelector(By.cssSelector("div[id='wizForm:tChangerEnd']>div:nth-child(2)>span"));
            
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[id='wizForm:timeChoose:stopTimeSelect_input']")));
            
            WebElement stopTimeInput = driver.findElement(
                    By.cssSelector("input[id='wizForm:timeChoose:stopTimeSelect_input']"));
            
            assertThat(stopTimeInput, notNullValue());
            
            Calendar calendar = TimeUtil.getCalendarForTime(currentTime);
            calendar.add(Calendar.MINUTE, 20);
            String stopTime = TimeUtil.getFormattedTime(calendar);
            
            stopTimeInput.clear();
            stopTimeInput.sendKeys(stopTime);
            
            //find the done button to close the calendar
            GeneralHelper.safeClickBySelector(By.cssSelector("button[class='ui-datepicker-close " +
                    "ui-state-default ui-priority-primary ui-corner-all']"));
        }
        
        
        //Select and click the restart check box.
        GeneralHelper.safeClickBySelector(
                By.cssSelector("span[class='ui-chkbox-icon ui-chkbox-icon ui-icon ui-icon-check']"));

        //Select and click the next button to proceed to the finish tab.
        GeneralHelper.safeClickBySelector(By.xpath("//button[contains(@id, 'missionWiz_next')]"));
        
        return startTime;
    }
    
    /**
     * Loads a mission to a controller with a given name. Assumes the current page is the last page of the 
     * mission wizard.
     * @param driver
     *  the web driver currently running
     * @param missionName
     *  the name of the mission that will be loaded
     */
    public static void loadMissionToController(final WebDriver driver, final 
            String missionName) throws InterruptedException, ExecutionException, TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(
                "input[name='wizForm:programName_editableInput']")));
        
        WebElement programNameDropDown = driver.findElement(
                By.cssSelector("input[name='wizForm:programName_editableInput']"));
        
        assertThat(programNameDropDown, notNullValue());
        
        programNameDropDown.sendKeys(missionName);
        
        WebElement loadMissionButton = driver.findElement(By.cssSelector("button[id='wizForm:loadMission']"));
        
        assertThat(loadMissionButton, notNullValue());
        
        //verify mission load message 
        GrowlVerifier.verifyAndWaitToDisappear(20, loadMissionButton, "Mission Info:");
    }
    
    /**
     * Method that retrieves the WebElement that represents the specified mission template. 
     * Returns null if the mission cannot be found.
     * 
     * @param driver
     *          {@link WebDriver} performing the test.
     * @param missionName
     *          Name of the mission to be retrieved.
     * @return
     *          {@link WebElement} that represents the mission or null if it cannot be found.
     */
    public static WebElement retrieveMissionByName(final WebDriver driver, final String missionName)
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[id='wizForm:missionGrid']")));
        
        WebElement topPaginator = driver.findElement(By.cssSelector("div[id*='missionGrid_paginator_top']"));
        int index = 0;
        int numPages = topPaginator.findElements(By.cssSelector("span[class*='ui-paginator-page']")).size();
        do
        {
            WebElement missionGrid = driver.findElement(By.cssSelector("div[id='wizForm:missionGrid_content']"));
            WebElement desiredMission = searchMissionPanel(missionGrid.findElements(
                    By.cssSelector("div[id$='missionPanel']")), missionName);
            
            if (desiredMission != null)
            {
                return desiredMission;   
            }
            
            topPaginator = driver.findElement(By.cssSelector("div[id*='missionGrid_paginator_top']"));
            List<WebElement> pageButtons = 
                    topPaginator.findElements(By.cssSelector("span[class*='ui-paginator-page ui-state-default']"));
            index ++;
            changePage(driver, pageButtons, index + 1);
            
        } while (index < numPages);
        return null;
    }
    
    /**
     * Method that searches the list of missions for a mission with the specified name. Returns null if the mission 
     * cannot be found.
     * 
     * @param missionList
     *          List of {@link WebElement}s that represents all missions on the current mission selection page. 
     * @param missionName
     *          Name of the mission to be retrieved.
     * @return 
     *          {@link WebElement} that represents the mission or null if it cannot be found.     
     */
    private static WebElement searchMissionPanel(final List<WebElement> missionList, final String missionName)
    {
        for (WebElement mission: missionList)
        {
            WebElement name = mission.findElement(By.cssSelector("span[id*='missionName']"));
            if (name.getText().equals(missionName))
            {
                return mission;
            }
        }
        return null;
    }
    
    /**
     * Method that navigates to the specified page of the mission selection grid on the mission setup page.
     * 
     * @param driver
     *          {@link WebDriver} performing the test.
     * @param pages
     *          List of page elements. List is 1-based value (not a 0 based index)
     * @param desiredPage
     *          Integer that represents the page to be navigated to.
     */
    private static void changePage(final WebDriver driver, final List<WebElement> pages, final int desiredPage)
    {
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        pages.get(desiredPage - 1).click();
        
        Boolean result = fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement activePage = driver.findElement(By.cssSelector("span[class='ui-paginator-page " +
                        "ui-state-default ui-corner-all ui-state-active']"));
                return Integer.valueOf(activePage.getText()) == desiredPage;
            }
        });
        assertThat(result, is(true));
    }
    
    /**
     * Setup the parameters for the test-mission. Drop down menu "Option 2" will be chosen.
     * Sets values and advances to the schedule page.
     * TD:Could be refactored to be able set the parameters for any mission.
     * @param testInt
     *      Integer value, will be submitted as a string
     * @param testDouble
     *      Double value, will be submitted as a string
     * @param testString
     *      String value, will be submitted as a string
     * @param driver
     *      the web driver to use
     */
    public static void setupMissionParameters(final Integer testInt, final Double testDouble, final String testString, 
            final WebDriver driver) throws InterruptedException
    {
      //Wait needed to assure that the elements for the tab has loaded.
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("span[class='ui-icon ui-icon-pencil']")));
        
        //Retrieves a list of all the pencil icon buttons which are used to edit a variable.
        List<WebElement> editButtons = 
                driver.findElements(By.cssSelector("span[class='ui-icon ui-icon-pencil']"));
        //Retrieves a list of all check icon buttons used to accept changes made to a variable.
        List<WebElement> acceptButtons = driver.findElements(By.cssSelector("span[class='ui-icon ui-icon-check']"));
        //Retrieves a list of all input boxes used to enter the desired value for a variable.
        List<WebElement> inputBoxes = driver.findElements(By.cssSelector(
                             "input[class='ui-inputfield ui-inputtext ui-widget ui-state-default ui-corner-all']"));

        //Loops through all elements in the list to set values for all variables.
        for (int i = 0; i < editButtons.size(); i++)
        { 
            //The web driver moves too swiftly and therefore elements can't change fast enough to keep up. Unfortunately
            //the built in explicit and implicit waits in selenium that have been used elsewhere do not seem to be 
            //sufficient enough in this case so a sleep was needed to add time between setting each variable.
            Thread.sleep(1000);
            
            //Select and click the edit button to make the variable value editable.
            WebElement editButton = editButtons.get(i);
            assertThat(editButton, is(notNullValue()));
            wait.until(ExpectedConditions.visibilityOf(editButton));
            editButton.click();
            
            WebElement variableInputBox = null;
            //Set the value of the variable depending on which variable it is.
            switch(i)
            {
                case 0: 
                    variableInputBox = inputBoxes.get(0);
                    assertThat(variableInputBox, is(notNullValue()));
                    wait.until(ExpectedConditions.visibilityOf(variableInputBox));
                    GeneralHelper.retrySendKeys(driver, variableInputBox, testInt.toString(), 5);
                    variableInputBox.click();
                    break;
                case 1: 
                    variableInputBox = inputBoxes.get(1);
                    assertThat(variableInputBox, is(notNullValue()));
                    wait.until(ExpectedConditions.visibilityOf(variableInputBox));
                    GeneralHelper.retrySendKeys(driver, variableInputBox, testDouble.toString(), 5);
                    variableInputBox.click();
                    break;
                case 2: 
                    variableInputBox = inputBoxes.get(2);
                    assertThat(variableInputBox, is(notNullValue()));
                    wait.until(ExpectedConditions.visibilityOf(variableInputBox));
                    GeneralHelper.retrySendKeys(driver, variableInputBox, testString, 5);
                    variableInputBox.click();
                    break;
                case 3:
                    WebElement dropDown = driver.findElement(By.cssSelector("div[id$='variableDropDown']"));
                    WebElement dropDownButton = dropDown.findElement(
                            By.cssSelector("label[class='ui-selectonemenu-label ui-inputfield ui-corner-all']"));
                    dropDownButton.click();
                    wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("div[id*='variableDropDown_panel']")));
                    WebElement dropDownValue = driver.findElement(
                            By.cssSelector("div[id*='variableDropDown_panel']")).findElements(
                                    By.tagName("li")).get(1);
                    dropDownValue.click();
                    break;
                default:
                    break;
            }
            
            //Select and click the button that accepts changes to the variable currently being edited.
            WebElement acceptButton = acceptButtons.get(i);
            assertThat(acceptButton, is(notNullValue()));
            wait.until(ExpectedConditions.visibilityOf(acceptButton));
            acceptButton.click();
        }
        
        //Select and click the next button once all variables have been set.
        WebElement nextButton = driver.findElement(By.xpath("//button[contains(@id, 'missionWiz_next')]"));
        assertThat(nextButton, is(notNullValue()));
        nextButton.click();
    }
    
    /**
     * Set up a start and stop time for a mission. Assumes that upon calling of this method that the driver in on
     * the Schedule tab of the mission setup wizard.
     * Sets the start and stop time a month in the future.
     * Advances to the 'finish' tab.
     * @param driver
     *      the web driver to use.
     */
    public static void setupTimedMission(WebDriver driver, long startTimeMs, long stopTimeMs)
    {
        //Wait needed to assure that the elements for the tab has loaded.
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@id, 'tChangerStart')]")));
        
        //get the web element
        WebElement tChangerStartTimedButton = driver.findElement(
                    By.cssSelector("div[id='wizForm:tChangerStart']>div:nth-child(2)>span"));
            
        tChangerStartTimedButton.click();
            
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[id='wizForm:timeChoose:startTimeSelect_input']")));
            
        WebElement startTimeInput = driver.findElement(
                By.cssSelector("input[id='wizForm:timeChoose:startTimeSelect_input']"));
            
        assertThat(startTimeInput, notNullValue());

        //input start time 
        String startTimeStr = TimeUtil.getFormattedTime(startTimeMs);
        GeneralHelper.retrySendKeys(driver, startTimeInput, startTimeStr, 5);

        WebElement startDoneButton = driver.findElement(By.cssSelector("button[class~='ui-datepicker-close']"));
        startDoneButton.click();
        
        WebElement tChangerStopTimedButton = driver.findElement(
                By.cssSelector("div[id='wizForm:tChangerEnd']>div:nth-child(2)>span"));
            
        tChangerStopTimedButton.click();
            
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[id='wizForm:timeChoose:stopTimeSelect_input']")));
            
        WebElement stopTimeInput = driver.findElement(
                    By.cssSelector("input[id='wizForm:timeChoose:stopTimeSelect_input']"));
            
        assertThat(stopTimeInput, notNullValue());
            
        String stopTimeStr = TimeUtil.getFormattedTime(stopTimeMs);
        GeneralHelper.retrySendKeys(driver, stopTimeInput, stopTimeStr, 5);
        
        WebElement stopDoneButton = driver.findElement(By.cssSelector("button[class~='ui-datepicker-close']"));
        stopDoneButton.click();
        
        //Select and click the restart check box.
        WebElement restartCheck = driver.findElement(
                 By.cssSelector("span[class='ui-chkbox-icon ui-chkbox-icon ui-icon ui-icon-check']"));
        assertThat(restartCheck, is(notNullValue()));
        restartCheck.click();
        
        //Select and click the next button to proceed to the finish tab.
        WebElement nextButton = driver.findElement(By.xpath("//button[contains(@id, 'missionWiz_next')]"));
        assertThat(nextButton, is(notNullValue()));
        nextButton.click();
    }
}
