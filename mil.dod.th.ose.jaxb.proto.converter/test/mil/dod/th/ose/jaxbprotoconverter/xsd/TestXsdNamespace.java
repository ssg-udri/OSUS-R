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

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestXsdNamespace
{
    private XsdNamespace m_SUT;
    private XsdModel m_XsdModel;
    
    @Before
    public void setup()
    {
        m_XsdModel = new XsdModel();
        m_SUT = new XsdNamespace(m_XsdModel);
    }
    
    @Test
    public void testGetXsdModel()
    {
        assertThat(m_SUT.getXsdModel(), is(m_XsdModel));
    }
    
    @Test
    public void testGetTypesMap()
    {
        assertThat(m_SUT.getTypesMap(), is(notNullValue()));
    }
}
