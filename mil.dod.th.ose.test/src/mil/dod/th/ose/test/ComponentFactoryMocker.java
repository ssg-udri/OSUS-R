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

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.mockito.Mockito;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * @author Dave Humeniuk
 *
 */
public class ComponentFactoryMocker
{
    /**
     * Mock component instances for a {@link ComponentFactory}.  Will mock the backing object as well as the 
     * {@link ComponentInstance}.  Factory will return each instance created when {@link 
     * ComponentFactory#newInstance(Dictionary)} is called up to the <code>instanceCount</code>.
     * 
     * @param <T>
     *      type that the factory creates
     * @param clazz
     *      type that the factory creates
     * @param factory
     *      factory to mock
     * @param instanceCount
     *      how many instances to mock, will throw {@link IndexOutOfBoundsException} if factory tries to create more
     */
    @SuppressWarnings("unchecked")
    public static <T> List<ComponentInfo<T>> mockComponents(Class<T> clazz, ComponentFactory factory, 
            final int instanceCount)
    {
        NewInstanceAnswer<T> mocker = new NewInstanceAnswer<T>(clazz, instanceCount);
        when(factory.newInstance(Mockito.any(Dictionary.class))).thenAnswer(mocker);
        
        List<ComponentInfo<T>> components = new ArrayList<>();
        
        for (int i=0; i < instanceCount; i++)
        {
            ComponentInfo<T> info = new ComponentInfo<T>(factory, mocker.getComponentInstances()[i], 
                    (T)mocker.getComponentInstances()[i].getInstance());
            components.add(info);
        }
        
        return components;
    }
    
    /**
     * Mock a single component instance for a {@link ComponentFactory}. Will mock the backing object as well as the
     * {@link ComponentInstance}.
     * 
     * @param <T>
     *      type that the factory creates
     * @param clazz
     *      type that the factory creates
     * @param factory
     *      factory to mock
     * @return
     *      the mocked component instance
     */
    public static <T> ComponentInfo<T> mockSingleComponent(Class<T> clazz, ComponentFactory factory)
    {
        return mockComponents(clazz, factory, 1).get(0);
    }
    
    public static final class ComponentInfo<T>
    {
        private final ComponentInstance m_Instance;
        private final T m_Object;
        private final ComponentFactory m_Factory;
        
        public ComponentInfo(ComponentFactory factory, ComponentInstance instance, T object)
        {
            m_Factory = factory;
            m_Instance = instance;
            m_Object = object;
        }

        public ComponentFactory getFactory()
        {
            return m_Factory;
        }
        
        public ComponentInstance getInstance() 
        {
            return m_Instance;
        }
        
        public T getObject()
        {
            return m_Object;
        }
    }
}
