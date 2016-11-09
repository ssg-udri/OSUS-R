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
    README for the HikVision Camera Asset Plug-In

==============================================================================
-->

#HikVision Camera Asset Plug-In

##Introduction
This plug-in will allow you to gain access to a HikVision IP Surveillance Camera.
You can connect over the internet using the correct IP, user name and password. 

####Abilities
    azimuth: 0 - 360 degrees
    elevation: 0 - 90 degrees
    zoom: 0X - 30X
    capture images 

This README assumes familiarity with OSUS and basic knowledge of the HikVision 
Camera to obtain the IP address.

Once the configuration is complete you will be able to obtain an image and move 
the camera as needed.

##Prerequisites

    1. Version: set to current version (1 is default)
    2. IP Address: set in (xxx.xxx.xxx.xxx) format
    3. User Name: set a valid user 
    4. Password: set valid password

###Azimuth
You have the full range of 360 degrees. The limitation of the GUI are -179 to 180. 
These are fixed degrees on the camera.
Within the GUI you can access the azimuth in the 
Command/Control tab -> Pan Tilt -> panTilt, click Add. 
Within the azimuth row -> click add -> then you can change the value to change the position of the camera.

###Elevation
You can move the camera's elevation 90 degrees, where 0 is parallel to the base of 
the camera and 90 is perpendicular to the base of the camera.
Within the GUI you can access elevation in the 
Command/Control tab -> Pan Tilt -> panTilt, click Add.
Within the elevation row -> click add -> then you can change the value to change the elevation of the camera.

###Zoom
You have the full range of the camera, where 0.00 is completely zoomed out and 1.00 
is completely zoomed in.
Within the GUI you can access zoom in Command/Control tab -> Camera Settings.
Within the zoom row you can change the value to increase or decrease the zoom.

###Capture Image
Within the GUI you can select Capture Data, you will capture a still image from the camera. 
You can view the images in the Observations tab. 