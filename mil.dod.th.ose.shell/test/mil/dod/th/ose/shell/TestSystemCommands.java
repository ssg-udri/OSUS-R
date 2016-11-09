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
package mil.dod.th.ose.shell;

import static org.mockito.Mockito.*;

import java.io.PrintStream;

import org.apache.felix.service.command.CommandSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author dlandoll
 *
 */
public class TestSystemCommands
{
    private SystemCommands m_SUT;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new SystemCommands();
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testMemory()
    {
        CommandSession testSession = mock(CommandSession.class);
        PrintStream testStream = mock(PrintStream.class);
        when(testSession.getConsole()).thenReturn(testStream);

        m_SUT.memory(testSession);

        verify(testStream, atLeastOnce()).format(anyString(), anyVararg());
    }
}
