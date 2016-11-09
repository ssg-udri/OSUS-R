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
package mil.dod.th.ose.core.impl.pm;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.pm.WakeLockState;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * Implementation of the {@link PowerManager}.
 */
@Component
public class PowerManagerImpl implements PowerManager, PowerManagerInternal
{
    /** Used to log messages. */
    private LoggingService m_Logging;

    /**
     * Platform specific power manager, delegate platform operations to this service.
     */
    private PlatformPowerManager m_PlatformPowerManager;

    /**
     * Lock used for access to {@link #m_PlatformPowerManager}.
     */
    private final Object m_PPMLock = new Object();

    /**
     * Component factory used to create {@link WakeLock}'s.
     */
    private ComponentFactory m_WakeLockFactory;

    /**
     * Map of wake locks and their associated component instance.
     */
    private final Map<WakeLock, ComponentInstance> m_InstanceMap = new HashMap<WakeLock, ComponentInstance>();

    /**
     * Primary data structure used to maintain all wake locks created by the power manager.
     */
    private final Map<WakeLockKey, Map<String, WakeLock>> m_WakeLockMap = 
            new HashMap<WakeLockKey, Map<String, WakeLock>>();

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    /**
     * Binds the platform's power manager.
     * 
     * This reference is optional and not required.
     * 
     * @param platformPowerManager
     *            power manager for the specific platform
     */
    @Reference(optional = true, dynamic = true)
    public void setPlatformPowerManager(final PlatformPowerManager platformPowerManager)
    {
        synchronized (m_PPMLock)
        {
            m_PlatformPowerManager = platformPowerManager;
        }
    }

    /**
     * Unbind the platform's power manager.
     * 
     * @param platformPowerManager
     *      parameter not used, must match binding method signature
     */
    public void unsetPlatformPowerManager(final PlatformPowerManager platformPowerManager)
    {
        synchronized (m_PPMLock)
        {
            m_PlatformPowerManager = null; // NOPMD: NullAssignment, Must assign to null, field is checked before using
        }
    }

    /**
     * Bind the {@link WakeLock} component factory used to create new wake locks.
     * 
     * @param factory
     *            Component factory provided by OSGi
     */
    @Reference(target = "(component.factory=" + WakeLockImpl.COMPONENT_FACTORY + ")")
    public void setFactory(final ComponentFactory factory)
    {
        m_WakeLockFactory = factory;
    }

    @Override
    public synchronized WakeLock createWakeLock(final Class<?> context, final String lockId)
            throws IllegalArgumentException
    {
        final WakeLockKey key = new WakeLockKey(context);
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(WakeLockImpl.COMPONENT_PROP_ID, lockId);
        properties.put(WakeLockImpl.COMPONENT_PROP_CONTEXT, context);

        final WakeLock lock = createWakeLock(key, properties);

        m_Logging.debug("Created new WakeLock with context [%s] and ID [%s]", context.getName(), lockId);

        return lock;
    }

    @Override
    public synchronized WakeLock createWakeLock(final Class<? extends FactoryObjectProxy> context, 
            final FactoryObject sourceObject, final String lockId) throws IllegalArgumentException
    {
        final WakeLockKey key = new WakeLockKey(context, sourceObject);
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(WakeLockImpl.COMPONENT_PROP_ID, lockId);
        properties.put(WakeLockImpl.COMPONENT_PROP_CONTEXT, context);
        properties.put(WakeLockImpl.COMPONENT_PROP_SRC_OBJ, sourceObject);

        final WakeLock lock = createWakeLock(key, properties);

        m_Logging.debug("Created new WakeLock with source object [%s] and ID [%s]", sourceObject.getName(), lockId);

        return lock;
    }

    @Override
    public synchronized void deleteWakeLock(final WakeLock lock) throws IllegalArgumentException
    {
        final ComponentInstance instance = m_InstanceMap.remove(lock);

        final WakeLockKey key;
        if (lock.getSourceObject() == null)
        {
            key = new WakeLockKey(lock.getContext());
        }
        else
        {
            key = new WakeLockKey(lock.getContext(), lock.getSourceObject());
        }

        final Map<String, WakeLock> lockMap = m_WakeLockMap.get(key);
        if (lockMap != null)
        {
            lockMap.remove(lock.getId());

            if (lockMap.isEmpty())
            {
                m_WakeLockMap.remove(key);
            }
        }

        if (instance == null)
        {
            throw new IllegalArgumentException(String.format("WakeLock [%s:%s] has been deleted or does not exist",
                    key, lock.getId()));
        }
        else
        {
            synchronized (m_PPMLock)
            {
                if (m_PlatformPowerManager != null)
                {
                    try
                    {
                        m_PlatformPowerManager.cancelWakeLock(lock);
                    }
                    catch (final IllegalStateException ex)
                    {
                        m_Logging.debug("WakeLock [%s] is being deleted and is not active or scheduled", lock);
                    }
                }
            }

            m_Logging.debug("Deleting WakeLock [%s]", lock);

            // Delete the instance
            instance.dispose();
        }
    }

    @Override
    public synchronized Set<WakeLock> getWakeLocks(final WakeLockState state)
    {
        final Set<WakeLock> activeLocks = getActiveWakeLocks();
        final Set<WakeLock> schedLocks = getScheduledWakeLocks();

        final Set<WakeLock> retLockSet = new HashSet<WakeLock>();
        switch (state)
        {
            case Active:
                retLockSet.addAll(activeLocks);
                break;
            case Scheduled:
                retLockSet.addAll(schedLocks);
                break;
            case Inactive:
                retLockSet.addAll(getAllWakeLocks());
                retLockSet.removeAll(activeLocks);
                retLockSet.removeAll(schedLocks);
                break;
            case Any:
                retLockSet.addAll(getAllWakeLocks());
                break;
            default:
                break;
        }

        return Collections.unmodifiableSet(retLockSet);
    }

    @Override
    public synchronized Set<WakeLock> getWakeLocks(final Class<?> context, final WakeLockState state)
    {
        for (WakeLockKey key : m_WakeLockMap.keySet())
        {
            if (key.getContext().equals(context))
            {
                return getWakeLocksByKey(key, state);
            }
        }
        
        return Collections.emptySet();
    }

    @Override
    public synchronized Set<WakeLock> getWakeLocks(final FactoryObject sourceObject, final WakeLockState state)
    {
        for (WakeLockKey key : m_WakeLockMap.keySet())
        {
            if (sourceObject.equals(key.getFactoryObject()))
            {
                return getWakeLocksByKey(key, state);
            }
        }
        
        return Collections.emptySet();
    }

    @Override
    public synchronized Set<Class<?>> getWakeLockContexts()
    {
        final Set<Class<?>> contexts = new HashSet<Class<?>>();
        for (WakeLockKey key : m_WakeLockMap.keySet())
        {
            contexts.add(key.getContext());
        }
        return Collections.unmodifiableSet(contexts);
    }

    /**
     * Helper function used to create new wake locks.
     * 
     * @param key
     *      Wake lock key
     * @param properties
     *      Wake lock properties, including ID, context, and source object (if applicable)
     * @return New wake lock
     * @throws IllegalArgumentException
     *      if the combination of key and lock ID property are in use by an existing lock
     */
    private WakeLock createWakeLock(final WakeLockKey key, final Dictionary<String, Object> properties)
            throws IllegalArgumentException
    {
        properties.put(WakeLockImpl.COMPONENT_PROP_POWER_MGR, this);
        
        final String lockId = (String)properties.get(WakeLockImpl.COMPONENT_PROP_ID);

        Map<String, WakeLock> lockMap = m_WakeLockMap.get(key);
        if (lockMap != null)
        {
            final WakeLock existingLock = lockMap.get(lockId);
            if (existingLock != null)
            {
                throw new IllegalArgumentException(String.format("WakeLock ID [%s] already exists for %s", lockId,
                        key));
            }
        }

        final ComponentInstance instance = m_WakeLockFactory.newInstance(properties);
        final WakeLock lock = (WakeLock)instance.getInstance();

        // Save the component instance object for deletion of wake lock
        m_InstanceMap.put(lock, instance);

        // Add map if the key is new
        if (lockMap == null)
        {
            lockMap = new HashMap<String, WakeLock>();
            m_WakeLockMap.put(key, lockMap);
        }

        // Save the new wake lock
        lockMap.put(lockId, lock);

        return lock;
    }

    /**
     * Retrieves all wake locks create by this {@link PowerManager}.
     * 
     * @return a set of all wake locks
     */
    private Set<WakeLock> getAllWakeLocks()
    {
        final Set<WakeLock> retLockSet = new HashSet<WakeLock>();
        for (Map<String, WakeLock> map : m_WakeLockMap.values())
        {
            retLockSet.addAll(map.values());
        }
        return retLockSet;
    }

    /**
     * Retrieves all wake locks associated with the given key and state.
     * 
     * @param key
     *      Wake lock key to search for
     * @param state
     *      State of wake locks to search for
     * @return a set of wake locks for the given key and state
     */
    private Set<WakeLock> getWakeLocksByKey(final WakeLockKey key, final WakeLockState state)
    {
        final Map<String, WakeLock> lockMap = m_WakeLockMap.get(key);
        if (lockMap == null)
        {
            return Collections.emptySet();
        }

        final Collection<WakeLock> existingLocks = lockMap.values();
        final Set<WakeLock> activeLocks = getActiveWakeLocks();
        final Set<WakeLock> schedLocks = getScheduledWakeLocks();

        final Set<WakeLock> retLockSet = new HashSet<WakeLock>();
        switch (state)
        {
            case Active:
                retLockSet.addAll(Sets.intersection(new HashSet<WakeLock>(existingLocks), activeLocks));
                break;
            case Scheduled:
                retLockSet.addAll(Sets.intersection(new HashSet<WakeLock>(existingLocks), schedLocks));
                break;
            case Inactive:
                retLockSet.addAll(existingLocks);
                retLockSet.removeAll(activeLocks);
                retLockSet.removeAll(schedLocks);
                break;
            case Any:
                retLockSet.addAll(existingLocks);
                break;
            default:
                break;
        }

        return Collections.unmodifiableSet(retLockSet);
    }

    /**
     * Retrieves all active wake locks managed by {@link PlatformPowerManager}.
     * 
     * @return a set of all active wake locks
     */
    private Set<WakeLock> getActiveWakeLocks()
    {
        final Set<WakeLock> activeLocks;

        synchronized (m_PPMLock)
        {
            if (m_PlatformPowerManager == null)
            {
                activeLocks = Collections.emptySet();
            }
            else
            {
                activeLocks = m_PlatformPowerManager.getActiveWakeLocks();
            }
        }

        return activeLocks;
    }

    /**
     * Retrieves all scheduled wake locks managed by {@link PlatformPowerManager}.
     * 
     * @return a set of all scheduled wake locks
     */
    private Set<WakeLock> getScheduledWakeLocks()
    {
        final Set<WakeLock> schedLocks;

        synchronized (m_PPMLock)
        {
            if (m_PlatformPowerManager == null)
            {
                schedLocks = Collections.emptySet();
            }
            else
            {
                schedLocks = m_PlatformPowerManager.getScheduledWakeLocks();
            }
        }

        return schedLocks;
    }

    /**
     * Representation of a wake lock key using either a context or source object (but not both).
     */
    private class WakeLockKey
    {
        /**
         * Class context.
         */
        private final Class<?> m_Context;

        /**
         * Factory object reference.
         */
        private final FactoryObject m_FactoryObject;

        /**
         * Create key with a context.
         * 
         * @param context
         *      class context to use as the key
         */
        WakeLockKey(final Class<?> context)
        {
            Preconditions.checkNotNull(context);
            
            m_Context = context;
            m_FactoryObject = null;
        }

        /**
         * Create key with a factory object reference.
         * 
         * @param context
         *      context for the key, in this case the {@link FactoryObjectProxy} implementation class
         * @param factoryObject
         *      factory object to use as the key
         */
        WakeLockKey(final Class<?> context, final FactoryObject factoryObject)
        {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(factoryObject);
            
            m_Context = context;
            m_FactoryObject = factoryObject;
        }

        @Override
        public boolean equals(final Object obj)
        {
            final WakeLockKey key = (WakeLockKey)obj;
            // every key has a context, if no factory object, then key is unique by context
            if (m_FactoryObject == null)
            {
                return m_Context.equals(key.m_Context);
            }
            else
            {
                // can be multiple keys with the same context, but will have different keys
                return m_FactoryObject.equals(key.m_FactoryObject);
            }
        }

        @Override
        public int hashCode()
        {
            if (m_FactoryObject == null)
            {
                return m_Context.hashCode();
            }
            else
            {
                return m_FactoryObject.hashCode();
            }
        }

        @Override
        public String toString()
        {
            return (m_FactoryObject == null) ? m_Context.getName() : m_FactoryObject.getName();
        }

        /**
         * Return the context of the key.
         * 
         * @return
         *      Returns the product type for factory objects or given context if not a factory object key
         */
        public Class<?> getContext()
        {
            return m_Context;
        }
        
        public FactoryObject getFactoryObject()
        {
            return m_FactoryObject;
        }
    }
}
