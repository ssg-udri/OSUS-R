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
package mil.dod.th.ose.remote.integration.namespace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetOperationModeReponseData;
import mil.dod.th.core.remote.proto.BaseMessages.OperationMode;
import mil.dod.th.core.remote.proto.BaseMessages.SetOperationModeRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.remote.lexicon.controller.capability.ControllerCapabilitiesGen.ControllerCapabilities;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

/**
 * This tests the {@link mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace} of the remote interface.
 * 
 * @author Dave Humeniuk
 *
 */
public class TestBaseNamespace
{
    private Socket socket;
    
    /**
     * Sets up the socket that connects remote interface to the controller
     */
    @Before
    public void setUp() throws Exception
    {
        socket = SocketHostHelper.connectToController();        
    }

    /**
     * Closes the socket.  
     */
    @After
    public void teardown() throws UnknownHostException, IOException
    {       
        socket.close();
    }
    
    /**
     * Verify sending a controller info request message returns with controller info 
     */
    @Test
    public void testRequestControllerInfo() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // send out message
        TerraHarvestMessage request = TerraHarvestMessageHelper.createRequestControllerInfoMsg();
        request.writeDelimitedTo(socket.getOutputStream());

        // read in response
        Message response = listener.waitForMessage(Namespace.Base, 
                BaseMessageType.ControllerInfo, 300); 
        BaseNamespace namespaceResponse = (BaseNamespace)response;
        assertThat(namespaceResponse, is(notNullValue()));

        ControllerInfoData systemInfo = ControllerInfoData.parseFrom(namespaceResponse.getData());
        assertThat(systemInfo.getName(), is(notNullValue()));
        assertThat(systemInfo.getBuildInfoCount(), greaterThan(0));
        assertThat(systemInfo.getCurrentSystemTime(), is(notNullValue()));
    }
    
    /**
     * Verify sending a controller info request message returns with controller info even if namespace is not 
     * explicitly set.
     */
    @Test
    public void testRequestControllerInfoDefaultNamesapce() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // send out message without setting the namespace (so don't use helper class)
        BaseNamespace baseNamespaceMsg = BaseNamespace.newBuilder().setType(
                BaseMessageType.RequestControllerInfo).build();
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.Base).
                setNamespaceMessage(baseNamespaceMsg.toByteString()).build();
        TerraHarvestMessage request = TerraHarvestMessage.newBuilder().setVersion(RemoteConstants.SPEC_VERSION)
                .setSourceId(TerraHarvestMessageHelper.getSourceId())
                .setDestId(TerraHarvestMessageHelper.getControllerId())
                .setMessageId(0)
                .setTerraHarvestPayload(payload.toByteString())
                .build();
        request.writeDelimitedTo(socket.getOutputStream());

        // read in response
        Message response = listener.waitForMessage(Namespace.Base, 
                BaseMessageType.ControllerInfo, 300);
        BaseNamespace namespaceResponse = (BaseNamespace)response;
        assertThat(namespaceResponse, is(notNullValue()));
        assertThat(namespaceResponse.getType(), is(BaseMessageType.ControllerInfo));
        
        ControllerInfoData controllerInfo = ControllerInfoData.parseFrom(namespaceResponse.getData());
        assertThat(controllerInfo.getName(), is(notNullValue()));
        assertThat(controllerInfo.getVersion(), is(notNullValue()));
        assertThat(controllerInfo.getBuildInfoCount(), greaterThan(0));        
    }

    /**
     * Verify sending a operation mode request message returns with default operation mode of test.
     */
    @Test
    public void testSystemStatus() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        // send out message 
        BaseNamespace.Builder baseNamespaceMsg = BaseNamespace.newBuilder().
                setType(BaseMessageType.GetOperationModeRequest);
        TerraHarvestMessage request = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.Base, baseNamespaceMsg);
        request.writeDelimitedTo(socket.getOutputStream());

        // read in response
        Message response = listener.waitForMessage(Namespace.Base, 
                BaseMessageType.GetOperationModeResponse, 300);
        BaseNamespace namespaceResponse = (BaseNamespace)response;
        assertThat(namespaceResponse, is(notNullValue()));
        assertThat(namespaceResponse.getType(), is(BaseMessageType.GetOperationModeResponse));
        
        GetOperationModeReponseData opMode = GetOperationModeReponseData.parseFrom(namespaceResponse.getData());
        assertThat(opMode.getMode(), is(OperationMode.TEST_MODE));
        
        socket.close();
    }

    /**
     * Verify sending a set operation mode request message changes the operation mode of the controller.
     */
    @Test
    public void testSetOperationStatus() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        // send out message
        SetOperationModeRequestData request = SetOperationModeRequestData.newBuilder().
                setMode(OperationMode.OPERATIONAL_MODE).build();
        BaseNamespace.Builder baseNamespaceMsg = BaseNamespace.newBuilder().
                setType(BaseMessageType.SetOperationModeRequest).
                setData(request.toByteString());
        
        TerraHarvestMessage thRequest = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.Base, baseNamespaceMsg);
        thRequest.writeDelimitedTo(socket.getOutputStream());

        // read in response
        Message response = listener.waitForMessage(Namespace.Base, 
                BaseMessageType.SetOperationModeResponse, 300);
        BaseNamespace namespaceResponse = (BaseNamespace)response;
        assertThat(namespaceResponse, is(notNullValue()));

        assertThat(namespaceResponse.getType(), is(BaseMessageType.SetOperationModeResponse));

        //replay 
        baseNamespaceMsg = BaseNamespace.newBuilder().
                setType(BaseMessageType.GetOperationModeRequest);
        thRequest = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.Base, baseNamespaceMsg);
        thRequest.writeDelimitedTo(socket.getOutputStream());

        // read in response
        Message getSystemResponse = listener.waitForMessage(Namespace.Base, 
                BaseMessageType.GetOperationModeResponse, 500);
        BaseNamespace getSysNamespaceResponse = (BaseNamespace)getSystemResponse;
        assertThat(getSysNamespaceResponse, is(notNullValue()));

        assertThat(getSysNamespaceResponse.getType(), is(BaseMessageType.GetOperationModeResponse));

        GetOperationModeReponseData systemMode = 
                GetOperationModeReponseData.parseFrom(getSysNamespaceResponse.getData());
        assertThat(systemMode.getMode(), is(OperationMode.OPERATIONAL_MODE));

        // send out message to reset to test mode
        request = SetOperationModeRequestData.newBuilder().
                setMode(OperationMode.TEST_MODE).build();
        baseNamespaceMsg = BaseNamespace.newBuilder().
                setType(BaseMessageType.SetOperationModeRequest).
                setData(request.toByteString());
        thRequest = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.Base, baseNamespaceMsg);
        thRequest.writeDelimitedTo(socket.getOutputStream());
    }
    
    /**
     *  
     * Verify sending a get controller capabilities request message returns with
     * generic controller capabilities response.
     *  
     */
    @Test
    public final void testGetControllerCapabilities() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        // send out message
        BaseNamespace.Builder baseNamespaceMsg = 
                BaseNamespace.newBuilder().setType(BaseMessageType.GetControllerCapabilitiesRequest);
        TerraHarvestMessage thRequest = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.Base, baseNamespaceMsg);
        thRequest.writeDelimitedTo(socket.getOutputStream());

        // read in response
        Message response = listener.waitForMessage(Namespace.Base, 
                BaseMessageType.GetControllerCapabilitiesResponse, 300);
        BaseNamespace namespaceResponse = (BaseNamespace)response;
        assertThat(namespaceResponse, is(notNullValue()));

        assertThat(namespaceResponse.getType(), is(BaseMessageType.GetControllerCapabilitiesResponse));
        GetControllerCapabilitiesResponseData dataResponse = 
                GetControllerCapabilitiesResponseData.parseFrom(namespaceResponse.getData());
        //test various capabilities
        ControllerCapabilities caps = dataResponse.getControllerCapabilitiesNative();
        assertThat(caps, is(notNullValue()));
        assertThat(caps.getBase().getDescription(), is("Example Controller"));
        assertThat(caps.getLowPowerModeSupported(), is(false));
        assertThat(caps.getVoltageReported(), is(false));
        assertThat(caps.getLowPowerModeSupported(), is(false));
        assertThat(caps.getBatteryAmpHourReported(), is(false));
        assertThat(caps.getIdOverridden(), is(false));
        assertThat(caps.getNameOverridden(), is(false));
        assertThat(caps.getVersionOverridden(), is(false));
        assertThat(caps.getBuildInfoOverridden(), is(false));
        assertThat(caps.getBase().getPrimaryImage().getEncoding(), is("image/png"));
        
        BufferedImage img= ImageIO.read(
                new ByteArrayInputStream(caps.getBase().getPrimaryImage().getValue().toByteArray()));
        int imHeight = img.getHeight();
        int imWidth = img.getWidth();
        assertThat(imHeight, is(128));
        assertThat(imWidth, is(128));
    }
}
