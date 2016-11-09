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

import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import mil.dod.th.ose.gui.webapp.controller.history.ControllerHistory;

/**
 * Class that maintains the selected controller within the controller history list.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "controllerHistHelper")
@SessionScoped
public class ControllerHistoryHelper
{
    @ManagedProperty(value = "#{controllerHistMgr}")
    private ControllerHistoryMgr controllerHistMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
 
    private String m_SelectedController;
    
    public void setControllerHistMgr(final ControllerHistoryMgr controllerHistoryMgr)
    {
        controllerHistMgr = controllerHistoryMgr;
    }
    
    public String getSelectedController()
    {
        return m_SelectedController;
    }
    
    public void setSelectedController(final String selectedController)
    {
        m_SelectedController = selectedController;
    }
    
    public Set<String> getControllerHistoryList()
    {
        return controllerHistMgr.getControllerHistory().keySet();
    }
    
    /**
     * Gets the currently selected controller in the controller history list.
     * 
     * @return
     *      Model that represents the needed information to connect to a controller.
     */
    public ControllerHistory getSelectedControllerHistory()
    {
        //If selected controller is null then return localhost as that is always the first controller in the
        //controller history list.
        if (m_SelectedController == null)
        {
            return controllerHistMgr.getControllerHistory().entrySet().iterator().next().getValue();
        }
        return controllerHistMgr.getControllerHistory().get(m_SelectedController);
    }
}
