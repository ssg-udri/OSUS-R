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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationRequestData;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.glassfish.osgicdi.OSGiService;

/**
 * Get the config prop model setters from the selected config display model, to be used for the dialog to create a 
 * new factory configuration.
 * @author matt
 *
 */
@ManagedBean(name = "createFactoryConfig")
@ViewScoped
public class CreateFactoryConfigurationImpl implements CreateFactoryConfiguration
{
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;

    /**
     * List of configuration properties that will be edited to create a new factory confiugration.
     */
    private List<ModifiablePropertyModel> m_PropertiesList;
    
    /**
     * PID of the factory to create the configuration for.
     */
    private String m_FactoryPid;
    
    /**
     * ID of the controller containing the factory for which the configuration will be created.
     */
    private int m_ControllerId;
    
    /**
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *          MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    @Override
    public List<ModifiablePropertyModel> getPropertiesList()
    {
        return m_PropertiesList;
    }
    
    @Override
    public void setPropertiesList(final UnmodifiableConfigMetatypeModel config, 
            final String pid, final int controllerId)
    {
        m_PropertiesList = new ArrayList<ModifiablePropertyModel>();
    
        for (UnmodifiablePropertyModel model : config.getProperties())
        {
            final ModifiablePropertyModel cpyModel = new ConfigPropModelImpl((ConfigPropModelImpl)model);
            m_PropertiesList.add(cpyModel);
        }
    
        m_FactoryPid = pid;
        m_ControllerId = controllerId;
    }
    
    @Override
    public void createConfiguration()
    {
        final Dictionary<String, Object> updatedConfigs = new Hashtable<String, Object>();
        
        for (UnmodifiablePropertyModel model: m_PropertiesList)
        {
            updatedConfigs.put(model.getKey(), model.getValue());
        }
        
        final CreateFactoryConfigurationRequestData createFactoryConfig = CreateFactoryConfigurationRequestData.
                newBuilder().
                addAllFactoryProperty(SharedMessageUtils.convertDictionarytoMap(updatedConfigs)).
                setFactoryPid(m_FactoryPid).
                build();
  
        m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.CreateFactoryConfigurationRequest, 
                createFactoryConfig).queue(m_ControllerId, null);
    }
}
