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
package mil.dod.th.core.remote;

/**
 * Enumerative type for channel statuses.
 * 
 * @author callen
 */
public enum ChannelStatus 
{
    /** Represents that the channel is active, and available. */
    Active,

    /** Represents that the channel is unavailable or otherwise not functioning correctly. */
    Unavailable,

    /** Displayed while determining if the channel is valid. */
    Unknown;
}
