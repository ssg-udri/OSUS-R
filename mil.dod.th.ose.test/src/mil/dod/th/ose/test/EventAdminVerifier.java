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
package mil.dod.th.ose.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;

import org.hamcrest.Matcher;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @author nickmarcucci
 *
 */
public class EventAdminVerifier
{
    public static Event assertEventByTopicOnly(EventAdmin eventAdmin, String topic)
    {
        return assertEventByTopicOnly(eventAdmin, topic, 1).get(0);
    }
    
    public static List<Event> assertEventByTopicOnly(EventAdmin eventAdmin, String topic, int times)
    {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());
        
        return assertEventByTopicOnly(eventCaptor, topic, times);
    }
    
    public static void assertNoEventByTopic(EventAdmin eventAdmin, String topic)
    {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventAdmin, atLeast(0)).postEvent(eventCaptor.capture());
        
        assertNoEventByTopic(eventCaptor, topic);
    }
    
    public static List<Event> assertEventByTopicOnly(ArgumentCaptor<Event> eventCaptor, String topic, int times)
    {
        String topicStr = "";
        List<Event> events = new ArrayList<Event>();
        
        for (Event event : eventCaptor.getAllValues())
        {
            if (event.getTopic().equals(topic))
            {
                events.add(event);
            }
            
            topicStr += event.getTopic() + "\n"; 
        }
        
        if (times != events.size())
        {
            fail(String.format("Topic [%s] not found %d times, but %d times%n%s", 
                    topic, times, events.size(), topicStr));
        }
        return events;
    }

    public static Event assertEventByTopicOnly(ArgumentCaptor<Event> eventCaptor, String topic)
    {
        return assertEventByTopicOnly(eventCaptor, topic, 1).get(0);
    }
    
    /**
     * Create a map where the key is the topic and the value are the event properties.  This can be used to assert 
     * equality and verify event was posted.
     */
    public static void assertEventByTopicAsset(EventAdmin eventAdmin, String topic, Asset asset)
    {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());
        
        String topicStr = "";
        
        for (Event event : eventCaptor.getAllValues())
        {
            if (event.getTopic().equals(topic))
            {
                if (event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ).equals(asset))
                {
                    assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), 
                            is(asset.getName()));
                    assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                            is(asset.getFactory().getProductType()));
                    assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID), 
                            is(asset.getUuid().toString()));
                    assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), 
                            is(asset.getPid()));
                    return;
                }
            }
            
            topicStr += event.getTopic() + ": " + event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ) + "\n"; 
        }
        
        fail("Topic [" + topic + "] not found for asset: " + asset + "\n" + topicStr);
    }

    public static Event assertEventByTopicFactoryObject(ArgumentCaptor<Event> eventCaptor, String topic, 
            FactoryObject obj)
    {
        String topicStr = "";
        
        for (Event event : eventCaptor.getAllValues())
        {
            if (event.getTopic().equals(topic))
            {
                if (event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ).equals(obj))
                {
                    assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE),
                            is(obj.getFactory().getProductType()));
                    return event;
                }
            }
            
            topicStr += event.getTopic() + ": " + event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ) + "\n"; 
        }
        
        fail("Topic [" + topic + "] not found for object: " + obj + "\n" + topicStr);
        return null;
    }

    public static void assertNoEventByTopicAsset(ArgumentCaptor<Event> eventCaptor, String topic, Asset asset)
    {
        for (Event event : eventCaptor.getAllValues())
        {
            if (event.getTopic().equals(topic))
            {
                if (event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ).equals(asset))
                {
                    fail("Topic " + topic + " found for asset: " + asset);
                }
            }
        }
    }

    public static void assertNoEventByTopic(ArgumentCaptor<Event> eventCaptor, String topic)
    {
        for (Event event : eventCaptor.getAllValues())
        {
            if (event.getTopic().equals(topic))
            {
                fail("Topic " + topic + " found");
            }
        }
    }

    public static void assertEventByTopicAssetName(ArgumentCaptor<Event> eventCaptor, String topic, String assetName)
    {
        String topicStr = "";
        
        for (Event event : eventCaptor.getAllValues())
        {
            Asset asset = (Asset)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ);
            if (event.getTopic().equals(topic))
            {
                if (asset.getName().equals(assetName))
                {
                    return;
                }
            }
            
            if (asset != null)
            {
                topicStr += event.getTopic() + ": " + asset.getName() + "\n";
            }
        }
        
        fail("Topic [" + topic + "] not found for asset: " + assetName + "\n" + topicStr);
    }

    public static void assertEventByTopicAssetType(
          ArgumentCaptor<Event> eventCaptor, String topic, String assetTypeName)
    {
        String topicStr = "";
        
        for (Event event : eventCaptor.getAllValues())
        {
            if (event.getTopic().equals(topic))
            {
                assertThat(topic + " is missing asset type property",
                        event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), is(notNullValue()));
                if (event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE).equals(assetTypeName))
                {
                    return;
                }
            }
            
            topicStr += event.getTopic() + ": " + event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE) + "\n"; 
        }
        
        fail("Topic [" + topic + "] not found for asset type: " + assetTypeName + "\n" + topicStr);
    }

    public static void assertEventByTopicNoAssetType(ArgumentCaptor<Event> eventCaptor, String topic)
    {
        String topicStr = "";
        
        for (Event event : eventCaptor.getAllValues())
        {
            if (event.getTopic().equals(topic))
            {
                boolean foundProp = false;
                for (String key : event.getPropertyNames())
                {
                    if (key.equals(FactoryDescriptor.EVENT_PROP_OBJ_TYPE))
                    {
                        foundProp = true;
                        continue;
                    }
                }
                
                if (!foundProp)
                {
                    return; // found topic without type property set
                }
            }
            
            topicStr += event.getTopic() + ": " + event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE) + "\n"; 
        }
        
        fail("Topic [" + topic + "] not found without asset type set\n" + topicStr);
    }
    
    /**
     * Method to assert only the given topics regardless of other posted events. Order of expected topics
     * does not matter but it is expected that topic and properties lists correspond. Only given properties
     * will be checked.
     * @param admin
     *  the event admin to use
     * @param expectedTopics
     *  the expected topic to check for
     * @param expectedProperties
     *  the expected properties that correspond to the given expected topic
     */
    public static void assertOnlyGivenEvents(EventAdmin admin, 
            String expectedTopics, Map<String, Object> expectedProperties)
    {
        List<String> topics = new ArrayList<>();
        topics.add(expectedTopics);
        List<Map<String, Object>> maps = new ArrayList<>();
        maps.add(expectedProperties);
        assertOnlyGivenEvents(admin, topics, maps);
    }
   
    /**
     * Method to assert only the given topics regardless of other posted events. Order of expected topics
     * does not matter but it is expected that topic and properties lists correspond. Only given properties
     * will be checked.
     * @param admin
     *  the event admin to use
     * @param expectedTopics
     *  the expected topics to check for
     * @param expectedProperties
     *  the expected properties that correspond to the given expected topics
     */
    public static void assertOnlyGivenEvents(EventAdmin admin, 
            List<String> expectedTopics, List<Map<String, Object>> expectedProperties)
    {
        ArgumentCaptor<Event> evtCaptor = ArgumentCaptor.forClass(Event.class);
        verify(admin, Mockito.atLeast(expectedTopics.size())).postEvent(evtCaptor.capture());
        
        for (Event event : evtCaptor.getAllValues())
        {
            for (int i = 0; i < expectedTopics.size(); i++)
            {
                if (expectedTopics.get(i).equals(event.getTopic()))
                {
                    verifyGivenProperties(event, expectedTopics.get(i), expectedProperties.get(i));
                    break;
                }
            }
        }
    }
    
    /**
     * Method used to verify last event posted to {@link EventAdmin}.
     * 
     * @param admin
     *      the event admin instance to verify against
     * @param expectedTopic
     *      the expected topic
     * @param expectedProps
     *      the expected properties or null to ignore properties set
     */
    public static void assertLastEvent(EventAdmin admin, String expectedTopic, Map<String, Object> expectedProps)
    {
        ArgumentCaptor<Event> evtCaptor = ArgumentCaptor.forClass(Event.class);
        verify(admin, atLeastOnce()).postEvent(evtCaptor.capture());

        verifyGivenProperties(evtCaptor.getValue(), expectedTopic, expectedProps);
    }
    
    /**
     * Method used to verify last event posted to {@link EventAdmin}. Ignore properties of event.
     * 
     * @param admin
     *      the event admin instance to verify against
     * @param expectedTopic
     *      the expected topics in order of when they will be captured
     */
    public static void assertLastEvent(EventAdmin admin, String expectedTopic)
    {
        assertLastEvent(admin, expectedTopic, null);
    }
    
    /**
     * Method verifies a given event has the given topic and contains at least the given properties.
     * 
     * @param givenEvent
     *      the event that is to be verified
     * @param givenTopic
     *      the expected topic for the given event
     * @param givenProps
     *      the expected properties for the given event
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void verifyGivenProperties(Event givenEvent, String givenTopic, Map<String, Object> givenProps)
    {
        assertThat(givenEvent.getTopic(), is(givenTopic));
        
        if (givenProps == null)
        {
            return;
        }
        
        for (String key : givenProps.keySet())
        {
            if (givenProps.get(key) instanceof Matcher)
            {
                assertThat(givenEvent.getProperty(key), (Matcher)givenProps.get(key));
            }
            else
            {
                assertThat(givenEvent.getProperty(key), is(givenProps.get(key)));
            }
        }
    }
}
