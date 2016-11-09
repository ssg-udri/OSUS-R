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
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TestProtoEnum
{
    private ProtoEnum m_SUT;
    private ProtoMessage m_ProtoMessage;
    private ProtoFile m_ProtoFile;

    @Before
    public void setUp() throws Exception
    {
        m_ProtoFile = new ProtoFile(null, null);
        m_ProtoMessage = new ProtoMessage(null, null, null, null);
        m_SUT = new ProtoEnum(m_ProtoMessage, m_ProtoFile, null, null, null);
    }

    @Test
    public void testGetValues()
    {
        List<String> values = new ArrayList<>();
        m_SUT.setValues(values);
        assertThat(m_SUT.getValues(), equalTo(values));
    }
    
    @Test
    public void testGetProtoMessage()
    {
        assertThat(m_SUT.getProtoMessage(), equalTo(m_ProtoMessage));
    }
}
