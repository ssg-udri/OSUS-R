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
package mil.dod.th.ose.core.impl.asset.data;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.AssetObjectData;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.CoordinatesEntry;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.FactoryObjectData;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.OrientationEntry;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.BankDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.ElevationDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HeadingDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LatitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LongitudeWgsDegrees;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Test the asset implementation of the
 * {@link mil.dod.th.ose.core.factory.api.data.BaseFactoryObjectDataManager}.
 * 
 * @author callen
 */
public class TestAssetFactoryObjectDataManagerImpl
{
    private AssetFactoryObjectDataManagerImpl m_SUT;
    private PersistentDataStore m_PersistentDataStore;
    private PersistentData m_PersistentData;
    private FactoryObjectData m_FactData;
    private JaxbProtoObjectConverter m_JaxbConverter;
    private UUID m_UUID = UUID.randomUUID();
    private Coordinates m_Coordinates;
    private Coordinates m_Coordinates2;
    private Map<String, Coordinates> m_CoordsMap;
    private SpatialTypesGen.Coordinates m_ProtoCoors;
    private SpatialTypesGen.Coordinates m_ProtoCoors2;
    private List<CoordinatesEntry> m_ProtoCoorsList;
    private Orientation m_Orientation;
    private Orientation m_Orientation2;
    private Map<String, Orientation> m_OrienMap;
    private SpatialTypesGen.Orientation m_ProtoOrien;
    private SpatialTypesGen.Orientation m_ProtoOrien2;
    private List<OrientationEntry> m_ProtoOrienList;
    
    @Before
    public void setUp() throws ObjectConverterException
    {
        m_SUT = new AssetFactoryObjectDataManagerImpl();
        m_PersistentDataStore = mock(PersistentDataStore.class);
        m_PersistentData = mock(PersistentData.class);
        m_JaxbConverter = mock(JaxbProtoObjectConverter.class);

        m_Coordinates = SpatialTypesFactory.newCoordinates(40.0, 23.0);
        m_Coordinates2 = SpatialTypesFactory.newCoordinates(43.0, 33.0);
        m_CoordsMap = new HashMap<>();
        m_CoordsMap.put(null, m_Coordinates);
        m_Orientation = SpatialTypesFactory.newOrientation(21.0, 19.0, 0.0);
        m_Orientation2 = SpatialTypesFactory.newOrientation(31.0, 29.0, 0.0);
        m_OrienMap = new HashMap<>();
        m_OrienMap.put(null, m_Orientation);

        m_ProtoCoors = SpatialTypesGen.Coordinates.newBuilder().
                setLatitude(LatitudeWgsDegrees.newBuilder().setValue(23).build()).
                setLongitude(LongitudeWgsDegrees.newBuilder().setValue(40).build())
                                .build();
        m_ProtoCoors2 = SpatialTypesGen.Coordinates.newBuilder()
                .setLatitude(LatitudeWgsDegrees.newBuilder().setValue(33).build())
                .setLongitude(LongitudeWgsDegrees.newBuilder().setValue(43).build())
                .build();
        m_ProtoCoorsList = new ArrayList<>();
        m_ProtoCoorsList.add(CoordinatesEntry.newBuilder().setValue(m_ProtoCoors).build());
        m_ProtoOrien = SpatialTypesGen.Orientation.newBuilder().
                setElevation(ElevationDegrees.newBuilder().setValue(19).build()).
                setHeading(HeadingDegrees.newBuilder().setValue(21).build()).
                setBank(BankDegrees.newBuilder().setValue(0.0))
                .build();
        m_ProtoOrien2 = SpatialTypesGen.Orientation.newBuilder().
                setElevation(ElevationDegrees.newBuilder().setValue(29).build()).
                setHeading(HeadingDegrees.newBuilder().setValue(31).build()).
                setBank(BankDegrees.newBuilder().setValue(0.0))
                .build();
        m_ProtoOrienList = new ArrayList<>();
        m_ProtoOrienList.add(OrientationEntry.newBuilder().setValue(m_ProtoOrien).build());

        m_SUT.setPersistentDataStore(m_PersistentDataStore);
        m_SUT.setJaxbProtoObjectConverter(m_JaxbConverter);
    }
    
    /**
     * Verify activate creates registry.
     */
    @Test
    public void testActivate()
    {
        m_SUT.activate();
        
        //per documentation this will return null if the extension is not found
        assertThat(
            m_SUT.getRegistry().findExtensionByName(AssetObjectData.coordinates.getDescriptor().getFullName()), 
                is(notNullValue()));
    }
    
    /**
     * Verify factory object service type is asset.
     */
    @Test
    public void testGetServiceObjectType()
    {
        assertThat(m_SUT.getServiceObjectType().getName(), is(Asset.class.getName()));
    }
    
    /**
     * Verify setting of coordinates for an asset.
     */
    @Test
    public void testSetCoordinates() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException, ValidationFailedException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Coordinates)).thenReturn(m_ProtoCoors);
        
        m_SUT.setCoordinates(m_UUID, m_CoordsMap);
        
        //verify
        ArgumentCaptor<byte[]> pDataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentData).setEntity(pDataCaptor.capture());
        verify(m_PersistentDataStore).merge(m_PersistentData);
        
        byte[] byteData = pDataCaptor.getValue();
        
        FactoryObjectData captMessage = FactoryObjectData.parseFrom(byteData, m_SUT.getRegistry());
        
        List<CoordinatesEntry> coordCapt = captMessage.getExtension(AssetObjectData.coordinates);
        assertThat(coordCapt, is(m_ProtoCoorsList));
    }

    /**
     * Verify setting of orientation for an asset.
     * Verify exception if orientation cannot be converted.
     */
    @Test
    public void testSetOrientationException() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Orientation)).
            thenThrow(new ObjectConverterException("Converter fail!"));
        
        try
        {
            m_SUT.setOrientation(m_UUID, m_OrienMap);
            fail("Expected exception because the converter should have thrown an exception!");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected exception
        }
    }

    /**
     * Verify setting of coordinates for an asset when using a sensor ID.
     */
    @Test
    public void testSetCoordinatesBySensorId() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException, ValidationFailedException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message with additional entry
        m_ProtoCoorsList.add(CoordinatesEntry.newBuilder()
                .setKey("example-sensor-id")
                .setValue(m_ProtoCoors2)
                .build());
        m_CoordsMap.put("example-sensor-id", m_Coordinates2);
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Coordinates)).thenReturn(m_ProtoCoors);
        when(m_JaxbConverter.convertToProto(m_Coordinates2)).thenReturn(m_ProtoCoors2);
        
        m_SUT.setCoordinates(m_UUID, m_CoordsMap);
        
        //verify
        ArgumentCaptor<byte[]> pDataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentData).setEntity(pDataCaptor.capture());
        verify(m_PersistentDataStore).merge(m_PersistentData);
        
        byte[] byteData = pDataCaptor.getValue();
        
        FactoryObjectData captMessage = FactoryObjectData.parseFrom(byteData, m_SUT.getRegistry());
        
        List<CoordinatesEntry> coordCapt = captMessage.getExtension(AssetObjectData.coordinates);
        assertThat(coordCapt, is(m_ProtoCoorsList));
    }

    /**
     * Verify setting of orientation for an asset.
     */
    @Test
    public void testSetOrientation() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException, ValidationFailedException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Orientation)).thenReturn(m_ProtoOrien);
        
        m_SUT.setOrientation(m_UUID, m_OrienMap);
        
        //verify
        ArgumentCaptor<byte[]> pDataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentData).setEntity(pDataCaptor.capture());
        verify(m_PersistentDataStore).merge(m_PersistentData);
        
        byte[] byteData = pDataCaptor.getValue();
        
        FactoryObjectData captMessage = FactoryObjectData.parseFrom(byteData, m_SUT.getRegistry());
        
        List<OrientationEntry> orienCapt = captMessage.getExtension(AssetObjectData.orientation);
        assertThat(orienCapt, is(m_ProtoOrienList));
    }

    /**
     * Verify setting of orientation for an asset when using a sensor ID.
     */
    @Test
    public void testSetOrientationBySensorId() throws FactoryObjectInformationException, ObjectConverterException,
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException, ValidationFailedException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message with additional entry
        m_ProtoOrienList.add(OrientationEntry.newBuilder()
                .setKey("example-sensor-id")
                .setValue(m_ProtoOrien2)
                .build());
        m_OrienMap.put("example-sensor-id", m_Orientation2);
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Orientation)).thenReturn(m_ProtoOrien);
        when(m_JaxbConverter.convertToProto(m_Orientation2)).thenReturn(m_ProtoOrien2);
        
        m_SUT.setOrientation(m_UUID, m_OrienMap);
        
        //verify
        ArgumentCaptor<byte[]> pDataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentData).setEntity(pDataCaptor.capture());
        verify(m_PersistentDataStore).merge(m_PersistentData);
        
        byte[] byteData = pDataCaptor.getValue();
        
        FactoryObjectData captMessage = FactoryObjectData.parseFrom(byteData, m_SUT.getRegistry());
        
        List<OrientationEntry> orienCapt = captMessage.getExtension(AssetObjectData.orientation);
        assertThat(orienCapt, is(m_ProtoOrienList));
    }

    /**
     * Verify setting of coordinates for an asset.
     * Verify exception if coordinates cannot be converted.
     */
    @Test
    public void testSetCoordinatesException() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToProto(m_Coordinates)).
            thenThrow(new ObjectConverterException("Converter fail!"));
        
        try
        {
            m_SUT.setCoordinates(m_UUID, m_CoordsMap);
            fail("Expected exception because the converter should have thrown an exception!");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected exception
        }
    }
     
    /**
     * Verify getting of coordinates for an asset.
     */
    @Test
    public void testGetCoordinates() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.coordinates, m_ProtoCoorsList).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoCoors)).thenReturn(m_Coordinates);
        
        Map<String, Coordinates> newCoords = m_SUT.getCoordinates(m_UUID);
        
        //verify
        assertThat(newCoords, is(m_CoordsMap));
    }

    /**
     * Verify getting of coordinates for an asset when using a sensor ID.
     */
    @Test
    public void testGetCoordinatesBySensorId() throws FactoryObjectInformationException, ObjectConverterException,
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message with additional entry
        m_ProtoCoorsList.add(CoordinatesEntry.newBuilder()
                .setKey("example-sensor-id")
                .setValue(m_ProtoCoors2)
                .build());
        m_CoordsMap.put("example-sensor-id", m_Coordinates2);
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.coordinates, m_ProtoCoorsList).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoCoors)).thenReturn(m_Coordinates);
        when(m_JaxbConverter.convertToJaxb(m_ProtoCoors2)).thenReturn(m_Coordinates2);
        
        Map<String, Coordinates> newCoords = m_SUT.getCoordinates(m_UUID);
        
        //verify
        assertThat(newCoords, is(m_CoordsMap));
    }

    /**
     * Verify getting of orientation for an asset.
     */
    @Test
    public void testGetOrientation() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.orientation, m_ProtoOrienList).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoOrien)).thenReturn(m_Orientation);
        
        Map<String, Orientation> newOrien = m_SUT.getOrientation(m_UUID);
        
        //verify orientation is returned
        assertThat(newOrien, is(m_OrienMap));
    }

    /**
     * Verify getting of orientation for an asset when using a sensor ID.
     */
    @Test
    public void testGetOrientationBySensorId() throws FactoryObjectInformationException, ObjectConverterException,
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message with additional entry
        m_ProtoOrienList.add(OrientationEntry.newBuilder()
                .setKey("example-sensor-id")
                .setValue(m_ProtoOrien2)
                .build());
        m_OrienMap.put("example-sensor-id", m_Orientation2);
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.orientation, m_ProtoOrienList).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoOrien)).thenReturn(m_Orientation);
        when(m_JaxbConverter.convertToJaxb(m_ProtoOrien2)).thenReturn(m_Orientation2);
        
        Map<String, Orientation> newOrien = m_SUT.getOrientation(m_UUID);
        
        //verify orientation is returned
        assertThat(newOrien, is(m_OrienMap));
    }

    /**
     * Verify getting of orientation for an asset.
     * Verify exception if the orientation cannot be converted.
     */
    @Test
    public void testGetOrientationException() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.orientation, m_ProtoOrienList).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoOrien)).
            thenThrow(new ObjectConverterException("Converter fail!"));
        
        try
        {
            m_SUT.getOrientation(m_UUID);
            fail("Expected exception from the conversion of the orientation");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected an exception
        }
    }
    
    /**
     * Verify getting of coordinates for an asset.
     * Verify exception if coordinates cannot be converted.
     */
    @Test
    public void testGetCoordinatesException() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").
                setExtension(AssetObjectData.coordinates, m_ProtoCoorsList).build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);

        when(m_JaxbConverter.convertToJaxb(m_ProtoCoors)).
            thenThrow(new ObjectConverterException("Converter fail"));
        
        try
        {
            m_SUT.getCoordinates(m_UUID);
            fail("Expected exception because the converter should have thrown an exception!");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected exception
        }
    }
    
    /**
     * Verify getting of coordinates for an asset.
     */
    @Test
    public void testGetCoordinatesNoData() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(null);
        
        Map<String, Coordinates> newCoords = m_SUT.getCoordinates(m_UUID);
        
        //verify
        assertThat(newCoords, is(nullValue()));
    }
    
    /**
     * Verify getting of orientation for an asset when there is not data information.
     */
    @Test
    public void testGetOrientationNoData() throws FactoryObjectInformationException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //activate to create converters 
        m_SUT.activate();
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(null);
        
        Map<String, Orientation> newOrien = m_SUT.getOrientation(m_UUID);
        
        //verify
        assertThat(newOrien, is(nullValue()));
    }
    
    /**
     * Verify getting the coordinates for an asset when there is no coordinate information.
     */
    @Test
    public void testGetCoordinatesNoCords() throws FactoryObjectInformationException
    {        
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);
        
        Map<String, Coordinates> newCoords = m_SUT.getCoordinates(m_UUID);
        
        //verify
        assertThat(newCoords.size(), is(0));
    }
    
    /**
     * Verify getting the orientation for an asset when there is no orientation information.
     */
    @Test
    public void testGetOrientationsNoOrient() throws FactoryObjectInformationException
    {        
        //activate to create converters 
        m_SUT.activate();
        
        //persisted message
        m_FactData = FactoryObjectData.newBuilder().
                setName("original").build();
        when(m_PersistentData.getEntity()).thenReturn(m_FactData.toByteArray());
        when(m_PersistentData.getUUID()).thenReturn(m_UUID);
        
        when(m_PersistentDataStore.find(m_UUID)).thenReturn(m_PersistentData);
        
        Map<String, Orientation> newOrient = m_SUT.getOrientation(m_UUID);
        
        //verify
        assertThat(newOrient.size(), is(0));
    }
}
