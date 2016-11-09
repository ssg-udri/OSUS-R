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
//    The purpose of this script is to allow for Growl messages to be 
//    automatically pushed to a user. This script is used in conjunction with
//    the utils.push package of THOSE.
//==============================================================================

/*
 * GLOBALS
 *               
 * growlSticky, growlTimed - the p:growl tags located on the mainscreenTemplate which
 *                           display growl messages.
 */

/*
 * Function to properly update the correct Growl message handler based on the 
 * sticky flag that is sent within the message. If sticky is true then the 
 * growlSticky component will be given the message data. Otherwise, the 
 * growlTimed message will be updated. This data has been pushed via the socket 
 * on the mainScreenTemplate.
 * 
 * @param growl
 *  the JSON object which contains all data necessary to display a growl 
 *  message.
 */
function handleGrowlMessage(growl)
{
    //make sure that the growl data is not null. Sometimes multiple requests 
    //occur with the growl data being null. Therefore, this is why the check is needed.
    if(growl != null)
    {
        //if this message is to be displayed by the sticky growl
        if(growl.sticky)
        {
            //growl messages are updated via their JSON format 
            growlSticky.renderMessage({summary:growl.summary, detail: growl.detail, 
                severity: growl.severity, rendered: true});
        }
        else
        {
            growlTimed.renderMessage({summary:growl.summary, detail: growl.detail, 
                severity: growl.severity, rendered: true});
        }
    }
    
    return;
}

/**
 * Function will produce a timed growl message to indicate page
 * has been refreshed to keep session alive and will refresh the page.
 */
function produceSessionTimeoutGrowl()
{
    growlTimed.show([{summary:'Session About to Timeout', 
        detail:'Session is about to timeout. The page will be refreshed in 5 seconds to maintain session.',
        severity:'info', rendered: true}]);
    
    setTimeout(function(){
        location.reload(true);
    }, 5000);
}
