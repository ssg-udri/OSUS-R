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

import aQute.bnd.annotation.ProviderType;

import com.google.common.base.Strings;

/**
 * General utility class for integers.
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public final class Integers
{
    /**
     * Private constructor to prevent instantiation.
     */
    private Integers()
    {
        
    }
    
    /**
     * Parse a string as either a hexadecimal or decimal integer. If the value starts with '0x' it is treated as 
     * hexadecimal, otherwise it is treated as decimal.
     * 
     * @param value
     *      integer string to parse, values larger than an int will result in unpredictable behavior
     * @return
     *      parsed value
     * @throws IllegalArgumentException
     *      if value is null or an empty string
     */
    public static int parseHexOrDecimal(final String value) throws IllegalArgumentException
    {
        if (Strings.isNullOrEmpty(value))
        {
            throw new IllegalArgumentException("Unable to parse null or empty value");
        }
        
        if (value.startsWith("0x"))
        {
            // assume hex
            final int hexRadix = 16;
            
            final int maxLength = 10; // '0x' plus 2 characters for each byte
            if (value.length() > maxLength)
            {
                throw new IllegalArgumentException("Hexadecimal value is too large");
            }
            
            return (int)Long.parseLong(value.substring(2), hexRadix);
        }
        else
        {
            return Integer.parseInt(value);
        }
    }
}
