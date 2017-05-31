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
package example.asset.lexicon;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.AudioMetadata;
import mil.dod.th.core.observation.types.Biological;
import mil.dod.th.core.observation.types.BiologicalEntry;
import mil.dod.th.core.observation.types.CbrneTrigger;
import mil.dod.th.core.observation.types.CbrneTriggerEntry;
import mil.dod.th.core.observation.types.Chemical;
import mil.dod.th.core.observation.types.ChemicalEntry;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.ImageMetadata;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.ObservationRef;
import mil.dod.th.core.observation.types.Power;
import mil.dod.th.core.observation.types.Precipitation;
import mil.dod.th.core.observation.types.Relationship;
import mil.dod.th.core.observation.types.RoadCondition;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.observation.types.TargetClassification;
import mil.dod.th.core.observation.types.VideoMetadata;
import mil.dod.th.core.observation.types.WaterQuality;
import mil.dod.th.core.observation.types.Weather;
import mil.dod.th.core.observation.types.WeatherCondition;
import mil.dod.th.core.observation.types.WindMeasurement;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.AlarmState;
import mil.dod.th.core.types.Amperes;
import mil.dod.th.core.types.ConcentrationGramsPerLiter;
import mil.dod.th.core.types.ConfidenceFactor;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.FrequencyKhz;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.PH;
import mil.dod.th.core.types.PowerWatts;
import mil.dod.th.core.types.PressureMillibars;
import mil.dod.th.core.types.ScoringCriteria;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.SpeedMetersPerSecond;
import mil.dod.th.core.types.TemperatureCelsius;
import mil.dod.th.core.types.VAR;
import mil.dod.th.core.types.VoltageVolts;
import mil.dod.th.core.types.audio.AudioRecorder;
import mil.dod.th.core.types.audio.AudioRecorderEnum;
import mil.dod.th.core.types.biological.BiologicalAgent;
import mil.dod.th.core.types.biological.BiologicalAgentEnum;
import mil.dod.th.core.types.biological.BiologicalAssayResultEnum;
import mil.dod.th.core.types.cbrnetrigger.CbrneModality;
import mil.dod.th.core.types.cbrnetrigger.CbrneModalityEnum;
import mil.dod.th.core.types.chemical.ChemicalAgent;
import mil.dod.th.core.types.chemical.ChemicalAgentEnum;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.detection.TargetClassificationType;
import mil.dod.th.core.types.detection.TargetClassificationTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.ImageCaptureReason;
import mil.dod.th.core.types.image.ImageCaptureReasonEnum;
import mil.dod.th.core.types.image.PictureTypeEnum;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.core.types.observation.RelationshipTypeEnum;
import mil.dod.th.core.types.power.PowerClassification;
import mil.dod.th.core.types.power.PowerClassificationEnum;
import mil.dod.th.core.types.power.PowerSource;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.DistanceMeters;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.HeadingDegrees;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.spatial.TrackElement;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.types.weather.RoadConditionType;
import mil.dod.th.core.types.weather.RoadConditionTypeEnum;
import mil.dod.th.core.types.weather.WeatherIntensityEnum;
import mil.dod.th.core.types.weather.WeatherPhenomena;
import mil.dod.th.core.types.weather.WeatherPhenomenaEnum;
import mil.dod.th.core.types.weather.WeatherQualifierEnum;
import mil.dod.th.core.validator.ValidationFailedException;

import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;


/**
 * Example observation asset for creating observations when the activate method is called.
 * 
 * @author matt
 */
@Component(factory = Asset.FACTORY)
public class ExampleObservationsAsset implements AssetProxy
{
    private LoggingService m_Logging;
    private AssetContext m_Context;
    private WakeLock m_WakeLock;
 
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props)
    {
        m_Context = context;
        checkWakeLock();
    }
    
    @Deactivate
    public void deactivateInstance()
    {
        if (m_WakeLock != null)
        {
            m_WakeLock.delete();
            m_WakeLock = null;
        }
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        //empty
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        try
        {
            tryActivateWakeLock();
            Logging.log(LogService.LOG_INFO, "Example observations asset activated");
            m_Context.setStatus(SummaryStatusEnum.GOOD, "Asset Activated");

            try 
            {
                createTestObservations();
            } 
            catch (ValidationFailedException e)
            {
                throw new AssetException(e);
            }
        }
        finally
        {
            tryDeactivateWakeLock();
        }
    }
    
    /**
     * Create observations to demonstrate all the different types of detections an asset can make. 
     */
    private void createTestObservations() throws ValidationFailedException
    {
        try
        {
            tryActivateWakeLock();

            final Observation statusObs = new Observation().withStatus(new Status().withSummaryStatus(
                    new OperatingStatus(SummaryStatusEnum.GOOD, "Asset Activated"))).withSensorId("status");

            m_Context.persistObservation(statusObs);

            //IMAGE
            DigitalMedia digitalMedia = new DigitalMedia();
            digitalMedia.withEncoding("image/jpeg");
            digitalMedia.setValue(new byte[100]);

            final ImageMetadata imageData = new ImageMetadata().
                    withCaptureTime(System.currentTimeMillis()).
                    withColor(true).
                    withResolution(new PixelResolution()).
                    withZoom(1).
                    withPictureType(PictureTypeEnum.FULL_FIELD_OF_VIEW).
                    withImageCaptureReason(new ImageCaptureReason().
                            withValue(ImageCaptureReasonEnum.MANUAL));

            imageData.setImager(new Camera().withType(CameraTypeEnum.IR));


            final Observation testObs = new Observation().withSensorId("image").withDigitalMedia(digitalMedia).
                    withImageMetadata(imageData);

            m_Context.persistObservation(testObs);

            //IMAGE WITH NO RESOLUTION
            final ImageMetadata imgData = new ImageMetadata().
                    withCaptureTime(System.currentTimeMillis()).
                    withColor(true).
                    withZoom(1).
                    withPictureType(PictureTypeEnum.FULL_FIELD_OF_VIEW).
                    withImageCaptureReason(new ImageCaptureReason().
                            withValue(ImageCaptureReasonEnum.MANUAL)).
                            withImager(new Camera().withType(CameraTypeEnum.IR));
            DigitalMedia digitalMediaNoRes = new DigitalMedia().withEncoding("image/jpeg").withValue(new byte[100]);
            final Observation testObsNoRes = new Observation().withSensorId("imageNoRes").
                    withDigitalMedia(digitalMediaNoRes).withImageMetadata(imgData);
            m_Context.persistObservation(testObsNoRes);

            //AUDIO
            DigitalMedia digitalMedia2 = new DigitalMedia();
            digitalMedia2.withEncoding("audio/mp3");
            digitalMedia2.setValue(new byte[100]);

            final AudioMetadata audio = new AudioMetadata().withSampleRateKHz(1200.00).
                    withRecorderType(new AudioRecorder().withValue(AudioRecorderEnum.MICROPHONE).
                            withDescription("A description.")).withStartTime(System.currentTimeMillis()).
                            withEndTime(System.currentTimeMillis());

            final Observation testAudio = new Observation().withSensorId("audio").withDigitalMedia(digitalMedia2).
                    withAudioMetadata(audio);

            m_Context.persistObservation(testAudio);

            //VIDEO
            DigitalMedia digitalMedia3 = new DigitalMedia();
            digitalMedia3.withEncoding("video/wav");
            digitalMedia3.setValue(new byte[100]);

            final VideoMetadata video = new VideoMetadata().
                    withRecorder(new Camera().withType(CameraTypeEnum.IR));

            final Observation testVideo = new Observation().withSensorId("video").withDigitalMedia(digitalMedia3).
                    withVideoMetadata(video);

            m_Context.persistObservation(testVideo);

            //DETECTION WITH TARGET CLASSIFICATIONS AND SENSINGS
            int classSize = TargetClassificationTypeEnum.values().length;
            int modalitySize = SensingModalityEnum.values().length;
            for (int i=0; i < Math.max(classSize, modalitySize); i++)
            {
                int classIdx = Math.min(i, classSize - 1);
                TargetClassificationTypeEnum classification = TargetClassificationTypeEnum.values()[classIdx];
                int modalityIdx = Math.min(i, modalitySize - 1);
                SensingModalityEnum modality = SensingModalityEnum.values()[modalityIdx];

                String sensorId = String.format("d-c=%d;m=%d", classIdx, modalityIdx);
                Observation classificationSensingObs = 
                        createObservationsWithTargetAndSensings(sensorId, classification, modality);

                // Offset the observed time so it is easily distinguishable from the created time, in particular when
                // viewing from the web GUI
                classificationSensingObs.setObservedTimestamp(
                        System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(1));
                m_Context.persistObservation(classificationSensingObs);
            }

            //BIOLOGICAL WITH SENSING
            Biological biological = new Biological();
            biological.getEntries().add(new BiologicalEntry()
                .withAgent(new BiologicalAgent().withType(BiologicalAgentEnum.BOTULINUM_TOXINS))
                .withAlarm(new AlarmState(true, 1.0))
                .withAssayResult(BiologicalAssayResultEnum.POSITIVE));

            SensingModality modality = new SensingModality(SensingModalityEnum.BIOLOGICAL, "bio sensing");
            Observation biologicalObs = new Observation().withSensorId("biological").withBiological(biological)
                    .withModalities(modality);
            m_Context.persistObservation(biologicalObs);

            //CHEMICAL
            Chemical chemical = new Chemical();
            chemical.getEntries().add(new ChemicalEntry()
                .withAgent(new ChemicalAgent().withType(ChemicalAgentEnum.ARSIN))
                .withConcentration(new ConcentrationGramsPerLiter().withValue(38.2))
                .withAlarm(new AlarmState(false, 0.5)));

            Observation chemicalObs = new Observation().withSensorId("chemical").withChemical(chemical);
            m_Context.persistObservation(chemicalObs);

            //CBRNE TRIGGER
            CbrneTrigger cbrneTrigger = new CbrneTrigger();
            cbrneTrigger.getEntries().add(new CbrneTriggerEntry()
                .withModality(new CbrneModality().withType(CbrneModalityEnum.SCATTERING))
                .withAlarm(new AlarmState(true, 0.8)));

            Observation cbrneTriggerObs = new Observation().withSensorId(
                    "cbrne-trigger").withCbrneTrigger(cbrneTrigger);
            m_Context.persistObservation(cbrneTriggerObs);

            //WATER QUALITY WITH SENSING
            WaterQuality waterQuality = new WaterQuality();
            waterQuality.setPH(new PH().withValue(7.0));

            modality = new SensingModality(SensingModalityEnum.WATER_QUALITY, "water sensing");
            Observation waterQualityObs = new Observation().withSensorId("water-quality").withWaterQuality(waterQuality)
                    .withModalities(modality);
            m_Context.persistObservation(waterQualityObs);

            //WEATHER
            TemperatureCelsius temperature = new TemperatureCelsius().withValue(35);
            PressureMillibars pressure = new PressureMillibars().withValue(1020);
            DistanceMeters pressureAlt = new DistanceMeters().withValue(300);
            DistanceMeters visibility = new DistanceMeters().withValue(10);
            SpeedMetersPerSecond speedAvg = new SpeedMetersPerSecond().withValue(1);
            WindMeasurement windAvg = new WindMeasurement().withSpeed(speedAvg);
            Precipitation precipitation = new Precipitation().withAccumulation(5.5);
            TemperatureCelsius freezingPoint = new TemperatureCelsius().withValue(0);
            TemperatureCelsius surfaceTemp = new TemperatureCelsius().withValue(30);
            TemperatureCelsius subSurfaceTemp = new TemperatureCelsius().withValue(30);
            RoadConditionType roadCondType = new RoadConditionType().withValue(RoadConditionTypeEnum.WET);
            WeatherPhenomena phenomena = new WeatherPhenomena().withValue(WeatherPhenomenaEnum.FUNNEL_CLOUD);
            RoadCondition roadCondition = new RoadCondition()
                .withChemicalSaturation(1)
                .withFreezingPoint(freezingPoint)
                .withSurfaceTemperature(surfaceTemp)
                .withSubSurfaceTemperature(subSurfaceTemp)
                .withType(roadCondType);
            WeatherCondition weatherCondition = new WeatherCondition()
                .withIntensity(WeatherIntensityEnum.HEAVY)
                .withPhenomena(phenomena)
                .withQualifier(WeatherQualifierEnum.THUNDERSTORM);

            Weather weather = new Weather()
                .withTemperature(temperature)
                .withPressure(pressure)
                .withPressureAltitude(pressureAlt)
                .withVisibility(visibility)
                .withWindAvg(windAvg)
                .withPrecipitation(precipitation)
                .withRoadCondition(roadCondition)
                .withWeatherConditions(weatherCondition);

            final Observation weatherObs = new Observation().withSensorId("weather").withWeather(weather);
            m_Context.persistObservation(weatherObs);

            //POWER
            final Power power = new Power()
                .withClassification(new PowerClassification().withValue(PowerClassificationEnum.INDUCTIVE_ON))
                .withActivePower(new PowerWatts().withValue(39))
                .withReactivePower(new VAR().withValue(-52))
                .withDuration(0.3)
                .withVoltage(new VoltageVolts().withValue(75))
                .withCurrent(new Amperes().withValue(21))
                .withLoadActivePower(new PowerWatts().withValue(47))
                .withLoadCurrent(new Amperes().withValue(19))
                .withLoadReactivePower(new VAR().withValue(-28))
                .withSource(new PowerSource(
                    new ConfidenceFactor().withValue(0.50).withScore(
                            new ScoringCriteria(
                                    new SensingModality().withValue(SensingModalityEnum.POWER), 0.68),
                                    new ScoringCriteria(
                                            new SensingModality().withValue(SensingModalityEnum.UNKNOWN), 0.3)),
                                            "Appliance A did X"),
                                            new PowerSource(new ConfidenceFactor().withValue(0.35), 
                                                    "Appliance A did Y"));

            modality = new SensingModality(SensingModalityEnum.POWER, "power sensing");
            Observation powerObs = new Observation().withSensorId("power").withPower(power)
                    .withModalities(modality);
            m_Context.persistObservation(powerObs);

            //GENERIC
            final Observation genericObs = new Observation().withSensorId("generic")
                    .withAssetLocation(SpatialTypesFactory.newCoordinates(45.5, 60.4, 1000))
                    .withAssetOrientation(SpatialTypesFactory.newOrientation(90.0, 45.0, 15.0))
                    .withReserved(new MapEntry("resKey", "resValue"));
            m_Context.persistObservation(genericObs);

            //MERGE Observation
            genericObs.getReserved().add(new MapEntry("mergeKey", "mergeValue"));
            m_Context.mergeObservation(genericObs);
        }
        finally
        {
            tryDeactivateWakeLock();
        }
    }

    @Override
    public Observation onCaptureData() throws AssetException
    {
        try
        {
            tryActivateWakeLock();
            Logging.log(LogService.LOG_INFO, "Example Observations Asset Data captured");

            final AssetCapabilities capabilities = m_Context.getFactory().getAssetCapabilities();

            final LatitudeWgsDegrees lat = new LatitudeWgsDegrees().withValue(50.0);
            final LongitudeWgsDegrees lng = new LongitudeWgsDegrees().withValue(70.0);

            final Observation obs = new Observation();

            // Offset the observed time so it is easily distinguishable from the created time, in particular when 
            // viewing from the web GUI
            obs.setObservedTimestamp(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(1));
            obs.withModalities(new SensingModality().withValue(capabilities.getModalities().get(1).getValue()));

            final Detection detection = new Detection();
            detection.setType(DetectionTypeEnum.ALARM);
            final HeadingDegrees heading = new HeadingDegrees().withValue(50);
            final ElevationDegrees elevation = new ElevationDegrees().withValue(90);

            final Coordinates targetCoords = new Coordinates().withLatitude(lat).withLongitude(lng);
            detection.withTargetLocation(targetCoords);

            final Coordinates coords = new Coordinates().withLatitude(lat).withLongitude(lng);
            detection.withTrackHistories(new TrackElement(coords, new SpeedMetersPerSecond(205.5, null, null, null, 
                    null), new Orientation(heading, elevation, null), null, 2003L));

            detection.withTargetOrientation(new Orientation(heading, elevation, null));
            detection.setTargetFrequency(new FrequencyKhz().withValue(30));
            detection.setTargetId("example-target-id");

            obs.setDetection(detection);

            final Coordinates obsCoords = new Coordinates().withLatitude(lat).withLongitude(lng);
            obs.setAssetLocation(obsCoords);

            final ObservationRef obsRef = new ObservationRef();
            obsRef.setUuid(UUID.randomUUID());
            obsRef.setRelationship(new Relationship(RelationshipTypeEnum.CHILD, "test"));
            obs.getRelatedObservations().add(obsRef);

            return obs;
        }
        finally
        {
            tryDeactivateWakeLock();
        }
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new AssetException(
            new UnsupportedOperationException("ExampleObservationsAsset does not support capturing data by sensorId."));
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        try
        {
            tryActivateWakeLock();
            m_Logging.info("Example observations asset deactivated");
            m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
        }
        finally
        {
            tryDeactivateWakeLock();
        }
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        try
        {
            tryActivateWakeLock();
            Logging.log(LogService.LOG_INFO, "Performing BIT");
            return new Status().withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "BIT Passed"));
        }
        finally
        {
            tryDeactivateWakeLock();
        }
    }
    
    @Override 
    public Response onExecuteCommand(final Command capabilityCommand) throws CommandExecutionException
    {
        throw new CommandExecutionException("Could not execute specified command.");
    }
    
    private Observation createObservationsWithTargetAndSensings(String sensorId, 
            TargetClassificationTypeEnum classification, SensingModalityEnum modality)
    {
        final LatitudeWgsDegrees lat = new LatitudeWgsDegrees().withValue(50.0);
        final LongitudeWgsDegrees lng = new LongitudeWgsDegrees().withValue(70.0);
        
        final Observation obs = new Observation();
        obs.withModalities(new SensingModality().withValue(modality));
        
        final Detection detection = new Detection();
        detection.withType(DetectionTypeEnum.TEST);
        detection.withTargetClassifications(new TargetClassification()
            .withType(new TargetClassificationType().withValue(classification)));
        
        final HeadingDegrees heading = new HeadingDegrees().withValue(50);
        final ElevationDegrees elevation = new ElevationDegrees().withValue(90);
        
        final Coordinates targetCoords = new Coordinates().withLatitude(lat).withLongitude(lng);
        detection.withTargetLocation(targetCoords);

        final Coordinates coords = new Coordinates().withLatitude(lat).withLongitude(lng);
        detection.withTrackHistories(new TrackElement(coords, new SpeedMetersPerSecond(205.5, null, null, null, 
                null), new Orientation(heading, elevation, null), null, 2003L));
        
        detection.withTargetOrientation(new Orientation(heading, elevation, null));
        detection.setTargetFrequency(new FrequencyKhz().withValue(30));
        detection.setTargetId("example-target-id");
        
        obs.setDetection(detection);
        
        final Coordinates obsCoords = new Coordinates().withLatitude(lat).withLongitude(lng);
        obs.setAssetLocation(obsCoords);
        
        obs.withSensorId(sensorId);
        obs.setSystemInTestMode(false);
        
        return obs;
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
    
    private void checkWakeLock()
    {
        if (m_WakeLock == null)
        {
            m_WakeLock = m_Context.createPowerManagerWakeLock(this.getClass().getSimpleName() + "WakeLock");
        }
    }
    
    private void tryActivateWakeLock()
    {
        if (m_WakeLock != null)
        {
            m_WakeLock.activate();
        }
    }
    
    private void tryDeactivateWakeLock()
    {
        if (m_WakeLock != null)
        {
            m_WakeLock.cancel();
        }
    }
}
