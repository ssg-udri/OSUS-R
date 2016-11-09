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
package mil.dod.th.ose.shell.integration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import junit.framework.TestCase;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Test that all SDK plug-ins are available through the respective service.  That they register correctly.
 * 
 * TD: test more of the commands
 * 
 * @author Dave Humeniuk
 *
 */
public class TestThoseCommands extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    private CommandProcessor m_Processor;
    private CommandSession m_Session;
    private PrintStream m_PrintStream;
    private ByteArrayOutputStream m_ByteStream = new ByteArrayOutputStream();

    @Override
    public void setUp()
    {
        m_Processor = ServiceUtils.getService(m_Context, CommandProcessor.class);
        m_PrintStream = new PrintStream(m_ByteStream);
        m_Session = m_Processor.createSession(System.in, m_PrintStream, m_PrintStream);
    }
    
    /**
     * Verify the version command prints out the version.
     */
    public void testVersionCommand() throws Exception
    {
        executeThoseCommand("version");
    }
    
    private void executeThoseCommand(String command) throws Exception
    {
        m_Session.execute("those:" + command);
    }
}
