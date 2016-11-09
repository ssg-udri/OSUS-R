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
package mil.dod.th.ose.gui.webapp.utils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.component.layout.LayoutUnit;
import org.primefaces.event.ToggleEvent;

/**
 * This bean persists layout units collapsed value.
 * @author matt
 */
@ManagedBean
@SessionScoped
public class LayoutCollapsed
{
    /**
     * If the layout unit that holds controllers is collapsed or expanded.
     */
    private boolean m_ControllerLayout;
    
    /**
     * If the layout unit that holds the navigation is collapsed or expanded.
     */
    private boolean m_NavigationLayout;
    
    /**
     * Get the controller layout collapsed value.
     * @return
     *  value representing layout collapsed attribute.
     */
    public boolean isControllerCollapsed()
    {
        return m_ControllerLayout;
    }
    
    /**
     * Get the navigation layout is collapsed value.
     * @return
     *  value representing layout collapsed attribute.
     */
    public boolean isNavigationCollapsed()
    {
        return m_NavigationLayout;
    }
    
     /**
     * Handle toggling layout unit states.
     * @param event
     *  event that holds the component with the collapsed property to persist.
     */
    public void handleToggle(final ToggleEvent event)
    {
        final LayoutUnit toggledUnit = (LayoutUnit)event.getComponent();

        if (toggledUnit.getId().equals("controllerLayout"))
        {
            m_ControllerLayout = !m_ControllerLayout;
        }
        else if (toggledUnit.getId().equals("navigationLayout"))
        {
            m_NavigationLayout = !m_NavigationLayout;
        }
    }
}
