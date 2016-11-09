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
package mil.dod.th.ose.gui.webapp.advanced;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.BundleMessages;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.BundleMessages.InstallRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.UpdateRequestData;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.primefaces.model.UploadedFile;

import com.google.protobuf.Message;

/**
 * Test class for the {@link BundleImportMgrImpl} class.
 * 
 * @author cweisenborn
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BundleImportMgrImpl.class)
public class TestBundleImportMgrImpl
{
    private BundleImportMgrImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private GrowlMessageUtil m_GrowlUtil;
    private UploadedFile m_File;
    private MessageWrapper m_MessageWrapper;
    
    @Before
    public void setup()
    {
        m_MessageFactory = mock(MessageFactory.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_File = mock(UploadedFile.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        m_SUT = new BundleImportMgrImpl();

        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setFile(m_File);
        
        when(m_MessageFactory.createBundleMessage(Mockito.any(BundleMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
    }
    
    /**
     * Test getting the boolean value for whether the bundle should be started when installed/updated.
     * Verify the boolean value returned is false since that is the default value.
     */
    @Test
    public void testIsStartBundle()
    {
        assertThat(m_SUT.isStartBundle(), is(false));
    }
    
    /**
     * Test setting whether bundle should be started when installed/updated.
     * Verify that the correct boolean value is returned after being set.
     */
    @Test
    public void testSetStartBundle()
    {
        m_SUT.setStartBundle(true);
        assertThat(m_SUT.isStartBundle(), is(true));
    }
    
    /**
     * Test retrieving the file to be uploaded.
     * Verify the correct file is returned.
     */
    @Test
    public void testGetFile()
    {
        assertThat(m_SUT.getFile(), is(m_File));
    }
    
    /**
     * Test installing and updating a bundle.
     * Verify that the appropriate message with correct contents is sent for both installing and updating a bundle.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testHandleImportBundle() throws IOException, Exception
    {
        //Mock all classes needed for the import method.
        BundleConfigurationMgrImpl bundleMgr = mock(BundleConfigurationMgrImpl.class);
        JarInputStream jis = PowerMockito.mock(JarInputStream.class);
        InputStream is = mock(InputStream.class);
        Manifest manifest = mock(Manifest.class);
        Attributes attributes = mock(Attributes.class);
        
        //Mock jar file contents.
        byte[] byteArr = {1, 2, 3, 4};
        when(m_File.getContents()).thenReturn(byteArr);
        when(m_File.getInputstream()).thenReturn(is);
        
        //Mock methods for bundle configuration manager service.
        List<BundleModel> bundleList = new ArrayList<BundleModel>();
        BundleModel testBundle = mock(BundleModel.class);
        bundleList.add(testBundle);
        when(testBundle.getSymbolicName()).thenReturn("Some Name");
        when(testBundle.getBundleId()).thenReturn(Long.valueOf(5));
        m_SUT.setBundleConfigurationMgr(bundleMgr);
        when(bundleMgr.getBundles()).thenReturn(bundleList);
        
        
        //Mock the jar input stream constructor and all methods pertaining to he jar input stream.
        PowerMockito.whenNew(JarInputStream.class).withArguments(Mockito.any(InputStream.class)).thenReturn(jis);
        when(jis.getManifest()).thenReturn(manifest);
        when(manifest.getMainAttributes()).thenReturn(attributes);
        when(attributes.getValue("Bundle-SymbolicName")).thenReturn("Some Name");
        
        //Argument captor to capture the update message.
        ArgumentCaptor<BundleMessages.UpdateRequestData> updateCaptor = 
                ArgumentCaptor.forClass(BundleMessages.UpdateRequestData.class);
        
        //replay first time to verify updating bundle.
        m_SUT.handleImportBundle(1);
        
        //Verify that an update message was sent.
        verify(m_MessageFactory).createBundleMessage(eq(BundleMessageType.UpdateRequest), updateCaptor.capture());
        verify(m_MessageWrapper).queue(eq(1), (ResponseHandler) eq(null));
        
        //Verify the contents of the update message.
        UpdateRequestData updateMsg = updateCaptor.getValue();
        assertThat(updateMsg.getBundleId(), is(Long.valueOf(5)));
        assertThat(updateMsg.getBundleFile().toByteArray(), is(byteArr));
        
        //Remove the bundle from the bundle manager list so that an install request can be tested for.
        bundleList.remove(0);
        when(m_File.getFileName()).thenReturn("File Name");
        
        //replay second time to verify installing bundle.
        m_SUT.handleImportBundle(1);
        
        //Argument captor to capture the install message.
        ArgumentCaptor<BundleMessages.InstallRequestData> installCaptor = 
                ArgumentCaptor.forClass(BundleMessages.InstallRequestData.class);
        
        //Verify that an install request was sent.
        verify(m_MessageFactory).createBundleMessage(eq(BundleMessageType.InstallRequest), installCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(1), (ResponseHandler) eq(null));
        
        //Verify the contents of the install message.
        InstallRequestData installMsg = installCaptor.getValue();
        assertThat(installMsg.getBundleLocation(), is("File Name"));
        assertThat(installMsg.getBundleFile().toByteArray(), is(byteArr));
        
        //Mock causing an IOException when the file being installed/updated is not a jar file.
        when(jis.getManifest()).thenReturn(null);
        m_SUT.handleImportBundle(1);
        
        //Mock IOException when jar input stream is closed.
        doThrow(IOException.class).when(jis).close();
        m_SUT.handleImportBundle(1);
        
        //Mock IOException when creating jar input stream.
        PowerMockito.whenNew(JarInputStream.class).withArguments(Mockito.any(InputStream.class)).
            thenThrow(IOException.class);
        m_SUT.handleImportBundle(1);
        
        //Verify that all three of the possible IOExceptions are caught and handled appropriately when thrown.
        verify(m_GrowlUtil, times(3)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), eq("Bundle Error!"), 
                Mockito.anyString(), Mockito.any(IOException.class));
        
        //Mock no file being selected to install/update.
        m_File = null;
        m_SUT.setFile(m_File);
        m_SUT.handleImportBundle(1);
        
        //Verify that a growl message is appropriately displayed when no bundle is selected to install/update.
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), eq("No file selected!"), 
                Mockito.anyString());
    }
}
