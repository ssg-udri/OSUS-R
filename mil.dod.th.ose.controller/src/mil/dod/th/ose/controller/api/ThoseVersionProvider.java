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
package mil.dod.th.ose.controller.api;

import java.util.Map;

/**
 * Provided as an OSGi service to get version info for the entire THOSE system.
 * @author dhumeniuk
 *
 */
public interface ThoseVersionProvider
{
    /**
     * File name of the file that holds build info like version information.
     */
    String BUILD_INFO_FILE_NAME = "buildinfo.properties";

    /**
     * Property name for the major version number, defined in {@link #BUILD_INFO_FILE_NAME}.
     */
    String MAJOR_NUM_PROP_NAME = "build.majornum";
    
    /**
     * Property name for the minor version number, defined in {@link #BUILD_INFO_FILE_NAME}.
     */
    String MINOR_NUM_PROP_NAME = "build.minornum";
    
    /**
     * Property name for the patch version number, defined in {@link #BUILD_INFO_FILE_NAME}.
     */
    String PATCH_NUM_PROP_NAME = "build.patchnum";
    
    /**
     * Property name for the metadata for the build, defined in {@link #BUILD_INFO_FILE_NAME}.
     */
    String METADATA_PROP_NAME = "build.metadata";
    
    /**
     * File name of the file that holds temporary build info like build number.
     */
    String TMP_BUILD_FILE_NAME = "tmp.build.properties";
    
    /**
     * Used to denote an individual build for the {@link #BUILD_TYPE_PROP_NAME} property.
     */
    String INDIVIDUAL_BUILD_TYPE = "ind";
    
    /**
     * Used to denote an automated build for the {@link #BUILD_TYPE_PROP_NAME} property.
     */
    String AUTOMATED_BUILD_TYPE = "automated";

    /**
     * Used to denote an unknown build for the {@link #BUILD_TYPE_PROP_NAME} property.
     */
    String UNKNOWN_BUILD_TYPE = "unknown";

    /**
     * Property name for the build type, either {@link #INDIVIDUAL_BUILD_TYPE}, {@link #AUTOMATED_BUILD_TYPE}, 
     * or {@link #UNKNOWN_BUILD_TYPE} defined in {@link #TMP_BUILD_FILE_NAME}.
     */
    String BUILD_TYPE_PROP_NAME = "build.type";
    
    /**
     * Property name for the branch from which the build is built.
     */
    String BUILD_BRANCH_PROP_NAME = "build.branch";
    
    /**
     * Property name for the build number, defined in {@link #TMP_BUILD_FILE_NAME}.
     */
    String BUILD_NUM_PROP_NAME = "build.buildnum";
    
    /**
     * Property name for the user that built the software, defined in {@link #TMP_BUILD_FILE_NAME} if build type is 
     * {@link #INDIVIDUAL_BUILD_TYPE}.
     */
    String BUILD_USER_PROP_NAME = "build.user";
    
    /**
     * Property name for the GIT hash value of the commit object against which the build was built.
     */
    String BUILD_GIT_HASH = "build.git.sha";

    /**
     * Get the version of the entire software system.
     * 
     * @return
     *      String representing the version
     * @throws IllegalStateException
     *      if unable to get version because the software was not built properly
     */
    String getVersion() throws IllegalStateException;
    
    /**
     * Get a Map containing build info like build workspace, build time.  The keys are the property names while 
     * the values are free form fields that are left up to the system developer to determine what is returned.
     * 
     * @return
     *      Contains build specific information.
     */
    Map<String, String> getBuildInfo();
    
}
