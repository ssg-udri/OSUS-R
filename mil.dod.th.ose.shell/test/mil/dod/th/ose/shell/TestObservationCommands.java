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
package mil.dod.th.ose.shell;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;

import javax.xml.bind.MarshalException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.ose.utils.FileService;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * 
 * 
 * @author cweisenborn
 */
public class TestObservationCommands
{
    private ObservationCommands m_SUT;
    @Mock private XmlMarshalService m_XmlMarshalService;
    @Mock private ObservationStore m_ObsStore;
    @Mock private FileService m_FileService;
    @Mock private LoggingService m_LogService;
    @Mock private CommandSession m_CommandSession;
    @Mock private PrintStream m_Stream;
    @Mock private FileOutputStream m_FileOut;
    @Mock private File m_DirFile;
    @Mock private File m_XmlFile;
    @Mock private Observation m_Observation;
    
    private String m_UuidStr = "62748b12-a891-43b7-90d1-739f28dc2fdc";
    private byte[] m_XmlBytes = {1, 3, 5};
    private UUID m_Uuid = UUID.fromString(m_UuidStr);
    
    @Before
    public void setup() throws FileNotFoundException, MarshalException
    {
        MockitoAnnotations.initMocks(this);
        
        when(m_CommandSession.getConsole()).thenReturn(m_Stream);
        when(m_Observation.getUuid()).thenReturn(m_Uuid);
        when(m_ObsStore.find(m_Uuid)).thenReturn(m_Observation);
        when(m_XmlMarshalService.createXmlByteArray(m_Observation, true)).thenReturn(m_XmlBytes);
        when(m_FileService.getFile("exports")).thenReturn(m_DirFile);
        when(m_FileService.getFile(m_DirFile, m_UuidStr + ".xml")).thenReturn(m_XmlFile);
        when(m_FileService.createFileOutputStream(m_XmlFile)).thenReturn(m_FileOut);
        
        m_SUT = new ObservationCommands();
        m_SUT.setXmlMarshalService(m_XmlMarshalService);
        m_SUT.setObservationStore(m_ObsStore);
        m_SUT.setFileService(m_FileService);
        m_SUT.setLogService(m_LogService);
    }
    
    @Test
    public void testExportObsByUuid() throws MarshalException, IOException
    {
        m_SUT.exportObsByUuid(m_CommandSession, m_UuidStr);
        
        verify(m_ObsStore).find(m_Uuid);
        verify(m_XmlMarshalService).createXmlByteArray(m_Observation, true);
        verify(m_FileService).getFile(m_DirFile, m_UuidStr + ".xml");
        verify(m_FileService).createFileOutputStream(m_XmlFile);
        verify(m_FileOut).write(m_XmlBytes);
        verify(m_FileOut).flush();
        verify(m_Stream).format("Observation with UUID [%s] was successfully exported.%n", m_Uuid);
    }
    
    @Test
    public void testExportObsByUuidNull() throws FileNotFoundException, MarshalException
    {
        m_SUT.exportObsByUuid(m_CommandSession, null);
        
        verify(m_Stream).println("Please specify a UUID.");
        verify(m_ObsStore, never()).find(m_Uuid);
        verify(m_XmlMarshalService, never()).createXmlByteArray(m_Observation, true);
        verify(m_FileService, never()).getFile(m_DirFile, m_UuidStr + ".xml");
        verify(m_FileService, never()).createFileOutputStream(m_XmlFile);
    }
    
    @Test
    public void testExportObsByUuidObsNotFound() throws FileNotFoundException, MarshalException
    {
        when(m_ObsStore.find(m_Uuid)).thenReturn(null);
        
        m_SUT.exportObsByUuid(m_CommandSession, m_UuidStr);
        
        verify(m_Stream).format("No observation with UUID [%s] could be found.%n", m_Uuid);
        verify(m_ObsStore).find(m_Uuid);
        verify(m_XmlMarshalService, never()).createXmlByteArray(m_Observation, true);
        verify(m_FileService, never()).getFile(m_DirFile, m_UuidStr + ".xml");
        verify(m_FileService, never()).createFileOutputStream(m_XmlFile);
    }
    
    @Test
    public void testExportObs() throws MarshalException, IOException
    {
        m_SUT.exportObs(m_CommandSession, m_Observation);
        
        verify(m_XmlMarshalService).createXmlByteArray(m_Observation, true);
        verify(m_FileService).getFile(m_DirFile, m_UuidStr + ".xml");
        verify(m_FileService).createFileOutputStream(m_XmlFile);
        verify(m_FileOut).write(m_XmlBytes);
        verify(m_FileOut).flush();
        verify(m_Stream).format("Observation with UUID [%s] was successfully exported.%n", m_Uuid);
    }
    
    @Test
    public void testExportObsNull() throws MarshalException, FileNotFoundException
    {
        m_SUT.exportObs(m_CommandSession, null);
        
        verify(m_Stream).println("Please specify an observation to be exported.");
        verify(m_ObsStore, never()).find(m_Uuid);
        verify(m_XmlMarshalService, never()).createXmlByteArray(m_Observation, true);
        verify(m_FileService, never()).getFile(m_DirFile, m_UuidStr + ".xml");
        verify(m_FileService, never()).createFileOutputStream(m_XmlFile);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testExportObsMarshallException() throws MarshalException, FileNotFoundException
    {
        when(m_XmlMarshalService.createXmlByteArray(m_Observation, true)).thenThrow(MarshalException.class);
        
        m_SUT.exportObs(m_CommandSession, m_Observation);
        
        verify(m_LogService).warning(Mockito.any(MarshalException.class), 
                eq("Unable to convert observation with UUID [%s] to xml."), eq(m_Uuid));
        verify(m_Stream, never()).format("Observation with UUID [%s] was successfully exported.%n", m_Uuid);
        verify(m_XmlMarshalService).createXmlByteArray(m_Observation, true);
        verify(m_FileService, never()).getFile(m_DirFile, m_UuidStr + ".xml");
        verify(m_FileService, never()).createFileOutputStream(m_XmlFile);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testExportObsIoExceptioin() throws MarshalException, FileNotFoundException
    {
        when(m_FileService.getFile("exports")).thenThrow(IOException.class);
        
        m_SUT.exportObs(m_CommandSession, m_Observation);
        
        verify(m_LogService).warning(Mockito.any(IOException.class), 
                eq("Unable to wrtie observation with UUID [%s] to a file."), eq(m_Uuid));
        verify(m_Stream, never()).format("Observation with UUID [%s] was successfully exported.%n", m_Uuid);
        verify(m_XmlMarshalService).createXmlByteArray(m_Observation, true);
        verify(m_FileService, never()).getFile(m_DirFile, m_UuidStr + ".xml");
        verify(m_FileService, never()).createFileOutputStream(m_XmlFile);
    }
}
