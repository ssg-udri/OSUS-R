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

/**
 * Class that contains event topic constants related to controller encryption type updates.
 * 
 * @author bachmakm
 */
public final class ControllerEncryptionConstants
{    
    /** Event topic prefix to use for all topics. */
    public static final String TOPIC_PREFIX = "mil/dod/th/ose/gui/api/ControllerEncryption/";
    
    /**
     * Topic used when controller encryption info is updated.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - ID of the 
     * controller whose encryption information has been updated.</li>
     * <li>{@link #EVENT_PROP_ENCRYPTION_TYPE} - the string representation of the 
     * mil.dod.th.core.remote.proto.RemoteBase.EncryptType used by the specified controller.</li>
     * </ul>
     */
    public static final String TOPIC_CONTROLLER_ENCRYPTION_TYPE_UPDATED = TOPIC_PREFIX  
            + "CONTROLLER_ENCRYPTION_TYPE_UPDATED";
    
    /**Event property key for the controller's encryption type.**/
    public static final String EVENT_PROP_ENCRYPTION_TYPE = "controller.encryption.type";
    
    /**
     * Defined to prevent instantiation.
     */
    private ControllerEncryptionConstants()
    {
        
    }
}
