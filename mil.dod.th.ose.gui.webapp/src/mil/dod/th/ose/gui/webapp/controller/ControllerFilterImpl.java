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

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import mil.dod.th.ose.gui.webapp.channel.ChannelMgr;

/**
 * Implementation of the controller filter interface.
 * @author matt
 */

@ManagedBean(name = "controllerFilter")
@RequestScoped
public class ControllerFilterImpl implements ControllerFilter
{
    /**
     * Filter enum to hold the filter option for controllers.
     */
    private Filter m_Filter = Filter.All;
    
    /**
     * Channel Manager service to get the status of the controllers.
     */
    @ManagedProperty(value = "#{channelManager}")
    private ChannelMgr channelManager; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Controller Manager service to get the list of controllers.
     */
    @ManagedProperty(value = "#{controllerManager}")
    private ControllerMgr controllerManager; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Set the Controller Manager service.
     * @param controllerMgr
     *      the controller manager service to use
     */
    public void setControllerManager(final ControllerMgr controllerMgr)
    {
        controllerManager = controllerMgr;
    }
    
    /**
     * Set the Channel Manager service.
     * @param channelMgr
     *      the channel manager service to use
     */
    public void setChannelManager(final ChannelMgr channelMgr)
    {
        channelManager = channelMgr;
    }
    
    @Override
    public void setFilter(final Filter theFilter)
    {
        m_Filter = theFilter;
    }
    
    @Override
    public Filter getFilter()
    {
        return m_Filter;
    }
    
    
    @Override
    public List<ControllerModel> getFilterList()
    {
        final List<ControllerModel> filteredControllerList = new ArrayList<ControllerModel>();
        
        switch (m_Filter)
        {
            case CommsUp:
                for (ControllerModel controller : controllerManager.getAllControllers())
                {
                    if (channelManager.getStatusForController(controller.getId()).equals(ControllerStatus.Good))
                    {
                        filteredControllerList.add(controller);
                    }
                }
                return filteredControllerList;
            case CommsDown:
                for (ControllerModel controller : controllerManager.getAllControllers())
                {
                    if (channelManager.getStatusForController(controller.getId()).equals(ControllerStatus.Bad))
                    {
                        filteredControllerList.add(controller);
                    }
                }
                return filteredControllerList;
            case Degraded:
                for (ControllerModel controller : controllerManager.getAllControllers())
                {
                    if (channelManager.getStatusForController(controller.getId()).equals(ControllerStatus.Degraded))
                    {
                        filteredControllerList.add(controller);
                    }
                }
                return filteredControllerList;
            default:
                return controllerManager.getAllControllers();
        }
    }
}
