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
package mil.dod.th.ose.config.loading.impl;

import java.util.List;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.model.config.EventConfig;
import mil.dod.th.ose.config.loading.RemoteEventLoader;
import mil.dod.th.ose.remote.api.RemoteEventAdmin;
import mil.dod.th.remote.converter.LexiconFormatEnumConverter;

/**
 * This service reads in the remote events denoted in the configs.xml and registers them with the
 * {@link RemoteEventAdmin}. 
 * 
 * @author allenchl
 */
@Component
public class RemoteEventLoaderImpl implements RemoteEventLoader
{
    private RemoteEventAdmin m_RemoteEventAdmin;
    private LoggingService m_Log;
    
    @Reference
    public void setRemoteEventAdmin(final RemoteEventAdmin remoteEventAdmin)
    {
        m_RemoteEventAdmin = remoteEventAdmin;
    }
    
    @Reference
    public void setLoggingService(final LoggingService logService)
    {
        m_Log = logService;
    }
    
    @Override
    public void process(final List<EventConfig> eventConfigs)
    {
        for (EventConfig config : eventConfigs)
        {
            try
            {
                final EventRegistrationRequestData.Builder data = EventRegistrationRequestData.newBuilder()
                    .setExpirationTimeHours(config.getExpirationTimeHours())
                    .setCanQueueEvent(config.isCanQueueEvent())
                    .addAllTopic(config.getEventTopics())
                    .setObjectFormat(LexiconFormatEnumConverter.convertJavaEnumToProto(config.getObjectFormat()));
                if (config.isSetEventFilter())
                {
                    data.setFilter(config.getEventFilter());
                }
                
                m_Log.debug("Loading event registration for system %d: %s", config.getSystemId(), data);
                m_RemoteEventAdmin.addRemoteEventRegistration(config.getSystemId(), data.build());
            }
            catch (final Exception e)
            {
                m_Log.error(e, "Unable to load a remote event config: %s", config);
            }
        }
    }
}
