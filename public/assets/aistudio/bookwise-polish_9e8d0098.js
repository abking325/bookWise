/* BookWise editorial hero and optional local library tools.
   The original compiled React application remains untouched. */
(()=>{
'use strict';
let ORIGINAL = Array.isArray(window.BOOKWISE_ORIGINAL_BOOKS)?window.BOOKWISE_ORIGINAL_BOOKS:[];
try{const o=JSON.parse(localStorage.getItem('bookwise-original-overrides-v1')||'{}');ORIGINAL=ORIGINAL.map(b=>({...b,...(o[b.id]||{}),id:b.id}));}catch(e){}
const DB='bookwise-personal-library-v1', STORE='books', PREF_KEY='bookwise-hero-prefs-v1';
const REDUCED=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
const defaults={motion:'silk',hover:true,descriptions:true,shadows:true};
let appliedLanguage='',prefs={...defaults},added=[],all=[],hero=null,stage=null,stack=null,active=0,nodes=new Map(),frame=0,lastT=0,pointerTarget=0,pointerCurrent=0,pointerActive=false,hoverTimer=0,hoverIndex=null,hoverLockUntil=0,dragStart=null,dragMoved=false,modal=null,headerControls=null,shelf=null,bookDetail=null;
try{prefs={...defaults,...JSON.parse(localStorage.getItem(PREF_KEY)||'{}')}}catch(e){}
const esc=(x)=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const svg=(name)=>({plus:'<path d="M12 5v14M5 12h14"/>',settings:'<path d="M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-1.86 1.86-.06-.06A1.7 1.7 0 0 0 16 18.4a1.7 1.7 0 0 0-1 .6 1.7 1.7 0 0 0-.4 1.12V21H9.4v-.88A1.7 1.7 0 0 0 8 18.4a1.7 1.7 0 0 0-1.88.34l-.06.06-1.86-1.86.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.72-1.4H2V10.4h.88A1.7 1.7 0 0 0 4.6 9a1.7 1.7 0 0 0-.34-1.88l-.06-.06 1.86-1.86.06.06A1.7 1.7 0 0 0 8 5.6 1.7 1.7 0 0 0 9.4 4V3h5.2v1A1.7 1.7 0 0 0 16 5.6a1.7 1.7 0 0 0 1.88-.34l.06-.06 1.86 1.86-.06.06A1.7 1.7 0 0 0 19.4 9a1.7 1.7 0 0 0 1.72 1.4H22v3.2h-.88A1.7 1.7 0 0 0 19.4 15Z"/>',arrow:'<path d="M5 12h14m-6-6 6 6-6 6"/>',book:'<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z"/>'})[name];
const icon=(name)=>`<svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round" aria-hidden="true">${svg(name)}</svg>`;
function home(){return !location.hash || location.hash==='#/' || location.hash==='#' || location.hash.startsWith('#/?')}
function language(){const buttons=[...document.querySelectorAll('header button[aria-pressed]')];let am=buttons.find(el=>el.textContent.includes('አማ'));return am?.getAttribute('aria-pressed')==='true'?'am':'en'}
function trans(en,am){return language()==='am'?am:en}
function storedPrefs(){try{localStorage.setItem(PREF_KEY,JSON.stringify(prefs))}catch(e){}}
function openDB(){return new Promise((resolve,reject)=>{if(!window.indexedDB){reject(new Error('Local storage is not supported.'));return}const r=indexedDB.open(DB,1);r.onupgradeneeded=()=>{if(!r.result.objectStoreNames.contains(STORE))r.result.createObjectStore(STORE,{keyPath:'id'})};r.onerror=()=>reject(r.error);r.onsuccess=()=>resolve(r.result)})}
const FALLBACK='bookwise-local-library-fallback-v1';let memoryBooks=[];
function readFallback(){try{const raw=localStorage.getItem(FALLBACK);if(raw){memoryBooks=JSON.parse(raw);return memoryBooks.slice()}}catch(e){}return memoryBooks.slice()}
function storeFallback(next){memoryBooks=next;try{localStorage.setItem(FALLBACK,JSON.stringify(next))}catch(e){/* In restricted file previews, keep books in the current session. */}}
async function getBooks(){try{const db=await openDB();return await new Promise((resolve,reject)=>{const t=db.transaction(STORE,'readonly');const r=t.objectStore(STORE).getAll();r.onsuccess=()=>{resolve(r.result||[]);db.close()};r.onerror=()=>{reject(r.error);db.close()}})}catch(e){return readFallback()}}
async function putBook(b){try{const db=await openDB();return await new Promise((resolve,reject)=>{const t=db.transaction(STORE,'readwrite');t.objectStore(STORE).put(b);t.oncomplete=()=>{db.close();resolve()};t.onerror=()=>{db.close();reject(t.error)}})}catch(e){const list=readFallback().filter(x=>x.id!==b.id);list.push(b);storeFallback(list)}}
async function delBook(id){try{const db=await openDB();return await new Promise((resolve,reject)=>{const t=db.transaction(STORE,'readwrite');t.objectStore(STORE).delete(id);t.oncomplete=()=>{db.close();resolve()};t.onerror=()=>{db.close();reject(t.error)}})}catch(e){storeFallback(readFallback().filter(x=>x.id!==id))}}
function syncCatalog(){all=ORIGINAL.concat(added);if(!all.length)return;active=((active%all.length)+all.length)%all.length;if(hero){makeBooks();updateContent();renderShelf()}}
async function fetchExtras(){try{added=(await getBooks()).sort((a,b)=>(a.addedAt||0)-(b.addedAt||0));}catch(e){console.warn('Personal books could not load:',e)}syncCatalog()}
// The carousel is a continuous 3D turntable. It never reflows book widths
// or swaps between a wide poster and a thin spine: both are faces of the book.
// Shallow orbital arc: near books sit beyond the main cover, then taper toward
// the edges.  The offset is continuous for fractional phase while dragging.
function orbitAngle(offset){
 const d=Math.abs(offset);
 return Math.sign(offset)*(.115*(1-Math.exp(-d/.48))+.09*d);
}

let phase=0, phaseTarget=0, angularVelocity=0, tickTime=0, stagePointer=0;
const clamp=(v,lo,hi)=>Math.max(lo,Math.min(hi,v));
function val(v,min,max){return clamp(v,min,max)}
function mod(i,n){return ((i%n)+n)%n}
function getVisual(i){return all[mod(i,all.length)]}
function stageSizes(){
 const width=stage?.clientWidth||1024;
 const compact=width<650;
 // Keep the central cover reasonably sized, with thick neighboring volumes.
 const bookWidth=compact?clamp(width*.43,145,179):clamp(width*.16,183,225);
 const bookHeight=bookWidth*1.55;
 const radius=compact?Math.max(500,width*1.25):clamp(width*.52,580,805);
 const depth=compact?154:230;
 return {width,bookWidth,bookHeight,radius,depth};
}
function bookEl(virtual){
 const b=getVisual(virtual);
 const el=document.createElement('button');
 el.type='button';el.className='bw-book bw-orbit-book';
 el.dataset.index=virtual;el.dataset.book=b.id;
 el.setAttribute('aria-label',`${b.title}, ${b.author}`);
 let luminance=0;
 const match=/^#([\da-fA-F]{6})$/.exec(b.spine||'');
 if(match){const n=parseInt(match[1],16);luminance=((n>>16)*.299+((n>>8)&255)*.587+(n&255)*.114)}
 const ink=luminance>155?'#262420':'#f6f0e9';
 el.style.setProperty('--bw-spine',b.spine||'#514c44');
 el.style.setProperty('--bw-spine-ink',ink);
 el.style.setProperty('--bw-spine-size',b.title.length>19?'13px':'16px');
 // Real front and side surfaces; originals stay unaltered and fully visible.
 el.innerHTML=`<span class="bw-book-spine"><span class="bw-spine-title">${esc(b.title)}</span><span class="bw-spine-edition">BOOKWISE</span></span><span class="bw-book-face"><img src="${esc(b.cover||'')}" alt="${esc(b.title)} book cover" draggable="false" loading="eager" /><span class="bw-cover-text"><small>${esc(b.author)}</small><strong>${esc(b.title)}</strong></span></span>`;
 el.addEventListener('click',()=>{
  if(dragMoved)return;
  if(virtual===active&&Math.abs(phase-active)<.18){openBook(b)}
  else moveTo(virtual);
 });
 el.addEventListener('keydown',e=>{
  if(e.key==='ArrowRight'){e.preventDefault();move(1)}
  if(e.key==='ArrowLeft'){e.preventDefault();move(-1)}
 });
 el.addEventListener('pointerenter',e=>{
  if(e.pointerType==='touch'||!prefs.hover||virtual===active||Date.now()<hoverLockUntil||dragStart)return;
  hoverIndex=virtual;clearTimeout(hoverTimer);
  hoverTimer=setTimeout(()=>{if(hoverIndex===virtual&&!dragStart&&Date.now()>=hoverLockUntil)moveTo(virtual)},185);
 });
 el.addEventListener('pointerleave',()=>{if(hoverIndex===virtual){hoverIndex=null;clearTimeout(hoverTimer)}});
 return el;
}
function virtualRange(){const center=Math.round(phaseTarget);const sides=(stage?.clientWidth||900)<650?9:16;return [center-sides,center+sides]}
function ensureNodes(){
 if(!stack||!all.length)return;
 const [lo,hi]=virtualRange();
 const dims=stageSizes();
 for(let i=lo;i<=hi;i++){
  if(nodes.has(i))continue;
  const book=getVisual(i),el=bookEl(i);
  el.style.width=dims.bookWidth+'px';el.style.height=dims.bookHeight+'px';
  el.style.opacity='0';
  const side=document.createElement('button');
  side.className='bw-orbit-spine';side.type='button';
  side.setAttribute('aria-label',book.title+', select this book');
  side.style.setProperty('--bw-spine',book.spine||'#514c44');
  side.style.setProperty('--bw-spine-ink',el.style.getPropertyValue('--bw-spine-ink'));
  side.style.setProperty('--bw-spine-size',el.style.getPropertyValue('--bw-spine-size'));
  side.innerHTML=`<span class="bw-spine-title">${esc(book.title)}</span><span class="bw-spine-edition">BOOKWISE</span>`;
  side.onclick=()=>{if(!dragMoved)moveTo(i)};
  side.addEventListener('pointerenter',e=>{
    if(e.pointerType==='touch'||!prefs.hover||i===active||Date.now()<hoverLockUntil||dragStart)return;
    hoverIndex=i;clearTimeout(hoverTimer);
    hoverTimer=setTimeout(()=>{if(hoverIndex===i&&!dragStart)moveTo(i)},180);
  });
  side.addEventListener('pointerleave',()=>{if(hoverIndex===i){hoverIndex=null;clearTimeout(hoverTimer)}});
  nodes.set(i,{el,spineEl:side});stack.append(el,side);
 }
}
function makeBooks(){
 if(!stack||!all.length)return;
 nodes.clear();stack.replaceChildren();
 phase=active;phaseTarget=active;angularVelocity=0;tickTime=0;
 ensureNodes();paintFrame(true);
}
function placeBook(i,n,dims){
 const theta=orbitAngle(i-phase);
 const abs=Math.abs(theta),c=Math.cos(theta);
 const x=Math.sin(theta)*dims.radius + stagePointer*.007*c;
 const z=-dims.depth*(1-c);
 // Natural curved arc: real artwork always remains behind the titled spine.
 const yaw=Math.sign(theta)*78*(1-Math.exp(-abs/.092));
 const scale=clamp(1-.42*(1-c),.51,1);
 const rise=Math.max(0,1-c)*(dims.width<650?15:22);
 const visibility=clamp((1.59-abs)/.20,0,1);
 const sideMix=clamp((abs-.075)/.105,0,1);
 const el=n.el;
 el.style.transform=`translate3d(calc(-50% + ${x.toFixed(2)}px),${(-rise).toFixed(2)}px,${z.toFixed(2)}px) rotateY(${yaw.toFixed(2)}deg) scale(${scale.toFixed(4)})`;
 el.style.opacity=visibility.toFixed(3);
 el.style.zIndex=String(Math.floor((c+1)*1000));
 el.style.pointerEvents=visibility*(1-sideMix)<.13?'none':'auto';
 const side=n.spineEl;
 const sideWidth=(dims.width<650?27:41)*scale;
 const labelX=x+(theta<0?-5:5);
 side.style.width=sideWidth.toFixed(2)+'px';
 side.style.height=(dims.bookHeight*scale).toFixed(2)+'px';
 side.style.transform=`translate3d(calc(-50% + ${labelX.toFixed(2)}px),${(-rise).toFixed(2)}px,${(z+22).toFixed(2)}px)`;
 side.style.opacity=(visibility*sideMix).toFixed(3);
 side.style.zIndex=String(Math.floor((c+1)*1000)+1);
 side.style.pointerEvents=visibility*sideMix<.16?'none':'auto';
 const left=theta<0;
 if(el.classList.contains('is-left-arc')!==left)el.classList.toggle('is-left-arc',left);
 const centered=Math.abs(theta)<.095;
 if(el.classList.contains('is-centered')!==centered)el.classList.toggle('is-centered',centered);
 if(el.getAttribute('aria-pressed')!==String(virtualSelected(i)))el.setAttribute('aria-pressed',String(virtualSelected(i)));
 if(el.style.filter!==(prefs.shadows===false?'none':''))el.style.filter=prefs.shadows===false?'none':'';
}
function virtualSelected(i){return i===active}
function paintFrame(instant=false,time=performance.now()){
 if(!stage||!all.length)return;
 const dims=stageSizes();
 const dt=tickTime?clamp((time-tickTime)/1000,.005,.034):1/60;
 tickTime=time;
 if(instant||prefs.motion==='still'||REDUCED){phase=phaseTarget;angularVelocity=0;stagePointer=0}
 else{
  const stiffness=prefs.motion==='gentle'?66:112;
  const friction=prefs.motion==='gentle'?17:21;
  angularVelocity+=(stiffness*(phaseTarget-phase)-friction*angularVelocity)*dt;
  phase+=angularVelocity*dt;
  stagePointer+=(pointerTarget-stagePointer)*Math.min(1,dt*5);
  if(Math.abs(phase-phaseTarget)<.0002&&Math.abs(angularVelocity)<.003){phase=phaseTarget;angularVelocity=0}
 }
 for(const [i,n] of nodes){
  // Nodes behind the camera fade out before they can recycle into the front.
  const distance=Math.abs(orbitAngle(i-phase));
  if(distance>2.38 && Math.abs(i-phaseTarget)>8){n.el.remove();n.spineEl.remove();nodes.delete(i);continue}
  placeBook(i,n,dims);
 }
 const unsettled=Math.abs(phase-phaseTarget)>.0002||Math.abs(angularVelocity)>.003||Math.abs(stagePointer-pointerTarget)>.5;
 if(!instant&&unsettled)requestFrame();
 if(!unsettled){
  const [lo,hi]=virtualRange();
  for(const [i,n] of nodes){if(i<lo||i>hi){n.el.remove();n.spineEl.remove();nodes.delete(i)}}
 }
}
function requestFrame(){if(frame)return;frame=requestAnimationFrame(time=>{frame=0;paintFrame(false,time)})}
function updateContent(){if(!hero||!all.length)return;const b=getVisual(active);const title=hero.querySelector('#bwFeaturedTitle');title.textContent=b.title;hero.querySelector('#bwFeaturedMeta').textContent=`${trans('by','በ')} ${b.author}  ·  ${b.year}  ·  ${b.category==='personal'?'Personal development':b.category.replace(/(^.|-.)/g,s=>s.toUpperCase())}`;hero.querySelector('#bwFeaturedDesc').textContent=b.subtitle||'';hero.querySelector('#bwFeaturedDesc').hidden=!prefs.descriptions;const line=hero.querySelector('#bwCount');line.textContent=String(mod(active,all.length)+1).padStart(2,'0')+' / '+String(all.length).padStart(2,'0');hero.querySelector('#bwProgressDot').style.left=(all.length>1?(mod(active,all.length)/(all.length-1))*100:0)+'%';hero.querySelector('#bwFeatureKicker').textContent=trans('THE BOOK COLLECTION','የመጽሐፍት ስብስብ');hero.querySelector('#bwHeroAction').textContent=b.custom?trans(b.chapters?.length?'Read book':b.pdf?'Open PDF':'Book details','መጽሐፉን ይመልከቱ'):trans('Explore this book','ይህን መጽሐፍ ይመልከቱ');hero.querySelector('#bwHeroTitleLabel').textContent=trans('A MORE CONSIDERED WAY TO READ','የተሻለ የንባብ መንገድ')}
function moveTo(virtual){
 if(!all.length)return;
 // A selected virtual index is always the closest copy of that book on the ring.
 let selected=virtual;
 if(Math.abs(selected-active)>all.length/2){
  const delta=mod(virtual-active,all.length);
  selected=active+(delta>all.length/2?delta-all.length:delta);
 }
 if(selected===active&&Math.abs(phaseTarget-selected)<.001)return;
 hoverLockUntil=Date.now()+430;
 active=selected;phaseTarget=selected;
 clearTimeout(hoverTimer);hoverIndex=null;
 ensureNodes();updateContent();requestFrame();
}
function move(dir){moveTo(active+dir)}
function centerBook(){openBook(getVisual(active))}
function openBook(book){if(!book)return;if(book.custom){window.BWUpgrade?.openBook(book);return;}window.location.hash='#/books/'+encodeURIComponent(book.id)}
function heroElement(){const el=document.createElement('section');el.className='bw-hero';el.id='bw-enhanced-home-hero';el.setAttribute('aria-label','BookWise interactive book collection');el.innerHTML=`<div class="bw-hero-eyebrow"><span><b>BOOKWISE<span class="bw-mark">✦</span></b> &nbsp; / &nbsp; ${trans('THE READING ARCHIVE','የንባብ ማኅደር')}</span><span>${trans('DISCOVER · LEARN · APPLY · TRACK','ያግኙ · ይማሩ · ይተግብሩ')}</span></div><div class="bw-hero-stage" id="bwHeroStage" aria-label="Interactive books: hover, swipe, or use arrows"><div class="bw-stamp">BOOKWISE / THE COLLECTION</div><div class="bw-hero-bigword left" aria-hidden="true">BOOK</div><div class="bw-hero-bigword right" aria-hidden="true">WISE</div><div class="bw-book-stack" id="bwBookStack"></div></div><div class="bw-feature"><div class="bw-feature-top" id="bwFeatureKicker">THE BOOK COLLECTION</div><div class="bw-feature-row"><button class="bw-chevron" id="bwPrev" type="button" aria-label="Previous book">←</button><h1 class="bw-feature-title" id="bwFeaturedTitle"></h1><button class="bw-chevron" id="bwNext" type="button" aria-label="Next book">→</button></div><p class="bw-feature-meta" id="bwFeaturedMeta"></p><p class="bw-feature-desc" id="bwFeaturedDesc"></p><div class="bw-feature-actions"><button class="bw-main-action" id="bwHeroAction" type="button">Explore this book</button><a class="bw-alt-action" href="#/discover">${trans('Discover all books','ሁሉንም ያግኙ')}</a></div><div class="bw-feature-progress"><span>01</span><div class="bw-progress-track"><span class="bw-progress-dot" id="bwProgressDot"></span></div><span id="bwCount">01 / 12</span></div><div class="bw-feature-bottom"><span id="bwHeroTitleLabel">A MORE CONSIDERED WAY TO READ</span><span>${trans('DRAG OR SWIPE TO EXPLORE','ለመቃኘት ያንሸራትቱ')}</span></div></div>`;
 el.querySelector('#bwPrev').onclick=()=>move(-1);el.querySelector('#bwNext').onclick=()=>move(1);el.querySelector('#bwHeroAction').onclick=centerBook;stage=el.querySelector('#bwHeroStage');stack=el.querySelector('#bwBookStack');
 stage.addEventListener('pointermove',e=>{
  if(dragStart&&dragStart.id===e.pointerId){
   const dx=e.clientX-dragStart.x,dy=e.clientY-dragStart.y;
   if(!dragMoved&&Math.abs(dx)>8&&Math.abs(dx)>Math.abs(dy)*1.02){dragMoved=true;clearTimeout(hoverTimer);hoverIndex=null;}
   if(dragMoved){
    const pixelsPerBook=Math.max(72,Math.min(160,stage.clientWidth*.26));
    phaseTarget=dragStart.active-dx/pixelsPerBook;
    ensureNodes();requestFrame();
   }
   return;
  }
  if(e.pointerType==='touch')return;
  pointerActive=true;pointerTarget=e.clientX-stage.getBoundingClientRect().left-stage.clientWidth/2;
  requestFrame();
 });
 stage.addEventListener('pointerleave',()=>{if(dragStart)return;pointerActive=false;pointerTarget=0;hoverIndex=null;clearTimeout(hoverTimer);requestFrame()});
 stage.addEventListener('pointerdown',e=>{
  if(e.button!==undefined&&e.button!==0)return;
  dragStart={x:e.clientX,y:e.clientY,active:phaseTarget,id:e.pointerId};dragMoved=false;
  hoverIndex=null;clearTimeout(hoverTimer);
 });
 const finishDrag=e=>{
  if(!dragStart||dragStart.id!==e.pointerId)return;
  if(dragMoved){
   const snapped=Math.round(phaseTarget);
   dragStart=null;
   moveTo(snapped);
   // Clicks generated after pointerup must not open a book.
   setTimeout(()=>dragMoved=false,180);
  }else{dragStart=null;}
 };
 stage.addEventListener('pointerup',finishDrag);
 stage.addEventListener('pointercancel',e=>{if(dragStart?.id===e.pointerId){dragStart=null;phaseTarget=active;requestFrame()}});
 stage.addEventListener('wheel',e=>{
  if(Math.abs(e.deltaX)>Math.abs(e.deltaY)&&Math.abs(e.deltaX)>8){
   e.preventDefault();
   if(!stage._wheelAt||Date.now()-stage._wheelAt>160){move(e.deltaX>0?1:-1);stage._wheelAt=Date.now()}
  }
 },{passive:false});
 return el}
function ensureHeader(){if(location.hash.startsWith('#/read/')){headerControls?.remove();headerControls=null;return;}const header=document.querySelector('#root header');if(!header)return;const bar=header.firstElementChild;if(!bar)return;const right=bar.lastElementChild;if(!right)return;
 if(!headerControls||!headerControls.isConnected){headerControls=document.createElement('div');headerControls.className='bw-header-tools';headerControls.innerHTML=`<button class="bw-header-tool bw-theme-quick" type="button" id="bwThemeQuick" role="switch" aria-checked="false" aria-label="Toggle light and dark appearance"><span class="bw-quick-sun">☀</span><span class="bw-quick-moon">☾</span><span class="bw-quick-knob"></span></button><button class="bw-header-tool" id="bwSettingsOpen" type="button" aria-label="Settings">${icon('settings')}<span class="bw-tool-text">${trans('Settings','ቅንብሮች')}</span></button>`;headerControls.querySelector('#bwSettingsOpen').onclick=showSettings;headerControls.querySelector('#bwThemeQuick').onclick=()=>window.BWUpgrade?.toggleTheme();right.appendChild(headerControls)}
 window.BWUpgrade?.syncHeader();}
function ensureHome(){const root=document.querySelector('#root');if(!root)return;ensureHeader();const main=root.querySelector('main');if(!main||!home()){document.body.classList.remove('bw-homepage');if(hero&&hero.isConnected)hero.remove();hero=null;stage=null;stack=null;nodes.clear();if(shelf?.isConnected)shelf.remove();shelf=null;return}
 const container=main.firstElementChild;if(!container)return;const original=[...container.children].find(e=>e.tagName==='SECTION'&&!e.classList.contains('bw-hero')&&(e.classList.contains('bw-original-hero')||e.querySelector('h1.font-display')));if(!original)return;
 document.body.classList.add('bw-homepage');original.classList.add('bw-original-hero');if(!hero||!hero.isConnected){hero=heroElement();container.insertBefore(hero,original);makeBooks();updateContent()}
 if(appliedLanguage!==language()){appliedLanguage=language();updateLocale()}}
function updateLocale(){if(!hero)return;
 const eye=hero.querySelector('.bw-hero-eyebrow');if(eye){eye.children[0].innerHTML=`<b>BOOKWISE<span class="bw-mark">✦</span></b> &nbsp; / &nbsp; ${trans('THE READING ARCHIVE','የንባብ ማኅደር')}`;eye.children[1].textContent=trans('DISCOVER · LEARN · APPLY · TRACK','ያግኙ · ይማሩ · ይተግብሩ')}
 const browse=hero.querySelector('.bw-alt-action');if(browse)browse.textContent=trans('Discover all books','ሁሉንም ያግኙ');const foot=hero.querySelector('.bw-feature-bottom span:last-child');if(foot)foot.textContent=trans('DRAG OR SWIPE TO EXPLORE','ለመቃኘት ያንሸራትቱ');
 if(headerControls){const labels=headerControls.querySelectorAll('.bw-tool-text');if(labels[0])labels[0].textContent=trans('Settings','ቅንብሮች')}
 updateContent();renderShelf()}
function renderShelf(){if(!shelf||!shelf.isConnected)return;let cards=added.length?`<div class="bw-library-grid">${added.map(b=>`<button class="bw-library-card" type="button" data-custom-book="${esc(b.id)}"><img src="${esc(b.cover)}" alt="${esc(b.title)}" loading="lazy"/><strong>${esc(b.title)}</strong><small>${esc(b.author)}</small></button>`).join('')}</div>`:`<div class="bw-empty-state"><p>${trans('Your own book collection starts here. Add a cover and book details to include it in the interactive homepage carousel.','የራስዎን መጽሐፍት ለመጨመር የመጽሐፍ ሽፋን እና መረጃ ያስገቡ።')}</p><button class="bw-btn primary" type="button" id="bwShelfAdd">${trans('Add your first book','መጽሐፍ ጨምር')}</button></div>`;shelf.innerHTML=`<div class="bw-collection-head"><div><p class="bw-section-kicker">${trans('YOUR PERSONAL SHELF','የእርስዎ መጽሐፍት')}</p><h2>${trans('Books you add.','እርስዎ የጨመሯቸው መጽሐፍት')}</h2><p class="bw-collection-sub">${trans('Saved on this device and featured in your carousel.','በዚህ መሣሪያ የሚቀመጡ እና በላይ የሚታዩ።')}</p></div>${added.length?'<button class="bw-btn" type="button" id="bwShelfAdd">+ Add book</button>':''}</div>${cards}`;
 const addBtn=shelf.querySelector('#bwShelfAdd');if(addBtn)addBtn.onclick=showAdd;shelf.querySelectorAll('[data-custom-book]').forEach(el=>el.onclick=()=>{const b=added.find(x=>x.id===el.dataset.customBook);if(b)showDetail(b)})}
function ensureModal(){if(modal&&modal.isConnected)return;modal=document.createElement('div');modal.className='bw-modal-layer';modal.id='bwModalLayer';modal.setAttribute('role','presentation');modal.innerHTML='<div class="bw-modal" role="dialog" aria-modal="true" aria-labelledby="bwModalTitle"><button type="button" class="bw-modal-close" aria-label="Close" id="bwModalClose">×</button><div id="bwModalBody"></div></div>';document.body.appendChild(modal);modal.querySelector('#bwModalClose').onclick=closeModal;modal.addEventListener('pointerdown',e=>{if(e.target===modal)closeModal()})}
function openModal(html){ensureModal();modal.querySelector('#bwModalBody').innerHTML=html;modal.classList.add('open');document.body.style.overflow='hidden';modal.querySelector('#bwModalClose').focus()}
function closeModal(){if(modal)modal.classList.remove('open');document.body.style.overflow=''}
document.addEventListener('click',e=>{if(e.target.closest('header button[aria-pressed]')){requestAnimationFrame(()=>{appliedLanguage='';scan()})}});
document.addEventListener('keydown',e=>{if(e.key==='Escape'&&modal?.classList.contains('open'))closeModal();if((e.key==='ArrowLeft'||e.key==='ArrowRight')&&home()&&hero&&!modal?.classList.contains('open')&&!['INPUT','TEXTAREA','SELECT'].includes(document.activeElement?.tagName)){move(e.key==='ArrowRight'?1:-1)}});
async function optimizeCover(file){if(!file||!file.type.startsWith('image/'))throw new Error('Choose a JPG, PNG or WEBP cover image.');if(file.size>20*1024*1024)throw new Error('Cover image is larger than 20MB.');const data=await new Promise((resolve,reject)=>{const fr=new FileReader();fr.onload=()=>resolve(fr.result);fr.onerror=()=>reject(new Error('Could not read image.'));fr.readAsDataURL(file)});const img=await new Promise((resolve,reject)=>{const im=new Image();im.onload=()=>resolve(im);im.onerror=()=>reject(new Error('Invalid cover image.'));im.src=data});const maxW=760,maxH=1100,scale=Math.min(1,maxW/img.width,maxH/img.height);const canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(img.width*scale));canvas.height=Math.max(1,Math.round(img.height*scale));const ctx=canvas.getContext('2d');ctx.fillStyle='#fff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(img,0,0,canvas.width,canvas.height);return canvas.toDataURL('image/jpeg',.88)}
function showAdd(){window.BWUpgrade?.openEditor()}
async function saveFromForm(e){e.preventDefault();const f=e.currentTarget;const msg=modal.querySelector('#bwFormMessage');const save=modal.querySelector('#bwFormSave');save.disabled=true;save.textContent='Saving…';msg.textContent='Preparing book cover…';try{const file=f.querySelector('#bwBookCover').files[0];const pdf=f.querySelector('#bwBookPDF').files[0];if(pdf&&pdf.size>30*1024*1024)throw new Error('Please choose a PDF smaller than 30MB.');const cover=await optimizeCover(file);const title=f.elements.title.value.trim(),author=f.elements.author.value.trim();if(!title||!author)throw new Error('Title and author are required.');const name=f.elements.description.value.trim();const b={id:'custom-'+Date.now()+'-'+Math.floor(Math.random()*10000),title,author,subtitle:name.slice(0,160),description:name,category:f.elements.category.value,language:f.elements.language.value,year:Number(f.elements.year.value)||new Date().getFullYear(),format:pdf?'pdf':'book',cover,pdf:pdf||null,spine:'#55504b',addedAt:Date.now(),custom:true};await putBook(b);added.push(b);const oldCount=all.length;all=ORIGINAL.concat(added);active=oldCount;makeBooks();updateContent();renderShelf();closeModal();if(home()&&hero)hero.scrollIntoView({behavior:'smooth',block:'start'});}catch(err){msg.style.color='#a74132';msg.textContent=err?.message||'Could not save this book.'}finally{save.disabled=false;save.textContent='Save book'}}
function showSettings(){window.BWUpgrade?.openSettings()}
function showDetail(b){window.BWUpgrade?.openDetails(b)}
let queued=false;function scan(){if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;ensureHome()})}
const observer=new MutationObserver(scan);
function start(){syncCatalog();observer.observe(document.getElementById('root'),{childList:true,subtree:true});window.addEventListener('hashchange',scan);window.addEventListener('resize',()=>{if(hero){makeBooks();updateContent()}});scan();fetchExtras()}
window.BWInternal={getOriginal:()=>ORIGINAL.slice(),getAdded:()=>added.slice(),getAll:()=>all.slice(),putBook,delBook,closeModal,openModal,openOriginal:(b)=>window.location.hash='#/books/'+encodeURIComponent(b.id),refresh:fetchExtras,
 refreshOriginal(){const o=JSON.parse(localStorage.getItem('bookwise-original-overrides-v1')||'{}');ORIGINAL=(window.BOOKWISE_ORIGINAL_BOOKS||[]).map(b=>({...b,...(o[b.id]||{}),id:b.id}));syncCatalog();},
 openCurrent:()=>openBook(getVisual(active)),selectById(id){const idx=all.findIndex(b=>b.id===id);if(idx>=0){moveTo(idx);return true}return false}};
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();
})();
