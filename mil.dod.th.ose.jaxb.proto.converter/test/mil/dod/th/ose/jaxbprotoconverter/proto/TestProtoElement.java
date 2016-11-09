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
import static org.hamcrest.Matchers.is;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdType;

import org.junit.Before;
import org.junit.Test;

public class TestProtoElement
{
    private ProtoElement m_SUT;
    private ProtoFile m_ProtoFile;
    private XsdType m_XsdType;

    @Before
    public void setUp() throws Exception
    {
        m_ProtoFile = new ProtoFile(null, null);
        m_XsdType = new XsdType();
        m_SUT = new ProtoElement(m_ProtoFile, "elementName", Object.class, m_XsdType){};
    }
    
    @Test
    public void testGetProtoFile()
    {
        assertThat(m_SUT.getProtoFile(), equalTo(m_ProtoFile));
    }
    
    @Test
    public void testGetType()
    {
        assertThat(m_SUT.getType(), is((Object)Object.class));
        m_SUT.setType(ProtoElement.class);
        assertThat(m_SUT.getType(), is((Object)ProtoElement.class));
    }
    
    @Test
    public void testGetXsdType()
    {
        assertThat(m_SUT.getXsdType(), equalTo(m_XsdType));
        XsdType xsdType = new XsdType();
        m_SUT.setXsdType(xsdType);
        assertThat(m_SUT.getXsdType(), equalTo(xsdType));
    }

    @Test
    public void testGetName()
    {
        assertThat(m_SUT.getName(), equalTo("elementName"));
        m_SUT.setName("Test");
        assertThat(m_SUT.getName(), equalTo("Test"));
    }

    @Test
    public void testIsProcessed()
    {
        m_SUT.setProcessed(false);
        assertThat(m_SUT.isProcessed(), equalTo(false));
        
        m_SUT.setProcessed(true);
        assertThat(m_SUT.isProcessed(), equalTo(true));
    }
}
