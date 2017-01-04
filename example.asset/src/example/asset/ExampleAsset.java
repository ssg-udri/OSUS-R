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
package example.asset;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetPanTiltCommand;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.asset.commands.GetPositionResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.asset.commands.SetPanTiltResponse;
import mil.dod.th.core.asset.commands.SetPositionCommand;
import mil.dod.th.core.asset.commands.SetPositionResponse;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Detection;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.pm.DevicePowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.FrequencyKhz;
import mil.dod.th.core.types.PowerWatts;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.SpeedMetersPerSecond;
import mil.dod.th.core.types.detection.DetectionTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.spatial.TrackElement;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

import org.osgi.service.log.LogService;

/**
 * Asset must have the property {@link ExampleAssetAttributes#devicePowerName()} set to a valid device power entry if 
 * the example DevicePowerManager is installed.
 * 
 * @author dhumeniuk
 *
 */
@Component(factory = Asset.FACTORY)
public class ExampleAsset implements AssetProxy
{
    private DevicePowerManager m_DevicePowerManager;
    private double m_Pan;
    private LoggingService m_Log;
    private AssetContext m_Context;
    private ExampleAssetAttributes m_Attributes;
    
    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();

    @Reference
    public void setLogService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }
    
    @Reference(optional = true, dynamic = false)
    public void setDevicePowerManager(final DevicePowerManager devicePowerManager)
    { 
        m_DevicePowerManager = devicePowerManager;
    }

    public void unsetDevicePowerManager(final DevicePowerManager devicePowerMananger)
    {
        m_DevicePowerManager = null;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props)
    {
        m_Log.info("Activating example asset instance");
        m_Context = context;
        m_Attributes = Configurable.createConfigurable(ExampleAssetAttributes.class, props);
        m_CountingLock.setWakeLock(m_Context.createPowerManagerWakeLock(getClass().getSimpleName() + "WakeLock"));
    }
    
    @Deactivate
    public void deactivateInstance()
    {
        m_Log.info("Deactivating example asset instance");
        m_CountingLock.deleteWakeLock();
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        m_Attributes = Configurable.createConfigurable(ExampleAssetAttributes.class, props);
        m_Log.debug("Choice value is now: %s", m_Attributes.exampleChoice());
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            try
            {
                //only test with example platform interface
                if (m_DevicePowerManager != null && 
                        m_DevicePowerManager.getDevices().contains(m_Attributes.devicePowerName())) 
                {
                    m_DevicePowerManager.on(m_Attributes.devicePowerName());
                }
            }
            catch (Exception e)
            {
                throw new AssetException(e);
            }

            Logging.log(LogService.LOG_INFO, "Example asset activated");

            try
            {
                m_Context.setStatus(new Status().withNextStatusDurationMs(100).withSummaryStatus(
                        new OperatingStatus(SummaryStatusEnum.GOOD, "Asset Activated")));
            }
            catch (ValidationFailedException ex)
            {
                m_Log.error(ex, "Unable to set status for example asset: [%s]", m_Context.getName());
            }
        }
    }
    
    @Override
    public Observation onCaptureData() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            Logging.log(LogService.LOG_INFO, "Example Asset Data captured");
            
            final Observation obs = new Observation();
            obs.withModalities(new SensingModality().withValue(SensingModalityEnum.ACOUSTIC));
            
            final Detection detection = new Detection();
            detection.withType(DetectionTypeEnum.TEST);
            detection.withTargetLocation(SpatialTypesFactory.newCoordinates(70, 50));
            detection.withTargetOrientation(SpatialTypesFactory.newOrientation(50, 90, 0));
            detection.withTrackHistories(new TrackElement(SpatialTypesFactory.newCoordinates(50.0, 70.0), 
                    new SpeedMetersPerSecond(205.5, null, null, null, null), 
                    SpatialTypesFactory.newOrientation(1.5, 30.55, 55.55), null, 2003L));
            detection.setTargetFrequency(new FrequencyKhz().withValue(30));
            detection.setTargetId("example-target-id");
            
            obs.setDetection(detection);
            
            obs.setAssetLocation(SpatialTypesFactory.newCoordinates(54, 74));
            obs.setAssetOrientation(SpatialTypesFactory.newOrientation(180.0, -80.0, 45.0));
            obs.setPlatformOrientation(SpatialTypesFactory.newOrientation(90.0, 10.0, 45.0));
            obs.setPointingLocation(SpatialTypesFactory.newCoordinates(20.0, 50.0, 50.0));
            
            return obs;
        }
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new AssetException(
            new UnsupportedOperationException("ExampleAsset does not support capturing data by sensorId."));
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            try
            {
                // only test with example platform interface
                if (m_DevicePowerManager != null && 
                        m_DevicePowerManager.getDevices().contains(m_Attributes.devicePowerName())) 
                {
                    m_DevicePowerManager.off(m_Attributes.devicePowerName());
                }
            }
            catch (Exception e)
            {
                throw new AssetException(e);
            }
            
            m_Log.info("Example asset deactivated");
            m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
        }
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            Logging.log(LogService.LOG_INFO, "Performing BIT");
            return new Status().withComponentStatuses(new ComponentStatus(
                    new ComponentType(ComponentTypeEnum.CPU, "Single Board CPU 1"), 
                    new OperatingStatus(SummaryStatusEnum.BAD, "POST failed."))).
                    withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "BIT Passed")).
                    withPowerConsumption(new PowerWatts().withValue(12));
        }
    }
    
    private Response setPanTilt(final SetPanTiltCommand setPT) throws UnmarshalException, CommandExecutionException
    {
        AssetCapabilities capabilities = m_Context.getFactory().getAssetCapabilities();

        double currentPan = setPT.getPanTilt().getAzimuth().getValue();
        double minPan = capabilities.getCommandCapabilities().getPanTilt().getMinAzimuth().getValue();
        double maxPan = capabilities.getCommandCapabilities().getPanTilt().getMaxAzimuth().getValue();
        if (currentPan <= maxPan && currentPan >= minPan)
        {
            m_Pan = currentPan;
            Logging.log(LogService.LOG_INFO, "Updated pan to: " + m_Pan);

            return new SetPanTiltResponse();
        }
        else
        {
            throw new CommandExecutionException("Request pan value out of range");
        }
    }
    
    private Response getPanTilt()
    {
        return new GetPanTiltResponse().withPanTilt(SpatialTypesFactory.newOrientationOffset(m_Pan));
    }

    @Override 
    public Response onExecuteCommand(final Command capabilityCommand)
        throws CommandExecutionException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            if (capabilityCommand instanceof SetPanTiltCommand)
            {
                try
                {
                    return setPanTilt((SetPanTiltCommand)capabilityCommand);
                }
                catch (UnmarshalException e)
                {
                    throw new CommandExecutionException(e);
                } 
            }
            else if (capabilityCommand instanceof GetPanTiltCommand)
            {
                return getPanTilt();
            }
            else if (capabilityCommand instanceof SetPositionCommand)
            {
                return new SetPositionResponse();
            }
            else if (capabilityCommand instanceof GetPositionCommand)
            {
                return new GetPositionResponse().withLocation(new Coordinates().
                        withLongitude(
                                new LongitudeWgsDegrees().withValue(0)).
                                withLatitude(
                                        new LatitudeWgsDegrees().withValue(0)));
            }
            else
            {
                throw new CommandExecutionException("Could not execute specified command.");
            }
        }
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        Extension<ExampleAssetExtension1> extension1 = new Extension<ExampleAssetExtension1>()
        {
            @Override
            public Class<ExampleAssetExtension1> getType()
            {
                return ExampleAssetExtension1.class;
            }

            @Override
            public ExampleAssetExtension1 getObject()
            {
                return new ExampleAssetExtension1()
                {
                    @Override
                    public String addSuffix(String suffix)
                    {
                        return m_Context.getName() + suffix;
                    }
                };
            }
        };
        
        Extension<ExampleAssetExtension2> extension2 = new Extension<ExampleAssetExtension2>()
        {

            @Override
            public Class<ExampleAssetExtension2> getType()
            {
                return ExampleAssetExtension2.class;
            }

            @Override
            public ExampleAssetExtension2 getObject()
            {
                return new ExampleAssetExtension2()
                {
                    @Override
                    public void performFunction()
                    {
                    }
                };
            }
        };
        Set<Extension<?>> extensions = new HashSet<>();
        extensions.add(extension1);
        extensions.add(extension2);
        
        return extensions;
    }
}
