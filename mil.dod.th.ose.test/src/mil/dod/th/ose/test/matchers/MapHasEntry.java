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
package mil.dod.th.ose.test.matchers;

import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * Custom Hamcrest matcher to check if a Map has an entry with the given key-value pair.
 * 
 * @author dhumeniuk
 *
 */
public class MapHasEntry<K,V> extends TypeSafeMatcher<Map<K, V>>
{
    /*
     * Stores the key to test.
     */
    private final K key;
    
    /*
     * Stores the value to test.
     */
    private final V value;

    /*
     * Constructor that sets the key and value to test.
     */
    public MapHasEntry(K k, V v)
    {
        key = k;
        value = v;
    }
    
    @Override
    public void describeTo(Description description)
    {
        description.appendText("Map with key ").appendValue(key)
                   .appendText(" and value ").appendValue(value);        
    }

    @Override
    protected boolean matchesSafely(Map<K, V> map)
    {
        return Matchers.hasEntry(key, value).matches(map);
    }
    
    /*
     * Factory method to create instance of matcher. 
     * The point of the factory method is to make test code read clearly.
     */
    @Factory
    public static <K,V> Matcher<Map<K, V>> mapHasEntry(K key, V value)
    {
        return new MapHasEntry<K, V>(key, value);
    }
    
    /*
     * Factory method to create instance of matcher. 
     * The point of the factory method is to make test code read clearly.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Factory
    public static Matcher<Map> rawMapHasEntry(Object key, Object value)
    {
        return new MapHasEntry(key, value);
    }
}
