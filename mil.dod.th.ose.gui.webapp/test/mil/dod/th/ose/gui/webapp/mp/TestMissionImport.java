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
package mil.dod.th.ose.gui.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;

import javax.faces.application.FacesMessage;
import javax.xml.bind.UnmarshalException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

/**
 * Test class for {@link MissionImport}.
 * 
 * @author cweisenborn
 */
public class TestMissionImport
{
    private MissionImport m_SUT;
    private TemplateProgramManager m_TemplateProgramManager;
    private GrowlMessageUtil m_GrowlUtil;
    
    @Before
    public void setup()
    {
        m_TemplateProgramManager = mock(TemplateProgramManager.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_SUT = new MissionImport();
        m_SUT.setTemplateProgramManager(m_TemplateProgramManager);
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
    }
    
    /**
     * Test the handleMissionImport method.
     * Verify that {@link TemplateProgramManager} loadMissionTemplate method is called with the appropriate parameters.
     */
    @Test
    public void testHandleMissionImport() throws IOException, UnmarshalException, IllegalArgumentException, 
        PersistenceFailedException
    {
        UploadedFile testFile = mock(UploadedFile.class);
        FileUploadEvent mockUploadEvent = mock(FileUploadEvent.class);
        
        byte[] testArr = {100, 101, 102, 103, 104 ,105};
        InputStream is = mock(InputStream.class);
        when(testFile.getFileName()).thenReturn("TestMission.xml");
        when(testFile.getContents()).thenReturn(testArr);
        when(testFile.getInputstream()).thenReturn(is);
        
        //return mocked test file when mock upload event is called
        when(mockUploadEvent.getFile()).thenReturn(testFile);
        m_SUT.handleImportMission(mockUploadEvent);
        
        //Verify that load mission template is called with the appropriate template as a parameter.
        verify(m_TemplateProgramManager).loadFromStream(Mockito.any(InputStream.class), eq(false));
        
        //Setup argument captor to capture exceptions passed to the faces messages.
        ArgumentCaptor<Exception> testException = ArgumentCaptor.forClass(Exception.class);
        
        //Setup up exception stack for illegal argument exception.
        IllegalArgumentException argException = new IllegalArgumentException();
        //Set mocked template manager to throw exception when called.
        doThrow(argException).when(m_TemplateProgramManager).loadFromStream(Mockito.any(InputStream.class), 
                eq(false));
        m_SUT.handleImportMission(mockUploadEvent);
        //Verify create faces message called.
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), Mockito.anyString(),
                Mockito.anyString(), testException.capture());
        //Verify exception type is correct.
        assertThat(testException.getValue(), instanceOf(IllegalArgumentException.class));
        
        UnmarshalException unMarshalException = new UnmarshalException("Error");
        doThrow(unMarshalException).when(m_TemplateProgramManager).loadFromStream(Mockito.any(InputStream.class), 
                eq(false));
        m_SUT.handleImportMission(mockUploadEvent);
        verify(m_GrowlUtil, times(2)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), Mockito.anyString(), 
                Mockito.anyString(), testException.capture());
        assertThat(testException.getValue(), instanceOf(UnmarshalException.class));
        
        PersistenceFailedException persistFailException = new PersistenceFailedException();
        doThrow(persistFailException).when(m_TemplateProgramManager).loadFromStream(Mockito.any(InputStream.class), 
                eq(false));
        m_SUT.handleImportMission(mockUploadEvent);
        verify(m_GrowlUtil, times(3)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), Mockito.anyString(), 
                Mockito.anyString(), testException.capture());
        assertThat(testException.getValue(), instanceOf(PersistenceFailedException.class));
    }
    
    /**
     * Test the handleMissionImport method with an IllegalStateException being thrown during loading.
     */
    @Test
    public void testHandleMissionImportIllegalState() throws IOException, UnmarshalException, IllegalArgumentException, 
        PersistenceFailedException
    {
        UploadedFile testFile = mock(UploadedFile.class);
        FileUploadEvent mockUploadEvent = mock(FileUploadEvent.class);
        
        byte[] testArr = {100, 101, 102, 103, 104 ,105};
        InputStream is = mock(InputStream.class);
        when(testFile.getFileName()).thenReturn("TestMission.xml");
        when(testFile.getContents()).thenReturn(testArr);
        when(testFile.getInputstream()).thenReturn(is);
        
        //return mocked test file when mock upload event is called
        when(mockUploadEvent.getFile()).thenReturn(testFile);
        m_SUT.handleImportMission(mockUploadEvent);
        
        //Verify that load mission template is called with the appropriate template as a parameter.
        verify(m_TemplateProgramManager).loadFromStream(Mockito.any(InputStream.class), eq(false));
        
        //Setup argument captor to capture exceptions passed to the faces messages.
        ArgumentCaptor<Exception> testException = ArgumentCaptor.forClass(Exception.class);
        
        //Setup up exception stack for illegal State exception.
        IllegalStateException argException = new IllegalStateException();
        //Set mocked template manager to throw exception when called.
        doThrow(argException).when(m_TemplateProgramManager).loadFromStream(Mockito.any(InputStream.class), 
                eq(false));
        m_SUT.handleImportMission(mockUploadEvent);
        //Verify create faces message called.
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), Mockito.anyString(),
                Mockito.anyString(), testException.capture());
        //Verify exception type is correct.
        assertThat(testException.getValue(), instanceOf(IllegalStateException.class));
    }
}
