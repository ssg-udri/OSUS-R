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

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * This interface serves as an Address Manager Service. Addresses may be looked up using specific parameters.
 * If an address meeting that specification is not found, a new one is created.
 * 
 * <p>
 * This service uses the {@link AddressProxy} implementations from bundle developers.  A proxy is considered available
 * once it has been registered as an OSGi {@link org.osgi.service.component.ComponentFactory} service. All previously 
 * created addresses will be restored by this service when the component factory is registered.
 * 
 * <p>
 * This is an OSGi service provided by the core and may be obtained by getting an OSGi service reference or using
 * declarative services.
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface AddressManagerService
{
    /**
     * Creates and adds an address to the service.
     * 
     * @param addressDescription
     *      string representation of an address as defined by {@link Address#getDescription()}
     * @return 
     *      address for given description, will only be created if address is not currently in lookup
     * @throws CCommException
     *      operational error related to the address
     * @throws IllegalArgumentException
     *      if the supplied description is improperly formed
     */
    Address getOrCreateAddress(String addressDescription) throws CCommException, IllegalArgumentException;
    
    /**
     * Creates and adds an address to the service with the given name.
     * 
     * @param addressDescription
     *      string representation of an address as defined by {@link Address#getDescription()}
     * @param name
     *      the name to give the address, name will be ignored if address is already created
     * @return 
     *      address for given description, will only be created if address is not currently in lookup
     * @throws CCommException
     *      operational error related to the address
     * @throws IllegalArgumentException
     *      if the supplied description is improperly formed
     */
    Address getOrCreateAddress(String addressDescription, String name) throws CCommException, IllegalArgumentException;

    /**
     * Synchronizes an incoming address and returns the result. The main purpose of this function is to allow the
     * address to be sync'd to the AddressManagerService. If the incoming address doesn't exist at all it is added to
     * the AddressManagerService and given the specified name. If the address does exist, one and only one result is 
     * always returned. The returned addressed is renamed to the specified name.
     * NOTE: Address is compared by content (using {@link Address#equalProperties}), not name.
     * 
     * @param addressType
     *      the incoming address type to sync as returned by {@link AddressFactory#getProductType()}
     * @param name
     *      the name to give the address, name will be ignored if address is already created
     * @param properties
     *      properties associated with the address to match on, all properties inspected by {@link 
     *      Address#equalProperties} must be set
     * @return 
     *      address for given description, will only be created if address is not currently in lookup
     * @throws CCommException
     *      operational error related to the address 
     */
    Address getOrCreateAddress(String addressType, String name, Map<String, Object> properties) throws CCommException;
    
    /**
     * Checks to see if the passed address is already located in the service.
     * 
     * @param addressDescription
     *      string representation of an address as defined by {@link Address#getDescription()}
     * @return 
     *      true if the address was found, false if not
     * @throws IllegalArgumentException
     *      if the supplied description is improperly formed
     */
    boolean checkAddressAlreadyExists(String addressDescription) throws IllegalArgumentException;  

    /**
     * Remove all entries from the service.
     */
    void flush();

    /**
     * Returns a list of address strings currently in the AddressManagerService.
     * 
     * @return 
     *      the List of address descriptors.
     */
    List<String> getAddressDescriptiveStrings();

    /**
     * This is a debugging hook to print the status of this service to the given {@link PrintStream}.
     * 
     * @param printStream
     *      the output stream.
     */
    void printDeep(PrintStream printStream);
}
