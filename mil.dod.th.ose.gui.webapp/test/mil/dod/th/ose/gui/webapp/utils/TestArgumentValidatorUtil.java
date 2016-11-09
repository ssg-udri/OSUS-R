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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil.ValidationEnum;

import org.junit.Test;

/**
 * Test class for the {@link ArgumentValidatorUtil}.
 * 
 * @author cweisenborn
 */
public class TestArgumentValidatorUtil
{
    /**
     * Verify that the validator will correctly return pass or fail given a range of acceptable values. 
     */
    @Test
    public void testIsValidValue()
    {
        //Test to make sure valid integer returns true.
        Object value = 10;
        String minValue = "5";
        String maxValue = "20";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Integer.class), 
                is(ValidationEnum.PASSED));
        //Test to make sure non-valid integer returns false.
        value = 10.11;
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Integer.class), 
                is(ValidationEnum.PARSING_FAILURE));
        
        //Test to make sure valid double returns true.
        value = 25.55;
        minValue="5.55";
        maxValue = "100.25";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Double.class), 
                 is(ValidationEnum.PASSED));
        //Test to make sure invalid double returns false.
        value = "not a double";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Double.class), 
                is(ValidationEnum.PARSING_FAILURE));
        
        //Test to make sure valid float returns true.
        value = (float) 25.5;
        minValue="5.5";
        maxValue = "100.2";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Float.class), 
                is(ValidationEnum.PASSED));
        //Test to make sure invalid float returns false.
        value = "not a float";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Float.class), 
                is(ValidationEnum.PARSING_FAILURE));
        
        //Test to make sure valid long returns true.
        value = 123456789L;
        minValue="20000";
        maxValue = "987654321";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Long.class), 
                is(ValidationEnum.PASSED));
        //Test to make sure invalid long returns false.
        value = "not a long";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Long.class), 
                is(ValidationEnum.PARSING_FAILURE));
        
        //Test to make sure valid short returns true.
        value = (short) 2312;
        minValue="2000";
        maxValue = null;
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Short.class), 
                is(ValidationEnum.PASSED));
        //Test to make sure invalid short returns false.
        value = "75000";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Short.class), 
                is(ValidationEnum.PARSING_FAILURE));
        
        //Test to make sure valid byte returns true.
        value = (byte) 100;
        minValue= "-25";
        maxValue = null;
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Byte.class), 
                is(ValidationEnum.PASSED));
        //Test to make sure invalid short returns false.
        value = "129";
        assertThat(ArgumentValidatorUtil.isValidValue(value.toString(), minValue, maxValue, Byte.class), 
                is(ValidationEnum.PARSING_FAILURE));
    }
    
    /**
     * Verify that the validator will return the correct response if an Integer value is greater than or less than the 
     * range of acceptable values. 
     */
    @Test
    public void testCheckInteger()
    {
        Integer value = 10;
        String minValue = "5";
        String maxValue = "20";
        
        //Check valid value returns true.
        assertThat(ArgumentValidatorUtil.checkInteger(value, minValue, maxValue), is (ValidationEnum.PASSED));
        
        //Check value below minimum returns false.
        value = 1;
        assertThat(ArgumentValidatorUtil.checkInteger(value, minValue, maxValue), is (ValidationEnum.LESS_THAN_MIN));
        
        //Check value above maximum returns false.
        value = 55;
        assertThat(ArgumentValidatorUtil.checkInteger(value, minValue, maxValue), is (ValidationEnum.GREATER_THAN_MAX));
        
        //Check no min or max set returns true.
        minValue = null;
        maxValue = null;
        assertThat(ArgumentValidatorUtil.checkInteger(value, minValue, maxValue), is (ValidationEnum.PASSED));
    }
    
    /**
     * Verify that the validator will return the correct response if a Double value is greater than or less than the 
     * range of acceptable values. 
     */
    @Test
    public void testCheckDouble()
    {
        Double value = 10.55;
        String minValue = "5.11";
        String maxValue = "20.62";
        
        //Check valid value returns true.
        assertThat(ArgumentValidatorUtil.checkDouble(value, minValue, maxValue), is (ValidationEnum.PASSED));
        
        //Check value below minimum returns false.
        value = 1.21;
        assertThat(ArgumentValidatorUtil.checkDouble(value, minValue, maxValue), is (ValidationEnum.LESS_THAN_MIN));
        
        //Check value above maximum returns false.
        value = 55.78;
        assertThat(ArgumentValidatorUtil.checkDouble(value, minValue, maxValue), is (ValidationEnum.GREATER_THAN_MAX));
        
        //Check no min or max set returns true.
        minValue = null;
        maxValue = null;
        assertThat(ArgumentValidatorUtil.checkDouble(value, minValue, maxValue), is (ValidationEnum.PASSED));
    }
    
    /**
     * Verify that the validator will return the correct response if a Float value is greater than or less than the 
     * range of acceptable values. 
     */
    @Test
    public void testCheckFloat()
    {
        Float value = 10.5f;
        String minValue = "5.1";
        String maxValue = "20.6";
        
        //Check valid value returns true.
        assertThat(ArgumentValidatorUtil.checkFloat(value, minValue, maxValue), is (ValidationEnum.PASSED));
        
        //Check value below minimum returns false.
        value = 1.2f;
        assertThat(ArgumentValidatorUtil.checkFloat(value, minValue, maxValue), is (ValidationEnum.LESS_THAN_MIN));
        
        //Check value above maximum returns false.
        value = 55.7f;
        assertThat(ArgumentValidatorUtil.checkFloat(value, minValue, maxValue), is (ValidationEnum.GREATER_THAN_MAX));
        
        //Check no min or max set returns true.
        minValue = null;
        maxValue = null;
        assertThat(ArgumentValidatorUtil.checkFloat(value, minValue, maxValue), is (ValidationEnum.PASSED));
    }
    
    /**
     * Verify that the validator will return the correct response if a Long value is greater than or less than the 
     * range of acceptable values. 
     */
    @Test
    public void testCheckLong()
    {
        Long value = 10212324L;
        String minValue = "50000";
        String maxValue = "10213802380222";
        
        //Check valid value returns true.
        assertThat(ArgumentValidatorUtil.checkLong(value, minValue, maxValue), is (ValidationEnum.PASSED));
        
        //Check value below minimum returns false.
        value = 1000L;
        assertThat(ArgumentValidatorUtil.checkLong(value, minValue, maxValue), is (ValidationEnum.LESS_THAN_MIN));
        
        //Check value above maximum returns false.
        value = 9999999999999999L;
        assertThat(ArgumentValidatorUtil.checkLong(value, minValue, maxValue), is (ValidationEnum.GREATER_THAN_MAX));
        
        //Check no min or max set returns true.
        minValue = null;
        maxValue = null;
        assertThat(ArgumentValidatorUtil.checkLong(value, minValue, maxValue), is (ValidationEnum.PASSED));
    }
    
    /**
     * Verify that the validator will return the correct response if a Short value is greater than or less than the 
     * range of acceptable values. 
     */
    @Test
    public void testCheckShort()
    {
        Short value = 1021;
        String minValue = "500";
        String maxValue = "30000";
        
        //Check valid value returns true.
        assertThat(ArgumentValidatorUtil.checkShort(value, minValue, maxValue), is (ValidationEnum.PASSED));
        
        //Check value below minimum returns false.
        value = 100;
        assertThat(ArgumentValidatorUtil.checkShort(value, minValue, maxValue), is (ValidationEnum.LESS_THAN_MIN));
        
        //Check value above maximum returns false.
        value = 32767;
        assertThat(ArgumentValidatorUtil.checkShort(value, minValue, maxValue), is (ValidationEnum.GREATER_THAN_MAX));
        
        //Check no min or max set returns true.
        minValue = null;
        maxValue = null;
        assertThat(ArgumentValidatorUtil.checkShort(value, minValue, maxValue), is (ValidationEnum.PASSED));
    }
    
    /**
     * Verify that the validator will return the correct response if a Byte value is greater than or less than the 
     * range of acceptable values. 
     */
    @Test
    public void testCheckByte()
    {
        Byte value = 120;
        String minValue = "-25";
        String maxValue = "125";
        
        //Check valid value returns true.
        assertThat(ArgumentValidatorUtil.checkByte(value, minValue, maxValue), is (ValidationEnum.PASSED));
        
        //Check value below minimum returns false.
        value = -75;
        assertThat(ArgumentValidatorUtil.checkByte(value, minValue, maxValue), is (ValidationEnum.LESS_THAN_MIN));
        
        //Check value above maximum returns false.
        value = 127;
        assertThat(ArgumentValidatorUtil.checkByte(value, minValue, maxValue), is (ValidationEnum.GREATER_THAN_MAX));
        
        //Check no min or max set returns true.
        minValue = null;
        maxValue = null;
        assertThat(ArgumentValidatorUtil.checkByte(value, minValue, maxValue), is (ValidationEnum.PASSED));
    }
    
    /**
     * Verify that value's actual types are correctly converted from a string when the type is given. 
     */
    @Test
    public void testConvertToActualType()
    {
        assertThat(ArgumentValidatorUtil.convertToActualType("125", Integer.class), is((Object)125));
        assertThat(ArgumentValidatorUtil.convertToActualType("1500", Long.class), is((Object)1500L));
        assertThat(ArgumentValidatorUtil.convertToActualType("12.25", Double.class), is((Object)12.25));
        assertThat(ArgumentValidatorUtil.convertToActualType("122.2", Float.class), is((Object)122.2F));
        assertThat(ArgumentValidatorUtil.convertToActualType("100", Byte.class), is((Object)(byte)100));
        assertThat(ArgumentValidatorUtil.convertToActualType("12", Short.class), is((Object)(short)12));
        assertThat(ArgumentValidatorUtil.convertToActualType("true", Boolean.class), is((Object)true));
        assertThat(ArgumentValidatorUtil.convertToActualType("C", Character.class), is((Object)'C'));
        assertThat(ArgumentValidatorUtil.convertToActualType("test", String.class), is((Object)"test"));
    }
}
