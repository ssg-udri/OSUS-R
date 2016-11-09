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
package mil.dod.th.ose.remote.encryption;


import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Interface that encrypts the TerraHarvestPayload.
 * @author powarniu
 *
 */
public interface EncryptMessageService 
{
     
    /**
     * Encrypt the payload inside the TerraHarvestMessage.
     * 
     * @param message
     *        TerraHarvestMessage contains encrypt info field and other information necessary
     *        for encryption
     * @param payload 
     *        Input the payload that needs to be encrypted
     * @return TerraHarvestMessage
     *        TerraHarvestMessage contains encrypted payload, encryption type, encryptInfo
     */
    TerraHarvestMessage encryptMessage(TerraHarvestMessage.Builder message, TerraHarvestPayload payload);

    /**
     * Decrypt the payload inside the TerraHarvestMessage.
     * 
     * 
     * @param message
     *     TerraHarvestMessage contains the encrypted payload bytes 
     * @return
     *     returns a decrypted (original) payload
     * @throws InvalidProtocolBufferException
     *      thrown when a protocol message being parsed is invalid
     * @throws InvalidKeySignatureException 
     *      thrown when key signatures can not be verified during decryption
     */
    TerraHarvestPayload decryptRemoteMessage(TerraHarvestMessage message)throws InvalidProtocolBufferException,
            InvalidKeySignatureException;
        
    
}


