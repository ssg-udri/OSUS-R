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
//    File used to connect and disconnect socket connections for the eventSocket.
//    This file exists because there seems to be some problems establishing and 
//    disconnecting from a connection from each individual page script. 
//==============================================================================

/*
 * GLOBALS
 * 
 * messageSocket - is located on the mainscreenTemplate and serves as the connection over 
 *               which messages are sent.
 *
 */

/**
 * Function which executes when the DOM for the page is ready.
 * This is needed as per the workaround for PrimeFaces Push to 
 * work with GlassFish. See 
 * http://forum.primefaces.org/viewtopic.php?f=10&t=27224
 */
$(window).load(function()
{
    //this is here for a couple of reasons. Since there is a broadcaster cache
    //now implemented to ensure that long-polling connections receive all messages
    //it is once again possible for multiple data messages to be appended within one 
    //message when sent. Therefore, the track message length attribute is required. 
    //The web.xml must also specify that the trackMessageLength interceptor is to be 
    //used.
    messageSocket.cfg.request.trackMessageLength = true;
    
    messageSocket.connect();
    
    return;
});

/**
 * Function to disconnect the event socket before the page unloads. Listens to the unload 
 * event based on an issue with IE and how it posts certain events. Originally, the 
 * beforeunload event was listened to. However, in IE when an anchor tag is clicked, 
 * the beforeunload event is fired. This is not the case in browsers like Firefox. Therefore,
 * when sidebars were clicked in IE, this function would execute and close the socket connection.
 * The socket would not be reconnected because the page was not being reloaded (it was only an AJAX request).
 * Therefore, the unload event should be listened to because it is only fired when the page is refreshed or 
 * a new page is navigated to. See IE documentation on these events. 
 * 
 * beforeunload - (http://msdn.microsoft.com/en-us/library/ie/ms536907%28v=vs.85%29.aspx)
 * unload       - (http://msdn.microsoft.com/en-us/library/ie/ms536973%28v=vs.85%29.aspx)
 * 
 */
$(window).on('unload', function()
{
    messageSocket.disconnect();
    return;
});


