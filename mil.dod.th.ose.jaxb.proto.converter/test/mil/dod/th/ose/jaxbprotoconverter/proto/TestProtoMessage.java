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
package mil.dod.th.ose.jaxbprotoconverter.proto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestProtoMessage
{
    private ProtoMessage m_SUT;
    private ProtoFile m_ProtoFile;
    
    @Before
    public void setup()
    {
        m_ProtoFile = new ProtoFile(null, null);
        m_SUT = new ProtoMessage(m_ProtoFile, "SomeMessage", Object.class, null);
    }
    
    @Test
    public void testGetEntries()
    {
        assertThat(m_SUT.getFields(), is(notNullValue()));
    }
    
    @Test
    public void testGetRelatedClass()
    {
        Class<?> clazz = Class.class;
        m_SUT.setType(clazz);
        assertEquals(clazz, m_SUT.getType());
    }
    
    @Test
    public void testGetEnumeration()
    {
        ProtoEnum protoEnum = new ProtoEnum(null, null, null, null, null);
        m_SUT.setEnumeration(protoEnum);
        assertThat(m_SUT.getEnumeration(), equalTo(protoEnum));
    }
}
