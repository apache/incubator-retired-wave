/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

{var JSON,___,attachDocumentStub,bridal,bridalMaker,cajita,css,cssparser,domitaModules,escape,html,html4,json_sans_eval,safeJSON,unicode;typeof
Date.prototype.toJSON!=='function'&&(Date.prototype.toJSON=function(key){return isFinite(this.valueOf())?this.getUTCFullYear()+'-'+f(this.getUTCMonth()+1)+'-'+f(this.getUTCDate())+'T'+f(this.getUTCHours())+':'+f(this.getUTCMinutes())+':'+f(this.getUTCSeconds())+'Z':null},String.prototype.toJSON=Number.prototype.toJSON=Boolean.prototype.toJSON=function(key){return this.valueOf()}),json_sans_eval=(function(){var
hop=Object.hasOwnProperty,EMPTY_STRING,SLASH,completeToken,cx,escapable,escapeSequence,escapes,gap,indent,meta,number,oneChar,rep,significantToken,string;function
f(n){return n<10?'0'+n:n}cx=/[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,escapable=/[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,meta={'\b':'\\b','	':'\\t','\n':'\\n','':'\\f','\r':'\\r','\"':'\\\"','\\':'\\\\'};function
quote(string){return escapable.lastIndex=0,escapable.test(string)?'\"'+string.replace(escapable,function(a){var
c=meta[a];return typeof c==='string'?c:'\\u'+('0000'+a.charCodeAt(0).toString(16)).slice(-4)})+'\"':'\"'+string+'\"'}function
str(key,holder){var mind=gap,value=holder[key],i,k,length,partial,v;value&&typeof
value==='object'&&typeof value.toJSON==='function'&&(value=value.toJSON(key)),typeof
rep==='function'&&(value=rep.call(holder,key,value));switch(typeof value){case'string':return quote(value);case'number':return isFinite(value)?String(value):'null';case'boolean':case'null':return String(value);case'object':if(!value)return'null';gap+=indent,partial=[];if(Object.prototype.toString.apply(value)==='[object Array]'){length=value.length;for(i=0;i<length;i+=1)partial[i]=str(i,value)||'null';return v=partial.length===0?'[]':gap?'[\n'+gap+partial.join(',\n'+gap)+'\n'+mind+']':'['+partial.join(',')+']',gap=mind,v}if(rep&&typeof
rep==='object'){length=rep.length;for(i=0;i<length;i+=1)k=rep[i],typeof k==='string'&&(v=str(k,value),v&&partial.push(quote(k)+(gap?': ':':')+v))}else
for(k in value)hop.call(value,k)&&(v=str(k,value),v&&partial.push(quote(k)+(gap?': ':':')+v));return v=partial.length===0?'{}':gap?'{\n'+gap+partial.join(',\n'+gap)+'\n'+mind+'}':'{'+partial.join(',')+'}',gap=mind,v}}function
stringify(value,replacer,space){var i;gap='',indent='';if(typeof space==='number')for(i=0;i<space;i+=1)indent+=' ';else
if(typeof space==='string')indent=space;rep=replacer;if(replacer&&typeof replacer!=='function'&&(typeof
replacer!=='object'||typeof replacer.length!=='number'))throw new Error('json_sans_eval.stringify');return str('',{'':value})}number='(?:-?\\b(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?\\b)',oneChar='(?:[^\\0-\\x08\\x0a-\\x1f\"\\\\]|\\\\(?:[\"/\\\\bfnrt]|u[0-9A-Fa-f]{4}))',string='(?:\"'+oneChar+'*\")',significantToken=new
RegExp('(?:false|true|null|[\\{\\}\\[\\]]|'+number+'|'+string+')','g'),escapeSequence=new
RegExp('\\\\(?:([^u])|u(.{4}))','g'),escapes={'\"':'\"','/':'/','\\':'\\','b':'\b','f':'','n':'\n','r':'\r','t':'	'};function
unescapeOne(_,ch,hex){return ch?escapes[ch]:String.fromCharCode(parseInt(hex,16))}EMPTY_STRING=new
String(''),SLASH='\\',completeToken=new RegExp('(?:false|true|null|[ 	\r\n]+|[\\{\\}\\[\\],:]|'+number+'|'+string+'|.)','g');function
blank(arr,s,e){while(--e>=s)arr[e]=''}function checkSyntax(text,keyFilter){var toks=(''+text).match(completeToken),i=0,n=toks.length;checkArray();if(i<n)throw new
Error('Trailing tokens '+toks.slice(i-1).join(''));return toks.join('');function
checkArray(){var t;while(i<n){t=toks[i++];switch(t){case']':return;case'[':checkArray();break;case'{':checkObject()}}}function
checkObject(){var state=0,len,t;while(i<n){t=toks[i++];switch(t.charCodeAt(0)){case
9:case 10:case 13:case 32:continue;case 34:len=t.length;if(len===1)throw new Error(t);if(state===0){if(keyFilter&&!keyFilter(t.substring(1,len-1).replace(escapeSequence,unescapeOne)))throw new
Error(t)}else if(state!==2)throw new Error(t);break;case 39:throw new Error(t);case
44:if(state!==3)throw new Error(t);state=0;continue;case 58:if(state!==1)throw new
Error(t);break;case 91:if(state!==2)throw new Error(t);checkArray();break;case 123:if(state!==2)throw new
Error(t);checkObject();break;case 125:return;default:if(state!==2)throw new Error(t)}++state}}}function
parse(json,opt_reviver){var toks=json.match(significantToken),tok=toks[0],cont,i,key,n,result,stack,walk;if('{'===tok)result={};else
if('['===tok)result=[];else throw new Error(tok);stack=[result];for(i=1,n=toks.length;i<n;++i){tok=toks[i];switch(tok.charCodeAt(0)){default:cont=stack[0],cont[key||cont.length]=+tok,key=void
0;break;case 34:tok=tok.substring(1,tok.length-1),tok.indexOf(SLASH)!==-1&&(tok=tok.replace(escapeSequence,unescapeOne)),cont=stack[0];if(!key)if(cont
instanceof Array)key=cont.length;else{key=tok||EMPTY_STRING;break}cont[key]=tok,key=void
0;break;case 91:cont=stack[0],stack.unshift(cont[key||cont.length]=[]),key=void 0;break;case
93:stack.shift();break;case 102:cont=stack[0],cont[key||cont.length]=false,key=void
0;break;case 110:cont=stack[0],cont[key||cont.length]=null,key=void 0;break;case
116:cont=stack[0],cont[key||cont.length]=true,key=void 0;break;case 123:cont=stack[0],stack.unshift(cont[key||cont.length]={}),key=void
0;break;case 125:stack.shift()}}if(stack.length)throw new Error;return opt_reviver&&(walk=function(holder,key){var
value=holder[key],i,k,toDelete,v;if(value&&typeof value==='object'){toDelete=null;for(k
in value)hop.call(value,k)&&value!==holder&&(v=walk(value,k),v!==void 0?(value[k]=v):(toDelete||(toDelete=[]),toDelete.push(k)));if(toDelete)for(i=toDelete.length;--i>=0;)delete
value[toDelete[i]]}return opt_reviver.call(holder,key,value)},result=walk({'':result},'')),result}return{'checkSyntax':checkSyntax,'parse':parse,'stringify':stringify}})(),typeof
JSON==='undefined'&&(JSON={}),typeof JSON.stringify!=='function'&&(JSON.stringify=json_sans_eval.stringify),typeof
JSON.parse!=='function'&&(JSON.parse=json_sans_eval.parse),Array.typeTag___='Array',Object.typeTag___='Object',String.typeTag___='String',Boolean.typeTag___='Boolean',Number.typeTag___='Number',Date.typeTag___='Date',RegExp.typeTag___='RegExp',Error.typeTag___='Error',EvalError.typeTag___='EvalError',RangeError.typeTag___='RangeError',ReferenceError.typeTag___='ReferenceError',SyntaxError.typeTag___='SyntaxError',TypeError.typeTag___='TypeError',URIError.typeTag___='URIError',Object.prototype.proto___=null,Date.prototype.toISOString===void
0&&typeof Date.prototype.toJSON==='function'&&(Date.prototype.toISOString=function(){return Date.prototype.toJSON.call(this)});try{(function(){}).apply({},{'length':0})}catch(ex){ex
instanceof TypeError&&(Function.prototype.apply___=Function.prototype.apply,Function.prototype.apply=function
applyGuard(self,args){return args&&args.CLASS___==='Arguments'&&(args=Array.slice(args,0)),this.apply___(self,args)})}Array.slice===void
0&&(Array.slice=function(self,opt_start,opt_end){return self&&typeof self==='object'?(opt_end===void
0&&(opt_end=self.length),Array.prototype.slice.call(self,opt_start,opt_end)):[]}),Function.prototype.bind===void
0&&(Function.prototype.bind=function(self,var_args){var thisFunc=this,leftArgs=Array.slice(arguments,1);function
funcBound(var_args){var args=leftArgs.concat(Array.slice(arguments,0));return thisFunc.apply(self,args)}return funcBound}),(function(global){var
BREAK,GuardMark,GuardStamp,GuardT,MAGIC_NAME,MAGIC_NUM,MAGIC_TOKEN,NO_RESULT,PseudoFunctionProto,USELESS,attribute,endsWith__,endsWith___,endsWith_canDelete___,endsWith_canRead___,endsWith_canSet___,goodJSON,magicCount,myKeeper,myLogFunc,myNewModuleHandler,myOriginalHOP,myOriginalToString,obtainNewModule,poisonArgsCallee,poisonArgsCaller,poisonFuncArgs,poisonFuncCaller,pushMethod,registeredImports,sharedImports,stackInfoFields;function
ToInt32(alleged_int){return alleged_int>>0}function ToUInt32(alleged_int){return alleged_int>>>0}function
arrayIndexOf(specimen,i){var len=ToUInt32(this.length);i=ToInt32(i),i<0&&((i+=len)<0&&(i=0));for(;i<len;++i)if(i
in this&&identical(this[i],specimen))return i;return -1}Array.prototype.indexOf=arrayIndexOf;function
arrayLastIndexOf(specimen,i){var len=ToUInt32(this.length);if(isNaN(i))i=len-1;else{i=ToInt32(i);if(i<0){i+=len;if(i<0)return -1}else
if(i>=len)i=len-1}for(;i>=0;--i)if(i in this&&identical(this[i],specimen))return i;return -1}Array.prototype.lastIndexOf=arrayLastIndexOf,endsWith_canDelete___=/_canDelete___$/,endsWith_canRead___=/_canRead___$/,endsWith_canSet___=/_canSet___$/,endsWith___=/___$/,endsWith__=/__$/;function
typeOf(obj){var result=typeof obj,ctor;return result!=='function'?result:(ctor=obj.constructor,typeof
ctor==='function'&&ctor.typeTag___==='RegExp'&&obj instanceof ctor?'object':'function')}typeof
new RegExp('x')==='object'&&(typeOf=function fastTypeof(obj){return typeof obj}),myOriginalHOP=Object.prototype.hasOwnProperty,myOriginalToString=Object.prototype.toString;function
hasOwnProp(obj,name){var t;return obj?(t=typeof obj,t!=='object'&&t!=='function'?false:myOriginalHOP.call(obj,name)):false}function
identical(x,y){return x===y?x!==0||1/x===1/y:x!==x&&y!==y}function callFault(var_args){return asFunc(this).apply(USELESS,arguments)}Object.prototype.CALL___=callFault;function
defaultLogger(str,opt_stop){}myLogFunc=markFuncFreeze(defaultLogger);function getLogFunc(){return myLogFunc}function
setLogFunc(newLogFunc){myLogFunc=newLogFunc}function log(str){myLogFunc(String(str))}function
fail(var_args){var message=Array.slice(arguments,0).join('');throw myLogFunc(message,true),new
Error(message)}function enforce(test,var_args){return test||fail.apply(USELESS,Array.slice(arguments,1))}function
enforceType(specimen,typename,opt_name){return typeOf(specimen)!==typename&&fail('expected ',typename,' instead of ',typeOf(specimen),': ',opt_name||specimen),specimen}function
enforceNat(specimen){return enforceType(specimen,'number'),Math.floor(specimen)!==specimen&&fail('Must be integral: ',specimen),specimen<0&&fail('Must not be negative: ',specimen),Math.floor(specimen-1)!==specimen-1&&fail('Beyond precision limit: ',specimen),Math.floor(specimen-1)>=specimen&&fail('Must not be infinite: ',specimen),specimen}function
deprecate(func,badName,advice){var warningNeeded=true;return function(){return warningNeeded&&(log('\"'+badName+'\" is deprecated.\n'+advice),warningNeeded=false),func.apply(USELESS,arguments)}}function
debugReference(obj){var constr;switch(typeOf(obj)){case'object':return obj===null?'<null>':(constr=directConstructor(obj),'['+(constr&&constr.name||'Object')+']');default:return'('+obj+':'+typeOf(obj)+')'}}myKeeper={'toString':function
toString(){return'<Logging Keeper>'},'handleRead':function handleRead(obj,name){return},'handleCall':function
handleCall(obj,name,args){fail('Not callable: (',debugReference(obj),').',name)},'handleSet':function
handleSet(obj,name,val){fail('Not writable: (',debugReference(obj),').',name)},'handleDelete':function
handleDelete(obj,name){fail('Not deletable: (',debugReference(obj),').',name)}},Object.prototype.handleRead___=function
handleRead___(name){var handlerName=name+'_getter___';return this[handlerName]?this[handlerName]():myKeeper.handleRead(this,name)},Object.prototype.handleCall___=function
handleCall___(name,args){var handlerName=name+'_handler___';return this[handlerName]?this[handlerName].call(this,args):myKeeper.handleCall(this,name,args)},Object.prototype.handleSet___=function
handleSet___(name,val){var handlerName=name+'_setter___';return this[handlerName]?this[handlerName](val):myKeeper.handleSet(this,name,val)},Object.prototype.handleDelete___=function
handleDelete___(name){var handlerName=name+'_deleter___';return this[handlerName]?this[handlerName]():myKeeper.handleDelete(this,name)};function
directConstructor(obj){var oldConstr,proto,result;if(obj===null)return;if(obj===void
0)return;if(typeOf(obj)==='function')return;obj=Object(obj);if(myOriginalHOP.call(obj,'proto___')){proto=obj.proto___;if(proto===null)return;result=proto.constructor,(result.prototype!==proto||typeOf(result)!=='function')&&(result=directConstructor(proto))}else{if(!myOriginalHOP.call(obj,'constructor'))result=obj.constructor;else{oldConstr=obj.constructor;if(delete
obj.constructor)result=obj.constructor,obj.constructor=oldConstr;else if(isPrototypical(obj))log('Guessing the directConstructor of : '+obj),result=Object;else
return fail('Discovery of direct constructors unsupported when the ','constructor property is not deletable: ',obj,'.constructor === ',oldConstr,'(',obj===global,')')}(typeOf(result)!=='function'||!(obj
instanceof result))&&fail('Discovery of direct constructors for foreign begotten ','objects not implemented on this platform.\n'),result.prototype.constructor===result&&(obj.proto___=result.prototype)}return result}function
getFuncCategory(fun){return enforceType(fun,'function'),fun.typeTag___?fun.typeTag___:fun}function
isDirectInstanceOf(obj,ctor){var constr=directConstructor(obj);return constr===void
0?false:getFuncCategory(constr)===getFuncCategory(ctor)}function isInstanceOf(obj,ctor){return obj
instanceof ctor?true:!!isDirectInstanceOf(obj,ctor)}function isRecord(obj){return obj?obj.RECORD___===obj?true:isDirectInstanceOf(obj,Object)?(obj.RECORD___=obj,true):false:false}function
isArray(obj){return isDirectInstanceOf(obj,Array)}function isJSONContainer(obj){var
constr,typeTag;return obj?obj.RECORD___===obj?true:(constr=directConstructor(obj),constr===void
0?false:(typeTag=constr.typeTag___,typeTag!=='Object'&&typeTag!=='Array'?false:!isPrototypical(obj))):false}function
isFrozen(obj){var t;return obj?obj.FROZEN___===obj?true:(t=typeof obj,t!=='object'&&t!=='function'):true}function
primFreeze(obj){var badFlags,flag,i,k;if(isFrozen(obj))return obj;if(obj.SLOWFREEZE___){badFlags=[];for(k
in obj)(endsWith_canSet___.test(k)||endsWith_canDelete___.test(k))&&(obj[k]&&badFlags.push(k));for(i=0;i<badFlags.length;++i)flag=badFlags[i],myOriginalHOP.call(obj,flag)&&(delete
obj[flag]||fail('internal: failed delete: ',debugReference(obj),'.',flag)),obj[flag]&&(obj[flag]=false);delete
obj.SLOWFREEZE___}return obj.FROZEN___=obj,typeOf(obj)==='function'&&(isFunc(obj)&&(grantCall(obj,'call'),grantCall(obj,'apply'),obj.CALL___=obj),obj.prototype&&primFreeze(obj.prototype)),obj}function
freeze(obj){if(isJSONContainer(obj))return primFreeze(obj);if(typeOf(obj)==='function')return enforce(isFrozen(obj),'Internal: non-frozen function: '+obj),obj;if(isInstanceOf(obj,Error))return primFreeze(obj);fail('cajita.freeze(obj) applies only to JSON Containers, ','functions, and Errors: ',debugReference(obj))}function
copy(obj){var result;return isJSONContainer(obj)||fail('cajita.copy(obj) applies only to JSON Containers: ',debugReference(obj)),result=isArray(obj)?[]:{},forOwnKeys(obj,markFuncFreeze(function(k,v){result[k]=v})),result}function
snapshot(obj){return primFreeze(copy(obj))}function canRead(obj,name){return obj===void
0||obj===null?false:!!obj[name+'_canRead___']}function canEnum(obj,name){return obj===void
0||obj===null?false:!!obj[name+'_canEnum___']}function canCall(obj,name){return obj===void
0||obj===null?false:obj[name+'_canCall___']?true:obj[name+'_grantCall___']?(fastpathCall(obj,name),true):false}function
canSet(obj,name){return obj===void 0||obj===null?false:obj[name+'_canSet___']===obj?true:obj[name+'_grantSet___']===obj?(fastpathSet(obj,name),true):false}function
canDelete(obj,name){return obj===void 0||obj===null?false:obj[name+'_canDelete___']===obj}function
fastpathRead(obj,name){name==='toString'&&fail('internal: Can\'t fastpath .toString'),obj[name+'_canRead___']=obj}function
fastpathEnum(obj,name){obj[name+'_canEnum___']=obj}function fastpathCall(obj,name){name==='toString'&&fail('internal: Can\'t fastpath .toString'),obj[name+'_canSet___']&&(obj[name+'_canSet___']=false),obj[name+'_grantSet___']&&(obj[name+'_grantSet___']=false),obj[name+'_canCall___']=obj}function
fastpathSet(obj,name){name==='toString'&&fail('internal: Can\'t fastpath .toString'),isFrozen(obj)&&fail('Can\'t set .',name,' on frozen (',debugReference(obj),')'),typeOf(obj)==='function'&&fail('Can\'t make .',name,' writable on a function (',debugReference(obj),')'),fastpathEnum(obj,name),fastpathRead(obj,name),obj[name+'_canCall___']&&(obj[name+'_canCall___']=false),obj[name+'_grantCall___']&&(obj[name+'_grantCall___']=false),obj.SLOWFREEZE___=obj,obj[name+'_canSet___']=obj}function
fastpathDelete(obj,name){name==='toString'&&fail('internal: Can\'t fastpath .toString'),isFrozen(obj)&&fail('Can\'t delete .',name,' on frozen (',debugReference(obj),')'),typeOf(obj)==='function'&&fail('Can\'t make .',name,' deletable on a function (',debugReference(obj),')'),obj.SLOWFREEZE___=obj,obj[name+'_canDelete___']=obj}function
grantRead(obj,name){fastpathRead(obj,name)}function grantEnum(obj,name){fastpathEnum(obj,name)}function
grantCall(obj,name){fastpathCall(obj,name),obj[name+'_grantCall___']=obj}function
grantSet(obj,name){fastpathSet(obj,name),obj[name+'_grantSet___']=obj}function grantDelete(obj,name){fastpathDelete(obj,name)}function
tamesTo(f,t){var ftype=typeof f,ttype;(!f||ftype!=='function'&&ftype!=='object')&&fail('Unexpected feral primitive: ',f),ttype=typeof
t,(!t||ttype!=='function'&&ttype!=='object')&&fail('Unexpected tame primitive: ',t);if(f.TAMED_TWIN___===t&&t.FERAL_TWIN___===f)return log('multiply tamed: '+f+', '+t),void
0;f.TAMED_TWIN___&&hasOwnProp(f,'TAMED_TWIN___')&&fail('Already tames to something: ',f),t.FERAL_TWIN___&&hasOwnProp(t,'FERAL_TWIN___')&&fail('Already untames to something: ',t),f.FERAL_TWIN___&&hasOwnProp(f,'FERAL_TWIN___')&&fail('Already tame: ',f),t.TAMED_TWIN___&&hasOwnProp(t,'TAMED_TWIN___')&&fail('Already feral: ',t),f.TAMED_TWIN___=t,t.FERAL_TWIN___=f}function
tamesToSelf(obj){var otype=typeof obj;(!obj||otype!=='function'&&otype!=='object')&&fail('Unexpected primitive: ',obj);if(obj.TAMED_TWIN___===obj&&obj.FERAL_TWIN___===obj)return log('multiply tamed: '+obj),void
0;obj.TAMED_TWIN___&&hasOwnProp(obj,'TAMED_TWIN___')&&fail('Already tames to something: ',obj),obj.FERAL_TWIN___&&hasOwnProp(obj,'FERAL_TWIN___')&&fail('Already untames to something: ',obj),obj.TAMED_TWIN___=obj.FERAL_TWIN___=obj}function
tame(f){var ftype=typeof f,realFeral,t;return!f||ftype!=='function'&&ftype!=='object'?f:(t=f.TAMED_TWIN___,t&&t.FERAL_TWIN___===f?t:(realFeral=f.FERAL_TWIN___,realFeral&&realFeral.TAMED_TWIN___===f?(log('Tame-only object from feral side: '+f),f):f.AS_TAMED___?(t=f.AS_TAMED___(),t&&tamesTo(f,t),t):isRecord(f)?(t=tameRecord(f),t&&tamesTo(f,t),t):undefined))}function
untame(t){var ttype=typeof t,f,realTame;return!t||ttype!=='function'&&ttype!=='object'?t:(f=t.FERAL_TWIN___,f&&f.TAMED_TWIN___===t?f:(realTame=t.TAMED_TWIN___,realTame&&realTame.FERAL_TWIN___===t?(log('Feral-only object from tame side: '+t),t):t.AS_FERAL___?(f=t.AS_FERAL___(),f&&tamesTo(f,t),f):isRecord(t)?(f=untameRecord(t),f&&tamesTo(f,t),f):undefined))}global.AS_TAMED___=function(){fail('global object almost leaked')},global.AS_FERAL___=function(){fail('global object leaked')};function
tameRecord(f){var t={},changed=!isFrozen(f),fv,i,k,keys,len,tv;tamesTo(f,t);try{keys=ownKeys(f),len=keys.length;for(i=0;i<len;++i)k=keys[i],fv=f[k],tv=tame(fv),tv===void
0&&fv!==void 0?(changed=true):(fv!==tv&&fv===fv&&(changed=true),t[k]=tv)}finally{delete
f.TAMED_TWIN___,delete t.FERAL_TWIN___}return changed?primFreeze(t):f}function
untameRecord(t){var f={},changed=!isFrozen(t),fv,i,k,keys,len,tv;tamesTo(f,t);try{keys=ownKeys(t),len=keys.length;for(i=0;i<len;++i)k=keys[i],tv=t[k],fv=untame(tv),fv===void
0&&tv!==void 0?(changed=true):(tv!==fv&&tv===tv&&(changed=true),f[k]=fv)}finally{delete
t.FERAL_TWIN___,delete f.TAMED_TWIN___}return changed?primFreeze(f):t}Array.prototype.AS_TAMED___=function
tameArray(){var f=this,t=[],changed=!isFrozen(f),fv,i,len,tv;tamesTo(f,t);try{len=f.length;for(i=0;i<len;++i)i
in f?(fv=f[i],tv=tame(fv),fv!==tv&&fv===fv&&(changed=true),t[i]=tv):(changed=true,t[i]=void
0)}finally{delete f.TAMED_TWIN___,delete t.FERAL_TWIN___}return changed?primFreeze(t):f},Array.prototype.AS_FERAL___=function
untameArray(){var t=this,f=[],changed=!isFrozen(t),fv,i,len,tv;tamesTo(f,t);try{len=t.length;for(i=0;i<len;++i)i
in t?(tv=t[i],fv=untame(tv),tv!==fv&&tv===tv&&(changed=true),f[i]=fv):(changed=true,f[i]=void
0)}finally{delete t.FERAL_TWIN___,delete f.TAMED_TWIN___}return changed?primFreeze(f):t},Function.prototype.AS_TAMED___=function
defaultTameFunc(){var f=this;return isFunc(f)||isCtor(f)?f:void 0},Function.prototype.AS_FERAL___=function
defaultUntameFunc(){var t=this;return isFunc(t)||isCtor(t)?t:void 0};function stopEscalation(val){return val===null||val===void
0||val===global?USELESS:val}function tameXo4a(){var xo4aFunc=this,result;function
tameApplyFuncWrapper(self,opt_args){return xo4aFunc.apply(stopEscalation(self),opt_args||[])}markFuncFreeze(tameApplyFuncWrapper);function
tameCallFuncWrapper(self,var_args){return tameApplyFuncWrapper(self,Array.slice(arguments,1))}return markFuncFreeze(tameCallFuncWrapper),result=PseudoFunction(tameCallFuncWrapper,tameApplyFuncWrapper),result.length=xo4aFunc.length,result.toString=markFuncFreeze(xo4aFunc.toString.bind(xo4aFunc)),primFreeze(result)}function
tameInnocent(){var feralFunc=this,result;function tameApplyFuncWrapper(self,opt_args){var
feralThis=stopEscalation(untame(self)),feralArgs=untame(opt_args),feralResult=feralFunc.apply(feralThis,feralArgs||[]);return tame(feralResult)}markFuncFreeze(tameApplyFuncWrapper);function
tameCallFuncWrapper(self,var_args){return tameApplyFuncWrapper(self,Array.slice(arguments,1))}return markFuncFreeze(tameCallFuncWrapper),result=PseudoFunction(tameCallFuncWrapper,tameApplyFuncWrapper),result.length=feralFunc.length,result.toString=markFuncFreeze(feralFunc.toString.bind(feralFunc)),primFreeze(result)}function
makePoisonPill(badThing){function poisonPill(){throw new TypeError(''+badThing+' forbidden by ES5/strict')}return poisonPill}poisonArgsCallee=makePoisonPill('arguments.callee'),poisonArgsCaller=makePoisonPill('arguments.caller'),poisonFuncCaller=makePoisonPill('A function\'s .caller'),poisonFuncArgs=makePoisonPill('A function\'s .arguments');function
args(original){var result={'length':0};return pushMethod.apply(result,original),result.CLASS___='Arguments',useGetHandler(result,'callee',poisonArgsCallee),useSetHandler(result,'callee',poisonArgsCallee),useGetHandler(result,'caller',poisonArgsCaller),useSetHandler(result,'caller',poisonArgsCaller),result}pushMethod=[].push,PseudoFunctionProto={'toString':markFuncFreeze(function(){return'pseudofunction(var_args) {\n    [some code]\n}'}),'PFUNC___':true,'CLASS___':'Function','AS_FERAL___':function
untamePseudoFunction(){var tamePseudoFunc=this;function feralWrapper(var_args){var
feralArgs=Array.slice(arguments,0),tamedSelf=tame(stopEscalation(this)),tamedArgs=tame(feralArgs),tameResult=callPub(tamePseudoFunc,'apply',[tamedSelf,tamedArgs]);return untame(tameResult)}return feralWrapper}},useGetHandler(PseudoFunctionProto,'caller',poisonFuncCaller),useSetHandler(PseudoFunctionProto,'caller',poisonFuncCaller),useGetHandler(PseudoFunctionProto,'arguments',poisonFuncArgs),useSetHandler(PseudoFunctionProto,'arguments',poisonFuncArgs),primFreeze(PseudoFunctionProto);function
PseudoFunction(callFunc,opt_applyFunc){var applyFunc,result;return callFunc=asFunc(callFunc),applyFunc=opt_applyFunc?asFunc(opt_applyFunc):markFuncFreeze(function
applyFun(self,opt_args){var args=[self];return opt_args!==void 0&&opt_args!==null&&args.push.apply(args,opt_args),callFunc.apply(USELESS,args)}),result=primBeget(PseudoFunctionProto),result.call=callFunc,result.apply=applyFunc,result.bind=markFuncFreeze(function
bindFun(self,var_args){var args;return self=stopEscalation(self),args=[USELESS,self].concat(Array.slice(arguments,1)),markFuncFreeze(callFunc.bind.apply(callFunc,args))}),result.length=callFunc.length-1,result}function
isCtor(constr){return constr&&!!constr.CONSTRUCTOR___}function isFunc(fun){return fun&&!!fun.FUNC___}function
isXo4aFunc(func){return func&&!!func.XO4A___}function isPseudoFunc(fun){return fun&&fun.PFUNC___}function
markCtor(constr,opt_Sup,opt_name){return enforceType(constr,'function',opt_name),isFunc(constr)&&fail('Simple functions can\'t be constructors: ',constr),isXo4aFunc(constr)&&fail('Exophoric functions can\'t be constructors: ',constr),constr.CONSTRUCTOR___=true,opt_Sup?derive(constr,opt_Sup):constr!==Object&&fail('Only \"Object\" has no super: ',constr),opt_name&&(constr.NAME___=String(opt_name)),constr!==Object&&constr!==Array&&(constr.prototype.AS_TAMED___=constr.prototype.AS_FERAL___=function(){return this}),constr}function
derive(constr,sup){var proto=constr.prototype;sup=asCtor(sup),isFrozen(constr)&&fail('Derived constructor already frozen: ',constr),proto
instanceof sup||fail('\"'+constr+'\" does not derive from \"',sup),'__proto__'in
proto&&proto.__proto__!==sup.prototype&&fail('\"'+constr+'\" does not derive directly from \"',sup),isFrozen(proto)||(proto.proto___=sup.prototype)}function
extend(feralCtor,someSuper,opt_name){var inert,noop;return'function'!==typeof feralCtor&&fail('Internal: Feral constructor is not a function'),someSuper=asCtor(someSuper.prototype.constructor),noop=function(){},noop.prototype=someSuper.prototype,feralCtor.prototype=new
noop,feralCtor.prototype.proto___=someSuper.prototype,inert=function(){fail('This constructor cannot be called directly')},inert.prototype=feralCtor.prototype,feralCtor.prototype.constructor=inert,markCtor(inert,someSuper,opt_name),tamesTo(feralCtor,inert),primFreeze(inert)}function
markXo4a(func,opt_name){return enforceType(func,'function',opt_name),isCtor(func)&&fail('Internal: Constructors can\'t be exophora: ',func),isFunc(func)&&fail('Internal: Simple functions can\'t be exophora: ',func),func.XO4A___=true,opt_name&&(func.NAME___=opt_name),func.AS_TAMED___=tameXo4a,primFreeze(func)}function
markInnocent(func,opt_name){return enforceType(func,'function',opt_name),isCtor(func)&&fail('Internal: Constructors aren\'t innocent: ',func),isFunc(func)&&fail('Internal: Simple functions aren\'t innocent: ',func),isXo4aFunc(func)&&fail('Internal: Exophoric functions aren\'t innocent: ',func),opt_name&&(func.NAME___=opt_name),func.AS_TAMED___=tameInnocent,primFreeze(func)}function
markFuncFreeze(fun,opt_name){return typeOf(fun)!=='function'&&fail('expected function instead of ',typeOf(fun),': ',opt_name||fun),fun.CONSTRUCTOR___&&fail('Constructors can\'t be simple functions: ',fun),fun.XO4A___&&fail('Exophoric functions can\'t be simple functions: ',fun),fun.FUNC___=opt_name?String(opt_name):true,primFreeze(fun)}function
asCtorOnly(constr){if(isCtor(constr)||isFunc(constr))return constr;enforceType(constr,'function'),fail('Untamed functions can\'t be called as constructors: ',constr)}function
asCtor(constr){return primFreeze(asCtorOnly(constr))}function asFunc(fun){if(fun&&fun.FUNC___)return fun.FROZEN___===fun?fun:primFreeze(fun);enforceType(fun,'function');if(isCtor(fun)){if(fun===Number||fun===String||fun===Boolean)return primFreeze(fun);fail('Constructors can\'t be called as simple functions: ',fun)}isXo4aFunc(fun)&&fail('Exophoric functions can\'t be called as simple functions: ',fun),fail('Untamed functions can\'t be called as simple functions: ',fun)}function
toFunc(fun){return isPseudoFunc(fun)?markFuncFreeze(function applier(var_args){return callPub(fun,'apply',[USELESS,Array.slice(arguments,0)])}):asFunc(fun)}function
isPrototypical(obj){var constr;return typeOf(obj)!=='object'?false:obj===null?false:(constr=obj.constructor,typeOf(constr)!=='function'?false:constr.prototype===obj)}function
asFirstClass(value){switch(typeOf(value)){case'function':if(isFunc(value)||isCtor(value)){if(isFrozen(value))return value;fail('Internal: non-frozen function encountered: ',value)}else
if(isXo4aFunc(value))fail('Internal: toxic exophora encountered: ',value);else fail('Internal: toxic function encountered: ',value);break;case'object':return value!==null&&isPrototypical(value)&&fail('Internal: prototypical object encountered: ',value),value;default:return value}}function
canReadPub(obj,name){return typeof name==='number'&&name>=0?name in obj:(name=String(name),obj===null?false:obj===void
0?false:obj[name+'_canRead___']?name in Object(obj):endsWith__.test(name)?false:name==='toString'?false:isJSONContainer(obj)?myOriginalHOP.call(obj,name)?(fastpathRead(obj,name),true):false:false)}function
hasOwnPropertyOf(obj,name){return typeof name==='number'&&name>=0?hasOwnProp(obj,name):(name=String(name),obj&&obj[name+'_canRead___']===obj?true:canReadPub(obj,name)&&myOriginalHOP.call(obj,name))}function
inPub(name,obj){var t=typeof obj;if(!obj||t!=='object'&&t!=='function')throw new
TypeError('invalid \"in\" operand: '+obj);return obj=Object(obj),canReadPub(obj,name)?true:canCallPub(obj,name)?true:name+'_getter___'in
obj?true:name+'_handler___'in obj}function readPub(obj,name){if(typeof name==='number'&&name>=0)return typeof
obj==='string'?obj.charAt(name):obj[name];name=String(name);if(canReadPub(obj,name))return obj[name];if(obj===null||obj===void
0)throw new TypeError('Can\'t read '+name+' on '+obj);return obj.handleRead___(name)}function
readOwn(obj,name,pumpkin){if(typeof obj!=='object'||!obj){if(typeOf(obj)!=='object')return pumpkin};return typeof
name==='number'&&name>=0?myOriginalHOP.call(obj,name)?obj[name]:pumpkin:(name=String(name),obj[name+'_canRead___']===obj?obj[name]:myOriginalHOP.call(obj,name)?endsWith__.test(name)?pumpkin:name==='toString'?pumpkin:isJSONContainer(obj)?(fastpathRead(obj,name),obj[name]):pumpkin:pumpkin)}function
enforceStaticPath(result,permitsUsed){forOwnKeys(permitsUsed,markFuncFreeze(function(name,subPermits){enforce(isFrozen(result),'Assumed frozen: ',result);if(name==='()');else
enforce(canReadPub(result,name),'Assumed readable: ',result,'.',name),inPub('()',subPermits)&&enforce(canCallPub(result,name),'Assumed callable: ',result,'.',name,'()'),enforceStaticPath(readPub(result,name),subPermits)}))}function
readImport(module_imports,name,opt_permitsUsed){var pumpkin={},result=readOwn(module_imports,name,pumpkin);return result===pumpkin?(log('Linkage warning: '+name+' not importable'),void
0):(opt_permitsUsed&&enforceStaticPath(result,opt_permitsUsed),result)}function
canInnocentEnum(obj,name){return name=String(name),!endsWith___.test(name)}function
canEnumPub(obj,name){return obj===null?false:obj===void 0?false:(name=String(name),obj[name+'_canEnum___']?true:endsWith__.test(name)?false:isJSONContainer(obj)?myOriginalHOP.call(obj,name)?(fastpathEnum(obj,name),name==='toString'||fastpathRead(obj,name),true):false:false)}function
canEnumOwn(obj,name){return name=String(name),obj&&obj[name+'_canEnum___']===obj?true:canEnumPub(obj,name)&&myOriginalHOP.call(obj,name)}function
Token(name){return name=String(name),primFreeze({'toString':markFuncFreeze(function
tokenToString(){return name}),'throwable___':true})}markFuncFreeze(Token),BREAK=Token('BREAK'),NO_RESULT=Token('NO_RESULT');function
forOwnKeys(obj,fn){var i,keys;fn=toFunc(fn),keys=ownKeys(obj);for(i=0;i<keys.length;++i)if(fn(keys[i],readPub(obj,keys[i]))===BREAK)return}function
forAllKeys(obj,fn){var i,keys;fn=toFunc(fn),keys=allKeys(obj);for(i=0;i<keys.length;++i)if(fn(keys[i],readPub(obj,keys[i]))===BREAK)return}function
ownKeys(obj){var result=[],i,k,len;if(isArray(obj)){len=obj.length;for(i=0;i<len;++i)result.push(i)}else{for(k
in obj)canEnumOwn(obj,k)&&result.push(k);obj!==void 0&&obj!==null&&obj.handleEnum___&&(result=result.concat(obj.handleEnum___(true)))}return result}function
allKeys(obj){var k,result;if(isArray(obj))return ownKeys(obj);result=[];for(k in
obj)canEnumPub(obj,k)&&result.push(k);return obj!==void 0&&obj!==null&&obj.handleEnum___&&(result=result.concat(obj.handleEnum___(false))),result}function
canCallPub(obj,name){var func;return obj===null?false:obj===void 0?false:(name=String(name),obj[name+'_canCall___']?true:obj[name+'_grantCall___']?(fastpathCall(obj,name),true):canReadPub(obj,name)?endsWith__.test(name)?false:name==='toString'?false:(func=obj[name],!isFunc(func)&&!isXo4aFunc(func)?false:(fastpathCall(obj,name),true)):false)}function
callPub(obj,name,args){name=String(name);if(obj===null||obj===void 0)throw new TypeError('Can\'t call '+name+' on '+obj);if(obj[name+'_canCall___']||canCallPub(obj,name))return obj[name].apply(obj,args);if(obj.handleCall___)return obj.handleCall___(name,args);fail('not callable:',debugReference(obj),'.',name)}function
canSetPub(obj,name){return name=String(name),canSet(obj,name)?true:endsWith__.test(name)?false:name==='valueOf'?false:name==='toString'?false:!isFrozen(obj)&&isJSONContainer(obj)}function
setPub(obj,name,val){if(typeof name==='number'&&name>=0&&obj instanceof Array&&obj.FROZEN___!==obj)return obj[name]=val;name=String(name);if(obj===null||obj===void
0)throw new TypeError('Can\'t set '+name+' on '+obj);return obj[name+'_canSet___']===obj?(obj[name]=val):canSetPub(obj,name)?(fastpathSet(obj,name),obj[name]=val):obj.handleSet___(name,val)}function
canSetStatic(fun,staticMemberName){return staticMemberName=''+staticMemberName,typeOf(fun)!=='function'?(log('Cannot set static member of non function: '+fun),false):isFrozen(fun)?(log('Cannot set static member of frozen function: '+fun),false):isFunc(fun)?staticMemberName==='toString'?false:endsWith__.test(staticMemberName)||staticMemberName==='valueOf'?(log('Illegal static member name: '+staticMemberName),false):staticMemberName
in fun?(log('Cannot override static member: '+staticMemberName),false):true:(log('Can only set static members on simple-functions: '+fun),false)}function
setStatic(fun,staticMemberName,staticMemberValue){staticMemberName=''+staticMemberName,canSetStatic(fun,staticMemberName)?(fun[staticMemberName]=staticMemberValue,fastpathEnum(fun,staticMemberName),fastpathRead(fun,staticMemberName)):fun.handleSet___(staticMemberName,staticMemberValue)}function
canDeletePub(obj,name){return name=String(name),isFrozen(obj)?false:endsWith__.test(name)?false:name==='valueOf'?false:name==='toString'?false:!!isJSONContainer(obj)}function
deletePub(obj,name){name=String(name);if(obj===null||obj===void 0)throw new TypeError('Can\'t delete '+name+' on '+obj);return canDeletePub(obj,name)?deleteFieldEntirely(obj,name):obj.handleDelete___(name)}function
deleteFieldEntirely(obj,name){return delete obj[name+'_canRead___'],delete obj[name+'_canEnum___'],delete
obj[name+'_canCall___'],delete obj[name+'_grantCall___'],delete obj[name+'_grantSet___'],delete
obj[name+'_canSet___'],delete obj[name+'_canDelete___'],delete obj[name]||(fail('not deleted: ',name),false)}USELESS=Token('USELESS');function
manifest(ignored){}stackInfoFields=['stack','fileName','lineNumer','description','stackTrace','sourceURL','line'];function
callStackUnsealer(ex){var i,k,numStackInfoFields,stackInfo;if(ex&&isInstanceOf(ex,Error)){stackInfo={},numStackInfoFields=stackInfoFields.length;for(i=0;i<numStackInfoFields;++i)k=stackInfoFields[i],k
in ex&&(stackInfo[k]=ex[k]);return'cajitaStack___'in ex&&(stackInfo.cajitaStack=ex.cajitaStack___),primFreeze(stackInfo)}return}function
tameException(ex){var name;if(ex&&ex.UNCATCHABLE___)throw ex;try{switch(typeOf(ex)){case'string':case'number':case'boolean':case'undefined':return ex;case'object':return ex===null?null:ex.throwable___?ex:isInstanceOf(ex,Error)?primFreeze(ex):''+ex;case'function':name=''+(ex.name||ex);function
inLieuOfThrownFunction(){return'In lieu of thrown function: '+name}return markFuncFreeze(inLieuOfThrownFunction,name);default:return log('Unrecognized exception type: '+typeOf(ex)),'Unrecognized exception type: '+typeOf(ex)}}catch(_){return log('Exception during exception handling.'),'Exception during exception handling.'}}function
primBeget(proto){var result;proto===null&&fail('Cannot beget from null.'),proto===void
0&&fail('Cannot beget from undefined.');function F(){}return F.prototype=proto,result=new
F,result.proto___=proto,result}function initializeMap(list){var result={},i;for(i=0;i<list.length;i+=2)setPub(result,list[i],asFirstClass(list[i+1]));return result}function
useGetHandler(obj,name,getHandler){obj[name+'_getter___']=getHandler}function useApplyHandler(obj,name,applyHandler){obj[name+'_handler___']=applyHandler}function
useCallHandler(obj,name,callHandler){useApplyHandler(obj,name,function callApplier(args){return callHandler.apply(this,args)})}function
useSetHandler(obj,name,setHandler){obj[name+'_setter___']=setHandler}function useDeleteHandler(obj,name,deleteHandler){obj[name+'_deleter___']=deleteHandler}function
grantFunc(obj,name){markFuncFreeze(obj[name],name),grantCall(obj,name),grantRead(obj,name)}function
grantGenericMethod(proto,name){var func=markXo4a(proto[name],name),pseudoFunc;grantCall(proto,name),pseudoFunc=tame(func),useGetHandler(proto,name,function
xo4aGetter(){return pseudoFunc})}function handleGenericMethod(obj,name,func){var
feral=obj[name],pseudoFunc;hasOwnProp(obj,name)?hasOwnProp(feral,'TAMED_TWIN___')&&(feral=func):(feral=func),useCallHandler(obj,name,func),pseudoFunc=tameXo4a.call(func),tamesTo(feral,pseudoFunc),useGetHandler(obj,name,function
genericGetter(){return pseudoFunc})}function grantTypedMethod(proto,name){var original=proto[name];handleGenericMethod(proto,name,function
guardedApplier(var_args){return inheritsFrom(this,proto)||fail('Can\'t call .',name,' on a non ',directConstructor(proto),': ',this),original.apply(this,arguments)})}function
grantMutatingMethod(proto,name){var original=proto[name];handleGenericMethod(proto,name,function
nonMutatingApplier(var_args){return isFrozen(this)&&fail('Can\'t .',name,' a frozen object'),original.apply(this,arguments)})}function
grantInnocentMethod(proto,name){var original=proto[name];handleGenericMethod(proto,name,function
guardedApplier(var_args){var feralThis=stopEscalation(untame(this)),feralArgs=untame(Array.slice(arguments,0)),feralResult=original.apply(feralThis,feralArgs);return tame(feralResult)})}function
enforceMatchable(regexp){isInstanceOf(regexp,RegExp)?isFrozen(regexp)&&fail('Can\'t match with frozen RegExp: ',regexp):enforceType(regexp,'string')}function
all2(func2,arg1,arg2s){var len=arg2s.length,i;for(i=0;i<len;i+=1)func2(arg1,arg2s[i])}all2(grantRead,Math,['E','LN10','LN2','LOG2E','LOG10E','PI','SQRT1_2','SQRT2']),all2(grantFunc,Math,['abs','acos','asin','atan','atan2','ceil','cos','exp','floor','log','max','min','pow','random','round','sin','sqrt','tan']);function
grantToString(proto){proto.TOSTRING___=tame(markXo4a(proto.toString,'toString'))}function
makeToStringMethod(toStringValue){function toStringMethod(var_args){var args=Array.slice(arguments,0),result,toStringValueApply;return isFunc(toStringValue)?toStringValue.apply(this,args):(toStringValueApply=readPub(toStringValue,'apply'),isFunc(toStringValueApply)?toStringValueApply.call(toStringValue,this,args):(result=myOriginalToString.call(this),log('Not correctly printed: '+result),result))}return toStringMethod}function
toStringGetter(){return hasOwnProp(this,'toString')&&typeOf(this.toString)==='function'&&!hasOwnProp(this,'TOSTRING___')&&grantToString(this),this.TOSTRING___}useGetHandler(Object.prototype,'toString',toStringGetter),useApplyHandler(Object.prototype,'toString',function
toStringApplier(args){var toStringValue=toStringGetter.call(this);return makeToStringMethod(toStringValue).apply(this,args)}),useSetHandler(Object.prototype,'toString',function
toStringSetter(toStringValue){var firstClassToStringValue;return isFrozen(this)||!isJSONContainer(this)?myKeeper.handleSet(this,'toString',toStringValue):(firstClassToStringValue=asFirstClass(toStringValue),this.TOSTRING___=firstClassToStringValue,this.toString=makeToStringMethod(firstClassToStringValue),toStringValue)}),useDeleteHandler(Object.prototype,'toString',function
toStringDeleter(){return isFrozen(this)||!isJSONContainer(this)?myKeeper.handleDelete(this,'toString'):delete
this.toString&&delete this.TOSTRING___}),markCtor(Object,void 0,'Object'),Object.prototype.TOSTRING___=tame(markXo4a(function(){return this.CLASS___?'[object '+this.CLASS___+']':myOriginalToString.call(this)},'toString')),all2(grantGenericMethod,Object.prototype,['toLocaleString','valueOf','isPrototypeOf']),grantRead(Object.prototype,'length'),handleGenericMethod(Object.prototype,'hasOwnProperty',function
hasOwnPropertyHandler(name){return hasOwnPropertyOf(this,name)}),handleGenericMethod(Object.prototype,'propertyIsEnumerable',function
propertyIsEnumerableHandler(name){return name=String(name),canEnumPub(this,name)}),useCallHandler(Object,'freeze',markFuncFreeze(freeze)),useGetHandler(Object,'freeze',function(){return freeze}),grantToString(Function.prototype),handleGenericMethod(Function.prototype,'apply',function
applyHandler(self,opt_args){return toFunc(this).apply(USELESS,opt_args||[])}),handleGenericMethod(Function.prototype,'call',function
callHandler(self,var_args){return toFunc(this).apply(USELESS,Array.slice(arguments,1))}),handleGenericMethod(Function.prototype,'bind',function
bindHandler(self,var_args){var thisFunc=toFunc(this),leftArgs=Array.slice(arguments,1);function
boundHandler(var_args){var args=leftArgs.concat(Array.slice(arguments,0));return thisFunc.apply(USELESS,args)}return markFuncFreeze(boundHandler)}),useGetHandler(Function.prototype,'caller',poisonFuncCaller),useGetHandler(Function.prototype,'arguments',poisonFuncArgs),markCtor(Array,Object,'Array'),grantFunc(Array,'slice'),grantToString(Array.prototype),all2(grantTypedMethod,Array.prototype,['toLocaleString']),all2(grantGenericMethod,Array.prototype,['concat','join','slice','indexOf','lastIndexOf']),all2(grantMutatingMethod,Array.prototype,['pop','push','reverse','shift','splice','unshift']),handleGenericMethod(Array.prototype,'sort',function
sortHandler(comparator){return isFrozen(this)&&fail('Can\'t sort a frozen array.'),comparator?Array.prototype.sort.call(this,toFunc(comparator)):Array.prototype.sort.call(this)}),markCtor(String,Object,'String'),grantFunc(String,'fromCharCode'),grantToString(String.prototype),all2(grantTypedMethod,String.prototype,['indexOf','lastIndexOf']),all2(grantGenericMethod,String.prototype,['charAt','charCodeAt','concat','localeCompare','slice','substr','substring','toLowerCase','toLocaleLowerCase','toUpperCase','toLocaleUpperCase']),handleGenericMethod(String.prototype,'match',function
matchHandler(regexp){return enforceMatchable(regexp),this.match(regexp)}),handleGenericMethod(String.prototype,'replace',function
replaceHandler(searcher,replacement){return enforceMatchable(searcher),isFunc(replacement)?(replacement=asFunc(replacement)):isPseudoFunc(replacement)?(replacement=toFunc(replacement)):(replacement=''+replacement),this.replace(searcher,replacement)}),handleGenericMethod(String.prototype,'search',function
searchHandler(regexp){return enforceMatchable(regexp),this.search(regexp)}),handleGenericMethod(String.prototype,'split',function
splitHandler(separator,limit){return enforceMatchable(separator),this.split(separator,limit)}),markCtor(Boolean,Object,'Boolean'),grantToString(Boolean.prototype),markCtor(Number,Object,'Number'),all2(grantRead,Number,['MAX_VALUE','MIN_VALUE','NaN','NEGATIVE_INFINITY','POSITIVE_INFINITY']),grantToString(Number.prototype),all2(grantTypedMethod,Number.prototype,['toLocaleString','toFixed','toExponential','toPrecision']),markCtor(Date,Object,'Date'),grantFunc(Date,'parse'),grantFunc(Date,'UTC'),grantToString(Date.prototype),all2(grantTypedMethod,Date.prototype,['toDateString','toTimeString','toUTCString','toLocaleString','toLocaleDateString','toLocaleTimeString','toISOString','toJSON','getDay','getUTCDay','getTimezoneOffset','getTime','getFullYear','getUTCFullYear','getMonth','getUTCMonth','getDate','getUTCDate','getHours','getUTCHours','getMinutes','getUTCMinutes','getSeconds','getUTCSeconds','getMilliseconds','getUTCMilliseconds']),all2(grantMutatingMethod,Date.prototype,['setTime','setFullYear','setUTCFullYear','setMonth','setUTCMonth','setDate','setUTCDate','setHours','setUTCHours','setMinutes','setUTCMinutes','setSeconds','setUTCSeconds','setMilliseconds','setUTCMilliseconds']),markCtor(RegExp,Object,'RegExp'),grantToString(RegExp.prototype),handleGenericMethod(RegExp.prototype,'exec',function
execHandler(specimen){return isFrozen(this)&&fail('Can\'t .exec a frozen RegExp'),specimen=String(specimen),this.exec(specimen)}),handleGenericMethod(RegExp.prototype,'test',function
testHandler(specimen){return isFrozen(this)&&fail('Can\'t .test a frozen RegExp'),specimen=String(specimen),this.test(specimen)}),all2(grantRead,RegExp.prototype,['source','global','ignoreCase','multiline','lastIndex']),markCtor(Error,Object,'Error'),grantToString(Error.prototype),grantRead(Error.prototype,'name'),grantRead(Error.prototype,'message'),markCtor(EvalError,Error,'EvalError'),markCtor(RangeError,Error,'RangeError'),markCtor(ReferenceError,Error,'ReferenceError'),markCtor(SyntaxError,Error,'SyntaxError'),markCtor(TypeError,Error,'TypeError'),markCtor(URIError,Error,'URIError');function
getNewModuleHandler(){return myNewModuleHandler}function setNewModuleHandler(newModuleHandler){myNewModuleHandler=newModuleHandler}obtainNewModule=freeze({'handle':markFuncFreeze(function
handleOnly(newModule){return newModule})});function registerClosureInspector(module){this&&this.CLOSURE_INSPECTOR___&&this.CLOSURE_INSPECTOR___.supportsCajaDebugging&&this.CLOSURE_INSPECTOR___.registerCajaModule(module)}function
makeNormalNewModuleHandler(){var imports=void 0,lastOutcome=void 0;function getImports(){return imports||(imports=copy(sharedImports)),imports}return freeze({'getImports':markFuncFreeze(getImports),'setImports':markFuncFreeze(function
setImports(newImports){imports=newImports}),'getLastOutcome':markFuncFreeze(function
getLastOutcome(){return lastOutcome}),'getLastValue':markFuncFreeze(function getLastValue(){return lastOutcome&&lastOutcome[0]?lastOutcome[1]:void
0}),'handle':markFuncFreeze(function handle(newModule){var outcome,result;registerClosureInspector(newModule),outcome=void
0;try{result=newModule.instantiate(___,getImports()),result!==NO_RESULT&&(outcome=[true,result])}catch(ex){outcome=[false,ex]}lastOutcome=outcome;if(outcome){if(outcome[0])return outcome[1];throw outcome[1]}return}),'handleUncaughtException':function
handleUncaughtException(exception,onerror,source,lineNum){var message,shouldReport;lastOutcome=[false,exception],message=tameException(exception),'object'===typeOf(exception)&&exception!==null&&(message=String(exception.message||exception.desc||message)),isPseudoFunc(onerror)&&(onerror=toFunc(onerror)),shouldReport=isFunc(onerror)?onerror.CALL___(message,String(source),String(lineNum)):onerror!==null,shouldReport!==false&&log(source+':'+lineNum+': '+message)}})}function
prepareModule(module,load){registerClosureInspector(module);function theModule(imports){var
completeImports=copy(sharedImports),k;completeImports.load=load;for(k in imports)hasOwnProp(imports,k)&&(completeImports[k]=imports[k]);return module.instantiate(___,primFreeze(completeImports))}return theModule.FUNC___='theModule',setStatic(theModule,'cajolerName',module.cajolerName),setStatic(theModule,'cajolerVersion',module.cajolerVersion),setStatic(theModule,'cajoledDate',module.cajoledDate),setStatic(theModule,'moduleURL',module.moduleURL),!module.includedModules||setStatic(theModule,'includedModules',___.freeze(module.includedModules)),primFreeze(theModule)}function
loadModule(module){return freeze(module),markFuncFreeze(module.instantiate),callPub(myNewModuleHandler,'handle',[module])}registeredImports=[];function
getId(imports){var id;return enforceType(imports,'object','imports'),'id___'in imports?(id=enforceType(imports.id___,'number','id')):(id=imports.id___=registeredImports.length),registeredImports[id]=imports,id}function
getImports(id){var result=registeredImports[enforceType(id,'number','id')];return result===void
0&&fail('imports#',id,' unregistered'),result}function unregister(imports){var id;enforceType(imports,'object','imports'),'id___'in
imports&&(id=enforceType(imports.id___,'number','id'),registeredImports[id]=void
0)}function identity(x){return x}function callWithEjector(attemptFunc,opt_failFunc){var
failFunc=opt_failFunc||identity,disabled=false,token=new Token('ejection'),stash;token.UNCATCHABLE___=true,stash=void
0;function ejector(result){if(disabled)cajita.fail('ejector disabled');else throw stash=result,token}markFuncFreeze(ejector);try{try{return callPub(attemptFunc,'call',[USELESS,ejector])}finally{disabled=true}}catch(e){if(e===token)return callPub(failFunc,'call',[USELESS,stash]);throw e}}function
eject(opt_ejector,result){opt_ejector?(callPub(opt_ejector,'call',[USELESS,result]),fail('Ejector did not exit: ',opt_ejector)):fail(result)}function
makeTrademark(typename,table){return typename=String(typename),primFreeze({'toString':markFuncFreeze(function(){return typename+'Mark'}),'stamp':primFreeze({'toString':markFuncFreeze(function(){return typename+'Stamp'}),'mark___':markFuncFreeze(function(obj){return table.set(obj,true),obj})}),'guard':{'toString':markFuncFreeze(function(){return typename+'T'}),'coerce':markFuncFreeze(function(specimen,opt_ejector){if(table.get(specimen))return specimen;eject(opt_ejector,'Specimen does not have the \"'+typename+'\" trademark')})}})}GuardMark=makeTrademark('Guard',newTable(true)),GuardT=GuardMark.guard,GuardStamp=GuardMark.stamp,primFreeze(GuardStamp.mark___(GuardT));function
Trademark(typename){var result=makeTrademark(typename,newTable(true));return primFreeze(GuardStamp.mark___(result.guard)),result}markFuncFreeze(Trademark);function
guard(g,specimen,opt_ejector){return g=GuardT.coerce(g),g.coerce(specimen,opt_ejector)}function
passesGuard(g,specimen){return g=GuardT.coerce(g),callWithEjector(markFuncFreeze(function(opt_ejector){return g.coerce(specimen,opt_ejector),true}),markFuncFreeze(function(ignored){return false}))}function
stamp(stamps,record){var i,numStamps;isRecord(record)||fail('Can only stamp records: ',record),isFrozen(record)&&fail('Can\'t stamp frozen objects: ',record),numStamps=stamps.length>>>0;for(i=0;i<numStamps;++i)'mark___'in
stamps[i]||fail('Can\'t stamp with a non-stamp: ',stamps[i]);for(i=0;i<numStamps;++i)stamps[i].mark___(record);return freeze(record)}function
makeSealerUnsealerPair(){var table=newTable(true),undefinedStandin={};function seal(payload){var
box;return payload===void 0&&(payload=undefinedStandin),box=Token('(box)'),table.set(box,payload),box}function
unseal(box){var payload=table.get(box);if(payload===void 0)fail('Sealer/Unsealer mismatch');else
if(payload===undefinedStandin)return;else return payload}return freeze({'seal':markFuncFreeze(seal),'unseal':markFuncFreeze(unseal)})}function
construct(ctor,args){var tmp;ctor=asCtor(ctor);switch(args.length){case 0:return new
ctor;case 1:return new ctor(args[0]);case 2:return new ctor(args[0],args[1]);case
3:return new ctor(args[0],args[1],args[2]);case 4:return new ctor(args[0],args[1],args[2],args[3]);case
5:return new ctor(args[0],args[1],args[2],args[3],args[4]);case 6:return new ctor(args[0],args[1],args[2],args[3],args[4],args[5]);case
7:return new ctor(args[0],args[1],args[2],args[3],args[4],args[5],args[6]);case 8:return new
ctor(args[0],args[1],args[2],args[3],args[4],args[5],args[6],args[7]);case 9:return new
ctor(args[0],args[1],args[2],args[3],args[4],args[5],args[6],args[7],args[8]);case
10:return new ctor(args[0],args[1],args[2],args[3],args[4],args[5],args[6],args[7],args[8],args[9]);case
11:return new ctor(args[0],args[1],args[2],args[3],args[4],args[5],args[6],args[7],args[8],args[9],args[10]);case
12:return new ctor(args[0],args[1],args[2],args[3],args[4],args[5],args[6],args[7],args[8],args[9],args[10],args[11]);default:return ctor.typeTag___==='Array'?ctor.apply(USELESS,args):(tmp=function(args){return ctor.apply(this,args)},tmp.prototype=ctor.prototype,new
tmp(args))}}magicCount=0,MAGIC_NUM=Math.random(),MAGIC_TOKEN=Token('MAGIC_TOKEN_FOR:'+MAGIC_NUM),MAGIC_NAME='_index;'+MAGIC_NUM+';';function
newTable(opt_useKeyLifetime,opt_expectedSize){var myMagicIndexName,myValues;++magicCount,myMagicIndexName=MAGIC_NAME+magicCount+'___';function
setOnKey(key,value){var ktype=typeof key,i,list;(!key||ktype!=='function'&&ktype!=='object')&&fail('Can\'t use key lifetime on primitive keys: ',key),list=key[myMagicIndexName];if(!list||list[0]!==key)key[myMagicIndexName]=[key,MAGIC_TOKEN,value];else{for(i=1;i<list.length;i+=2)if(list[i]===MAGIC_TOKEN)break;list[i]=MAGIC_TOKEN,list[i+1]=value}}function
getOnKey(key){var ktype=typeof key,i,list;(!key||ktype!=='function'&&ktype!=='object')&&fail('Can\'t use key lifetime on primitive keys: ',key),list=key[myMagicIndexName];if(!list||list[0]!==key)return;for(i=1;i<list.length;i+=2)if(list[i]===MAGIC_TOKEN)return list[i+1];return}if(opt_useKeyLifetime)return primFreeze({'set':markFuncFreeze(setOnKey),'get':markFuncFreeze(getOnKey)});myValues=[];function
setOnTable(key,value){var index;switch(typeof key){case'object':case'function':if(null===key)return myValues.prim_null=value,void
0;index=getOnKey(key);if(value===void 0){if(index===void 0)return;setOnKey(key,void
0)}else index===void 0&&(index=myValues.length,setOnKey(key,index));break;case'string':index='str_'+key;break;default:index='prim_'+key}value===void
0?delete myValues[index]:(myValues[index]=value)}function getOnTable(key){var index;switch(typeof
key){case'object':case'function':return null===key?myValues.prim_null:(index=getOnKey(key),void
0===index?void 0:myValues[index]);case'string':return myValues['str_'+key];default:return myValues['prim_'+key]}}return primFreeze({'set':markFuncFreeze(setOnTable),'get':markFuncFreeze(getOnTable)})}function
inheritsFrom(obj,allegedParent){if(null===obj)return false;if(void 0===obj)return false;if(typeOf(obj)==='function')return false;if(typeOf(allegedParent)!=='object')return false;if(null===allegedParent)return false;function
F(){}return F.prototype=allegedParent,Object(obj)instanceof F}function getSuperCtor(func){var
result;enforceType(func,'function');if(isCtor(func)||isFunc(func)){result=directConstructor(func.prototype);if(isCtor(result)||isFunc(result))return result}return}attribute=new
RegExp('^([\\s\\S]*)_(?:canRead|canCall|getter|handler)___$');function getOwnPropertyNames(obj){var
result=[],seen={},implicit=isJSONContainer(obj),base,k,match;for(k in obj)hasOwnProp(obj,k)&&(implicit&&!endsWith__.test(k)?myOriginalHOP.call(seen,k)||(seen[k]=true,result.push(k)):(match=attribute.exec(k),match!==null&&(base=match[1],myOriginalHOP.call(seen,base)||(seen[base]=true,result.push(base)))));return result}function
getProtoPropertyNames(func){return enforceType(func,'function'),getOwnPropertyNames(func.prototype)}function
getProtoPropertyValue(func,name){return asFirstClass(readPub(func.prototype,name))}function
beget(parent){var result;return isRecord(parent)||fail('Can only beget() records: ',parent),result=primBeget(parent),result.RECORD___=result,result}function
jsonParseOk(json){var x;try{return x=json.parse('{\"a\":3}'),x.a===3}catch(e){return false}}function
jsonStringifyOk(json){var x;try{return x=json.stringify({'a':3,'b__':4},function
replacer(k,v){return/__$/.test(k)?void 0:v}),x!=='{\"a\":3}'?false:(x=json.stringify(void
0,'invalid'),x===void 0)}catch(e){return false}}goodJSON={},goodJSON.parse=jsonParseOk(global.JSON)?global.JSON.parse:json_sans_eval.parse,goodJSON.stringify=jsonStringifyOk(global.JSON)?global.JSON.stringify:json_sans_eval.stringify,safeJSON=primFreeze({'CLASS___':'JSON','parse':markFuncFreeze(function(text,opt_reviver){var
reviver=void 0;return opt_reviver&&(opt_reviver=toFunc(opt_reviver),reviver=function(key,value){return opt_reviver.apply(this,arguments)}),goodJSON.parse(json_sans_eval.checkSyntax(text,function(key){return key!=='valueOf'&&key!=='toString'&&!endsWith__.test(key)}),reviver)}),'stringify':markFuncFreeze(function(obj,opt_replacer,opt_space){var
replacer;switch(typeof opt_space){case'number':case'string':case'undefined':break;default:throw new
TypeError('space must be a number or string')}return opt_replacer?(opt_replacer=toFunc(opt_replacer),replacer=function(key,value){return canReadPub(this,key)?opt_replacer.apply(this,arguments):void
0}):(replacer=function(key,value){return canReadPub(this,key)?value:void 0}),goodJSON.stringify(obj,replacer,opt_space)})}),cajita={'log':log,'fail':fail,'enforce':enforce,'enforceType':enforceType,'directConstructor':directConstructor,'getFuncCategory':getFuncCategory,'isDirectInstanceOf':isDirectInstanceOf,'isInstanceOf':isInstanceOf,'isRecord':isRecord,'isArray':isArray,'isJSONContainer':isJSONContainer,'freeze':freeze,'isFrozen':isFrozen,'copy':copy,'snapshot':snapshot,'canReadPub':canReadPub,'readPub':readPub,'hasOwnPropertyOf':hasOwnPropertyOf,'readOwn':readOwn,'canEnumPub':canEnumPub,'canEnumOwn':canEnumOwn,'canInnocentEnum':canInnocentEnum,'BREAK':BREAK,'allKeys':allKeys,'forAllKeys':forAllKeys,'ownKeys':ownKeys,'forOwnKeys':forOwnKeys,'canCallPub':canCallPub,'callPub':callPub,'canSetPub':canSetPub,'setPub':setPub,'canDeletePub':canDeletePub,'deletePub':deletePub,'Token':Token,'identical':identical,'newTable':newTable,'identity':identity,'callWithEjector':callWithEjector,'eject':eject,'GuardT':GuardT,'Trademark':Trademark,'guard':guard,'passesGuard':passesGuard,'stamp':stamp,'makeSealerUnsealerPair':makeSealerUnsealerPair,'USELESS':USELESS,'manifest':manifest,'args':args,'construct':construct,'inheritsFrom':inheritsFrom,'getSuperCtor':getSuperCtor,'getOwnPropertyNames':getOwnPropertyNames,'getProtoPropertyNames':getProtoPropertyNames,'getProtoPropertyValue':getProtoPropertyValue,'beget':beget,'PseudoFunctionProto':PseudoFunctionProto,'PseudoFunction':PseudoFunction,'isPseudoFunc':isPseudoFunc,'enforceNat':deprecate(enforceNat,'___.enforceNat','Use (x === x >>> 0) instead as a UInt32 test')},forOwnKeys(cajita,markFuncFreeze(function(k,v){switch(typeOf(v)){case'object':v!==null&&primFreeze(v);break;case'function':markFuncFreeze(v)}})),sharedImports={'cajita':cajita,'null':null,'false':false,'true':true,'NaN':NaN,'Infinity':Infinity,'undefined':void
0,'parseInt':markFuncFreeze(parseInt),'parseFloat':markFuncFreeze(parseFloat),'isNaN':markFuncFreeze(isNaN),'isFinite':markFuncFreeze(isFinite),'decodeURI':markFuncFreeze(decodeURI),'decodeURIComponent':markFuncFreeze(decodeURIComponent),'encodeURI':markFuncFreeze(encodeURI),'encodeURIComponent':markFuncFreeze(encodeURIComponent),'escape':escape?markFuncFreeze(escape):void
0,'Math':Math,'JSON':safeJSON,'Object':Object,'Array':Array,'String':String,'Boolean':Boolean,'Number':Number,'Date':Date,'RegExp':RegExp,'Error':Error,'EvalError':EvalError,'RangeError':RangeError,'ReferenceError':ReferenceError,'SyntaxError':SyntaxError,'TypeError':TypeError,'URIError':URIError},forOwnKeys(sharedImports,markFuncFreeze(function(k,v){switch(typeOf(v)){case'object':v!==null&&primFreeze(v);break;case'function':primFreeze(v)}})),primFreeze(sharedImports),___={'getLogFunc':getLogFunc,'setLogFunc':setLogFunc,'primFreeze':primFreeze,'canRead':canRead,'grantRead':grantRead,'canEnum':canEnum,'grantEnum':grantEnum,'canCall':canCall,'canSet':canSet,'grantSet':grantSet,'canDelete':canDelete,'grantDelete':grantDelete,'readImport':readImport,'isCtor':isCtor,'isFunc':isFunc,'markCtor':markCtor,'extend':extend,'markFuncFreeze':markFuncFreeze,'markXo4a':markXo4a,'markInnocent':markInnocent,'asFunc':asFunc,'toFunc':toFunc,'inPub':inPub,'canSetStatic':canSetStatic,'setStatic':setStatic,'typeOf':typeOf,'hasOwnProp':hasOwnProp,'deleteFieldEntirely':deleteFieldEntirely,'tameException':tameException,'primBeget':primBeget,'callStackUnsealer':callStackUnsealer,'RegExp':RegExp,'GuardStamp':GuardStamp,'asFirstClass':asFirstClass,'initializeMap':initializeMap,'iM':initializeMap,'useGetHandler':useGetHandler,'useSetHandler':useSetHandler,'grantFunc':grantFunc,'grantGenericMethod':grantGenericMethod,'handleGenericMethod':handleGenericMethod,'grantTypedMethod':grantTypedMethod,'grantMutatingMethod':grantMutatingMethod,'grantInnocentMethod':grantInnocentMethod,'enforceMatchable':enforceMatchable,'all2':all2,'tamesTo':tamesTo,'tamesToSelf':tamesToSelf,'tame':tame,'untame':untame,'getNewModuleHandler':getNewModuleHandler,'setNewModuleHandler':setNewModuleHandler,'obtainNewModule':obtainNewModule,'makeNormalNewModuleHandler':makeNormalNewModuleHandler,'loadModule':loadModule,'prepareModule':prepareModule,'NO_RESULT':NO_RESULT,'getId':getId,'getImports':getImports,'unregister':unregister,'grantEnumOnly':deprecate(grantEnum,'___.grantEnumOnly','Use ___.grantEnum instead.'),'grantCall':deprecate(grantGenericMethod,'___.grantCall','Choose a method tamer (e.g., ___.grantFunc,___.grantGenericMethod,etc) according to the safety properties of calling and reading the method.'),'grantGeneric':deprecate(grantGenericMethod,'___.grantGeneric','Use ___.grantGenericMethod instead.'),'useApplyHandler':deprecate(useApplyHandler,'___.useApplyHandler','Use ___.handleGenericMethod instead.'),'useCallHandler':deprecate(useCallHandler,'___.useCallHandler','Use ___.handleGenericMethod instead.'),'handleGeneric':deprecate(useCallHandler,'___.handleGeneric','Use ___.handleGenericMethod instead.'),'grantTypedGeneric':deprecate(useCallHandler,'___.grantTypedGeneric','Use ___.grantTypedMethod instead.'),'grantMutator':deprecate(useCallHandler,'___.grantMutator','Use ___.grantMutatingMethod instead.'),'useDeleteHandler':deprecate(useDeleteHandler,'___.useDeleteHandler','Refactor to avoid needing to handle deletions.'),'isXo4aFunc':deprecate(isXo4aFunc,'___.isXo4aFunc','Refactor to avoid needing to dynamically test whether a function is marked exophoric.'),'xo4a':deprecate(markXo4a,'___.xo4a','Consider refactoring to avoid needing to explicitly mark a function as exophoric. Use one of the exophoric method tamers (e.g., ___.grantGenericMethod) instead.Otherwise, use ___.markXo4a instead.'),'ctor':deprecate(markCtor,'___.ctor','Use ___.markCtor instead.'),'func':deprecate(markFuncFreeze,'___.func','___.func should not be called from manually written code.'),'frozenFunc':deprecate(markFuncFreeze,'___.frozenFunc','Use ___.markFuncFreeze instead.'),'markFuncOnly':deprecate(markFuncFreeze,'___.markFuncOnly','___.markFuncOnly should not be called from manually written code.'),'sharedImports':sharedImports},forOwnKeys(cajita,markFuncFreeze(function(k,v){k
in ___&&fail('internal: initialization conflict: ',k),typeOf(v)==='function'&&grantFunc(cajita,k),___[k]=v})),setNewModuleHandler(makeNormalNewModuleHandler())})(this),unicode={},unicode.BASE_CHAR='A-Za-z\xc0-\xd6\xd8-\xf6\xf8-\xff\u0100-\u0131\u0134-\u013e\u0141-\u0148\u014a-\u017e\u0180-\u01c3\u01cd-\u01f0\u01f4-\u01f5\u01fa-\u0217\u0250-\u02a8\u02bb-\u02c1\u0386\u0388-\u038a\u038c\u038e-\u03a1\u03a3-\u03ce\u03d0-\u03d6\u03da\u03dc\u03de\u03e0\u03e2-\u03f3\u0401-\u040c\u040e-\u044f\u0451-\u045c\u045e-\u0481\u0490-\u04c4\u04c7-\u04c8\u04cb-\u04cc\u04d0-\u04eb\u04ee-\u04f5\u04f8-\u04f9\u0531-\u0556\u0559\u0561-\u0586\u05d0-\u05ea\u05f0-\u05f2\u0621-\u063a\u0641-\u064a\u0671-\u06b7\u06ba-\u06be\u06c0-\u06ce\u06d0-\u06d3\u06d5\u06e5-\u06e6\u0905-\u0939\u093d\u0958-\u0961\u0985-\u098c\u098f-\u0990\u0993-\u09a8\u09aa-\u09b0\u09b2\u09b6-\u09b9\u09dc-\u09dd\u09df-\u09e1\u09f0-\u09f1\u0a05-\u0a0a\u0a0f-\u0a10\u0a13-\u0a28\u0a2a-\u0a30\u0a32-\u0a33\u0a35-\u0a36\u0a38-\u0a39\u0a59-\u0a5c\u0a5e\u0a72-\u0a74\u0a85-\u0a8b\u0a8d\u0a8f-\u0a91\u0a93-\u0aa8\u0aaa-\u0ab0\u0ab2-\u0ab3\u0ab5-\u0ab9\u0abd\u0ae0\u0b05-\u0b0c\u0b0f-\u0b10\u0b13-\u0b28\u0b2a-\u0b30\u0b32-\u0b33\u0b36-\u0b39\u0b3d\u0b5c-\u0b5d\u0b5f-\u0b61\u0b85-\u0b8a\u0b8e-\u0b90\u0b92-\u0b95\u0b99-\u0b9a\u0b9c\u0b9e-\u0b9f\u0ba3-\u0ba4\u0ba8-\u0baa\u0bae-\u0bb5\u0bb7-\u0bb9\u0c05-\u0c0c\u0c0e-\u0c10\u0c12-\u0c28\u0c2a-\u0c33\u0c35-\u0c39\u0c60-\u0c61\u0c85-\u0c8c\u0c8e-\u0c90\u0c92-\u0ca8\u0caa-\u0cb3\u0cb5-\u0cb9\u0cde\u0ce0-\u0ce1\u0d05-\u0d0c\u0d0e-\u0d10\u0d12-\u0d28\u0d2a-\u0d39\u0d60-\u0d61\u0e01-\u0e2e\u0e30\u0e32-\u0e33\u0e40-\u0e45\u0e81-\u0e82\u0e84\u0e87-\u0e88\u0e8a\u0e8d\u0e94-\u0e97\u0e99-\u0e9f\u0ea1-\u0ea3\u0ea5\u0ea7\u0eaa-\u0eab\u0ead-\u0eae\u0eb0\u0eb2-\u0eb3\u0ebd\u0ec0-\u0ec4\u0f40-\u0f47\u0f49-\u0f69\u10a0-\u10c5\u10d0-\u10f6\u1100\u1102-\u1103\u1105-\u1107\u1109\u110b-\u110c\u110e-\u1112\u113c\u113e\u1140\u114c\u114e\u1150\u1154-\u1155\u1159\u115f-\u1161\u1163\u1165\u1167\u1169\u116d-\u116e\u1172-\u1173\u1175\u119e\u11a8\u11ab\u11ae-\u11af\u11b7-\u11b8\u11ba\u11bc-\u11c2\u11eb\u11f0\u11f9\u1e00-\u1e9b\u1ea0-\u1ef9\u1f00-\u1f15\u1f18-\u1f1d\u1f20-\u1f45\u1f48-\u1f4d\u1f50-\u1f57\u1f59\u1f5b\u1f5d\u1f5f-\u1f7d\u1f80-\u1fb4\u1fb6-\u1fbc\u1fbe\u1fc2-\u1fc4\u1fc6-\u1fcc\u1fd0-\u1fd3\u1fd6-\u1fdb\u1fe0-\u1fec\u1ff2-\u1ff4\u1ff6-\u1ffc\u2126\u212a-\u212b\u212e\u2180-\u2182\u3041-\u3094\u30a1-\u30fa\u3105-\u312c\uac00-\ud7a3',unicode.IDEOGRAPHIC='\u4e00-\u9fa5\u3007\u3021-\u3029',unicode.LETTER=unicode.BASE_CHAR+unicode.IDEOGRAPHIC,unicode.COMBINING_CHAR='\u0300-\u0345\u0360-\u0361\u0483-\u0486\u0591-\u05a1\u05a3-\u05b9\u05bb-\u05bd\u05bf\u05c1-\u05c2\u05c4\u064b-\u0652\u0670\u06d6-\u06dc\u06dd-\u06df\u06e0-\u06e4\u06e7-\u06e8\u06ea-\u06ed\u0901-\u0903\u093c\u093e-\u094c\u094d\u0951-\u0954\u0962-\u0963\u0981-\u0983\u09bc\u09be\u09bf\u09c0-\u09c4\u09c7-\u09c8\u09cb-\u09cd\u09d7\u09e2-\u09e3\u0a02\u0a3c\u0a3e\u0a3f\u0a40-\u0a42\u0a47-\u0a48\u0a4b-\u0a4d\u0a70-\u0a71\u0a81-\u0a83\u0abc\u0abe-\u0ac5\u0ac7-\u0ac9\u0acb-\u0acd\u0b01-\u0b03\u0b3c\u0b3e-\u0b43\u0b47-\u0b48\u0b4b-\u0b4d\u0b56-\u0b57\u0b82-\u0b83\u0bbe-\u0bc2\u0bc6-\u0bc8\u0bca-\u0bcd\u0bd7\u0c01-\u0c03\u0c3e-\u0c44\u0c46-\u0c48\u0c4a-\u0c4d\u0c55-\u0c56\u0c82-\u0c83\u0cbe-\u0cc4\u0cc6-\u0cc8\u0cca-\u0ccd\u0cd5-\u0cd6\u0d02-\u0d03\u0d3e-\u0d43\u0d46-\u0d48\u0d4a-\u0d4d\u0d57\u0e31\u0e34-\u0e3a\u0e47-\u0e4e\u0eb1\u0eb4-\u0eb9\u0ebb-\u0ebc\u0ec8-\u0ecd\u0f18-\u0f19\u0f35\u0f37\u0f39\u0f3e\u0f3f\u0f71-\u0f84\u0f86-\u0f8b\u0f90-\u0f95\u0f97\u0f99-\u0fad\u0fb1-\u0fb7\u0fb9\u20d0-\u20dc\u20e1\u302a-\u302f\u3099\u309a',unicode.DIGIT='0-9\u0660-\u0669\u06f0-\u06f9\u0966-\u096f\u09e6-\u09ef\u0a66-\u0a6f\u0ae6-\u0aef\u0b66-\u0b6f\u0be7-\u0bef\u0c66-\u0c6f\u0ce6-\u0cef\u0d66-\u0d6f\u0e50-\u0e59\u0ed0-\u0ed9\u0f20-\u0f29',unicode.EXTENDER='\xb7\u02d0\u02d1\u0387\u0640\u0e46\u0ec6\u3005\u3031-\u3035\u309d-\u309e\u30fc-\u30fe',css={'properties':(function(){var
s=['|left|center|right','|top|center|bottom','#(?:[\\da-f]{3}){1,2}|aqua|black|blue|fuchsia|gray|green|lime|maroon|navy|olive|orange|purple|red|silver|teal|white|yellow|rgb\\(\\s*(?:-?\\d+|0|[+\\-]?\\d+(?:\\.\\d+)?%)\\s*,\\s*(?:-?\\d+|0|[+\\-]?\\d+(?:\\.\\d+)?%)\\s*,\\s*(?:-?\\d+|0|[+\\-]?\\d+(?:\\.\\d+)?%)\\)','[+\\-]?\\d+(?:\\.\\d+)?(?:[cem]m|ex|in|p[ctx])','\\d+(?:\\.\\d+)?(?:[cem]m|ex|in|p[ctx])','none|hidden|dotted|dashed|solid|double|groove|ridge|inset|outset','[+\\-]?\\d+(?:\\.\\d+)?%','\\d+(?:\\.\\d+)?%','url\\(\"[^()\\\\\"\\r\\n]+\"\\)','repeat-x|repeat-y|(?:repeat|space|round|no-repeat)(?:\\s+(?:repeat|space|round|no-repeat)){0,2}'],c=[RegExp('^\\s*(?:\\s*(?:0|'+s[3]+'|'+s[6]+')){1,2}\\s*$','i'),RegExp('^\\s*(?:\\s*(?:0|'+s[3]+'|'+s[6]+')){1,4}(?:\\s*\\/(?:\\s*(?:0|'+s[3]+'|'+s[6]+')){1,4})?\\s*$','i'),RegExp('^\\s*(?:\\s*none|(?:(?:\\s*(?:'+s[2]+')\\s+(?:0|'+s[3]+')(?:\\s*(?:0|'+s[3]+')){1,4}(?:\\s*inset)?|(?:\\s*inset)?\\s+(?:0|'+s[3]+')(?:\\s*(?:0|'+s[3]+')){1,4}(?:\\s*(?:'+s[2]+'))?)\\s*,)*(?:\\s*(?:'+s[2]+')\\s+(?:0|'+s[3]+')(?:\\s*(?:0|'+s[3]+')){1,4}(?:\\s*inset)?|(?:\\s*inset)?\\s+(?:0|'+s[3]+')(?:\\s*(?:0|'+s[3]+')){1,4}(?:\\s*(?:'+s[2]+'))?))\\s*$','i'),RegExp('^\\s*(?:'+s[2]+'|transparent|inherit)\\s*$','i'),RegExp('^\\s*(?:'+s[5]+'|inherit)\\s*$','i'),RegExp('^\\s*(?:thin|medium|thick|0|'+s[3]+'|inherit)\\s*$','i'),RegExp('^\\s*(?:(?:thin|medium|thick|0|'+s[3]+'|'+s[5]+'|'+s[2]+'|transparent|inherit)(?:\\s+(?:thin|medium|thick|0|'+s[3]+')|\\s+(?:'+s[5]+')|\\s*#(?:[\\da-f]{3}){1,2}|\\s+aqua|\\s+black|\\s+blue|\\s+fuchsia|\\s+gray|\\s+green|\\s+lime|\\s+maroon|\\s+navy|\\s+olive|\\s+orange|\\s+purple|\\s+red|\\s+silver|\\s+teal|\\s+white|\\s+yellow|\\s+rgb\\(\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\)|\\s+transparent|\\s+inherit){0,2}|inherit)\\s*$','i'),/^\s*(?:none|inherit)\s*$/i,RegExp('^\\s*(?:'+s[8]+'|none|inherit)\\s*$','i'),RegExp('^\\s*(?:0|'+s[3]+'|'+s[6]+'|auto|inherit)\\s*$','i'),RegExp('^\\s*(?:0|'+s[4]+'|'+s[7]+'|none|inherit|auto)\\s*$','i'),RegExp('^\\s*(?:0|'+s[4]+'|'+s[7]+'|inherit|auto)\\s*$','i'),/^\s*(?:0(?:\.\d+)?|\.\d+|1(?:\.0+)?|inherit)\s*$/i,RegExp('^\\s*(?:(?:'+s[2]+'|invert|inherit|'+s[5]+'|thin|medium|thick|0|'+s[3]+')(?:\\s*#(?:[\\da-f]{3}){1,2}|\\s+aqua|\\s+black|\\s+blue|\\s+fuchsia|\\s+gray|\\s+green|\\s+lime|\\s+maroon|\\s+navy|\\s+olive|\\s+orange|\\s+purple|\\s+red|\\s+silver|\\s+teal|\\s+white|\\s+yellow|\\s+rgb\\(\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\)|\\s+invert|\\s+inherit|\\s+(?:'+s[5]+'|inherit)|\\s+(?:thin|medium|thick|0|'+s[3]+'|inherit)){0,2}|inherit)\\s*$','i'),RegExp('^\\s*(?:'+s[2]+'|invert|inherit)\\s*$','i'),/^\s*(?:visible|hidden|scroll|auto|no-display|no-content)\s*$/i,RegExp('^\\s*(?:0|'+s[4]+'|'+s[7]+'|inherit)\\s*$','i'),/^\s*(?:auto|always|avoid|left|right|inherit)\s*$/i,RegExp('^\\s*(?:0|[+\\-]?\\d+(?:\\.\\d+)?m?s|'+s[6]+'|inherit)\\s*$','i'),/^\s*(?:0|[+\-]?\d+(?:\.\d+)?|inherit)\s*$/i,/^\s*(?:clip|ellipsis)\s*$/i,RegExp('^\\s*(?:normal|0|'+s[3]+'|inherit)\\s*$','i')];return{'-moz-border-radius':c[1],'-moz-border-radius-bottomleft':c[0],'-moz-border-radius-bottomright':c[0],'-moz-border-radius-topleft':c[0],'-moz-border-radius-topright':c[0],'-moz-box-shadow':c[2],'-moz-opacity':c[12],'-moz-outline':c[13],'-moz-outline-color':c[14],'-moz-outline-style':c[4],'-moz-outline-width':c[5],'-o-text-overflow':c[20],'-webkit-border-bottom-left-radius':c[0],'-webkit-border-bottom-right-radius':c[0],'-webkit-border-radius':c[1],'-webkit-border-radius-bottom-left':c[0],'-webkit-border-radius-bottom-right':c[0],'-webkit-border-radius-top-left':c[0],'-webkit-border-radius-top-right':c[0],'-webkit-border-top-left-radius':c[0],'-webkit-border-top-right-radius':c[0],'-webkit-box-shadow':c[2],'azimuth':/^\s*(?:0|[+\-]?\d+(?:\.\d+)?(?:g?rad|deg)|(?:left-side|far-left|left|center-left|center|center-right|right|far-right|right-side|behind)(?:\s+(?:left-side|far-left|left|center-left|center|center-right|right|far-right|right-side|behind))?|leftwards|rightwards|inherit)\s*$/i,'background':RegExp('^\\s*(?:\\s*(?:'+s[8]+'|none|(?:(?:0|'+s[6]+'|'+s[3]+s[0]+')(?:\\s+(?:0|'+s[6]+'|'+s[3]+s[1]+'))?|(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?)(?:\\s+(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?))?)(?:\\s*\\/\\s*(?:(?:0|'+s[4]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[4]+'|'+s[6]+'|auto)){0,2}|cover|contain))?|\\/\\s*(?:(?:0|'+s[4]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[4]+'|'+s[6]+'|auto)){0,2}|cover|contain)|'+s[9]+'|scroll|fixed|local|(?:border|padding|content)-box)(?:\\s*'+s[8]+'|\\s+none|(?:\\s+(?:0|'+s[6]+'|'+s[3]+s[0]+')(?:\\s+(?:0|'+s[6]+'|'+s[3]+s[1]+'))?|(?:\\s+(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?)){1,2})(?:\\s*\\/\\s*(?:(?:0|'+s[4]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[4]+'|'+s[6]+'|auto)){0,2}|cover|contain))?|\\s*\\/\\s*(?:(?:0|'+s[4]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[4]+'|'+s[6]+'|auto)){0,2}|cover|contain)|\\s+repeat-x|\\s+repeat-y|(?:\\s+(?:repeat|space|round|no-repeat)){1,2}|\\s+(?:scroll|fixed|local)|\\s+(?:border|padding|content)-box){0,4}\\s*,)*\\s*(?:'+s[2]+'|transparent|inherit|'+s[8]+'|none|(?:(?:0|'+s[6]+'|'+s[3]+s[0]+')(?:\\s+(?:0|'+s[6]+'|'+s[3]+s[1]+'))?|(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?)(?:\\s+(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?))?)(?:\\s*\\/\\s*(?:(?:0|'+s[4]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[4]+'|'+s[6]+'|auto)){0,2}|cover|contain))?|\\/\\s*(?:(?:0|'+s[4]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[4]+'|'+s[6]+'|auto)){0,2}|cover|contain)|'+s[9]+'|scroll|fixed|local|(?:border|padding|content)-box)(?:\\s*#(?:[\\da-f]{3}){1,2}|\\s+aqua|\\s+black|\\s+blue|\\s+fuchsia|\\s+gray|\\s+green|\\s+lime|\\s+maroon|\\s+navy|\\s+olive|\\s+orange|\\s+purple|\\s+red|\\s+silver|\\s+teal|\\s+white|\\s+yellow|\\s+rgb\\(\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\)|\\s+transparent|\\s+inherit|\\s*'+s[8]+'|\\s+none|(?:\\s+(?:0|'+s[6]+'|'+s[3]+s[0]+')(?:\\s+(?:0|'+s[6]+'|'+s[3]+s[1]+'))?|(?:\\s+(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?)){1,2})(?:\\s*\\/\\s*(?:(?:0|'+s[4]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[4]+'|'+s[6]+'|auto)){0,2}|cover|contain))?|\\s*\\/\\s*(?:(?:0|'+s[4]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[4]+'|'+s[6]+'|auto)){0,2}|cover|contain)|\\s+repeat-x|\\s+repeat-y|(?:\\s+(?:repeat|space|round|no-repeat)){1,2}|\\s+(?:scroll|fixed|local)|\\s+(?:border|padding|content)-box){0,5}\\s*$','i'),'background-attachment':/^\s*(?:scroll|fixed|local)(?:\s*,\s*(?:scroll|fixed|local))*\s*$/i,'background-color':c[3],'background-image':RegExp('^\\s*(?:'+s[8]+'|none)(?:\\s*,\\s*(?:'+s[8]+'|none))*\\s*$','i'),'background-position':RegExp('^\\s*(?:(?:0|'+s[6]+'|'+s[3]+s[0]+')(?:\\s+(?:0|'+s[6]+'|'+s[3]+s[1]+'))?|(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?)(?:\\s+(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?))?)(?:\\s*,\\s*(?:(?:0|'+s[6]+'|'+s[3]+s[0]+')(?:\\s+(?:0|'+s[6]+'|'+s[3]+s[1]+'))?|(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?)(?:\\s+(?:center|(?:lef|righ)t(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?|(?:top|bottom)(?:\\s+(?:0|'+s[6]+'|'+s[3]+'))?))?))*\\s*$','i'),'background-repeat':RegExp('^\\s*(?:'+s[9]+')(?:\\s*,\\s*(?:'+s[9]+'))*\\s*$','i'),'border':RegExp('^\\s*(?:(?:thin|medium|thick|0|'+s[3]+'|'+s[5]+'|'+s[2]+'|transparent)(?:\\s+(?:thin|medium|thick|0|'+s[3]+')|\\s+(?:'+s[5]+')|\\s*#(?:[\\da-f]{3}){1,2}|\\s+aqua|\\s+black|\\s+blue|\\s+fuchsia|\\s+gray|\\s+green|\\s+lime|\\s+maroon|\\s+navy|\\s+olive|\\s+orange|\\s+purple|\\s+red|\\s+silver|\\s+teal|\\s+white|\\s+yellow|\\s+rgb\\(\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\)|\\s+transparent){0,2}|inherit)\\s*$','i'),'border-bottom':c[6],'border-bottom-color':c[3],'border-bottom-left-radius':c[0],'border-bottom-right-radius':c[0],'border-bottom-style':c[4],'border-bottom-width':c[5],'border-collapse':/^\s*(?:collapse|separate|inherit)\s*$/i,'border-color':RegExp('^\\s*(?:(?:'+s[2]+'|transparent)(?:\\s*#(?:[\\da-f]{3}){1,2}|\\s+aqua|\\s+black|\\s+blue|\\s+fuchsia|\\s+gray|\\s+green|\\s+lime|\\s+maroon|\\s+navy|\\s+olive|\\s+orange|\\s+purple|\\s+red|\\s+silver|\\s+teal|\\s+white|\\s+yellow|\\s+rgb\\(\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\s*,\\s*(?:-?\\d+|0|'+s[6]+')\\)|\\s+transparent){0,4}|inherit)\\s*$','i'),'border-left':c[6],'border-left-color':c[3],'border-left-style':c[4],'border-left-width':c[5],'border-radius':c[1],'border-right':c[6],'border-right-color':c[3],'border-right-style':c[4],'border-right-width':c[5],'border-spacing':RegExp('^\\s*(?:(?:\\s*(?:0|'+s[3]+')){1,2}|\\s*inherit)\\s*$','i'),'border-style':RegExp('^\\s*(?:(?:'+s[5]+')(?:\\s+(?:'+s[5]+')){0,4}|inherit)\\s*$','i'),'border-top':c[6],'border-top-color':c[3],'border-top-left-radius':c[0],'border-top-right-radius':c[0],'border-top-style':c[4],'border-top-width':c[5],'border-width':RegExp('^\\s*(?:(?:thin|medium|thick|0|'+s[3]+')(?:\\s+(?:thin|medium|thick|0|'+s[3]+')){0,4}|inherit)\\s*$','i'),'bottom':c[9],'box-shadow':c[2],'caption-side':/^\s*(?:top|bottom|inherit)\s*$/i,'clear':/^\s*(?:none|left|right|both|inherit)\s*$/i,'clip':RegExp('^\\s*(?:rect\\(\\s*(?:0|'+s[3]+'|auto)\\s*,\\s*(?:0|'+s[3]+'|auto)\\s*,\\s*(?:0|'+s[3]+'|auto)\\s*,\\s*(?:0|'+s[3]+'|auto)\\)|auto|inherit)\\s*$','i'),'color':RegExp('^\\s*(?:'+s[2]+'|inherit)\\s*$','i'),'counter-increment':c[7],'counter-reset':c[7],'cue':RegExp('^\\s*(?:(?:'+s[8]+'|none|inherit)(?:\\s*'+s[8]+'|\\s+none|\\s+inherit)?|inherit)\\s*$','i'),'cue-after':c[8],'cue-before':c[8],'cursor':RegExp('^\\s*(?:(?:\\s*'+s[8]+'\\s*,)*\\s*(?:auto|crosshair|default|pointer|move|e-resize|ne-resize|nw-resize|n-resize|se-resize|sw-resize|s-resize|w-resize|text|wait|help|progress|all-scroll|col-resize|hand|no-drop|not-allowed|row-resize|vertical-text)|\\s*inherit)\\s*$','i'),'direction':/^\s*(?:ltr|rtl|inherit)\s*$/i,'display':/^\s*(?:inline|block|list-item|run-in|inline-block|table|inline-table|table-row-group|table-header-group|table-footer-group|table-row|table-column-group|table-column|table-cell|table-caption|none|inherit|-moz-inline-box|-moz-inline-stack)\s*$/i,'elevation':/^\s*(?:0|[+\-]?\d+(?:\.\d+)?(?:g?rad|deg)|below|level|above|higher|lower|inherit)\s*$/i,'empty-cells':/^\s*(?:show|hide|inherit)\s*$/i,'filter':RegExp('^\\s*(?:\\s*alpha\\(\\s*opacity\\s*=\\s*(?:0|'+s[6]+'|[+\\-]?\\d+(?:\\.\\d+)?)\\))+\\s*$','i'),'float':/^\s*(?:left|right|none|inherit)\s*$/i,'font':RegExp('^\\s*(?:(?:normal|italic|oblique|inherit|small-caps|bold|bolder|lighter|100|200|300|400|500|600|700|800|900)(?:\\s+(?:normal|italic|oblique|inherit|small-caps|bold|bolder|lighter|100|200|300|400|500|600|700|800|900)){0,2}\\s+(?:xx-small|x-small|small|medium|large|x-large|xx-large|(?:small|larg)er|0|'+s[4]+'|'+s[7]+'|inherit)(?:\\s*\\/\\s*(?:normal|0|\\d+(?:\\.\\d+)?|'+s[4]+'|'+s[7]+'|inherit))?(?:(?:\\s*\"\\w(?:[\\w-]*\\w)(?:\\s+\\w([\\w-]*\\w))*\"|\\s+(?:serif|sans-serif|cursive|fantasy|monospace))(?:\\s*,\\s*(?:\"\\w(?:[\\w-]*\\w)(?:\\s+\\w([\\w-]*\\w))*\"|serif|sans-serif|cursive|fantasy|monospace))*|\\s+inherit)|caption|icon|menu|message-box|small-caption|status-bar|inherit)\\s*$','i'),'font-family':/^\s*(?:(?:"\w(?:[\w-]*\w)(?:\s+\w([\w-]*\w))*"|serif|sans-serif|cursive|fantasy|monospace)(?:\s*,\s*(?:"\w(?:[\w-]*\w)(?:\s+\w([\w-]*\w))*"|serif|sans-serif|cursive|fantasy|monospace))*|inherit)\s*$/i,'font-size':RegExp('^\\s*(?:xx-small|x-small|small|medium|large|x-large|xx-large|(?:small|larg)er|0|'+s[4]+'|'+s[7]+'|inherit)\\s*$','i'),'font-stretch':/^\s*(?:normal|wider|narrower|ultra-condensed|extra-condensed|condensed|semi-condensed|semi-expanded|expanded|extra-expanded|ultra-expanded)\s*$/i,'font-style':/^\s*(?:normal|italic|oblique|inherit)\s*$/i,'font-variant':/^\s*(?:normal|small-caps|inherit)\s*$/i,'font-weight':/^\s*(?:normal|bold|bolder|lighter|100|200|300|400|500|600|700|800|900|inherit)\s*$/i,'height':c[9],'left':c[9],'letter-spacing':c[21],'line-height':RegExp('^\\s*(?:normal|0|\\d+(?:\\.\\d+)?|'+s[4]+'|'+s[7]+'|inherit)\\s*$','i'),'list-style':RegExp('^\\s*(?:(?:disc|circle|square|decimal|decimal-leading-zero|lower-roman|upper-roman|lower-greek|lower-latin|upper-latin|armenian|georgian|lower-alpha|upper-alpha|none|inherit|inside|outside|'+s[8]+')(?:\\s+(?:disc|circle|square|decimal|decimal-leading-zero|lower-roman|upper-roman|lower-greek|lower-latin|upper-latin|armenian|georgian|lower-alpha|upper-alpha|none|inherit)|\\s+(?:inside|outside|inherit)|\\s*'+s[8]+'|\\s+none|\\s+inherit){0,2}|inherit)\\s*$','i'),'list-style-image':c[8],'list-style-position':/^\s*(?:inside|outside|inherit)\s*$/i,'list-style-type':/^\s*(?:disc|circle|square|decimal|decimal-leading-zero|lower-roman|upper-roman|lower-greek|lower-latin|upper-latin|armenian|georgian|lower-alpha|upper-alpha|none|inherit)\s*$/i,'margin':RegExp('^\\s*(?:(?:0|'+s[3]+'|'+s[6]+'|auto)(?:\\s+(?:0|'+s[3]+'|'+s[6]+'|auto)){0,4}|inherit)\\s*$','i'),'margin-bottom':c[9],'margin-left':c[9],'margin-right':c[9],'margin-top':c[9],'max-height':c[10],'max-width':c[10],'min-height':c[11],'min-width':c[11],'opacity':c[12],'outline':c[13],'outline-color':c[14],'outline-style':c[4],'outline-width':c[5],'overflow':/^\s*(?:visible|hidden|scroll|auto|inherit)\s*$/i,'overflow-x':c[15],'overflow-y':c[15],'padding':RegExp('^\\s*(?:(?:\\s*(?:0|'+s[4]+'|'+s[7]+')){1,4}|\\s*inherit)\\s*$','i'),'padding-bottom':c[16],'padding-left':c[16],'padding-right':c[16],'padding-top':c[16],'page-break-after':c[17],'page-break-before':c[17],'page-break-inside':/^\s*(?:avoid|auto|inherit)\s*$/i,'pause':RegExp('^\\s*(?:(?:\\s*(?:0|[+\\-]?\\d+(?:\\.\\d+)?m?s|'+s[6]+')){1,2}|\\s*inherit)\\s*$','i'),'pause-after':c[18],'pause-before':c[18],'pitch':/^\s*(?:0|\d+(?:\.\d+)?k?Hz|x-low|low|medium|high|x-high|inherit)\s*$/i,'pitch-range':c[19],'play-during':RegExp('^\\s*(?:'+s[8]+'\\s*(?:mix|repeat)(?:\\s+(?:mix|repeat))?|auto|none|inherit)\\s*$','i'),'position':/^\s*(?:static|relative|absolute|inherit)\s*$/i,'quotes':c[7],'richness':c[19],'right':c[9],'speak':/^\s*(?:normal|none|spell-out|inherit)\s*$/i,'speak-header':/^\s*(?:once|always|inherit)\s*$/i,'speak-numeral':/^\s*(?:digits|continuous|inherit)\s*$/i,'speak-punctuation':/^\s*(?:code|none|inherit)\s*$/i,'speech-rate':/^\s*(?:0|[+\-]?\d+(?:\.\d+)?|x-slow|slow|medium|fast|x-fast|faster|slower|inherit)\s*$/i,'stress':c[19],'table-layout':/^\s*(?:auto|fixed|inherit)\s*$/i,'text-align':/^\s*(?:left|right|center|justify|inherit)\s*$/i,'text-decoration':/^\s*(?:none|(?:underline|overline|line-through|blink)(?:\s+(?:underline|overline|line-through|blink)){0,3}|inherit)\s*$/i,'text-indent':RegExp('^\\s*(?:0|'+s[3]+'|'+s[6]+'|inherit)\\s*$','i'),'text-overflow':c[20],'text-shadow':c[2],'text-transform':/^\s*(?:capitalize|uppercase|lowercase|none|inherit)\s*$/i,'text-wrap':/^\s*(?:normal|unrestricted|none|suppress)\s*$/i,'top':c[9],'unicode-bidi':/^\s*(?:normal|embed|bidi-override|inherit)\s*$/i,'vertical-align':RegExp('^\\s*(?:baseline|sub|super|top|text-top|middle|bottom|text-bottom|0|'+s[6]+'|'+s[3]+'|inherit)\\s*$','i'),'visibility':/^\s*(?:visible|hidden|collapse|inherit)\s*$/i,'voice-family':/^\s*(?:(?:\s*(?:"\w(?:[\w-]*\w)(?:\s+\w([\w-]*\w))*"|male|female|child)\s*,)*\s*(?:"\w(?:[\w-]*\w)(?:\s+\w([\w-]*\w))*"|male|female|child)|\s*inherit)\s*$/i,'volume':RegExp('^\\s*(?:0|\\d+(?:\\.\\d+)?|'+s[7]+'|silent|x-soft|soft|medium|loud|x-loud|inherit)\\s*$','i'),'white-space':/^\s*(?:normal|pre|nowrap|pre-wrap|pre-line|inherit|-o-pre-wrap|-moz-pre-wrap|-pre-wrap)\s*$/i,'width':RegExp('^\\s*(?:0|'+s[4]+'|'+s[7]+'|auto|inherit)\\s*$','i'),'word-spacing':c[21],'word-wrap':/^\s*(?:normal|break-word)\s*$/i,'z-index':/^\s*(?:auto|-?\d+|inherit)\s*$/i,'zoom':RegExp('^\\s*(?:normal|0|\\d+(?:\\.\\d+)?|'+s[7]+')\\s*$','i')}})(),'alternates':{'MozBoxShadow':['boxShadow'],'WebkitBoxShadow':['boxShadow'],'float':['cssFloat','styleFloat']},'HISTORY_INSENSITIVE_STYLE_WHITELIST':{'display':true,'filter':true,'float':true,'height':true,'left':true,'opacity':true,'overflow':true,'position':true,'right':true,'top':true,'visibility':true,'width':true,'padding-left':true,'padding-right':true,'padding-top':true,'padding-bottom':true}},html4={},html4
.atype={'NONE':0,'URI':1,'URI_FRAGMENT':11,'SCRIPT':2,'STYLE':3,'ID':4,'IDREF':5,'IDREFS':6,'GLOBAL_NAME':7,'LOCAL_NAME':8,'CLASSES':9,'FRAME_TARGET':10},html4
.ATTRIBS={'*::class':9,'*::dir':0,'*::id':4,'*::lang':0,'*::onclick':2,'*::ondblclick':2,'*::onkeydown':2,'*::onkeypress':2,'*::onkeyup':2,'*::onload':2,'*::onmousedown':2,'*::onmousemove':2,'*::onmouseout':2,'*::onmouseover':2,'*::onmouseup':2,'*::style':3,'*::title':0,'a::accesskey':0,'a::coords':0,'a::href':1,'a::hreflang':0,'a::name':7,'a::onblur':2,'a::onfocus':2,'a::rel':0,'a::rev':0,'a::shape':0,'a::tabindex':0,'a::target':10,'a::type':0,'area::accesskey':0,'area::alt':0,'area::coords':0,'area::href':1,'area::nohref':0,'area::onblur':2,'area::onfocus':2,'area::shape':0,'area::tabindex':0,'area::target':10,'bdo::dir':0,'blockquote::cite':1,'br::clear':0,'button::accesskey':0,'button::disabled':0,'button::name':8,'button::onblur':2,'button::onfocus':2,'button::tabindex':0,'button::type':0,'button::value':0,'caption::align':0,'col::align':0,'col::char':0,'col::charoff':0,'col::span':0,'col::valign':0,'col::width':0,'colgroup::align':0,'colgroup::char':0,'colgroup::charoff':0,'colgroup::span':0,'colgroup::valign':0,'colgroup::width':0,'del::cite':1,'del::datetime':0,'dir::compact':0,'div::align':0,'dl::compact':0,'font::color':0,'font::face':0,'font::size':0,'form::accept':0,'form::action':1,'form::autocomplete':0,'form::enctype':0,'form::method':0,'form::name':7,'form::onreset':2,'form::onsubmit':2,'form::target':10,'h1::align':0,'h2::align':0,'h3::align':0,'h4::align':0,'h5::align':0,'h6::align':0,'hr::align':0,'hr::noshade':0,'hr::size':0,'hr::width':0,'iframe::align':0,'iframe::frameborder':0,'iframe::height':0,'iframe::marginheight':0,'iframe::marginwidth':0,'iframe::width':0,'img::align':0,'img::alt':0,'img::border':0,'img::height':0,'img::hspace':0,'img::ismap':0,'img::name':7,'img::src':1,'img::usemap':11,'img::vspace':0,'img::width':0,'input::accept':0,'input::accesskey':0,'input::align':0,'input::alt':0,'input::autocomplete':0,'input::checked':0,'input::disabled':0,'input::ismap':0,'input::maxlength':0,'input::name':8,'input::onblur':2,'input::onchange':2,'input::onfocus':2,'input::onselect':2,'input::readonly':0,'input::size':0,'input::src':1,'input::tabindex':0,'input::type':0,'input::usemap':11,'input::value':0,'ins::cite':1,'ins::datetime':0,'label::accesskey':0,'label::for':5,'label::onblur':2,'label::onfocus':2,'legend::accesskey':0,'legend::align':0,'li::type':0,'li::value':0,'map::name':7,'menu::compact':0,'ol::compact':0,'ol::start':0,'ol::type':0,'optgroup::disabled':0,'optgroup::label':0,'option::disabled':0,'option::label':0,'option::selected':0,'option::value':0,'p::align':0,'pre::width':0,'q::cite':1,'select::disabled':0,'select::multiple':0,'select::name':8,'select::onblur':2,'select::onchange':2,'select::onfocus':2,'select::size':0,'select::tabindex':0,'table::align':0,'table::bgcolor':0,'table::border':0,'table::cellpadding':0,'table::cellspacing':0,'table::frame':0,'table::rules':0,'table::summary':0,'table::width':0,'tbody::align':0,'tbody::char':0,'tbody::charoff':0,'tbody::valign':0,'td::abbr':0,'td::align':0,'td::axis':0,'td::bgcolor':0,'td::char':0,'td::charoff':0,'td::colspan':0,'td::headers':6,'td::height':0,'td::nowrap':0,'td::rowspan':0,'td::scope':0,'td::valign':0,'td::width':0,'textarea::accesskey':0,'textarea::cols':0,'textarea::disabled':0,'textarea::name':8,'textarea::onblur':2,'textarea::onchange':2,'textarea::onfocus':2,'textarea::onselect':2,'textarea::readonly':0,'textarea::rows':0,'textarea::tabindex':0,'tfoot::align':0,'tfoot::char':0,'tfoot::charoff':0,'tfoot::valign':0,'th::abbr':0,'th::align':0,'th::axis':0,'th::bgcolor':0,'th::char':0,'th::charoff':0,'th::colspan':0,'th::headers':6,'th::height':0,'th::nowrap':0,'th::rowspan':0,'th::scope':0,'th::valign':0,'th::width':0,'thead::align':0,'thead::char':0,'thead::charoff':0,'thead::valign':0,'tr::align':0,'tr::bgcolor':0,'tr::char':0,'tr::charoff':0,'tr::valign':0,'ul::compact':0,'ul::type':0},html4
.eflags={'OPTIONAL_ENDTAG':1,'EMPTY':2,'CDATA':4,'RCDATA':8,'UNSAFE':16,'FOLDABLE':32,'SCRIPT':64,'STYLE':128},html4
.ELEMENTS={'a':0,'abbr':0,'acronym':0,'address':0,'applet':16,'area':2,'b':0,'base':18,'basefont':18,'bdo':0,'big':0,'blockquote':0,'body':49,'br':2,'button':0,'caption':0,'center':0,'cite':0,'code':0,'col':2,'colgroup':1,'dd':1,'del':0,'dfn':0,'dir':0,'div':0,'dl':0,'dt':1,'em':0,'fieldset':0,'font':0,'form':0,'frame':18,'frameset':16,'h1':0,'h2':0,'h3':0,'h4':0,'h5':0,'h6':0,'head':49,'hr':2,'html':49,'i':0,'iframe':4,'img':2,'input':2,'ins':0,'isindex':18,'kbd':0,'label':0,'legend':0,'li':1,'link':18,'map':0,'menu':0,'meta':18,'noframes':20,'noscript':20,'object':16,'ol':0,'optgroup':0,'option':1,'p':1,'param':18,'pre':0,'q':0,'s':0,'samp':0,'script':84,'select':0,'small':0,'span':0,'strike':0,'strong':0,'style':148,'sub':0,'sup':0,'table':0,'tbody':1,'td':1,'textarea':8,'tfoot':1,'th':1,'thead':1,'title':24,'tr':1,'tt':0,'u':0,'ul':0,'var':0},html=(function(){var
ENTITIES,INSIDE_TAG_TOKEN,OUTSIDE_TAG_TOKEN,ampRe,decimalEscapeRe,entityRe,eqRe,gtRe,hexEscapeRe,lcase,looseAmpRe,ltRe,nulRe,quotRe;'script'==='SCRIPT'.toLowerCase()?(lcase=function(s){return s.toLowerCase()}):(lcase=function(s){return s.replace(/[A-Z]/g,function(ch){return String.fromCharCode(ch.charCodeAt(0)|32)})}),ENTITIES={'lt':'<','gt':'>','amp':'&','nbsp':'\xa0','quot':'\"','apos':'\''},decimalEscapeRe=/^#(\d+)$/,hexEscapeRe=/^#x([0-9A-Fa-f]+)$/;function
lookupEntity(name){var m;return name=lcase(name),ENTITIES.hasOwnProperty(name)?ENTITIES[name]:(m=name.match(decimalEscapeRe),m?String.fromCharCode(parseInt(m[1],10)):(m=name.match(hexEscapeRe))?String.fromCharCode(parseInt(m[1],16)):'')}function
decodeOneEntity(_,name){return lookupEntity(name)}nulRe=/\0/g;function stripNULs(s){return s.replace(nulRe,'')}entityRe=/&(#\d+|#x[0-9A-Fa-f]+|\w+);/g;function
unescapeEntities(s){return s.replace(entityRe,decodeOneEntity)}ampRe=/&/g,looseAmpRe=/&([^a-z#]|#(?:[^0-9x]|x(?:[^0-9a-f]|$)|$)|$)/gi,ltRe=/</g,gtRe=/>/g,quotRe=/\"/g,eqRe=/\=/g;function
escapeAttrib(s){return s.replace(ampRe,'&amp;').replace(ltRe,'&lt;').replace(gtRe,'&gt;').replace(quotRe,'&#34;').replace(eqRe,'&#61;')}function
normalizeRCData(rcdata){return rcdata.replace(looseAmpRe,'&amp;$1').replace(ltRe,'&lt;').replace(gtRe,'&gt;')}INSIDE_TAG_TOKEN=new
RegExp('^\\s*(?:(?:([a-z][a-z-]*)(\\s*=\\s*(\"[^\"]*\"|\'[^\']*\'|(?=[a-z][a-z-]*\\s*=)|[^>\"\'\\s]*))?)|(/?>)|.[^a-z\\s>]*)','i'),OUTSIDE_TAG_TOKEN=new
RegExp('^(?:&(\\#[0-9]+|\\#[x][0-9a-f]+|\\w+);|<!--[\\s\\S]*?-->|<!\\w[^>]*>|<\\?[^>*]*>|<(/)?([a-z][a-z0-9]*)|([^<&>]+)|([<&>]))','i');function
makeSaxParser(handler){return function parse(htmlText,param){var attribName,attribs,dataEnd,decodedValue,eflags,encodedValue,htmlLower,inTag,m,openTag,tagName;htmlText=String(htmlText),htmlLower=null,inTag=false,attribs=[],tagName=void
0,eflags=void 0,openTag=void 0,handler.startDoc&&handler.startDoc(param);while(htmlText){m=htmlText.match(inTag?INSIDE_TAG_TOKEN:OUTSIDE_TAG_TOKEN),htmlText=htmlText.substring(m[0].length);if(inTag){if(m[1]){attribName=lcase(m[1]);if(m[2]){encodedValue=m[3];switch(encodedValue.charCodeAt(0)){case
34:case 39:encodedValue=encodedValue.substring(1,encodedValue.length-1)}decodedValue=unescapeEntities(stripNULs(encodedValue))}else
decodedValue=attribName;attribs.push(attribName,decodedValue)}else if(m[4])eflags!==void
0&&(openTag?handler.startTag&&handler.startTag(tagName,attribs,param):handler.endTag&&handler.endTag(tagName,param)),openTag&&eflags&(html4
.eflags.CDATA|html4 .eflags.RCDATA)&&(htmlLower===null?(htmlLower=lcase(htmlText)):(htmlLower=htmlLower.substring(htmlLower.length-htmlText.length)),dataEnd=htmlLower.indexOf('</'+tagName),dataEnd<0&&(dataEnd=htmlText.length),eflags&html4
.eflags.CDATA?handler.cdata&&handler.cdata(htmlText.substring(0,dataEnd),param):handler.rcdata&&handler.rcdata(normalizeRCData(htmlText.substring(0,dataEnd)),param),htmlText=htmlText.substring(dataEnd)),tagName=eflags=openTag=void
0,attribs.length=0,inTag=false}else if(m[1])handler.pcdata&&handler.pcdata(m[0],param);else
if(m[3])openTag=!m[2],inTag=true,tagName=lcase(m[3]),eflags=html4 .ELEMENTS.hasOwnProperty(tagName)?html4
.ELEMENTS[tagName]:void 0;else if(m[4])handler.pcdata&&handler.pcdata(m[4],param);else
if(m[5]){if(handler.pcdata)switch(m[5]){case'<':handler.pcdata('&lt;',param);break;case'>':handler.pcdata('&gt;',param);break;default:handler.pcdata('&amp;',param)}}}handler.endDoc&&handler.endDoc(param)}}return{'normalizeRCData':normalizeRCData,'escapeAttrib':escapeAttrib,'unescapeEntities':unescapeEntities,'makeSaxParser':makeSaxParser}})(),html.makeHtmlSanitizer=function(sanitizeAttributes){var
ignoring,stack;return html.makeSaxParser({'startDoc':function(_){stack=[],ignoring=false},'startTag':function(tagName,attribs,out){var
attribName,eflags,i,n,value;if(ignoring)return;if(!html4 .ELEMENTS.hasOwnProperty(tagName))return;eflags=html4
.ELEMENTS[tagName];if(eflags&html4 .eflags.FOLDABLE)return;else if(eflags&html4 .eflags.UNSAFE)return ignoring=!(eflags&html4
.eflags.EMPTY),void 0;attribs=sanitizeAttributes(tagName,attribs);if(attribs){eflags&html4
.eflags.EMPTY||stack.push(tagName),out.push('<',tagName);for(i=0,n=attribs.length;i<n;i+=2)attribName=attribs[i],value=attribs[i+1],value!==null&&value!==void
0&&out.push(' ',attribName,'=\"',html.escapeAttrib(value),'\"');out.push('>')}},'endTag':function(tagName,out){var
eflags,i,index,stackEl;if(ignoring)return ignoring=false,void 0;if(!html4 .ELEMENTS.hasOwnProperty(tagName))return;eflags=html4
.ELEMENTS[tagName];if(!(eflags&(html4 .eflags.UNSAFE|html4 .eflags.EMPTY|html4 .eflags.FOLDABLE))){if(eflags&html4
.eflags.OPTIONAL_ENDTAG)for(index=stack.length;--index>=0;){stackEl=stack[index];if(stackEl===tagName)break;if(!(html4
.ELEMENTS[stackEl]&html4 .eflags.OPTIONAL_ENDTAG))return}else for(index=stack.length;--index>=0;)if(stack[index]===tagName)break;if(index<0)return;for(i=stack.length;--i>index;)stackEl=stack[i],html4
.ELEMENTS[stackEl]&html4 .eflags.OPTIONAL_ENDTAG||out.push('</',stackEl,'>');stack.length=index,out.push('</',tagName,'>')}},'pcdata':function(text,out){ignoring||out.push(text)},'rcdata':function(text,out){ignoring||out.push(text)},'cdata':function(text,out){ignoring||out.push(text)},'endDoc':function(out){var
i;for(i=stack.length;--i>=0;)out.push('</',stack[i],'>');stack.length=0}})};function
html_sanitize(htmlText,opt_uriPolicy,opt_nmTokenPolicy){var out=[];return html.makeHtmlSanitizer(function
sanitizeAttribs(tagName,attribs){var attribKey,attribName,atype,i,value;for(i=0;i<attribs.length;i+=2){attribName=attribs[i],value=attribs[i+1],atype=null,((attribKey=tagName+'::'+attribName,html4
.ATTRIBS.hasOwnProperty(attribKey))||(attribKey='*::'+attribName,html4 .ATTRIBS.hasOwnProperty(attribKey)))&&(atype=html4
.ATTRIBS[attribKey]);if(atype!==null)switch(atype){case html4 .atype.NONE:break;case
html4 .atype.SCRIPT:case html4 .atype.STYLE:value=null;break;case html4 .atype.ID:case
html4 .atype.IDREF:case html4 .atype.IDREFS:case html4 .atype.GLOBAL_NAME:case html4
.atype.LOCAL_NAME:case html4 .atype.CLASSES:value=opt_nmTokenPolicy?opt_nmTokenPolicy(value):value;break;case
html4 .atype.URI:value=opt_uriPolicy&&opt_uriPolicy(value);break;case html4 .atype.URI_FRAGMENT:value&&'#'===value.charAt(0)?(value=opt_nmTokenPolicy?opt_nmTokenPolicy(value):value,value&&(value='#'+value)):(value=null);break;default:value=null}else
value=null;attribs[i+1]=value}return attribs})(htmlText,out),out.join('')}function
HtmlEmitter(base,opt_tameDocument){var arraySplice,bridal,detached,idMap,insertionPoint;if(!base)throw new
Error('Host page error: Virtual document element was not provided');insertionPoint=base,bridal=bridalMaker(base.ownerDocument),detached=null,idMap=null,arraySplice=Array.prototype.splice;function
buildIdMap(){var desc,descs,i,id;idMap={},descs=base.getElementsByTagName('*');for(i=0;desc=descs[i];++i)id=desc.getAttributeNode('id'),id&&id.value&&(idMap[id.value]=desc)}function
byId(id){var node;idMap||buildIdMap(),node=idMap[id];if(node)return node;for(;node=base.ownerDocument.getElementById(id);){if(base.contains?base.contains(node):base.compareDocumentPosition(node)&16)return idMap[id]=node,node;node.id=''}return null}function
emitStatic(htmlString){if(!base)throw new Error('Host page error: HtmlEmitter.emitStatic called after document finish()ed');if(base.firstChild)throw new
Error('Host page error: Virtual document element is not empty');base.innerHTML=htmlString}function
detachOnto(limit,out){var anc,child,greatAnc,next,sibling;for(child=limit.firstChild;child;child=next)next=child.nextSibling,out.push(child,limit),limit.removeChild(child);for(anc=limit;anc&&anc!==base;anc=greatAnc){greatAnc=anc.parentNode;for(sibling=anc.nextSibling;sibling;sibling=next)next=sibling.nextSibling,out.push(sibling,greatAnc),greatAnc.removeChild(sibling)}}function
attach(id){var limit=byId(id),isLimitClosed,limitAnc,nConsumed,newDetached,parent,reattAnc,reattach;if(detached){newDetached=[0,0],detachOnto(limit,newDetached),limitAnc=limit;for(;parent=limitAnc.parentNode;)limitAnc=parent;nConsumed=0;while(nConsumed<detached.length){reattach=detached[nConsumed],reattAnc=reattach;for(;reattAnc.parentNode;reattAnc=reattAnc.parentNode);detached[nConsumed+1].appendChild(reattach),nConsumed+=2;if(reattAnc===limitAnc)break}newDetached[1]=nConsumed,arraySplice.apply(detached,newDetached)}else
detached=[],detachOnto(limit,detached);return isLimitClosed=detached[1]!==limit,insertionPoint=isLimitClosed?limit.parentNode:limit,limit}function
discard(placeholder){placeholder.parentNode.removeChild(placeholder)}function finish(){var
i,n;insertionPoint=null;if(detached)for(i=0,n=detached.length;i<n;i+=2)detached[i+1].appendChild(detached[i]);return idMap=detached=base=null,this}function
addBodyClasses(classes){base.className+=' '+classes}function signalLoaded(){var
doc=opt_tameDocument;return doc&&doc.signalLoaded___(),this}this.byId=byId,this.attach=attach,this.discard=discard,this.emitStatic=emitStatic,this.finish=finish,this.signalLoaded=signalLoaded,this.setAttr=bridal.setAttribute,this.addBodyClasses=addBodyClasses,(function(tameDoc){var
documentWriter,tameDocWrite,ucase;if(!tameDoc||tameDoc.write)return;function concat(items){return Array.prototype.join.call(items,'')}'script'.toUpperCase()==='SCRIPT'?(ucase=function(s){return s.toUpperCase()}):(ucase=function(s){return s.replace(/[a-z]/g,function(ch){return String.fromCharCode(ch.charCodeAt(0)&-33)})}),documentWriter={'startTag':function(tagName,attribs){var
eltype=html4 .ELEMENTS[tagName],el;if(!html4 .ELEMENTS.hasOwnProperty(tagName)||(eltype&html4
.eflags.UNSAFE)!==0)return;tameDoc.sanitizeAttrs___(tagName,attribs),el=bridal.createElement(tagName,attribs),eltype&html4
.eflags.OPTIONAL_ENDTAG&&el.tagName===insertionPoint.tagName&&documentWriter.endTag(el.tagName,true),insertionPoint.appendChild(el),eltype&html4
.eflags.EMPTY||(insertionPoint=el)},'endTag':function(tagName,optional){var anc=insertionPoint,p;tagName=ucase(tagName);while(anc!==base&&!/\bvdoc-body___\b/.test(anc.className)){p=anc.parentNode;if(anc.tagName===tagName)return insertionPoint=p,void
0;anc=p}},'pcdata':function(text){insertionPoint.appendChild(insertionPoint.ownerDocument.createTextNode(html.unescapeEntities(text)))},'cdata':function(text){insertionPoint.appendChild(insertionPoint.ownerDocument.createTextNode(text))}},documentWriter.rcdata=documentWriter.pcdata,tameDocWrite=function
write(html_varargs){var htmlText=concat(arguments),lexer;insertionPoint||(insertionPoint=base),lexer=html.makeSaxParser(documentWriter),lexer(htmlText)},tameDoc.write=___.markFuncFreeze(tameDocWrite,'write'),tameDoc.writeln=___.markFuncFreeze(function
writeln(html){tameDocWrite(concat(arguments),'\n')},'writeln'),___.grantFunc(tameDoc,'write'),___.grantFunc(tameDoc,'writeln')})(opt_tameDocument)}bridalMaker=function(document){var
isOpera=navigator.userAgent.indexOf('Opera')===0,isIE=!isOpera&&navigator.userAgent.indexOf('MSIE')!==-1,isWebkit=!isOpera&&navigator.userAgent.indexOf('WebKit')!==-1,featureAttachEvent=!!(window.attachEvent&&!window.addEventListener),featureExtendedCreateElement=(function(){try{return document.createElement('<input type=\"radio\">').type==='radio'}catch(e){return false}})(),CUSTOM_EVENT_TYPE_SUFFIX='_custom___',endsWith__,escapeAttrib;function
tameEventType(type,opt_isCustom,opt_tagName){var tagAttr;type=String(type);if(endsWith__.test(type))throw new
Error('Invalid event type '+type);return tagAttr=false,opt_tagName&&(tagAttr=String(opt_tagName).toLowerCase()+'::on'+type),!opt_isCustom&&(tagAttr&&html4
.atype.SCRIPT===html4 .ATTRIBS[tagAttr]||html4 .atype.SCRIPT===html4 .ATTRIBS['*::on'+type])?type:type+CUSTOM_EVENT_TYPE_SUFFIX}function
eventHandlerTypeFilter(handler,tameType){return function(event){if(tameType===event.eventType___)return handler.call(this,event)}}endsWith__=/__$/,escapeAttrib=html.escapeAttrib;function
constructClone(node,deep){var attr,attrs,child,clone,cloneChild,i,tagDesc;if(node.nodeType===1&&featureExtendedCreateElement){tagDesc=node.tagName;switch(node.tagName){case'INPUT':tagDesc='<input name=\"'+escapeAttrib(node.name)+'\" type=\"'+escapeAttrib(node.type)+'\" value=\"'+escapeAttrib(node.defaultValue)+'\"'+(node.defaultChecked?' checked=\"checked\">':'>');break;case'BUTTON':tagDesc='<button name=\"'+escapeAttrib(node.name)+'\" type=\"'+escapeAttrib(node.type)+'\" value=\"'+escapeAttrib(node.value)+'\">';break;case'OPTION':tagDesc='<option '+(node.defaultSelected?' selected=\"selected\">':'>');break;case'TEXTAREA':tagDesc='<textarea value=\"'+escapeAttrib(node.defaultValue)+'\">'}clone=document.createElement(tagDesc),attrs=node.attributes;for(i=0;attr=attrs[i];++i)attr.specified&&!endsWith__.test(attr.name)&&setAttribute(clone,attr.nodeName,attr.nodeValue)}else
clone=node.cloneNode(false);if(deep)for(child=node.firstChild;child;child=child.nextSibling)cloneChild=constructClone(child,deep),clone.appendChild(cloneChild);return clone}function
fixupClone(node,clone){var attribs,child,cloneChild,originalAttribs;for(child=node.firstChild,cloneChild=clone.firstChild;cloneChild;child=child.nextSibling,cloneChild=cloneChild.nextSibling)fixupClone(child,cloneChild);if(node.nodeType===1)switch(node.tagName){case'INPUT':clone.value=node.value,clone.checked=node.checked;break;case'OPTION':clone.selected=node.selected,clone.value=node.value;break;case'TEXTAREA':clone.value=node.value}originalAttribs=node.attributes___,originalAttribs&&(attribs={},clone.attributes___=attribs,cajita.forOwnKeys(originalAttribs,___.markFuncFreeze(function(k,v){switch(typeof
v){case'string':case'number':case'boolean':attribs[k]=v}})))}function getWindow(element){var
doc=element.ownerDocument,s;return doc.parentWindow?doc.parentWindow:doc.defaultView?doc.defaultView:(s=doc.createElement('script'),s.innerHTML='document.parentWindow = window;',doc.body.appendChild(s),doc.body.removeChild(s),doc.parentWindow)}function
untameEventType(type){var suffix=CUSTOM_EVENT_TYPE_SUFFIX,tlen=type.length,slen=suffix.length,end;return end=tlen-slen,end>=0&&suffix===type.substring(end)&&(type=type.substring(0,end)),type}function
initEvent(event,type,bubbles,cancelable){type=tameEventType(type,true),bubbles=Boolean(bubbles),cancelable=Boolean(cancelable);if(event.initEvent)event.initEvent(type,bubbles,cancelable);else
if(bubbles&&cancelable)event.eventType___=type;else throw new Error('Browser does not support non-bubbling/uncanceleable events')}function
dispatchEvent(element,event){return element.dispatchEvent?Boolean(element.dispatchEvent(event)):(element.fireEvent('ondataavailable',event),Boolean(event.returnValue))}function
addEventListener(element,type,handler,useCapture){var tameType,wrapper;return type=String(type),tameType=tameEventType(type,false,element.tagName),featureAttachEvent?type!==tameType?(wrapper=eventHandlerTypeFilter(handler,tameType),element.attachEvent('ondataavailable',wrapper),wrapper):(element.attachEvent('on'+type,handler),handler):(element.addEventListener(tameType,handler,useCapture),handler)}function
removeEventListener(element,type,handler,useCapture){var tameType;type=String(type),tameType=tameEventType(type,false,element.tagName),featureAttachEvent?tameType!==type?element.detachEvent('ondataavailable',handler):element.detachEvent('on'+type,handler):element.removeEventListener(tameType,handler,useCapture)}function
cloneNode(node,deep){var clone;return document.all?(clone=constructClone(node,deep)):(clone=node.cloneNode(deep)),fixupClone(node,clone),clone}function
createElement(tagName,attribs){var el,i,n,tag;if(featureExtendedCreateElement){tag=['<',tagName];for(i=0,n=attribs.length;i<n;i+=2)tag.push(' ',attribs[i],'=\"',escapeAttrib(attribs[i+1]),'\"');return tag.push('>'),document.createElement(tag.join(''))}el=document.createElement(tagName);for(i=0,n=attribs.length;i<n;i+=2)setAttribute(el,attribs[i],attribs[i+1]);return el}function
createStylesheet(document,cssText){var styleSheet=document.createElement('style');return styleSheet.setAttribute('type','text/css'),styleSheet.styleSheet?(styleSheet.styleSheet.cssText=cssText):styleSheet.appendChild(document.createTextNode(cssText)),styleSheet}function
setAttribute(element,name,value){var attr;switch(name){case'style':return element.style.cssText=value,value;case'href':/^javascript:/i.test(value)?(element.stored_target___=element.target,element.target=''):element.stored_target___&&(element.target=element.stored_target___,delete
element.stored_target___);break;case'target':if(element.href&&/^javascript:/i.test(element.href))return element.stored_target___=value,value}try{attr=element.ownerDocument.createAttribute(name),attr.value=value,element.setAttributeNode(attr)}catch(e){return element.setAttribute(name,value,0)}return value}function
getBoundingClientRect(el){var doc=el.ownerDocument,cRect,elBoxObject,fixupLeft,fixupTop,left,op,opPosition,pageX,pageY,scrollEl,top,viewPortBoxObject,viewport;if(el.getBoundingClientRect)return cRect=el.getBoundingClientRect(),isIE&&(fixupLeft=doc.documentElement.clientLeft+doc.body.clientLeft,cRect.left-=fixupLeft,cRect.right-=fixupLeft,fixupTop=doc.documentElement.clientTop+doc.body.clientTop,cRect.top-=fixupTop,cRect.bottom-=fixupTop),{'top':+cRect.top,'left':+cRect.left,'right':+cRect.right,'bottom':+cRect.bottom};viewport=isIE&&doc.compatMode==='CSS1Compat'?doc.body:doc.documentElement,pageX=0,pageY=0;if(el===viewport);else
if(doc.getBoxObjectFor)elBoxObject=doc.getBoxObjectFor(el),viewPortBoxObject=doc.getBoxObjectFor(viewport),pageX=elBoxObject.screenX-viewPortBoxObject.screenX,pageY=elBoxObject.screenY-viewPortBoxObject.screenY;else{for(op=el;op&&op!==el;op=op.offsetParent){pageX+=op.offsetLeft,pageY+=op.offsetTop,op!==el&&(pageX+=op.clientLeft||0,pageY+=op.clientTop||0);if(isWebkit){opPosition=doc.defaultView.getComputedStyle(op,'position'),opPosition==='fixed'&&(pageX+=doc.body.scrollLeft,pageY+=doc.body.scrollTop);break}}(isWebkit&&doc.defaultView.getComputedStyle(el,'position')==='absolute'||isOpera)&&(pageY-=doc.body.offsetTop);for(op=el;(op=op.offsetParent)&&op!==doc.body;)pageX-=op.scrollLeft,(!isOpera||op.tagName!=='TR')&&(pageY-=op.scrollTop)}return scrollEl=!isWebkit&&doc.compatMode==='CSS1Compat'?doc.documentElement:doc.body,left=pageX-scrollEl.scrollLeft,top=pageY-scrollEl.scrollTop,{'top':top,'left':left,'right':left+el.clientWidth,'bottom':top+el.clientHeight}}function
getAttribute(element,name){var attr;if(name==='style'){if(typeof element.style.cssText==='string')return element.style.cssText};return attr=element.getAttributeNode(name),attr&&attr.specified?attr.value:null}function
hasAttribute(element,name){var attr;return element.hasAttribute?element.hasAttribute(name):(attr=element.getAttributeNode(name),attr!==null&&attr.specified)}function
getComputedStyle(element,pseudoElement){if(element.currentStyle&&pseudoElement===void
0)return element.currentStyle;else if(window.getComputedStyle)return window.getComputedStyle(element,pseudoElement);throw new
Error('Computed style not available for pseudo element '+pseudoElement)}function
makeXhr(){var activeXClassIds,candidate,i,n;if(typeof XMLHttpRequest==='undefined'){activeXClassIds=['MSXML2.XMLHTTP.5.0','MSXML2.XMLHTTP.4.0','MSXML2.XMLHTTP.3.0','MSXML2.XMLHTTP','MICROSOFT.XMLHTTP.1.0','MICROSOFT.XMLHTTP.1','MICROSOFT.XMLHTTP'];for(i=0,n=activeXClassIds.length;i<n;++i){candidate=activeXClassIds[i];try{return new
ActiveXObject(candidate)}catch(e){}}}return new XMLHttpRequest}return{'addEventListener':addEventListener,'removeEventListener':removeEventListener,'initEvent':initEvent,'dispatchEvent':dispatchEvent,'cloneNode':cloneNode,'createElement':createElement,'createStylesheet':createStylesheet,'setAttribute':setAttribute,'getAttribute':getAttribute,'hasAttribute':hasAttribute,'getBoundingClientRect':getBoundingClientRect,'getWindow':getWindow,'untameEventType':untameEventType,'extendedCreateElementFeature':featureExtendedCreateElement,'getComputedStyle':getComputedStyle,'makeXhr':makeXhr}},bridal=bridalMaker(document),cssparser=(function(){var
ucaseLetter=/[A-Z]/g,BS,COMMENT,DQ,ESCAPE,HASH,HEX,IDENT,IDENT_RE,KEYWORD,LCASE,NAME,NEWLINE,NMCHAR,NMSTART,NON_ASCII,NON_HEX_ESC_RE,NUM,PROP_DECLS_TOKENS,PUNC,QUANTITY,SPACE,SPACE_RE,STRING,STRING1,STRING2,UNICODE,UNIT,URL,URL_CHARS,URL_RE,URL_SPECIAL_CHARS,WHITESPACE,unicodeEscape;function
lcaseOne(ch){return String.fromCharCode(ch.charCodeAt(0)|32)}LCASE='i'==='I'.toLowerCase()?function(s){return s.toLowerCase()}:function(s){return s.replace(ucaseLetter,lcaseOne)},KEYWORD='(?:\\@(?:import|page|media|charset))',NEWLINE='\\n|\\r\\n|\\r|\\f',HEX='[0-9a-f]',NON_ASCII='[^\\0-\\177]',UNICODE='(?:(?:\\\\'+HEX+'{1,6})(?:\\r\\n|[ 	\\r\\n\\f])?)',ESCAPE='(?:'+UNICODE+'|\\\\[^\\r\\n\\f0-9a-f])',NMSTART='(?:[_a-z]|'+NON_ASCII+'|'+ESCAPE+')',NMCHAR='(?:[_a-z0-9-]|'+NON_ASCII+'|'+ESCAPE+')',IDENT='-?'+NMSTART+NMCHAR+'*',NAME=NMCHAR+'+',HASH='#'+NAME,STRING1='\"(?:[^\\\"\\\\]|\\\\[\\s\\S])*\"',STRING2='\'(?:[^\\\'\\\\]|\\\\[\\s\\S])*\'',STRING='(?:'+STRING1+'|'+STRING2+')',NUM='(?:[0-9]*\\.[0-9]+|[0-9]+)',SPACE='[ \\t\\r\\n\\f]',WHITESPACE=SPACE+'*',URL_SPECIAL_CHARS='[!#$%&*-~]',URL_CHARS='(?:'+URL_SPECIAL_CHARS+'|'+NON_ASCII+'|'+ESCAPE+')*',URL='url\\('+WHITESPACE+'(?:'+STRING+'|'+URL_CHARS+')'+WHITESPACE+'\\)',COMMENT='/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/',UNIT='(?:em|ex|px|cm|mm|in|pt|pc|deg|rad|grad|ms|s|hz|khz|%)',QUANTITY=NUM+'(?:'+WHITESPACE+UNIT+'|'+IDENT+')?',PUNC='<!--|-->|~=|[|=\\{\\+>,:;()]',PROP_DECLS_TOKENS=new
RegExp('(?:'+[STRING,COMMENT,QUANTITY,URL,NAME,HASH,IDENT,SPACE+'+',PUNC].join('|')+')','gi'),IDENT_RE=new
RegExp('^(?:'+IDENT+')$','i'),URL_RE=new RegExp('^(?:'+URL+')$','i'),NON_HEX_ESC_RE=/\\(?:\r\n?|[^0-9A-Fa-f\r]|$)/g,SPACE_RE=new
RegExp(SPACE+'+','g'),BS=/\\/g,DQ=/"/g;function normEscs(x){var out='',i,n;for(i=1,n=x.length;i<n;++i)out+='\\'+x.charCodeAt(i).toString(16)+' ';return out}function
toCssStr(s){return'\"'+s.replace(BS,'\\5c ').replace(DQ,'\\22 ')+'\"'}function parse(cssText,handler){var
toks=(''+cssText).match(PROP_DECLS_TOKENS),buf,i,k,n,propName,tok,url;if(!toks)return;propName=null,buf=[],k=0;for(i=0,n=toks.length;i<n;++i){tok=toks[i];switch(tok.charCodeAt(0)){case
9:case 10:case 12:case 13:case 32:continue;case 39:tok='\"'+tok.substring(1,tok.length-1).replace(DQ,'\\22 ')+'\"';case
34:tok=tok.replace(NON_HEX_ESC_RE,normEscs);break;case 47:if('*'===tok.charAt(1))continue;break;case
46:case 48:case 49:case 50:case 51:case 52:case 53:case 54:case 55:case 56:case 57:tok=tok.replace(SPACE_RE,'');break;case
58:k===1&&IDENT_RE.test(buf[0])?(propName=LCASE(buf[0])):(propName=null),k=buf.length=0;continue;case
59:propName&&(buf.length&&handler(propName,buf.slice(0)),propName=null),k=buf.length=0;continue;case
85:case 117:url=toUrl(tok),url!==null&&(tok='url('+toCssStr(url)+')')}buf[k++]=tok}propName&&buf.length&&handler(propName,buf.slice(0))}unicodeEscape=/\\(?:([0-9a-fA-F]{1,6})(?:\r\n?|[ \t\f\n])?|[^\r\n\f0-9a-f])/g;function
decodeOne(_,hex){return hex?String.fromCharCode(parseInt(hex,16)):_.charAt(1)}function
toUrl(cssToken){if(!URL_RE.test(cssToken))return null;cssToken=cssToken.replace(/^url[\s\(]+|[\s\)]+$/gi,'');switch(cssToken.charAt(0)){case'\"':case'\'':cssToken=cssToken.substring(1,cssToken.length-1)}return cssToken.replace(unicodeEscape,decodeOne)}return{'parse':parse,'toUrl':toUrl,'toCssStr':toCssStr}})(),domitaModules||(domitaModules={}),domitaModules.classUtils=function(){function
getterSetterSuffix(name){return String.fromCharCode(name.charCodeAt(0)&-33)+name.substring(1)+'___'}function
exportFields(object,fields){var count,field,getterName,i,setterName,suffix;for(i=fields.length;--i>=0;){field=fields[i],suffix=getterSetterSuffix(field),getterName='get'+suffix,setterName='set'+suffix,count=0,object[getterName]&&(++count,___.useGetHandler(object,field,object[getterName])),object[setterName]&&(++count,___.useSetHandler(object,field,object[setterName]));if(!count)throw new
Error('Failed to export field '+field+' on '+object)}}function applyAccessors(object,handlers){function
propertyOnlyHasGetter(_){throw new TypeError('setting a property that only has a getter')}___.forOwnKeys(handlers,___.markFuncFreeze(function(propertyName,def){var
setter=def.set||propertyOnlyHasGetter;___.useGetHandler(object,propertyName,def.get),___.useSetHandler(object,propertyName,setter)}))}function
ensureValidCallback(aCallback){if('function'!==typeof aCallback&&!('object'===typeof
aCallback&&aCallback!==null&&___.canCallPub(aCallback,'call')))throw new Error('Expected function not '+typeof
aCallback)}return{'exportFields':exportFields,'ensureValidCallback':ensureValidCallback,'applyAccessors':applyAccessors,'getterSetterSuffix':getterSetterSuffix}},domitaModules.XMLHttpRequestCtor=function(XMLHttpRequest,ActiveXObject){var
activeXClassId;if(XMLHttpRequest)return XMLHttpRequest;else if(ActiveXObject)return function
ActiveXObjectForIE(){var activeXClassIds,candidate,i,n;if(activeXClassId===void 0){activeXClassId=null,activeXClassIds=['MSXML2.XMLHTTP.5.0','MSXML2.XMLHTTP.4.0','MSXML2.XMLHTTP.3.0','MSXML2.XMLHTTP','MICROSOFT.XMLHTTP.1.0','MICROSOFT.XMLHTTP.1','MICROSOFT.XMLHTTP'];for(i=0,n=activeXClassIds.length;i<n;++i){candidate=activeXClassIds[i];try{new
ActiveXObject(candidate),activeXClassId=candidate;break}catch(e){}}activeXClassIds=null}return new
ActiveXObject(activeXClassId)};throw new Error('ActiveXObject not available')},domitaModules.TameXMLHttpRequest=function(xmlHttpRequestMaker,uriCallback){var
classUtils=domitaModules.classUtils(),FROZEN,INVALID_SUFFIX,endsWith__;function TameXMLHttpRequest(){this.xhr___=new
xmlHttpRequestMaker,classUtils.exportFields(this,['onreadystatechange','readyState','responseText','responseXML','status','statusText'])}return FROZEN='Object is frozen.',INVALID_SUFFIX='Property names may not end in \'__\'.',endsWith__=/__$/,TameXMLHttpRequest.prototype.handleRead___=function(name){var
handlerName;return name=''+name,endsWith__.test(name)?void 0:(handlerName=name+'_getter___',this[handlerName]?this[handlerName]():___.hasOwnProp(this.xhr___.properties___,name)?this.xhr___.properties___[name]:void
0)},TameXMLHttpRequest.prototype.handleCall___=function(name,args){var handlerName;name=''+name;if(endsWith__.test(name))throw new
Error(INVALID_SUFFIX);handlerName=name+'_handler___';if(this[handlerName])return this[handlerName].call(this,args);if(___.hasOwnProp(this.xhr___.properties___,name))return this.xhr___.properties___[name].call(this,args);throw new
TypeError(name+' is not a function.')},TameXMLHttpRequest.prototype.handleSet___=function(name,val){var
handlerName;name=''+name;if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);if(___.isFrozen(this))throw new
Error(FROZEN);return handlerName=name+'_setter___',this[handlerName]?this[handlerName](val):(this.xhr___.properties___||(this.xhr___.properties___={}),this[name+'_canEnum___']=true,this.xhr___.properties___[name]=val)},TameXMLHttpRequest.prototype.handleDelete___=function(name){var
handlerName;name=''+name;if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);if(___.isFrozen(this))throw new
Error(FROZEN);return handlerName=name+'_deleter___',this[handlerName]?this[handlerName]():this.xhr___.properties___?delete
this.xhr___.properties___[name]&&delete this[name+'_canEnum___']:true},TameXMLHttpRequest.prototype.setOnreadystatechange___=function(handler){var
self=this;this.xhr___.onreadystatechange=function(event){var evt={'target':self};return ___.callPub(handler,'call',[void
0,evt])},this.handler___=handler},TameXMLHttpRequest.prototype.getReadyState___=function(){return Number(this.xhr___.readyState)},TameXMLHttpRequest.prototype.open=function(method,URL,opt_async,opt_userName,opt_password){var
safeUri;method=String(method),safeUri=uriCallback.rewrite(String(URL),'*/*');if(safeUri===void
0)throw'URI violates security policy';switch(arguments.length){case 2:this.async___=true,this.xhr___.open(method,safeUri);break;case
3:this.async___=opt_async,this.xhr___.open(method,safeUri,Boolean(opt_async));break;case
4:this.async___=opt_async,this.xhr___.open(method,safeUri,Boolean(opt_async),String(opt_userName));break;case
5:this.async___=opt_async,this.xhr___.open(method,safeUri,Boolean(opt_async),String(opt_userName),String(opt_password));break;default:throw'XMLHttpRequest cannot accept '+arguments.length+' arguments'}},TameXMLHttpRequest.prototype.setRequestHeader=function(label,value){this.xhr___.setRequestHeader(String(label),String(value))},TameXMLHttpRequest.prototype.send=function(opt_data){var
evt;arguments.length===0?this.xhr___.send(''):typeof opt_data==='string'?this.xhr___.send(opt_data):this.xhr___.send(''),this.xhr___.overrideMimeType&&(!this.async___&&this.handler___&&(evt={'target':this},___.callPub(this.handler___,'call',[void
0,evt])))},TameXMLHttpRequest.prototype.abort=function(){this.xhr___.abort()},TameXMLHttpRequest.prototype.getAllResponseHeaders=function(){var
result=this.xhr___.getAllResponseHeaders();return result===undefined||result===null?result:String(result)},TameXMLHttpRequest.prototype.getResponseHeader=function(headerName){var
result=this.xhr___.getResponseHeader(String(headerName));return result===undefined||result===null?result:String(result)},TameXMLHttpRequest.prototype.getResponseText___=function(){var
result=this.xhr___.responseText;return result===undefined||result===null?result:String(result)},TameXMLHttpRequest.prototype.getResponseXML___=function(){return{}},TameXMLHttpRequest.prototype.getStatus___=function(){var
result=this.xhr___.status;return result===undefined||result===null?result:Number(result)},TameXMLHttpRequest.prototype.getStatusText___=function(){var
result=this.xhr___.statusText;return result===undefined||result===null?result:String(result)},TameXMLHttpRequest.prototype.toString=___.markFuncFreeze(function(){return'Not a real XMLHttpRequest'}),___.markCtor(TameXMLHttpRequest,Object,'TameXMLHttpRequest'),___.all2(___.grantTypedMethod,TameXMLHttpRequest.prototype,['open','setRequestHeader','send','abort','getAllResponseHeaders','getResponseHeader']),TameXMLHttpRequest},domitaModules.CssPropertiesCollection=function(cssPropertyNameCollection,anElement,css){var
canonicalStylePropertyNames={},cssPropertyNames={};return ___.forOwnKeys(cssPropertyNameCollection,___.markFuncFreeze(function(cssPropertyName){var
baseStylePropertyName=cssPropertyName.replace(/-([a-z])/g,function(_,letter){return letter.toUpperCase()}),canonStylePropertyName=baseStylePropertyName,alts,i;cssPropertyNames[baseStylePropertyName]=cssPropertyNames[canonStylePropertyName]=cssPropertyName;if(css.alternates.hasOwnProperty(canonStylePropertyName)){alts=css.alternates[canonStylePropertyName];for(i=alts.length;--i>=0;)cssPropertyNames[alts[i]]=cssPropertyName,alts[i]in
anElement.style&&!(canonStylePropertyName in anElement.style)&&(canonStylePropertyName=alts[i])}canonicalStylePropertyNames[cssPropertyName]=canonStylePropertyName})),{'isCanonicalProp':function(p){return cssPropertyNames.hasOwnProperty(p)},'isCssProp':function(p){return canonicalStylePropertyNames.hasOwnProperty(p)},'getCanonicalPropFromCss':function(p){return canonicalStylePropertyNames[p]},'getCssPropFromCanonical':function(p){return cssPropertyNames[p]}}},attachDocumentStub=(function(){var
FORBIDDEN_ID_LIST_PATTERN,FORBIDDEN_ID_PATTERN,IntervalIdMark,IntervalIdT,JS_IDENT,JS_SPACE,SIMPLE_HANDLER_PATTERN,TameEventMark,TameEventT,TameNodeMark,TameNodeT,TimeoutIdMark,TimeoutIdT,VALID_ID_CHAR,VALID_ID_LIST_PATTERN,VALID_ID_PATTERN,XML_SPACE,classUtils,cssSealerUnsealerPair;function
arrayRemove(array,from,to){var rest=array.slice((to||from)+1||array.length);return array.length=from<0?array.length+from:from,array.push.apply(array,rest)}TameNodeMark=___.Trademark('TameNode'),TameNodeT=TameNodeMark.guard,TameEventMark=___.Trademark('TameEvent'),TameEventT=TameEventMark.guard;function
Html(htmlFragment){this.html___=String(htmlFragment||'')}Html.prototype.valueOf=Html.prototype.toString=___.markFuncFreeze(function(){return this.html___});function
safeHtml(htmlFragment){return htmlFragment instanceof Html?htmlFragment.html___:html.escapeAttrib(String(htmlFragment||''))}function
blessHtml(htmlFragment){return htmlFragment instanceof Html?htmlFragment:new Html(htmlFragment)}XML_SPACE='	\n\r ',JS_SPACE='	\n\r ',JS_IDENT='(?:[a-zA-Z_][a-zA-Z0-9$_]*[a-zA-Z0-9$]|[a-zA-Z])_?',SIMPLE_HANDLER_PATTERN=new
RegExp('^['+JS_SPACE+']*'+'(return['+JS_SPACE+']+)?'+'('+JS_IDENT+')['+JS_SPACE+']*'+'\\((?:this'+'(?:['+JS_SPACE+']*,['+JS_SPACE+']*event)?'+'['+JS_SPACE+']*)?\\)'+'['+JS_SPACE+']*(?:;?['+JS_SPACE+']*)$'),VALID_ID_CHAR=unicode.LETTER+unicode.DIGIT+'_'+'$\\-.:;=()\\[\\]'+unicode.COMBINING_CHAR+unicode.EXTENDER,VALID_ID_PATTERN=new
RegExp('^['+VALID_ID_CHAR+']+$'),VALID_ID_LIST_PATTERN=new RegExp('^['+XML_SPACE+VALID_ID_CHAR+']*$'),FORBIDDEN_ID_PATTERN=new
RegExp('__\\s*$'),FORBIDDEN_ID_LIST_PATTERN=new RegExp('__(?:\\s|$)');function isValidId(s){return!FORBIDDEN_ID_PATTERN.test(s)&&VALID_ID_PATTERN.test(s)}function
isValidIdList(s){return!FORBIDDEN_ID_LIST_PATTERN.test(s)&&VALID_ID_LIST_PATTERN.test(s)}function
trimCssSpaces(input){return input.replace(/^[ \t\r\n\f]+|[ \t\r\n\f]+$/g,'')}function
decodeCssString(s){return s.replace(/\\(?:(\r\n?|\n|\f)|([0-9a-f]{1,6})(?:\r\n?|[ \t\n\f])?|(.))/gi,function(_,nl,hex,esc){return esc||(nl?'':String.fromCharCode(parseInt(hex,16)))})}function
sanitizeStyleAttrValue(styleAttrValue){var sanitizedDeclarations=[];return cssparser.parse(String(styleAttrValue),function(property,value){property=property.toLowerCase(),css.properties.hasOwnProperty(property)&&css.properties[property].test(value+'')&&sanitizedDeclarations.push(property+': '+value)}),sanitizedDeclarations.join(' ; ')}function
mimeTypeForAttr(tagName,attribName){if(attribName==='src'){if(tagName==='img')return'image/*';if(tagName==='script')return'text/javascript'}return'*/*'}function
assert(cond){if(!cond)throw typeof console!=='undefined'&&(console.error('domita assertion failed'),console.trace()),new
Error('Domita assertion failed')}classUtils=domitaModules.classUtils(),cssSealerUnsealerPair=___.makeSealerUnsealerPair(),TimeoutIdMark=___.Trademark('TimeoutId'),TimeoutIdT=TimeoutIdMark.guard;function
tameSetTimeout(timeout,delayMillis){var timeoutId;if(timeout){if(typeof timeout==='string')throw new
Error('setTimeout called with a string.  Please pass a function instead of a string of javascript');timeoutId=setTimeout(function(){___.callPub(timeout,'call',[___.USELESS])},delayMillis|0)}else
timeoutId=NaN;return ___.stamp([TimeoutIdMark.stamp],{'timeoutId___':timeoutId})}___.markFuncFreeze(tameSetTimeout);function
tameClearTimeout(timeoutId){var rawTimeoutId;if(timeoutId===null||timeoutId===void
0)return;try{timeoutId=TimeoutIdT.coerce(timeoutId)}catch(e){return}rawTimeoutId=timeoutId.timeoutId___,rawTimeoutId===rawTimeoutId&&clearTimeout(rawTimeoutId)}___.markFuncFreeze(tameClearTimeout),IntervalIdMark=___.Trademark('IntervalId'),IntervalIdT=IntervalIdMark.guard;function
tameSetInterval(interval,delayMillis){var intervalId;if(interval){if(typeof interval==='string')throw new
Error('setInterval called with a string.  Please pass a function instead of a string of javascript');intervalId=setInterval(function(){___.callPub(interval,'call',[___.USELESS])},delayMillis|0)}else
intervalId=NaN;return ___.stamp([IntervalIdMark.stamp],{'intervalId___':intervalId})}___.markFuncFreeze(tameSetInterval);function
tameClearInterval(intervalId){var rawIntervalId;if(intervalId===null||intervalId===void
0)return;try{intervalId=IntervalIdT.coerce(intervalId)}catch(e){return}rawIntervalId=intervalId.intervalId___,rawIntervalId===rawIntervalId&&clearInterval(rawIntervalId)}___.markFuncFreeze(tameClearInterval);function
makeScrollable(element){var window=bridal.getWindow(element),overflow=null;element.currentStyle?(overflow=element.currentStyle.overflow):window.getComputedStyle?(overflow=window.getComputedStyle(element,void
0).overflow):(overflow=null);switch(overflow&&overflow.toLowerCase()){case'visible':case'hidden':element.style.overflow='auto'}}function
tameScrollTo(element,x,y){if(x!==+x||y!==+y||x<0||y<0)throw new Error('Cannot scroll to '+x+':'+typeof
x+','+y+' : '+typeof y);element.scrollLeft=x,element.scrollTop=y}function tameScrollBy(element,dx,dy){if(dx!==+dx||dy!==+dy)throw new
Error('Cannot scroll by '+dx+':'+typeof dx+', '+dy+':'+typeof dy);element.scrollLeft+=dx,element.scrollTop+=dy}function
guessPixelsFromCss(cssStr){var m;return cssStr?(m=cssStr.match(/^([0-9]+)/),m?+m[1]:0):0}function
tameResizeTo(element,w,h){if(w!==+w||h!==+h)throw new Error('Cannot resize to '+w+':'+typeof
w+', '+h+':'+typeof h);element.style.width=w+'px',element.style.height=h+'px'}function
tameResizeBy(element,dw,dh){var extraHeight,extraWidth,goalHeight,goalWidth,h,hError,style,w,wError;if(dw!==+dw||dh!==+dh)throw new
Error('Cannot resize by '+dw+':'+typeof dw+', '+dh+':'+typeof dh);if(!dw&&!dh)return;style=element.currentStyle,style||(style=bridal.getWindow(element).getComputedStyle(element,void
0)),extraHeight=guessPixelsFromCss(style.paddingBottom)+guessPixelsFromCss(style.paddingTop),extraWidth=guessPixelsFromCss(style.paddingLeft)+guessPixelsFromCss(style.paddingRight),goalHeight=element.clientHeight+dh,goalWidth=element.clientWidth+dw,h=goalHeight-extraHeight,w=goalWidth-extraWidth,dh&&(element.style.height=Math.max(0,h)+'px'),dw&&(element.style.width=Math.max(0,w)+'px'),dh&&element.clientHeight!==goalHeight&&(hError=element.clientHeight-goalHeight,element.style.height=Math.max(0,h-hError)+'px'),dw&&element.clientWidth!==goalWidth&&(wError=element.clientWidth-goalWidth,element.style.width=Math.max(0,w-wError)+'px')}function
attachDocumentStub(idSuffix,uriCallback,imports,pseudoBodyNode,optPseudoWindowLocation){var
pluginId=___.getId(imports),document=pseudoBodyNode.ownerDocument,bridal=bridalMaker(document),window=bridal.getWindow(pseudoBodyNode),ID_LIST_PARTS_PATTERN,INDEX_SIZE_ERROR,INVALID_SUFFIX,NOT_EDITABLE,PSEUDO_ELEMENT_WHITELIST,UNKNOWN_TAGNAME,UNSAFE_TAGNAME,allCssProperties,commonElementPropertyHandlers,defaultNodeClassCtor,defaultNodeClasses,editableTameNodeCache,elementPolicies,endsWith__,historyInsensitiveCssProperties,htmlSanitizer,i,idClass,idClassPattern,innerHtmlTamer,k,nodeClasses,outers,prop,readOnlyTameNodeCache,tameDefaultView,tameDocument,tameLocation,tameNavigator,tameNodeFields,tameNodePublicMembers,tameWindow,windowProps,wpLen;if(arguments.length<4)throw new
Error('arity mismatch: '+arguments.length);optPseudoWindowLocation||(optPseudoWindowLocation={}),elementPolicies={},elementPolicies.form=function(attribs){var
sawHandler=false,i,n;for(i=0,n=attribs.length;i<n;i+=2)attribs[i]==='onsubmit'&&(sawHandler=true);return sawHandler||attribs.push('onsubmit','return false'),attribs},elementPolicies.a=elementPolicies.area=function(attribs){return attribs.push('target','_blank'),attribs};function
sanitizeHtml(htmlText){var out=[];return htmlSanitizer(htmlText,out),out.join('')}function
sanitizeAttrs(tagName,attribs){var n=attribs.length,attribKey,attribName,atype,i,policy,value;for(i=0;i<n;i+=2)attribName=attribs[i],value=attribs[i+1],atype=null,(attribKey=tagName+'::'+attribName,html4
.ATTRIBS.hasOwnProperty(attribKey))||(attribKey='*::'+attribName,html4 .ATTRIBS.hasOwnProperty(attribKey))?(atype=html4
.ATTRIBS[attribKey],value=rewriteAttribute(tagName,attribName,atype,value)):(value=null),value!==null&&value!==void
0?(attribs[i+1]=value):(attribs[i+1]=attribs[--n],attribs[i]=attribs[--n],i-=2);return attribs.length=n,policy=elementPolicies[tagName],policy&&elementPolicies.hasOwnProperty(tagName)?policy(attribs):attribs}htmlSanitizer=html.makeHtmlSanitizer(sanitizeAttrs);function
unsuffix(str,suffix,fail){var n;return typeof str!=='string'?fail:(n=str.length-suffix.length,0<n&&str.substring(n)===suffix?str.substring(0,n):fail)}ID_LIST_PARTS_PATTERN=new
RegExp('([^'+XML_SPACE+']+)(['+XML_SPACE+']+|$)','g');function virtualizeAttributeValue(attrType,realValue){realValue=String(realValue);switch(attrType){case
html4 .atype.GLOBAL_NAME:case html4 .atype.ID:case html4 .atype.IDREF:return unsuffix(realValue,idSuffix,null);case
html4 .atype.IDREFS:return realValue.replace(ID_LIST_PARTS_PATTERN,function(_,id,spaces){return unsuffix(id,idSuffix,'')+(spaces?' ':'')});case
html4 .atype.URI_FRAGMENT:if(realValue&&'#'===realValue.charAt(0)){realValue=unsuffix(realValue.substring(1),idSuffix,null);return realValue?'#'+realValue:null}else{return null}default:return realValue}}function
tameInnerHtml(htmlText){var out=[];return innerHtmlTamer(htmlText,out),out.join('')}innerHtmlTamer=html.makeSaxParser({'startTag':function(tagName,attribs,out){var
aname,atype,i,value;out.push('<',tagName);for(i=0;i<attribs.length;i+=2)aname=attribs[i],atype=getAttributeType(tagName,aname),value=attribs[i+1],aname!=='target'&&atype!==void
0&&(value=virtualizeAttributeValue(atype,value),typeof value==='string'&&out.push(' ',aname,'=\"',html.escapeAttrib(value),'\"'));out.push('>')},'endTag':function(name,out){out.push('</',name,'>')},'pcdata':function(text,out){out.push(text)},'rcdata':function(text,out){out.push(text)},'cdata':function(text,out){out.push(text)}});function
rewriteAttribute(tagName,attribName,type,value){var css,cssPropertiesAndValues,doesReturn,fnName,i,match,propName,propValue,semi;switch(type){case
html4 .atype.NONE:return String(value);case html4 .atype.CLASSES:return value=String(value),FORBIDDEN_ID_LIST_PATTERN.test(value)?null:value;case
html4 .atype.GLOBAL_NAME:case html4 .atype.ID:case html4 .atype.IDREF:return value=String(value),value&&isValidId(value)?value+idSuffix:null;case
html4 .atype.IDREFS:return value=String(value),value&&isValidIdList(value)?value.replace(ID_LIST_PARTS_PATTERN,function(_,id,spaces){return id+idSuffix+(spaces?' ':'')}):null;case
html4 .atype.LOCAL_NAME:return value=String(value),value&&isValidId(value)?value:null;case
html4 .atype.SCRIPT:return value=String(value),match=value.match(SIMPLE_HANDLER_PATTERN),match?(doesReturn=match[1],fnName=match[2],value=(doesReturn?'return ':'')+'plugin_dispatchEvent___('+'this, event, '+pluginId+', \"'+fnName+'\");',attribName==='onsubmit'&&(value='try { '+value+' } finally { return false; }'),value):null;case
html4 .atype.URI:return value=String(value),uriCallback?uriCallback.rewrite(value,mimeTypeForAttr(tagName,attribName))||null:null;case
html4 .atype.URI_FRAGMENT:return value=String(value),value.charAt(0)==='#'&&isValidId(value.substring(1))?'#'+value+idSuffix:null;case
html4 .atype.STYLE:if('function'!==typeof value)return sanitizeStyleAttrValue(String(value));cssPropertiesAndValues=cssSealerUnsealerPair.unseal(value);if(!cssPropertiesAndValues)return null;css=[];for(i=0;i<cssPropertiesAndValues.length;i+=2)propName=cssPropertiesAndValues[i],propValue=cssPropertiesAndValues[i+1],semi=propName.indexOf(';'),semi>=0&&(propName=propName.substring(0,semi)),css.push(propName+' : '+propValue);return css.join(' ; ');case
html4 .atype.FRAME_TARGET:default:return null}}function makeCache(){var cache=___.newTable(false);return cache.set(null,null),cache.set(void
0,null),cache}editableTameNodeCache=makeCache(),readOnlyTameNodeCache=makeCache();function
defaultTameNode(node,editable){var cache,tagName,tamed;if(node===null||node===void
0)return null;cache=editable?editableTameNodeCache:readOnlyTameNodeCache,tamed=cache.get(node);if(tamed!==void
0)return tamed;switch(node.nodeType){case 1:tagName=node.tagName.toLowerCase();switch(tagName){case'a':tamed=new
TameAElement(node,editable);break;case'form':tamed=new TameFormElement(node,editable);break;case'select':case'button':case'option':case'textarea':case'input':tamed=new
TameInputElement(node,editable);break;case'iframe':tamed=new TameIFrameElement(node,editable);break;case'img':tamed=new
TameImageElement(node,editable);break;case'label':tamed=new TameLabelElement(node,editable);break;case'script':tamed=new
TameScriptElement(node,editable);break;case'td':case'thead':case'tfoot':case'tbody':case'th':tamed=new
TameTableCompElement(node,editable);break;case'tr':tamed=new TameTableRowElement(node,editable);break;case'table':tamed=new
TameTableElement(node,editable);break;default:!html4 .ELEMENTS.hasOwnProperty(tagName)||html4
.ELEMENTS[tagName]&html4 .eflags.UNSAFE?(tamed=new TameOpaqueNode(node,editable)):(tamed=new
TameElement(node,editable,editable))}break;case 2:throw'Internal: Attr nodes cannot be generically wrapped';case
3:case 4:tamed=new TameTextNode(node,editable);break;case 8:tamed=new TameCommentNode(node,editable);break;case
11:tamed=new TameBackedNode(node,editable,editable);break;default:tamed=new TameOpaqueNode(node,editable)}return node.nodeType===1&&cache.set(node,tamed),tamed}function
tameRelatedNode(node,editable,tameNodeCtor){var ancestor,docElem;if(node===null||node===void
0)return null;if(node===tameDocument.body___){if(tameDocument.editable___&&!editable)throw new
Error(NOT_EDITABLE);return tameDocument.getBody___()}try{docElem=node.ownerDocument.documentElement;for(ancestor=node;ancestor;ancestor=ancestor.parentNode)if(idClassPattern.test(ancestor.className))return tameNodeCtor(node,editable);else
if(ancestor===docElem)return null;return tameNodeCtor(node,editable)}catch(e){}return null}function
getNodeListLength(nodeList){var limit=nodeList.length;return limit!==+limit&&(limit=(1/0)),limit}function
mixinNodeList(tamed,nodeList,editable,opt_tameNodeCtor){var limit=getNodeListLength(nodeList),i;if(limit>0&&!opt_tameNodeCtor)throw'Internal: Nonempty mixinNodeList() without a tameNodeCtor';for(i=0;i<limit&&nodeList[i];++i)tamed[i]=opt_tameNodeCtor(nodeList[i],editable);return nodeList=null,tamed.item=___.markFuncFreeze(function(k){k&=2147483647;if(k!==k)throw new
Error;return tamed[k]||null}),tamed}function tameNodeList(nodeList,editable,opt_tameNodeCtor){return ___.freeze(mixinNodeList([],nodeList,editable,opt_tameNodeCtor))}function
tameOptionsList(nodeList,editable,opt_tameNodeCtor){var nl=mixinNodeList([],nodeList,editable,opt_tameNodeCtor);return nl.selectedIndex=+nodeList.selectedIndex,___.grantRead(nl,'selectedIndex'),___.freeze(nl)}function
fakeNodeList(array){return array.item=___.markFuncFreeze(function(i){return array[i]}),___.freeze(array)}function
mixinHTMLCollection(tamed,nodeList,editable,opt_tameNodeCtor){var i,name,tameNode,tameNodesByName;mixinNodeList(tamed,nodeList,editable,opt_tameNodeCtor),tameNodesByName={};for(i=0;i<tamed.length&&(tameNode=tamed[i]);++i)name=tameNode.getAttribute('name'),name&&!(name.charAt(name.length-1)==='_'||name
in tamed||name===String(name&2147483647))&&(tameNodesByName[name]||(tameNodesByName[name]=[]),tameNodesByName[name].push(tameNode));return ___.forOwnKeys(tameNodesByName,___.markFuncFreeze(function(name,tameNodes){tameNodes.length>1?(tamed[name]=fakeNodeList(tameNodes)):(tamed[name]=tameNodes[0])})),tamed.namedItem=___.markFuncFreeze(function(name){return name=String(name),name.charAt(name.length-1)==='_'?null:___.hasOwnProp(tamed,name)?___.passesGuard(TameNodeT,tamed[name])?tamed[name]:tamed[name][0]:null}),tamed}function
tameHTMLCollection(nodeList,editable,opt_tameNodeCtor){return ___.freeze(mixinHTMLCollection([],nodeList,editable,opt_tameNodeCtor))}function
tameGetElementsByTagName(rootNode,tagName,editable){tagName=String(tagName);if(tagName!=='*'){tagName=tagName.toLowerCase();if(!___.hasOwnProp(html4
.ELEMENTS,tagName)||html4 .ELEMENTS[tagName]&html4 .ELEMENTS.UNSAFE)return new fakeNodeList([])}return tameNodeList(rootNode.getElementsByTagName(tagName),editable,defaultTameNode)}function
tameGetElementsByClassName(rootNode,className,editable){var candidate,candidateClass,candidates,classes,classi,i,j,k,limit,matches,nClasses,tamed;className=String(className),classes=className.match(/[^\t\n\f\r ]+/g);for(i=classes?classes.length:0;--i>=0;)classi=classes[i],FORBIDDEN_ID_PATTERN.test(classi)&&(classes[i]=classes[classes.length-1],--classes.length);if(!classes||classes.length===0)return fakeNodeList([]);if(typeof
rootNode.getElementsByClassName==='function')return tameNodeList(rootNode.getElementsByClassName(classes.join(' ')),editable,defaultTameNode);nClasses=classes.length;for(i=nClasses;--i>=0;)classes[i]=' '+classes[i]+' ';candidates=rootNode.getElementsByTagName('*'),matches=[],limit=candidates.length,limit!==+limit&&(limit=(1/0));a:for(j=0,k=-1;j<limit&&(candidate=candidates[j]);++j){candidateClass=' '+candidate.className+' ';for(i=nClasses;--i>=0;)if(-1===candidateClass.indexOf(classes[i]))continue a;tamed=defaultTameNode(candidate,editable),tamed&&(matches[++k]=tamed)}return fakeNodeList(matches)}function
makeEventHandlerWrapper(thisNode,listener){classUtils.ensureValidCallback(listener);function
wrapper(event){return plugin_dispatchEvent___(thisNode,event,___.getId(imports),listener)}return wrapper}NOT_EDITABLE='Node not editable.',INVALID_SUFFIX='Property names may not end in \'__\'.',UNSAFE_TAGNAME='Unsafe tag name.',UNKNOWN_TAGNAME='Unknown tag name.',INDEX_SIZE_ERROR='Index size error.';function
defProperty(ctor,name,useAttrGetter,toValue,useAttrSetter,fromValue){var getterSetterSuffix=classUtils.getterSetterSuffix(name),proto=ctor.prototype;toValue&&(proto['get'+getterSetterSuffix]=useAttrGetter?function(){return toValue.call(this,this.getAttribute(name))}:function(){return toValue.call(this,this.node___[name])}),fromValue&&(proto['set'+getterSetterSuffix]=useAttrSetter?function(value){return this.setAttribute(name,fromValue.call(this,value)),value}:function(value){if(!this.editable___)throw new
Error(NOT_EDITABLE);return this.node___[name]=fromValue.call(this,value),value})}function
defAttributeAlias(ctor,name,toValue,fromValue){defProperty(ctor,name,true,toValue,true,fromValue)}function
tameAddEventListener(name,listener,useCapture){var wrappedListener;if(!this.editable___)throw new
Error(NOT_EDITABLE);this.wrappedListeners___||(this.wrappedListeners___=[]),useCapture=Boolean(useCapture),wrappedListener=makeEventHandlerWrapper(this.node___,listener),wrappedListener=bridal.addEventListener(this.node___,name,wrappedListener,useCapture),wrappedListener.originalListener___=listener,this.wrappedListeners___.push(wrappedListener)}function
tameRemoveEventListener(name,listener,useCapture){var i,wrappedListener;if(!this.editable___)throw new
Error(NOT_EDITABLE);if(!this.wrappedListeners___)return;wrappedListener=null;for(i=this.wrappedListeners___.length;--i>=0;)if(this.wrappedListeners___[i].originalListener___===listener){wrappedListener=this.wrappedListeners___[i],arrayRemove(this.wrappedListeners___,i,i);break}if(!wrappedListener)return;bridal.removeEventListener(this.node___,name,wrappedListener,useCapture)}nodeClasses={};function
inertCtor(tamedCtor,someSuper,name){return nodeClasses[name]=___.extend(tamedCtor,someSuper,name)}tameNodeFields=['nodeType','nodeValue','nodeName','firstChild','lastChild','nextSibling','previousSibling','parentNode','ownerDocument','childNodes','attributes'];function
TameNode(editable){this.editable___=editable,TameNodeMark.stamp.mark___(this),classUtils.exportFields(this,tameNodeFields)}inertCtor(TameNode,Object,'Node'),TameNode.prototype.getOwnerDocument___=function(){if(!this.editable___&&tameDocument.editable___)throw new
Error(NOT_EDITABLE);return tameDocument},tameNodePublicMembers=['cloneNode','appendChild','insertBefore','removeChild','replaceChild','getElementsByClassName','getElementsByTagName','dispatchEvent','hasChildNodes'];function
TameBackedNode(node,editable,childrenEditable){if(!node)throw new Error('Creating tame node with undefined native delegate');this.node___=node,this.childrenEditable___=editable&&childrenEditable,TameNode.call(this,editable)}___.extend(TameBackedNode,TameNode),TameBackedNode.prototype.getNodeType___=function(){return this.node___.nodeType},TameBackedNode.prototype.getNodeName___=function(){return this.node___.nodeName},TameBackedNode.prototype.getNodeValue___=function(){return this.node___.nodeValue},TameBackedNode.prototype.cloneNode=function(deep){var
clone=bridal.cloneNode(this.node___,Boolean(deep));return defaultTameNode(clone,true)},TameBackedNode.prototype.appendChild=function(child){child=TameNodeT.coerce(child);if(!this.childrenEditable___||!child.editable___)throw new
Error(NOT_EDITABLE);return this.node___.appendChild(child.node___),child},TameBackedNode.prototype.insertBefore=function(toInsert,child){toInsert=TameNodeT.coerce(toInsert),child===void
0&&(child=null);if(child!==null){child=TameNodeT.coerce(child);if(!child.editable___)throw new
Error(NOT_EDITABLE)}if(!this.childrenEditable___||!toInsert.editable___)throw new
Error(NOT_EDITABLE);return this.node___.insertBefore(toInsert.node___,child!==null?child.node___:null),toInsert},TameBackedNode.prototype.removeChild=function(child){child=TameNodeT.coerce(child);if(!this.childrenEditable___||!child.editable___)throw new
Error(NOT_EDITABLE);return this.node___.removeChild(child.node___),child},TameBackedNode.prototype.replaceChild=function(newChild,oldChild){newChild=TameNodeT.coerce(newChild),oldChild=TameNodeT.coerce(oldChild);if(!this.childrenEditable___||!newChild.editable___||!oldChild.editable___)throw new
Error(NOT_EDITABLE);return this.node___.replaceChild(newChild.node___,oldChild.node___),oldChild},TameBackedNode.prototype.getFirstChild___=function(){return defaultTameNode(this.node___.firstChild,this.childrenEditable___)},TameBackedNode.prototype.getLastChild___=function(){return defaultTameNode(this.node___.lastChild,this.childrenEditable___)},TameBackedNode.prototype.getNextSibling___=function(){return tameRelatedNode(this.node___.nextSibling,this.editable___,defaultTameNode)},TameBackedNode.prototype.getPreviousSibling___=function(){return tameRelatedNode(this.node___.previousSibling,this.editable___,defaultTameNode)},TameBackedNode.prototype.getParentNode___=function(){return tameRelatedNode(this.node___.parentNode,this.editable___,defaultTameNode)},TameBackedNode.prototype.getElementsByTagName=function(tagName){return tameGetElementsByTagName(this.node___,tagName,this.childrenEditable___)},TameBackedNode.prototype.getElementsByClassName=function(className){return tameGetElementsByClassName(this.node___,className,this.childrenEditable___)},TameBackedNode.prototype.getChildNodes___=function(){return tameNodeList(this.node___.childNodes,this.childrenEditable___,defaultTameNode)},TameBackedNode.prototype.getAttributes___=function(){var
thisNode=this.node___,tameNodeCtor=function(node,editable){return new TameBackedAttributeNode(node,editable,thisNode)};return tameNodeList(this.node___.attributes,this.editable___,tameNodeCtor)},endsWith__=/__$/,TameBackedNode.prototype.handleRead___=function(name){var
handlerName;return name=String(name),endsWith__.test(name)?void 0:(handlerName=name+'_getter___',this[handlerName]?this[handlerName]():(handlerName=handlerName.toLowerCase(),this[handlerName]?this[handlerName]():___.hasOwnProp(this.node___.properties___,name)?this.node___.properties___[name]:void
0))},TameBackedNode.prototype.handleCall___=function(name,args){var handlerName;name=String(name);if(endsWith__.test(name))throw new
Error(INVALID_SUFFIX);handlerName=name+'_handler___';if(this[handlerName])return this[handlerName].call(this,args);handlerName=handlerName.toLowerCase();if(this[handlerName])return this[handlerName].call(this,args);if(___.hasOwnProp(this.node___.properties___,name))return this.node___.properties___[name].call(this,args);throw new
TypeError(name+' is not a function.')},TameBackedNode.prototype.handleSet___=function(name,val){var
handlerName;name=String(name);if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);if(!this.editable___)throw new
Error(NOT_EDITABLE);return handlerName=name+'_setter___',this[handlerName]?this[handlerName](val):(handlerName=handlerName.toLowerCase(),this[handlerName]?this[handlerName](val):(this.node___.properties___||(this.node___.properties___={}),this[name+'_canEnum___']=true,this.node___.properties___[name]=val))},TameBackedNode.prototype.handleDelete___=function(name){var
handlerName;name=String(name);if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);if(!this.editable___)throw new
Error(NOT_EDITABLE);return handlerName=name+'_deleter___',this[handlerName]?this[handlerName]():(handlerName=handlerName.toLowerCase(),this[handlerName]?this[handlerName]():this.node___.properties___?delete
this.node___.properties___[name]&&delete this[name+'_canEnum___']:true)},TameBackedNode.prototype.handleEnum___=function(ownFlag){return this.node___.properties___?___.allKeys(this.node___.properties___):[]},TameBackedNode.prototype.hasChildNodes=function(){return!!this.node___.hasChildNodes()},TameBackedNode.prototype.dispatchEvent=function
dispatchEvent(evt){evt=TameEventT.coerce(evt),bridal.dispatchEvent(this.node___,evt.event___)},___.all2(___.grantTypedMethod,TameBackedNode.prototype,tameNodePublicMembers),document.documentElement.contains&&(TameBackedNode.prototype.contains=function(other){var
otherNode;return other=TameNodeT.coerce(other),otherNode=other.node___,this.node___.contains(otherNode)}),'function'===typeof
document.documentElement.compareDocumentPosition&&(TameBackedNode.prototype.compareDocumentPosition=function(other){var
bitmask,otherNode;return other=TameNodeT.coerce(other),otherNode=other.node___,otherNode?(bitmask=+this.node___.compareDocumentPosition(otherNode),bitmask&1&&(bitmask&=-7),bitmask&31):0},___.hasOwnProp(TameBackedNode.prototype,'contains')||(TameBackedNode.prototype.contains=function(other){var
docPos=this.compareDocumentPosition(other);return!(!(docPos&16)&&docPos)})),___.all2(function(o,k){___.hasOwnProp(o,k)&&___.grantTypedMethod(o,k)},TameBackedNode.prototype,['contains','compareDocumentPosition']);function
TamePseudoNode(editable){TameNode.call(this,editable),this.properties___={}}___.extend(TamePseudoNode,TameNode),TamePseudoNode.prototype.appendChild=TamePseudoNode.prototype.insertBefore=TamePseudoNode.prototype.removeChild=TamePseudoNode.prototype.replaceChild=function(){return ___.log('Node not editable; no action performed.'),void
0},TamePseudoNode.prototype.getFirstChild___=function(){var children=this.getChildNodes___();return children.length?children[0]:null},TamePseudoNode.prototype.getLastChild___=function(){var
children=this.getChildNodes___();return children.length?children[children.length-1]:null},TamePseudoNode.prototype.getNextSibling___=function(){var
parentNode=this.getParentNode___(),i,siblings;if(!parentNode)return null;siblings=parentNode.getChildNodes___();for(i=siblings.length-1;--i>=0;)if(siblings[i]===this)return siblings[i+1];return null},TamePseudoNode.prototype.getPreviousSibling___=function(){var
parentNode=this.getParentNode___(),i,siblings;if(!parentNode)return null;siblings=parentNode.getChildNodes___();for(i=siblings.length;--i>=1;)if(siblings[i]===this)return siblings[i-1];return null},TamePseudoNode.prototype.handleRead___=function(name){var
handlerName;return name=String(name),endsWith__.test(name)?void 0:(handlerName=name+'_getter___',this[handlerName]?this[handlerName]():(handlerName=handlerName.toLowerCase(),this[handlerName]?this[handlerName]():___.hasOwnProp(this.properties___,name)?this.properties___[name]:void
0))},TamePseudoNode.prototype.handleCall___=function(name,args){var handlerName;name=String(name);if(endsWith__.test(name))throw new
Error(INVALID_SUFFIX);handlerName=name+'_handler___';if(this[handlerName])return this[handlerName].call(this,args);handlerName=handlerName.toLowerCase();if(this[handlerName])return this[handlerName].call(this,args);if(___.hasOwnProp(this.properties___,name))return this.properties___[name].call(this,args);throw new
TypeError(name+' is not a function.')},TamePseudoNode.prototype.handleSet___=function(name,val){var
handlerName;name=String(name);if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);if(!this.editable___)throw new
Error(NOT_EDITABLE);return handlerName=name+'_setter___',this[handlerName]?this[handlerName](val):(handlerName=handlerName.toLowerCase(),this[handlerName]?this[handlerName](val):(this.properties___||(this.properties___={}),this[name+'_canEnum___']=true,this.properties___[name]=val))},TamePseudoNode.prototype.handleDelete___=function(name){var
handlerName;name=String(name);if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);if(!this.editable___)throw new
Error(NOT_EDITABLE);return handlerName=name+'_deleter___',this[handlerName]?this[handlerName]():(handlerName=handlerName.toLowerCase(),this[handlerName]?this[handlerName]():this.properties___?delete
this.properties___[name]&&delete this[name+'_canEnum___']:true)},TamePseudoNode.prototype.handleEnum___=function(ownFlag){return this.properties___?___.allKeys(this.properties___):[]},TamePseudoNode.prototype.hasChildNodes=function(){return this.getFirstChild___()!=null},___.all2(___.grantTypedMethod,TamePseudoNode.prototype,tameNodePublicMembers),commonElementPropertyHandlers={'clientWidth':{'get':function(){return this.getGeometryDelegate___().clientWidth}},'clientHeight':{'get':function(){return this.getGeometryDelegate___().clientHeight}},'offsetLeft':{'get':function(){return this.getGeometryDelegate___().offsetLeft}},'offsetTop':{'get':function(){return this.getGeometryDelegate___().offsetTop}},'offsetWidth':{'get':function(){return this.getGeometryDelegate___().offsetWidth}},'offsetHeight':{'get':function(){return this.getGeometryDelegate___().offsetHeight}},'scrollLeft':{'get':function(){return this.getGeometryDelegate___().scrollLeft},'set':function(x){if(!this.editable___)throw new
Error(NOT_EDITABLE);return this.getGeometryDelegate___().scrollLeft=+x,x}},'scrollTop':{'get':function(){return this.getGeometryDelegate___().scrollTop},'set':function(y){if(!this.editable___)throw new
Error(NOT_EDITABLE);return this.getGeometryDelegate___().scrollTop=+y,y}},'scrollWidth':{'get':function(){return this.getGeometryDelegate___().scrollWidth}},'scrollHeight':{'get':function(){return this.getGeometryDelegate___().scrollHeight}}};function
TamePseudoElement(tagName,tameDoc,childNodesGetter,parentNodeGetter,innerHTMLGetter,geometryDelegate,editable){TamePseudoNode.call(this,editable),this.tagName___=tagName,this.tameDoc___=tameDoc,this.childNodesGetter___=childNodesGetter,this.parentNodeGetter___=parentNodeGetter,this.innerHTMLGetter___=innerHTMLGetter,this.geometryDelegate___=geometryDelegate,classUtils.exportFields(this,['tagName','innerHTML']),classUtils.applyAccessors(this,commonElementPropertyHandlers)}___.extend(TamePseudoElement,TamePseudoNode),TamePseudoElement.prototype.getNodeType___=function(){return 1},TamePseudoElement.prototype.getNodeName___=TamePseudoElement.prototype.getTagName___=function(){return this.tagName___},TamePseudoElement.prototype.getNodeValue___=function(){return null},TamePseudoElement.prototype.getAttribute=function(attribName){return null},TamePseudoElement.prototype.setAttribute=function(attribName,value){},TamePseudoElement.prototype.hasAttribute=function(attribName){return false},TamePseudoElement.prototype.removeAttribute=function(attribName){},TamePseudoElement.prototype.getOwnerDocument___=function(){return this.tameDoc___},TamePseudoElement.prototype.getChildNodes___=function(){return this.childNodesGetter___()},TamePseudoElement.prototype.getAttributes___=function(){return tameNodeList([],false,undefined)},TamePseudoElement.prototype.getParentNode___=function(){return this.parentNodeGetter___()},TamePseudoElement.prototype.getInnerHTML___=function(){return this.innerHTMLGetter___()},TamePseudoElement.prototype.getElementsByTagName=function(tagName){return tagName=String(tagName).toLowerCase(),tagName===this.tagName___?fakeNodeList([]):this.getOwnerDocument___().getElementsByTagName(tagName)},TamePseudoElement.prototype.getElementsByClassName=function(className){return this.getOwnerDocument___().getElementsByClassName(className)},TamePseudoElement.prototype.getBoundingClientRect=function(){return this.geometryDelegate___.getBoundingClientRect()},TamePseudoElement.prototype.getGeometryDelegate___=function(){return this.geometryDelegate___},TamePseudoElement.prototype.toString=___.markFuncFreeze(function(){return'<'+this.tagName___+'>'}),___.all2(___.grantTypedMethod,TamePseudoElement.prototype,['getAttribute','setAttribute','hasAttribute','removeAttribute','getBoundingClientRect','getElementsByTagName']);function
TameOpaqueNode(node,editable){TameBackedNode.call(this,node,editable,editable)}___.extend(TameOpaqueNode,TameBackedNode),TameOpaqueNode.prototype.getNodeValue___=TameBackedNode.prototype.getNodeValue___,TameOpaqueNode.prototype.getNodeType___=TameBackedNode.prototype.getNodeType___,TameOpaqueNode.prototype.getNodeName___=TameBackedNode.prototype.getNodeName___,TameOpaqueNode.prototype.getNextSibling___=TameBackedNode.prototype.getNextSibling___,TameOpaqueNode.prototype.getPreviousSibling___=TameBackedNode.prototype.getPreviousSibling___,TameOpaqueNode.prototype.getFirstChild___=TameBackedNode.prototype.getFirstChild___,TameOpaqueNode.prototype.getLastChild___=TameBackedNode.prototype.getLastChild___,TameOpaqueNode.prototype.getParentNode___=TameBackedNode.prototype.getParentNode___,TameOpaqueNode.prototype.getChildNodes___=TameBackedNode.prototype.getChildNodes___,TameOpaqueNode.prototype.getOwnerDocument___=TameBackedNode.prototype.getOwnerDocument___,TameOpaqueNode.prototype.getElementsByTagName=TameBackedNode.prototype.getElementsByTagName,TameOpaqueNode.prototype.getElementsByClassName=TameBackedNode.prototype.getElementsByClassName,TameOpaqueNode.prototype.hasChildNodes=TameBackedNode.prototype.hasChildNodes,TameOpaqueNode.prototype.getAttributes___=function(){return tameNodeList([],false,undefined)};for(i=tameNodePublicMembers.length;--i>=0;)k=tameNodePublicMembers[i],TameOpaqueNode.prototype.hasOwnProperty(k)||(TameOpaqueNode.prototype[k]=___.markFuncFreeze(function(){throw new
Error('Node is opaque')}));___.all2(___.grantTypedMethod,TameOpaqueNode.prototype,tameNodePublicMembers);function
TameTextNode(node,editable){var pn;assert(node.nodeType===3),pn=node.parentNode,editable&&pn&&(1===pn.nodeType&&html4
.ELEMENTS[pn.tagName.toLowerCase()]&html4 .eflags.UNSAFE&&(editable=false)),TameBackedNode.call(this,node,editable,editable),classUtils.exportFields(this,['nodeValue','data','textContent','innerText'])}inertCtor(TameTextNode,TameBackedNode,'Text'),TameTextNode.prototype.setNodeValue___=function(value){if(!this.editable___)throw new
Error(NOT_EDITABLE);return this.node___.nodeValue=String(value||''),value},TameTextNode.prototype.getTextContent___=TameTextNode.prototype.getInnerText___=TameTextNode.prototype.getData___=TameTextNode.prototype.getNodeValue___,TameTextNode.prototype.setTextContent___=TameTextNode.prototype.setInnerText___=TameTextNode.prototype.setData___=TameTextNode.prototype.setNodeValue___,TameTextNode.prototype.toString=___.markFuncFreeze(function(){return'#text'});function
TameCommentNode(node,editable){assert(node.nodeType===8),TameBackedNode.call(this,node,editable,editable)}inertCtor(TameCommentNode,TameBackedNode,'CommentNode'),TameCommentNode.prototype.toString=___.markFuncFreeze(function(){return'#comment'});function
getAttributeType(tagName,attribName){var attribKey=tagName+'::'+attribName;return html4
.ATTRIBS.hasOwnProperty(attribKey)?html4 .ATTRIBS[attribKey]:(attribKey='*::'+attribName,html4
.ATTRIBS.hasOwnProperty(attribKey)?html4 .ATTRIBS[attribKey]:void 0)}function TameBackedAttributeNode(node,editable,ownerElement){TameBackedNode.call(this,node,editable),this.ownerElement___=ownerElement,classUtils.exportFields(this,['name','specified','value','ownerElement'])}inertCtor(TameBackedAttributeNode,TameBackedNode,'Attr'),TameBackedAttributeNode.prototype.getNodeName___=TameBackedAttributeNode.prototype.getName___=function(){return String(this.node___.name)},TameBackedAttributeNode.prototype.getSpecified___=function(){return defaultTameNode(this.ownerElement___,this.editable___).hasAttribute(this.getName___())},TameBackedAttributeNode.prototype.getNodeValue___=TameBackedAttributeNode.prototype.getValue___=function(){return defaultTameNode(this.ownerElement___,this.editable___).getAttribute(this.getName___())},TameBackedAttributeNode.prototype.setNodeValue___=TameBackedAttributeNode.prototype.setValue___=function(value){return defaultTameNode(this.ownerElement___,this.editable___).setAttribute(this.getName___(),value)},TameBackedAttributeNode.prototype.getOwnerElement___=function(){return defaultTameNode(this.ownerElement___,this.editable___)},TameBackedAttributeNode.prototype.getNodeType___=function(){return 2},TameBackedAttributeNode.prototype.cloneNode=function(deep){var
clone=bridal.cloneNode(this.node___,Boolean(deep));return new TameBackedAttributeNode(clone,true,this.ownerElement____)},TameBackedAttributeNode.prototype.appendChild=TameBackedAttributeNode.prototype.insertBefore=TameBackedAttributeNode.prototype.removeChild=TameBackedAttributeNode.prototype.replaceChild=TameBackedAttributeNode.prototype.getFirstChild___=TameBackedAttributeNode.prototype.getLastChild___=TameBackedAttributeNode.prototype.getNextSibling___=TameBackedAttributeNode.prototype.getPreviousSibling___=TameBackedAttributeNode.prototype.getParentNode___=TameBackedAttributeNode.prototype.getElementsByTagName=TameBackedAttributeNode.prototype.getElementsByClassName=TameBackedAttributeNode.prototype.getChildNodes___=TameBackedAttributeNode.prototype.getAttributes___=function(){throw new
Error('Not implemented.')},TameBackedAttributeNode.prototype.toString=___.markFuncFreeze(function(){return'[Fake attribute node]'});function
registerElementScriptAttributeHandlers(aTameElement){var attrNameRe=/::(.*)/,html4Attrib;for(html4Attrib
in html4 .ATTRIBS)html4 .atype.SCRIPT===html4 .ATTRIBS[html4Attrib]&&(function(attribName){___.useSetHandler(aTameElement,attribName,function
eventHandlerSetter(listener){if(!this.editable___)throw new Error(NOT_EDITABLE);return listener?(this.node___[attribName]=makeEventHandlerWrapper(this.node___,listener)):(this.node___[attribName]=null),listener})})((html4Attrib.match(attrNameRe))[1])}function
TameElement(node,editable,childrenEditable){assert(node.nodeType===1),TameBackedNode.call(this,node,editable,childrenEditable),classUtils.exportFields(this,['className','id','innerHTML','tagName','style','offsetParent','title','dir','innerText','textContent']),classUtils.applyAccessors(this,commonElementPropertyHandlers),registerElementScriptAttributeHandlers(this)}nodeClasses.Element=inertCtor(TameElement,TameBackedNode,'HTMLElement'),TameElement.prototype.blur=function(){this.node___.blur()},TameElement.prototype.focus=function(){imports.isProcessingEvent___&&this.node___.focus()},document.documentElement.setActive&&(TameElement.prototype.setActive=function(){imports.isProcessingEvent___&&this.node___.setActive()},___.grantTypedMethod(TameElement.prototype,'setActive')),document.documentElement.hasFocus&&(TameElement.prototype.hasFocus=function(){return this.node___.hasFocus()},___.grantTypedMethod(TameElement.prototype,'hasFocus')),defAttributeAlias(TameElement,'id',defaultToEmptyStr,identity),TameElement.prototype.getAttribute=function(attribName){var
atype,tagName,value;return attribName=String(attribName).toLowerCase(),tagName=this.node___.tagName.toLowerCase(),atype=getAttributeType(tagName,attribName),atype===void
0?this.node___.attributes___?this.node___.attributes___[attribName]||null:null:(value=bridal.getAttribute(this.node___,attribName),'string'!==typeof
value?value:virtualizeAttributeValue(atype,value))},TameElement.prototype.getAttributeNode=function(name){var
hostDomNode=this.node___.getAttributeNode(name);return hostDomNode===null?null:new
TameBackedAttributeNode(hostDomNode,this.editable___,this.node___)},TameElement.prototype.hasAttribute=function(attribName){var
atype,tagName;return attribName=String(attribName).toLowerCase(),tagName=this.node___.tagName.toLowerCase(),atype=getAttributeType(tagName,attribName),atype===void
0?!!(this.node___.attributes___&&___.hasOwnProp(this.node___.attributes___,attribName)):bridal.hasAttribute(this.node___,attribName)},TameElement.prototype.setAttribute=function(attribName,value){var
atype,sanitizedValue,tagName;if(!this.editable___)throw new Error(NOT_EDITABLE);return attribName=String(attribName).toLowerCase(),tagName=this.node___.tagName.toLowerCase(),atype=getAttributeType(tagName,attribName),atype===void
0?(this.node___.attributes___||(this.node___.attributes___={}),this.node___.attributes___[attribName]=String(value)):(sanitizedValue=rewriteAttribute(tagName,attribName,atype,value),sanitizedValue!==null&&bridal.setAttribute(this.node___,attribName,sanitizedValue)),value},TameElement.prototype.removeAttribute=function(attribName){var
atype,tagName;if(!this.editable___)throw new Error(NOT_EDITABLE);attribName=String(attribName).toLowerCase(),tagName=this.node___.tagName.toLowerCase(),atype=getAttributeType(tagName,attribName),atype===void
0?this.node___.attributes___&&delete this.node___.attributes___[attribName]:this.node___.removeAttribute(attribName)},TameElement.prototype.getBoundingClientRect=function(){var
elRect=bridal.getBoundingClientRect(this.node___),vbody=bridal.getBoundingClientRect(this.getOwnerDocument___().body___),vbodyLeft=vbody.left,vbodyTop=vbody.top;return{'top':elRect.top-vbodyTop,'left':elRect.left-vbodyLeft,'right':elRect.right-vbodyLeft,'bottom':elRect.bottom-vbodyTop}},TameElement.prototype.getClassName___=function(){return this.getAttribute('class')||''},TameElement.prototype.setClassName___=function(classes){if(!this.editable___)throw new
Error(NOT_EDITABLE);return this.setAttribute('class',String(classes))};function
defaultToEmptyStr(x){return x||''}defAttributeAlias(TameElement,'title',defaultToEmptyStr,String),defAttributeAlias(TameElement,'dir',defaultToEmptyStr,String);function
innerTextOf(rawNode,out){var c,tagName;switch(rawNode.nodeType){case 1:tagName=rawNode.tagName.toLowerCase();if(html4
.ELEMENTS.hasOwnProperty(tagName)&&!(html4 .ELEMENTS[tagName]&html4 .eflags.UNSAFE))for(c=rawNode.firstChild;c;c=c.nextSibling)innerTextOf(c,out);break;case
3:case 4:out[out.length]=rawNode.data;break;case 11:for(c=rawNode.firstChild;c;c=c.nextSibling)innerTextOf(c,out)}}TameElement.prototype.getTextContent___=TameElement.prototype.getInnerText___=function(){var
text=[];return innerTextOf(this.node___,text),text.join('')},TameElement.prototype.setTextContent___=TameElement.prototype.setInnerText___=function(newText){var
c,el,newTextStr;if(!this.editable___)throw new Error(NOT_EDITABLE);newTextStr=newText!=null?String(newText):'',el=this.node___;for(;c=el.firstChild;)el.removeChild(c);return newTextStr&&el.appendChild(el.ownerDocument.createTextNode(newTextStr)),newText},TameElement.prototype.getTagName___=TameBackedNode.prototype.getNodeName___,TameElement.prototype.getInnerHTML___=function(){var
tagName=this.node___.tagName.toLowerCase(),flags,innerHtml;return html4 .ELEMENTS.hasOwnProperty(tagName)?(flags=html4
.ELEMENTS[tagName],innerHtml=this.node___.innerHTML,flags&html4 .eflags.CDATA?(innerHtml=html.escapeAttrib(innerHtml)):flags&html4
.eflags.RCDATA?(innerHtml=html.normalizeRCData(innerHtml)):(innerHtml=tameInnerHtml(innerHtml)),innerHtml):''},TameElement.prototype.setInnerHTML___=function(htmlFragment){var
flags,htmlFragmentString,isRcData,sanitizedHtml,tagName;if(!this.editable___)throw new
Error(NOT_EDITABLE);tagName=this.node___.tagName.toLowerCase();if(!html4 .ELEMENTS.hasOwnProperty(tagName))throw new
Error;flags=html4 .ELEMENTS[tagName];if(flags&html4 .eflags.UNSAFE)throw new Error;return isRcData=flags&html4
.eflags.RCDATA,!isRcData&&htmlFragment instanceof Html?(htmlFragmentString=''+safeHtml(htmlFragment)):htmlFragment===null?(htmlFragmentString=''):(htmlFragmentString=''+htmlFragment),sanitizedHtml=isRcData?html.normalizeRCData(htmlFragmentString):sanitizeHtml(htmlFragmentString),this.node___.innerHTML=sanitizedHtml,htmlFragment};function
identity(x){return x}defProperty(TameElement,'style',false,function(styleNode){return new
TameStyle(styleNode,this.editable___,this)},true,identity),TameElement.prototype.updateStyle=function(style){var
cssPropertiesAndValues,i,propName,propValue,semi,styleNode;if(!this.editable___)throw new
Error(NOT_EDITABLE);cssPropertiesAndValues=cssSealerUnsealerPair.unseal(style);if(!cssPropertiesAndValues)throw new
Error;styleNode=this.node___.style;for(i=0;i<cssPropertiesAndValues.length;i+=2)propName=cssPropertiesAndValues[i],propValue=cssPropertiesAndValues[i+1],semi=propName.indexOf(';'),semi>=0&&(propName=propName.substring(semi+1)),styleNode[propName]=propValue},TameElement.prototype.getOffsetParent___=function(){return tameRelatedNode(this.node___.offsetParent,this.editable___,defaultTameNode)},TameElement.prototype.getGeometryDelegate___=function(){return this.node___},TameElement.prototype.toString=___.markFuncFreeze(function(){return'<'+this.node___.tagName+'>'}),TameElement.prototype.addEventListener=tameAddEventListener,TameElement.prototype.removeEventListener=tameRemoveEventListener,___.all2(___.grantTypedMethod,TameElement.prototype,['addEventListener','removeEventListener','blur','focus','getAttribute','setAttribute','removeAttribute','hasAttribute','getAttributeNode','getBoundingClientRect','updateStyle']);function
TameAElement(node,editable){TameElement.call(this,node,editable,editable),classUtils.exportFields(this,['href'])}inertCtor(TameAElement,TameElement,'HTMLAnchorElement'),defProperty(TameAElement,'href',false,identity,true,identity);function
TameFormElement(node,editable){TameElement.call(this,node,editable,editable),this.length=node.length,classUtils.exportFields(this,['action','elements','enctype','method','target'])}inertCtor(TameFormElement,TameElement,'HTMLFormElement'),TameFormElement.prototype.handleRead___=function(name){var
tameElements;name=String(name);if(endsWith__.test(name))return;if(___.passesGuard(TameNodeT,this)){tameElements=this.getElements___();if(___.hasOwnProp(tameElements,name))return tameElements[name]}return TameBackedNode.prototype.handleRead___.call(this,name)},TameFormElement.prototype.submit=function(){return this.node___.submit()},TameFormElement.prototype.reset=function(){return this.node___.reset()},defAttributeAlias(TameFormElement,'action',defaultToEmptyStr,String),TameFormElement.prototype.getElements___=function(){return tameHTMLCollection(this.node___.elements,this.editable___,defaultTameNode)},defAttributeAlias(TameFormElement,'enctype',defaultToEmptyStr,String),defAttributeAlias(TameFormElement,'method',defaultToEmptyStr,String),defAttributeAlias(TameFormElement,'target',defaultToEmptyStr,String),TameFormElement.prototype.reset=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);this.node___.reset()},TameFormElement.prototype.submit=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);this.node___.submit()},___.all2(___.grantTypedMethod,TameFormElement.prototype,['reset','submit']);function
TameInputElement(node,editable){TameElement.call(this,node,editable,editable),classUtils.exportFields(this,['form','value','defaultValue','checked','disabled','readOnly','options','selected','selectedIndex','name','accessKey','tabIndex','text','defaultChecked','defaultSelected','maxLength','size','type','index','label','multiple','cols','rows'])}inertCtor(TameInputElement,TameElement,'HTMLInputElement'),defProperty(TameInputElement,'checked',false,identity,false,Boolean),defProperty(TameInputElement,'defaultChecked',false,identity,false,identity),defProperty(TameInputElement,'value',false,function(x){return x==null?null:String(x)},false,function(x){return x==null?'':''+x}),defProperty(TameInputElement,'defaultValue',false,function(x){return x==null?null:String(x)},false,function(x){return x==null?'':''+x}),TameInputElement.prototype.select=function(){this.node___.select()},TameInputElement.prototype.getForm___=function(){return tameRelatedNode(this.node___.form,this.editable___,defaultTameNode)},defProperty(TameInputElement,'disabled',false,identity,false,identity),defProperty(TameInputElement,'readOnly',false,identity,false,identity),TameInputElement.prototype.getOptions___=function(){return tameOptionsList(this.node___.options,this.editable___,defaultTameNode,'name')},defProperty(TameInputElement,'selected',false,identity,false,identity),defProperty(TameInputElement,'defaultSelected',false,identity,false,Boolean);function
toInt(x){return x|0}defProperty(TameInputElement,'selectedIndex',false,identity,false,toInt),defProperty(TameInputElement,'name',false,identity,false,identity),defProperty(TameInputElement,'accessKey',false,identity,false,identity),defProperty(TameInputElement,'tabIndex',false,identity,false,identity),defProperty(TameInputElement,'text',false,String),defProperty(TameInputElement,'maxLength',false,identity,false,identity),defProperty(TameInputElement,'size',false,identity,false,identity),defProperty(TameInputElement,'type',false,identity,false,identity),defProperty(TameInputElement,'index',false,identity,false,identity),defProperty(TameInputElement,'label',false,identity,false,identity),defProperty(TameInputElement,'multiple',false,identity,false,identity),defProperty(TameInputElement,'cols',false,identity,false,identity),defProperty(TameInputElement,'rows',false,identity,false,identity),___.all2(___.grantTypedMethod,TameInputElement.prototype,['select']);function
TameImageElement(node,editable){TameElement.call(this,node,editable,editable),classUtils.exportFields(this,['src','alt'])}inertCtor(TameImageElement,TameElement,'HTMLImageElement'),defProperty(TameImageElement,'src',false,identity,true,identity),defProperty(TameImageElement,'alt',false,identity,false,String);function
TameLabelElement(node,editable){TameElement.call(this,node,editable,editable),classUtils.exportFields(this,['htmlFor'])}inertCtor(TameLabelElement,TameElement,'HTMLLabelElement'),TameLabelElement.prototype.getHtmlFor___=function(){return this.getAttribute('for')},TameLabelElement.prototype.setHtmlFor___=function(id){return this.setAttribute('for',id),id};function
TameScriptElement(node,editable){TameElement.call(this,node,editable,false),classUtils.exportFields(this,['src'])}inertCtor(TameScriptElement,TameElement,'HTMLScriptElement'),defProperty(TameScriptElement,'src',false,identity,true,identity);function
TameIFrameElement(node,editable){TameElement.call(this,node,editable,false),classUtils.exportFields(this,['align','frameBorder','height','width'])}inertCtor(TameIFrameElement,TameElement,'HTMLIFrameElement'),TameIFrameElement.prototype.getAlign___=function(){return this.node___.align},TameIFrameElement.prototype.setAlign___=function(alignment){if(!this.editable___)throw new
Error(NOT_EDITABLE);alignment=String(alignment),(alignment==='left'||alignment==='right'||alignment==='center')&&(this.node___.align=alignment)},TameIFrameElement.prototype.getAttribute=function(attr){var
attrLc=String(attr).toLowerCase();return attrLc!=='name'&&attrLc!=='src'?TameElement.prototype.getAttribute.call(this,attr):null},TameIFrameElement.prototype.setAttribute=function(attr,value){var
attrLc=String(attr).toLowerCase();return attrLc!=='name'&&attrLc!=='src'?TameElement.prototype.setAttribute.call(this,attr,value):(___.log('Cannot set the ['+attrLc+'] attribute of an iframe.'),value)},TameIFrameElement.prototype.getFrameBorder___=function(){return this.node___.frameBorder},TameIFrameElement.prototype.setFrameBorder___=function(border){if(!this.editable___)throw new
Error(NOT_EDITABLE);border=String(border).toLowerCase(),(border==='0'||border==='1'||border==='no'||border==='yes')&&(this.node___.frameBorder=border)},defProperty(TameIFrameElement,'height',false,identity,false,Number),defProperty(TameIFrameElement,'width',false,identity,false,Number),TameIFrameElement.prototype.handleRead___=function(name){var
nameLc=String(name).toLowerCase();return nameLc!=='src'&&nameLc!=='name'?TameElement.prototype.handleRead___.call(this,name):undefined},TameIFrameElement.prototype.handleSet___=function(name,value){var
nameLc=String(name).toLowerCase();return nameLc!=='src'&&nameLc!=='name'?TameElement.prototype.handleSet___.call(this,name,value):(___.log('Cannot set the ['+nameLc+'] property of an iframe.'),value)},___.all2(___.grantTypedMethod,TameIFrameElement.prototype,['getAttribute','setAttribute']);function
TameTableCompElement(node,editable){TameElement.call(this,node,editable,editable),classUtils.exportFields(this,['colSpan','cells','cellIndex','rowSpan','rows','rowIndex','align','vAlign','nowrap','sectionRowIndex'])}___.extend(TameTableCompElement,TameElement),defProperty(TameTableCompElement,'colSpan',false,identity,false,identity),TameTableCompElement.prototype.getCells___=function(){return tameNodeList(this.node___.cells,this.editable___,defaultTameNode)},TameTableCompElement.prototype.getCellIndex___=function(){return this.node___.cellIndex},defProperty(TameTableCompElement,'rowSpan',false,identity,false,identity),TameTableCompElement.prototype.getRows___=function(){return tameNodeList(this.node___.rows,this.editable___,defaultTameNode)},TameTableCompElement.prototype.getRowIndex___=function(){return this.node___.rowIndex},TameTableCompElement.prototype.getSectionRowIndex___=function(){return this.node___.sectionRowIndex},defProperty(TameTableCompElement,'align',false,identity,false,identity),defProperty(TameTableCompElement,'vAlign',false,identity,false,identity),defProperty(TameTableCompElement,'nowrap',false,identity,false,identity),TameTableCompElement.prototype.insertRow=function(index){if(!this.editable___)throw new
Error(NOT_EDITABLE);return requireIntIn(index,-1,this.node___.rows.length),defaultTameNode(this.node___.insertRow(index),this.editable___)},TameTableCompElement.prototype.deleteRow=function(index){if(!this.editable___)throw new
Error(NOT_EDITABLE);requireIntIn(index,-1,this.node___.rows.length),this.node___.deleteRow(index)},___.all2(___.grantTypedMethod,TameTableCompElement.prototype,['insertRow','deleteRow']);function
requireIntIn(idx,min,max){if(idx!==(idx|0)||idx<min||idx>max)throw new Error(INDEX_SIZE_ERROR)}function
TameTableRowElement(node,editable){TameTableCompElement.call(this,node,editable)}inertCtor(TameTableRowElement,TameTableCompElement,'HTMLTableRowElement'),TameTableRowElement.prototype.insertCell=function(index){if(!this.editable___)throw new
Error(NOT_EDITABLE);return requireIntIn(index,-1,this.node___.cells.length),defaultTameNode(this.node___.insertCell(index),this.editable___)},TameTableRowElement.prototype.deleteCell=function(index){if(!this.editable___)throw new
Error(NOT_EDITABLE);requireIntIn(index,-1,this.node___.cells.length),this.node___.deleteCell(index)},___.all2(___.grantTypedMethod,TameTableRowElement.prototype,['insertCell','deleteCell']);function
TameTableElement(node,editable){TameTableCompElement.call(this,node,editable),classUtils.exportFields(this,['tBodies','tHead','tFoot','cellPadding','cellSpacing','border'])}inertCtor(TameTableElement,TameTableCompElement,'HTMLTableElement'),TameTableElement.prototype.getTBodies___=function(){return tameNodeList(this.node___.tBodies,this.editable___,defaultTameNode)},TameTableElement.prototype.getTHead___=function(){return defaultTameNode(this.node___.tHead,this.editable___)},TameTableElement.prototype.getTFoot___=function(){return defaultTameNode(this.node___.tFoot,this.editable___)},TameTableElement.prototype.createTHead=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);return defaultTameNode(this.node___.createTHead(),this.editable___)},TameTableElement.prototype.deleteTHead=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);this.node___.deleteTHead()},TameTableElement.prototype.createTFoot=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);return defaultTameNode(this.node___.createTFoot(),this.editable___)},TameTableElement.prototype.deleteTFoot=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);this.node___.deleteTFoot()},TameTableElement.prototype.createCaption=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);return defaultTameNode(this.node___.createCaption(),this.editable___)},TameTableElement.prototype.deleteCaption=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);this.node___.deleteCaption()},TameTableElement.prototype.insertRow=function(index){if(!this.editable___)throw new
Error(NOT_EDITABLE);return requireIntIn(index,-1,this.node___.rows.length),defaultTameNode(this.node___.insertRow(index),this.editable___)},TameTableElement.prototype.deleteRow=function(index){if(!this.editable___)throw new
Error(NOT_EDITABLE);requireIntIn(index,-1,this.node___.rows.length),this.node___.deleteRow(index)};function
fromInt(x){return''+(x|0)}defAttributeAlias(TameTableElement,'cellPadding',Number,fromInt),defAttributeAlias(TameTableElement,'cellSpacing',Number,fromInt),defAttributeAlias(TameTableElement,'border',Number,fromInt),___.all2(___.grantTypedMethod,TameTableElement.prototype,['createTHead','deleteTHead','createTFoot','deleteTFoot','createCaption','deleteCaption','insertRow','deleteRow']);function
tameEvent(event){return event.tamed___?event.tamed___:(event.tamed___=new TameEvent(event))}function
TameEvent(event){assert(!!event),this.event___=event,TameEventMark.stamp.mark___(this),classUtils.exportFields(this,['type','target','pageX','pageY','altKey','ctrlKey','metaKey','shiftKey','button','screenX','screenY','currentTarget','relatedTarget','fromElement','toElement','srcElement','clientX','clientY','keyCode','which'])}inertCtor(TameEvent,Object,'Event'),TameEvent.prototype.getType___=function(){return bridal.untameEventType(String(this.event___.type))},TameEvent.prototype.getTarget___=function(){var
event=this.event___;return tameRelatedNode(event.target||event.srcElement,true,defaultTameNode)},TameEvent.prototype.getSrcElement___=function(){return tameRelatedNode(this.event___.srcElement,true,defaultTameNode)},TameEvent.prototype.getCurrentTarget___=function(){var
e=this.event___;return tameRelatedNode(e.currentTarget,true,defaultTameNode)},TameEvent.prototype.getRelatedTarget___=function(){var
e=this.event___,t=e.relatedTarget;return t||(e.type==='mouseout'?(t=e.toElement):e.type==='mouseover'&&(t=e.fromElement)),tameRelatedNode(t,true,defaultTameNode)},TameEvent.prototype.setRelatedTarget___=function(newValue){return newValue},TameEvent.prototype.getFromElement___=function(){return tameRelatedNode(this.event___.fromElement,true,defaultTameNode)},TameEvent.prototype.getToElement___=function(){return tameRelatedNode(this.event___.toElement,true,defaultTameNode)},TameEvent.prototype.getPageX___=function(){return Number(this.event___.pageX)},TameEvent.prototype.getPageY___=function(){return Number(this.event___.pageY)},TameEvent.prototype.stopPropagation=function(){this.event___.stopPropagation?this.event___.stopPropagation():(this.event___.cancelBubble=true)},TameEvent.prototype.preventDefault=function(){this.event___.preventDefault?this.event___.preventDefault():(this.event___.returnValue=false)},TameEvent.prototype.getAltKey___=function(){return Boolean(this.event___.altKey)},TameEvent.prototype.getCtrlKey___=function(){return Boolean(this.event___.ctrlKey)},TameEvent.prototype.getMetaKey___=function(){return Boolean(this.event___.metaKey)},TameEvent.prototype.getShiftKey___=function(){return Boolean(this.event___.shiftKey)},TameEvent.prototype.getButton___=function(){var
e=this.event___;return e.button&&Number(e.button)},TameEvent.prototype.getClientX___=function(){return Number(this.event___.clientX)},TameEvent.prototype.getClientY___=function(){return Number(this.event___.clientY)},TameEvent.prototype.getScreenX___=function(){return Number(this.event___.screenX)},TameEvent.prototype.getScreenY___=function(){return Number(this.event___.screenY)},TameEvent.prototype.getWhich___=function(){var
w=this.event___.which;return w&&Number(w)},TameEvent.prototype.getKeyCode___=function(){var
kc=this.event___.keyCode;return kc&&Number(kc)},TameEvent.prototype.toString=___.markFuncFreeze(function(){return'[Fake Event]'}),___.all2(___.grantTypedMethod,TameEvent.prototype,['stopPropagation','preventDefault']);function
TameCustomHTMLEvent(event){TameEvent.call(this,event),this.properties___={}}___.extend(TameCustomHTMLEvent,TameEvent),TameCustomHTMLEvent.prototype.initEvent=function(type,bubbles,cancelable){bridal.initEvent(this.event___,type,bubbles,cancelable)},TameCustomHTMLEvent.prototype.handleRead___=function(name){var
handlerName;return name=String(name),endsWith__.test(name)?void 0:(handlerName=name+'_getter___',this[handlerName]?this[handlerName]():___.hasOwnProp(this.event___.properties___,name)?this.event___.properties___[name]:void
0)},TameCustomHTMLEvent.prototype.handleCall___=function(name,args){var handlerName;name=String(name);if(endsWith__.test(name))throw new
Error(INVALID_SUFFIX);handlerName=name+'_handler___';if(this[handlerName])return this[handlerName].call(this,args);if(___.hasOwnProp(this.event___.properties___,name))return this.event___.properties___[name].call(this,args);throw new
TypeError(name+' is not a function.')},TameCustomHTMLEvent.prototype.handleSet___=function(name,val){var
handlerName;name=String(name);if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);return handlerName=name+'_setter___',this[handlerName]?this[handlerName](val):(this.event___.properties___||(this.event___.properties___={}),this[name+'_canEnum___']=true,this.event___.properties___[name]=val)},TameCustomHTMLEvent.prototype.handleDelete___=function(name){var
handlerName;name=String(name);if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);return handlerName=name+'_deleter___',this[handlerName]?this[handlerName]():this.event___.properties___?delete
this.event___.properties___[name]&&delete this[name+'_canEnum___']:true},TameCustomHTMLEvent.prototype.handleEnum___=function(ownFlag){return this.event___.properties___?___.allKeys(this.event___.properties___):[]},TameCustomHTMLEvent.prototype.toString=___.markFuncFreeze(function(){return'[Fake CustomEvent]'}),___.grantTypedMethod(TameCustomHTMLEvent.prototype,'initEvent');function
TameHTMLDocument(doc,body,domain,editable){var tameBody,tameBodyElement,tameDoc,tameHeadElement,tameHtmlElement,tameTitleElement,title;TamePseudoNode.call(this,editable),this.doc___=doc,this.body___=body,this.domain___=domain,this.onLoadListeners___=[],tameDoc=this,tameBody=defaultTameNode(body,editable),this.tameBody___=tameBody,tameBodyElement=new
TamePseudoElement('BODY',this,function(){return tameNodeList(body.childNodes,editable,defaultTameNode)},function(){return tameHtmlElement},function(){return tameInnerHtml(body.innerHTML)},tameBody,editable),___.forOwnKeys({'appendChild':0,'removeChild':0,'insertBefore':0,'replaceChild':0},___.markFuncFreeze(function(k){tameBodyElement[k]=tameBody[k].bind(tameBody),___.grantFunc(tameBodyElement,k)})),title=doc.createTextNode(body.getAttribute('title')||''),tameTitleElement=new
TamePseudoElement('TITLE',this,function(){return[defaultTameNode(title,false)]},function(){return tameHeadElement},function(){return html.escapeAttrib(title.nodeValue)},null,editable),tameHeadElement=new
TamePseudoElement('HEAD',this,function(){return[tameTitleElement]},function(){return tameHtmlElement},function(){return'<title>'+tameTitleElement.getInnerHTML___()+'</title>'},null,editable),tameHtmlElement=new
TamePseudoElement('HTML',this,function(){return[tameHeadElement,tameBodyElement]},function(){return tameDoc},function(){return'<head>'+tameHeadElement.getInnerHTML___()+'</head><body>'+tameBodyElement.getInnerHTML___()+'</body>'},tameBody,editable),body.contains&&(tameHtmlElement.contains=function(other){var
otherNode;return other=TameNodeT.coerce(other),otherNode=other.node___,body.contains(otherNode)},___.grantFunc(tameHtmlElement,'contains')),'function'===typeof
body.compareDocumentPosition&&(tameHtmlElement.compareDocumentPosition=function(other){var
bitmask,otherNode;return other=TameNodeT.coerce(other),otherNode=other.node___,otherNode?(bitmask=+body.compareDocumentPosition(otherNode),bitmask&31):0},___.hasOwnProp(tameHtmlElement,'contains')||(tameHtmlElement.contains=(function(other){var
docPos=this.compareDocumentPosition(other);return!(!(docPos&16)&&docPos)}).bind(tameHtmlElement),___.grantFunc(tameHtmlElement,'contains')),___.grantFunc(tameHtmlElement,'compareDocumentPosition')),this.documentElement___=tameHtmlElement,classUtils.exportFields(this,['documentElement','body','title','domain','forms','compatMode'])}inertCtor(TameHTMLDocument,TamePseudoNode,'HTMLDocument'),TameHTMLDocument.prototype.getNodeType___=function(){return 9},TameHTMLDocument.prototype.getNodeName___=function(){return'#document'},TameHTMLDocument.prototype.getNodeValue___=function(){return null},TameHTMLDocument.prototype.getChildNodes___=function(){return[this.documentElement___]},TameHTMLDocument.prototype.getAttributes___=function(){return[]},TameHTMLDocument.prototype.getParentNode___=function(){return null},TameHTMLDocument.prototype.getElementsByTagName=function(tagName){var
nodes;tagName=String(tagName).toLowerCase();switch(tagName){case'body':return fakeNodeList([this.getBody___()]);case'head':return fakeNodeList([this.getHead___()]);case'title':return fakeNodeList([this.getTitle___()]);case'html':return fakeNodeList([this.getDocumentElement___()]);default:return nodes=tameGetElementsByTagName(this.body___,tagName,this.editable___),tagName==='*'&&(nodes.unshift(this.getBody___()),nodes.unshift(this.getTitle___()),nodes.unshift(this.getHead___()),nodes.unshift(this.getDocumentElement___())),nodes}},TameHTMLDocument.prototype.getDocumentElement___=function(){return this.documentElement___},TameHTMLDocument.prototype.getBody___=function(){return this.documentElement___.getLastChild___()},TameHTMLDocument.prototype.getHead___=function(){return this.documentElement___.getFirstChild___()},TameHTMLDocument.prototype.getTitle___=function(){return this.getHead___().getFirstChild___()},TameHTMLDocument.prototype.getDomain___=function(){return this.domain___},TameHTMLDocument.prototype.getElementsByClassName=function(className){return tameGetElementsByClassName(this.body___,className,this.editable___)},TameHTMLDocument.prototype.addEventListener=function(name,listener,useCapture){return this.tameBody___.addEventListener(name,listener,useCapture)},TameHTMLDocument.prototype.removeEventListener=function(name,listener,useCapture){return this.tameBody___.removeEventListener(name,listener,useCapture)},TameHTMLDocument.prototype.createComment=function(text){return defaultTameNode(this.doc___.createComment(' '),true)},TameHTMLDocument.prototype.createDocumentFragment=function(){if(!this.editable___)throw new
Error(NOT_EDITABLE);return defaultTameNode(this.doc___.createDocumentFragment(),true)},TameHTMLDocument.prototype.createElement=function(tagName){var
attribs,flags,i,newEl;if(!this.editable___)throw new Error(NOT_EDITABLE);tagName=String(tagName).toLowerCase();if(!html4
.ELEMENTS.hasOwnProperty(tagName))throw new Error(UNKNOWN_TAGNAME+'['+tagName+']');flags=html4
.ELEMENTS[tagName];if(flags&html4 .eflags.UNSAFE&&!(flags&html4 .eflags.SCRIPT))return ___.log(UNSAFE_TAGNAME+'['+tagName+']: no action performed'),null;newEl=this.doc___.createElement(tagName);if(elementPolicies.hasOwnProperty(tagName)){attribs=elementPolicies[tagName]([]);if(attribs)for(i=0;i<attribs.length;i+=2)bridal.setAttribute(newEl,attribs[i],attribs[i+1])}return defaultTameNode(newEl,true)},TameHTMLDocument.prototype.createTextNode=function(text){if(!this.editable___)throw new
Error(NOT_EDITABLE);return defaultTameNode(this.doc___.createTextNode(text!==null&&text!==void
0?''+text:''),true)},TameHTMLDocument.prototype.getElementById=function(id){var
node;return id+=idSuffix,node=this.doc___.getElementById(id),defaultTameNode(node,this.editable___)},TameHTMLDocument.prototype.getForms___=function(){var
tameForms=[],i,tameForm;for(i=0;i<this.doc___.forms.length;++i)tameForm=tameRelatedNode(this.doc___.forms.item(i),this.editable___,defaultTameNode),tameForm!==null&&tameForms.push(tameForm);return fakeNodeList(tameForms)},TameHTMLDocument.prototype.getCompatMode___=function(){return'CSS1Compat'},TameHTMLDocument.prototype.toString=___.markFuncFreeze(function(){return'[Fake Document]'}),TameHTMLDocument.prototype.createEvent=function(type){var
document,rawEvent,tamedEvent;type=String(type);if(type!=='HTMLEvents')throw new Error('Unrecognized event type '+type);return document=this.doc___,document.createEvent?(rawEvent=document.createEvent(type)):(rawEvent=document.createEventObject(),rawEvent.eventType='ondataavailable'),tamedEvent=new
TameCustomHTMLEvent(rawEvent),rawEvent.tamed___=tamedEvent,tamedEvent},TameHTMLDocument.prototype.getOwnerDocument___=function(){return null},TameHTMLDocument.prototype.signalLoaded___=function(){var
onload=___.canRead(imports,'$v')&&___.canCallPub(imports.$v,'ros')&&imports.$v.ros('onload')||imports.window&&___.readPub(imports.window,'onload'),i,listeners,n;onload&&setTimeout(function(){___.callPub(onload,'call',[___.USELESS])},0),listeners=this.onLoadListeners___,this.onLoadListeners___=[];for(i=0,n=listeners.length;i<n;++i)(function(listener){setTimeout(function(){___.callPub(listener,'call',[___.USELESS])},0)})(listeners[i])},___.all2(___.grantTypedMethod,TameHTMLDocument.prototype,['addEventListener','removeEventListener','createComment','createDocumentFragment','createElement','createEvent','createTextNode','getElementById','getElementsByClassName','getElementsByTagName']),imports.handlers___=[],imports.TameHTMLDocument___=TameHTMLDocument,imports.tameNode___=defaultTameNode,imports.feralNode___=___.markFuncFreeze(function(tameNode){return tameNode=TameNodeT.coerce(tameNode),tameNode.node___}),imports.tameEvent___=tameEvent,imports.blessHtml___=blessHtml,imports.blessCss___=function(var_args){var
arr=[],i,n;for(i=0,n=arguments.length;i<n;++i)arr[i]=arguments[i];return cssSealerUnsealerPair.seal(arr)},imports.htmlAttr___=function(s){return html.escapeAttrib(String(s||''))},imports.html___=safeHtml,imports.rewriteUri___=function(uri,mimeType){var
s=rewriteAttribute(null,null,html4 .atype.URI,uri);if(!s)throw new Error;return s},imports.suffix___=function(nmtokens){var
p=String(nmtokens).replace(/^\s+|\s+$/g,'').split(/\s+/g),out=[],i,nmtoken;for(i=0;i<p.length;++i){nmtoken=rewriteAttribute(null,null,html4
.atype.ID,p[i]);if(!nmtoken)throw new Error(nmtokens);out.push(nmtoken)}return out.join(' ')},imports.ident___=function(nmtokens){var
p=String(nmtokens).replace(/^\s+|\s+$/g,'').split(/\s+/g),out=[],i,nmtoken;for(i=0;i<p.length;++i){nmtoken=rewriteAttribute(null,null,html4
.atype.CLASSES,p[i]);if(!nmtoken)throw new Error(nmtokens);out.push(nmtoken)}return out.join(' ')},allCssProperties=domitaModules.CssPropertiesCollection(css.properties,document.documentElement,css),historyInsensitiveCssProperties=domitaModules.CssPropertiesCollection(css.HISTORY_INSENSITIVE_STYLE_WHITELIST,document.documentElement,css);function
TameStyle(style,editable,tameEl){this.style___=style,this.editable___=editable,this.tameEl___=tameEl}inertCtor(TameStyle,Object,'Style'),TameStyle.prototype.readByCanonicalName___=function(canonName){return String(this.style___[canonName]||'')},TameStyle.prototype.writeByCanonicalName___=function(canonName,val){this.style___[canonName]=val},TameStyle.prototype.allowProperty___=function(cssPropertyName){return allCssProperties.isCssProp(cssPropertyName)},TameStyle.prototype.handleRead___=function(stylePropertyName){var
self=this,canonName,cssPropertyName;return String(stylePropertyName)==='getPropertyValue'?___.markFuncFreeze(function(args){return TameStyle.prototype.getPropertyValue.call(self,args)}):!this.style___||!allCssProperties.isCanonicalProp(stylePropertyName)?void
0:(cssPropertyName=allCssProperties.getCssPropFromCanonical(stylePropertyName),this.allowProperty___(cssPropertyName)?(canonName=allCssProperties.getCanonicalPropFromCss(cssPropertyName),this.readByCanonicalName___(canonName)):void
0)},TameStyle.prototype.handleCall___=function(name,args){if(String(name)==='getPropertyValue')return TameStyle.prototype.getPropertyValue.call(this,args);throw'Cannot handle method '+String(name)},TameStyle.prototype.getPropertyValue=function(cssPropertyName){var
canonName;return cssPropertyName=String(cssPropertyName||'').toLowerCase(),this.allowProperty___(cssPropertyName)?(canonName=allCssProperties.getCanonicalPropFromCss(cssPropertyName),this.readByCanonicalName___(canonName)):''},TameStyle.prototype.handleSet___=function(stylePropertyName,value){var
canonName,cssPropertyName,pattern,val;if(!this.editable___)throw new Error('style not editable');stylePropertyName=String(stylePropertyName);if(stylePropertyName==='cssText')return typeof
this.style___.cssText==='string'?(this.style___.cssText=sanitizeStyleAttrValue(value)):this.tameEl___.setAttribute('style',value),value;if(!allCssProperties.isCanonicalProp(stylePropertyName))throw new
Error('Unknown CSS property name '+stylePropertyName);cssPropertyName=allCssProperties.getCssPropFromCanonical(stylePropertyName);if(!this.allowProperty___(cssPropertyName))return;pattern=css.properties[cssPropertyName];if(!pattern)throw new
Error('style not editable');val=''+(value||''),val=val.replace(/\burl\s*\(\s*\"([^\"]*)\"\s*\)/gi,function(_,url){var
decodedUrl=decodeCssString(url),rewrittenUrl=uriCallback?uriCallback.rewrite(decodedUrl,'image/*'):null;return rewrittenUrl||(rewrittenUrl='about:blank'),'url(\"'+rewrittenUrl.replace(/[\"\'\{\}\(\):\\]/g,function(ch){return'\\'+ch.charCodeAt(0).toString(16)+' '})+'\")'});if(val&&!pattern.test(val+' '))throw new
Error('bad value `'+val+'` for CSS property '+stylePropertyName);return canonName=allCssProperties.getCanonicalPropFromCss(cssPropertyName),this.writeByCanonicalName___(canonName,val),value},TameStyle.prototype.toString=___.markFuncFreeze(function(){return'[Fake Style]'});function
isNestedInAnchor(rawElement){for(;rawElement&&rawElement!=pseudoBodyNode;rawElement=rawElement.parentNode)if(rawElement.tagName.toLowerCase()==='a')return true;return false}function
TameComputedStyle(rawElement,pseudoElement){rawElement=rawElement||document.createElement('div'),TameStyle.call(this,bridal.getComputedStyle(rawElement,pseudoElement),false),this.rawElement___=rawElement,this.pseudoElement___=pseudoElement}___.extend(TameComputedStyle,TameStyle),TameComputedStyle.prototype.readByCanonicalName___=function(canonName){var
canReturnDirectValue=historyInsensitiveCssProperties.isCanonicalProp(canonName)||!isNestedInAnchor(this.rawElement___);return canReturnDirectValue?TameStyle.prototype.readByCanonicalName___.call(this,canonName):new
TameComputedStyle(pseudoBodyNode,this.pseudoElement___).readByCanonicalName___(canonName)},TameComputedStyle.prototype.writeByCanonicalName___=function(canonName){throw'Computed styles not editable: This code should be unreachable'},TameComputedStyle.prototype.toString=___.markFuncFreeze(function(){return'[Fake Computed Style]'}),nodeClasses.XMLHttpRequest=domitaModules.TameXMLHttpRequest(domitaModules.XMLHttpRequestCtor(window.XMLHttpRequest,window.ActiveXObject),uriCallback),imports.cssNumber___=function(num){if('number'===typeof
num&&isFinite(num)&&!isNaN(num))return''+num;throw new Error(num)},imports.cssColor___=function(color){var
hex;if('number'!==typeof color||color!=(color|0))throw new Error(color);return hex='0123456789abcdef','#'+hex.charAt(color>>20&15)+hex.charAt(color>>16&15)+hex.charAt(color>>12&15)+hex.charAt(color>>8&15)+hex.charAt(color>>4&15)+hex.charAt(color&15)},imports.cssUri___=function(uri,mimeType){var
s=rewriteAttribute(null,null,html4 .atype.URI,uri);if(!s)throw new Error;return s},imports.emitCss___=function(cssText){this.getCssContainer___().appendChild(bridal.createStylesheet(document,cssText))},imports.getCssContainer___=function(){return(document.getElementsByTagName('head'))[0]};if(!/^-/.test(idSuffix))throw new
Error('id suffix \"'+idSuffix+'\" must start with \"-\"');if(!/___$/.test(idSuffix))throw new
Error('id suffix \"'+idSuffix+'\" must end with \"___\"');idClass=idSuffix.substring(1),idClassPattern=new
RegExp('(?:^|\\s)'+idClass.replace(/[\.$]/g,'\\$&')+'(?:\\s|$)'),imports.getIdClass___=function(){return idClass},bridal.setAttribute(pseudoBodyNode,'class',bridal.getAttribute(pseudoBodyNode,'class')+' '+idClass+' vdoc-body___'),imports.domitaTrace___=0,imports.getDomitaTrace=___.markFuncFreeze(function(){return imports.domitaTrace___}),imports.setDomitaTrace=___.markFuncFreeze(function(x){imports.domitaTrace___=x}),imports.setTimeout=tameSetTimeout,imports.setInterval=tameSetInterval,imports.clearTimeout=tameClearTimeout,imports.clearInterval=tameClearInterval,tameDocument=new
TameHTMLDocument(document,pseudoBodyNode,String(optPseudoWindowLocation.hostname||'nosuchhost.fake'),true),imports.document=tameDocument,imports.document.tameNode___=imports.tameNode___,imports.document.feralNode___=imports.feralNode___,tameLocation=___.primFreeze({'toString':___.markFuncFreeze(function(){return tameLocation.href}),'href':String(optPseudoWindowLocation.href||'http://nosuchhost.fake/'),'hash':String(optPseudoWindowLocation.hash||''),'host':String(optPseudoWindowLocation.host||'nosuchhost.fake'),'hostname':String(optPseudoWindowLocation.hostname||'nosuchhost.fake'),'pathname':String(optPseudoWindowLocation.pathname||'/'),'port':String(optPseudoWindowLocation.port||''),'protocol':String(optPseudoWindowLocation.protocol||'http:'),'search':String(optPseudoWindowLocation.search||'')}),tameNavigator=___.primFreeze({'appName':String(window.navigator.appName),'appVersion':String(window.navigator.appVersion),'platform':String(window.navigator.platform),'userAgent':String(window.navigator.userAgent),'cajaVersion':'1.0'}),PSEUDO_ELEMENT_WHITELIST={'first-letter':true,'first-line':true};function
TameWindow(){this.properties___={}}function TameDefaultView(){this.document=tameDocument,assert(tameDocument.editable___),___.grantRead(this,'document')}___.forOwnKeys({'document':tameDocument,'location':tameLocation,'navigator':tameNavigator,'setTimeout':tameSetTimeout,'setInterval':tameSetInterval,'clearTimeout':tameClearTimeout,'clearInterval':tameClearInterval,'addEventListener':___.markFuncFreeze(function(name,listener,useCapture){name==='load'?(classUtils.ensureValidCallback(listener),tameDocument.onLoadListeners___.push(listener)):tameDocument.addEventListener(name,listener,useCapture)}),'removeEventListener':___.markFuncFreeze(function(name,listener,useCapture){var
i,k,listeners,n;if(name==='load'){listeners=tameDocument.onLoadListeners___,k=0;for(i=0,n=listeners.length;i<n;++i)listeners[i-k]=listeners[i],listeners[i]===listener&&++k;listeners.length-=k}else
tameDocument.removeEventListener(name,listener,useCapture)}),'dispatchEvent':___.markFuncFreeze(function(evt){})},___.markFuncFreeze(function(propertyName,value){TameWindow.prototype[propertyName]=value,___.grantRead(TameWindow.prototype,propertyName)})),___.forOwnKeys({'scrollBy':___.markFuncFreeze(function(dx,dy){(dx||dy)&&makeScrollable(tameDocument.body___),tameScrollBy(tameDocument.body___,dx,dy)}),'scrollTo':___.markFuncFreeze(function(x,y){makeScrollable(tameDocument.body___),tameScrollTo(tameDocument.body___,x,y)}),'resizeTo':___.markFuncFreeze(function(w,h){tameResizeTo(tameDocument.body___,w,h)}),'resizeBy':___.markFuncFreeze(function(dw,dh){tameResizeBy(tameDocument.body___,dw,dh)}),'getComputedStyle':___.markFuncFreeze(function(tameElement,pseudoElement){tameElement=TameNodeT.coerce(tameElement),pseudoElement=pseudoElement===null||pseudoElement===void
0||''===pseudoElement?void 0:String(pseudoElement).toLowerCase();if(pseudoElement!==void
0&&!PSEUDO_ELEMENT_WHITELIST.hasOwnProperty(pseudoElement))throw new Error('Bad pseudo element '+pseudoElement);return new
TameComputedStyle(tameElement.node___,pseudoElement)})},___.markFuncFreeze(function(propertyName,value){TameWindow.prototype[propertyName]=value,___.grantRead(TameWindow.prototype,propertyName),TameDefaultView.prototype[propertyName]=value,___.grantRead(TameDefaultView.prototype,propertyName)})),TameWindow.prototype.handleRead___=function(name){var
handlerName;return name=String(name),endsWith__.test(name)?void 0:(handlerName=name+'_getter___',this[handlerName]?this[handlerName]():___.hasOwnProp(this,name)?this[name]:void
0)},TameWindow.prototype.handleSet___=function(name,val){var handlerName;name=String(name);if(endsWith__.test(name))throw new
Error(INVALID_SUFFIX);return handlerName=name+'_setter___',this[handlerName]?this[handlerName](val):(this[name+'_canEnum___']=true,this[name+'_canRead___']=true,this[name]=val)},TameWindow.prototype.handleDelete___=function(name){var
handlerName;name=String(name);if(endsWith__.test(name))throw new Error(INVALID_SUFFIX);return handlerName=name+'_deleter___',this[handlerName]?this[handlerName]():___.deleteFieldEntirely(this,name)},tameWindow=new
TameWindow,tameDefaultView=new TameDefaultView(tameDocument.editable___);function
propertyOnlyHasGetter(_){throw new TypeError('setting a property that only has a getter')}___.forOwnKeys({'clientLeft':{'get':function(){return tameDocument.body___.clientLeft}},'clientTop':{'get':function(){return tameDocument.body___.clientTop}},'clientHeight':{'get':function(){return tameDocument.body___.clientHeight}},'clientWidth':{'get':function(){return tameDocument.body___.clientWidth}},'offsetLeft':{'get':function(){return tameDocument.body___.offsetLeft}},'offsetTop':{'get':function(){return tameDocument.body___.offsetTop}},'offsetHeight':{'get':function(){return tameDocument.body___.offsetHeight}},'offsetWidth':{'get':function(){return tameDocument.body___.offsetWidth}},'pageXOffset':{'get':function(){return tameDocument.body___.scrollLeft}},'pageYOffset':{'get':function(){return tameDocument.body___.scrollTop}},'scrollLeft':{'get':function(){return tameDocument.body___.scrollLeft},'set':function(x){return tameDocument.body___.scrollLeft=+x,x}},'scrollTop':{'get':function(){return tameDocument.body___.scrollTop},'set':function(y){return tameDocument.body___.scrollTop=+y,y}},'scrollHeight':{'get':function(){return tameDocument.body___.scrollHeight}},'scrollWidth':{'get':function(){return tameDocument.body___.scrollWidth}}},___.markFuncFreeze(function(propertyName,def){var
views=[tameWindow,tameDefaultView,tameDocument.getBody___(),tameDocument.getDocumentElement___()],setter=def.set||propertyOnlyHasGetter,getter=def.get,i,view;for(i=views.length;--i>=0;)view=views[i],___.useGetHandler(view,propertyName,getter),___.useSetHandler(view,propertyName,setter)})),___.forOwnKeys({'innerHeight':function(){return tameDocument.body___.clientHeight},'innerWidth':function(){return tameDocument.body___.clientWidth},'outerHeight':function(){return tameDocument.body___.clientHeight},'outerWidth':function(){return tameDocument.body___.clientWidth}},___.markFuncFreeze(function(propertyName,handler){___.useGetHandler(tameWindow,propertyName,handler),___.useGetHandler(tameDefaultView,propertyName,handler)})),windowProps=['top','self','opener','parent','window'],wpLen=windowProps.length;for(i=0;i<wpLen;++i)prop=windowProps[i],tameWindow[prop]=tameWindow,___.grantRead(tameWindow,prop);tameDocument.editable___&&(tameDocument.defaultView=tameDefaultView,___.grantRead(tameDocument,'defaultView'),tameDocument.sanitizeAttrs___=sanitizeAttrs),___.forOwnKeys(nodeClasses,___.markFuncFreeze(function(name,ctor){___.primFreeze(ctor),tameWindow[name]=ctor,___.grantRead(tameWindow,name)})),defaultNodeClasses=['HTMLAppletElement','HTMLAreaElement','HTMLBaseElement','HTMLBaseFontElement','HTMLBodyElement','HTMLBRElement','HTMLButtonElement','HTMLDirectoryElement','HTMLDivElement','HTMLDListElement','HTMLFieldSetElement','HTMLFontElement','HTMLFrameElement','HTMLFrameSetElement','HTMLHeadElement','HTMLHeadingElement','HTMLHRElement','HTMLHtmlElement','HTMLIFrameElement','HTMLIsIndexElement','HTMLLabelElement','HTMLLegendElement','HTMLLIElement','HTMLLinkElement','HTMLMapElement','HTMLMenuElement','HTMLMetaElement','HTMLModElement','HTMLObjectElement','HTMLOListElement','HTMLOptGroupElement','HTMLOptionElement','HTMLParagraphElement','HTMLParamElement','HTMLPreElement','HTMLQuoteElement','HTMLScriptElement','HTMLSelectElement','HTMLStyleElement','HTMLTableCaptionElement','HTMLTableCellElement','HTMLTableColElement','HTMLTableElement','HTMLTableRowElement','HTMLTableSectionElement','HTMLTextAreaElement','HTMLTitleElement','HTMLUListElement'],defaultNodeClassCtor=nodeClasses.Element;for(i=0;i<defaultNodeClasses.length;++i)tameWindow[defaultNodeClasses[i]]=defaultNodeClassCtor,___.grantRead(tameWindow,defaultNodeClasses[i]);outers=imports.outers,___.isJSONContainer(outers)?(___.forOwnKeys(outers,___.markFuncFreeze(function(k,v){k
in tameWindow||(tameWindow[k]=v,___.grantRead(tameWindow,k))})),imports.outers=tameWindow):(imports.window=tameWindow)}return attachDocumentStub})();function
plugin_dispatchEvent___(thisNode,event,pluginId,handler){var imports,node;return event=event||bridal.getWindow(thisNode).event,event.currentTarget||(event.currentTarget=thisNode),imports=___.getImports(pluginId),node=imports.tameNode___(thisNode,true),plugin_dispatchToHandler___(pluginId,handler,[node,imports.tameEvent___(event),node])}function
plugin_dispatchToHandler___(pluginId,handler,args){var sig=(''+handler).match(/^function\b[^\)]*\)/),imports=___.getImports(pluginId),$v,fn,tameWin;imports.domitaTrace___&1&&___.log('Dispatch pluginId='+pluginId+', handler='+(sig?sig[0]:handler)+', args='+args);switch(typeof
handler){case'number':handler=imports.handlers___[handler];break;case'string':fn=void
0,tameWin=void 0,$v=___.readPub(imports,'$v'),$v&&(fn=___.callPub($v,'ros',[handler]),fn||(tameWin=___.callPub($v,'ros',['window']))),fn||(fn=___.readPub(imports,handler),fn||(tameWin||(tameWin=___.readPub(imports,'window')),tameWin&&(fn=___.readPub(tameWin,handler)))),handler=fn&&typeof
fn.call==='function'?fn:void 0;break;case'function':case'object':break;default:throw new
Error('Expected function as event handler, not '+typeof handler)}___.startCallerStack&&___.startCallerStack(),imports.isProcessingEvent___=true;try{return ___.callPub(handler,'call',args)}catch(ex){throw ex&&ex.cajitaStack___&&'undefined'!==typeof
console&&console.error('Event dispatch %s: %s',handler,ex.cajitaStack___.join('\n')),ex}finally{imports.isProcessingEvent___=false}}}
(function(){
  var imports = ___.getNewModuleHandler().getImports();
  imports.loader = {provide:___.markFuncFreeze(function(v){valijaMaker = v;})};
  imports.outers = imports;
})();

{
  ___.loadModule({
      'instantiate': function (___, IMPORTS___) {
        var Array = ___.readImport(IMPORTS___, 'Array');
        var Object = ___.readImport(IMPORTS___, 'Object');
        var ReferenceError = ___.readImport(IMPORTS___, 'ReferenceError', {});
        var RegExp = ___.readImport(IMPORTS___, 'RegExp', {});
        var TypeError = ___.readImport(IMPORTS___, 'TypeError', {});
        var cajita = ___.readImport(IMPORTS___, 'cajita', {
            'beget': { '()': {} },
            'freeze': { '()': {} },
            'newTable': { '()': {} },
            'getFuncCategory': { '()': {} },
            'enforceType': { '()': {} },
            'getSuperCtor': { '()': {} },
            'getOwnPropertyNames': { '()': {} },
            'getProtoPropertyNames': { '()': {} },
            'getProtoPropertyValue': { '()': {} },
            'inheritsFrom': { '()': {} },
            'readOwn': { '()': {} },
            'directConstructor': { '()': {} },
            'USELESS': {},
            'construct': { '()': {} },
            'forAllKeys': { '()': {} },
            'Token': { '()': {} },
            'args': { '()': {} }
          });
        var loader = ___.readImport(IMPORTS___, 'loader');
        var outers = ___.readImport(IMPORTS___, 'outers');
        var moduleResult___, valijaMaker;
        moduleResult___ = ___.NO_RESULT;
        valijaMaker = (function () {
            function valijaMaker$_var(outers) {
              var ObjectPrototype, DisfunctionPrototype, Disfunction,
              ObjectShadow, x0___, FuncHeader, x1___, myPOE, x2___, pumpkin,
              x3___, x4___, x5___, t, undefIndicator;
              function disfuncToString($dis) {
                var callFn, printRep, match, name;
                callFn = $dis.call_canRead___? $dis.call: ___.readPub($dis,
                  'call');
                if (callFn) {
                  printRep = callFn.toString_canCall___? callFn.toString():
                  ___.callPub(callFn, 'toString', [ ]);
                  match = FuncHeader.exec_canCall___? FuncHeader.exec(printRep)
                    : ___.callPub(FuncHeader, 'exec', [ printRep ]);
                  if (null !== match) {
                    name = $dis.name_canRead___? $dis.name: ___.readPub($dis,
                      'name');
                    if (name === void 0) {
                      name = match[ 1 ];
                    }
                    return 'function ' + name + '(' + match[ 2 ] +
                      ') {\n  [cajoled code]\n}';
                  }
                  return printRep;
                }
                return 'disfunction(var_args){\n   [cajoled code]\n}';
              }
              disfuncToString.FUNC___ = 'disfuncToString';
              function getShadow(func) {
                var cat, result, parentFunc, parentShadow, proto, statics, i,
                k, meths, i, k, v;
                cajita.enforceType(func, 'function');
                cat = cajita.getFuncCategory(func);
                result = myPOE.get_canCall___? myPOE.get(cat):
                ___.callPub(myPOE, 'get', [ cat ]);
                if (void 0 === result) {
                  result = cajita.beget(DisfunctionPrototype);
                  parentFunc = cajita.getSuperCtor(func);
                  if (___.typeOf(parentFunc) === 'function') {
                    parentShadow = getShadow.CALL___(parentFunc);
                  } else {
                    parentShadow = ObjectShadow;
                  }
                  proto = cajita.beget(parentShadow.prototype_canRead___?
                    parentShadow.prototype: ___.readPub(parentShadow,
                      'prototype'));
                  result.prototype_canSet___ === result? (result.prototype =
                    proto): ___.setPub(result, 'prototype', proto);
                  proto.constructor_canSet___ === proto? (proto.constructor =
                    func): ___.setPub(proto, 'constructor', func);
                  statics = cajita.getOwnPropertyNames(func);
                  for (i = 0; i < statics.length; i++) {
                    k = ___.readPub(statics, i);
                    if (k !== 'valueOf') {
                      ___.setPub(result, k, ___.readPub(func, k));
                    }
                  }
                  meths = cajita.getProtoPropertyNames(func);
                  for (i = 0; i < meths.length; i++) {
                    k = ___.readPub(meths, i);
                    if (k !== 'valueOf') {
                      v = cajita.getProtoPropertyValue(func, k);
                      if (___.typeOf(v) === 'object' && v !== null &&
                        ___.typeOf(v.call_canRead___? v.call: ___.readPub(v,
                            'call')) === 'function') {
                        v = dis.CALL___(v.call_canRead___? v.call:
                          ___.readPub(v, 'call'), k);
                      }
                      ___.setPub(proto, k, v);
                    }
                  }
                  myPOE.set_canCall___? myPOE.set(cat, result):
                  ___.callPub(myPOE, 'set', [ cat, result ]);
                }
                return result;
              }
              getShadow.FUNC___ = 'getShadow';
              function getFakeProtoOf(func) {
                var shadow;
                if (___.typeOf(func) === 'function') {
                  shadow = getShadow.CALL___(func);
                  return shadow.prototype_canRead___? shadow.prototype:
                  ___.readPub(shadow, 'prototype');
                } else if (___.typeOf(func) === 'object' && func !== null) {
                  return func.prototype_canRead___? func.prototype:
                  ___.readPub(func, 'prototype');
                } else { return void 0; }
              }
              getFakeProtoOf.FUNC___ = 'getFakeProtoOf';
              function typeOf(obj) {
                var result;
                result = ___.typeOf(obj);
                if (result !== 'object') { return result; }
                if (cajita.isPseudoFunc_canCall___? cajita.isPseudoFunc(obj):
                  ___.callPub(cajita, 'isPseudoFunc', [ obj ])) { return 'function'; }
                return result;
              }
              typeOf.FUNC___ = 'typeOf';
              function instanceOf(obj, func) {
                if (___.typeOf(func) === 'function' && obj instanceof func) {
                  return true; } else {
                  return cajita.inheritsFrom(obj, getFakeProtoOf.CALL___(func))
                    ;
                }
              }
              instanceOf.FUNC___ = 'instanceOf';
              function read(obj, name) {
                var result, stepParent;
                result = cajita.readOwn(obj, name, pumpkin);
                if (result !== pumpkin) { return result; }
                if (___.typeOf(obj) === 'function') {
                  return ___.readPub(getShadow.CALL___(obj), name);
                }
                if (obj === null || obj === void 0) {
                  throw ___.construct(TypeError, [ 'Cannot read property \"' +
                        name + '\" from ' + obj ]);
                }
                if (___.inPub(name, ___.construct(Object, [ obj ]))) {
                  return ___.readPub(obj, name);
                }
                stepParent =
                  getFakeProtoOf.CALL___(cajita.directConstructor(obj));
                if (stepParent !== void 0 && ___.inPub(name,
                    ___.construct(Object, [ stepParent ])) && name !==
                  'valueOf') {
                  return ___.readPub(stepParent, name);
                }
                return ___.readPub(obj, name);
              }
              read.FUNC___ = 'read';
              function set(obj, name, newValue) {
                if (___.typeOf(obj) === 'function') {
                  ___.setPub(getShadow.CALL___(obj), name, newValue);
                } else {
                  ___.setPub(obj, name, newValue);
                }
                return newValue;
              }
              set.FUNC___ = 'set';
              function callFunc(func, args) {
                var x0___;
                return x0___ = cajita.USELESS, func.apply_canCall___?
                  func.apply(x0___, args): ___.callPub(func, 'apply', [ x0___,
                    args ]);
              }
              callFunc.FUNC___ = 'callFunc';
              function callMethod(obj, name, args) {
                var m;
                m = read.CALL___(obj, name);
                if (!m) {
                  throw ___.construct(TypeError, [ 'callMethod: ' + obj +
                        ' has no method ' + name ]);
                }
                return m.apply_canCall___? m.apply(obj, args): ___.callPub(m,
                  'apply', [ obj, args ]);
              }
              callMethod.FUNC___ = 'callMethod';
              function construct(ctor, args) {
                var result, altResult;
                if (___.typeOf(ctor) === 'function') {
                  return cajita.construct(ctor, args);
                }
                result = cajita.beget(ctor.prototype_canRead___?
                  ctor.prototype: ___.readPub(ctor, 'prototype'));
                altResult = ctor.apply_canCall___? ctor.apply(result, args):
                ___.callPub(ctor, 'apply', [ result, args ]);
                switch (___.typeOf(altResult)) {
                case 'object':
                  if (null !== altResult) { return altResult; }
                  break;
                case 'function':
                  return altResult;
                }
                return result;
              }
              construct.FUNC___ = 'construct';
              function dis(callFn, opt_name) {
                var template, result, x0___, disproto, x1___;
                template = cajita.PseudoFunction_canCall___?
                  cajita.PseudoFunction(callFn): ___.callPub(cajita,
                  'PseudoFunction', [ callFn ]);
                result = cajita.beget(DisfunctionPrototype);
                result.call_canSet___ === result? (result.call = callFn):
                ___.setPub(result, 'call', callFn);
                x0___ = template.apply_canRead___? template.apply:
                ___.readPub(template, 'apply'), result.apply_canSet___ ===
                  result? (result.apply = x0___): ___.setPub(result, 'apply',
                  x0___);
                disproto = cajita.beget(ObjectPrototype);
                result.prototype_canSet___ === result? (result.prototype =
                  disproto): ___.setPub(result, 'prototype', disproto);
                disproto.constructor_canSet___ === disproto?
                  (disproto.constructor = result): ___.setPub(disproto,
                  'constructor', result);
                x1___ = template.length, result.length_canSet___ === result?
                  (result.length = x1___): ___.setPub(result, 'length', x1___);
                if (opt_name !== void 0 && opt_name !== '') {
                  result.name_canSet___ === result? (result.name = opt_name):
                  ___.setPub(result, 'name', opt_name);
                }
                return result;
              }
              dis.FUNC___ = 'dis';
              function disfuncCall($dis, self, var_args) {
                var a___ = ___.args(arguments);
                var x0___;
                return x0___ = Array.slice(a___, 2), $dis.apply_canCall___?
                  $dis.apply(self, x0___): ___.callPub($dis, 'apply', [ self,
                    x0___ ]);
              }
              disfuncCall.FUNC___ = 'disfuncCall';
              function disfuncApply($dis, self, args) {
                return $dis.apply_canCall___? $dis.apply(self, args):
                ___.callPub($dis, 'apply', [ self, args ]);
              }
              disfuncApply.FUNC___ = 'disfuncApply';
              function disfuncBind($dis, self, var_args) {
                var a___ = ___.args(arguments);
                var leftArgs;
                function disfuncBound(var_args) {
                  var a___ = ___.args(arguments);
                  var x0___, x1___;
                  return x1___ = (x0___ = Array.slice(a___, 0),
                    leftArgs.concat_canCall___? leftArgs.concat(x0___):
                    ___.callPub(leftArgs, 'concat', [ x0___ ])),
                  $dis.apply_canCall___? $dis.apply(self, x1___):
                  ___.callPub($dis, 'apply', [ self, x1___ ]);
                }
                disfuncBound.FUNC___ = 'disfuncBound';
                leftArgs = Array.slice(a___, 2);
                return ___.primFreeze(disfuncBound);
              }
              disfuncBind.FUNC___ = 'disfuncBind';
              function getOuters() {
                cajita.enforceType(outers, 'object');
                return outers;
              }
              getOuters.FUNC___ = 'getOuters';
              function readOuter(name) {
                var result;
                result = cajita.readOwn(outers, name, pumpkin);
                if (result !== pumpkin) { return result; }
                if (canReadRev.CALL___(name, outers)) {
                  return read.CALL___(outers, name);
                } else {
                  throw ___.construct(ReferenceError, [
                      'Outer variable not found: ' + name ]);
                }
              }
              readOuter.FUNC___ = 'readOuter';
              function readOuterSilent(name) {
                if (canReadRev.CALL___(name, outers)) {
                  return read.CALL___(outers, name);
                } else { return void 0; }
              }
              readOuterSilent.FUNC___ = 'readOuterSilent';
              function setOuter(name, val) {
                return ___.setPub(outers, name, val);
              }
              setOuter.FUNC___ = 'setOuter';
              function initOuter(name) {
                if (canReadRev.CALL___(name, outers)) { return; }
                set.CALL___(outers, name, void 0);
              }
              initOuter.FUNC___ = 'initOuter';
              function remove(obj, name) {
                var shadow;
                if (___.typeOf(obj) === 'function') {
                  shadow = getShadow.CALL___(obj);
                  return ___.deletePub(shadow, name);
                } else {
                  return ___.deletePub(obj, name);
                }
              }
              remove.FUNC___ = 'remove';
              function keys(obj) {
                var result;
                result = [ ];
                cajita.forAllKeys(obj, ___.markFuncFreeze(function (name) {
                      result.push_canCall___? result.push(name):
                      ___.callPub(result, 'push', [ name ]);
                    }));
                cajita.forAllKeys(getSupplement.CALL___(obj),
                  ___.markFuncFreeze(function (name) {
                      if (!___.inPub(name, obj) && name !== 'constructor') {
                        result.push_canCall___? result.push(name):
                        ___.callPub(result, 'push', [ name ]);
                      }
                    }));
                return result;
              }
              keys.FUNC___ = 'keys';
              function canReadRev(name, obj) {
                if (___.inPub(name, obj)) { return true; }
                return ___.inPub(name, getSupplement.CALL___(obj));
              }
              canReadRev.FUNC___ = 'canReadRev';
              function getSupplement(obj) {
                var ctor;
                if (___.typeOf(obj) === 'function') {
                  return getShadow.CALL___(obj);
                } else {
                  ctor = cajita.directConstructor(obj);
                  return getFakeProtoOf.CALL___(ctor);
                }
              }
              getSupplement.FUNC___ = 'getSupplement';
              function exceptionTableSet(ex) {
                var result, x0___;
                result = cajita.Token('' + ex);
                x0___ = ex === void 0? undefIndicator: ex, t.set_canCall___?
                  t.set(result, x0___): ___.callPub(t, 'set', [ result, x0___ ]
                );
                return result;
              }
              exceptionTableSet.FUNC___ = 'exceptionTableSet';
              function exceptionTableRead(key) {
                var v, x0___;
                v = t.get_canCall___? t.get(key): ___.callPub(t, 'get', [ key ]
                );
                x0___ = void 0, t.set_canCall___? t.set(key, x0___):
                ___.callPub(t, 'set', [ key, x0___ ]);
                return v === void 0? key: v === undefIndicator? void 0: v;
              }
              exceptionTableRead.FUNC___ = 'exceptionTableRead';
              function disArgs(original) {
                return cajita.args(Array.slice(original, 1));
              }
              disArgs.FUNC___ = 'disArgs';
              ObjectPrototype = ___.iM([ 'constructor', Object ]);
              DisfunctionPrototype =
                cajita.beget(cajita.PseudoFunctionProto_canRead___?
                cajita.PseudoFunctionProto: ___.readPub(cajita,
                  'PseudoFunctionProto'));
              Disfunction = cajita.beget(DisfunctionPrototype);
              Disfunction.prototype_canSet___ === Disfunction?
                (Disfunction.prototype = DisfunctionPrototype):
              ___.setPub(Disfunction, 'prototype', DisfunctionPrototype);
              Disfunction.length_canSet___ === Disfunction? (Disfunction.length
                = 1): ___.setPub(Disfunction, 'length', 1);
              DisfunctionPrototype.constructor_canSet___ ===
                DisfunctionPrototype? (DisfunctionPrototype.constructor =
                Disfunction): ___.setPub(DisfunctionPrototype, 'constructor',
                Disfunction);
              outers.Function_canSet___ === outers? (outers.Function =
                Disfunction): ___.setPub(outers, 'Function', Disfunction);
              ObjectShadow = cajita.beget(DisfunctionPrototype);
              ObjectShadow.prototype_canSet___ === ObjectShadow?
                (ObjectShadow.prototype = ObjectPrototype):
              ___.setPub(ObjectShadow, 'prototype', ObjectPrototype);
              x0___ = (function () {
                  function freeze$_meth(obj) {
                    if (___.typeOf(obj) === 'function') {
                      cajita.freeze(getShadow.CALL___(obj));
                    } else {
                      cajita.freeze(obj);
                    }
                    return obj;
                  }
                  return ___.markFuncFreeze(freeze$_meth, 'freeze$_meth');
                })(), ObjectShadow.freeze_canSet___ === ObjectShadow?
                (ObjectShadow.freeze = x0___): ___.setPub(ObjectShadow,
                'freeze', x0___);
              FuncHeader = ___.construct(RegExp, [
                  '^\\s*function\\s*([^\\s\\(]*)\\s*\\(' + '(?:\\$dis,?\\s*)?'
                    + '([^\\)]*)\\)' ]);
              x1___ = dis.CALL___(___.primFreeze(disfuncToString), 'toString'),
              DisfunctionPrototype.toString_canSet___ === DisfunctionPrototype?
                (DisfunctionPrototype.toString = x1___):
              ___.setPub(DisfunctionPrototype, 'toString', x1___);
              outers.Function_canSet___ === outers? (outers.Function =
                Disfunction): ___.setPub(outers, 'Function', Disfunction);
              myPOE = cajita.newTable();
              x2___ = cajita.getFuncCategory(Object), myPOE.set_canCall___?
                myPOE.set(x2___, ObjectShadow): ___.callPub(myPOE, 'set', [
                  x2___, ObjectShadow ]);
              pumpkin = ___.iM([ ]);
              x3___ = dis.CALL___(___.primFreeze(disfuncCall), 'call'),
              DisfunctionPrototype.call_canSet___ === DisfunctionPrototype?
                (DisfunctionPrototype.call = x3___):
              ___.setPub(DisfunctionPrototype, 'call', x3___);
              x4___ = dis.CALL___(___.primFreeze(disfuncApply), 'apply'),
              DisfunctionPrototype.apply_canSet___ === DisfunctionPrototype?
                (DisfunctionPrototype.apply = x4___):
              ___.setPub(DisfunctionPrototype, 'apply', x4___);
              x5___ = dis.CALL___(___.primFreeze(disfuncBind), 'bind'),
              DisfunctionPrototype.bind_canSet___ === DisfunctionPrototype?
                (DisfunctionPrototype.bind = x5___):
              ___.setPub(DisfunctionPrototype, 'bind', x5___);
              t = cajita.newTable();
              undefIndicator = ___.iM([ ]);
              return cajita.freeze(___.iM([ 'typeOf', ___.primFreeze(typeOf),
                    'instanceOf', ___.primFreeze(instanceOf), 'tr',
                    ___.primFreeze(exceptionTableRead), 'ts',
                    ___.primFreeze(exceptionTableSet), 'r',
                    ___.primFreeze(read), 's', ___.primFreeze(set), 'cf',
                    ___.primFreeze(callFunc), 'cm', ___.primFreeze(callMethod),
                    'construct', ___.primFreeze(construct), 'getOuters',
                    ___.primFreeze(getOuters), 'ro', ___.primFreeze(readOuter),
                    'ros', ___.primFreeze(readOuterSilent), 'so',
                    ___.primFreeze(setOuter), 'initOuter',
                    ___.primFreeze(initOuter), 'remove', ___.primFreeze(remove)
                      , 'keys', ___.primFreeze(keys), 'canReadRev',
                    ___.primFreeze(canReadRev), 'disArgs',
                    ___.primFreeze(disArgs), 'dis', ___.primFreeze(dis) ]));
            }
            return ___.markFuncFreeze(valijaMaker$_var, 'valijaMaker$_var');
          })();
        if (___.typeOf(loader) !== 'undefined') {
          loader.provide_canCall___? loader.provide(valijaMaker):
          ___.callPub(loader, 'provide', [ valijaMaker ]);
        }
        if (___.typeOf(outers) !== 'undefined') {
          moduleResult___ = valijaMaker.CALL___(outers);
        }
        return moduleResult___;
      },
      'cajolerName': 'com.google.caja',
      'cajolerVersion': '4314',
      'cajoledDate': 1288211990790
});
}
