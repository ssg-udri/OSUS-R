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

# Intro

This project tests the mil.dod.th.remote.client to verify the bundle can be used to communicate
with a remote system.

# How to run

The tests can be run from Eclipse or Ant. Eclipse is the preferred way as the results will be displayed in a JUnit 
result window and individual tests can be run. 

If running from Eclipse, use the *Bnd OSGi Run Launcher* to run the `controller.bndrun` configuration (right click on
the file, *Run As* -> *Bnd OSGi Run Launcher*). This will launch a test controller that the remote client will 
communicate with. Once the controller is running, run any of the tests in the `src` folder using the *Bnd OSGi Test 
Launcher (OSGi)* (right click on any individual test or group of test and select the launcher under *Run As*).

If running from Ant, run the *test* target from the Ant build. This will launch the controller and run all tests.