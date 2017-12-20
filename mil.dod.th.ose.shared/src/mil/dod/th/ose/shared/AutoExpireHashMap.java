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
package mil.dod.th.ose.shared;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of an {@link AutoExpireMap} hash map.
 * 
 * @param <K>
 *            the Key type
 * @param <V>
 *            the Value type
 */
public class AutoExpireHashMap<K, V> implements AutoExpireMap<K, V>
{
    private final DelayQueue<ExpiringKey<K>> m_ExpiringQueue = new DelayQueue<>();
    private final Map<K, V> m_Map;
    private final long m_MaxExpireTimeMillis;
    private final long m_CheckExpireTimeMillis;
    private final AutoExpireMapCallback<K, V> m_Callback;
    private ScheduledExecutorService m_Executor;

    /**
     * Creates an auto expiring hash map.
     * 
     * @param maxExpireTimeMillis
     *      The default max expiration time (milliseconds) used for new entries added with
     *      {@link AutoExpireHashMap#put(Object, Object)}.
     * @param checkExpireTimeMillis
     *      How often, in milliseconds, a background thread should check for expired entries
     * @param callback
     *      Callback object used to notify when an entry expires
     */
    public AutoExpireHashMap(final long maxExpireTimeMillis, final long checkExpireTimeMillis,
            final AutoExpireMapCallback<K, V> callback)
    {
        m_Map = new ConcurrentHashMap<>();
        m_MaxExpireTimeMillis = maxExpireTimeMillis;
        m_CheckExpireTimeMillis = checkExpireTimeMillis;
        m_Callback = callback;
    }

    @Override
    public int size()
    {
        return m_Map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return m_Map.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key)
    {
        return m_Map.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value)
    {
        return m_Map.containsValue(value);
    }

    @Override
    public V get(final Object key)
    {
        return m_Map.get(key);
    }

    @Override
    public V put(final K key, final V value)
    {
        return put(key, value, m_MaxExpireTimeMillis);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(final Object key)
    {
        m_ExpiringQueue.remove(new ExpiringKey<K>((K)key, 0));
        try
        {
            stopExecutor();
        }
        catch (final InterruptedException e)
        {
            // ignore
        }
        return m_Map.remove(key);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> map)
    {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue(), m_MaxExpireTimeMillis);
        }
    }

    @Override
    public void clear()
    {
        m_ExpiringQueue.clear();
        try
        {
            stopExecutor();
        }
        catch (final InterruptedException e)
        {
            // Ignore
        }
        m_Map.clear();
    }

    @Override
    public Set<K> keySet()
    {
        return m_Map.keySet();
    }

    @Override
    public Collection<V> values()
    {
        return m_Map.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        return m_Map.entrySet();
    }

    @Override
    public V put(final K key, final V value, final long expireTimeMillis)
    {
        final ExpiringKey<K> keyToExpire = new ExpiringKey<K>(key, expireTimeMillis);
        m_ExpiringQueue.offer(keyToExpire);
        startExecutor();
        return m_Map.put(key, value);
    }

    @Override
    public long getRemainingTime(final K key, final TimeUnit unit)
    {
        return m_ExpiringQueue.stream().filter(ek -> ek.equals(new ExpiringKey<K>(key, 0)))
                                       .mapToLong(ek -> ek.getDelay(unit))
                                       .sum();
    }

    /**
     * Removes expired entries from the delay queue, removes expired map entries and issues the callback for
     * notification.
     */
    private void checkDelayQueue()
    {
        ExpiringKey<K> delayedKey = m_ExpiringQueue.poll();
        while (delayedKey != null)
        {
            final V value = m_Map.remove(delayedKey.getKey());
            m_Callback.entryExpired(delayedKey.getKey(), value);
            delayedKey = m_ExpiringQueue.poll();
        }
    }

    /**
     * Starts the background executor (if not already started) used to monitor for expired entries.
     */
    private void startExecutor()
    {
        if (!m_ExpiringQueue.isEmpty() && m_Executor == null)
        {
            m_Executor = Executors.newScheduledThreadPool(1);
            m_Executor.scheduleWithFixedDelay(() -> checkDelayQueue(), 0, m_CheckExpireTimeMillis,
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops the background executor if it is currently running and the delay queue is empty.
     * @throws InterruptedException 
     *      if timeout occurs waiting for executor to shutdown
     */
    private void stopExecutor() throws InterruptedException
    {
        if (m_ExpiringQueue.isEmpty() && m_Executor != null && !m_Executor.isShutdown())
        {
            m_Executor.shutdown();
            try
            {
                m_Executor.awaitTermination(2, TimeUnit.SECONDS);
            }
            finally
            {
                m_Executor = null;
            }
        }
    }

    /**
     * Wrapper for the actual key type to support expiring keys.
     *
     * @param <K>
     *      The key type
     */
    @SuppressWarnings("hiding")
    private class ExpiringKey<K> implements Delayed
    {
        private long m_StartTime = System.currentTimeMillis();
        private final long m_ExpireTimeMillis;
        private final K m_Key;

        /**
         * Create an expiring key with the given expire time in milliseconds.
         * 
         * @param key
         *      Key for the map entry
         * @param expireTimeMillis
         *      Expiration time in milliseconds
         */
        ExpiringKey(final K key, final long expireTimeMillis)
        {
            this.m_ExpireTimeMillis = expireTimeMillis;
            this.m_Key = key;
        }

        @SuppressWarnings("unchecked")
        @Override
        public int compareTo(final Delayed obj)
        {
            return Long.compare(this.getDelayMillis(), ((ExpiringKey<?>) obj).getDelayMillis());
        }

        @Override
        public long getDelay(final TimeUnit unit)
        {
            return unit.convert(getDelayMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int hashCode()
        {
            return m_Key.hashCode();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object obj)
        {
            if (obj instanceof ExpiringKey)
            {
                return m_Key.equals(((ExpiringKey<?>)obj).getKey());
            }

            return false;
        }

        public K getKey()
        {
            return m_Key;
        }

        private long getDelayMillis()
        {
            return (m_StartTime + m_ExpireTimeMillis) - System.currentTimeMillis();
        }
    }
}
