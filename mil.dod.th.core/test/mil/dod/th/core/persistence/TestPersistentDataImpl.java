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
package mil.dod.th.core.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.junit.Test;

/**
 * @author jconn
 *
 */
public class TestPersistentDataImpl
{   
    private PersistentData m_SUT;
    
    private final UUID m_UUID = UUID.randomUUID();
    private final String m_Description = "TestDescription";
    private final Serializable m_Entity = Long.valueOf(10);
    private final String m_SymbolicName = java.lang.Integer.class.getName();
    private Date m_TimeStamp = new Date();

    @Test
    public void testPersistentDataImpl()
    {
        m_SUT = new PersistentData(m_UUID, m_Description, m_SymbolicName, m_Entity);
        assertThat(m_SUT, is(notNullValue()));
        assertThat(m_SUT.getContext(), is(m_SymbolicName));
        assertThat(m_SUT.getTimestamp(), greaterThanOrEqualTo(m_TimeStamp.getTime()));
        assertThat(m_SUT.getUUID(), is(m_UUID));
        assertThat(m_SUT.getDescription(), is(m_Description));
        assertThat(m_SUT.getEntity(), is(m_Entity));
        m_SUT.setDescription(m_Description);
        m_SUT.setEntity(m_Entity);
        assertThat(m_SUT.getDescription(), is(m_Description));
        assertThat(m_SUT.getEntity(), is(m_Entity));
    }
}
