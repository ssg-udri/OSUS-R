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
package mil.dod.th.ose.controller.impl;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.ose.controller.api.ThoseVersionProvider;
import mil.dod.th.ose.utils.PropertyRetriever;

import org.osgi.framework.BundleContext;

/**
 * Provides version information for THOSE.
 * 
 * @author dhumeniuk
 */
@Component
public class ThoseVersionProviderImpl implements ThoseVersionProvider
{
    /**
     * Service to retrieve properties.
     */
    private PropertyRetriever m_PropRetriever;

    /**
     * Properties from {@link #BUILD_INFO_FILE_NAME}.
     */
    private Properties m_BuildInfoProps;

    /**
     * Properties from {@link #TMP_BUILD_FILE_NAME}.
     */
    private Properties m_TmpBuildProps;
    
    /**
     * The branch name of the branch from which the software was built.
     */
    private String m_BranchName;
    
    /**
     * The build type should be {@link #UNKNOWN_BUILD_TYPE}, {@link #AUTOMATED_BUILD_TYPE}, or
     * {@link #INDIVIDUAL_BUILD_TYPE}.
     */
    private String m_BuildType;
    
    /**
     * Bind to the service for reading properties.
     * 
     * @param propRetriever
     *      service used to get properties
     */
    @Reference
    public void setPropertyRetriever(final PropertyRetriever propRetriever)
    {
        m_PropRetriever = propRetriever;
    }

    /**
     * Activate this component by initializing the property maps with build information.
     * 
     * @param context
     *      context for the bundle containing the properties
     * @throws IOException
     *      if there is a failure to get properties from the expected location
     */
    @Activate
    public void activate(final BundleContext context) throws IOException
    {
        final URL infoPropsUrl = context.getBundle().getResource(BUILD_INFO_FILE_NAME);
        m_BuildInfoProps = m_PropRetriever.getPropertiesFromUrl(infoPropsUrl);

        final URL tmpPropsUrl = context.getBundle().getResource(TMP_BUILD_FILE_NAME);
        m_TmpBuildProps = m_PropRetriever.getPropertiesFromUrl(tmpPropsUrl);
        
        m_BuildType = (String)m_TmpBuildProps.get(BUILD_TYPE_PROP_NAME);
        
        //fetch the branch name, fail if it is null and an automated build type
        m_BranchName = (String)m_TmpBuildProps.get(BUILD_BRANCH_PROP_NAME);
        if (m_BuildType.equals(AUTOMATED_BUILD_TYPE) && m_BranchName == null)
        {
            throw new IllegalStateException("The branch property is null, and is required to accurately record" 
                    + " the build information.");
        }
    }
    
    @Override
    public String getVersion()
    {
        final StringBuilder builder = new StringBuilder();
        
        // base version string
        builder.append(m_BuildInfoProps.get(MAJOR_NUM_PROP_NAME)).append(".") // NOCHECKSTYLE: repeated string, 
               .append(m_BuildInfoProps.get(MINOR_NUM_PROP_NAME)).append(".") // easier to read this way
               .append(m_BuildInfoProps.get(PATCH_NUM_PROP_NAME));
        
        //release build flag
        boolean isRelease = false;
        
        switch (m_BuildType)
        {
            case AUTOMATED_BUILD_TYPE:
            {
                //if a release don't append branch and git hash
                if (m_BranchName.contains("release/"))
                { 
                    isRelease = true;
                }
                else
                {
                    final int endPointToAbbrHashTo = 7;
                    builder.append("-").append(m_BranchName).append(".").append(
                            m_TmpBuildProps.get(BUILD_GIT_HASH).toString().substring(0, endPointToAbbrHashTo));
                }
                break;
            }
            case INDIVIDUAL_BUILD_TYPE:
            {
                builder.append("-ind.").append(m_TmpBuildProps.get(BUILD_USER_PROP_NAME)).append(".").
                    append(m_TmpBuildProps.get(BUILD_NUM_PROP_NAME));
                break;
            }
            case UNKNOWN_BUILD_TYPE:
            {
                builder.append("-UNKNOWN.").append(m_TmpBuildProps.get(BUILD_USER_PROP_NAME)).append(".").
                    append(m_TmpBuildProps.get(BUILD_NUM_PROP_NAME));
                break;
            }
            default:
                throw new IllegalStateException(String.format("Invalid type [%s] for build", m_BuildType));
        }
        
        //don't append metadata if release build
        if (!isRelease)
        {
            final String metadata = m_BuildInfoProps.getProperty(METADATA_PROP_NAME);
            if (metadata != null)
            {
                builder.append("+").append(metadata);
            }
        }
        
        return builder.toString(); 
    }

    @SuppressWarnings({"rawtypes", "unchecked" }) //Rawtypes is the generic Map with no type specified. Unchecked is
    @Override                                 // the casting of the generic map to a the typified hashmap
    public Map<String, String> getBuildInfo()
    {
        return new HashMap<String, String>((Map)m_TmpBuildProps);
    }
}
