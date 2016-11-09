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

import java.io.InputStream;
import java.net.URL;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.ProviderType;

import org.osgi.framework.Bundle;

/**
 * Class which handles unmarshalling XML documents according to specifications indicated in the corresponding schema.
 * @author bachmakm
 *
 */
@ProviderType
public interface XmlUnmarshalService
{
 
    /**
     * This method is to be used to return an object based on XML documents in a bundle.  
     * @param <T>
     *      type of class corresponding to the type of XML documents being unmarshalled.
     * @param clazz
     *      the class to be recognized for unmarshalling.  The type of the unmarshalled
     *      object will be identical to this class.  
     * @param xmlResource
     *      URL object pointing to location of XML document; needed to 
     *      successfully unmarshal XML.
     * @return an object of type T corresponding to the type of XML docs being unmarshalled. 
     * @throws UnmarshalException
     *      thrown if method cannot unmarshal XML.
     * @throws IllegalStateException
     *      if the class doesn't have a valid XSD file
     */
    <T> T getXmlObject(Class<T> clazz, URL xmlResource) throws UnmarshalException, IllegalStateException;
    
    /**
     * This method is used to return an object based on the XML documents in a bundle.
     * @param <T>
     *      type of class corresponding to the type of XML documents being unmarshalled.
     * @param clazz
     *      the class to be recognized for unmarshalling.  The type of the unmarshalled
     *      object will be identical to this class.
     * @param stream
     *      Stream containing the contents of the file to be unmarshalled.
     * @return an object of type T corresponding to the type of XML docs being unmarshalled.
     * @throws UnmarshalException
     *      thrown if method cannot unmarshal XML
     * @throws IllegalStateException
     *      if the class doesn't have a valid XSD file
     */
    <T> T getXmlObject(Class<T> clazz, InputStream stream) throws UnmarshalException, IllegalStateException;
    
    /**
     * This method is used to return an object based on XML binary data.
     * @param <T>
     *      type of class corresponding to the type of XML documents being unmarshalled.
     * @param clazz
     *      the class to be recognized for unmarshalling.  The type of the unmarshalled
     *      object will be identical to this class.
     * @param xmlData
     *      XML data in binary form
     * @return an object of type T corresponding to the type of XML docs being unmarshalled.
     * @throws UnmarshalException
     *      thrown if method cannot unmarshal XML
     * @throws IllegalStateException
     *      if the class doesn't have a valid XSD file
     */
    <T> T getXmlObject(Class<T> clazz, byte[] xmlData) throws UnmarshalException, IllegalStateException;
    
  /**
   * Returns a URL object corresponding to the location of the XML documents contained within an asset bundle.  
   * @param bundle
   *        the asset bundle (this bundle contains the XML doc).
   * @param xmlFolderName
   *        name of bundle folder where XML is located.
   * @param className
   *        class name associated with a particular bundle; used to identify XML in a bundle
   *        in the event of having multiple classes in a bundle.
   * @return URL
   *        URL representing location of XML file or <code>null</code> if XML file could not be found.  
   * @throws IllegalArgumentException
   *        if the given arguments don't reference an accessible resource
   */
    URL getXmlResource(Bundle bundle, String xmlFolderName, String className) throws IllegalArgumentException;
}
