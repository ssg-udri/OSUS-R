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

import java.util.Set;

import mil.dod.th.core.asset.AssetDirectoryService.ScanResults;

/**
 * Optional interface implemented by an asset plug-in if it is capable of finding assets available to the controller, 
 * such as, performing a port scan. The implementation of this interface must include the {@link 
 * mil.dod.th.core.factory.ProductType} annotation on the class definition so the scanner will be available for use.
 * 
 * @author dlandoll
 */
public interface AssetScanner
{
    /**
     * Scan for new assets and update the provided results object as they are found. The set of assets, that currently
     * exist in the asset directory, are provided here as a matter convenience (so new assets may be compared with
     * existing ones to ensure uniqueness). Do not add found assets to the {@link AssetDirectoryService}, only update 
     * the provided results object. It is assumed that threading will be handled outside of this method call, do not use
     * another thread to perform the scan. It is okay to take a while to scan for assets but this method should return 
     * in a reasonable amount of time (&lt;10 seconds). The provided results object will be become invalid and will no 
     * longer accept new assets once this method returns. If the scan fails, throw an {@link AssetException} with 
     * details.
     * 
     * @param results
     *            invoke {@link ScanResults#found(java.lang.String, java.util.Map)} for each asset found with properties
     *            defining the found asset. It is important that the results object is invoked immediately after an 
     *            asset is discovered so that higher level functionality remains responsive (e.g., a user interface).
     * @param existing
     *            an unmodifiable set of assets that currently exist in the {@link AssetDirectoryService}. This 
     *            parameter may be used to compare found assets with existing ones to ensure uniqueness before updating 
     *            the results
     * @throws AssetException
     *            if the scan fails
     */
    void scanForNewAssets(ScanResults results, Set<Asset> existing) throws AssetException;
}
