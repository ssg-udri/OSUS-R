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
package mil.dod.th.ose.jaxbprotoconverter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Custom implementation of the {@link Map} class. This class is used throughout the JAXB-Proto converter and does not
 * allow existing entries to be replaced.
 * 
 * @author Dave Humeniuk
 *
 * @param <K>
 *          Class type of the keys in the map.
 * @param <V>
 *          Class type of the values in the map.
 */
public class NonReplacingHashMap<K, V> implements Map<K, V>
{
    /**
     * Reference to a {@link HashMap} class.
     */
    private final Map<K, V> m_InnerMap = new HashMap<K, V>();
    

    @Override
    public int size()
    {
        return m_InnerMap.size();
    }

    @Override
    public boolean isEmpty()
    {
        return m_InnerMap.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key)
    {
        return m_InnerMap.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value)
    {
        return m_InnerMap.containsValue(value);
    }

    @Override
    public V get(final Object key)
    {
        return m_InnerMap.get(key);
    }

    @Override
    public V put(final K key, final V value)
    {
        if (containsKey(key))
        {
            throw new IllegalArgumentException("Entry already exists for: " + key);
        }
        
        return m_InnerMap.put(key, value);
    }

    @Override
    public V remove(final Object key)
    {
        return m_InnerMap.remove(key);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> map)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void clear()
    {
        m_InnerMap.clear();
    }

    @Override
    public Set<K> keySet()
    {
        return m_InnerMap.keySet();
    }

    @Override
    public Collection<V> values()
    {
        return m_InnerMap.values();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet()
    {
        return m_InnerMap.entrySet();
    }    
}
