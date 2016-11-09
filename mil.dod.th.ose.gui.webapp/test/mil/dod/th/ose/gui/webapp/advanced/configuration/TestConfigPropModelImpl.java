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
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Test class for the {@link ConfigPropModelImpl} class.
 * 
 * @author cweisenborn
 */
public class TestConfigPropModelImpl
{
    private ConfigPropModelImpl m_SUT;
    private List<String> m_DefaultValues = new ArrayList<String>();
    private AttributeModel m_AttributeModel;
    
    @Before
    public void setup()
    {
        m_DefaultValues.add("value 1");
        m_DefaultValues.add("value 2");
        m_DefaultValues.add("value 3");
        
        m_AttributeModel = mock(AttributeModel.class);
        when(m_AttributeModel.getId()).thenReturn("test key");
        when(m_AttributeModel.getName()).thenReturn("test name");
        when(m_AttributeModel.getDescription()).thenReturn("test description");
        when(m_AttributeModel.getDefaultValues()).thenReturn(m_DefaultValues);
        when(m_AttributeModel.isRequired()).thenReturn(true);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
    }
    
    /**
     * Test the get key method.
     * Verify that the correct key is returned.
     */
    @Test
    public void testGetKey()
    {       
        assertThat(m_SUT.getKey(), is("test key"));
    }
    
    /**
     * Test the get name method.
     * Verify that the correct name is returned.
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
     * Test the get value method.
     * Verify that if no value is set that the default is returned.
     * Verify that the correct value is returned if a value is a set.
     */
    @Test
    public void testGetValue()
    {
        // single default value
        when(m_AttributeModel.getDefaultValues()).thenReturn(Arrays.asList(new String[]{"one default"}));
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getValue(), is((Object)"one default"));
        
        when(m_AttributeModel.getDefaultValues()).thenReturn(Arrays.asList(new String[]{"default 1", "default 2"}));
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getValue(), is((Object)Arrays.asList(new String[]{"default 1", "default 2"})));
        
        // set a value
        m_SUT.setValue("test non-default");
        assertThat(m_SUT.getValue(), is((Object)"test non-default"));
        
        //test empty default value array
        when(m_AttributeModel.getDefaultValues()).thenReturn(Arrays.asList(new String[]{}));
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getValue(), is((Object)""));
        
        //ensure that returned type corresponds to given attribute definition type (boolean)
        when(m_AttributeModel.getDefaultValues()).thenReturn(Arrays.asList(new String[]{"true"}));
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.BOOLEAN);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        Boolean myBool = (Boolean)m_SUT.getValue();
        assertThat(myBool, is(true));
        
        //for good measure, verify bad boolean type will result in exception
        when(m_AttributeModel.getDefaultValues()).thenReturn(Arrays.asList(new String[]{"not a bool"}));
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        try
        {
            fail("illegal argument exception expected. Value is: "+ m_SUT.getValue());
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString("cannot be parsed to a boolean"));
        }
    }
    
    
    /**
     * Test the set value method.
     * Verify that all value types can be set appropriately.
     */
    @Test
    public void testSetValue()
    {
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.LONG);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        m_SUT.setValue(5L);
        assertThat(m_SUT.getValue(), is((Object)5L));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.INTEGER);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        m_SUT.setValue(25);
        assertThat(m_SUT.getValue(), is((Object)25));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.SHORT);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        m_SUT.setValue((short)10);
        assertThat(m_SUT.getValue(), is((Object)(short)10));

        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.BYTE);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        m_SUT.setValue((byte)120);
        assertThat(m_SUT.getValue(), is((Object)(byte)120));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.DOUBLE);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        m_SUT.setValue(1.22);
        assertThat(m_SUT.getValue(), is((Object)1.22));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.FLOAT);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        m_SUT.setValue(12.5f);
        assertThat(m_SUT.getValue(), is((Object)12.5f));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.BOOLEAN);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        m_SUT.setValue(true);
        assertThat(m_SUT.getValue(), is((Object)true));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.CHARACTER);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        m_SUT.setValue('A');
        assertThat(m_SUT.getValue(), is((Object)'A'));
        
        //test invalid types
        m_SUT.setValue(null);
        assertThat(m_SUT.getValue(), is((Object)m_DefaultValues));
        m_SUT.setValue("");
        assertThat(m_SUT.getValue(), is((Object)""));
    }
    
    /**
     * Test get default values method.
     * Verify that the appropriate default values are returned.
     */
    @Test
    public void testGetDefaultValues()
    {        
        assertThat(m_SUT.getDefaultValues(), is(m_DefaultValues));
    }
    
    /**
     * Test the get option labels method.
     * Verify that the appropriate option labels are returned.
     */
    @Test
    public void testGetOptions()
    {
        List<String> labels = new ArrayList<>();
        labels.add("option 1");
        labels.add("option 2");
        labels.add("option 3");
        when(m_AttributeModel.getOptionLabels()).thenReturn(labels);
        
        List<String> values = new ArrayList<>();
        values.add("1");
        values.add("2");
        values.add("3");
        when(m_AttributeModel.getOptionValues()).thenReturn(values);
        
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);

        assertThat(m_SUT.getOptions().size(), is(3));
        assertThat(m_SUT.getOptions().get(0).getLabel(), is("option 1"));
        assertThat(m_SUT.getOptions().get(0).getValue(), is("1"));
        assertThat(m_SUT.getOptions().get(1).getLabel(), is("option 2"));
        assertThat(m_SUT.getOptions().get(1).getValue(), is("2"));
        assertThat(m_SUT.getOptions().get(2).getLabel(), is("option 3"));
        assertThat(m_SUT.getOptions().get(2).getValue(), is("3"));
    }
    
    /**
     * Test the get type method.
     * Verify that all class types are appropriately returned.
     */
    @Test
    public void testGetType()
    {
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.STRING);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)String.class));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.LONG);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)Long.class));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.INTEGER);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)Integer.class));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.SHORT);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)Short.class));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.CHARACTER);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)Character.class));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.BYTE);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)Byte.class));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.DOUBLE);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)Double.class));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.FLOAT);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)Float.class));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.BOOLEAN);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.getType(), is((Object)Boolean.class));
    }
    
    /**
     * Verify boolean type returns true when type is boolean, false otherwise
     */
    @Test
    public void testIsBooleanType()
    {        
        assertThat(m_SUT.isBooleanType(), is(false));
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.BOOLEAN);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        assertThat(m_SUT.isBooleanType(), is(true));
    }
    
    /**
     * Test the validate value method.
     * Verify that method takes the appropriate action depending on whether value is valid or not.
     */
    @Test
    public void testValidateValue()
    {
        FacesContext context = mock(FacesContext.class);
        UIComponent component = mock(UIComponent.class);
        
        when(m_AttributeModel.getType()).thenReturn(AttributeDefinition.INTEGER);
        m_SUT = new ConfigPropModelImpl(m_AttributeModel);
        
        try
        {
            m_SUT.validateValue(context, component, "not int");
            fail("Validation exception should be thrown.");
        }
        catch (ValidatorException exception)
        {
            //ExpectedException
            assertThat(exception.getFacesMessage().getSummary(), is("Value Invalid Type!"));
        }
        
        try
        {
            m_SUT.validateValue(context, component, "34");
        }
        catch (ValidatorException exception)
        {
            fail("Validation should have passed and therefore no exception should be thrown.");
        }
    }
}
