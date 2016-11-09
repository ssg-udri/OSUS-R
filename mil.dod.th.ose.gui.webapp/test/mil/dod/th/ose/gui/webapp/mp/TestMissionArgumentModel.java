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
package mil.dod.th.ose.gui.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil.ValidationEnum;

/**
 * Test class for {@link MissionArgumentModel}.
 * 
 * @author cweisenborn
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ArgumentValidatorUtil.class)
public class TestMissionArgumentModel
{
    private MissionArgumentModel m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new MissionArgumentModel();
    }
    
    @Test
    public void testGetName()
    {
        m_SUT.setName("SomeName");
        
        assertThat(m_SUT.getName(), equalTo("SomeName"));
    }
    
    @Test
    public void testGetDescription()
    {
        final String description = "Some really long description that describes the variable. BLah BLah Blah blah...";
        
        m_SUT.setDescription(description);
        
        assertThat(m_SUT.getDescription(), equalTo(description));
    }
    
    @Test
    public void testGetType()
    {
        m_SUT.setType(MissionVariableTypesEnum.ASSET);
        
        assertThat(m_SUT.getType(), equalTo(MissionVariableTypesEnum.ASSET));
    }
    
    @Test
    public void testGetDefaultValue()
    {
        final String defaultValue = "SOMEVALUE OF SOMETYPE";
        
        m_SUT.setType(MissionVariableTypesEnum.STRING);
        m_SUT.setCurrentValue(defaultValue);
        
        assertThat((String)m_SUT.getCurrentValue(), equalTo(defaultValue));
    }
    
    @Test
    public void testGetOptionalValues()
    {
        assertThat(m_SUT.getOptionValues().isEmpty(), is(true));
        
        List<String> optionalValues = new ArrayList<String>();
        optionalValues.add("Option Value 1");
        optionalValues.add("Option Value 2");
        optionalValues.add("Option Value 3");
    
        m_SUT.setOptionValues(optionalValues);
    
        assertThat(m_SUT.getOptionValues(), equalTo(optionalValues));
    }
    
    @Test
    public void testGetMinValue()
    {
        final String minValue = "Some Value";
        
        m_SUT.setMinValue(minValue);
        
        assertThat(m_SUT.getMinValue(), equalTo(minValue));
    }
    
    @Test
    public void testGetMaxValue()
    {
        final String maxValue = "Some MAX Value";
        
        m_SUT.setMaxValue(maxValue);
        
        assertThat(m_SUT.getMaxValue(), equalTo(maxValue));
    }
    
    /**
     * Test getting the value of the argument. Should return the proper type if the variable type is numeric.
     */
    @Test
    public void testGetValue()
    {
        m_SUT.setType(MissionVariableTypesEnum.STRING);
        String value = "";
        m_SUT.setCurrentValue(value);
        assertThat((String)m_SUT.getCurrentValue(), is(""));
        
        value = null;
        m_SUT.setCurrentValue(value);
        assertThat((String)m_SUT.getCurrentValue(), nullValue());
        
        //set type to integer, then set value
        m_SUT.setType(MissionVariableTypesEnum.INTEGER);
        //all information from text boxes on pages are strings
        value = "9";
        m_SUT.setCurrentValue(value);
        //verify
        assertThat(Integer.valueOf(m_SUT.getCurrentValue().toString()), is(9));
        assertThat(m_SUT.getCurrentValue(), instanceOf(Integer.class));
        
        //set type to double, then set value
        m_SUT.setType(MissionVariableTypesEnum.DOUBLE);
        //all information from text boxes on pages are strings
        value = "9.9987";
        m_SUT.setCurrentValue(value);
        //verify
        assertThat(Double.valueOf(m_SUT.getCurrentValue().toString()), is(9.9987));
        assertThat(m_SUT.getCurrentValue(), instanceOf(Double.class));
        
        m_SUT.setType(MissionVariableTypesEnum.LONG);
        value = "109203839";
        m_SUT.setCurrentValue(value);
        assertThat(Long.valueOf(m_SUT.getCurrentValue().toString()), is(Long.valueOf(value)));
        assertThat(m_SUT.getCurrentValue(), instanceOf(Long.class));
        
        m_SUT.setType(MissionVariableTypesEnum.FLOAT);
        value = "109.2";
        m_SUT.setCurrentValue(value);
        assertThat(Float.valueOf(m_SUT.getCurrentValue().toString()), is(Float.valueOf(value)));
        assertThat(m_SUT.getCurrentValue(), instanceOf(Float.class));
        
        m_SUT.setType(MissionVariableTypesEnum.BYTE);
        value = "102";
        m_SUT.setCurrentValue(value);
        Byte byteValue = 102;
        assertThat(Byte.valueOf(m_SUT.getCurrentValue().toString()), is(byteValue));
        assertThat(m_SUT.getCurrentValue(), instanceOf(Byte.class));
        
        m_SUT.setType(MissionVariableTypesEnum.SHORT);
        value = "100";
        m_SUT.setCurrentValue(value);
        assertThat(Short.valueOf(m_SUT.getCurrentValue().toString()), is(Short.valueOf(value)));
        assertThat(m_SUT.getCurrentValue(), instanceOf(Short.class));
        
        m_SUT.setType(MissionVariableTypesEnum.STRING);
        value = "some string";
        m_SUT.setCurrentValue(value);
        assertThat((String)m_SUT.getCurrentValue(), is(value));
        assertThat(m_SUT.getCurrentValue(), instanceOf(String.class));
        
        //Verify that the string representation of false is returned as the boolean representation.
        m_SUT.setType(MissionVariableTypesEnum.BOOLEAN);
        value = "false";
        m_SUT.setCurrentValue(value);
        assertThat((Boolean)m_SUT.getCurrentValue(), is(false));
        assertThat(m_SUT.getCurrentValue(), instanceOf(Boolean.class));
        
        //Verify that the string representation of true is returned as the boolean representation.
        m_SUT.setType(MissionVariableTypesEnum.BOOLEAN);
        value = "true";
        m_SUT.setCurrentValue(value);
        assertThat((Boolean)m_SUT.getCurrentValue(), is(true));
        assertThat(m_SUT.getCurrentValue(), instanceOf(Boolean.class));
        
        //Verify that a string that does not represent a boolean type throws an illegal argument exception.
        m_SUT.setType(MissionVariableTypesEnum.BOOLEAN);
        value = "not a boolean";
        try
        {
            m_SUT.setCurrentValue(value);
            fail("Illegal argument exception should be thrown.");
        }
        catch (IllegalArgumentException e)
        {
            //Expected exception.
        }
        
        //Test all types that should return a string below.
        m_SUT.setType(MissionVariableTypesEnum.ASSET);
        value = "Asset";
        m_SUT.setCurrentValue(value);
        assertThat((String)m_SUT.getCurrentValue(), is(value));
        assertThat(m_SUT.getCurrentValue(), instanceOf(String.class));
        
        m_SUT.setType(MissionVariableTypesEnum.LINK_LAYER);
        value = "link layer";
        m_SUT.setCurrentValue(value);
        assertThat((String)m_SUT.getCurrentValue(), is(value));
        assertThat(m_SUT.getCurrentValue(), instanceOf(String.class));
        
        m_SUT.setType(MissionVariableTypesEnum.PHYSICAL_LINK);
        value = "physical link";
        m_SUT.setCurrentValue(value);
        assertThat((String)m_SUT.getCurrentValue(), is(value));
        assertThat(m_SUT.getCurrentValue(), instanceOf(String.class));
        
        m_SUT.setType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        value = "program dependencies";
        m_SUT.setCurrentValue(value);
        assertThat((String)m_SUT.getCurrentValue(), is(value));
        assertThat(m_SUT.getCurrentValue(), instanceOf(String.class));
        
        m_SUT.setType(MissionVariableTypesEnum.TRANSPORT_LAYER);
        value = "transport layer";
        m_SUT.setCurrentValue(value);
        assertThat((String)m_SUT.getCurrentValue(), is(value));
        assertThat(m_SUT.getCurrentValue(), instanceOf(String.class));
    }
    
    @Test
    public void testValidateValue()
    {
        FacesContext context = mock(FacesContext.class);
        UIComponent component = mock(UIComponent.class);
        PowerMockito.mockStatic(ArgumentValidatorUtil.class);
        
        Object value = "Value doesn't matter!";
        try
        {
            m_SUT.setType(MissionVariableTypesEnum.INTEGER);
            PowerMockito.when(ArgumentValidatorUtil.isValidValue((String)value, null, null, Integer.class)).
                thenReturn(ValidationEnum.PARSING_FAILURE);
            
            m_SUT.validateValue(context, component, value);
            fail("Validation exception should be thrown.");
        }
        catch (ValidatorException exception)
        {
            //ExpectedException
            assertThat(exception.getFacesMessage().getSummary(), is("Value Invalid Type!"));
        }
        
        try
        {
            m_SUT.setType(MissionVariableTypesEnum.FLOAT);
            PowerMockito.when(ArgumentValidatorUtil.isValidValue((String)value, null, null, Float.class)).
                thenReturn(ValidationEnum.GREATER_THAN_MAX);
            m_SUT.validateValue(context, component, value);
            fail("Validation exception should be thrown.");
        }
        catch (ValidatorException exception)
        {
            //ExpectedException
            assertThat(exception.getFacesMessage().getSummary(), is("Value Above Max!"));
        }
        
        try
        {
            m_SUT.setType(MissionVariableTypesEnum.DOUBLE);
            PowerMockito.when(ArgumentValidatorUtil.isValidValue((String)value, null, null, Double.class)).
                thenReturn(ValidationEnum.LESS_THAN_MIN);
            m_SUT.validateValue(context, component, value);
            fail("Validation exception should be thrown.");
        }
        catch (ValidatorException exception)
        {
            //ExpectedException
            assertThat(exception.getFacesMessage().getSummary(), is("Value Below Min!"));
        }
        
        try
        {
            m_SUT.setType(MissionVariableTypesEnum.SHORT);
            PowerMockito.when(ArgumentValidatorUtil.isValidValue((String)value, null, null, Short.class)).
                thenReturn(ValidationEnum.PASSED);
            m_SUT.validateValue(context, component, value);
        }
        catch (ValidatorException exception)
        {
            fail("Validation should have passed and therefore no exception should be thrown.");
        }
    }
}
