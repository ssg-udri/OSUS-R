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
package mil.dod.th.ose.core.impl.asset;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.asset.AssetAttributes;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.RegistryDependency;

import org.osgi.service.cm.ConfigurationException;

/**
 * Registry that holds assets.
 * 
 * @author dhumeniuk
 *
 */
final class AssetRegistryCallback implements FactoryRegistryCallback<AssetInternal>
{
    /**
     * The {@link mil.dod.th.core.asset.AssetDirectoryService}'s factory service context.
     */
    private final FactoryServiceContext<AssetInternal> m_FactoryServiceContext;

    /**
     * Constructor for this callback.
     * @param factoryServiceContext
     *      the factory service context of the directory service creating this callback
     */
    AssetRegistryCallback(final FactoryServiceContext<AssetInternal> factoryServiceContext)
    {
        m_FactoryServiceContext = factoryServiceContext;
    }

    @Override
    public void preObjectInitialize(final AssetInternal object)
    {
        // nothing needs to be done prior to initialization
    }

    @Override
    public void postObjectInitialize(final AssetInternal object) throws FactoryException
    {
        final AssetAttributes attributes = object.getConfig();
        
        //activate the asset IF it is new and the 'activate on startup' property is set to true
        if (!m_FactoryServiceContext.getRegistry().isObjectCreated(object.getName()) 
                && attributes.activateOnStartup())
        {
            object.activateAsync();
        }
    }

    @Override
    public void preObjectUpdated(final AssetInternal object) throws FactoryException, ConfigurationException
    {
        // nothing needs to be done prior to updating
    }
    
    @Override
    public void onRemovedObject(final AssetInternal object)
    {
        // nothing needs to be done after removal
    }

    @Override
    public List<RegistryDependency> retrieveRegistryDependencies()
    {
        return new ArrayList<RegistryDependency>();
    }
}