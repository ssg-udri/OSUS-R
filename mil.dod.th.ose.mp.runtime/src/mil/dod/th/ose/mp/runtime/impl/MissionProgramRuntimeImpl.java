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
package mil.dod.th.ose.mp.runtime.impl;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.ose.mp.runtime.MissionProgramRuntime;
import mil.dod.th.ose.utils.CoverageIgnore;

/**
 * Provides the {@link MissionProgramRuntime} service.
 * 
 * @author dhumeniuk
 *
 */
@Component
public class MissionProgramRuntimeImpl implements MissionProgramRuntime
{
    @Override
    // simple method not worth the complexity of unit testing that would be required, integration testing more useful
    @CoverageIgnore 
    public ClassLoader getClassLoader()
    {
        return getClass().getClassLoader();
    }
}
