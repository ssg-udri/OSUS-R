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
package mil.dod.th.ose.gui.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.MarshalException;

import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.xml.XmlMarshalService;

/**
 * Test class for {@link MissionExport}.
 * 
 * @author cweisenborn
 */
public class TestMissionExport
{
    private MissionExport m_SUT;
    private TemplateProgramManager m_TemplateProgramManager;
    private XmlMarshalService m_XMLMarshalService;
    
    @Before
    public void setup()
    {
        m_TemplateProgramManager = mock(TemplateProgramManager.class);
        m_XMLMarshalService = mock(XmlMarshalService.class);
        
        m_SUT = new MissionExport();
        m_SUT.setTemplateProgramManager(m_TemplateProgramManager);
        m_SUT.setXMLMarshalService(m_XMLMarshalService);
    }
    
    /**
     * Test the handleMissionExport method.
     * Verify that the information stored within the download file is correct after being set by the handleMissionExport
     * method.
     */
    @Test
    public void testHandleMissionExport() throws MarshalException, IOException
    {
        String missionName = "TestMission";
        byte[] testArr = {100, 101, 102};
        MissionProgramTemplate testTemp = mock(MissionProgramTemplate.class);
        when(m_TemplateProgramManager.getTemplate(missionName)).thenReturn(testTemp);
        when(m_XMLMarshalService.createXmlByteArray(testTemp, true)).thenReturn(testArr);
        
        m_SUT.handleExportMission(missionName);
        //Verify that the exporting mission's name is correct.
        assertThat(m_SUT.getDownloadFile().getName(), is(missionName + ".xml"));
        
        int readNum;
        int index = 0;
        InputStream is = m_SUT.getDownloadFile().getStream();
        //Verify the information stored within the streamed content is correct.
        while ((readNum = is.read()) != -1)
        { 
            assertThat(testArr[index], is((byte)readNum));
            index++;
        }
    }
}
