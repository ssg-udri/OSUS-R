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
package mil.dod.th.ose.jaxbprotoconverter.proto;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to store all information from a JAXB class needed to generate a Google Protocol Buffer compatible
 * proto file.
 * 
 * @author Dave Humeniuk
 */
public class ProtoModel
{
    /**
     * Set of all proto enumeration models contained within the proto model.
     */
    private final Set<ProtoEnum> m_EnumSet = new HashSet<ProtoEnum>();
    
    /**
     * Map that contains proto file models which correlate 1 to 1 with an XSD schema file. The key is the XSD schema
     * file and value is the proto file model which represents protocol buffer file to be generated.
     */
    private final Map<File, ProtoFile> m_ProtoFileMap = new HashMap<File, ProtoFile>();
    
    /**
     * Returns the set of all known enumerations.
     * 
     * @return
     *      Set of all {@link ProtoEnum}s models contained within the proto model.
     */
    public Set<ProtoEnum> getEnums()
    {
        return m_EnumSet;
    }
    
    /**
     * Method that a map of proto file models. The key is the XSD schema file which the proto file model represents.
     * 
     * @return
     *      Map of the proto file models.
     */
    public Map<File, ProtoFile> getProtoFileMap()
    {
        return m_ProtoFileMap;
    }
}
