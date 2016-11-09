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

import java.util.Map;

/**
 * Each address defined must have a unique description string as returned by {@link Address#getDescription()} where the
 * description must be unique across different address plug-ins. Therefore, each description string has system unique
 * prefix for each plug-in defined by {@link mil.dod.th.core.ccomm.capability.AddressCapabilities#getPrefix()}. The
 * suffix is then parsed by the plug-in through this interface and converted back to a string by {@link 
 * AddressProxy#getAddressDescriptionSuffix()}. The prefix and suffix is delimited by a colon.
 * 
 * <p>
 * This interface must be implemented by an address plug-in to help translate the address description to an address 
 * object. The implementation of this interface must include the {@link mil.dod.th.core.factory.ProductType} annotation 
 * to associate with the correct address type.
 * 
 * @author dhumeniuk
 *
 */
public interface AddressTranslator
{
    /**
     * Get properties for an {@link mil.dod.th.core.ccomm.Address} from the provided description suffix. The supplied
     * string will already have the prefix and delimiter striped, passing only the suffix. For example, if the address
     * description is "prefix:1.2.3.4", then this method will be passed "1.2.3.4".
     * 
     * @param addressDescriptionSuffix
     *      the human-readable description of the address uniquely describing an address
     * @return
     *      properties that uniquely define an address given the provided description
     * @throws CCommException
     *      if the provided input in not in the correct syntax
     *          
     * @see Address#getDescription()
     */
    Map<String, Object> getAddressPropsFromString(String addressDescriptionSuffix) throws CCommException;
}
