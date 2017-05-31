#!/usr/bin/env groovy
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

// Only keep the 50 most recent builds
properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '50']]])

// Individual build flags
env.ENABLE_LINUX64_BUILD = "true"
env.ENABLE_LINUX32_BUILD = "false"
env.ENABLE_WINDOWS_BUILD = "false"
env.ENABLE_GUI_ASSET_BUILD = "false"
env.ENABLE_GUI_OTHER_BUILD = "false"
env.ENABLE_DOCS_BUILD = "false"

if (env.BRANCH_NAME == "master" || env.BRANCH_NAME == "development" || env.BRANCH_NAME.startsWith("release/")) {                                          
    env.ENABLE_LINUX64_BUILD = "true"
    env.ENABLE_LINUX32_BUILD = "true"
    env.ENABLE_WINDOWS_BUILD = "true"
    env.ENABLE_GUI_ASSET_BUILD = "true"
    env.ENABLE_GUI_OTHER_BUILD = "true"
    env.ENABLE_DOCS_BUILD = "true"
} else {
    try {
        timeout(time: 60, unit: 'SECONDS') {
            def userInput = input(
              id: 'userInput', message: 'Enable Builds', parameters: [
                [$class: 'BooleanParameterDefinition', defaultValue: true,  description: '', name: 'linux64'],
                [$class: 'BooleanParameterDefinition', defaultValue: false, description: '', name: 'linux32'],
                [$class: 'BooleanParameterDefinition', defaultValue: false, description: '', name: 'windows'],
                [$class: 'BooleanParameterDefinition', defaultValue: false, description: '', name: 'gui-asset'],
                [$class: 'BooleanParameterDefinition', defaultValue: false, description: '', name: 'gui-other'],
                [$class: 'BooleanParameterDefinition', defaultValue: false, description: '', name: 'docs']
            ])

            env.ENABLE_LINUX64_BUILD = userInput['linux64']
            env.ENABLE_LINUX32_BUILD = userInput['linux32']
            env.ENABLE_WINDOWS_BUILD = userInput['windows']
            env.ENABLE_GUI_ASSET_BUILD = userInput['gui-asset']
            env.ENABLE_GUI_OTHER_BUILD = userInput['gui-other']
            env.ENABLE_DOCS_BUILD = userInput['docs']
        }
    } catch(err) { // timeout reached or input false
        echo "No input received for build options"
    }
}

echo "Linux64 build enabled:   ${env.ENABLE_LINUX64_BUILD}"
echo "Linux32 build enabled:   ${env.ENABLE_LINUX32_BUILD}"
echo "Windows build enabled:   ${env.ENABLE_WINDOWS_BUILD}"
echo "GUI Asset build enabled: ${env.ENABLE_GUI_ASSET_BUILD}"
echo "GUI Other build enabled: ${env.ENABLE_GUI_OTHER_BUILD}"
echo "Docs build enabled:      ${env.ENABLE_DOCS_BUILD}"

// Get source code and stash it for use on each platform build
node('linux') {
    stage('Checkout') {
        // Configure the Pipeline Git configuration to clean workspace instead of doing manually here
        checkout scm
        stash name: 'sources', useDefaultExcludes: false
    }
}

// Define the different platform builds to execute in parallel
Map platforms = [:]

if (env.ENABLE_WINDOWS_BUILD == "true") {
    platforms['windows'] = {
        node('windows') {
            deleteDir()
            unstash 'sources'
            withEnv([
                "JAVA_HOME=${tool 'JDK 8'}",
                "JAVA_INCLUDE=${tool 'JDK 8'}/include",
                "PATH+ANT=${tool 'Ant 1.8.4'}/bin",
                "PATH+JAVA_BIN=${tool 'JDK 8'}/bin"
            ]) {
                bat 'set'
                bat 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-standard'
            }
            saveOsusResults('windows-results')
        }
    }
}

if (env.ENABLE_LINUX64_BUILD == "true") {
    platforms['linux64'] = {
        node('linux && 64bit') {
            deleteDir()
            unstash 'sources'
            withEnv([
                "JAVA_HOME=${tool 'JDK 8'}",
                "JAVA_INCLUDE=${tool 'JDK 8'}/include",
                "PATH+ANT=${tool 'Ant 1.8.4'}/bin",
                "PATH+JAVA_BIN=${tool 'JDK 8'}/bin"
            ]) {
                sh 'env'
                sh 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-standard'
            }
            saveOsusResults('linux64-results')
        }
    }
}

if (env.ENABLE_LINUX32_BUILD == "true") {
    platforms['linux32'] = {
        node('linux && 32bit') {
            deleteDir()
            unstash 'sources'
            withEnv([
                "JAVA_HOME=${tool 'JDK 8'}",
                "JAVA_INCLUDE=${tool 'JDK 8'}/include",
                "PATH+ANT=${tool 'Ant 1.8.4'}/bin",
                "PATH+JAVA_BIN=${tool 'JDK 8'}/bin"
            ]) {
                sh 'env'
                sh 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-standard'
            }
            saveOsusResults('linux32-results')
        }
    }
}

if (env.ENABLE_DOCS_BUILD == "true") {
    platforms['docs'] = {
        node {
            deleteDir()
            unstash 'sources'
            withEnv([
                "JAVA_HOME=${tool 'JDK 8'}",
                "JAVA_INCLUDE=${tool 'JDK 8'}/include",
                "PATH+ANT=${tool 'Ant 1.8.4'}/bin",
                "PATH+JAVA_BIN=${tool 'JDK 8'}/bin"
            ]) {
                if (NODE_LABELS.contains('linux')) {
                    sh 'env'
                    sh 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-docs'
                }
                else {
                    bat 'set'
                    bat 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-docs'
                }
            }
            saveOsusResults('docs-results')
            step(
                [$class: 'WarningsPublisher',
                 //consoleParsers: [[parserName: 'JavaDoc Tool'], [parserName: 'Javadoc Compiler Warning']],
                 consoleParsers: [[parserName: 'Javadoc Compiler Warning']],
                 unstableTotalAll: '0',
                 canComputeNew: true]
            )
            step(
                [$class: 'JavadocArchiver',
                 javadocDir: 'mil.dod.th.core/generated/javadoc',
                 keepAll: false]
            )
        }
    }
}

if (env.ENABLE_GUI_ASSET_BUILD == "true") {
    platforms['gui-asset'] = {
        node('linux && 64bit') {
            deleteDir()
            unstash 'sources'
            withEnv([
                "JAVA_HOME=${tool 'JDK 8'}",
                "JAVA_INCLUDE=${tool 'JDK 8'}/include",
                "PATH+ANT=${tool 'Ant 1.8.4'}/bin",
                "PATH+JAVA_BIN=${tool 'JDK 8'}/bin"
            ]) {
                sh 'env'
                wrap([$class: 'Xvfb', screen: '1280x800x24', additionalOptions: '-ac', debug: true]) {
                    sh 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-gui-integration-asset'
                }
            }
            saveOsusResults('gui-asset-results')
        }
    }
}

if (env.ENABLE_GUI_OTHER_BUILD == "true") {
    platforms['gui-other'] = {
        node('linux && 64bit') {
            deleteDir()
            unstash 'sources'
            withEnv([
                "JAVA_HOME=${tool 'JDK 8'}",
                "JAVA_INCLUDE=${tool 'JDK 8'}/include",
                "PATH+ANT=${tool 'Ant 1.8.4'}/bin",
                "PATH+JAVA_BIN=${tool 'JDK 8'}/bin"
            ]) {
                sh 'env'
                wrap([$class: 'Xvfb', screen: '1280x800x24', additionalOptions: '-ac', debug: true]) {
                    sh 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-gui-integration-other'
                }
            }
            saveOsusResults('gui-other-results')
        }
    }
}

node {
    step([$class: 'StashNotifier'])
}

try {
    stage('Build and Test') {
        // Execute builds
        parallel(platforms)
    }

    node {
        deleteDir()
        stage('Reporting') {
            extractOsusResults()
            doOsusReports()
        }

        stage('Archive') {
            archiveArtifacts '*/*/generated/*.jar, */*/generated/*.zip, */target/*/*/bin/*.zip, */reports/build.properties, */mil.dod.th.ose.gui.integration/generated/test-data/**, docs/mil.dod.th.core.*/generated/*'
        }
    }
} catch(Exception err) {
    currentBuild.result = 'FAILURE'
} finally {
    node {
        step([$class: 'StashNotifier'])
    }
}

def saveOsusResults(resultName) {
    if (resultName.startsWith("docs")) {
        stash name: resultName, includes: 'mil.dod.th.core.*/generated/*, reports/build.properties'
    } else {
        stash name: resultName, includes: 'reports/checkstyle-*results.xml, reports/pmd-*results.xml, */generated/*.jar, */generated/*.zip, target/*/*/bin/*.zip, reports/build.properties, mil.dod.th.ose.gui.integration/generated/test-data/**, reports/cobertura/coverage.xml'
    }

    // Publish test results based on which builds are enabled
    def windowsTestsPublished = false
    if (env.ENABLE_LINUX64_BUILD == "true") {
        if (resultName.startsWith("linux64")) {
            junit testResults: '*/generated/**/TEST-*.xml', keepLongStdio: true
            step(
                [$class: 'TasksPublisher',
                 pattern: '**/*',
                 excludePattern: 'Jenkinsfile, deps/**, */generated/**, **/*.epf, **/*.jar, **/*.zip, **/.settings/**, **/*.class, target/*/*/deploy/**, **/*.exe, **/*.png, */lib/**/*.so, */obj/**/*.o',
                 high: 'FIXME, XXX',
                 normal: 'TODO',
                 low: 'TD',
                 unstableTotalHigh: '0',
                 canComputeNew: true]
            )
        }
    } else if (env.ENABLE_LINUX32_BUILD == "true") {
        if (resultName.startsWith("linux32")) {
            junit testResults: '*/generated/**/TEST-*.xml', keepLongStdio: true
            step(
                [$class: 'TasksPublisher',
                 pattern: '**/*',
                 excludePattern: 'Jenkinsfile, deps/**, */generated/**, **/*.epf, **/*.jar, **/*.zip, **/.settings/**, **/*.class, target/*/*/deploy/**, **/*.exe, **/*.png, */lib/**/*.so, */obj/**/*.o',
                 high: 'FIXME, XXX',
                 normal: 'TODO',
                 low: 'TD',
                 unstableTotalHigh: '0',
                 canComputeNew: true]
            )
        }
    } else if (env.ENABLE_WINDOWS_BUILD == "true") {
        if (resultName.startsWith("windows")) {
            windowsTestsPublished = true
            junit testResults: '*/generated/**/TEST-*.xml', keepLongStdio: true
            step(
                [$class: 'TasksPublisher',
                 pattern: '**/*',
                 excludePattern: 'Jenkinsfile, deps/**, */generated/**, **/*.epf, **/*.jar, **/*.zip, **/.settings/**, **/*.class, target/*/*/deploy/**, **/*.exe, **/*.png, */lib/**/*.so, */obj/**/*.o',
                 high: 'FIXME, XXX',
                 normal: 'TODO',
                 low: 'TD',
                 unstableTotalHigh: '0',
                 canComputeNew: true]
            )
        }
    }

    if (resultName.startsWith("gui")) {
        junit testResults: '*/generated/**/TEST-*.xml', keepLongStdio: true
    } else if (!windowsTestsPublished && resultName.startsWith("windows")) {
        junit testResults: 'mil.dod.th.ose.rxtxtty.integration/generated/**/TEST-*.xml, mil.dod.th.ose.sdk.integration/generated/**/TEST-*.xml', keepLongStdio: true
    }
}

def extractOsusResults() {
    if (env.ENABLE_WINDOWS_BUILD == "true") {
        dir('windows') {
            unstash 'windows-results'
        }
    }

    if (env.ENABLE_LINUX64_BUILD == "true") {
        dir('linux64') {
            unstash 'linux64-results'
        }
    }

    if (env.ENABLE_LINUX32_BUILD == "true") {
        dir('linux32') {
            unstash 'linux32-results'
        }
    }

    if (env.ENABLE_DOCS_BUILD == "true") {
        dir('docs') {
            unstash 'docs-results'
        }
    }

    if (env.ENABLE_GUI_ASSET_BUILD == "true") {
        dir('gui-asset') {
            unstash 'gui-asset-results'
        }
    }

    if (env.ENABLE_GUI_OTHER_BUILD == "true") {
        dir('gui-other') {
            unstash 'gui-other-results'
        }
    }
}

def doOsusReports() {
    // Use older format for now
    step(
        [$class: 'WarningsPublisher',
         consoleParsers: [[parserName: 'Java Compiler (javac)'], [parserName: 'BND']],
         unstableTotalAll: '0',
         canComputeNew: true]
    )
    step(
        [$class: 'CoberturaPublisher',
         coberturaReportFile: '*/reports/cobertura/coverage.xml',
         onlyStable: false,
         failNoReports: false]
    )
    step(
        [$class: 'hudson.plugins.checkstyle.CheckStylePublisher',
         pattern: '*/reports/checkstyle-*results.xml',
         unstableTotalAll: '0',
         canComputeNew: true]
    )
    step(
        [$class: 'PmdPublisher',
         pattern: '*/reports/pmd-*results.xml',
         unstableTotalAll: '0',
         canComputeNew: true]
    )
    step(
        [$class: 'AnalysisPublisher',
         canComputeNew: true]
    )
}
