package ${package};

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkFrame;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerContext;
import mil.dod.th.core.ccomm.transport.TransportLayerProxy;
import mil.dod.th.core.ccomm.transport.TransportPacket;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.pm.WakeLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * ${description} implementation.
 * 
 * @author ${author}
 */
@Component(factory = TransportLayer.FACTORY)
public class ${class} implements TransportLayerProxy
{
    /**
     * Reference to the context which provides this class with methods to interact with the rest of the system.
     */
    private TransportLayerContext m_Context;
    
    /**
     * Reference to the class that handles listening for events.
     */
    private ${class}EventHandler m_${class}EventHandler;
    
    /**
     * Reference to the wake lock used for vital transport layer operations.
     */
    private WakeLock m_WakeLock;
    
    /**
     * Reference to the wake lock used for a transport layer when receiving data.
     */ 
    private WakeLock m_RecvWakeLock;
 
    /**
     * Activate the transport layer upon creation.
     * 
     * @param bundleContext
     *      context for the bundle containing the plug-in
     */    
    @Activate
    public void activate(final BundleContext bundleContext)
    {
        // ${task}: Add custom code that must be performed on activation of the plug-in
        
        
        // ${task}: Customize code below for registration of link layer events if needed, remove if not
        // The following demonstrates how to register for data that is received from a link layer.
        // This is done by registering the class which implements the EventHandler interface with 
        // the OSGi system.
        
        m_${class}EventHandler = new ${class}EventHandler();
        m_${class}EventHandler.registerEvents(bundleContext);
    }
    
    /**
     * Deactivate the transport layer when removed or core is shutdown.
     */
    @Deactivate
    public void deactivate()
    {
        // ${task}: Add custom code that must be performed on deactivation of the plug-in
        
        // ${task}: Remove unregister call if event handler is not needed
        m_${class}EventHandler.unregisterEvents();
        
        // Remove the wake locks before the transport layer is deactivated.
        m_WakeLock.delete();
        m_RecvWakeLock.delete();
    }

    @Override
    public void initialize(final TransportLayerContext context, final Map<String, Object> props)
    {
        m_Context = context;
        
        // Retrieve a wake lock used to keep the system awake during vital transport layer operations.
        m_WakeLock = m_Context.createPowerManagerWakeLock("${class}WakeLock");

        // Retrieve a wake lock used to keep the system awake while the transport layer is receiving data.
        m_RecvWakeLock = m_Context.createPowerManagerWakeLock("${class}RecvWakeLock");

        // ${task}: Replace with custom handling of properties when transport layer is created or restored. `config` 
        // object uses reflection to obtain property from map and therefore should not be used in processing intensive 
        // code.
        final ${class}Attributes config = Configurable.createConfigurable(${class}Attributes.class, props);
        
        // Properties that have been defined in ${class}Attributes will be accessible through the `config` object
        // m_SomeProperty = config.someProperty();
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // ${task}: Replace with custom handling of properties when they are updated. `config` object uses
        // reflection to obtain property from map and therefore should not be used in processing intensive code.
        final ${class}Attributes config = Configurable.createConfigurable(${class}Attributes.class, props);
        
        // Properties that have been defined in ${class}Attributes will be accessible through the `config` object
        // m_SomeProperty = config.someProperty();
    }
    
    @Override
    public void connect(final Address address) throws CCommException
    {
        try
        {
            m_WakeLock.activate();
       
            // ${task}: If layer is connection oriented (see capabilities XML), this method should setup the connection 
            // to the given address. If not, this method will not be called and should be empty.
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }
    
    @Override
    public void disconnect() throws CCommException
    {
    	try
        {
            m_WakeLock.activate();
       
            // ${task}: If layer is connection oriented (see capabilities XML), this method should clean up the 
            // connection to the previously connected address. If not, this method will not be called and should be 
            // empty.
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }
    
    @Override
    public boolean isConnected()
    {
        // ${task}: If layer is connection oriented (see capabilities XML), this method should return true if the 
        // connection is currently active and the endpoint is able to receive data, false if not. If layer is not 
        // connection oriented, this method will not be called and should throw an UnsupportedOperationException.
        return false;
    }

    @Override
    public void send(final ByteBuffer data, final Address addr) throws CCommException
    {
        // ${task}: If layer is NOT connection oriented (see capabilities XML), this method must be implemented. If 
        // connection oriented layer, method can be left blank. If implementing, method must send data to the given 
        // address. Typically this requires breaking up the data into link frames to be sent using a link layer, but 
        // other mechanisms can be used.
    }
    
    @Override
    public void send(final ByteBuffer data) throws CCommException
    {
        // ${task}: If layer is connection oriented (see capabilities XML), this method must be implemented. If NOT 
        // connection oriented layer, method can be left blank. If implementing, method must send data to the connected 
        // address. Typically this requires breaking up the data into link frames to be sent using a link layer, but 
        // other mechanisms can be used. Method will only be called if layer is connected to an endpoint.
    }
    
    @Override
    public boolean isAvailable(final Address address)
    {
        // ${task}: Return whether the address passed in defines an endpoint that can currently be reached
        return false;
    }
    
    @Override
    public void onShutdown()
    {
        // ${task}: Handle shutdown of transport layer. Any code needed to cleanup the transport layer should go here.
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        // ${task}: Modify so that the set returned contains any plug-in specific extensions. The use of extensions is 
        // discouraged, but can be used in cases where it is necessary to provide an extended API.
        
        return new HashSet<Extension<?>>();
    }
    
    /**
     * Class to handle registering, handling, and unregistering for events.
     */
    class ${class}EventHandler implements EventHandler
    {
        /**
         * The service registration reference for this handler class.
         */
        private ServiceRegistration<EventHandler> m_EventHandlerReg;
        
        /**
         * Method used to register this handler class for events.
         * @param bundleContext
         *      the context of the transport layer
         */
        public void registerEvents(final BundleContext bundleContext)
        {
            final Dictionary<String, String> dict = new Hashtable<>();
        
            dict.put(EventConstants.EVENT_TOPIC, LinkLayer.TOPIC_DATA_RECEIVED);
            
            // This filter will make it so that the EventHandler is only notified for data that is produced
            // by a link layer named myLinkLayer. It is recommended that the myLinkLayer value be represented 
            // by a configuration value.
            dict.put(EventConstants.EVENT_FILTER, "(" + FactoryDescriptor.EVENT_PROP_OBJ_NAME + "=myLinkLayer)");
            m_EventHandlerReg = bundleContext.registerService(EventHandler.class, this, dict);
        }
        
        /**
         * Method used to unregister this handler class.
         */
        public void unregisterEvents()
        {
            m_EventHandlerReg.unregister();
        }
        
        /**
         * ${task}: Modify the following code to be able to process data that is received by a transport layer's data
         *             stream. The data stream could be a link layer or something like a UDP stream.
         *
         * Below demonstrates what an event handler handleEvent method should do when receiving data. The handleEvent
         * method must indicate when it begins and ends receiving data. This is done by using the
         * TransportLayerContext's beginReceiving() and endReceiving() methods.
         *
         * {@inheritDoc}
         *
         * @param event
         *      the event from the link layer
         */
        @Override
        public void handleEvent(final Event event)
        {
            try
            {
                m_RecvWakeLock.activate();

                // This indicates to the core that this transport layer is currently receiving data.
                // This call must be made when processing of data begins.
                m_Context.beginReceiving();
            
                final Address sourceAddress = (Address)event.getProperty(Address.EVENT_PROP_SOURCE_ADDRESS_PREFIX 
                    + Address.EVENT_PROP_MESSAGE_ADDRESS_SUFFIX);
            
                final Address destAddress = (Address)event.getProperty(Address.EVENT_PROP_DEST_ADDRESS_PREFIX 
                    + Address.EVENT_PROP_MESSAGE_ADDRESS_SUFFIX);
                
                final LinkFrame linkframe = (LinkFrame)event.getProperty(LinkLayer.EVENT_PROP_LINK_FRAME);
            
                // ${task}: Link frames would be gathered into some kind of collection that could be used 
                // to determine, at some point in time, whether or not all frames were received. If so, then 
                // a TransportPacket should be created and the following call should be made:
            
                final TransportPacket transportPkt = null;
            
                // This indicates to the core that the transport layer has finished receiving data and will post 
                // a TransportLayer.TOPIC_PACKET_RECEIVED event.
                m_Context.endReceiving(transportPkt, sourceAddress, destAddress);
            }
            finally
            {
                m_RecvWakeLock.cancel();
            }
        }
    }
}
