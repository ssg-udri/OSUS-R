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
package mil.dod.th.ose.sdk.those;

/**
 * Defines the different THOSE SDK project types.
 * 
 * @author dlandoll
 */
public enum ProjectType
{
    /**
     * Asset project type.
     */
    PROJECT_ASSET,

    /**
     * Physical link project type.
     */
    PROJECT_PHYLINK,

    /**
     * Link layer project type.
     */
    PROJECT_LINKLAYER,

    /**
     * Transport layer project type.
     */
    PROJECT_TRANSPORTLAYER
}
