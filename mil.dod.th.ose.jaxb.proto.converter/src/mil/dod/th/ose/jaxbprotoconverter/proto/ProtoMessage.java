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
package mil.dod.th.ose.jaxbprotoconverter.proto;

import java.util.Map;

import mil.dod.th.ose.jaxbprotoconverter.NonReplacingHashMap;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdType;

/**
 * This class is responsible for storing all information needed to generate a protocol buffer message.
 */
public class ProtoMessage extends ProtoElement
{    
    /**
     * {@link NonReplacingHashMap} used to store a string as the key and a {@link ProtoField} for the value. This map
     * stores all the fields within the message.
     */
    private final  Map<String, ProtoField> m_Fields = new NonReplacingHashMap<String, ProtoField>();
    
    /**
     * Reference to an enumeration nested in this message. May be <code>null</code> if no enumeration is nested within 
     * this message.
     */
    private ProtoEnum m_Enum;
    
    /**
     * Reference to the message this proto message extends.
     */
    private ProtoMessage m_BaseMessage;
    
    /**
     * Constructor that accepts the proto file this proto message is contained within.
     * 
     * @param protoFile
     *      The proto file this proto element is contained within.
     * @param messageName
     *      Name of the proto message.
     * @param type
     *      Class type the message represents.
     * @param xsdType
     *      XSD type associated with the message.
     */
    public ProtoMessage(final ProtoFile protoFile, final String messageName, final Class<?> type, 
            final XsdType xsdType)
    {
        super(protoFile, messageName, type, xsdType);
    }
    
    /**
     * Returns the nested enumeration or <code>null</code> if there isn't one.
     * 
     * @return 
     *      The model that represents the nested enumeration or <code>null</code> if no enumeration is associated with 
     *      this message.
     */
    public ProtoEnum getEnumeration()
    {
        return m_Enum;
    }

    /**
     * Sets the nested enumeration for the message.
     * 
     * @param protoEnum
     *      The model that represents the nested enumeration.
     */
    public void setEnumeration(final ProtoEnum protoEnum)
    {
        m_Enum = protoEnum;
    }
    
    /**
     * Returns the base message this message extends or null if this message does not extend another message.
     * 
     * @return
     *      The base message this message extends or null if this message does not extend another message.
     */
    public ProtoMessage getBaseMessage()
    {
        return m_BaseMessage;
    }
    
    /**
     * Set the base message that this message extends.
     * 
     * @param baseMessage
     *      The base message which this message extends.
     */
    public void setBaseMessage(final ProtoMessage baseMessage)
    {
        m_BaseMessage = baseMessage;
    }
    
    /**
     * Gets the map used to store all {@link ProtoField}s within the message.
     * 
     * @return the entries
     */
    public Map<String, ProtoField> getFields()
    {
        return m_Fields;
    }
}
