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
package mil.dod.th.ose.gui.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.core.mp.Program.ProgramStatus;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests functionality of the CurrentMissionFilterConverter.
 * @author nickmarcucci
 *
 */
public class TestCurrentMissionFilterConverter
{
    private CurrentMissionFilterConverter m_SUT;
    
    @Before
    public void init()
    {
        m_SUT = new CurrentMissionFilterConverter();
    }
    
    /*
     * Verify correct ProgramStatus is returned based on passed in string.
     */
    @Test
    public void testConvertStringToProgramStatus()
    {
        assertThat((ProgramStatus)m_SUT.getAsObject(null, null, "SHUTDOWN"), is(ProgramStatus.SHUTDOWN));
    }
    
    /**
     * Verify correct string is returned based on the passed in ProgramStatus.
     */
    @Test
    public void testConvertProgramStatusToString()
    {
        assertThat(m_SUT.getAsString(null, null, ProgramStatus.SHUTDOWN), is("SHUTDOWN"));
    }
}
