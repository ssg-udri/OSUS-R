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
package mil.dod.th.ose.gui.webapp.utils.push;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests PushEventMessage class
 * @author nickmarcucci
 *
 */
public class TestPushEventMessage
{
    private PushEventMessage m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new PushEventMessage("new/event", new HashMap<String, Object>());
    }
    
    /**
     * Verify toString method properly prints out values.
     */
    @Test
    public void testToString()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("prop1", "propval");
        map.put("prop2", "propval2");
        m_SUT.setProperties(map);
        
        String toString = m_SUT.toString();
        
        assertThat(toString, anyOf(is("MessageType: EVENT { "
                + "topic: 'new/event' properties: { 'prop2:propval2' 'prop1:propval' }}"), 
                is("MessageType: EVENT { "
                        + "topic: 'new/event' properties: { 'prop1:propval' 'prop2:propval2' }}")));
        
        m_SUT.setProperties(new HashMap<String, Object>());
        
        toString = m_SUT.toString();
        
        assertThat(toString, is("MessageType: EVENT { topic: 'new/event' properties: { }}"));
    } 
}
