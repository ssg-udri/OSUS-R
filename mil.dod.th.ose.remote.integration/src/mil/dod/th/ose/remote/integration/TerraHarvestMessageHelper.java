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
package mil.dod.th.ose.remote.integration;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;

/**
 * Use to create basic {@link TerraHarvestMessage}s.
 * 
 * Will fill out system ids based on properties or defaults.
 * 
 * @author Dave Humeniuk
 *
 */
public class TerraHarvestMessageHelper
{
    /**
     * System property that holds the system id of the controller.
     */
    public static final String CONTROLLER_ID_PROP_NAME = "mil.dod.th.ose.remote.controller.id";
    
    /**
     * System property that holds the system id of the source of the message.
     */
    public static final String SOURCE_ID_PROP_NAME = "mil.dod.th.ose.remote.source.id";
    
    /**
     * An addition system id used to mimic multiple systems.
     */
    public static final int ADDITIONAL_SYSTEM_ID = 7;
    
    /**
     * Value is used to set the messageId field of each constructed {@link TerraHarvestMessage}.
     */
    private static int m_NextMessageId;
    
    /**
     * Value is used to set the messageId field of each constructed {@link TerraHarvestMessage} being sent 
     * from the additional controller ID.
     */
    private static int m_NextMessageIdAdditionalSystem;
    
    public static int getControllerId()
    {
        String controllerIdStr = System.getProperty(CONTROLLER_ID_PROP_NAME);
        
        if (controllerIdStr == null)
        {
            return 0;
        }
            
        return Integer.parseInt(controllerIdStr);
    }
    
    public static int getSourceId()
    {
        String sourceIdStr = System.getProperty(SOURCE_ID_PROP_NAME);
        
        if (sourceIdStr == null)
        {
            return 1;
        }
        
        return Integer.parseInt(sourceIdStr);
    }
    
    /**
     * Create a builder for a {@link TerraHarvestMessage} that fills in the source and destination id.
     * 
     * MessageId will be set and incremented for each constructed message.
     */
    public static TerraHarvestMessage createTerraHarvestMsg(Namespace namespace, Message.Builder namnspaceBuilder)
    {
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                                      .setNamespace(namespace)
                                      .setNamespaceMessage(namnspaceBuilder.build().toByteString())
                                      .build();
    
        return TerraHarvestMessage.newBuilder().setVersion(RemoteConstants.SPEC_VERSION)
            .setSourceId(getSourceId())
            .setDestId(getControllerId())
            .setTerraHarvestPayload(payload.toByteString())
            .setMessageId(m_NextMessageId++)
            .build();
    }
    
    /**
     * Create just a simple request system info message.
     */
    public static TerraHarvestMessage createRequestControllerInfoMsg()
    {
        return createTerraHarvestMsg(Namespace.Base,
                BaseNamespace.newBuilder().setType(BaseMessageType.RequestControllerInfo));
    }
    
    /**
     * Create a builder for a {@link TerraHarvestMessage} that fills in the source (id will be 7) and destination id.
     * 
     * MessageId will be set and incremented for each constructed message.
     */
    public static TerraHarvestMessage createAdditionalSystemTerraHarvestMsg(Namespace namespace, 
            Message.Builder namnspaceBuilder)
    {
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                                      .setNamespace(namespace)
                                      .setNamespaceMessage(namnspaceBuilder.build().toByteString())
                                      .build();
    
        return TerraHarvestMessage.newBuilder().setVersion(RemoteConstants.SPEC_VERSION)
            .setSourceId(ADDITIONAL_SYSTEM_ID)
            .setDestId(getControllerId())
            .setTerraHarvestPayload(payload.toByteString())
            .setMessageId(m_NextMessageIdAdditionalSystem++)
            .build();
    }
}
