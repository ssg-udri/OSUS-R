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
package mil.dod.th.ose.core;

import static org.mockito.Mockito.*;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressFactory;
import mil.dod.th.core.ccomm.capability.AddressCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.ose.core.factory.api.FactoryInternal;

/**
 * @author dhumeniuk
 *
 */
public class FactoryMocker
{
    private static void stubMethods(final FactoryDescriptor factory, 
            final Class<? extends FactoryObject> type)
    {
        final FactoryObjectProxy proxy;
        String proxyClassName = type.getName() + "Proxy";
        try
        {
            proxy = (FactoryObjectProxy)mock(Class.forName(proxyClassName));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to mock class: " + proxyClassName);
        }
        doReturn(proxy.getClass()).when(factory).getProductType();
        doReturn(proxy.getClass().getSimpleName()).when(factory).getProductName();
    }       
    
    /**
     * Mock an address factory that produces the given product type.
     * 
     * @param type
     *      product type that the factory will produce
     * @param prefix
     *      address prefix the factory uses, see {@link Address#getDescription()}
     */
    public static <T extends Address> AddressFactory mockAddressFactory(Class<T> type, String prefix)
    {
        AddressFactory factory = mock(FactoryInternal.class);
        stubMethods(factory, type);

        AddressCapabilities caps = new AddressCapabilities().withPrefix(prefix);
        when(factory.getCapabilities()).thenReturn(caps);
        
        return factory;
    }

    public static FactoryDescriptor mockFactoryDescriptor(String productType)
    {
        FactoryDescriptor descriptor = mock(FactoryDescriptor.class);
        when(descriptor.getProductType()).thenReturn(productType);
        
        return descriptor;
    }
    
    public static FactoryInternal mockFactoryInternal(String productType)
    {
        FactoryInternal internal = mock(FactoryInternal.class);
        when(internal.getProductType()).thenReturn(productType);
        
        return internal;
    }
}
