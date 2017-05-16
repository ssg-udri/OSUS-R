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
package mil.dod.th.ose.gui.webapp.comms;

/**
 * Describes view scoped service that sends requests to create new comms stacks.
 * @author allenchl
 *
 */
public interface AddCommsMessageController 
{
    /**
     * Method which sends series of remote requests to successfully build a new comms stack.
     * @param systemId
     *      ID of the controller to send the request
     * @param model
     *      the comms creation model containing the stack information to request
     */
    void submitNewCommsStackRequest(int systemId, CommsStackCreationModel model);
}
