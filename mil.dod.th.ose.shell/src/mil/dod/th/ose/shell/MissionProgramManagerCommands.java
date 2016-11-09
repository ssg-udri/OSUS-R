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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;

/**
 * Mission program manager commands.
 * 
 * @author cweisenborn
 */
@Component(provide = MissionProgramManagerCommands.class, properties = {"osgi.command.scope=js", 
        "osgi.command.function=execute|executeFile" })
public class MissionProgramManagerCommands
{   
    /**
     * Reference to script engine.
     */
    private ScriptEngine m_ScriptEngine;
    
    /**
     * Set the script engine.
     * 
     * @param scriptEngine
     *              script engine to be set
     */
    @Reference(target = "(name=JavaScript)")
    public void setScriptEngine(final ScriptEngine scriptEngine)
    {
        m_ScriptEngine = scriptEngine;
    }
    
    /**
     * Execute a JavaScript from the command line.
     * 
     * @param session
     *      command session that is executing the command
     * @param script
     *      script to execute
     * @throws ScriptException
     *      if the script fails in some way
     */
    @Descriptor("Execute a JavaScript")
    public void execute(final CommandSession session,
            @Descriptor("script to execute")
            final String script) throws ScriptException
    {
        final Object result;
        synchronized (m_ScriptEngine)
        {
            m_ScriptEngine.put(ScriptEngine.FILENAME, "(none)");
            result = m_ScriptEngine.eval(script);
        }
        session.getConsole().println(result);
    }
    
    /**
     * Execute a JavaScript from the given file.
     * 
     * @param file
     *      file containing script to execute
     * @throws ScriptException
     *      if the script fails in some way
     * @throws IOException
     *      if error occurs with given file
     */
    @Descriptor("Execute a JavaScript")
    public void executeFile(@Descriptor("file containing script to execute")final File file) 
            throws ScriptException, IOException
    {
        final Reader reader = new FileReader(file);
        try
        {
            synchronized (m_ScriptEngine)
            {
                m_ScriptEngine.put(ScriptEngine.FILENAME, file.getPath());
                m_ScriptEngine.eval(reader);
            }
        }
        finally
        {
            reader.close();
        }
    }
}
