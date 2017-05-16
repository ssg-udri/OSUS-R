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
package mil.dod.th.ose.time.service.util;

/**
 * Interface which defines the ability to set the system time.
 * @author nickmarcucci
 *
 */
public interface ChangeSystemTimeWrapper
{
    /**
     * Sets the system time to the specified long in milliseconds.
     * @param timeToSet
     *  the time to set the system to.
     */
    void setSystemTime(long timeToSet);
}
