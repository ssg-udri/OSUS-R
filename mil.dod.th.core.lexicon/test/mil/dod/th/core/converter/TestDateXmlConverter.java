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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

public class TestDateXmlConverter
{
    private final String m_DateTimeString = "2011-09-28T12:28:55.180-04:00";
    private final Long m_TimeInMillis = 1317227335180L;

    @Test
    public void testDateXmlConverter()
    {
        final DateXmlConverter converter = new DateXmlConverter();
        assertThat(converter, is(notNullValue()));
        
        final String xsdDateTimeString = converter.marshal(m_TimeInMillis);
        assertThat(xsdDateTimeString, is(m_DateTimeString));
        
        final Long recoveredTimeInMillis = converter.unmarshal(m_DateTimeString);
        assertThat(recoveredTimeInMillis, is(m_TimeInMillis));
    }

}
