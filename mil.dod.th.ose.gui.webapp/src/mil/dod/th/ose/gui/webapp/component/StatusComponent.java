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
package mil.dod.th.ose.gui.webapp.component;

import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;

import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.gui.webapp.general.StatusCapable;

/**
 * This component assists with the logic needed to render the appropriate style classes for {@link SummaryStatusEnum}s.
 * @author allenchl
 *
 */
@FacesComponent(value = "mil.dod.th.ose.gui.webapp.component.StatusComponent")
public class StatusComponent extends UINamingContainer 
{
    /**
     * Get the appropriate style class for the component's attribute object's status.
     * @return
     *      string representation of the appropriate style to use for the status
     */
    public String getStatusStyle()
    {
        final StatusCapable capable = (StatusCapable)getAttributes().get("statusCapableObject");
        final SummaryStatusEnum status = capable.getSummaryStatus();
        final String statusStyle = "led-";
        if (status == null || status == SummaryStatusEnum.UNKNOWN)
        {
            //this applies the help icon and the associated icon class to this class
            return statusStyle + "UNKNOWN ui-icon-help ui-icon";
        }
        else
        {
            return statusStyle + status;
        }
    }
}
