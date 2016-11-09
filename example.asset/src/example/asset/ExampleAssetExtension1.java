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
package example.asset;

/**
 * Purpose of interface is to test plug-in extensions.
 * @author dhumeniuk
 *
 */
public interface ExampleAssetExtension1
{
    /**
     * Method that should add suffix to asset's current name.
     */
    String addSuffix(String suffix);
}
