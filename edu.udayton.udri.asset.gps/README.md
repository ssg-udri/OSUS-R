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

# GPS Plugin

## Introduction
This plug-in creates an asset that communicates over a serial connection that retrieves and interprets NMEA 0183 sentences from a GPS device. These recordings are persistently stored as observations by the asset.

This README assumes familiarity with OSUS as well as some basic knowledge of the NMEA 0183 protocol and GPS devices as some terms will be referenced.

Once the prerequisites and preparation is complete the asset is ready to be created and to capture data.

## Prerequisites
    1. A NMEA 0183 compatible GPS device
    2. A means of connecting the computer and sensor, e.g. USB, etc.
    3. JDK 7 installed on the computer
    4. The GPS Asset plug-in JAR file
    5. The appropriate driver for your device (If required), as well as anything else for device specific setup.
    
## GPS Asset Properties
The GPS Asset has multiple properties that must be set in order to properly read from a GPS device. Because every device is different you should be sure to look up the technical specifications for your GPS device. Below, each of these properties are briefly described.

Baud rate: The baud rate is the rate at which information is transferred in a communication channel. In the serial port context, "9600 baud" means that the serial port is capable of transferring a maximum of 9600 bits per second.

Data-bits: The number of bits per transmission. Typically this value is 8.

Comm-Port: This is the name of the port over which you are connecting, for example "COM4". If you are unsure of what your Comm-Port is named you can look in your computers's Device Manager under COM/SERIAL PORT.

## Troubleshooting
Sometimes Windows may not connect to the device properly. In this event there are a couple of steps to take:

    1. Check in the Device Manager in Windows to see that your device is connected under the COM/SERIAL PORT section.
    2. Using a program such as PuTTY, attempt to connect to your device and see if it is receiving data. Note: a comms port can only have one connection at a time. Using PuTTY while you are attempting to make observations with the GPS Asset will cause the asset to fail.
    3. Be sure to check if your device requires any special set up such as a driver.
