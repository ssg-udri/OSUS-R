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
package example.stream.profile;

import java.util.Map;
import java.util.Set;

import org.osgi.service.cm.ConfigurationException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileContext;
import mil.dod.th.core.datastream.StreamProfileException;
import mil.dod.th.core.datastream.StreamProfileProxy;
import mil.dod.th.core.datastream.capability.StreamProfileCapabilities;
import mil.dod.th.core.datastream.types.StreamFormat;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;

/**
 * @author jmiller
 *
 */
@Component(factory = StreamProfile.FACTORY)
public class ExampleStreamProfile implements StreamProfileProxy
{
    private LoggingService m_Log;
    private StreamProfileContext m_Context;
    private ExampleStreamProfileAttributes m_Attributes;
    
    @Reference
    public void setLogService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }

    @Override
    public void updated(Map<String, Object> props) throws ConfigurationException
    {
        m_Attributes = Configurable.createConfigurable(ExampleStreamProfileAttributes.class, props);
        
        m_Log.info("Updating example stream profile instance");
        m_Log.info("Associated asset: %s", m_Attributes.assetName());
        m_Log.info("Format: %s", m_Attributes.format());
        m_Log.info("Sensor Id: %s", m_Attributes.sensorId());
        m_Log.info("Bitrate [kbps]: %f", m_Attributes.bitrateKbps());
        m_Log.info("Data source: %s", m_Attributes.dataSource().toString());
        
        StreamProfileCapabilities capabilities = m_Context.getFactory().getStreamProfileCapabilities();
        if (m_Attributes.bitrateKbps() < capabilities.getMinBitrateKbps())
        {
            m_Log.warning("Bitrate %f kbps is less than minimum allowable (%f kbps)", 
                    m_Attributes.bitrateKbps(), capabilities.getMinBitrateKbps());
        }
        
        if (m_Attributes.bitrateKbps() > capabilities.getMaxBitrateKbps())
        {
            m_Log.warning("Bitrate %f kbps is greater than maximum allowable (%f kbps)", 
                    m_Attributes.bitrateKbps(), capabilities.getMaxBitrateKbps());
        }

        boolean formatAllowed = false;

        for (StreamFormat format : capabilities.getFormat())
        {
            if ((format.isSetMimeFormat() && format.getMimeFormat().equals(m_Attributes.format()))
                    || (format.isSetCustomFormat() && format.getCustomFormat().equals(m_Attributes.format())))
            {
                formatAllowed = true;
                break;
            }
        }

        if (!formatAllowed)
        {
            m_Log.warning("Format %s not supported", m_Attributes.format());
        }
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }

    @Override
    public void initialize(StreamProfileContext context, Map<String, Object> props) throws FactoryException
    {
        
        m_Attributes = Configurable.createConfigurable(ExampleStreamProfileAttributes.class, props);
        m_Context = context;
        
        m_Log.info("Initializing example stream profile instance");
        m_Log.info("Stream profile name: [%s]", m_Context.getName());
        m_Log.info("Associated asset: %s", m_Attributes.assetName());
        m_Log.info("Format: %s", m_Attributes.format());
        m_Log.info("Sensor Id: %s", m_Attributes.sensorId());
        m_Log.info("Bitrate [kbps]: %f", m_Attributes.bitrateKbps());
        m_Log.info("Data source: %s", m_Attributes.dataSource().toString());
        
    }

    @Override
    public void onEnabled() throws StreamProfileException
    {
        m_Log.info("Stream Profile %s enabled", m_Context.getName());

    }

    @Override
    public void onDisabled() throws StreamProfileException
    {
        m_Log.info("Stream Profile %s disabled", m_Context.getName());

    }
}
