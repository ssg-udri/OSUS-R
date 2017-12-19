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

/**
 * Callback object for {@link AutoExpireMap}.
 *
 * @param <K>
 *            the Key type
 * @param <V>
 *            the Value type
 */
public interface AutoExpireMapCallback<K, V>
{
    /**
     * Called when an entry in the map expires.
     * 
     * @param key
     *      entry key
     * @param value
     *      entry value
     */
    void entryExpired(K key, V value);
}
