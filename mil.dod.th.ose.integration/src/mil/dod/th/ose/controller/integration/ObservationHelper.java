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
package mil.dod.th.ose.controller.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;

import mil.dod.th.core.observation.types.AlgorithmStatus;
import mil.dod.th.core.observation.types.AudioMetadata;
import mil.dod.th.core.observation.types.Biological;
import mil.dod.th.core.observation.types.BiologicalEntry;
import mil.dod.th.core.observation.types.CbrneTrigger;
import mil.dod.th.core.observation.types.CbrneTriggerEntry;
import mil.dod.th.core.observation.types.ChannelMetadata;
import mil.dod.th.core.observation.types.Chemical;
import mil.dod.th.core.observation.types.ChemicalEntry;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.ImageMetadata;
import mil.dod.th.core.observation.types.Lightning;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.ObservationRef;
import mil.dod.th.core.observation.types.Precipitation;
import mil.dod.th.core.observation.types.Power;
import mil.dod.th.core.observation.types.Relationship;
import mil.dod.th.core.observation.types.RoadCondition;
import mil.dod.th.core.observation.types.SkyCover;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.observation.types.TargetClassification;
import mil.dod.th.core.observation.types.VideoMetadata;
import mil.dod.th.core.observation.types.WaterQuality;
import mil.dod.th.core.observation.types.Weather;
import mil.dod.th.core.observation.types.WeatherCondition;
import mil.dod.th.core.observation.types.WindMeasurement;
import mil.dod.th.core.types.AlarmState;
import mil.dod.th.core.types.AngleDegrees;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.ConcentrationGramsPerLiter;
import mil.dod.th.core.types.ConductivitySiemensPerMeter;
import mil.dod.th.core.types.ConfidenceFactor;
import mil.dod.th.core.types.CountsPerTime;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.Direction;
import mil.dod.th.core.types.DirectionEnum;
import mil.dod.th.core.types.FrequencyDeltaKhz;
import mil.dod.th.core.types.FrequencyKhz;
import mil.dod.th.core.types.LangelierSaturationIndex;
import mil.dod.th.core.types.MagneticFluxDensityTeslas;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.PH;
import mil.dod.th.core.types.PhaseRadians;
import mil.dod.th.core.types.PowerWatts;
import mil.dod.th.core.types.PressureMillibars;
import mil.dod.th.core.types.PressurePascals;
import mil.dod.th.core.types.ProbabilityFactor;
import mil.dod.th.core.types.RadianceJoulesPerSqMeter;
import mil.dod.th.core.types.SNRdB;
import mil.dod.th.core.types.SPLdB;
import mil.dod.th.core.types.SalinityPsu;
import mil.dod.th.core.types.ScoringCriteria;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.SpecificGravity;
import mil.dod.th.core.types.SpeedMetersPerSecond;
import mil.dod.th.core.types.SquareDegrees;
import mil.dod.th.core.types.TemperatureCelsius;
import mil.dod.th.core.types.TurbidityNtu;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.types.VoltageVolts;
import mil.dod.th.core.types.audio.AudioRecorder;
import mil.dod.th.core.types.audio.AudioRecorderEnum;
import mil.dod.th.core.types.audio.AudioSampleOfInterest;
import mil.dod.th.core.types.biological.BiologicalAgent;
import mil.dod.th.core.types.biological.BiologicalAgentEnum;
import mil.dod.th.core.types.biological.BiologicalAssayResultEnum;
import mil.dod.th.core.types.biological.BiologicalCategory;
import mil.dod.th.core.types.biological.BiologicalCategoryEnum;
import mil.dod.th.core.types.biological.ColonyFormingUnits;
import mil.dod.th.core.types.biological.PlaqueFormingUnits;
import mil.dod.th.core.types.cbrnetrigger.CbrneModality;
import mil.dod.th.core.types.cbrnetrigger.CbrneModalityEnum;
import mil.dod.th.core.types.chemical.ChemicalAgent;
import mil.dod.th.core.types.chemical.ChemicalAgentEnum;
import mil.dod.th.core.types.chemical.ChemicalCategory;
import mil.dod.th.core.types.chemical.ChemicalCategoryEnum;
import mil.dod.th.core.types.detection.AcousticSignalInfo;
import mil.dod.th.core.types.detection.AcousticSignature;
import mil.dod.th.core.types.detection.ColorDescriptorType;
import mil.dod.th.core.types.detection.DetectionProbability;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.detection.HOGDescriptorType;
import mil.dod.th.core.types.detection.ImagerSignature;
import mil.dod.th.core.types.detection.ImagerWheelInfo;
import mil.dod.th.core.types.detection.MagneticSignature;
import mil.dod.th.core.types.detection.OpticalFlowType;
import mil.dod.th.core.types.detection.SeismicSignalInfo;
import mil.dod.th.core.types.detection.SeismicSignature;
import mil.dod.th.core.types.detection.TargetClassificationType;
import mil.dod.th.core.types.detection.TargetClassificationTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraScene;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.Compression;
import mil.dod.th.core.types.image.ExposureModeEnum;
import mil.dod.th.core.types.image.ExposureSettings;
import mil.dod.th.core.types.image.ImageCaptureReason;
import mil.dod.th.core.types.image.ImageCaptureReasonEnum;
import mil.dod.th.core.types.image.ImageGeoBoundingBox;
import mil.dod.th.core.types.image.PictureTypeEnum;
import mil.dod.th.core.types.image.PixelCircle;
import mil.dod.th.core.types.image.PixelPosition;
import mil.dod.th.core.types.image.PixelRectangle;
import mil.dod.th.core.types.image.PixelRegion;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.core.types.image.WhiteBalanceEnum;
import mil.dod.th.core.types.observation.RelationshipTypeEnum;
import mil.dod.th.core.types.spatial.HaeMeters;
import mil.dod.th.core.types.spatial.BankDegrees;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.DegreesPerSecond;
import mil.dod.th.core.types.spatial.DistanceMeters;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.Ellipse;
import mil.dod.th.core.types.spatial.HeadingDegrees;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.spatial.OrientationOffset;
import mil.dod.th.core.types.spatial.OrientationRate;
import mil.dod.th.core.types.spatial.TrackElement;
import mil.dod.th.core.types.status.AmbientStatus;
import mil.dod.th.core.types.status.AmbientType;
import mil.dod.th.core.types.status.AmbientTypeEnum;
import mil.dod.th.core.types.status.BatteryChargeLevel;
import mil.dod.th.core.types.status.ChargeLevelEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.InternalArchiveStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.types.video.VideoSampleOfInterest;
import mil.dod.th.core.types.weather.RoadConditionType;
import mil.dod.th.core.types.weather.RoadConditionTypeEnum;
import mil.dod.th.core.types.weather.SkyCoverAmountEnum;
import mil.dod.th.core.types.weather.WeatherIntensityEnum;
import mil.dod.th.core.types.weather.WeatherPhenomena;
import mil.dod.th.core.types.weather.WeatherPhenomenaEnum;
import mil.dod.th.core.types.weather.WeatherQualifierEnum;

/**
 * Class helps with creating observations and related inner objects.  Objects created will contain the same data unless
 * otherwise noted.  These objects can be used when the system is restarted to compare.
 * 
 * @author Dave Humeniuk
 *
 */
public class ObservationHelper
{
    /**
     * This will create a complete observation for an image.  The {@link ImageMetadata} and {@link DigitalMedia} object
     * will have all fields filled out as much as possible.  This can be used to compare with something pull from the 
     * database to prove that it is actually persisted.  Fields will always contain the same info so this method if 
     * called multiple times will create a different object, but will be equal to the previous one.
     * @return 
     *      Observation of a complete image
     */
    public static Observation createCompleteImageObservation()
    {
        PixelResolution resolution = new PixelResolution(200, 300);
        
        List<PixelRegion> samplesOfInterest = new ArrayList<PixelRegion>();
        samplesOfInterest.add(new PixelRegion(new PixelCircle(new PixelPosition(1, 2), 20), null, 
                new Direction(DirectionEnum.FN, "test")));
        samplesOfInterest.add(new PixelRegion(null, 
                new PixelRectangle(new PixelPosition(3, 5), new PixelPosition(8, 9)), 
                new Direction(DirectionEnum.LR_NF, "b")));
          
        List<DigitalMedia> maskSamplesOfInterest = new ArrayList<>();
        maskSamplesOfInterest.add(createCompleteDigitalMedia());
        maskSamplesOfInterest.add(createCompleteDigitalMedia());
        maskSamplesOfInterest.add(createCompleteDigitalMedia());

        Camera camera = new Camera(3, "test", CameraTypeEnum.VISIBLE);
        ExposureSettings exposureSettings = new ExposureSettings(ExposureModeEnum.APERTURE_PRIORITY, 3, 0.5, 1);
        CameraScene motionDetectionWindow = new CameraScene(1.0f, 0.5f, 2.3f, 2.9f);
        ImageCaptureReason imageCaptureReason = new ImageCaptureReason(ImageCaptureReasonEnum.MANUAL, "something");
        Compression compressionRatio = new Compression(2, null);
        ImageGeoBoundingBox imageGeographicRegion = new ImageGeoBoundingBox(createCoordinateObject(), 
                createCoordinateObject(), createCoordinateObject(), createCoordinateObject());
        Long captureTime = 20L;
        PictureTypeEnum pictureType = PictureTypeEnum.AREA_OF_INTEREST;
        Float focus = 0.5f;
        Float zoom = 1f;
        Boolean color = true;
        Double changedPixels = 0.5;
        WhiteBalanceEnum whiteBalance = WhiteBalanceEnum.AUTO;
        ImageMetadata imageMetaData = new ImageMetadata(resolution, 
                samplesOfInterest, 
                maskSamplesOfInterest, 
                camera, 
                exposureSettings, 
                motionDetectionWindow, 
                imageCaptureReason, 
                compressionRatio, 
                imageGeographicRegion,
                captureTime, 
                pictureType, 
                focus, 
                zoom, 
                color, 
                changedPixels, 
                whiteBalance);
        
        return ObservationHelper.createBaseObservation()
                   .withUuid(UUID.fromString("326da040-85bb-11e2-9e96-0800200c9a68"))
                   .withImageMetadata(imageMetaData)
                   .withDigitalMedia(ObservationHelper.createCompleteDigitalMedia());
    }

    /**
     * This will create a {@link DigitalMedia} object with all fields filed out.  Fields will always contain the same
     * info so this method if called multiple times will create a different object, but will be equal to the previous 
     * one.
     * @return 
     *      DigitalMedia
     */
    public static DigitalMedia createCompleteDigitalMedia() 
    {
        return new DigitalMedia(new byte[] { 0, 1, 2, 3}, "image/png");
    }
    
    /**
     * This will create a complete observation for coordinates  The {@link Coordinates} object
     * will have all fields filled out as much as possible.  This can be used to compare with something pull from the 
     * database to prove that it is actually persisted.  Fields will always contain the same info so this method if 
     * called multiple times will create a different object, but will be equal to the previous one.
     * @return 
     *      Observation of coordinates 
     */
    public static Observation createCoordinatesObservation()
    {
        
        Coordinates coords = createCoordinateObject();
        
        return ObservationHelper.createBaseObservation()
                   .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a68"))
                   .withAssetLocation(coords);      
    }

    /**
     * This will create a complete observation for orientation  The {@link Orientation} object
     * will have all fields filled out as much as possible.  This can be used to compare with something pull from the 
     * database to prove that it is actually persisted.  Fields will always contain the same info so this method if 
     * called multiple times will create a different object, but will be equal to the previous one.
     * @return 
     *      Observation of orientation 
     */
    public static Observation createOrientationObservation()
    {
        HeadingDegrees heading = new HeadingDegrees(80.0, 1.0, 1.0, 1.0, 0.0);
        ElevationDegrees elevation = new ElevationDegrees(90.0, 1.0, 1.0, 1.0, 0.0);
        BankDegrees bank = new BankDegrees(45.0, 1.0, 1.0, 1.0, 1.0);
        Orientation orient = new Orientation(heading, elevation, bank);
        
        return ObservationHelper.createBaseObservation()
                   .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a69")).withAssetOrientation(orient);
    }
    
    /**
     * This will create a complete observation for detection  The {@link Detection} object
     * will have all fields filled out as much as possible.  This can be used to compare with something pull from the 
     * database to prove that it is actually persisted.  Fields will always contain the same info so this method if 
     * called multiple times will create a different object, but will be equal to the previous one.
     * 
     * Variation 1 sets targetOrientation and not targetLob.
     * 
     * @return 
     *      Observation of detection
     */
    public static Observation createDetectionObservation(int variation)
    {
        if (variation == 1)
        {
            Orientation targetOrientation1 = SpatialTypesFactory.newOrientation(80, 90, 45);
            Orientation targetOrientation2 = SpatialTypesFactory.newOrientation(34, 43, 99.2);
            
            List<Orientation> targetOrientation = new ArrayList<>();
            targetOrientation.add(targetOrientation1);
            targetOrientation.add(targetOrientation2);
            
            AcousticSignature acousticSignature = new AcousticSignature()
                    .withCylinderCount(2)
                    .withDeltaFundamentalFrequency(new FrequencyDeltaKhz().withValue(120.0))
                    .withFootstepCadence(new FrequencyKhz().withValue(0.01))
                    .withFootstepCount(new CountsPerTime().withCount(2).withTimeWindowSeconds(1.0))
                    .withFrequencyChange(10.0)
                    .withFundamentalFrequency(new FrequencyKhz().withValue(500.0))
                    .withLargestMagnitude(new PressurePascals().withValue(4.0))
                    .withSignalInfo(new AcousticSignalInfo()
                            .withAmplitude(new PressurePascals().withValue(3.0))
                            .withFrequency(new FrequencyKhz().withValue(550.0))
                            .withPhase(new PhaseRadians().withValue(3.14)))
                    .withSignalToNoiseRatio(new SNRdB().withValue(0.001))
                    .withSoundPressureLevel(new SPLdB().withValue(0.01))
                    .withTimeLargestMagnitude(10L);

            return createDetectionObservation("328da040-85bb-11e2-9e96-0800200c9a70", targetOrientation, null,
                    acousticSignature, null, null, null);
        }
        else if (variation == 2)
        {
            OrientationOffset targetLob1 = SpatialTypesFactory.newOrientationOffset(180, 90, 45);
            OrientationOffset targetLob2 = SpatialTypesFactory.newOrientationOffset(33, 44, 55);
            
            List<OrientationOffset> targetLob = new ArrayList<>();
            targetLob.add(targetLob1);
            targetLob.add(targetLob2);
            
            SeismicSignature seismicSignature = new SeismicSignature()
                    .withCylinderCount(2)
                    .withDeltaFundamentalFrequency(new FrequencyDeltaKhz().withValue(120.0))
                    .withFootstepCadence(new FrequencyKhz().withValue(0.01))
                    .withFootstepCount(new CountsPerTime().withCount(2).withTimeWindowSeconds(1.0))
                    .withFrequencyChange(10.0)
                    .withFundamentalFrequency(new FrequencyKhz().withValue(500.0))
                    .withLargestMagnitude(new SpeedMetersPerSecond().withValue(4.0))
                    .withSignalInfo(new SeismicSignalInfo()
                            .withAmplitude(new SpeedMetersPerSecond().withValue(3.0))
                            .withFrequency(new FrequencyKhz().withValue(550.0))
                            .withPhase(new PhaseRadians().withValue(3.14)))
                    .withSignalToNoiseRatio(new SNRdB().withValue(0.001))
                    .withTimeLargestMagnitude(20L);

            return createDetectionObservation("328da040-85bc-11e2-9e96-0800200c9a70", null, targetLob,
                    null, seismicSignature, null, null);
        }
        else if (variation == 3)
        {
            OrientationOffset targetLob1 = SpatialTypesFactory.newOrientationOffset(180, 90, 45);
            OrientationOffset targetLob2 = SpatialTypesFactory.newOrientationOffset(33, 44, 55);
            
            List<OrientationOffset> targetLob = new ArrayList<>();
            targetLob.add(targetLob1);
            targetLob.add(targetLob2);
            
            MagneticSignature magneticSignature = new MagneticSignature()
                    .withCylinderCount(2)
                    .withDeltaFundamentalFrequency(new FrequencyDeltaKhz().withValue(120.0))
                    .withFootstepCadence(new FrequencyKhz().withValue(0.01))
                    .withFootstepCount(new CountsPerTime().withCount(2).withTimeWindowSeconds(1.0))
                    .withFrequencyChange(10.0)
                    .withFundamentalFrequency(new FrequencyKhz().withValue(500.0))
                    .withIntegral(0.002)
                    .withLargestMagnitude(new MagneticFluxDensityTeslas().withValue(4.0))
                    .withMagnitude(new MagneticFluxDensityTeslas().withValue(3.5))
                    .withSignalToNoiseRatio(new SNRdB().withValue(0.001))
                    .withTimeLargestMagnitude(30L);

            return createDetectionObservation("328da040-85bd-11e2-9e96-0800200c9a70", null, targetLob,
                    null, null, magneticSignature, null);
        }
        else if (variation == 4)
        {
            OrientationOffset targetLob1 = SpatialTypesFactory.newOrientationOffset(180, 90, 45);
            OrientationOffset targetLob2 = SpatialTypesFactory.newOrientationOffset(33, 44, 55);
            
            List<OrientationOffset> targetLob = new ArrayList<>();
            targetLob.add(targetLob1);
            targetLob.add(targetLob2);
            
            ImagerSignature imagerSignature = new ImagerSignature()
                    .withArea(new SquareDegrees().withValue(8.0))
                    .withColorDesc(new ColorDescriptorType()
                            .withCellHeight(1)
                            .withCellWidth(1)
                            .withData(new byte[] {0, 1, 2, 3, 4, 5})
                            .withHorizontalStride(2)
                            .withHueBins(3)
                            .withImageHeight(300)
                            .withImageWidth(200)
                            .withVerticalStride(1))
                    .withHOG(new HOGDescriptorType()
                            .withBlockHeight(1)
                            .withBlockWidth(1)
                            .withCellHeight(1)
                            .withCellWidth(1)
                            .withData(new byte[] {6, 7, 8, 9, 0})
                            .withHorizontalBlockStride(1)
                            .withHorizontalCellStride(1)
                            .withImageHeight(300)
                            .withImageWidth(200)
                            .withOrientationBins(4)
                            .withVerticalBlockStride(1)
                            .withVerticalCellStride(1))
                    .withOpticalFlow(new OpticalFlowType()
                            .withData(new byte[] {0, 0, 0, 0})
                            .withHorizontalStride(1)
                            .withImageHeight(300)
                            .withImageWidth(200)
                            .withVerticalStride(1))
                    .withPerimeter(new AngleDegrees().withValue(45.0))
                    .withWheelInfo(new ImagerWheelInfo()
                            .withWheelConfidence(0.99)
                            .withWheelLob(SpatialTypesFactory.newOrientationOffset(30))
                            .withWheelRadius(new AngleDegrees().withValue(10.0)));

            return createDetectionObservation("328da040-85be-11e2-9e96-0800200c9a70", null, targetLob,
                    null, null, null, imagerSignature);
        }
        else
        {
            throw new IllegalArgumentException("Invalid variation = " + variation);
        }
    }
    
    private static Observation createDetectionObservation(String uuid, List<Orientation> targetOrientation, 
            List<OrientationOffset> targetLob, AcousticSignature acousticSignature, SeismicSignature seismicSignature,
            MagneticSignature magneticSignature, ImagerSignature imagerSignature)
    {
        Coordinates targetLocation = createCoordinateObject();
        
        List<TargetClassification> listTarget= new ArrayList<TargetClassification>();
        List<ConfidenceFactor> listConfidence = new ArrayList<ConfidenceFactor>();
        List<ProbabilityFactor> listProbability = new ArrayList<ProbabilityFactor>();
        List<ScoringCriteria> listScoreCriteria = new ArrayList<ScoringCriteria>();
        SensingModality sense = new SensingModality(SensingModalityEnum.ACOUSTIC, "acoustic");
        ScoringCriteria scoreCriteria = new ScoringCriteria(sense, 0.0);
        listScoreCriteria.add(scoreCriteria);
        ConfidenceFactor confidenceFactor = new ConfidenceFactor(listScoreCriteria, 0.0);
        listConfidence.add(confidenceFactor);
        ProbabilityFactor probFactor = new ProbabilityFactor(0.0);
        listProbability.add(probFactor);
        TargetClassification targetClassifications = 
                new TargetClassification(
                        new TargetClassificationType(TargetClassificationTypeEnum.AIRCRAFT, "aircraft"), 
                        listConfidence, listProbability, 1);
        listTarget.add(targetClassifications);
        
        SpeedMetersPerSecond targetSpeed =
                new SpeedMetersPerSecond(100.0, 1.0, 1.0, 1.0, 0.0);
        DistanceMeters targetRange =
                new DistanceMeters(1000.0, 1.0, 1.0, 1.0, 0.0);
        
        FrequencyKhz targetFrequency =
                new FrequencyKhz(20.0, 1.0, 1.0, 1.0, 0.0);
        List<SensingModality> listSensings = new ArrayList<SensingModality>();
        SensingModality sensings = 
                new SensingModality(SensingModalityEnum.ACOUSTIC, "sensing");
        listSensings.add(sensings);
        
        List<TrackElement> listTrackHistory = new ArrayList<>();
        
        TrackElement trackElement1 = 
                new TrackElement(createCoordinateObject(), new SpeedMetersPerSecond(600.1, 0.44, 0.02, 0.55, 0.12), 
                        SpatialTypesFactory.newOrientation(32, 38, 102), null, 10L);
        
        TrackElement trackElement2 = new TrackElement(null, null, null, 
                SpatialTypesFactory.newOrientationOffset(25.5, 35.4, 135.5), 6L);
        
        listTrackHistory.add(trackElement1);
        listTrackHistory.add(trackElement2);
        
        Direction directionOfTravel = new Direction(DirectionEnum.FN, "blah");
        DetectionProbability detectionProbability = new DetectionProbability().withValue(0.75);
        Orientation targetLobGlobal = SpatialTypesFactory.newOrientation(90, 45, 45);
        OrientationOffset targetLobLocal = SpatialTypesFactory.newOrientationOffset(10, 4, 60.5);
        Double detectionLength = 1.5;
        OrientationRate targetAngularVelocity = new OrientationRate()
                .withAzimuth(new DegreesPerSecond().withValue(4.0))
                .withBank(new DegreesPerSecond().withValue(1.0))
                .withElevation(new DegreesPerSecond().withValue(10.5));
        Double targetRadialVelocityNormalized = 1.0;
        String targetId = "someID";
        String targetName = "someName";
        DetectionTypeEnum type = DetectionTypeEnum.ALARM;
        Integer targetCount = 1;
        String algorithmId = "algorithm1";
        
        Detection detect = new Detection(targetLocation, 
                listTarget, 
                targetSpeed, 
                targetRange,
                targetOrientation,
                targetLob, // targetLob can only be set if targetOrientation is not
                targetFrequency,
                listTrackHistory, 
                directionOfTravel,
                detectionProbability,
                targetLobGlobal,
                targetLobLocal,
                detectionLength,
                targetAngularVelocity,
                targetRadialVelocityNormalized,
                acousticSignature,
                seismicSignature,
                magneticSignature,
                imagerSignature,
                type,
                targetId,
                targetName,
                targetCount,
                algorithmId);
        
        return ObservationHelper.createBaseObservation()
                   .withModalities(listSensings)
                   .withUuid(UUID.fromString(uuid))
                   .withDetection(detect)
                   .withObservedTimestamp(90L);
    }
    
    /**
     * This will create a complete observation for status  The {@link Status} object
     * will have all fields filled out as much as possible.  This can be used to compare with something pull from the 
     * database to prove that it is actually persisted.  Fields will always contain the same info so this method if 
     * called multiple times will create a different object, but will be equal to the previous one.
     * @return 
     *      Observation of status
     * @throws DatatypeConfigurationException 
     *      thrown when there is duration configuration error
     */ 
    public static Observation createStatusObservation() throws DatatypeConfigurationException
    {
        ComponentStatus status1 = new ComponentStatus(new ComponentType(ComponentTypeEnum.NAVIGATION, "ins"),
                new OperatingStatus(SummaryStatusEnum.BAD, "no satellites"));
        ComponentStatus status2 = new ComponentStatus(new ComponentType(ComponentTypeEnum.DSP, "yup"),
                new OperatingStatus(SummaryStatusEnum.GOOD, "stuff"));
        
        List<ComponentStatus> componentStatuses = new ArrayList<>();
        componentStatuses.add(status1);
        componentStatuses.add(status2);
        BatteryChargeLevel batteryChargeLevel = 
                new BatteryChargeLevel(ChargeLevelEnum.FULL, null);
        Double sensorFov = 300.00;
        VoltageVolts batteryVoltage = new VoltageVolts().withValue(15.00);
        TemperatureCelsius temperature = new TemperatureCelsius().withValue(10.00);
        PowerWatts powerConsumption = new PowerWatts().withValue(10.00);
        VoltageVolts analogAnalogVoltage = new VoltageVolts().withValue(10.00);
        VoltageVolts analogDigitalVoltage = new VoltageVolts().withValue(10.00);
        VoltageVolts analogMagVoltage = new VoltageVolts().withValue(10.00);
        
        DistanceMeters sensorRange =
                new DistanceMeters(100.0, 1.0, 1.0, 1.0, 0.0);
        int assetOnTime = 10;
        
        InternalArchiveStatus internalArchiveStatus = new InternalArchiveStatus();
        internalArchiveStatus.setArchivingInProgress(true);
        internalArchiveStatus.setArchiveTimeAvailable(1000L);
        internalArchiveStatus.setTotalArchiveTime(3000L);
        int nextStatusDurationMs = 12000;
        
        List<AmbientStatus> ambientStatus = new ArrayList<>();
        ambientStatus.add(new AmbientStatus(
                new AmbientType(AmbientTypeEnum.OCCLUSION, "occlusion"), 
                    new OperatingStatus(SummaryStatusEnum.BAD, "occlusions are bad")));

        List<AlgorithmStatus> algoStatus = new ArrayList<>();
        algoStatus.add(new AlgorithmStatus()
            .withAlgorithmId("testAlgorithm")
            .withDetectionInterval(1.0f)
            .withFrameSizeSamples(1)
            .withHighFrequencyCutoff(new FrequencyKhz().withValue(100.0))
            .withLowFrequencyCutoff(new FrequencyKhz().withValue(50.0))
            .withProbablityDetection(0.5)
            .withSampleRateKHz(2.0f)
            .withSensitivity(0.6)
            .withSignatureInterval(2.0f));

        Status status = new Status(new OperatingStatus(SummaryStatusEnum.GOOD, "stuff too"),
                componentStatuses,
                ambientStatus,
                batteryChargeLevel,
                sensorRange,
                sensorFov,
                internalArchiveStatus, 
                temperature,
                batteryVoltage,
                powerConsumption,
                analogAnalogVoltage,
                analogDigitalVoltage,
                analogMagVoltage,
                algoStatus,
                assetOnTime,
                nextStatusDurationMs);

        return ObservationHelper.createBaseObservation()
                   .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a81")).withStatus(status);       
    }
    
    /**
     * This will create a complete observation for weather  The {@link Weather} object
     * will have all fields filled out as much as possible.  This can be used to compare with something pull from the 
     * database to prove that it is actually persisted.  Fields will always contain the same info so this method if 
     * called multiple times will create a different object, but will be equal to the previous one.
     * @return 
     *      Observation of weather data
     */
    public static Observation createWeatherObservation()
    {
        TemperatureCelsius temperature =
                new TemperatureCelsius(30.0, 1.0, 1.0, 1.0, 1.0);
        PressureMillibars pressure =
                new PressureMillibars(1000.0, 1.0, 1.0, 1.0, 1.0);
        PressureMillibars pressureAtSeaLevel = 
                new PressureMillibars(1200, 1.0, 1.0, 1.0, 1.0);
        DistanceMeters pressureAltitude = 
                new DistanceMeters(33, 1.0, 1.0, 1.0, 1.0);
        DistanceMeters densityAltitude = 
                new DistanceMeters(45, 1.0, 1.0, 1.0, 1.0);
        DistanceMeters visibility =
                new DistanceMeters(100.0, 1.0, 1.0, 1.0, 1.0);
        DistanceMeters waterDepth =
                new DistanceMeters(100.0, 1.0, 1.0, 1.0, 1.0);
        DistanceMeters ceilingHeight =
                new DistanceMeters(100.0, 1.0, 1.0, 1.0, 1.0);
        WindMeasurement windAvgSpeed =
                new WindMeasurement(new HeadingDegrees().withValue(45.06), 
                        new SpeedMetersPerSecond().withValue(3),
                        10d);
        WindMeasurement windGustSpeed =
                new WindMeasurement(new HeadingDegrees().withValue(45.10), 
                        new SpeedMetersPerSecond().withValue(60),
                        1.5d);
        SpeedMetersPerSecond waterAvgSpeed =
                new SpeedMetersPerSecond(100.00, 1.0, 1.0, 1.0, 1.0);
        TemperatureCelsius waterTemperature = new TemperatureCelsius().withValue(90);
        Precipitation precipitation = new Precipitation(20.0, 100.0);
        RoadCondition roadCondition = 
                new RoadCondition(
                        temperature, temperature,temperature, 
                        new RoadConditionType(RoadConditionTypeEnum.SHALLOW_FLOWING_WATER, "road"), 10.0);
        RadianceJoulesPerSqMeter solarRadiance =
                new RadianceJoulesPerSqMeter(100.0, 1.0, 1.0, 1.0, 1.0);

        List<WeatherCondition> weatherConditions = new ArrayList<>();
        weatherConditions.add(
                new WeatherCondition(new WeatherPhenomena(WeatherPhenomenaEnum.CLEAR, "sunny"), 
                        WeatherIntensityEnum.LIGHT, WeatherQualifierEnum.LOW_DRIFTING));
        Lightning lightning = new Lightning(new DistanceMeters().withValue(3), new CountsPerTime(3, 10));
        List<SkyCover> skyCovers = new ArrayList<>();
        skyCovers.add(new SkyCover(new HaeMeters().withValue(100), SkyCoverAmountEnum.OVERCAST));
        TemperatureCelsius dewPoint = new TemperatureCelsius().withValue(32);
        double relativeHumidity = .125;

        Weather weather = new Weather(temperature,
                pressure,
                pressureAtSeaLevel,
                pressureAltitude,
                densityAltitude,
                visibility,
                windAvgSpeed,
                windGustSpeed,
                waterAvgSpeed,
                waterTemperature,
                waterDepth,
                precipitation,
                roadCondition,
                ceilingHeight,
                solarRadiance,
                weatherConditions,
                lightning, 
                skyCovers, 
                dewPoint, 
                relativeHumidity);
             
        return ObservationHelper.createBaseObservation()
                .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a72")).withWeather(weather);
    }
    
    /**
     * This will create a complete observation for audio.  The {@link AudioMetadata} and {@link DigitalMedia} object
     * will have all fields filled out as much as possible.  This can be used to compare with something pull from the 
     * database to prove that it is actually persisted.  Fields will always contain the same info so this method if 
     * called multiple times will create a different object, but will be equal to the previous one.
     * @return 
     *      Observation of audio data
     */ 
    public static Observation createAudioObservation()
    {
        List<AudioSampleOfInterest> listSamples = new ArrayList<AudioSampleOfInterest>();
        AudioSampleOfInterest sampleOfInterest = new AudioSampleOfInterest(10L, 100L);
        listSamples.add(sampleOfInterest);
        
        AudioRecorder recorder = 
               new AudioRecorder(AudioRecorderEnum.MICROPHONE, "mic", 1);
        Double sampleRateKHz = 100.00;
        Long startTime = 10L;
        Long endTime = 200L;
        Long triggerTime = 100L;
       
        AudioMetadata audio = new AudioMetadata(
                listSamples,
                recorder,
                sampleRateKHz,
                startTime,
                endTime,
                triggerTime);
       
        return ObservationHelper.createBaseObservation()
                .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a73")).withAudioMetadata(audio)
                .withDigitalMedia(ObservationHelper.createCompleteDigitalMedia());
    }
    
    /**
     * This will create a complete observation for video.  The {@link VideoMetadata} and {@link DigitalMedia} object
     * will have all fields filled out as much as possible.  This can be used to compare with something pull from the 
     * database to prove that it is actually persisted.  Fields will always contain the same info so this method if 
     * called multiple times will create a different object, but will be equal to the previous one.
     * @return 
     *      Observation of video data
     */ 
    public static Observation createVideoObservation()
    {
        PixelResolution resolution = new PixelResolution(100, 100);
        
        List<VideoSampleOfInterest> samplesOfInterest = new ArrayList<VideoSampleOfInterest>();
        samplesOfInterest.add(new VideoSampleOfInterest(new PixelCircle(new PixelPosition(1, 2), 20), null,
                new Direction(DirectionEnum.FN, "test"), 10L, 100L));
        samplesOfInterest.add(new VideoSampleOfInterest(null,
                new PixelRectangle(new PixelPosition(3, 5), new PixelPosition(8, 9)), 
                new Direction(DirectionEnum.LR_NF, "b"), 5L, 50L));
        
        Camera camera = new Camera(7, "other-camera", CameraTypeEnum.IR);
        
        Double framesPerSecond = 100.00;
        Double zoom = 100.00;
        Boolean color = false;
        Long startTime = 10L;
        Long endTime = 300L;
        
        VideoMetadata video = new VideoMetadata(resolution,
                samplesOfInterest,
                camera,
                framesPerSecond,
                startTime,
                endTime,
                zoom,
                color);
        
        return ObservationHelper.createBaseObservation()
                .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a74")).withVideoMetadata(video)
                .withDigitalMedia(ObservationHelper.createCompleteDigitalMedia());
    }
 
    /**
     * Create a Chemical Observation.
     */
    public static Observation createChemicalObservation()
    {
        List<ChemicalEntry> entries = new ArrayList<>();
        ChemicalCategory chemCat1 = new ChemicalCategory(ChemicalCategoryEnum.PENETRATING_AGENT, "penetrating agent");
        ChemicalAgent chmAgent1 = new ChemicalAgent(ChemicalAgentEnum.NITROGEN_MUSTARD, "mustard gas");
        double scaledSignalLevel = .125;
        AlarmState alarm = new AlarmState(true, .5);
        ChemicalEntry entry1 = new ChemicalEntry(chemCat1, chmAgent1, null, null, scaledSignalLevel, alarm);
        entries.add(entry1);
        
        ChemicalCategory chemCat2 = new ChemicalCategory(ChemicalCategoryEnum.CHOKING_AGENT, "Blech");
        ChemicalAgent chmAgent2 = new ChemicalAgent(ChemicalAgentEnum.SULFUR_DIOXIDE, "rotten eggs");
        CountsPerTime counts2 = new CountsPerTime().withCount(25.5);
        AlarmState alarm2 = new AlarmState(false, .10);
        ChemicalEntry entry2 = new ChemicalEntry(chemCat2, chmAgent2, null, counts2, null, 
                alarm2);
        entries.add(entry2);
        
        ChemicalCategory chemCat3 = new ChemicalCategory(ChemicalCategoryEnum.INCAPACITATING_AGENT, "busy");
        ChemicalAgent chmAgent3 = new ChemicalAgent(ChemicalAgentEnum.TABUN, "something");
        ConcentrationGramsPerLiter concentration = new ConcentrationGramsPerLiter().withValue(25.5);
        AlarmState alarm3 = new AlarmState(false, .60);
        ChemicalEntry entry3 = new ChemicalEntry(chemCat3, chmAgent3, concentration, null, null, 
                alarm3);
        entries.add(entry3);
        
        return ObservationHelper.createBaseObservation()
                .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a76"))
                .withChemical(new Chemical(entries));
    }
    
    /**
     * Create a Water Quality Observation.
     */
    public static Observation createWaterQualityObservation()
    {
        PH ph = new PH().withValue(7.5);
        ConcentrationGramsPerLiter dissolvedOxygen = new ConcentrationGramsPerLiter().withValue(12.1);
        ConductivitySiemensPerMeter electricalConductivity = new ConductivitySiemensPerMeter().withValue(11.1);
        VoltageVolts oxydationReductionPotential = new VoltageVolts().withValue(9.2);
        TurbidityNtu turbidity = new TurbidityNtu().withValue(123.4);
        TemperatureCelsius temperature = new TemperatureCelsius().withValue(33.3);
        ConcentrationGramsPerLiter chlorine = new ConcentrationGramsPerLiter().withValue(123.0);
        ConcentrationGramsPerLiter totalDisolvedSolid = new ConcentrationGramsPerLiter().withValue(3.0);
        SalinityPsu salinity = new SalinityPsu().withValue(88.0);
        SpecificGravity specificGravity = new SpecificGravity().withValue(77.7);
        PressureMillibars waterFlowPressure = new PressureMillibars().withValue(90.0);
        LangelierSaturationIndex langelierSaturationIndex = new LangelierSaturationIndex().withValue(12.33);
        WaterQuality waterQuality = new WaterQuality(ph, dissolvedOxygen, electricalConductivity, 
                oxydationReductionPotential, turbidity, temperature, chlorine, totalDisolvedSolid, salinity, 
                specificGravity, waterFlowPressure, langelierSaturationIndex);
        
        return ObservationHelper.createBaseObservation()
                .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a77"))
                .withWaterQuality(waterQuality);
    }
    
    /**
     * Create a CBRNE trigger observation.
     */
    public static Observation createCbrneTriggerObservation()
    {
        List<CbrneTriggerEntry> entries = new ArrayList<>();
        CbrneModality modality = new CbrneModality(CbrneModalityEnum.SCATTERING, "goof");
        CountsPerTime counts = new CountsPerTime(312.1, 150.9);
        AlarmState alarm = new AlarmState(false, .90);
        CbrneTriggerEntry entry = new CbrneTriggerEntry(modality, counts, null, alarm);
        entries.add(entry);
        
        CbrneModality modality2 = new CbrneModality(CbrneModalityEnum.FLUORESCENCE, "glow");
        double scaledSignalLevel = .876;
        AlarmState alarm2 = new AlarmState(false, .70);
        CbrneTriggerEntry entry2 = new CbrneTriggerEntry(modality2, null, scaledSignalLevel, alarm2);
        entries.add(entry2);
        
        return ObservationHelper.createBaseObservation()
                .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a78"))
                .withCbrneTrigger(new CbrneTrigger(entries));
    }
    
    /**
     * Create a Biological observation.
     */
    public static Observation createBiologicalObservation()
    {
        List<BiologicalEntry> entries = new ArrayList<>();
        
        BiologicalCategory category = new BiologicalCategory(BiologicalCategoryEnum.BACTERIAL, "ecoli");
        BiologicalAgent agent = new BiologicalAgent(BiologicalAgentEnum.CRIMEAN_CONGO_HEMORRHAGIC_FEVER_VIRUS, "oh");
        ConcentrationGramsPerLiter concentration = new ConcentrationGramsPerLiter().withValue(900.2);
        AlarmState alarm = new AlarmState(true, .787);
        BiologicalAssayResultEnum assayResult = BiologicalAssayResultEnum.POSITIVE;
        BiologicalEntry entry = new BiologicalEntry(category, agent, concentration, null, null, 
                null, null, alarm, assayResult);
        entries.add(entry);
        
        BiologicalCategory category2 = new BiologicalCategory(BiologicalCategoryEnum.CHLAMYDIA, "not good");
        BiologicalAgent agent2 = new BiologicalAgent(BiologicalAgentEnum.CHLAMYDIA_PSITTACI, "eww");
        ColonyFormingUnits cfus = new ColonyFormingUnits().withValue(88.1);
        AlarmState alarm2 = new AlarmState(true, .66);
        BiologicalEntry entry2 = new BiologicalEntry(category2, agent2, null, cfus, null, 
                null, null, alarm2, assayResult);
        entries.add(entry2);
        
        BiologicalCategory category3 = new BiologicalCategory(BiologicalCategoryEnum.SIMULANT, "like coffee?");
        BiologicalAgent agent3 = new BiologicalAgent(BiologicalAgentEnum.TETRADOTOXIN, "nice");
        PlaqueFormingUnits pfus = new PlaqueFormingUnits().withValue(78.6);
        AlarmState alarm3 = new AlarmState(true, .35);
        BiologicalEntry entry3 = new BiologicalEntry(category3, agent3, null, null, pfus, 
                null, null, alarm3, assayResult);
        entries.add(entry3);
        
        BiologicalCategory category4 = new BiologicalCategory(BiologicalCategoryEnum.VIRAL, "like a cold");
        BiologicalAgent agent4 = new BiologicalAgent(BiologicalAgentEnum.RIFT_VALLEY_FEVER_VIRUS, "yuck");
        CountsPerTime counts = new CountsPerTime(1.0, 10.5);
        AlarmState alarm4 = new AlarmState(true, .01);
        BiologicalEntry entry4 = new BiologicalEntry(category4, agent4, null, null, null, 
                counts, null, alarm4, assayResult);
        entries.add(entry4);
        
        BiologicalCategory category5 = new BiologicalCategory(BiologicalCategoryEnum.RICKETTSIAE, "hmm");
        BiologicalAgent agent5 = new BiologicalAgent(BiologicalAgentEnum.BACILLUS_GLOBIGII, "one");
        double scaledSignalLevel = .99;
        AlarmState alarm5 = new AlarmState(false, .04);
        BiologicalAssayResultEnum assayResult5 = BiologicalAssayResultEnum.NOT_APPLICABLE;
        BiologicalEntry entry5 = new BiologicalEntry(category5, agent5, null, null, null, 
                null, scaledSignalLevel, alarm5, assayResult5);
        entries.add(entry5);
        
        return ObservationHelper.createBaseObservation()
                .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a79"))
                .withBiological(new Biological(entries));
    }
    
    /**
     * This Observation is used to verify the system mode and version fields correctly persist.
     * @return 
     *      basic Observation data
     */ 
    public static Observation createBasicObservation()
    {
        return ObservationHelper.createBaseObservation().withSystemInTestMode(true)
                .withUuid(UUID.fromString("328da040-85bb-11e2-9e96-0800200c9a75"));
    }
    
    /**
     * Create a static coordinate object that can be reused in different observations.
     */
    private static Coordinates createCoordinateObject()
    {
        LongitudeWgsDegrees lng =
                new LongitudeWgsDegrees(70.0, 1.0, 1.0, 1.0, 0.0);
        LatitudeWgsDegrees lat =
                new LatitudeWgsDegrees(50.0, 1.0, 1.0, 1.0, 0.0 );
        HaeMeters hae =
                new HaeMeters(60.0, 1.0, 1.0, 1.0, 0.0);
        Ellipse ellipse = new Ellipse(
                new DistanceMeters(100.0, 1.0, 1.0, 1.0, 1.0),
                new DistanceMeters(50.0, 1.0, 1.0, 1.0, 1.0),
                new HeadingDegrees(90.0, 5.0, 1.5, 0.8, 0.8));
        Coordinates coords = new Coordinates(lng, lat, hae, ellipse);
        return coords;
    }
    
    /**
     * This will create an observation that has all fields filed out except for the complex types in the main xs:choice,
     * like status, detection, digital media, those that denote the type of observation.  Fields will always contain the
     * same info so this method if called multiple times will create a different object, but will be equal to the 
     * previous one.  UUID field must be filled out still. 
     * @return 
     *      Base observation object 
     */
    private static Observation createBaseObservation()
    {
        ArrayList<ObservationRef> relatedObservations = new ArrayList<ObservationRef>();
        relatedObservations.add(new ObservationRef(new Relationship(RelationshipTypeEnum.CHILD, "something"), 
                UUID.fromString("271d30b0-85b7-11e2-9e96-0800200c9a68")));
        relatedObservations.add(new ObservationRef(new Relationship(RelationshipTypeEnum.PARENT, "something-else"), 
                UUID.fromString("3fd8fa30-85b7-11e2-9e96-0800200c9a68")));
        
        // all complex types in xs:choice are not included
        Coordinates assetLocation = new Coordinates().withLatitude(
                new LatitudeWgsDegrees(20.0, 1.0, 1.0, 1.0, 0.0))
                .withLongitude(new LongitudeWgsDegrees(10.0, 1.0, 1.0, 1.0, 0.0))
                .withAltitude(new HaeMeters(60.0, 1.0, 1.0, 1.0, 0.0))
                .withEllipseRegion(new Ellipse(new DistanceMeters(100.0, 1.0, 1.0, 1.0, 1.0),
                        new DistanceMeters(50.0, 1.0, 1.0, 1.0, 1.0),
                        new HeadingDegrees(90.0, 5.0, 1.5, 0.8, 0.8)));
        Orientation assetOrientation = new Orientation().withHeading(new HeadingDegrees(40.0, 1.0, 2.0, 1.0, 0.0))
                .withElevation(new ElevationDegrees(75.0, 1.0, 1.0, 1.0, 0.0))
                .withBank(new BankDegrees(170.0, 1.0, 1.0, 1.0, 0.0));
        Orientation platformOrientation = new Orientation().withHeading(
                new HeadingDegrees(20.0, 1.0, 1.0, 1.0, 0.0))
                .withElevation(new ElevationDegrees(64.0, 1.0, 1.0, 1.0, 0.0))
                .withBank(new BankDegrees(88.0, 1.0, 1.0, 1.0, 0.0));
        Coordinates pointingLocation = new Coordinates().withLatitude(
                new LatitudeWgsDegrees(30.0, 1.0, 1.0, 1.0, 0.0))
                .withLongitude(new LongitudeWgsDegrees(30.0, 1.0, 1.0, 1.0, 0.0))
                .withAltitude(new HaeMeters(45.0, 1.0, 1.0, 1.0, 0.0))
                .withEllipseRegion(new Ellipse(new DistanceMeters(88.0, 1.0, 1.0, 1.0, 0.0),
                        new DistanceMeters(77.0, 1.0, 1.0, 1.0, 0.0),
                        new HeadingDegrees(65.0, 5.0, 1.5, 0.8, 0.8)));
        
        Detection detection = null;
        Status status = null;
        Weather weather = null;
        DigitalMedia digitalMedia = null;
        AudioMetadata audioMetadata = null;
        ImageMetadata imageMetadata = null;
        VideoMetadata videoMetadata = null;
        ChannelMetadata channelMetadata = null;
        WaterQuality waterQuality = null;
        CbrneTrigger cbrneTrigger = null;
        Biological biological = null;
        Chemical chemical = null;
        Power power = null;
        
        List<MapEntry> reserved = new ArrayList<MapEntry>();
        reserved.add(new MapEntry("String", "Bob"));
        reserved.add(new MapEntry("Double", 2.25));
        reserved.add(new MapEntry("Integer", 100));
        reserved.add(new MapEntry("Boolean", true));
        reserved.add(new MapEntry("Long", 12345L));
        UUID uuid = null; // must be set by individual create methods
        Long observedTimestamp = null; // set by assets if applicable
        Long createdTimestamp = 100L;
        UUID assetUuid = UUID.fromString("328da040-85bb-11e2-9e96-8888200c9a75");
        String assetName = "asset-name";
        String assetType = "asset-type";
        String sensorId = "sensor-id";
        boolean systemInTestMode = false;
        Version version = new Version(1,2);
        int systemId = 0123;
        
        List<SensingModality> sensings = new ArrayList<>();
        sensings.add(new SensingModality(SensingModalityEnum.BIOLOGICAL, "bio"));

        return new Observation(version, 
                relatedObservations, 
                assetLocation, 
                assetOrientation,
                platformOrientation,
                pointingLocation, 
                sensings,
                detection, 
                status, 
                weather, 
                waterQuality,
                cbrneTrigger,
                biological,
                chemical,
                power,
                digitalMedia, 
                audioMetadata, 
                imageMetadata, 
                videoMetadata, 
                channelMetadata,
                reserved,
                uuid, 
                observedTimestamp, 
                createdTimestamp, 
                assetUuid, 
                assetName, 
                assetType, 
                sensorId, 
                systemInTestMode, 
                systemId);
    }
}
