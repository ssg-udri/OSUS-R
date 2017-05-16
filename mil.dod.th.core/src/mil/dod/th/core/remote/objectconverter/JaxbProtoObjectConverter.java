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
package mil.dod.th.core.remote.objectconverter;

import aQute.bnd.annotation.ProviderType;

import com.google.protobuf.Message;

/**
 * This interface is responsible for converting JAXB objects to proto messages.
 * 
 * This is an OSGi service provided by the core and may be obtained by getting an OSGi service reference or using
 * declarative services.
 * 
 * @author cweisenborn
 */
@ProviderType
public interface JaxbProtoObjectConverter
{    
    /**
     * Method responsible for converting a JAXB object into a proto message.
     * 
     * @param object
     *          The JAXB object to be converted to a proto message.
     * @return The {@link Message} created  from the JAXB object.
     * @throws ObjectConverterException
     *          When the object is unable to be converted to a proto message.
     */
    Message convertToProto(Object object) throws ObjectConverterException;
    
    /**
     * Method responsible for converting a proto message into the corresponding JAXB object.
     * 
     * @param message
     *          {@link Message} to be converted into an object.
     * @return
     *          The {@link Object} created from the converted message.
     * @throws ObjectConverterException
     *          When the message cannot be converted into an object.
     */
    Object convertToJaxb(Message message) throws ObjectConverterException;
}
