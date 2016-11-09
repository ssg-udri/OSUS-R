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
package mil.dod.th.ose.gui.webapp.asset;

import java.util.List;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ModifiablePropertyModel;

/**
 * This interface is used to assist with the add asset dialog. It holds the asset factory to use and the name desired
 * for the newly created asset.
 * @author callen
 *
 */
public interface AddAssetDialogHelper 
{
    /**
     * Set the asset factory to use for the creation of an asset.
     * @param factory
     *     the model representing the asset factory to use
     */
    void setAssetFactory(AssetFactoryModel factory);
    
    /**
     * Get the asset factory to use for the creation of an asset.
     * @return
     *     the model representing the asset factory to use
     */
    AssetFactoryModel getAssetFactory();
    
    /**
     * Flag value to represent if the asset factory is set.
     * @return
     *     true if set, false otherwise
     */
    boolean isSetAssetFactory();
    
    /**
     * Get the new asset's intended name.
     * @return
     *    the desired name of the asset
     */
    String getNewAssetName();
    
    /**
     * Set the name intended for the newly created asset.
     * @param name
     *     the name to assign to the newly created asset
     */
    void setNewAssetName(final String name);
    
    /**
     * Clear all fields that may have been previously set.
     */
    void init();

    /**
     * Request an update of the factories known for the controller id given.
     * @param controllerId
     *     the system id of the controller from which the request originated
     */
    void requestAssetTypeUpdate(int controllerId);

    /**
     * Create an asset of the specified type on the controller id given.
     * @param productType
     *     the asset factory's product type that the new asset will be
     * @param controllerId
     *     the id of the controller to which the request will be sent
     * @param name
     *     the name to set for the asset
     */
    void requestCreateAsset(String productType, int controllerId, String name);
    
    /**
     * Get the AssetCapabilities from the Factory Model, if they are set.
     * @return an AssetCapabilities object, or null.
     */
    AssetCapabilities getAssetCaps();

    /**
     * Returns a list of configuration properties for the currently set asset factory model.
     * 
     * @return
     *      List of modifiable configuration properties for the asset factory model.
     */
    List<ModifiablePropertyModel> getProperties();
}
