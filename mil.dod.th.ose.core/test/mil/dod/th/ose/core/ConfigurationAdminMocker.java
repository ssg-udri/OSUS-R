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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.ose.core.factory.api.AbstractFactoryObject;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class ConfigurationAdminMocker
{
    //number appended to PIDs so that they are unique
    private static int m_PidNum = 0;
    //map of configurations keyed by PID
    private static Map<String, Configuration> m_Configs = 
            Collections.synchronizedMap(new Hashtable<String, Configuration>());

    //Properties
    private static Map<String, Dictionary<String, ?>> m_Properties = 
            Collections.synchronizedMap(new Hashtable<String, Dictionary<String, ?>>());
    //known factories
    private static Map<String, ManagedServiceFactory> m_MSF = 
            Collections.synchronizedMap(new Hashtable<String, ManagedServiceFactory>());
    
    /**
     * create a configuration
     * @param factoryPid
     *      the PID of the factory to which the configuration belongs
     * @return
     *      the created configuration
     */
    private static Configuration createMockConfiguration(final String factoryPid) throws IOException,
            ConfigurationException
    {
        final String pid = factoryPid + "_" + m_PidNum++;
        return createMockConfiguration(factoryPid, pid);
    }

    /**
     * Create a configuration
     * @param factoryPid
     *      the PID of the factory to which the configuration belongs
     * @param pid
     *      the PID to assign to the configuration
     * @return
     *      created configuration
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Configuration createMockConfiguration(final String factoryPid, final String pid) throws IOException,
            ConfigurationException
    {
        Preconditions.checkNotNull(factoryPid);
        Preconditions.checkNotNull(pid);
        
        //configuration to return
        Configuration config = mock(Configuration.class);

        //store the mock config with the associated PID
        m_Configs.put(pid, config);

        //mock config behavior
        when(config.getFactoryPid()).thenReturn(factoryPid);
        when(config.getPid()).thenReturn(pid);

        //the pid is not known, then add the properties to the property map... mapped by PID
        if (m_Properties.get(pid) == null)
        {
            final Dictionary<String, Object> properties = new Hashtable<String, Object>();
            m_Properties.put(pid, properties);
        }
        else
        {
            // This configuration is being restored, so notify the ManagedServiceFactory
            ManagedServiceFactory msf = m_MSF.get(factoryPid);
            if (msf != null)
            {
                try
                {
                    //update the configuration map with the properties from the prop map
                    msf.updated(pid, m_Properties.get(pid));
                }
                catch (final ConfigurationException e)
                {
                    e.printStackTrace();
                }
            }
        }

        //configuration mocking behavior, when getProps is called return the most up to date properties from the
        // map of properties... the props are stored by PID
        when(config.getProperties()).thenAnswer(new Answer<Dictionary>()
        {
            @Override
            public Dictionary answer(InvocationOnMock invocation) throws Throwable
            {

                return m_Properties.get(pid);
            }
        });

        //when update is called capture the new properties, and store them in the property map, then call update 
        //from the factory service
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                Dictionary newProps = (Dictionary)invocation.getArguments()[0];

                m_Properties.put(pid, newProps);

                ManagedServiceFactory msf = m_MSF.get(factoryPid);
                if (msf != null)
                {
                    try
                    {
                        msf.updated(pid, newProps);
                    }
                    catch (ConfigurationException e)
                    {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }).when(config).update(Mockito.any(Dictionary.class));

        //delete properties and mock config from maps, then call factory service delete
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                m_Configs.remove(pid);
                m_Properties.remove(pid);

                ManagedServiceFactory msf = m_MSF.get(factoryPid);
                if (msf != null)
                {
                    msf.deleted(pid);
                }

                return null;
            }
        }).when(config).delete();

        return config;
    }

    /**
     * Create a configuration admin mock. 
     * @return
     *      initialized configuration admin mock with built in mocking behavior
     */
    public static ConfigurationAdmin createMockConfigAdmin()
    {
        m_Configs.clear();
        m_Properties.clear();
        m_MSF.clear();

        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);

        try
        {
            //create and store a new configuration
            when(configAdmin.createFactoryConfiguration(Mockito.anyString(), Mockito.anyString())).thenAnswer(
                    new Answer<Configuration>()
                    {
                        @Override
                        public Configuration answer(InvocationOnMock invocation) throws Throwable
                        {
                            String factoryPid = (String)invocation.getArguments()[0];
                            Preconditions.checkNotNull(factoryPid);
                            Configuration config = createMockConfiguration(factoryPid);
                            return config;
                        }
                    });
    
            //look up the configuration by PID from the config map
            when(configAdmin.getConfiguration(Mockito.anyString(), anyString())).thenAnswer(new Answer<Configuration>()
            {
                @Override
                public Configuration answer(InvocationOnMock invocation) throws Throwable
                {
                    String pid = (String)invocation.getArguments()[0];
                    return m_Configs.get(pid);
                }
            });
    
            //list all known configuration from config map
            when(configAdmin.listConfigurations(Mockito.anyString())).thenAnswer(new Answer<Configuration[]>()
            {
                @Override
                public Configuration[] answer(InvocationOnMock invocation) throws Throwable
                {
                    Collection<Configuration> configs = m_Configs.values();
                    configs = Collections2.filter(configs, new Predicate<Configuration>()
                    {
                        @Override
                        public boolean apply(Configuration config)
                        {
                            // only keep configs in list that have properties set
                            return config.getProperties() != null;
                        }
                    });
                    
                    if (configs.size() == 0)
                    {
                        return null;
                    }
                    return configs.toArray(new Configuration[configs.size()]);
                }
            });
        }
        catch (InvalidSyntaxException | IOException e)
        {
            throw new IllegalStateException(e);
        }

        return configAdmin;
    }
    
    /**
     * Mock the configuration for an object.
     */
    public static Configuration addMockConfiguration(final FactoryObject object)
    {
        final String pid = object.getPid();
        
        return addMockConfiguration(object, pid);
    }

    /**
     * Mock the configuration for an object.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    // This is due to the mocking of Dictionary type
    public static Configuration addMockConfiguration(final FactoryObject object, final String pid)
    {
        Preconditions.checkNotNull(pid);
        
        if (m_Configs.containsKey(pid))
        {
            throw new IllegalArgumentException("PID already exists in map");
        }
        
        Configuration config = mock(Configuration.class);

        m_Configs.put(pid, config);

        when(config.getFactoryPid()).thenReturn(null);
        when(config.getPid()).thenReturn(pid);

        when(config.getProperties()).thenAnswer(new Answer<Dictionary>()
        {
            @Override
            public Dictionary answer(InvocationOnMock invocation) throws Throwable
            {
                return m_Properties.get(pid);
            }
        });

        try
        {
            doAnswer(new Answer<Object>()
            {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable
                {
                    final Dictionary<String, ?> newProps = (Dictionary<String, ?>)invocation.getArguments()[0];
                    
                    Map<String, Object> newPropMap = new HashMap<>();
                    Enumeration<String> enumerable = newProps.keys();
                    while (enumerable.hasMoreElements())
                    {
                        Object key = enumerable.nextElement();
                        newPropMap.put((String)key, newProps.get(key));
                    }
                    
                    m_Properties.put(pid, newProps);
                    ((AbstractFactoryObject) object).blockingPropsUpdate(newPropMap);
                    return null;
                }
            }).when(config).update(Mockito.any(Dictionary.class));
    
            doAnswer(new Answer<Object>()
            {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable
                {
                    m_Configs.remove(pid);
                    m_Properties.remove(pid);
                    return null;
                }
            }).when(config).delete();
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
        
        return config;
    }

    /**
     * Store factory information, and create initial factory config.
     */
    public static void registerManagedServiceFactory(String factoryPid, ManagedServiceFactory msf) throws IOException,
            ConfigurationException
    {
        m_MSF.put(factoryPid, msf);
        Set<String> restorePidSet = new HashSet<String>();
        for (String pid : m_Configs.keySet())
        {
            if (pid.startsWith(factoryPid))
            {
                restorePidSet.add(pid);
            }
        }

        for (String pid : restorePidSet)
        {
            createMockConfiguration(factoryPid, pid);
        }
    }

    /**
     * Remove the factory from the lookup.
     * @param factoryPid
     *      the factory to remove from the lookup
     */
    public static void unregisterManagedServiceFactory(String factoryPid)
    {
        m_MSF.remove(factoryPid);
    }
    
    public static Configuration getConfigByPid(String pid)
    {
        return m_Configs.get(pid);
    }
}
