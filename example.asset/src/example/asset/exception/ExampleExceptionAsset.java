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
package example.asset.exception;

import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;

/**
 * @author dhumeniuk
 *
 */
@Component(factory = Asset.FACTORY)
public class ExampleExceptionAsset implements AssetProxy
{
    @Override
    public void initialize(AssetContext context, Map<String,Object> props)
    {
        // Do nothing
    }
    
    @Override
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }

    @Override
    public void onActivate() throws AssetException
    {
        throw new AssetException("Unable to activate");
    }

    @Override
    public Observation onCaptureData()
    {
        throw new NullPointerException();
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new AssetException(
            new UnsupportedOperationException("ExampleExceptionAsset does not support capturing data by sensorId."));
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        throw new AssetException("Unable to deactivate");
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        throw new NullPointerException();
    }
    
    @Override
    public Response onExecuteCommand(final Command capabilityCommand)
        throws CommandExecutionException
    {
        return null;
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
