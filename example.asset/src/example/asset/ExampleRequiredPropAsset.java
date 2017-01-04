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

import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.types.status.SummaryStatusEnum;

import org.osgi.service.cm.ConfigurationException;

/**
 * Example asset that has required configuration properties. Used to test that an asset with required properties can be 
 * created through the remote interface and GUI.
 * 
 * @author cweisenborn
 */
@Component(factory = Asset.FACTORY)
public class ExampleRequiredPropAsset implements AssetProxy
{
    private AssetContext m_Context;
    
    @Override
    public void initialize(AssetContext context, Map<String, Object> props) throws FactoryException
    {
        m_Context = context;
        Configurable.createConfigurable(ExampleRequiredPropAssetAttributes.class, props);
    }
    
    @Override
    public void updated(Map<String, Object> props) throws ConfigurationException
    {
        Configurable.createConfigurable(ExampleRequiredPropAssetAttributes.class, props);
    }

    @Override
    public void onActivate() throws AssetException
    {
        m_Context.setStatus(SummaryStatusEnum.GOOD, "Asset Activated");
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        m_Context.setStatus(SummaryStatusEnum.OFF, "Asset Deactivated");
    }

    @Override
    public Observation onCaptureData() throws AssetException
    {
        throw new UnsupportedOperationException("Asset does not support capturing data.");
    }

    @Override
    public Observation onCaptureData(final String sensorId) throws AssetException
    {
        throw new UnsupportedOperationException("Asset does not support capturing data by sensorId.");
    }

    @Override
    public Status onPerformBit() throws AssetException
    {
        throw new UnsupportedOperationException("Asset does not support a BIT.");
    }

    @Override
    public Response onExecuteCommand(Command command) throws CommandExecutionException, InterruptedException
    {
        throw new CommandExecutionException("Asset does not support executing commands.");
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
