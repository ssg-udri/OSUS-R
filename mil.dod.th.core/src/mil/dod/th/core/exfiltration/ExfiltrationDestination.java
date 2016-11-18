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
package mil.dod.th.core.exfiltration;

/**
 * Enumeration of exfiltration destinations.
 * 
 * @author dlandoll
 */
public enum ExfiltrationDestination
{
    /** Destination corresponding to immediate neighbors. **/
    NEIGHBORS,

    /** Destination corresponding to the entire sensor field. **/
    ENTIRE_FIELD,

    /** Destination corresponding to a backend system(s). **/
    BACKEND;
}
