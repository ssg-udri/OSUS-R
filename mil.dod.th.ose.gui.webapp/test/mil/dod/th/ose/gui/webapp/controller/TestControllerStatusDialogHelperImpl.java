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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.protobuf.Message;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.BaseMessages;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.SetOperationModeRequestData;

/**
 * Test class for testing the functions within the ControllerStatusDialogHelperImpl class.
 * @author bachmakm
 *
 */
public class TestControllerStatusDialogHelperImpl
{
    private ControllerStatusDialogHelperImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private MessageWrapper m_MessageWrapper;
    private ControllerImage m_ControllerImageInterface;
    
    @Before
    public void setUp()
    {
        //mock services
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        m_ControllerImageInterface = mock(ControllerImage.class);

        //create bean
        m_SUT = new ControllerStatusDialogHelperImpl();
        
        //set services
        m_SUT.setMessageFactory(m_MessageFactory);
        
        when(m_MessageFactory.createBaseMessage(Mockito.any(BaseMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
    }
    
    /**
     * Verify controller and mode are set when using setter.
     * Verify correct controller is returned.
     */
    @Test
    public void testGetSetController()
    {
        ControllerModel model = new ControllerModel(123, m_ControllerImageInterface);
        model.setOperatingMode(OperationMode.TEST_MODE);
        
        m_SUT.setController(model);
        
        assertThat(m_SUT.getController(), is(model));
    }
    
    /**
     * Verify status update message is queued. 
     */
    @Test
    public void testUpdatedSystemStatus()
    {
        ControllerModel model = new ControllerModel(123, m_ControllerImageInterface);        
        
        //verify update to operational mode
        model.setOperatingMode(OperationMode.TEST_MODE);
        m_SUT.setController(model);
        m_SUT.updatedSystemStatus(OperationMode.OPERATIONAL_MODE.toString());
        
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createBaseMessage(eq(BaseMessageType.SetOperationModeRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(123), (ResponseHandler)eq(null));
        
        SetOperationModeRequestData data = (SetOperationModeRequestData)messageCaptor.getValue();
        assertThat(data.getMode(), is(BaseMessages.OperationMode.OPERATIONAL_MODE));
        
        //verify update to test mode
        model.setOperatingMode(OperationMode.OPERATIONAL_MODE);
        m_SUT.setController(model);
        m_SUT.updatedSystemStatus(OperationMode.TEST_MODE.toString());
        
        verify(m_MessageFactory, times(2)).createBaseMessage(eq(BaseMessageType.SetOperationModeRequest),
                messageCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(123), (ResponseHandler)eq(null));
        
        data = (SetOperationModeRequestData)messageCaptor.getValue();
        assertThat(data.getMode(), is(BaseMessages.OperationMode.TEST_MODE));
    }
}
