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

import java.io.File;
import java.util.Dictionary;
import java.util.Map;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

/**
 * Collection of matchers.
 * 
 * @author dhumeniuk
 *
 */
public class Matchers
{
    /**
     * Check if the map has the given entry.
     */
    public static <K,V> Matcher<Map<K, V>> mapHasEntry(K key, V value)
    {
        return MapHasEntry.mapHasEntry(key, value);
    }
    
    /**
     * Check if the map has the given entry without checking types of key and value.
     */
    @SuppressWarnings("rawtypes")
    @Factory
    public static Matcher<Map> rawMapHasEntry(Object key, Object value)
    {
        return MapHasEntry.rawMapHasEntry(key, value);
    }
    
    /**
     * Check if the dictionary has the given entry.
     */
    public static <K,V> Matcher<Dictionary<K, V>> dictionaryHasEntry(K key, V value)
    {
        return DictionaryHasEntry.dictionaryHasEntry(key, value);
    }
    
    /**
     * Check if the dictionary has the given entry without checking types of key and value.
     */
    @SuppressWarnings("rawtypes")
    @Factory
    public static Matcher<Dictionary> rawDictionaryHasEntry(Object key, Object value)
    {
        return DictionaryHasEntry.rawDictionaryHasEntry(key, value);
    }
    
    /**
     * Test if actually a file using {@link File#isDirectory()}.
     */
    public static Matcher<File> isDirectory()
    {
        return FileMatchers.isDirectory();
    }

    /**
     * Test if actually a file using {@link File#exists()}.
     */
    public static Matcher<File> exists()
    {
        return FileMatchers.exists();
    }

    /**
     * Test if actually a file using {@link File#isFile()}.
     */
    public static Matcher<File> isFile()
    {
        return FileMatchers.isFile();
    }
}
