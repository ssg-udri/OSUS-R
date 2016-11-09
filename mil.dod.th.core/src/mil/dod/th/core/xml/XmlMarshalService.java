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
package mil.dod.th.core.xml;

import javax.xml.bind.MarshalException;

import aQute.bnd.annotation.ProviderType;

/**
 * Class which handles marshaling XML documents according to specifications indicated in the corresponding schema.
 * 
 * @author cweisenborn
 */
@ProviderType
public interface XmlMarshalService
{
    /**
     * Method used to create an XML file from a JAXB object.
     * 
     * @param object
     *          Object being converted to an XML file.
     * @param enablePrettyPrint
     *          true if marshalled XML data should be formatted with lines and indentations. False if data 
     *          should not be formatted.
     * @return
     *          The byte array that contains the XML data.
     * @throws MarshalException 
     *          If the object is unable to be converted to an XML file
     */
    byte[] createXmlByteArray(Object object, boolean enablePrettyPrint) throws MarshalException;
}
