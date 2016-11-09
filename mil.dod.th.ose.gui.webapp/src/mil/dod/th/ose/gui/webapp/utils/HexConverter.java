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

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 * Class to convert an int into a hexadecimal representation, and to convert hexadecimal into an int.
 * format of the string supported is 0x******** with * representing the hex value plus prepended 0s
 * @author matt
 */
@FacesConverter("hexConverter")
public class HexConverter implements Converter
{
    /**
     * Hexadecimal values have a radix of 16.
     */
    static final private int HEX_RADIX = 16;
    
    /**
     * Largest long value to accept for conversion into an int.
     */
    static final private long MAX_VALUE = 0xffffffffL;
    
    @Override
    public Object getAsObject(final FacesContext context, final UIComponent component, final String hexString)
    {
        try
        {
            // If the string input after the 0x is empty
            if (hexString.length() <= 2)
            {
                final FacesMessage emptyMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Invalid System ID", "The System ID field requires at least one character.");
                
                throw new ConverterException(emptyMessage);
            }
            
            // Need to use a long for the initial conversion of the hex string value otherwise it will throw an error
            final Long convertedNumber = Long.valueOf(hexString.substring(2), HEX_RADIX);

            // If the converted number has a value greater than the maximum accepted value for conversion into an int
            if (convertedNumber > MAX_VALUE)
            {
                final FacesMessage maxMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Hexadecimal value is greater than a possible integer value", 
                        "The hexadecimal value that was converted to a long data type is greater than a possible "
                        + "integer value.");
                throw new ConverterException(maxMessage);
            }
            
            return convertedNumber.intValue();
        }
        catch (final NumberFormatException exception)
        {
            final FacesMessage formatMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Invalid hexadecimal value.",
                    "Number format exception was thrown during the conversion of the hexadecimal value into a "
                    + "long data type, this could mean that the hexadecimal value was not in 0xAAAAAAAA format, "
                    + "or that the hexadecimal value has invalid characters.");
            
            throw new ConverterException(formatMessage, exception);
            
        }
    }
    
    @Override
    public String getAsString(final FacesContext context, final UIComponent component, final Object convertObject)
    {
        return String.format("0x%08X", convertObject);
    }
}
