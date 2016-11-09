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
package mil.dod.th.ose.gui.integration.webapp.observation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.ChannelsHelper;
import mil.dod.th.ose.gui.integration.helpers.GeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.MissionHelper;
import mil.dod.th.ose.gui.integration.helpers.MissionSetupHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.TimeUtil;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper.AssetTabConstants;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper.ExpandCollapse;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Class will test observation retrieval and deletion using provided filters
 * located at the top of the Observations Tab.
 * 
 * @author cweisenborn
 */
public class TestObservationFilters
{
    private static WebDriver m_Driver;
    
    private static final String ASSET_NAME = "testExampleAsset";
    private static final String ASSET_TYPE = "ExampleAsset";
    
    private static final Logger LOG = Logger.getLogger("test.observation.filters");
    
    @BeforeClass
    public static void setUpClass() throws InterruptedException, ExecutionException, TimeoutException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();

        NavigationHelper.collapseSideBars(m_Driver);
        AssetHelper.createAsset(m_Driver, ASSET_TYPE, ASSET_NAME);
        
        // TODO: TH-1995: once this issue is fixed, no longer need to delete observations first, but right now there 
        // will be pages of observations when first starting this class of tests
        ObservationHelper.deleteAllObservations(m_Driver);
        
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.CONFIGURATION_TAB);
    }
    
    @AfterClass
    public static void tearDownClass() throws InterruptedException
    {
        //Ensure that the asset used to create the observations is removed before the next test class.
        AssetHelper.removeAllAssets(m_Driver);
            
        //Refresh page and navigate to observation tab to ensure observation count is reset.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB);
    }
    
    @After
    public void cleanupTest() throws InterruptedException, ExecutionException, TimeoutException
    {
        //Ensure all observations have been removed before continuing on to the next test. 
        Thread.sleep(1000); //Some of the tests may still be triggering ajax updates.
        ObservationHelper.deleteAllObservations(m_Driver);
    }
    
    /**
     * Tests the observation tab's filter by string expression.
     * Verify that the filter displays the correct number of observations.
     */
    @Test
    public void testFilterObservationsByFilterExpression()
        throws TimeoutException, InterruptedException, ExecutionException
    {
        AssetHelper.assetCaptureData(m_Driver, ASSET_NAME, 2);
        
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB);
        
        // navigate to asset page again to cause a refresh of the obs table
        // if changing to the asset tab would just update the table, this wouldn't be necessary
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        
        // ensure all 5 obs are there on the page before proceeding
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 2, 25);
        
        // should be zero now
        ObservationFilterHelper.clickFilterByExpressionAndEnterExpression(m_Driver, 
                "assetName!='testExampleAsset'");
        ObservationFilterHelper.executeFiltering(m_Driver);
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 0, 10);
        
        //Return to no filter.
        ObservationFilterHelper.clickFilterByExpressionCheckBox(m_Driver);
        ObservationFilterHelper.executeFiltering(m_Driver);
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 2, 10);
    }
    
    /**
     * Tests the observation tab's filter by string expression and date.
     * Verify that the filter displays the correct number of observations.
     */
    @Test
    public void testFilterObservationsByFilterExpressionAndDate()
        throws TimeoutException, InterruptedException, ExecutionException
    {
        //setup mission
        final Calendar beforeTime = Calendar.getInstance();
        AssetHelper.assetCaptureData(m_Driver, ASSET_NAME, 3);
        final Calendar middleTime = Calendar.getInstance();
        
        LOG.log(Level.INFO, "Will look for observations between {0} and {1}", 
                new Object[] {TimeUtil.getFormattedTime(beforeTime), TimeUtil.getFormattedTime(middleTime)});
        
        //Thread sleep needed to ensure time difference between initial set of observations.
        Thread.sleep(5000);
        
        AssetHelper.assetCaptureData(m_Driver, ASSET_NAME, 2);
        
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB);
        
        // navigate to asset page again to cause a refresh of the obs table
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        
        // ensure all 7 obs show up on the page before proceeding
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 5, 30);
        
        //enter dates
        ObservationFilterHelper.clickFilterByDateAndEnterDates(m_Driver, beforeTime, middleTime);
        ObservationFilterHelper.executeFiltering(m_Driver);
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 3, 10);
        
        //enter expression
        ObservationFilterHelper.clickFilterByExpressionAndEnterExpression(m_Driver, 
                "assetName!='testExampleAsset'");
        ObservationFilterHelper.executeFiltering(m_Driver);
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 0, 10);

        //view all obs again
        ObservationFilterHelper.clickFilterByExpressionCheckBox(m_Driver);
        ObservationFilterHelper.clickFilterByDateCheckBox(m_Driver);
        ObservationFilterHelper.executeFiltering(m_Driver);
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 5, 10);
    }
    
    /**
     * Test the observation tab's retrieve by number and date simultaneously.
     * Verify that the correct number of observations are displayed when both the retrieve by number and date range are
     * selected.
     */
    @Test
    public void testRetrieveByDateAndByNumber() throws InterruptedException, ExecutionException, TimeoutException
    {
        long missionStopTime = setupObsMission(15);
        
        try
        {
            ChannelsHelper.removeSocketChannel(m_Driver, ChannelsHelper.HOST_NAME, "4000", false);

            final Calendar beforeTime = Calendar.getInstance();
            //give time for mission to complete
            int timeToWaitSec = (int)((missionStopTime - System.currentTimeMillis()) / 1000 + 1);
            GeneralHelper.sleepWithText(timeToWaitSec, "Waiting for mission to complete");
            
            ChannelsHelper.createSocketChannel(m_Driver);
            
            //time when mission should be stopped
            final Calendar middleTime = Calendar.getInstance();
            NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
            AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB); 
            
            ObservationFilterHelper.clickRetrieveDeleteAccordionPanel(m_Driver, ExpandCollapse.EXPAND);
            
            ObservationFilterHelper.clickNumberCheckBoxAndEnterNumberOfObs(m_Driver, "2");
            ObservationFilterHelper.executeObservationRetrieval(m_Driver);
            ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 2, 10);
            
            ObservationFilterHelper.clickNumberFilterCheckBox(m_Driver);
            
            // mission runs for 15 seconds, new obs every 2 seconds, should be at least 5 of them
            ObservationFilterHelper.clickDateCheckBoxAndEnterDateRange(m_Driver, beforeTime, middleTime);
            ObservationFilterHelper.executeObservationRetrieval(m_Driver);
            ObservationFilterHelper.waitTillObsTableUpdatesWithAtLeast(m_Driver, 5, 15);
        }
        finally
        {
            try
            {
                NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_MISSIONS);
                MissionHelper.executeRemoveOnMission(m_Driver, "obs-time-mission");
                NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
            }
            catch (Exception e)
            {
                LOG.log(Level.SEVERE, "Failed to clean up created mission", e);
            }
        }
    }
    
    /**
     * Test the observation tab's delete observation button with the date range enabled.
     * Verify that only the observations within the date range selected are removed.
     */
    @Test
    public void testDeleteObservationsByDate() throws InterruptedException, ExecutionException, TimeoutException
    {
        Calendar beforeTime = Calendar.getInstance();
        AssetHelper.assetCaptureData(m_Driver, ASSET_NAME, 2);
        Calendar middleTime = Calendar.getInstance();
        
        //Thread sleep needed to ensure time difference between initial set of observations.
        Thread.sleep(5000);
        
        AssetHelper.assetCaptureData(m_Driver, ASSET_NAME, 3);
        
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB); 
        
        // navigate to asset page again to cause a refresh of the obs table
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        
        ObservationFilterHelper.clickRetrieveDeleteAccordionPanel(m_Driver, ExpandCollapse.EXPAND);
        
        // wait to ensure all 5 capture obs are available
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 5, 25);
        
        ObservationFilterHelper.clickDateCheckBoxAndEnterDateRange(m_Driver, beforeTime, middleTime);
        ObservationFilterHelper.executeObservationDeletion(m_Driver);
        ObservationFilterHelper.waitTillObsTableUpdates(m_Driver, 3, 10);
        
        ObservationFilterHelper.clickDateFilterCheckBox(m_Driver);
    }
    
    /**
     * Walks through the setting of the data capture mission for testing the retrieval aspects of the obs tab.
     */
    private long setupObsMission(int missionDurationSecs) throws InterruptedException, ExecutionException, 
        TimeoutException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SETUP_MIS);
        NavigationHelper.collapseSideBars(m_Driver);
        WebElement missionTemplate = MissionSetupHelper.retrieveMissionByName(m_Driver, "timed-data-capture-loop");
        //Select and click the select mission button to proceed to the parameters tab.
        WebElement selectMissionButton = missionTemplate.findElement(
                By.cssSelector("div[class='ui-panel-content ui-widget-content']")).findElement(By.tagName("button"));
        assertThat(selectMissionButton, is(notNullValue()));
        selectMissionButton.click();
        
        //params page
        WebDriverWait wait = new WebDriverWait(m_Driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("span[class='ui-icon ui-icon-pencil']")));
        
        //Retrieves a list of all the pencil icon buttons which are used to edit a variable.
        List<WebElement> editButtons = 
                m_Driver.findElements(By.cssSelector("span[class='ui-icon ui-icon-pencil']"));
        //Retrieves a list of all check icon buttons used to accept changes made to a variable.
        List<WebElement> acceptButtons = m_Driver.findElements(By.cssSelector("span[class='ui-icon ui-icon-check']"));
        //Retrieves a list of all input boxes used to enter the desired value for a variable.
        List<WebElement> inputBoxes = m_Driver.findElements(By.cssSelector(
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
                    //asset
                    variableInputBox = inputBoxes.get(i);
                    assertThat(variableInputBox, is(notNullValue()));
                    wait.until(ExpectedConditions.visibilityOf(variableInputBox));
                    variableInputBox.clear();
                    variableInputBox.sendKeys(ASSET_NAME);
                    variableInputBox.click();
                    break;
                case 1: 
                    //time delay, should be one
                    break;
                case 2: 
                    //interval
                    variableInputBox = inputBoxes.get(i);
                    assertThat(variableInputBox, is(notNullValue()));
                    wait.until(ExpectedConditions.visibilityOf(variableInputBox));
                    variableInputBox.clear();
                    variableInputBox.sendKeys("2");
                    variableInputBox.click();
                    break;
                case 3:
                    WebElement dropDown = m_Driver.findElement(By.cssSelector("div[id$='variableDropDown']"));
                    WebElement dropDownButton = dropDown.findElement(
                            By.cssSelector("label[class='ui-selectonemenu-label ui-inputfield ui-corner-all']"));
                    dropDownButton.click();
                    wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("div[id*='variableDropDown_panel']")));
                    WebElement dropDownValue = m_Driver.findElement(
                            By.cssSelector("div[id*='variableDropDown_panel']")).findElements(
                                    By.tagName("li")).get(i);
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
        WebElement nextButton = m_Driver.findElement(By.xpath("//button[contains(@id, 'missionWiz_next')]"));
        assertThat(nextButton, is(notNullValue()));
        nextButton.click();
        
        long stopTime = System.currentTimeMillis() + (65 + missionDurationSecs) * 1000;
        MissionSetupHelper.setupTimedMission(m_Driver, System.currentTimeMillis() + 65 * 1000, stopTime);
        
        //load 
        MissionSetupHelper.loadMissionToController(m_Driver, "obs-time-mission");
        
        return stopTime;
    }
}
