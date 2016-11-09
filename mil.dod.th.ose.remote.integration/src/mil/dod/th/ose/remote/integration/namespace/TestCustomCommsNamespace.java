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
import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameResponseData;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.integration.CustomCommsNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.RemotePhysicalLinkUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.Message;

import example.ccomms.ExamplePhysicalLink;

/**
 * Tests the interaction of the remote interface with the {@link CustomCommsNamespace}.  Specifically, 
 * the class tests that CustomComms messages are properly sent and that appropriate responses are
 * received.
 * @author matt
 */
public class TestCustomCommsNamespace
{
    private SharedMessages.UUID pLinkUuid;
    private SharedMessages.UUID pLinkUuid2;
    
    private String pLinkPid;
    private String pLinkPid2;

    private Socket socket;

    /**
     * Setup and connect the socket to the controller.
     * Create physical links for use with unit tests that need one.
     */
    @Before
    public void setUp() throws IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
        
        // request data to create a new physical link
        CreatePhysicalLinkResponseData createPhysicalLinkResponse = RemotePhysicalLinkUtils.
                createPhysicalCommLayer(
                        CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, "honeybadger", socket);
        
        pLinkUuid = createPhysicalLinkResponse.getInfo().getUuid();
        pLinkPid = createPhysicalLinkResponse.getInfo().getPid();
        
        //---Create a second physical link with different name so that we can create 2 different link layer objects
        createPhysicalLinkResponse = RemotePhysicalLinkUtils.
                createPhysicalCommLayer(
                        CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, "ashe",socket);
        
        pLinkUuid2 = createPhysicalLinkResponse.getInfo().getUuid();
        pLinkPid2 = createPhysicalLinkResponse.getInfo().getPid();   
    }
    
    /**
     * Remove both physical links created in setup, verify they are removed.
     */
    @After
    public void tearDown() throws IOException, InterruptedException
    {
        try
        {
            // request data to remove the created physical links in setup
            RemotePhysicalLinkUtils.tryRemovePhysicalLink(pLinkUuid, socket);
            RemotePhysicalLinkUtils.tryRemovePhysicalLink(pLinkUuid2, socket);
            
            // verify physical links are gone
            List<SharedMessages.UUID> uuidList = 
                    CustomCommsNamespaceUtils.getLayerUuidsByType(socket, CommType.PhysicalLink);
            assertThat(uuidList.contains(pLinkUuid), is(false));
            assertThat(uuidList.contains(pLinkUuid2), is(false));
            
            MessageListener.unregisterEvent(socket);
        }
        finally
        {
            socket.close();
        }
    }

    /**
     * Verify that the UUIDs of the  physical link created in the setup can be retrieved.
     */
    @Test
    public void testCreatePhysicalLink() throws IOException
    {
        assertThat(pLinkUuid, is(notNullValue()));
        assertThat(pLinkUuid2, is(notNullValue()));
    }

    /**
     * Verify that the system can remotely retrieve the available physical link types.
     */
    @Test
    public void testGetAvailableCommTypesPhysicalLink() throws IOException, InterruptedException
    {
        List<String> fqcnList = CustomCommsNamespaceUtils.getAvailableCommTypes(CommType.PhysicalLink, socket);
        assertThat(fqcnList.contains(ExamplePhysicalLink.class.getName()), is(true));
    }
    
    /**
     * Verify the system can remotely get the layers of physical link type.
     */
    @Test
    public void testGetLayersPhysicalLink() throws IOException, InterruptedException
    {
        //---Get the list of layers of physical link type and verify both physical links are in it---
        List<FactoryObjectInfo> infoList = CustomCommsNamespaceUtils.getLayersByType(socket, CommType.PhysicalLink);
        
        List<SharedMessages.UUID> uuidList = new ArrayList<SharedMessages.UUID>();
        List<String> pidList = new ArrayList<String>();
        
        for (FactoryObjectInfo factoryObjectInfo : infoList)
        {
            uuidList.add(factoryObjectInfo.getUuid());            
            pidList.add(factoryObjectInfo.getPid());
        }
        assertThat(uuidList, hasItems(pLinkUuid, pLinkUuid2));
        assertThat(pidList, hasItems(pLinkPid, pLinkPid2));
    }
    
    /**
     * Test getting the name of a Physical layer.
     * Verify that the name is the expected value.
     */
    @Test
    public void testGetPhysName() throws IOException, InterruptedException
    {
        //get the layer's name
        GetLayerNameResponseData responseData  = 
             CustomCommsNamespaceUtils.getCommLayerName(socket, pLinkUuid, CommType.PhysicalLink);
        
        //verify
        assertThat(responseData.getLayerName(), is("honeybadger"));
        assertThat(responseData.getCommType(), is(CommType.PhysicalLink));
    }

    /**
     * Test setting the name of a physical layer.
     * Verify that the name is the expected value.
     */
    @Test
    public void testSetPhysName() throws IOException, InterruptedException
    {
        //set the name
        CustomCommsNamespaceUtils.setCommLayerName(socket, pLinkUuid, CommType.PhysicalLink, "wallabeeeees");

        //get the layer's name
        GetLayerNameResponseData responseData  = 
             CustomCommsNamespaceUtils.getCommLayerName(socket, pLinkUuid, CommType.PhysicalLink);
        
        //verify
        assertThat(responseData.getLayerName(), is("wallabeeeees"));
        assertThat(responseData.getCommType(), is(CommType.PhysicalLink));
        
        //set the name back
        CustomCommsNamespaceUtils.setCommLayerName(socket, pLinkUuid, CommType.PhysicalLink, "honeybadger");
    }

    /**
     * Helper method for creating CustomComms messages to be sent to controller. 
     * @param type
     *      type of message to be contained in the sent TerraHarvestMessage
     * @param message
     *      message data to be contained in the sent TerraHarvestMessage
     * @return
     *      TerraHarvestMessage to be sent to the controller
     */
    public static TerraHarvestMessage createCustomCommsMessage(final CustomCommsMessageType type, 
            final Message message)
    {
        CustomCommsNamespace.Builder ccommMessageBuilder = CustomCommsNamespace.
                newBuilder().
                setType(type);

        if (message != null)
        {
            ccommMessageBuilder.setData(message. toByteString());
        }

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createTerraHarvestMsg(Namespace.CustomComms, ccommMessageBuilder);
        return thMessage;
    }
}
