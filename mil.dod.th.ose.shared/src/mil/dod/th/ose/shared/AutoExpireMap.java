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

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An object that maps keys to values with automatic expiration of entries based on time.
 * 
 * @see Map
 * 
 * @param <K>
 *            the Key type
 * @param <V>
 *            the Value type
 */
public interface AutoExpireMap<K, V> extends Map<K, V>
{
    /**
     * Associates the specified value with the specified key in this map and specifies an expiration time in
     * milliseconds.
     * 
     * @see Map#put(Object, Object)
     *
     * @param key
     *            key with which the specified value is to be associated
     * @param value
     *            value to be associated with the specified key
     * @param expireTimeMillis
     *            expiration time in milliseconds
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for
     *         <tt>key</tt>. (A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt>
     *         with <tt>key</tt>, if the implementation supports <tt>null</tt> values.)
     * @throws UnsupportedOperationException
     *            if the <tt>put</tt> operation is not supported by this map
     * @throws ClassCastException
     *            if the class of the specified key or value prevents it from being stored in this map
     * @throws NullPointerException
     *            if the specified key or value is null and this map does not permit null keys or values
     * @throws IllegalArgumentException
     *            if some property of the specified key or value prevents it from being stored in this map
     */
    V put(K key, V value, long expireTimeMillis);

    /**
     * Get the remaining expiration time for an entry in the map.
     * 
     * @param key
     *            key to get remaining expiration time for
     * @param unit
     *            time unit the return value should be in
     * @return
     *            remaining expiration time in units specified by the unit parameter
     */
    long getRemainingTime(K key, TimeUnit unit);
}
