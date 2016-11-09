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

import java.util.Date;

import javax.faces.convert.ConverterException;

import org.junit.Before;
import org.junit.Test;

/**
 * Testing the date time converter which takes a long and converts it to a date time string
 * and vice versa.
 * 
 * @author nickmarcucci
 *
 */
public class TestDateTimeConverterUtil
{
    private DateTimeConverterUtil m_SUT;
    
    @Before
    public void init()
    {
        m_SUT = new DateTimeConverterUtil();
    }
    
    @Test
    public void testConvertFromStringToLong()
    {
        Long time = (Long)m_SUT.getAsObject(null, null, "06/10/2010 16:49:49Z");
        assertThat(time, is(1276188589000L));
    }
    
    @Test
    public void testConvertFromLongToString()
    {
        String date = m_SUT.getAsString(null, null, 1276188589000L);
        assertThat(date, is("06/10/2010 16:49:49Z"));
    }
    
    @Test
    public void testInvalidStringToLong()
    {
        try
        {
            m_SUT.getAsObject(null, null, "o boy an exception!");
            fail("Expected ConverterException for invalid date time string.");
        }
        catch(ConverterException exception)
        {
            assertThat(exception.getFacesMessage().getSummary(), 
                    is("Error parsing date string value"));
        }
    }
    
    /**
     * Verify that the date is rounded to last millisecond of the second.
     */
    @Test
    public void testRoundToEndOfSecond()
    {
        //Verify rounding a second to last millisecond.
        Date initialDate = new Date(3421L);
        Date roundedDate = DateTimeConverterUtil.roundToEndOfSecond(initialDate);
        assertThat(roundedDate.getTime(), is(3999L));
        
        initialDate = new Date(3500L);
        roundedDate = DateTimeConverterUtil.roundToEndOfSecond(initialDate);
        assertThat(roundedDate.getTime(), is(3999L));
        
        initialDate = new Date(3999L);
        roundedDate = DateTimeConverterUtil.roundToEndOfSecond(initialDate);
        assertThat(roundedDate.getTime(), is(3999L));
    }
}
