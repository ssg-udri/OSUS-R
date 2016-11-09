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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import mil.dod.th.ose.test.AttributeDefinitionMocker;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * @author kucktoni

 */
public class TestFactoryObjectClassDefinition
{
    private FactoryObjectClassDefinition m_SUT;
    private FactoryObjectClassDefinition m_SUTNullAttribs;
    private String m_Name;
    private String m_Id;
    private String m_Description;
    private AttributeDefinition[] m_AttributesRequired;
    private AttributeDefinition[] m_AttributesOptional;
    private AttributeDefinition[] m_AttributesNull;
    private int m_BadFilter;
    private int m_Size;
    private InputStream m_InputStreamNull;
    
    @Before
    public void setUp() throws Exception
    {
        m_Name = "name";
        m_Id = "id";
        m_Description = "desc";
        m_AttributesRequired = AttributeDefinitionMocker.mockArrayRequired();
        m_AttributesOptional = AttributeDefinitionMocker.mockArrayOptional();
        m_AttributesNull = null;
        m_BadFilter = 5;
        m_Size = 57;
        m_InputStreamNull = null;
        
        m_SUT = new FactoryObjectClassDefinition(m_Name, m_Id, m_Description, m_AttributesRequired, 
                m_AttributesOptional);
        m_SUTNullAttribs = new FactoryObjectClassDefinition(m_Name, m_Id, m_Description, m_AttributesNull, 
                m_AttributesNull);
    }

    @Test
    public void testGetName()
    {
        assertThat(m_Name, is(m_SUT.getName()));
    }
    
    @Test
    public void testGetID()
    {
        assertThat(m_Id, is(m_SUT.getID()));
    }
    
    @Test
    public void testGetDescription()
    {
        assertThat(m_Description, is(m_SUT.getDescription()));
    }
    
    @Test
    public void testGetAttributeDefinitionsRequired()
    {
        List<AttributeDefinition> attrs = Arrays.asList(
                m_SUT.getAttributeDefinitions(ObjectClassDefinition.REQUIRED));
        assertThat(attrs.size(), is(m_AttributesRequired.length));
        assertThat(attrs, not(hasItems(m_AttributesOptional)));
        assertThat(attrs, hasItems(m_AttributesRequired));
    }
    
    @Test
    public void testGetAttributeDefinitionsRequiredNull()
    {
        assertThat(m_SUTNullAttribs.getAttributeDefinitions(ObjectClassDefinition.REQUIRED).length, is(0));
    }
    
    @Test
    public void testGetAttributeDefinitionsOptional()
    {
        List<AttributeDefinition> attrs = Arrays.asList(
                m_SUT.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL));
        assertThat(attrs.size(), is(m_AttributesOptional.length));
        assertThat(attrs, not(hasItems(m_AttributesRequired)));
        assertThat(attrs, hasItems(m_AttributesOptional));
    }
    
    @Test
    public void testGetAttributeDefinitionsOptionalNull()
    {
        assertThat(m_SUTNullAttribs.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL).length, is(0));
    }
    
    @Test
    public void testGetAttributeDefinitionsAll()
    {
        List<AttributeDefinition> attrs = Arrays.asList(
                m_SUT.getAttributeDefinitions(ObjectClassDefinition.ALL));
        assertThat(attrs.size(), is(m_AttributesRequired.length + m_AttributesOptional.length));
        assertThat(attrs, hasItems(m_AttributesOptional));
        assertThat(attrs, hasItems(m_AttributesRequired));
    }
    
    @Test
    public void testGetAttributeDefinitionsAllNullRequired()
    {
        final FactoryObjectClassDefinition sutNullRequired = new FactoryObjectClassDefinition(m_Name, m_Id, 
                m_Description, m_AttributesNull, m_AttributesOptional);
        List<AttributeDefinition> attrs = Arrays.asList(
                sutNullRequired.getAttributeDefinitions(ObjectClassDefinition.ALL));
        assertThat(attrs.size(), is(m_AttributesOptional.length));
        assertThat(attrs, hasItems(m_AttributesOptional));
        assertThat(attrs, not(hasItems(m_AttributesRequired)));
    }
    
    @Test
    public void testGetAttributeDefinitionsAllNullOptional()
    {
        final FactoryObjectClassDefinition sutNullOptional = new FactoryObjectClassDefinition(m_Name, m_Id, 
                m_Description, m_AttributesRequired, m_AttributesNull);
        List<AttributeDefinition> attrs = Arrays.asList(
                sutNullOptional.getAttributeDefinitions(ObjectClassDefinition.ALL));
        assertThat(attrs.size(), is(m_AttributesRequired.length));
        assertThat(attrs, hasItems(m_AttributesRequired));
        assertThat(attrs, not(hasItems(m_AttributesOptional)));
    }
    
    @Test
    public void testGetAttributeDefinitionsAllNullBoth()
    {
        assertThat(m_SUTNullAttribs.getAttributeDefinitions(ObjectClassDefinition.ALL).length, is(0));
    }
    
    @Test
    public void testGetAttributeDefinitionsBadFilter()
    {
        assertThat(m_SUT.getAttributeDefinitions(m_BadFilter).length, is(0));
    }

    @Test
    public void testGetIcon()
    {
        try
        {
            assertThat(m_SUT.getIcon(m_Size), is(m_InputStreamNull));
        } 
        catch (IOException ex) 
        {

        }
    }
}
