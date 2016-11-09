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
package mil.dod.th.ose.test;

import static org.mockito.Mockito.*;

import java.util.UUID;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Mocks {@link FactoryObject}s.
 */
public class FactoryObjectMocker
{
    public static <T extends FactoryObject> T mockFactoryObject(Class<T> type, String pid)
    {
        T object = mock(type);
        
        FactoryDescriptor factory;
        String factoryClassName = type.getName() + "Factory";
        try
        {
            factory = (FactoryDescriptor)mock(Class.forName(factoryClassName));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to mock class: " + factoryClassName);
        }
        
        when(object.getPid()).thenReturn(pid);
        when(object.getUuid()).thenReturn(UUID.randomUUID());
        when(object.getFactory()).thenReturn(factory);
        
        when(factory.getProductType()).thenReturn("product-type");
        
        return object;
    }
}
