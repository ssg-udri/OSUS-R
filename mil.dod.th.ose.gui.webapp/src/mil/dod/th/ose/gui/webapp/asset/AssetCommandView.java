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

import java.util.UUID;

import org.primefaces.model.TreeNode;

/**
 * Interface for the bean that handles building the tree tables on the asset page command/control tab.
 * 
 * @author cweisenborn
 */
public interface AssetCommandView
{
    /**
     * Method that returns a tree which represents the command object passed to the method.
     * 
     * @param uuid
     *          UUID of the asset which the command pertains to.
     * @param command
     *          Command object that a tree is to be created for.
     * @return
     *          The root {@link TreeNode} for the tree that was created for the command object.
     */
    TreeNode getTree(UUID uuid, Object command);

    /**
     * Method that adds an object to the specified containing object.
     * 
     * @param nodeObject
     *              {@link CommandNodeModel} represents the containing object that a value will be added to.
     */
    void addField(CommandNodeModel nodeObject);
    
    /**
     * Method that removes an object from the specified containing object.
     * 
     * @param nodeObject
     *          {@link CommandNodeModel} represents the object that a value will be removed from the containing object.
     */
    void removeField(CommandNodeModel nodeObject);
}
