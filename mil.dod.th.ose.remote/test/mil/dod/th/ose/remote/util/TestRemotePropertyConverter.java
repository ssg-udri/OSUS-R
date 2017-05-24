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

import static mil.dod.th.ose.test.matchers.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.asset.commands.GetVersionResponse;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.CustomCommsTypes;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry.ValueCase;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionStatus;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionTestResult;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.SummaryStatusEnumConverter;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen.Response;
import mil.dod.th.remote.lexicon.asset.commands.GetVersionResponseGen;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.event.EventConstants;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;

/**
 * @author callen
 *
 */
public class TestRemotePropertyConverter
{
    private RemotePropertyConverter m_SUT;
    @Mock private JaxbProtoObjectConverter m_Converter;
    @Mock private CommandConverter m_CommandConverter;
    @Mock private XmlMarshalService m_XmlMarshalService;
    @Mock private XmlUnmarshalService m_XmlUnmarshalService;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new RemotePropertyConverter();
        MockitoAnnotations.initMocks(this);
        
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        m_SUT.setCommandConverter(m_CommandConverter);
        m_SUT.setXmlMarshalService(m_XmlMarshalService);
        m_SUT.setXmlUnmarshalService(m_XmlUnmarshalService);
    }

    /**
     * Test creating a property map from protocol buffer message event properties.
     */
    @Test
    public void testComplexTypesMapToMap() throws Exception
    {
        List<ComplexTypesMapEntry> entries = new ArrayList<ComplexTypesMapEntry>();

        entries.add(ComplexTypesMapEntry.newBuilder().setKey("test").setMulti(Multitype.newBuilder().
                setType(Type.STRING).
                setStringValue("blah").build()).
                build());
        
        entries.add(ComplexTypesMapEntry.newBuilder().setKey("prop2").setMulti(Multitype.newBuilder().
                setType(Type.INT32).
                setInt32Value(28).build()).
                build());

        entries.add(ComplexTypesMapEntry.newBuilder().setKey("prop3").
                setLinkLayerStatus(CustomCommsTypes.LinkStatus.OK).build());
        
        entries.add(ComplexTypesMapEntry.newBuilder().setKey("prop4").
                setProgramStatus(MissionStatus.EXECUTED).build());
        
        entries.add(ComplexTypesMapEntry.newBuilder().setKey("prop5").
                setProgramTestResult(MissionTestResult.PASSED).build());

        List<String> strings = new ArrayList<String>();
        strings.add("String");
        strings.add("String2");

        entries.add(ComplexTypesMapEntry.newBuilder().
             setKey("prop6").
             setMulti(SharedMessageUtils.convertObjectToMultitype(strings)).build());
        
        entries.add(ComplexTypesMapEntry.newBuilder().setKey("prop7").
                setSummaryStatus(SummaryStatusEnumConverter.convertJavaEnumToProto(SummaryStatusEnum.UNKNOWN))
                .build());
        
        //create a map from the list of proto properties
        Map<String, Object> map = m_SUT.complexTypesMapToMap(entries);

        assertThat(map.size(), is(7));
        
        //verify values
        assertThat(map, rawMapHasEntry("test", "blah"));
        assertThat(map, rawMapHasEntry("prop2", 28));
        assertThat(map, rawMapHasEntry("prop3", LinkStatus.OK));
        assertThat(map, rawMapHasEntry("prop4", ProgramStatus.EXECUTED));
        assertThat(map, rawMapHasEntry("prop5", TestResult.PASSED));
        assertThat(map, rawMapHasEntry("prop6", strings));
        assertThat(map, rawMapHasEntry("prop7", SummaryStatusEnum.UNKNOWN));
    }
    
    /**
     * Verify if a type cannot be converted an exception is thrown.
     */
    @Test
    public void testComplexTypesMapToMap_UnsupportedTypes() throws Exception
    {
        List<ComplexTypesMapEntry> entries = new ArrayList<ComplexTypesMapEntry>();

        entries.add(ComplexTypesMapEntry.newBuilder()
                .setKey("prop7") // don't set the value to simulate an unknown type
                .build());
        
        try
        {
            m_SUT.complexTypesMapToMap(entries);
            fail("Expecting exception");
        }
        catch (UnsupportedOperationException e) 
        {
            
        }
    }
    
    /**
     * Test converting a complex types map that contains an protocol buffer observation to a map with a JAXB 
     * observation.
     */
    @Test
    public final void testComplexTypesMapToMap_ObservationNative() throws Exception
    {
        // mock
        List<ComplexTypesMapEntry> props = new ArrayList<ComplexTypesMapEntry>();
        ComplexTypesMapEntry mapType = ComplexTypesMapEntry.newBuilder().
            setKey("Observation").
            setObservationNative(TerraHarvestMessageHelper.getProtoObs()).build();
        props.add(mapType);
        
        Observation observation = new Observation().withAssetUuid(UUID.randomUUID()).withSensorId("test sensor");
        when(m_Converter.convertToJaxb(Mockito.any(ObservationGen.Observation.class))).
            thenReturn(observation);

        Map<String, Object> map = m_SUT.complexTypesMapToMap(props);
        assertThat(map, rawMapHasEntry("Observation", observation));
    }
    
    /**
     * Test converting a complex types map that contains an XML observation to a map with a JAXB observation.
     */
    @Test
    public final void testComplexTypesMapToMap_ObservationXml() throws Exception
    {
        byte[] obsXml = new byte[] { 0x01, 0x02, 0x03 };
        
        // mock
        List<ComplexTypesMapEntry> props = new ArrayList<ComplexTypesMapEntry>();
        ComplexTypesMapEntry mapType = ComplexTypesMapEntry.newBuilder().
            setKey("Observation").
            setObservationXml(ByteString.copyFrom(obsXml)).build();
        props.add(mapType);
        
        Observation observation = new Observation().withAssetUuid(UUID.randomUUID()).withSensorId("test sensor");
        when(m_XmlUnmarshalService.getXmlObject(Observation.class, obsXml)).thenReturn(observation);

        Map<String, Object> map = m_SUT.complexTypesMapToMap(props);
        assertThat(map, rawMapHasEntry("Observation", observation));
    }
    
    /**
     * Test creating a property map from a list of google proto message equivalents.
     * 
     * Verify exception as observation fails to convert.
     */
    @Test
    public final void testComplexTypesMapToMap_BadObs() throws Exception
    {
        // mock
        List<ComplexTypesMapEntry> props = new ArrayList<ComplexTypesMapEntry>();
        ComplexTypesMapEntry mapType = ComplexTypesMapEntry.newBuilder().
            setKey("Observation").
            setObservationNative(TerraHarvestMessageHelper.getProtoObs()).build();
        props.add(mapType);
        
        when(m_Converter.convertToJaxb(Mockito.any(ObservationGen.Observation.class))).
            thenThrow(new ObjectConverterException("object converter fail"));

        try
        {
            m_SUT.complexTypesMapToMap(props);
            fail("Expected exception because of mocking.");
        }
        catch (ObjectConverterException e)
        {
            //expected exception
        }
    }

    /**
     * Test creating a java map equivalent of a proto property map which was a command response event.
     * 
     * Verify expected values are converted and verify interactions with command converter.
     */
    @Test
    public final void testComplexTypesMapToMap_CommandResponseEvent() throws Exception
    {
        // mock
        Map<String, Object> props = new HashMap<String, Object>();
        GetVersionResponse versionResponse = new GetVersionResponse(null, null, "version");
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, GetVersionResponse.class.getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, versionResponse);
        
        //command converter mocking
        JaxbProtoObjectConverter converter = mock(JaxbProtoObjectConverter.class);
        m_SUT.setJaxbProtoObjectConverter(converter);
        GetVersionResponseGen.GetVersionResponse protoVersion = 
                GetVersionResponseGen.GetVersionResponse.newBuilder().setCurrentVersion("piggies")
                .setBase(Response.getDefaultInstance()).build();
        when(converter.convertToProto(versionResponse)).thenReturn(protoVersion);
        when(m_CommandConverter.getCommandResponseEnumFromClassName(GetVersionResponse.class.getName())).
            thenReturn(CommandResponseEnum.GET_VERSION_RESPONSE);

        List<ComplexTypesMapEntry> convertStringProps = m_SUT.mapToComplexTypesMap(props, null);
        
        //command converter mocking
        when(m_CommandConverter.getJavaResponseType(protoVersion.toByteArray(), 
                CommandResponseEnum.GET_VERSION_RESPONSE)).thenReturn(versionResponse);
        
        Map<String, Object> propsAfterConv = m_SUT.complexTypesMapToMap(convertStringProps);
        assertThat(propsAfterConv, rawMapHasEntry(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, 
                GetVersionResponse.class.getName()));
        assertThat(propsAfterConv, rawMapHasEntry(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, versionResponse));
    }
    
    /**
     * Test creating a java map equivalent of a proto property map which was a command response event.
     * 
     * Verify exception because the command enum type is not available.
     */
    @Test
    public final void testComplexTypesMapToMap_CommandResponseEventJavaMapBad() throws Exception
    {
        // mock
        List<ComplexTypesMapEntry> entries = new ArrayList<>();
        entries.add(ComplexTypesMapEntry.newBuilder().setKey(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE).
                setMulti(Multitype.newBuilder().setType(Type.BYTES).
                        setByteStringValue(ByteString.EMPTY).build()).build());
        
        try
        {
            m_SUT.complexTypesMapToMap(entries);
            fail("Expected exception");
        }
        catch (IllegalStateException e)
        {
            //expected because the event has parts of a command response, but not the actual enum type value
        }
    }
    
    /**
     * Test creating a protocol buffer message equivalent of property map.
     * 
     * Verify expected values are converted, while unsupported types, like an asset are ignored.
     */
    @Test
    public final void testMapToComplexTypesMap() throws Exception
    {
        Asset mockAsset = mock(Asset.class);
        // mock
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ, mockAsset);
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, "9433d480-d03e-11e1-9b23-0800200c9a66");
        props.put(EventConstants.EVENT_TOPIC, "blah");
        props.put("test", "hello");
        props.put("test2", new Object());
        props.put("test3", AssetActiveStatus.ACTIVATED);
        props.put("test4", LinkStatus.OK);
        props.put("test5", ProgramStatus.EXECUTED);
        props.put("test6", TestResult.FAILED);
        props.put("test7", SummaryStatusEnum.GOOD);
        Bundle bundle = mock(Bundle.class);
        props.put(EventConstants.EVENT, new BundleEvent(5, bundle));

        Map<String, ComplexTypesMapEntry> convertedProps = 
                Maps.uniqueIndex(m_SUT.mapToComplexTypesMap(props, null), new Function<ComplexTypesMapEntry, String>()
                {
                    @Override
                    public String apply(ComplexTypesMapEntry entry)
                    {
                        return entry.getKey();
                    }
                });
        
        assertThat(convertedProps, not(hasKey(FactoryDescriptor.EVENT_PROP_OBJ)));
        assertThat(convertedProps, not(hasKey("test2")));
        assertThat(convertedProps, not(hasKey("test3")));
        assertThat(convertedProps, not(hasKey(EventConstants.EVENT)));
        
        //verify expected values
        assertThat(convertedProps.get(FactoryDescriptor.EVENT_PROP_OBJ_UUID).getValueCase(), is(ValueCase.MULTI)); 
        assertThat(convertedProps.get(FactoryDescriptor.EVENT_PROP_OBJ_UUID).getMulti().getStringValue(), 
                is("9433d480-d03e-11e1-9b23-0800200c9a66"));
        assertThat(convertedProps.get("test").getValueCase(), is(ValueCase.MULTI)); 
        assertThat(convertedProps.get("test").getMulti().getStringValue(), is("hello"));
        assertThat(convertedProps.get(EventConstants.EVENT_TOPIC).getValueCase(), is(ValueCase.MULTI)); 
        assertThat(convertedProps.get(EventConstants.EVENT_TOPIC).getMulti().getStringValue(), is("blah"));
        assertThat(convertedProps.get("test4").getValueCase(), is(ValueCase.LINKLAYERSTATUS)); 
        assertThat(convertedProps.get("test4").getLinkLayerStatus(), is(CustomCommsTypes.LinkStatus.OK));
        assertThat(convertedProps.get("test5").getValueCase(), is(ValueCase.PROGRAMSTATUS)); 
        assertThat(convertedProps.get("test5").getProgramStatus(), is(MissionStatus.EXECUTED));
        assertThat(convertedProps.get("test6").getValueCase(), is(ValueCase.PROGRAMTESTRESULT)); 
        assertThat(convertedProps.get("test6").getProgramTestResult(), is(MissionTestResult.FAILED));
        assertThat(convertedProps.get("test7").getValueCase(), is(ValueCase.SUMMARYSTATUS)); 
        assertThat(convertedProps.get("test7").getSummaryStatus(), 
                is(SummaryStatusEnumConverter.convertJavaEnumToProto(SummaryStatusEnum.GOOD)));
    }

    /**
     * Test creating a protocol buffer message equivalent of property map.
     * 
     * Verify expected values are converted.
     */
    @Test
    public final void testMapToComplexTypesMap_ObservationNative() throws Exception
    {
        // mock
        Map<String, Object> props = new HashMap<String, Object>();
        Observation obs = mock(Observation.class);
        props.put("obs", obs);
        
        ObservationGen.Observation genObs = TerraHarvestMessageHelper.getProtoObs();
        when(m_Converter.convertToProto(Mockito.any(Observation.class))).thenReturn(genObs);
        when(m_Converter.convertToJaxb(Mockito.any(ObservationGen.Observation.class))).
            thenReturn(obs);
        List<ComplexTypesMapEntry> complexMap = m_SUT.mapToComplexTypesMap(props, 
                RemoteTypesGen.LexiconFormat.Enum.NATIVE);
        
        // verify
        assertThat(complexMap.get(0).getValueCase(), is(ValueCase.OBSERVATIONNATIVE));
        assertThat(complexMap.get(0).getObservationNative(), is(genObs));
    }
    
    /**
     * Verify observation in map is translated to XML if that is the desired format.
     */
    @Test
    public final void testMapToComplexTypesMap_ObservationXml() throws Exception
    {
        // mock
        Map<String, Object> props = new HashMap<String, Object>();
        Observation obs = mock(Observation.class);
        props.put("obs", obs);
        
        byte[] expectedBytes = new byte[] {0x12, 0x34, 0x56, 0x78};
        
        when(m_XmlMarshalService.createXmlByteArray(obs, false)).thenReturn(expectedBytes);
        
        List<ComplexTypesMapEntry> complexMap = m_SUT.mapToComplexTypesMap(props, 
                RemoteTypesGen.LexiconFormat.Enum.XML);
        
        // verify
        assertThat(complexMap.get(0).getValueCase(), is(ValueCase.OBSERVATIONXML));
        assertThat(complexMap.get(0).getObservationXml(), is(ByteString.copyFrom(expectedBytes)));
    }
    
    /**
     * Verify requesting an invalid lexicon format causes an exception.
     */
    @Test
    public final void testMapToComplexTypesMap_ObservationInvalidFormat() throws Exception
    {
        // mock
        Map<String, Object> props = new HashMap<String, Object>();
        Observation obs = mock(Observation.class);
        props.put("obs", obs);

        when(m_Converter.convertToProto(Mockito.any(Observation.class))).
            thenThrow(new ObjectConverterException("object converter fail"));

        try
        {
            m_SUT.mapToComplexTypesMap(props, 
                    RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY);
            fail("Expecting exception as format is invalid");
        }
        catch (UnsupportedOperationException e)
        {
            
        }
    }
    
    /**
     * Test creating a protocol buffer message equivalent of property map from an event.
     * 
     * Verify exception as observation fails to convert.
     */
    @Test
    public final void testMapToComplexTypesMap_BadObs() throws Exception
    {
        // mock
        Map<String, Object> props = new HashMap<String, Object>();
        Observation obs = mock(Observation.class);
        props.put("observation", obs);

        when(m_Converter.convertToProto(Mockito.any(ObservationGen.Observation.class))).
            thenThrow(new ObjectConverterException("object converter fail"));

        try
        {
            m_SUT.mapToComplexTypesMap(props, RemoteTypesGen.LexiconFormat.Enum.NATIVE);
            fail("Expected exception because of mocking.");
        }
        catch (ObjectConverterException e)
        {
            //expected exception
        }
    }

    /**
     * Test creating a protocol buffer message equivalent of property map which was a command response event.
     * 
     * Verify expected values are converted and verify interactions with command converter.
     */
    @Test
    public final void testMapToComplexTypesMap_CommandResponse() throws Exception
    {
        // mock
        Map<String, Object> props = new HashMap<String, Object>();
        GetVersionResponse versionResponse = new GetVersionResponse(null, null, "version");
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, versionResponse);
        
        //command converter mocking
        JaxbProtoObjectConverter converter = mock(JaxbProtoObjectConverter.class);
        GetVersionResponseGen.GetVersionResponse protoVersion = 
                GetVersionResponseGen.GetVersionResponse.newBuilder()
                .setBase(BaseTypesGen.Response.getDefaultInstance())
                .setCurrentVersion("piggies")
                .build();
        when(converter.convertToProto(versionResponse)).thenReturn(protoVersion);
        when(m_CommandConverter.getCommandResponseEnumFromClassName(GetVersionResponse.class.getName())).
            thenReturn(CommandResponseEnum.GET_VERSION_RESPONSE);
        
        m_SUT.setJaxbProtoObjectConverter(converter);

        List<ComplexTypesMapEntry> complexMap = m_SUT.mapToComplexTypesMap(props, null);
        
        // verify
        boolean commandByteStringFound = false;
        for (ComplexTypesMapEntry entry : complexMap)
        {
            if (entry.getKey().equals(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE))
            {
                commandByteStringFound = true;
                assertThat(entry.getMulti().getByteStringValue(), is(protoVersion.toByteString()));
            }
        }
        assertThat(commandByteStringFound, is(true));
    }
}
