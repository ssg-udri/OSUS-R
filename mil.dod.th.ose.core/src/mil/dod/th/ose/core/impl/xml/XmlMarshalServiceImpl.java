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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.ose.core.xml.JAXBContextFactory;

/**
 * Implementation class for marshaling XML documents.
 * 
 * @author cweisenborn
 */
@Component
public class XmlMarshalServiceImpl implements XmlMarshalService
{
    /**
     * Reference to the service used to create XML context for marshaling XML.
     */
    private JAXBContextFactory m_JaxbFactory;
    
    /**
     * Instantiates member JAXBContextFactory variable.
     * 
     * @param factory 
     *          JAXBContextFactory object to be set
     */
    @Reference
    public void setJAXBContextFactory(final JAXBContextFactory factory)
    {
        this.m_JaxbFactory = factory;
    }
    
    @Override
    public byte[] createXmlByteArray(final Object object, final boolean enablePrettyPrint)
            throws MarshalException
    {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try
        {
            final Marshaller marshaller = m_JaxbFactory.getContext(object.getClass()).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, enablePrettyPrint);
            marshaller.marshal(object, byteStream);
            byteStream.close();
            return byteStream.toByteArray();
        }
        catch (final JAXBException e)
        {
            throw new MarshalException("Object cannot be converted to an XML file - Marshal Error", e);
        }
        catch (final IOException e)
        {
            throw new MarshalException("Byte array output stream cannot be closed.", e);
        }
    }
}
