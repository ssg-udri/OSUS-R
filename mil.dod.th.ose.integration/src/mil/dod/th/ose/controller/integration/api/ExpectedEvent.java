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
package mil.dod.th.ose.controller.integration.api;

import org.osgi.service.event.Event;

/**
 * Interface for an object which defines the conditions for the acceptance of an expected event.
 * 
 * @author nickmarcucci
 *
 */
public interface ExpectedEvent
{
    /**
     * Callback method used to define the criteria for determining whether an expected 
     * event has occurred.
     * @param event
     *  the event that is to be used in determining whether the expected event occurred.
     * @return
     *  true if the expected event occurred based on the given event; false otherwise
     */
    boolean isExpectedEvent(Event event);
}
