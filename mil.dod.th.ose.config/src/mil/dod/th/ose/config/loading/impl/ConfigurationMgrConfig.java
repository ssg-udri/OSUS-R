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
package mil.dod.th.ose.config.loading.impl;

import aQute.bnd.annotation.metatype.Meta;

/**
 * Interface for configurable properties of the ConfigurationMgr.
 * @author nickmarcucci
 *
 */
@Meta.OCD
public interface ConfigurationMgrConfig
{
    /**
     * Configuration property which indicates whether or not this is the first time that the configuration process has
     * taken place.
     */
    String FIRST_RUN_PROPERTY = "configuration.first.run";
    
    /**
     * Holds the flag which indicates whether or not the configuration process has run for the first time.
     * @return
     *  true if this is the first run; false otherwise.
     */
    @Meta.AD(required = false, deflt = "true", description = "This flag indicates if configuration of the "
            + "THOSE system has been previously run.", id = FIRST_RUN_PROPERTY)
    boolean isFirstRun();
}
