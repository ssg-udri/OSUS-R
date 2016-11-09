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

import mil.dod.th.model.config.PidConfig;

/**
 * OSGi service which creates and sets {@link org.osgi.service.cm.Configuration}s based on configurations passed in.
 * 
 * @author dhumeniuk
 *
 */
public interface OSGiConfigLoader
{

    /**
     * Create the given factory configurations and update properties for non-factory configurations.
     * 
     * @param pidConfigs
     *      configurations to load based on the supplied PID
     * @param firstRun
     *      whether this should be treated as a first run load, factory configurations should only be processed during
     *      the first run
     */
    void process(List<PidConfig> pidConfigs, boolean firstRun);

}
