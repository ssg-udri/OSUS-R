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
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author cweisenborn
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MissionProgramManagerCommands.class)
public class TestMissionProgramManagerCommands
{
    private MissionProgramManagerCommands m_SUT;
    private ScriptEngine m_ScriptEngine;
    
    @Before
    public void setup()
    {
        m_SUT = new MissionProgramManagerCommands();
        
        m_ScriptEngine = mock(ScriptEngine.class);
        
        m_SUT.setScriptEngine(m_ScriptEngine);
    }

    @Test
    public void execute() throws ScriptException
    {
        String script = "test";
        
        CommandSession session = mock(CommandSession.class);
        Object result = mock(Object.class);
        PrintStream printStream = mock(PrintStream.class);
        
        when(m_ScriptEngine.eval(script)).thenReturn(result);
        when(session.getConsole()).thenReturn(printStream);
        
        m_SUT.execute(session, script);
        
        verify(m_ScriptEngine).put(ScriptEngine.FILENAME, "(none)");
        verify(m_ScriptEngine).eval(script);
        verify(printStream).println(result);
    }
    
    @Test
    public void executeFile() throws Exception
    {
        String path = "test";
        
        File file = mock(File.class);
        Object result = mock(Object.class);
        FileReader fileReader = mock(FileReader.class);
        
        PowerMockito.whenNew(FileReader.class).withArguments(file).thenReturn(fileReader);
        when(file.getPath()).thenReturn(path);
        when(m_ScriptEngine.eval(fileReader)).thenReturn(result);
        
        m_SUT.executeFile(file);
        
        verify(m_ScriptEngine).put(ScriptEngine.FILENAME, file.getPath());
        verify(m_ScriptEngine).eval(fileReader);
        verify(fileReader).close();
    }
}
