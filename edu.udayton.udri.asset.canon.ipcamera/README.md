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
    README for the Canon IP Camera Asset Plug-In

==============================================================================
-->

#Canon IP Camera Asset Plug-In

##Introduction
This plug-in will give you access to a Canon VB-C60 IP camera. You are able to connect to the camera through the internet with the correct IP address.

####Abilites
    azimuth: -180 - 180 degrees
    elevation: -25 - 90 degrees
    zoom: 0X - 40X
    capture images

This README assumes familiarity with OSUS and basic knowledge of the Canon VB-C60 IP Camera to obtain the IP address.

Once the configuration is complete you will be able to use the GUI to obtain an image and move the camera as needed.

##Prerequisites

    1. IP Address: set in (xxx.xxx.xxx.xxx) format

###Azimuth
You have the full range of 360 degrees. The limitation of the GUI are -179 to 180. Within the GUI you can access the azimuth in the
Command/Control tab -> Pan Tilt -> panTilt, click Add.
Within the azimuth row -> click add -> then you can change the value to change the azimuth of the camera.

###Elevation
You can move the camera's elevation between -25 and 90 degrees,
where 0 is parallel to the base of the camera and 90 is perpendicular to the base of the camera.
Within the GUI you can access elevation in the
Command/Control tab -> Pan Tilt -> panTilt, click Add.
Within the elevation row -> click add -> then you can change the value to change the elevation of the camera.

###Zoom
You have the full range of the camera, where 0.00 is the least amount of zoom and 1.00 is the most.
Within the GUI you can access zoom in the Command/Control tab -> Camera Settings.
Within the zoom row you can change the value to increase or decrease the zoom.

###Capture Image
Within the GUI you can select Capture Data to capture a still image from the camera. You can view the images in the Observations tab. 