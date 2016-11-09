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
import java.util.Map;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetDirectoryService.ScanResults;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.AssetScanner;
import mil.dod.th.core.factory.ProductType;
import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;

/**
 * Thread class to be used by the scan for new assets methods.
 * 
 * @author nmarcucci
 */
public class Scanner implements Runnable
{     
    /**
     * Asset scanner for the asset to be scanned.
     */
    private final AssetScanner m_Scanner;

    /**
     * Reference to the asset directory service using this class.
     */
    private final AssetDirectoryService m_AssetDirectoryService;

    /**
     * Listener to be informed of scan completion.
     */
    private final ScannerListener m_ScannerListener;

    /**
     * Create a scanner for a single asset type.
     * 
     * @param scanner
     *      asset scanner for the asset
     * @param assetDirectoryService
     *      reference to the asset directory service
     * @param listener
     *      listener to this scanner
     */
    public Scanner(final AssetScanner scanner,
            final AssetDirectoryService assetDirectoryService,
            final ScannerListener listener)
    {
        m_Scanner = scanner;
        m_AssetDirectoryService = assetDirectoryService;
        m_ScannerListener = listener;
    }

    /**
     * Method run called automatically when thread is spun off of main
     * thread.
     */
    @Override
    public void run() 
    {
        @SuppressWarnings("unchecked")
        final Class<? extends AssetProxy> productType =
                (Class<? extends AssetProxy>)m_Scanner.getClass().getAnnotation(ProductType.class).value();
        
        Logging.log(LogService.LOG_INFO, "Started thread to scan for assets of type [%s]", 
                productType.getCanonicalName());
        
        m_ScannerListener.scannerStartedScanning(productType);
        
        final List<ScanResultsData> foundAssets = new ArrayList<>();
        final ScanResults scanResults = new ScanResults()
        {
            @Override
            public void found(final String newAssetName, final Map<String, Object> newAssetProperties)
            {
                foundAssets.add(new ScanResultsData(newAssetName, newAssetProperties));
            }
        };

        try
        {
            m_Scanner.scanForNewAssets(scanResults, m_AssetDirectoryService.getAssetsByType(productType.getName()));
        }
        catch (final AssetException e)
        {
            Logging.log(LogService.LOG_WARNING, e, "Could not scan for new assets of type %s", 
                    productType.getName());
        }

        m_ScannerListener.scanCompleteForType(productType, foundAssets);
    }
}
