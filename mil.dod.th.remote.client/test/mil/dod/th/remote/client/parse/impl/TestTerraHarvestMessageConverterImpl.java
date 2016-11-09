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
package mil.dod.th.remote.client.parse.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;

import org.junit.Test;

public class TestTerraHarvestMessageConverterImpl
{
    private static int DEST_ID = 1;
    private static int SRC_ID = 2;
    private static int MSG_ID = 11;

    private TerraHarvestMessageConverterImpl m_SUT;

    /**
     * Verify that a message for all namespaces (that are supported) can be converted.
     */
    @Test
    public void testConvertMessages() throws InvalidProtocolBufferException
    {
        m_SUT = new TerraHarvestMessageConverterImpl();

        for (Namespace type : Namespace.values())
        {
            verifyConverter(type);
        }
    }

    private void verifyConverter(Namespace type) throws InvalidProtocolBufferException
    {
        TerraHarvestMessage message;
        Message namespaceMessage = null;
        switch (type)
        {
            case Asset:
                message = null;
                break;
            case AssetDirectoryService:
                namespaceMessage = createADSNamespaceMessage();
                message = createTestMessage(type, namespaceMessage);
                break;
            case Base:
                namespaceMessage = createBaseNamespaceMessage();
                message = createTestMessage(type, namespaceMessage);
                break;
            case Bundle:
                message = null;
                break;
            case ConfigAdmin:
                message = null;
                break;
            case CustomComms:
                message = null;
                break;
            case DataStreamService:
                namespaceMessage = createDataStreamServiceNamespaceMessage();
                message = createTestMessage(type, namespaceMessage);
                break;
            case DataStreamStore:
                message = null;
                break;
            case EncryptionInfo:
                message = null;
                break;
            case EventAdmin:
                namespaceMessage = createEventAdminNamespaceMessage();
                message = createTestMessage(type, namespaceMessage);
                break;
            case LinkLayer:
                message = null;
                break;
            case MetaType:
                message = null;
                break;
            case MissionProgramming:
                message = null;
                break;
            case ObservationStore:
                message = null;
                break;
            case Persistence:
                message = null;
                break;
            case PhysicalLink:
                message = null;
                break;
            case PowerManagement:
                message = null;
                break;
            case RemoteChannelLookup:
                message = null;
                break;
            case TransportLayer:
                message = null;
                break;
            default:
                throw new UnsupportedOperationException("Unknown namespace type " + type.name());
        }

        RemoteMessage<?> remoteMessage;
        if (message == null)
        {
            TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(type)
                .setNamespaceMessage(ByteString.EMPTY)
                .build();

            message = TerraHarvestMessage.newBuilder()
                .setSourceId(SRC_ID)
                .setDestId(DEST_ID)
                .setEncryptType(EncryptType.NONE)
                .setIsResponse(true)
                .setMessageId(MSG_ID)
                .setVersion(RemoteConstants.SPEC_VERSION)
                .setTerraHarvestPayload(payload.toByteString())
                .build();

            assertThat(m_SUT.isSupported(type), is(false));

            remoteMessage = m_SUT.convertMessage(message);
            assertThat(remoteMessage, nullValue());
        }
        else
        {
            assertThat(m_SUT.isSupported(type), is(true));

            remoteMessage = m_SUT.convertMessage(message);
            assertThat(remoteMessage.getDestId(), is(DEST_ID));
            assertThat(remoteMessage.getNamespace(), is(type));
            assertThat(remoteMessage.getNamespaceMessage(), is(namespaceMessage));
            assertThat(remoteMessage.getRawMessage(), is(message));
            assertThat(remoteMessage.getSrcId(), is (SRC_ID));
            assertThat(remoteMessage.getRawMessage().getMessageId(), is(MSG_ID));
        }
    }

    private Message createEventAdminNamespaceMessage()
    {
        EventAdminNamespace namespaceMessage = EventAdminNamespace.newBuilder()
            .setType(EventAdminNamespace.EventAdminMessageType.CleanupRequest)
            .build();

        return namespaceMessage;
    }

    private Message createADSNamespaceMessage()
    {
        AssetDirectoryServiceNamespace namespaceMessage = AssetDirectoryServiceNamespace.newBuilder()
            .setType(AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType.GetAssetsRequest)
            .build();

        return namespaceMessage;
    }

    private Message createBaseNamespaceMessage()
    {
        GenericErrorResponseData errorData = GenericErrorResponseData.newBuilder()
            .setError(BaseMessages.ErrorCode.ASSET_ERROR)
            .setErrorDescription("Error message")
            .build();

        BaseNamespace namespaceMessage = BaseNamespace.newBuilder()
            .setType(BaseMessageType.GenericErrorResponse)
            .setData(errorData.toByteString())
            .build();

        return namespaceMessage;
    }
    
    private Message createDataStreamServiceNamespaceMessage()
    {
        DataStreamServiceNamespace namespaceMessage = DataStreamServiceNamespace.newBuilder()
                .setType(DataStreamServiceNamespace.DataStreamServiceMessageType.GetStreamProfilesRequest)
                .build();
        return namespaceMessage;
    }

    private TerraHarvestMessage createTestMessage(Namespace namespace, Message namespaceMessage)
    {
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
            .setNamespace(namespace)
            .setNamespaceMessage(namespaceMessage.toByteString())
            .build();

        TerraHarvestMessage thMessage = TerraHarvestMessage.newBuilder()
            .setSourceId(SRC_ID)
            .setDestId(DEST_ID)
            .setEncryptType(EncryptType.NONE)
            .setIsResponse(true)
            .setMessageId(MSG_ID)
            .setVersion(RemoteConstants.SPEC_VERSION)
            .setTerraHarvestPayload(payload.toByteString())
            .build();

        return thMessage;
    }
}
