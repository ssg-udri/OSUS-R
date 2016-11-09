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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestProtoFileParser
{
    private ProtoFileParser m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new ProtoFileParser();
    }
    
    @After
    public void tearDown()
    {
        File file = new File("./testFolder/test.proto");
        file.delete();
        file = new File("./testFolder/test2.proto");
        file.delete();
        File testFolder = new File("testFolder");
        testFolder.delete(); 
    }
    
    @Test
    public void testImportableMessages() throws IOException
    {
        File testFolder = new File("testFolder");
        testFolder.mkdir();
        BufferedWriter buff = new BufferedWriter(new FileWriter("./testFolder/test.proto"));
        buff.write("message Duration {" + System.getProperty("line.separator"));
        buff.write("  optional int32 months = 1;" + System.getProperty("line.separator"));
        buff.write("}" + System.getProperty("line.separator"));
        buff.write("message SomeType" + System.getProperty("line.separator"));
        buff.write("{" + System.getProperty("line.separator"));
        buff.write("  optional int32 somefield = 1" + System.getProperty("line.separator"));
        buff.write("}" + System.getProperty("line.separator"));
        buff.close();
        
        buff = new BufferedWriter(new FileWriter("./testFolder/test2.proto"));
        buff.write("message AnotherType {" + System.getProperty("line.separator"));
        buff.write("  optional int32 months = 1;" + System.getProperty("line.separator"));
        buff.write("}" + System.getProperty("line.separator"));
        buff.write("message Something" + System.getProperty("line.separator"));
        buff.write("{" + System.getProperty("line.separator"));
        buff.write("  optional int32 somefield = 1" + System.getProperty("line.separator"));
        buff.write("}" + System.getProperty("line.separator"));
        buff.close();
        
        String folderPath = "./testFolder";
        
        Map<String, String> importableMessages = m_SUT.parseProtoFilesForTypes(folderPath);
        
        assertThat(importableMessages.size(), is(4));
        assertThat(importableMessages.containsKey("Duration"), is(true));
        assertThat(importableMessages.get("Duration"), equalTo("test.proto"));
        assertThat(importableMessages.containsKey("SomeType"), is(true));
        assertThat(importableMessages.get("SomeType"), equalTo("test.proto"));
        assertThat(importableMessages.containsKey("AnotherType"), is(true));
        assertThat(importableMessages.get("AnotherType"), equalTo("test2.proto"));
        assertThat(importableMessages.containsKey("Something"), is(true));
        assertThat(importableMessages.get("Something"), equalTo("test2.proto"));
    }
}
