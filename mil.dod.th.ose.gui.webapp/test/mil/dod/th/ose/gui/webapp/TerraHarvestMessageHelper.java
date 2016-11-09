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
package mil.dod.th.ose.gui.webapp;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

import com.google.protobuf.Message;

/**
 * Helper class to assist with the creation of valid {@link TerraHarvestMessage}s.
 * @author callen
 *
 */
public class TerraHarvestMessageHelper 
{
    /**
     * Create a valid terra harvest message.
     * @param sourceId
     *     the source id desired
     * @param destId
     *     the destination id desired
     * @param namespace
     *     the namespace this terra harvest message is representing
     * @param messageId
     *     the desired message id
     * @param message
     *     the namespace message to insert
     * @return
     *     a valid terra harvest message
     */
    public static TerraHarvestMessage createTerraHarvestMessage(final int sourceId, final int destId,
        final Namespace namespace, final int messageId, final Message message)
    {
        final TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
                setNamespace(namespace).
                setNamespaceMessage(message.toByteString()).
                build();
        TerraHarvestMessage thMessage = TerraHarvestMessage.newBuilder().
            setVersion(RemoteConstants.SPEC_VERSION).
            setSourceId(sourceId).
            setDestId(destId).
            setMessageId(messageId).
            setTerraHarvestPayload(payload.toByteString()).build();

        //return the message
        return thMessage;
    }
    
    public static TerraHarvestMessage createTerraHarvestMessage(final TerraHarvestPayload payload)
    {
        TerraHarvestMessage thMessage = TerraHarvestMessage.newBuilder().
            setVersion(RemoteConstants.SPEC_VERSION).
            setSourceId(0).
            setDestId(1).
            setMessageId(100).
            setTerraHarvestPayload(payload.toByteString()).build();

        //return the message
        return thMessage;
    }
    
    public static void handleAssetMessage(final AssetMessageType type, final Message data,
            final ResponseHandler handler)
    {
        AssetNamespace namespaceMsg = AssetNamespace.newBuilder().setType(type).setData(data.toByteString()).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.Asset)
                .setNamespaceMessage(namespaceMsg.toByteString())
                .build();
        TerraHarvestMessage thMessage = createTerraHarvestMessage(payload);
        handler.handleResponse(thMessage, payload, namespaceMsg, data);
    }
    
    public static void handleBaseMessage(final BaseMessageType type, final Message data,
            final ResponseHandler handler)
    {
        BaseNamespace namespaceMsg = BaseNamespace.newBuilder().setType(type).setData(data.toByteString()).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.Base)
                .setNamespaceMessage(namespaceMsg.toByteString())
                .build();
        TerraHarvestMessage thMessage = createTerraHarvestMessage(payload);
        handler.handleResponse(thMessage, payload, namespaceMsg, data);
    }
}
