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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetOperationModeReponseData;
import mil.dod.th.core.remote.proto.BaseMessages.OperationMode;
import mil.dod.th.core.remote.proto.BaseMessages.SetOperationModeRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.remote.client.RemoteMessage;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen.LexiconFormat;

import org.junit.Test;

public class TestBaseParser
{
    private BaseParser m_SUT;

    @Test
    public void testParse() throws InvalidProtocolBufferException
    {
        m_SUT = new BaseParser();

        for (BaseNamespace.BaseMessageType type : BaseNamespace.BaseMessageType.values())
        {
            verifyMessageType(type);
        }
    }

    private void verifyMessageType(BaseNamespace.BaseMessageType type) throws InvalidProtocolBufferException
    {
        ByteString dataMessage;
        switch (type)
        {
            case RequestControllerInfo:
                dataMessage = ByteString.EMPTY;
                break;
            case ControllerInfo:
                dataMessage = ControllerInfoData.newBuilder().setName("name").setVersion("1.0").build().toByteString();
                break;
            case GenericErrorResponse:
                dataMessage = GenericErrorResponseData.newBuilder()
                    .setError(ErrorCode.ASSET_ERROR).setErrorDescription("error").build().toByteString();
                break;
            case ShutdownSystem:
                dataMessage = ByteString.EMPTY;
                break;
            case ReceivedShutdownRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case SetOperationModeRequest:
                dataMessage = SetOperationModeRequestData.newBuilder()
                    .setMode(OperationMode.OPERATIONAL_MODE).build().toByteString();
                break;
            case SetOperationModeResponse:
                dataMessage = ByteString.EMPTY;
                break;
            case GetOperationModeRequest:
                dataMessage = ByteString.EMPTY;
                break;
            case GetOperationModeResponse:
                dataMessage = GetOperationModeReponseData.newBuilder()
                    .setMode(OperationMode.OPERATIONAL_MODE).build().toByteString();
                break;
            case GetControllerCapabilitiesRequest:
                dataMessage = GetControllerCapabilitiesRequestData.newBuilder()
                    .setControllerCapabilitiesFormat(LexiconFormat.Enum.XML).build().toByteString();
                break;      
            case GetControllerCapabilitiesResponse:
                dataMessage = GetControllerCapabilitiesResponseData.newBuilder()
                    .setControllerCapabilitiesXml(ByteString.EMPTY).build().toByteString();
                break; 
            default:
                throw new UnsupportedOperationException("Unknown message type " + type.name());
        }

        BaseNamespace namespaceMessage = BaseNamespace.newBuilder()
                .setType(type)
                .setData(dataMessage)
                .build();
        
        TerraHarvestPayload payload = MessageUtils.createPayload(Namespace.Base, namespaceMessage);
        TerraHarvestMessage thmessage = MessageUtils.createMessage(payload);

        // replay
        RemoteMessage<BaseNamespace> remoteMessage = m_SUT.parse(thmessage, payload);

        // verify
        MessageUtils.verifyRemoteMessage(remoteMessage, Namespace.Base, namespaceMessage, type, dataMessage);
    }
}
