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
package mil.dod.th.ose.gui.api;

import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;

/**
 * This interface describes the service that allows the retrieval of the encryption type for a system asynchronously 
 * and  which handles responding to encryption namespace messages. Handles updating the 
 * {@link mil.dod.th.core.remote.RemoteSystemEncryption} service with that information. Also, produces event to notify 
 * subscribers that a system's encryption type has been updated.
 * 
 * @author cweisenborn
 */
public interface EncryptionTypeManager
{

    /**
     * Retrieve the encryption type for the controller with the specified ID. Will send an 
     * get encryption type request message if the encryption type is unknown.
     * 
     * @param controllerId
     *      ID of the controller
     * @return
     *      The encryption type or null if the encryption type is not known.
     */
    EncryptType getEncryptTypeAsnyc(int controllerId);
}
