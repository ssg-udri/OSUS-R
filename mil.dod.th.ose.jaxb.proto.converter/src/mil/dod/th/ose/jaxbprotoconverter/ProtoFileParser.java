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
package mil.dod.th.ose.jaxbprotoconverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is capable of parsing a set of Protocol files for all messages within those Protocol Files. The class
 * returns a {@link Map} with the message name as the key and the Protocol file that message is in as the value.
 * 
 * @author cweisenborn
 */
public class ProtoFileParser
{   
    /**
     * This method retrieves all the available types for import from the protocol buffer files within the specified 
     * folder.
     * 
     * @param folderPath
     *          String representation of folder path.
     * @return
     *          A {@link Map} containing the names of all available types as the keys and the Protocol file the 
     *          messages are located in as the values.  
     * @throws IOException
     *          If a protocol buffer file cannot be found or read from properly.
     */
    public Map<String, String> parseProtoFilesForTypes(final String folderPath) 
            throws IOException
    {
        final Map<String, String> avaiableTypesForImport = new HashMap<String, String>();
        final File folder = new File(folderPath);
        final File[] arrayOfFiles = folder.listFiles();
        
        if (arrayOfFiles == null)
        {
            System.out.println("No files found in the specified folder path: " + folderPath);
            return avaiableTypesForImport;
        }
        

        for (File file : arrayOfFiles)
        {
            if (file.isFile())
            {
                final String fileName = file.getName();

                if (fileName.endsWith("proto"))
                {
                    try (FileReader fr = new FileReader(file); 
                            BufferedReader br = new BufferedReader(fr))
                    {
                        String line;
                    
                        while ((line = br.readLine()) != null)
                        {   
                            //Check if line defines a message.
                            final Pattern messagePattern = Pattern.compile("message[\\s]+([a-zA-Z]+).*$");
                            Matcher matcher = messagePattern.matcher(line);
                            if (matcher.lookingAt())
                            {
                                avaiableTypesForImport.put(matcher.group(1), fileName);
                            }
                            
                            //Check if a line defines an enumeration.
                            final Pattern enumPattern = Pattern.compile("enum[\\s]+([a-zA-Z]+).*$");
                            matcher = enumPattern.matcher(line);
                            if (matcher.lookingAt())
                            {
                                avaiableTypesForImport.put(matcher.group(1), fileName);
                            }
                        }
                    }
                }
            }
        }
        return avaiableTypesForImport;
    }
}
