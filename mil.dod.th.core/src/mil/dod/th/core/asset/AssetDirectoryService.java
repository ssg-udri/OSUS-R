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

package mil.dod.th.core.asset;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.ProviderType;

/**
 * <p>
 * The primary job of the Asset Directory Service is to manage the life-cycle of all {@link Asset} instances.
 * 
 * <p>
 * This service uses the {@link AssetProxy} implementations from bundle developers.  A proxy is considered available
 * once it has been registered as an OSGi {@link org.osgi.service.component.ComponentFactory} service. All previously 
 * created assets will be restored by this service when the component factory is registered.
 * 
 * <p>
 * This is an OSGi service provided by the core and may be obtained by getting an OSGi service reference or using
 * declarative services.
 * 
 * <p>
 * This service allows clients to,
 * </p>
 * <ul>
 * <li>get a set of existing assets</li>
 * <li>get a set of available asset factories</li>
 * <li>request an asset to be created</li>
 * <li>and request to scan the system for new assets (and listen for results)</li>
 * </ul>
 * 
 */
@ProviderType
public interface AssetDirectoryService
{
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/asset/AssetDirectoryService/";
    
    /** Topic used for when a factory is starts scanning for assets. */
    String TOPIC_SCANNING_FOR_ASSETS = TOPIC_PREFIX + "SCANNING_FOR_ASSETS";
    
    /** 
     * Topic used for when a {@link AssetScanner} has completed scanning for assets or when all {@link AssetScanner}s 
     * have completed.
     */
    String TOPIC_SCANNING_FOR_ASSETS_COMPLETE = TOPIC_PREFIX + "SCANNING_FOR_ASSETS_COMPLETE";
    
    /**
     * Same as {@link #createAsset(String, String)}, but use a default name.
     * 
     * @param productType
     *      Fully qualified class name of the type of asset to be created as returned by {@link 
     *      AssetFactory#getProductType()}.
     * @return
     *      The newly created asset.
     * @throws AssetException
     *      If the directory is unable to add the give asset.
     * @throws IllegalArgumentException
     *      If invalid product type name is given.
     */
    Asset createAsset(String productType) throws AssetException, IllegalArgumentException;
    
    /**
     * Same as {@link #createAsset(String, String, Map)}, but do not specify any properties.
     * 
     * @param productType
     *      Fully qualified class name of the type of asset to be created as returned by {@link 
     *      AssetFactory#getProductType()}.
     * @param name
     *      The name to give to the asset. If the name is a duplicate of another asset's name then an exception
     *      will be thrown and the asset will not be created.
     * @return
     *      The newly created asset
     * @throws AssetException
     *      If the directory is unable to add the give asset.
     * @throws IllegalArgumentException
     *      If invalid product type name is given.
     */
    Asset createAsset(String productType, String name) throws AssetException, IllegalArgumentException;

    /**
     * Create, name, set properties and add a new asset to the directory given a product type. This directory will 
     * create a new asset instance of the specified type and return it if one could be created.
     * 
     * @param productType
     *      Fully qualified class name of the type of asset to be created as returned by {@link 
     *      AssetFactory#getProductType()}.
     * @param name
     *      The name to give to the asset. If the name is a duplicate of another asset's name then an exception
     *      will be thrown and the asset will not be created.
     * @param properties
     *      Properties to use for the new {@link Asset}. Only properties that override the defaults need to be included.
     * @return
     *      The newly created asset.
     * @throws AssetException
     *      If the directory is unable to add the give asset.
     * @throws IllegalArgumentException
     *      If invalid asset type is given or a duplicate name is given.
     */
    Asset createAsset(String productType, String name, Map<String, Object> properties)
            throws AssetException, IllegalArgumentException;

    /**
     * Get a set of factories that describe asset plug-ins registered with the directory service. 
     * 
     * <p>
     * As asset plug-ins come and go via services, once the set has been retrieved, it may not accurately reflect the 
     * current state of the directory.
     * 
     * @return Set of all Asset factory descriptors.
     */
    Set<AssetFactory> getAssetFactories();

    /**
     * Get an unmodifiable set of assets that exist in this directory. To add or remove assets to or from this
     * directory, use {@link #createAsset} or {@link Asset#delete()}.
     * 
     * @return The set of all Assets registered, set should be empty of in no asset, not null 
     */
    Set<Asset> getAssets();

    /**
     * Get an unmodifiable set of assets that are of a particular product type.
     * 
     * @param productType
     *            Product type to filter on as returned by {@link AssetFactory#getProductType()}.
     * @return The set of Assets matching the given class, set should be empty of in no asset, not null
     */
    Set<Asset> getAssetsByType(String productType);

    /**
     * Get the asset by its name.
     * 
     * @param name
     *      name of the asset as returned by {@link Asset#getName()}
     * @return
     *      asset with the given name
     * @throws IllegalArgumentException
     *      if the name is not for a known asset
     */
    Asset getAssetByName(String name) throws IllegalArgumentException;
    
    /**
     * Check if an asset with the given name is in the directory.
     * 
     * @param name
     *      name of the asset
     * @return
     *      true if the directory contains the asset, false otherwise
     */
    boolean isAssetAvailable(String name);
    
    /**
     * Get the asset by its universally unique identifier.
     * 
     * @param uuid
     *      universally unique identifier
     * @return
     *      asset with the unique identifier
     * @throws IllegalArgumentException
     *      if the id is not for a known asset
     */
    Asset getAssetByUuid(UUID uuid) throws IllegalArgumentException;

    /**
     * Request the asset service to scan the system for any new assets. Each provided {@link AssetScanner} shall be
     * instructed to scan for new assets, see
     * {@link AssetScanner#scanForNewAssets(AssetDirectoryService.ScanResults, Set)}. An event with the topic
     * {@link #TOPIC_SCANNING_FOR_ASSETS} will be posted when each asset scanner starts scanning. Each scanner may take
     * some time (&lt; 10 seconds) so they should be started on separate threads. The service should ensure each scanner
     * completes scanning or times out.
     * 
     * <p>
     * If any assets are discovered as a result of calling this method, the service shall post the
     * {@link AssetFactory#TOPIC_FACTORY_OBJ_CREATED} event.
     * </p>
     * 
     * This is an asynchronous request and an event with the {@link #TOPIC_SCANNING_FOR_ASSETS_COMPLETE} topic will be
     * posted when scanning is complete for each asset scanner and when complete for all scanners. Scanning will
     * complete if all scanners have completed or a timeout occurs.
     */
    void scanForNewAssets();

    /**
     * Request the asset service to scan the system for any new assets of the given product type. The
     * {@link AssetScanner} associated with the given type shall be instructed to scan for new assets (
     * {@link AssetScanner#scanForNewAssets(AssetDirectoryService.ScanResults, Set)}). An event with the topic
     * {@link #TOPIC_SCANNING_FOR_ASSETS} will be posted when the asset scanner starts scanning.
     * 
     * <p>
     * If any assets are discovered as a result of calling this method, the service shall post the
     * {@link AssetFactory#TOPIC_FACTORY_OBJ_CREATED} event.
     * </p>
     * 
     * <p>
     * This is an asynchronous request and an event with the {@link #TOPIC_SCANNING_FOR_ASSETS_COMPLETE} topic will be
     * posted when scanning is complete. Scanning will complete when the scanner has finished or a timeout occurs.
     * </p>
     * 
     * @param productType
     *            The type of asset as returned by {@link AssetFactory#getProductType()}
     * @throws IllegalArgumentException
     *             If invalid asset type is given or the asset type does not have an {@link AssetScanner}
     */
    void scanForNewAssets(String productType) throws IllegalArgumentException;

    /**
     * Get the set of asset types with registered asset scanners. This includes assets that implement the
     * {@link AssetScanner} interface. Types returned can then be passed to {@link #scanForNewAssets(String)}.
     * 
     * @return 
     *      The set of asset types with an associated asset scanner where asset type as returned by {@link 
     *      AssetFactory#getProductType()}
     */
    Set<String> getScannableAssetTypes();
    
    /**
     * The asset directory service uses this interface to discover new assets during an asset scan.
     */
    interface ScanResults
    {        
        /**
         * The {@link AssetScanner} should call this method when it discovers a new asset configuration during a
         * requested scan.
         * 
         * @param newAssetName
         *            Name of the new asset or null if a default name should be assigned to the asset
         * @param newAssetProperties
         *            Properties that define the found asset or null if no properties
         */
        void found(String newAssetName, Map<String, Object> newAssetProperties);
    }
}
