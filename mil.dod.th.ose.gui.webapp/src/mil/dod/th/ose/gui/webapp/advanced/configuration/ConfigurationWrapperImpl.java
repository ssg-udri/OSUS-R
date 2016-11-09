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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.inject.Inject;

import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

/**
 * Bean used to retrieve an intermediary model that contains configuration and meta type information for a service with
 * the specified PID. Can also be used to set a configuration property value.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "configWrapper")
@ApplicationScoped
public class ConfigurationWrapperImpl implements ConfigurationWrapper
{
    /**
     * Reference to the system configuration manager bean.
     */
    @ManagedProperty(value = "#{configAdminMgr}")
    private SystemConfigurationMgr configAdminMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Reference to the system meta type manager bean.
     */
    @ManagedProperty(value = "#{metatypeMgr}")
    private SystemMetaTypeMgr metatypeMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Reference to the growl message utility.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
      
    /**
     * Sets the system config manager to be used.
     * @param sysConfigMgr
     *      system config manager to be set.
     */
    public void setConfigAdminMgr(final SystemConfigurationMgr sysConfigMgr)
    {
        this.configAdminMgr = sysConfigMgr;
    }
    
    /**
     * Sets the system metatype manager to be used.
     * @param sysMetatypeMgr
     *      system metatype manager to be set.
     */
    public void setMetatypeMgr(final SystemMetaTypeMgr sysMetatypeMgr)
    {
        this.metatypeMgr = sysMetatypeMgr;
    }
    
    /**
     * Sets the growl message utility to be used.
     * @param growlUtil
     *      {@link GrowlMessageUtil} to be set.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    @Override
    public UnmodifiableConfigMetatypeModel getConfigurationByPidAsync(final int controllerId, final String pid)
    {
        UnmodifiableConfigMetatypeModel configMetaModel = null;
        
        if (!pid.isEmpty())
        {
            final ConfigAdminModel config = this.configAdminMgr.getConfigurationByPidAsync(controllerId, pid);
            final MetaTypeModel metaType = retrieveMetaTypeAsync(controllerId, pid);
            
            if (metaType != null)
            {
                configMetaModel = new UnmodifiableConfigMetatypeModel(pid);
                for (AttributeModel attribute: metaType.getAttributes())
                {
                    final ConfigPropModelImpl propDisplayModel = createConfigPropModel(attribute, config);
                    configMetaModel.getProperties().add(propDisplayModel);
                }
            }
        }
        return configMetaModel;
    }
    

    @Override
    public UnmodifiableConfigMetatypeModel getConfigurationDefaultsByFactoryPidAsync(
            final int controllerId, final String factoryPid)
    {
        UnmodifiableConfigMetatypeModel configMetaModel = null;
        
        final MetaTypeModel metaType = retrieveMetaTypeAsync(controllerId, factoryPid);
        
        if (metaType != null)
        {
            configMetaModel = new UnmodifiableConfigMetatypeModel("");
            for (AttributeModel attribute : metaType.getAttributes())
            {
                final ConfigPropModelImpl propDisplayModel = createConfigPropModel(attribute, null);
                configMetaModel.getProperties().add(propDisplayModel);
            }
        }
        
        return configMetaModel;
    }
    
    
    @Override
    public UnmodifiablePropertyModel getConfigurationPropertyAsync(
            final int controllerId, final String pid, final String key)
    {
        final UnmodifiableConfigMetatypeModel configMetaModel = getConfigurationByPidAsync(controllerId, pid);
        
        if (configMetaModel != null)
        {
            for (UnmodifiablePropertyModel property: configMetaModel.getProperties())
            {
                if (property.getKey().equals(key))
                {
                    return property;
                }
            }
        }
        
        return null;
    }

    @Override
    public void setConfigurationValueAsync(final int controllerId, final String pid, 
            final List<ModifiablePropertyModel> properties)
    {
        final List<ModifiablePropertyModel> changedProperties = 
                findChangedPropertiesAsync(controllerId, pid, properties);
        
        //If nothing has changed then just return.
        if (changedProperties.isEmpty())
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "No Property Altered:", 
                    "Please alter a property before saving.");
            return;
            
        }
        //Send the altered properties.
        this.configAdminMgr.setConfigurationValueAsync(controllerId, pid, changedProperties);
    }
    
    @Override
    public List<ModifiablePropertyModel> findChangedPropertiesAsync(final int controllerId, final String pid, 
            final List<ModifiablePropertyModel> properties)
    {
        final ConfigAdminModel config = this.configAdminMgr.getConfigurationByPidAsync(controllerId, pid);
        final MetaTypeModel metaType = retrieveMetaTypeAsync(controllerId, pid);
        
        final List<ModifiablePropertyModel> changedProperties = new ArrayList<ModifiablePropertyModel>();
        
        ConfigAdminPropertyModel propertyModel = null;
        boolean equalsConfigValue = false;
        boolean equalsMetaValue = false;
        
        for (final ModifiablePropertyModel property: properties)
        {
            if (config != null)
            {
                propertyModel = checkForProperty(config, property.getKey());
                if (propertyModel != null)
                {
                    equalsConfigValue = checkConfigValue(property.getValue(), propertyModel);
                }
            }
            
            if (metaType == null)
            {
                throw new IllegalArgumentException(String.format("No meta type information currently available " 
                        + "for the PID: %s", pid));
            } 
            else
            {
                equalsMetaValue = checkAttributeValue(property.getKey(), property.getValue(), metaType);
            }
            
            if (propertyModel == null && !equalsMetaValue)
            {
                changedProperties.add(property);
            }
            else if (propertyModel != null && !equalsConfigValue)
            {
                changedProperties.add(property);
            }
        }
        
        return changedProperties;
    }
    
    /**
     * Method used to find the corresponding meta type information for the specified PID. First checks if there is meta
     * type information for the specific PID. If none can be found it then checks to see if there is factory meta type
     * information for the PID. May return null if no meta type information exists for the PID.
     * 
     * @param controllerId
     *          ID of the controller where the meta type information is located.
     * @param pid
     *          PID of the service to find meta type information for.
     * @return
     *          {@link MetaTypeModel} that represents the meta type information for the service. May return null if no
     *          meta type information can be found for the 
     */
    private MetaTypeModel retrieveMetaTypeAsync(final int controllerId, final String pid)
    {
        final MetaTypeModel metaType = this.metatypeMgr.getConfigInformationAsync(controllerId, pid);
        if (metaType != null)
        {
            return metaType;
        }
        
        for (MetaTypeModel metaTypeFactory: this.metatypeMgr.getFactoriesListAsync(controllerId))
        {
            if (pid.contains(metaTypeFactory.getPid()))
            {
                return metaTypeFactory;
            }
        }
        return null;
    }
    
    /**
     * Checks the specified configuration model to see if it contain a specific property. If it does then the model
     * that represents that configuration property is returned otherwise null is returned.
     * 
     * @param configModel
     *          Configuration model to be checked for the specified property.
     * @param key
     *          Key of the property to be checked for existence.
     * @return
     *          The model that represents the property if the configuration contains it. If no property is
     *          found then this method returns null.
     */
    private ConfigAdminPropertyModel checkForProperty(final ConfigAdminModel configModel, final String key)
    {
        for (ConfigAdminPropertyModel property: configModel.getProperties())
        {
            if (property.getKey().equals(key))
            {
                return property;
            }
        }
        return null;
    }
    
    /**
     * Method that checks if the configuration property value is equals to the current value set for that configuration
     * property.
     * 
     * @param value
     *          Value to be checked versus the current configuration property value.
     * @param propertyModel
     *          The properties model that represents the property stored in the config admin with which to compare
     *          against the property displayed on the configuration XHTML page.
     * @return
     *          True if the display property value matches the configuration property value. Otherwise false is 
     *          returned.
     */
    private boolean checkConfigValue(final Object value, final ConfigAdminPropertyModel propertyModel)
    {
        if (value.equals(propertyModel.getValue()))
        {
            return true;
        }
        return false;
    }
    
    /**
     * Method that checks if the configuration property value is equal to the meta type default value.
     * 
     * @param key
     *          Key of the property to be checked.
     * @param value
     *          Value to be checked.
     * @param metaTypeModel
     *          The meta type model that represents the meta type information currently stored by the meta type service
     *          for the specified configuration.
     * @return
     *          True if the property model value matches the meta type default value. Otherwise false is returned.
     */
    private boolean checkAttributeValue(final String key, final Object value, final MetaTypeModel metaTypeModel)
    {
        for (AttributeModel attribute: metaTypeModel.getAttributes())
        {
            if (!attribute.getDefaultValues().isEmpty() 
                    && attribute.getDefaultValues().size() == 1
                    && key.equals(attribute.getId()))
            {
                final String modelValue = value.toString();
                final String defaultValue = attribute.getDefaultValues().get(0);
                if (modelValue.equals(defaultValue))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Method that creates a configuration property display model.
     * 
     * @param attribute
     *          {@link AttributeModel} that contains meta information for the property.
     * @param config
     *          {@link ConfigAdminModel} that contains the configuration.
     * @return
     *          {@link ConfigPropModelImpl} that represents the property.
     */
    private ConfigPropModelImpl createConfigPropModel(final AttributeModel attribute, final ConfigAdminModel config)
    {
        final ConfigPropModelImpl propDisplayModel = new ConfigPropModelImpl(attribute);
        if (config != null)
        {
            for (ConfigAdminPropertyModel property: config.getProperties())
            {
                if (property.getKey().equals(attribute.getId()))
                {
                    propDisplayModel.setValue(property.getValue());
                }
            }
        }
        return propDisplayModel;
    }
}
