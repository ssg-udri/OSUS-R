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
package mil.dod.th.ose.shell;

import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;

import org.apache.felix.service.command.Descriptor;


/**
 * Asset Commands.
 * 
 * @author cweisenborn
 *
 */
@Component(provide = AssetCommands.class, properties = { "osgi.command.scope=thasset",
        "osgi.command.function=getAssets|getAssetByName|createAsset|scanForNewAssets|assetActiveStatus" })
public class AssetCommands
{
    /**
     * Reference to the asset directory service.
     */
    private AssetDirectoryService m_AssetDirectoryService;
    
    /**
     * Set the asset directory service to use.
     * 
     * @param assetDirectoryService the m_AssetDirectoryService to set
     */
    @Reference
    public void setAssetDirectoryService(final AssetDirectoryService assetDirectoryService)
    {
        m_AssetDirectoryService = assetDirectoryService;
    }
    
    
    /**
     * Returns all assets.
     * 
     * @return assets
     *      returns a set of assets
     */
    @Descriptor("Returns all assets belonging to AssetDirectoryService.")
    public Set<Asset> getAssets()
    {
        return m_AssetDirectoryService.getAssets();
    }
    
    /**
     * Retrieves an asset by name.
     * 
     * @param name
     *           name of the asset
     * @return asset
     */
    @Descriptor("Returns the asset with the given name.")
    public Asset getAssetByName(
            @Descriptor("Name of the asset instance.")
            final String name) 
    {
        return m_AssetDirectoryService.getAssetByName(name);
    }
    
    /**
     * Create and add a new asset to the directory given the asset product type (
     * {@link mil.dod.th.core.factory.FactoryDescriptor#getProductType()}).
     * 
     * @param assetProductType
     *            Product type of the asset in fully qualified class name format.
     * @return the newly created asset.
     * @throws AssetException
     *            If the directory is unable to add the given asset.
     */
    @Descriptor("Creates an asset with the given asset product type.")
    public Asset createAsset(
            @Descriptor("Unique product type string of the asset in fully qualified class name format.")
            final String assetProductType) throws AssetException
    {
        return m_AssetDirectoryService.createAsset(assetProductType);
    }
    
    /**
     * Scans for all new assets.
     */
    @Descriptor("Scans for new assets that have been created.")
    public void scanForNewAssets()
    {
        m_AssetDirectoryService.scanForNewAssets();
    }
    
    /**
     * Returns the active status of all assets.
     * 
     * @return StringBuilder
     */
    @Descriptor("Returns the active status of all assets.")
    public String assetActiveStatus()
    {
        final StringBuilder buffer = new StringBuilder(100);
        
        for (Asset asset : m_AssetDirectoryService.getAssets())
        {
            buffer.append(asset.getName()
                          + ":\n" 
                          + "\tActivate On Startup = " 
                          + asset.getConfig().activateOnStartup() 
                          + "\n\tActive Status = " 
                          + asset.getActiveStatus()
                          + '\n');
        }

        return buffer.toString();
    }
}
