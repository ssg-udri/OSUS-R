package ${package};

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.asset.commands.SetPanTiltResponse;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.ImageMetadata;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.observation.types.Weather;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.PowerWatts;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.TemperatureCelsius;
import mil.dod.th.core.types.image.Camera;
import mil.dod.th.core.types.image.CameraTypeEnum;
import mil.dod.th.core.types.image.ImageCaptureReason;
import mil.dod.th.core.types.image.ImageCaptureReasonEnum;
import mil.dod.th.core.types.image.PictureTypeEnum;
import mil.dod.th.core.types.image.PixelResolution;
import mil.dod.th.core.types.status.AmbientStatus;
import mil.dod.th.core.types.status.AmbientType;
import mil.dod.th.core.types.status.AmbientTypeEnum;
import mil.dod.th.core.types.status.BatteryChargeLevel;
import mil.dod.th.core.types.status.ChargeLevelEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

import org.osgi.service.log.LogService;

/**
 * ${description} implementation.
 * 
 * @author ${author}
 */
@Component(factory = Asset.FACTORY)
public class ${class} implements AssetProxy
{ 
    /**
     * Reference to the context which provides this class with methods to interact with the rest of the system.
     */
    private AssetContext m_Context;
    
    /**
     * Reference to the wake lock used for vital asset operations.
     */
    private WakeLock m_WakeLock;

    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props) throws FactoryException
    {
        m_Context = context;
        
        // Retrieve a wake lock used to keep the system awake during vital asset operations. 
        m_WakeLock = m_Context.createPowerManagerWakeLock("${class}WakeLock");
        
        // ${task}: Replace with custom handling of properties when asset is created or restored. `config` object uses
        // reflection to obtain property from map and therefore should not be used in processing intensive code.
        final ${class}Attributes config = Configurable.createConfigurable(${class}Attributes.class, props);
        
        // Properties that have been defined in ${class}Attributes will be accessible through the `config` object
        // m_SomeProperty = config.someProperty();
        
        // ${task}: The following demonstrates the ability to post observations at any time, which will cause
        // the persisted information to be available to the rest of the system. Use the AssetContext's
        // persistObservation() method to post observations asynchronously.
        final Observation obs = new Observation().withWeather(new Weather()
                .withTemperature(new TemperatureCelsius().withValue(98.6)));

        try
        {
            m_Context.persistObservation(obs);
        }
        catch (final ValidationFailedException e)
        {
            throw new AssetException(e);
        }
    }

    /**
     * Deactivate the asset when removed or core is shutdown.
     */
    @Deactivate
    public void release()
    {
        // Remove the wake lock before the asset is deactivated.
        m_WakeLock.delete();
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // ${task}: Replace with custom handling of properties when they are updated. `config` object uses
        // reflection to obtain property from map and therefore should not be used in processing intensive code.
        final ${class}Attributes config = Configurable.createConfigurable(${class}Attributes.class, props);
        
        // Properties that have been defined in ${class}Attributes will be accessible through the `config` object
        // m_SomeProperty = config.someProperty();
    }
    
    @Override
    public void onActivate() throws AssetException
    {
        try
        {
            // Use a wake lock to ensure the system stays awake while activating
            m_WakeLock.activate();

            Logging.log(LogService.LOG_INFO, "${description} activated");

            // ${task}: Handle asset activation by AssetDirectoryService, i.e. initialize hardware, input/output 
            // streams, when the asset is active, it should be collecting data, doing processing, whatever it means to 
            // be in an active state
            
            // The below is an example of setting the asset's status using a Status object.
            // Assume this information would be fetched from the physical asset itself. Therefore,
            // establishing an relevant status for the asset and also checking communication with the physical device. 
            // If for some reason the information could not be gathered the asset generally should not be activated like
            // seen below, if the status observation is not valid an exception is thrown which would prevent the asset 
            // from assuming the 'Activated' status.
            final AmbientStatus ambientStatus = new AmbientStatus(
                    new AmbientType(AmbientTypeEnum.TEMPERATURE, "temperature is low"), 
                        new OperatingStatus(SummaryStatusEnum.GOOD, "warming up"));
            final BatteryChargeLevel batLevel = new BatteryChargeLevel().withChargeLevel(ChargeLevelEnum.FULL);
            final Status status = new Status()
                .withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "Asset Activated"))
                .withAmbientStatus(ambientStatus)
                .withBatteryChargeLevel(batLevel);
            
            try
            {
                m_Context.setStatus(status);
            }
            catch (final ValidationFailedException e)
            {
                throw new AssetException(e);
            }
        }
        finally
        {
            // Ensure that the wake lock is cancelled regardless of whether or not the asset successfully activated.
            m_WakeLock.cancel();
        }
    }
    
    @Override
    public void onDeactivate() throws AssetException
    {
        try
        {
            m_WakeLock.activate();
            Logging.log(LogService.LOG_INFO, "${description} deactivated");

            // ${task}: Handle asset deactivation by AssetDirectoryService, i.e. release hardware resources, power off 
            // devices, should no longer be collecting data, processing data, etc., whatever it means to be in an 
            // inactive state, this asset can be later activated again
            m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }
    
    @Override
    public Observation onCaptureData()
    {
        // ${task}: Replace with specific data captured by this plug-in, this is just an example

        Logging.log(LogService.LOG_INFO, "${description} data captured");

        final ImageMetadata imageMeta = new ImageMetadata();
        imageMeta.setColor(true);
        imageMeta.setImager(new Camera(0, "Example Camera", CameraTypeEnum.VISIBLE));
        imageMeta.setPictureType(PictureTypeEnum.FULL_FIELD_OF_VIEW);
        final int pixWidth = 752;
        final int pixHeight = 480;
        imageMeta.setResolution(new PixelResolution(pixWidth, pixHeight));
        imageMeta.setImageCaptureReason(new ImageCaptureReason(
            ImageCaptureReasonEnum.MANUAL, "Manual order to capture image."));
 
        // getImage is an example method that retrieves an image in JPEG format
        //byte[] imageData = getImage();
        final byte[] imageData = new byte[]{1, 2, 0};
        final DigitalMedia digitalMedia = new DigitalMedia(imageData, "image/jpg");
 
        final Observation obs = new Observation()
            .withDigitalMedia(digitalMedia)
            .withImageMetadata(imageMeta)
            .withModalities(new SensingModality().withValue(SensingModalityEnum.IMAGER));
        return obs;
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        // Update the capabilities flag and provide implementation if the asset supports multiple sensors
        throw new AssetException(
            new UnsupportedOperationException("Asset does not support capturing data by sensorId."));
    }

    @Override
    public Status onPerformBit()
    {
        // ${task}: Replace with actual BIT testing. A BIT should actively query hardware and peripherals 
        // for their statuses. With that in mind, the status for the Asset 
        // after performing its built-in-test should include as much information as possible.
        // The status below is an example of including the status of other components, and additional
        // power consumption information. 

        Logging.log(LogService.LOG_INFO, "Performing ${description} BIT");
        final int powerConsumptionWatts = 12; // some value acquired from device
        return new Status().withComponentStatuses(new ComponentStatus(
                new ComponentType(ComponentTypeEnum.CPU, "Single Board CPU 1"), 
                    new OperatingStatus(SummaryStatusEnum.BAD, "POST failed."))).
                withSummaryStatus(new OperatingStatus(SummaryStatusEnum.GOOD, "BIT Passed")).
                withPowerConsumption(new PowerWatts().withValue(powerConsumptionWatts));
    }
    
    @Override
    public Response onExecuteCommand(final Command command) throws CommandExecutionException, InterruptedException
    {
        // ${task}: Replace with supported commands, if any, this is just an example

        if (command instanceof SetPanTiltCommand)
        {
            final SetPanTiltCommand setPT = (SetPanTiltCommand)command;
            if (setPT.isSetSensorId())
            {
                // If the asset supports executing commands for different sensors, use the command.getSensorId() method
                // to retrieve the sensor ID and handle accordingly.
            }

            Logging.log(LogService.LOG_INFO, "Updated pan to: " + setPT.getPanTilt().getAzimuth());

            // Executing this command may typically take some time to execute. When executing any command,
            // the code implemented should wait for some kind of verification that the command
            // successfully affected a change to the asset before returning the response.
            // 
            // If the command has the sensorId field set, it should also be included in the response:
            //  return new SetPanTiltResponse().withSensorId(setPT.getSensorId());
            return new SetPanTiltResponse();
        }
        else
        {
            throw new CommandExecutionException("Could not execute specified command.");
        }
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        // ${task}: Modify so that the set returned contains any plug-in specific extensions. The use of extensions is 
        // discouraged, but can be used in cases where it is necessary to provide an extended API.
    
        return new HashSet<Extension<?>>();
    }
}
