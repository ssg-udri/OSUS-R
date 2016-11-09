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
package mil.dod.th.core.archiver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

/**
 * @author jmiller
 *
 */
public class TestArchiverException
{
    private ArchiverException m_SUT;
    
    /**
     * Test method for
     * {@link mil.dod.th.core.archiver.ArchiverException#ArchiverException(String)}.
     */
    @Test
    public void testArchiverExceptionString()
    {
        m_SUT = new ArchiverException("test msg");
        assertThat(m_SUT.getMessage(), is("test msg"));
    }
    
    /**
     * Test method for
     * {@link mil.dod.th.core.archiver.ArchiverException#ArchiverException(String, Throwable)}.
     */
    @Test
    public void testArchiverExceptionStringThrowable()
    {
        Exception cause = new Exception();
        m_SUT = new ArchiverException("test msg", cause);
        assertThat(m_SUT.getMessage(), is("test msg"));
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }
}
