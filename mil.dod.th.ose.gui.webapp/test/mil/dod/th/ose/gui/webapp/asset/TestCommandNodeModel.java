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
package mil.dod.th.ose.gui.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import mil.dod.th.ose.gui.webapp.asset.ExampleCommandObject.TestEnum;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil.ValidationEnum;
import mil.dod.th.ose.gui.webapp.utils.ReflectionsUtilException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test class for the {@link CommandNodeModel} class.
 * 
 * @author cweisenborn
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ArgumentValidatorUtil.class)
public class TestCommandNodeModel
{
    private CommandNodeModel m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    private ExampleCommandObject m_TestObject;
    private List<Integer> m_IntList;
    
    @Before
    public void setup()
    {
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_IntList = new ArrayList<Integer>();
        m_IntList.add(4);
        m_IntList.add(3);
        m_IntList.add(12);
        m_IntList.add(7);
        
        m_TestObject = new ExampleCommandObject(5, true, false,10.35, TestEnum.TestEnum1, 5.2f, m_IntList);
        m_SUT = new CommandNodeModel("intField", int.class, false, m_TestObject, m_GrowlUtil);
    }
    
    /**
     * Test the get name method.
     * Verify the correct name is returned.
     */
    @Test
    public void testGetName()
    {
        assertThat(m_SUT.getName(), is("intField"));
    }
    
    /**
     * Test the set name method.
     * Verify that correct name is set.
     */
    @Test
    public void testSetName()
    {
        m_SUT.setName("Bob");
        assertThat(m_SUT.getName(), is("Bob"));
    }
    
    /**
     * Test the get type method.
     * Verify that correct class type is returned.
     */
    @Test
    public void testGetType()
    {
        assertThat(m_SUT.getType(), is((Object)int.class));
    }
    
    /**
     * Test the set type method.
     * Verify the correct class type is set.
     */
    @Test
    public void testSetType()
    {
        m_SUT.setType(Object.class);
        assertThat(m_SUT.getType(), is((Object)Object.class));
    }
    
    /**
     * Test the is reference method.
     * Verify that the correct boolean value is returned.
     */
    @Test
    public void testIsReference()
    {
        assertThat(m_SUT.isReference(), is(false));
    }
    
    /**
     * Test the get index method.
     * Verify the correct index number is returned.
     */
    @Test
    public void testGetIndex()
    {
        m_SUT.setIndex(2);
        assertThat(m_SUT.getIndex(), is(2));
    }
    
    /**
     * Test the get containing object method.
     * Verify that the correct object is returned.
     */
    @Test 
    public void testGetContainingObject()
    {
        assertThat(m_SUT.getContainingObject(), is((Object)m_TestObject));
    }
    
    /**
     * Test the set value method.
     * Verify that set value method handles the various class types appropriately.
     */
    @Test
    public void testSetValue() throws ReflectionsUtilException
    {
        PowerMockito.mockStatic(ArgumentValidatorUtil.class);
        
        //Testing setting standard value.
        assertThat(m_TestObject.getIntField(), is(5));
        PowerMockito.when(ArgumentValidatorUtil.convertToActualType("2", int.class)).thenReturn(2);
        m_SUT.setValue("2");
        assertThat(m_TestObject.getIntField(), is(2));
        
        //Test setting enumeration value.
        m_SUT.setType(TestEnum.class);
        m_SUT.setName("enumField");
        assertThat(m_TestObject.getEnumField(), is(TestEnum.TestEnum1));
        PowerMockito.when(ArgumentValidatorUtil.convertToActualType(TestEnum.TestEnum2.toString(), 
                TestEnum.class)).thenReturn(TestEnum.TestEnum2);
        m_SUT.setValue(TestEnum.TestEnum2.toString());
        assertThat(m_TestObject.getEnumField(), is(TestEnum.TestEnum2));
        
        //Test setting nullable field.
        m_SUT.setType(Float.class);
        m_SUT.setName("nullableField");
        assertThat(m_TestObject.getNullableField(), is(5.2f));
        m_SUT.setValue(null);
        assertThat(m_TestObject.getNullableField(), is(nullValue()));
        m_SUT.setValue("");
        assertThat(m_TestObject.getNullableField(), is(nullValue()));
        
        //Test setting a list value.
        m_SUT = new CommandNodeModel("listField", Integer.class, false, m_IntList, m_GrowlUtil);
        m_SUT.setIndex(2);
        assertThat(m_IntList.get(2), is(12));
        PowerMockito.when(ArgumentValidatorUtil.convertToActualType("125", Integer.class)).thenReturn(125);
        m_SUT.setValue("125");
        assertThat(m_IntList.get(2), is(125));
        assertThat(m_TestObject.getListField(), is(m_IntList));
        
        //Test setting an optional integer that has getter/setter methods that take a primitive int value.
        m_SUT = new CommandNodeModel("wrapIntField", Integer.class, false, m_TestObject, m_GrowlUtil);
        assertThat(m_SUT.getValue(), is((Object)""));
        PowerMockito.when(ArgumentValidatorUtil.convertToActualType("25", Integer.class)).thenReturn(25);
        m_SUT.setValue("25");
        assertThat(m_SUT.getValue(), is((Object)25));
        m_SUT.setValue(null);
        assertThat(m_SUT.getValue(), is((Object)""));
    }
    
    /**
     * Test the get value method.
     * Verify that the get value method returns the appropriate value for the various class types. 
     */
    @Test
    public void testGetValue() throws ReflectionsUtilException
    {
        //Test getting standard value.
        assertThat(m_SUT.getValue(), is((Object)5));
        
        //Test getting boolean value.
        m_SUT.setType(boolean.class);
        m_SUT.setName("boolField");
        assertThat(m_SUT.getValue(), is((Object)true));
        
        //Test getting wrapped boolean value.
        m_SUT.setType(Boolean.class);
        m_SUT.setName("wrapBoolField");
        assertThat(m_SUT.getValue(), is((Object)false));
        
        //Test getting list value.
        m_SUT = new CommandNodeModel("listField", Integer.class, false, m_IntList, m_GrowlUtil);
        m_SUT.setIndex(2);
        assertThat(m_SUT.getValue(), is((Object)12));
        
        //Test getting optional integer value with getter method that returns a primitive int.
        m_SUT = new CommandNodeModel("wrapIntField", Integer.class, false, m_TestObject, m_GrowlUtil);
        assertThat(m_SUT.getValue(), is((Object)""));
    }
    
    /**
     * Test the validate value method.
     * Verify validation passes and fails when appropriate.
     */
    @Test
    public void testValidateValue()
    {
        FacesContext context = mock(FacesContext.class);
        UIComponent component = mock(UIComponent.class);
        PowerMockito.mockStatic(ArgumentValidatorUtil.class);
        
        try
        {
            m_SUT.setType(Integer.class);
            m_SUT.validateValue(context, component, null);
        }
        catch (ValidatorException exception)
        {
            fail("Validation should have succeeded.");
        }
        
        try
        {
            m_SUT.setType(Integer.class);
            m_SUT.validateValue(context, component, "");
        }
        catch (ValidatorException exception)
        {
            fail("Validation should have succeeded.");
        }
        
        Object value = "Value doesn't matter!";
        try
        {
            m_SUT.setType(int.class);
            PowerMockito.when(ArgumentValidatorUtil.isValidValue((String)value, null, null, int.class)).
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
            m_SUT.setType(int.class);
            PowerMockito.when(ArgumentValidatorUtil.isValidValue((String)value, null, null, int.class)).
                thenReturn(ValidationEnum.PASSED);
            
            m_SUT.validateValue(context, component, value);
        }
        catch (ValidatorException exception)
        {
            fail("Validation should have succeeded.");
        }
    }
    
    /**
     * Test the is add supported method.
     * Verify the correct boolean value is returned.
     */
    @Test
    public void testIsAddSupported() throws ReflectionsUtilException
    {
        assertThat(m_SUT.isAddSupported(), is(false));
        
        m_SUT = new CommandNodeModel("listField", List.class, true, m_TestObject, m_GrowlUtil);
        assertThat(m_SUT.isAddSupported(), is(true));
       
        m_SUT = new CommandNodeModel("nullableField", Float.class, true, m_TestObject, m_GrowlUtil);
        m_SUT.setValue(null);
        assertThat(m_SUT.isAddSupported(), is(true));
    }
    
    /**
     * Test the is delete supported method.
     * Verify the correct boolean value is returned.
     */
    @Test
    public void testIsDeleteSupported() throws ReflectionsUtilException
    {
        assertThat(m_SUT.isDeleteSupported(), is(false));
        
        m_SUT = new CommandNodeModel("listInt", Integer.class, true, m_TestObject.getListField(), m_GrowlUtil);
        assertThat(m_SUT.isDeleteSupported(), is(true));
        
        m_SUT = new CommandNodeModel("nullableField", Float.class, true, m_TestObject, m_GrowlUtil);
        m_SUT.setValue("5.0");
        assertThat(m_SUT.isDeleteSupported(), is(true));
    }
}
