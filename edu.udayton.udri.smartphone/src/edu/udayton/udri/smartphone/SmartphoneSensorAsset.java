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
package edu.udayton.udri.smartphone;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.status.BatteryChargeLevel;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.log.LogService;

/**
 * A class representing a smartphone client connected to a socket server. A client can contain information such as
 * GPS coordinates and battery level. A client is represented by its hostname.
 * 
 * @author rosenwnj
 *
 */
@Component(factory = Asset.FACTORY)
public class SmartphoneSensorAsset implements AssetProxy
{
    private static final String LATITUDE_STRING = "latitude";
    private static final String LONGITUDE_STRING = "longitude";
    private static final String BATTERY_STRING = "batteryLevel";
    
    private AssetContext m_Context;

    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props) throws FactoryException 
    {
        m_Context = context;

        m_Context.setStatus(SummaryStatusEnum.GOOD, "Initialized");
    }
    
    @Override
    public void updated(final Map<String, Object> props) throws ConfigurationException 
    {
        final Observation obs = new Observation();
        
        final OperatingStatus op = new OperatingStatus()
            .withSummary(SummaryStatusEnum.GOOD)
            .withDescription("Operating correctly");

        if (props.containsKey(LONGITUDE_STRING) && props.containsKey(LATITUDE_STRING))
        {
            final LatitudeWgsDegrees lat = new LatitudeWgsDegrees().withValue((Double) props.get(LATITUDE_STRING));
            final LongitudeWgsDegrees lon = new LongitudeWgsDegrees()
                .withValue((Double) props.get(LONGITUDE_STRING));
            
            final Coordinates coords = new Coordinates().withLatitude(lat).withLongitude(lon);
            
            obs.setAssetLocation(coords);
        }
        
        if (props.containsKey(BATTERY_STRING))
        {
            final BatteryChargeLevel battery = new BatteryChargeLevel()
                .withChargePercentage((Double) props.get(BATTERY_STRING));
            
            final Status stat = new Status()
                    .withSummaryStatus(op)
                    .withBatteryChargeLevel(battery);
            
            obs.setStatus(stat);
        }
        
        try 
        {
            m_Context.persistObservation(obs);
            m_Context.setStatus(new Status().withSummaryStatus(op));
        } 
        catch (final PersistenceFailedException | ValidationFailedException e) 
        {

            m_Context.setStatus(SummaryStatusEnum.BAD, "Failed to persist last observation");
            Logging.log(LogService.LOG_ERROR, "Failed to persist observation");
            e.printStackTrace();
        }
        
    }

    @Override
    public void onActivate() throws AssetException 
    {
        throw new UnsupportedOperationException("Activation is not supported.");
    }

    @Override
    public void onDeactivate() throws AssetException 
    {
        throw new UnsupportedOperationException("Deactivation is not supported.");
    }

    @Override
    public Observation onCaptureData() throws AssetException 
    {
        throw new UnsupportedOperationException("Capturing data is not supported.");
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException 
    {
        throw new UnsupportedOperationException("Capturing data by sensorId is not supported.");
    }

    @Override
    public Status onPerformBit() throws AssetException 
    {
        throw new UnsupportedOperationException("BIT is not supported.");
    }

    @Override
    public Response onExecuteCommand(final Command command) throws CommandExecutionException, InterruptedException 
    {
        throw new UnsupportedOperationException("Command execution is not supported.");
    }

    @Override
    public Set<Extension<?>> getExtensions() 
    {
        return new HashSet<Extension<?>>();    
    }
    
}
