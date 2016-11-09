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
package mil.dod.th.ose.gui.webapp.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Field; //NOCHECKSTYLE: illegal package, needed to test this utility
import java.lang.reflect.InvocationTargetException;  // NOCHECKSTYLE: ditto
import java.lang.reflect.Method; // NOCHECKSTYLE: ditto

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

import org.junit.Before;
import org.junit.Test;

/**
 * Class to test ReflectionsUtil functionality.
 */
public class TestReflectionsUtil
{
    private TestObject m_Obj;
    
    @Before
    public void init()
    {
        m_Obj = new TestObject();
    }
    
    /**
     * Verify can retrieve the correct get methods on an object.
     */
    @Test
    public void testRetrieveGetMethod() throws ReflectionsUtilException, IllegalArgumentException, 
        IllegalAccessException, InvocationTargetException
    {
        Method methodInteger = ReflectionsUtil.retrieveGetMethod(m_Obj, "TestInteger", int.class);
        
        assertThat(methodInteger, notNullValue());
        assertThat((Integer)methodInteger.invoke(m_Obj), is(1));
        
        Method methodBooleanType = ReflectionsUtil.retrieveGetMethod(m_Obj, "TestBooleanType", boolean.class);
        assertThat(methodBooleanType, notNullValue());
        assertThat((Boolean)methodBooleanType.invoke(m_Obj), is(false));
        
        Method methodBooleanObject = ReflectionsUtil.retrieveGetMethod(m_Obj, "TestBooleanObject", Boolean.class);
        assertThat(methodBooleanObject, notNullValue());
        assertThat((Boolean)methodBooleanObject.invoke(m_Obj), is(true));
    }
    
    /**
     * Verify a method that does not exist produces a no such method exception. 
     */
    @Test
    public void testRetrieveGetMethodNoSuchMethod() throws SecurityException
    {
        try
        {
            ReflectionsUtil.retrieveGetMethod(m_Obj, "DoesNotExistMethod", int.class);
            fail("Method does not exist therefore a NoSuchMethodException should have been thrown.");
        }
        catch (final ReflectionsUtilException exception)
        {
            //Expected exception.
        }
    }
    
    /**
     * Verify class with no methods returns null.
     */
    @Test
    public void testRetrieveSetMethodWithClassWithNoMethods()
    {
        TestClassNoMethods noMethods = new TestClassNoMethods();
        
        Method returnedMethod = ReflectionsUtil.retrieveSetMethod(noMethods, "methodName");
        
        assertThat(returnedMethod, nullValue());
    }
    
    /**
     * Verify a set method that does not exist will return null and a correct 
     * method name returns the correct method.
     */
    @Test
    public void testRetrieveSetMethod() throws 
        IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        Method methodDNE = ReflectionsUtil.retrieveSetMethod(m_Obj, "methodName");
        
        assertThat(methodDNE, nullValue());
        
        Method methodExists = ReflectionsUtil.retrieveSetMethod(m_Obj, "TestInteger");
        assertThat(methodExists, notNullValue());
        
        methodExists.invoke(m_Obj, 11);
        
        assertThat(m_Obj.getTestInteger(), is(11));
    }
    
    /**
     * Verify that the inner object returned is correct.
     */
    @Test
    public void testRetrieveInnerObject() throws ReflectionsUtilException
    {
        final Integer testObject = (Integer)ReflectionsUtil.retrieveInnerObject(m_Obj, "TestInteger", Integer.class);
        assertThat(testObject, is(1));
    }
    
    /**
     * Verify that an exception is thrown when trying retrieve an inner object for a field that has no getter method.
     */
    @Test
    public void testRetrieveInnerObjectWithIncorrectMethod()
    {
        try
        {
            ReflectionsUtil.retrieveInnerObject(m_Obj, "DoesNotExistMethod", int.class);
            fail("Get method does not exists therefore a NoSuchMethod exception should have been thrown.");
        }
        catch (final ReflectionsUtilException exception)
        {
            //Expected Exception
        }
    }
    
    /**
     * Verify that setting the inner object for a method does not exist causes an error to 
     * be output.
     */
    @Test
    public void testSetInnerObjectWithIncorrectMethod()
    {
        try
        {
            ReflectionsUtil.setInnerObject(m_Obj, "DoesNotExistMethod", 10);
            fail("Set method does not exists therefore a NoSuchMethod exception should have been thrown.");
        }
        catch (final ReflectionsUtilException exception)
        {
            //Expected Exception
        }
    }
    
    /**
     * Verify that the isSet method will return null if the method does not exist and a correct method name returns
     * a valid method.
     */
    @Test
    public void tesRetrievetIsSetMethod() throws IllegalArgumentException, IllegalAccessException, 
        InvocationTargetException
    {
        Method methodDNE = ReflectionsUtil.retrieveIsSetMethod(m_Obj, "DoesNotExist");
        assertThat(methodDNE, is(nullValue()));
        
        Method isSetMethod = ReflectionsUtil.retrieveIsSetMethod(m_Obj, "TestInteger");
        assertThat(isSetMethod, is(notNullValue()));
        
        boolean isSet = (Boolean)isSetMethod.invoke(m_Obj, new Object[] {});
        assertThat(isSet, is(true));
    }
    
    /**
     * Verify that the unset method will return null if the method does not exist and a correct method name returns
     * a valid method.
     */
    @Test
    public void testRetrieveUnsetMethod() throws IllegalArgumentException, IllegalAccessException, 
        InvocationTargetException
    {
        Method methodDNE = ReflectionsUtil.retrieveUnsetMethod(m_Obj, "DoesNotExist");
        assertThat(methodDNE, is(nullValue()));
        
        Method unsetMethod = ReflectionsUtil.retrieveUnsetMethod(m_Obj, "TestInteger");
        assertThat(unsetMethod, is(notNullValue()));
        
        unsetMethod.invoke(m_Obj, new Object[] {});
        try
        {
            m_Obj.getTestInteger();
            fail("Null pointer exception should be thrown since get method returns a primitive integer.");
        }
        catch (NullPointerException ex)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that the isFieldSet method throws an exception if the there is no isSet method or returns the correct 
     * boolean value after invoking the isSet method.
     */
    @Test
    public void testIsFieldSet() throws ReflectionsUtilException
    {
        try
        {
            ReflectionsUtil.isFieldSet(m_Obj, "DoseNotExist");
            fail("isSet method for the field does not exist therefore a NoSuchMethodException should be thrown.");
        }
        catch (final ReflectionsUtilException exception)
        {
            //Expected Exception
        }
        
        Boolean isSet = ReflectionsUtil.isFieldSet(m_Obj, "TestInteger");
        assertThat(isSet, is(true));
    }
    
    /**
     * Verify that the unset method sets the specified field to null either through the unset method or through the
     * fields setter methods.
     */
    @Test
    public void testUnsetField() throws ReflectionsUtilException
    {
        //Verify if a field with no unset method calls the unset field method then it attempts to use the fields setter
        //method instead.
        assertThat(m_Obj.getTestDouble(), is(5.25));
        ReflectionsUtil.unsetField(m_Obj, "TestDouble");
        assertThat(m_Obj.getTestDouble(), is(nullValue()));
        
        //Verify if a field with an unset method class the unsetField method then the value is set to null for that
        //field.
        assertThat(m_Obj.getTestInteger(), is(1));
        ReflectionsUtil.unsetField(m_Obj, "TestInteger");
        try
        {
            m_Obj.getTestInteger();
            fail("Null pointer exception should be thrown since get method returns a primitive integer.");
        }
        catch (NullPointerException ex)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that if an exception is thrown if the unset and setter methods do not exists for a field.
     */
    @Test
    public void testUnsetAndSetterFieldDoesNotExist() throws ReflectionsUtilException
    {
        try
        {
            ReflectionsUtil.unsetField(m_Obj, "DoesNotExist");
            fail("Neither unset nor setter methods exists for specified field therefore a NoSuchMethodException should"
                    + "be thrown.");
        }
        catch (final ReflectionsUtilException exception)
        {
            //Expected Exception
        }
    }
    
    /**
     * Verify that the correct boolean value is returned for the class types passed in.
     */
    @Test
    public void testIsPrimitiveOrWrapper()
    {
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(int.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(Boolean.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(Integer.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(Double.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(Float.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(Long.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(Short.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(Byte.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(Character.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(String.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(TestEnum.class), is(true));
        assertThat(ReflectionsUtil.isPrimitiveOrWrapper(TestObject.class), is(false));
    }
    
    /**
     * Verify that the correct boolean is returned for the specified field.
     */
    @Test
    public void testHasJaxbAnnotation() throws SecurityException, NoSuchFieldException
    {
        Field xmlAttributeField = TestClassXmlAnnotations.class.getDeclaredField("m_XmlAttribute");
        assertThat(ReflectionsUtil.hasJaxbAnnotation(xmlAttributeField), is(true));
        
        Field xmlElementField = TestClassXmlAnnotations.class.getDeclaredField("m_XmlElement");
        assertThat(ReflectionsUtil.hasJaxbAnnotation(xmlElementField), is(true));
        
        Field xmlValueField = TestClassXmlAnnotations.class.getDeclaredField("m_XmlValue");
        assertThat(ReflectionsUtil.hasJaxbAnnotation(xmlValueField), is(true));
    }
    
    /**
     * Verify that a field can be retrieve from an object even if that field is inherited.
     */
    @Test
    public void testRetrieveField()
    {
        Field field = ReflectionsUtil.retrieveField("DoseNotExist", m_Obj);
        assertThat(field, is(nullValue()));
        
        field = ReflectionsUtil.retrieveField("m_Integer", m_Obj);
        assertThat(field, is(notNullValue()));
        assertThat(field.getType(), is((Object)Integer.class));
        
        field = ReflectionsUtil.retrieveField("m_Boolean", m_Obj);
        assertThat(field, is(notNullValue()));
        assertThat(field.getType(), is((Object)Boolean.class));
        
        field = ReflectionsUtil.retrieveField("m_Float", m_Obj);
        assertThat(field, is(notNullValue()));
        assertThat(field.getType(), is((Object)Float.class));
    }
    
    /**
     * Test class to in case that there are no methods.
     */
    private class TestClassNoMethods
    {
        @SuppressWarnings("unused") // called through reflection
        private int m_PrivateVar;
        
        TestClassNoMethods()
        {
            m_PrivateVar = 1;
        }
    }
    
    /**
     * Test class where fields contain XML annotations.
     */
    private class TestClassXmlAnnotations
    {
        @XmlAttribute
        private String m_XmlAttribute;
        
        @XmlElement
        private String m_XmlElement;
        
        @XmlValue
        private String m_XmlValue;
    }
    
    /**
     * Test class object that has some simple methods that can be retrieved.
     */
    private class TestObject extends SuperTestClass
    {
        private Integer m_Integer;
        
        private Double m_Double;
        
        TestObject()
        {
            m_Integer = 1;
            m_Double = 5.25;
        }
        
        public int getTestInteger()
        {
            return m_Integer;
        }
        
        public Double getTestDouble()
        {
            return m_Double;
        }
        
        @SuppressWarnings("unused") // called through reflection
        public boolean isTestBooleanType()
        {
            return false;
        }
        
        @SuppressWarnings("unused") // called through reflection
        public Boolean isTestBooleanObject()
        {
            return true;
        }
        
        @SuppressWarnings("unused") // called through reflection
        public void setTestInteger(int val)
        {
            m_Integer = val;
        }
        
        @SuppressWarnings("unused")
        public void setTestDouble(Double val)
        {
            m_Double = val;
        }
        
        @SuppressWarnings("unused")
        public boolean isSetTestInteger()
        {
            if (m_Integer == null)
            {
                return false;
            }
            return true;
        }
        
        @SuppressWarnings("unused")
        public void unsetTestInteger()
        {
            m_Integer = null;
        }
        
        @SuppressWarnings("unused")
        public void isSetException()
        {
            throw new IllegalArgumentException("ERROR!");
        }
        
        @SuppressWarnings("unused")
        public void unsetException()
        {
            throw new IllegalArgumentException("ERROR!");
        }
    }
    
    /**
     * Super class extended by the test object.
     */
    private class SuperTestClass extends SuperSuperTestClass
    {
        @SuppressWarnings("unused")
        private Boolean m_Boolean;
    }
    
    /**
     * Super class extended by the super class.
     */
    private class SuperSuperTestClass
    {
        @SuppressWarnings("unused")
        private Float m_Float;
    }
    
    /**
     * Simple enumeration used for testing.
     */
    private enum TestEnum
    {
        value1,
        value2,
        value3;
    }
}
