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
package mil.dod.th.ose.gui.integration.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.AssetCommandHelper;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper.AssetTabConstants;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.ImageHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper.ExpandCollapse;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
 * Integration test class to test Asset commands tab. Loads an example asset bundle
 * and creates an ExampleCommandAsset Asset to perform testing.
 * 
 * @author nickmarcucci
 *
 */
public class TestAssetCommands
{
    private static WebDriver m_Driver;
    private static final String ASSET_NAME = "testExampleCommandAsset";
    private static final String ASSET_TYPE = "ExampleCommandAsset";
    private static final String COMMAND_MSG_TITLE = "Asset Command Executed:";
    
    @BeforeClass
    public static void beforeClass() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        NavigationHelper.collapseSideBars(m_Driver);
    }
    
    @Before
    public void setup() throws InterruptedException
    {
        AssetHelper.createAsset(m_Driver, ASSET_TYPE, ASSET_NAME);
        
        //---choose the command tab ---
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.COMMAND_TAB);
    }
    
    @After
    public void teardown() throws InterruptedException
    {           
        //Remove any left over assets. Cleanup in case of test failure.
        AssetHelper.removeAllAssets(m_Driver);
    }
    
    /**
     * Verify the correct image shows up for the asset in the command and control tab of the assets page.
     */
    @Test
    public void testAssetImage()
    {
        WebElement assetCommandControl = m_Driver.findElement(By.cssSelector("div[id$='assetCommandControl']"));
        WebElement assetImage = assetCommandControl.findElement(By.tagName("img"));
        
        ImageHelper.getPictureOrIconNameAndVerify(assetImage, "magnetic.png", ".png");
    }
    
    /*
     * Verify sync pan tilt works as intended.
     */
    @Test
    public void testSyncPanTilt() throws InterruptedException, ExecutionException, TimeoutException
    {
        AssetCommandHelper.executeSyncCommand(m_Driver, ASSET_NAME, "Pan Tilt", "Sync Pan Tilt");
        
        List<String> parameterValues = new ArrayList<String>();
        parameterValues.add("22.0");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("10.0");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        
        grabParameterTableAndVerify(AssetCommandHelper.getAssetCommandPanel(m_Driver, ASSET_NAME), 
                parameterValues);
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Pan Tilt", ExpandCollapse.COLLAPSE);
    }
    
    /**
     * Verify that a single get command works.
     */
    @Test
    public void testAGetCommand() throws InterruptedException
    {
        AssetCommandHelper.executeGetCommand(m_Driver, ASSET_NAME, "Profiles", 
                "profiles=[mil.dod.th.core.types.Profile");
    }
    
    /*
     * Verify sync camera settings works as intended.
     */
    @Test
    public void testSyncCameraSettings() throws InterruptedException, ExecutionException, TimeoutException
    {
        AssetCommandHelper.executeSyncCommand(m_Driver, ASSET_NAME, "Camera Settings", "Sync Camera Settings");
        
        List<String> parameterValues = new ArrayList<String>();
        parameterValues.add("1.0");
        parameterValues.add("1.0");
        parameterValues.add("CLOUDY");
        parameterValues.add("");
        parameterValues.add("");
        
        grabParameterTableAndVerify(AssetCommandHelper.getAssetCommandPanel(m_Driver, ASSET_NAME), 
                parameterValues);
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Camera Settings", ExpandCollapse.COLLAPSE);
    }
    
    /*
     * Verify sync all works as intended.
     */
    @Test
    public void testSyncAll() throws InterruptedException, ExecutionException, TimeoutException
    {
        AssetCommandHelper.executeSyncAll(m_Driver, ASSET_NAME);
        
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Pan Tilt", ExpandCollapse.EXPAND);
        
        List<String> parameterValues = new ArrayList<String>();
        parameterValues.add("22.0");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("10.0");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");
        parameterValues.add("");

        grabParameterTableAndVerify(AssetCommandHelper.getAssetCommandPanel(m_Driver, ASSET_NAME), 
                parameterValues);
        
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Camera Settings", ExpandCollapse.EXPAND);
        
        parameterValues.clear();
        
        parameterValues.add("1.0");
        parameterValues.add("1.0");
        parameterValues.add("CLOUDY");
        parameterValues.add("");
        parameterValues.add("");
        
        grabParameterTableAndVerify(AssetCommandHelper.getAssetCommandPanel(m_Driver, ASSET_NAME), 
                parameterValues);
        
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Camera Settings", ExpandCollapse.COLLAPSE);
    }
    
    /*
     * Verify that you can hit add/delete button and the parameter table updates accordingly.
     */
    @Test
    public void testAddDeleteParameterButton() throws InterruptedException, ExecutionException, TimeoutException
    {
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Capture Image", ExpandCollapse.EXPAND);
        
        List<WebElement> tableRows = getParameterTableRows(AssetCommandHelper.
                getAssetCommandPanel(m_Driver, ASSET_NAME));
        
        final int rowCount = tableRows.size();
        
        //find an add button
        for (WebElement row : tableRows)
        {
            List<WebElement> thirdTd = row.findElements(By.cssSelector("td:nth-child(3)"));
            
            if (thirdTd.size() > 0)
            {
                //find what it contains. used the findElements method to avoid the NoSuchElementException
                //so lists should always be a size of one or empty.
                List<WebElement> buttons = thirdTd.get(0).findElements(By.cssSelector("button"));
                
                if (buttons.size() > 0)
                {
                    assertThat(buttons.get(0).getText(), is("Add"));
                    buttons.get(0).click();
                    break;
                }
            }
        }
        
        //Fluent wait used to wait for the parameter table to be updated when adding or deleting a parameter.
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        //Fluent wait needed because it may take a second for the table to be updated when a parameter is removed.
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                List<WebElement> tableRows = getParameterTableRows(AssetCommandHelper.
                        getAssetCommandPanel(m_Driver, ASSET_NAME));
                return tableRows.size() > rowCount;
            }
        });
        
        tableRows = getParameterTableRows(AssetCommandHelper.
                getAssetCommandPanel(m_Driver, ASSET_NAME));
        
        //find the delete button that was the add button
        for (WebElement row : tableRows)
        {
            List<WebElement> thirdTd = row.findElements(By.cssSelector("td:nth-child(3)"));
            
            if (thirdTd.size() > 0)
            {
                //find what it contains. used the findElements method to avoid the NoSuchElementException
                //so lists should always be a size of one or empty.
                List<WebElement> buttons = thirdTd.get(0).findElements(By.cssSelector("button"));
                
                if (buttons.size() > 0)
                {
                    assertThat(buttons.get(0).getText(), is("Delete"));
                    buttons.get(0).click();
                    break;
                }
            }
        }
        
        //Fluent wait needed because it may take a second for the table to be updated when a parameter is removed.
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                List<WebElement> tableRows = getParameterTableRows(AssetCommandHelper.
                        getAssetCommandPanel(m_Driver, ASSET_NAME));
                return tableRows.size() == rowCount;
            }
        });
        
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Capture Image", ExpandCollapse.COLLAPSE);
    }
    
    /*
     * Verify capture image command works as intended.
     */
    @Test
    public void testCaptureImageCommand() throws InterruptedException, ExecutionException, TimeoutException
    {
        Map<String, String> params = new HashMap<>();
        params.put("camera", AssetCommandHelper.ADD);
        params.put("camera.id", "12");
        params.put("camera.type", "IR");
        params.put("areaOfInterest", AssetCommandHelper.ADD);
        params.put("areaOfInterest.top", "1.0");
        params.put("areaOfInterest.right", "1.0");
        params.put("burst", AssetCommandHelper.ADD);
        params.put("burst.burstNumber", "3");
        params.put("burst.burstIntervalInMS","100");
        params.put("imageCompression", AssetCommandHelper.ADD);
        params.put("imageCompression.ratio", "20");
        params.put("pictureType", "AREA_OF_INTEREST");
        
        AssetCommandHelper.inputAllParameters(m_Driver, ASSET_NAME, "Capture Image", params);
        
        WebElement sendCommandButton = AssetCommandHelper.findCorrectSendButton(m_Driver, ASSET_NAME, 
                "Send Command", "sendCommand");
        assertThat(sendCommandButton, notNullValue());
        
        GrowlVerifier.verifyAndWaitToDisappear(20, sendCommandButton, COMMAND_MSG_TITLE);
        
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Capture Image", ExpandCollapse.COLLAPSE);
    }
    
    /*
     * Verify pan tilt command works as intended.
     */
    @Test
    public void testPanTiltCommand() throws InterruptedException, ExecutionException, TimeoutException
    {
        Map<String, String> params = new HashMap<>();
        params.put("panTilt", AssetCommandHelper.ADD);
        params.put("panTilt.azimuth", AssetCommandHelper.ADD);
        params.put("panTilt.azimuth.value", "12.0");
        params.put("panTilt.elevation", AssetCommandHelper.ADD);
        params.put("panTilt.elevation.value", "10.0");

        AssetCommandHelper.inputAllParameters(m_Driver, ASSET_NAME, "Pan Tilt", params);
        
        WebElement sendCommandButton = AssetCommandHelper.findCorrectSendButton(m_Driver, ASSET_NAME, "Send Command",
                "sendCommand");
        assertThat(sendCommandButton, notNullValue());
        
        GrowlVerifier.verifyNoWait(10, sendCommandButton, COMMAND_MSG_TITLE);
        
        AssetCommandHelper.clickAccordionPanel(m_Driver, ASSET_NAME, "Pan Tilt", ExpandCollapse.COLLAPSE);
    }
    
    /**
     * Function to verify parameter values in a command table panel. Function assumes that the command
     * table panel is already visible/expanded.
     * @param assetCommandPanel
     *  the asset command panel which holds the command and its fields that are to be verified
     * @param expectedValues
     *  the expected values of the parameters, in order, as they are expected to appear in the 
     *  parameter table
     */
    private void grabParameterTableAndVerify(WebElement assetCommandPanel, List<String> expectedValues)
    {
        List<WebElement> rows = getParameterTableRows(assetCommandPanel);
        
        int parameterToValidate = 0; 
        
        for (WebElement row : rows)
        {
            List<WebElement> thirdTd = row.findElements(By.cssSelector("td:nth-child(3)"));
            
            if (thirdTd.size() > 0)
            {
                List<WebElement> inputs = thirdTd.get(0).findElements(By.cssSelector("input[id*='treeInput']" +
                        "[type='text']"));
                List<WebElement> dropDowns = thirdTd.get(0).findElements(By.cssSelector("div[class$='dropDownMenu']"));
                
                
                if (inputs.size() > 0)
                {
                    WebElement textInput = inputs.get(0);
                    
                    assertThat(textInput.getAttribute("value"), is(expectedValues.get(parameterToValidate)));
                    parameterToValidate++;
                }
                else if (dropDowns.size() > 0)
                {
                    WebElement dropDown = dropDowns.get(0).findElement(By.cssSelector("label"));
                    
                    assertThat(dropDown.getText(), is(expectedValues.get(parameterToValidate)));
                    parameterToValidate++;
                }
            }
        }
    }
    
    /**
     * Retrieve the currently open asset command parameter table.
     * @param assetCommandPanel
     *  the asset command panel that holds the command panel to retrieve
     * @return
     *  the list of rows that are contained in the parameter table
     */
    private List<WebElement> getParameterTableRows(WebElement assetCommandPanel)
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 30);
        //table which holds parameters
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
                "div[class*='ui-accordion-content ui-helper-reset ui-widget-content']" +
                "[aria-hidden='false'][id*='tabView:assetCommandControl']")));
        WebElement accordion = assetCommandPanel.findElement(By.cssSelector(
                "div[class*='ui-accordion-content ui-helper-reset ui-widget-content']" +
                "[aria-hidden='false'][id*='tabView:assetCommandControl']"));
        WebElement table = accordion.findElement(By.tagName("tbody"));
        List<WebElement> rows = table.findElements(By.tagName("tr"));
        
        assertThat(rows.size(), greaterThan(0));
        
        return rows;
    }
}
