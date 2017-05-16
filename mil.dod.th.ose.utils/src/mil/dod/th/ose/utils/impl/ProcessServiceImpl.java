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
package mil.dod.th.ose.utils.impl;

import java.io.IOException;
import java.util.List;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.ose.utils.CoverageIgnore;
import mil.dod.th.ose.utils.ProcessService;

/**
 * Implementation of the {@link ProcessService}.
 * 
 * @author cweisenborn
 */
@Component
public class ProcessServiceImpl implements ProcessService
{    
    @CoverageIgnore
    @Override
    public Process createProcess(final List<String> arguments) throws IOException
    {
        final ProcessBuilder builder = new ProcessBuilder(arguments);
        return builder.start();
    }

}
