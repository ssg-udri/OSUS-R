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
//
// This javascript file is used to only allow numbers 0-9 and letters a-f or A-F
// based on a key code retrieved from the event object. Used specifically for 
// the editable system id field on the gui configuration page. Your inputmask 
// component must have the id 'sysId' set for this script to work.
//
//==============================================================================

//Function executed on load of the page. This function adds a custom mask to the 
//masked input plugin (digitalbush.com/projects/masked-input-plugin). It is the 
//plugin which comes with JQuery which the inputmask component uses for its
//functionality. The default masks do not include a mask for things like hex input.
//Therefore, we must add the below mask to the default plugin and define a new symbol
//to denote the mask. Last, we must set the mask on the inputmask component. The 
//default inputmask requires all of the required characters to be input before it
//will process the user input. This is fixed by adding a '?' to make the following
//characters optional. The default placeholder for a inputmask is an underscore. 
//This causes problems with scrolling into parts of the mask that aren't filled. This 
//was solved by changing the placeholder into an empty string.
jQuery(function($){
    var inputMask = $('[id$="sysId"]');
    
    if(inputMask != null)
    {  
        $.mask.definitions['~'] = '[0-9a-fA-F]';
        
        inputMask.mask("0x?~~~~~~~~", {placeholder:""});
    }
});