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

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.ose.shared.LogLevel;

/**
 * Configuration interface used by a {@link LogFilter}.
 * 
 * @author dhumeniuk
 */
@OCD
public interface LogFilterConfig
{
    /**
     * Get the regular expression representing the symbolic names of bundles to include in the filter.
     * 
     * @return
     *      regular expression of a bundle symbolic name
     */
    @AD(description = "Regular expression representing the symbolic names of the bundles to include in the filter")
    String bundleSymbolicFilter();
    
    /**
     * Get the lowest severity level this filter will include in logged output.  If the value is {@link 
     * LogLevel#Warning}, both warnings and errors will be logged, but not debug or info messages.
     * 
     * @return
     *      severity level
     */
    @AD(deflt = "Info",
            description = "Lowest severity level this filter will include in logged output.  For example, if the value "
                        + "is Warning, both warnings and errors will be logged, but not debug or info messages")
    LogLevel severity();
}
