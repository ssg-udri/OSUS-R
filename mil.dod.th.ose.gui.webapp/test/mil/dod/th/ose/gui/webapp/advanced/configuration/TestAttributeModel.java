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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the {@link AttributeModel} class.
 * 
 * @author cweisenborn
 */
public class TestAttributeModel
{
    private AttributeModel m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new AttributeModel(AttributeDefinitionType.newBuilder()
                    .setId("test id")
                    .setName("test name")
                    .setDescription("test description")
                    .setAttributeType(1)
                    .setCardinality(2)
                    .addOptionValue("value1")
                    .addOptionValue("value2")
                    .addOptionValue("value3")
                    .addOptionLabel("label1")
                    .addOptionLabel("label2")
                    .addOptionLabel("label3")
                    .addDefaultValue("default1")
                    .addDefaultValue("default2")
                    .addDefaultValue("default3")
                    .setRequired(true)
                    .build());
    }
    
    /**
     * Test get id method.
     * Verify the correct id is returned.
     */
    @Test
    public void testGetId()
    {    
        assertThat(m_SUT.getId(), is("test id"));
    }
    
    /**
     * Test the get name method.
     * Verify the correct name is returned.
     */
    @Test
    public void testGetName()
    {
        assertThat(m_SUT.getName(), is("test name"));
    }
    
    /**
     * Test the get description method.
     * Verify that the correct description is returned.
     */
    @Test
    public void testGetDescription()
    {
        assertThat(m_SUT.getDescription(), is("test description"));
    }
    
    /**
     * Test the get type method.
     * Verify that the correct type is returned.
     */
    @Test
    public void testGetType()
    {
        assertThat(m_SUT.getType(), is(1));
    }
    
    /**
     * Test the get cardinality method.
     * Verify that the correct cardinality is returned.
     */
    @Test
    public void testGetCardinality()
    {
        assertThat(m_SUT.getCardinality(), is(2));
    }
    
    /**
     * Test the get default values method.
     * Verify that the correct default values list is returned.
     */
    @Test
    public void testGetDefaultValues()
    {       
        assertThat(m_SUT.getDefaultValues().size(), is(3));
        assertThat(m_SUT.getDefaultValues(), hasItem("default1"));
        assertThat(m_SUT.getDefaultValues(), hasItem("default2"));
        assertThat(m_SUT.getDefaultValues(), hasItem("default3"));
    }
    
    /**
     * Test the get option labels method.
     * Verify that the correct option labels list is returned.
     */
    @Test
    public void testGetOptionLabels()
    {
        assertThat(m_SUT.getOptionLabels().size(), is(3));
        assertThat(m_SUT.getOptionLabels(), hasItem("label1"));
        assertThat(m_SUT.getOptionLabels(), hasItem("label2"));
        assertThat(m_SUT.getOptionLabels(), hasItem("label3"));
    }
    
    /**
     * Test the get option values method.
     * Verify that the correct option values list is returned.
     */
    @Test
    public void testGetOptionValues()
    {
        assertThat(m_SUT.getOptionValues().size(), is(3));
        assertThat(m_SUT.getOptionValues(), hasItem("value1"));
        assertThat(m_SUT.getOptionValues(), hasItem("value2"));
        assertThat(m_SUT.getOptionValues(), hasItem("value3"));
    }
}
