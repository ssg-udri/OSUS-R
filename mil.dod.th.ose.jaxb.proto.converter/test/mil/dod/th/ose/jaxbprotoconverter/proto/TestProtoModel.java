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

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestProtoModel
{
    private ProtoModel m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new ProtoModel();
    }
    
    @Test
    public void testGetEnums()
    {
        assertThat(m_SUT.getEnums(), is(notNullValue()));
    }
    
    @Test
    public void testGetProtoFileMap()
    {
        assertThat(m_SUT.getProtoFileMap(), is(notNullValue()));
    }
}
