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
package mil.dod.th.ose.remote.api;

/**
 * Interface containing properties to be used by the remote interface bundle.
 * 
 * @author Dave Humeniuk
 *
 */
public interface RemoteSettings
{
    /**
     * Persistent identity (PID) for the configuration. Providers of this service must use this string as the PID.
     */
    String PID = "mil.dod.th.ose.remote.api.RemoteSettings";
    
    /**
     * Key for the {@link #getEncryptionMode()} configuration property.
     */
    String KEY_ENCRYPTION_MODE = "encryptionMode";
    
    /**
     * Key for the {@link #isLogRemoteMessagesEnabled()} configuration property.
     */
    String KEY_LOG_REMOTE_MESSAGES = "logRemoteMessages";
    
    /**
     * Key for the {@link #getMaxMessageSize()} configuration property.
     */
    String KEY_MAX_MSG_SIZE_BYTES = "maxMsgSizeInBytes";

    /**
     * Key for the {@link #isPreventSleepModeEnabled()} configuration property.
     */
    String KEY_PREVENT_SLEEP_MODE = "preventSleepMode";

    /**
     * Whether logging of remote messages is enabled for the system.
     * 
     * @return
     *      true if logging is enabled, false if not
     */
    boolean isLogRemoteMessagesEnabled();
    
    /**
     * The encryption mode for the system.
     * 
     * @return
     *      the encryption mode or none if no mode is enabled
     */
    EncryptionMode getEncryptionMode();
    
    /**
     * The maximum size for a message.
     * @return
     *        the maximum size, in bytes, of a remote message.
     */
    long getMaxMessageSize();

    /**
     * Whether prevention of the power management sleep mode is enabled for the system when a remote socket channel is
     * active or connected.
     * 
     * @return
     *      true if is enabled, false if not
     */
    boolean isPreventSleepModeEnabled();

    /**
     * Enumeration representing the encryption mode of the remote interface.
     */
    enum EncryptionMode
    {
        /**
         * No encryption.
         */
        NONE,
    
        /**
         * Advanced Encryption Standard with Elliptic Curve Diffie-Hellman/Elliptic Curve Digital Signature Algorithm.
         */
        AES_ECDH_ECDSA;
    }
}
