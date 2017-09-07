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

package mil.dod.th.ose.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.protobuf.ByteString;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.core.remote.proto.SharedMessages;

import org.junit.Test;

public class TestSharedMessageUtils
{
    @Test
    public void testIsValueConvertableToMultitype()
    {
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(234), is(true));
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(402078178989078L), is(true));
        assertThat(SharedMessageUtils.isValueConvertableToMultitype("test"), is(true));
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(false), is(true));
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(null), is(true));
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(234.0f), is(true));
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(234d), is(true));
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(Short.valueOf("2")), is(true));
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(ByteString.copyFrom(new byte[]{1,2,3})), is(true));

        //test a list
        List<String> strings = new ArrayList<String>();
        strings.add("String");
        strings.add("String2");
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(strings), is(true));
        
        Character theChar = 'b';
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(theChar), is(true));
        
        Byte theByte = new Byte("10"); 
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(theByte), is(true));
        
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(UUID.randomUUID()), is(true));
        
        assertThat(SharedMessageUtils.isValueConvertableToMultitype(new Object()), is(false));       
    }
    
    @Test
    public void testConvertObjectToMultitype()
    {
        Multitype value = SharedMessageUtils.convertObjectToMultitype(5);
        assertThat(value.getType(), is(Type.INT32));
        assertThat(value.getInt32Value(), is(5));
        
        value = SharedMessageUtils.convertObjectToMultitype(28972839790289L);
        assertThat(value.getType(), is(Type.INT64));
        assertThat(value.getInt64Value(), is(28972839790289L));
        
        value = SharedMessageUtils.convertObjectToMultitype("test");
        assertThat(value.getType(), is(Type.STRING));
        assertThat(value.getStringValue(), is("test"));
        
        value = SharedMessageUtils.convertObjectToMultitype(null);
        assertThat(value.getType(), is(Type.NONE));
        
        value = SharedMessageUtils.convertObjectToMultitype(false);
        assertThat(value.getType(), is(Type.BOOL));
        assertThat(value.getBoolValue(), is(false));
        
        value = SharedMessageUtils.convertObjectToMultitype(true);
        assertThat(value.getType(), is(Type.BOOL));
        assertThat(value.getBoolValue(), is(true));
        
        value = SharedMessageUtils.convertObjectToMultitype(234.234f);
        assertThat(value.getType(), is(Type.FLOAT));
        assertThat(value.getFloatValue(), is(234.234f));
        
        value = SharedMessageUtils.convertObjectToMultitype(234.12d);
        assertThat(value.getType(), is(Type.DOUBLE));
        assertThat(value.getDoubleValue(), is(234.12d));
        
        value = SharedMessageUtils.convertObjectToMultitype(Short.valueOf("2"));
        assertThat(value.getType(), is(Type.SHORT));
        assertThat(value.getShortValue(), is(2));
        
        Character theChar = 'b';
        value = SharedMessageUtils.convertObjectToMultitype(theChar);
        assertThat(value.getType(), is(Type.CHARACTER));
        assertThat(value.getCharValue(), is("b"));
        
        Byte theByte = new Byte("10");
        value = SharedMessageUtils.convertObjectToMultitype(theByte);
        assertThat(value.getType(), is(Type.BYTE));
        assertThat(value.getByteValue(), is(10));
        
        ByteString someBytes = ByteString.copyFrom(new byte[]{1,2,3,4});
        value = SharedMessageUtils.convertObjectToMultitype(someBytes);
        assertThat(value.getType(), is(Type.BYTES));
        assertThat(value.getByteStringValue(), is(someBytes));
        
        UUID uuid = UUID.randomUUID();
        value = SharedMessageUtils.convertObjectToMultitype(uuid);
        assertThat(value.getType(), is(Type.UUID));
        assertThat(new UUID(value.getUuidMostSigBitsValue(), value.getUuidLeastSigBitsValue()), is(uuid));

        //test a list
        List<String> strings = new ArrayList<String>();
        strings.add("String");
        strings.add("String2");
        //multitype equiv.
        Multitype.Builder multi = Multitype.newBuilder().setType(Type.MULTITYPE_LIST);
        for (String string : strings)
        {
            multi.addList(SharedMessageUtils.convertObjectToMultitype(string));
        }
        value = SharedMessageUtils.convertObjectToMultitype(strings);
        assertThat(value.getType(), is(Type.MULTITYPE_LIST));
        assertThat(value, is(multi.build()));
        
        try
        {
            SharedMessageUtils.convertObjectToMultitype(new Object());
        }
        catch (IllegalArgumentException e){}
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertMultitypeToObject()
    {
        Object value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().setType(Type.NONE).build());
        assertThat(value, is(nullValue()));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.INT32).
                setInt32Value(28).build());
        assertThat((Integer)value, is(28));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.INT64).
                setInt64Value(2818828989478278L).build());
        assertThat((Long)value, is(2818828989478278L));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.STRING).
                setStringValue("blah").build());
        assertThat((String)value, is("blah"));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.BOOL).
                setBoolValue(false).build());
        assertThat((Boolean)value, is(false));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.FLOAT).
                setFloatValue(234.234f).build());
        assertThat((Float)value, is(234.234f));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.DOUBLE).
                setDoubleValue(234.12d).build());
        assertThat((Double)value, is(234.12d));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.SHORT).
                setShortValue(2).build());
        assertThat((Short)value, is((short)2));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.CHARACTER).
                setCharValue("b").build());
        assertThat((Character)value, is('b'));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.BYTE).
                setByteValue(10).build());
        assertThat((Byte)value, is((byte)10));
        
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.BYTES).
                setByteStringValue(ByteString.copyFrom(new byte[]{2,3,4,5})).build());
        assertThat((ByteString)value, is(ByteString.copyFrom(new byte[]{2,3,4,5})));
        
        UUID uuid = UUID.randomUUID();
        value = SharedMessageUtils.convertMultitypeToObject(Multitype.newBuilder().
                setType(Type.UUID).
                setUuidMostSigBitsValue(uuid.getMostSignificantBits()).
                setUuidLeastSigBitsValue(uuid.getLeastSignificantBits()).build());
        assertThat((UUID)value, is(uuid));

        //test a list
        List<String> strings = new ArrayList<String>();
        strings.add("String");
        strings.add("String2");
        //multitype equiv.
        Multitype.Builder multi = Multitype.newBuilder().setType(Type.MULTITYPE_LIST);
        for (String string : strings)
        {
            multi.addList(SharedMessageUtils.convertObjectToMultitype(string));
        }
        value = SharedMessageUtils.convertMultitypeToObject(multi.build());
        assertThat((List<String>)value, is(strings));
    }

    /**
     * Test and verify that a Java UUID object is properly converted to a google protocol buffer
     * UUID message.
     */
    @Test
    public void testConvertUUIDToProtoUUID()
    {
        UUID testUuid = UUID.randomUUID();
        try
        {
            SharedMessageUtils.convertUUIDToProtoUUID(null);
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        SharedMessages.UUID convertedUuid = SharedMessageUtils.convertUUIDToProtoUUID(testUuid);
        
        assertThat(testUuid.getLeastSignificantBits(), is(convertedUuid.getLeastSignificantBits()));
        assertThat(testUuid.getMostSignificantBits(), is(convertedUuid.getMostSignificantBits()));        
    }
    
    /**
     * Test and verify that a google protocol buffer UUID message is properly converted to a Java
     * UUID object.
     */
    @Test 
    public void testConvertProtoUUIDtoUUID()
    {
        SharedMessages.UUID testUuid = SharedMessages.UUID.newBuilder().
                setLeastSignificantBits(55555).
                setMostSignificantBits(333).build();
        try
        {
            SharedMessageUtils.convertProtoUUIDtoUUID(null);
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }

        UUID convertedUuid = SharedMessageUtils.convertProtoUUIDtoUUID(testUuid);
        
        assertThat(testUuid.getLeastSignificantBits(), is(convertedUuid.getLeastSignificantBits()));
        assertThat(testUuid.getMostSignificantBits(), is(convertedUuid.getMostSignificantBits()));        
    }

    /**
     * Test the creation of a factory object info message from a given factory object.
     */
    @Test
    public void testCreateFactoryObjectMessage()
    {
        //mock factory object
        FactoryObject object = mock(FactoryObject.class);
        FactoryDescriptor descriptor = mock(FactoryDescriptor.class);
        when(descriptor.getProductType()).thenReturn("product-type");
        when(object.getUuid()).thenReturn(UUID.randomUUID());
        when(object.getPid()).thenReturn("pid");
        when(object.getFactory()).thenReturn(descriptor);
        
        //create message
        FactoryObjectInfo info = SharedMessageUtils.createFactoryObjectInfoMessage(object);

        //verify values
        assertThat(info.getPid(), is("pid"));
        assertThat(info.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(object.getUuid())));
        assertThat(info.getProductType(), is("product-type"));
        
        //verify that a message is still created even if a pid does not exist
        when(object.getPid()).thenReturn(null);
        
        FactoryObjectInfo pidNull = SharedMessageUtils.createFactoryObjectInfoMessage(object);
        
        assertThat(pidNull.getPid(), is(""));
        assertThat(pidNull.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(object.getUuid())));
        assertThat(pidNull.getProductType(), is("product-type"));
    }
    
    /**
     * Verify method with passed in variables works correctly.
     */
    @Test
    public void testCreateFactoryObjecMessageWithoutFactoryObject()
    {
        //mock factory object
        FactoryObject object = mock(FactoryObject.class);
        FactoryDescriptor descriptor = mock(FactoryDescriptor.class);
        when(descriptor.getProductType()).thenReturn("product-type");
        when(object.getUuid()).thenReturn(UUID.randomUUID());
        when(object.getPid()).thenReturn("pid");
        when(object.getFactory()).thenReturn(descriptor);
        
        //SamplePluginAsset asset = FactoryObjectMocker.mockFactoryObject(SamplePluginAsset.class, "pid");
        
        FactoryObjectInfo pidFromBaseMethod = SharedMessageUtils.createFactoryObjectInfoMessage(object.getPid(), 
                object.getUuid(), descriptor);
        
        assertThat(pidFromBaseMethod.getPid(), is("pid"));
        assertThat(pidFromBaseMethod.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(object.getUuid())));
        assertThat(pidFromBaseMethod.getProductType(), is("product-type"));
        
        FactoryObjectInfo pidFromBaseMethodNull = SharedMessageUtils.createFactoryObjectInfoMessage(null, 
                object.getUuid(), descriptor);
        
        assertThat(pidFromBaseMethodNull.getPid(), is(""));
        assertThat(pidFromBaseMethodNull.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(object.getUuid())));
        assertThat(pidFromBaseMethodNull.getProductType(), is("product-type"));
    }
       
    /**
     * Test and verify that a list of SimpleTypesMapEntry is properly converted to a Java native type Dictionary object
     */
    @Test 
    public void testConvertMaptoDictionary()
    {
        //Create a list of map entries
        SimpleTypesMapEntry testEntry = SimpleTypesMapEntry.newBuilder().setKey("something").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test1")).build();
 
        SimpleTypesMapEntry testEntry1 = SimpleTypesMapEntry.newBuilder().setKey("something1").
                setValue(Multitype.newBuilder().
                        setType(Type.UUID).setUuidMostSigBitsValue(20L).setUuidLeastSigBitsValue(10L)).build();
        
        SimpleTypesMapEntry testEntry2 = SimpleTypesMapEntry.newBuilder().setKey("something2").
                setValue(Multitype.newBuilder().
                        setType(Type.BYTE).setByteValue(10)).build();
        
        SimpleTypesMapEntry testEntry3 = SimpleTypesMapEntry.newBuilder().setKey("something3").
                setValue(Multitype.newBuilder().
                        setType(Type.CHARACTER).setCharValue("t")).build();
        
        SimpleTypesMapEntry testEntry4 = SimpleTypesMapEntry.newBuilder().setKey("something4").
                setValue(Multitype.newBuilder().
                        setType(Type.FLOAT).setFloatValue(10f)).build();
        
        SimpleTypesMapEntry testEntry5 = SimpleTypesMapEntry.newBuilder().setKey("something5").
                setValue(Multitype.newBuilder().
                        setType(Type.BOOL).setBoolValue(false)).build();
        
        SimpleTypesMapEntry testEntry6 = SimpleTypesMapEntry.newBuilder().setKey("something6").
                setValue(Multitype.newBuilder().
                        setType(Type.DOUBLE).setDoubleValue(10)).build();
        
        SimpleTypesMapEntry testEntry7 = SimpleTypesMapEntry.newBuilder().setKey("something7").
                setValue(Multitype.newBuilder().
                        setType(Type.INT32).setInt32Value(10)).build();
        SimpleTypesMapEntry testEntry8 = SimpleTypesMapEntry.newBuilder().setKey("something8").
                setValue(Multitype.newBuilder().
                        setType(Type.INT64).setInt64Value(5000)).build();
        SimpleTypesMapEntry testEntry9 = SimpleTypesMapEntry.newBuilder().setKey("something9").
                setValue(Multitype.newBuilder().
                        setType(Type.SHORT).setShortValue(2)).build();
        
        List<SimpleTypesMapEntry> testList = new ArrayList<SimpleTypesMapEntry>();   
        testList.add(testEntry);
        testList.add(testEntry1);
        testList.add(testEntry2);
        testList.add(testEntry3);
        testList.add(testEntry4);
        testList.add(testEntry5);
        testList.add(testEntry6);
        testList.add(testEntry7);
        testList.add(testEntry8);
        testList.add(testEntry9);
        
        //convert the map list to dictionary object
        Dictionary<String, Object> convertedList = SharedMessageUtils.convertMaptoDictionary(testList);
        UUID resultUUID = (UUID) convertedList.get("something1");
        Byte resultByte = (Byte) convertedList.get("something2");
        Character resultChar = (Character)convertedList.get("something3");
        Short resultShort = (Short) convertedList.get("something9");
        //verify values
        assertThat(testList.get(0).getValue().getStringValue(), is(convertedList.get("something"))); 
        assertThat(testList.get(1).getValue().getUuidLeastSigBitsValue(), is(resultUUID.getLeastSignificantBits())); 
        assertThat(testList.get(2).getValue().getByteValue(), is(resultByte.intValue()));
        assertThat(testList.get(3).getValue().getCharValue(), is(resultChar.toString()));
        assertThat(testList.get(4).getValue().getFloatValue(), is(convertedList.get("something4")));
        assertThat(testList.get(5).getValue().getBoolValue(), is(convertedList.get("something5")));
        assertThat(testList.get(6).getValue().getDoubleValue(), is(convertedList.get("something6")));
        assertThat(testList.get(7).getValue().getInt32Value(), is(convertedList.get("something7")));
        assertThat(testList.get(8).getValue().getInt64Value(), is(convertedList.get("something8")));
        assertThat(testList.get(9).getValue().getShortValue(), is(resultShort.intValue()));
    }
    
    /**
     * Test and verify that when there is a duplicate key entry in a list, IllegalArgumentException is caught
     */
    @Test 
    public void testConvertMaptoDictionaryException()
    {
        //Create a list of map entries with duplicate keys
        List<SimpleTypesMapEntry> testList = new ArrayList<SimpleTypesMapEntry>(); 
        SimpleTypesMapEntry testEntry = SimpleTypesMapEntry.newBuilder().setKey("something").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test1")).build();  
        SimpleTypesMapEntry testEntry1 = SimpleTypesMapEntry.newBuilder().setKey("something").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test2")).build();
        testList.add(testEntry);
        testList.add(testEntry1);
        
        //catch IllegalArgument exception during conversion
        try
        {
            SharedMessageUtils.convertMaptoDictionary(testList);
            fail("Expecting Exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
     * Test and verify that a Java native type Dictionary object is properly converted to a list of SimpleTypesMapEntry.
     */
    @Test
    public void testConvertDictionarytoMap()
    {
        Dictionary<String, Object> testDictionary = new Hashtable<String, Object>();
        testDictionary.put("testKey", "testValue");
        
        UUID uuid = UUID.randomUUID() ;
        testDictionary.put("testKey1", uuid );
        
        Byte byteTest = Byte.valueOf("100");
        testDictionary.put("testKey2", byteTest );
        
        String stringChar = "test";
        Character charTest = stringChar.charAt(0);
        testDictionary.put("testKey3", charTest );
        
        Boolean boolTest = false ;
        testDictionary.put("testKey4", boolTest);

        testDictionary.put("testKey5", 2);
        
        List<SimpleTypesMapEntry> listTest = SharedMessageUtils.convertDictionarytoMap(testDictionary);
        Dictionary<String, Multitype> entryMap = new Hashtable<String, Multitype>();
        
        for (SimpleTypesMapEntry entry:listTest)        
        {
            entryMap.put(entry.getKey(), entry.getValue());
        }

        assertThat(entryMap.get("testKey").getStringValue(),is("testValue"));
        assertThat(entryMap.get("testKey1").getUuidMostSigBitsValue(), is(uuid.getMostSignificantBits()));
        assertThat(entryMap.get("testKey2").getByteValue(), is(100));
        assertThat(entryMap.get("testKey3").getCharValue(), is("t"));
        assertThat(entryMap.get("testKey4").getBoolValue(), is(false));
        assertThat(entryMap.get("testKey5").getInt32Value(), is(2));
    }

    /**
     * Test and verify that a list of SimpleTypesMapEntry is properly converted to a Java native type Map object
     */
    @Test 
    public void testConvertListSimpleTypesMapEntrytoMap()
    {
        //Create a list of map entries
        SimpleTypesMapEntry testEntry1 = SimpleTypesMapEntry.newBuilder().setKey("something").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test1")).build();
 
        SimpleTypesMapEntry testEntry2 = SimpleTypesMapEntry.newBuilder().setKey("something2").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test2")).build();
        
        List<SimpleTypesMapEntry> testList = new ArrayList<SimpleTypesMapEntry>();   
        testList.add(testEntry1);
        testList.add(testEntry2);
        
        //convert the map list to dictionary object
        Map<String, String> convertedMap = SharedMessageUtils.convertListSimpleTypesMapEntrytoMap(testList);

        //verify values
        assertThat(testList.get(0).getValue().getStringValue(), is(convertedMap.get("something"))); 
        assertThat(testList.get(1).getValue().getStringValue(), is(convertedMap.get("something2"))); 
    }
    
    /**
     * Test and verify that a list of SimpleTypesMapEntry is properly converted to a Java native type Map object
     */
    @Test 
    public void testConvertListSimpleTypesMapEntrytoMapStringObject()
    {
        //Create a list of map entries
        SimpleTypesMapEntry testEntry1 = SimpleTypesMapEntry.newBuilder().setKey("something").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test1")).build();
 
        SimpleTypesMapEntry testEntry2 = SimpleTypesMapEntry.newBuilder().setKey("something2").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test2")).build();
        
        List<SimpleTypesMapEntry> testList = new ArrayList<SimpleTypesMapEntry>();   
        testList.add(testEntry1);
        testList.add(testEntry2);
        
        //convert the map list to dictionary object
        Map<String, Object> convertedMap = SharedMessageUtils.convertListSimpleTypesMapEntrytoMapStringObject(testList);

        //verify values
        assertThat(testList.get(0).getValue().getStringValue(), is(convertedMap.get("something"))); 
        assertThat(testList.get(1).getValue().getStringValue(), is(convertedMap.get("something2"))); 
    }
    
    /**
     * Test and verify that when there is a duplicate key entry in a list, IllegalArgumentException is caught
     */
    @Test 
    public void testConvertListSimpleTypesMapEntrytoMapException()
    {
        //Create a list of map entries with duplicate keys
        List<SimpleTypesMapEntry> testList = new ArrayList<SimpleTypesMapEntry>(); 
        SimpleTypesMapEntry testEntry = SimpleTypesMapEntry.newBuilder().setKey("something").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test1")).build();  
        SimpleTypesMapEntry testEntry1 = SimpleTypesMapEntry.newBuilder().setKey("something").
                setValue(Multitype.newBuilder().setType(Type.STRING).setStringValue("test2")).build();
        testList.add(testEntry);
        testList.add(testEntry1);
        
        //catch IllegalArgument exception during conversion
        try
        {
            SharedMessageUtils.convertListSimpleTypesMapEntrytoMap(testList);
            fail("Expecting Exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
     * Test and verify that a Java native type Map object is properly converted to a list of SimpleTypesMapEntry.
     */
    @Test
    public void testConvertMaptoListSimpleTypesMapEntry()
    {
        Map<String, String> testMap = new HashMap<String, String>();
        testMap.put("testKey", "testValue");
        
        testMap.put("testKey1", "yup" );
        
        List<SimpleTypesMapEntry> listTest = SharedMessageUtils.convertStringMapToListSimpleTypesMapEntry(testMap);
        Dictionary<String, Multitype> entryMap = new Hashtable<String, Multitype>();
        
        for (SimpleTypesMapEntry entry:listTest)        
        {
            entryMap.put(entry.getKey(), entry.getValue());
        }

        assertThat(entryMap.get("testKey").getStringValue(),is("testValue"));
        assertThat(entryMap.get("testKey1").getStringValue(), is("yup"));
    }
}
