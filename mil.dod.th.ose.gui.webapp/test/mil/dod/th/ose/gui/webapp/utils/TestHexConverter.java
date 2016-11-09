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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import javax.faces.convert.ConverterException;

import org.junit.Test;

/**
 * Testing the hex converter class which takes an integer value and converts it to hexadecimal then converts hexadecimal
 * values back into their integer value.
 * @author matt
 */
public class TestHexConverter
{
    /**
     * Test that the converter will properly convert an integer to a string with the correct units.
     */
    @Test
    public void testGetAsString()
    {
        HexConverter hexConverter = new HexConverter();
        
        String hex = hexConverter.getAsString(null, null, 123);
        
        assertThat(hex, is("0x0000007B"));
    }
    
    /**
     * Test that the converter correctly converts a full string.
     */
    @Test
    public void testGetAsObject()
    {
        HexConverter hexConverter = new HexConverter();
        
        assertThat((Integer)hexConverter.getAsObject(null, null, "0x0000007B"), is(123));
    }
    
    /**
     * Test that the converter correctly converts a partial string.
     */
    @Test
    public void testHexStringPartial()
    {
        HexConverter hexConverter = new HexConverter();
        
        assertThat((Integer)hexConverter.getAsObject(null, null, "0x7B"), is(123));
    }
    
    /**
     * Test that the converter correctly converts a correctly formated string.
     */
    @Test
    public void testHexStringFormats()
    {
        HexConverter hexConverter = new HexConverter();
        
        String hex = "0xffffffff";
        
        assertThat((Integer)hexConverter.getAsObject(null, null, hex), is(-1));
    }
    
    /**
     * Test that the converter correctly reports not supporting conversion of an invalid formated string
     */
    @Test
    public void testInvalidStringInput()
    {
        HexConverter hexConverter = new HexConverter();
        
        //invalid characters and no prepended 0x indicating it is a hex value
        try
        {
            hexConverter.getAsObject(null, null, "ssksksksaas");
            fail("Expected ConverterException for invalid input.");
        }
        catch(ConverterException exception)
        {
            //expecting exception
            assertThat(exception.getFacesMessage().getSummary(), is("Invalid hexadecimal value."));
        }
        
        //invalid characters for hex value
        try
        {
            hexConverter.getAsObject(null, null, "0x%&@");
            fail("Expected ConverterException for invalid input.");
        }
        catch(ConverterException exception)
        {
            //expecting exception
            assertThat(exception.getFacesMessage().getSummary(), is("Invalid hexadecimal value."));
        }
        
        //user doesn't input anything
        try
        {
            hexConverter.getAsObject(null, null, "0x");
            fail("Expected ConverterException for invalid input."); 
        }
        catch(ConverterException exception)
        {
            //expecting exception
            assertThat(exception.getFacesMessage().getDetail(),
                    is("The System ID field requires at least one character."));
        }
        
        //input field is completely blank (field was cleared using "x" button)
        try
        {
            hexConverter.getAsObject(null, null, "");
            fail("Expected ConverterException for invalid input."); 
        }
        catch(ConverterException exception)
        {
            //expecting exception
            assertThat(exception.getFacesMessage().getDetail(), 
                    is("The System ID field requires at least one character."));
        }
        
        //converted number will be greater than integer value can hold
        try
        {
            hexConverter.getAsObject(null, null, "0x100000000");
            fail("Expected ConverterException for invalid input.");
        }
        catch(ConverterException exception)
        {
            //expecting exception
            assertThat(exception.getFacesMessage().getDetail(), is("The hexadecimal value that was converted to a long"
                    + " data type is greater than a possible integer value."));
        }
    }
}
