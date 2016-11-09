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
    README for the BMP180 Temperate/Pressure Sensor Plug-In

==============================================================================
-->

# BMP180 Temperate/Pressure Sensor Plug-In

## Introduction
This plug-in creates an asset that communicates over an I2C bus to retrieve and interpret the temperature and pressure from a BMP180 sensor connected to the Raspberry Pi 2 Model B. The temperature returned is in Celsius and the pressure returned is in millibars. The pressure returned from the sensor is converted to sea-level pressure as it is a common standard for weather stations. The user must provide the altitude of the sensor.

This README assumes a small knowledge of Linux and familiarity of OSUS. The OSUS controller must be started with root permission.

Once the prerequisites, preparation, and wiring have been completed, the asset is ready to be created and to capture data.

## Prerequisites
    1. A Raspberry Pi computer (Pi 2 Model B) with Raspbian
    2. A BMP180 sensor
    3. A means of connecting the Pi and sensor, i.e. jumper cables
    4. JDK 7 installed on the Pi with root permissions
    5. The Raspberry Pi BMP180 Asset plug-in JAR file

## BMP180 Sampling Settings
The BMP180 sensor has different sampling settings affecting the accuracy, speed, and power consumption of the process of recording pressure. The user must set the sampling setting when configuring the asset.

    Sampling Setting     | Parameter | Number of Samples | Retrieval Time 
    ----------------------------------------------------------------------
    Ultra Low Power      |     0     |         1         |     4.5ms
    ----------------------------------------------------------------------
    Standard             |     1     |         2         |     7.5ms
    ----------------------------------------------------------------------
    High Resolution      |     2     |         4         |     13.5ms
    ----------------------------------------------------------------------
    Ultra High Resolution|     3     |         8         |     25.5ms
    
## Preparation
In order for your Raspberry Pi to communicate over I2C, you must first configure I2C on the Pi.

    1. Open a new terminal window
    2. Type and enter 'sudo modprobe i2c-bcm2708'
    3. Type and enter 'sudo modprobe i2c-dev'
    4. Type and enter 'sudo raspi-config'
    5. Select Select '8 Advanced Options'
    6. Select 'A7 I2C'
    7. Select 'Yes'
    8. Select 'Ok'
    9. Select 'Yes'
    10.Select 'Ok'
    11.Select 'Finish'
    12.Reboot your Pi
    
## Wiring
Note: This is looking top-down to the Pi with the GPIO pins at the top of your view, and most excess pins have been removed from the diagram.

    *---*---*---*---*
    |   |   |GND|   |
    *---*---*---*---*
    | 1 | 2 | 3 |   |
    *---*---*---*---*
Pin 1 is the 3.3 VDC power pin, pin 2 is the SDA pin, pin 3 is the SCL pin, and GND is the ground pin. Connect the Raspberry Pi pins to the corresponding BMP180 pins using jumper wires, a breadboard, etc.
    