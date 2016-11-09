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
package mil.dod.th.ose.remote.osgi;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.BundleMessages.BundleErrorCode;
import mil.dod.th.core.remote.proto.BundleMessages.BundleInfoType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespaceErrorData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundlesResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.InstallRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.InstallResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.StartRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.StopRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.UninstallRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.UpdateRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

/**
 * Test that the BundleManagementMessageService correctly handles messages, and that the expected responses are sent.
 * @author callen
 *
 */
public class TestBundleManagementMessageService 
{
    private BundleManagementMessageService m_SUT;
    private MessageFactory m_MessageFactory;
    private EventAdmin m_EventAdmin;
    private BundleContext m_Context;
    private LoggingService m_Logging;
    private MessageRouterInternal m_MessageRouter;
    private MessageResponseWrapper m_ResponseWrapper;
    
    @Before
    public void setUp() throws Exception
    {
        //initialization of members
        m_SUT = new BundleManagementMessageService();
        m_EventAdmin = mock(EventAdmin.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_Context = mock(BundleContext.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_MessageRouter = mock(MessageRouterInternal.class);
        
        //set services
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setMessageRouter(m_MessageRouter);
        
        when(m_MessageFactory.createBundleResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(BundleMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
        
        //activate the component
        m_SUT.activate(m_Context);
    }
    
    /**
     * Verify message service is registered on activation and unregistered on deactivation.
     */
    @Test
    public void testActivateDeactivate()
    {
        // verify service is bound
        verify(m_MessageRouter).bindMessageService(m_SUT);
        
        m_SUT.deactivate();
        
        // verify service is unbound
        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }

    /**
     * Test getting the namespace.
     * 
     * Verify the namespace is Bundle.
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.Bundle));
    }
    
    /**
     * Test that the message service posts events in response to messages received.
     * 
     * Verify generic handling of a message.
     * 
     * Verify events are correctly posted for all response message types.
     */
    @Test
    public void testGenericHandleMessage() throws IOException
    {
        //construct a single bundle namespace message to verify handling of bundle message service
        BundleNamespace namespaceMessage = BundleNamespace.newBuilder().
            setType(BundleMessageType.StartResponse).build();
      
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Bundle).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        TerraHarvestMessage message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Bundle, 100, 
                namespaceMessage);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(namespaceMessage.getType().toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespaceMessage));
        
        //construct a single bundle namespace message to verify handling of bundle message service
        namespaceMessage = BundleNamespace.newBuilder().setType(BundleMessageType.GetBundlesResponse).
            build();
        payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Bundle).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Bundle, 100, namespaceMessage);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(namespaceMessage.getType().toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespaceMessage));
        
        //construct a single bundle namespace message to verify handling of bundle message service
        namespaceMessage = BundleNamespace.newBuilder().setType(BundleMessageType.GetBundlesResponse).build();
        payload = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Bundle).
                setNamespaceMessage(namespaceMessage.toByteString()).
                build();
        message = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Bundle, 100, namespaceMessage);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(namespaceMessage.getType().toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(namespaceMessage));
    }
    
    /**
     * Test the start bundle request.
     * 
     * Verify that in the event of a successful start that the appropriate response is sent.
     */
    @Test
    public void testStartBundle() throws IOException, BundleException
    {
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        
        // mock behavior
        when(m_Context.getBundle(1L)).thenReturn(bundleA);
        
        //request
        StartRequestData request = StartRequestData.newBuilder().setBundleId(1L).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.StartRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.StartRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        verify(m_MessageFactory).createBundleResponseMessage(message, BundleMessageType.StartResponse, null);
        verify(m_ResponseWrapper).queue(channel);
        verify(bundleA).start();
        
        // verify event is posted for start bundle request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
                toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(BundleMessageType.StartRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(getNamespaceMessage(message)));
    }
    
    /**
     * Test the start bundle request.
     * 
     * Verify that in the event that an exception occurs while trying to start the bundle, that the appropriate 
     * response is sent.
     */
    @Test
    public void testStartBundleBad() throws IOException, BundleException
    {
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        
        //request
        StartRequestData request = StartRequestData.newBuilder().setBundleId(9L).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.StartRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.StartRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.INVALID_VALUE), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
        
        //try again mocking exception
        when(m_Context.getBundle(1L)).thenReturn(bundleA);
        request = StartRequestData.newBuilder().setBundleId(1L).build();
        payload = createPayload(request, BundleMessageType.StartRequest);
        message = createTerraHarvestMessage(request, BundleMessageType.StartRequest);
        
        //mock exception
        doThrow(new BundleException("Whammy!")).when(bundleA).start();
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<BundleNamespaceErrorData> responseCaptor = ArgumentCaptor.forClass(
            BundleNamespaceErrorData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.BundleNamespaceError),
            responseCaptor.capture());
        verify(m_ResponseWrapper, times(2)).queue(channel);
        BundleNamespaceErrorData error = responseCaptor.getValue();
        assertThat(error.getError(), is(BundleErrorCode.OSGiBundleException));
        verify(bundleA).start();
    }
    
    /**
     * Test stop bundle request.
     * 
     * Verify that the correct response is sent.
     */
    @Test
    public void testStopBundle() throws IOException, BundleException
    {
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        
        // mock behavior
        when(m_Context.getBundle(1L)).thenReturn(bundleA);
        
        //request
        StopRequestData request = StopRequestData.newBuilder().setBundleId(1L).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.StopRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.StopRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        verify(m_MessageFactory).createBundleResponseMessage(message, BundleMessageType.StopResponse, null);
        verify(m_ResponseWrapper).queue(channel);
        verify(bundleA).stop();
        
        // verify event is posted for stop bundle request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.StopRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
    }
    
    /**
     * Test stop bundle request with exception and null bundle.
     * 
     * Verify that the correct responses are sent.
     */
    @Test
    public void testStopBundleBad() throws BundleException, IOException
    {
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        
        // mock behavior
        when(m_Context.getBundle(1L)).thenReturn(bundleA);
        
        //request
        StopRequestData request = StopRequestData.newBuilder().setBundleId(5L).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.StopRequest);

        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.StopRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify, bundle should of been null
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.INVALID_VALUE), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
        
        //try again mocking exception
        when(m_Context.getBundle(1L)).thenReturn(bundleA);
        request = StopRequestData.newBuilder().setBundleId(1L).build();
        payload = createPayload(request, BundleMessageType.StopRequest);

        message = createTerraHarvestMessage(request, BundleMessageType.StopRequest);
        
        //mock exception
        doThrow(new BundleException("Whammy!")).when(bundleA).stop();
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<BundleNamespaceErrorData> responseCaptor = ArgumentCaptor.forClass(
            BundleNamespaceErrorData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.BundleNamespaceError),
            responseCaptor.capture());
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        verify(bundleA).stop();
        BundleNamespaceErrorData error = responseCaptor.getValue();
        assertThat(error.getError(), is(BundleErrorCode.OSGiBundleException));
    }
    
    /**
     * Test get bundles request.
     * 
     * Verify that the appropriate response is sent if there are no bundles, or if there are multiples.
     */
    @Test
    public void testGetBundles() throws IOException
    {
        //mock behavior
        when(m_Context.getBundles()).thenReturn(new Bundle[] {});

        //test that there are no bundles and the an empty list is returned
        TerraHarvestPayload payload = createPayload(null, BundleMessageType.GetBundlesRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(null, BundleMessageType.GetBundlesRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<GetBundlesResponseData> responseCaptor = ArgumentCaptor.forClass(
            GetBundlesResponseData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.GetBundlesResponse), 
            responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        // verify event is posted for the get bundle request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundlesRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //verify empty list
        GetBundlesResponseData responseData = responseCaptor.getValue();
        assertThat(responseData.getBundleIdCount(), is(0));
        
        //send again with bundles
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        Bundle bundleB = mock(Bundle.class);
        when(bundleB.getBundleId()).thenReturn(2L);
        Bundle bundleC = mock(Bundle.class);
        when(bundleC.getBundleId()).thenReturn(3L);
        when(m_Context.getBundles()).thenReturn(new Bundle[] { bundleA, bundleC, bundleB});
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory, times(2)).createBundleResponseMessage(eq(message), 
                eq(BundleMessageType.GetBundlesResponse), responseCaptor.capture());
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        
        //verify non-empty list
        responseData = responseCaptor.getValue();
        assertThat(responseData.getBundleIdCount(), greaterThan(0));
        //verify all ids were returned
        assertThat(responseData.getBundleIdList(), hasItem(1L));
        assertThat(responseData.getBundleIdList(), hasItem(2L));
        assertThat(responseData.getBundleIdList(), hasItem(3L));
    }
    
    /**
     * Test BundleInfo Request with no id specified returns all known bundles.
     */
    @Test
    public void testGetBundleInfoForMultipleBundles() throws Exception
    {
        //initialize the fake bundles
        setFakeBundles();
        
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().
                setBundleDescription(true).
                setBundleLastModified(true).
                setBundleLocation(true).
                setBundleName(true).
                setBundleSymbolicName(true).
                setBundleState(true).
                setBundleVendor(true).
                setBundleVersion(true).
                setPackageExports(true).
                setPackageImports(true).build();
        
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.GetBundleInfoRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.GetBundleInfoRequest);
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<GetBundleInfoResponseData> responseCaptor = ArgumentCaptor.forClass(
            GetBundleInfoResponseData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.GetBundleInfoResponse), 
            responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        // verify event is posted for get bundle info request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundleInfoRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //verify data sent back as expected
        GetBundleInfoResponseData responseData = responseCaptor.getValue();
        
        assertThat(responseData.getInfoDataCount(), is(3));
        
        for (BundleInfoType type : responseData.getInfoDataList())
        {
            assertThat(type.hasBundleId(), is(true));
            
            assertThat(type.getBundleId(), anyOf(equalTo(1L), equalTo(2L), equalTo(3L)));
            
            if (type.getBundleId() == 1L)
            {
                assertThat(type.getBundleDescription(), is("Like a fine wine."));
                assertThat(type.getBundleLastModified(), is(23L));
                assertThat(type.getBundleLocation(), is("mil.dod.th.when.is.dinner.served"));
                assertThat(type.getBundleName(), is("Who's asking?"));
                assertThat(type.getBundleState(), is(Bundle.ACTIVE));
                assertThat(type.getBundleSymbolicName(), is("Symbolically speaking, bundle"));
                assertThat(type.getBundleVendor(), is("Spaghetti"));
                assertThat(type.getBundleVersion(), is("1.9876"));
                assertThat(type.getPackageExportCount(), is(2));
                assertThat(type.getPackageExportList(), hasItem("com.giraffes.waltz"));
                assertThat(type.getPackageImportCount(), is(2));
                assertThat(type.getPackageImportList(), hasItem("com.elephants.dance"));
            }
            else if (type.getBundleId() == 2L)
            {
                assertThat(type.getBundleDescription(), is("Like a airplane."));
                assertThat(type.getBundleLastModified(), is(42L));
                assertThat(type.getBundleLocation(), is("mil.dod.th.soaring.hawks.2013"));
                assertThat(type.getBundleName(), is("I'm asking?"));
                assertThat(type.getBundleState(), is(Bundle.INSTALLED));
                assertThat(type.getBundleSymbolicName(), is("symbolic name here!"));
                assertThat(type.getBundleVendor(), is("Louie"));
                assertThat(type.getBundleVersion(), is("2.0023"));
                assertThat(type.getPackageExportCount(), is(3));
                assertThat(type.getPackageExportList(), hasItem("com.soaring.hawks"));
                assertThat(type.getPackageImportCount(), is(3));
                assertThat(type.getPackageImportList(), hasItem("com.what.about.it"));
            }
            else if (type.getBundleId() == 3L)
            {
                assertThat(type.getBundleDescription(), is("Like a boat."));
                assertThat(type.getBundleLastModified(), is(22L));
                assertThat(type.getBundleLocation(), is("mil.dod.th.location"));
                assertThat(type.getBundleName(), is("My name"));
                assertThat(type.getBundleState(), is(Bundle.RESOLVED));
                assertThat(type.getBundleSymbolicName(), is("so symbolic man"));
                assertThat(type.getBundleVendor(), is("Peter"));
                assertThat(type.getBundleVersion(), is("1.0003"));
                assertThat(type.getPackageExportCount(), is(0));
                assertThat(type.getPackageImportCount(), is(1));
                assertThat(type.getPackageImportList(), hasItem("com.i.just.want.a.soda"));
            }               
        }
    }
    
    /**
     * Test BundleInfo Request with no id specified returns all known bundles.
     */
    @Test
    public void testGetBundleInfoForMultipleBundlesWithNoRequestedData() throws Exception
    {
        //initialize the fake bundles
        setFakeBundles();
        
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().
                setBundleDescription(false).
                setBundleLastModified(false).
                setBundleLocation(false).
                setBundleName(false).
                setBundleSymbolicName(false).
                setBundleState(false).
                setBundleVendor(false).
                setBundleVersion(false).
                setPackageExports(false).
                setPackageImports(false).build();
        
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.GetBundleInfoRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.GetBundleInfoRequest);
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<GetBundleInfoResponseData> responseCaptor = ArgumentCaptor.forClass(
            GetBundleInfoResponseData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.GetBundleInfoResponse), 
            responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        // verify event is posted for get bundle info request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundleInfoRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //verify data sent back as expected
        GetBundleInfoResponseData responseData = responseCaptor.getValue();
        
        assertThat(responseData.getInfoDataCount(), is(3));
        
        for (BundleInfoType type : responseData.getInfoDataList())
        {
            assertThat(type.hasBundleId(), is(true));
            
            assertThat(type.getBundleId(), anyOf(equalTo(1L), equalTo(2L), equalTo(3L)));
            
            assertThat(type.hasBundleDescription(), is(false));
            assertThat(type.hasBundleLastModified(), is(false));
            assertThat(type.hasBundleLocation(), is(false));
            assertThat(type.hasBundleName(), is(false));
            assertThat(type.hasBundleState(), is(false));
            assertThat(type.hasBundleSymbolicName(), is(false));
            assertThat(type.hasBundleVendor(), is(false));
            assertThat(type.hasBundleVersion(), is(false));
            assertThat(type.getPackageExportCount(), is(0));
            assertThat(type.getPackageImportCount(), is(0));
        }
    }
    
    /**
     * Test get bundle info request with an id specified. Should only get bundle back.
     * 
     * Verify correct response is sent.
     */
    @Test
    public void testGetBundleInfoForSingleBundle() throws Exception
    {
        //initialize bundles
        setFakeBundles();
        
        //request
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().
            setBundleDescription(true).
            setBundleId(1L).
            setBundleLastModified(true).
            setBundleLocation(true).
            setBundleName(true).
            setBundleSymbolicName(true).
            setBundleState(true).
            setBundleVendor(true).
            setBundleVersion(true).
            setPackageExports(true).
            setPackageImports(true).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.GetBundleInfoRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.GetBundleInfoRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<GetBundleInfoResponseData> responseCaptor = ArgumentCaptor.forClass(
            GetBundleInfoResponseData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.GetBundleInfoResponse), 
            responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        // verify event is posted for get bundle info request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundleInfoRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //verify data sent back as expected
        GetBundleInfoResponseData responseData = responseCaptor.getValue();
        
        assertThat(responseData.getInfoDataCount(), is(1));
        
        BundleInfoType type = responseData.getInfoDataList().get(0);
        
        assertThat(type.getBundleDescription(), is("Like a fine wine."));
        assertThat(type.getBundleLastModified(), is(23L));
        assertThat(type.getBundleLocation(), is("mil.dod.th.when.is.dinner.served"));
        assertThat(type.getBundleName(), is("Who's asking?"));
        assertThat(type.getBundleState(), is(Bundle.ACTIVE));
        assertThat(type.getBundleSymbolicName(), is("Symbolically speaking, bundle"));
        assertThat(type.getBundleVendor(), is("Spaghetti"));
        assertThat(type.getBundleVersion(), is("1.9876"));
        assertThat(type.getPackageExportCount(), is(2));
        assertThat(type.getPackageExportList(), hasItem("com.giraffes.waltz"));
        assertThat(type.getPackageImportCount(), is(2));
        assertThat(type.getPackageImportList(), hasItem("com.elephants.dance"));
        assertThat(type.hasBundleId(), is(true));
        assertThat(type.getBundleId(), is(1L));        
    }
    
    /**
     * Test get bundle info for a bundle that does not exist and one that throws exception.
     * 
     * Verify error messages sent.
     */
    @Test
    public void testGetBundleInfoBad() throws IOException, BundleException
    {
        //request
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().
            setBundleDescription(true).
            setBundleId(1L).
            setBundleLastModified(true).
            setBundleLocation(true).
            setBundleName(true).
            setBundleSymbolicName(true).
            setBundleState(true).
            setBundleVendor(true).
            setBundleVersion(true).
            setPackageExports(true).
            setPackageImports(true).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.GetBundleInfoRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.GetBundleInfoRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.INVALID_VALUE), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test get bundle info request.
     * 
     * Verify correct response is sent if a field is requested, but does not exist.
     */
    @Test
    public void testGetBundleInfoMissingData() throws Exception
    {
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        when(bundleA.getState()).thenReturn(Bundle.ACTIVE);
        when(bundleA.getLastModified()).thenReturn(23L);
        when(bundleA.getLocation()).thenReturn("mil.dod.th.when.is.dinner.served");
        
        Dictionary<String, String> dict = new Hashtable<String, String>();
        when(bundleA.getHeaders()).thenReturn(dict);
        
        // mock context lookup
        when(m_Context.getBundle(1L)).thenReturn(bundleA);
        
        //request
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().
            setBundleDescription(true).
            setBundleId(1L).
            setBundleLastModified(true).
            setBundleLocation(true).
            setBundleName(true).
            setBundleSymbolicName(true).
            setBundleState(true).
            setBundleVendor(true).
            setBundleVersion(true).
            setPackageExports(true).
            setPackageImports(true).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.GetBundleInfoRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.GetBundleInfoRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<GetBundleInfoResponseData> responseCaptor = ArgumentCaptor.forClass(
            GetBundleInfoResponseData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.GetBundleInfoResponse), 
            responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        // verify event is posted for get bundle info request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundleInfoRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //verify data sent back as expected
        GetBundleInfoResponseData responseData = responseCaptor.getValue();
        
        assertThat(responseData.getInfoDataCount(), is(1));
        
        BundleInfoType type = responseData.getInfoData(0);
        
        assertThat(type.getBundleDescription(), is(""));
        assertThat(type.getBundleLastModified(), is(23L));
        assertThat(type.getBundleLocation(), is("mil.dod.th.when.is.dinner.served"));
        assertThat(type.getBundleName(), is(""));
        assertThat(type.getBundleState(), is(Bundle.ACTIVE));
        assertThat(type.getBundleSymbolicName(), is(""));
        assertThat(type.getBundleVendor(), is(""));
        assertThat(type.getBundleVersion(), is(""));
        assertThat(type.getPackageExportCount(), is(0));
        assertThat(type.getPackageImportCount(), is(0));
        assertThat(type.hasBundleId(), is(true));
        assertThat(type.getBundleId(), is(1L));
    }
    
    /**
     * Test get bundle info request.
     * 
     * Verify correct response is sent even if no fields are requested to be returned.
     */
    @Test
    public void testGetBundleNoFieldsRequested() throws Exception
    {
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        
        // mock context lookup
        when(m_Context.getBundle(1L)).thenReturn(bundleA);
        
        //request
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().
            setBundleDescription(false).
            setBundleId(1L).
            setBundleLastModified(false).
            setBundleLocation(false).
            setBundleName(false).
            setBundleSymbolicName(false).
            setBundleState(false).
            setBundleVendor(false).
            setBundleVersion(false).
            setPackageExports(false).
            setPackageImports(false).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.GetBundleInfoRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.GetBundleInfoRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<GetBundleInfoResponseData> responseCaptor = ArgumentCaptor.forClass(
            GetBundleInfoResponseData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.GetBundleInfoResponse), 
            responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        // verify event is posted for get bundle info request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundleInfoRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //verify data sent back as expected
        GetBundleInfoResponseData responseData = responseCaptor.getValue();
        assertThat(responseData.getInfoDataCount(), is(1));
        
        BundleInfoType type = responseData.getInfoData(0);
        assertThat(type.hasBundleDescription(), is(false));
        assertThat(type.hasBundleLastModified(), is(false));
        assertThat(type.hasBundleLocation(), is(false));
        assertThat(type.hasBundleName(), is(false));
        assertThat(type.hasBundleState(), is(false));
        assertThat(type.hasBundleSymbolicName(), is(false));
        assertThat(type.hasBundleVendor(), is(false));
        assertThat(type.hasBundleVersion(), is(false));
        assertThat(type.getPackageExportCount(), is(0));
        assertThat(type.getPackageImportCount(), is(0));
        assertThat(type.hasBundleId(), is(true));
        assertThat(type.getBundleId(), is(1L));
    }
    
    /**
     * Test install bundle request.
     * 
     * Verify that the correct responses are sent.
     */
    @Test
    public void testInstallBundle() throws BundleException, IOException
    {
        //mock bundle
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);

        //mock behavior
        when(m_Context.installBundle(anyString(), Mockito.any(InputStream.class))).thenReturn(bundleA);

        //request
        InstallRequestData request = InstallRequestData.newBuilder().
            setBundleLocation("org.those.x.Bundle").
            setBundleFile(ByteString.copyFrom(new byte[] { 0, 1, 3, 5, 9, (byte)0xff, 0x3f, 0x56, (byte)0xdd, 18})).
            setStartAtInstall(true).
            build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.InstallRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.InstallRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<InstallResponseData> responseCaptor = ArgumentCaptor.forClass(
            InstallResponseData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.InstallResponse), 
            responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        // verify event is posted for install bundle request
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.InstallRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        ArgumentCaptor<InputStream> stream = ArgumentCaptor.forClass(InputStream.class);
        verify(m_Context).installBundle(eq("org.those.x.Bundle"), stream.capture());
        verify(bundleA).start();
        
        byte[] actualBytes = new byte[10];
        stream.getValue().read(actualBytes);
        assertThat(actualBytes, is(new byte[] { 0, 1, 3, 5, 9, (byte)0xff, 0x3f, 0x56, (byte)0xdd, 18}));
        
        //verify bundle id returned
        InstallResponseData responseData = responseCaptor.getValue();
        assertThat(responseData.getBundleId(), is (1L));
        
        when(m_Context.installBundle(anyString(), Mockito.any(InputStream.class))).
            thenThrow(new BundleException("test"));
        
        // handle the message again... expecting exception
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<BundleNamespaceErrorData> responseCaptorError = ArgumentCaptor.forClass(
            BundleNamespaceErrorData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.BundleNamespaceError),
            responseCaptorError.capture());
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        BundleNamespaceErrorData error = responseCaptorError.getValue();
        assertThat(error.getError(), is(BundleErrorCode.OSGiBundleException));
    }

    /**
     * Test update bundle request.
     * 
     * Verify correct response is sent.
     */
    @Test
    public void testUpdateBundle() throws IOException, BundleException
    {
        //mock
        Bundle updateBundle = mock(Bundle.class);
        when(updateBundle.getBundleId()).thenReturn(6L);
        when(m_Context.getBundle(6L)).thenReturn(updateBundle);
        
        //request
        UpdateRequestData request = UpdateRequestData.newBuilder().
            setBundleFile(ByteString.copyFrom(new byte[] { 0, 1, 3, 5, 9, (byte)0xff, 0x3f, 0x56, (byte)0xdd, 18})).
            setBundleId(6L).build();
        
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.UpdateRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.UpdateRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        verify(m_MessageFactory).createBundleResponseMessage(message, BundleMessageType.UpdateResponse, 
            null);
        verify(m_ResponseWrapper).queue(channel);
        ArgumentCaptor<InputStream> stream = ArgumentCaptor.forClass(InputStream.class);
        verify(updateBundle).update(stream.capture());
        
        byte[] actualBytes = new byte[10];
        stream.getValue().read(actualBytes);
        assertThat(actualBytes, is(new byte[] { 0, 1, 3, 5, 9, (byte)0xff, 0x3f, 0x56, (byte)0xdd, 18}));
    }

    /**
     * Test update bundle request with null bundle and exception.
     * 
     * Verify correct responses are sent.
     */
    @Test
    public void testUpdateBundleBad() throws BundleException, IOException
    {
        //mock
        Bundle updateBundle = mock(Bundle.class);
        when(updateBundle.getBundleId()).thenReturn(6L);
        when(m_Context.getBundle(6L)).thenReturn(updateBundle);
        doThrow(new BundleException("High chairs")).when(updateBundle).
            update(Mockito.any(InputStream.class));
        
        //request
        UpdateRequestData request = UpdateRequestData.newBuilder().
            setBundleFile(ByteString.copyFrom(new byte[] { 0, 1, 3, 5, 9, (byte)0xff, 0x3f, 0x56, (byte)0xdd, 18})).
            setBundleId(6L).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.UpdateRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.UpdateRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<BundleNamespaceErrorData> responseCaptorError = ArgumentCaptor.forClass(
            BundleNamespaceErrorData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.BundleNamespaceError),
            responseCaptorError.capture());
        verify(m_ResponseWrapper).queue(channel);
        BundleNamespaceErrorData error = responseCaptorError.getValue();
        assertThat(error.getError(), is(BundleErrorCode.OSGiBundleException));
        
        //try again but this time the bundle won't be found
        request = UpdateRequestData.newBuilder().setBundleId(8L).setBundleFile(ByteString.copyFrom(
            new byte[] { 0, 1, 3, 5, 9, (byte)0xff, 0x3f, 0x56, (byte)0xdd, 18})).build();
        payload = createPayload(request, BundleMessageType.UpdateRequest);
        message = createTerraHarvestMessage(request, BundleMessageType.UpdateRequest);
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.INVALID_VALUE), Mockito.anyString());
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
    }
    
    /**
     * Test uninstall bundle request.
     * 
     * Verify correct responses are sent.
     */
    @Test
    public void testUninstallBundle() throws IOException, BundleException
    {
        //request to uninstall bundle that is not in look up
        UninstallRequestData request = UninstallRequestData.newBuilder().setBundleId(2L).build();
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.UninstallRequest);

        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.UninstallRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.INVALID_VALUE), Mockito.anyString());
        verify(m_ResponseWrapper).queue(channel);
        
        //mock bundle to uninstall
        Bundle removeBundle = mock(Bundle.class);
        when(removeBundle.getBundleId()).thenReturn(7L);
        when(m_Context.getBundle(7L)).thenReturn(removeBundle);
        
        //try send message again
        request = UninstallRequestData.newBuilder().setBundleId(7L).build();
        payload = createPayload(request, BundleMessageType.UninstallRequest);
        message = createTerraHarvestMessage(request, BundleMessageType.UninstallRequest);
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        verify(removeBundle).uninstall();
        verify(m_MessageFactory).createBundleResponseMessage(message, BundleMessageType.UninstallResponse, null);
        //reused channel
        verify(m_ResponseWrapper, times(2)).queue(channel);
        
        //mock exception from bundle while un-installing
        doThrow(new BundleException("The bundle is in a bungle!!")).when(removeBundle).uninstall();
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //verify
        ArgumentCaptor<BundleNamespaceErrorData> responseCaptorError = ArgumentCaptor.forClass(
            BundleNamespaceErrorData.class);
        verify(m_MessageFactory).createBundleResponseMessage(eq(message), eq(BundleMessageType.BundleNamespaceError),
            responseCaptorError.capture());
        //reused channel
        verify(m_ResponseWrapper, times(3)).queue(channel);
        BundleNamespaceErrorData error = responseCaptorError.getValue();
        assertThat(error.getError(), is(BundleErrorCode.OSGiBundleException));
    }
    
    /**
     * Verify the bundle namespace error message sends and event and that the messages in the event are correct.
     */
    @Test
    public void testBundleNamespaceError() throws IOException
    {
        BundleNamespaceErrorData request = BundleNamespaceErrorData.newBuilder().
                setError(BundleErrorCode.OSGiBundleException).
                setErrorDescription("Error").
                build();
        
        TerraHarvestPayload payload = createPayload(request, BundleMessageType.BundleNamespaceError);
        TerraHarvestMessage message = createTerraHarvestMessage(request, BundleMessageType.BundleNamespaceError);
        
        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);
        
        //capture the message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
                toString()));
        
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(BundleMessageType.BundleNamespaceError.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(getNamespaceMessage(message)));
    }
    
    /**
     * Test response data messages that are null.
     * 
     * Verify responses are posted as events.
     */
    @Test
    public void testMessagesWithoutProcessing() throws IOException
    {
        //response to start bundle
        TerraHarvestPayload payload = createPayload(null, BundleMessageType.GetBundleInfoRequest);
        TerraHarvestMessage message = createTerraHarvestMessage(null, BundleMessageType.GetBundleInfoRequest);

        //mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        setFakeBundles();
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted for start bundle response
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundleInfoRequest.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //response to stop bundle
        payload = createPayload(null, BundleMessageType.StopResponse);
        message = createTerraHarvestMessage(null, BundleMessageType.StopResponse);
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted for stop bundle response
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.StopResponse.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //response to get bundle info
        BundleInfoType bundleInfoType = BundleInfoType.newBuilder().
                setBundleState(Bundle.ACTIVE).setBundleId(1L).build();
        
        GetBundleInfoResponseData response = GetBundleInfoResponseData.newBuilder().addInfoData(bundleInfoType).build();
        payload = createPayload(response, BundleMessageType.GetBundleInfoResponse);
        message = createTerraHarvestMessage(response, BundleMessageType.GetBundleInfoResponse);
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted for get bundle info response
        eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundleInfoResponse.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //response to update bundle
        payload = createPayload(null, BundleMessageType.UpdateResponse);
        message = createTerraHarvestMessage(null, BundleMessageType.UpdateResponse);
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted for update
        eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(4)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.UpdateResponse.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //response to uninstall bundle
        payload = createPayload(null, BundleMessageType.UninstallResponse);
        message = createTerraHarvestMessage(null, BundleMessageType.UninstallResponse);
        
        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted for uninstall
        eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(5)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.UninstallResponse.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //request to get bundle names and ids
        BundleInfoType type = BundleInfoType.newBuilder().
            setBundleId(3L).
            setBundleSymbolicName("Shananigans").build();
        GetBundleInfoResponseData response2 = GetBundleInfoResponseData.newBuilder().
            addInfoData(type).build();
        payload = createPayload(response2, BundleMessageType.GetBundleInfoResponse);
        message = createTerraHarvestMessage(response2, BundleMessageType.GetBundleInfoResponse);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted for start bundle request
        eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(6)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.GetBundleInfoResponse.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
        
        //install bundle response
        InstallResponseData installResponse = InstallResponseData.newBuilder().setBundleId(9L).build();
        payload = createPayload(installResponse, BundleMessageType.InstallResponse);
        message = createTerraHarvestMessage(installResponse, BundleMessageType.InstallResponse);

        // handle the message
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted for start bundle request
        eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(7)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID), is(0));
        assertThat((Integer)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DEST_ID), is(1));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE), is(Namespace.Bundle.
            toString()));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(BundleMessageType.InstallResponse.toString()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
        assertThat((BundleNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
            is(getNamespaceMessage(message)));
    }
    
    /**
     * Construct a TerraHarvestMessage wrapping a bundle message.
     * @param bundleMessage
     *     bundle message
     * @param type
     *     the type of the message
     */
    private TerraHarvestMessage createTerraHarvestMessage(final Message bundleMessage, final BundleMessageType type)
    {
        BundleNamespace.Builder namespaceBuilder = BundleNamespace.newBuilder().setType(type);
        if (bundleMessage != null)
        {
            namespaceBuilder.setData(bundleMessage.toByteString());
        }
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Bundle, 100, 
                namespaceBuilder.build());
    }
    
    /**
     * Pull out namespace message for checking events.
     */
    private BundleNamespace getNamespaceMessage(final TerraHarvestMessage terraHarvestMessage) throws IOException
    {
        final TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(terraHarvestMessage.getTerraHarvestPayload());
        return BundleNamespace.parseFrom(payload.getNamespaceMessage());
    }
    
    private TerraHarvestPayload createPayload(final Message bundleMessage, final BundleMessageType type )
    {
        BundleNamespace.Builder namespaceBuilder = BundleNamespace.newBuilder().setType(type);
        if (bundleMessage != null)
        {
            namespaceBuilder.setData(bundleMessage.toByteString());
        }
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.Bundle).
               setNamespaceMessage(namespaceBuilder.build().toByteString()).
               build();
    }
    
    private void setFakeBundles()
    {
        //mock all bundle info possible.
        Dictionary <String, String> dictHeadersA = new Hashtable<String, String>();
        dictHeadersA.put("Bundle-Description", "Like a fine wine.");
        dictHeadersA.put("Bundle-Vendor", "Spaghetti");
        dictHeadersA.put("Bundle-Version", "1.9876");
        dictHeadersA.put("Bundle-Name", "Who's asking?");
        dictHeadersA.put("Export-Package", "com.giraffes.waltz, com.karate.hiyuhh");
        dictHeadersA.put("Import-Package", "com.elephants.dance, com.everyone.say.eh");
        dictHeadersA.put("Bundle-SymbolicName", "Symbolically speaking, bundle");
        
        Bundle bundleA = mock(Bundle.class);
        when(bundleA.getBundleId()).thenReturn(1L);
        when(bundleA.getState()).thenReturn(Bundle.ACTIVE);
        when(bundleA.getLastModified()).thenReturn(23L);
        when(bundleA.getLocation()).thenReturn("mil.dod.th.when.is.dinner.served");
        when(bundleA.getHeaders()).thenReturn(dictHeadersA);
        
        // mock context lookup
        when(m_Context.getBundle(1L)).thenReturn(bundleA);
               
        //mock all bundle info possible.
        Dictionary <String, String> dictHeadersB = new Hashtable<String, String>();
        dictHeadersB.put("Bundle-Description", "Like a airplane.");
        dictHeadersB.put("Bundle-Vendor", "Louie");
        dictHeadersB.put("Bundle-Version", "2.0023");
        dictHeadersB.put("Bundle-Name", "I'm asking?");
        dictHeadersB.put("Export-Package", "com.soaring.hawks, com.hockey.pucks, " +
                "com.drink.a.bevarage;uses=\"com.man.im.thirsty, com.why.arethere.twoofyou\"");
        dictHeadersB.put("Import-Package", "com.what.about.it, " +
                "com.im.tired;version=\"1.0\";uses=\"com.quit.testing\", com.greatest.class.ever");
        dictHeadersB.put("Bundle-SymbolicName", "symbolic name here!");
        
        Bundle bundleB = mock(Bundle.class);
        when(bundleB.getBundleId()).thenReturn(2L);
        when(bundleB.getState()).thenReturn(Bundle.INSTALLED);
        when(bundleB.getLastModified()).thenReturn(42L);
        when(bundleB.getLocation()).thenReturn("mil.dod.th.soaring.hawks.2013");
        when(bundleB.getHeaders()).thenReturn(dictHeadersB);
        
        // mock context lookup
        when(m_Context.getBundle(2L)).thenReturn(bundleB);
        
        
        Dictionary <String, String> dictHeadersC = new Hashtable<String, String>();
        dictHeadersC.put("Bundle-Description", "Like a boat.");
        dictHeadersC.put("Bundle-Vendor", "Peter");
        dictHeadersC.put("Bundle-Version", "1.0003");
        dictHeadersC.put("Bundle-Name", "My name");
        dictHeadersC.put("Export-Package", "");
        dictHeadersC.put("Import-Package", "com.i.just.want.a.soda");
        dictHeadersC.put("Bundle-SymbolicName", "so symbolic man");
        
        Bundle bundleC = mock(Bundle.class);
        when(bundleC.getBundleId()).thenReturn(3L);
        when(bundleC.getState()).thenReturn(Bundle.RESOLVED);
        when(bundleC.getLastModified()).thenReturn(22L);
        when(bundleC.getLocation()).thenReturn("mil.dod.th.location");
        when(bundleC.getHeaders()).thenReturn(dictHeadersC);
        
        Bundle[] bundlesToReturn = new Bundle[3];
        bundlesToReturn[0] = bundleA;
        bundlesToReturn[1] = bundleB;
        bundlesToReturn[2] = bundleC;
        when(m_Context.getBundles()).thenReturn(bundlesToReturn);
    }
}
