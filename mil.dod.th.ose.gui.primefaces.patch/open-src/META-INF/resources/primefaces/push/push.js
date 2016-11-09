/**
 * Copyright 2012 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * IE streaming/XDR supports is copied/highly inspired by http://code.google.com/p/jquery-stream/
 *
 * Copyright 2011, Donghwan Kim
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * LocalStorage supports is copied/highly inspired by https://github.com/flowersinthesand/jquery-socket
 * Copyright 2011, Donghwan Kim
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * ATMOSPHERE VERSION INCLUDED HERE IS 1.0.13
 *
 * */
/**
 * Official documentation of this library: https://github.com/Atmosphere/atmosphere/wiki/jQuery.atmosphere.js-API
 */
jQuery.atmosphere=function(){jQuery(window).bind("unload.atmosphere",function(){jQuery.atmosphere.unsubscribe()});jQuery(window).keypress(function(f){27==f.keyCode&&f.preventDefault()});return{version:"1.0.13",requests:[],callbacks:[],onError:function(){},onClose:function(){},onOpen:function(){},onMessage:function(){},onReconnect:function(){},onMessagePublished:function(){},onTransportFailure:function(){},onLocalMessage:function(){},AtmosphereRequest:function(f){function j(b){l();L=!0;F=!1;A=0;r= H=x=n=null;c=jQuery.extend(c,b);c.mrequest=c.reconnect;c.reconnect||(c.reconnect=!0)}function d(){if(c.shared){a:{var b=c,a=function(a){a=jQuery.parseJSON(a);var e=a.data;if("c"===a.target)switch(a.type){case "open":p("opening","local",c);break;case "close":h||(h=!0,"aborted"===e.reason?M():e.heir===E?d():setTimeout(function(){d()},100));break;case "message":s(e,"messageReceived",200,b.transport);break;case "localMessage":V(e)}},g=function(){var a=RegExp("(?:^|; )("+encodeURIComponent(G)+")=([^;]*)").exec(document.cookie); if(a)return jQuery.parseJSON(decodeURIComponent(a[2]))},e,B,h,G="atmosphere-"+b.url,f={storage:function(){if(jQuery.atmosphere.supportStorage()){var c=window.localStorage,e=function(a){return jQuery.parseJSON(c.getItem(G+"-"+a))};return{init:function(){var b=e("children").concat([E]);c.setItem(G+"-children",jQuery.stringifyJSON(b));jQuery(window).on("storage.socket",function(b){b=b.originalEvent;b.key===G&&b.newValue&&a(b.newValue)});return e("opened")},signal:function(a,b){c.setItem(G,jQuery.stringifyJSON({target:"p", type:a,data:b}))},close:function(){var a,g=e("children");jQuery(window).off("storage.socket");g&&(a=jQuery.inArray(b.id,g),-1<a&&(g.splice(a,1),c.setItem(G+"-children",jQuery.stringifyJSON(g))))}}}},windowref:function(){var b=window.open("",G.replace(/\W/g,""));if(b&&!b.closed&&b.callbacks)return{init:function(){b.callbacks.push(a);b.children.push(E);return b.opened},signal:function(a,c){!b.closed&&b.fire&&b.fire(jQuery.stringifyJSON({target:"p",type:a,data:c}))},close:function(){if(!h){var c=b.callbacks, e=jQuery.inArray(a,c);-1<e&&c.splice(e,1);c=b.children;e=jQuery.inArray(E,c);-1<e&&c.splice(e,1)}}}}};if((e=g())&&!(1E3<jQuery.now()-e.ts))if(B=f.storage()||f.windowref()){w={open:function(){var c;N=setInterval(function(){var b=e;e=g();(!e||b.ts===e.ts)&&a(jQuery.stringifyJSON({target:"c",type:"close",data:{reason:"error",heir:b.heir}}))},1E3);(c=B.init())&&setTimeout(function(){p("opening","local",b)},50);return c},send:function(a){B.signal("send",a)},localSend:function(a){B.signal("localSend",jQuery.stringifyJSON({id:E, event:a}))},close:function(){F||(clearInterval(N),B.signal("close"),B.close())}};break a}w=void 0}if(null!=w&&("debug"==c.logLevel&&jQuery.atmosphere.debug("Storage service available. All communication will be local"),w.open(c)))return;"debug"==c.logLevel&&jQuery.atmosphere.debug("No Storage service available.");w=null}c.firstMessage=!0;c.ctime=jQuery.now();"websocket"!=c.transport&&"sse"!=c.transport?(setTimeout(function(){p("opening",c.transport,c)},500),I()):"websocket"==c.transport?null!=c.webSocketImpl|| window.WebSocket||window.MozWebSocket?W(!1):q("Websocket is not supported, using request.fallbackTransport ("+c.fallbackTransport+")"):"sse"==c.transport&&(window.EventSource?t(!1):q("Server Side Events(SSE) is not supported, using request.fallbackTransport ("+c.fallbackTransport+")"))}function p(b,a,g){if(c.shared&&"local"!=a){var K=function(a){a=jQuery.parseJSON(a);var b=a.data;if("p"===a.target)switch(a.type){case "send":X(b);break;case "localSend":V(b);break;case "close":M()}},B=function(){document.cookie= encodeURIComponent(d)+"="+encodeURIComponent(jQuery.stringifyJSON({ts:jQuery.now()+1,heir:(h.get("children")||[])[0]}))},h,d="atmosphere-"+c.url,f={storage:function(){if(jQuery.atmosphere.supportStorage()){var a=window.localStorage;return{init:function(){jQuery(window).on("storage.socket",function(a){a=a.originalEvent;a.key===d&&a.newValue&&K(a.newValue)})},signal:function(b,c){a.setItem(d,jQuery.stringifyJSON({target:"c",type:b,data:c}))},get:function(b){return jQuery.parseJSON(a.getItem(d+"-"+b))}, set:function(b,c){a.setItem(d+"-"+b,jQuery.stringifyJSON(c))},close:function(){jQuery(window).off("storage.socket");a.removeItem(d);a.removeItem(d+"-opened");a.removeItem(d+"-children")}}}},windowref:function(){var a=d.replace(/\W/g,""),b=(jQuery('iframe[name="'+a+'"]')[0]||jQuery('<iframe name="'+a+'" />').hide().appendTo("body")[0]).contentWindow;return{init:function(){b.callbacks=[K];b.fire=function(a){var c;for(c=0;c<b.callbacks.length;c++)b.callbacks[c](a)}},signal:function(a,c){!b.closed&&b.fire&& b.fire(jQuery.stringifyJSON({target:"c",type:a,data:c}))},get:function(a){return!b.closed?b[a]:null},set:function(a,c){b.closed||(b[a]=c)},close:function(){}}}};Q=function(a){h.signal("message",a)};h=f.storage()||f.windowref();h.init();"debug"==c.logLevel&&jQuery.atmosphere.debug("Installed StorageService "+h);h.set("children",[]);null!=h.get("opened")&&!h.get("opened")&&h.set("opened",!1);B();N=setInterval(B,1E3);C=h}null!=C&&C.set("opened",!0);g.close=function(){M()};null==e.error&&(e.request=g, g=e.state,e.state=b,e.status=200,b=e.transport,e.transport=a,a=e.responseBody,z(),e.responseBody=a,e.state=g,e.transport=b)}function J(b){b.transport="jsonp";var a=c;null!=b&&"undefined"!=typeof b&&(a=b);b=a.url;var g=a.data;a.attachHeadersAsQueryString&&(b=k(a),""!=g&&(b+="&X-Atmosphere-Post-Body="+encodeURIComponent(g)),g="");u=jQuery.ajax({url:b,type:a.method,dataType:"jsonp",error:function(b,c,g){e.error=!0;300>b.status&&a.reconnect&&A++<a.maxReconnectOnClose?v(u,a):m(b.status,g)},jsonp:"jsonpTransport", success:function(b){if(a.reconnect)if(-1==a.maxRequest||a.requestCount++<a.maxRequest){R(u,a);a.executeCallbackBeforeReconnect||v(u,a);b=b.message;if(null!=b&&"string"!=typeof b)try{b=jQuery.stringifyJSON(b)}catch(e){}D(a,b)&&s(b,"messageReceived",200,a.transport);a.executeCallbackBeforeReconnect&&v(u,a)}else jQuery.atmosphere.log(c.logLevel,["JSONP reconnect maximum try reached "+c.requestCount]),m(0,"maxRequest reached")},data:a.data,beforeSend:function(b){S(b,a,!1)}})}function t(b){e.transport= "sse";var a;a=k(c);"debug"==c.logLevel&&(jQuery.atmosphere.debug("Invoking executeSSE"),jQuery.atmosphere.debug("Using URL: "+a));b&&p("re-opening","sse",c);if(c.enableProtocol&&b){var g=jQuery.now()-c.ctime;c.lastTimestamp=Number(c.stime)+Number(g)}if(b&&!c.reconnect)null!=x&&l();else{try{x=new EventSource(a,{withCredentials:c.withCredentials})}catch(d){m(0,d);q("SSE failed. Downgrading to fallback transport and resending");return}0<c.connectTimeout&&(c.id=setTimeout(function(){b||l()},c.connectTimeout)); x.onopen=function(){"debug"==c.logLevel&&jQuery.atmosphere.debug("SSE successfully opened");b||p("opening","sse",c);b=!0;"POST"==c.method&&(e.state="messageReceived",x.send(c.data))};x.onmessage=function(a){a.origin!=window.location.protocol+"//"+window.location.host?jQuery.atmosphere.log(c.logLevel,["Origin was not "+window.location.protocol+"//"+window.location.host]):(a=a.data,D(c,a)&&(e.state="messageReceived",e.status=200,y(a,c,e)||(z(),e.responseBody="",e.messages=[])))};x.onerror=function(){clearTimeout(c.id); O(b);l();F?jQuery.atmosphere.log(c.logLevel,["SSE closed normally"]):b?c.reconnect&&"sse"==e.transport&&(A++<c.maxReconnectOnClose?(c.id=setTimeout(function(){t(!0)},c.reconnectInterval),e.responseBody="",e.messages=[]):(jQuery.atmosphere.log(c.logLevel,["SSE reconnect maximum try reached "+A]),m(0,"maxReconnectOnClose reached"))):q("SSE failed. Downgrading to fallback transport and resending")}}}function W(b){e.transport="websocket";if(c.enableProtocol&&b){var a=jQuery.now()-c.ctime;c.lastTimestamp= Number(c.stime)+Number(a)}var a=k(c),a=decodeURI(jQuery('<a href="'+a+'"/>')[0].href.replace(/^http/,"ws")),g=!1;"debug"==c.logLevel&&(jQuery.atmosphere.debug("Invoking executeWebSocket"),jQuery.atmosphere.debug("Using URL: "+a));b&&p("re-opening","websocket",c);b&&!c.reconnect?null!=n&&l():(n=null!=c.webSocketImpl?c.webSocketImpl:window.WebSocket?new WebSocket(a):new MozWebSocket(a),0<c.connectTimeout&&(c.id=setTimeout(function(){if(!b){n.onclose({code:1002,reason:"",wasClean:!1});try{l()}catch(a){}}}, c.connectTimeout)),c.id=setTimeout(function(){setTimeout(function(){l()},c.reconnectInterval)},c.timeout),n.onopen=function(){"debug"==c.logLevel&&jQuery.atmosphere.debug("Websocket successfully opened");b||p("opening","websocket",c);b=!0;n.webSocketOpened=b;"POST"==c.method&&(e.state="messageReceived",n.send(c.data))},n.onmessage=function(a){clearTimeout(c.id);c.id=setTimeout(function(){setTimeout(function(){l()},c.reconnectInterval)},c.timeout);a=a.data;D(c,a)&&(e.state="messageReceived",e.status= 200,y(a,c,e)||(z(),e.responseBody="",e.messages=[]))},n.onerror=function(){clearTimeout(c.id)},n.onclose=function(a){if(!g){clearTimeout(c.id);var d=a.reason;if(""===d)switch(a.code){case 1E3:d="Normal closure; the connection successfully completed whatever purpose for which it was created.";break;case 1001:d="The endpoint is going away, either because of a server failure or because the browser is navigating away from the page that opened the connection.";break;case 1002:d="The endpoint is terminating the connection due to a protocol error."; break;case 1003:d="The connection is being terminated because the endpoint received data of a type it cannot accept (for example, a text-only endpoint received binary data).";break;case 1004:d="The endpoint is terminating the connection because a data frame was received that is too large.";break;case 1005:d="Unknown: no status code was provided even though one was expected.";break;case 1006:d="Connection was closed abnormally (that is, with no close frame being sent)."}jQuery.atmosphere.warn("Websocket closed, reason: "+ d);jQuery.atmosphere.warn("Websocket closed, wasClean: "+a.wasClean);O(b);g=!0;F?jQuery.atmosphere.log(c.logLevel,["Websocket closed normally"]):b?c.reconnect&&"websocket"==e.transport&&(l(),c.reconnect&&A++<c.maxReconnectOnClose?c.id=setTimeout(function(){e.responseBody="";e.messages=[];W(!0)},c.reconnectInterval):(jQuery.atmosphere.log(c.logLevel,["Websocket reconnect maximum try reached "+A]),jQuery.atmosphere.warn("Websocket error, reason: "+a.reason),m(0,"maxReconnectOnClose reached"))):q("Websocket failed. Downgrading to Comet and resending")}})} function D(b,a){if(0!=jQuery.trim(a)&&b.enableProtocol&&b.firstMessage){b.firstMessage=!1;var c=a.split(b.messageDelimiter),e=2==c.length?0:1;b.uuid=jQuery.trim(c[e]);b.stime=jQuery.trim(c[e+1]);return!1}return!0}function m(b,a){l();e.state="error";e.reasonPhrase=a;e.responseBody="";e.messages=[];e.status=b;z()}function y(b,a,c){if(a.trackMessageLength){0!=c.partialMessage.length&&(b=c.partialMessage+b);for(var e=[],d=0,h=b.indexOf(a.messageDelimiter);-1!=h;){d=jQuery.trim(b.substring(d,h));b=b.substring(h+ a.messageDelimiter.length,b.length);if(0==b.length||b.length<d)break;h=b.indexOf(a.messageDelimiter);e.push(b.substring(0,d))}c.partialMessage=0==e.length||-1!=h&&0!=b.length&&d!=b.length?d+a.messageDelimiter+b:"";if(0!=e.length)c.responseBody=e.join(a.messageDelimiter),c.messages=e;else return c.responseBody="",c.messages=[],!0}else c.responseBody=b;return!1}function q(b){jQuery.atmosphere.log(c.logLevel,[b]);if("undefined"!=typeof c.onTransportFailure)c.onTransportFailure(b,c);else if("undefined"!= typeof jQuery.atmosphere.onTransportFailure)jQuery.atmosphere.onTransportFailure(b,c);c.transport=c.fallbackTransport;b=-1==c.connectTimeout?0:c.connectTimeout;c.reconnect&&"none"!=c.transport||null==c.transport?(c.method=c.fallbackMethod,e.transport=c.fallbackTransport,c.fallbackTransport="none",c.id=setTimeout(function(){d()},b)):m(500,"Unable to reconnect with fallback transport")}function k(b){var a=c;null!=b&&"undefined"!=typeof b&&(a=b);var g=a.url;if(!a.attachHeadersAsQueryString||-1!=g.indexOf("X-Atmosphere-Framework"))return g; g+=-1!=g.indexOf("?")?"&":"?";g+="X-Atmosphere-tracking-id="+a.uuid;g+="&X-Atmosphere-Framework="+jQuery.atmosphere.version;g+="&X-Atmosphere-Transport="+a.transport;a.trackMessageLength&&(g+="&X-Atmosphere-TrackMessageSize=true");g=void 0!=a.lastTimestamp?g+("&X-Cache-Date="+a.lastTimestamp):g+"&X-Cache-Date=0";""!=a.contentType&&(g+="&Content-Type="+a.contentType);a.enableProtocol&&(g+="&X-atmo-protocol=true");jQuery.each(a.headers,function(c,d){var h=jQuery.isFunction(d)?d.call(this,a,b,e):d;null!= h&&(g+="&"+encodeURIComponent(c)+"="+encodeURIComponent(h))});return g}function I(b){var a=c;if(null!=b||"undefined"!=typeof b)a=b;a.lastIndex=0;a.readyState=0;if("jsonp"==a.transport||a.enableXDR&&jQuery.atmosphere.checkCORSSupport())J(a);else if("ajax"==a.transport){var g=c;null!=b&&"undefined"!=typeof b&&(g=b);b=g.url;var d=g.data;g.attachHeadersAsQueryString&&(b=k(g),""!=d&&(b+="&X-Atmosphere-Post-Body="+encodeURIComponent(d)),d="");u=jQuery.ajax({url:b,type:g.method,error:function(a,b,c){e.error= !0;300>a.status?v(u,g):m(a.status,c)},success:function(a){g.reconnect&&(-1==g.maxRequest||g.requestCount++<g.maxRequest?(g.executeCallbackBeforeReconnect||v(u,g),D(g,a)&&s(a,"messageReceived",200,g.transport),g.executeCallbackBeforeReconnect&&v(u,g)):(jQuery.atmosphere.log(c.logLevel,["AJAX reconnect maximum try reached "+c.requestCount]),m(0,"maxRequest reached")))},beforeSend:function(a){S(a,g,!1)},crossDomain:g.enableXDR,async:"undefined"!=typeof g.async?g.async:!0})}else{if(jQuery.browser.msie&& 10>jQuery.browser.version){if("streaming"==a.transport){a.enableXDR&&window.XDomainRequest?Y(a):(r=T(a),r.open());return}if(a.enableXDR&&window.XDomainRequest){Y(a);return}}var f=function(){a.reconnect&&A++<a.maxReconnectOnClose?v(h,a,!0):m(0,"maxReconnectOnClose reached")};if(a.reconnect&&(-1==a.maxRequest||a.requestCount++<a.maxRequest)){var h;jQuery.browser.msie&&"undefined"==typeof XMLHttpRequest&&(XMLHttpRequest=function(){try{return new ActiveXObject("Msxml2.XMLHTTP.6.0")}catch(a){}try{return new ActiveXObject("Msxml2.XMLHTTP.3.0")}catch(b){}try{return new ActiveXObject("Microsoft.XMLHTTP")}catch(c){}throw Error("This browser does not support XMLHttpRequest."); });h=new XMLHttpRequest;S(h,a,!0);a.suspend&&(H=h);"polling"!=a.transport&&(e.transport=a.transport);h.onabort=function(){O(!0)};h.onerror=function(){e.error=!0;try{e.status=XMLHttpRequest.status}catch(a){e.status=500}e.status||(e.status=500);l();e.errorHandled||f()};h.onreadystatechange=function(){if(!F){e.error=null;var b=!1,g=!1;if(jQuery.browser.opera&&"streaming"==a.transport&&2<a.readyState&&4==h.readyState)a.readyState=0,a.lastIndex=0,f();else if(a.readyState=h.readyState,"streaming"==a.transport&& 3<=h.readyState?g=!0:"long-polling"==a.transport&&4===h.readyState&&(g=!0),clearTimeout(a.id),g)if(g=0,0!=h.readyState&&(g=1E3<h.status?0:h.status),300<=g||0==g)e.errorHandled=!0,l(),f();else if(g=h.responseText,0==jQuery.trim(g.length)&&"long-polling"==a.transport)h.hasData?h.hasData=!1:f();else{h.hasData=!0;R(h,c);if("streaming"==a.transport)if(jQuery.browser.opera)jQuery.atmosphere.iterate(function(){if(500!=e.status&&h.responseText.length>a.lastIndex){try{e.status=h.status}catch(g){e.status=404}e.state= "messageReceived";var d=h.responseText.substring(a.lastIndex);a.lastIndex=h.responseText.length;D(c,d)&&((b=y(d,a,e))||z(),Z(h,a))}else if(400<e.status)return a.lastIndex=h.responseText.length,!1},0);else{var d=g.substring(a.lastIndex,g.length);a.lastIndex=g.length;if(!D(c,d))return;b=y(d,a,e)}else{if(!D(c,g)){v(h,a,!1);return}b=y(g,a,e);a.lastIndex=g.length}try{e.status=h.status;for(var g=e,j=h.getAllResponseHeaders(),k,d=/^(.*?):[ \t]*([^\r\n]*)\r?$/mg,K={};k=d.exec(j);)K[k[1]]=k[2];g.headers=K; R(h,a)}catch(m){e.status=404}e.state=a.suspend?0==e.status?"closed":"messageReceived":"messagePublished";a.executeCallbackBeforeReconnect||v(h,a,!1);-1!=e.responseBody.indexOf("parent.callback")&&jQuery.atmosphere.log(a.logLevel,["parent.callback no longer supported with 0.8 version and up. Please upgrade"]);b||z();a.executeCallbackBeforeReconnect&&v(h,a,!1);Z(h,a)}}};h.send(a.data);a.suspend&&(a.id=setTimeout(function(){L&&setTimeout(function(){l();I(a)},a.reconnectInterval)},a.timeout));L=!0}else"debug"== a.logLevel&&jQuery.atmosphere.log(a.logLevel,["Max re-connection reached."]),m(0,"maxRequest reached")}}function S(b,a,g){var d=k(a),d=jQuery.atmosphere.prepareURL(d);g&&(b.open(a.method,d,!0),-1<a.connectTimeout&&(a.id=setTimeout(function(){0==a.requestCount&&(l(),s("Connect timeout","closed",200,a.transport))},a.connectTimeout)));c.withCredentials&&"withCredentials"in b&&(b.withCredentials=!0);c.dropAtmosphereHeaders||(b.setRequestHeader("X-Atmosphere-Framework",jQuery.atmosphere.version),b.setRequestHeader("X-Atmosphere-Transport", a.transport),void 0!=a.lastTimestamp?b.setRequestHeader("X-Cache-Date",a.lastTimestamp):b.setRequestHeader("X-Cache-Date",0),a.trackMessageLength&&b.setRequestHeader("X-Atmosphere-TrackMessageSize","true"),b.setRequestHeader("X-Atmosphere-tracking-id",a.uuid));""!=a.contentType&&b.setRequestHeader("Content-Type",a.contentType);jQuery.each(a.headers,function(c,d){var f=jQuery.isFunction(d)?d.call(this,b,a,g,e):d;null!=f&&b.setRequestHeader(c,f)})}function v(b,a,c){if(c||"streaming"!=a.transport)if(a.reconnect|| a.suspend&&L){var d=0;0!=b.readyState&&(d=1E3<b.status?0:b.status);e.status=0==d?204:d;e.reason=0==d?"Server resumed the connection or down.":"OK";b=-1==a.connectTimeout?0:a.connectTimeout;c?I(a):a.id=setTimeout(function(){I(a)},b)}}function Y(b){"polling"!=b.transport?(r=$(b),r.open()):$(b).open()}function $(b){var a=c;null!=b&&"undefined"!=typeof b&&(a=b);var e=a.transport,d=function(a){a=a.responseText;D(b,a)&&s(a,"messageReceived",200,e)},f=new window.XDomainRequest,h=a.rewriteURL||function(a){var b= /(?:^|;\s*)(JSESSIONID|PHPSESSID)=([^;]*)/.exec(document.cookie);switch(b&&b[1]){case "JSESSIONID":return a.replace(/;jsessionid=[^\?]*|(\?)|$/,";jsessionid="+b[2]+"$1");case "PHPSESSID":return a.replace(/\?PHPSESSID=[^&]*&?|\?|$/,"?PHPSESSID="+b[2]+"&").replace(/&$/,"")}return a};f.onprogress=function(){j(f)};f.onerror=function(){"polling"!=a.transport&&v(f,a,!1)};f.onload=function(){j(f)};var j=function(b){if(a.lastMessage!=b.responseText){a.executeCallbackBeforeReconnect&&d(b);if("long-polling"== a.transport&&a.reconnect&&(-1==a.maxRequest||a.requestCount++<a.maxRequest))b.status=200,v(b,a,!1);a.executeCallbackBeforeReconnect||d(b);a.lastMessage=b.responseText}};return{open:function(){"POST"==a.method&&(a.attachHeadersAsQueryString=!0);var b=k(a);"POST"==a.method&&(b+="&X-Atmosphere-Post-Body="+encodeURIComponent(a.data));f.open(a.method,h(b));f.send();-1<a.connectTimeout&&(a.id=setTimeout(function(){0==a.requestCount&&(l(),s("Connect timeout","closed",200,a.transport))},a.connectTimeout))}, close:function(){f.abort();s(f.responseText,"closed",200,e)}}}function T(b){var a=c;null!=b&&"undefined"!=typeof b&&(a=b);var d,f=new window.ActiveXObject("htmlfile");f.open();f.close();var j=a.url;"polling"!=a.transport&&(e.transport=a.transport);return{open:function(){var b=f.createElement("iframe");j=k(a);""!=a.data&&(j+="&X-Atmosphere-Post-Body="+encodeURIComponent(a.data));j=jQuery.atmosphere.prepareURL(j);b.src=j;f.body.appendChild(b);var c=b.contentDocument||b.contentWindow.document;d=jQuery.atmosphere.iterate(function(){try{if(c.firstChild){if("complete"=== c.readyState)try{jQuery.noop(c.fileSize)}catch(b){return s("Connection Failure","error",500,a.transport),!1}var h=c.body?c.body.lastChild:c,j=function(){var a=h.cloneNode(!0);a.appendChild(c.createTextNode("."));a=a.innerText;return a=a.substring(0,a.length-1)};if(!jQuery.nodeName(h,"pre")){var k=c.head||c.getElementsByTagName("head")[0]||c.documentElement||c,l=c.createElement("script");l.text="document.write('<plaintext>')";k.insertBefore(l,k.firstChild);k.removeChild(l);h=c.body.lastChild}s(j(), "opening",200,a.transport);d=jQuery.atmosphere.iterate(function(){var b=j();b.length>a.lastIndex&&(e.status=200,e.error=null,0!=b.length&&D(a,b)&&(h.innerText="",s(b,"messageReceived",200,a.transport)),a.lastIndex=0);if("complete"===c.readyState)return s("","closed",200,a.transport),s("","re-opening",200,a.transport),a.id=setTimeout(function(){r=T(a);r.open()},a.reconnectInterval),!1},null);return!1}}catch(p){return e.error=!0,A++<a.maxReconnectOnClose?a.id=setTimeout(function(){r=T(a);r.open()}, a.reconnectInterval):m(0,"maxReconnectOnClose reached"),f.execCommand("Stop"),f.close(),!1}})},close:function(){d&&d();f.execCommand("Stop");s("","closed",200,a.transport)}}}function X(b){if(408==e.status)b=U(b),b.transport="ajax",b.method="GET",b.async=!1,b.reconnect=!1,I(b);else if(null!=w)w.send(b);else if(null!=H||null!=x)P(b);else if(null!=r)c.enableXDR&&jQuery.atmosphere.checkCORSSupport()?(b=U(b),b.reconnect=!1,J(b)):P(b);else if(null!=u)P(b);else if(null!=n){var a=aa(b),d;try{d=null!=c.webSocketUrl? c.webSocketPathDelimiter+c.webSocketUrl+c.webSocketPathDelimiter+a:a,n.send(d)}catch(f){n.onclose=function(){},l(),q("Websocket failed. Downgrading to Comet and resending "+d),P(b)}}}function P(b){b=U(b);I(b)}function aa(b){var a=b;"object"==typeof a&&(a=b.data);return a}function U(b){var a=aa(b),a={connected:!1,timeout:6E4,method:"POST",url:c.url,contentType:c.contentType,headers:{},reconnect:!0,callback:null,data:a,suspend:!1,maxRequest:-1,logLevel:"info",requestCount:0,withCredentials:c.withCredentials, transport:"polling",attachHeadersAsQueryString:!0,enableXDR:c.enableXDR,uuid:c.uuid,messageDelimiter:"|",enableProtocol:!1,maxReconnectOnClose:c.maxReconnectOnClose};"object"==typeof b&&(a=jQuery.extend(a,b));return a}function V(b){b=jQuery.parseJSON(b);if(b.id!=E)if("undefined"!=typeof c.onLocalMessage)c.onLocalMessage(b.event);else if("undefined"!=typeof jQuery.atmosphere.onLocalMessage)jQuery.atmosphere.onLocalMessage(b.event)}function s(b,a,d,f){if("messageReceived"==a){if(y(b,c,e))return}else e.responseBody= b;e.transport=f;e.status=d;e.state=a;z()}function R(b,a){if(!a.readResponsesHeaders&&!a.enableProtocol)a.lastTimestamp=jQuery.now(),a.uuid=jQuery.atmosphere.guid();else try{var d=b.getResponseHeader("X-Cache-Date");d&&(null!=d&&0<d.length)&&(a.lastTimestamp=d.split(" ").pop());var f=b.getResponseHeader("X-Atmosphere-tracking-id");f&&null!=f&&(a.uuid=f.split(" ").pop());a.headers&&jQuery.each(c.headers,function(a){var c=b.getResponseHeader(a);c&&(e.headers[a]=c)})}catch(j){}}function ba(b,a){switch(b.state){case "messageReceived":A= 0;if("undefined"!=typeof a.onMessage)a.onMessage(b);break;case "error":if("undefined"!=typeof a.onError)a.onError(b);break;case "opening":if("undefined"!=typeof a.onOpen)a.onOpen(b);break;case "messagePublished":if("undefined"!=typeof a.onMessagePublished)a.onMessagePublished(b);break;case "re-opening":if("undefined"!=typeof a.onReconnect)a.onReconnect(c,b);break;case "unsubscribe":case "closed":var d="undefined"!=typeof c.closed?c.closed:!1;if("undefined"!=typeof a.onClose&&!d)a.onClose(b);c.closed= !0}}function O(b){e.state="closed";e.responseBody="";e.messages=[];e.status=!b?501:200;z()}function z(){var b=function(a,b){b(e)};null==w&&null!=Q&&Q(e.responseBody);c.reconnect=c.mrequest;for(var a="string"==typeof e.responseBody&&c.trackMessageLength?0<e.messages.length?e.messages:[""]:Array(e.responseBody),d=0;d<a.length;d++)if(!(1<a.length&&0==a[d].length)&&(e.responseBody=jQuery.trim(a[d]),!(0==e.responseBody.length&&"messageReceived"==e.state))){var f=e;ba(f,c);ba(f,jQuery.atmosphere);if(0< jQuery.atmosphere.callbacks.length){"debug"==c.logLevel&&jQuery.atmosphere.debug("Invoking "+jQuery.atmosphere.callbacks.length+" global callbacks: "+e.state);try{jQuery.each(jQuery.atmosphere.callbacks,b)}catch(j){jQuery.atmosphere.log(c.logLevel,["Callback exception"+j])}}if("function"==typeof c.callback){"debug"==c.logLevel&&jQuery.atmosphere.debug("Invoking request callbacks");try{c.callback(e)}catch(h){jQuery.atmosphere.log(c.logLevel,["Callback exception"+h])}}}}function Z(b,a){""==e.partialMessage&& ("streaming"==a.transport&&b.responseText.length>a.maxStreamingLength)&&(e.messages=[],O(!0),ca(),l(),v(b,a,!0))}function ca(){if(c.enableProtocol){var b="X-Atmosphere-Transport=close&X-Atmosphere-tracking-id="+c.uuid,a=c.url.replace(/([?&])_=[^&]*/,b),a=a+(a===c.url?(/\?/.test(c.url)?"&":"?")+b:"");-1<c.connectTimeout?jQuery.ajax({url:a,async:!1,timeout:c.connectTimeout}):jQuery.ajax({url:a,async:!1})}}function M(){c.reconnect=!1;F=!0;e.request=c;e.state="unsubscribe";e.responseBody="";e.messages= [];e.status=408;z();l()}function l(){null!=r&&(r.close(),r=null);null!=u&&(u.abort(),u=null);null!=H&&(H.abort(),H=null);null!=n&&(n.webSocketOpened&&n.close(),n=null);null!=x&&(x.close(),x=null);null!=C&&(clearInterval(N),document.cookie=encodeURIComponent("atmosphere-"+c.url)+"=; expires=Thu, 01 Jan 1970 00:00:00 GMT",C.signal("close",{reason:"",heir:!F?E:(C.get("children")||[])[0]}),C.close());null!=w&&w.close()}var c={timeout:3E5,method:"GET",headers:{},contentType:"",callback:null,url:"",data:"", suspend:!0,maxRequest:-1,reconnect:!0,maxStreamingLength:1E7,lastIndex:0,logLevel:"info",requestCount:0,fallbackMethod:"GET",fallbackTransport:"streaming",transport:"long-polling",webSocketImpl:null,webSocketUrl:null,webSocketPathDelimiter:"@@",enableXDR:!1,rewriteURL:!1,attachHeadersAsQueryString:!0,executeCallbackBeforeReconnect:!1,readyState:0,lastTimestamp:0,withCredentials:!1,trackMessageLength:!1,messageDelimiter:"|",connectTimeout:-1,reconnectInterval:0,dropAtmosphereHeaders:!0,uuid:0,shared:!1, readResponsesHeaders:!0,maxReconnectOnClose:5,enableProtocol:!1,onError:function(){},onClose:function(){},onOpen:function(){},onMessage:function(){},onReconnect:function(){},onMessagePublished:function(){},onTransportFailure:function(){},onLocalMessage:function(){}},e={status:200,reasonPhrase:"OK",responseBody:"",messages:[],headers:[],state:"messageReceived",transport:"polling",error:null,request:null,partialMessage:"",errorHandled:!1,id:0},n=null,x=null,H=null,r=null,u=null,L=!0,A=0,F=!1,Q=null, C,w=null,E=jQuery.now(),N;j(f);this.subscribe=function(b){j(b);d()};this.execute=function(){d()};this.invokeCallback=function(){z()};this.close=function(){M()};this.disconnect=function(){ca()};this.getUrl=function(){return c.url};this.getUUID=function(){return c.uuid};this.push=function(b){X(b)};this.pushLocal=function(b){if(0!=b.length)try{w?w.localSend(b):C&&C.signal("localMessage",jQuery.stringifyJSON({id:E,event:b}))}catch(a){jQuery.atmosphere.error(a)}};this.enableProtocol=function(){return c.enableProtocol}; this.response=e},subscribe:function(f,j,d){"function"==typeof j&&jQuery.atmosphere.addCallback(j);"string"!=typeof f?d=f:d.url=f;f=new jQuery.atmosphere.AtmosphereRequest(d);f.execute();return jQuery.atmosphere.requests[jQuery.atmosphere.requests.length]=f},addCallback:function(f){-1==jQuery.inArray(f,jQuery.atmosphere.callbacks)&&jQuery.atmosphere.callbacks.push(f)},removeCallback:function(f){f=jQuery.inArray(f,jQuery.atmosphere.callbacks);-1!=f&&jQuery.atmosphere.callbacks.splice(f,1)},unsubscribe:function(){if(0< jQuery.atmosphere.requests.length)for(var f=[].concat(jQuery.atmosphere.requests),j=0;j<f.length;j++){var d=f[j];d.disconnect();d.close();clearTimeout(d.response.request.id)}jQuery.atmosphere.requests=[];jQuery.atmosphere.callbacks=[]},unsubscribeUrl:function(f){var j=-1;if(0<jQuery.atmosphere.requests.length)for(var d=0;d<jQuery.atmosphere.requests.length;d++){var p=jQuery.atmosphere.requests[d];if(p.getUrl()==f){p.disconnect();p.close();clearTimeout(p.response.request.id);j=d;break}}0<=j&&jQuery.atmosphere.requests.splice(j, 1)},publish:function(f){"function"==typeof f.callback&&jQuery.atmosphere.addCallback(callback);f.transport="polling";f=new jQuery.atmosphere.AtmosphereRequest(f);return jQuery.atmosphere.requests[jQuery.atmosphere.requests.length]=f},checkCORSSupport:function(){return jQuery.browser.msie&&!window.XDomainRequest||jQuery.browser.opera&&12>jQuery.browser.version?!0:-1<navigator.userAgent.toLowerCase().indexOf("android")?!0:!1},S4:function(){return(65536*(1+Math.random())|0).toString(16).substring(1)}, guid:function(){return jQuery.atmosphere.S4()+jQuery.atmosphere.S4()+"-"+jQuery.atmosphere.S4()+"-"+jQuery.atmosphere.S4()+"-"+jQuery.atmosphere.S4()+"-"+jQuery.atmosphere.S4()+jQuery.atmosphere.S4()+jQuery.atmosphere.S4()},prepareURL:function(f){var j=jQuery.now(),d=f.replace(/([?&])_=[^&]*/,"$1_="+j);return d+(d===f?(/\?/.test(f)?"&":"?")+"_="+j:"")},param:function(f){return jQuery.param(f,jQuery.ajaxSettings.traditional)},supportStorage:function(){var f=window.localStorage;if(f)try{return f.setItem("t", "t"),f.removeItem("t"),window.StorageEvent&&!jQuery.browser.msie&&!(jQuery.browser.mozilla&&"1"===jQuery.browser.version.split(".")[0])}catch(j){}return!1},iterate:function(f,j){var d;j=j||0;(function J(){d=setTimeout(function(){!1!==f()&&J()},j)})();return function(){clearTimeout(d)}},log:function(f,j){if(window.console){var d=window.console[f];"function"==typeof d&&d.apply(window.console,j)}},warn:function(){jQuery.atmosphere.log("warn",arguments)},info:function(){jQuery.atmosphere.log("info",arguments)}, debug:function(){jQuery.atmosphere.log("debug",arguments)},error:function(){jQuery.atmosphere.log("error",arguments)}}}(); (function(){var f,j;jQuery.uaMatch=function(d){d=d.toLowerCase();d=/(chrome)[ \/]([\w.]+)/.exec(d)||/(webkit)[ \/]([\w.]+)/.exec(d)||/(opera)(?:.*version|)[ \/]([\w.]+)/.exec(d)||/(msie) ([\w.]+)/.exec(d)||0>d.indexOf("compatible")&&/(mozilla)(?:.*? rv:([\w.]+)|)/.exec(d)||[];return{browser:d[1]||"",version:d[2]||"0"}};f=jQuery.uaMatch(navigator.userAgent);j={};f.browser&&(j[f.browser]=!0,j.version=f.version);j.chrome?j.webkit=!0:j.webkit&&(j.safari=!0);jQuery.browser=j;jQuery.sub=function(){function d(f, j){return new d.fn.init(f,j)}jQuery.extend(!0,d,this);d.superclass=this;d.fn=d.prototype=this();d.fn.constructor=d;d.sub=this.sub;d.fn.init=function(j,t){t&&(t instanceof jQuery&&!(t instanceof d))&&(t=d(t));return jQuery.fn.init.call(this,j,t,f)};d.fn.init.prototype=d.fn;var f=d(document);return d}})(); (function(f){function j(d){return'"'+d.replace(J,function(d){var f=t[d];return"string"===typeof f?f:"\\u"+("0000"+d.charCodeAt(0).toString(16)).slice(-4)})+'"'}function d(d){return 10>d?"0"+d:d}function p(f,t){var m,y,q,k=t[f];q=typeof k;k&&("object"===typeof k&&"function"===typeof k.toJSON)&&(k=k.toJSON(f),q=typeof k);switch(q){case "string":return j(k);case "number":return isFinite(k)?String(k):"null";case "boolean":return String(k);case "object":if(!k)return"null";switch(Object.prototype.toString.call(k)){case "[object Date]":return isFinite(k.valueOf())? '"'+k.getUTCFullYear()+"-"+d(k.getUTCMonth()+1)+"-"+d(k.getUTCDate())+"T"+d(k.getUTCHours())+":"+d(k.getUTCMinutes())+":"+d(k.getUTCSeconds())+'Z"':"null";case "[object Array]":y=k.length;q=[];for(m=0;m<y;m++)q.push(p(m,k)||"null");return"["+q.join(",")+"]";default:q=[];for(m in k)Object.prototype.hasOwnProperty.call(k,m)&&(y=p(m,k))&&q.push(j(m)+":"+y);return"{"+q.join(",")+"}"}}}var J=/[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g, t={"\b":"\\b","\t":"\\t","\n":"\\n","\f":"\\f","\r":"\\r",'"':'\\"',"\\":"\\\\"};f.stringifyJSON=function(d){return window.JSON&&window.JSON.stringify?window.JSON.stringify(d):p("",{"":d})}})(jQuery);


/**
 * PrimeFaces Socket Widget
 */
PrimeFaces.widget.Socket=PrimeFaces.widget.BaseWidget.extend({init:function(a){this.cfg=a;var b=this;this.cfg.request={url:this.cfg.url,transport:"websocket",fallbackTransport:"long-polling",enableXDR:!1,onMessage:function(a){b.onMessage(a)}};this.cfg.autoConnect&&this.connect()},connect:function(a){a&&(this.cfg.request.url+=a);this.connection=$.atmosphere.subscribe(this.cfg.request)},push:function(a){this.connection.push(JSON.stringify(a))},disconnect:function(){this.connection.close()},onMessage:function(a){a= $.parseJSON(a.responseBody);this.cfg.onMessage&&this.cfg.onMessage.call(this,a.data);this.cfg.behaviors&&this.cfg.behaviors.message&&this.cfg.behaviors.message.call(this)}});