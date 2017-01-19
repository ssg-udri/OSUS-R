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
package mil.dod.th.ose.integration.commons;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.Logging;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class AssetUtils
{
    /**
     * Activate the given asset.
     * @param asset
     *      the asset to activate
     * @param timeoutSecs
     *      how long to wait for the asset to activate 
     */
    public static void activateAsset(BundleContext context, Asset asset, int timeoutSecs)
    {
        //create a syncer that will listen for the activated event
        final EventHandlerSyncer actSyncer = new EventHandlerSyncer(context, Asset.TOPIC_ACTIVATION_COMPLETE, 
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
    public static void deactivateAsset(BundleContext context, Asset asset)
    {
        //create a syncer that will listen for the deactivated event
        final EventHandlerSyncer deactSyncer = new EventHandlerSyncer(context, Asset.TOPIC_DEACTIVATION_COMPLETE, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid()));
        final int timeout = 10;
        
        //deactivate
        asset.deactivateAsync();

        //listen
        deactSyncer.waitForEvent(timeout);

        assertThat(asset.getActiveStatus(), is(Asset.AssetActiveStatus.DEACTIVATED));
    }

    /**
     * Deletes all assets from the controller. Try to deactivate the asset first if it is currently activating or 
     * activated.
     */
    public static void deleteAsset(BundleContext context, Asset asset)    
    {
        //Wait for asset to finish activating
        EventHandlerSyncer activationListener = new EventHandlerSyncer(context, Asset.TOPIC_ACTIVATION_COMPLETE,
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid().toString()));

        if (asset.getActiveStatus() == AssetActiveStatus.ACTIVATING)
        {              
            activationListener.waitForEvent(10);
        }

        if (asset.getActiveStatus() == AssetActiveStatus.ACTIVATED)
        {
            deactivateAsset(context, asset);
        }
        
        asset.delete();
    }
    
    /**
     * Deletes all assets from the controller by calling {@link #deleteAsset(BundleContext, Asset)} for each asset known
     * to the {@link AssetDirectoryService}. This should used for final cleanup and throws an exception at the end, with
     * all the asset names that failed to be removed.
     * 
     * @exception IllegalStateException
     *  if one or more assets fail to be removed
     */
    public static void deleteAllAssets(BundleContext context) throws IllegalStateException
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(context, AssetDirectoryService.class);
        final List<String> errAssetNames = new ArrayList<>();
        boolean didError = false;
        for (Asset asset : assetDirectoryService.getAssets())
        {
            try
            {
                deleteAsset(context, asset);
            }
            catch (Exception e)
            {
                didError = true;
                errAssetNames.add(asset.getName() + ", ");
                Logging.log(LogService.LOG_ERROR, "Unable to delete asset %s", asset.getName());
            }
        }
        if (didError)
        {
            throw new IllegalStateException("One or more assets failed to be deleted, their names are: " 
                                            + errAssetNames);
        }
    }
}
