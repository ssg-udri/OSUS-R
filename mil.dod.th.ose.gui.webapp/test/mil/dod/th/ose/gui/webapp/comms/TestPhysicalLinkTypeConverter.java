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
package mil.dod.th.ose.gui.webapp.comms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;


import org.junit.Before;
import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestPhysicalLinkTypeConverter
{
    private PhysicalLinkTypeConverter m_SUT;

    @Before
    public void setUp()
    {
        m_SUT = new PhysicalLinkTypeConverter();
    }
    
    /**
     * Verify correct translation of physical link {@link PhysicalLinkTypeEnum}s to a readable string.
     */
    @Test
    public void testGetAsString() 
    {
        assertThat(m_SUT.getAsString(null, null, PhysicalLinkTypeEnum.GPIO), is("GPIO"));
        assertThat(m_SUT.getAsString(null, null, PhysicalLinkTypeEnum.SPI), is("SPI"));
        assertThat(m_SUT.getAsString(null, null, PhysicalLinkTypeEnum.SERIAL_PORT), is("Serial Port"));
        assertThat(m_SUT.getAsString(null, null, PhysicalLinkTypeEnum.I_2_C), is("I 2 C"));
        // make sure null value doesn't cause exception
        assertThat(m_SUT.getAsString(null, null, null), is(nullValue()));
    }
    
    /**
     * Verify translation from the string representation of {@link PhysicalLinkTypeEnum}s to actual physical link types.
     */
    @Test
    public void testGetAsObject()
    {
        assertThat((PhysicalLinkTypeEnum)m_SUT.getAsObject(null, null, "GPIO"), is(PhysicalLinkTypeEnum.GPIO));
        assertThat((PhysicalLinkTypeEnum)m_SUT.getAsObject(null, null, "SPI"), is(PhysicalLinkTypeEnum.SPI));
        assertThat((PhysicalLinkTypeEnum)m_SUT.getAsObject(null, null, "Serial Port"), 
            is(PhysicalLinkTypeEnum.SERIAL_PORT));
        assertThat((PhysicalLinkTypeEnum)m_SUT.getAsObject(null, null, "I 2 C"), is(PhysicalLinkTypeEnum.I_2_C));
        // make sure null value doesn't cause exception
        assertThat((PhysicalLinkTypeEnum)m_SUT.getAsObject(null, null, null), is(nullValue()));
        // make sure empty string doesn't cause exception
        assertThat((PhysicalLinkTypeEnum)m_SUT.getAsObject(null, null, ""), is(nullValue()));
    }
}
