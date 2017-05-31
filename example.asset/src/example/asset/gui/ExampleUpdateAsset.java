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
package example.asset.gui;

import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import example.asset.lexicon.ExampleAssetUtils;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.asset.commands.GetVersionCommand;
import mil.dod.th.core.asset.commands.GetVersionResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.validator.ValidationFailedException;

/**
 * This asset factory will create asset instances which post a 
 * {@link mil.dod.th.core.asset.Asset#TOPIC_COMMAND_RESPONSE_UPDATED} event at activation.
 * 
 * @author Cheryl
 */
@Component(factory = Asset.FACTORY)
public class ExampleUpdateAsset  implements AssetProxy
{
    private LoggingService m_Log;
    private AssetContext m_Context;

    @Reference
    public void setLogService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props)
    {
        m_Log.info("Activating example update asset instance");
        m_Context = context;
    }
    
    @Deactivate
    public void deactivateInstance()
    {
        m_Log.info("Deactivating example update asset instance");
    }
    
    @Override
    public void onActivate()
    {
        m_Context.postResponseUpdate(
                new GetVersionResponse()
                    .withReserved(ExampleAssetUtils.buildReservedList())
                    .withCurrentVersion("V activate-version")
        );
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        // nothing
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        ExampleUpdateAssetAttributes attributes =
                Configurable.createConfigurable(ExampleUpdateAssetAttributes.class, props);
        try
        {
            if (attributes.sensorId().isEmpty())
            {
                m_Context.setPositionLocation(new Coordinates()
                        .withLatitude(new LatitudeWgsDegrees().withValue(attributes.latitude()))
                        .withLongitude(new LongitudeWgsDegrees().withValue(attributes.longitude())));

                final Observation obs = new Observation()
                        .withAssetLocation(m_Context.getPositionLocation())
                        .withReserved(new MapEntry("resKey", "resValue"));
                m_Context.persistObservation(obs);
            }
            else
            {
                m_Context.setPositionLocation(attributes.sensorId(), new Coordinates()
                        .withLatitude(new LatitudeWgsDegrees().withValue(attributes.latitude()))
                        .withLongitude(new LongitudeWgsDegrees().withValue(attributes.longitude())));

                final Observation obs = new Observation()
                        .withSensorId(attributes.sensorId())
                        .withAssetLocation(m_Context.getPositionLocation(attributes.sensorId()))
                        .withReserved(new MapEntry("resKey", "resValue"));
                m_Context.persistObservation(obs);
            }
        }
        catch (AssetException e)
        {
            m_Log.error("Example update asset unable to store position data");
        }
        catch (IllegalStateException e)
        {
            m_Log.error("Example update asset try to save position in the core when overriding position");
        }
        catch (PersistenceFailedException | ValidationFailedException e)
        {
            m_Log.error("Example update asset failed to persist position observations");
        }
    }

    @Override
    public Observation onCaptureData() throws AssetException
    {
        throw new UnsupportedOperationException("The example update asset does not support capturing data.");
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new UnsupportedOperationException(
                "The example update asset does not support capturing data by sensorId.");
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        throw new UnsupportedOperationException("The example update asset does not support a BIT.");
    }

    @Override
    public Response onExecuteCommand(final Command capabilityCommand) throws CommandExecutionException, 
        InterruptedException
    {
        if (capabilityCommand instanceof GetVersionCommand)
        {
            return new GetVersionResponse()
                    .withReserved(ExampleAssetUtils.buildReservedList())
                    .withCurrentVersion("V R.2.d.200");
        }
        else
        {
            throw new CommandExecutionException("Could not execute specified command.");
        }
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
