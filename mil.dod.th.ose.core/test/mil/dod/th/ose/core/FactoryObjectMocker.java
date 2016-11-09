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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.ose.core.factory.api.AbstractFactoryObject;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;

import org.mockito.MockSettings;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.cm.Configuration;

/**
 * @author dhumeniuk
 *
 */
public class FactoryObjectMocker
{
    /**
     * Used to keep track of the name for each object if set.
     */
    private static Map<FactoryObject, String> m_NameMap = new HashMap<>();
    
    /**
     * Same as {@link #mockFactoryObject(Class, String, Config)} passing null for the config.
     */
    public static <T extends FactoryObject> T mockFactoryObject(Class<T> type, String pid)
    {
        return mockFactoryObject(type, pid, null);
    }
    
    /**
     * Mock a factory object of the given type including the mocking of its factory.
     * 
     * @param type
     *      type of factory object to mock
     * @param pid
     *      configuration PID to use for the factory object
     * @param config
     *      extra configuration to give to the factory object or null if no configuration
     * @return
     *      the mocked factory object
     */
    public static <T extends FactoryObject> T mockFactoryObject(Class<T> type, 
            String pid, final Config config)
    {
        MockSettings settings = withSettings().extraInterfaces(FactoryObjectInternal.class);
        if (config != null && config.getExtraInterfaces().length > 0)
        {
            settings.extraInterfaces(config.getExtraInterfaces());
        }
            
        T object = mock(type, settings);
        when(object.getUuid()).thenReturn(UUID.randomUUID());
        when(object.getPid()).thenReturn(pid);
        when(object.getName()).thenReturn(pid + "-name");
        
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

        String proxyClassName = type.getName() + "Proxy";
        doReturn(proxyClassName).when(factory).getProductType();
        
        when(object.getFactory()).thenReturn(factory);
        
        FactoryObjectInternal internalObj = (FactoryObjectInternal)object;
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                m_NameMap.put((FactoryObject)invocation.getMock(), (String)invocation.getArguments()[0]);
                return null;
            }
        }).when(internalObj).internalSetName(anyString());
        when(object.getName()).thenAnswer(new Answer<String>()
        {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return m_NameMap.get(invocation.getMock());
            }
        });
        
        return object;
    }
    
    /**
     * Add mocked objects to a real factory object.  Factory object can then be interacted with.  This method should 
     * only be used when testing factory objects and not classes that use factory objects.  {@link 
     * #mockFactoryObject(Class, String, Config)} should be used in those cases instead.  
     * 
     * @param object
     *      object to add mocks to
     * @param pid
     *      configuration PID to use for the factory object
     */
    public static Configuration addMockConfiguration(AbstractFactoryObject object, String pid)
    {
        object.setPid(pid);
        return ConfigurationAdminMocker.addMockConfiguration(object);
    }
    
    /**
     * Class used to add extra configuration settings to a {@link FactoryObject} mock.
     */
    public static class Config
    {
        /**
         * extra interfaces the mocked factory object will implement
         */
        private Class<?>[] m_ExtraInterfaces;
        
        // prevent instantiation outside this file, use withSettings
        private Config() {}
        
        private Class<?>[] getExtraInterfaces()
        {
            return m_ExtraInterfaces;
        }
        
        public Config extraInterfaces(Class<?> ... extraInterfaces)
        {
            m_ExtraInterfaces = extraInterfaces;
            return this; // return for method chaining
        }
    }

    /**
     * This is how unit tests should get a config object.
     */
    public static Config withConfig()
    {
        return new Config();
    }
}
