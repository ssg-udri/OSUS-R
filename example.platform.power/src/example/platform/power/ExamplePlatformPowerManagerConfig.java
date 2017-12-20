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
package example.platform.power;

import mil.dod.th.core.pm.PlatformPowerManager;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Configuration interface used by a {@link ExamplePlatformPowerManager}.
 * 
 * @author Josh
 */
@OCD
public interface ExamplePlatformPowerManagerConfig
{
    @AD(id = PlatformPowerManager.CONFIG_PROP_ENABLED, name = "Enabled on startup",
            description = "Flag indicating if the platform power management starts enabled.", required = false, 
            deflt = "false")
    boolean enabled();
    
    @AD(id = PlatformPowerManager.CONFIG_PROP_MIN_WAKE_TIME_MS, name = "Minimum Wake Time (ms)", 
            description = "The minimum wake time in milliseconds", required = false, deflt = "10000")
    long minWakeTimeMs();
    
    @AD(id = PlatformPowerManager.CONFIG_PROP_STDBY_NOTICE_TIME_MS, name = "Standby Notice Time (ms)", 
            description = "The time, in milliseconds, between standy notice event and the system going into low power "
                    + "mode.", required = false, deflt ="10000")
    long standbyNoticeTimeMs();
    
    @AD(id = PlatformPowerManager.CONFIG_PROP_MAX_SLEEP_TIME_MS, name = "Maximum Sleep Time (ms)", 
            description = "The maximum time to sleep for, in milliseconds.", required = false, deflt = "60000")
    long maxSleepTimeMs();
    
    @AD(id = PlatformPowerManager.CONFIG_PROP_STARTUP_TIME_MS, name = "Startup Time (ms)", 
            description = "The startup time in milliseconds", required = false, deflt = "1000")
    long startupTimeMs();
}
