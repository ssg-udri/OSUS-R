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

 DESCRIPTION:
 README for Raspberry Pi PIR Asset

==============================================================================
-->

#Raspberry Pi PIR Asset

##Introduction
This plug-in creates an asset for a PIR sensor connected to a Raspberry Pi through the GPIO interface. This README assumes a small knowledge of Linux and familiarity of OSUS.


Further documentation on the Pi4J library can be found at http://www.pi4j.com with more detailed pin diagrams to specific models, more installation/upgrade/uninstallation processes, and various other useful pages.

##Common GPIO Layout
Note: This is looking top-down to the Pi with the GPIO pins at the top of your view, and most excess pins have been removed from the diagram.

    *---*---*---*---*---*
    | 2 |   |GND|   |   |
    *---*---*---*---*---*
    | 1 |   |   | 7 |   |
    *---*---*---*---*---*

Pin 1 is a 3.3 VDC power pin and Pin 2 is a 5.0 VDC power pin. Be sure to connect to the correct pin your PIR sensor requires in the following setup. As well, GND is the ground pin and pin 7 is GPIO signal pin to be connected to.

##Prerequisites
    1. A Raspberry Pi computer (models A, A+, B Rev. 1 and 2, B+, and Pi 2 Model B) with Raspbian
    2. A 3-pin PIR sensor, where the pins are power, ground, and signal output
    3. 3 GPIO jumper cables that will likely be female-to-female, check your PIR sensor
    4. JDK 7 installed on the Pi with root permissions
    5. The Raspberry Pi PIR Asset plug-in JAR file
    6. The Raspbian distribution of Linux is the strongly suggested OS

##Physical Setup
    1. Be sure that the Pi is powered off.
    2. Connect the ground pins on both the Pi and the PIR.
    3. Connect the power input pin on your PIR to the corresponding power output pin on the Pi.
    4. Connect the signal output pin from the PIR to pin 7 on the Pi.
    5. Power on the Pi.

##Software Setup
    1. Install Java on the Pi if it is not already, as some earlier builds of Raspbian and other Linux distributions do not ship with it preinstalled.
        a. Enter 'java -version' in a terminal window to determine if Java is installed.
        b. Install JDK 7 by opening a terminal window and entering 'sudo apt-get update && sudo apt-get install oracle-java7-jdk'.
    2. Obtain the files and create a directory for an OSUS controller.
    3. Start the controller.
        a. Open a terminal window and navigate to the /bin/ directory in the directory of the OSUS controller.
        b. Enter 'sudo sh start_controller.sh'.
    4. Connect to the Pi using the OSUS GUI, load the plug-in JAR file, and create the asset as you typically would.
