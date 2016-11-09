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
package mil.dod.th.ose.core.impl.ccomm;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import mil.dod.th.core.ccomm.AddressProxy;
import mil.dod.th.core.ccomm.AddressTranslator;
import mil.dod.th.core.factory.ProductType;
import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;

/**
 * This manager organizes and makes available {@link AddressTranslator} services.
 * @author allenchl
 *
 */
@Component (provide = AddressTranslatorManager.class)
public class AddressTranslatorManager
{
    /**
     * Map of address translators and their associated types.
     */
    private final BiMap<String, AddressTranslator> m_AddrTranslators;
    
    /**
     * Constructor for the {@link AddressTranslatorManager} component.
     */
    public AddressTranslatorManager()
    {
        m_AddrTranslators = Maps.synchronizedBiMap(HashBiMap.<String, AddressTranslator>create());
    }
    
    /**
     * Binding method for registered address translators. This method is called for each registered translator.
     * 
     * @param addressTranslator
     *      {@link AddressTranslator} instance to bind
     */
    @Reference(multiple = true, optional = true, dynamic = true)
    public void setAddressTranslator(final AddressTranslator addressTranslator)
    {
        final ProductType annotation = addressTranslator.getClass().getAnnotation(ProductType.class);
        if (annotation == null)
        {
            Logging.log(LogService.LOG_WARNING, 
                "Address translator [%s] is not properly annotated with the ProductType and"
                + " therefore will not be registered.", addressTranslator.getClass().getCanonicalName());
        }
        else
        {
            @SuppressWarnings("unchecked")
            final Class<? extends AddressProxy> addrType = (Class<? extends AddressProxy>) annotation.value();
            final String addrTypeName = addrType.getName();
            if (m_AddrTranslators.containsKey(addrTypeName))
            {
                Logging.log(LogService.LOG_ERROR, "Address translator for address type [%s] is already registered, "
                        + "the registered translator is [%s], and the translator that failed to register is [%s].", 
                        addrTypeName, 
                        m_AddrTranslators.get(addrTypeName).getClass().getCanonicalName(),
                        addressTranslator.getClass().getCanonicalName());
            }
            else
            {
                m_AddrTranslators.put(addrTypeName, addressTranslator);
                Logging.log(LogService.LOG_INFO, "Address translator registered for [%s]", addrTypeName);
            }
        }
    }

    /**
     * Unbind method for address translators. This method is called for each translator that is unregistered.
     * 
     * @param addressTranslator
     *      {@link AddressTranslator} instance to unbind
     */
    public void unsetAddressTranslator(final AddressTranslator addressTranslator)
    {
        m_AddrTranslators.inverse().remove(addressTranslator);
    }
    
    /**
     * Get the translator for the given {@link AddressProxy} product type, the product type passed must be equivalent
     * to {@link mil.dod.th.core.factory.FactoryDescriptor#getProductType()}.
     * @param addrProxyProductType
     *      the proxy type of the translator to be returned
     * @return 
     *      the appropriate translator based on the product type
     * @throws IllegalArgumentException
     *      if there is not a known translator for the given product type
     */
    public AddressTranslator getAddressTranslator(final String addrProxyProductType) 
            throws IllegalArgumentException
    {
        synchronized (m_AddrTranslators)
        {
            if (!m_AddrTranslators.containsKey(addrProxyProductType))
            {
                throw new IllegalArgumentException(String.format("No address translator exists for the type [%s].", 
                        addrProxyProductType));
            }
            return m_AddrTranslators.get(addrProxyProductType);
        }
    }
}
