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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.ose.core.xml.JAXBContextFactory;
import mil.dod.th.ose.utils.xml.XmlService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

import com.google.common.base.CaseFormat;

/**
 * @author jconn
 *
 */
public class TestValidatorImpl
{
    private ValidatorImpl m_SUT;
    private SchemaFactory m_SchemaFactory;
    private Validator m_Validator;
    private Bundle m_Bundle;
    private Schema m_Schema;
    
    class TestClass {}
    
    @Before
    public void setUp()
        throws Exception
    {
        m_SUT = new ValidatorImpl();
        
        JAXBContextFactory jaxbContextFactory = mock(JAXBContextFactory.class);
        m_SUT.setJAXBContextFactory(jaxbContextFactory);
        when(jaxbContextFactory.getContext(TestClass.class)).thenReturn(mock(JAXBContext.class));
        
        URL url = new URL("file:schema.xsd");
        BundleContext context = mock(BundleContext.class);
        m_Bundle = mock(Bundle.class);
        when(context.getBundle()).thenReturn(m_Bundle);
        when(m_Bundle.getResource(anyString())).thenReturn(url);
        
        XmlService xmlService = mock(XmlService.class);
        m_SUT.setXmlService(xmlService);
        when(xmlService.createJAXBSource(Matchers.any(JAXBContext.class), anyObject()))
            .thenReturn(mock(JAXBSource.class));
        m_SchemaFactory = mock(SchemaFactory.class);
        when(xmlService.createSchemaFactory(XMLConstants.W3C_XML_SCHEMA_NS_URI)).thenReturn(m_SchemaFactory);
        m_Schema = mock(Schema.class);
        when(m_SchemaFactory.newSchema(url)).thenReturn(m_Schema);
        m_Validator = mock(Validator.class);
        when(m_Schema.newValidator()).thenReturn(m_Validator);
        
        m_SUT.activate(context);
    }

    /**
     * Test the validate method. Make sure the correct schema is used based on the type being validated.
     */
    @Test
    public void testValidate() throws ValidationFailedException, MalformedURLException, SAXException
    {
        final TestClass data = new TestClass();
        
        m_SUT.validate(data);
        verify(m_Bundle).getResource("schemas/ose/core/impl/validator/TestClass.xsd");
        
        verify(m_SchemaFactory).newSchema(new URL("file:schema.xsd"));
    }
    
    /**
     * Test the validate method will cache the validator and schema.
     */
    @Test
    public void testValidateCached() throws ValidationFailedException, MalformedURLException, SAXException
    {
        final TestClass data = new TestClass();
        
        m_SUT.validate(data);
        m_SUT.validate(data);
        
        verify(m_SchemaFactory, times(1)).newSchema(new URL("file:schema.xsd"));
        verify(m_Schema, times(1)).newValidator();
    }
    
    /**
     * Verify null data is handled properly
     */
    @Test
    public void testNullData() throws ValidationFailedException
    {
        try
        {
            m_SUT.validate(null);
            fail("Expecting exception since object is null");
        }
        catch (NullPointerException exception)
        {
        }
    }

    /**
     * Verify the ValidationFailedException contains object xml. 
     */
    @Test
    public void testObjectXMLOutputOnError() throws SAXException, IOException, MarshalException
    {
        TestClass data = new TestClass();
        doThrow(new SAXException("blah")).when(m_Validator).validate(Matchers.any(Source.class));
        
        XmlMarshalService xmlMarshallService = mock(XmlMarshalService.class);
        m_SUT.setXMLMarshaller(xmlMarshallService);
        when(xmlMarshallService.createXmlByteArray(anyObject(), eq(true))).thenReturn("<blah>".getBytes());
        
        //try to validate this invalid data and catch exception
        //verify that exception method contains object's representation in
        //XML
        try
        {
            m_SUT.validate(data);
            fail("Expecting exception");
        }
        catch (ValidationFailedException exception)
        {
            assertThat(exception.getMessage(), is("Problem validating XML:\n<blah>"));
        }
        
        // verify able to handle situation if XML cannot be created
        when(xmlMarshallService.createXmlByteArray(anyObject(), eq(true)))
            .thenThrow(new MarshalException("marshall exception"));
        
        try
        {
            m_SUT.validate(data);
            fail("Expecting exception");
        }
        catch (ValidationFailedException exception)
        {
            assertThat(exception.getMessage(), 
                     is("Problem validating XML (and unable to marshall to XML: marshall exception)"));
        }
    }
    
    /**
     * Verify that a command is wrapped in a {@link JAXBElement} before being validated.
     */
    @Test
    public void testValidateCommand() throws ValidationFailedException, MalformedURLException, SAXException, 
        JAXBException
    {
        //setup mocking
        XmlService xmlService = mock(XmlService.class);
        m_SUT.setXmlService(xmlService);
        when(xmlService.createJAXBSource(Matchers.any(JAXBContext.class), anyObject()))
            .thenReturn(mock(JAXBSource.class));
        
        final SetPanTiltCommand data = new SetPanTiltCommand();
        
        final XmlType xmlType = data.getClass().getAnnotation(XmlType.class);
        final QName qName = new QName(xmlType.namespace(),
                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, xmlType.name()));
        
        m_SUT.validate(data);
        
        //capture jaxb element wrapping
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<JAXBElement> jaxElementCap = ArgumentCaptor.forClass(JAXBElement.class);
        verify(xmlService).createJAXBSource(any(JAXBContext.class), jaxElementCap.capture());
        
        assertThat(jaxElementCap.getValue().getName(), is(qName));
        
        verify(m_Bundle).getResource("schemas/core/asset/commands/SetPanTiltCommand.xsd");
        
        verify(m_SchemaFactory).newSchema(new URL("file:schema.xsd"));
    }
    
    /**
     * Verify illegal state exception if unable to create jaxb source.
     */
    @Test
    public void testIllegalStateBadSource() throws JAXBException, ValidationFailedException
    {
        //setup mocking
        XmlService xmlService = mock(XmlService.class);
        m_SUT.setXmlService(xmlService);
        when(xmlService.createJAXBSource(Matchers.any(JAXBContext.class), anyObject()))
            .thenThrow(new JAXBException("blah"));
        
        final TestClass data = new TestClass();
        
        try
        {
            m_SUT.validate(data);
            fail("Expected exception because of mocking above");
        }
        catch (ValidationFailedException e)
        {
            //expected
        }
    }
    
    /**
     * Verify illegal state exception if unable create schema.
     */
    @Test
    public void testIllegalStateCantCreateSchema() throws JAXBException, ValidationFailedException, SAXException
    {
        //setup mocking
        m_Schema = mock(Schema.class);
        when(m_SchemaFactory.newSchema(any(URL.class))).thenThrow(new SAXException("blah"));
        
        final TestClass data = new TestClass();
        
        try
        {
            m_SUT.validate(data);
            fail("Expected exception because of mocking above");
        }
        catch (ValidationFailedException e)
        {
            //expected
        }
    }
}
