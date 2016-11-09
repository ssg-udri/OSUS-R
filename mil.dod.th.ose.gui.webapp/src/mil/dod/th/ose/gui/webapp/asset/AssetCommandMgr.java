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

import mil.dod.th.core.types.command.CommandTypeEnum;

/**
 * Interface for the bean that manages all {@link AssetSyncableCommandModel} for all known assets. 
 * Handles sending/receiving messages for syncing command data from the controller and sending 
 * commands to be executed by an asset on the specified controller.
 * 
 * @author cweisenborn
 */
public interface AssetCommandMgr
{
    /** 
     * Event topic prefix for all {@link AssetCommandMgr} events. 
     */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/asset/AssetCommandMgr/";
    
    /**
     * Topic used when a command's values have been synced.
     */
    String TOPIC_COMMAND_SYNCED = TOPIC_PREFIX + "COMMAND_SYNCED";
    
    /**
     * Method used to retrieve the model that represents the supported commands for the specified asset.
     * 
     * @param model
     *          {@link AssetModel} that represents the asset to retrieve commands for.
     * @return
     *          {@link AssetSyncableCommandModel} that represents the supported commands for the 
     *          specified asset. Will return null if no {@link AssetSyncableCommandModel} can be 
     *          created for the specified asset.
     */
    AssetSyncableCommandModel getAssetCommands(AssetModel model);

    /**
     * Method that makes the remote call to retrieve values for the all currently supported commands for the specified
     * asset.
     * 
     * @param controllerId
     *          ID of the controller where the asset is located.
     * @param model
     *          Model that contains the commands that can be synced.
     */
    void syncCall(int controllerId, AssetSyncableCommandModel model);
    
    /**
     * Method that makes the remote call to retrieve the current settings for a command of an asset.
     * 
     * @param controllerId
     *          ID of the controller where the asset is located.
     * @param model
     *          {@link AssetSyncableCommandModel} that contains the command to be synced with the current 
     *          settings of the remote asset.
     * @param commandType
     *          The enumeration that represents the set command that is to be synced with the current settings of the 
     *          remote asset.
     */
    void doSync(int controllerId, AssetSyncableCommandModel model, CommandTypeEnum commandType);
    
}
