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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import javax.xml.bind.UnmarshalException;

import junit.framework.TestCase;
import example.ccomms.ConnectionOrientedTransport;
import example.ccomms.DirectTransport;
import example.ccomms.EchoTransport;
import example.ccomms.ExampleLinkLayer;
import example.ccomms.ExamplePhysicalLink;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.ccomm.transport.TransportPacket;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.integration.commons.CustomCommsUtils;
import mil.dod.th.ose.integration.commons.EventHandlerSyncer;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * @author dhumeniuk
 *
 */
public class TestTransportLayer extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private Event m_LastEvent;
    
    @Override
    public void setUp()
    {
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExamplePhysicalLink.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleLinkLayer.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, EchoTransport.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, DirectTransport.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ConnectionOrientedTransport.class, 5000);
        
        CustomCommsUtils.deleteAllLayers(m_Context);        
    }
    
    @Override
    public void tearDown()
    {
        CustomCommsUtils.deleteAllLayers(m_Context);
    }

    /**
     * Verify a transport can use the built in post event method to post an event for a received packet.
     */
    public final void testReceiveEvent() 
        throws CCommException, InterruptedException, IllegalArgumentException, FactoryException,
        PersistenceFailedException, IOException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        AddressManagerService addressMgrSvc = ServiceUtils.getService(m_Context, AddressManagerService.class);
        
        PhysicalLink pLink = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        LinkLayer linkLayer = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), pLink.getName());
        TransportLayer transport = customCommsService.createTransportLayer(EchoTransport.class.getName(), 
                "test-transport", linkLayer.getName());
        
        Address address = addressMgrSvc.getOrCreateAddress("Example:3000");

        // handler called when the event is posted
        EventHandler handler = new EventHandler()
        {
            @Override
            public void handleEvent(Event event)
            {
                // just keep track of event for later inspection
                m_LastEvent = event;
            }
        };
        
        // setup to wait for the package received event
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, TransportLayer.TOPIC_PACKET_RECEIVED,
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_NAME, "test-transport"),
                handler);
        
        // send a message, example code will call the protected post event method
        transport.send(ByteBuffer.wrap("test message".getBytes()), address);
       
        // no wait for event
        syncer.waitForEvent(2);
        
        // verify event props are correct
        assertThat(((TransportPacket)m_LastEvent.getProperty(TransportLayer.EVENT_PROP_PACKET)).getPayload().array(),
                is("test message".getBytes()));
        // source should not be filled out
        assertThat((Address)m_LastEvent.getProperty(Address.EVENT_PROP_SOURCE_ADDRESS_PREFIX
                + Address.EVENT_PROP_ADDRESS_SUFFIX), is(nullValue()));
        assertThat((Address)m_LastEvent.getProperty(Address.EVENT_PROP_DEST_ADDRESS_PREFIX 
                + Address.EVENT_PROP_ADDRESS_SUFFIX), is(address));
    }
    
    /**
     * Verify a connection oriented transport can use the built in post event method to post an event 
     * for a received packet.
     */
    public final void testReceiveEventConnectionOriented() 
        throws CCommException, InterruptedException, IllegalArgumentException, FactoryException,
        PersistenceFailedException, IOException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        AddressManagerService addressMgrSvc = ServiceUtils.getService(m_Context, AddressManagerService.class);
        
        PhysicalLink pLink = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        LinkLayer linkLayer = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), pLink.getName());
        TransportLayer transport = customCommsService.createTransportLayer(ConnectionOrientedTransport.class.getName(), 
                "test-connection-transport", linkLayer.getName());
                
        Address address = addressMgrSvc.getOrCreateAddress("Example:3000");
        
        // handler called when the event is posted
        EventHandler handler = new EventHandler()
        {
            @Override
            public void handleEvent(Event event)
            {
                // just keep track of event for later inspection
                m_LastEvent = event;
            }
        };
        
        // setup to wait for the package received event
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, TransportLayer.TOPIC_PACKET_RECEIVED,
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_NAME, "test-connection-transport"),
                handler);
        
        transport.connect(address);
        assertTrue(transport.isAvailable(address));
        
        // send a message, example code will call the protected post event method
        transport.send(ByteBuffer.wrap("test message".getBytes()));
        
        // no wait for event
        syncer.waitForEvent(2);
        
        // verify event props are correct
        assertThat(((TransportPacket)m_LastEvent.getProperty(TransportLayer.EVENT_PROP_PACKET)).getPayload().array(),
                is("test message".getBytes()));
        // source should not be filled out
        assertThat((Address)m_LastEvent.getProperty(Address.EVENT_PROP_SOURCE_ADDRESS_PREFIX
                + Address.EVENT_PROP_ADDRESS_SUFFIX), is(nullValue()));
        assertThat((Address)m_LastEvent.getProperty(Address.EVENT_PROP_DEST_ADDRESS_PREFIX 
                + Address.EVENT_PROP_ADDRESS_SUFFIX), is(nullValue()));

        transport.disconnect();
        assertFalse(transport.isAvailable(address));
    }

    /**
     * Verify a transport layer can get its name and description from the capabilities.
     */
    public final void testCapabilities() 
        throws CCommException, InterruptedException, IllegalArgumentException, FactoryException, UnmarshalException
    {
        //echo transport
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        Set<TransportLayerFactory> factories = 
                customCommsService.getTransportLayerFactories();
        
        boolean foundFactory = false;
        boolean foundConnOrientedFactory = false;
        for (TransportLayerFactory factory : factories)
        {
            if (factory.getProductType().equals(EchoTransport.class.getName()))
            {
                foundFactory = true;
                assertThat(factory.getCapabilities().getProductName(), is("Echo Transport"));
                assertThat(factory.getCapabilities().getDescription(), 
                    is("An example transport layer that echoes messages back through the same transport layer"));
            }
            
            if (factory.getProductType().equals(ConnectionOrientedTransport.class.getName()))
            {
                foundConnOrientedFactory = true;
                assertThat(factory.getCapabilities().getProductName(), is("Connection Oriented Transport"));
                assertThat(factory.getCapabilities().getDescription(), 
                        is("An example connection oriented transport layer that echoes messages sent back through "
                                + "the same transport layer"));
                assertThat(((TransportLayerCapabilities)factory.getCapabilities()).isConnectionOriented(), is(true));
            }
        }
        
        assertThat("Echo factory is found", foundFactory, is(true));
        assertThat("Connection Oriented Transport is found", foundConnOrientedFactory, is(true));
    }
}
