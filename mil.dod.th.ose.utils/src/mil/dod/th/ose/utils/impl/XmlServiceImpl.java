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
package mil.dod.th.ose.utils.impl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.SchemaFactory;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.ose.utils.xml.XmlService;

/**
 * Implements {@link XmlService}.
 * 
 * @author dhumeniuk
 *
 */
@Component
public class XmlServiceImpl implements XmlService
{
    @Override
    public JAXBSource createJAXBSource(final JAXBContext context, final Object contentObject) throws JAXBException
    {
        return new JAXBSource(context, contentObject);
    }

    @Override
    public SchemaFactory createSchemaFactory(final String schemaLanguage)
    {
        return SchemaFactory.newInstance(schemaLanguage);
    }

}
