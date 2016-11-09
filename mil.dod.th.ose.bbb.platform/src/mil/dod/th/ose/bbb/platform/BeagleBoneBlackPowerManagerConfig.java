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
package mil.dod.th.ose.bbb.platform;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.core.pm.PlatformPowerManager;

/**
 * Configuration properties for the BeagleBone Black platform power manager.
 * 
 * @author cweisenborn
 */
@OCD
public interface BeagleBoneBlackPowerManagerConfig 
{
    /**
     * Configuration property key that represents the standby mode to be used by the system.
     */
    String CONFIG_STANDBY_MODE = "standby.mode";
    
    /**
     * Configuration property key that represents the standby backoff time to be used by the system.
     */
    String CONFIG_BACKOFF_TIME = "standby.backoff.time.ms";
    
    /**
     * Configuration property used to determine if the platform power manager should be enabled.
     * 
     * @return
     *      Whether or not the platform power manager has been enabled.
     */
    @AD(id = PlatformPowerManager.CONFIG_PROP_ENABLED, name = "Enabled",
            description = "Flag indicating if the platform power management is enabled.", required = false, 
            deflt = "false")
    boolean enabled();
    
    /**
     * Configuration property for the minimum time system should be stay awake after being in low power mode.
     * 
     * @return
     *      Minimum wake time in milliseconds.
     */
    @AD(id = PlatformPowerManager.CONFIG_PROP_MIN_WAKE_TIME_MS, name = "Minimum Wake Time (ms)", 
            description = "The minimum wake time in milliseconds", required = false, deflt = "10000")
    long minWakeTimeMs();
    
    /**
     * Configuration property for the time length between standby notice and the system going into low power.
     * 
     * @return
     *      Standby notice time in milliseconds.
     */
    @AD(id = PlatformPowerManager.CONFIG_PROP_STDBY_NOTICE_TIME_MS, name = "Standby Notice Time (ms)", 
            description = "The time, in milliseconds, between standby notice event and the system going into low power "
                    + "mode.", required = false, deflt = "10000")
    long standbyNoticeTimeMs();
    
    /**
     * Configuration property for the time length between a standby notice being canceled and the system issuing
     * another standby notice.
     * 
     * @return
     *      Backoff standby time in milliseconds.
     */
    @AD(id = CONFIG_BACKOFF_TIME, name = "Backoff Standby Time (ms)", description = "The time, in millisecond between "
            + "a standby notice being canceled and the system issuing a new standby notice.", required = false, 
            deflt = "10000")
    long standbyBackoffTimeMs();
    
    /**
     * Configuration property for the max time the system should be in low power.
     * 
     * @return
     *      Returns the max sleep time in milliseconds.
     */
    @AD(id = PlatformPowerManager.CONFIG_PROP_MAX_SLEEP_TIME_MS, name = "Maximum Sleep Time (ms)", 
            description = "The maximum time to sleep for, in milliseconds.", required = false, deflt = "60000")
    long maxSleepTimeMs();
    
    /**
     * Configuration property for the time system should wait at startup.
     * 
     * @return
     *      Start wait time in milliseconds.
     */
    @AD(id = PlatformPowerManager.CONFIG_PROP_STARTUP_TIME_MS, name = "Startup Time (ms)", 
            description = "The startup time in milliseconds", required = false, deflt = "300000")
    long startupTimeMs();
    
    /**
     * Configuration property for the standby mode to be used by the system.
     * 
     * @return
     *      Enumeration that represents the standby mode to be used by the system.
     */
    @AD(id = CONFIG_STANDBY_MODE, name = "Standby Mode", description = "The standby mode to be used by the system.", 
            required = false, deflt = "MEM")
    BeagleBoneBlackStandbyMode standbyMode();
}
