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

package mil.dod.th.ose.shared.protoconverter;

import aQute.bnd.annotation.component.Component;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;

/**
 * This class has the capability to convert a JAXB object to a proto message and vice versa.
 * 
 * @author cweisenborn
 */
@Component
public class JaxbProtoObjectConverterImpl implements JaxbProtoObjectConverter
{   
    @Override
    public Message convertToProto(final Object object) throws ObjectConverterException
    {   
        return JaxbConverter.convertToProto(object);
    }

    @Override
    public Object convertToJaxb(final Message message) throws ObjectConverterException
    {
        return MessageConverter.convertToJaxbRec(message);
    }
}
