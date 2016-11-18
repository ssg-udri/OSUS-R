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
package mil.dod.th.ose.core.factory.api;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common .base.Objects;
import com.google.common.base.Preconditions;

import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * Abstract implementation of the Core factory object.
 */
public abstract class AbstractFactoryObject implements FactoryObjectInternal
{
    /**
     * Latch release lock object.
     */
    private final Object m_LatchLock = new Object();
    
    /**
     * Unique Persistence ID of the object.
     */
    private String m_Pid;
    
    /**
     * Base type of the object (asset, physical link, etc.).
     */
    private String m_BaseType;
    
    /**
     * Name of the object. This is cached from the database and must be updated if the database changes. 
     */
    private String m_Name;
    
    /**
     * Universal Unique Identifier of the object. This is cached from the database and must be updated if the
     * property is changed. In general practice, the UUID should not be modified.
     */
    private UUID m_Uuid;
    
    /**
     * The factory that created the object must be passed to the object when it is created.
     */
    private FactoryInternal m_Factory;

    /**
     * The factory registry which created this instance.
     */
    private FactoryRegistry<?> m_Registry;
    
    /**
     * The {@link ConfigurationAdmin} service to use.
     */
    private ConfigurationAdmin m_ConfigAdmin;
    
    /**
     * The {@link EventAdmin} service to use.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Reference to the object's proxy.
     */
    private FactoryObjectProxy m_FactoryObjectProxy;
    
    /**
     * Reference to the internal power manager service.
     */
    private PowerManagerInternal m_PowerManagerInternal;
    
    /**
     * Latch that will hold back thread to wait for property updates to be complete.
     */
    private CountDownLatch m_Latch;

    /**
     * Map of extensions where the key is the extension type.
     */
    private Map<Class<?>, Object> m_ExtensionMap;

    /**
     * Wake lock used for base factory object operations.
     */
    private WakeLock m_WakeLock;

    @Override
    public void initialize(final FactoryRegistry<?> registry, final FactoryObjectProxy proxy, //NOPMD: 
            final FactoryInternal factory, final ConfigurationAdmin configAdmin, final EventAdmin eventAdmin, 
            final PowerManagerInternal powerMgr, final UUID uuid, final String name, 
            final String pid, final String baseType) throws IllegalStateException //excessive number of parameters,
            //needed to fulfill all the object's needed services.
    {
        Preconditions.checkNotNull(proxy);
        
        m_Name = name;
        m_Uuid = uuid;
        m_Pid = pid;
        m_BaseType = baseType;
        m_Registry = registry;
        m_Factory = factory;
        m_FactoryObjectProxy = proxy;

        m_EventAdmin = eventAdmin;
        m_ConfigAdmin = configAdmin;
        m_PowerManagerInternal = powerMgr;

        m_WakeLock = powerMgr.createWakeLock(m_FactoryObjectProxy.getClass(), this, "coreFactoryObject");

        m_ExtensionMap = new HashMap<>();
        final Set<Extension<?>> extensions = 
                Objects.firstNonNull(m_FactoryObjectProxy.getExtensions(), new HashSet<Extension<?>>());
        for (Extension<?> extension : extensions)
        {
            m_ExtensionMap.put(extension.getType(), extension.getObject());
        }
    }

    @Override
    public String getPid()
    {
        return m_Pid;
    }
    
    @Override
    public void setPid(final String pid)
    {
        m_Pid = pid;
    }
    
    @Override
    public String getBaseType()
    {
        return m_BaseType;
    }
    
    @Override
    public UUID getUuid()
    {
        return m_Uuid;
    }

    @Override
    public String getName()
    {
        return m_Name;
    }

    @Override
    public void internalSetName(final String name)
    {
        m_Name = name;
    }

    @Override
    public void configUpdated(final Map<String, Object> props) throws ConfigurationException
    {
        m_FactoryObjectProxy.updated(props);
    }
    
    @Override
    public Map<String, Object> getProperties()
    {
        // return whatever comes back from config admin, don't get defaults.
        Configuration config;//NOCHECKSTYLE assigned inside try/catch
        try
        {
            config = FactoryServiceUtils.getFactoryConfiguration(m_ConfigAdmin, m_Pid);
        }
        catch (final FactoryException e)
        {
            throw new IllegalStateException(String.format("Configuration could not be retrieved for %s" 
                    + " for PID %s", getName(), m_Pid));
        }

        Dictionary<String, Object> configProps = null;
        if (config ==  null) 
        {
            // PID not set or no configuration for given PID
            configProps = new Hashtable<>();
        }
        else
        {
            configProps = config.getProperties();
        }
        return ConfigurationUtils.convertDictionaryPropsToMap(configProps);
    }

    @Override
    public synchronized void setProperties(final Map<String, Object> properties) throws IllegalArgumentException,
            IllegalStateException, FactoryException
    {
        try
        {
            m_WakeLock.activate();

            Configuration configuration = FactoryServiceUtils.getFactoryConfiguration(m_ConfigAdmin, getPid());
            if (configuration == null) 
            {
                // PID has not been set or could not find configuration for current PID
                try
                {
                    configuration = m_Registry.createConfiguration(m_Uuid, getFactory().getPid(), this);
                }
                catch (final FactoryObjectInformationException e)
                {
                    throw new FactoryException("Failed to create factory configuration for object.", e);
                }
            }
            
            //If there are no properties config will return null, wrap the new prop in Dictionary and update config.
            final Dictionary<String, Object> propsDict = new Hashtable<String, Object>(properties);
            try
            {
                synchronized (m_LatchLock)
                {
                    m_Latch = new CountDownLatch(1);
                    // Update properties in ConfigAdmin
                    configuration.update(propsDict);
                }
            }
            catch (final IOException e)
            {
                throw new FactoryException("Failed to update configuration", e);
            }
            
            final int timeout = 15;
            final boolean success;
            //latch wait
            try
            {
                success = m_Latch.await(timeout, TimeUnit.SECONDS);
            }
            catch (final InterruptedException e)
            {
                throw new FactoryException("Interrupted while waiting for property setting to complete", e);
            }

            if (!success)
            {
                throw new FactoryException("Set properties wait time expired!");
            }
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s)", getName(), m_Factory.getProductName());
    }

    @Override
    public FactoryInternal getFactory() 
    {
        return m_Factory;
    }
    
    @Override
    public FactoryObjectProxy getProxy()
    {
        return m_FactoryObjectProxy;
    }

    @Override
    public WakeLock createPowerManagerWakeLock(final String lockId) throws IllegalArgumentException
    {
        return m_PowerManagerInternal.createWakeLock(m_FactoryObjectProxy.getClass(), this, lockId);
    }
    
    @Override
    public void blockingPropsUpdate(final Map<String, Object> props) throws ConfigurationException
    {
        synchronized (m_LatchLock)
        {
            try
            {
                configUpdated(props);
            }
            finally
            {
                if (m_Latch != null)
                {
                    // if this is called for some other reason and the latch is already zero
                    // this call does nothing
                    m_Latch.countDown();
                }
            }
        }
    }
    
    @Override
    public void postEvent(final String topic, final Map<String, Object> props)
    {
        final Map<String, Object> propsToPost = FactoryServiceUtils.getFactoryObjectBaseEventProps(this);
        
        if (props != null)
        {
            for (String key : props.keySet())
            {
                propsToPost.put(key, props.get(key));
            }
        }
        
        m_EventAdmin.postEvent(new Event(topic, propsToPost));
        Logging.log(LogService.LOG_DEBUG, "Factory object [%s] posted event [%s]", m_Name, topic);
    }
    
    @Override
    public <T> T getExtension(final Class<T> type) throws IllegalArgumentException
    {
        @SuppressWarnings("unchecked")
        final T object = (T)m_ExtensionMap.get(type);
        if (object == null)
        {
            throw new IllegalArgumentException("Invalid extension type: " + type.getName());
        }
        return object;
    }
    
    @Override
    public Set<Class<?>> getExtensionTypes()
    {
        return m_ExtensionMap.keySet();
    }
    
    @Override
    public void setName(final String name) throws IllegalArgumentException, FactoryException
    {
        try
        {
            m_WakeLock.activate();
            m_Registry.setName(this, name);
        }
        catch (final FactoryObjectInformationException e)
        {
            throw new FactoryException(e);
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }
    
    @Override
    public void delete() throws IllegalStateException
    {
        m_Registry.delete(this);
        m_PowerManagerInternal.deleteWakeLock(m_WakeLock);
    }
    
    @Override
    public boolean isManaged()
    {
        for (FactoryObjectInternal object : m_Registry.getObjects())
        {
            if (this == object)
            {
                return true;
            }
        }
        return false;
    }
}
