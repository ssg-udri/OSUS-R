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

package mil.dod.th.core.ccomm;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.ccomm.capability.AddressCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;

/**
 * <p>
 * Interface defines attributes of a single address plug-in and can be retrieved through {@link Address#getFactory()}.
 */
@ProviderType
public interface AddressFactory extends FactoryDescriptor
{
    /**
     * Property set for each registered address factory service containing the address prefix.
     */
    String ADDRESS_FACTORY_PREFIX_SERVICE_PROPERTY = FactoryObject.TH_PROP_PREFIX + ".factory.address.prefix";

    /**
     * Same as {@link FactoryDescriptor#getCapabilities()}, but returns additional capability information specific to an
     * address.
     * 
     * @return the address's capabilities
     */
    AddressCapabilities getAddressCapabilities();
}
