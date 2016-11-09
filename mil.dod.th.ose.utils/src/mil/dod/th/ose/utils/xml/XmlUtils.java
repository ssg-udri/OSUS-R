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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility methods for jaxb object to xml output.
 * 
 */
public final class XmlUtils
{
    /**
     * Map of classes and respective JAXBContexts.
     */
    private static Map<Class<?>, JAXBContext> serMap = new HashMap<Class<?>, JAXBContext>();
    
    /**
     * Constructor.
     */
    private XmlUtils()
    {
        
    }
    
    /**
     * Gets the JAXBContext for the passed in class.
     * @param theClass
     *  the class to retrieve the JAXBContext for
     * @return
     *  the JAXBContext that has been found or created
     * @throws JAXBException
     *  throws a JAXBException if context cannot be created
     */
    private static JAXBContext getContext(final Class<?> theClass) throws JAXBException
    {
        if (serMap.containsKey(theClass))
        {
            return serMap.get(theClass);
        }
        else
        {
            final JAXBContext jContext = JAXBContext.newInstance(theClass);
            serMap.put(theClass, jContext);
            return jContext;
        }
    }
    
    /**
     * Converts object to XML and returns the byte array of the XML text created.
     * @param obj 
     *  the object to be converted into its XML format
     * @param prettyPrint
     *  specifies whether or not to set property JAXB_FORMATTED_OUTPUT on the marshaller
     * @return
     *  byte array which represents the XML output for the object passed in
     */
    public static byte[] toXML(final Object obj, final boolean prettyPrint)
    {
        return toXML(obj, prettyPrint, null);
    }
    
    
    /**
     * Converts object to XML and returns the byte array of the XML text created.
     * @param obj
     *  the object to be converted into its XML format
     * @param prettyPrint
     *  specifies whether or not to set property JAXB_FORMATTED_OUTPUT on the marshaller
     * @param rootElementName
     *  if object does not have a root element annotation a root element name must be specified
     * @return
     *  byte array which represents the XML output for the object passed in 
     */
    public static byte[] toXML(final Object obj, final boolean prettyPrint, final String rootElementName)
    {
        try
        {
            final Class<?> theClass = obj.getClass();
            
            final JAXBContext context = getContext(theClass);
            
            final Marshaller marshaller = context.createMarshaller();
            
            if (prettyPrint)
            {
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            }
            
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            
            if (rootElementName == null)
            {
                marshaller.marshal(obj, byteStream);
            }
            else
            {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                final JAXBElement jaxbElement = new JAXBElement(new QName(rootElementName), theClass, obj);
                
                marshaller.marshal(jaxbElement, byteStream);
            }
            
            return byteStream.toByteArray();
        }
        catch (final JAXBException exception)
        {
            throw new IllegalStateException(exception);
        }
    }
    
    /**
     * Converts an object to an Element object. 
     * @param obj
     *  the object to be converted
     * @return
     *  the DOM Element object
     */
    public static Element toXMLElement(final Object obj)
    {
        try
        {
            final Class<?> theClass = obj.getClass();
            
            final JAXBContext context = getContext(theClass);
            
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            
            factory.setNamespaceAware(true);
            
            final DocumentBuilder builder = factory.newDocumentBuilder();
            
            final Document doc = builder.newDocument();
            
            final Marshaller marshaller = context.createMarshaller();
            
            marshaller.marshal(obj, doc);
            
            return doc.getDocumentElement();
            
        }
        catch (final Exception exception)
        {
            throw new IllegalStateException(exception);
        }
    }
    
    /**
     * Converts byte data representing XML of a JAXB object to an object.
     * @param data
     *  the data to be converted to an object
     * @param type
     *  the class that is to be created and instantiated from the data
     * @return
     *  the object that was derived from the XML data
     */
    public static Object fromXML(final byte[] data, final Class<?> type)
    {
        try
        {
            final JAXBContext context = getContext(type);
            
            final Unmarshaller unMarshal = context.createUnmarshaller();
            
            final JAXBElement<?> temp = unMarshal.unmarshal(new StreamSource(new ByteArrayInputStream(data)), type);
            
            return temp.getValue();
        }
        catch (final JAXBException exception)
        {
            throw new IllegalStateException(exception);
        }
    }    
}
