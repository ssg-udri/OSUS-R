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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.AssetScanner;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.ProductType;
import mil.dod.th.core.log.LoggingService;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;


/**
 * This class is in charge of spinning off the threads that scan for a particular asset type, using the
 * {@link AssetScanner} provided by the asset plug-in. Once it is determined that all threads are complete or they have
 * timed-out then the scan listener is notified with {@link ScannerManagerListener#allScannersCompleted()}.
 * 
 * @author nmarcucci
 * 
 */
@Component(provide = ScannerManager.class)
public class ScannerManager
{
    /**
     * Reference to the core logging service.
     */
    private LoggingService m_Logging;

    /**
     * Reference to the OSGi EventAdmin service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Map of asset scanners and their associated types as defined by 
     * {@link mil.dod.th.core.asset.AssetFactory#getProductType()}.
     */
    final private BiMap<String, AssetScanner> m_AssetScanners;

    /**
     * Constructor for the {@link ScannerManager} component.
     */
    public ScannerManager()
    {
        m_AssetScanners = Maps.synchronizedBiMap(HashBiMap.<String, AssetScanner>create());
    }

    /**
     * Binds the EventAdmin service.
     * 
     * @param eventAdmin
     *      EventAdmin service object
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }

    /**
     * Binds the logging service.
     * 
     * @param logging
     *      Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    /**
     * Binding method for registered asset scanners. This method is called for each registered asset scanner.
     * 
     * @param assetScanner
     *      {@link AssetScanner} instance to bind
     */
    @Reference(multiple = true, optional = true, dynamic = true)
    public void setAssetScanner(final AssetScanner assetScanner)
    {
        final ProductType annotation = assetScanner.getClass().getAnnotation(ProductType.class);
        if (annotation != null)
        {
            @SuppressWarnings("unchecked")
            final Class<? extends AssetProxy> assetType = (Class<? extends AssetProxy>) annotation.value();
            final String assetTypeName = assetType.getName();
            if (m_AssetScanners.containsKey(assetTypeName))
            {
                m_Logging.error("AssetScanner for asset type [%s] is already registered", assetType.getName());
            }
            else
            {
                m_AssetScanners.put(assetType.getName(), assetScanner);
            }
        }
    }

    /**
     * Unbind method for asset scanners. This method is called for each asset scanner that is unregistered.
     * 
     * @param assetScanner
     *      {@link AssetScanner} instance to unbind
     */
    public void unsetAssetScanner(final AssetScanner assetScanner)
    {
        m_AssetScanners.inverse().remove(assetScanner);
    }

    /**
     * Start a scan of all assets that have a registered {@link AssetScanner}. New assets are created using the
     * {@link AssetDirectoryService} reference.
     * 
     * @param assetDirService
     *            Reference to the {@link AssetDirectoryService}
     */
    public void scanForAllAssets(final AssetDirectoryService assetDirService)
    {
        final Runnable runnable = new ScannerManagerThread(new HashSet<>(m_AssetScanners.values()),
                assetDirService, new ScanListenerBridge(assetDirService));
        final Thread thread = new Thread(runnable);
        thread.setName(ScannerManagerThread.class.getName());
        thread.start();
    }

    /**
     * Start a scan for the given asset product type. It must have a registered {@link AssetScanner} associated with
     * that type. New assets are created using the {@link AssetDirectoryService} reference.
     * 
     * @param assetDirService
     *            Reference to the {@link AssetDirectoryService}
     * @param productType
     *            Asset product type to scan for as returned by 
     *            {@link mil.dod.th.core.asset.AssetFactory#getProductType()}
     */
    public void scanForAssetsByType(final AssetDirectoryService assetDirService, final String productType)
    {
        final AssetScanner assetScanner = m_AssetScanners.get(productType);
        if (assetScanner == null)
        {
            throw new IllegalArgumentException(String.format("No Scanner found for Asset type [%s]", productType));
        }
        else
        {
            final Set<AssetScanner> scannerList = new HashSet<>();
            scannerList.add(assetScanner);

            // kick off thread to scan for new Assets
            final Runnable runnable = new ScannerManagerThread(scannerList, assetDirService, new ScanListenerBridge(
                    assetDirService));
            final Thread thread = new Thread(runnable);
            thread.setName(ScannerManagerThread.class.getName());
            thread.start();
        }
    }

    /**
     * Provides a set of all asset types with registered asset scanners.
     * 
     * @return
     *      Set of all asset types with scanners
     */
    public Set<String> getScannableAssetTypes()
    {
        return new HashSet<>(m_AssetScanners.keySet());
    }

    /**
     * Post scanning related events.
     * 
     * @param topic
     *      Topic of the event as defined by {@link AssetDirectoryService} interface.
     * @param productType
     *      Type of the asset as returned by {@link mil.dod.th.core.asset.AssetFactory#getProductType()}
     */
    private void postEvent(final String topic, final Class<? extends AssetProxy> productType)
    {
        final Map<String, Object> props = new HashMap<>();
        if (productType != null)
        {
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, productType.getName());
        }

        m_EventAdmin.postEvent(new Event(topic, props));

        m_Logging.log(LogService.LOG_DEBUG, "%s posted event %s", this.getClass().getSimpleName(), topic);
    }

    /**
     * Inner class for listening to scanner thread events and responding accordingly with the directory service.
     * 
     * @author dhumeniuk
     *
     */
    private final class ScanListenerBridge implements ScannerManagerListener
    {
        /**
         * Reference to the asset directory service.
         */
        private final AssetDirectoryService m_AssetDirectoryService;
        
        /**
         * Constructor used for creating scanner manager callback listeners.
         * 
         * @param assetDirService
         *      Reference to the asset directory service
         */
        ScanListenerBridge(final AssetDirectoryService assetDirService)
        {
            m_AssetDirectoryService = assetDirService;
        }

        @Override
        public void scannerStartedScanning(final Class<? extends AssetProxy> productType)
        {
            postEvent(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS, productType);
        }

        @Override
        public void scanCompleteForType(final Class<? extends AssetProxy> productType,
                final List<ScanResultsData> scanResults)
        {
            for (ScanResultsData scanData : scanResults)
            {
                try
                {
                    m_AssetDirectoryService.createAsset(productType.getName(), scanData.getName(), 
                            scanData.getProperties());
                }
                catch (final Exception ex)
                {
                    m_Logging.log(LogService.LOG_ERROR, ex, "Error creating scanned Asset.");
                }
            }

            postEvent(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE, productType);

            m_Logging.log(LogService.LOG_INFO, "%d new Asset(s) found of the type %s", scanResults.size(),
                    productType.getName());
        }

        @Override
        public void allScannersCompleted()
        {
            postEvent(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE, null);
        }
    } 
}
