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
package mil.dod.th.ose.shared;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.observation.types.Observation;

import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;

/**
 * @author dhumeniuk
 *
 */
public class TestEventUtils
{
    /**
     * Test method for {@link mil.dod.th.ose.shared.EventUtils#getEventTopic(java.lang.Class, java.lang.String)}.
     */
    @Test
    public final void testGetEventTopic()
    {
        assertThat(EventUtils.getEventTopic(Asset.class, "TEST"), is("mil/dod/th/core/asset/Asset/TEST"));
    }

    @Test
    public final void testGetAllActions()
    {
        assertThat(EventUtils.getEventTopic(Asset.class), is("mil/dod/th/core/asset/Asset/*"));
    }
    
    @Test
    public final void testMatchTopicClass()
    {
        assertThat(EventUtils.topicMatchesClass("blah/di/Blah/ACTION", EventUtils.class), is(false));
        assertThat(EventUtils.topicMatchesClass("mil/dod/th/ose/shared/EventUtils/BLAH", EventUtils.class), is(true));
    }

    /**
     * Test getting the properties of an event in a map.
     */
    @Test
    public void testCreateMap()
    {
        //Observation
        Observation obs = new Observation();
        
        //properties for event
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Prop1", "value1");
        props.put("Prop2", obs);

        //create event
        Event event = new Event("TOPIC", props);

        //create prop map from event
        Map<String, Object> props2 = EventUtils.getEventProps(event);

        //topic will be added to the properties
        props.put(EventConstants.EVENT_TOPIC, "TOPIC");
        assertThat(props2, is(props));
    }
}
