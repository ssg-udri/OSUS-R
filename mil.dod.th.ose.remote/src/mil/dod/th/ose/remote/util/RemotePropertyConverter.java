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
package mil.dod.th.ose.remote.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.SummaryStatusEnumConverter;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

/**
 * This service assists with converting properties that are complex types into protocol buffer message equivalents.
 * @author callen
 *
 */
@Component(provide = { RemotePropertyConverter.class })
public class RemotePropertyConverter
{
    /**
     * Service that assists in converting from JAXB objects to proto messages.
     */
    private JaxbProtoObjectConverter m_Converter;

    /**
     * The {@link CommandConverter} for command responses embedded within events.
     */
    private CommandConverter m_CommandConverter;

    /**
     * The XML marshal service.
     */
    private XmlMarshalService m_XmlMarshalService;
    
    /**
     * The XML unmarshal service.
     */
    private XmlUnmarshalService m_XmlUnmarshalService;

    @Reference
    public void setJaxbProtoObjectConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;        
    }
    
    /**
     * Set the {@link CommandConverter}.
     * 
     * @param converter
     *     the command converter service responsible for converting command responses into their proto
     *     equivalent.
     */
    @Reference
    public void setCommandConverter(final CommandConverter converter)
    {
        m_CommandConverter = converter;
    }
    
    /**
     * Binds the XML marshal service.
     * 
     * @param xmlMS
     *            the XML marshal service
     */
    @Reference
    public void setXmlMarshalService(final XmlMarshalService xmlMS)
    {
        m_XmlMarshalService = xmlMS;
    }
    
    /**
     * Binds the XML unmarshal service.
     * @param xmlUS
     *            the XML unmarshal service
     */
    @Reference
    public void setXmlUnmarshalService(final XmlUnmarshalService xmlUS)
    {
        m_XmlUnmarshalService = xmlUS;
    }
    
    /**
     * Generate a list of map entries that are a protocol buffer equivalent of the original map. If a value is of a type
     * that cannot be converted the property will be ignored.
     * 
     * @param props
     *      properties to convert
     * @param format     
     *      format to use for lexicon based objects
     * @return
     *      properties converted into a list of map entries
     * @throws ObjectConverterException
     *      thrown in the event that a property cannot be converted
     * @throws MarshalException
     *      if unable to generated XML from JAXB object (applies when format is XML)
     */
    public List<ComplexTypesMapEntry> mapToComplexTypesMap(final Map<String, Object> props, 
            final RemoteTypesGen.LexiconFormat.Enum format) throws ObjectConverterException, MarshalException
    {
        final List<ComplexTypesMapEntry> entries = new ArrayList<ComplexTypesMapEntry>();
        for (String key : props.keySet())
        {
            final Object value = props.get(key);
            final ComplexTypesMapEntry.Builder entry = ComplexTypesMapEntry.newBuilder().setKey(key);

            if (SharedMessageUtils.isValueConvertableToMultitype(value))
            {
                entry.setMulti(SharedMessageUtils.convertObjectToMultitype(value));
            }
            else if (value instanceof Observation)
            {
                switch (format)
                {
                    case NATIVE:
                        entry.setObservationNative(
                                (ObservationGen.Observation)m_Converter.convertToProto(value));
                        break;
                        
                    case XML:
                        entry.setObservationXml(
                                ByteString.copyFrom(m_XmlMarshalService.createXmlByteArray(value, false)));
                        break;
                        
                    default:
                        throw new UnsupportedOperationException(
                                String.format("Lexicon format %s is not valid for observations", format)); 
                }
            }
            else if (value instanceof LinkStatus)
            {
                entry.setLinkLayerStatus(EnumConverter.convertJavaLinkStatusToProto((LinkStatus)value));
            }
            else if (value instanceof ProgramStatus)
            {
                entry.setProgramStatus(EnumConverter.convertProgramStatusToMissionStatus((ProgramStatus)value));
            }
            else if (value instanceof TestResult)
            {
                entry.setProgramTestResult(
                        EnumConverter.convertToMissionTestResult((TestResult)value));
            }
            else if (value instanceof SummaryStatusEnum)
            {
                entry.setSummaryStatus(SummaryStatusEnumConverter
                        .convertJavaEnumToProto((SummaryStatusEnum)value));
            }
            //asset command response type may be in event
            else if (value instanceof Response)
            {
                final ByteString commandBytes = m_Converter.convertToProto(value).toByteString();
                entry.setMulti(SharedMessageUtils.convertObjectToMultitype(commandBytes));
            }
            else
            {
                continue;
            }
            
            entries.add(entry.build());
        }

        //return list
        return entries;
    }

    /**
     * Create a map with all of the entries in the list.
     * 
     * @param entries
     *      list of entries
     * @return
     *      map containing all of the argument entries
     * @throws ObjectConverterException
     *      thrown in the event that a property cannot be properly converted
     * @throws InvalidProtocolBufferException
     *      thrown in the event that a property's message cannot be parsed
     * @throws UnmarshalException
     *      if unable to unmarshal XML data into a Java object (applies when format is XML)
     */
    public Map<String, Object> complexTypesMapToMap(final List<ComplexTypesMapEntry> entries) 
            throws ObjectConverterException, InvalidProtocolBufferException, UnmarshalException
    {
        final Map<String, Object> arguments = new HashMap<String, Object>();
        
        for (ComplexTypesMapEntry entry : entries)
        {
            final Object value;
            switch (entry.getValueCase())
            {
                case MULTI:
                    value = SharedMessageUtils.convertMultitypeToObject(entry.getMulti());
                    break;
                    
                case LINKLAYERSTATUS:
                    value = EnumConverter.convertProtoLinkStatusToJava(entry.getLinkLayerStatus());
                    break;
                    
                case PROGRAMSTATUS:
                    value = EnumConverter.convertMissionStatusToProgramStatus(entry.getProgramStatus());
                    break;
                    
                case PROGRAMTESTRESULT:
                    value = EnumConverter.convertToTestResult(entry.getProgramTestResult());
                    break;
                    
                case SUMMARYSTATUS:
                    value = SummaryStatusEnumConverter.convertProtoEnumToJava(entry.getSummaryStatus());
                    break;
                    
                case OBSERVATIONNATIVE:
                    value = m_Converter.convertToJaxb(entry.getObservationNative());
                    break;
                    
                case OBSERVATIONXML:
                    value = 
                        m_XmlUnmarshalService.getXmlObject(Observation.class, entry.getObservationXml().toByteArray());
                    break;
                    
                default:
                    throw new UnsupportedOperationException(
                            String.format("Maps with value case [%s] are not supported", entry.getValueCase())); 
                    
            }
            arguments.put(entry.getKey(), value);
        }
        //check if this event is a update command response
        if (arguments.containsKey(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE))
        {
            final String responseType = (String)arguments.get(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE);
            final ByteString commandResponse = (ByteString)arguments.get(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE);
            if (responseType == null)
            {
                throw new IllegalStateException("Unable to convert event because the event has some data for an asset"
                        + " command response, but does not contain all needed data.");
            }
            final Response responseJava = m_CommandConverter.getJavaResponseType(commandResponse.toByteArray(), 
                    m_CommandConverter.getCommandResponseEnumFromClassName(responseType));
            arguments.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, responseJava);
        }
        return arguments;
    }
}
