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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.inject.Guice;
import com.google.inject.Injector;

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.utils.UtilInjectionModule;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.internal.Strings;

/**
 * Implements the those application main and command line input handling.
 * 
 * @author dlandoll
 */
// simple types should not really be considered in data coupling like List and File type
@SuppressWarnings("classdataabstractioncoupling")
public final class ThoseMain
{
    /**
     * Create asset command line option.
     */
    private static final String OPTION_ASSET = "a";

    /**
     * Create physical link command line option.
     */
    private static final String OPTION_PHYLINK = "p";

    /**
     * Create link layer command line option.
     */
    private static final String OPTION_LINKLAYER = "l";

    /**
     * Create transport layer command line option.
     */
    private static final String OPTION_TRANSPORT = "t";

    /**
     * Create transport layer command line option.
     */
    private static final String OPTION_DEFAULT_PROPS = "d";
    
    /**
     * Pass the package name as an argument.
     */
    private static final String OPTION_PACKAGE_NAME = "k";
    
    /**
     * Pass the attribute base type as an argument.
     */
    private static final String OPTION_PHYS_LINK_TYPE = "b";
    
    /*
     * Stack trace print out option.
     */
    private static final String OPTION_VERBOSE = "v";

    /**
     * Console output stream.
     */
    private static final PrintStream OUT_STREAM = System.out;

    /**
     * Private constructor to prevent instantiation.
     */
    private ThoseMain()
    {
        // Do nothing
    }

    /**
     * Application main function.
     * 
     * @param args
     *            Command line arguments
     * @throws Exception
     *            if an exception occurs processing the command
     */
    public static void main(final String[] args) throws Exception
    {
        // Create options parser
        final OptionParser parser = new OptionParser();
        parser.accepts(OPTION_VERBOSE);
        parser.accepts(OPTION_ASSET).withRequiredArg().ofType(String.class);
        parser.accepts(OPTION_PHYLINK).withRequiredArg().ofType(String.class);
        parser.accepts(OPTION_LINKLAYER).withRequiredArg().ofType(String.class);
        parser.accepts(OPTION_TRANSPORT).withRequiredArg().ofType(String.class);
        parser.accepts(OPTION_DEFAULT_PROPS);
        parser.accepts(OPTION_PACKAGE_NAME).withRequiredArg();
        parser.accepts(OPTION_PHYS_LINK_TYPE).withRequiredArg();

        // Parse the command string (minus the sub command name)
        final OptionSet options;
        if (args.length == 0)
        {
            options = parser.parse();
        }
        else
        {
            try
            {
                options = parser.parse(args);
            }
            catch (final Exception ex)
            {
                for (int i = 0; i < args.length; i++)
                {
                    if (args[i].equals("-" + OPTION_VERBOSE))
                    {
                        throw ex;
                    }
                }
                
                OUT_STREAM.println("\nError parsing the given arguments:");
                OUT_STREAM.println(ex.getMessage());
                return;
            }
        }

        // Empty line before command output
        OUT_STREAM.println();

        // Get the list of non-option arguments (sub-command, etc.)
        final List<String> nonOptions = options.nonOptionArguments();
        if (nonOptions.isEmpty())
        {
            // Print usage info and return if no options/arguments are given
            OUT_STREAM.println(getUsage());
            return;
        }

        // Use first non-option argument as the sub-command
        final String subcmd = nonOptions.get(0);
        try
        {
            processSubcommand(subcmd, options);
        }
        catch (final Exception ex)
        {
            checkVerbose(ex, options);
            OUT_STREAM.println("\nError processing command: " + subcmd);
            OUT_STREAM.println(ex.getMessage());
        }
    }
    
    /**
     * Gets project property values from the user via stdin.
     * 
     * @param props
     *            project properties
     * @param packageNameSet 
     *            whether the package name has already been set from the command line
     * @param baseClassSet
     *            whether the base class has been already been set from the command line
     * @throws IOException
     *             if an I/O error occurs
     */
    private static void getProjectProperties(final ProjectProperties props, final boolean packageNameSet, 
            final boolean baseClassSet) 
            throws IOException
    {
        final BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String outString = String.format("Enter Project Name (%s): ", props.getProjectName());
        OUT_STREAM.print(outString);
        String inputString = input.readLine();
        if (inputString != null && !inputString.isEmpty())
        {
            props.setProjectName(inputString);
        }

        /*
         * TODO: TH-389 use when sdk is functional for previous api versions
        outString = String.format("Enter API Version (%s): ", props.getApiVersion());
        OUT_STREAM.print(outString);
        inputString = input.readLine();
        if (!Strings.isNullOrEmpty(inputString))
        {
           props.setApiVersion(inputString); 
        }
        */

        outString = String.format("Enter Description (%s): ", props.getDescription());
        OUT_STREAM.print(outString);
        inputString = input.readLine();
        if (!Strings.isNullOrEmpty(inputString))
        {
            props.setDescription(inputString);
        }

        if (!packageNameSet)
        {
            outString = String.format("Enter Package Name (%s): ", props.getPackageName());
            OUT_STREAM.print(outString);
            inputString = input.readLine();
            if (!Strings.isNullOrEmpty(inputString))
            {
                props.setPackageName(inputString);
            }
        }
        
        //only ask for type if the type of project is a physical link.
        if (props.getProjectType().equals(ProjectType.PROJECT_PHYLINK) && !baseClassSet)
        {
            promptPhysicalLinkTypeEnum(input, props);
        }
        
        outString = String.format("Enter Vendor (%s): ", props.getVendor());
        OUT_STREAM.print(outString);
        inputString = input.readLine();
        if (!Strings.isNullOrEmpty(inputString))
        {
            props.setVendor(inputString);
        }
    }
    
    /**
     * Method used to prompt the user to enter in the physical link type for the project. Method only applies
     * if plug-in is a physical link project.
     * @param input
     *  the {@link BufferedReader} that is used to read input from the command line
     * @param props
     *  the project properties for this project
     * @throws IOException 
     *  if there is an inability to read from the input stream associated with the console
     */
    private static void promptPhysicalLinkTypeEnum(final BufferedReader input, final ProjectProperties props) 
            throws IOException
    {
        boolean typeEntered = false;
        do
        {
            final String outString = String.format("Enter a Physical Link type (%s): ", formatPlTypesValueString());
            OUT_STREAM.print(outString);
            final String inputString = input.readLine();
        
            if (!Strings.isNullOrEmpty(inputString))
            {
                try
                {
                    //see if input is valid. if not will throw exception and ask for input again.
                    final PhysicalLinkTypeEnum plType = PhysicalLinkTypeEnum.fromValue(inputString);
                    props.setPhysicalLinkTypeEnum(plType);
                    typeEntered = true;
                }
                catch (final IllegalArgumentException ex)
                { //NOCHECKSTYLE:Need empty catch to be able to loop through and prompt for input
                    //catch the exception as an invalid link type has been given so that loop can continue
                }
            }
            
            if (!typeEntered)
            {
                OUT_STREAM.println(String.format("Invalid Physical Link type %s!", inputString));
            }
        
        } while (!typeEntered);
    }

    /**
     * Returns the application command line usage help.
     * 
     * @return 
     *  command line usage info as a string
     */
    private static String getUsage()
    {
        final StringBuilder usage = new StringBuilder(400);

                        // Empty line before usage info
        usage.append("\n " 
                     // Describe commands/options
                     + "those <option> <command> <cmd-options> \n"
                     + "  Options:\n"
                     + "\t-v : Allows stack trace to be printed.\n"     
                     + "  Commands:\n"
                     + getCreateUsage()
                     + "     help\n" 
                     + "\t  Displays this help message\n" 
                     + "     version\n" 
                     + "\t  Displays the SDK version and supported Core APIs\n");

        return usage.toString();
    }
    
    /**
     * Returns the application command line for command line usage.
     * 
     * @return 
     *  command line create usage specification
     */
    private static String getCreateUsage()
    {
        final StringBuilder createUsage = new StringBuilder(200);
        
        createUsage.append("     create\n"
                            + "\t  Syntax:\n"
                            + "\t\tcreate <Template.Type ClassName> [-d] [-b phys.link.type] "
                            + "[-k package.name] [directory]\n"
                            + "\n\t  Template Type\n"
                            + "\t\t-a : Asset template\n"
                            + "\t\t-p : PhysicalLink template\n"
                            + "\t\t-l : LinkLayer template\n" 
                            + "\t\t-t : TransportLayer template\n\n"
                            + "\t  Cmd-Options\n"
                            + "\t\t-d : Use default properties (package, vendor, API, etc.)\n"
                            + "\t\t-b : If creating a physical link plug-in,"
                            + " specify the type of physicallink being created. "
                            + "Available values are: " + formatPlTypesValueString()
                            + "\n\t\t-k : Set the package name (also used for bundle symbolic name and JAR filename)\n"
                            + "\n\t    directory : Directory to generate plug-in files in. "
                            + "If none is specified the current working "
                            + "directory will be used.\n\n");
        
        return createUsage.toString();
    }
    
    /**
     * Helper function to return a formatted string of all possible.
     * @return
     *  a formatted string of all the {@link PhysicalLinkTypeEnum}s
     */
    private static String formatPlTypesValueString()
    {
        final Joiner joiner = Joiner.on(", ");
        
        final List<String> typeStrs = new ArrayList<>();
        
        for (PhysicalLinkTypeEnum type : PhysicalLinkTypeEnum.values())
        {
            typeStrs.add(type.value());
        }
        
        return joiner.join(typeStrs);
    }

    /**
     * Process and handle the given sub-command.
     * 
     * @param subcmd
     *            sub command given by the user
     * @param options
     *            options parsed from the command line
     * @throws Exception 
     *            IllegalArgumentExceptions if there is a bad argument. 
     *            Also throws an exception if API version isn't retrieved.
     */
    private static void processSubcommand(final String subcmd, final OptionSet options)
            throws Exception
    {
        final List<String> nonOptions = options.nonOptionArguments();

        final ProjectProperties props = new ProjectProperties(new BndService());

        // Check for a correct sub-command
        if ("create".equals(subcmd))
        {
            //figure out project type and class name
            setProjectTypeAndClassName(options, props);
            
            //figure out physical link type 
            setPhysicalLinkTypeEnum(options, props);
            
            // optionally, the package name can be set from the command line
            if (options.hasArgument(OPTION_PACKAGE_NAME))
            {
                props.setPackageName((String)options.valueOf(OPTION_PACKAGE_NAME));
            }

            // Retrieve the path where the SDK is located.
            final String sdkDir = new File(ThoseMain.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath()).getParentFile().getParent();
            final String sdkDirErr = "\nInvalid project directory. Please select a directory besides "
                    + "the one the SDK is located in.";
            
            // Check if the user specified a project directory, otherwise use the current directory
            final Path projectDir;
            if (nonOptions.size() > 1)
            {
                projectDir = Paths.get(nonOptions.get(1));
                // Verify the user specified directory is not the same directory the SDK is located in.
                if (sdkDir.equals(projectDir.toFile().getCanonicalPath()))
                {
                    final Exception error = new IllegalArgumentException(sdkDirErr);
                    checkVerbose(error, options);
                    OUT_STREAM.println(sdkDirErr);
                    System.exit(1);
                }
            }
            else
            {
                final Path cwd = Paths.get(System.getProperty("user.dir"));
                // Verify the current directory is not the same directory the SDK is located in.
                if (sdkDir.equals(cwd.toFile().getCanonicalPath()))
                {
                    final Exception error = new IllegalArgumentException(sdkDirErr);
                    checkVerbose(error, options);
                    OUT_STREAM.println(sdkDirErr);
                    System.exit(1);
                }
                projectDir = cwd;
            }

            // Ask the user for input on project properties if the default option was not used
            if (!options.has(OPTION_DEFAULT_PROPS))
            {
                getProjectProperties(props, options.hasArgument(OPTION_PACKAGE_NAME), 
                        options.hasArgument(OPTION_PHYS_LINK_TYPE));
            }

            // Create the new project
            createProject(props, projectDir);
        }
        else if ("help".equals(subcmd))
        {
            OUT_STREAM.println(getUsage());
        }
        else if ("version".equals(subcmd))
        {
            try
            {
                OUT_STREAM.format("API Version: %s%n", props.getApiVersion());
            }
            catch (final Exception error)
            {
                checkVerbose(error, options);
                OUT_STREAM.println("\nError retrieving the API verision");
            }
        }  
        else
        {
            final String invalidSub = "\nInvalid sub-command\n" + getUsage();
            final Exception error = new IllegalArgumentException(invalidSub);
            checkVerbose(error, options);
            OUT_STREAM.print(invalidSub);
        }
    }
    
    /**
     * Method used to determine and set the desired project type and name of class of the plug-in to be made. 
     * @param options
     *  the entered set of options from the command line
     * @param props
     *  the properties of the project to be created
     * @throws Exception
     *  throws an exception if the create command is missing an option 
     */
    private static void setProjectTypeAndClassName(final OptionSet options, final ProjectProperties props)
             throws Exception
    {
        if (options.hasArgument(OPTION_ASSET))
        {
            props.setProjectType(ProjectType.PROJECT_ASSET);
            props.setClassName((String)options.valueOf(OPTION_ASSET));
            props.setBasePackage("mil.dod.th.core.asset");
        }
        else if (options.hasArgument(OPTION_PHYLINK))
        {
            props.setProjectType(ProjectType.PROJECT_PHYLINK);
            props.setClassName((String)options.valueOf(OPTION_PHYLINK));
            props.setBasePackage("mil.dod.th.core.ccomm.physical");
        }
        else if (options.hasArgument(OPTION_LINKLAYER))
        {
            props.setProjectType(ProjectType.PROJECT_LINKLAYER);
            props.setClassName((String)options.valueOf(OPTION_LINKLAYER));
            props.setBasePackage("mil.dod.th.core.ccomm.link");
        }
        else if (options.hasArgument(OPTION_TRANSPORT))
        {
            props.setProjectType(ProjectType.PROJECT_TRANSPORTLAYER);
            props.setClassName((String)options.valueOf(OPTION_TRANSPORT));
            props.setBasePackage("mil.dod.th.core.ccomm.transport");
        }
        else
        {
            final String argumentErrMsg = "\nThe <create> sub-command requires an option of \n";
            final Exception error = new IllegalArgumentException(argumentErrMsg + "\n" + getCreateUsage());
            checkVerbose(error, options);
            OUT_STREAM.println(argumentErrMsg);
            OUT_STREAM.println(getCreateUsage());
            System.exit(1);
        }
    }
    
    /**
     * Method used to determine the proxy and attribute type of the plugin-in to be created.
     * 
     * @param options
     *  the entered set of options from the command line
     * @param props
     *  the properties of the project to be created
     */
    private static void setPhysicalLinkTypeEnum(final OptionSet options, final ProjectProperties props)
    {
        if (props.getProjectType() == ProjectType.PROJECT_PHYLINK)
        {
            if (options.hasArgument(OPTION_PHYS_LINK_TYPE))
            {
                final String type = (String)options.valueOf(OPTION_PHYS_LINK_TYPE);
                
                final PhysicalLinkTypeEnum plType = PhysicalLinkTypeEnum.fromValue(type);
                props.setPhysicalLinkTypeEnum(plType);
            }
        }
    }
  
    /**
     * Creates a new project based on the given properties.
     * 
     * @param props
     *            Project properties
     * @param projectDir
     *            Directory to create project in
     * @throws IOException
     *             if an I/O error occurs
     */
    private static void createProject(final ProjectProperties props, final Path projectDir) throws IOException
    {
        // Create and set the Freemarker configuration
        final Configuration freeMarkerConfig = new Configuration();
        final File templateDir = new File(props.getApiDirectory(), "templates");
        freeMarkerConfig.setDirectoryForTemplateLoading(templateDir);
        freeMarkerConfig.setObjectWrapper(new DefaultObjectWrapper());

        // Generate the project
        final Injector injector = Guice.createInjector(new UtilInjectionModule(), new LocalInjectionModule());
        final ProjectGenerator projectGen = injector.getInstance(ProjectGenerator.class);
        projectGen.initialize(freeMarkerConfig, OUT_STREAM);
        try
        {
            projectGen.create(props, projectDir.toFile());
        }
        catch (final Exception ex)
        {
            OUT_STREAM.println("Error generating the project: " + ex.getMessage());
        }
    }
    
    /**
     * Check to see if the verbose option exists and if so the exception needs to be thrown.
     * 
     * @param err
     *            err that will be thrown 
     * @param options
     *            Entire set of options from the command line
     * @throws Exception
     *            throws exception if the verbose option exists
     */
    private static void checkVerbose(final Exception err, final OptionSet options) throws Exception
    {
        if (options.has(OPTION_VERBOSE))
        {
            throw err;
        }
        else
        {
            return;
        }
    }
}
