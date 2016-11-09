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

import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;

import org.apache.felix.service.command.Descriptor;


/**
 * Shell commands for DataStreamService.
 * 
 * @author jmiller
 *
 */
@Component(provide = DataStreamServiceCommands.class, properties = { "osgi.command.scope=thstream",
    "osgi.command.function=getStreamProfiles|getStreamProfileByName|getStreamProfilesByAssetName" })
public class DataStreamServiceCommands
{
    /**
     * Reference to DataStreamService.
     */
    private DataStreamService m_DataStreamService;
    
    /**
     * Reference to AssetDirectoryService.
     */
    private AssetDirectoryService m_AssetDirectoryService;
    
    /**
     * Bind the DataStreamService instance.
     * 
     * @param dataStreamService
     *      the m_DataStreamService to set
     */
    @Reference
    public void setDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = dataStreamService;
    }
    
    /**
     * Bind the AssetDirectoryService instance.
     * 
     * @param assetDirectoryService
     *      the m_AssetDirectoryService to set
     */
    @Reference
    public void setAssetDirectoryService(final AssetDirectoryService assetDirectoryService)
    {
        m_AssetDirectoryService = assetDirectoryService;
    }
    
    /**
     * Returns all StreamProfile objects on the controller.
     * 
     * @return
     *      a set of {@link StreamProfile}s
     */
    @Descriptor("Returns all StreamProfile objects on the controller.")
    public Set<StreamProfile> getStreamProfiles()
    {
        return m_DataStreamService.getStreamProfiles();
    }
    
    /**
     * Returns StreamProfile object with the specified name.
     * 
     * @param name
     *      name of StreamProfile object
     * @return
     *      the StreamProfile object
     */
    public StreamProfile getStreamProfileByName(final String name)
    {
        for (StreamProfile profile : m_DataStreamService.getStreamProfiles())
        {
            if (profile.getName().equals(name))
            {
                return profile;
            }
        }
        
        return null;
    }
    
    /**
     * Returns all StreamProfile objects associated with a particular asset instance.
     * 
     * @param name
     *      name of the associated Asset
     * @return
     *      set of StreamProfile objects
     */
    @Descriptor("Returns all StreamProfile objects associated with a particular asset instance.")
    public Set<StreamProfile> getStreamProfilesByAssetName(
            @Descriptor("Name of the asset instance.")
            final String name)
    {
        final Asset asset = m_AssetDirectoryService.getAssetByName(name);
        return m_DataStreamService.getStreamProfiles(asset);
    }
    
    
}
