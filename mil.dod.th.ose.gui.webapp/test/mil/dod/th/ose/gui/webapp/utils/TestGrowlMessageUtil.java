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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import javax.faces.application.FacesMessage;
import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.gui.webapp.utils.push.PushChannelMessageManager;
import mil.dod.th.ose.gui.webapp.utils.push.PushContextUtil;
import mil.dod.th.ose.gui.webapp.utils.push.PushDataMessage;
import mil.dod.th.ose.gui.webapp.utils.push.PushGrowlMessage;
import mil.dod.th.ose.gui.webapp.utils.push.PushMessageType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.osgi.service.log.LogService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.primefaces.push.PushContext;

/**
 * Test class for {@link GrowlMessageUtil}.
 * 
 * @author cweisenborn
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Logging.class})
public class TestGrowlMessageUtil
{
    public static final String GROWL_STICKY_ID = "growl-sticky";
    public static final String GROWL_TIMED_ID = "growl-timed";
    
    private GrowlMessageUtil m_SUT;
    private PushContext m_PushContext;
    private PushContextUtil m_PushContextUtil;
    private PushChannelMessageManager m_Manager;
    
    @Before
    public void setup() throws Exception
    {
        m_SUT = new GrowlMessageUtil();
        
        m_Manager = mock(PushChannelMessageManager.class);
        
        m_PushContext = mock(PushContext.class);
        
        m_PushContextUtil = mock(PushContextUtil.class);
        
        PowerMockito.mockStatic(Logging.class);
        
        m_SUT.setPushManager(m_Manager);
        
        when(m_PushContextUtil.getPushContext()).thenReturn(m_PushContext);
    }
    
    /**
     * Test creating a push growl message with no exception.
     */
    @Test
    public void testCreateGlobalFacesMessage()
    {
        m_SUT.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Summary", "Details");
        
        ArgumentCaptor<PushDataMessage> msgCaptor = ArgumentCaptor.forClass(PushDataMessage.class);
        
        verify(m_Manager, times(1)).addMessage(msgCaptor.capture());
        
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_INFO, "Details");
        
        PushGrowlMessage pushData = (PushGrowlMessage)msgCaptor.getValue();
        
        assertThat(pushData.getType(), is(PushMessageType.GROWL_MESSAGE.toString()));
        
        assertThat(pushData.getSeverity(), is("info"));
        assertThat(pushData.getSummary(), is("Summary"));
        assertThat(pushData.getDetail(), is("Details"));
        assertThat(pushData.getSticky(), is(false));
    }
    
    /**
     * Test creating a message with the sticky attribute.
     * Verify that the growl message is created appropriately.
     */
    @Test
    public void testGlobalFacesMessageSticky()
    {
        IllegalArgumentException exception =  mock(IllegalArgumentException.class);
        
        m_SUT.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "INFO", "INFO_DETAILS", exception, true);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_INFO, exception,"INFO_DETAILS");
        
        ArgumentCaptor<PushDataMessage> msgCaptor = ArgumentCaptor.forClass(PushDataMessage.class);
        verify(m_Manager).addMessage(msgCaptor.capture());
        
        PushGrowlMessage pushData = (PushGrowlMessage)msgCaptor.getAllValues().get(0);
        
        assertThat(pushData.getType(), is(PushMessageType.GROWL_MESSAGE.toString()));
        
        assertThat(pushData.getSeverity(), is("info"));
        assertThat(pushData.getSummary(), is("INFO"));
        assertThat(pushData.getDetail(), is("INFO_DETAILS"));
        assertThat(pushData.getSticky(), is(true));
    }
    
    /**
     * Test creating a growl message with an exception.
     */
    @Test
    public void testCreateGlobalFacesMessageException()
    {
        IllegalArgumentException exception =  mock(IllegalArgumentException.class);
        
        //Create an growl message with severity of info.
        m_SUT.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "INFO", "INFO_DETAILS", exception);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_INFO, exception,"INFO_DETAILS");
        
      //Create an growl message with severity of warn.
        m_SUT.createGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "WARN", "WARN_DETAILS", exception);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_WARNING, exception,"WARN_DETAILS");
        
        //Create an growl message with severity of error.
        m_SUT.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "ERROR_DETAILS", exception);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_ERROR, exception,"ERROR_DETAILS");
        
        //Create an growl message with severity of fatal.
        m_SUT.createGlobalFacesMessage(FacesMessage.SEVERITY_FATAL, "FATAL", "FATAL_DETAILS", exception);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_ERROR, exception,"FATAL_DETAILS");
        
        //Verify that the growl message with info severity was handled correctly.
        ArgumentCaptor<PushDataMessage> msgCaptor = ArgumentCaptor.forClass(PushDataMessage.class);
        verify(m_Manager, times(4)).addMessage(msgCaptor.capture());
        
        PushGrowlMessage pushData = (PushGrowlMessage)msgCaptor.getAllValues().get(0);
        
        assertThat(pushData.getType(), is(PushMessageType.GROWL_MESSAGE.toString()));
       
        assertThat(pushData.getSeverity(), is("info"));
        assertThat(pushData.getSummary(), is("INFO"));
        assertThat(pushData.getDetail(), is("INFO_DETAILS"));
        assertThat(pushData.getSticky(), is(false));
        
        //Verify that the growl message with warn severity was handled correctly.
        pushData = (PushGrowlMessage)msgCaptor.getAllValues().get(1);
        
        assertThat(pushData.getType(), is(PushMessageType.GROWL_MESSAGE.toString()));
        
        assertThat(pushData.getSeverity(), is("warn"));
        assertThat(pushData.getSummary(), is("WARN"));
        assertThat(pushData.getDetail(), is("WARN_DETAILS"));
        assertThat(pushData.getSticky(), is(true));
        
        //Verify that the growl message with error severity was handled correctly.
        pushData = (PushGrowlMessage)msgCaptor.getAllValues().get(2);
        
        assertThat(pushData.getType(), is(PushMessageType.GROWL_MESSAGE.toString()));
        
        assertThat(pushData.getSeverity(), is("error"));
        assertThat(pushData.getSummary(), is("ERROR"));
        assertThat(pushData.getDetail(), is("ERROR_DETAILS"));
        assertThat(pushData.getSticky(), is(true));
        
        //Verify that the growl message with fatal severity was handled correctly.
        pushData = (PushGrowlMessage)msgCaptor.getAllValues().get(3);
        
        assertThat(pushData.getType(), is(PushMessageType.GROWL_MESSAGE.toString()));
       
        assertThat(pushData.getSeverity(), is("fatal"));
        assertThat(pushData.getSummary(), is("FATAL"));
        assertThat(pushData.getDetail(), is("FATAL_DETAILS"));
        assertThat(pushData.getSticky(), is(true));
    }
    
    /**
     * Test creating a growl message with no exception.
     * Verify that the message is appropriately created.
     */
    @Test
    public void testCreateLocalFacesMessageNoException()
    {
        m_SUT.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "Summary", "Details");
        
        ArgumentCaptor<PushGrowlMessage> msgCaptor = ArgumentCaptor.forClass(PushGrowlMessage.class);
        verify(m_Manager).addMessage(msgCaptor.capture());
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_INFO, "Details");
        
        PushGrowlMessage msg = msgCaptor.getValue();
        assertThat(msg.getSeverity(), is("info"));
        assertThat(msg.getSummary(), is("Summary"));
        assertThat(msg.getDetail(), is("Details"));
    }
    
    /**
     * Test creating a growl message with an exception.
     * Verify that a growl message is made appropriate for each kind of severity level.
     */
    @Test
    public void testCreateLocalFacesMessageException()
    {
        IllegalArgumentException exception =  mock(IllegalArgumentException.class);
        
        //Create an growl message with severity of info.
        m_SUT.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "INFO", "INFO_DETAILS", exception);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_INFO, exception,"INFO_DETAILS");
        
        //Create an growl message with severity of warn.
        m_SUT.createLocalFacesMessage(FacesMessage.SEVERITY_WARN, "WARN", "WARN_DETAILS", exception);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_WARNING, exception,"WARN_DETAILS");
        
        //Create an growl message with severity of error.
        m_SUT.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR", "ERROR_DETAILS", exception);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_ERROR, exception,"ERROR_DETAILS");
        
        //Create an growl message with severity of fatal.
        m_SUT.createLocalFacesMessage(FacesMessage.SEVERITY_FATAL, "FATAL", "FATAL_DETAILS", exception);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_ERROR, exception,"FATAL_DETAILS");
        
        //Verify that the growl message with info severity was handled correctly.
        ArgumentCaptor<PushGrowlMessage> msgCaptor = ArgumentCaptor.forClass(PushGrowlMessage.class);
        verify(m_Manager, times(4)).addMessage(msgCaptor.capture());
        PushGrowlMessage msg = msgCaptor.getAllValues().get(0);
        assertThat(msg.getSeverity(), is("info"));
        assertThat(msg.getSticky(), is(false));
        assertThat(msg.getSummary(), is("INFO"));
        assertThat(msg.getDetail(), is("INFO_DETAILS"));
        
        //Verify that the growl message with warn severity was handled correctly.
        msg = msgCaptor.getAllValues().get(1);
        assertThat(msg.getSeverity(), is("warn"));
        assertThat(msg.getSticky(), is(true));
        assertThat(msg.getSummary(), is("WARN"));
        assertThat(msg.getDetail(), is("WARN_DETAILS"));
        
        //Verify that the growl message with error severity was handled correctly.
        msg = msgCaptor.getAllValues().get(2);
        assertThat(msg.getSeverity(), is("error"));
        assertThat(msg.getSticky(), is(true));
        assertThat(msg.getSummary(), is("ERROR"));
        assertThat(msg.getDetail(), is("ERROR_DETAILS"));
        
        //Verify that the growl message with fatal severity was handled correctly.
        msg = msgCaptor.getAllValues().get(3);
        assertThat(msg.getSeverity(), is("fatal"));
        assertThat(msg.getSticky(), is(true));
        assertThat(msg.getSummary(), is("FATAL"));
        assertThat(msg.getDetail(), is("FATAL_DETAILS"));
    }
    
    /**
     * Test creating a message with the sticky attribute.
     * Verify that the growl message is created appropriately.
     */
    @Test
    public void testCreateLocalFacesMessageSticky()
    {
        IllegalArgumentException exception =  mock(IllegalArgumentException.class);
        
        m_SUT.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "INFO", "INFO_DETAILS", exception, true);
        PowerMockito.verifyStatic();
        Logging.log(LogService.LOG_INFO, exception,"INFO_DETAILS");
        
        ArgumentCaptor<PushGrowlMessage> msgCaptor = ArgumentCaptor.forClass(PushGrowlMessage.class);
        verify(m_Manager).addMessage(msgCaptor.capture());
        
        PushGrowlMessage msg = msgCaptor.getAllValues().get(0);
        assertThat(msg.getSeverity(), is("info"));
        assertThat(msg.getSticky(), is(true));
        assertThat(msg.getSummary(), is("INFO"));
        assertThat(msg.getDetail(), is("INFO_DETAILS"));
    }
}
