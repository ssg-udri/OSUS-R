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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoErrorResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionErrorCode;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;
import mil.dod.th.ose.remote.messaging.TerraHarvestMessageUtil;

import org.junit.Test;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author Dave Humeniuk
 *
 */
public class TestTerraHarvestMessageUtil
{
    /**
     * Test utility method to verify basic info is filled out (really, just the spec version).
     */
    @Test
    public void testGetPartialMessage()
    {
        // get partial message, set necessary fields and build
        //build a TerraHarvestPayLoad message
        final TerraHarvestPayload payLoad = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(ByteString.EMPTY).
                build();
        TerraHarvestMessage message = TerraHarvestMessageUtil.getPartialMessage()
                .setSourceId(0)
                .setDestId(1)
                .setMessageId(2)
                .setTerraHarvestPayload(payLoad.toByteString())
                .build();
        
        assertThat(message.getVersion(), is(RemoteConstants.SPEC_VERSION));
    }
    
    /**
     * Verify that a error response message can be built, and has the correct values from the util class.
     * 
     */
    @Test
    public final void testBuildErrorResponseMessage() throws InvalidProtocolBufferException
    {
        //build the data first
        final GenericErrorResponseData genericErrorResponseMessage = GenericErrorResponseData.newBuilder().
                setError(ErrorCode.ILLEGAL_STATE).
                setErrorDescription("errorrrrrr").
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
       
        TerraHarvestMessage requestMessage = TerraHarvestMessageUtil.getPartialMessage().
                setSourceId(0).
                setDestId(1).
                setMessageId(100).             
                setTerraHarvestPayload(payLoad.toByteString()).
                build();
        
        TerraHarvestMessage testMessage = TerraHarvestMessageUtil.buildErrorResponseMessage(requestMessage, 
                ErrorCode.ILLEGAL_STATE, "errorrrrrr");

        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(testMessage.getTerraHarvestPayload());
        BaseNamespace baseTest = BaseNamespace.parseFrom(payLoadTest.getNamespaceMessage()); 
        GenericErrorResponseData genericErrorResponseTest = GenericErrorResponseData.parseFrom(baseTest.getData());
        
        assertThat(testMessage.getVersion(), is(RemoteConstants.SPEC_VERSION));
        assertThat(genericErrorResponseTest.getError(), is(ErrorCode.ILLEGAL_STATE));
        assertThat(genericErrorResponseTest.getErrorDescription(), is("errorrrrrr"));
        assertThat(testMessage.getSourceId(), is(1));
        assertThat(testMessage.getDestId(), is(requestMessage.getSourceId()));
        assertThat(testMessage.getMessageId(), is(requestMessage.getMessageId()));
        assertThat(payLoadTest.getNamespace(), is(Namespace.Base));
        assertThat(payLoadTest.getNamespaceMessage(), is(baseMessage.toByteString()));
    }
    
    /**
     * Verify that a error response message can be built, and has the correct values from the util class.
     * 
     */
    @Test
    public final void testBuildEncryptionErrorResponseMessage() throws InvalidProtocolBufferException
    {    
        // build a base namespace message
        final BaseNamespace namespaceMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.RequestControllerInfo).
                build();
        //build a TerraHarvestPayLoad message
        final TerraHarvestPayload payLoad = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
       
        TerraHarvestMessage requestMessage = TerraHarvestMessageUtil.getPartialMessage().
                setSourceId(0).
                setDestId(1).
                setMessageId(100).             
                setTerraHarvestPayload(payLoad.toByteString()).
                build();
        
        TerraHarvestMessage testMessage = TerraHarvestMessageUtil.buildEncryptionErrorResponseMessage(requestMessage, 
                EncryptionErrorCode.INVALID_ENCRYPTION_LEVEL, "errorrrrrr", EncryptionMode.AES_ECDH_ECDSA);

        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(testMessage.getTerraHarvestPayload());
        EncryptionInfoNamespace encryptionTest = EncryptionInfoNamespace.parseFrom(payLoadTest.getNamespaceMessage()); 
        EncryptionInfoErrorResponseData encryptionErrorResponseTest = 
                EncryptionInfoErrorResponseData.parseFrom(encryptionTest.getData());
        
        assertThat(testMessage.getVersion(), is(RemoteConstants.SPEC_VERSION));
        assertThat(encryptionErrorResponseTest.getError(), is(EncryptionErrorCode.INVALID_ENCRYPTION_LEVEL));
        assertThat(encryptionErrorResponseTest.getErrorDescription(), is("errorrrrrr"));
        assertThat(encryptionErrorResponseTest.getType(), is(EncryptType.AES_ECDH_ECDSA));
        assertThat(testMessage.getSourceId(), is(requestMessage.getDestId()));
        assertThat(testMessage.getDestId(), is(requestMessage.getSourceId()));
        assertThat(testMessage.getMessageId(), is(requestMessage.getMessageId()));
        assertThat(payLoadTest.getNamespace(), is(Namespace.EncryptionInfo));
        assertThat(payLoadTest.getNamespaceMessage(), is(encryptionTest.toByteString()));
    }
}
