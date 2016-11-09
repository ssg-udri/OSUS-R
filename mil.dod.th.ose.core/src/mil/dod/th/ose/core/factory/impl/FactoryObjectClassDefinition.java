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
package mil.dod.th.ose.core.factory.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Default implementation of the {@link ObjectClassDefinition} interface for use by implementors of the
 * {@link mil.dod.th.ose.core.factory.api.DirectoryService} abstract class.
 * 
 * @author dlandoll
 */
public class FactoryObjectClassDefinition implements ObjectClassDefinition
{
    /**
     * Name of the OCD.
     */
    private final String m_Name;

    /**
     * OCD identifier.
     */
    private final String m_Id;

    /**
     * OCD description.
     */
    private final String m_Description;

    /**
     * OCD required attribute definitions.
     */
    private final AttributeDefinition[] m_AttributesRequired;
    
    /**
     * OCD optional attribute definitions.
     */
    private final AttributeDefinition[] m_AttributesOptional;

    /**
     * Creates the factory Object Class Definition.
     * 
     * @param name
     *            Name of the OCD
     * @param identifier
     *            Identifier of the OCD
     * @param description
     *            Description of the OCD
     * @param attributesRequired
     *            Required attribute definitions of the OCD
     * @param attributesOptional
     *            Optional attribute definitions of the OCD
     */
    public FactoryObjectClassDefinition(final String name, final String identifier, final String description,
            final AttributeDefinition[] attributesRequired, final AttributeDefinition[] attributesOptional)
    {
        m_Name = name;
        m_Id = identifier;
        m_Description = description;
        if (attributesRequired == null)
        {
            m_AttributesRequired = new AttributeDefinition[]{};   
        }
        else
        {
            m_AttributesRequired = attributesRequired.clone();
        }
        
        if (attributesOptional == null)
        {
            m_AttributesOptional = new AttributeDefinition[]{};   
        }
        else
        {
            m_AttributesOptional = attributesOptional.clone();
        }
    }

    @Override
    public String getName()
    {
        return m_Name;
    }

    @Override
    public String getID()
    {
        return m_Id;
    }

    @Override
    public String getDescription()
    {
        return m_Description;
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions(final int filter)
    { 
        final AttributeDefinition[] attributesEmpty = new AttributeDefinition[]{};
        
        switch (filter)
        {
            case ObjectClassDefinition.REQUIRED:
                return m_AttributesRequired.clone();
            case ObjectClassDefinition.OPTIONAL:
                return m_AttributesOptional.clone();
            case ObjectClassDefinition.ALL:
                //if both required and optional arrays are empty then no processing needed
                if (m_AttributesRequired.length == 0 && m_AttributesOptional.length == 0)
                {
                    return attributesEmpty; 
                }
                
                final ArrayList<AttributeDefinition> attrList = new ArrayList<AttributeDefinition>();
                for (AttributeDefinition attr : m_AttributesRequired)
                {
                    attrList.add(attr);
                }
                for (AttributeDefinition attr : m_AttributesOptional)
                {
                    attrList.add(attr);
                }
                final AttributeDefinition[] attributes = new AttributeDefinition[attrList.size()];
                return attrList.toArray(attributes);
            default:
                return attributesEmpty;
        }
    }

    @Override
    public InputStream getIcon(final int size) throws IOException
    {
        return null;
    }
}
