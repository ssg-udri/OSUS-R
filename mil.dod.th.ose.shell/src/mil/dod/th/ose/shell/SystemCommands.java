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

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import aQute.bnd.annotation.component.Component;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;

/**
 * General system command to get information about the underlying OS/platform.
 * 
 * @author dhumeniuk
 *
 */
@Component(provide = SystemCommands.class, properties = { "osgi.command.scope=those", 
        "osgi.command.function=memory|memoryPools" })
public class SystemCommands
{
    /**
     * Display memory usage statistics.
     * 
     * @param session
     *            provides access to the Gogo shell session
     */
    @Descriptor("Display heap and non-heap memory usage")
    public void memory(final CommandSession session)
    {
        final PrintStream out = session.getConsole();
        
        out.format("heap: %s%n", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
        out.format("non-heap: %s%n", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());
    }
    
    /**
     * Display memory pool statistics for peak usage.
     * 
     * @param session
     *            provides access to the Gogo shell session
     */
    @Descriptor("Display memory pool statistics for peak usage")
    public void memoryPools(final CommandSession session)
    {
        final PrintStream out = session.getConsole();
        
        out.println("Peak Usage:");
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans())
        {
            out.format("%s (%s): %s%n", pool.getName(), pool.getType(), pool.getPeakUsage());
        }
    }
}
