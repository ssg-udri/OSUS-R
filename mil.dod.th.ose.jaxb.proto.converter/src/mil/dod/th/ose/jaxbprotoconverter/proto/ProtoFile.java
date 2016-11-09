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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class used to store the information needed to generate a protocol buffer file. 
 * 
 * @author cweisenborn
 */
public class ProtoFile
{
    /**
     * The proto model this proto file is associated with.
     */
    private final ProtoModel m_Model;
    
    /**
     * Set of strings which represents all import statements for the proto file.
     */
    private final Set<String> m_Imports = new TreeSet<String>();
    
    /**
     * Reference XSD file which associated with the proto file.
     */
    private final File m_XsdFile;
    
    /**
     * Map of all proto message contained within the proto file. The key is the string which represents the name of the
     * proto message and the value is the model which represents the proto message.
     */
    private final Map<String, ProtoMessage> m_MessageMap = new TreeMap<String, ProtoMessage>();
    
    /**
     * Constructor that accepts the proto model and XSD file the proto file is associated with.
     * 
     * @param model
     *      The proto model the proto file is associated with.
     * @param xsdFile
     *      The XSD file the proto file is associated with.
     */
    public ProtoFile(final ProtoModel model, final File xsdFile)
    {
        m_Model = model;
        m_XsdFile = xsdFile;
    }
    
    /**
     * Returns the proto model the proto file is associated with.
     * 
     * @return
     *      The proto model this proto file is associated with.
     */
    public ProtoModel getProtoModel()
    {
        return m_Model;
    }
    
    /**
     * Returns the set of import statements needed by the proto file.
     * 
     * @return
     *      Set that represents the import statements for the proto file.
     */
    public Set<String> getImports()
    {
        return m_Imports;
    }
     
    /**
     * The XSD file associated with the proto file.
     * 
     * @return
     *      Return the associated XSD file.
     */
    public File getXsdFile()
    {
        return m_XsdFile;
    }
    
    /**
     * Returns the map of all proto messages contained within the proto file.
     * 
     * @return
     *      Returns the map of all proto messages contain within the proto file. The key is name of the proto message
     *      and the value is the model which represents the proto message.
     */
    public Map<String, ProtoMessage> getMessageMap()
    {
        return m_MessageMap;
    }
}
