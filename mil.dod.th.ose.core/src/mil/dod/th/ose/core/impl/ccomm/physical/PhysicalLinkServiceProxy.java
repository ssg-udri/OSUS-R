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
package mil.dod.th.ose.core.impl.ccomm.physical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.Objects;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.ccomm.physical.Gpio;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLinkProxy;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.impl.ccomm.PhysicalLinkRegistryCallback;
import mil.dod.th.ose.core.impl.ccomm.physical.data.PhysicalLinkFactoryObjectDataManager;

import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Proxy to provide {@link mil.dod.th.core.ccomm.physical.PhysicalLink} specific service functionality.
 * 
 * @author allenchl
 *
 */
@Component(properties = { PhysicalLinkInternal.SERVICE_TYPE_PAIR })
public class PhysicalLinkServiceProxy implements FactoryServiceProxy<PhysicalLinkInternal>
{
    /**
     * Metatype service reference.
     */
    private MetaTypeService m_MetaService;
    
    /**
     * Component factory used to create {@link PhysicalLinkImpl}'s.
     */
    private ComponentFactory m_PhysicalLinkComponentFactory;
    
    /**
     * Component factory used to create {@link SerialPortImpl}'s.
     */
    private ComponentFactory m_SerialPortComponentFactory;
    
    /**
     * Service for storing persisted data about physical links.
     */
    private PhysicalLinkFactoryObjectDataManager m_PhysicalFactoryObjectDataManager;
    
    private LoggingService m_Logging;
    
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Metatype service for fetching {@link MetaTypeInformation} information.
     * @param metaService
     *  the metatype service to use
     */
    @Reference
    public void setMetaTypeService(final MetaTypeService metaService)
    {
        m_MetaService = metaService;
    }
    
    /**
     * Method to set the component factory that will be used to create 
     * instances of {@link PhysicalLinkImpl}.
     * @param factory
     *  the factory that will be used to create the instances
     */
    @Reference(target = "(component.factory=" + PhysicalLinkInternal.COMPONENT_FACTORY_REG_ID + ")")
    public void setPhysicalLinkFactory(final ComponentFactory factory)
    {
        m_PhysicalLinkComponentFactory = factory;
    }
    
    /**
     * Method to set the component factory that will be used to create 
     * instances of {@link PhysicalLinkImpl}.
     * @param factory
     *  the factory that will be used to create the instances
     */
    @Reference(target = "(component.factory=" + PhysicalLinkInternal.COMPONENT_SERIAL_PORT_FACTORY_REG_ID + ")")
    public void setSerialPortFactory(final ComponentFactory factory)
    {
        m_SerialPortComponentFactory = factory;
    }
    
    /**
     * Bind the factory object data information manager for physical links.
     * 
     * @param physFactObjManager
     *      used to store the persisted data
     */
    @Reference
    public void setPhysicalLinkFactoryObjectDataManager(final PhysicalLinkFactoryObjectDataManager physFactObjManager)
    {
        m_PhysicalFactoryObjectDataManager = physFactObjManager;
    }
    
    @Override
    public void initializeProxy(final PhysicalLinkInternal object, final FactoryObjectProxy proxy, 
            final Map<String, Object> props) throws FactoryException
    {
        final PhysicalLinkProxy pLinkProxy = (PhysicalLinkProxy)proxy;
        pLinkProxy.initialize(object, props);
    }

    @Override
    public ComponentInstance createFactoryObjectInternal(final FactoryInternal factory)
    {
        if (((PhysicalLinkCapabilities)factory.getCapabilities()).getLinkType() == PhysicalLinkTypeEnum.SERIAL_PORT)
        {
            return m_SerialPortComponentFactory.newInstance(new Hashtable<String, Object>());
        }
        else
        {
            return m_PhysicalLinkComponentFactory.newInstance(new Hashtable<String, Object>());
        }
    }

    @Override
    public AttributeDefinition[] getExtendedServiceAttributeDefinitions(
            final FactoryServiceContext<PhysicalLinkInternal> factoryServiceContext, 
            final FactoryInternal factory, final int filter)
    {
        final PhysicalLinkCapabilities capabilities = (PhysicalLinkCapabilities)factory.getCapabilities();
        final PhysicalLinkTypeEnum linkType = capabilities.getLinkType();
        
        final String physPidBasedOnType;
        if (linkType == PhysicalLinkTypeEnum.SERIAL_PORT)
        {
            physPidBasedOnType = SerialPort.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX;
        }
        else if (linkType == PhysicalLinkTypeEnum.GPIO)
        {
            physPidBasedOnType = Gpio.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX;
        }
        else
        {
            return new AttributeDefinition[0];
        }

        final MetaTypeInformation info = m_MetaService.getMetaTypeInformation(factoryServiceContext.getApiBundle());

        final List<AttributeDefinition> attrs = new ArrayList<>();
        
        if (Arrays.asList(info.getPids()).contains(physPidBasedOnType))
        {
            final ObjectClassDefinition ocd = info.getObjectClassDefinition(physPidBasedOnType, null);
            final AttributeDefinition[] attributeDefinitions = ocd.getAttributeDefinitions(filter);
            final List<AttributeDefinition> linkTypeList = 
                    Arrays.asList(Objects.firstNonNull(attributeDefinitions, new AttributeDefinition[] {}));
            attrs.addAll(linkTypeList);
        } 

        return attrs.toArray(new AttributeDefinition[0]);
    }
    
    @Override
    public Dictionary<String, Object> getAdditionalFactoryRegistrationProps(final FactoryInternal factory)
    {
        final Dictionary<String, Object> physFactProps = new Hashtable<>();
        physFactProps.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, PhysicalLinkFactory.class);
        physFactProps.put(PhysicalLink.LINK_TYPE_SERVICE_PROPERTY, 
                factory.getPhysicalLinkCapabilities().getLinkType().value());
        return physFactProps;
    }

    @Override
    public Class<? extends BaseCapabilities> getCapabilityType()
    {
        return PhysicalLinkCapabilities.class;
    }

    @Override
    public Class<? extends FactoryObject> getBaseType()
    {
        return PhysicalLink.class;
    }

    @Override
    public void beforeAddFactory(final FactoryServiceContext<PhysicalLinkInternal> factoryServiceContext,
            final FactoryInternal factory)
    {
        // nothing to check
    }

    @Override
    public void onRemoveFactory(final FactoryServiceContext<PhysicalLinkInternal> factoryServiceContext,
            final FactoryInternal factory)
    {
        // disconnect all Physical Links
        for (PhysicalLink cl : factoryServiceContext.getRegistry().getObjectsByProductType(factory.getProductType()))
        {
            if (cl.isOpen())
            {
                try
                {
                    cl.close();
                }
                catch (final PhysicalLinkException e)
                {
                    m_Logging.log(LogService.LOG_WARNING, e, 
                            "Unable to close Physical Link [%s] during deactivation.", cl.getName());
                }
            }
        }
    }

    @Override
    public FactoryObjectDataManager getDataManager()
    {
        return m_PhysicalFactoryObjectDataManager;
    }

    @Override
    public FactoryRegistryCallback<PhysicalLinkInternal> createCallback(
            final FactoryServiceContext<PhysicalLinkInternal> factoryServiceContext)
    {
        return new PhysicalLinkRegistryCallback(m_Logging);
    }
}
