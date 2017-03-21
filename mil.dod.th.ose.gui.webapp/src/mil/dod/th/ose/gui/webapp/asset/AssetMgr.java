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
import java.util.UUID;

/**
 * The asset manager allows access to remote system's assets, and assets available locally.
 * @author callen
 *
 */
public interface AssetMgr
{
    /**
     * Event property key for the string version of the asset active status from {@link AssetModel#getActiveStatus()}.
     */
    String EVENT_PROP_ACTIVE_STATUS_SUMMARY = "asset.activestatus.summary";
    
    /** 
     * Event topic prefix for all {@link AssetMgr} events. 
     */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/asset/AssetMgr/";
    
    /** 
     * Topic used for when a new asset is added.
     * 
     * Contains the following properties:
     * <ul>
     * <li>{@link AssetModel#EVENT_PROP_UUID} - UUID of the asset</li>
     * </ul>
     */
    String TOPIC_ASSET_ADDED = TOPIC_PREFIX + "ASSET_ADDED";
    
    /** 
     * Topic used for when an asset is removed.
     * 
     * Contains the following properties:
     * <ul>
     * <li>{@link AssetModel#EVENT_PROP_UUID} - UUID of the asset</li>
     * </ul>
     */
    String TOPIC_ASSET_REMOVED = TOPIC_PREFIX + "ASSET_REMOVED";
    
    /** 
     * Topic used for generic updates to the asset, such as updating the name.
     * 
     * Contains the following properties:
     * <ul>
     * <li>{@link AssetModel#EVENT_PROP_UUID} - UUID of the asset</li>
     * </ul>
     */
    String TOPIC_ASSET_UPDATED = TOPIC_PREFIX + "ASSET_UPDATED";
    
    /**
     * Topic used for when the status of the asset has changed.
     * 
     * Contains the following properties:
     * <ul>
     * <li>{@link AssetModel#EVENT_PROP_UUID} - UUID of the asset</li>
     * <li>{@link mil.dod.th.core.asset.Asset#EVENT_PROP_ASSET_STATUS_SUMMARY} - summary of the changed status</li>
     * <li>{@link mil.dod.th.core.asset.Asset#EVENT_PROP_ASSET_STATUS_DESCRIPTION} - description of the changed 
     * status</li>
     * </ul>
     */
    String TOPIC_ASSET_STATUS_UPDATED = TOPIC_PREFIX + "ASSET_STATUS_UPDATED";
    
    /**
     * Topic used for asset activation/deactivation status changes.
     * 
     * Contains the following properties:
     * <ul>
     * <li>{@link AssetModel#EVENT_PROP_UUID} - UUID of the asset</li>
     * <li>{@link #EVENT_PROP_ACTIVE_STATUS_SUMMARY} - summary of the active status for the asset </li>
     * </ul>
     */
    String TOPIC_ASSET_ACTIVATION_STATUS_UPDATED = TOPIC_PREFIX + "ASSET_ACTIVATION_STATUS_UPDATED";
    
    /**
     * Topic used for when the location of an asset has changed.
     * 
     * Contains the following properties:
     * <ul>
     * <li>{@link AssetModel#EVENT_PROP_UUID} - UUID of the asset</li>
     * </ul>
     */
    String TOPIC_ASSET_LOCATION_UPDATED = TOPIC_PREFIX + "ASSET_LOCATION_UPDATED";

    /**
     * Topic used for when the sensor ID list of an asset has changed.
     * 
     * Contains the following properties:
     * <ul>
     * <li>{@link AssetModel#EVENT_PROP_UUID} - UUID of the asset</li>
     * </ul>
     */
    String TOPIC_ASSET_SENSOR_IDS_UPDATED = TOPIC_PREFIX + "ASSET_SENSOR_IDS_UPDATED";

    /**
     * Topic used for when an observation was created for an asset.
     * 
     * Contains the following properties:
     * <ul>
     * <li>{@link AssetModel#EVENT_PROP_UUID} - UUID of the asset</li>
     * </ul>
     */
    String TOPIC_ASSET_OBSERVATION_UPDATED = TOPIC_PREFIX + "ASSET_OBSERVATION_UPDATED";
    
    
    /**
     * Get assets for a controller.
     * @param controllerId
     *    the controller id to retrieve assets for
     * @return
     *    list of asset models that belong to the specified controller
     */
    List<AssetModel> getAssetsForControllerAsync(int controllerId);
    
    /**
     * Returns an asset model based on the asset UUID.
     * @param assetUuid
     *  UUID of the asset 
     * @param systemId
     *  the system id of the system the asset resides on
     * @return
     *  the asset model if found; otherwise null
     */
    AssetModel getAssetModelByUuid(UUID assetUuid, int systemId);
    
}
