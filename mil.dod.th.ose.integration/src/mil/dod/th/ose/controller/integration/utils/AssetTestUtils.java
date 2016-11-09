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
package mil.dod.th.ose.controller.integration.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jdo.Query;

import org.osgi.service.log.LogService;

import com.google.common.base.Joiner;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.controller.integration.api.EventHandlerSyncer;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;
import mil.dod.th.ose.shared.JdoDataStore;

/**
 * Contains functions that are common to all Asset related integration tests.
 * @author nickmarcucci
 *
 */
public class AssetTestUtils
{
    /**
     * Private Constructor.
     */
    private AssetTestUtils()
    {
        
    }
    
    /**
     * Retrieves the {@link AssetDirectoryService}.
     * @return
     *  the asset directory service
     */
    public static AssetDirectoryService getAssetDirectoryService()
    {
        final AssetDirectoryService assetDirectoryService = 
                IntegrationTestRunner.getService(AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        return assetDirectoryService;
    }
    
    /**
     * Creates an asset of the specified class through the {@link AssetDirectoryService}. 
     * @param productType
     *  the class of the asset that is to be created.
     * @return
     *  the asset that has been created.
     */
    public static Asset createTestableAsset(final String productType)
        throws IllegalArgumentException, AssetException
    {
        final AssetDirectoryService assetDirectoryService = AssetTestUtils.getAssetDirectoryService();
        
        final Asset asset = assetDirectoryService.createAsset(productType);
        
        assertThat(asset, notNullValue());
        
        return asset;
    }
    
    /**
     * Creates an asset of specified class with specified properties set.
     * 
     * @param productType
     *  the class of the asset that is to be created.
     * @param props
     *  the properties to be set.
     * @return
     *  the asset that has been created.
     */
    public static Asset createTestableAssetWithProps(final String productType, 
            final Map<String, Object> props) throws IllegalArgumentException, AssetException, 
            IllegalStateException, FactoryException
    {
        final Asset asset = createTestableAsset(productType);
        asset.setProperties(new Hashtable<String, Object>(props));
        
        return asset;
    }
    
    /**
     * Creates an asset of the specified class through the {@link AssetDirectoryService}. Once created the 
     * asset will be activated.
     * @param productType
     *  the class of the asset that is to be created.
     * @param timeout
     *      how long to wait for the asset to activate 
     * @return
     *  the asset that has been created.
     */
    public static Asset createTestableActivatedAsset(final String productType, final int timeout)
        throws IllegalArgumentException, AssetException, InterruptedException
    {
        final Asset asset = createTestableAsset(productType);
        
        assertThat(asset, notNullValue());
        
        activateAsset(asset, timeout);

        return asset;
    }
    
    /**
     * Creates an asset of the specified class through the {@link AssetDirectoryService}. Once created the 
     * asset will be activated.
     * @param productType
     *      the class of the asset that is to be created.
     * @param props
     *      the properties to be set.
     * @param timeout
     *      how long to wait for the asset to activate 
     * @return
     *  the asset that has been created.
     */
    public static Asset createTestableActivatedAsset(final String productType, 
        final Map<String,Object> props, final int timeout) throws IllegalArgumentException, 
        AssetException, InterruptedException, IllegalStateException, FactoryException
    {
        final Asset asset = createTestableAssetWithProps(productType, props);
        assertThat(asset, is(notNullValue()));
        
        activateAsset(asset, timeout);
        
        return asset;
    }
    
    /**
     * Retrieves an asset's {@link AssetFactory}.
     * @param productType
     *  the asset factory that needs to be matched on.
     * @return
     *  the {@link AssetFactory} that corresponds to the factory class passed in.
     */
    public static AssetFactory grabAssetFactoryByClass(final String productType)
    {
        final AssetDirectoryService assetDirectoryService = getAssetDirectoryService();
        
        final Iterator<AssetFactory> factoryList = 
                assetDirectoryService.getAssetFactories().iterator();
        
        AssetFactory foundFactory = null;
        
        while (factoryList.hasNext())
        {
            final AssetFactory assetFactory = factoryList.next();
            if (assetFactory.getProductType().equals(productType))
            {
                foundFactory = assetFactory;
                break;
            }
        }
        
        assertThat(foundFactory, notNullValue());
        
        return foundFactory;
    }
    
    /**
     * Verifies the asset of the specified class can be activated and deactivated.
     * @param productType
     *  the class of the asset that needs to be instantiated.
     * @param timeout
     *  how long to wait for the activate and deactivate events
     * @return
     *  the asset that was created as a result of checking if asset could be activated or deactivated.
     */
    public static Asset verifyActivateDeactivateAsset(
            final String productType, final int timeout)
        throws IllegalArgumentException, AssetException, InterruptedException
    {
        final Asset assetToVerify = createTestableActivatedAsset(productType, timeout);
        
        deactivateAsset(assetToVerify);

        return assetToVerify;
    }
    
    /**
     * Verifies the asset of the specified class can be activated and deactivated.
     * @param productType
     *  the class of the asset that needs to be instantiated.
     * @param timeout
     *  how long to wait for the activate and deactivate events
     * @param props
     *  properties to be passed to the asset at the time of creation
     * @return
     *  the asset that was created as a result of checking if asset could be activated or deactivated.

     */
    public static Asset verifyActivateDeactivateAsset(final String productType, 
            final int timeout, final Map<String, Object> props) throws IllegalArgumentException, AssetException, 
                InterruptedException, IllegalStateException, FactoryException
    {
        final Asset assetToVerify = createTestableActivatedAsset(productType, props, timeout);

        deactivateAsset(assetToVerify);

        return assetToVerify;
    }
    
    /**
     * Activate the given asset.
     * @param asset
     *      the asset to activate
     * @param timeoutSecs
     *      how long to wait for the asset to activate 
     */
    public static void activateAsset(final Asset asset, final int timeoutSecs)
    {
        //create a syncer that will listen for the activated event
        final EventHandlerSyncer actSyncer = new EventHandlerSyncer(Asset.TOPIC_ACTIVATION_COMPLETE, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid()));
        
        //activate
        asset.activateAsync();
        
        //listen
        actSyncer.waitForEvent(timeoutSecs);
        
        assertThat(asset.getActiveStatus(), is(Asset.AssetActiveStatus.ACTIVATED));
    }
    
    /**
     * Deactivate the given asset.
     * @param asset
     *      the asset to deactivate
     */
    public static void deactivateAsset(final Asset asset)
    {
        deactivateAsset(asset, 10);
    }
    
    /**
     * Deactivate the given asset.
     * @param asset
     *      the asset to deactivate
     * @param timeout
     *      the length of time to wait
     */
    public static void deactivateAsset(final Asset asset, final int timeout)
    {
        //create a syncer that will listen for the deactivated event
        final EventHandlerSyncer deactSyncer = new EventHandlerSyncer(Asset.TOPIC_DEACTIVATION_COMPLETE, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid()));
        
        //deactivate
        asset.deactivateAsync();

        //listen
        deactSyncer.waitForEvent(timeout);

        assertThat(asset.getActiveStatus(), is(Asset.AssetActiveStatus.DEACTIVATED));
    }
    
    /**
     * Attempts to remove the given asset. If unable to remove, logs reason and returns.
     * 
     * @param asset
     *      the asset to remove
     */
    public static void tryDeleteAsset(final Asset asset)
    {
        try
        {
            asset.delete();
        }
        catch (Exception e)
        {            
            Logging.log(LogService.LOG_WARNING, e, "Unable to remove asset [%s].", asset.getName());
        }
    }
    
    /**
     * Wait for a specified amount of time for the specified status change event. Will only wait if the status
     * is not the specified status. (The current status is checked.)
     * @param asset
     *      the asset to expect the status change from
     * @param status
     *      the status to wait for the asset to change to
     * @param timeout
     *      the length of time to wait
     */
    public static void waitForStatusChange(final Asset asset, final SummaryStatusEnum status, final int timeout) 
        throws InterruptedException
    {
        //syncer for status
        final EventHandlerSyncer statusChangeSyncer = new EventHandlerSyncer(
                Asset.TOPIC_STATUS_CHANGED, String.format("(&(%s=%s)(%s=%s))", 
                    FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid().toString(),
                    Asset.EVENT_PROP_ASSET_STATUS_SUMMARY, status.toString()));

        final Observation statusObs = asset.getLastStatus();
        if (statusObs.getStatus().getSummaryStatus().getSummary() != status)
        {
            statusChangeSyncer.waitForEvent(timeout);
            
            //verify the status
            final Observation postStatusObs = asset.getLastStatus();
            assertThat(postStatusObs, is(notNullValue()));
            assertThat(postStatusObs.getStatus().getSummaryStatus().getSummary(), is(status));
        }
    }
    
    /**
     * Get the most recent observation from the given asset.
     * @param query
     *      query to use, but max to a single observation
     * @return observation
     *      the most recent observation from the asset
     */
    public static Observation getMostRecentObs(final ObservationQuery query)
    {
        final ObservationStore obsStore = IntegrationTestRunner.getService(ObservationStore.class);
        assertThat(obsStore, is(notNullValue()));
        
        //latest obs
        final Collection<Observation> observations = query.withMaxObservations(1).execute();
        assertThat(observations.isEmpty(), is(false));
        return observations.iterator().next();
    }
    
    /**
     * Get the most recent observation using the given filter.
     * 
     * @param filter
     *      Filter used when querying the data store for the specified observation.
     * @return
     *      Observation that matches the specified filter.
     */
    @SuppressWarnings("unchecked")
    public static Observation getObservationByFilter(final String filter) 
        throws IllegalArgumentException, IllegalAccessException
    {
        final JdoDataStore<Observation> obsStore = 
                (JdoDataStore<Observation>)IntegrationTestRunner.getService("obsDataStore");
        assertThat(obsStore, is(notNullValue()));
        Query query = obsStore.newJdoQuery();
        query.setFilter(filter);
        query.setOrdering("createdTimestamp descending");
        query.compile();
        
        final Collection<Observation> observations = obsStore.executeJdoQuery(query);
        assertThat(observations.isEmpty(), is(false));
        return observations.iterator().next();
    }
    
    /**
     * Deletes all assets from the controller. Try to deactivate the asset first if it is currently activating or 
     * activated.
     */
    public static void deleteAsset(Asset asset)    
    {
        //Wait for asset to finish activating
        EventHandlerSyncer activationListener = new EventHandlerSyncer(Asset.TOPIC_ACTIVATION_COMPLETE,
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid().toString()));

        if (asset.getActiveStatus() == AssetActiveStatus.ACTIVATING)
        {              
            activationListener.waitForEvent(10);
        }

        if (asset.getActiveStatus() == AssetActiveStatus.ACTIVATED)
        {
            deactivateAsset(asset);
        }
        
        asset.delete();
    }
    
    /**
     * Deletes all assets from the controller by calling {@link #deleteAsset(Asset)} for each asset known to the 
     * {@link AssetDirectoryService}. This should used for final cleanup and THROWs an exception at the end, with all
     * the asset names that failed to be removed.
     * @exception IllegalStateException
     *  if one or more assets fail to be removed
     */
    public static void deleteAllAssets() throws IllegalStateException
    {
        AssetDirectoryService assetDirectoryService = IntegrationTestRunner.getService(AssetDirectoryService.class);
        final List<String> errAssetNames = new ArrayList<>();
        boolean didError = false;
        for (Asset asset : assetDirectoryService.getAssets())
        {
            try
            {
                deleteAsset(asset);
            }
            catch (Exception e)
            {
                didError = true;
                errAssetNames.add(asset.getName() + ", ");
                Logging.log(LogService.LOG_ERROR, "Unable to remove asset %s", asset.getName());
            }
        }
        if (didError)
        {
            throw new IllegalStateException("One or more assets failed to be removed, their names are: " 
                    + Joiner.on(',').join(errAssetNames));
        }
    }
}
