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
package mil.dod.th.ose.config.loading.impl;

import java.util.Map;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.model.config.FactoryObjectConfig;

import org.osgi.service.event.EventAdmin;

/**
 * This service is used to track the configuration of assets.
 * 
 * @author dlandoll
 */
public class AssetTrackerCustomizer extends FactoryObjectTrackerCustomizer
{
    /**
     * Reference to the asset directory service.
     */
    private final AssetDirectoryService m_AssetDirectoryService;

    /**
     * Creates a new service tracker customizer.
     * 
     * @param objectConfig
     *      Factory object configuration
     * @param assetDirectoryService
     *      Asset manager service used to create and manage assets
     * @param loggingService
     *      Logging service used to record messages in the system log
     * @param eventAdmin
     *      Service for sending OSGi events
     */
    public AssetTrackerCustomizer(final FactoryObjectConfig objectConfig, 
            final AssetDirectoryService assetDirectoryService, final LoggingService loggingService,
            final EventAdmin eventAdmin)
    {
        super(objectConfig, loggingService, eventAdmin);
        m_AssetDirectoryService = assetDirectoryService;
    }

    @Override
    public void addingFactoryDescriptor(final FactoryDescriptor factory) throws FactoryException
    {
        final FactoryObjectConfig objectConfig = getObjectConfig();
        final String assetName = objectConfig.getName();
        final String assetProductType = objectConfig.getProductType();

        //At this point it is either the first run of the configuration or the create policy
        //is set to IfMissing. Therefore, if the asset is not in the ADS at either of these cases then I 
        //should create it and set the properties.
        if (!m_AssetDirectoryService.isAssetAvailable(assetName))
        {
            final Map<String, Object> properties = translateStringMap(objectConfig.getProperties(), factory);

            try
            {
                m_AssetDirectoryService.createAsset(assetProductType, assetName, properties);
            }
            catch (final AssetException ex)
            {
                throw new FactoryException("Unable to create asset: " + objectConfig, ex);
            }
        }
    }
}
