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
 * Enumeration used to indicate the type of data that 
 * is being sent via Push.
 * @author nickmarcucci
 *
 */
public enum PushMessageType
{
    /**
     * Growl Message type indication.
     */
    GROWL_MESSAGE,
    /**
     * Event type indication.
     */
    EVENT;
}
