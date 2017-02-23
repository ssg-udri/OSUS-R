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
package mil.dod.th.ose.remote.integration.ssl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;

import org.junit.Test;

/**
 * This test class verifies that SSL connections can be made to a controller.
 * 
 * @author dlandoll
 */
public class TestSslSocketChannel
{
    @Test
    public void testSslConnectionAndShutdown() throws IOException, NoSuchAlgorithmException
    {
        try (Socket socket = SocketHostHelper.connectToControllerSsl())
        {
            // send out message
            TerraHarvestMessage request = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.Base,
                    BaseNamespace.newBuilder().setType(BaseMessageType.ShutdownSystem));
            request.writeDelimitedTo(socket.getOutputStream());

            // read in response
            TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
            TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
            assertThat(response, is(notNullValue()));
            assertThat(payLoadTest.getNamespace(), is(Namespace.Base));

            BaseNamespace namespaceResponse = BaseNamespace.parseFrom(payLoadTest.getNamespaceMessage());
            assertThat(namespaceResponse.getType(), is(BaseMessageType.ReceivedShutdownRequest));        
        }
    }
}
