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

import java.util.UUID;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.messaging.TerraHarvestMessageUtil;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.Version;

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
        TerraHarvestPayload payLoad = TerraHarvestPayload.newBuilder().setNamespace(namespace).
            setNamespaceMessage(message.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageUtil.getPartialMessage().
            setSourceId(sourceId).
            setDestId(destId).
            setTerraHarvestPayload(payLoad.toByteString()).
            setMessageId(messageId).
            build();

        //return the message
        return thMessage;
    }
    
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
     * @param type
     *     the encryption type to be used for the message
     * @return
     *     a valid terra harvest message
     */
    public static TerraHarvestMessage createTerraHarvestMessage(final int sourceId, final int destId,
        final Namespace namespace, final int messageId, final Message message, final EncryptType type)
    {
        TerraHarvestPayload payLoad = TerraHarvestPayload.newBuilder().setNamespace(namespace).
            setNamespaceMessage(message.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageUtil.getPartialMessage().
            setSourceId(sourceId).
            setDestId(destId).
            setTerraHarvestPayload(payLoad.toByteString()).
            setMessageId(messageId).
            setEncryptType(type).
            build();

        //return the message
        return thMessage;
    }
    
    /**
     * Create a basic message that can be sent, Base namespace and SystemInfo.
     * 
     * @return
     *      a simple message
     */
    public static TerraHarvestMessage createBaseMessage()
    {
        // construct a single base namespace message to verify sent to socket
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .build();
        
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Base, 100, baseNamespaceMessage);
    }
    
    /**
     * Create a proto observation with required fields filled in.
     * @return
     *      a proto obs
     */
    public static ObservationGen.Observation getProtoObs()
    {
        UUID obsGenUuid = UUID.randomUUID();
        UUID assetUuid = UUID.randomUUID();
        //Observation proto message
        ObservationGen.Observation obsGen = ObservationGen.
                Observation.newBuilder().
                setSystemInTestMode(true).
                setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(assetUuid)).
                setAssetName("Asset").
                setCreatedTimestamp(System.currentTimeMillis()).
                setAssetType("a.new.Asset").
                setVersion(Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
                setSystemId(0).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(obsGenUuid)).build();
        
        return obsGen;
    }
}
