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

/**
 * This class stores all the information needed to generate a field in a proto file.
 *
 *@author cweisenborn
 */
public class ProtoField
{
    /**
     * Reference to the parent message the proto field belongs to.
     */
    private final ProtoMessage m_ParentMsg;
    
    /**
     * The name of the field.
     */
    private String m_Name;
    
    /**
     * Enumeration which states the fields class type. 
     */
    private ProtoType m_Type;
    
    /**
     * Enumeration which states whether the field is required, optional, or repeated.
     */
    private Rule m_Rule;
    
    /**
     * Reference to element which represents the type referenced by this field.
     */
    private ProtoElement m_TypeRef;
    
    /**
     * Constructor which accepts the proto message which this field is associated with.
     * 
     * @param message
     *      The proto message this field is associated with.
     */
    public ProtoField(final ProtoMessage message)
    {
        m_ParentMsg = message;
    }
    
    /**
     * Gets the proto message the field is associated with.
     * 
     * @return
     *      The proto message model the proto field is associated with.
     */
    public ProtoMessage getProtoMessage()
    {
        return m_ParentMsg;
    }
    
    /**
     * Gets the currently set name for the field.
     * 
     * @return 
     *      The name of the field.
     */
    public String getName()
    {
        return m_Name;
    }

    /**
     * Sets the name for the field.
     * 
     * @param name 
     *      The name to set.
     */
    public void setName(final String name)
    {
        this.m_Name = name;
    }
    
    /**
     * Gets the enumeration that represents the fields type.
     * 
     * @return 
     *      Field type.
     */
    public ProtoType getType()
    {
        return m_Type;
    }

    /**
     * Sets the fields type.
     * 
     * @param type 
     *      The {@link ProtoType} enumeration that represents the fields type.
     */
    public void setType(final ProtoType type)
    {
        m_Type = type;
    }
    
    /**
     * Gets the enumeration that represents the protocol buffer rule type for the field.
     * 
     * @return 
     *      The rule type for the field.
     */
    public Rule getRule()
    {
        return m_Rule;
    }

    /**
     * Sets the fields protocol buffer rule type.
     * 
     * @param rule 
     *      The {@link Rule} enumeration that represents the fields protocol buffer rule type.
     */
    public void setRule(final Rule rule)
    {
        this.m_Rule = rule;
    }
    
    /**
     * If the field is of the type {@link ProtoType#Reference} then this method will return the {@link ProtoElement}
     * that represents the type referenced by this field. Otherwise, this method will return null.
     * 
     * @return
     *      The type referenced by the field or null if the field is a not of the type {@link ProtoType#Reference}.
     */
    public ProtoElement getTypeRef()
    {
        return m_TypeRef;
    }

    /**
     * Sets the type referenced by the field.
     * 
     * @param typeref
     *      The {@link ProtoElement} that represents the type referenced by the field.
     */
    public void setTypeRef(final ProtoElement typeref)
    {
        m_TypeRef = typeref;
    }
    
    /**
     * Enumeration that holds all the possible types for a {@link ProtoField}.
     */
    public enum ProtoType
    {
        /**
         * This field maps directly to the Protocol Buffer String field type.
         */
        String,
        
        /**
         * This field maps directly to the Protocol Buffer Int32 field type.
         */
        Int32,
        
        /**
         * This field maps directly to the Protocol Buffer Int64 field type.
         */
        Int64,
        
        /**
         * This field maps directly to the Protocol Buffer UInt32 field type. 
         */
        UInt32,
        
        /**
         * This field maps directly to the Protocol Buffer UInt64 field type.
         */
        UInt64,
        
        /**
         * This field maps directly to the Protocol Buffer Byte field type.
         */
        Bytes,
        
        /**
         * This field type is used to flag a field as being a reference to another Protocol Buffer message or
         * enum so that the proto file generator knows how to handle it appropriately.
         */
        Reference,
        
        /**
         * This field maps directly to the Protocol Buffer Enum field type.
         */
        Enum,
        
        /**
         * This field maps directly to the Protocol Buffer Double field type.
         */
        Double,
        
        /**
         * This field maps directly to the Protocol Buffer Bool field type.
         */
        Boolean,
        
        /**
         * This field maps directly to the Protocol Buffer Float field type.
         */
        Float
    }
    
    /**
     * Enumeration that holds all possible rule types for a proto field.
     */
    public enum Rule
    {
        /**
         * This field type is used to flag a field as being repeatable. This means it may show up in a Protocol 
         * Buffer message multiple times.
         */
        Repeated,
        
        /**
         * This field type is used to flag a field as being optional. This means it does not have to show up in the 
         * Protocol Buffer message but may.
         */
        Optional,
        
        /**
         * This field type is used to flag a field as being required. This means it must show up within the given
         * Protocol Buffer message but may not show up more than once.
         */
        Required
    } 
}
