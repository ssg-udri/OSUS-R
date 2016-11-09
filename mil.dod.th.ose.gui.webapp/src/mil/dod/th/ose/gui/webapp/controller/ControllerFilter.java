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

import java.util.List;

/**
 * Holds the selected filter option from the user and returns the filtered list of controllers.
 * @author matt
 */
public interface ControllerFilter
{
    /**
     * Sets the filter option for controllers.
     * 
     * @param theFilter
     *  the string that identifies the currently selected filter option
     */
    void setFilter(Filter theFilter);
    
    /**
     * Get the chosen filter option.
     * @return
     *  the chosen filter option.
     */
    Filter getFilter();
    
    /**
     * Get the list of controllers filtered on the user defined filter option.
     * @return
     *  List of controllers that has been filtered.
     */
    List<ControllerModel> getFilterList();
    
    /** 
     * Filter enum to hold the user selected filter option to filter controllers. 
     */
    enum Filter 
    {
        /** Show all controllers. */
        All, 
        
        /** Show controllers with all comms up. */
        CommsUp,
        
        /** Show controllers with all comms down. */
        CommsDown,
        
        /** Show controllers with some comms up and some comms down. */
        Degraded;
    }
}
