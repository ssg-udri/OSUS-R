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

import mil.dod.th.model.config.AddressConfig;

/**
 * OSGi service which loads {@link mil.dod.th.core.ccomm.Address}'s based on a configuration.
 * 
 * @author dhumeniuk
 *
 */
public interface AddressLoader
{
    /**
     * Load the given address configurations.
     * 
     * @param addressConfigs
     *      configurations to load
     * @param firstRun
     *      whether this should be treated as a first run load (configuration items can be marked to be created on first
     *      run only)
     */
    void process(List<AddressConfig> addressConfigs, boolean firstRun);
}
