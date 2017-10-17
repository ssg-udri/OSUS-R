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
package mil.dod.th.ose.utils;

import java.io.IOException;
import java.util.List;

import aQute.bnd.annotation.ProviderType;

/**
 * OSGi service that handles the creation of processes.
 * 
 * @author cweisenborn
 */
@ProviderType
public interface ProcessService
{
    /**
     * Returns a process based on the specified command arguments.
     * 
     * @param arguments
     *      List of strings that represent the command arguments for the process.
     * @return
     *      A process based on the specified command arguments.
     * @throws IOException
     *      Thrown if the process cannot be started due to an issue with the specified command arguments.
     */
    Process createProcess(List<String> arguments) throws IOException;
}
