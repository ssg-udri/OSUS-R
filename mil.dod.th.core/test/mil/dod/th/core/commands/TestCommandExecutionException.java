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

package mil.dod.th.core.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

public class TestCommandExecutionException
{
    private CommandExecutionException m_SUT;
    
    @Test
    public void testCommandExecutionExceptionString()
    {
        m_SUT = new CommandExecutionException("test msg");
        assertThat(m_SUT.getMessage(), is("test msg"));
    }

    @Test
    public void testCommandExecutionExceptionThrowable()
    {
        Exception cause = new Exception();
        m_SUT = new CommandExecutionException(cause);
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }

    @Test
    public void testCommandExecutionExceptionStringThrowable()
    {
        Exception cause = new Exception();
        m_SUT = new CommandExecutionException("test msg", cause);
        assertThat(m_SUT.getMessage(), is("test msg"));
        assertThat(m_SUT.getCause(), is((Throwable)cause));
    }
}