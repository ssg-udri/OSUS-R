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
    README for the Smartphone Websocket Server, HTTP Server, and Asset Plug-In

==============================================================================
-->

# Smartphone Sensor Websocket Server, HTTP Server, and Asset Plug-In

## Introduction
This plug-in creates and starts a websocket and HTTP server on an OSUS controller. The HTTP server hosts the web content for a smartphone. Using that web content a smartphone can connect to the websocket server and return data such as GPS coordinates and battery status. When the smartphone connects to the websocket server, an asset representing that specific smartphone is created and all data returned from that phone will be shown as observations.

## Browser Compatibility
The latest Firefox and Chrome mobile applications are compatible with the hosted web content. Safari mobile and Apple devices have not been tested. The content does work with desktop Firefox and Chrome, as well, but the battery status will only be available for battery reliant devices (i.e., laptops and tablets).

You must give your browser permission to use your GPS location.

## Server Information
The web content is hosted on port 9191 and the websocket server is hosted on port 9090.

If either of these servers are unable to start, check that these ports are available.

Due to a limitation with the current websocket implementation, a websocket server restart requires an entire controller restart and not just a bundle restart/reinstall. The websocket server is unable to unbind from its port, so the only way to unbind is by restarting the controller (in which the Java process is killed and the port is unbound by the OS).

## Smartphone Asset
The smartphone asset is an asset representing a smartphone connected to the websocket server. A smartphone is identified by its hostname.

Do NOT remove an asset of this type or you will no longer retrieve observations from the smartphone that this asset represents until the smartphone reconnects. Creating an asset of this type will create an asset that does nothing, as it should only be created by the websocket server.

## Web Page Features
The hosted web page provides switches to turn on or off certain functionality. Each switch and its purpose is provided below:

##### Sensor Switch
The sensor switch creates the websocket connection to the websocket server. While this switch is off, no other functionality is performed. Turning this switch off while background functionality takes place will disconnect the smartphone from the websocket server and halt all functionality.

##### GPS Switch
The GPS switch determines whether or not any GPS functionality is available. If any background GPS functions are running when this is switched off, all background GPS functionality will halt. Tapping this switch back to on with any sub-switches on will resume GPS functionality.

##### Timed GPS Switch
The timed GPS switch determines if the timed GPS update function should be on or off. This function updates the GPS coordinates based on a set interval of time. The number field provided is the frequency in minutes in which the GPS coordinates update.

##### GPS Position-Based Update
The GPS position-based switch determines if the position-based function should be on or off. This function finds the difference of the current GPS coordinates and the previous recorded coordinates to determine if the phone position has changed more than the specified distance. The number field provided is the distance in meters in which the phone must travel to update.

##### Battery Switch
The battery switch determines whether or not any battery functionality is available. If any background battery functions are running when this is switched off, all background battery functionality will halt. Tapping this switch back to on with any sub-switches on will resume battery functionality.

##### Timed Battery Switch
The timed battery switch determines if the timed battery status update function should be on or off. This function updates the battery status based on a set interval of time. The number field provided is the frequency in minutes in which the battery status updates.

##### Status-Based Battery Switch
This switch turns on or off the functionality of listening for changes in the system battery status and determines if the most recent battery status change is lower than the specified battery status minimum. The number field provided determines the minimum in percentage.