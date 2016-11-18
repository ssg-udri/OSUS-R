package ${package};

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkFrame;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.link.LinkLayerContext;
import mil.dod.th.core.ccomm.link.LinkLayerProxy;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.pm.WakeLock;

import org.osgi.service.log.LogService;

/**
 * ${description} implementation.
 * 
 * @author ${author}
 */
@Component(factory = LinkLayer.FACTORY)
public class ${class} implements LinkLayerProxy
{
    /**
     * Reference to the context which provides this class with methods to interact with the rest of the system.
     */
    private LinkLayerContext m_Context;
    
    /**
     * Reference to the wake lock used for vital link layer operations.
     */
    private WakeLock m_WakeLock;
    
    /**
     * Reference to the address manager service needed to get/create addresses.
     */
    private AddressManagerService m_AddressManagerService;
    
    /**
     * Set the reference to the address manager service.
     *
     * @param addrMgrService
     *     reference to the address manager service needed to get/create addresses
     */
    @Reference
    public void setAddressManagerService(final AddressManagerService addrMgrService)
    {
        m_AddressManagerService = addrMgrService;
    }
    
    @Override
    public void initialize(final LinkLayerContext context, final Map<String, Object> props) throws FactoryException
    {
        m_Context = context;

        // Create the wake lock used to keep the system awake during vital operations.
        m_WakeLock = m_Context.createPowerManagerWakeLock("${class}WakeLock");

        // ${task}: Replace with custom handling of properties when link layer is created or restored. `config` object 
        // uses reflection to obtain property from map and therefore should not be used in processing intensive code.
        final ${class}Attributes config = Configurable.createConfigurable(${class}Attributes.class, props);

        // Properties that have been defined in ${class}Attributes will be accessible through the `config` object
        // m_SomeProperty = config.someProperty();
    }

    /**
     * Deactivate the link layer when removed or core is shutdown.
     */
    @Deactivate
    public void release()
    {
        // Remove the wake lock before the link layer is deactivated.
        m_WakeLock.delete();
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
    public void onActivate()
    {
        Logging.log(LogService.LOG_INFO, "${description} activated");
        
        // ${task}: Can enable the processing of data here. Below is an example of how to initiate 
        // and start reading data from the link layer's physical link
        
        final PhysicalLinkListener listener = new PhysicalLinkListener();
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(listener);
    }

    @Override
    public void onDeactivate()
    {
        Logging.log(LogService.LOG_INFO, "${description} deactivated");

        // ${task}: Can disable the processing of data here.
        try
        {
            m_Context.getPhysicalLink().close();
        }
        catch (final PhysicalLinkException ex)
        {
            Logging.log(LogService.LOG_ERROR, ex, "Error closing physical link");
        }
    }
    
    @Override
    public boolean isAvailable(final Address address)
    {
        // ${task}: Return whether the address passed in defines an endpoint that can currently be reached
        return false;
    }
    
    @Override
    public int send(final LinkFrame frame, final Address addr) throws CCommException
    {
        try
        {
            // Activate the wake lock to prevent the system from going to sleep while trying to send.
            m_WakeLock.activate();
            
            // ${task}: Need to support sending data.
        
            // The example below assumes that ${class}Address and ${class}AddressAttributes classes exist
            // to handle the addressing of the message.
            //
            // Get the address info out of the address properties.
            // Map<String, Object> addrProps = addr.getProperties();
            // int destDeviceId = (Integer)addrProps.get(${class}AddressAttributes.KEY_DEVICE_ID);
            // int destSubId = (Integer)addrProps.get(${class}AddressAttributes.KEY_SUB_ID);
            //
            // Get the number of bytes sent.
            // byte[] data = frame.getPayload().array();
            // Logging.log(LogService.LOG_INFO, "${class}: sending %d bytes to address %d/%d",
            //    data.length, destDeviceId, destSubId);
            //
            // Return the number of bytes sent.  
            // return data.length;
        
            // Used if not returning the number of bytes sent.
            return 0;
        }
        finally
        {
            // Cancel the wake lock regardless of whether or not sending succeeded.
            m_WakeLock.cancel();
        }
    }
    
    @Override
    public LinkStatus onPerformBit()
    {
        // ${task}: Replace with actual BIT testing, this is just an example. This should perform any built-in-test
        // that the link layer device supports, and report the outcome of the action.
        Logging.log(LogService.LOG_INFO, "Performing ${description} BIT");
        return LinkStatus.OK;
    }    
    
    @Override
    public int getDynamicMtu() throws UnsupportedOperationException
    {
        // ${task}: If the MTU cannot be statically set in the capabilities XML for the link layer, then it should 
        // be dynamically returned here. Be sure to indicate in the LinkLayerCapabilities that the MTU is not 
        // statically set.
        
        throw new UnsupportedOperationException("Dynamic MTU is not supported");
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        // ${task}: Modify so that the set returned contains any plug-in specific extensions. The use of extensions is 
        // discouraged, but can be used in cases where it is necessary to provide an extended API.
    
    	return new HashSet<Extension<?>>();
    }
    
    /**
     * ${task}: Modify the following code to be able to process data that is sent over a physical link.
     *
     * The following is an example of what should be done to properly receive data. Typically, this is done by polling 
     * the physical link periodically for data. Once data has been received, it is expected that the link layer will 
     * post an event indicating that it just received data. This can be done by using the LinkLayerContext's 
     * postReceiveEvent() method. This receive service should be started when a link layer is activated. See the 
     * activate method above for an example of how this class should be executed.
     */
    private class PhysicalLinkListener implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                m_Context.getPhysicalLink().open();
            }
            catch (final PhysicalLinkException ex)
            {
                Logging.log(LogService.LOG_ERROR, ex, "Error opening physical link");
            }
        
            while (m_Context.getPhysicalLink().isOpen())
            {
                //read from the stream
                InputStream stream = null;
                try
                {
                    stream = m_Context.getPhysicalLink().getInputStream();
                }
                catch (final PhysicalLinkException ex)
                {
                    Logging.log(LogService.LOG_ERROR, ex, "Error retrieving physical link input stream");
                }
                
                if (stream != null)
                {
                    // ${task}: Create a custom link frame that holds the necessary data.
                    // The example below assumes some MyLinkFrame class exists somewhere
                    // and defines the structure of the data that is read from the physical link.
                    // The example also assumes that an AddressTranslator class exists somewhere
                    // to parse the source and destination addresses from the message.
                    
                    // final MyLinkFrame frame = new MyLinkFrame(stream);
                    // final String srcAddrSuffix = frame.getSourceAddressSuffix();
                    // final String destAddrSuffix = frame.getDestinationAddressSuffix();
                    // final int srcSensorId = frame.getSourceAddressSensorId();
                    // final int destSensorId = frame.getDestinationSensorId(); 
                     
                    // Map<String, Object> sourceAddressProps = 
                    //     ${class}AddressTranslator.getAddressPropsFromString(srcAddrSuffix);
                    // final Address sourceAddr = 
                    //     m_AddressManagerService.getOrCreateAddress(${class}Address.ADDRESS_TYPE,
                    //     String.format("${class}Auto%d", srcSensorId), sourceAddressProps); 
                    
                    // Map<String, Object> destAddressProps = 
                    //     ${class}AddressTranslator.getAddressPropsFromString(destAddrSuffix);
                    // final Address destAddr = 
                    //     m_AddressManagerService.getOrCreateAddress(${class}Address.ADDRESS_TYPE,
                    //     String.format("${class}Auto%d", destSensorId), destAddressProps);      
                    
                    // post event that data was received
                    // m_Context.postReceiveEvent(sourceAddr, destAddr, frame);
                }
            }
        }
    }
}
