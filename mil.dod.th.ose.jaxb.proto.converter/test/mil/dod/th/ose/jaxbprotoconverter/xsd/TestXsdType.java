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
package mil.dod.th.ose.jaxbprotoconverter.xsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestXsdType
{
    private XsdType m_SUT;
    private File m_XsdFile;
    private XsdNamespace m_XsdNamespace;
    
    @Before
    public void setup()
    {
        m_XsdNamespace = new XsdNamespace(null);
        
        m_XsdFile = mock(File.class);
        m_SUT = new XsdType();
    }
    
    /**
     * Verify that the appropriate file is returned by the getXsdFile method.
     */
    @Test
    public void testGetXsdFile()
    {
        m_SUT.setXsdFile(m_XsdFile);
        assertThat(m_SUT.getXsdFile(), is(m_XsdFile));
    }
    
    /**
     * Verify that a map is returned by the getFieldsMap method.
     */
    @Test
    public void testGetFieldsMap()
    {
        assertThat(m_SUT.getFieldsMap(), is(notNullValue()));
    }
    
    /**
     * Verify that a list is returned by the getOverriddenIds method.
     */
    @Test
    public void testGetOverriddenIdsList()
    {
        assertThat(m_SUT.getOverriddenIds(), is(notNullValue()));
    }
    
    /**
     * Verify that the appropriate XSD namespace is returned by the get XSD namespace method.
     */
    @Test
    public void testGetXsdNamespace()
    {
        m_SUT.setXsdNamespace(m_XsdNamespace);
        assertThat(m_SUT.getXsdNamespace(), is(m_XsdNamespace));
    }
    
    @Test
    public void testGetJaxbType()
    {
        m_SUT.setJaxbType(Object.class);
        assertThat(m_SUT.getJaxbType(), is((Object)Object.class));
    }
    
    @Test
    public void testGetBaseType()
    {
        XsdType baseType = new XsdType();
        m_SUT.setBaseType(baseType);
        assertThat(m_SUT.getBaseType(), is(baseType));
    }
}
