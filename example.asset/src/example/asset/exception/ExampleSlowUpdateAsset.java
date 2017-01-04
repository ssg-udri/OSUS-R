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

import org.osgi.service.cm.ConfigurationException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;

/**
 * This asset updates its configuration values SLOWLY.
 * 
 * @author Cheryl
 */
@Component(factory = Asset.FACTORY)
public class ExampleSlowUpdateAsset implements AssetProxy 
{
    private LoggingService m_Log;
    private String m_Property;

    @Reference
    public void setLogService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }
    
    @Override
    public void initialize(final AssetContext context, final Map<String, Object> props)
    {
        m_Log.info("Activating example slow update asset instance");
        ExampleSlowUpdateAssetAttributes config = 
                Configurable.createConfigurable(ExampleSlowUpdateAssetAttributes.class, props);
        m_Property = config.exampleProperty();
    }
    
    @Deactivate
    public void deactivateInstance()
    {
        m_Log.info("Deactivating example slow update asset instance");
    }
    
    @Override
    public void onActivate()
    {
        if (m_Property.equals("default"))
        {
            throw new IllegalStateException("Property is still set to default, "
                    + "set property didn't update property");
        }
    }
    
    @Override
    public void onDeactivate() throws AssetException
    {
        //nothing
    }

    @Override
    public void updated(final Map<String, Object> props) throws ConfigurationException
    {
        try
        {
            Thread.sleep(10000);
        }
        catch (InterruptedException e)
        {
            throw new ConfigurationException("Some property from exampleSlowUpdateAsset", "It is slow!", e);
        }
        m_Property = Configurable.createConfigurable(ExampleSlowUpdateAssetAttributes.class, props)
                .exampleProperty();
    }

    @Override
    public Observation onCaptureData() throws AssetException
    {
        throw new UnsupportedOperationException("The example slow update asset does not support capturing data.");
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new UnsupportedOperationException(
                "The example slow update asset does not support capturing data by sensorId.");
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        throw new UnsupportedOperationException("The example slow update asset does not support a BIT.");
    }

    @Override
    public Response onExecuteCommand(final Command capabilityCommand) throws CommandExecutionException, 
        InterruptedException
    {
        throw new CommandExecutionException("Example slow update assets don't support commands.");
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
