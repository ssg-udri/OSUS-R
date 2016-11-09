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

import java.util.List;

import mil.dod.th.core.asset.AssetProxy;

/**
 * Interface is used to listen to individual asset factory scanners.
 * 
 * @author dhumeniuk
 *
 */
public interface ScannerListener
{
    /**
     * Scanner thread has started scanning for a particular type of asset.
     * 
     * @param productType
     *      Product type of asset being scanned as returned by {@link 
     *      mil.dod.th.core.asset.AssetFactory#getProductType()}
     */
    void scannerStartedScanning(Class<? extends AssetProxy> productType);
    
    /**
     * Scanner has completed for a particular type.
     * 
     * @param productType
     *      Product type of asset being scanned as returned by {@link 
     *      mil.dod.th.core.asset.AssetFactory#getProductType()}
     * @param scanResults
     *      List of {@link ScanResultsData} that contains new asset configurations that were found but have
     *      not yet been created and added to directory
     */
    void scanCompleteForType(Class<? extends AssetProxy> productType, List<ScanResultsData> scanResults);
}
