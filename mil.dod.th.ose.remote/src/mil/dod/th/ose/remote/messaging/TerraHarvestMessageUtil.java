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
package mil.dod.th.ose.remote.messaging;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionErrorCode;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoErrorResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;

/**
 * Class is used to create {@link TerraHarvestMessage}s.  Will fill out basic information in a builder so other fields
 * can be filled out.
 * 
 * @author Dave Humeniuk
 *
 */
public final class TerraHarvestMessageUtil
{
    /**
     * Hidden constructor to prevent instantiation.
     */
    private TerraHarvestMessageUtil()
    {
        
    }
    
    /**
     * Get a builder for a {@link TerraHarvestMessage} that has the correct version set based on the core API.
     * 
     * @return
     *      builder for messages
     */
    public static TerraHarvestMessage.Builder getPartialMessage()
    {
        return TerraHarvestMessage.newBuilder().setVersion(RemoteConstants.SPEC_VERSION);
    }
    
    /**
     * Create a terra harvest message for error response handling. This method should be used for all generic errors, 
     * as in those not specific to a namespace.  These errors are defined by {@link ErrorCode}.
     * 
     * @param request
     *      request message that this error response is being sent for
     * @param errorCode
     *      represents the occurrence of a generic error defined by the TerraHarvestMessage ErrorCode enum type
     * @param errorDescription
     *      field can optionally describe the TerraHarvestMessage error in more detail
     * @return 
     *      the terra harvest message created
     */
    public static TerraHarvestMessage buildErrorResponseMessage(final TerraHarvestMessage request, 
            final ErrorCode errorCode, final String errorDescription)
    {
        //build the data first
        final GenericErrorResponseData genericErrorResponseMessage = GenericErrorResponseData.newBuilder().
            setError(errorCode).
            setErrorDescription(errorDescription).
            build();
        
        // build a base namespace message
        final BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GenericErrorResponse).
                setData(genericErrorResponseMessage.toByteString()).
                build();
        //build a TerraHarvestPayLoad message
        final TerraHarvestPayload payLoad = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(baseMessage.toByteString()).
                build();
        // build a terra harvest message
        final TerraHarvestMessage fullMessage = getPartialMessage().
                setSourceId(request.getDestId()).
                // send message back to source
                setDestId(request.getSourceId()).
                setMessageId(request.getMessageId()).
                setTerraHarvestPayload(payLoad.toByteString()).
                build();

        return fullMessage;
    }
    
    /**
     * Creates a terra harvest message for the encryption info error response type. This method should be used for all 
     * encryption errors, as in those specific to the encryption information namespace. These errors are defined by 
     * {@link EncryptionErrorCode}. EncryptionInfo error messages are always sent unencrypted.
     * 
     * @param request
     *      request message that this error response is being sent for
     * @param errorCode
     *      represents the occurrence of a encryption error defined by the TerraHarvestMessage EncryptionErrorCode enum 
     *      type
     * @param errorDescription
     *      field can optionally describe the TerraHarvestMessage error in more detail
     * @param mode
     *      {@link EncryptionMode} of the system
     * @return 
     *      the terra harvest message created
     */
    public static TerraHarvestMessage buildEncryptionErrorResponseMessage(final TerraHarvestMessage request, 
            final EncryptionErrorCode errorCode, final String errorDescription, final EncryptionMode mode)
    {
        //build the data first
        final EncryptionInfoErrorResponseData errorResponseMessage = EncryptionInfoErrorResponseData.newBuilder().
            setError(errorCode).
            setErrorDescription(errorDescription).
            setType(EnumConverter.convertEncryptionModeToEncryptType(mode)).
            build();
        
        // build a base namespace message
        final EncryptionInfoNamespace namespaceMessage = EncryptionInfoNamespace.newBuilder().
                setType(EncryptionInfoMessageType.EncryptionInfoErrorResponse).
                setData(errorResponseMessage.toByteString()).
                build();
        //build a TerraHarvestPayLoad message
        final TerraHarvestPayload payLoad = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.EncryptionInfo).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        
        // build a terra harvest message
        final TerraHarvestMessage fullMessage = getPartialMessage().
                setSourceId(request.getDestId()).
                // send message back to source
                setDestId(request.getSourceId()).
                setMessageId(request.getMessageId()).
                setTerraHarvestPayload(payLoad.toByteString()).
                build();

        return fullMessage;
    }
}
