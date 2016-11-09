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

import java.util.Dictionary;
import java.util.Map;

import mil.dod.th.ose.utils.ConfigurationUtils;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * Custom Hamcrest matcher to check if a dictionary has an entry with the given key-value pair.
 * 
 * @author jlatham
 *
 */
public class DictionaryHasEntry<K,V> extends TypeSafeMatcher<Dictionary<K, V>>
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
     * Contains the converted Map<String, Object> from the Dictionary<String, Object> passed in as part of the 
     * Hamcrest TypeSafeMatcher
     */
    private Map<K, V> map;

    /*
     * Constructor that sets the key and value to test.
     */
    public DictionaryHasEntry(K k, V v)
    {
        key = k;
        value = v;
    }
    
    /* (non-Javadoc)
     * @see org.hamcrest.SelfDescribing#describeTo(org.hamcrest.Description)
     */
    @Override
    public void describeTo(Description description)
    {
        description.appendText("Dictionary with key ").appendValue(key)
                   .appendText(" and value ").appendValue(value);        
    }

    /* (non-Javadoc)
     * @see org.hamcrest.TypeSafeMatcher#matchesSafely(java.lang.Object)
     */
    @Override
    protected boolean matchesSafely(Dictionary<K, V> dictionary)
    {
        map = ConfigurationUtils.convertDictionaryPropsToMap(dictionary); 
        return Matchers.hasEntry(key, value).matches(map);
    }
    
    /*
     * Factory method to create instance of matcher. 
     * The point of the factory method is to make test code read clearly.
     */
    @Factory
    public static <K,V> Matcher<Dictionary<K, V>> dictionaryHasEntry(K key, V value)
    {
        return new DictionaryHasEntry<K, V>(key, value);
    }
    
    /*
     * Factory method to create instance of matcher. 
     * The point of the factory method is to make test code read clearly.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Factory
    public static Matcher<Dictionary> rawDictionaryHasEntry(Object key, Object value)
    {
        return new DictionaryHasEntry(key, value);
    }
}
