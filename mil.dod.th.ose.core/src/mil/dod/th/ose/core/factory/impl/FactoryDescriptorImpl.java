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

package mil.dod.th.ose.core.factory.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Objects;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.ccomm.capability.AddressCapabilities;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.datastream.capability.StreamProfileCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Implementation of {@link FactoryInternal}.
 * 
 * @author cashioka
 */
@Component(factory = FactoryInternal.FACTORY_NAME)
public class FactoryDescriptorImpl implements FactoryInternal
{
    /**
     * The persistent identifier of the factory object plug-in.
     */
    private String m_PluginPid;
    
    /**
     * List of ServiceRegistrations for this plug-in.
     */
    private List<ServiceRegistration<?>> m_ServiceRegs = new ArrayList<>();
    
    /**
     * The XML unmarshal service.
     */
    private XmlUnmarshalService m_XMLUnmarshalService;
    
    /**
     * The capabilities object for the represented {@link FactoryDescriptor}.
     */
    private BaseCapabilities m_Capabilities;

    /**
     * Context for the {@link mil.dod.th.ose.core.factory.api.DirectoryService} that manages this factory descriptor.
     */
    private FactoryServiceContext<?> m_FactoryServiceContext;

    /**
     * Product type for the plug-in described by this component.
     */
    private String m_ProductType;

    /**
     * Reference to the {@link ComponentFactory} service registered by the plug-in for the {@link FactoryObjectProxy} 
     * implementation.
     */
    private ServiceReference<ComponentFactory> m_PluginServiceRef;

    /**
     * Factory which produces {@link FactoryObjectProxy}.
     */
    private ComponentFactory m_ComponentFactory;

    /**
     * Map to keep track of {@link FactoryObjectProxy} instances created.
     */
    private Map<FactoryObjectProxy, ComponentInstance> m_InstanceMap = 
            Collections.synchronizedMap(new HashMap<FactoryObjectProxy, ComponentInstance>());
    
    /**
     * Binds the XML unmarshal service.
     * 
     * @param xmlUS
     *            the XML unmarshal service
     */
    @Reference
    public void setXMLUnmarshalService(final XmlUnmarshalService xmlUS)
    {
        m_XMLUnmarshalService = xmlUS;
    }
    
    /**
     * Activates the factory.
     * 
     * @param props
     *      component properties, must have {@link #KEY_SERVICE_CONTEXT}, {@link #KEY_SERVICE_REFERENCE} and
     *      {@link #KEY_COMPONENT_FACTORY} properties set
     * @throws UnmarshalException 
     *      if the capabilities resource is invalid and cannot be read
     */
    @Activate
    public void activate(final Map<String, Object> props) throws UnmarshalException
    {   
        m_ComponentFactory = (ComponentFactory)props.get(KEY_COMPONENT_FACTORY);
        m_FactoryServiceContext = (FactoryServiceContext<?>)props.get(KEY_SERVICE_CONTEXT);
        @SuppressWarnings("unchecked")
        final ServiceReference<ComponentFactory> serviceRef = 
                (ServiceReference<ComponentFactory>)props.get(KEY_SERVICE_REFERENCE);
        m_PluginServiceRef = serviceRef;
        
        m_ProductType = (String)serviceRef.getProperty(ComponentConstants.COMPONENT_NAME);

        // read in capabilities XML
        final URL xmlResource = m_XMLUnmarshalService.getXmlResource(m_PluginServiceRef.getBundle(), 
                CAPABILITIES_XML_FOLDER_NAME, m_ProductType);
        m_Capabilities = m_XMLUnmarshalService.getXmlObject(m_FactoryServiceContext.getCapabilityType(), xmlResource);
        
        m_PluginPid = getProductType() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX; 
    
        //verify plug-in OCD and associated attribute definitions can be found
        getPluginAttributeDefinitions(ObjectClassDefinition.ALL);
    }
    
    @Override
    public BaseCapabilities getCapabilities()
    {
        return m_Capabilities;
    }
    
    @Override
    public AssetCapabilities getAssetCapabilities()
    {
        // method is provided by AssetFactory only so casting should be okay
        return (AssetCapabilities)m_Capabilities;
    }
    
    @Override
    public StreamProfileCapabilities getStreamProfileCapabilities()
    {
        //method is provided by StreamProfileFactory ony so casting should be okay
        return (StreamProfileCapabilities)m_Capabilities;
    }

    @Override
    public AddressCapabilities getAddressCapabilities()
    {
        // method is provided by AddressFactory only so casting should be okay
        return (AddressCapabilities)m_Capabilities;
    }
    
    @Override
    public PhysicalLinkCapabilities getPhysicalLinkCapabilities()
    {
        // method is provided by PhysicalLinkFactory only so casting should be okay
        return (PhysicalLinkCapabilities)m_Capabilities;
    }
    
    @Override
    public LinkLayerCapabilities getLinkLayerCapabilities()
    {
        // method is provided by LinkLayerFactory only so casting should be okay
        return (LinkLayerCapabilities)m_Capabilities;
    }
    
    @Override
    public TransportLayerCapabilities getTransportLayerCapabilities()
    {
        // method is provided by TransportLayerFactory only so casting should be okay
        return (TransportLayerCapabilities)m_Capabilities;
    }
    
    @Override
    public String getProductType()
    {
        return m_ProductType;
    }
    
    @Override
    public String getProductName()
    {
        return this.getCapabilities().getProductName();
    }
    
    @Override
    public String getProductDescription()
    {
        return this.getCapabilities().getDescription();
    }

    @Override
    public AttributeDefinition[] getPluginAttributeDefinitions(final int filter)
    {
        final MetaTypeInformation info = m_FactoryServiceContext.getMetaTypeService().getMetaTypeInformation(
                m_PluginServiceRef.getBundle());
        
        if (Arrays.asList(info.getPids()).contains(m_PluginPid))
        {
            final ObjectClassDefinition ocd = info.getObjectClassDefinition(m_PluginPid, null);
            if (!ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION.equals(ocd.getDescription()))
            {
                throw new IllegalStateException(String.format(
                        "The Attributes interface for plug-in with pid [%s] does not have the OCD description: [%s]", 
                        m_PluginPid, ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION));
            }
            return Objects.firstNonNull(ocd.getAttributeDefinitions(filter), 
                    new AttributeDefinition[] {});
        }
        else
        {
            throw new IllegalStateException(String.format(
                    "Unable to find attribute definitions for plug-in with PID: [%s], verify that the Attribute "
                    + "Definition interface is annotated properly.", m_PluginPid));
        }
    }
    
    /**
     * Register a factory's {@link MetaTypeProvider} and {@link ManagedServiceFactory} services. These services are 
     * used by the {@link org.osgi.service.cm.ConfigurationAdmin} to provide multiple configurations, provide default 
     * values for configuration properties, and define property keys.
     * 
     * After this call the factory is not available as a factory to other services. 
     * Instead see {@link #makeAvailable()}.
     * 
     */
    @Override
    public void registerServices()
    {
        // Register the ManagedServiceFactory and MetaTypeProvider services
        final FactoryConfigurationService configService = 
                new FactoryConfigurationService(this, m_FactoryServiceContext);

        final Dictionary<String, Object> configServProps = new Hashtable<String, Object>();
        configServProps.put(Constants.SERVICE_PID, getPid());
        configServProps.put(MetaTypeProvider.METATYPE_FACTORY_PID, getPid());
        final String[] clazzes = {ManagedServiceFactory.class.getName(), MetaTypeProvider.class.getName()};
        final BundleContext context = 
                m_FactoryServiceContext.getMetaTypeProviderBundle().getBundle().getBundleContext();
        final ServiceRegistration<?> configServReg = context.registerService(clazzes, configService, configServProps);
        Logging.log(LogService.LOG_DEBUG, "Configuration services registered for PID [%s]", getPid());
        
        m_ServiceRegs.add(configServReg);
    }
    
    @Override
    public void makeAvailable()
    {
        final Dictionary<String, Object> factDescProps = new Hashtable<String, Object>();
        factDescProps.put(FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY, getProductType());
        
        //additional service props
        final Dictionary<String, Object> addFactProps = 
                m_FactoryServiceContext.getAdditionalFactoryRegistrationProps(FactoryDescriptorImpl.this);
        if (!addFactProps.isEmpty())
        {
            final Enumeration<String> keys = addFactProps.keys();
            while (keys.hasMoreElements())
            {
                final String key = keys.nextElement();
                factDescProps.put(key, addFactProps.get(key));
            }
        }
        
        final ServiceRegistration<FactoryDescriptor> factDescReg = m_FactoryServiceContext.getCoreContext()
                .registerService(FactoryDescriptor.class, this, factDescProps);
        m_ServiceRegs.add(factDescReg);

        Logging.log(LogService.LOG_INFO, "Factory for [%s] registered", getProductName());
    }
    
    @Override
    public void makeUnavailable()
    {
        for (ServiceRegistration<?> reg : new ArrayList<>(m_ServiceRegs))
        {
            final Object prop = reg.getReference().getProperty(FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY);
            if (prop != null && getProductType().equals(prop))
            {
                reg.unregister();
                //clean-up, as the reference is no longer valid
                m_ServiceRegs.remove(reg);
                Logging.log(LogService.LOG_INFO, "FactoryDescriptor for [%s] unregistered", getProductName());
            }
        }
    }
    
    @Override
    public void cleanup()
    {
        for (ServiceRegistration<?> reg : new ArrayList<>(m_ServiceRegs))
        {
            reg.unregister();
            //clean-up, as the reference is no longer valid
            m_ServiceRegs.remove(reg);
        }

        Logging.log(LogService.LOG_INFO, "Factory [%s] completed unregistration.", getProductName());
    }

    @Override
    public FactoryObjectProxy create()
    {
        final ComponentInstance instance = m_ComponentFactory.newInstance(null);
        final FactoryObjectProxy proxy = (FactoryObjectProxy)instance.getInstance();
        m_InstanceMap.put(proxy, instance);
        return proxy;
    }

    @Override
    public void dispose(final FactoryObjectProxy obj)
    {
        final ComponentInstance instance = m_InstanceMap.remove(obj);
        if (instance != null)
        {
            instance.dispose();
        }
    }
    
    @Override
    public String getPid()
    {
        return getProductType() + FactoryDescriptor.PID_SUFFIX;
    }
    
    @Override
    public AttributeDefinition[] getAttributeDefinitions()
    {
        final MetaTypeInformation metaInfo = m_FactoryServiceContext.getMetaTypeService().getMetaTypeInformation(
                        m_FactoryServiceContext.getMetaTypeProviderBundle().getBundle());
        if (metaInfo != null)
        {
            final ObjectClassDefinition ocd = metaInfo.getObjectClassDefinition(getPid(), null);
            return Objects.firstNonNull(ocd.getAttributeDefinitions(ObjectClassDefinition.ALL), 
                    new AttributeDefinition[] {});             
        }

        return new AttributeDefinition[] {};
    }
    
    @Override
    public AttributeDefinition[] getAttributeDefinitions(final int filter)
    {
        final MetaTypeInformation metaInfo = m_FactoryServiceContext.getMetaTypeService().getMetaTypeInformation(
                        m_FactoryServiceContext.getMetaTypeProviderBundle().getBundle());
        if (metaInfo != null)
        {
            final ObjectClassDefinition ocd = metaInfo.getObjectClassDefinition(getPid(), null);
            switch (filter)
            {
                case ObjectClassDefinition.REQUIRED:
                    return Objects.firstNonNull(ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED), 
                            new AttributeDefinition[] {});
                case ObjectClassDefinition.OPTIONAL:
                    return Objects.firstNonNull(ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL), 
                            new AttributeDefinition[] {});
                case ObjectClassDefinition.ALL:
                    return Objects.firstNonNull(ocd.getAttributeDefinitions(ObjectClassDefinition.ALL), 
                            new AttributeDefinition[] {});
                default:
                    return new AttributeDefinition[] {};
            }             
        }
        
        return new AttributeDefinition[] {};
    }
}
