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
package mil.dod.th.ose.remote.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.remote.RemoteMetatypeConstants;
import mil.dod.th.ose.remote.util.MetatypeInformationListener.MetatypeBundleHandler;
import mil.dod.th.ose.remote.util.MetatypeInformationListener.MetatypeHandler;
import mil.dod.th.ose.shared.OSGiEventConstants;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.MetaTypeProvider;
import org.xml.sax.SAXException;

/**
 * @author callen
 *
 */
public class TestMetatypeInformationListener
{
    private MetatypeInformationListener m_SUT;
    private EventAdmin m_EventAdmin;
    private BundleContext m_Context;
    private MetatypeHandler m_Handler;
    private LoggingService m_Logging;
    private ServiceRegistration<EventHandler> m_Reg;
    private MetatypeBundleHandler m_BundleHandler;
    private MetaTypeXMLParsingService m_XMLParsingService;
    private URL m_TestURL;
    private URL m_TestURL2;
    private Collection<ServiceReference<MetaTypeProvider>> m_ServiceReferences;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException, SAXException, ParserConfigurationException, URISyntaxException,
        InvalidSyntaxException
    {
        //system under test
        m_SUT = new MetatypeInformationListener();

        //mock services that this system relies on
        m_EventAdmin = mock(EventAdmin.class);
        m_Context = mock(BundleContext.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_Reg = mock(ServiceRegistration.class);
        m_XMLParsingService = mock(MetaTypeXMLParsingService.class);
        m_TestURL = new URL("file://file");
        m_TestURL2 = new URL("file://file");

        //set the mock services
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setXMLParsingService(m_XMLParsingService);

        //mock behavior for event listener
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_Reg);

        when(m_Context.getBundles()).thenReturn(new Bundle[]{});

        m_ServiceReferences = new ArrayList<ServiceReference<MetaTypeProvider>>();
        when(m_Context.getServiceReferences(MetaTypeProvider.class, null)).thenReturn(m_ServiceReferences);

        //activate component
        m_SUT.activate(m_Context);

        //capture the value and use to assign value
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_Context, times(2)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        m_Handler = (MetatypeHandler)captor.getAllValues().get(0);
        m_BundleHandler = (MetatypeBundleHandler)captor.getAllValues().get(1);
    }

    /**
     * Test deactivation.
     * Verify the unregistering of event handlers.
     */
    @Test
    public void testDeactivate()
    {
        //deactivate
        m_SUT.deactivate();

        //verify
        verify(m_Reg, times(2)).unregister();
    }

    @Test
    public void testProcessBundlesDuringActivation() throws InterruptedException, IOException, SAXException,
        URISyntaxException
    {
        Bundle bundle1 = mock(Bundle.class);
        when(bundle1.getState()).thenReturn(Bundle.INSTALLED);
        when(bundle1.getBundleId()).thenReturn(1L);
        Bundle bundle2 = mock(Bundle.class);
        when(bundle2.getState()).thenReturn(Bundle.RESOLVED);
        when(bundle2.getBundleId()).thenReturn(2L);
        when(m_Context.getBundles()).thenReturn(new Bundle[]{bundle1, bundle2});
        when(m_Context.getBundle(2L)).thenReturn(bundle2);

        List<URL> urlList = new ArrayList<URL>();
        urlList.add(m_TestURL);
        Enumeration<URL> urls = getfileEnum(urlList);
        when(bundle2.findEntries("OSGI-INF/metatype", "*.xml", true)).thenReturn(urls);

        when(m_XMLParsingService.getPidAttribute(m_TestURL.toURI())).thenReturn("metatype.bundle.SWEETNESS");

        m_SUT.activate(m_Context);

        //wait for thread to finish
        Thread.sleep(100);
        
        //verify event posted with expected data
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        @SuppressWarnings("unchecked")
        List<String> listPids = (List<String>)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_PIDS);
        assertThat(listPids, hasItem("metatype.bundle.SWEETNESS"));
        assertThat((Long)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(2L));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProcessServicesDuringActivation()
    {
        @SuppressWarnings("rawtypes")
        ServiceReference ref = mock(ServiceReference.class);
        Bundle bundle = mock(Bundle.class);
        
        when(ref.getBundle()).thenReturn(bundle);
        when(ref.getProperty(EventConstants.SERVICE_PID)).thenReturn("pid");
        when(bundle.getBundleId()).thenReturn(1L);
        
        m_ServiceReferences.add(ref);
        
        m_SUT.activate(m_Context);

        //verify event posted with expected data
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        List<String> listPids = (List<String>)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_PIDS);
        assertThat(listPids, hasItem("pid"));
        assertThat((Long)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(1L));
    }

    /**
     * Test service registered event when the service is not one that provides metatype information.
     * Verify no event is posted.
     */
    @Test
    public void testHandleUnrelatedServiceRegistration()
    {
        //handle event
        m_Handler.handleEvent(unrelatedEvent());
        //verify that because the registered class is not a managed service nor metatype provider 
        //that no event is posted
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }

    /**
     * Test service registered event when the service is a metatype provider.
     * Verify event is posted with the pid and bundle ID.
     */
    @Test
    public void testHandleMetaTypeProviderServiceRegistration()
    {
        //handle event
        m_Handler.handleEvent(metatypeProviderEvent());
        
        //verify event posted with expected data
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        @SuppressWarnings("unchecked")
        List<String> listPids = (List<String>)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_PIDS);
        assertThat(listPids, hasItem("pid"));
        assertThat((Long)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(1L));
    }

    /**
     * Test service registered event when the service is a managed service.
     * Verify event is posted with the pid and bundle ID.
     */
    @Test
    public void testHandleManagedServiceRegistration()
    {
        //handle event
        m_Handler.handleEvent(managedServiceEvent());
        
        //verify event posted with expected data
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        @SuppressWarnings("unchecked")
        List<String> listPids = (List<String>)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_PIDS);
        assertThat(listPids, hasItem("pid"));
        assertThat((Long)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(1L));
    }

    /**
     * Test a bundle event where the bundle has XML data.
     * Verify event is posted with the pids and bundle ID.
     */
    @Test
    public void testHandleBundleEvent() throws InterruptedException, IOException, SAXException, URISyntaxException
    {
        //mock behavior
        when(m_XMLParsingService.getPidAttribute(m_TestURL.toURI())).thenReturn("metatype.bundle.SWEETNESS");

        //handle event
        m_BundleHandler.handleEvent(xmlServiceEvent());
        
        //wait for thread to finish
        Thread.sleep(100);
        
        //verify event posted with expected data
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        @SuppressWarnings("unchecked")
        List<String> listPids = (List<String>)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_PIDS);
        assertThat(listPids, hasItem("metatype.bundle.SWEETNESS"));
        assertThat((Long)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(1L));
    }

    /**
     * Test a bundle event where the bundle has XML data. And multiple pids.
     * Verify event is posted with the pids and bundle ID.
     */
    @Test
    public void testHandleBundleEventUpdate() throws InterruptedException, IOException, SAXException, URISyntaxException
    {
        //mock behavior
        when(m_XMLParsingService.getPidAttribute(m_TestURL.toURI())).
            thenReturn("metatype.bundle.SWEETNESS", "chocolate.snacks.rule");

        //handle event
        m_BundleHandler.handleEvent(xmlServiceEventUpdate());
        
        //wait for thread to finish
        Thread.sleep(100);
        
        //verify event posted with expected data
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        @SuppressWarnings("unchecked")
        List<String> listPids = (List<String>)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_PIDS);
        assertThat(listPids, hasItems("metatype.bundle.SWEETNESS", "chocolate.snacks.rule"));
        assertThat((Long)captor.getValue().getProperty(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID), is(1L));
    }

    /**
     * Test a bundle event where the bundle does not have XML metadata.
     * Verify NO event is posted.
     */
    @Test
    public void testHandleBundleEventNoMetaTypeData()
    {
        //handle event
        m_BundleHandler.handleEvent(xmlServiceEventNoXMLData());
        
        //verify NO event posted
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }

    /**
     * Test a bundle event where the bundle is adjudicated to have xml data, but the PID is not set.
     */
    @Test
    public void testMissingPID() throws InterruptedException, IOException, SAXException, URISyntaxException
    {
        //mock behavior
        when(m_XMLParsingService.getPidAttribute(m_TestURL.toURI())).thenReturn(null);

        //handle event
        m_BundleHandler.handleEvent(xmlServiceEventUpdate());
        
        //wait for thread to finish
        Thread.sleep(100);
        
        //verify event posted with expected data
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }

    /**
     * Test a bundle event where the bundle is adjudicated to have xml data, but the data cannot be parsed.
     */
    @Test
    public void testIOException() throws InterruptedException, IOException, SAXException, URISyntaxException
    {
        //mock behavior IOException
        when(m_XMLParsingService.getPidAttribute(m_TestURL.toURI())).thenThrow(new IOException());

        //handle event
        m_BundleHandler.handleEvent(xmlServiceEventUpdate());
        
        //wait for thread to finish
        Thread.sleep(100);
        
        //verify event posted with expected data
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }

    /**
     * Test a bundle event where the bundle is adjudicated to have xml data, but the data cannot be parsed.
     */
    @Test
    public void testSAXException() throws InterruptedException, IOException, SAXException, URISyntaxException
    {
        //mock behavior IOException
        when(m_XMLParsingService.getPidAttribute(m_TestURL.toURI())).thenThrow(new SAXException());

        //handle event
        m_BundleHandler.handleEvent(xmlServiceEventUpdate());
        
        //wait for thread to finish
        Thread.sleep(100);
        
        //verify event posted with expected data
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }

    /**
     * Test a bundle event where the bundle is adjudicated to have xml data, but the data cannot be parsed.
     *
     */
    @Test
    public void testURISyntaxException() throws MalformedURLException, InterruptedException
    {
        //handle event
        m_BundleHandler.handleEvent(xmlServiceEventBadURL());
        
        //wait for thread to finish
        Thread.sleep(100);
        
        //verify event posted with expected data
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Mock event that contains a service class that is not one that the component cares about.
     */
    private Event unrelatedEvent()
    {
        //mimic event
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EventConstants.SERVICE_OBJECTCLASS, new String[]{MissionProgramManager.class.getName()});

        //return created event
        return new Event(OSGiEventConstants.TOPIC_SERVICE_REGISTERED, props);
    }

    /**
     * Mock event that contains a metatype provider class. Many service registrations offer more than one service
     * this mocks the provider and another unrelated class.
     */
    private Event metatypeProviderEvent()
    {
        //mock service behavior
        @SuppressWarnings("rawtypes")
        ServiceReference reg = mock(ServiceReference.class);
        Bundle bundle = mock(Bundle.class);
        
        when(reg.getBundle()).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(1L);

        //mimic event
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EventConstants.SERVICE_OBJECTCLASS, 
            new String[]{MetaTypeProvider.class.getName(), MetaTypeProvider.class.getName()});
        props.put(EventConstants.SERVICE, reg);
        props.put(EventConstants.SERVICE_PID, "pid");

        //return created event
        return new Event(OSGiEventConstants.TOPIC_SERVICE_REGISTERED, props);
    }

    /**
     * Mock event that contains a service class that is not one that the component cares about.
     */
    private Event managedServiceEvent()
    {
      //mock service behavior
        @SuppressWarnings("rawtypes")
        ServiceReference reg = mock(ServiceReference.class);
        Bundle bundle = mock(Bundle.class);
        
        when(reg.getBundle()).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(1L);

        //mimic event
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EventConstants.SERVICE_OBJECTCLASS, 
            new String[]{ManagedService.class.getName(), MetaTypeProvider.class.getName()});
        props.put(EventConstants.SERVICE, reg);
        props.put(EventConstants.SERVICE_PID, "pid");

        //return created event
        return new Event(OSGiEventConstants.TOPIC_SERVICE_REGISTERED, props);
    }

    /**
     * Mock a bundle event where the bundle contains XML data.
     */
    private Event xmlServiceEvent()
    {
        //mock service behavior
        Bundle bundle = mock(Bundle.class);
        List<URL> urlList = new ArrayList<URL>();
        urlList.add(m_TestURL);
        Enumeration<URL> urls = getfileEnum(urlList);
        
        //mock behavior
        when(m_Context.getBundle(1L)).thenReturn(bundle);
        when(bundle.findEntries("OSGI-INF/metatype", "*.xml", true)).thenReturn(urls);

        //mimic event information used
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EventConstants.BUNDLE_ID, 1L);

        //return created event
        return new Event(OSGiEventConstants.TOPIC_BUNDLE_RESOLVED, props);
    }

    /**
     * Mock a bundle event where the bundle contains XML data.
     */
    private Event xmlServiceEventUpdate()
    {
        //mock service behavior
        Bundle bundle = mock(Bundle.class);
        List<URL> urlList = new ArrayList<URL>();
        urlList.add(m_TestURL);
        urlList.add(m_TestURL2);
        Enumeration<URL> urls = getfileEnum(urlList);
        
        //mock behavior
        when(m_Context.getBundle(1L)).thenReturn(bundle);
        when(bundle.findEntries("OSGI-INF/metatype", "*.xml", true)).thenReturn(urls);

        //mimic event information used
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EventConstants.BUNDLE_ID, 1L);

        //return created event
        return new Event(OSGiEventConstants.TOPIC_BUNDLE_RESOLVED, props);
    }

    /**
     * Mock a bundle event where the bundle return a bad URL.
     */
    private Event xmlServiceEventBadURL() throws MalformedURLException
    {
        //mock service behavior
        Bundle bundle = mock(Bundle.class);
        List<URL> urlList = new ArrayList<URL>();
        urlList.add(new URL("file:// asdfasdfsadf"));
        Enumeration<URL> urls = getfileEnum(urlList);
        
        //mock behavior
        when(m_Context.getBundle(1L)).thenReturn(bundle);
        when(bundle.findEntries("OSGI-INF/metatype", "*.xml", true)).thenReturn(urls);

        //mimic event information used
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EventConstants.BUNDLE_ID, 1L);

        //return created event
        return new Event(OSGiEventConstants.TOPIC_BUNDLE_RESOLVED, props);
    }
    
    /**
     * Mock a bundle event where the bundle does not contain XML data.
     */
    private Event xmlServiceEventNoXMLData()
    {
        //mock service behavior
        Bundle bundle = mock(Bundle.class);
        @SuppressWarnings("unchecked")
        Enumeration<URL> urls = mock(Enumeration.class);
        
        //mock behavior
        when(m_Context.getBundle(1L)).thenReturn(bundle);
        when(bundle.findEntries("OSGI-INF/metatype", "*.xml", true)).thenReturn(null);
        when(urls.hasMoreElements()).thenReturn(false);

        //mimic event information used
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EventConstants.BUNDLE_ID, 1L);

        //return created event
        return new Event(OSGiEventConstants.TOPIC_BUNDLE_RESOLVED, props);
    }

    /**
     * Framework would return an enumeration of urls, because we actually parse information in the handle event
     * method an enumeration of URLs is returned.
     */
    private Enumeration<URL> getfileEnum(List<URL> urls)
    {
        final Enumeration<URL> fileEnum;
        final Vector<URL> ofFiles = new Vector<URL>();
        ofFiles.addAll(urls);
        fileEnum = ofFiles.elements();
        return fileEnum;
    }
}
