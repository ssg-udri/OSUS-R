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
package mil.dod.th.ose.core.integration;


import java.util.UUID;

import junit.framework.TestCase;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.AudioMetadata;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.ImageMetadata;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.FrequencyKhz;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.types.audio.AudioRecorder;
import mil.dod.th.core.types.audio.AudioRecorderEnum;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.ImageCaptureReason;
import mil.dod.th.core.types.image.ImageCaptureReasonEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.integration.commons.EventHandlerSyncer;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;

/**
 * @author Pinar French
 *
 */
public class TestObservation extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private ObservationStore m_ObservationStore;
    private Observation m_PersistedObservation;
    private Coordinates m_AssetLocation;
    private Orientation m_AssetOrientation;
    private Coordinates m_PointingLocation;
    private Orientation m_PlatformOrientation;
    private DigitalMedia m_DigitalMedia1;
    private DigitalMedia m_DigitalMedia2;
    
    @Override
    public void setUp()
    {
        m_ObservationStore = ServiceUtils.getService(m_Context, ObservationStore.class);

        m_AssetLocation = SpatialTypesFactory.newCoordinates(54, 74);
        m_AssetOrientation = SpatialTypesFactory.newOrientation(180.0, -80.0, 45.0);
        m_PointingLocation = SpatialTypesFactory.newCoordinates(70, 50);
        m_PlatformOrientation = SpatialTypesFactory.newOrientation(50, 90, 0);
        m_DigitalMedia1 = new DigitalMedia(new byte[] {0, 1, 2}, "image/jpeg");
        m_DigitalMedia2 = new DigitalMedia(new byte[] {3, 4, 5}, "image/jpeg");
    }
    
    @Override
    public void tearDown()
    {
        if (m_PersistedObservation != null)
        {
            m_ObservationStore.remove(m_PersistedObservation);
        }
    }
    
    /**
     * Make sure that created observations can be validated
     */
    public void testValidateObservation() throws IllegalArgumentException, PersistenceFailedException
    {
        ObservationStore observationStore = 
                ServiceUtils.getService(m_Context, ObservationStore.class);
        
        Observation observation = new Observation();
        
        try
        {
            observationStore.persist(observation);
            fail("Validation failed exception expected.");
        }
        catch (ValidationFailedException e)
        {
            Logging.log(LogService.LOG_INFO, e, "Test Chained Exception Message");
        }
    }

    /**
     * Make sure that observations with duplicate sub-type data cannot be persisted.
     */
    public void testObsDupDetection() throws PersistenceFailedException, ValidationFailedException
    {
        Detection detection = new Detection().withType(DetectionTypeEnum.TEST)
            .withTargetLocation(SpatialTypesFactory.newCoordinates(5, 44))
            .withTargetOrientation(SpatialTypesFactory.newOrientation(180.0, -80.0, 45.0))
            .withTargetFrequency(new FrequencyKhz().withValue(30))
            .withTargetId("example-target-id");

        // Persist the first observation
        m_PersistedObservation = createBaseObservation();
        m_PersistedObservation.setDetection(detection);
        m_ObservationStore.persist(m_PersistedObservation);

        // Persist second observation containing some duplicate data
        Observation observation = createBaseObservation();
        observation.setDetection(detection);

        try
        {
            m_ObservationStore.persist(observation);
            fail("Persistence failed exception expected.");
        }
        catch (PersistenceFailedException e)
        {
            // Expected
        }
    }

    /**
     * Make sure that observations with duplicate media data can be persisted and deleted.
     */
    public void testObsDupDigitalMedia() throws PersistenceFailedException, ValidationFailedException
    {
        ImageMetadata imgMetaData1 = new ImageMetadata()
                .withCaptureTime(50L)
                .withMaskSamplesOfInterest(m_DigitalMedia1, m_DigitalMedia2)
                .withImager(new Camera(1, "Camera1", CameraTypeEnum.VISIBLE))
                .withImageCaptureReason(new ImageCaptureReason(ImageCaptureReasonEnum.OTHER, "reason1"));

        // Persist the first observation
        m_PersistedObservation = createBaseObservation();
        m_PersistedObservation.setDigitalMedia(m_DigitalMedia1);
        m_PersistedObservation.setImageMetadata(imgMetaData1);
        m_ObservationStore.persist(m_PersistedObservation);

        ImageMetadata imgMetaData2 = new ImageMetadata()
                .withCaptureTime(100L)
                .withMaskSamplesOfInterest(m_DigitalMedia1, m_DigitalMedia2)
                .withImager(new Camera(1, "Camera1", CameraTypeEnum.VISIBLE))
                .withImageCaptureReason(new ImageCaptureReason(ImageCaptureReasonEnum.OTHER, "reason2"));

        // Persist second observation containing some duplicate data
        Observation observation = createBaseObservation();
        observation.setDigitalMedia(m_DigitalMedia1);
        observation.setImageMetadata(imgMetaData2);
        m_ObservationStore.persist(observation);
        m_ObservationStore.remove(observation);

        // Verify that all data exists for the first observation persisted
        observation = m_ObservationStore.find(m_PersistedObservation.getUuid());
        assertEquals(m_PersistedObservation, observation);
    }

    /**
     * Make sure that observations with duplicate location data can be persisted and deleted.
     */
    public void testObsDupAssetLocation() throws PersistenceFailedException, ValidationFailedException
    {
        // Persist the first observation
        m_PersistedObservation = createTestObservation();
        m_PersistedObservation.setAssetLocation(m_AssetLocation);
        m_ObservationStore.persist(m_PersistedObservation);

        // Persist second observation containing some duplicate data
        Observation observation = createTestObservation();
        observation.setAssetLocation(m_AssetLocation);
        m_ObservationStore.persist(observation);
        m_ObservationStore.remove(observation);

        // Verify that all data exists for the first observation persisted
        observation = m_ObservationStore.find(m_PersistedObservation.getUuid());
        assertEquals(m_PersistedObservation, observation);
    }

    /**
     * Make sure that observations with duplicate location data can be persisted and deleted.
     */
    public void testObsDupAssetOrientation() throws PersistenceFailedException, ValidationFailedException
    {
        // Persist the first observation
        m_PersistedObservation = createTestObservation();
        m_PersistedObservation.setAssetOrientation(m_AssetOrientation);
        m_ObservationStore.persist(m_PersistedObservation);

        // Persist second observation containing some duplicate data
        Observation observation = createTestObservation();
        observation.setAssetOrientation(m_AssetOrientation);
        m_ObservationStore.persist(observation);
        m_ObservationStore.remove(observation);

        // Verify that all data exists for the first observation persisted
        observation = m_ObservationStore.find(m_PersistedObservation.getUuid());
        assertEquals(m_PersistedObservation, observation);
    }

    /**
     * Make sure that observations with duplicate location data can be persisted and deleted.
     */
    public void testObsDupPointingLocation() throws PersistenceFailedException, ValidationFailedException
    {
        // Persist the first observation
        m_PersistedObservation = createTestObservation();
        m_PersistedObservation.setPointingLocation(m_PointingLocation);
        m_ObservationStore.persist(m_PersistedObservation);

        // Persist second observation containing some duplicate data
        Observation observation = createTestObservation();
        observation.setPointingLocation(m_PointingLocation);
        m_ObservationStore.persist(observation);
        m_ObservationStore.remove(observation);

        // Verify that all data exists for the first observation persisted
        observation = m_ObservationStore.find(m_PersistedObservation.getUuid());
        assertEquals(m_PersistedObservation, observation);
    }

    /**
     * Make sure that observations with duplicate location data can be persisted and deleted.
     */
    public void testObsDupPlatformOrientation() throws PersistenceFailedException, ValidationFailedException
    {
        // Persist the first observation
        m_PersistedObservation = createTestObservation();
        m_PersistedObservation.setPlatformOrientation(m_PlatformOrientation);
        m_ObservationStore.persist(m_PersistedObservation);

        // Persist second observation containing some duplicate data
        Observation observation = createTestObservation();
        observation.setPlatformOrientation(m_PlatformOrientation);
        m_ObservationStore.persist(observation);
        m_ObservationStore.remove(observation);

        // Verify that all data exists for the first observation persisted
        observation = m_ObservationStore.find(m_PersistedObservation.getUuid());
        assertEquals(m_PersistedObservation, observation);
    }

    /**
     * Validate an observation that has an invalid field, but the schema for the invalid field is in a shared schema. 
     * This tests verifies that the shared schema can be resolved, especially when schema is in a different folder.
     */
    public void testValidateObservationSharedType() throws IllegalArgumentException, PersistenceFailedException
    {
        ObservationStore observationStore = 
                ServiceUtils.getService(m_Context, ObservationStore.class);
        
        Observation observation = new Observation();
        // set minimum fields
        observation.setAssetUuid(UUID.randomUUID());
        observation.setAssetName("something");
        observation.setAssetType("something");
        observation.setSystemInTestMode(false);
        observation.setSensorId("something");
        observation.setCreatedTimestamp(3L);
        observation.setUuid(UUID.randomUUID());
        observation.setVersion(new Version(1,2));
        observation.setSystemId(159);
        // DigitalMedia is shared type, encoding must be start with audio/video/image, so this is invalid 
        observation.setDigitalMedia(new DigitalMedia(new byte[] {0, 1, 2}, "invalid encoding"));
        observation.setAudioMetadata(new AudioMetadata().withRecorderType(
                new AudioRecorder().withValue(AudioRecorderEnum.MICROPHONE)));
        
        try
        {
            observationStore.persist(observation);
            fail("Expecting observation to be invalid");
        }
        catch (ValidationFailedException e)
        {
            Logging.log(LogService.LOG_INFO, e, "Validation should fail due to invalid encoding");
        }
    }
    
    /**
     * Verify that event filter works when filtering by asset name.
     */
    public void testObservationFilteringByAssetName() throws InterruptedException, 
        IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_NAME, "asset.name"));
        
        m_PersistedObservation = createTestObservation();
        m_ObservationStore.persist(m_PersistedObservation);
        
        syncer.waitForEvent(5);
    }
    
    /**
     * Verify that event filter works when filtering by system id.
     */
    public void testObservationFilteringBySystemId() throws InterruptedException, IllegalArgumentException, 
        PersistenceFailedException, ValidationFailedException
    {
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", ObservationStore.EVENT_PROP_SYS_ID, 159));
        
        m_PersistedObservation = createTestObservation();
        m_ObservationStore.persist(m_PersistedObservation);
        
        syncer.waitForEvent(5);
    }
    
    /**
     * Verify that event filter works when filtering by observation type.
     */
    public void testObservationFilteringByObservationSubTypeEnum() throws InterruptedException, 
        IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", ObservationStore.EVENT_PROP_OBSERVATION_TYPE, 
                        ObservationSubTypeEnum.AUDIO_METADATA.toString()));
        
        m_PersistedObservation = createTestObservation();
        m_ObservationStore.persist(m_PersistedObservation);
        
        syncer.waitForEvent(5);
    }
    
    /**
     * Verify that event filter works when filtering by sensor id.
     */
    public void testObservationFilteringBySensorID() throws InterruptedException, IllegalArgumentException, 
        PersistenceFailedException, ValidationFailedException
    {
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", ObservationStore.EVENT_PROP_SENSOR_ID, "asset.sensor.id"));
        
        m_PersistedObservation = createTestObservation();
        m_ObservationStore.persist(m_PersistedObservation);
        
        syncer.waitForEvent(5);
    }
    
    /**
     * Verify that event filter works when filtering by system in test mode.
     */
    public void testObservationFilteringBySysInTestMode() throws InterruptedException, IllegalArgumentException, 
        PersistenceFailedException, ValidationFailedException
    {
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", ObservationStore.EVENT_PROP_SYS_IN_TEST_MODE, false));
        
        m_PersistedObservation = createTestObservation();
        m_ObservationStore.persist(m_PersistedObservation);
        
        syncer.waitForEvent(5);
    }
    
    /**
     * Function to create a valid observation.
     * 
     * @return
     *  the valid observation
     */
    private Observation createTestObservation()
    {
        Observation observation = createBaseObservation();

        // DigitalMedia is shared type, encoding must be start with audio/video/image 
        observation.setDigitalMedia(new DigitalMedia(new byte[] {0, 1, 2}, "audio/mp3"));
        observation.setAudioMetadata(new AudioMetadata().withRecorderType(
                new AudioRecorder().withValue(AudioRecorderEnum.MICROPHONE)));

        return observation;
    }

    /**
     * Create a base observation with only the common/required fields set.
     * 
     * @return
     *  the base observation
     */
    private Observation createBaseObservation()
    {
        Observation observation = new Observation();

        // set minimum fields
        observation.setAssetUuid(UUID.randomUUID());
        observation.setAssetName("asset.name");
        observation.setAssetType("asset.product.type");
        observation.setSystemInTestMode(false);
        observation.setSensorId("asset.sensor.id");
        observation.setCreatedTimestamp(3L);
        observation.setUuid(UUID.randomUUID());
        observation.setVersion(new Version(1,2));
        observation.setSystemId(159);

        return observation;
    }
}
