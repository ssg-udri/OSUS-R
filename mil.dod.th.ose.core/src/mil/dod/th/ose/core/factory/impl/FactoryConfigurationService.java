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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Base implementation of a factory configuration service used to register attributes of a factory with the 
 * {@link org.osgi.service.cm.ConfigurationAdmin} and {@link MetaTypeProvider}.  Each service that maintains 
 * {@link mil.dod.th.core.factory.FactoryObject}s 
 * must extend this class.
 * 
 * @author dlandoll
 */
public class FactoryConfigurationService implements ManagedServiceFactory, MetaTypeProvider
{ 
    /**
     * Utility used to log messages.
     */
    protected LoggingService m_Logging;
    
    /**
     * Reference to the associated factory descriptor.
     */
    protected final FactoryInternal m_Factory;
    
    /**
     * MetaTypeService used for lookup of annotated config interface information.
     */
    protected MetaTypeService m_MetaType;
    
    /**
     * Name used for the ManagedServiceFactory and ObjectClassDefinition.
     */
    private final String m_Name;
    
    /**
     * Factory PID value.
     */
    private final String m_Pid;
    
    /**
     * Base PID value.
     */
    private final String m_ServicePid;
    
    /**
     * Reference to the associated MetaType ObjectClassDefinition.
     */
    private ObjectClassDefinition m_OCD;

    /**
     * Registry that maintains all objects for the objects created by this factory configuration.
     */
    @SuppressWarnings("rawtypes") //registry is not parameterized
    private final FactoryRegistry m_Registry;

    /**
     * Context for the {@link mil.dod.th.ose.core.factory.api.DirectoryService} using this class.
     */
    private FactoryServiceContext<?> m_FactoryServiceContext;
    
    /**
     * Constructor used to initialize the configuration service.
     * 
     * @param factory
     *  Reference to the associated factory
     * @param context
     *  Reference to the associated factory service context
     */
    public FactoryConfigurationService(final FactoryInternal factory, final FactoryServiceContext<?> context)
    {
        m_Pid = factory.getPid();
        m_Name = factory.getProductType();
        m_Factory = factory;
        m_FactoryServiceContext = context;
        m_Registry = context.getRegistry();
        m_ServicePid = context.getBaseType().getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX;
        m_MetaType = context.getMetaTypeService();
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(final String pid, final String locale)
    {
        if (m_Pid.equals(pid))
        {
            if (m_OCD == null)
            {
                ArrayList<AttributeDefinition> adListReq = new ArrayList<AttributeDefinition>();
                ArrayList<AttributeDefinition> adListOpt = new ArrayList<AttributeDefinition>();
                
                adListReq = buildAdList(ObjectClassDefinition.REQUIRED);
                adListOpt = buildAdList(ObjectClassDefinition.OPTIONAL);
               
                m_OCD = new FactoryObjectClassDefinition(m_Name, m_Pid, m_Factory.getProductDescription(),
                        adListReq.toArray(new AttributeDefinition[adListReq.size()]),
                        adListOpt.toArray(new AttributeDefinition[adListOpt.size()]));
            }

            return m_OCD;
        }
        else
        {
            throw new IllegalStateException(String.format("Invalid PID passed to getObjectClassDefinition %s", pid));
        }
    }
    
    /**
     * Builds an ArrayList of attributes.
     * 
     * @param filter
     *  Whether the attribute is required or optional
     * @return
     *  Filter specific definitions 
     */
    private ArrayList<AttributeDefinition> buildAdList(final int filter)
    {
        final ArrayList<AttributeDefinition> adList = new ArrayList<AttributeDefinition>();

        // Add the service specific attributes
        final AttributeDefinition[] serviceAttrs = getServiceAttributeDefinitions(filter);
        if (serviceAttrs != null)
        {
            for (AttributeDefinition attr : serviceAttrs)
            {
                adList.add(attr);
            }
        }

        // Add the product specific attributes
        final AttributeDefinition[] pluginAttrs = m_Factory.getPluginAttributeDefinitions(filter);
        if (pluginAttrs != null)
        {
            for (AttributeDefinition attr : pluginAttrs)
            {
                adList.add(attr);
            }
        }
        
        return adList;
    }

    @Override
    public String[] getLocales() //NOPMD: API specifies to return null if no local specific localization is used 
    {
        return null;
    }

    @Override
    public String getName()
    {
        return m_Name;
    }
    
    /**
     * Get the service specific attribute definitions.
     * 
     * @param filter
     *  Whether the attribute is required or optional
     * 
     * @return 
     *  Factory specific definitions
     */
    public AttributeDefinition[] getServiceAttributeDefinitions(final int filter)
    {
        final Bundle apiBundle = m_FactoryServiceContext.getApiBundle();
        final MetaTypeInformation info = m_MetaType.getMetaTypeInformation(apiBundle);
        
        final List<AttributeDefinition> attrs = new ArrayList<>();
        
        if (Arrays.asList(info.getPids()).contains(m_ServicePid))
        {
            final ObjectClassDefinition ocd = info.getObjectClassDefinition(m_ServicePid, null);
            if (!ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION.equals(ocd.getDescription()))
            {
                throw new IllegalStateException(String.format(
                    "The Attributes interface for the service with pid [%s] does not have the OCD description: [%s]",
                    m_ServicePid, ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION));
            }
            final AttributeDefinition[] attrDefs = ocd.getAttributeDefinitions(filter);
            if (attrDefs != null)
            {
                attrs.addAll(Arrays.asList(attrDefs));
            }
        }
        else
        {
            throw new IllegalStateException(
                    String.format("Unable to retrieve service attribute definitions for PID [%s] from bundle [%s]", 
                            m_ServicePid, apiBundle));
        }
        
        final AttributeDefinition[] extServAttrDefs = m_FactoryServiceContext.
                getExtendedServiceAttributeDefinitions(m_Factory, filter);
        
        if (extServAttrDefs != null)
        {
            attrs.addAll(Arrays.asList(extServAttrDefs));
        }
            
        return attrs.toArray(new AttributeDefinition[attrs.size()]);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked" })
    @Override
    public void updated(final String pid, final Dictionary properties) throws ConfigurationException
    {
        // TODO: TH-60 need to handle case where deps no longer satisfied after creation
        final FactoryObjectInternal object = m_Registry.getObjectByPid(pid);
        if (object != null)
        {
            try
            {
                m_Registry.handleUpdated(object);

                //This call should come last as it releases a lock that the initiator
                //of this call may have. Also rebase is meant to setup new dependencies
                //if needed and therefore the object should be rebased before being notified
                //of any new properties.
                object.blockingPropsUpdate(ConfigurationUtils.convertDictionaryPropsToMap(properties));
            }
            catch (final FactoryException e)
            {
                Logging.log(LogService.LOG_ERROR, e, "Unable to update object");
            }
        }
    }

    @Override
    public void deleted(final String pid)
    {
        final FactoryObjectInternal object = m_Registry.getObjectByPid(pid);
        if (object != null)
        {
            try
            {
                m_Registry.unAssignPidForObj(object);
            }
            catch (final FactoryObjectInformationException e)
            {
                Logging.log(LogService.LOG_ERROR, e, "Unable to parse data for object.");
            }
        }
    }
}