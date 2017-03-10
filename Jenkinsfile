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

env.ENABLE_GUI_BUILDS = "false"
if (env.BRANCH_NAME == "master" || env.BRANCH_NAME == "development" || env.BRANCH_NAME.startsWith("release/")) {                                          
    env.ENABLE_GUI_BUILDS = "true"
} else {
    try {
        timeout(time: 45, unit: 'SECONDS') {
            env.ENABLE_GUI_BUILDS = input(
              id: 'GUI', message: 'Enable GUI Builds?', parameters: [
                [$class: 'BooleanParameterDefinition', defaultValue: false, description: '', name: 'Enable checkbox to confirm']
              ]).toString()
        }
    } catch(err) { // timeout reached or input false
        echo "No input received for GUI builds"
    }
}

echo "GUI builds enabled: ${env.ENABLE_GUI_BUILDS}"

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
        step(
            [$class: 'TasksPublisher',
             pattern: '**/*',
             excludePattern: 'Jenkinsfile, deps/**, */generated/**, **/*.epf, **/*.jar, **/*.zip, **/.settings/**, **/*.class, target/*/*/deploy/**, **/*.exe, **/*.png',
             high: 'FIXME, XXX',
             normal: 'TODO',
             low: 'TD',
             unstableTotalHigh: '0',
             canComputeNew: true]
        )
    }
}

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

platforms['docs'] = {
    node('linux') {
        deleteDir()
        unstash 'sources'
        withEnv([
            "JAVA_HOME=${tool 'JDK 8'}",
            "JAVA_INCLUDE=${tool 'JDK 8'}/include",
            "PATH+ANT=${tool 'Ant 1.8.4'}/bin",
            "PATH+JAVA_BIN=${tool 'JDK 8'}/bin"
        ]) {
            success = true
            try {
                sh 'env'
                sh 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-docs'
            }
            catch (e) {
                echo "Shell script fail is because node is not linux, trying windows scripts next"
                success = false
            }
            if(!success) {
                try {
                    bat 'set'
                    bat 'ant -logger org.apache.tools.ant.listener.BigProjectLogger ci-standard'
                }
                catch (e) {
                    throw (e) //throw exception, neither worked, something went wrong
                }
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

if (env.ENABLE_GUI_BUILDS == "true") {
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
} finally {
    node {
        step([$class: 'StashNotifier'])
    }
}

def saveOsusResults(resultName) {
    if (resultName.startsWith("docs")) {
        stash name: resultName, includes: 'mil.dod.th.core.*/generated/*, reports/build.properties'
    } else {
        stash name: resultName, includes: 'reports/checkstyle-*results.xml, reports/pmd-*results.xml, */generated/*.jar, */generated/*.zip, target/*/*/bin/*.zip, reports/build.properties, mil.dod.th.ose.gui.integration/generated/test-data/**'
    }

    if (resultName.startsWith("linux64") || resultName.startsWith("gui")) {
        junit testResults: '*/generated/**/TEST-*.xml', keepLongStdio: true
    } else if (resultName.startsWith("windows")) {
        junit testResults: 'mil.dod.th.ose.rxtxtty.integration/generated/**/TEST-*.xml, mil.dod.th.ose.sdk.integration/generated/**/TEST-*.xml', keepLongStdio: true
    }
}

def extractOsusResults() {
    dir('windows') {
        unstash 'windows-results'
    }
    dir('linux64') {
        unstash 'linux64-results'
    }
    dir('linux32') {
        unstash 'linux32-results'
    }
    dir('docs') {
        unstash 'docs-results'
    }

    if (env.ENABLE_GUI_BUILDS == "true") {
        dir('gui-asset') {
            unstash 'gui-asset-results'
        }
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
    // Cobertura not compatible with pipeline yet
    //step(
    //    [$class: 'CoberturaPublisher',
    //     coberturaReportFile: 'linux64/reports/cobertura/coverage.xml']
    //)
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
