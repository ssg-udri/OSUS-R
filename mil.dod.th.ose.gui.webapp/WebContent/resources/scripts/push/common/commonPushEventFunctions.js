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
//    Common push event functions to be used by all scripts which perform 
//    PrimeFaces push updates to specific pages. 
//==============================================================================
//thEvent prototype, all THOSE eventing variable should be properties of this.
window.thEvent || (window.thEvent = {});

//Array of components registered with the window.
window.thEvent.callbackMap || (window.thEvent.callbackMap={});

/**
 * Constructor for TopicCallback data structure. 
 * @param clientId
 *      the id of the component to updated
 * @param uniqueId
 *      unique id of the object associated with the callback registration. UniqueId is not needed in cases where the 
 *      componentId uniquely identifies the component to be updated i.e. if there is only one instance of the component
 *      on the page.
 * @param componentId
 *      component id of the web component to be updated via the callback
 * @param filter
 *      the filter to process when updating, may be null. A filter set as the empty string will
 *      have the same affect as a filter set to null. Both will be treated as if no filter was set.
 * @param callback
 *      the callback function to call for updates
 * @param regProps
 *      registration properties used when registering the callback function for the component represented by the id property
 * @param cleanupFilter
 *      function to determine if a topic callback needs to be removed
 * @param cleanupTopic
 *      push topic that denotes when the callbacks for this component should be cleaned up 
 */
function TopicCallback(clientId, uniqueId, componentId, filter, callback, regProps, cleanupFilter, cleanupTopic)
{
    this.clientId = clientId;
    this.uniqueId = uniqueId;
    this.componentId = componentId;
    this.filter = filter;
    this.callback = callback;
    this.regProps = regProps;
    this.cleanupTopic = cleanupTopic;
    this.cleanupFilter = cleanupFilter;
}

/**
 * Register a component with the window. Once registered if an event matches the topic and filter
 * used when registering the component, the component will be updated.
 * @param clientId
 *      client id of the component to be registered
 * @param topic
 *      push topic that denotes when the component should be updated
 * @param uniqueId
 *      unique id of the object associated with the callback registration. UniqueId is not needed in cases where the 
 *      componentId uniquely identifies the component to be updated i.e. if there is only one instance of the component
 *      on the page.
 * @param componentId
 *      component id of the web component to be updated via the callback
 * @param filter
 *      a filter value that will be used to further identify when a component should be updated, may be null or empty, but
 *      if present the format should be <key>=<value> (e.g., obj.uuid=#{linkLayer.uuid}). A null or empty string filter
 *      will be ignored.
 * @param callback
 *      the callback function to execute to actually update the given component
 * @param regProps
 *      additional properties identifying variable to evaluate when the callback function is called
 * @param cleanupFilter
 *      function to determine if a topic callback needs to be removed
 * @param cleanupTopic
 *      push topic that denotes when the callbacks for this component should be cleaned up 
 */
function registerComponent(clientId, topic, uniqueId, componentId, filter, callback, regProps, cleanupFilter, cleanupTopic)
{
    var callbacksForTopic = window.thEvent.callbackMap[topic];
    var eventReg;
    
    //this topic has not been previously registered. 
    if (!callbacksForTopic)
    {
        eventReg = new TopicCallback(clientId, uniqueId, componentId, filter, callback, regProps, cleanupFilter, cleanupTopic);
        callbacksForTopic = new Array();
        window.thEvent.callbackMap[topic] = callbacksForTopic;
        callbacksForTopic[callbacksForTopic.length] = eventReg;
    }
    else
    {
        var index = findCallBackNeedingUpdate(callbacksForTopic, uniqueId, componentId);
        
        if (index >= 0)
        {
            if (callbacksForTopic[index].clientId != clientId)
            {
                eventReg = callbacksForTopic[index];
                eventReg.clientId = clientId;
            }
        }
        else
        {
            eventReg = new TopicCallback(clientId, uniqueId, componentId, filter, callback, regProps, cleanupFilter, cleanupTopic);
            callbacksForTopic[callbacksForTopic.length] = eventReg;
        }
    }
}

/**
 * Unregisters a component from the window. 
 * 
 * @param uniqueIdToRemove
 *      the universally unique id of the object to remove call-backs for
 */
function unregisterComponents(uniqueIdToRemove)
{
    for (var topic in window.thEvent.callbackMap)
    {
        var callbacks = window.thEvent.callbackMap[topic];
        
        for (var i = 0; i < callbacks.length; i++)
        {
            var topicCallback = callbacks[i];

            if (topicCallback.uniqueId == uniqueIdToRemove)
            {
                window.thEvent.callbackMap[topic].splice(i,1);
                i--;
            }
        }
    }
}

/**
 * Checks if any callbacks need to be cleaned up based upon the components cleanup filter function.
 * 
 * @param topic
 *      the topic to check for cleanup on.
 * @param props
 *      properties of the message to pass to cleanup filter function.
 */
function checkForCleanup(topic, props)
{
    for (var currentTopic in window.thEvent.callbackMap)
    {
        var callbacks = window.thEvent.callbackMap[currentTopic];
        
        for (var i = 0; i < callbacks.length; i++)
        {
            var topicCallback = callbacks[i];
            if (topicCallback.cleanupTopic == topic)
            {
                if (topicCallback.cleanupFilter(props, topicCallback))
                {
                    // Remove callback if true
                    window.thEvent.callbackMap[currentTopic].splice(i,1);
                    i--;
                }
            }            
        }
    }
}

/**
 * Function to search through a given array which holds TopicCallbacks and see if it
 * already contains an object with the given unique id and component id
 * @param arrayToSearch
 *  array of TopicCallback objects to search through 
 * @param uniqueId
 *  unique id to find
 * @param componentId
 *  component id to find
 * @returns {Number}
 *  returns the index of the callback needing update if found. -1 if match not found
 */
function findCallBackNeedingUpdate(arrayToSearch, uniqueId, componentId)
{
    for (var i = 0; i < arrayToSearch.length; i++)
    {
        var topicCallback = arrayToSearch[i];

        if (topicCallback.uniqueId == uniqueId && topicCallback.componentId == componentId)
        {
            return i;
        }
    }

    return -1;
}

/**
 * Method to perform basic handling of push messages. If message is of type 
 * growl message the message will be output. If it is a generic event like 
 * updating the controller side bar then the event will be handled. If it is
 * an event specific to a certain page, then the handleMessage function of the
 * script located on the current page will be invoked.
 */
function messageRouterFunction(message)
{
    if (message != null)
    {
        performGenericOperations(message);
        
        if (isMessageEvent(message.type))
        {
            handleMessage(message);
        }
    }

    //necessary function!!! see commonPushFunctions.js for more explanation
    decrementRequestCount(messageSocket);
}

/**
 * Function to handle all registered component callbacks.
 * @param topic
 *  the topic on which to handle generic events.
 * @param props
 *  the properties that may be need to process the particular topic
 */
function handleRegisteredCallbacks(topic, props)
{
    var eventRegs = window.thEvent.callbackMap[topic];
    
    if (!eventRegs)
    {
        return;
    }
    
    for (var i = 0; i < eventRegs.length; i++)
    {
        var eventReg = eventRegs[i];

        if (eventReg.filter && eventReg.filter != "")
        {
            var filterPair = eventReg.filter.split("=");
            var filterKey = filterPair[0];
            var filterValue = filterPair[1];
            if (props[filterKey] == filterValue)
            {
                eventReg.callback(eventReg.clientId, topic, props, eventReg.regProps);
            }
        }
        else
        {
            // no filter, just call on callback
            eventReg.callback(eventReg.clientId, topic, props, eventReg.regProps);
        }
    }
}

/**
 * Function to handle all generic events by specified topic.
 * @param message
 *  the JSON object that is the event message
 */
function handleGenericEvents(message)
{
    var topic = message.topic;
    if (topic == window.thTopic.controllerUpdated || topic == window.thTopic.controllerRemoved
            || topic == window.thTopic.controllerAdded || topic == window.thTopic.activeControllerChanged)
    {        
        rcUpdateSidebar();
        
        checkForCleanup(topic, message.properties);
        
        if (topic != window.thTopic.controllerAdded && topic != window.thTopic.controllerRemoved)
        {
            rcUpdateActiveController();            
        }
        
        if (topic == window.thTopic.activeControllerChanged)
        {
            rcUpdateMainContent();
        }       
    }
    
    return;
}


/**
 * Function to handle performing generic events or displaying a growl message.
 * @param message
 *  the data that has been sent via push and contains data and a message type
 */
function performGenericOperations(message)
{
    if (isMessageGrowl(message.type))
    {
        //message is to be displayed as a growl message.
        handleGrowlMessage(message);
        logMessage("Client received growl message: " + message.summary + " (" + message.detail + ")");
    }
    else if (isMessageEvent(message.type))
    { 
        //message is an event. check to see if common things
        //should be updated.
        handleGenericEvents(message);
        handleRegisteredCallbacks(message.topic, message.properties);
        logMessage("Client received event message: " + message.topic);
    }
}

/**
 * Identifies if the passed in type is of the event type.
 * @param type
 *  the type of the message that is to be checked.
 * @returns {Boolean}
 *  true if the message is an event.
 */
function isMessageEvent(type)
{
    if (type == window.thType.eventMessageType)
    {
        return true;
    }
    
    return false;
}

/**
 * Identifies if the passed in type is of the growl message type.
 * @param type
 *  the type of the message that is to be checked.
 * @returns {Boolean}
 *  true if the message is a growl message.
 */
function isMessageGrowl(type)
{
    if (type == window.thType.growlMessageType)
    {
        return true;
    }
    
    return false;
}