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

import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;

/**
 * An address plug-in implements this interface providing any custom behavior of the plug-in that is needed to describe 
 * the end point and is meaningful to a particular {@link mil.dod.th.core.ccomm.link.LinkLayer} or {@link 
 * mil.dod.th.core.ccomm.transport.TransportLayer}.
 * 
 * <p>
 * Primarily, this interface provides the suffix of the address description through {@link 
 * #getAddressDescriptionSuffix()}. This allows {@link Address} to provide the full description based on the prefix.
 * 
 * <p>
 * This implementation class must be used as the value for the {@link mil.dod.th.core.factory.ProductType} annotation 
 * for the {@link AddressTranslator} implementation.
 * 
 * @author dhumeniuk
 */
public interface AddressProxy extends FactoryObjectProxy
{
    /**
     * Called to initialize the object and provide the plug-in with the {@link AddressContext} to interact with the
     * rest of the system.
     * 
     * @param context
     *      context specific to the {@link Address} instance
     * @param props
     *      the address's configuration properties, available as a convenience so {@link 
     *      AddressContext#getProperties()} does not have to be called
     * @throws FactoryException
     *      if there is an error initializing the object.
     */
    void initialize(AddressContext context, Map<String, Object> props) throws FactoryException;
    
    /**
     * Determines if the {@link Address} instance is equivalent to the properties of another address. Only properties
     * related to a specific {@link Address} implementation should be checked.
     * 
     * @param properties
     *      The properties of the desired address
     * @return true if the address properties match
     */
    boolean equalProperties(Map<String, Object> properties);
    
    /**
     * Returns the string representation of the address without the factory specific prefix.
     * 
     * @return
     *      message address without the prefix
     */
    String getAddressDescriptionSuffix();
}
