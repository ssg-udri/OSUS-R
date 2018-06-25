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
package mil.dod.th.ose.gui.webapp.controller;

import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import org.glassfish.osgicdi.OSGiService;

/**
 * Holds information for the 'active' selected controller. 
 * @author callen
 *
 */
@ManagedBean(name = "activeController")
@SessionScoped
public class ActiveControllerImpl implements ActiveController
{    
    /**
     * TD: Replace ControllerModel with just SystemId of active controller.
     * TD cont'd: This will prevent the models from getting out of sync.
     * The controller model to use as the active controller.
     */
    private ControllerModel m_Controller;
    
    /**
     * Controller manager service. Used to verify the controller is valid.
     */
    @ManagedProperty(value = "#{controllerManager}")
    private ControllerMgr controllerManager; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * EventAdmin for handling OSGI Events.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * Set the growl message utility service.
     * @param growlUtil
     *      the growl message utility service to use.
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    /**
     * Set the ControllerMgr service to use.
     * @param controllerMgr
     *    the controller manager service to use
     */
    public void setControllerManager(final ControllerMgr controllerMgr)
    {
        controllerManager = controllerMgr;
    }
    
    /**
     * Sets the event admin service to use.
     * @param eventAdmin
     *      the event admin service to be set.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    @Override
    public void setActiveController(final ControllerModel model)
    {
        
        //do not update active controller if both are null
        if (m_Controller == null && model == null)
        {
            return;
        }
        
        // if model has changed to null state or ready state, post controller changed event
        //must check if model is null first to prevent null pointer exception with model.isReady() call
        if (model == null || model.isReady())
        {
            final Map<String, Object> map =  new HashMap<>();   
            if (model != null)
            {
                map.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, model.getId());
            }            
            
            m_Controller = model;
            final Event activeControllerChanged = new Event(TOPIC_ACTIVE_CONTROLLER_CHANGED, map);
            m_EventAdmin.postEvent(activeControllerChanged);
        }
    }
    
    @Override
    public ControllerModel getActiveController()
    {
        //if there is no model set, set the first ready one
        if (m_Controller == null)
        {
            final ControllerModel readyController = findReadyController();
            if (readyController != null)
            {
                setActiveController(readyController);
            }            
        }
        else
        {
            //if there is a model set make sure that it is the most up to date model
            final ControllerModel mostRecentModel = controllerManager.getController(m_Controller.getId());
            
            updateActiveController(mostRecentModel);
            
            //if the model returns null, notify user
            if (m_Controller == null)
            {
                m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "No active controller", 
                        "All controller channels have been removed. Add a channel to access a controller.");
            }
        }
        return m_Controller;
    }

    @Override
    public boolean isActiveControllerSet()
    {
        return getActiveController() != null;
    }
    
    /**
     * Out of the list of known controllers, return the first one that is
     * in a ready state.  Otherwise, return <code>null</code>.
     * @return
     *      the first controller in a ready state, otherwise return <code>null</code>.
     */
    private ControllerModel findReadyController()
    {
        ControllerModel readyController = null;
        for (ControllerModel model : controllerManager.getAllControllers())
        {            
            if (model.isReady())
            {
                readyController = model;
                break;
            }
        }
        return readyController;
    }
    
    /**
     * Updates current active controller if given model is in a ready state.
     * Otherwise the active controller is set to the first ready controller.
     * @param updatedModel
     *      Most up-to-date model of the current active controller
     */
    private void updateActiveController(final ControllerModel updatedModel)
    {
        //update to most recent model if is is ready
        if (updatedModel != null && updatedModel.isReady())
        {
            m_Controller = updatedModel; //update current model without posting event
        }
        else //model is null or is not ready
        {
            setActiveController(findReadyController()); //set active controller to a ready controller, if available 
        }
    }
}
