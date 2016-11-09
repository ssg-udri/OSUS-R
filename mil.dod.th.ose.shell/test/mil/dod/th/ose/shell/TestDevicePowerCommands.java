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
import java.util.HashSet;
import java.util.Set;

import mil.dod.th.core.pm.DevicePowerManager;
import mil.dod.th.core.pm.PowerManagerException;

import org.apache.felix.service.command.CommandSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author dlandoll
 *
 */
public class TestDevicePowerCommands
{
    private DevicePowerCommands m_SUT;
    private DevicePowerManager m_DevicePowerManager;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new DevicePowerCommands();
        m_DevicePowerManager = mock(DevicePowerManager.class);

        m_SUT.setDevicePowerManager(m_DevicePowerManager);
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testDev() throws IllegalArgumentException, PowerManagerException
    {
        CommandSession testSession = mock(CommandSession.class);
        PrintStream testStream = mock(PrintStream.class);
        when(testSession.getConsole()).thenReturn(testStream);

        m_SUT.dev(testSession, true, false, "name");
        verify(m_DevicePowerManager).on(eq("name"));

        m_SUT.dev(testSession, false, true, "name");
        verify(m_DevicePowerManager).off(eq("name"));

        m_SUT.dev(testSession, false, false, "name");
        verify(m_DevicePowerManager).isOn(eq("name"));
        verify(testStream).format(anyString(), anyVararg());
    }

    @Test
    public void testDevs() throws IllegalArgumentException, PowerManagerException
    {
        CommandSession testSession = mock(CommandSession.class);
        PrintStream testStream = mock(PrintStream.class);
        when(testSession.getConsole()).thenReturn(testStream);
        Set<String> devices = new HashSet<String>();
        devices.add("dev1");
        when(m_DevicePowerManager.getDevices()).thenReturn(devices);

        m_SUT.devs(testSession);
        verify(testStream).format(anyString(), anyVararg());
    }
}
