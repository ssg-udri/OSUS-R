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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationHelper;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationImageHelper;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Test class for testing downloading audio, video, and image observations. Also tests viewing image observations from
 * within the browser.
 * 
 * @author cweisenborn
 */
public class TestObsViewer
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void beforeClass() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        //Navigate to page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS);
        NavigationHelper.collapseSideBars(m_Driver);
    }
    
    @After
    public void afterEach() throws InterruptedException
    {
        //Remove assets. Also cleanup in case of test failure.
        AssetHelper.removeAllAssets(m_Driver);
        
        //Remove observations.
        ObservationHelper.deleteAllObservations(m_Driver);
    }
    
    /**
     * Method that tests to make sure the download button is shown for observations that contain audio, video, and 
     * image. Also checks to make sure that image observation can be viewed directly in the browser.
     */
    @Test
    public void testViewingObs() throws InterruptedException, ExecutionException, TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 60);
        
        //Name of the asset to be created.
        String assetName = "testAsset";
        
        //Create asset.
        AssetHelper.createAsset(m_Driver, "ExampleObservationsAsset", assetName);      
        
        //Activate asset.
        AssetHelper.activateAsset(m_Driver, assetName);
        
        //Refresh needs since there is not push updates for the observation tab.
        m_Driver.navigate().refresh();

        //Navigate to the observation tab.
        WebElement obsTabButton = m_Driver.findElement(By.cssSelector("a[href*='assetObsTab']"));
        assertThat(obsTabButton, is(notNullValue()));
        obsTabButton.click();
        
        //Wait till the form on the observation tab is displayed.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("form[id*='obsForm']")));
        
        //Filter to only display image, audio, and video observations.
        ObservationFilterHelper.clickFilterByExpressionAndEnterExpression(m_Driver, 
                "imageMetadata != null || audioMetadata != null || videoMetadata != null");
        
        //Retrieve the table that contains observations.
        WebElement obsTable = m_Driver.findElement(By.cssSelector("tbody[id*='obsTable_data']"));
        assertThat(obsTable, is(notNullValue()));
        
        //Verify the number of observations.
        List<WebElement> obsList = obsTable.findElements(By.cssSelector("div[id$='observationPanel']"));
        assertThat(obsList.size(), is(4));
        
        int foundVideo = 0; 
        int foundAudio = 0;
        int foundImage = 0;
        List<WebElement> imageLinks = new ArrayList<WebElement>();
        for (WebElement obsPanel : obsList)
        {
            List<WebElement> videoLink = obsPanel.findElements(By.cssSelector("a[id*='dlVideo']"));
            List<WebElement> audioLink = obsPanel.findElements(By.cssSelector("a[id*='dlAudio']"));
            List<WebElement> imageLink = obsPanel.findElements(By.cssSelector("a[id*='viewImageObs']"));
            
            if (videoLink.size() != 0)
            {
                foundVideo++;
                assertThat(videoLink.size(), is(1));
                assertThat(videoLink.get(0).isDisplayed(), is(true));
            }
            else if (audioLink.size() != 0)
            {
                foundAudio++;
                assertThat(audioLink.size(), is(1));
                assertThat(audioLink.get(0).isDisplayed(), is(true));
            }
            else if (imageLink.size() != 0)
            {
                foundImage++;
                assertThat(imageLink.size(), is(1));
                assertThat(imageLink.get(0).isDisplayed(), is(true));
                imageLinks.add(imageLink.get(0));
            }
        }
        
        assertThat(imageLinks.size(), is(2));
        
        //Image with resolution metadata already set.
        imageLinks.get(0).click();
        ObservationImageHelper.verifyImageIsDisplayed(m_Driver);
        
        //Image without resolution metadata. ImageIO then gives the image resolution metadata.
        imageLinks.get(1).click();
        ObservationImageHelper.verifyImageIsDisplayed(m_Driver);
        
        assertThat(foundVideo, is(1));
        assertThat(foundAudio, is(1));
        assertThat(foundImage, is(2));
        
        //Clear and remove the filter.
        ObservationFilterHelper.clearFilterByExpression(m_Driver);
    }
}
