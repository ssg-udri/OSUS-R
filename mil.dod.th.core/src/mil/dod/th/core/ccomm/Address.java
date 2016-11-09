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

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Address which identifies an end point to be used by bundles for communications.
 * 
 * <p>
 * All functionality of an address needed by the consumer should be available through this interface. It should never be
 * necessary to use the {@link AddressProxy} interface implemented by the plug-in. For example, if SomeAddress 
 * implements {@link AddressProxy}, consumers should not directly access SomeAddress methods, but instead access this 
 * interface.
 * 
 * <p>
 * Instances of an Address are managed (created, tracked, deleted) by the core. This interface should never be 
 * implemented by a plug-in. Instead, a plug-in implements {@link AddressProxy} to define custom behavior that is 
 * invoked when consumers use this interface. To interact with an address, use the {@link AddressManagerService}.
 */
@ProviderType
public interface Address extends FactoryObject
{
    /** 
     * Each {@link AddressProxy} implementation must provide a {@link org.osgi.service.component.ComponentFactory} with 
     * the factory attribute set to this constant.
     * 
     * <p>
     * For example:
     * 
     * <pre>
     * {@literal @}Component(factory = Address.FACTORY)
     * public class MyAddress implements AddressProxy
     * {
     *     ...
     * </pre>
     */
    String FACTORY = "mil.dod.th.core.ccomm.Address";
    
    /** Prefix for source address event properties. */
    String EVENT_PROP_SOURCE_ADDRESS_PREFIX = "source.";
    
    /** Prefix for source address event properties. */
    String EVENT_PROP_DEST_ADDRESS_PREFIX = "dest.";
    
    /** Event property key suffix for the address. */
    String EVENT_PROP_ADDRESS_SUFFIX = "addr";

    /** Event property key suffix for the address type as returned by {@link AddressFactory#getProductType()}. */
    String EVENT_PROP_ADDRESS_TYPE_SUFFIX = "addr.type";

    /** Event property key suffix for the address logical name. */
    String EVENT_PROP_ADDRESS_NAME_SUFFIX = "addr.name";
    
    /** Event property key suffix for uniquely identifying an address from which messages are sent or received. 
     * The value associate with the key comes from {@link #getDescription()}.*/
    String EVENT_PROP_MESSAGE_ADDRESS_SUFFIX = "addr.desc";

    /**
     * Get a set of event properties based on the prefix for the address. Additional properties can be added before 
     * sending out.
     * 
     * <p>
     * Contains the following properties:
     * <ul>
     * <li>{@link #EVENT_PROP_ADDRESS_SUFFIX}</li>
     * <li>{@link #EVENT_PROP_ADDRESS_TYPE_SUFFIX}</li>
     * <li>{@link #EVENT_PROP_ADDRESS_NAME_SUFFIX}</li>
     * <li>{@link #EVENT_PROP_MESSAGE_ADDRESS_SUFFIX}</li>
     * </ul>
     * </p>
     * 
     * @param prefix
     *      string to prepend to each address, if prefix is "source." address property will be "source.addr"
     * @return Map of basic event properties.
     */
    Map<String, Object> getEventProperties(final String prefix);
    
    /**
     * Determine if an Address is equivalent to the properties of another address. Note that
     * only specific properties are checked; this is unique to each implementation of Address.
     * @param properties
     *              The properties of the desired address
     * @return
     *              Return true if the address properties match
     */
    boolean equalProperties(final Map<String, Object> properties);
    
    /**
     * Method that returns the concatenation of the address prefix and a unique address defined for 
     * a particular address type.  Specifically, the concatenation is as follows: <code>PREFIX ":" SUFFIX</code>
     * where <code>PREFIX</code> is defined by {@link mil.dod.th.core.ccomm.capability.AddressCapabilities#getPrefix()} 
     * and <code>SUFFIX</code> is defined by {@link AddressProxy#getAddressDescriptionSuffix()}.
     * 
     * @return
     *      String representation of the address prefix and defined address format
     */
    String getDescription();
    
    /**
     * Same as {@link FactoryObject#getFactory()}, but returns the address specific factory.
     * 
     * @return
     *      factory for the address
     */
    @Override
    AddressFactory getFactory();
}
