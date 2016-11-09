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
package edu.udayton.udri.asset.novatel.message;

import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

/**
 * This interface describes the mechanism which handle message data and outcome interactions.
 * @author allenchl
 *
 */
public interface MessageReceiver
{
    /**
     * Handle a string which contains data message information.
     * @param message
     *      the string which contains the data from a message
     * @throws ValidationFailedException
     *      thrown if the observation created from the data string is not valid
     */
    void handleDataString(String message) throws ValidationFailedException;
    
    /**
     * Handle updating the status if there is an error while trying to acquire message data.
     * @param summaryStatusEnum
     *      the appropriate summary status
     * @param statusDescription
     *      description of the status
     */
    void handleReadError(SummaryStatusEnum summaryStatusEnum, String statusDescription);
}
