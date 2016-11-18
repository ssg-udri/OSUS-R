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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;

import mil.dod.th.core.observation.types.AlgorithmStatus;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.PowerWatts;
import mil.dod.th.core.types.TemperatureCelsius;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.types.VoltageVolts;
import mil.dod.th.core.types.spatial.DistanceMeters;
import mil.dod.th.core.types.status.AmbientStatus;
import mil.dod.th.core.types.status.AmbientType;
import mil.dod.th.core.types.status.AmbientTypeEnum;
import mil.dod.th.core.types.status.BatteryChargeLevel;
import mil.dod.th.core.types.status.ChargeLevelEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.InternalArchiveStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.ose.test.matchers.JaxbUtil;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Test class that ensures that proto message objects are correctly converted to equivalent Jaxb objects 
 * and vice a versa.
 * @author allenchl
 *
 */
public class TestJaxbConverter extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    /**
     * Verify that an observation can be created using jaxb converted to a proto message and back without losing its 
     * integrity.
     */
    public final void testConvertingObservation() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException, ObjectConverterException
    {
        final Validator validator = ServiceUtils.getService(m_Context, Validator.class);
        final JaxbProtoObjectConverter converter = ServiceUtils.getService(m_Context, JaxbProtoObjectConverter.class);
        Observation obs = createStatusObservation();
        
        //verify valid
        validator.validate(obs);
        
        //convert
        ObservationGen.Observation obsGen = (ObservationGen.Observation)converter.convertToProto(obs);
        Observation obsThatWasConverted = (Observation)converter.convertToJaxb(obsGen);
        
        //verify valid
        validator.validate(obsThatWasConverted);
        
        //verify equal content
        JaxbUtil.assertEqualContent(obs, obsThatWasConverted);
    }
    
    /**
     * Create an observation.
     */
    private Observation createStatusObservation()
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

        List<AlgorithmStatus> algorithmStatus = null;

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
                algorithmStatus,
                assetOnTime,
                nextStatusDurationMs);

        UUID uuid = UUID.randomUUID(); 
        Long createdTimestamp = 100L;
        String assetName = "asset-name";
        String assetType = "asset-type";
        String sensorId = "sensor-id";
        boolean systemInTestMode = false;
        Version version = new Version(1,2);
        int systemId = 0123;
        return new Observation(version, null, null, null, null, null, null, null, status, null, null, null, null, null,
                null, null, null, null, null, null, null, uuid, null, createdTimestamp, null, assetName, assetType,
                sensorId, systemInTestMode, systemId).withAssetUuid(UUID.randomUUID());
    }
}
