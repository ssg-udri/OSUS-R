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

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;

/**
 * <p>
 * Interface defines attributes of a single asset plug-in and can be retrieved through {@link Asset#getFactory()}.
 */
@ProviderType
public interface AssetFactory extends FactoryDescriptor
{
    /**
     * Same as {@link FactoryDescriptor#getCapabilities()}, but returns additional capability information specific to an
     * asset.
     * 
     * @return the asset's capabilities
     */
    AssetCapabilities getAssetCapabilities();
}
