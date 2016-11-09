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
/*
 * This file is part of the Weborganic XMLDoclet library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.udri.doclet.msword;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.tools.doclets.Taglet;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for the options for the XML Doclet.
 * 
 * @author Christophe Lauret
 * 
 * @version 30 March 2010
 */
public final class Options
{

    /**
     * An empty array constant for reuse.
     */
    private static final String[] EMPTY_ARRAY = new String[] {};

    /**
     * The default encoding for the output
     */
    private static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");

    /**
     * The default filename for the output.
     */
    private static final String DEFAULT_FILENAME = "those-doclet.htm";

    /**
     * Determines whether the output is a single file or multiple files.
     * 
     * Populated from the command line via the "-multiple" flag.
     */
    private boolean multipleFiles = false;

    /**
     * Determines the directory where output is placed.
     * 
     * Populated from the command line via the "-d [directory]" flag.
     */
    private File directory;

    /**
     * The output encoding of the XML files.
     */
    private Charset encoding = DEFAULT_CHARSET;

    /**
     * Filter classes extending the specified class.
     */
    private String extendsFilter = null;

    /**
     * Filter classes implementing the specified class.
     */
    private String implementsFilter = null;

    /**
     * The taglets loaded by this doclet.
     */
    private Map<String, Taglet> taglets = new HashMap<String, Taglet>();

    /**
     * Name of the file - used for single output only.
     * 
     * Populated from the command line via the "-filename [file]" flag.
     */
    private String filename = DEFAULT_FILENAME;


    /**
     *  Name of the input *.htm file used
     */
    private String inputFile = null;

    /**
     * Path for report for broken links
     */
    private String brokenLinksReportFile = null;

    /**
     * Path for XSD directory 
     */
    private String xsdDir = null;

    /**
     * Creates new options.
     */
    public Options()
    {
        // Load the standard taglets
        for (Tags t : Tags.values())
        {
            this.taglets.put(t.getName(), t);
        }
    }

    /**
     * Indicates whether these options should use multiple files.
     */
    public boolean useMultipleFiles()
    {
        return this.multipleFiles;
    }

    /**
     * Returns the charset to use to encode the output.
     * 
     * @return the charset to use to encode the output.
     */
    public Charset getEncoding()
    {
        return this.encoding;
    }

    /**
     * Returns the directory where to store the files.
     * 
     * @return where to store the files.
     */
    public File getDirectory()
    {
        return this.directory;
    }

    /**
     * Returns the name of the file for single output.
     * 
     * @return the name of the file for single output.
     */
    public String getFilename()
    {
        return this.filename;
    }

    /**
     * Returns the name of the template htm file used
     */
    public String getInputFile()
    {
        return this.inputFile;
    }

    /**
     * Return the name of the broken links report file path
     */
    public String getBrokenLinksReportFile()
    {
        return this.brokenLinksReportFile;
    }

    /**
     * Return the parent XSD directory
     */
    public String getXsdDirectory()
    {
        return this.xsdDir;
    }

    /**
     * Returns the taglet instance for the specified tag name.
     * 
     * @param name
     *            The name of the tag.
     * @return The corresponding <code>Taglet</code> or <code>null</code>.
     */
    public Taglet getTagletForName(String name)
    {
        return this.taglets.get(name);
        //        for (String n : this.taglets.keySet())
        //        {
        //            if (n.equals(name))
        //                return this.taglets.get(n);
        //        }
        //        return null;
    }

    /**
     * Filters the included set of classes by checking whether the given class matches the '-extends' and '-implements'
     * options.
     * 
     * @param doc
     *            the class documentation.
     * @return <code>true</code> if the class should be included; <code>false</code> otherwise.
     */
    public boolean filter(ClassDoc doc)
    {
        // Extends
        if (this.extendsFilter != null)
        {
            ClassDoc superclass = doc.superclass();
            return superclass != null && this.extendsFilter.equals(superclass.toString());
        }
        // Implements
        if (this.implementsFilter != null)
        {
            ClassDoc[] interfaces = doc.interfaces();
            for (ClassDoc i : interfaces)
            {
                if (this.implementsFilter.equals(i.toString()))
                    return true;
            }
            return false;
        }
        // No filtering
        return true;
    }

    @Override
    public String toString()
    {
        return super.toString();
    }

    // static methods for use by Doclet =============================================================

    /**
     * A JavaDoc option parsing handler.
     * 
     * <p>
     * This one returns the number of arguments required for the given option.
     * 
     * @see com.sun.javadoc.Doclet#optionLength(String)
     * 
     * @param option
     *            The name of the option.
     * 
     * @return The number of arguments for that option.
     */
    public static int getLength(String option)
    {
        // possibly specified by javadoc understood by this doclet
        if ("-d".equals(option))
            return 2;
        if ("-docencoding".equals(option))
            return 2;

        // specific to this doclet
        if ("-multiple".equals(option))
            return 1;
        if ("-outputFile".equals(option))
            return 2;
        if ("-inputFile".equals(option))
            return 2;
        if ("-xsdDir".equals(option))
            return 2;
        if ("-brokenLinksReport".equals(option))
            return 2;
        if ("-implements".equals(option))
            return 2;
        if ("-extends".equals(option))
            return 2;
        if ("-tag".equals(option))
            return 2;
        if ("-taglet".equals(option))
            return 2;
        return 0;
    }

    /**
     * Retrieve the expected options from the given array of options.
     * 
     * 
     * @param options
     *            The two dimensional array of options.
     * @param reporter
     *            The error reporter.
     */
    public static Options toOptions(String options[][], DocErrorReporter reporter)
    {
        Options o = new Options();

        // Flags
        o.multipleFiles = has(options, "-multiple");

        // Output directory
        if (has(options, "-d"))
        {
            String directory = get(options, "-d");
            if (directory == null)
            {
                reporter.printError("Missing value for <directory>, usage:");
                reporter.printError("-d <directory> Destination directory for output files");
                return null;
            }
            else
            {
                o.directory = new File(directory);
                reporter.printNotice("Output directory: " + directory);
            }
        }
        else
        {
            reporter.printError("Output directory not specified; use -d <directory>");
            return null;
        }

        // Output encoding
        if (has(options, "-docencoding"))
        {
            String encoding = get(options, "-docencoding");
            if (encoding == null)
            {
                reporter.printError("Missing value for <name>, usage:");
                reporter.printError("-docencoding <name> \t Output encoding name");
                return null;
            }
            else
            {
                o.encoding = Charset.forName(encoding);
                reporter.printNotice("Output encoding: " + o.encoding);
            }
        }

        // Extends
        if (has(options, "-outputFile"))
        {
            String name = get(options, "-outputFile");
            if (name != null && !o.multipleFiles)
            {
                o.filename = name;
                reporter.printNotice("Using output file name: " + name);
            }
            else
                reporter.printWarning("'-outputFile' option ignored");
        }
        if (has(options, "-inputFile"))
        {
            String name = get(options, "-inputFile");
            if (name != null && !o.multipleFiles)
            {
                o.inputFile = name;
                reporter.printNotice("Using input file name: " + name);
            }
            else
                reporter.printWarning("'-inputFile' option ignored");
        }
        if (has(options, "-brokenLinksReport"))
        {
            String name = get(options, "-brokenLinksReport");
            if (name != null && !o.multipleFiles)
            {
                o.brokenLinksReportFile = name;
                reporter.printNotice("Using broking links report file name: " + name);
            }
            else
                reporter.printWarning("'-brokenLinksReport' option ignored");
        }
        if (has(options, "-xsdDir"))
        {
            String name = get(options, "-xsdDir");
            if (name != null && !o.multipleFiles)
            {
                o.xsdDir = name;
                reporter.printNotice("Using xsd directory: " + name);
            }
            else
                reporter.printWarning("'-xsdDir' option ignored");
        }

        // Extends
        if (has(options, "-extends"))
        {
            String superclass = get(options, "-extends");
            if (superclass != null)
            {
                o.extendsFilter = superclass;
                reporter.printNotice("Filtering classes extending: " + superclass);
            }
            else
                reporter.printWarning("'-extends' option ignored - superclass not specified");
        }

        // Implements
        if (has(options, "-implements"))
        {
            String iface = get(options, "-implements");
            if (iface != null)
            {
                o.implementsFilter = iface;
                reporter.printNotice("Filtering classes implementing: " + iface);
            }
            else
                reporter.printWarning("'-implements' option ignored - interface not specified");
        }

        // Taglets
        if (has(options, "-taglet"))
        {
            String classes = get(options, "-taglet");
            if (classes != null)
            {
                for (String c : classes.split(":"))
                {
                    try
                    {
                        Class<?> x = Class.forName(c);
                        Class<? extends Taglet> t = x.asSubclass(Taglet.class);
                        Method m = t.getMethod("register", Map.class);
                        m.invoke(null, o.taglets);
                        reporter.printNotice("Using Taglet " + t.getName());
                    }
                    catch (Exception ex)
                    {
                        reporter.printError("'-taglet' option reported error - :" + ex.getMessage());
                    }
                }
            }
            else
                reporter.printWarning("'-taglet' option ignored - classes not specified");
        }

        // If we reached this point everything is OK
        return o;
    }

    /**
     * Indicates whether the specified option is defined.
     * 
     * @param options
     *            the matrix of command line options.
     * @param name
     *            the name of the requested option.
     * 
     * @return <code>true</code> if defined; <code>false</code> otherwise.
     */
    private static boolean has(String[][] options, String name)
    {
        for (String[] option : options)
        {
            if (option[0].equals(name))
                return true;
        }
        return false;
    }

    /**
     * Returns the single value for the specified option if defined.
     * 
     * @param options
     *            the matrix of command line options.
     * @param name
     *            the name of the requested option.
     * 
     * @return the value if available or <code>null</code>.
     */
    private static String get(String[][] options, String name)
    {
        String[] option = find(options, name);
        return (option.length > 1) ? option[1] : null;
    }

    /**
     * Finds the options array for the specified option name.
     * 
     * <p>
     * The first element is <i>always</i> the name of the option.
     * 
     * @param options
     *            the matrix of command line options.
     * @param name
     *            the name of the requested option.
     * 
     * @return the corresponding array or an empty array.
     */
    private static String[] find(String[][] options, String name)
    {
        for (String[] option : options)
        {
            if (option[0].equals(name))
                return option;
        }
        // Option not available
        return EMPTY_ARRAY;
    }

}
