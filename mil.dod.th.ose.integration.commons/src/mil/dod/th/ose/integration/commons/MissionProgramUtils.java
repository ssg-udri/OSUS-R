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
package mil.dod.th.ose.integration.commons;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;

import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;

public class MissionProgramUtils
{
    public static void shutdownSync(BundleContext context, Program program)
    {
        EventHandlerSyncer shutdownSync = new EventHandlerSyncer(context, Program.TOPIC_PROGRAM_SHUTDOWN, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, program.getProgramName()));
        program.executeShutdown();
        shutdownSync.waitForEvent(5);
    }

    public static void removeAllPrograms(BundleContext context)
    {
        MissionProgramManager missionProgramManager = ServiceUtils.getService(context, MissionProgramManager.class);
        for (Program program : missionProgramManager.getPrograms())
        {
            // stop program is currently executing
            if (program.getProgramStatus() == ProgramStatus.EXECUTED)
            {
                EventHandlerSyncer shutdownSyncer = new EventHandlerSyncer(context, 
                        new String[] { Program.TOPIC_PROGRAM_SHUTDOWN, Program.TOPIC_PROGRAM_SHUTDOWN_FAILURE }, 
                        String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, program.getProgramName()));
                
                program.executeShutdown();
                
                shutdownSyncer.waitForEvent(5);
            }
            
            program.remove();
        }
    }
}
