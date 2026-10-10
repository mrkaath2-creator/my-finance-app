(function(){
'use strict';
var activeType='lifeExpense',editId=null,fuelEditId=null;

function M(n){return typeof money==='function'?money(n):Math.round(Number(n)||0).toLocaleString('ru-RU')+' ₽'}
function P(n){return typeof pct==='function'?pct(n):(Number(n)||0).toLocaleString('ru-RU')+'%'}
function Q(id){return document.getElementById(id)}
function say(t){if(typeof toast==='function')toast(t);else alert(t)}
function escM(s){return String(s==null?'':s).replace(/[&<>'"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]})}

function addStyle(){
 if(Q('manualStyle'))return;
 var s=document.createElement('style');s.id='manualStyle';
 s.textContent='.manual-modal{position:fixed;inset:0;background:rgba(0,0,0,.66);backdrop-filter:blur(9px);display:none;align-items:flex-end;justify-content:center;padding:12px;z-index:120}.manual-modal.show{display:flex}.manual-sheet{width:min(780px,100%);max-height:90vh;overflow:auto;background:#101b2d;border:1px solid var(--line);border-radius:24px;padding:16px;box-shadow:var(--shadow)}.manual-row{display:flex;justify-content:space-between;gap:10px;padding:10px 0;border-bottom:1px solid var(--line)}.manual-actions{display:flex;gap:5px;justify-content:flex-end;flex-wrap:wrap;margin-top:5px}.manual-actions button{border:1px solid var(--line);background:#182842;color:var(--text);border-radius:10px;padding:6px 8px;font-size:10px;font-weight:800}.manual-mini{font-size:11px;color:var(--muted);line-height:1.4;margin-top:4px}.manual-card{width:100%;text-align:left;color:var(--text);cursor:pointer}.manual-card:active{transform:scale(.99)}.manual-insight{padding:14px;border:1px solid rgba(110,231,183,.18);background:linear-gradient(135deg,rgba(110,231,183,.09),rgba(96,165,250,.07));border-radius:18px;line-height:1.45}.manual-insight b{color:var(--accent)}.manual-data-tools{margin-top:20px}.manual-danger{border:1px solid rgba(251,113,133,.35)!important;color:#ff9eae!important;background:rgba(251,113,133,.08)!important}.manual-muted{font-size:11px;color:var(--muted);line-height:1.45}@media(max-width:430px){.nav{grid-template-columns:repeat(4,minmax(0,1fr))!important}.nav button{font-size:9px}.nav .ico{font-size:15px}}';
 document.head.appendChild(s)
}

function compactNav(){
 var nav=document.querySelector('.nav');if(!nav)return;
 ['invest','history'].forEach(function(id){var b=nav.querySelector('[data-go="'+id+'"]');if(b)b.style.display='none'});
 var stats=nav.querySelector('[data-go="stats"]');if(stats){stats.innerHTML='<span class="ico">▥</span>Финансы';stats.style.display='block'}
 var settings=nav.querySelector('[data-go="settings"]');if(settings)settings.style.display='block';
 nav.style.gridTemplateColumns='repeat(4,minmax(0,1fr))';
 nav.querySelectorAll('button').forEach(function(b){var id=b.getAttribute('data-go');if(id==='home'||id==='calc')b.onclick=function(){showView(id)};if(id==='stats')b.onclick=function(){showManualView('stats')};if(id==='settings')b.onclick=function(){showView('settings')}});
}

function replaceFinance(){
 var s=Q('stats');if(!s)return;
 s.innerHTML='<div class="section-title" style="margin-top:5px">Финансы</div>'+
 '<div class="hero" style="padding:18px"><div class="eyebrow">Твои деньги</div><h2 style="font-size:24px">Всё можно менять вручную.</h2><p>Нажми на любую карточку, чтобы открыть историю, добавить запись, изменить или удалить её.</p></div>'+
 '<div class="section-title">Этот месяц</div><div class="summary-grid"><div class="metric"><div class="k">Доходы</div><div class="v good" id="mMonthIncome">0 ₽</div></div><div class="metric"><div class="k">Траты на жизнь</div><div class="v danger" id="mMonthExpense">0 ₽</div></div><div class="metric"><div class="k">Остаток месяца</div><div class="v" id="mMonthNet">0 ₽</div></div><div class="metric"><div class="k">Операций</div><div class="v" id="mMonthOps">0</div></div></div><div class="manual-insight" id="mInsight" style="margin-top:10px">Собираю картину финансов…</div>'+
 '<div class="section-title">Баланс</div><div class="summary-grid">'+
 '<button class="metric manual-card" data-manual-type="income"><div class="k">💰 Доход</div><div class="v" id="mIncome">0 ₽</div><div class="manual-mini" id="mIncomeSub">Среднее 0 ₽/день</div></button>'+
 '<button class="metric manual-card" data-manual-type="lifeExpense"><div class="k">🛒 Траты на жизнь</div><div class="v danger" id="mLife">0 ₽</div><div class="manual-mini" id="mLifeSub">Среднее 0 ₽/день</div></button>'+
 '<button class="metric manual-card" data-manual-type="fuel"><div class="k">⛽ Бензин</div><div class="v good" id="mFuel">0 ₽</div><div class="manual-mini" id="mFuelSub">Отложено 0 ₽</div></button>'+
 '<button class="metric manual-card" data-manual-type="car"><div class="k">🚗 Машина</div><div class="v" id="mCar">0 ₽</div><div class="manual-mini">Резерв</div></button>'+
 '<button class="metric manual-card" data-manual-type="buffer"><div class="k">🛡️ Финансовый запас</div><div class="v" id="mBuffer">0 ₽</div><div class="manual-mini">Подушка</div></button>'+
 '<button class="metric manual-card" data-manual-type="deposit"><div class="k">🏦 Вклад</div><div class="v good" id="mDeposit">0 ₽</div><div class="manual-mini" id="mDepositSub">Ставка —</div></button>'+
 '<button class="metric manual-card" data-manual-type="investment"><div class="k">📈 Инвестиции</div><div class="v good" id="mInvest">0 ₽</div><div class="manual-mini">Внесено</div></button>'+
 '<div class="metric"><div class="k">🚗 Рабочий пробег</div><div class="v" id="mKm">0 км</div><div class="manual-mini" id="mKmSub">0 рабочих дней</div></div>'+
 '</div>'+
 '<div class="section-title">➕ Быстро добавить</div><div class="row"><button class="btn secondary" id="mAddExpense" type="button">Трата</button><button class="btn secondary" id="mAddDeposit" type="button">Вклад</button></div><div class="row" style="margin-top:10px"><button class="btn secondary" id="mAddCar" type="button">Машина</button><button class="btn secondary" id="mAddBuffer" type="button">Запас</button></div>'+
 '<div class="section-title">🧾 Последние записи</div><div id="mRecent"></div>'+
 '<button class="btn secondary full" id="mAllHistory" style="margin-top:10px" type="button">Вся история</button>';
 s.querySelectorAll('[data-manual-type]').forEach(function(b){b.onclick=function(){openManual(b.getAttribute('data-manual-type'))}});
 Q('mAddExpense').onclick=function(){openManual('lifeExpense')};Q('mAddDeposit').onclick=function(){openManual('deposit')};Q('mAddCar').onclick=function(){openManual('car')};Q('mAddBuffer').onclick=function(){openManual('buffer')};Q('mAllHistory').onclick=function(){openManual('all')}
}

function typeName(t){return ({income:'Доход',lifeExpense:'Траты на жизнь',fuel:'Бензин',car:'Машина',buffer:'Финансовый запас',deposit:'Вклад',investment:'Инвестиции'})[t]||'Операция'}

async function totalsManual(){
 var c=await all('calculations'),l=await all('ledger'),r=await all('refuels');
 var income=0,km=0,fuel=0,car=0,buf=0,dep=0,inv=0,life=0,recent=[],monthIncome=0,monthExpense=0,monthOps=0;var now=new Date(),monthStart=new Date(now.getFullYear(),now.getMonth(),1).getTime();
 c.forEach(function(x){var d=x.data||{};income+=Number(d.amount)||0;km+=Number(d.mileage)||0;fuel+=Number(d.fuel)||0;car+=Number(d.car)||0;buf+=Number(d.buffer)||0;dep+=Number(d.deposit)||0;inv+=Number(d.invest)||0;if(Number(x.ts)>=monthStart){monthIncome+=Number(d.amount)||0;monthOps++}recent.push({source:'day',id:x.id,type:'income',amount:Number(d.amount)||0,sign:1,category:'Яндекс Доставка',note:'Расчёт дня',ts:x.ts})});
 l.forEach(function(x){var a=Number(x.amount)||0,s=Number(x.sign||1);if(Number(x.ts)>=monthStart){monthOps++;if(x.type==='income')monthIncome+=a*s;if(x.type==='lifeExpense')monthExpense+=a*(s<0?1:-1)}if(x.type==='income')income+=a*s;if(x.type==='car')car+=a*s;if(x.type==='buffer')buf+=a*s;if(x.type==='deposit')dep+=a*s;if(x.type==='investment')inv+=a*s;if(x.type==='lifeExpense')life+=a*(s<0?1:-1);recent.push({source:'ledger',id:x.id,type:x.type,amount:a,sign:s,category:x.category,note:x.note,ts:x.ts})});
 var spent=r.reduce(function(s,x){return s+(Number(x.cost)||0)},0);r.forEach(function(x){recent.push({source:'fuel',id:x.id,type:'fuel',amount:Number(x.cost)||0,sign:-1,category:'Бензин',note:x.note||'Заправка',ts:x.ts})});
 recent.sort(function(a,b){return b.ts-a.ts});
 var days=Math.max(1,c.length),allSpent=life+spent;
 return {c:c,l:l,r:r,income:income,km:km,fuel:fuel,car:car,buf:buf,dep:dep,inv:inv,life:life,spent:spent,fuelLeft:Math.max(0,fuel-spent),avgIncome:income/days,avgSpend:allSpent/days,days:days,allSpent:allSpent,recent:recent,monthIncome:monthIncome,monthExpense:monthExpense,monthNet:monthIncome-monthExpense,monthOps:monthOps}
}

async function refreshManual(){
 var t=await totalsManual(),s=await loadSettings(),set=function(id,v){var e=Q(id);if(e)e.textContent=v};
 set('mMonthIncome',M(t.monthIncome));set('mMonthExpense',M(t.monthExpense));set('mMonthNet',M(t.monthNet));set('mMonthOps',String(t.monthOps));set('mInsight',t.monthIncome===0&&t.monthExpense===0?'Добавь первый доход или расход — и здесь появится понятная сводка месяца.':t.monthNet<0?'В этом месяце расходы на жизнь выше учтённых доходов. Проверь записи и запланируй траты на оставшиеся дни.':'Учтённый остаток месяца: '+M(t.monthNet)+'. '+(t.monthIncome>0?'На жизнь ушло '+Math.round(t.monthExpense/t.monthIncome*100)+'% от доходов.':'Добавь доходы, чтобы увидеть долю расходов.'));set('mIncome',M(t.income));set('mIncomeSub','Среднее '+M(t.avgIncome)+'/день');
 set('mLife',M(t.life));set('mLifeSub','Среднее '+M(t.life/Math.max(1,t.days))+'/день');
 set('mFuel',M(t.fuelLeft));set('mFuelSub','Отложено '+M(t.fuel)+' · заправки '+M(t.spent));
 set('mCar',M(Math.max(0,t.car)));set('mBuffer',M(Math.max(0,t.buf)));set('mDeposit',M(Math.max(0,t.dep)));set('mDepositSub','Ставка '+P(Number(s.depositRate)||0)+' · ≈ '+M(t.dep*(Number(s.depositRate)||0)/100/12)+'/мес');
 set('mInvest',M(Math.max(0,t.inv)));set('mKm',t.km.toLocaleString('ru-RU')+' км');set('mKmSub',t.c.length+' рабочих дней');
 var arr=t.recent.slice(0,12),el=Q('mRecent');
 el.innerHTML=arr.length?arr.map(function(x){return '<div class="manual-row"><div><div class="date">'+new Date(x.ts).toLocaleString('ru-RU')+'</div><b>'+escM(typeName(x.type))+'</b><div class="manual-mini">'+escM(x.category||'')+(x.note?' · '+escM(x.note):'')+'</div></div><div><b>'+((x.sign||1)>0?'+':'−')+M(x.amount)+'</b><div class="manual-actions"><button data-me="'+x.source+'" data-mi="'+x.id+'" data-mt="'+x.type+'">'+(x.source==='day'?'Открыть':'Изм.')+'</button>'+(x.source==='day'?'':'<button data-md="'+x.source+'" data-mi="'+x.id+'">Удал.</button>')+'</div></div></div>'}).join(''):'<div class="card empty">Операций пока нет.</div>';
 el.querySelectorAll('[data-me]').forEach(function(b){b.onclick=function(){editManual(b.getAttribute('data-me'),Number(b.getAttribute('data-mi')),b.getAttribute('data-mt'))}});
 el.querySelectorAll('[data-md]').forEach(function(b){b.onclick=function(){deleteManual(b.getAttribute('data-md'),Number(b.getAttribute('data-mi')))}})
}

function addModal(){
 if(Q('manualOpModal'))return;
 document.body.insertAdjacentHTML('beforeend','<div class="manual-modal" id="manualOpModal"><div class="manual-sheet"><div class="row"><div><div class="eyebrow" id="manualEye">Операция</div><h2 id="manualTitle" style="font-size:22px;margin:5px 0 0">Добавить</h2></div><button class="icon-btn" id="manualClose" type="button">×</button></div><div class="grid" style="margin-top:14px"><div class="field"><label>Тип операции</label><select id="manualType"><option value="income">💰 Доход</option><option value="lifeExpense">🛒 Трата на жизнь</option><option value="deposit">🏦 Вклад</option><option value="investment">📈 Инвестиции</option><option value="car">🚗 Машина</option><option value="buffer">🛡️ Финансовый запас</option></select></div><div class="field"><label>Сумма, ₽</label><input id="manualAmount" type="number" inputmode="decimal" step="10"></div><div class="field"><label>Действие</label><select id="manualSign"><option value="1">Добавить</option><option value="-1">Уменьшить / списать</option></select></div><div class="field" id="manualCatField"><label>Категория</label><input id="manualCat" placeholder="Например, продукты"></div><div class="field"><label>Комментарий</label><input id="manualNote" placeholder="Например, супермаркет"></div><div class="field"><label>Дата</label><input id="manualDate" type="date"></div></div><div class="hint" style="margin-top:8px">Запись сохраняется локально. Её потом можно изменить или удалить.</div><div class="row" style="margin-top:12px"><button class="btn secondary" id="manualCancel" type="button">Отмена</button><button class="btn" id="manualSave" type="button">Сохранить</button></div><div class="section-title">История</div><div id="manualHistory"></div></div></div>');
 Q('manualClose').onclick=closeManual;Q('manualCancel').onclick=closeManual;Q('manualSave').onclick=saveManual;Q('manualType').onchange=function(){activeType=this.value;Q('manualCatField').style.display=this.value==='lifeExpense'||this.value==='income'?'block':'none';renderManualHistory(this.value)}
}

async function openManual(type,id){
 if(type==='fuel'){openFuelManual(id);return}
 activeType=type;editId=id||null;Q('manualType').value=type==='all'?'lifeExpense':type;Q('manualTitle').textContent=id?'Изменить: '+typeName(type):(type==='all'?'Вся история':'Добавить: '+typeName(type));Q('manualEye').textContent=type==='all'?'Финансы':typeName(type);Q('manualAmount').value='';Q('manualCat').value='';Q('manualNote').value='';Q('manualDate').value=new Date().toISOString().slice(0,10);Q('manualSign').value=type==='lifeExpense'?'-1':'1';Q('manualCatField').style.display=type==='lifeExpense'||type==='income'?'block':'none';
 if(id){var e=(await all('ledger')).find(function(x){return x.id===id});if(e){Q('manualAmount').value=e.amount;Q('manualCat').value=e.category||'';Q('manualNote').value=e.note||'';Q('manualDate').value=new Date(e.ts).toISOString().slice(0,10);Q('manualSign').value=String(e.sign||1)}}
 Q('manualOpModal').classList.add('show');await renderManualHistory(type)
}
function closeManual(){Q('manualOpModal').classList.remove('show');editId=null}
function editManual(source,id,type){if(source==='day'){showView('calc');return}if(source==='fuel'){openFuelManual(id);return}openManual(type,id)}
async function renderManualHistory(type){
 var t=await totalsManual(),rows=type==='all'?t.recent:(await all('ledger')).filter(function(x){return x.type===type}).sort(function(a,b){return b.ts-a.ts});
 if(!rows.length){Q('manualHistory').innerHTML='<div class="empty">Записей пока нет.</div>';return}
 Q('manualHistory').innerHTML=rows.slice(0,40).map(function(x){return '<div class="manual-row"><div><div class="date">'+new Date(x.ts).toLocaleString('ru-RU')+'</div><b>'+escM(typeName(x.type))+'</b><div class="manual-mini">'+escM(x.category||'')+' '+escM(x.note||'')+'</div></div><div><b>'+((x.sign||1)>0?'+':'−')+M(x.amount)+'</b><div class="manual-actions">'+(x.source==='day'?'<button data-mopen="calc" type="button">Открыть</button>':x.source==='fuel'?'<button data-medit-fuel="'+x.id+'" type="button">Изм.</button><button data-mdelete-fuel="'+x.id+'" type="button">Удал.</button>':'<button data-medit="'+x.id+'" data-mtype="'+x.type+'" type="button">Изм.</button><button data-mdelete="'+x.id+'" type="button">Удал.</button>')+'</div></div></div>'}).join('');
 Q('manualHistory').querySelectorAll('[data-medit]').forEach(function(b){b.onclick=function(){openManual(b.getAttribute('data-mtype'),Number(b.getAttribute('data-medit')))}});Q('manualHistory').querySelectorAll('[data-mdelete]').forEach(function(b){b.onclick=function(){deleteManual('ledger',Number(b.getAttribute('data-mdelete')))}});Q('manualHistory').querySelectorAll('[data-medit-fuel]').forEach(function(b){b.onclick=function(){openFuelManual(Number(b.getAttribute('data-medit-fuel')))}});Q('manualHistory').querySelectorAll('[data-mdelete-fuel]').forEach(function(b){b.onclick=function(){deleteManual('fuel',Number(b.getAttribute('data-mdelete-fuel')))}});Q('manualHistory').querySelectorAll('[data-mopen]').forEach(function(b){b.onclick=function(){closeManual();showView('calc')}})
}

async function saveManual(){
 var type=Q('manualType').value,amount=Math.abs(Number(Q('manualAmount').value)||0);if(!amount){say('Укажи сумму');return}
 var d=Q('manualDate').value,ts=d?new Date(d+'T12:00:00').getTime():Date.now(),data={type:type,amount:amount,sign:Number(Q('manualSign').value)||1,category:Q('manualCat').value.trim(),note:Q('manualNote').value.trim(),ts:ts};
 var wasEdit=!!editId;if(editId)data.id=editId;await put('ledger',data);closeManual();await refreshManual();say(wasEdit?'Запись изменена':'Запись сохранена')
}
async function deleteManual(source,id){
 if(source==='fuel'){await deleteFuelManual(id);return}
 await new Promise(function(res,rej){var tr=db.transaction('ledger','readwrite'),q=tr.objectStore('ledger').delete(id);q.onsuccess=res;q.onerror=function(){rej(q.error)}});await refreshManual();if(Q('manualOpModal').classList.contains('show'))await renderManualHistory(activeType);say('Запись удалена')
}
function openFuelManual(id){
 fuelEditId=id||null;Q('fuelModal').classList.add('show');Q('refuelLiters').value='';Q('refuelCost').value='';Q('refuelPriceHint').textContent='Цена за литр посчитается автоматически.';
 if(id)all('refuels').then(function(a){var x=a.find(function(r){return r.id===id});if(x){Q('refuelLiters').value=x.liters;Q('refuelCost').value=x.cost;Q('refuelPriceHint').textContent='Цена: '+Number(x.pricePerLiter||0).toLocaleString('ru-RU',{maximumFractionDigits:2})+' ₽/л.'}})
}
async function saveFuelManual(){
 var l=Math.abs(Number(Q('refuelLiters').value)||0),c=Math.abs(Number(Q('refuelCost').value)||0);if(!l||!c){say('Укажи литры и сумму');return}
 var d={ts:Date.now(),liters:l,cost:c,pricePerLiter:c/l};if(fuelEditId)d.id=fuelEditId;await put('refuels',d);fuelEditId=null;Q('fuelModal').classList.remove('show');await refreshManual();say('Заправка сохранена')
}
async function deleteFuelManual(id){await new Promise(function(res,rej){var tr=db.transaction('refuels','readwrite'),q=tr.objectStore('refuels').delete(id);q.onsuccess=res;q.onerror=function(){rej(q.error)}});await refreshManual();say('Заправка удалена')}

function showManualView(id){showView(id);if(id==='stats')refreshManual()}
async function clearAllManual(){
 if(!window.confirm('Полностью очистить историю доходов, расходов, расчётов и заправок? Настройки останутся. Отменить это действие нельзя.'))return;
 try{await new Promise(function(resolve,reject){var tr=db.transaction(['calculations','refuels','ledger','aiChat'],'readwrite');['calculations','refuels','ledger','aiChat'].forEach(function(n){tr.objectStore(n).clear()});tr.oncomplete=resolve;tr.onerror=function(){reject(tr.error)};tr.onabort=function(){reject(tr.error||new Error('Отменено'))}});await refreshManual();if(Q('manualHistory'))await renderManualHistory(activeType);say('История полностью очищена')}catch(e){say('Не удалось очистить историю. Попробуй ещё раз.')}
}
async function resetAllManual(){
 if(!window.confirm('СБРОСИТЬ ВСЁ ПРИЛОЖЕНИЕ? Будут удалены история, заправки, расчёты и твои настройки. Сначала сохрани резервную копию, если она нужна.'))return;
 try{await new Promise(function(resolve,reject){var tr=db.transaction(['calculations','refuels','ledger','aiChat','settings'],'readwrite');['calculations','refuels','ledger','aiChat','settings'].forEach(function(n){tr.objectStore(n).clear()});tr.oncomplete=resolve;tr.onerror=function(){reject(tr.error)}});await refreshManual();say('Приложение очищено. Перезапусти его для начальных настроек.')}catch(e){say('Не удалось сбросить данные.')}
}
function addDataTools(){var s=Q('settings');if(!s||Q('manualDataTools'))return;s.insertAdjacentHTML('beforeend','<div class="manual-data-tools" id="manualDataTools"><div class="section-title">🧰 История и резервная копия</div><div class="card grid"><div class="manual-muted">Сначала экспортируй резервную копию, если хочешь сохранить записи. Очистка истории удаляет все сохранённые доходы, расходы, расчёты и заправки, но оставляет настройки.</div><button class="btn secondary full" id="manualExport" type="button">⬇️ Экспортировать резервную копию</button><button class="btn secondary full manual-danger" id="manualClearHistory" type="button">🗑️ Полностью очистить историю</button><button class="btn secondary full manual-danger" id="manualResetAll" type="button">Сбросить всё приложение</button></div></div>');Q('manualExport').onclick=function(){if(typeof exportDB==='function')exportDB();else say('Экспорт сейчас недоступен')};Q('manualClearHistory').onclick=clearAllManual;Q('manualResetAll').onclick=resetAllManual;}

async function initManual(){
 addStyle();compactNav();replaceFinance();addModal();addDataTools();
 if(Q('fuelModal')){Q('saveFuelBtn').onclick=saveFuelManual;Q('closeFuelModal').onclick=function(){Q('fuelModal').classList.remove('show')};Q('cancelFuelBtn').onclick=function(){Q('fuelModal').classList.remove('show')};Q('refuelCost').oninput=function(){var l=Number(Q('refuelLiters').value)||0,c=Number(Q('refuelCost').value)||0;Q('refuelPriceHint').textContent=l&&c?'Получается '+(c/l).toLocaleString('ru-RU',{maximumFractionDigits:2})+' ₽/л.':'Цена за литр посчитается автоматически.'}};
 if(Q('clearHistoryBtn'))Q('clearHistoryBtn').onclick=clearAllManual;
 if(Q('exportBtn'))Q('exportBtn').onclick=function(){if(typeof exportDB==='function')exportDB()};
 await refreshManual();
 showView('home');
}
setTimeout(function(){initManual()},180);
})();