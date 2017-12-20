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
package mil.dod.th.ose.config.loading.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.remote.LexiconFormatEnum;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.model.config.EventConfig;
import mil.dod.th.ose.remote.api.RemoteEventAdmin;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

/**
 * @author allenchl
 *
 */
public class TestRemoteEventLoaderImpl
{
    @Mock private RemoteEventAdmin m_RemoteEventAdmin;
    private RemoteEventLoaderImpl m_SUT = new RemoteEventLoaderImpl();
    private LoggingService m_Log;
    private String m_Top1 = "Monkey";
    private String m_Top2 = "Snake";
    private String m_Top3 = "Whale";
    private String m_Top4 = "Lizard.dragon";
    private String m_Filter1 = "(monkey.name='bobo')";
    private String m_Filter2 = "(color=rainbow)";
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        m_Log = LoggingServiceMocker.createMock();
        m_SUT.setRemoteEventAdmin(m_RemoteEventAdmin);
        m_SUT.setLoggingService(m_Log);
    }
    
    /**
     * Verify registering of event config items.
     * Verify proper messages are created and passed to the remote event service.
     */
    @Test
    public void testEventConfigLoading()
    {
        m_SUT.process(createEventConfigs());
        
        //verify
        ArgumentCaptor<EventRegistrationRequestData> messageCap = 
                ArgumentCaptor.forClass(EventRegistrationRequestData.class);
        verify(m_RemoteEventAdmin).addRemoteEventRegistration(eq(1), messageCap.capture());
        verify(m_RemoteEventAdmin).addRemoteEventRegistration(eq(2), messageCap.capture());
        verify(m_RemoteEventAdmin).addRemoteEventRegistration(eq(3), messageCap.capture());
        verify(m_RemoteEventAdmin).addRemoteEventRegistration(eq(4), messageCap.capture());
        
        //verify each captured message
        for (EventRegistrationRequestData message : messageCap.getAllValues())
        {
            //messages were created such that the number of topics is the event configs order
            //config1 has one topic, config2, has two, see createEventConfigs method below
            int numberOfTopics = message.getTopicCount();
            switch (numberOfTopics)
            {
                case 1:
                    assertThat(message.getTopic(0), is(m_Top1));
                    assertThat(message.hasFilter(), is(false));
                    assertThat(message.getCanQueueEvent(), is(true));
                    assertThat(message.getObjectFormat(), is(RemoteTypesGen.LexiconFormat.Enum.XML));
                    assertThat(message.getExpirationTimeHours(), is(10));
                    break;
                case 2:
                    assertThat(message.getTopic(1), is(m_Top2));
                    assertThat(message.getFilter(), is(m_Filter1));
                    assertThat(message.getCanQueueEvent(), is(true));
                    assertThat(message.getObjectFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
                    assertThat(message.getExpirationTimeHours(), is(20));
                    break;
                case 3:
                    assertThat(message.getTopic(2), is(m_Top3));
                    assertThat(message.hasFilter(), is(false));
                    assertThat(message.getCanQueueEvent(), is(false));
                    assertThat(message.getObjectFormat(), is(RemoteTypesGen.LexiconFormat.Enum.NATIVE));
                    assertThat(message.getExpirationTimeHours(), is(30));
                    break;
                case 4:
                    assertThat(message.getTopic(3), is(m_Top4));
                    assertThat(message.getFilter(), is(m_Filter2));
                    assertThat(message.getCanQueueEvent(), is(true));
                    assertThat(message.getObjectFormat(), is(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY));
                    assertThat(message.getExpirationTimeHours(), is(40));
                    break;
                default:
                    fail("unknown event config!");
            }
        }
    }
    
    /**
     * Verify if one config fails to load that others are still loaded. 
     */
    @Test
    public void testExceptionWhileLoading()
    {
        //exception mocking 
        doNothing().doThrow(NullPointerException.class).doNothing().doNothing()
            .when(m_RemoteEventAdmin).addRemoteEventRegistration(anyInt(), 
                Mockito.any(EventRegistrationRequestData.class));
        m_SUT.process(createEventConfigs());
        
        //verify
        verify(m_RemoteEventAdmin, times(4)).addRemoteEventRegistration(anyInt(), 
                Mockito.any(EventRegistrationRequestData.class));
        verify(m_Log).error(Mockito.any(NullPointerException.class), anyString(), anyObject());
    }
    
    /**
     * Create event configs.
     */
    private List<EventConfig> createEventConfigs()
    {
        final List<EventConfig> configs = new ArrayList<>();
        List<String> topics = new ArrayList<>();
        topics.add(m_Top1);
        EventConfig config1 = new EventConfig(topics, null, true, LexiconFormatEnum.XML, 1, 10);
        configs.add(config1);
        
        topics = new ArrayList<>();
        topics.add(m_Top1);
        topics.add(m_Top2);
        EventConfig config2 = new EventConfig(topics, m_Filter1, true, LexiconFormatEnum.NATIVE, 2, 20);
        configs.add(config2);
        
        topics = new ArrayList<>();
        topics.add(m_Top1);
        topics.add(m_Top2);
        topics.add(m_Top3);
        EventConfig config3 = new EventConfig(topics, null, false, LexiconFormatEnum.NATIVE, 3, 30);
        configs.add(config3);
        
        topics = new ArrayList<>();
        topics.add(m_Top1);
        topics.add(m_Top2);
        topics.add(m_Top3);
        topics.add(m_Top4);
        EventConfig config4 = new EventConfig(topics, m_Filter2, true, LexiconFormatEnum.UUID_ONLY, 4, 40);
        configs.add(config4);
        return configs;
    }
}
