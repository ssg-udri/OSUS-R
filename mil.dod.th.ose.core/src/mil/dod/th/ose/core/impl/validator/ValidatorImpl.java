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
package mil.dod.th.ose.core.impl.validator;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.ose.core.xml.JAXBContextFactory;
import mil.dod.th.ose.core.xml.XsdResourceFinder;
import mil.dod.th.ose.utils.xml.XmlService;

import org.osgi.framework.BundleContext;

import org.xml.sax.SAXException;

/**
 * Implementation of validator.
 */
@Component
public class ValidatorImpl implements mil.dod.th.core.validator.Validator
{
    /**
     * Schema Factory.
     */
    private SchemaFactory m_SchemaFactory;
    
    /**
     * Map of Validators keyed by their respective classes.
     */
    private final Map<Class<?>, Validator> m_ClassValidators =
            Collections.synchronizedMap(new HashMap<Class<?>, Validator>());

    /**
     * JAXB Context.
     */
    private JAXBContextFactory m_JAXBContextFactory;

    /**
     * Service for accessing core XML functionality, so static methods are not called.
     */
    private XmlService m_XmlService;

    /**
     * Service to produce XML from JAXB objects.
     */
    private XmlMarshalService m_XMLMarshallService;
    
    /**
     * Context of the bundle containing this component.
     */
    private BundleContext m_Context;
    

    /**
     * Bind the service.
     * 
     * @param jaxbContextFactory
     *      service being bound to this component
     */
    @Reference
    public void setJAXBContextFactory(final JAXBContextFactory jaxbContextFactory)
    {
        m_JAXBContextFactory = jaxbContextFactory;
    }

    /**
     * Bind the service.
     * 
     * @param xmlService
     *      service being bound to this component
     */
    @Reference
    public void setXmlService(final XmlService xmlService)
    {
        m_XmlService = xmlService;
    }

    /**
     * Bind the service.
     * 
     * @param xmlMarshallService
     *      service being bound to this component
     */
    @Reference
    public void setXMLMarshaller(final XmlMarshalService xmlMarshallService)
    {
        m_XMLMarshallService = xmlMarshallService;
    }
    
    /**
     * Activate the component.
     * 
     * @param context
     *      context of the bundle containing this component
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
        m_SchemaFactory = m_XmlService.createSchemaFactory(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    }
    
    @Override
    public void validate(final Object object) throws ValidationFailedException
    {
        Preconditions.checkNotNull(object);
        
        final Source source = createSource(object);

        try
        {
            validateSource(source, object);
        }
        catch (final IllegalStateException e)
        {
            throw new ValidationFailedException(e);
        }
    }

    /**
     * Creates a new JAXBSource from the object and the JAXBContext.
     * 
     * @param object
     *            the specified object
     * @return a JAXBSource instance if successful, else null
     * @throws ValidationFailedException
     *             if JAXBSource creation fails
     */
    private Source createSource(final Object object)
            throws ValidationFailedException
    {
        Object objectContainingSource = object;
        if (object instanceof Command || object instanceof Response)
        {
            final XmlType xmlType = object.getClass().getAnnotation(XmlType.class);
            final QName qName = new QName(xmlType.namespace(),
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, xmlType.name()));
            @SuppressWarnings({"rawtypes", "unchecked"})
            final JAXBElement jaxbElementWrapper = new JAXBElement(qName, object.getClass(), null, object);
            //now that the data is wrapped to create root element the source can be properly created
            objectContainingSource = jaxbElementWrapper;
        }
        try
        {
            return m_XmlService.createJAXBSource(m_JAXBContextFactory.getContext(object.getClass()), 
                    objectContainingSource);
        }
        catch (final JAXBException exception)
        {
            throw new ValidationFailedException(
                String.format("Could not create JAXBSource for %s", object.getClass()), exception);
        }
    }

    /**
     * Private helper method for validation.
     * 
     * @param source
     *            transform source reference
     * @param object
     *            JAXB object to validate
     * @throws ValidationFailedException
     *             if validation, or parsing of XML fails
     * @throws IllegalStateException
     *             if the class doesn't have an associated XSD file
     */
    private void validateSource(final Source source,
                                final Object object)
            throws ValidationFailedException, IllegalStateException
    {
        final Validator validator = getSchemaValidator(object.getClass());
        
        try
        {
            synchronized (validator)
            {
                validator.validate(source);
            }
        }
        catch (final SAXException exception)
        {
            final StringBuilder messageBuilder = new StringBuilder("Problem validating XML");
            
            try
            {
                final byte[] bytes = m_XMLMarshallService.createXmlByteArray(object, true);
                messageBuilder.append(":\n").append(new String(bytes));
            }
            catch (final MarshalException e)
            {
                messageBuilder.append(" (and unable to marshall to XML: ");
                messageBuilder.append(e.getMessage());
                messageBuilder.append(")");
            } 
            
            throw new ValidationFailedException(messageBuilder.toString(), exception);
        }
        catch (final IOException exception)
        {
            throw new ValidationFailedException("Problem parsing XML", exception);
        }
    }

    /**
     * Helper method for validator creation based on the specified schema xsd.
     * 
     * @param clazz
     *      class to get the validator for
     * @return
     *      the created schema validator
     * @throws ValidationFailedException
     *      if unable to create a schema file, which is needed to create its associated validator
     * @throws IllegalStateException
     *      if the class doesn't have an associated XSD file
     */
    private synchronized Validator getSchemaValidator(final Class<?> clazz) throws ValidationFailedException, 
            IllegalStateException
    {
        final Validator validator = m_ClassValidators.get(clazz);
        if (validator != null)
        {
            // already cached, just return it
            return validator;
        }
        
        final URL resource = XsdResourceFinder.getXsdResource(m_Context, clazz);
        
        try
        {
            final Validator newValidator = m_SchemaFactory.newSchema(resource).newValidator();
            m_ClassValidators.put(clazz, newValidator);
            return newValidator;
        }
        catch (final SAXException exception)
        {
            throw new ValidationFailedException("Could not create schema: " + resource, exception);
        }
    }
}
