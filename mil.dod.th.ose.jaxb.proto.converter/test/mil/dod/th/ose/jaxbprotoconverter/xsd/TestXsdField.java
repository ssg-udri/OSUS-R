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

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestXsdField
{
    private XsdField m_SUT;
    private XsdType m_XsdType;
    
    @Before
    public void setup()
    {
        m_XsdType = new XsdType();
        m_SUT = new XsdField(m_XsdType, 12, true);
    }
    
    /**
     * Verify that the appropriate index is returned by the get index method.
     */
    @Test
    public void testGetIndex()
    {
        assertThat(m_SUT.getIndex(), is(12));
    }
    
    /**
     * Verify that the appropriate XSD type is returned by the get XSD type method.
     */
    @Test
    public void testGetXsdType()
    {
        assertThat(m_SUT.getXsdType(), is(m_XsdType));
    }
    
    /**
     * Verify that the appropriate boolean value is returned by is index overridden method.
     */
    @Test
    public void testIsIndexOverridden()
    {
        assertThat(m_SUT.isIndexOverridden(), is(true));
    }
}
