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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.osgi.Jar;

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

/**
 * Manages all of the project related properties used to generate files from the templates.
 * 
 * @author dlandoll
 */
public class ProjectProperties
{
    /** Property definition for the base package. */
    public static final String PROP_BASE_PACKAGE = "basePackage";

    /** Property definition for extra code to add to the PhysicalLinkProxy class impl. */
    public static final String PROP_PL_EXTRA_CODE = "physicalLinkExtraCode";

    /** Property definition for the attribute type for the plug-in. */
    public static final String PROP_ATTR_TYPE = "attrType";

    /** Property definition for the proxy type for the plug-in. */
    public static final String PROP_PROXY_TYPE = "proxyType";

    /** Property definition for SDK directory. */
    private static final String PROP_SDK_DIR = "sdk";

    /** The default child directory under the SDK directory where the resources are stored. */
    private static String SDK_SUB_DIRECTORY = "api";

    /** Property definition for project name. */
    private static final String PROP_PROJ_NAME = "name";

    /** Property definition for class name. */
    private static final String PROP_CLASS_NAME = "class";

    /** Property definition for package name. */
    private static final String PROP_PKG_NAME = "package";

    /**
     * Property definition for the actual physical link type.
     */
    private static final String PROP_PHYS_TYPE = "physType";

    /** Property definition for vendor. */
    private static final String PROP_VENDOR = "vendor";

    /** Property definition for project description. */
    private static final String PROP_DESCRIPTION = "description";

    /** Property definition for author. */
    private static final String PROP_AUTHOR = "author";

    /** Property definition for task. Used in place of instruction TO_DO's in the templates. */
    private static final String PROP_TASK = "task";

    /** Name of the jar. */
    private static final String JAR_NAME = "mil.dod.th.ose.sdk.jar";

    /** The java compiler target. Example: 1.8. */
    private static final String PROP_JAVAC_TARGET = "javacTarget";

    /**
     * Map of the project properties.
     */
    private final Map<String, Object> m_Properties;

    /**
     * Project type.
     */
    private ProjectType m_ProjectType;

    /**
     * Service for interacting with the Bnd library.
     */
    private BndService m_BndService;

    /**
     * Creates default map of project properties.
     * 
     * @param bndService
     *            Bnd library service to inject
     */
    public ProjectProperties(final BndService bndService)
    {
        m_BndService = bndService;

        m_Properties = new HashMap<String, Object>();

        m_Properties.put(PROP_SDK_DIR, locateSdkDirectory());
        m_Properties.put(PROP_PROJ_NAME, "example-project");
        m_Properties.put(PROP_CLASS_NAME, "ExampleClass");
        m_Properties.put(PROP_PKG_NAME, "example.project");
        m_Properties.put(PROP_VENDOR, "Terra Harvest Vendor");
        m_Properties.put(PROP_DESCRIPTION, "Example SDK Plug-in");
        m_Properties.put(PROP_AUTHOR, System.getProperty("user.name"));
        m_Properties.put(PROP_TASK, "TODO"); // this TO_DO is used as a string value
        m_Properties.put(PROP_JAVAC_TARGET, "1.8");

        // Default to an asset project
        m_ProjectType = ProjectType.PROJECT_ASSET;

        // There is no need to define a SDK_SUB_DIRECTORY environment variable in our production environment.
        // Set this environment variable to "resources" to run locally in eclipse.
        final String sdkSubDirectory = System.getenv("SDK_SUB_DIRECTORY");
        if (sdkSubDirectory != null && sdkSubDirectory.trim().length() > 0)
        {
            SDK_SUB_DIRECTORY = sdkSubDirectory;
        }
    }

    /**
     * Returns a map containing all project properties used for project generation.
     * 
     * @return Map of project properties
     */
    public Map<String, Object> getProperties()
    {
        return m_Properties;
    }

    /**
     * Returns the javac target version.
     * 
     * @return the javac target version. Example: 1.8.
     */
    public String getJavacTarget()
    {
        return (String)m_Properties.get(PROP_JAVAC_TARGET);
    }

    /**
     * Sets the javac target compiler version.
     * 
     * @param javacTarget
     *            the javac target version. Example: 1.8.
     */
    public void setJavacTarget(final String javacTarget)
    {
        m_Properties.put(PROP_JAVAC_TARGET, javacTarget);
    }

    /**
     * Returns the SDK directory path.
     * 
     * @return SDK directory path
     */
    public String getSdkDirectory()
    {
        return (String)m_Properties.get(PROP_SDK_DIR);
    }

    /**
     * Set the SDK directory property.
     * 
     * @param sdkDir
     *            property value
     */
    public void setSdkDirectory(final String sdkDir)
    {
        m_Properties.put(PROP_SDK_DIR, sdkDir);
    }

    /**
     * Returns the project name.
     * 
     * @return project name
     */
    public String getProjectName()
    {
        return (String)m_Properties.get(PROP_PROJ_NAME);
    }

    /**
     * Set the project name property.
     * 
     * @param projectName
     *            property value
     */
    public void setProjectName(final String projectName)
    {
        m_Properties.put(PROP_PROJ_NAME, projectName);
    }

    /**
     * Returns the class name.
     * 
     * @return class name
     */
    public String getClassName()
    {
        return (String)m_Properties.get(PROP_CLASS_NAME);
    }

    /**
     * Set the class name property.
     * 
     * @param className
     *            property value
     */
    public void setClassName(final String className)
    {
        m_Properties.put(PROP_CLASS_NAME, className);
    }

    /**
     * Returns the package name.
     * 
     * @return package name
     */
    public String getPackageName()
    {
        return (String)m_Properties.get(PROP_PKG_NAME);
    }

    /**
     * Set the package name property.
     * 
     * @param packageName
     *            property value
     */
    public void setPackageName(final String packageName)
    {
        m_Properties.put(PROP_PKG_NAME, packageName);
    }

    /**
     * Set the attribute type for the project.
     * 
     * @param attributeType
     *            the attribute type for the project
     */
    public void setAttributeType(final String attributeType)
    {
        m_Properties.put(PROP_ATTR_TYPE, attributeType);
    }

    /**
     * Set proxy type for the project.
     * 
     * @param proxyType
     *            the string for the type of proxy that the plugin needs
     */
    public void setProxyType(final String proxyType)
    {
        m_Properties.put(PROP_PROXY_TYPE, proxyType);
    }

    /**
     * Get the physical link type for the project. Only applies if project is a physical link project.
     * 
     * @return the physical link type
     */
    public PhysicalLinkTypeEnum getPhysicalLinkTypeEnum()
    {
        return (PhysicalLinkTypeEnum)m_Properties.get(PROP_PHYS_TYPE);
    }

    /**
     * Sets the physical link type for the project.
     * 
     * @param physType
     *            the type of the physical link
     */
    public void setPhysicalLinkTypeEnum(final PhysicalLinkTypeEnum physType)
    {
        m_Properties.put(PROP_PHYS_TYPE, physType);
    }

    /**
     * Returns the vendor.
     * 
     * @return vendor
     */
    public String getVendor()
    {
        return (String)m_Properties.get(PROP_VENDOR);
    }

    /**
     * Set the vendor property.
     * 
     * @param vendor
     *            property value
     */
    public void setVendor(final String vendor)
    {
        m_Properties.put(PROP_VENDOR, vendor);
    }

    /**
     * Returns the project description.
     * 
     * @return project description
     */
    public String getDescription()
    {
        return (String)m_Properties.get(PROP_DESCRIPTION);
    }

    /**
     * Set the project description property.
     * 
     * @param description
     *            property value
     */
    public void setDescription(final String description)
    {
        m_Properties.put(PROP_DESCRIPTION, description);
    }

    /**
     * Returns the project author.
     * 
     * @return project author
     */
    public String getAuthor()
    {
        return (String)m_Properties.get(PROP_AUTHOR);
    }

    /**
     * Set the project author property.
     * 
     * @param author
     *            property value
     */
    public void setAuthor(final String author)
    {
        m_Properties.put(PROP_AUTHOR, author);
    }

    /**
     * Returns the project type.
     * 
     * @return project type enum value
     */
    public ProjectType getProjectType()
    {
        return m_ProjectType;
    }

    /**
     * Sets the project type.
     * 
     * @param projectType
     *            project type value
     */
    public void setProjectType(final ProjectType projectType)
    {
        m_ProjectType = projectType;
    }

    /**
     * Sets the package prefix.
     * 
     * @param basePackage
     *            package of the core for the given plug-in
     */
    public void setBasePackage(final String basePackage)
    {
        m_Properties.put(PROP_BASE_PACKAGE, basePackage);
    }

    /**
     * Sets the extra code to add to {@link mil.dod.th.core.ccomm.physical.PhysicalLink} proxy class.
     * 
     * @param code
     *            code to add
     */
    public void setPhysicalLinkExtraCode(final String code)
    {
        m_Properties.put(PROP_PL_EXTRA_CODE, code);
    }

    /**
     * Returns a reference to the sdk directory.
     * 
     * @return {@link File} reference to the sdk directory.
     */
    private static String locateSdkDirectory()
    {
        final String classpath = System.getProperty("java.class.path");
        final int index = classpath.toLowerCase().indexOf(JAR_NAME);
        final int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
        File sdkDir;//NOCHECKSTYLE will get assigned in if/else statement
        if (index >= start)
        {
            final String libDirPath = classpath.substring(start, index);
            sdkDir = new File(libDirPath).getParentFile();
        }
        else
        {
            // Can't figure it out so use the current directory as default.
            sdkDir = new File(System.getProperty("user.dir"));
        }

        // ensures paths are correctly interpreted by ant
        return sdkDir.getAbsolutePath().replace('\\', '/');
    }

    /**
     * Returns the path to the core API directory.
     * 
     * @return path to the core directory
     */
    public File getApiDirectory()
    {
        return new File(getSdkDirectory(), SDK_SUB_DIRECTORY);
    }

    /**
     * Returns core API version string.
     * 
     * @return string containing the version in major.minor format
     * @throws Exception
     *             if unable to retrieve the API
     */
    public String getApiVersion() throws Exception
    {
        try (Jar jar = m_BndService.newJar(new File(getApiDirectory(), "mil.dod.th.core.api.jar")))
        {
            return jar.getVersion();
        }
    }
}
