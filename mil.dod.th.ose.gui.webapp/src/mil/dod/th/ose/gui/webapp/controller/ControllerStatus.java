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
package mil.dod.th.ose.gui.webapp.controller;

/**
 * Enum of controller statuses. Calculated by the the statuses of all the channels of the controller.
 * @author callen
 *
 */
public enum ControllerStatus 
{
    /** Status of a controller whose channels are all up. */
    Good, 
    
    /** Denotes a controller whose channels are all down. */
    Bad,
    
    /** This status represents a controller whose channels are not all good nor bad. */
    Degraded;
}
