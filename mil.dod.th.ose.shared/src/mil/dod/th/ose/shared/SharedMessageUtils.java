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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;

/**
 * Utility methods for working with shared Google protocol buffer messages.
 * 
 * @author dhumeniuk
 *
 */
final public class SharedMessageUtils
{
    /**
     * Private constructor to prevent instantiation.
     */
    private SharedMessageUtils()
    {

    }
    
    /**
     * Test if the value can be converted to a {@link Multitype}.  See {@link Type} for supported types.
     * 
     * @param value
     *      value to check
     * @return
     *      true if convertable, false if not
     */
    public static boolean isValueConvertableToMultitype(final Object value) //NOCHECKSTYLE: 
    {                                               //need to check for every supported type
        return value == null 
                || value instanceof Integer 
                || value instanceof Long 
                || value instanceof String 
                || value instanceof Boolean
                || value instanceof Float 
                || value instanceof Double
                || value instanceof Short
                || value instanceof Character
                || value instanceof Byte
                || value instanceof ByteString
                || value instanceof UUID
                || value instanceof List;
    }

    /**
     * Convert an object to a {@link Multitype} object that can be used within protocol buffer messages.
     * 
     * @param value
     *      object to convert 
     * @return
     *      object that contains the value of the object plus an enumeration that describes the type of data
     */
    @SuppressWarnings({"rawtypes"}) //NOCHECKSTYLE -> Complexity is 14; Max is 12. Need breakdown of if statements to identify correct object type.
    public static Multitype convertObjectToMultitype(final Object value) 
    {
        final Multitype.Builder builder = Multitype.newBuilder();
        if (value == null)
        {
            builder.setType(Type.NONE);
        }
        else if (value instanceof Integer)
        {
            builder.setType(Type.INT32);
            builder.setInt32Value((Integer)value);
        }
        else if (value instanceof Long)
        {
            builder.setType(Type.INT64);
            builder.setInt64Value((Long)value);
        }
        else if (value instanceof String)
        {
            builder.setType(Type.STRING);
            builder.setStringValue((String)value);
        }
        else if (value instanceof Boolean)
        {
            builder.setType(Type.BOOL);
            builder.setBoolValue((Boolean)value);
        }
        else if (value instanceof Float)
        {
            builder.setType(Type.FLOAT);
            builder.setFloatValue((Float)value);
        }
        else if (value instanceof Double)
        {
            builder.setType(Type.DOUBLE);
            builder.setDoubleValue((Double)value);
        }
        else if (value instanceof Short)
        {
            builder.setType(Type.SHORT);
            builder.setShortValue(((Short)value).intValue());
        }
        else if (value instanceof Character)
        {
            builder.setType(Type.CHARACTER);
            builder.setCharValue(((Character)value).toString());
        }
        else if (value instanceof Byte)
        {
            builder.setType(Type.BYTE);
            builder.setByteValue(((Byte)value).intValue());
        }
        else if (value instanceof ByteString)
        {
            builder.setType(Type.BYTES);
            builder.setByteStringValue((ByteString)value);
            
        }
        else if (value instanceof UUID)
        {
            final UUID uuid = (UUID)value;

            builder.setType(Type.UUID);
            builder.setUuidMostSigBitsValue(uuid.getMostSignificantBits());
            builder.setUuidLeastSigBitsValue(uuid.getLeastSignificantBits());
        }
        else if (value instanceof List)
        {
            builder.setType(Type.MULTITYPE_LIST);
            for (Object entry : (List)value)
            {
                builder.addList(convertObjectToMultitype(entry));
            }
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot convert object [%s] to Multitype, invalid type: %s", 
                            value, value.getClass()));
        }
        return builder.build();
    }

    /**
     * Convert a {@link Multitype} object to an object that can be used within the Java environment.
     * 
     * @param value
     *      object to convert
     * @return
     *      converted object, type is based on the type dictated by the multitype value
     */
    @SuppressWarnings("unchecked") //NOCHECKSTYLE -> Complexity 14; Max is 12. Need all switch cases to supply supported types
    public static Object convertMultitypeToObject(final Multitype value) 
    {
        switch (value.getType()) 
        {
            case NONE:
                return null;
                
            case INT32:
                return value.getInt32Value();
                
            case INT64:
                return value.getInt64Value();
                
            case STRING:
                return value.getStringValue();
                
            case BOOL:
                return value.getBoolValue();
                
            case FLOAT:
                return value.getFloatValue();
                
            case DOUBLE:
                return value.getDoubleValue();
                
            case SHORT:
                return ((Integer)value.getShortValue()).shortValue();
               
            case CHARACTER:
                return value.getCharValue().charAt(0);
                
            case BYTE:
                return ((Integer)value.getByteValue()).byteValue();
                
            case BYTES:
                return value.getByteStringValue();
                
            case UUID:
                return new UUID(value.getUuidMostSigBitsValue(), value.getUuidLeastSigBitsValue());

            case MULTITYPE_LIST:
                @SuppressWarnings("rawtypes")
                final List list = new ArrayList();
                for (Multitype multi : value.getListList())
                {
                    list.add(convertMultitypeToObject(multi));
                }
                return list;
                
            default:
                throw new IllegalArgumentException(
                        String.format("Multitype value contains an unknown type: %s", value.getType()));
        }
    }
    
    /**
     * Convert an enum ordinal back to the ordinal value.
     * 
     * @param <E>
     *      type of the enum
     * @param values
     *      possible values of the enum
     * @param ordinal
     *      ordinal to convert
     * @return
     *      enum value with the given ordinal
     */
    public static <E extends Enum<?>> E convertEnumOrdinalToValue(final E[] values, final int ordinal)
    {
        for (E value : values)
        {
            if (value.ordinal() == ordinal)
            {
                return value;
            }
        }
        throw new IllegalArgumentException(String.format("Ordinal %d is invalid for type: %s", ordinal,
                values[0].getClass().getName()));
    }
    
    /**
     * Convert Java UUID object to UUID protocol buffer message.  
     * @param uuid
     *      Java UUID to be converted.
     * @return
     *      Protocol buffer version of the given UUID.
     */
    public static SharedMessages.UUID convertUUIDToProtoUUID(final UUID uuid)
    {
        if (uuid != null)
        {
            final SharedMessages.UUID uuidBuilder = SharedMessages.UUID.newBuilder().
                    setLeastSignificantBits(uuid.getLeastSignificantBits()).
                    setMostSignificantBits(uuid.getMostSignificantBits()).build();

            return uuidBuilder;
        }
        throw new IllegalArgumentException("not a valid UUID"); 
    }
    
    /**
     * Convert a protocol buffer UUID message object to a Java UUID object.  
     * @param uuid
     *      Protocol buffer UUID to be converted.
     * @return
     *      Java UUID version of the given protocol buffer UUID.  
     */
    public static UUID convertProtoUUIDtoUUID(final SharedMessages.UUID uuid) 
    {
        if (uuid != null)
        {
            return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()); 
        }
  
        throw new IllegalArgumentException("not a valid UUID message");
    }

    /**
     * Creates a {@link FactoryObjectInfo} message from the given factory object.
     * @param factoryObject
     *     the factory object
     * @return 
     *     message representing the factory object
     */
    public static FactoryObjectInfo createFactoryObjectInfoMessage(final FactoryObject factoryObject)
    {
        Preconditions.checkNotNull(factoryObject);
        
        return createFactoryObjectInfoMessage(factoryObject.getPid(), factoryObject.getUuid(), 
                factoryObject.getFactory());
    } 
    
    /**
     * Creates a {@link FactoryObjectInfo} message from the given pid, uuid, and class name.
     * @param pid
     *  the pid of the factory object (may be null).
     * @param uuid
     *  the uuid of the factory object.
     * @param factory
     *  the factory that created the factory object.
     * @return
     *  the message representing the factory object.
     */
    public static FactoryObjectInfo createFactoryObjectInfoMessage(final String pid, 
            final UUID uuid, final FactoryDescriptor factory)
    {
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(factory);
        
        final FactoryObjectInfo.Builder builder = FactoryObjectInfo.newBuilder();
        
        if (pid != null)
        {
            builder.setPid(pid);
        }
        
        builder.setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).setProductType(factory.getProductType());
            
        return builder.build();
    }

    /**
     * Converts a list of SimpleTypesMapEntry used by the remote interface to a native java type Dictionary object.
     * @param listMap
     *    list of map entries
     * @return
     *    Dictionary object that has the keys and converted values   
     */
    public static Dictionary<String, Object> convertMaptoDictionary(final List<SimpleTypesMapEntry> listMap) 
    {
        final Dictionary<String, Object> dictionaryMap = new Hashtable<String, Object>();
        for (SimpleTypesMapEntry entry:listMap)        
        {
            if (dictionaryMap.get(entry.getKey()) != null)
            {
                throw new IllegalArgumentException("duplicate key"); 
                
            }
            dictionaryMap.put(entry.getKey(), convertMultitypeToObject(entry.getValue()));   

        }
        return dictionaryMap;
         
    }
    
    /**
     * Converts a native java type Dictionary object to a list of SimpleTypesMapEntry used by the remote interface.
     * @param dictionaryMap
     *    Dictionary object that has the keys and values. 
     *    The map must contain objects that can be converted to a {@link Multitype}. 
     * @throws IllegalArgumentException
     *   thrown when a duplicate key or a non multitype object is used 
     * @return
     *    List of SimpleTypesMapEntry object
     */
    public static List<SimpleTypesMapEntry> convertDictionarytoMap(final Dictionary<String, Object> dictionaryMap) 
            throws IllegalArgumentException 
    {
        final List<SimpleTypesMapEntry> listMap = new ArrayList<SimpleTypesMapEntry>();
        final Enumeration<String> keyEnum = dictionaryMap.keys();
        while (keyEnum.hasMoreElements())   
        {
            final String key = keyEnum.nextElement();
            final Multitype entryValue = convertObjectToMultitype(dictionaryMap.get(key));
            final SimpleTypesMapEntry entry = SimpleTypesMapEntry.newBuilder().
                    setValue(entryValue).setKey(key).build();
            listMap.add(entry);      
        }
        return listMap;
         
    }

    /**
     * Converts a Map of strings to a list of the SimpleTypesMapEntry type which can be used by the remote interface 
     * for proto messages.
     * 
     * @param map
     *    Map object to convert
     * @return
     *    List of SimpleTypesMapEntry objects   
     */
    public static List<SimpleTypesMapEntry> convertStringMapToListSimpleTypesMapEntry(final Map<String, String> map) 
    {
        final List<SimpleTypesMapEntry> listMap = new ArrayList<SimpleTypesMapEntry>();
        for (String key: map.keySet())        
        {
            final SimpleTypesMapEntry entry = SimpleTypesMapEntry.newBuilder().
                    setValue(Multitype.newBuilder().
                            setType(Type.STRING).
                            setStringValue(map.get(key)).build()).setKey(key).build();
            listMap.add(entry);
        }
        return listMap;
    }
    
    /**
     * Converts a Map of objects to a list of the SimpleTypesMapEntry type which can be used by the remote interface for
     * proto messages.
     * 
     * @param map
     *    Map object to convert
     * @return
     *    List of SimpleTypesMapEntry objects   
     */
    public static List<SimpleTypesMapEntry> convertMapToListSimpleTypesMapEntry(final Map<String, Object> map) 
    {
        final List<SimpleTypesMapEntry> listMap = new ArrayList<SimpleTypesMapEntry>();
        for (String key: map.keySet())        
        {
            final Multitype value = convertObjectToMultitype(map.get(key));
            final SimpleTypesMapEntry entry = SimpleTypesMapEntry.newBuilder().setValue(value).setKey(key).build();
            listMap.add(entry);
        }
        return listMap;
    }

    /**
     * Converts a list of SimpleTypesMapEntry used by the remote interface to a native java type Map object.
     * @param listMap
     *    list of map entries
     * @return
     *    Map object that has the keys and converted values   
     */
    public static Map<String, String> convertListSimpleTypesMapEntrytoMap(final List<SimpleTypesMapEntry> listMap) 
    {
        final Map<String, String> map = new HashMap<String, String>();
        for (SimpleTypesMapEntry entry: listMap)        
        {
            if (map.get(entry.getKey()) != null)
            {
                throw new IllegalArgumentException("Map already contains key"); 
                
            }
            if (entry.getValue().getType() != Type.STRING)
            {
                throw new IllegalArgumentException("Value is not a string"); 
                
            }
            map.put(entry.getKey(), (String)convertMultitypeToObject(entry.getValue()));   
    
        }
        return map;
    }
    
    /**
     * Converts a list of SimpleTypesMapEntry used by the remote interface to a native java type Map object.
     * @param listMap
     *    list of map entries
     * @return
     *    Map object that has the keys and converted values   
     */
    public static Map<String, Object> convertListSimpleTypesMapEntrytoMapStringObject(
            final List<SimpleTypesMapEntry> listMap) 
    {
        final Map<String, Object> map = new HashMap<>();
        for (SimpleTypesMapEntry entry: listMap)        
        {
            if (map.get(entry.getKey()) != null)
            {
                throw new IllegalArgumentException("The Map cannot have duplicate keys."); 
                
            }
            map.put(entry.getKey(), convertMultitypeToObject(entry.getValue()));   
    
        }
        return map;
    }
}
