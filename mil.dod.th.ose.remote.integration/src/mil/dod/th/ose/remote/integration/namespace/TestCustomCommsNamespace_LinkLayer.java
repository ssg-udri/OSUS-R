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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameResponseData;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.ose.remote.integration.CustomCommsNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.RemoteLinkLayerUtils;
import mil.dod.th.ose.remote.integration.RemotePhysicalLinkUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import example.ccomms.ExampleLinkLayer;
import example.ccomms.ExampleSocketLinkLayer;

/**
 * Test the custom comms namespace interactions that are specific to LinkLayers.
 * @author allenchl
 *
 */
public class TestCustomCommsNamespace_LinkLayer
{
    private SharedMessages.UUID pLinkUuid;
    private SharedMessages.UUID pLinkUuid2;
    
    private SharedMessages.UUID linkUuid;
    private SharedMessages.UUID linkUuid2;
    
    private String linkPid;
    private String linkPid2;

    private Socket socket;

    /**
     * Setup and connect the socket to the controller.
     * Create physical links and link layers for use with unit tests that need one. 
     */
    @Before
    public void setUp() throws IOException, InterruptedException
    {
        socket = SocketHostHelper.connectToController();
        
        // request data to create a new physical link
        pLinkUuid = RemotePhysicalLinkUtils.createPhysicalLink(
                CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, "honeybadger", socket);
        
        //---Create a second physical link with different name so that we can create 2 different link layer objects
        pLinkUuid2 = RemotePhysicalLinkUtils.createPhysicalLink(
                CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, "ashe",socket);
        
        //create link layers
        CreateLinkLayerResponseData link1 = RemoteLinkLayerUtils.
                createLinkCommLayer("yarrr", ExampleLinkLayer.class.getName(), pLinkUuid, socket);
        
        linkUuid = link1.getInfo().getUuid();
        linkPid = link1.getInfo().getPid();
        
        CreateLinkLayerResponseData link2 = RemoteLinkLayerUtils.
                createLinkCommLayer("ninja", ExampleLinkLayer.class.getName(), pLinkUuid2, socket);
        
        linkUuid2 = link2.getInfo().getUuid();
        linkPid2 = link2.getInfo().getPid();
    }
    
    /**
     * Remove the layers created in the setup.
     */
    @After
    public void tearDown() throws IOException, InterruptedException
    {
        try
        {
            //cleanup
            RemoteLinkLayerUtils.removeLink(linkUuid, socket);
            RemoteLinkLayerUtils.removeLink(linkUuid2, socket);
            
            // request data to remove the created physical links in setup
            RemotePhysicalLinkUtils.tryRemovePhysicalLink(pLinkUuid, socket);
            RemotePhysicalLinkUtils.tryRemovePhysicalLink(pLinkUuid2, socket);
            
            // verify physical links are gone
            List<SharedMessages.UUID> uuidList = 
                    CustomCommsNamespaceUtils.getLayerUuidsByType(socket, CommType.PhysicalLink);
            assertThat(uuidList.contains(pLinkUuid), is(false));
            assertThat(uuidList.contains(pLinkUuid2), is(false));
            uuidList = CustomCommsNamespaceUtils.getLayerUuidsByType(socket, CommType.Linklayer);
            assertThat(uuidList.contains(linkUuid), is(false));
            assertThat(uuidList.contains(linkUuid2), is(false));
            
            MessageListener.unregisterEvent(socket); 
        }
        finally
        {
            socket.close();
        }
    }
    
    /**
     * Verify the system can remotely get the layers of link layer type.
     */
    @Test
    public void testGetLayersLinkLayer() throws IOException, InterruptedException
    {
        //---Get the list of layers of link layer type and verify both link layers are in it---
        List<FactoryObjectInfo> infoList = CustomCommsNamespaceUtils.getLayersByType(socket, CommType.Linklayer);
        
        List<SharedMessages.UUID> uuidList = new ArrayList<SharedMessages.UUID>();
        List<String> pidList = new ArrayList<String>();
        
        for (FactoryObjectInfo factoryObjectInfo : infoList)
        {
            uuidList.add(factoryObjectInfo.getUuid());
            pidList.add(factoryObjectInfo.getPid());
        }
        assertThat(uuidList, hasItems(linkUuid, linkUuid2));
        assertThat(pidList, hasItems(linkPid, linkPid2));
    }
    
    /**
     * Verify that the system can remotely create a link layer.
     */
    @Test
    public void testCreateLinkLayer() throws IOException,InterruptedException
    {
        // physical link
        SharedMessages.UUID physUuid = 
                RemotePhysicalLinkUtils.createPhysicalLink(
                        CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, "phyzzy", socket);
        
        // link layer
        SharedMessages.UUID localLinkUuid = RemoteLinkLayerUtils.
                createLinkLayer("fizzle", ExampleLinkLayer.class.getName(), physUuid, socket);
        
        //cleanup
        RemoteLinkLayerUtils.removeLink(localLinkUuid, socket);
        RemotePhysicalLinkUtils.removePhysicalLink(physUuid, socket);
    }
    
    /**
     * Verify that the system can remotely create a link layer without a physical link.
     */
    @Test
    public void testCreateLinkLayer_NoPhysicalLink() throws Exception
    {
        // link layer
        SharedMessages.UUID localLinkUuid = RemoteLinkLayerUtils.
                createLinkLayer("fizzle", ExampleSocketLinkLayer.class.getName(), null, socket);
        
        //cleanup
        RemoteLinkLayerUtils.removeLink(localLinkUuid, socket);
    }
    
    /**
     * Test getting the name of a Link layer.
     * Verify that the name is the expected value.
     */
    @Test
    public void testGetLinkName() throws IOException, InterruptedException
    {
        //get the layer's name
        GetLayerNameResponseData responseData  = 
             CustomCommsNamespaceUtils.getCommLayerName(socket, linkUuid, CommType.Linklayer);
        
        //verify
        assertThat(responseData.getLayerName(), is("yarrr"));
        assertThat(responseData.getCommType(), is(CommType.Linklayer));
    }

    /**
     * Test setting the name of a Link layer.
     * Verify that the name is the expected value.
     */
    @Test
    public void testSetLinkName() throws IOException, InterruptedException
    {
        //set the name
        CustomCommsNamespaceUtils.setCommLayerName(socket, linkUuid, CommType.Linklayer, "kangaroos");

        //verify
        GetLayerNameResponseData responseData  = 
             CustomCommsNamespaceUtils.getCommLayerName(socket, linkUuid, CommType.Linklayer);
        
        //verify
        assertThat(responseData.getLayerName(), is("kangaroos"));
        assertThat(responseData.getCommType(), is(CommType.Linklayer));
        
        //cleanup
        CustomCommsNamespaceUtils.setCommLayerName(socket, linkUuid, CommType.Linklayer, "yarrr");
    }
    
    /**
     * Verify that the system can remotely retrieve the available comm types of link layer type.
     */
    @Test
    public void testGetAvailableCommTypesLinkLayer() throws IOException, InterruptedException
    {
        List<String> fqcnList = CustomCommsNamespaceUtils.getAvailableCommTypes(CommType.Linklayer, socket);
        assertThat(fqcnList.contains(ExampleLinkLayer.class.getName()), is(true));
    }
}
