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
package mil.dod.th.ose.core.factory.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import mil.dod.th.ose.core.factory.api.FactoryInternal;

import org.junit.Before;
import org.junit.Test;

/**
 * @author dlandoll
 *
 */
public class TestPendingFactoryObject
{
    private PendingFactoryObject m_SUT;
    private FactoryInternal m_Factory;
    private UUID m_Uuid = UUID.randomUUID();
    
    @Before
    public void setUp() throws Exception
    {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("test", 56);
        m_Factory = mock(FactoryInternal.class);
        m_SUT = new PendingFactoryObject(m_Uuid, props, m_Factory);
    }

    @Test
    public void testGetPid()
    {
        assertThat(m_Uuid, is(m_SUT.getUuid()));
    }
    
    @Test
    public void testGetProperties()
    {
        assertThat((Integer)m_SUT.getProperties().get("test"), is(56));
    }
    
    @Test
    public void testGetFactory()
    {
        assertThat(m_SUT.getFactory(), is(m_Factory));
    }
}
