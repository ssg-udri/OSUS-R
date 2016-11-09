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
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

/**
 * This class is to be used to retrieve and edit modifiable property entries for a given factory object.
 * 
 * @author nickmarcucci
 *
 */
@ManagedBean(name = "factoryObjConfigModifierView")
@ViewScoped
public class FactoryObjectConfigModifierViewImpl implements FactoryObjectConfigModifierView
{
    /**
     * Error message constant for growl messages. This constant will be shown in the 
     * summary of the growl message.
     */
    private static final String ERR_SUMMARY_MSG = "No Factory Model Set";
    
    /**
     * Reference to the configuration wrapper bean.
     */
    @ManagedProperty(value = "#{configWrapper}")
    private ConfigurationWrapper configWrapper; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlMessageUtil;
    
    /**
     * List of properties for the given factory object.
     */
    private final List<ModifiablePropertyModel> m_PropertyList;
    
    /**
     * The factory object that this bean represents.
     */
    private FactoryBaseModel m_FactoryBaseModel;
    
    /**
     * Constructor.
     */
    public FactoryObjectConfigModifierViewImpl()
    {
        m_PropertyList = new ArrayList<ModifiablePropertyModel>();
    }
    
    /**
     * Method that sets the configuration wrapper to use.
     * @param wrapper
     *      configuration wrapper to be set.
     */
    public void setConfigWrapper(final ConfigurationWrapper wrapper)
    {
        this.configWrapper = wrapper;
    }
    
    /**
     * Set the Growl message utility service.
     * @param growlUtil
     *     the growl message utility service to use
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlMessageUtil = growlUtil;
    }
    
    @Override
    public void setSelectedFactoryModel(final FactoryBaseModel model)
    {
        m_FactoryBaseModel = model;
        
        //clear list as this may not be the first time that the view scoped bean has been set.
        //this will enable the request to retrieve the correct properties for the newly set model.
        m_PropertyList.clear();
    }
    
    @Override
    public FactoryBaseModel getSelectedFactoryModel()
    {
        return m_FactoryBaseModel;
    }
    
    @Override
    public List<ModifiablePropertyModel> getProperties()
    {
        if (m_PropertyList.isEmpty())
        {
            setConfigPropertiesAsync();
        }
        
        return m_PropertyList;
    }
    
    @Override
    public void updateAllPropertiesAsync()
    {
        if (m_FactoryBaseModel == null)
        {
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, ERR_SUMMARY_MSG, 
                    "Could not update properties because no factory object has been set.");
            return;
        }
        
        final int controllerId = m_FactoryBaseModel.getControllerId();
        
        if (m_FactoryBaseModel.getPid().isEmpty())
        {
            //find the changed properties
            final List<ModifiablePropertyModel> props = this.configWrapper.findChangedPropertiesAsync(
                    controllerId, m_FactoryBaseModel.getFactoryPid(), m_PropertyList);
            
            //create the configuration
            if (props.isEmpty())
            {
                m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "No Properties Updated", 
                        "No properties were updated so no request sent to the controller.");
            }
            else
            {
                m_FactoryBaseModel.getFactoryManager().createConfiguration(controllerId, 
                        m_FactoryBaseModel, m_PropertyList);
            }
        }
        else
        {
            this.configWrapper.setConfigurationValueAsync(controllerId, m_FactoryBaseModel.getPid(), m_PropertyList);
        }
    }
    
    /**
     * Method used to asynchronously set the properties list for the given factory object.
     */
    private void setConfigPropertiesAsync()
    {
        if (m_FactoryBaseModel == null)
        {
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, ERR_SUMMARY_MSG, 
                    "Could not retrieve configuration properties because no factory object has been set.");
            return;
        }
        
        final int controllerId = m_FactoryBaseModel.getControllerId();
        
        final UnmodifiableConfigMetatypeModel configMetaModel;
        if (m_FactoryBaseModel.getPid().isEmpty())
        {
            configMetaModel = this.configWrapper.getConfigurationDefaultsByFactoryPidAsync(
                    controllerId, m_FactoryBaseModel.getFactoryPid());
        }
        else
        {
            configMetaModel = this.configWrapper.getConfigurationByPidAsync(controllerId, m_FactoryBaseModel.getPid());
        }
        
        //if model is null then clear the list
        if (configMetaModel == null)
        {
            m_PropertyList.clear();
        }
        else
        {
            for (UnmodifiablePropertyModel propModel : configMetaModel.getProperties())
            {
                final ModifiablePropertyModel model = new ConfigPropModelImpl((ConfigPropModelImpl)propModel);
                
                m_PropertyList.add(model);
            }
        }
    }
}
