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
package mil.dod.th.ose.remote;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;

/**
 * Configuration interface for the remote interface.
 * 
 * @author Dave Humeniuk
 */
@OCD
public interface RemoteSettingsConfig
{
    /**
     * The default max message size for a remote channel, 16MB.
     */
    String DFLT_MAX_MSG_SIZE = "16777216";
    
    /**
     * Get whether remote messages should be logged at the debug level.
     * 
     * @return
     *      true if messages should be logged
     */
    @AD(required = false, deflt = "false", description = "If true all remote message will be logged at debug level")
    boolean logRemoteMessages();
    
    /**
     * Get the encryption type to use.
     * 
     * @return
     *     the encryption mode
     */
    @AD(required = false, deflt = "NONE", name = RemoteSettings.KEY_ENCRYPTION_MODE, 
            description = "The level of encryption required to be applied to incoming"
            + " messages. Messages that do not meet this requirement will not be accepted.")
    EncryptionMode encryptionMode();
    
    /**
     * Get the max message size.  if a message exceeds the max size, then that remote channel will be closed.
     * @return
     *         the max message size
     */
    @AD(required = false, deflt = DFLT_MAX_MSG_SIZE, 
            description = "The maximum allowed size of a remote channel message (in bytes). Default is 16MB.")
    long maxMsgSizeInBytes();

    /**
     * Get whether the power management sleep mode should be prevented when remote socket channels are active or
     * connected.
     * 
     * @return
     *      true if sleep mode should be prevented
     */
    @AD(required = false, deflt = "false", description = "If true power management sleep mode will be prevented when"
        + " remote socket channels are active or connected. Note: Does not affect current channels, must reconnect"
        + " for changes to take effect")
    boolean preventSleepMode();
}
