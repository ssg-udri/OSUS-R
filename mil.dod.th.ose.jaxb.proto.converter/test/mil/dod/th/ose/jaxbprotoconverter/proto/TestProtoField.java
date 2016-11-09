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
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.ProtoType;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.Rule;

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestProtoField
{
    private ProtoField m_SUT;
    private ProtoMessage m_ProtoMessage;
    
    @Before
    public void setup()
    {
        m_ProtoMessage = new ProtoMessage(null, null, null, null);
        m_SUT = new ProtoField(m_ProtoMessage);
    }
    
    @Test
    public void testGetRef()
    {
        m_SUT.setTypeRef(m_ProtoMessage);
        
        assertThat(m_SUT.getTypeRef(), equalTo((ProtoElement)m_ProtoMessage));
    }
    
    @Test
    public void testGetRule()
    {
        m_SUT.setRule(Rule.Optional);
        assertThat(m_SUT.getRule(), equalTo(Rule.Optional));
        
        m_SUT.setRule(Rule.Repeated);
        assertThat(m_SUT.getRule(), equalTo(Rule.Repeated));
        
        m_SUT.setRule(Rule.Required);
        assertThat(m_SUT.getRule(), equalTo(Rule.Required));
    }
    
    @Test
    public void testGetType()
    {
        m_SUT.setType(ProtoType.Boolean);
        assertThat(m_SUT.getType(), equalTo(ProtoType.Boolean));
        
        m_SUT.setType(ProtoType.Bytes);
        assertThat(m_SUT.getType(), equalTo(ProtoType.Bytes));
        
        m_SUT.setType(ProtoType.Double);
        assertThat(m_SUT.getType(), equalTo(ProtoType.Double));
        
        m_SUT.setType(ProtoType.Float);
        assertThat(m_SUT.getType(), equalTo(ProtoType.Float));
        
        m_SUT.setType(ProtoType.Int32);
        assertThat(m_SUT.getType(), equalTo(ProtoType.Int32));
        
        m_SUT.setType(ProtoType.Int64);
        assertThat(m_SUT.getType(), equalTo(ProtoType.Int64));
        
        m_SUT.setType(ProtoType.Reference);
        assertThat(m_SUT.getType(), equalTo(ProtoType.Reference));
        
        m_SUT.setType(ProtoType.String);
        assertThat(m_SUT.getType(), equalTo(ProtoType.String));
        
        m_SUT.setType(ProtoType.UInt32);
        assertThat(m_SUT.getType(), equalTo(ProtoType.UInt32));
        
        m_SUT.setType(ProtoType.UInt64);
        assertThat(m_SUT.getType(), equalTo(ProtoType.UInt64));
        
        m_SUT.setType(ProtoType.Enum);
        assertThat(m_SUT.getType(), equalTo(ProtoType.Enum));
    }
    
    @Test
    public void testGetName()
    {
        m_SUT.setName("Test");
        assertThat(m_SUT.getName(), equalTo("Test"));
    }
}
