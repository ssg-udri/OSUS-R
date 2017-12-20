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
package mil.dod.th.ose.utils.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.SchemaFactory;

import aQute.bnd.annotation.ProviderType;

/**
 * Service for accessing core Java XML functionality without having to call on static methods.
 * 
 * @author dhumeniuk
 */
@ProviderType
public interface XmlService
{
    /**
     * Calls {@link JAXBSource#JAXBSource(JAXBContext, Object)}.
     * 
     * @param context
     *      JAXBContext that was used to create contentObject. This context is used to create a new instance of 
     *      marshaller and must not be null
     * @param contentObject
     *      An instance of a JAXB-generated class
     * @return
     *      Sources created based on the context and object
     * @throws JAXBException
     *      If there is an error calling the constructor
     */
    JAXBSource createJAXBSource(JAXBContext context, Object contentObject) throws JAXBException;
    
    /**
     * Calls {@link SchemaFactory#newInstance(String)}.
     * 
     * @param schemaLanguage
     *      Specifies the schema language which the returned SchemaFactory will understand.
     * @return
     *      Factory created with the given language
     */
    SchemaFactory createSchemaFactory(String schemaLanguage);
}
