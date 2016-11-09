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
package mil.dod.th.ose.gui.webapp.comms;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;

import org.osgi.service.log.LogService;

/**
 * Implementation for the {@link AddCommsDialogHelper} interface. 
 * @author bachmakm
 *
 */
@ManagedBean(name = "addCommsHelper")
@ViewScoped
public class AddCommsDialogHelperImpl implements AddCommsDialogHelper
{  
    /**
     * Model that stores the new comms stack information.
     */
    private CommsStackCreationModel m_Model;

    /**
     * Value which maintains the active index of the accordion panel.
     * Used to reset the active index after requests have been
     * submitted.
     */
    private int m_ActiveIndex;
    
    /**
     * The name of the comms layer for which the Specs button was pressed.
     * Note that this does not mean the layer was actually selected.
     */
    private String m_CurrentSpecsName;
    
    /**
     * CommsLayerTypes service to use.
     */
    @ManagedProperty(value = "#{commsTypesMgr}")
    private CommsLayerTypesMgr commsTypeMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Comms manager service to use.
     */
    @ManagedProperty(value = "#{commsMgr}")
    private CommsMgr commsMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Faces context utility for retrieving the request context.
     */
    @Inject
    private FacesContextUtil m_FacesUtil;
    
    /**
     * Set the comms layer types manager service.
     * @param commsTypesManager
     *      comms types manager service to set
     */
    public void setCommsTypeMgr(final CommsLayerTypesMgr commsTypesManager)
    {
        commsTypeMgr = commsTypesManager;
    }  
    
    /**
     * Set the comms manager service.
     * @param commsManager
     *      comms manager service to set
     */
    public void setCommsMgr(final CommsMgr commsManager)
    {
        commsMgr = commsManager;
    }
    
    /**
     * Set the face context utility service to use.
     * @param facesUtil
     *      the faces context utility service
     */
    public void setFacesContextUtil(final FacesContextUtil facesUtil)
    {
        m_FacesUtil = facesUtil;
    }
    
    /**
     * Initialize the model that stores the new stack information.
     */
    @PostConstruct
    public void setup()
    {
        m_Model = new CommsStackCreationModel();
    }


    @Override
    public void setCapsKey(final String key)
    {
        m_CurrentSpecsName = key;
    }
    
    @Override
    public String getCapsKey()
    {
        return m_CurrentSpecsName;
    }

    @Override
    public int getActiveIndex()
    {
        return m_ActiveIndex;
    }
    
    @Override
    public void setActiveIndex(final int activeIndex)
    {
        m_ActiveIndex = activeIndex;
    }
    
    @Override
    public void clearAllSelectedValues()
    {
        m_Model = new CommsStackCreationModel();
        m_ActiveIndex = 0;
    }
    
    @Override
    public void resetState()
    {
        m_FacesUtil.getRequestContext().reset("addCommsForm");
    }
    
    @Override
    public void validatePositiveTimeout(final FacesContext context, final UIComponent component, final Object value) 
            throws ValidatorException
    {        
        if ((Integer)value < 0)
        {             
            final FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "", "Timeout value must be a positive integer.");            
            throw new ValidatorException(message);
        }
    }

    @Override
    public CommsStackCreationModel getCommsCreationModel()
    {
        return m_Model;
    }
    
    /**
     * Get Description for a Link Layer from the Capabilities document.
     * @param systemId the system id
     * @return
     *      description for a layer.
     */
    public String getLinkLayerDescription(final int systemId)
    {
        return getDescription(systemId, m_Model.getSelectedLinkLayerType());
    }
    
    /**
     * Get Description for a Physical Link Layer from the Capabilities document.
     * @param systemId the system id
     * @return
     *      description for a layer.
     */
    public String getPhysLinkDescription(final int systemId)
    {
        if (m_Model.isForceAdd())
        {
            return getDescription(systemId, 
                    commsTypeMgr.getPhysicalLinkClassByType(systemId, m_Model.getSelectedPhysicalType()));            
        }
        else
        {
            return getDescription(systemId, commsMgr.getPhysicalClazzByName(
                m_Model.getSelectedPhysicalLink(), systemId));
        }
    }

    /**
     * Get Description for a Trans Layer from the Capabilities document.
     * @param systemId the system id
     * @return
     *      description for a layer.
     */
    public String getTransLayerDescription(final int systemId)
    {
        return getDescription(systemId, m_Model.getSelectedTransportLayerType());
    }

    /**
     * Get Description for a Link Layer from the Capabilities document.
     * @param systemId the system id
     * @param className the name of the class to search by.
     * @return
     *      description for a layer.
     */
    private String getDescription(final int systemId, final String className)
    {
        if (className == null)
        {
            return "Select a Layer to see Description.";
        }
        
        final Object caps = commsTypeMgr.getCapabilities(systemId, className);
        if (caps == null)
        {
            Logging.log(LogService.LOG_WARNING, "No Capabilities Document found for Controller 0x%08x and Class %s", 
                        systemId, className);
            return "No Description Found for " + className;
        }
        else if (caps instanceof BaseCapabilities)        
        {
            return ((BaseCapabilities) caps).getDescription();            
        }
        else
        {
            Logging.log(LogService.LOG_ERROR, "Invalid Capabilities object for Controller 0x%08x and Class %s", 
                    systemId, className);
            return "Invalid Capabilities Object";
        }
    }  
}
