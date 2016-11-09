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
package mil.dod.th.ose.gui.webapp.asset;

/**
 * Class for a command manager which handles only get commands that do not have a 
 * corresponding set command.
 * @author nickmarcucci
 *
 */
public interface AssetGetCommandMgr
{
    /** 
     * Event topic prefix for all {@link AssetCommandMgr} events. 
     */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/asset/AssetGetCommandMgr/";
    
    /**
     * Topic used when a get command with no corresponding set command has been successfully executed
     * and a response for the command is available.
     */
    String TOPIC_GET_RESPONSE_RECEIVED = TOPIC_PREFIX + "GET_RESPONSE_RECEIVED";
    
    /**
     * Method used to retrieve the model that represents the supported commands for the specified asset.
     * 
     * @param model
     *          {@link AssetModel} that represents the asset to retrieve commands for.
     * @return
     *          asset get command model that represents the supported commands for the 
     *          specified asset. Will return null if no asset get command model can be 
     *          created for the specified asset.
     */
    AssetGetCommandModel getAssetCommands(AssetModel model);
    
}
