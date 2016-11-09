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

package mil.dod.th.core.converter;

import java.util.Calendar;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * XmlAdatper class used by JAXB to bind a xsd:dateTime to a java.lang.Long object.
 * 
 * @author ssetter
 * @version 1.0
 */
public class DateXmlConverter extends XmlAdapter<String, Long>
{
    @Override
    public Long unmarshal(final String dateTime)
    {
        return Long.valueOf(DatatypeConverter.parseDateTime(dateTime).getTimeInMillis());
    }

    @Override
    public String marshal(final Long timeInMillis)
    {
        if (timeInMillis == null)
        {
            return null;
        }
        
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        return DatatypeConverter.printDateTime(calendar);
    }
}