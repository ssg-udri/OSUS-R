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
import javax.inject.Inject;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.SetOperationModeRequestData;
import mil.dod.th.ose.remote.api.EnumConverter;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of the {@link ControllerStatusDialogHelper} class.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "controllerStatusDialogHelper")
@ViewScoped
public class ControllerStatusDialogHelperImpl implements ControllerStatusDialogHelper
{
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;

    /**
     * Controller for which the status is being requested.
     */
    private ControllerModel m_Controller;
    
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
    public ControllerModel getController()
    {
        return m_Controller;
    }

    @Override
    public void setController(final ControllerModel model)
    {
        m_Controller = model;

    }
        
    @Override
    public void updatedSystemStatus(final String mode)
    {
        final OperationMode newMode = OperationMode.valueOf(mode);
        final BaseMessages.OperationMode protoMode = EnumConverter.convertJavaOperationModeToProto(newMode);       

        final SetOperationModeRequestData setModeRequest = 
                SetOperationModeRequestData.newBuilder().setMode(protoMode).build();

        final int systemId = m_Controller.getId();
        m_MessageFactory.createBaseMessage(BaseMessageType.SetOperationModeRequest, setModeRequest).
            queue(systemId, null);
    }
}
