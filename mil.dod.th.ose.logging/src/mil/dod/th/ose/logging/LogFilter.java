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
package mil.dod.th.ose.logging;

import org.osgi.framework.Bundle;

/**
 * Defines filtering used by the {@link LogWriter} to filter on things like bundle symbolic names and severity level.
 * 
 * @author dhumeniuk
 *
 */
public interface LogFilter
{
    /**
     * Determine if the given bundle matches this filter.
     * 
     * @param bundle
     *      bundle to check
     * @return
     *      true if the given bundle matches, false if not
     */
    boolean matches(Bundle bundle);
    
    /**
     * Get the lowest severity level this filter will include in logged output.  If the value is {@link 
     * org.osgi.service.log.LogService#LOG_WARNING}, both warnings and errors will be logged, but not debug or info 
     * messages.
     * 
     * @return
     *      severity level as defined by {@link org.osgi.service.log.LogService}
     */
    int getSeverity();
}
