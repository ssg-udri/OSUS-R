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
package mil.dod.th.ose.core.integration;


import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.UnmarshalException;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import junit.framework.TestCase;
import example.ccomms.DirectTransport;
import example.ccomms.EchoTransport;
import example.ccomms.ExampleLinkLayer;
import example.ccomms.ExamplePhysicalLink;
import example.ccomms.ExampleSocketLinkLayer;
import example.ccomms.serial.ExampleSerialPort;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkAttributes;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.ccomm.physical.SerialPortAttributes;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.integration.commons.CustomCommsUtils;
import mil.dod.th.ose.integration.commons.EventHandlerSyncer;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * @author dhumeniuk
 *
 */
public class TestCustomCommsService extends TestCase
{
    private BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    @Override
    public void setUp()
    {
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExamplePhysicalLink.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleSerialPort.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleLinkLayer.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, EchoTransport.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, DirectTransport.class, 5000);
        
        CustomCommsUtils.deleteAllLayers(m_Context);        
    }
    
    @Override
    public void tearDown()
    {
        CustomCommsUtils.deleteAllLayers(m_Context);
    }
    
    /**
     * Verify a {@link TransportLayer} can be created without a link layer.
     */
    public void testCreateTransportLayer_NoLinkLayer() throws Exception
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);

        customCommsService.createTransportLayer(EchoTransport.class.getName(), "example-transport", 
                new HashMap<String, Object>());
    }
    
    /**
     * Verify a {@link LinkLayer} can be created without a physical link.
     */
    public void testCreateLinkLayer_NoPhysicalLink() throws Exception
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);

        customCommsService.createLinkLayer(ExampleSocketLinkLayer.class.getName(), null);
    }
    
    /**
     * Test getting the known link layer factories.
     * Verify that the expected types are returned.
     */
    public void testGetLinkLayerFactoryTypes() throws InvalidSyntaxException
    {
        //get the custom comms service
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);

        //get the device factories for link layers
        Set<LinkLayerFactory> factories = customCommsService.getLinkLayerFactories();
        
        //check that the example link layer is returned
        assertThat("Link layer factories not found", factories.size() >= 1);
        List<String> productTypes = new ArrayList<>();
        for (LinkLayerFactory factory : factories)
        {
            productTypes.add(factory.getProductType());
        }
        assertThat(productTypes, hasItem(ExampleLinkLayer.class.getName()));
    }
    
    /**
     * Test getting the known physical link factories.
     * Verify that the expected types are returned.
     */
    public void testGetPhysicalLinkFactoryTypes() throws InvalidSyntaxException
    {
        //get the custom comms service
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        
        //get the device factories for physical link layers
        Set<PhysicalLinkFactory> factories = customCommsService.getPhysicalLinkFactories();
        
        //check that the example physical link factory is returned
        assertThat("Physical link factories not found", factories.size() > 1);
        
        //gather all the physical link product types
        List<String> productTypes = new ArrayList<String>();
        for (PhysicalLinkFactory factory : factories)
        {
            productTypes.add(factory.getProductType());
        }
        assertThat(productTypes, hasItem(ExamplePhysicalLink.class.getName()));
    }
    
    /**
     * Test getting the known transport layer factories.
     * Verify that the expected types are returned.
     */
    public void testGetTransportLayerFactoryTypes() throws InvalidSyntaxException
    {
        //get the custom comms service
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);

         //get the device factories for transport layers
        Set<TransportLayerFactory> factories = 
                customCommsService.getTransportLayerFactories();
        
        //check that the example physical link factory is returned
        assertThat("Transport layer factories not found", factories.size() > 1);
        
        //gather all the product names
        List<String> productTypes = new ArrayList<>();
        for (TransportLayerFactory factory : factories)
        {
            productTypes.add(factory.getProductType());
        }
        assertThat(productTypes, hasItem(EchoTransport.class.getName()));
        assertThat(productTypes, hasItem(DirectTransport.class.getName()));
    }
    
    public final void testCustomCommsService() throws CCommException, PersistenceFailedException, 
        IllegalArgumentException, IOException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        // get layers and check that they aren't doing anything
        final PhysicalLink cLink = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C, "linksPhys");

        assertThat(cLink, is(notNullValue()));
        assertFalse(cLink.isOpen());

        final LinkLayer lLayer = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), "linksPhys");

        assertThat(lLayer, is(notNullValue()));
        assertThat(lLayer.isActivated(), is(false));
        assertThat(lLayer.getPhysicalLink(), is(cLink));
        
        final TransportLayer tLayer = customCommsService.createTransportLayer(EchoTransport.class.getName(), "tl",
                lLayer.getName());
        assertThat(tLayer, is(notNullValue()));
        assertFalse(tLayer.isTransmitting());
        assertFalse(tLayer.isReceiving());
        assertThat(customCommsService.isTransportLayerCreated("tl"), is(true));
        assertThat(tLayer.getLinkLayer(), is(lLayer));
    }
    
    /**
     * Test setting a physical link layer's name. 
     * Verify that the name cannot be duplicated.
     */
    public void testSetPhysicalLinkName() throws IllegalArgumentException, IOException, InterruptedException, 
        CCommException, IllegalStateException, FactoryException, PersistenceFailedException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        EventHandlerSyncer syncer = 
            new EventHandlerSyncer(m_Context, FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED, 
                    "(obj.name=testResetNamePhys)");
        
        //set the name at creation
        UUID uuid = customCommsService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.I_2_C, "testSetNamePhys");
        PhysicalLink link = customCommsService.requestPhysicalLink(uuid);
        //add to list for cleanup
        assertThat(link.getName(), is("testSetNamePhys"));
        
        //duplicate name exception should not be thrown
        link.setName("testResetNamePhys");
        
        //wait for expected event
        syncer.waitForEvent(5);
        
        //release the link
        link.release();
        
        //replay
        PhysicalLink link2 = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        //add to list for cleanup
        try
        {
            link2.setName("testResetNamePhys");
            fail("Should have thrown exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        assertThat(link.getName(), is("testResetNamePhys"));
        assertThat(link2.getName(), is(not("testResetNamePhys")));
    }

    /**
     * Test setting the physical link layer's name using the UUID.
     */
    public void testSetPhysicalLinkNameUUID() throws IllegalArgumentException, IOException, InterruptedException, 
        CCommException, IllegalStateException, FactoryException, PersistenceFailedException, PhysicalLinkException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        PhysicalLink link = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        
        //opening the link to see if we can still change the name
        link.open();
        
        EventHandlerSyncer syncer = 
                new EventHandlerSyncer(m_Context, FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED, 
                     "(obj.name=testOpenSetName)");
        
        //name setting
        customCommsService.setPhysicalLinkName(link.getUuid(), "testOpenSetName");
        
        //wait for expected event
        syncer.waitForEvent(5);
        
        assertThat(link.getName(), is("testOpenSetName"));
        
        link.close();
    }
    
    /**
     * Test setting the link layer's name. The name should only be set if it is unique.
     */
    public void testSetLinkLayerName() throws IllegalArgumentException, IOException, InterruptedException, 
        CCommException, IllegalStateException, FactoryException, PersistenceFailedException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        UUID physUuid = customCommsService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.I_2_C, "testLinkPhys");
        PhysicalLink plink = customCommsService.requestPhysicalLink(physUuid);
        
        LinkLayer link = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), plink.getName());
        
        link.setName("testSetNameLink");
        
        int size = customCommsService.getLinkLayers().size();
        
        Map<String, Object> props = new HashMap<>();
        props.put("some prop", "some value");
        UUID  physUuid2 = customCommsService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.I_2_C, "testLinkPhys2", props);
        PhysicalLink plink2 = customCommsService.requestPhysicalLink(physUuid2);
        
        try
        {
            customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), "testSetNameLink", plink2.getName());
            fail("Should have thrown an exception");
        }
        catch (CCommException e)
        {
            assertThat(e.getCause().getMessage(), 
                    containsString("Duplicate name: [testSetNameLink] is already in use"));
        }
        
        assertThat(customCommsService.getLinkLayer("testSetNameLink"), is(link));
        // verify that no new link layer was created
        assertThat(customCommsService.getLinkLayers().size(), is(size));
        
        //verify valid creation of a link layer with specified name
        LinkLayer link2 = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), "testSetNameLink2", 
                plink2.getName());
        assertThat(link2.getName(), is("testSetNameLink2"));
        
        try
        {
            link2.setName("testSetNameLink");
            fail("Should have thrown an exception");
        }
        catch(IllegalArgumentException e)
        {
            //duplicate name
        }
        
        assertThat(link.getName(), is("testSetNameLink"));
        assertThat(link2.getName(), is(not("testSetNameLink")));
    }
    
    /**
     * Test setting the link layer's name. The name should only be set if it is unique.
     */
    public void testSetTransportLayerName() throws CCommException, FactoryException, InterruptedException, 
        PersistenceFailedException, IllegalArgumentException, IOException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        ConfigurationAdmin configAdmin = ServiceUtils.getService(m_Context, ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        //set the transport layer name at creation
        TransportLayer layer = customCommsService.createTransportLayer(EchoTransport.class.getName(), "tl-2", 
                (String)null);
        assertThat(customCommsService.isTransportLayerCreated("tl-2"), is(true));
        
        layer.setName("test-tl");
          
        assertThat(customCommsService.isTransportLayerCreated("test-tl"), is(true));
        
        TransportLayer layer2 = customCommsService.createTransportLayer(EchoTransport.class.getName(), "tl-3", 
                (String)null);

        try
        {
            layer2.setName("test-tl");
            fail("expecting exception");
        }
        catch (Exception e)
        {
            
        }
        
        assertThat(layer.getName(), is("test-tl"));
        assertThat(layer2.getName(), is(not("test-tl")));
        
        assertThat(customCommsService.isTransportLayerCreated("test-tl"), is(true));
        assertThat(customCommsService.isTransportLayerCreated("tl-3"), is(true));
    }
    
    /**
     * Verify the {@link CustomCommsService} will deactivate all layers when stopped.
     */
    @SuppressWarnings("unchecked")
    public final void testShutdown() throws BundleException, PhysicalLinkException, InterruptedException, 
        CCommException, IllegalArgumentException, FactoryException, PersistenceFailedException, IOException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        
        final PhysicalLink cLink = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        
        cLink.open();
        
        final LinkLayer lLayer = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), cLink.getName());
        
        customCommsService.createTransportLayer(EchoTransport.class.getName(), "tl-shutdown", lLayer.getName());
        
        EventHandlerSyncer listener;
        for (LinkLayer linkLayer : customCommsService.getLinkLayers())
        {
            listener = new EventHandlerSyncer(m_Context, LinkLayer.TOPIC_ACTIVATED,  
                    String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, linkLayer.getUuid().toString()));
            linkLayer.activateLayer();
            listener.waitForEvent(5);
            assertThat(linkLayer.isActivated(), is(true));
        }
        
        LogReaderService logReader = ServiceUtils.getService(m_Context, LogReaderService.class);
        
        final Semaphore physicalLinkClosedSemaphore = new Semaphore(0);
        
        logReader.addLogListener(new LogListener()
        {
            @Override
            public void logged(LogEntry entry)
            {
                if ("Example PhysicalLink closed".equals(entry.getMessage()))
                {
                    physicalLinkClosedSemaphore.release();
                }
            }
        });
        
        final Semaphore linkDeactivatedSemaphore = new Semaphore(0);
        
        logReader.addLogListener(new LogListener()
        {
            @Override
            public void logged(LogEntry entry)
            {
                if ("ExampleLinkLayer deactivated".equals(entry.getMessage()))
                {
                    linkDeactivatedSemaphore.release();
                }
            }
        });
        
        ServiceReference<AssetDirectoryService> ccsServiceRef = 
                ServiceUtils.getServiceReference(m_Context, CustomCommsService.class);
        Bundle ccsBundle = ccsServiceRef.getBundle();
        ccsBundle.stop();
        
        // verify log message is read
        assertThat(linkDeactivatedSemaphore.tryAcquire(10, TimeUnit.SECONDS), is(true));
        assertThat(physicalLinkClosedSemaphore.tryAcquire(5, TimeUnit.SECONDS), is(true));
        
        ccsBundle.start();
    }
    
    /**
     * Verify links can be created and opened. Since they are only example plug-ins nothing more meaningful can be 
     * tested.
     */
    public void testGetOpenPhysicalLinks() throws CCommException, PhysicalLinkException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        
        PhysicalLink i2c = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        PhysicalLink serial = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);
        
        i2c.open();
        i2c.close();
        
        serial.open();
        serial.close();
    }
    
    /**
     * Verify the {@link CustomCommsService#getPhysicalLinkPid(java.util.UUID)} method.
     */
    public void testGetPhysicalLinkPid() throws CCommException, PersistenceFailedException, IOException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        
        PhysicalLink link = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        
        assertThat(customCommsService.getPhysicalLinkPid(link.getUuid()), is(link.getPid()));
    }

    /**
     * Test physical link factory capabilities.
     */
    public void testGetPhysicalCaps() throws CCommException, UnmarshalException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        
        Set<PhysicalLinkFactory> links = customCommsService.getPhysicalLinkFactories();

        //list to add the desired descriptor to
        List<String>descriptions = new ArrayList<>();
        for (PhysicalLinkFactory factory : links)
        {
            //check example physical links properties
            if (factory.getProductName().equals("ExamplePhysicalLink"))
            {
                descriptions.add(factory.getCapabilities().getDescription());
            }
        }
        assertThat(descriptions, hasItem("An example physical link"));
    }
    
    /**
     * Test that if a physical link can be created by calling 
     * {@link CustomCommsService#tryCreatePhysicalLink(PhysicalLinkTypeEnum, String)}.
     * Verify once a physical link is made that if the aforementioned method is called again that the already created
     * link is returned. 
     */
    public void testTryCreatePhysicalLink() throws IllegalArgumentException, IOException, InterruptedException, 
        CCommException, IllegalStateException, FactoryException, PersistenceFailedException, PhysicalLinkException
    {
        String physicalLinkName = "PhysicalLinkTest";
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        UUID linkUuid = customCommsService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.I_2_C, physicalLinkName);
        assertThat(linkUuid, is(notNullValue()));
        
        //request the link
        PhysicalLink link = customCommsService.requestPhysicalLink(physicalLinkName);
        
        assertThat(linkUuid, is(link.getUuid()));
        
        //try to create a physical link of the same type, with the same name
        UUID linkUuidReplay = customCommsService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.I_2_C, physicalLinkName);
        assertThat(linkUuidReplay, is(linkUuid));
    }
    
    /**
     * Verify attributes can be retrieved for links that have extended attributes.
     */
    public void testExtendedAttributes() throws CCommException, PhysicalLinkException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        
        PhysicalLink serialPort = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);
        
        ImmutableMap<String, AttributeDefinition> attributeDefMap = 
                Maps.uniqueIndex(Arrays.asList(serialPort.getFactory().
                        getAttributeDefinitions(ObjectClassDefinition.ALL)), 
                        new Function<AttributeDefinition, String>()
                        {
                            @Override
                            public String apply(AttributeDefinition def)
                            {
                                return def.getID();
                            }
                        });

        // base props first
        assertThat(attributeDefMap.keySet(), hasItem(PhysicalLinkAttributes.CONFIG_PROP_DATA_BITS));
        // make sure extended properties are there
        assertThat(attributeDefMap.keySet(), hasItem(SerialPortAttributes.CONFIG_PROP_BAUD_RATE));
    }
    
    /**
     * Verify serial port defaults can be retrieved.
     */
    public void testSerialPortProperties() throws Exception
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        
        SerialPort serialPort = (SerialPort)customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);
        
        // just verify default properties based on enums can be retrieved
        serialPort.getConfig().flowControl();
        serialPort.getConfig().stopBits();
        serialPort.getConfig().parity();        
    }
}
