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
package mil.dod.th.ose.shared;

/**
 * Severity level that relates to those defined by {@link org.osgi.service.log.LogService}.
 */
public enum LogLevel
{
    /** Equivalent to {@link org.osgi.service.log.LogService#LOG_DEBUG}. */
    Debug,
    /** Equivalent to {@link org.osgi.service.log.LogService#LOG_INFO}. */
    Info,
    /** Equivalent to {@link org.osgi.service.log.LogService#LOG_WARNING}. */
    Warning,
    /** Equivalent to {@link org.osgi.service.log.LogService#LOG_ERROR}. */
    Error
}