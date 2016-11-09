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
package mil.dod.th.ose.config.loading;

import java.util.List;

import mil.dod.th.model.config.EventConfig;

/**
 *  OSGi service which loads remote event admin messages based on a configuration.
 * @author allenchl
 *
 */
public interface RemoteEventLoader
{
    /**
     * Create the given remote event registrations.
     * 
     * @param eventConfigs
     *      event admin configurations to load
     */
    void process(List<EventConfig> eventConfigs);
}
