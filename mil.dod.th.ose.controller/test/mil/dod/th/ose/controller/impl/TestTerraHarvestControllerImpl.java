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
package mil.dod.th.ose.controller.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestControllerProxy;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.controller.api.ThoseVersionProvider;
import mil.dod.th.ose.test.BundleContextMocker;
import mil.dod.th.ose.test.FileInputStreamMocker;
import mil.dod.th.ose.test.FileOutputStreamMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.utils.FileService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.common.collect.ImmutableMap;

public class TestTerraHarvestControllerImpl
{
    private static final String EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME = 
            "conf" + File.separator + "th.system.properties";
    private TerraHarvestControllerImpl m_SUT;
    private EventAdmin m_EventAdmin;
    private FileService m_FileService;
    private ControllerCapabilities m_MockCaps;
    private XmlUnmarshalService m_UnmarshalService;
    private BundleContext m_Context;
    private ByteArrayOutputStream m_OutputData = new ByteArrayOutputStream();
    private FileInputStream m_FileInputStream;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new TerraHarvestControllerImpl();
        
        m_Context = BundleContextMocker.createBasicMock();
        
        m_EventAdmin = mock(EventAdmin.class);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_FileService = mock(FileService.class);
        m_SUT.setFileService(m_FileService);
        m_UnmarshalService = mock(XmlUnmarshalService.class);
        m_SUT.setXMLUnmarshalService(m_UnmarshalService);
        m_SUT.setLogging(LoggingServiceMocker.createMock());
        
        m_MockCaps = mock(ControllerCapabilities.class);
        URL myUrl = new URL ("File:// myFile ");
        when(m_UnmarshalService.getXmlResource(Mockito.any(Bundle.class), anyString(), anyString())).thenReturn(myUrl);
        when(m_UnmarshalService.getXmlObject(Mockito.any(), eq(myUrl))).thenReturn(m_MockCaps);
        
        when(m_FileService.doesFileExist(Mockito.any(File.class))).thenReturn(true);
        when(m_FileService.createFileOutputStream(Mockito.any(File.class))).thenAnswer(new Answer<FileOutputStream>()
        {
            @Override
            public FileOutputStream answer(InvocationOnMock invocation) throws Throwable
            {
                m_OutputData.reset();
                return FileOutputStreamMocker.mockIt(m_OutputData);
            }
        });
        m_FileInputStream = mock(FileInputStream.class);
        when(m_FileService.createFileInputStream(Mockito.any(File.class))).thenReturn(m_FileInputStream);
        
        m_SUT.activate(m_Context);
    }
    
    /**
     * Verifies the directory is created during activation.
     */
    @Test
    public void testConfDirCreated()
    {
        verify(m_FileService).mkdir(new File("conf"));
    }
    
    /**
     * Verify system properties are read in during activation and properties are set based on contents of file.
     */
    @Test
    public void testLoadProperties() throws Exception
    {
        m_FileInputStream = FileInputStreamMocker.mockIt(
                "id=0xabcdefab\n name=stored-name\n operation.mode=operational");
        when(m_FileService.createFileInputStream(Mockito.any(File.class))).thenReturn(m_FileInputStream);
        
        m_SUT.activate(m_Context);
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        // should be twice, once during setup, once here
        verify(m_FileService, times(2)).createFileInputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        
        assertThat(m_SUT.getName(), is("stored-name"));
        assertThat(m_SUT.getOperationMode(), is(OperationMode.OPERATIONAL_MODE));
        assertThat(m_SUT.getId(), is(0xabcdefab));
        
        // update only one property, make sure other props are still in file
        m_SUT.setName("new-name");
        verify(m_FileService, atLeastOnce()).createFileOutputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        assertThat(new String(m_OutputData.toByteArray()), containsString("name=new-name"));
        assertThat(new String(m_OutputData.toByteArray()), containsString("id=0xabcdefab"));
        assertThat(new String(m_OutputData.toByteArray()), containsString("operation.mode=operational"));
    }
    
    /**
     * Verify decimal ids are allowed
     */
    @Test
    public void testLoadProperties_DecimalId() throws Exception
    {
        m_FileInputStream = FileInputStreamMocker.mockIt("id=123456");
        when(m_FileService.createFileInputStream(Mockito.any(File.class))).thenReturn(m_FileInputStream);
        
        m_SUT.activate(m_Context);
        
        assertThat(m_SUT.getId(), is(123456));
    }
    
    /**
     * Make sure that invalid values in file are set to defaults and that activation doesn't fail.
     */
    @Test
    public void testLoadPropertiesInvalidValues() throws Exception
    {
        m_FileInputStream = FileInputStreamMocker.mockIt(
                "id=blah \n operation.mode=invalid");
        when(m_FileService.createFileInputStream(Mockito.any(File.class))).thenReturn(m_FileInputStream);
        
        m_SUT.activate(m_Context);
        
        assertThat(m_SUT.getName(), is("<undefined>"));
        assertThat(m_SUT.getOperationMode(), is(OperationMode.TEST_MODE));
        assertThat(m_SUT.getId(), is(-1));
    }

    /**
     * Verify the file input stream is not created if file is not found. 
     */
    @Test
    public void testFileNotFound() throws Exception
    {
        // should be once from setUp method before file service was mocked to return false
        verify(m_FileService, times(1)).createFileInputStream(Mockito.any(File.class));
        
        when(m_FileService.doesFileExist(Mockito.any(File.class))).thenReturn(false);
        
        // call activate again with file service mocked to say file does NOT exist
        m_SUT.activate(m_Context);
        
        // make sure still holding at 1 from before
        verify(m_FileService, times(1)).createFileInputStream(Mockito.any(File.class));
    }
    
    /**
     * Verify version info is retrieved from internal service. 
     */
    @Test
    public final void testGetVersion()
    {
        ThoseVersionProvider versionProvider = mock(ThoseVersionProvider.class);
        when(versionProvider.getVersion()).thenReturn("1.0");
        m_SUT.setThoseVersionProvider(versionProvider);
        
        assertThat(m_SUT.getVersion(), is("1.0"));
    }
    
    /**
     * Verify build info is retrieved from internal service. 
     */
    @Test
    public final void testGetBuildInfo()
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put("key", "info");
        ThoseVersionProvider versionProvider = mock(ThoseVersionProvider.class);
        when(versionProvider.getBuildInfo()).thenReturn(props);
        m_SUT.setThoseVersionProvider(versionProvider);
        
        assertThat(m_SUT.getBuildInfo(), hasEntry("key", "info"));
    }
    
    /**
     * Verify the ID can be set and retrieved, that property file contains correct value for ID property.
     */
    @Test
    public final void testSetGetId() throws Exception
    {
        // Set id
        m_SUT.setId(0x342334);

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(m_FileService, atLeastOnce()).createFileOutputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        assertThat(new String(m_OutputData.toByteArray()), containsString("id=0x00342334"));

        // get id
        assertThat(m_SUT.getId(), is(0x342334));
    }
    
    /**
     * Test setting and getting operation mode of a controller. 
     */
    @Test
    public void testSetGetOperationMode() throws Exception
    {
        //set to test mode
        m_SUT.setOperationMode(OperationMode.OPERATIONAL_MODE);
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(m_FileService, atLeastOnce()).createFileOutputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        assertThat(new String(m_OutputData.toByteArray()), containsString("operation.mode=operational"));

        //verify
        assertThat(m_SUT.getOperationMode(), is(OperationMode.OPERATIONAL_MODE));
        
        ArgumentCaptor<Event> changeModeCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(changeModeCaptor.capture());
        
        final Event changeEvent = changeModeCaptor.getValue();
        assertThat(changeEvent.getTopic(), is(TerraHarvestController.TOPIC_CONTROLLER_MODE_CHANGED));
        assertThat(changeEvent.getProperty(TerraHarvestController.EVENT_PROP_SYSTEM_MODE), 
                is((Object)OperationMode.OPERATIONAL_MODE.value()));
    }
    
    /**
     * Verify the name can be set and retrieved, that property file contains correct value for name property.
     */
    @Test
    public final void testSetGetName() throws Exception
    {
        // Set name
        m_SUT.setName("Blah");
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(m_FileService, atLeastOnce()).createFileOutputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        assertThat(new String(m_OutputData.toByteArray()), containsString("name=Blah"));
        
        assertThat(m_SUT.getName(), is("Blah"));
    }

    /**
     * This tests the behavior of the class if a)the th.system.properties file is not found. Verifies default values are
     * set in file if not there.
     */
    @Test
    public void testDefaultId() throws IOException
    {
        assertThat(m_SUT.getId(), is(-1));
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(m_FileService, atLeastOnce()).createFileOutputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        assertThat(new String(m_OutputData.toByteArray()), containsString("id=0xffffffff"));
    }
    
    /**
     * This tests the behavior of the class if a)the th.system.properties file is not found. Verifies default values are
     * set in file if not there.
     */
    @Test
    public void testDefaultName() throws IOException
    {
        assertThat(m_SUT.getName(), is("<undefined>"));
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(m_FileService, atLeastOnce()).createFileOutputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        assertThat(new String(m_OutputData.toByteArray()), containsString("name=<undefined>"));
    }
    
    /**
     * This tests the behavior of the class if a)the th.system.properties file is not found. Verifies default values are
     * set in file if not there.
     */
    @Test
    public void testDefaultMode() throws IOException
    {
        assertThat(m_SUT.getOperationMode(), is(OperationMode.TEST_MODE));
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(m_FileService, atLeastOnce()).createFileOutputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        assertThat(new String(m_OutputData.toByteArray()), containsString("operation.mode=test"));
    }
    
    /**
     * Verify all properties end up in the file when written to.
     */
    @Test
    public final void testSetAll() throws Exception
    {
        // Set name
        m_SUT.setName("Blah");
        m_SUT.setId(0xabcdefab);
        m_SUT.setOperationMode(OperationMode.OPERATIONAL_MODE);
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(m_FileService, atLeastOnce()).createFileOutputStream(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getPath(), is(EXPECTED_TH_SYSTEM_PROPERTIES_FILENAME));
        assertThat(new String(m_OutputData.toByteArray()), containsString("name=Blah"));
        assertThat(new String(m_OutputData.toByteArray()), containsString("id=0xabcdefab"));
        assertThat(new String(m_OutputData.toByteArray()), containsString("operation.mode=operational"));
    }
    
    /**
     * Test to verify that getCapabilities returns unmarshalled capabilities XML for the base capabilities (not the 
     * extended version).
     */
    @Test
    public void testGetBaseCapabilities() throws UnmarshalException, MalformedURLException
    {  
        verify(m_UnmarshalService).getXmlResource(m_Context.getBundle(), FactoryDescriptor.CAPABILITIES_XML_FOLDER_NAME,
                TerraHarvestControllerImpl.class.getName());
        verify(m_UnmarshalService).getXmlObject(eq(ControllerCapabilities.class), Mockito.any(URL.class));

        ControllerCapabilities caps = m_SUT.getCapabilities();
        assertThat(caps, is(m_MockCaps));
    }
    
    /**
     * Test to verify that getCapabilities returns unmarshalled capabilities XML for the extended capabilities if the 
     * proxy service is available.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetProxyCapabilities() throws UnmarshalException, MalformedURLException
    {  
        ServiceReference<TerraHarvestControllerProxy> serviceRef = mock(ServiceReference.class);
        Bundle proxyBundle = mock(Bundle.class);
        TerraHarvestControllerProxy proxy = mock(TerraHarvestControllerProxy.class);
        URL proxyCapUrl = new URL("file:\\proxy-cap.xml");
        ControllerCapabilities proxyCaps = new ControllerCapabilities().withManufacturer("custom");
        
        when(m_Context.getServiceReference(TerraHarvestControllerProxy.class)).thenReturn(serviceRef);
        when(serviceRef.getBundle()).thenReturn(proxyBundle);
        when(m_UnmarshalService.getXmlResource(proxyBundle, FactoryDescriptor.CAPABILITIES_XML_FOLDER_NAME, 
                proxy.getClass().getName()))
            .thenReturn(proxyCapUrl);
        when(m_UnmarshalService.getXmlObject(Mockito.any(), eq(proxyCapUrl))).thenReturn(proxyCaps);
        m_SUT.setProxy(proxy);
        m_SUT.activate(m_Context);
        
        // verify proxy bundle cap URL is used instead
        verify(m_UnmarshalService).getXmlObject(ControllerCapabilities.class, proxyCapUrl);

        ControllerCapabilities caps = m_SUT.getCapabilities();
        assertThat(caps, is(proxyCaps));
    }
    
    /**
     * Verify the proxy method is called for ID field if capability is set and proxy is available
     */
    @Test
    public void testIdProxy() throws FileNotFoundException
    {
        TerraHarvestControllerProxy proxy = mock(TerraHarvestControllerProxy.class);
        // bind proxy, then unbind
        m_SUT.setProxy(proxy);
        m_SUT.setProxy(null);
        
        m_SUT.setId(5);
        m_SUT.getId();
        
        // verify that proxy is never called on since it has been unbound
        verify(proxy, never()).setId(anyInt());
        verify(proxy, never()).getId();
        
        m_SUT.setProxy(proxy);
        
        m_SUT.setId(8);
        m_SUT.getId();
        
        // verify that proxy is never called on since capabilities say not to
        verify(proxy, never()).setId(anyInt());
        verify(proxy, never()).getId();
        
        // make sure if no element if provided (null value), treated as false
        when(m_MockCaps.isIdOverridden()).thenReturn(null);
        
        m_SUT.setId(8);
        m_SUT.getId();
        
        // verify that proxy is never called on since capabilities are at defaults
        verify(proxy, never()).setId(anyInt());
        verify(proxy, never()).getId();
        
        // mock capabilities
        when(m_MockCaps.isIdOverridden()).thenReturn(true);
        
        // just make sure we have a baseline (3 times, set id 3 times here and 3 times during activation)
        verify(m_FileService, times(6)).createFileOutputStream(Mockito.any(File.class));
        
        // mock proxy
        when(proxy.getId()).thenReturn(27);
        
        m_SUT.setId(10);
        
        verify(proxy).setId(10);
        assertThat(m_SUT.getId(), is(27));

        // should still be 6 as the proxy should have been called the last go round
        verify(m_FileService, times(6)).createFileOutputStream(Mockito.any(File.class));
    }
    
    /**
     * Verify the proxy method is called for name filed if capability is set and proxy is available
     */
    @Test
    public void testNameProxy() throws FileNotFoundException
    {
        TerraHarvestControllerProxy proxy = mock(TerraHarvestControllerProxy.class);
        // bind proxy, then unbind
        m_SUT.setProxy(proxy);
        m_SUT.setProxy(null);
        
        m_SUT.setName("blah");
        m_SUT.getName();
        
        // verify that proxy is never called on since it has been unbound
        verify(proxy, never()).setName(anyString());
        verify(proxy, never()).getName();
        
        m_SUT.setProxy(proxy);
        
        m_SUT.setName("something");
        m_SUT.getName();
        
        // verify that proxy is never called on since capabilities say not to
        verify(proxy, never()).setName(anyString());
        verify(proxy, never()).getName();
        
        // make sure if no element if provided (null value), treated as false
        when(m_MockCaps.isNameOverridden()).thenReturn(null);

        m_SUT.setName("something");
        m_SUT.getName();
        
        // verify that proxy is never called on since capabilities are at defaults
        verify(proxy, never()).setName(anyString());
        verify(proxy, never()).getName();

        // mock capabilities
        when(m_MockCaps.isNameOverridden()).thenReturn(true);
        
        // just make sure we have a baseline (6 times, set name 3 times above, 3 times during activation)
        verify(m_FileService, times(6)).createFileOutputStream(Mockito.any(File.class));
        
        // mock proxy
        when(proxy.getName()).thenReturn("proxy-name");
        
        m_SUT.setName("hostname");
        
        verify(proxy).setName("hostname");
        assertThat(m_SUT.getName(), is("proxy-name"));
        
        // should still be 6 as the proxy should have been called the last go round
        verify(m_FileService, times(6)).createFileOutputStream(Mockito.any(File.class));
    }
    
    /**
     * Verify the proxy method is called for version if capability is set and proxy is available
     */
    @Test
    public void testVersionProxy() throws FileNotFoundException
    {
        ThoseVersionProvider versionProvider = mock(ThoseVersionProvider.class);
        when(versionProvider.getVersion()).thenReturn("1.0");
        m_SUT.setThoseVersionProvider(versionProvider);
        
        TerraHarvestControllerProxy proxy = mock(TerraHarvestControllerProxy.class);
        // bind proxy, then unbind
        m_SUT.setProxy(proxy);
        m_SUT.setProxy(null);
        
        m_SUT.getVersion();
        
        // verify that proxy is never called on since it has been unbound
        verify(proxy, never()).getVersion();
        
        m_SUT.setProxy(proxy);
        
        m_SUT.getVersion();
        
        // verify that proxy is never called on since capabilities say not to
        verify(proxy, never()).getVersion();
        
        // make sure if no element if provided (null value), treated as false
        when(m_MockCaps.isVersionOverridden()).thenReturn(null);
        
        m_SUT.getVersion();
        
        // verify that proxy is never called on since capabilities are set to defaults
        verify(proxy, never()).getVersion();
        
        // mock capabilities
        when(m_MockCaps.isVersionOverridden()).thenReturn(true);
        
        // mock proxy
        when(proxy.getVersion()).thenReturn("proxy-version");
        
        assertThat(m_SUT.getVersion(), is("proxy-version"));
    }
    
    /**
     * Verify the proxy method is called for build info if capability is set and proxy is available
     */
    @Test
    public void testBuildInfoProxy() throws FileNotFoundException
    {
        ThoseVersionProvider versionProvider = mock(ThoseVersionProvider.class);
        when(versionProvider.getVersion()).thenReturn("1.0");
        m_SUT.setThoseVersionProvider(versionProvider);
        
        TerraHarvestControllerProxy proxy = mock(TerraHarvestControllerProxy.class);
        // bind proxy, then unbind
        m_SUT.setProxy(proxy);
        m_SUT.setProxy(null);
        
        m_SUT.getBuildInfo();
        
        // verify that proxy is never called on since it has been unbound
        verify(proxy, never()).getBuildInfo();
        
        m_SUT.setProxy(proxy);
        
        m_SUT.getBuildInfo();
        
        // verify that proxy is never called on since capabilities say not to
        verify(proxy, never()).getBuildInfo();
        
        // make sure if no element if provided (null value), treated as false
        when(m_MockCaps.isBuildInfoOverridden()).thenReturn(null);
        
        m_SUT.getBuildInfo();
        
        // verify that proxy is never called on since capabilities are set to defaults
        verify(proxy, never()).getBuildInfo();
        
        // mock capabilities
        when(m_MockCaps.isBuildInfoOverridden()).thenReturn(true);
        
        // mock proxy
        when(proxy.getBuildInfo()).thenReturn(
                ImmutableMap.<String, String>builder().put("proxy-key", "proxy-value").build());
        
        assertThat(m_SUT.getBuildInfo(), hasEntry("proxy-key", "proxy-value"));
    }
}
