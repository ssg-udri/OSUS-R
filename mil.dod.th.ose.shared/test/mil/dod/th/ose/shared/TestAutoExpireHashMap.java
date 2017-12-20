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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class TestAutoExpireHashMap
{
    private AutoExpireHashMap<Integer, String> m_SUT;

    @Mock private AutoExpireMapCallback<Integer, String> m_Callback;

    @Before
    public void setUp() throws Exception
    {
        // mock
        MockitoAnnotations.initMocks(this);

        m_SUT = new AutoExpireHashMap<>(1000, 1000, m_Callback);
    }

    @After
    public void tearDown() throws Exception
    {
        m_SUT.clear();
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#size()}.
     */
    @Test
    public void testSize()
    {
        assertThat(m_SUT.size(), is(0));

        m_SUT.put(1, "val1");
        assertThat(m_SUT.size(), is(1));

        m_SUT.put(2, "val2");
        m_SUT.put(3, "val3");
        assertThat(m_SUT.size(), is(3));

        m_SUT.remove(2);
        assertThat(m_SUT.size(), is(2));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#isEmpty()}.
     */
    @Test
    public void testIsEmpty()
    {
        assertThat(m_SUT.isEmpty(), is(true));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#containsKey(java.lang.Object)}.
     */
    @Test
    public void testContainsKey()
    {
        assertThat(m_SUT.containsKey(1), is(false));

        m_SUT.put(1, "val1");
        assertThat(m_SUT.containsKey(1), is(true));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#containsValue(java.lang.Object)}.
     */
    @Test
    public void testContainsValue()
    {
        assertThat(m_SUT.containsValue("val1"), is(false));

        m_SUT.put(1, "val1");
        assertThat(m_SUT.containsValue("val1"), is(true));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#get(java.lang.Object)}.
     */
    @Test
    public void testPutAndGet()
    {
        assertThat(m_SUT.get(4), nullValue());

        m_SUT.put(4, "val4");
        assertThat(m_SUT.get(4), is("val4"));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#remove(java.lang.Object)}.
     */
    @Test
    public void testRemove() throws InterruptedException
    {
        m_SUT.put(1, "val1");
        assertThat(m_SUT.remove(1), is("val1"));
        Thread.sleep(1100);
        Mockito.verify(m_Callback, Mockito.never()).entryExpired(Mockito.any(Integer.class), Mockito.any(String.class));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#putAll(java.util.Map)}.
     */
    @Test
    public void testPutAll()
    {
        Map<Integer, String> map = new HashMap<>();
        map.put(3, "val3");
        map.put(4, "val4");
        map.put(5, "val5");

        m_SUT.putAll(map);
        assertThat(m_SUT, hasEntry(3, "val3"));
        assertThat(m_SUT, hasEntry(4, "val4"));
        assertThat(m_SUT, hasEntry(5, "val5"));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#clear()}.
     */
    @Test
    public void testClear() throws InterruptedException
    {
        m_SUT.put(1, "val1");
        m_SUT.put(2, "val2");
        m_SUT.put(3, "val3");
        m_SUT.clear();
        Thread.sleep(1100);
        Mockito.verify(m_Callback, Mockito.never()).entryExpired(Mockito.any(Integer.class), Mockito.any(String.class));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#keySet()}.
     */
    @Test
    public void testKeySet()
    {
        m_SUT.put(1, "val1");
        m_SUT.put(2, "val2");

        assertThat(m_SUT.keySet(), contains(1, 2));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#values()}.
     */
    @Test
    public void testValues()
    {
        m_SUT.put(1, "val1");
        m_SUT.put(2, "val2");

        assertThat(m_SUT.values(), contains("val1", "val2"));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#entrySet()}.
     */
    @Test
    public void testEntrySet()
    {
        Map<Integer, String> map = new HashMap<>();
        map.put(3, "val3");
        map.put(4, "val4");
        map.put(5, "val5");

        m_SUT.put(3, "val3");
        m_SUT.put(4, "val4");
        m_SUT.put(5, "val5");

        assertThat(m_SUT.entrySet(), is(map.entrySet()));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.AutoExpireHashMap#put(java.lang.Object, java.lang.Object, long)}.
     */
    @Test
    public void testPutWithExpireTime() throws InterruptedException
    {
        m_SUT.put(1, "val1", 1000);
        m_SUT.put(2, "val2", 2000);
        m_SUT.put(3, "val3", 3000);
        assertThat(m_SUT.get(3), is("val3"));
        assertThat(m_SUT.get(2), is("val2"));
        assertThat(m_SUT.get(1), is("val1"));

        Thread.sleep(4000);

        Mockito.verify(m_Callback).entryExpired(1, "val1");
        Mockito.verify(m_Callback).entryExpired(2, "val2");
        Mockito.verify(m_Callback).entryExpired(3, "val3");
    }

    /**
     * Test method for
     * {@link mil.dod.th.ose.shared.AutoExpireHashMap#getRemainingTime(java.lang.Object, java.util.concurrent.TimeUnit)}
     * .
     */
    @Test
    public void testGetRemainingTime()
    {
        m_SUT.put(1, "val1", 1000);
        m_SUT.put(2, "val2", 2000);
        m_SUT.put(3, "val3", 3000);
        assertThat(m_SUT.getRemainingTime(1, TimeUnit.MILLISECONDS), allOf(greaterThan(0L), lessThanOrEqualTo(1000L)));
        assertThat(m_SUT.getRemainingTime(1, TimeUnit.HOURS), is(0L));
        assertThat(m_SUT.getRemainingTime(2, TimeUnit.MILLISECONDS),
                allOf(greaterThan(1000L), lessThanOrEqualTo(2000L)));
        assertThat(m_SUT.getRemainingTime(2, TimeUnit.HOURS), is(0L));
        assertThat(m_SUT.getRemainingTime(3, TimeUnit.MILLISECONDS),
                allOf(greaterThan(2000L), lessThanOrEqualTo(3000L)));
        assertThat(m_SUT.getRemainingTime(3, TimeUnit.HOURS), is(0L));
    }
}
