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
package mil.dod.th.ose.gui.webapp.utils.push;

/**
 * This class contains constants for the various push channels that 
 * are used to transmit different types of data.
 * 
 * @author nickmarcucci
 *
 */
public final class PushChannelConstants
{
    /**
     * Channel used for all THOSE GUI events and messages which require a push update.
     */
    public static final String PUSH_CHANNEL_THOSE_MESSAGES = "/thoseMessages";

    /**
     * Defined to prevent instantiation.
     */
    private PushChannelConstants()
    {
        
    }
}
