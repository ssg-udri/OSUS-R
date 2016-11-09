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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestNonReplacingHashMap
{
    private NonReplacingHashMap<String, String> m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new NonReplacingHashMap<String, String>();
    }
    
    @Test
    public void testGet()
    {
        m_SUT.put("testKey1", "testValue1");
        assertThat(m_SUT.get("testKey1"), equalTo("testValue1"));
    }
    
    @Test
    public void testContainsKey()
    {
        assertThat(m_SUT.containsKey("testKey1"), equalTo(false));
        
        m_SUT.put("testKey1", "testValue1");
        assertThat(m_SUT.containsKey("testKey1"), equalTo(true));
    }
    
    @Test
    public void testContainsValue()
    {
        assertThat(m_SUT.containsValue("testValue1"), equalTo(false));
        
        m_SUT.put("testKey1", "testValue1");
        assertThat(m_SUT.containsValue("testValue1"), equalTo(true));
    }
    
    @Test
    public void testIsEmptry()
    {
        assertThat(m_SUT.isEmpty(), equalTo(true));
        
        m_SUT.put("testKey1", "testValue1");
        assertThat(m_SUT.isEmpty(), equalTo(false));
    }
    
    @Test
    public void testSize()
    {
        assertThat(m_SUT.size(), equalTo(0));
        
        m_SUT.put("testKey1", "testValue1");
        assertThat(m_SUT.size(), equalTo(1));
    }
    
    @Test
    public void testRemove()
    {
        m_SUT.put("testKey1", "testValue1");
        assertThat(m_SUT.containsKey("testKey1"), equalTo(true));
        m_SUT.remove("testKey1");
        assertThat(m_SUT.containsKey("testKey1"), equalTo(false));
    }
    
    @Test
    public void testClear()
    {
        m_SUT.put("testKey1", "testValue1");
        m_SUT.put("testKey2", "testValue2");
        m_SUT.put("testKey3", "testValue3");
        assertThat(m_SUT.size(), equalTo(3));
        m_SUT.clear();
        assertThat(m_SUT.isEmpty(), equalTo(true));
    }
    
    @Test
    public void testKeySet()
    {
        Set<String> testSet = new HashSet<String>();
        testSet.add("testKey1");
        testSet.add("testKey2");
        testSet.add("testKey3");
        m_SUT.put("testKey1", "testValue1");
        m_SUT.put("testKey2", "testValue2");
        m_SUT.put("testKey3", "testValue3");
        
        assertThat(m_SUT.keySet(), equalTo(testSet));
    }
    
    @Test
    public void testValues()
    {
        Set<String> testCollection = new HashSet<String>();
        Set<String> actualCollection = new HashSet<String>(); 
        
        testCollection.add("testValue3");
        testCollection.add("testValue2");
        testCollection.add("testValue1");
        m_SUT.put("testKey1", "testValue1");
        m_SUT.put("testKey2", "testValue2");
        m_SUT.put("testKey3", "testValue3");
        
        for (String value: m_SUT.values())
        {
            actualCollection.add(value);
        }
        
        assertThat(actualCollection, equalTo(testCollection));
    }
}
