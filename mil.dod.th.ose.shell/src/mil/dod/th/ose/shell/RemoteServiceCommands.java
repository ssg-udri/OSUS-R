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

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;

import org.apache.felix.service.command.Descriptor;
import org.osgi.service.log.LogService;

/**
 * Provides commands to access remote interface services.
 * 
 * @author dhumeniuk
 *
 */
@Component(provide = RemoteServiceCommands.class, properties = { "osgi.command.scope=those",
    "osgi.command.function="
    + MissionProgramManager.JAXB_PROTO_OBJECT_CONVERTER    + "|"
    + MissionProgramManager.REMOTE_CHANNEL_LOOKUKP      + "|"
    + MissionProgramManager.MESSAGE_FACTORY             + "|"
    + MissionProgramManager.REMOTE_SYSTEM_ENCRYPTION
    })
public class RemoteServiceCommands
{
    private LoggingService m_LoggingService; 
    
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    private JaxbProtoObjectConverter m_JaxbProtoObjectConverter;  
    
    private MessageFactory m_MessageFactory;  
    
    private RemoteSystemEncryption m_RemoteSystemEncryption;

    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_LoggingService = logging;
    }
    
    @Reference
    public void setJaxbProtoObjectConverter(final JaxbProtoObjectConverter dataConverterDirectoryService)
    {
        m_JaxbProtoObjectConverter = dataConverterDirectoryService;
    }   
    
    @Reference
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
     
    @Reference
    public void setRemoteChannelLookup(final RemoteChannelLookup remoteChannelLookup)
    {
        m_RemoteChannelLookup = remoteChannelLookup;
    }
    
    /** 
     * Binds the remote system encryption service to use as the command. Remote interface does not require this service
     * so it may not be available.
     * 
     * @param remoteSystemEncryption
     *      service to use for the command
     */
    @Reference (optional = true, dynamic = true)
    public void setRemoteSystemEncryption(final RemoteSystemEncryption remoteSystemEncryption)
    {
        m_RemoteSystemEncryption = remoteSystemEncryption;
    }
    
    /**
     * Unbinds the device power manager service.
     * 
     * @param remoteSystemEncryption
     *              parameter not used, must match binding method signature
     */
    public void unsetRemoteSystemEncryption(final RemoteSystemEncryption remoteSystemEncryption)
    {
        m_RemoteSystemEncryption = null; //NOPMD null is used to unbind the service.
    }
    
    /**
     * Returns the TH core {@link RemoteChannelLookup} service.
     * 
     * @return service reference
     */
    @Descriptor("Terra Harvest RemoteChannelLookup")
    public RemoteChannelLookup rmtChnLkp()
    {
        return m_RemoteChannelLookup;
    }
    
    /**
     * Returns the service.
     * 
     * @return {@link JaxbProtoObjectConverter} reference
     */
    @Descriptor("Terra Harvest JaxbProtoObjectConverter")
    public JaxbProtoObjectConverter jxbPrtObjCnvrtr()
    {
        return m_JaxbProtoObjectConverter;
    }
    
    /**
     * Returns the message factory service.
     * 
     * @return {@link MessageFactory} reference
     */
    @Descriptor("Terra Harvest MessageFactory")
    public MessageFactory msgFty()
    {
        return m_MessageFactory;
    }
    
    /**
     * Returns the remote system encryption service or null if the service is not available on this system.
     * 
     * @return {@link RemoteSystemEncryption} reference
     */
    @Descriptor("Terra Harvest RemoteSystemEncryption, or null if the service is not available.")
    public RemoteSystemEncryption rmtSysEnct()
    {
        if (m_RemoteSystemEncryption == null)
        {
            m_LoggingService.log(LogService.LOG_INFO, "The remote system encryption service is currently unavailable!");
        }
        return m_RemoteSystemEncryption;
    }
}
