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
    README for the UVC Webcam Asset Plug-In

==============================================================================
-->

#UVC Webcam Asset

##Introduction
This plug-in creates an asset to capture and return images from any UVC compliant webcam connected to a Linux or Windows computer. OS X has not been tested, but there is a chance for compatibility.

Be sure to check that your webcam is UVC compatible, or else this plug-in will not work.

##Supported Resolutions
    QQVGA || 176, 144
    CIF   || 352, 288
    HVGA  || 480, 400
    VGA   || 640, 480
    PAL   || 768, 576
    SVGA  || 800, 600
    HD720 || 1280, 720
    WXGA  || 1280, 768
    SXGA  || 1280, 1024
    UXGA  || 1600, 1200
    HD1080|| 1920, 1080
    QXGA  || 2048, 1536
    
    If a capture resolution is chosen that is higher than your camera's maximum resolution, then the capture resolution will default to your camera's maximum resolution.

##Prerequisites
    1. A Windows or Linux computer with USB
        a. If you're on an older Linux distribution, you may need to download and install the Video4Linux2 drivers, although most distributions have it preinstalled.
    2. A UVC webcam (preferably with a supported resolution)
    3. JRE or JDK 7+
    4. The asset plug-in JAR file

##Setup
    1. Install Java 7+ if it is not installed already.
    2. Connect the desired webcam to use to the computer. (Note: If any other webcam is connected first, disconnect that webcam first. The plug-in selects the first connected webcam as the default.)
    3. Obtain the files and create a directory for an OSUS controller.
    4. Start the controller, connect to it remotely from a GUI client, install the plug-in JAR file, and create the asset.
    5. To capture an image, click on the Command/Control tab on the asset page, click the Capture Image dropdown, and click Send Command. (Note: The image capture is instantaneous, but handling of the image data and the video stream can slow the computer depending on the computer's performance, such as a Raspberry Pi.)