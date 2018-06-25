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
package mil.dod.th.ose.core.impl.xml;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.core.xml.JAXBContextFactory;
import mil.dod.th.ose.core.xml.XsdResourceFinder;
import mil.dod.th.ose.utils.xml.XmlService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.xml.sax.SAXException;

/**
 * Implementation class for unmarshalling XML documents.
 * @author bachmakm
 *
 */
@Component
public class XmlUnmarshalServiceImpl implements XmlUnmarshalService
{
    /**
     * String variable used when there is an unmarshal error.
     */
    private static final String UNMARSHAL_ERROR = "Cannot unmarshal the XML specified - Unmarshal Error";
    
    /**
     * String variable used when there is a schema error.
     */
    private static final String SCHEMA_ERROR = "Cannot unmarshal the XML specified - Schema Error";
    
    /**
     * Variable used to create XML context for unmarshalled XML.
     */
    private JAXBContextFactory m_JaxbFactory;
    
    /**
     * Variable used to maintain the bundle context associated with the bundle.
     */
    private BundleContext m_BundleContext;
    
    /**
     * Service for accessing core XML functionality, so static methods are not called.
     */
    private XmlService m_XmlService;
    
    /**
     * Bind the service.
     * 
     * @param xmlService
     *      service to bind to the component
     */
    @Reference
    public void setXmlService(final XmlService xmlService)
    {
        m_XmlService = xmlService;
    }
    
    /**
     * This method stores the bundle context from the core.  
     * @param context
     *      bundle context of the core
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_BundleContext = context;
    }


    /**
     * Instantiates member JAXBContextFactory variable.
     * @param jaxbContextFactory
     *      expected JAXBContextFactory object
     */
    @Reference
    public void setJAXBContextFactory(final JAXBContextFactory jaxbContextFactory)
    {
        this.m_JaxbFactory = jaxbContextFactory;
    }

    @Override
    public URL getXmlResource(final Bundle bundle, final String xmlFolderName, final String className) 
            throws IllegalArgumentException
    {        
        final String myXmlPath = xmlFolderName + "/" + className + ".xml";
        final URL entry = bundle.getEntry(myXmlPath);
        if (entry == null)
        {
            throw new IllegalArgumentException(
                    String.format("Unable to find a resource for the XML path [%s] in bundle [%s]", myXmlPath, bundle));
        }
        return entry;            
  
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getXmlObject(final Class<T> clazz,  final URL xmlResource) throws UnmarshalException
    {  
        final Unmarshaller myUnmarshal = getUnmarshaller(clazz);
        try
        {
            return (T)myUnmarshal.unmarshal(xmlResource);
        }
        catch (final JAXBException e)
        {
            throw new UnmarshalException(UNMARSHAL_ERROR, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getXmlObject(final Class<T> clazz, final InputStream stream) throws UnmarshalException
    {
        final Unmarshaller myUnmarshal = getUnmarshaller(clazz);
        try
        {
            return (T)myUnmarshal.unmarshal(stream);
        }
        catch (final JAXBException e)
        {
            throw new UnmarshalException(UNMARSHAL_ERROR, e);
        }
    }


    @Override
    public <T> T getXmlObject(final Class<T> clazz, final byte[] xmlData) throws UnmarshalException
    {
        // translate array into stream so it can be consumed by the unmarshaller
        final InputStream stream = new ByteArrayInputStream(xmlData);
        
        return getXmlObject(clazz, stream);
    }
    
    /**
     * Get the unmarshaller for the given class.
     * 
     * @param clazz
     *      class to unmarshal
     * @return
     *      the unmarshaller for the given class
     * @throws UnmarshalException
     *      if unable to get a unmarshaller due to an issue with the class or schema, check cause
     * @throws IllegalStateException
     *      if the class given doesn't have an associated XSD file
     */
    private Unmarshaller getUnmarshaller(final Class<?> clazz) throws UnmarshalException, IllegalStateException
    {
        try
        {
            final Unmarshaller myUnmarshal = m_JaxbFactory.getContext(clazz).createUnmarshaller();
            //validate xml before it is unmarshalled
            final Schema schema = m_XmlService.createSchemaFactory(XMLConstants.W3C_XML_SCHEMA_NS_URI).
                    newSchema(XsdResourceFinder.getXsdResource(m_BundleContext, clazz));
            myUnmarshal.setSchema(schema);
            return myUnmarshal;
        }
        catch (final JAXBException e)
        {
            throw new UnmarshalException(UNMARSHAL_ERROR, e);
        }
        catch (final SAXException e)
        {
            throw new UnmarshalException(SCHEMA_ERROR, e);
        }
    }

}
