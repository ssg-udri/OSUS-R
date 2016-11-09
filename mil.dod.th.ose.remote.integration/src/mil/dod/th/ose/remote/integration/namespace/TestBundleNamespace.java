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

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;

import mil.dod.th.core.remote.proto.BundleMessages.BundleInfoType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundlesResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.BundleMessages.UpdateRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.integration.BundleNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.ResourceUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;

/**
 * Test routing of messages to the {@link BundleNamespace}.
 * @author callen
 *
 */
public class TestBundleNamespace 
{
    private Socket socket;
    
    /**
     * Create and connect the socket.
     */
    @Before
    public void setup() throws UnknownHostException, IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
    }
    
    /**
     * close the socket.
     */
    @After
    public void teardown() throws UnknownHostException, IOException, InterruptedException
    {   
        try
        {
            MessageListener.unregisterEvent(socket);
        }
        finally
        {
            socket.close();
        }
    }

    /**
     * Test install bundle request.
     * Test uninstall bundle request.
     * Verify responses are returned.
     */
    @Test
    public void testBundleInstallRequest() throws UnknownHostException, IOException, InterruptedException
    {
        //install
        long bundleId = BundleNamespaceUtils.installBundle(ResourceUtils.getExampleProjectBundleFile(), 
                "test.my.bundle", false, socket);
        //uninstall, will reinstall for future tests
        BundleNamespaceUtils.uninstallBundle(bundleId, socket);
    }

    /**
     * Test start bundle request.
     * 
     * Verify bundle is started.
     */
    @Test
    public void testStartStopBundleRequest() throws UnknownHostException, IOException, InterruptedException
    {
        //install the bundle
        long bundleId = BundleNamespaceUtils.installBundle(ResourceUtils.getExampleProjectBundleFile(), 
                "test.my.bundle", false, socket);
        
        //send start request
        BundleNamespaceUtils.startBundle(bundleId, socket);
        
        //stop and uninstall, will reinstall for future tests
        BundleNamespaceUtils.stopBundle(bundleId, socket);
        BundleNamespaceUtils.uninstallBundle(bundleId, socket);
    }
    
    /**
     * Test get bundle info request.
     * 
     * Verify bundle info is returned.
     */
    @Test
    public void testBundleInfoRequest() throws UnknownHostException, IOException, InterruptedException
    {
        //construct request, get info for system bundle
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().
            setBundleId(0L).
            setBundleDescription(true).
            setBundleSymbolicName(true).
            setBundleState(true).
            setPackageImports(true).
            setPackageExports(true).build();
        TerraHarvestMessage message = 
                BundleNamespaceUtils.createBundleMessage(request, BundleMessageType.GetBundleInfoRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());

        //read in response
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(socket.getInputStream());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        assertThat(response, is(notNullValue()));
        assertThat(payLoadTest.getNamespace(), is(Namespace.Bundle));

        //parse specific message type
        BundleNamespace namespaceResponse = BundleNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        assertThat(namespaceResponse.getType(), is(BundleMessageType.GetBundleInfoResponse));
        
        GetBundleInfoResponseData responseData = GetBundleInfoResponseData.parseFrom(namespaceResponse.getData());
       
        assertThat(responseData.getInfoDataCount(), is(1));
        
        BundleInfoType type = responseData.getInfoData(0);
        
        assertThat(type.getBundleId(), is(0L));
        assertThat(type.getBundleSymbolicName(), is("org.apache.felix.framework"));
        assertThat(type.getPackageExportList().get(0).contains("org.osgi.framework;"), is(true));
        //hex representation of the the bundle active status
        assertThat(type.getBundleState(), is(0x00000020));
    }
    
    /**
     * Verify that with no bundle id specified, all bundles are returned. 
     */
    @Test
    public void testBundleInfoRequestWithNoSpecifiedId() throws UnknownHostException, IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        GetBundlesResponseData info = BundleNamespaceUtils.getBundles(socket);
        int totalBundles = info.getBundleIdCount();
        
        //construct request, get info for system bundle
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().
            setBundleDescription(true).
            setBundleSymbolicName(true).
            setBundleState(true).
            setPackageImports(true).
            setPackageExports(true).build();
        TerraHarvestMessage message = 
                BundleNamespaceUtils.createBundleMessage(request, BundleMessageType.GetBundleInfoRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());
        
        BundleNamespace namespaceResponse = (BundleNamespace)listener.waitForMessage(Namespace.Bundle, 
                BundleMessageType.GetBundleInfoResponse, 1000);

        GetBundleInfoResponseData responseData = GetBundleInfoResponseData.parseFrom(namespaceResponse.getData());
        
        assertThat(responseData.getInfoDataCount(), is(totalBundles));
    }
    
    /**
     * Test get bundles request.
     */
    @Test
    public void testGetBundlesRequest() throws UnknownHostException, IOException
    {
        //test that there are no bundles and the an empty list is returned
        GetBundlesResponseData bundleResponse = BundleNamespaceUtils.getBundles(socket);
        
        //verify, should be greater than 4
        assertThat(bundleResponse.getBundleIdCount(), greaterThan(4));
        
        //find the system bundle, its id is always 0
        boolean found = false;
        for (Long bundle : bundleResponse.getBundleIdList())
        {
            if (bundle == 0)
            {
                found = true;
            }
        }
        assertThat(found, is(true));
    }
    
    /**
     * Test update bundle request.
     * 
     * Verify response is returned.
     */
    @Test
    public void testBundleUpdateRequest() throws UnknownHostException, IOException, InterruptedException
    {
        //listener that will wait for messages
        MessageListener listener = new MessageListener(socket);
        
        //install the bundle
        long bundleId = BundleNamespaceUtils.installBundle(ResourceUtils.getExampleProjectBundleFile(), 
                "test.my.bundle", false, socket);

        byte[] buf = FileUtils.readFileToByteArray(ResourceUtils.getExampleProjectBundleFile());
        
        //request
        UpdateRequestData request = UpdateRequestData.newBuilder().
            setBundleId(bundleId).
            setBundleFile(ByteString.copyFrom(buf)).build();
        TerraHarvestMessage message = 
                BundleNamespaceUtils.createBundleMessage(request, BundleMessageType.UpdateRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());

        //read in response
        listener.waitForMessage(Namespace.Bundle, BundleMessageType.UpdateResponse, 500);
        
        // uninstall the bundle
        BundleNamespaceUtils.uninstallBundle(bundleId, socket);
    }  
}
