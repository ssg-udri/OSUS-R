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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.xml.bind.MarshalException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.ose.utils.FileService;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;

/**
 * Observation related commands.
 * 
 * @author cweisenborn
 */
@Component(provide = ObservationCommands.class, properties = {"osgi.command.scope=thobs", 
    "osgi.command.function=exportObsByUuid|exportObs"})
public class ObservationCommands
{
    private static final String EXPORT_SUCCESSFUL = "Observation with UUID [%s] was successfully exported.%n";
    private XmlMarshalService m_XmlMarshalService;
    private ObservationStore m_ObsStore;
    private FileService m_FileService;
    private LoggingService m_LogService;
    
    @Reference
    public void setXmlMarshalService(final XmlMarshalService xmlMarshalService)
    {
        m_XmlMarshalService = xmlMarshalService;
    }
    
    @Reference
    public void setObservationStore(final ObservationStore obsStore)
    {
        m_ObsStore = obsStore;
    }
    
    @Reference
    public void setFileService(final FileService fileService)
    {
        m_FileService = fileService;
    }
    
    @Reference
    public void setLogService(final LoggingService logService)
    {
        m_LogService = logService;
    }
    
    /**
     * Exports the observation with the specified UUID to an XML file.
     * 
     * @param commandSession
     *      Command session used to access the console.
     * @param obsUuidStr
     *      String which represents the UUID of the observation to be exported.
     */
    @Descriptor("Exports the observation with specified UUID as an XML file to the exports folder.")
    public void exportObsByUuid(final CommandSession commandSession, 
            @Descriptor("String that represents the UUID of the observation to be exported.")
            final String obsUuidStr)
    {
        if (obsUuidStr == null)
        {
            commandSession.getConsole().println("Please specify a UUID.");
            return;
        }
        final UUID obsUuid = UUID.fromString(obsUuidStr);
        final Observation obsToExport = m_ObsStore.find(obsUuid);
        if (obsToExport == null)
        {
            commandSession.getConsole().format("No observation with UUID [%s] could be found.%n", obsUuid);
            return;
        }
        
        if (convertAndWrite(obsToExport))
        {
            commandSession.getConsole().format(EXPORT_SUCCESSFUL, obsUuid);
        }
    }
    
    /**
     * Exports the specified observation as an XML file.
     * 
     * @param commandSession
     *      Command session used to access the console.
     * @param observation
     *      The observation to be exported.
     */
    @Descriptor("Exports the specified observation as an XML file to the exports folder.")
    public void exportObs(final CommandSession commandSession,
            @Descriptor("The observation to be exported.")
            final Observation observation)
    {
        if (observation == null)
        {
            commandSession.getConsole().println("Please specify an observation to be exported.");
            return;
        }
        
        if (convertAndWrite(observation))
        {
            commandSession.getConsole().format(EXPORT_SUCCESSFUL, observation.getUuid());
        }
    }
    
    /**
     * Converts the specified observation to XML and then writes the XML data to a file.
     * 
     * @param obs
     *      Observation to be converted and written to a file.
     * @return
     *      True if the observation was successfully converted/written and false otherwise.
     */
    private boolean convertAndWrite(final Observation obs)
    {
        final byte[] xmlBytes;
        try
        {
            xmlBytes = m_XmlMarshalService.createXmlByteArray(obs, true);
        }
        catch (final MarshalException ex)
        {
            m_LogService.warning(ex, "Unable to convert observation with UUID [%s] to xml.", obs.getUuid());
            return false;
        }
        
        try
        {
            writeObsFile(obs.getUuid(), xmlBytes);
        }
        catch (final IOException ex)
        {
            m_LogService.warning(ex, "Unable to wrtie observation with UUID [%s] to a file.", obs.getUuid());
            return false;
        }
        return true;
    }
    
    /**
     * Method that writes an XML file using the specified bytes.
     * 
     * @param obsUuid
     *      UUID of the observation the bytes represent. The filename will be based off of this.
     * @param obsXmlBytes
     *      Byte array that represents the XML file to be written.
     * @throws IOException
     *      Thrown if the file cannot be created or an issue writing to the file occurs.
     */
    private void writeObsFile(final UUID obsUuid, final byte[] obsXmlBytes) throws IOException
    {
        final File exportDir = m_FileService.getFile("exports");
        if (!m_FileService.doesFileExist(exportDir))
        {
            m_FileService.mkdir(exportDir);
        }
        
        final File xmlFile = m_FileService.getFile(exportDir, obsUuid + ".xml");
        try (final FileOutputStream fileOut = m_FileService.createFileOutputStream(xmlFile))
        {
            fileOut.write(obsXmlBytes);
            fileOut.flush();
        }
    }
}
