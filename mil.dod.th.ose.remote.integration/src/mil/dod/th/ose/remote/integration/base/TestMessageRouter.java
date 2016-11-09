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
package mil.dod.th.ose.remote.integration.base;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;

import org.junit.Test;

import com.google.protobuf.ByteString;

/**
 * @author matt
 */
public class TestMessageRouter 
{
    /**
     * Verify invalid data returns a response after an exception is thrown when attempting to handle a message.
     */
    @Test
    public void testMessageRouterInvalidData() throws UnknownHostException, IOException
    {
        // verify can make connection
        try (Socket socket = SocketHostHelper.connectToController())
        {
            // set invalid data for the custom comms namespace builder and verify that
            // a message is still sent back after an exception is thrown
            CustomCommsNamespace.Builder customCommsMessage = CustomCommsNamespace.newBuilder().
                    setType(CustomCommsMessageType.CreatePhysicalLinkRequest).
                    setData(ByteString.copyFromUtf8("ImInvalidddd"));
                
            //send an invalid namespace
            TerraHarvestMessage message = TerraHarvestMessageHelper.
                    createTerraHarvestMsg(Namespace.CustomComms, customCommsMessage);
            
            message.writeDelimitedTo(socket.getOutputStream());
            
            // read in response
            TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
            TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
            
            BaseNamespace baseTest = BaseNamespace.parseFrom(payLoadTest.getNamespaceMessage()); 
            GenericErrorResponseData genericErrorResponseTest = GenericErrorResponseData.parseFrom(baseTest.getData());
           
            assertThat(response.getVersion(), is(RemoteConstants.SPEC_VERSION));
            assertThat(genericErrorResponseTest.getError(), is(ErrorCode.INVALID_REQUEST));
            assertThat(genericErrorResponseTest.getErrorDescription(), 
                is("Failed IO operation when handling [CustomComms] namespace message: " 
                        + "Protocol message end-group tag did not match expected tag."));
    
            assertThat(payLoadTest.getNamespace(), is(Namespace.Base));
            assertThat(baseTest.getType(), is(BaseMessageType.GenericErrorResponse));
        }
    }
}