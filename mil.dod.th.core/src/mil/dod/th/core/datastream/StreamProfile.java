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
package mil.dod.th.core.datastream;

import java.net.URI;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Interface for interacting with a streaming data profile configuration, which encapsulates
 * the required parameters for a client to receive and interpret a data stream.
 *  
 * <p>
 * Instances of a StreamProfile are managed (created, tracked, deleted) by the core. This interface 
 * should never be implemented by a plug-in. Instead, a plug-in implements {@link StreamProfileProxy} 
 * to define custom behavior that is invoked when consumers use this interface. To interact with a 
 * StreamProfile, use the {@link DataStreamService}.
 * 
 * 
 * @author jmiller
 *
 */
@ProviderType
public interface StreamProfile extends FactoryObject
{
    /** 
     * Each {@link StreamProfileProxy} implementation must provide a 
     * {@link org.osgi.service.component.ComponentFactory} with 
     * the factory attribute set to this constant.
     * 
     * <p>
     * For example:
     * 
     * <pre>
     * {@literal @}Component(factory = StreamProfile.FACTORY)
     * public class MyStreamProfile implements StreamProfileProxy
     * {
     *     ...
     * </pre>
     */
    String FACTORY = "mil.dod.th.core.datastream.StreamProfile";
    
    
    /**
     * The asset for which the profile corresponds.
     * 
     * @return Asset the asset reference
     */
    Asset getAsset();
    
    /**
     * The target output bitrate in kilobits per second (kbps).
     * 
     * @return bitrate A value less than 0 indicates that the data should be passed
     * through at its original bitrate with no transcoding.
     */
    double getBitrate();
    
    /**
     * Get the configuration for the stream profile.
     * 
     * <p>
     * <b>NOTE: the returned interface uses reflection to retrieve configuration and so values should be cached once 
     * retrieved</b>
     * 
     * @return configuration attributes for the stream profile
     */
    StreamProfileAttributes getConfig();
    
    /**
     * Same as {@link FactoryObject#getFactory()}, but returns the stream profile specific factory.
     * 
     * @return
     *      factory for the stream profile
     */
    @Override
    StreamProfileFactory getFactory();
    
    /**
     * <p>
     * Format of the output stream.  Must be a standard MIME type or a custom data format enumerated
     * in {@link mil.dod.th.core.datastream.capability.StreamProfileCapabilities}.
     * 
     * <p>
     * Some common video formats include:
     * 
     * <ul>
     * <li>video/mpeg
     * <li>video/mp4
     * </ul> 
     * 
     * @return streaming data format
     */
    String getFormat();

    /**
     * Optional sensor ID for assets that contain one or more sensors.
     * Can be null or empty if not needed.
     * 
     * @return sensor ID
     */
    String getSensorId();
    
    /**
     * Location of connection point, or "stream port" from which external clients can receive streaming data.
     * 
     * @return location of stream port
     */
    URI getStreamPort();
    
    /**
     * Reports whether the {@link StreamProfile} is enabled or disabled.
     * 
     * @return true if enabled; false otherwise
     */
    boolean isEnabled();
    
    /**
     * Set the enabled status of the {@link StreamProfile}.
     * 
     * @param enabled true to enable the profile; false to disable
     */
    void setEnabled(boolean enabled);
    
}
