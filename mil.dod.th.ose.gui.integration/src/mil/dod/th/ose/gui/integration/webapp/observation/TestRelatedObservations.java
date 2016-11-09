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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.types.observation.RelationshipTypeEnum;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper.AssetTabConstants;
import mil.dod.th.ose.gui.integration.helpers.observation.ExpectedObservation;
import mil.dod.th.ose.gui.integration.helpers.observation.GeneralObservationInformation;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationHelper;
import mil.dod.th.ose.gui.integration.helpers.observation.RelatedObsStruct;
import mil.dod.th.ose.gui.integration.helpers.observation.RelatedObservation;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper.ExpandCollapse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Verifies correct related observations are displayed and that when related observation
 * items are clicked then the dialog that is displayed displays the correct observations 
 * in the correct order.
 * 
 * @author nickmarcucci
 *
 */
public class TestRelatedObservations
{
    private static WebDriver m_Driver;
    
    private static final String ASSET_NAME = "testExampleObsAsset";
    
    @BeforeClass
    public static void beforeClass() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
    }
    
    @AfterClass
    public static void afterClass() throws InterruptedException
    {           
        //Remove any left over assets. Cleanup in case of test failure.
        AssetHelper.removeAllAssets(m_Driver);
    }
    
    @Before
    public void init() throws InterruptedException
    {
        NavigationHelper.collapseSideBars(m_Driver);
        
        AssetHelper.createAsset(m_Driver, "ExampleRelatedObsAsset", ASSET_NAME);
        
        //---activate asset---
        AssetHelper.activateAsset(m_Driver, ASSET_NAME);
        
        m_Driver.navigate().refresh();
    }
    
    @After
    public void cleanUp() throws InterruptedException
    {
        //---deactivate and remove asset---
        AssetHelper.deactivateAsset(m_Driver, ASSET_NAME);
        AssetHelper.removeAsset(m_Driver, ASSET_NAME);

        // remove all observations after deactivation as another observation is created during deactivation
        ObservationHelper.deleteAllObservations(m_Driver);
    }
    
    /**
     * Verify that the correct related observations are shown
     */
    @Test
    public void testRelatedObservations() throws InterruptedException, ExecutionException, TimeoutException
    {
        AssetHelper.assetCaptureData(m_Driver, ASSET_NAME, 1);
        
        m_Driver.navigate().refresh();
        
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB);
        
        ObservationHelper.verifyExpectedObservations(createRelatedObservations());
    }
    
    /**
     * Verify that when a related observation is selected then the correct items are displayed in the dialog
     * as well as in proper sequence.
     */
    @Test
    public void testRelatedObservationDialog() throws InterruptedException, ExecutionException, TimeoutException
    {
        //main obs is index 0; weather obs is index 1
        final List<ExpectedObservation> obs = createRelatedObservations();
        AssetHelper.assetCaptureData(m_Driver, ASSET_NAME, 1);
        
        m_Driver.navigate().refresh();
        
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB);
        
        ObservationFilterHelper.clickFilterAccordionPanel(m_Driver, ExpandCollapse.EXPAND);
        ObservationFilterHelper.clickFilterByExpressionCheckBox(m_Driver);
        
        //grab the observation identified by the sensor id 12345 which is the 'main' observation
        WebElement observationPanel = ObservationHelper.grabObservationPanelBySensorId(obs.get(0).getSensorId());
        ObservationHelper.toggleObservationPanel(observationPanel);
        WebElement relationSection = ObservationHelper.findRelatedObservationSectionByType(
                observationPanel, RelationshipTypeEnum.PARENT);
        ObservationHelper.getAndClickFirstRelatedObservation(relationSection);
        
        //wait for the related observation dialog to open
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='relatedObsDlg']")));
        
        //grab the observation panel and verify that this is the weather observation
        WebElement divWithObsInfo = m_Driver.findElement(
                By.cssSelector("span[id$='relatedObsDlgComponent']>div:nth-child(2)"));
        WebElement dlgObservationPanel = divWithObsInfo.findElement(By.cssSelector("div[id$='observationPanel']"));
        ObservationHelper.verifyObservationPanel(dlgObservationPanel, obs.get(1));
        
        //find the child which should be the main observation and then click that
        WebElement dlgRelationSection = ObservationHelper.findRelatedObservationSectionByType(
                dlgObservationPanel, RelationshipTypeEnum.CHILD);
        ObservationHelper.getAndClickFirstRelatedObservation(dlgRelationSection);
        
        //give dialog time to reload. since dialog never disappears no good way to verify that the observation
        //dialog has reloaded.
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(10, TimeUnit.SECONDS)
                .pollingEvery(1, TimeUnit.SECONDS).ignoring(
                        StaleElementReferenceException.class, NoSuchElementException.class);
        
        //wait until the main obs is visible by waiting for sensor id to change
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement divWithObsInfo = m_Driver.findElement(
                        By.cssSelector("span[id$='relatedObsDlgComponent']>div:nth-child(2)"));
                WebElement dlgObservationPanel = divWithObsInfo.findElement(
                        By.cssSelector("div[id$='observationPanel']"));
                WebElement sensorId = dlgObservationPanel.findElement(By.cssSelector("span[id*='sensorId']"));
                
                return sensorId.getText().contains(obs.get(0).getSensorId());
            }
        });
        
        //grab the observation panel and verify that this is the main observation
        divWithObsInfo = m_Driver.findElement(
                By.cssSelector("span[id$='relatedObsDlgComponent']>div:nth-child(2)"));
        dlgObservationPanel = divWithObsInfo.findElement(By.cssSelector("div[id$='observationPanel']"));
        ObservationHelper.verifyObservationPanel(dlgObservationPanel, obs.get(0));
        
        //now hit the back button to hopefully go back to the weather observation
        WebElement backButton = m_Driver.findElement(
                By.cssSelector(
                        "span[id$='relatedObsDlgComponent']>div:nth-child(1)>button[id$='relatedObsBack']"));
        backButton.click();
        
        //wait until the weather obs is visible by waiting for sensor id to change
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement divWithObsInfo = m_Driver.findElement(
                        By.cssSelector("span[id$='relatedObsDlgComponent']>div:nth-child(2)"));
                WebElement dlgObservationPanel = divWithObsInfo.findElement(
                        By.cssSelector("div[id$='observationPanel']"));
                WebElement sensorId = dlgObservationPanel.findElement(By.cssSelector("span[id*='sensorId']"));
                
                return sensorId.getText().contains(obs.get(1).getSensorId());
            }
        });
        
        //verify the back button worked which means the weather observation should be shown
        divWithObsInfo = m_Driver.findElement(
                By.cssSelector("span[id$='relatedObsDlgComponent']>div:nth-child(2)"));
        dlgObservationPanel = divWithObsInfo.findElement(By.cssSelector("div[id$='observationPanel']"));
        ObservationHelper.verifyObservationPanel(dlgObservationPanel, obs.get(1));
        
        //get button to go forward and make sure that the observation shown is the 'main' observation
        WebElement forwardButton = m_Driver.findElement(By.cssSelector(
                "span[id$='relatedObsDlgComponent']>div:nth-child(1)>button[id$='relatedObsForward']"));
        forwardButton.click();
        
        //wait until the main obs is visible by waiting for sensor id to change
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement divWithObsInfo = m_Driver.findElement(
                        By.cssSelector("span[id$='relatedObsDlgComponent']>div:nth-child(2)"));
                WebElement dlgObservationPanel = divWithObsInfo.findElement(
                        By.cssSelector("div[id$='observationPanel']"));
                WebElement sensorId = dlgObservationPanel.findElement(By.cssSelector("span[id*='sensorId']"));
                
                return sensorId.getText().contains(obs.get(0).getSensorId());
            }
        });
        
        divWithObsInfo = m_Driver.findElement(
                By.cssSelector("span[id$='relatedObsDlgComponent']>div:nth-child(2)"));
        dlgObservationPanel = divWithObsInfo.findElement(By.cssSelector("div[id$='observationPanel']"));
        ObservationHelper.verifyObservationPanel(dlgObservationPanel, obs.get(0));
        
        //close out the dialog
        WebElement dialogClose = m_Driver.findElement(By.cssSelector("div[id$='relatedObsDlg']>div:nth-child(1)>a"));
        dialogClose.click();
    }
    
    /**
     * Make related observation.
     * 
     * @return
     *  the main observation in question that holds the observation with related observations
     */
    private List<ExpectedObservation> createRelatedObservations()
    {
        ExpectedObservation mainObs = new ExpectedObservation(ASSET_NAME, null,
                OperationMode.TEST_MODE, true);
        
        mainObs.withObservationData("mil.dod.th.core.types.spatial.Coordinates" , 
                "longitude", "latitude", "value", "dnDetachedState")
                .withSensingModalities(SensingModalityEnum.ACOUSTIC)
                .withSensorId("12345");
        
        RelatedObservation relations = new RelatedObservation();
        
        //parent
        ExpectedObservation weather = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.WEATHER, 
                OperationMode.TEST_MODE, false);
        weather.withObservationData("mil.dod.th.core.observation.types.Weather", 
                "temperature", "pressure", "visibility", "weatherCondition", "dnDetachedState")
                .withSensorId("12352");
        RelatedObservation weRelation = new RelatedObservation();
        weather.withRelatedObservation(weRelation.withChildren(new RelatedObsStruct(mainObs, "")));
        
        //children
        ExpectedObservation status = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.STATUS, 
                OperationMode.TEST_MODE, false);
        status.withObservationData("mil.dod.th.core.observation.types.Status", "summaryStatus",
                "batteryChargeLevel", "componentStatuses", "sensorRange", "powerConsumption", "dnDetachedState")
                .withSensorId("12346");
        RelatedObservation sRelation = new RelatedObservation();
        status.withRelatedObservation(sRelation.withParents(new RelatedObsStruct(mainObs, "")));
        
        ExpectedObservation audio = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.AUDIO_METADATA, 
                OperationMode.TEST_MODE, false);
        audio.withObservationData("mil.dod.th.core.observation.types.AudioMetadata", 
                "sampleOfInterest", "recorder", "value", "description", "dnDetachedState")
                .withMimeType("Mime Type: audio/mp3")
                .withSensorId("12347");
        RelatedObservation aRelation = new RelatedObservation();
        audio.withRelatedObservation(aRelation.withParents(new RelatedObsStruct(mainObs, "")));
        
        ExpectedObservation image = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.IMAGE_METADATA, 
                OperationMode.TEST_MODE, false);
        image.withObservationData("mil.dod.th.core.observation.types.ImageMetadata", 
                "resolution", "samplesOfInterest", "imager", "imageCaptureReason", "dnDetachedState")
                .withMimeType("Mime Type: image/jpeg")
                .withSensorId("12348");
        RelatedObservation iRelation = new RelatedObservation();
        image.withRelatedObservation(iRelation.withParents(new RelatedObsStruct(mainObs, "")));
        
        ExpectedObservation video = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.VIDEO_METADATA, 
                OperationMode.TEST_MODE, false);
        video.withObservationData("mil.dod.th.core.observation.types.VideoMetadata", 
                "recorder", "framesPerSecond", "startTime", "endTime", "dnDetachedState")
                .withMimeType("Mime Type: video/wav")
                .withSensorId("12349");
        RelatedObservation vRelation = new RelatedObservation();
        video.withRelatedObservation(vRelation.withParents(new RelatedObsStruct(mainObs, "")));
        
        ExpectedObservation detection = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.DETECTION, 
                OperationMode.TEST_MODE, false);
        detection.withObservationData()
                .withSensingModalities(SensingModalityEnum.ACOUSTIC)
                .withSensorId("12350");
        RelatedObservation dRelation = new RelatedObservation();
        detection.withRelatedObservation(dRelation.withParents(new RelatedObsStruct(mainObs, "")));
        
        ExpectedObservation orientation = new ExpectedObservation(ASSET_NAME, null, 
                OperationMode.TEST_MODE, false);
        
        GeneralObservationInformation info12351 = new GeneralObservationInformation();
        info12351.setHasAssetLocation(true);
        info12351.setHasAssetOrientation(true);
        info12351.setHasPlatformOrientation(true);
        info12351.setHasPointingLocation(true);
        
        orientation.withObservationData("mil.dod.th.core.types.spatial.Orientation", 
                "heading", "value", "elevation", "bank", "dnDetachedState")
                .withSensorId("12351").withGeneralInformation(info12351);
        
        RelatedObservation oRelation = new RelatedObservation();
        orientation.withRelatedObservation(oRelation.withParents(new RelatedObsStruct(mainObs, "")));
        
        //peers
        ExpectedObservation coordinates = new ExpectedObservation(ASSET_NAME, null, 
                OperationMode.TEST_MODE, false);
        
        GeneralObservationInformation info12353 = new GeneralObservationInformation();
        info12353.setHasAssetLocation(true);
        info12353.setHasAssetOrientation(true);
        info12353.setHasPlatformOrientation(true);
        info12353.setHasPointingLocation(true);
        
        coordinates.withObservationData("mil.dod.th.core.types.spatial.Coordinates" , 
                "longitude", "latitude", "value", "dnDetachedState")
                .withSensorId("12353").withGeneralInformation(info12353);
        
        RelatedObservation cRelation = new RelatedObservation();
        orientation.withRelatedObservation(cRelation.withParents(new RelatedObsStruct(mainObs, "")));
        
        relations.withParents(new RelatedObsStruct(weather, "parent uuid"));
        relations.withChildren(new RelatedObsStruct(status, ""),
                new RelatedObsStruct(audio, "child uuid"), new RelatedObsStruct(image, "sub image"),
                new RelatedObsStruct(video, "child uuid"), new RelatedObsStruct(detection, "child uuid"),
                new RelatedObsStruct(orientation, "child uuid"));
        
        relations.withPeers(new RelatedObsStruct(coordinates, "peer uuid"),
                new RelatedObsStruct(null, "sub image"), new RelatedObsStruct(null, ""));
        
        mainObs.withRelatedObservation(relations);
        
        return new ArrayList<ExpectedObservation>(Arrays.asList(
                mainObs, weather, status, audio, video, 
                image, detection, orientation, coordinates));
    }
}
