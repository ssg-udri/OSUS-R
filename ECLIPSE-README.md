<!--
==============================================================================
 This software is part of the Open Standard for Unattended Sensors (OSUS)
 reference implementation (OSUS-R).

 To the extent possible under law, the author(s) have dedicated all copyright
 and related and neighboring rights to this software to the public domain
 worldwide. This software is distributed without any warranty.

 You should have received a copy of the CC0 Public Domain Dedication along
 with this software. If not, see
 <http://creativecommons.org/publicdomain/zero/1.0/>.
==============================================================================
-->

# ECLIPSE
Eclipse is the suggested development environment for OSUS-R.  The following sections describe how to set up Eclipse.
The latest version of Eclipse available should be used. The setup instructions below are specific to Windows and
therefore setup may vary on Linux and OSX. These steps are not required for building the software, only for development.

# ECLIPSE PLUG-IN SETUP
There are several plug-ins available to be used with OSUS-R. The required and optional plug-ins and how to install them are
listed below.

## Required Plug-ins:
  * Java Developer Tools

## Optional Plug-is:
  * BndTools
  * Checkstyle
  * PMD
  * Java Decompiler
  * Java EE Developer Tools (Only used if working with the GUI)

## Installing Plug-ins
1. With Eclipse running select File -> Import...
2. Navigate to Install -> Install Software Items from File...
3. Select Browse -> Navigate to the `Eclipse_Plug-ins.p2f` file located under the `deps/eclipse-plug-ins`
   folder and click open...
4. Make sure all required plug-ins from the list above are selected. The rest are optional...
5. Make sure the Install latest version of selected software box is NOT selected...
6. Select Next
7. Select Next
8. Accept the license agreement and click Finish

## Java Decompiler (Optional)
The Java Decompiler is not installed with the other plug-ins and must be installed separately if you wish to use it.
This is due to the fact that there is no online repository available for this plug-in so therefore it must be installed
manually from the file stored in the tools directory.
1. With Eclipse running select Help -> Install New Software...
2. Select Add...
3. For the name enter JD Eclipse
4. Select Archive and browse to your `deps/eclipse-plug-ins` folder. In that folder select the
   `jdeclipse_update_site.zip` and select Open.
5. Select OK
6. Select the Java Decompiler Eclipse Plug-in and hit Next.
7. Select Next
8. Accept the license agreement and click Finish

# ECLIPSE INSTALLED JREs
Since OSUS-R is targeted for an execution environment of JavaSE-1.8, you must set up the location of Installed JREs.

1. Select Window -> Preferences...
2. Navigate to Java -> Installed JREs
3. If JDK 8 is already listed, skip to *Execution Environments*
4. Select Add...
5. Select *Standard VM* and then Next
6. Enter the location of the JDK 8 installation for the JRE home (This should file out other details once selected)
7. Select Finish
8. Select the checkbox for JDK 8 so it is used by default
9. Select OK

# Execution Environments
1. Select Window -> Preferences...
2. Navigate to Java -> Installed JREs -> Execution Environments
3. Select the *JavaSE-1.8* environment and then select the newly installed JRE on the right (JDK 8)
4. Select OK

# ECLIPSE USER LIBRARY SETUP
A *JDK Tools* User Library is required by the doclet project such as WordHTMDoclet project (found under tools).  If
working with these projects, add a User Library as follows:

1. Open Preferences dialog
2. Navigate to Java -> Build Path -> User Libraries
3. Select New...
4. Enter *JDK Tools* for the Name
5. Select OK
6. Select created user library and select *Add External JARs...*
7. For Windows and Linux, locate and open the tools.jar file found in the lib folder of the installed JDK.
   For OS X, locate and open the classes.jar file found in the Contents/Classes folder of the installed JDK.
8. Select OK

# ECLIPSE WORKSPACE SETUP
Many of the workspace settings are included in the workspace and must be configured in Eclipse to use them.

## Eclipse Preferences (includes formatter, code templates and compiler settings)
1. Select File -> Import...
2. Under the General folder, select Preferences, select Next.
3. Select Browse...
4. Select the location of the file as `eclipse-preferences.epf` which is located in the root of the main project
   workspace.
5. Select Finish.

## Javascript Formatter
1. Select Window -> Preferences...
2. Navigate to Javascript -> Code Style -> Formatter
3. Select Import...
4. Select the location of the file as `eclipse-javascript-profile.xml` which is located in the root of the main project
   workspace.
5. Select OK

**NOTE:** If preferences are updated, the file must be exported.  When exporting only select the features needed, don't
export all as this will include system specific settings.

## Checkstyle Rules (automatically updated)
1. Select Window -> Preferences...
2. Navigate to Checkstyle
3. Select New...
4. Change the type to External Configuration File, enter a name *THOSE Checkstyle* for the configuration
   and select the location of the file as `cnf/checkstyle/checkstyle-rules.xml`.
5. Select OK on both dialogs.
6. Select New...
7. Change the type to External Configuration File, enter a name *THOSE All Base Checkstyle* for the configuration
   and select the location of the file as `cnf/checkstyle/checkstyle-rules-all-files.xml`.
8. Select New...
9. Change the type to External Configuration File, enter a name *THOSE Test Checkstyle* for the configuration
   and select the location of the file as `cnf/checkstyle/checkstyle-test-rules.xml`.
10. Select OK on both dialogs.

## PMD Rules (must import any time the file changes)
1. Select Window -> Preferences...
2. Navigate to PMD -> Rules Configuration
3. Select Clear all to remove all existing rules
4. Select Import rule set... and choose the `cnf/pmd/pmd-java-rules.xml`
5. Ensure *Import by Reference* is selected and select OK
6. Select OK

# ECLIPSE PROJECT SETUP
Once all steps above have been completed, the Eclipse projects can be imported. To import all projects, follow the
steps below:

1. Build resources needed by Eclipse by executing `ant build-for-eclipse` from the command line where `workspace.dir` is
   the current directory
2. Select File -> Import...
3. Select the Existing Projects into Workspace option from the General folder.
4. Select the *Search for nested projects* option so all projects are found and make sure *Copy projects into workspace*
   is deselected.
5. Click Browse and select the root directory of OSUS-R in your workspace.
6. Select all found projects or a subset desired and select Finish.

# ECLIPSE ANT SETUP
## Ant Home
1. Select Window -> Preferences...
2. Navigate to Ant -> Runtime
3. Select Ant Home... and choose the `deps/apache-ant-1.8.4` folder
4. Select OK

## Ant View
1. Select Window -> Show View -> Ant
2. From within the Ant view, select the Add Buildfiles button and choose desired project `build.xml` files. The root
   build file can be found at `cnf\rootBuild.xml`.
3. Select OK

# SUGGESTED ECLIPSE SETTINGS
## TAB SPACE SETTINGS
1. Select Window -> Preferences...
2. General -> Editors -> Text Editors
3. Check the box *Insert spaces for tabs*, and make sure the *Displayed tab width* is 4.
4. Select Apply
5. In the Preferences window navigate to ANT -> Editor -> Formatter
6. Within the first section there is a check-box that is labeled *Use tab character instead of spaces*, un-check it.
7. Select Apply
8. In the Preferences window navigate to XML -> XML Files -> Editor
9. Mid-way through the window is a pair of radio buttons, select *Indent using spaces* and update the *Indentation size* to 4.
10. Select Apply

