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

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * Converts a Long into a display for bytes/kilobytes/megabytes/gigabytes. If the value is greater than or equal to 1024
 * (bytes/kb/etc), it will be shown in the next highest unit. So 1024 bytes will be shown as 1 KB, 1024 KB will be shown
 * as 1 MB.
 * 
 * @author Dave Humeniuk
 */
@FacesConverter("bytesConverter")
public class BytesConverter implements Converter
{

    @Override
    public Object getAsObject(final FacesContext context, final UIComponent component, final String value)
    {
        throw new UnsupportedOperationException("Converter only converts Longs to Strings.");
    }

    @Override
    public String getAsString(final FacesContext context, final UIComponent component, final Object value)
    {
        final int divisor = 1024;
        
        final Long bytes = (Long)value;
        final Long kilobytes = bytes / divisor;
        final Long megabytes = kilobytes / divisor;
        final Long gigabytes = megabytes / divisor;
        
        if (bytes < divisor)
        {
            return bytes + " B";
        }
        else if (kilobytes < divisor)
        {
            return kilobytes + " KB";
        }
        else if (megabytes < divisor)
        {
            return megabytes + " MB";
        }
        else if (gigabytes < divisor)
        {
            return gigabytes + " GB";
        }
        else
        {
            final Long terabytes = gigabytes / divisor;
            return terabytes + " TB";
        }
    }

}
