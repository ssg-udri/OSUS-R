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
//    File used to check for the existence of the eventSocket's handleEvent 
//    function.
//==============================================================================

/*
 * GLOBALS
 * 
 * messageSocket - is located on the mainscreenTemplate and serves as the connection over 
 *               which messages are sent.
 *
 * handleEvent - the function that needs to be present so that the messages sent over the
 *               event socket can be processed.
 */

/**
 * Function to check when the page loads if the handleEvent
 * function exists for the eventSocket.
 */
$(document).ready(function(){
    if (typeof handleMessage == 'function')
    {
        //do nothing. this is good. it means a the handleEvent is present. 
    }
    else
    {
        alert("WARNING! Push event socket function handleEvent not detected!");
    }
    
    return;
});