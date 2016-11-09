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
package mil.dod.th.ose.utils.numbers;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

public class TestIntegers
{
    /**
     * Test hex strings.
     */
    @Test
    public void testParseHexOrDecimal_Hex()
    {
        assertThat(Integers.parseHexOrDecimal("0xabcdefab"), is(0xabcdefab));
        assertThat(Integers.parseHexOrDecimal("0xffffffff"), is(0xffffffff));
        assertThat(Integers.parseHexOrDecimal("0x12"), is(0x12));
    }

    /**
     * Test decimal string.
     */
    @Test
    public void testParseHexOrDecimal_Decimal()
    {
        assertThat(Integers.parseHexOrDecimal("12345"), is(12345));
    }

    /**
     * Test invalid hex string.
     */
    @Test
    public void testParseHexOrDecimal_BadHex()
    {
        try
        {
            Integers.parseHexOrDecimal("0xabcdeij");
            fail("Expecting exception");
        }
        catch (NumberFormatException e)
        {
            
        }
    }
    
    /**
     * Test invalid decimal string.
     */
    @Test
    public void testParseHexOrDecimal_BadDec()
    {
        try
        {
            Integers.parseHexOrDecimal("abcdefab");
            fail("Expecting exception");
        }
        catch (NumberFormatException e)
        {
            
        }
    }
    
    /**
     * Test empty or null values.
     */
    @Test
    public void testParseHexOrDecimal_Empty()
    {
        try
        {
            Integers.parseHexOrDecimal(null);
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        try
        {
            Integers.parseHexOrDecimal("");
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
     * Test value that is too large for an int.
     */
    @Test
    public void testParseHexOrDecimal_TooLong()
    {
        try
        {
            Integers.parseHexOrDecimal("0x100000000");
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
}
