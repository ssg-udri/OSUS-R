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

[README.md](README.md) contains basic instructions on building the software. This contains additional information about
building the code and notes for development.

# <a name="DirectoryStructure" />Directory Structure
The following convention is used for structuring directories (where a `*` denotes any folder matching the pattern):

* `cnf` - Configuration directory for Bnd, contains settings for OSGi bundle repositories and other build configurations
  (see [Configuration Project](#ConfigurationProject))
* `deps` - Shared 3rd party open source and off-the-shelf non-bundle dependencies used by the project. Libraries used by
  a single project will be in the lib folder of the project and not here
* `example.*` - Contains example code, some of which can be used by integration tests
* `mil.dod.th.*` - Contains any Terra Harvest specific code (more restrictive patterns are explained below)
    * `mil.dod.th.asset.*` - Contains all basic Terra Harvest assets, specialized assets should go in a separate
      workspace
    * `mil.dod.th.core.*` - Contains all Terra Harvest core API, the standard API that all implemenations must use
    * `mil.dod.th.ose.*` - Contains all THOSE code (anything that runs within a THOSE controller and also tests for the
      source)
        * `mil.dod.th.ose.emac.*` - Contains code specific to the EMAC SoM prototype
        * `mil.dod.th.ose.gui.*` - Contains code specific to the THOSE web GUI
        * `mil.dod.th.ose.linux.*` - Contains code specific to a Linux controller
    * `mil.dod.th.checkstyle.ext` - Contains a Checkstyle extension rules project *(not currently used)*
    * `mil.dod.th.confluence.doclet` - Contains a Java doclet project for generating documentation in Confluence based
      on Java code *(not currently used)*
    * `mil.dod.th.junit4.test.runner` - Contains a test runner for integration tests that produces reports viewable from
      Eclipse and Jenkins
    * `mil.dod.th.word.htm.doclet` - Contains a Java doclet project for generating documentation in a Microsoft Word
      document based on Java code *(not currently used)*
* `reports` - Contains any generated reports from Checkstyle, PMD, etc that are not specific to a project
* `target` *deprecated* - Contains build configurations for building packages of software suited for a particular
  purpose or platform (e.g., building the controller application for a Windows target). In the future separate
  application project should be created (e.g., mil.dod.th.ose.controller.application)

## Other Conventions

### <a name="IntegrationTests" />Integration Tests
Integration tests are found in the project ending in `.integration` and map to the project with the same prefix. For
example, `mil.dod.th.ose.remote.integration` contains the integration tests for `mil.dod.th.ose.remote`. A deprecated
project contains integration tests in `mil.dod.th.ose.integration` and apply to the core mostly. These tests should be
moved to specific integration test projects going forward.

### Platform Interface
Platform interface projects are found in `mil.dod.th.ose.*.platform` or `mil.dod.th.ose.*.serial` where the exact
location depends on the platform name. For example, `mil.dod.th.ose.linux.serial` contains the serial port project
for Linux.

## <a name="ConfigurationProject" />Configuration Project
The configuration project located at `cnf` is organized as follows (does not include details of cnf files not currently
used):

* `ant` - Common Ant build files and property files that can be used by all projects (see
  [Ant Common Build Files](#AntCommons))
* `buildrepo` - Bundle repository for build path only dependencies
* `checkstyle` - Checkstyle rule configurations used by the project
* `ext` - Contains extensions for Bnd such as defining which OSGi bundle repositories are available
* `localrepo` - Bundle repository for bundles that are used for build dependencies and also at runtime
* `plugins` - Contains libraries used by Bnd
* `pmd` - PMD rule configurations used by the project
* `runrepo` - Bundle repository for runtime bundles only
* `buildinfo.properties` - Contains properties for the current system version. This version can be viewed from the
  controller shell using the `version` command.
* `rootBuild.xml` - Root build file for Ant used to build the system as whole
* `tmp.build.properties` - Contains properties generated during a build containing metadata about the build process.
  Metadata can be viewed from the controller shell using the `buildInfo` command.

# Building System
As detailed in [README.md](README.md), Ant is used to build the software.

## <a name="AntCommons" />Ant Common Build Files
The common files in `cnf/ant` are used as follows:

* `commons.xml` *(deprecated)* - Contains common targets and definitions that could apply to any build file
* `default.properties` *(deprecated)* - Contains all of the basic properties shared among build files
* `emac-commons.xml` - Contains common targets and definitions that apply to any EMAC build file
* `gui-commons.xml` - Contains common targets and definitions that apply to any GUI build file
* `integration-commons.xml` *(deprecated)* -  Imports `java-commons.xml`, contains targets and definitions that apply to
  all integration test projects (see [Integration Tests](#IntegrationTests))
* `protobuf.xml` - Contains common targets to work with protocol buffers
* `shared.xml` *(deprecated)* - Contains targets to build all *shared* code, this should be removed and calls should be
  made directly to individual projects
* `target-commons.xml` *(deprecated)* -  Contains targets and definitions that apply to building a target
  (see [Directory Structure](#DirectoryStructure))
* `tools.xml` *(deprecated)* - Contains targets to build all *tools*, this should be removed and calls should be
  made directly to individual projects

## Ant Targets
There are many standard targets that apply to multiple projects. These targets are defined here, while specific targets
used in one or a few places are not detailed.

These targets apply to Java projects (by importing `cnf/build.xml`):

* `init` - Initializes properties, each target must depend on this target to run properly
* `clean` - Removes previously generated/compiled files.
* `build` - Builds the projects
* `cc.instrument` - Adds Cobertura code coverage information to the compiled classes so they show up in coverage metrics
* `compile` - Compiles source code for the project
* `compileTest` - Compiles test code for the project if `test` folder exists
* `deploy-component` - Copies the built component to the target being deployed
* `junit` - Runs unit tests for the project if `test` folder exists
* `test` - Runs Bnd level integration tests for the project (will fail if not applicable)

These targets apply to *deprecated* (should create bnd integration project in the future) integration projects (by
importing `cnf/ant/integration-commons.xml`):

* `build-supporting` - Build any supporting projects needed by the integration tests
* `deploy-target` - Deploy the target being tested to `build/deploy` of the integraton test project, this is where the
  target will run during integration testing
* `setup` - Copies files need for integration testing (e.g., additional test bundles, configuration files)
* `integration-test` - Runs the integrtion tests

These targets apply to *deprecated* (should create bnd application project in the future) target projects (by importing
`cnf/ant/target-commons.xml`):

* `build-deploy` - Builds then calls `deploy` for a one step build and deploy
* `build-with-deps` - Ant target must be overridden in project for the target project specific building
* `deploy-app` - Ant target must be overridden in project for target project specific deployment
* `deploy` - Wrapper target that calls `deploy-clean`, `deploy-app` and `zip`
* `deploy-clean` - Remove all deployed files from the `deploy` folder
* `zip` - Zip the deployed target

These targets apply to GUI related projects (by importing `cnf/ant/gui-commons.xml`):

* `archive-log-files` - Archive log files produced by the web GUI to the `reports` folder
* `check-web-app` - Check if the web GUI is running by checking the root URL of the application
* `start-glassfish` - Start the web GUI Glassfish server
* `stop-delete-glassfish` - Stop the web GUI Glassfish server and delete the domain for the web GUI
* `stop-glassfish` - Stop the web GUI Glassfish server

These targets apply to EMAC related projects (by importing `cnf/ant/emac-commons.xml`):

* `install-to-target` - Install the EMAC target software to an EMAC controller
* `reboot-target` - Reboot the EMAC target

In addition, these macros apply as well:

* `scp-from` - Secure copy files from target machine to local machine
* `scp-to` - Secure copy files to target machine from local machine
* `ssh` - SSH command to a target machine

In additon, each Ant build file can override a target to behave differently, but it should perform the same overall
function.

If using Eclipse for development, follow these in addition [instructions](ECLIPSE-README.md).

