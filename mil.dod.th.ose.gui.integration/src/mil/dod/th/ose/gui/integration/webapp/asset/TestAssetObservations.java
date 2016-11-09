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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.detection.TargetClassificationTypeEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.ImageHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper.AssetTabConstants;
import mil.dod.th.ose.gui.integration.helpers.observation.ExpectedObservation;
import mil.dod.th.ose.gui.integration.helpers.observation.GeneralObservationInformation;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationConstants;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class TestAssetObservations
{
    private static WebDriver m_Driver;
    
    private static String ASSET_NAME = "testExampleAsset";
    
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
    public void init() throws Exception
    {
        NavigationHelper.collapseSideBars(m_Driver);
        
        AssetHelper.createAsset(m_Driver, "ExampleObservationsAsset", ASSET_NAME);

        //---activate asset which will create a bunch of observations---
        AssetHelper.activateAsset(m_Driver, ASSET_NAME);
        
        m_Driver.navigate().refresh();
        
        AssetHelper.chooseAssetTab(m_Driver, AssetTabConstants.OBSERVATION_TAB);
    }
    
    @After
    public void cleanUp() throws InterruptedException
    {
        //---deactivate and remove asset---
        AssetHelper.deactivateAsset(m_Driver, "testExampleAsset");
        AssetHelper.removeAsset(m_Driver, "testExampleAsset");
        
        // Delete all observations.
        ObservationHelper.deleteAllObservations(m_Driver);
    }

    /**
     * Create an example observation asset which will create observations on activation. Verify that the correct 
     * modalities, target classifications, mime types, and headers / images are correct for each observation that was 
     * assigned in the asset given a unique sensor ID.
     */
    @Test
    public void testObservations()
    {
        ObservationHelper.verifyExpectedObservations(makeObservationList());
    }

    /**
     * Verify the correct asset image shows up for the observation page.
     */
    @Test
    public void testObservationImages()
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        //list of all the panels
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div[id$='observationPanel']")));
        List<WebElement> panels = m_Driver.findElements(By.cssSelector("div[id$='observationPanel']"));
        
        //make sure there are actually observations to verify
        assertThat(panels.size(), greaterThan(0));
        
        //verify all the observations show the correct asset image
        for (WebElement webElement : panels)
        {
            WebElement assetImg = webElement.findElement(By.cssSelector("img[id$='obsAssetImg']"));
            assertThat(assetImg.isDisplayed(), is(true));
            
            String source = assetImg.getAttribute("src");
            String img = ImageHelper.getPictureOrIconName(source, ".png");
            
            assertThat(img, is(ObservationConstants.IMG_MOD_ACOUSTIC));
        }
    }
    
    /**
     * Verify that an observation can be removed and that it will no longer be displayed.
     */
    @Test
    public void testRemoveObsevation() throws InterruptedException, ExecutionException, TimeoutException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        //list of all the panels
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div[id$='observationPanel']")));
        
        //list of all trash cans
        List<WebElement> trashCans = m_Driver.findElements(By.cssSelector("button[id*='remObs']"));
        //verify we can remove an observation
        trashCans.get(0).click();
        
        //Confirm removal of observation.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id$='removeObservation']")));
        WebElement removeObsButton = m_Driver.findElement(By.cssSelector("button[id$='removeObservation']"));
        
        //Wait till observation is deleted
        GrowlVerifier.verifyAndWaitToDisappear(20, removeObsButton, "Observations Removed");
    }
    
    /**
     * Creates mocked observations.
     */
    private List<ExpectedObservation> makeObservationList()
    {
        List<ExpectedObservation> list = new ArrayList<>();
        
        // observations for each sensor modality and classification
        int classSize = TargetClassificationTypeEnum.values().length;
        int modalitySize = SensingModalityEnum.values().length;
        for (int i=0; i < Math.max(classSize, modalitySize); i++)
        {
            int classIdx = Math.min(i, classSize - 1);
            TargetClassificationTypeEnum classification = TargetClassificationTypeEnum.values()[classIdx];
            int modalityIdx = Math.min(i, modalitySize - 1);
            SensingModalityEnum modality = SensingModalityEnum.values()[modalityIdx];
            
            String sensorId = String.format("d-c=%d;m=%d", classIdx, modalityIdx);
            
            ExpectedObservation detectionObs = new ExpectedObservation(ASSET_NAME, 
                    ObservationSubTypeEnum.DETECTION, OperationMode.TEST_MODE, true);
            
            GeneralObservationInformation generalInfo = new GeneralObservationInformation();
            generalInfo.setHasAssetLocation(true);
            
            detectionObs.withObservationData("mil.dod.th.core.types.spatial.Coordinates" , 
                    "longitude", "latitude", "value", "dnDetachedState")
                    .withSensingModalities(modality)
                    .withTargetClassifications(classification)
                    .withSensorId(sensorId).withGeneralInformation(generalInfo)
                    .withObservedTime(100L);
            
            list.add(detectionObs);
        }
        
        //observation sensor id weather
        ExpectedObservation weatherObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.WEATHER, 
                OperationMode.TEST_MODE, false);
        
        weatherObs.withObservationData("mil.dod.th.core.observation.types.Weather", 
                "temperature", "pressure", "visibility", "weatherCondition", "dnDetachedState")
                .withSensorId("weather").withGeneralInformation(new GeneralObservationInformation());
        list.add(weatherObs);

        //observation sensor id video
        ExpectedObservation videoObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.VIDEO_METADATA, 
                OperationMode.TEST_MODE, false);
        
        videoObs.withObservationData("mil.dod.th.core.observation.types.VideoMetadata", 
                "recorder", "framesPerSecond", "startTime", "endTime", "dnDetachedState")
                .withMimeType("Mime Type: video/wav")
                .withSensorId("video").withGeneralInformation(new GeneralObservationInformation());
        list.add(videoObs);

        //observation sensor id audio
        ExpectedObservation audioObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.AUDIO_METADATA, 
                OperationMode.TEST_MODE, false);
        
        audioObs.withObservationData("mil.dod.th.core.observation.types.AudioMetadata", 
                "sampleOfInterest", "recorder", "value", "description", "dnDetachedState")
                .withMimeType("Mime Type: audio/mp3")
                .withSensorId("audio").withGeneralInformation(new GeneralObservationInformation());
        list.add(audioObs);

        //observation sensor id image
        ExpectedObservation imageObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.IMAGE_METADATA, 
                OperationMode.TEST_MODE, false);
        
        imageObs.withObservationData("mil.dod.th.core.observation.types.ImageMetadata", 
                "resolution", "samplesOfInterest", "imager", "imageCaptureReason", "dnDetachedState")
                .withMimeType("Mime Type: image/jpeg")
                .withSensorId("image").withGeneralInformation(new GeneralObservationInformation());
        list.add(imageObs);
        
        //observation sensor id status
        ExpectedObservation statusObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.STATUS, 
                OperationMode.TEST_MODE, false);
        
        statusObs.withObservationData("mil.dod.th.core.observation.types.Status", "summaryStatus",
                "batteryChargeLevel", "componentStatuses", "sensorRange", "powerConsumption", "dnDetachedState")
                .withSensorId("status").withGeneralInformation(new GeneralObservationInformation());
        list.add(statusObs);
        
        //observation sensor id biological
        ExpectedObservation biologicalObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.BIOLOGICAL, 
                OperationMode.TEST_MODE, false);
        
        biologicalObs.withObservationData("mil.dod.th.core.observation.types.Biological", "entries", "dnDetachedState")
                .withSensorId("biological").withSensingModalities(SensingModalityEnum.BIOLOGICAL)
                .withGeneralInformation(new GeneralObservationInformation());
        list.add(biologicalObs);
        
        //observation sensor id chemical
        ExpectedObservation chemicalObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.CHEMICAL, 
                OperationMode.TEST_MODE, false);
        
        chemicalObs.withObservationData("mil.dod.th.core.observation.types.Chemical", "entries", "dnDetachedState")
                .withSensorId("chemical").withGeneralInformation(new GeneralObservationInformation());
        list.add(chemicalObs);
        
        //observation sensor id cbrneTrigger
        ExpectedObservation cbrneTriggerObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.CBRNE_TRIGGER,
                OperationMode.TEST_MODE, false);
        
        cbrneTriggerObs.withObservationData("mil.dod.th.core.observation.types.CbrneTrigger", "entries",
                "dnDetachedState").withSensorId("cbrne-trigger").withGeneralInformation(
                new GeneralObservationInformation());
        list.add(cbrneTriggerObs);
        
        //observation sensor id waterQuality
        ExpectedObservation waterQualityObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.WATER_QUALITY,
                OperationMode.TEST_MODE, false);
        
        waterQualityObs.withObservationData("mil.dod.th.core.observation.types.WaterQuality", "ph", "dissolvedOxygen",
                "electricalConductivity", "oxydationReductionPotential", "turbidity", "temperature", "chlorine",
                "totalDisolvedSolid", "salinity", "specificGravity", "waterFlowPressure", "langelierSaturationIndex",
                "dnDetachedState").withSensorId("water-quality")
                .withSensingModalities(SensingModalityEnum.WATER_QUALITY)
                .withGeneralInformation(new GeneralObservationInformation());
        list.add(waterQualityObs);

        //observation sensor id power
        ExpectedObservation powerObs = new ExpectedObservation(ASSET_NAME, ObservationSubTypeEnum.POWER,
                OperationMode.TEST_MODE, false);

        powerObs.withObservationData("mil.dod.th.core.observation.types.Power", "classification", "activePower",
                "reactivePower", "voltage", "current", "loadActivePower", "loadReactivePower", "loadCurrent", "source", 
                "dnDetachedState").withSensorId("power")
                .withSensingModalities(SensingModalityEnum.POWER)
                .withGeneralInformation(new GeneralObservationInformation());
        list.add(powerObs);

        //observation sensor id generic
        ExpectedObservation genericObs = new ExpectedObservation(ASSET_NAME, null, OperationMode.TEST_MODE, false);
        GeneralObservationInformation generalInfo = new GeneralObservationInformation();
        generalInfo.setHasAssetLocation(true);
        generalInfo.setHasAssetOrientation(true);
        List<MapEntry> reservedFieldValues = new ArrayList<MapEntry>();
        reservedFieldValues.add(new MapEntry("resKey", "resValue"));
        generalInfo.withReservedFields(reservedFieldValues);
        genericObs.withSensorId("generic").withGeneralInformation(generalInfo);
        list.add(genericObs);
        return list;
    }
}
