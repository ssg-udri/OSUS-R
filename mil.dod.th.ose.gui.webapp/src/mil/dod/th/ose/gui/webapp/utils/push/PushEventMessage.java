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

import java.util.Map;

/**
 *  This class represents an event and its respective properties when it is passed by PrimeFaces Push
 *  to a web browser for altering the state of the page.
 * 
 * @author nickmarcucci
 *
 */
public class PushEventMessage extends PushDataMessage
{
    /**
     * The event topic that this event message represents.
     */
    private String m_Topic; 
    
    /**
     * The properties that are associated with this event.
     */
    private Map<String, Object> m_Props;
    
    /**
     * Constructor.
     * @param topic
     *  the topic that this event message is to represent.
     * @param map
     *  the map that holds the event properties for this message.
     */
    public PushEventMessage(final String topic, final Map<String, Object> map)
    {
        super();
        m_Topic = topic;
        m_Props = map;
        m_Type = PushMessageType.EVENT;
    }
    
    /**
     * Sets the topic that this message represents.
     * @param topic
     *  the event topic that is to be used for this message
     */
    public void setTopic(final String topic)
    {
        m_Topic = topic;
    }
    
    /**
     * Gets the topic that this message represents.
     * @return
     *  the event topic that this message contains
     */
    public String getTopic()
    {
        return m_Topic;
    }
    
    /**
     * Sets the properties that are associated with the given 
     * event topic.
     * @param props
     *  the properties to be set
     */
    public void setProperties(final Map<String, Object> props)
    {
        m_Props = props;
    }
    
    /**
     * Gets the propertied associated with the event that this 
     * message represents.
     * @return
     *  the properties that are associated with the event topic
     */
    public Map<String, Object> getProperties()
    {
        return m_Props;
    }

    @Override
    public void setType(final PushMessageType type)
    {
        //this function is needed by the JSONifier so that object is properly converted
    }

    @Override
    public String getType()
    {
        return m_Type.toString();
    }
    
    @Override
    public String toString()
    {
        final StringBuffer buffer = new StringBuffer(String.format(
                "%s { topic: '%s' properties: { ", printMessageType(), m_Topic));
       
        for (String key : m_Props.keySet())
        {
            buffer.append(String.format("'%s:%s' ", key, m_Props.get(key)));
        }
        
        buffer.append("}}");
        
        return buffer.toString();
    }
    
}
