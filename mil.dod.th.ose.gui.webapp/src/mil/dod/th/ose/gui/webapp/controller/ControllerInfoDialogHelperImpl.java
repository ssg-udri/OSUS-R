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

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import mil.dod.th.core.controller.capability.ControllerCapabilities;

/**
 * Implementation of {@link ControllerInfoDialogHelper}.
 * @author nickmarcucci
 *
 */
@ManagedBean (name = "controllerInfoDialogHelper")
@ViewScoped
public class ControllerInfoDialogHelperImpl implements ControllerInfoDialogHelper
{
    /**
     * Controller for which information is being requested.
     */
    private ControllerModel m_InfoController;

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.controller.ControllerInfoDialogHelper#getInfoController()
     */
    @Override
    public ControllerModel getInfoController()
    {
        return m_InfoController;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.controller.ControllerInfoDialogHelper#setInfoController
     * (mil.dod.th.ose.gui.webapp.controller.ControllerModel)
     */
    @Override
    public void setInfoController(final ControllerModel model)
    {
        m_InfoController = model;
    }
    
    @Override
    public ControllerCapabilities getCtlrCaps()
    {
        if (m_InfoController == null)
        {
            return null;
        }
        else
        {
            return m_InfoController.getCapabilities();
        }
    }
}
