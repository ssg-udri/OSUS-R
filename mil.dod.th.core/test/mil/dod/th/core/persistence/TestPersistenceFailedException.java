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
package mil.dod.th.core.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

/**
 * @author jconn
 *
 */
public class TestPersistenceFailedException
{
    private final String m_ExceptionMessage = "Test Exception Message String";
    private final Throwable m_Throwable = new Throwable();

    /**
     * Test method for {@link mil.dod.th.core.persistence.PersistenceFailedException#PersistenceFailedException()}.
     */
    @Test
    public void testPersistenceFailedException()
    {
        assertThat(new PersistenceFailedException(), is(notNullValue()));
    }

    /**
     * Test method for {@link mil.dod.th.core.persistence.PersistenceFailedException#PersistenceFailedException
     * (java.lang.String)}.
     */
    @Test
    public void testPersistenceFailedExceptionString()
    {
        final PersistenceFailedException exception = new PersistenceFailedException(m_ExceptionMessage);
        assertThat(exception, is(notNullValue()));
        assertThat(exception.getMessage(), is(m_ExceptionMessage));
    }

    /**
     * Test method for {@link mil.dod.th.core.persistence.PersistenceFailedException#PersistenceFailedException
     * (java.lang.String, java.lang.Throwable)}.
     */
    @Test
    public void testPersistenceFailedExceptionStringThrowable()
    {
        final PersistenceFailedException exception = new PersistenceFailedException(m_ExceptionMessage, m_Throwable);
        assertThat(exception, is(notNullValue()));
        assertThat(exception.getMessage(), is(m_ExceptionMessage));
        assertThat(exception.getCause(), is(m_Throwable));
    }

    /**
     * Test method for {@link mil.dod.th.core.persistence.PersistenceFailedException#PersistenceFailedException
     * (java.lang.Throwable)}.
     */
    @Test
    public void testPersistenceFailedExceptionThrowable()
    {
        final PersistenceFailedException exception = new PersistenceFailedException(m_Throwable);
        assertThat(exception, is(notNullValue()));
        assertThat(exception.getCause(), is(m_Throwable));
    }
}
