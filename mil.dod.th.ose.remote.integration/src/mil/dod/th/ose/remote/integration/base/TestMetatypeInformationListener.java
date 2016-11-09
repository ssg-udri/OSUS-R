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
import static org.hamcrest.Matchers.*;
import static mil.dod.th.ose.test.matchers.Matchers.*;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;

import mil.dod.th.core.remote.RemoteMetatypeConstants;
import mil.dod.th.core.remote.proto.BundleMessages.StartRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.StopRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.BundleMessages.UpdateRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.integration.BundleNamespaceUtils;
import mil.dod.th.ose.remote.integration.MatchCount;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.RemoteEventRegistration;
import mil.dod.th.ose.remote.integration.ResourceUtils;
import mil.dod.th.ose.remote.integration.SharedRemoteInterfaceUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.MessageListener.MessageDetails;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the Metatype information listener. The metatype listener posts an event when a bundle/service becomes available
 * and the service provides or otherwise maintains metadata.
 * @author callen
 *
 */
public class TestMetatypeInformationListener
{
    private static Socket socket;
    
    private static int TIME_OUT = 3000; 

    /**
     * Setup the socket 
     */
    @Before
    public void setup() throws UnknownHostException, IOException
    {
        socket = SocketHostHelper.connectToController();
    }

    /**
     * Make sure to clean up any registrations.  
     */
    @After
    public void tearDown() throws UnknownHostException, IOException
    {
        MessageListener.unregisterEvent(socket);
        socket.close();
    }

    /**
     * Test stopping and starting the example asset bundle.
     * Verify Meta type information available event is received with expected information.
     */
    @Test
    public void testMetatypeInformationInstalledBundle() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);

        //find the test bundle
        long testBundleId = BundleNamespaceUtils.getBundleBySymbolicName(
                "mil.dod.th.ose.integration.example.metatype", socket);

        //construct request to stop bundle
        StopRequestData request = StopRequestData.newBuilder().setBundleId(testBundleId).build();
        TerraHarvestMessage message = BundleNamespaceUtils.createBundleMessage(request, BundleMessageType.StopRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());

        int regId = RemoteEventRegistration.regRemoteEventMessages(socket, 
                RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE);

        //construct request to start bundle
        StartRequestData requestStart = StartRequestData.newBuilder().setBundleId(testBundleId).build();
        message = BundleNamespaceUtils.createBundleMessage(requestStart, BundleMessageType.StartRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());

        //going to be at least one because there is the example XML class
        List<MessageDetails> responses = listener.waitForRemoteEvents(
                RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE, TIME_OUT, MatchCount.atLeast(1));
        
        //unreg listener
        MessageListener.unregisterEvent(regId, socket);

        //parse Response
        EventAdminNamespace namespace = (EventAdminNamespace)responses.get(0).getNamespaceMessage();
        SendEventData event = SendEventData.parseFrom(namespace.getData());

        //Look at property keys find the bundle id, this should be the same ID of the bundle that was stopped
        
        Map<String, Object> props = SharedRemoteInterfaceUtils.getSimpleMapFromComplexTypesMap(event.getPropertyList());
        assertThat(props, rawMapHasEntry(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID, testBundleId));
    }

    /**
     * Test starting and stopping a bundle that does not have metatype information.
     * Verify Meta type information available event is NOT received from the metatype listener.
     */
    @Test
    public void testMetatypeInformationTestBundle() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);

        //install test bundle
        final long testBundleId = BundleNamespaceUtils.installBundle(
                ResourceUtils.getExampleProjectBundleFile(), "test.RemoteInterface.bundle", false, socket);

        //register to listen to event
        int regId = RemoteEventRegistration.regRemoteEventMessages(
             socket, RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE);

        //construct request to start bundle
        StartRequestData requestStart = StartRequestData.newBuilder().setBundleId(testBundleId).build();
        TerraHarvestMessage message = 
                BundleNamespaceUtils.createBundleMessage(requestStart, BundleMessageType.StartRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());

        //listen for response
        try
        {
            listener.waitForMessage(Namespace.EventAdmin, EventAdminMessageType.SendEvent, TIME_OUT);
            fail("Expected exception because we do not expect a message.");
        }
        catch (AssertionError e)
        {
            //expecting exception
        }

        //unreg listener
        MessageListener.unregisterEvent(regId, socket);

        //uninstall bundle
        BundleNamespaceUtils.uninstallBundle(testBundleId, socket);
    }

    /**
     * Test stopping and starting the example metatype bundle. This bundle contains both a managed service and 
     * XML based metatype information. 
     * Verify Meta type information available events are received with expected information.
     */
    @SuppressWarnings({"unchecked"})//class casting to string array
    @Test
    public void testMetatypeInformationInstalledBundleXML() throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);

        //find the test bundle
        long testBundleId = BundleNamespaceUtils.getBundleBySymbolicName("mil.dod.th.ose.integration.example.metatype", 
                socket);

        //verify that the test bundle was found
        assertThat(testBundleId, greaterThan(0L));

        int regId = RemoteEventRegistration.regRemoteEventMessages(socket, 
                RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE);

        //jar to update
        File jarFile = new File(ResourceUtils.getBaseIntegrationPath(), 
                "generated/mil.dod.th.ose.integration.example.metatype.jar");
        byte[] buf = FileUtils.readFileToByteArray(jarFile);
        //construct request to start bundle
        UpdateRequestData requestStart = UpdateRequestData.newBuilder().setBundleFile(ByteString.copyFrom(buf)).
                setBundleId(testBundleId).build();
        TerraHarvestMessage message = 
                BundleNamespaceUtils.createBundleMessage(requestStart, BundleMessageType.UpdateRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());

        //listen for response from the configuration listener, extraneous wait
        //because the bundle needs time to start, framework needs to post bundle event and then
        //then the metatype listener will post its event.
        List<MessageDetails> responses = listener.waitForRemoteEvents(
                RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE, TIME_OUT, MatchCount.atLeast(2));

        //unreg listener
        MessageListener.unregisterEvent(regId, socket);

        //parse Response
        EventAdminNamespace namespace = (EventAdminNamespace)responses.get(0).getNamespaceMessage();
        SendEventData event = SendEventData.parseFrom(namespace.getData());

        Map<String, Object> propertyMap = 
             SharedRemoteInterfaceUtils.getSimpleMapFromComplexTypesMap(event.getPropertyList());

        //check other response
        namespace = (EventAdminNamespace)responses.get(1).getNamespaceMessage();
        event = SendEventData.parseFrom(namespace.getData());

        Map<String, Object> propertyMap2 = 
             SharedRemoteInterfaceUtils.getSimpleMapFromComplexTypesMap(event.getPropertyList());
        //verify events
        if (((List<String>)propertyMap.get(RemoteMetatypeConstants.EVENT_PROP_PIDS)).
                contains("example.metatype.XML.ExampleClass"))
        {
            assertThat((Long)propertyMap.get(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(testBundleId));
        }
        else
        {
            //verify
            assertThat((Long)propertyMap.get(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(testBundleId));
            assertThat((List<String>)propertyMap.get(RemoteMetatypeConstants.EVENT_PROP_PIDS), 
                hasItem("example.metatype.configadmin.ExampleInMemConfigClass"));
        }
        //verify events
        if (((List<String>)propertyMap2.get(RemoteMetatypeConstants.EVENT_PROP_PIDS)).
                contains("example.metatype.XML.ExampleClass"))
        {
            assertThat((Long)propertyMap2.get(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(testBundleId));
        }
        else
        {
            //verify
            assertThat((Long)propertyMap2.get(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(testBundleId));
            assertThat((List<String>)propertyMap2.get(RemoteMetatypeConstants.EVENT_PROP_PIDS), 
                hasItem("example.metatype.configadmin.ExampleInMemConfigClass"));
        }
    }
}
