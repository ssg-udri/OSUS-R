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
package mil.dod.th.ose.utils;

/**
 * Functional interface that will be introduced in Java 8. Use custom version until then.
 * 
 * @param <T>
 *      type of object to consume
 */
public interface Consumer<T>
{
    /**
     * Interface will consume the object using custom logic.
     * 
     * @param object
     *      object to consume
     */
    void consume(T object);
}
