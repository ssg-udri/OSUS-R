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
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameResponseData;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.ose.remote.integration.CustomCommsNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.RemoteLinkLayerUtils;
import mil.dod.th.ose.remote.integration.RemotePhysicalLinkUtils;
import mil.dod.th.ose.remote.integration.RemoteTransportLayerUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import example.ccomms.EchoTransport;
import example.ccomms.ExampleLinkLayer;

/**
 * Test Custom Comms functionality that is specific to TransportLayers.
 * @author allenchl
 *
 */
public class TestCustomCommsNamespace_TransportLayer
{
    private SharedMessages.UUID pLinkUuid;
    private SharedMessages.UUID pLinkUuid2;
    
    private SharedMessages.UUID linkUuid;
    private SharedMessages.UUID linkUuid2;
    
    private SharedMessages.UUID transUuid;
    private SharedMessages.UUID transUuid2;
    
    private String transPid;
    private String transPid2;

    private Socket socket;

    /**
     * Setup and connect the socket to the controller.
     * Create physical links, link layers, and transport layers for use with unit tests that need one.
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
        
        CreateLinkLayerResponseData link2 = RemoteLinkLayerUtils.
                createLinkCommLayer("ninja", ExampleLinkLayer.class.getName(), pLinkUuid2, socket);
        
        linkUuid2 = link2.getInfo().getUuid();
        
        CreateTransportLayerResponseData transport1 = RemoteTransportLayerUtils.createTransportCommLayer(
                "avast", EchoTransport.class.getName(), linkUuid, socket);
        
        transUuid = transport1.getInfo().getUuid();
        transPid = transport1.getInfo().getPid();

        CreateTransportLayerResponseData transport2 = RemoteTransportLayerUtils.createTransportCommLayer(
                "baaaaa", EchoTransport.class.getName(), linkUuid2, socket);
        
        transUuid2 = transport2.getInfo().getUuid();
        transPid2 = transport2.getInfo().getPid();
    }
    
    /**
     * Remove the layers created in setup.
     */
    @After
    public void tearDown() throws IOException, InterruptedException
    {
        try
        {
            //remove transports
            RemoteTransportLayerUtils.removeTransportLayer(transUuid, socket);
            RemoteTransportLayerUtils.removeTransportLayer(transUuid2, socket);
            
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
     * Verify that the system can remotely create a transport layer. 
     */
    @Test
    public void testCreateTransportLayer() throws IOException, InterruptedException
    {
        //phys and link layers
        SharedMessages.UUID phys = RemotePhysicalLinkUtils.createPhysicalLink(
                CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C, 
                "transPhys", socket);
        
        SharedMessages.UUID link = RemoteLinkLayerUtils.createLinkLayer(
                "transLink", ExampleLinkLayer.class.getName(), phys, socket);
        
        // request data to create a new transport layer
        SharedMessages.UUID localTrans = RemoteTransportLayerUtils.createTransportLayer(
            "trannyTrans", EchoTransport.class.getName(), link, socket);

        //cleanup
        RemoteTransportLayerUtils.removeTransportLayer(localTrans, socket);
        RemoteLinkLayerUtils.removeLink(link, socket);
        RemotePhysicalLinkUtils.removePhysicalLink(phys, socket);
    }
    
    /**
     * Verify that the system can remotely create a transport layer with no link layer. 
     */
    @Test
    public void testCreateTransportLayer_NoLinkLayer() throws Exception
    {
        // request data to create a new transport layer
        SharedMessages.UUID localTrans = RemoteTransportLayerUtils.createTransportLayer(
            "trannyTrans", EchoTransport.class.getName(), null, socket);

        //cleanup
        RemoteTransportLayerUtils.removeTransportLayer(localTrans, socket);
    }
    
    /**
     * Verify that the system can remotely retrieve the available comm types of transport layer type.
     */
    @Test
    public void testGetAvailableCommTypesTransportLayer() throws IOException, InterruptedException
    {
        List<String> fqcnList = CustomCommsNamespaceUtils.getAvailableCommTypes(CommType.TransportLayer, socket);
        assertThat(fqcnList.contains(EchoTransport.class.getName()), is(true));
    }
    
    /**
     * Verify the system can remotely get the layers of transport layer type.
     */
    @Test
    public void testGetLayersTransportLayer() throws IOException, InterruptedException
    {
        //---Get the list of layers of transport layer type and verify both transport layers are in it---
        List<FactoryObjectInfo> infoList = CustomCommsNamespaceUtils.getLayersByType(socket, CommType.TransportLayer);
        
        List<SharedMessages.UUID> uuidList = new ArrayList<SharedMessages.UUID>();
        List<String> pidList = new ArrayList<String>();
        
        for (FactoryObjectInfo factoryObjectInfo : infoList)
        {
            uuidList.add(factoryObjectInfo.getUuid());            
            pidList.add(factoryObjectInfo.getPid());
        }
        
        assertThat(uuidList, hasItems(transUuid, transUuid2));
        assertThat(pidList, hasItems(transPid, transPid2));
    }
    
    /**
     * Test getting the name of a Transport layer.
     * Verify that the name is the expected value.
     */
    @Test
    public void testGetTransName() throws IOException, InterruptedException
    {
        //get the layer's name
        GetLayerNameResponseData responseData  = 
             CustomCommsNamespaceUtils.getCommLayerName(socket, transUuid, CommType.TransportLayer);
        
        //verify
        assertThat(responseData.getLayerName(), is("avast"));
        assertThat(responseData.getCommType(), is(CommType.TransportLayer));
    }

    /**
     * Test getting the name of a Transport layer.
     * Verify that the name is the expected value.
     */
    @Test
    public void testSetTransName() throws IOException, InterruptedException
    {
        //set the name
        CustomCommsNamespaceUtils.setCommLayerName(socket, transUuid, CommType.TransportLayer, "boomer");

        //verify
        GetLayerNameResponseData responseData  = 
             CustomCommsNamespaceUtils.getCommLayerName(socket, transUuid, CommType.TransportLayer);
        
        assertThat(responseData.getLayerName(), is("boomer"));
        assertThat(responseData.getCommType(), is(CommType.TransportLayer));
        
      //set the name
        CustomCommsNamespaceUtils.setCommLayerName(socket, transUuid, CommType.TransportLayer, "avast");
    }
}
