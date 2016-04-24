(function(a,b){if(typeof define==="function"&&define.amd){define(b)
}else{a.atmosphere=b()
}}(this,function(){var c="2.1.5-javascript",a={},d,g=[],f=[],e=0,b=Object.prototype.hasOwnProperty;
a={onError:function(h){},onClose:function(h){},onOpen:function(h){},onReopen:function(h){},onMessage:function(h){},onReconnect:function(i,h){},onMessagePublished:function(h){},onTransportFailure:function(i,h){},onLocalMessage:function(h){},onFailureToReconnect:function(i,h){},onClientTimeout:function(h){},AtmosphereRequest:function(M){var O={timeout:300000,method:"GET",headers:{},contentType:"",callback:null,url:"",data:"",suspend:true,maxRequest:-1,reconnect:true,maxStreamingLength:10000000,lastIndex:0,logLevel:"info",requestCount:0,fallbackMethod:"GET",fallbackTransport:"streaming",transport:"long-polling",webSocketImpl:null,webSocketBinaryType:null,dispatchUrl:null,webSocketPathDelimiter:"@@",enableXDR:false,rewriteURL:false,attachHeadersAsQueryString:true,executeCallbackBeforeReconnect:false,readyState:0,lastTimestamp:0,withCredentials:false,trackMessageLength:false,messageDelimiter:"|",connectTimeout:-1,reconnectInterval:0,dropHeaders:true,uuid:0,async:true,shared:false,readResponsesHeaders:false,maxReconnectOnClose:5,enableProtocol:true,pollingInterval:0,onError:function(aA){},onClose:function(aA){},onOpen:function(aA){},onMessage:function(aA){},onReopen:function(aB,aA){},onReconnect:function(aB,aA){},onMessagePublished:function(aA){},onTransportFailure:function(aB,aA){},onLocalMessage:function(aA){},onFailureToReconnect:function(aB,aA){},onClientTimeout:function(aA){}};
var W={status:200,reasonPhrase:"OK",responseBody:"",messages:[],headers:[],state:"messageReceived",transport:"polling",error:null,request:null,partialMessage:"",errorHandled:false,closedByClientTimeout:false};
var Z=null;
var o=null;
var v=null;
var E=null;
var G=null;
var ak=true;
var l=0;
var aw=false;
var aa=null;
var aq;
var q=null;
var J=a.util.now();
var K;
var az;
ay(M);
function ar(){ak=true;
aw=false;
l=0;
Z=null;
o=null;
v=null;
E=null
}function A(){am();
ar()
}function L(aB,aA){if(W.partialMessage===""&&(aA.transport==="streaming")&&(aB.responseText.length>aA.maxStreamingLength)){W.messages=[];
aA.reconnectingOnLength=true;
aA.isReopen=true;
ai(true);
D();
am();
R(aB,aA,aA.pollingInterval)
}}function D(){if(O.enableProtocol&&!O.firstMessage){var aC="X-Atmosphere-Transport=close&X-Atmosphere-tracking-id="+O.uuid;
a.util.each(O.headers,function(aE,aG){var aF=a.util.isFunction(aG)?aG.call(this,O,O,W):aG;
if(aF!=null){aC+="&"+encodeURIComponent(aE)+"="+encodeURIComponent(aF)
}});
var aA=O.url.replace(/([?&])_=[^&]*/,aC);
aA=aA+(aA===O.url?(/\?/.test(O.url)?"&":"?")+aC:"");
var aB={connected:false};
var aD=new a.AtmosphereRequest(aB);
aD.attachHeadersAsQueryString=false;
aD.dropHeaders=true;
aD.url=aA;
aD.contentType="text/plain";
aD.transport="polling";
aD.method="GET";
aD.data="";
aD.async=false;
n("",aD)
}}function an(){if(O.reconnectId){clearTimeout(O.reconnectId);
delete O.reconnectId
}O.reconnect=false;
aw=true;
W.request=O;
W.state="unsubscribe";
W.responseBody="";
W.status=408;
W.partialMessage="";
C();
D();
am()
}function am(){W.partialMessage="";
if(O.id){clearTimeout(O.id)
}if(E!=null){E.close();
E=null
}if(G!=null){G.abort();
G=null
}if(v!=null){v.abort();
v=null
}if(Z!=null){if(Z.canSendMessage){Z.close()
}Z=null
}if(o!=null){o.close();
o=null
}at()
}function at(){if(aq!=null){clearInterval(K);
document.cookie=az+"=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
aq.signal("close",{reason:"",heir:!aw?J:(aq.get("children")||[])[0]});
aq.close()
}if(q!=null){q.close()
}}function ay(aA){A();
O=a.util.extend(O,aA);
O.mrequest=O.reconnect;
if(!O.reconnect){O.reconnect=true
}}function p(){return O.webSocketImpl!=null||window.WebSocket||window.MozWebSocket
}function S(){return window.EventSource
}function t(){if(O.shared){q=ah(O);
if(q!=null){if(O.logLevel==="debug"){a.util.debug("Storage service available. All communication will be local")
}if(q.open(O)){return
}}if(O.logLevel==="debug"){a.util.debug("No Storage service available.")
}q=null
}O.firstMessage=e==0?true:false;
O.isOpen=false;
O.ctime=a.util.now();
if(O.uuid===0){O.uuid=e
}W.closedByClientTimeout=false;
if(O.transport!=="websocket"&&O.transport!=="sse"){s(O)
}else{if(O.transport==="websocket"){if(!p()){Q("Websocket is not supported, using request.fallbackTransport ("+O.fallbackTransport+")")
}else{aj(false)
}}else{if(O.transport==="sse"){if(!S()){Q("Server Side Events(SSE) is not supported, using request.fallbackTransport ("+O.fallbackTransport+")")
}else{I(false)
}}}}}function ah(aE){var aF,aD,aI,aA="atmosphere-"+aE.url,aB={storage:function(){function aJ(aN){if(aN.key===aA&&aN.newValue){aC(aN.newValue)
}}if(!a.util.storage){return
}var aM=window.localStorage,aK=function(aN){return a.util.parseJSON(aM.getItem(aA+"-"+aN))
},aL=function(aN,aO){aM.setItem(aA+"-"+aN,a.util.stringifyJSON(aO))
};
return{init:function(){aL("children",aK("children").concat([J]));
a.util.on(window,"storage",aJ);
return aK("opened")
},signal:function(aN,aO){aM.setItem(aA,a.util.stringifyJSON({target:"p",type:aN,data:aO}))
},close:function(){var aN=aK("children");
a.util.off(window,"storage",aJ);
if(aN){if(aG(aN,aE.id)){aL("children",aN)
}}}}
},windowref:function(){var aJ=window.open("",aA.replace(/\W/g,""));
if(!aJ||aJ.closed||!aJ.callbacks){return
}return{init:function(){aJ.callbacks.push(aC);
aJ.children.push(J);
return aJ.opened
},signal:function(aK,aL){if(!aJ.closed&&aJ.fire){aJ.fire(a.util.stringifyJSON({target:"p",type:aK,data:aL}))
}},close:function(){if(!aI){aG(aJ.callbacks,aC);
aG(aJ.children,J)
}}}
}};
function aG(aM,aL){var aJ,aK=aM.length;
for(aJ=0;
aJ<aK;
aJ++){if(aM[aJ]===aL){aM.splice(aJ,1)
}}return aK!==aM.length
}function aC(aJ){var aL=a.util.parseJSON(aJ),aK=aL.data;
if(aL.target==="c"){switch(aL.type){case"open":N("opening","local",O);
break;
case"close":if(!aI){aI=true;
if(aK.reason==="aborted"){an()
}else{if(aK.heir===J){t()
}else{setTimeout(function(){t()
},100)
}}}break;
case"message":F(aK,"messageReceived",200,aE.transport);
break;
case"localMessage":ac(aK);
break
}}}function aH(){var aJ=new RegExp("(?:^|; )("+encodeURIComponent(aA)+")=([^;]*)").exec(document.cookie);
if(aJ){return a.util.parseJSON(decodeURIComponent(aJ[2]))
}}aF=aH();
if(!aF||a.util.now()-aF.ts>1000){return
}aD=aB.storage()||aB.windowref();
if(!aD){return
}return{open:function(){var aJ;
K=setInterval(function(){var aK=aF;
aF=aH();
if(!aF||aK.ts===aF.ts){aC(a.util.stringifyJSON({target:"c",type:"close",data:{reason:"error",heir:aK.heir}}))
}},1000);
aJ=aD.init();
if(aJ){setTimeout(function(){N("opening","local",aE)
},50)
}return aJ
},send:function(aJ){aD.signal("send",aJ)
},localSend:function(aJ){aD.signal("localSend",a.util.stringifyJSON({id:J,event:aJ}))
},close:function(){if(!aw){clearInterval(K);
aD.signal("close");
aD.close()
}}}
}function ad(){var aB,aA="atmosphere-"+O.url,aF={storage:function(){function aG(aI){if(aI.key===aA&&aI.newValue){aC(aI.newValue)
}}if(!a.util.storage){return
}var aH=window.localStorage;
return{init:function(){a.util.on(window,"storage",aG)
},signal:function(aI,aJ){aH.setItem(aA,a.util.stringifyJSON({target:"c",type:aI,data:aJ}))
},get:function(aI){return a.util.parseJSON(aH.getItem(aA+"-"+aI))
},set:function(aI,aJ){aH.setItem(aA+"-"+aI,a.util.stringifyJSON(aJ))
},close:function(){a.util.off(window,"storage",aG);
aH.removeItem(aA);
aH.removeItem(aA+"-opened");
aH.removeItem(aA+"-children")
}}
},windowref:function(){var aH=aA.replace(/\W/g,""),aG=document.getElementById(aH),aI;
if(!aG){aG=document.createElement("div");
aG.id=aH;
aG.style.display="none";
aG.innerHTML='<iframe name="'+aH+'" />';
document.body.appendChild(aG)
}aI=aG.firstChild.contentWindow;
return{init:function(){aI.callbacks=[aC];
aI.fire=function(aJ){var aK;
for(aK=0;
aK<aI.callbacks.length;
aK++){aI.callbacks[aK](aJ)
}}
},signal:function(aJ,aK){if(!aI.closed&&aI.fire){aI.fire(a.util.stringifyJSON({target:"c",type:aJ,data:aK}))
}},get:function(aJ){return !aI.closed?aI[aJ]:null
},set:function(aJ,aK){if(!aI.closed){aI[aJ]=aK
}},close:function(){}}
}};
function aC(aG){var aI=a.util.parseJSON(aG),aH=aI.data;
if(aI.target==="p"){switch(aI.type){case"send":al(aH);
break;
case"localSend":ac(aH);
break;
case"close":an();
break
}}}aa=function aE(aG){aB.signal("message",aG)
};
function aD(){document.cookie=az+"="+encodeURIComponent(a.util.stringifyJSON({ts:a.util.now()+1,heir:(aB.get("children")||[])[0]}))+"; path=/"
}aB=aF.storage()||aF.windowref();
aB.init();
if(O.logLevel==="debug"){a.util.debug("Installed StorageService "+aB)
}aB.set("children",[]);
if(aB.get("opened")!=null&&!aB.get("opened")){aB.set("opened",false)
}az=encodeURIComponent(aA);
aD();
K=setInterval(aD,1000);
aq=aB
}function N(aC,aF,aB){if(O.shared&&aF!=="local"){ad()
}if(aq!=null){aq.set("opened",true)
}aB.close=function(){an()
};
if(l>0&&aC==="re-connecting"){aB.isReopen=true;
ae(W)
}else{if(W.error==null){W.request=aB;
var aD=W.state;
W.state=aC;
var aA=W.transport;
W.transport=aF;
var aE=W.responseBody;
C();
W.responseBody=aE;
W.state=aD;
W.transport=aA
}}}function z(aC){aC.transport="jsonp";
var aB=O,aA;
if((aC!=null)&&(typeof(aC)!=="undefined")){aB=aC
}G={open:function(){var aE="atmosphere"+(++J);
function aD(){var aF=aB.url;
if(aB.dispatchUrl!=null){aF+=aB.dispatchUrl
}var aH=aB.data;
if(aB.attachHeadersAsQueryString){aF=X(aB);
if(aH!==""){aF+="&X-Atmosphere-Post-Body="+encodeURIComponent(aH)
}aH=""
}var aG=document.head||document.getElementsByTagName("head")[0]||document.documentElement;
aA=document.createElement("script");
aA.src=aF+"&jsonpTransport="+aE;
aA.clean=function(){aA.clean=aA.onerror=aA.onload=aA.onreadystatechange=null;
if(aA.parentNode){aA.parentNode.removeChild(aA)
}};
aA.onload=aA.onreadystatechange=function(){if(!aA.readyState||/loaded|complete/.test(aA.readyState)){aA.clean()
}};
aA.onerror=function(){aA.clean();
aB.lastIndex=0;
if(aB.openId){clearTimeout(aB.openId)
}if(aB.reconnect&&l++<aB.maxReconnectOnClose){N("re-connecting",aB.transport,aB);
R(G,aB,aC.reconnectInterval);
aB.openId=setTimeout(function(){ao(aB)
},aB.reconnectInterval+1000)
}else{af(0,"maxReconnectOnClose reached")
}};
aG.insertBefore(aA,aG.firstChild)
}window[aE]=function(aH){if(aB.reconnect){if(aB.maxRequest===-1||aB.requestCount++<aB.maxRequest){if(!aB.executeCallbackBeforeReconnect){R(G,aB,aB.pollingInterval)
}if(aH!=null&&typeof aH!=="string"){try{aH=aH.message
}catch(aG){}}var aF=x(aH,aB,W);
if(!aF){F(W.responseBody,"messageReceived",200,aB.transport)
}if(aB.executeCallbackBeforeReconnect){R(G,aB,aB.pollingInterval)
}}else{a.util.log(O.logLevel,["JSONP reconnect maximum try reached "+O.requestCount]);
af(0,"maxRequest reached")
}}};
setTimeout(function(){aD()
},50)
},abort:function(){if(aA&&aA.clean){aA.clean()
}}};
G.open()
}function j(aA){if(O.webSocketImpl!=null){return O.webSocketImpl
}else{if(window.WebSocket){return new WebSocket(aA)
}else{return new MozWebSocket(aA)
}}}function k(){return X(O,a.util.getAbsoluteURL(O.url)).replace(/^http/,"ws")
}function ax(){var aA=X(O);
return aA
}function I(aB){W.transport="sse";
var aA=ax();
if(O.logLevel==="debug"){a.util.debug("Invoking executeSSE");
a.util.debug("Using URL: "+aA)
}if(O.enableProtocol&&aB){var aD=a.util.now()-O.ctime;
O.lastTimestamp=Number(O.stime)+Number(aD)
}if(aB&&!O.reconnect){if(o!=null){am()
}return
}try{o=new EventSource(aA,{withCredentials:O.withCredentials})
}catch(aC){af(0,aC);
Q("SSE failed. Downgrading to fallback transport and resending");
return
}if(O.connectTimeout>0){O.id=setTimeout(function(){if(!aB){am()
}},O.connectTimeout)
}o.onopen=function(aE){y(O);
if(O.logLevel==="debug"){a.util.debug("SSE successfully opened")
}if(!O.enableProtocol){if(!aB){N("opening","sse",O)
}else{N("re-opening","sse",O)
}}else{if(O.isReopen){O.isReopen=false;
N("re-opening",O.transport,O)
}}aB=true;
if(O.method==="POST"){W.state="messageReceived";
o.send(O.data)
}};
o.onmessage=function(aF){y(O);
if(!O.enableXDR&&aF.origin&&aF.origin!==window.location.protocol+"//"+window.location.host){a.util.log(O.logLevel,["Origin was not "+window.location.protocol+"//"+window.location.host]);
return
}W.state="messageReceived";
W.status=200;
aF=aF.data;
var aE=x(aF,O,W);
if(!aE){C();
W.responseBody="";
W.messages=[]
}};
o.onerror=function(aE){clearTimeout(O.id);
if(W.closedByClientTimeout){return
}ai(aB);
am();
if(aw){a.util.log(O.logLevel,["SSE closed normally"])
}else{if(!aB){Q("SSE failed. Downgrading to fallback transport and resending")
}else{if(O.reconnect&&(W.transport==="sse")){if(l++<O.maxReconnectOnClose){N("re-connecting",O.transport,O);
if(O.reconnectInterval>0){O.reconnectId=setTimeout(function(){I(true)
},O.reconnectInterval)
}else{I(true)
}W.responseBody="";
W.messages=[]
}else{a.util.log(O.logLevel,["SSE reconnect maximum try reached "+l]);
af(0,"maxReconnectOnClose reached")
}}}}}
}function aj(aB){W.transport="websocket";
if(O.enableProtocol&&aB){var aE=a.util.now()-O.ctime;
O.lastTimestamp=Number(O.stime)+Number(aE)
}var aA=k(O.url);
if(O.logLevel==="debug"){a.util.debug("Invoking executeWebSocket");
a.util.debug("Using URL: "+aA)
}if(aB&&!O.reconnect){if(Z!=null){am()
}return
}Z=j(aA);
if(O.webSocketBinaryType!=null){Z.binaryType=O.webSocketBinaryType
}if(O.connectTimeout>0){O.id=setTimeout(function(){if(!aB){var aF={code:1002,reason:"",wasClean:false};
Z.onclose(aF);
try{am()
}catch(aG){}return
}},O.connectTimeout)
}Z.onopen=function(aG){y(O);
if(O.logLevel==="debug"){a.util.debug("Websocket successfully opened")
}var aF=aB;
if(Z!=null){Z.canSendMessage=true
}if(!O.enableProtocol){aB=true;
if(aF){N("re-opening","websocket",O)
}else{N("opening","websocket",O)
}}if(Z!=null){if(O.method==="POST"){W.state="messageReceived";
Z.send(O.data)
}}};
Z.onmessage=function(aH){y(O);
if(O.enableProtocol){aB=true
}W.state="messageReceived";
W.status=200;
aH=aH.data;
var aF=typeof(aH)==="string";
if(aF){var aG=x(aH,O,W);
if(!aG){C();
W.responseBody="";
W.messages=[]
}}else{if(!u(O,aH)){return
}W.responseBody=aH;
C();
W.responseBody=null
}};
Z.onerror=function(aF){clearTimeout(O.id)
};
Z.onclose=function(aF){clearTimeout(O.id);
if(W.state==="closed"){return
}var aG=aF.reason;
if(aG===""){switch(aF.code){case 1000:aG="Normal closure; the connection successfully completed whatever purpose for which it was created.";
break;
case 1001:aG="The endpoint is going away, either because of a server failure or because the browser is navigating away from the page that opened the connection.";
break;
case 1002:aG="The endpoint is terminating the connection due to a protocol error.";
break;
case 1003:aG="The connection is being terminated because the endpoint received data of a type it cannot accept (for example, a text-only endpoint received binary data).";
break;
case 1004:aG="The endpoint is terminating the connection because a data frame was received that is too large.";
break;
case 1005:aG="Unknown: no status code was provided even though one was expected.";
break;
case 1006:aG="Connection was closed abnormally (that is, with no close frame being sent).";
break
}}if(O.logLevel==="warn"){a.util.warn("Websocket closed, reason: "+aG);
a.util.warn("Websocket closed, wasClean: "+aF.wasClean)
}if(W.closedByClientTimeout){return
}ai(aB);
W.state="closed";
if(aw){a.util.log(O.logLevel,["Websocket closed normally"])
}else{if(!aB){Q("Websocket failed. Downgrading to Comet and resending")
}else{if(O.reconnect&&W.transport==="websocket"){am();
if(l++<O.maxReconnectOnClose){N("re-connecting",O.transport,O);
if(O.reconnectInterval>0){O.reconnectId=setTimeout(function(){W.responseBody="";
W.messages=[];
aj(true)
},O.reconnectInterval)
}else{W.responseBody="";
W.messages=[];
aj(true)
}}else{a.util.log(O.logLevel,["Websocket reconnect maximum try reached "+O.requestCount]);
if(O.logLevel==="warn"){a.util.warn("Websocket error, reason: "+aF.reason)
}af(0,"maxReconnectOnClose reached")
}}}}};
var aC=navigator.userAgent.toLowerCase();
var aD=aC.indexOf("android")>-1;
if(aD&&Z.url===undefined){Z.onclose({reason:"Android 4.1 does not support websockets.",wasClean:false})
}}function u(aD,aC){var aA=true;
if(aD.transport==="polling"){return aA
}if(a.util.trim(aC).length!==0&&aD.enableProtocol&&aD.firstMessage){aD.firstMessage=false;
var aB=aC.split(aD.messageDelimiter);
var aE=aB.length===2?0:1;
aD.uuid=a.util.trim(aB[aE]);
aD.stime=a.util.trim(aB[aE+1]);
aA=false;
if(aD.transport!=="long-polling"){ao(aD)
}e=aD.uuid
}else{if(aD.enableProtocol&&aD.firstMessage){aA=false
}else{ao(aD)
}}return aA
}function y(aA){clearTimeout(aA.id);
if(aA.timeout>0&&aA.transport!=="polling"){aA.id=setTimeout(function(){r(aA);
D();
am()
},aA.timeout)
}}function r(aA){W.closedByClientTimeout=true;
W.state="closedByClient";
W.responseBody="";
W.status=408;
W.messages=[];
C()
}function af(aA,aB){am();
clearTimeout(O.id);
W.state="error";
W.reasonPhrase=aB;
W.responseBody="";
W.status=aA;
W.messages=[];
C()
}function x(aE,aD,aA){if(!u(aD,aE)){return true
}if(aE.length===0){return true
}if(aD.trackMessageLength){aE=aA.partialMessage+aE;
var aC=[];
var aB=aE.indexOf(aD.messageDelimiter);
while(aB!==-1){var aG=a.util.trim(aE.substring(0,aB));
var aF=+aG;
if(isNaN(aF)){throw new Error('message length "'+aG+'" is not a number')
}aB+=aD.messageDelimiter.length;
if(aB+aF>aE.length){aB=-1
}else{aC.push(aE.substring(aB,aB+aF));
aE=aE.substring(aB+aF,aE.length);
aB=aE.indexOf(aD.messageDelimiter)
}}aA.partialMessage=aE;
if(aC.length!==0){aA.responseBody=aC.join(aD.messageDelimiter);
aA.messages=aC;
return false
}else{aA.responseBody="";
aA.messages=[];
return true
}}else{aA.responseBody=aE
}return false
}function Q(aA){a.util.log(O.logLevel,[aA]);
if(typeof(O.onTransportFailure)!=="undefined"){O.onTransportFailure(aA,O)
}else{if(typeof(a.util.onTransportFailure)!=="undefined"){a.util.onTransportFailure(aA,O)
}}O.transport=O.fallbackTransport;
var aB=O.connectTimeout===-1?0:O.connectTimeout;
if(O.reconnect&&O.transport!=="none"||O.transport==null){O.method=O.fallbackMethod;
W.transport=O.fallbackTransport;
O.fallbackTransport="none";
if(aB>0){O.reconnectId=setTimeout(function(){t()
},aB)
}else{t()
}}else{af(500,"Unable to reconnect with fallback transport")
}}function X(aC,aA){var aB=O;
if((aC!=null)&&(typeof(aC)!=="undefined")){aB=aC
}if(aA==null){aA=aB.url
}if(!aB.attachHeadersAsQueryString){return aA
}if(aA.indexOf("X-Atmosphere-Framework")!==-1){return aA
}aA+=(aA.indexOf("?")!==-1)?"&":"?";
aA+="X-Atmosphere-tracking-id="+aB.uuid;
aA+="&X-Atmosphere-Framework="+c;
aA+="&X-Atmosphere-Transport="+aB.transport;
if(aB.trackMessageLength){aA+="&X-Atmosphere-TrackMessageSize=true"
}if(aB.lastTimestamp!=null){aA+="&X-Cache-Date="+aB.lastTimestamp
}else{aA+="&X-Cache-Date="+0
}if(aB.contentType!==""){aA+="&Content-Type="+(aB.transport==="websocket"?aB.contentType:encodeURIComponent(aB.contentType))
}if(aB.enableProtocol){aA+="&X-atmo-protocol=true"
}a.util.each(aB.headers,function(aD,aF){var aE=a.util.isFunction(aF)?aF.call(this,aB,aC,W):aF;
if(aE!=null){aA+="&"+encodeURIComponent(aD)+"="+encodeURIComponent(aE)
}});
return aA
}function ao(aA){if(!aA.isOpen){aA.isOpen=true;
N("opening",aA.transport,aA)
}else{if(aA.isReopen){aA.isReopen=false;
N("re-opening",aA.transport,aA)
}}}function s(aC){var aA=O;
if((aC!=null)||(typeof(aC)!=="undefined")){aA=aC
}aA.lastIndex=0;
aA.readyState=0;
if((aA.transport==="jsonp")||((aA.enableXDR)&&(a.util.checkCORSSupport()))){z(aA);
return
}if(a.util.browser.msie&&+a.util.browser.version.split(".")[0]<10){if((aA.transport==="streaming")){if(aA.enableXDR&&window.XDomainRequest){P(aA)
}else{av(aA)
}return
}if((aA.enableXDR)&&(window.XDomainRequest)){P(aA);
return
}}var aD=function(){aA.lastIndex=0;
if(aA.reconnect&&l++<aA.maxReconnectOnClose){N("re-connecting",aC.transport,aC);
R(aB,aA,aC.reconnectInterval)
}else{af(0,"maxReconnectOnClose reached")
}};
if(aA.force||(aA.reconnect&&(aA.maxRequest===-1||aA.requestCount++<aA.maxRequest))){aA.force=false;
var aB=a.util.xhr();
aB.hasData=false;
h(aB,aA,true);
if(aA.suspend){v=aB
}if(aA.transport!=="polling"){W.transport=aA.transport;
aB.onabort=function(){ai(true)
};
aB.onerror=function(){W.error=true;
try{W.status=XMLHttpRequest.status
}catch(aF){W.status=500
}if(!W.status){W.status=500
}if(!W.errorHandled){am();
aD()
}}
}aB.onreadystatechange=function(){if(aw){return
}W.error=null;
var aG=false;
var aL=false;
if(aA.transport==="streaming"&&aA.readyState>2&&aB.readyState===4){if(aA.reconnectingOnLength){return
}am();
aD();
return
}aA.readyState=aB.readyState;
if(aA.transport==="streaming"&&aB.readyState>=3){aL=true
}else{if(aA.transport==="long-polling"&&aB.readyState===4){aL=true
}}y(O);
if(aA.transport!=="polling"){var aF=200;
if(aB.readyState===4){aF=aB.status>1000?0:aB.status
}if(aF>=300||aF===0){W.errorHandled=true;
am();
aD();
return
}if((!aA.enableProtocol||!aC.firstMessage)&&aB.readyState===2){ao(aA)
}}else{if(aB.readyState===4){aL=true
}}if(aL){var aJ=aB.responseText;
if(a.util.trim(aJ).length===0&&aA.transport==="long-polling"){if(!aB.hasData){R(aB,aA,aA.pollingInterval)
}else{aB.hasData=false
}return
}aB.hasData=true;
ag(aB,O);
if(aA.transport==="streaming"){if(!a.util.browser.opera){var aI=aJ.substring(aA.lastIndex,aJ.length);
aG=x(aI,aA,W);
aA.lastIndex=aJ.length;
if(aG){return
}}else{a.util.iterate(function(){if(W.status!==500&&aB.responseText.length>aA.lastIndex){try{W.status=aB.status;
W.headers=a.util.parseHeaders(aB.getAllResponseHeaders());
ag(aB,O)
}catch(aN){W.status=404
}y(O);
W.state="messageReceived";
var aM=aB.responseText.substring(aA.lastIndex);
aA.lastIndex=aB.responseText.length;
aG=x(aM,aA,W);
if(!aG){C()
}L(aB,aA)
}else{if(W.status>400){aA.lastIndex=aB.responseText.length;
return false
}}},0)
}}else{aG=x(aJ,aA,W)
}try{W.status=aB.status;
W.headers=a.util.parseHeaders(aB.getAllResponseHeaders());
ag(aB,aA)
}catch(aK){W.status=404
}if(aA.suspend){W.state=W.status===0?"closed":"messageReceived"
}else{W.state="messagePublished"
}var aH=aC.transport!=="streaming"&&aC.transport!=="polling";
if(aH&&!aA.executeCallbackBeforeReconnect){R(aB,aA,aA.pollingInterval)
}if(W.responseBody.length!==0&&!aG){C()
}if(aH&&aA.executeCallbackBeforeReconnect){R(aB,aA,aA.pollingInterval)
}L(aB,aA)
}};
try{aB.send(aA.data);
ak=true
}catch(aE){a.util.log(aA.logLevel,["Unable to connect to "+aA.url]);
af(0,aE)
}}else{if(aA.logLevel==="debug"){a.util.log(aA.logLevel,["Max re-connection reached."])
}af(0,"maxRequest reached")
}}function h(aC,aD,aB){var aA=aD.url;
if(aD.dispatchUrl!=null&&aD.method==="POST"){aA+=aD.dispatchUrl
}aA=X(aD,aA);
aA=a.util.prepareURL(aA);
if(aB){aC.open(aD.method,aA,aD.async);
if(aD.connectTimeout>0){aD.id=setTimeout(function(){if(aD.requestCount===0){am();
F("Connect timeout","closed",200,aD.transport)
}},aD.connectTimeout)
}}if(O.withCredentials){if("withCredentials" in aC){aC.withCredentials=true
}}if(!O.dropHeaders){aC.setRequestHeader("X-Atmosphere-Framework",a.util.version);
aC.setRequestHeader("X-Atmosphere-Transport",aD.transport);
if(aD.lastTimestamp!=null){aC.setRequestHeader("X-Cache-Date",aD.lastTimestamp)
}else{aC.setRequestHeader("X-Cache-Date",0)
}if(aD.trackMessageLength){aC.setRequestHeader("X-Atmosphere-TrackMessageSize","true")
}aC.setRequestHeader("X-Atmosphere-tracking-id",aD.uuid);
a.util.each(aD.headers,function(aE,aG){var aF=a.util.isFunction(aG)?aG.call(this,aC,aD,aB,W):aG;
if(aF!=null){aC.setRequestHeader(aE,aF)
}})
}if(aD.contentType!==""){aC.setRequestHeader("Content-Type",aD.contentType)
}}function R(aB,aC,aD){if(aC.reconnect||(aC.suspend&&ak)){var aA=0;
if(aB&&aB.readyState>1){aA=aB.status>1000?0:aB.status
}W.status=aA===0?204:aA;
W.reason=aA===0?"Server resumed the connection or down.":"OK";
clearTimeout(aC.id);
if(aC.reconnectId){clearTimeout(aC.reconnectId);
delete aC.reconnectId
}if(aD>0){O.reconnectId=setTimeout(function(){s(aC)
},aD)
}else{s(aC)
}}}function ae(aA){aA.state="re-connecting";
ab(aA)
}function P(aA){if(aA.transport!=="polling"){E=V(aA);
E.open()
}else{V(aA).open()
}}function V(aC){var aB=O;
if((aC!=null)&&(typeof(aC)!=="undefined")){aB=aC
}var aH=aB.transport;
var aG=0;
var aA=new window.XDomainRequest();
var aE=function(){if(aB.transport==="long-polling"&&(aB.reconnect&&(aB.maxRequest===-1||aB.requestCount++<aB.maxRequest))){aA.status=200;
P(aB)
}};
var aF=aB.rewriteURL||function(aJ){var aI=/(?:^|;\s*)(JSESSIONID|PHPSESSID)=([^;]*)/.exec(document.cookie);
switch(aI&&aI[1]){case"JSESSIONID":return aJ.replace(/;jsessionid=[^\?]*|(\?)|$/,";jsessionid="+aI[2]+"$1");
case"PHPSESSID":return aJ.replace(/\?PHPSESSID=[^&]*&?|\?|$/,"?PHPSESSID="+aI[2]+"&").replace(/&$/,"")
}return aJ
};
aA.onprogress=function(){aD(aA)
};
aA.onerror=function(){if(aB.transport!=="polling"){am();
if(l++<aB.maxReconnectOnClose){if(aB.reconnectInterval>0){aB.reconnectId=setTimeout(function(){N("re-connecting",aC.transport,aC);
P(aB)
},aB.reconnectInterval)
}else{N("re-connecting",aC.transport,aC);
P(aB)
}}else{af(0,"maxReconnectOnClose reached")
}}};
aA.onload=function(){};
var aD=function(aI){clearTimeout(aB.id);
var aK=aI.responseText;
aK=aK.substring(aG);
aG+=aK.length;
if(aH!=="polling"){y(aB);
var aJ=x(aK,aB,W);
if(aH==="long-polling"&&a.util.trim(aK).length===0){return
}if(aB.executeCallbackBeforeReconnect){aE()
}if(!aJ){F(W.responseBody,"messageReceived",200,aH)
}if(!aB.executeCallbackBeforeReconnect){aE()
}}};
return{open:function(){var aI=aB.url;
if(aB.dispatchUrl!=null){aI+=aB.dispatchUrl
}aI=X(aB,aI);
aA.open(aB.method,aF(aI));
if(aB.method==="GET"){aA.send()
}else{aA.send(aB.data)
}if(aB.connectTimeout>0){aB.id=setTimeout(function(){if(aB.requestCount===0){am();
F("Connect timeout","closed",200,aB.transport)
}},aB.connectTimeout)
}},close:function(){aA.abort()
}}
}function av(aA){E=w(aA);
E.open()
}function w(aD){var aC=O;
if((aD!=null)&&(typeof(aD)!=="undefined")){aC=aD
}var aB;
var aE=new window.ActiveXObject("htmlfile");
aE.open();
aE.close();
var aA=aC.url;
if(aC.dispatchUrl!=null){aA+=aC.dispatchUrl
}if(aC.transport!=="polling"){W.transport=aC.transport
}return{open:function(){var aF=aE.createElement("iframe");
aA=X(aC);
if(aC.data!==""){aA+="&X-Atmosphere-Post-Body="+encodeURIComponent(aC.data)
}aA=a.util.prepareURL(aA);
aF.src=aA;
aE.body.appendChild(aF);
var aG=aF.contentDocument||aF.contentWindow.document;
aB=a.util.iterate(function(){try{if(!aG.firstChild){return
}var aJ=aG.body?aG.body.lastChild:aG;
var aL=function(){var aN=aJ.cloneNode(true);
aN.appendChild(aG.createTextNode("."));
var aM=aN.innerText;
aM=aM.substring(0,aM.length-1);
return aM
};
if(!aG.body||!aG.body.firstChild||aG.body.firstChild.nodeName.toLowerCase()!=="pre"){var aI=aG.head||aG.getElementsByTagName("head")[0]||aG.documentElement||aG;
var aH=aG.createElement("script");
aH.text="document.write('<plaintext>')";
aI.insertBefore(aH,aI.firstChild);
aI.removeChild(aH);
aJ=aG.body.lastChild
}if(aC.closed){aC.isReopen=true
}aB=a.util.iterate(function(){var aN=aL();
if(aN.length>aC.lastIndex){y(O);
W.status=200;
W.error=null;
aJ.innerText="";
var aM=x(aN,aC,W);
if(aM){return""
}F(W.responseBody,"messageReceived",200,aC.transport)
}aC.lastIndex=0;
if(aG.readyState==="complete"){ai(true);
N("re-connecting",aC.transport,aC);
if(aC.reconnectInterval>0){aC.reconnectId=setTimeout(function(){av(aC)
},aC.reconnectInterval)
}else{av(aC)
}return false
}},null);
return false
}catch(aK){W.error=true;
N("re-connecting",aC.transport,aC);
if(l++<aC.maxReconnectOnClose){if(aC.reconnectInterval>0){aC.reconnectId=setTimeout(function(){av(aC)
},aC.reconnectInterval)
}else{av(aC)
}}else{af(0,"maxReconnectOnClose reached")
}aE.execCommand("Stop");
aE.close();
return false
}})
},close:function(){if(aB){aB()
}aE.execCommand("Stop");
ai(true)
}}
}function al(aA){if(q!=null){m(aA)
}else{if(v!=null||o!=null){i(aA)
}else{if(E!=null){Y(aA)
}else{if(G!=null){U(aA)
}else{if(Z!=null){H(aA)
}else{af(0,"No suspended connection available");
a.util.error("No suspended connection available. Make sure atmosphere.subscribe has been called and request.onOpen invoked before invoking this method")
}}}}}}function n(aB,aA){if(!aA){aA=ap(aB)
}aA.transport="polling";
aA.method="GET";
aA.async=false;
aA.withCredentials=false;
aA.reconnect=false;
aA.force=true;
aA.suspend=false;
aA.timeout=1000;
s(aA)
}function m(aA){q.send(aA)
}function B(aB){if(aB.length===0){return
}try{if(q){q.localSend(aB)
}else{if(aq){aq.signal("localMessage",a.util.stringifyJSON({id:J,event:aB}))
}}}catch(aA){a.util.error(aA)
}}function i(aB){var aA=ap(aB);
s(aA)
}function Y(aB){if(O.enableXDR&&a.util.checkCORSSupport()){var aA=ap(aB);
aA.reconnect=false;
z(aA)
}else{i(aB)
}}function U(aA){i(aA)
}function T(aA){var aB=aA;
if(typeof(aB)==="object"){aB=aA.data
}return aB
}function ap(aB){var aC=T(aB);
var aA={connected:false,timeout:60000,method:"POST",url:O.url,contentType:O.contentType,headers:O.headers,reconnect:true,callback:null,data:aC,suspend:false,maxRequest:-1,logLevel:"info",requestCount:0,withCredentials:O.withCredentials,async:O.async,transport:"polling",isOpen:true,attachHeadersAsQueryString:true,enableXDR:O.enableXDR,uuid:O.uuid,dispatchUrl:O.dispatchUrl,enableProtocol:false,messageDelimiter:"|",maxReconnectOnClose:O.maxReconnectOnClose};
if(typeof(aB)==="object"){aA=a.util.extend(aA,aB)
}return aA
}function H(aA){var aD=a.util.isBinary(aA)?aA:T(aA);
var aB;
try{if(O.dispatchUrl!=null){aB=O.webSocketPathDelimiter+O.dispatchUrl+O.webSocketPathDelimiter+aD
}else{aB=aD
}if(!Z.canSendMessage){a.util.error("WebSocket not connected.");
return
}Z.send(aB)
}catch(aC){Z.onclose=function(aE){};
am();
Q("Websocket failed. Downgrading to Comet and resending "+aA);
i(aA)
}}function ac(aB){var aA=a.util.parseJSON(aB);
if(aA.id!==J){if(typeof(O.onLocalMessage)!=="undefined"){O.onLocalMessage(aA.event)
}else{if(typeof(a.util.onLocalMessage)!=="undefined"){a.util.onLocalMessage(aA.event)
}}}}function F(aD,aA,aB,aC){W.responseBody=aD;
W.transport=aC;
W.status=aB;
W.state=aA;
C()
}function ag(aA,aD){if(!aD.readResponsesHeaders){if(!aD.enableProtocol){aD.lastTimestamp=a.util.now();
aD.uuid=J
}}else{try{var aC=aA.getResponseHeader("X-Cache-Date");
if(aC&&aC!=null&&aC.length>0){aD.lastTimestamp=aC.split(" ").pop()
}var aB=aA.getResponseHeader("X-Atmosphere-tracking-id");
if(aB&&aB!=null){aD.uuid=aB.split(" ").pop()
}}catch(aE){}}}function ab(aA){au(aA,O);
au(aA,a.util)
}function au(aB,aC){switch(aB.state){case"messageReceived":l=0;
if(typeof(aC.onMessage)!=="undefined"){aC.onMessage(aB)
}break;
case"error":if(typeof(aC.onError)!=="undefined"){aC.onError(aB)
}break;
case"opening":delete O.closed;
if(typeof(aC.onOpen)!=="undefined"){aC.onOpen(aB)
}break;
case"messagePublished":if(typeof(aC.onMessagePublished)!=="undefined"){aC.onMessagePublished(aB)
}break;
case"re-connecting":if(typeof(aC.onReconnect)!=="undefined"){aC.onReconnect(O,aB)
}break;
case"closedByClient":if(typeof(aC.onClientTimeout)!=="undefined"){aC.onClientTimeout(O)
}break;
case"re-opening":delete O.closed;
if(typeof(aC.onReopen)!=="undefined"){aC.onReopen(O,aB)
}break;
case"fail-to-reconnect":if(typeof(aC.onFailureToReconnect)!=="undefined"){aC.onFailureToReconnect(O,aB)
}break;
case"unsubscribe":case"closed":var aA=typeof(O.closed)!=="undefined"?O.closed:false;
if(typeof(aC.onClose)!=="undefined"&&!aA){aC.onClose(aB)
}O.closed=true;
break
}}function ai(aA){if(W.state!=="closed"){W.state="closed";
W.responseBody="";
W.messages=[];
W.status=!aA?501:200;
C()
}}function C(){var aC=function(aF,aG){aG(W)
};
if(q==null&&aa!=null){aa(W.responseBody)
}O.reconnect=O.mrequest;
var aA=typeof(W.responseBody)==="string";
var aD=(aA&&O.trackMessageLength)?(W.messages.length>0?W.messages:[""]):new Array(W.responseBody);
for(var aB=0;
aB<aD.length;
aB++){if(aD.length>1&&aD[aB].length===0){continue
}W.responseBody=(aA)?a.util.trim(aD[aB]):aD[aB];
if(q==null&&aa!=null){aa(W.responseBody)
}if(W.responseBody.length===0&&W.state==="messageReceived"){continue
}ab(W);
if(f.length>0){if(O.logLevel==="debug"){a.util.debug("Invoking "+f.length+" global callbacks: "+W.state)
}try{a.util.each(f,aC)
}catch(aE){a.util.log(O.logLevel,["Callback exception"+aE])
}}if(typeof(O.callback)==="function"){if(O.logLevel==="debug"){a.util.debug("Invoking request callbacks")
}try{O.callback(W)
}catch(aE){a.util.log(O.logLevel,["Callback exception"+aE])
}}}}this.subscribe=function(aA){ay(aA);
t()
};
this.execute=function(){t()
};
this.close=function(){an()
};
this.disconnect=function(){D()
};
this.getUrl=function(){return O.url
};
this.push=function(aC,aB){if(aB!=null){var aA=O.dispatchUrl;
O.dispatchUrl=aB;
al(aC);
O.dispatchUrl=aA
}else{al(aC)
}};
this.getUUID=function(){return O.uuid
};
this.pushLocal=function(aA){B(aA)
};
this.enableProtocol=function(aA){return O.enableProtocol
};
this.request=O;
this.response=W
}};
a.subscribe=function(h,k,j){if(typeof(k)==="function"){a.addCallback(k)
}e=0;
if(typeof(h)!=="string"){j=h
}else{j.url=h
}var i=new a.AtmosphereRequest(j);
i.execute();
g[g.length]=i;
return i
};
a.unsubscribe=function(){if(g.length>0){var h=[].concat(g);
for(var k=0;
k<h.length;
k++){var j=h[k];
j.close();
clearTimeout(j.response.request.id)
}}g=[];
f=[]
};
a.unsubscribeUrl=function(j){var h=-1;
if(g.length>0){for(var l=0;
l<g.length;
l++){var k=g[l];
if(k.getUrl()===j){k.close();
clearTimeout(k.response.request.id);
h=l;
break
}}}if(h>=0){g.splice(h,1)
}};
a.addCallback=function(h){if(a.util.inArray(h,f)===-1){f.push(h)
}};
a.removeCallback=function(i){var h=a.util.inArray(i,f);
if(h!==-1){f.splice(h,1)
}};
a.util={browser:{},parseHeaders:function(i){var h,k=/^(.*?):[ \t]*([^\r\n]*)\r?$/mg,j={};
while(h=k.exec(i)){j[h[1]]=h[2]
}return j
},now:function(){return new Date().getTime()
},isArray:function(h){return Object.prototype.toString.call(h)==="[object Array]"
},inArray:function(k,l){if(!Array.prototype.indexOf){var h=l.length;
for(var j=0;
j<h;
++j){if(l[j]===k){return j
}}return -1
}return l.indexOf(k)
},isBinary:function(h){return/^\[object\s(?:Blob|ArrayBuffer|.+Array)\]$/.test(Object.prototype.toString.call(h))
},isFunction:function(h){return Object.prototype.toString.call(h)==="[object Function]"
},getAbsoluteURL:function(h){var i=document.createElement("div");
i.innerHTML='<a href="'+h+'"/>';
return encodeURI(decodeURI(i.firstChild.href))
},prepareURL:function(i){var j=a.util.now();
var h=i.replace(/([?&])_=[^&]*/,"$1_="+j);
return h+(h===i?(/\?/.test(i)?"&":"?")+"_="+j:"")
},trim:function(h){if(!String.prototype.trim){return h.toString().replace(/(?:(?:^|\n)\s+|\s+(?:$|\n))/g,"").replace(/\s+/g," ")
}else{return h.toString().trim()
}},param:function(l){var j,h=[];
function k(m,n){n=a.util.isFunction(n)?n():(n==null?"":n);
h.push(encodeURIComponent(m)+"="+encodeURIComponent(n))
}function i(n,o){var m;
if(a.util.isArray(o)){a.util.each(o,function(q,p){if(/\[\]$/.test(n)){k(n,p)
}else{i(n+"["+(typeof p==="object"?q:"")+"]",p)
}})
}else{if(Object.prototype.toString.call(o)==="[object Object]"){for(m in o){i(n+"["+m+"]",o[m])
}}else{k(n,o)
}}}for(j in l){i(j,l[j])
}return h.join("&").replace(/%20/g,"+")
},storage:function(){try{return !!(window.localStorage&&window.StorageEvent)
}catch(h){return false
}},iterate:function(j,i){var k;
i=i||0;
(function h(){k=setTimeout(function(){if(j()===false){return
}h()
},i)
})();
return function(){clearTimeout(k)
}
},each:function(n,o,j){if(!n){return
}var m,k=0,l=n.length,h=a.util.isArray(n);
if(j){if(h){for(;
k<l;
k++){m=o.apply(n[k],j);
if(m===false){break
}}}else{for(k in n){m=o.apply(n[k],j);
if(m===false){break
}}}}else{if(h){for(;
k<l;
k++){m=o.call(n[k],k,n[k]);
if(m===false){break
}}}else{for(k in n){m=o.call(n[k],k,n[k]);
if(m===false){break
}}}}return n
},extend:function(l){var k,j,h;
for(k=1;
k<arguments.length;
k++){if((j=arguments[k])!=null){for(h in j){l[h]=j[h]
}}}return l
},on:function(j,i,h){if(j.addEventListener){j.addEventListener(i,h,false)
}else{if(j.attachEvent){j.attachEvent("on"+i,h)
}}},off:function(j,i,h){if(j.removeEventListener){j.removeEventListener(i,h,false)
}else{if(j.detachEvent){j.detachEvent("on"+i,h)
}}},log:function(j,i){if(window.console){var h=window.console[j];
if(typeof h==="function"){h.apply(window.console,i)
}}},warn:function(){a.util.log("warn",arguments)
},info:function(){a.util.log("info",arguments)
},debug:function(){a.util.log("debug",arguments)
},error:function(){a.util.log("error",arguments)
},xhr:function(){try{return new window.XMLHttpRequest()
}catch(i){try{return new window.ActiveXObject("Microsoft.XMLHTTP")
}catch(h){}}},parseJSON:function(h){return !h?null:window.JSON&&window.JSON.parse?window.JSON.parse(h):new Function("return "+h)()
},stringifyJSON:function(j){var m=/[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,k={"\b":"\\b","\t":"\\t","\n":"\\n","\f":"\\f","\r":"\\r",'"':'\\"',"\\":"\\\\"};
function h(n){return'"'+n.replace(m,function(o){var p=k[o];
return typeof p==="string"?p:"\\u"+("0000"+o.charCodeAt(0).toString(16)).slice(-4)
})+'"'
}function i(o){return o<10?"0"+o:o
}return window.JSON&&window.JSON.stringify?window.JSON.stringify(j):(function l(s,r){var q,p,n,o,u=r[s],t=typeof u;
if(u&&typeof u==="object"&&typeof u.toJSON==="function"){u=u.toJSON(s);
t=typeof u
}switch(t){case"string":return h(u);
case"number":return isFinite(u)?String(u):"null";
case"boolean":return String(u);
case"object":if(!u){return"null"
}switch(Object.prototype.toString.call(u)){case"[object Date]":return isFinite(u.valueOf())?'"'+u.getUTCFullYear()+"-"+i(u.getUTCMonth()+1)+"-"+i(u.getUTCDate())+"T"+i(u.getUTCHours())+":"+i(u.getUTCMinutes())+":"+i(u.getUTCSeconds())+'Z"':"null";
case"[object Array]":n=u.length;
o=[];
for(q=0;
q<n;
q++){o.push(l(q,u)||"null")
}return"["+o.join(",")+"]";
default:o=[];
for(q in u){if(b.call(u,q)){p=l(q,u);
if(p){o.push(h(q)+":"+p)
}}}return"{"+o.join(",")+"}"
}}})("",{"":j})
},checkCORSSupport:function(){if(a.util.browser.msie&&!window.XDomainRequest&&+a.util.browser.version.split(".")[0]<11){return true
}else{if(a.util.browser.opera&&+a.util.browser.version.split(".")<12){return true
}else{if(a.util.trim(navigator.userAgent).slice(0,16)==="KreaTVWebKit/531"){return true
}else{if(a.util.trim(navigator.userAgent).slice(-7).toLowerCase()==="kreatel"){return true
}}}}var h=navigator.userAgent.toLowerCase();
var i=h.indexOf("android")>-1;
if(i){return true
}return false
}};
d=a.util.now();
(function(){var i=navigator.userAgent.toLowerCase(),h=/(chrome)[ \/]([\w.]+)/.exec(i)||/(webkit)[ \/]([\w.]+)/.exec(i)||/(opera)(?:.*version|)[ \/]([\w.]+)/.exec(i)||/(msie) ([\w.]+)/.exec(i)||/(trident)(?:.*? rv:([\w.]+)|)/.exec(i)||i.indexOf("compatible")<0&&/(mozilla)(?:.*? rv:([\w.]+)|)/.exec(i)||[];
a.util.browser[h[1]||""]=true;
a.util.browser.version=h[2]||"0";
if(a.util.browser.trident){a.util.browser.msie=true
}if(a.util.browser.msie||(a.util.browser.mozilla&&+a.util.browser.version.split(".")[0]===1)){a.util.storage=false
}})();
a.util.on(window,"unload",function(h){a.unsubscribe()
});
a.util.on(window,"keypress",function(h){if(h.charCode===27||h.keyCode===27){if(h.preventDefault){h.preventDefault()
}}});
a.util.on(window,"offline",function(){a.unsubscribe()
});
return a
}));