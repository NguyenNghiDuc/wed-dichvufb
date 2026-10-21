(() => {
 'use strict';
 const input=document.getElementById('service-search'),results=document.getElementById('search-results');
 if(!input||!results)return;
 const plans=[...document.querySelectorAll('.service-plan-card')];
 const normalize=s=>s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d');
 input.addEventListener('input',()=>{
  results.replaceChildren();const q=normalize(input.value.trim());results.hidden=!q;if(!q)return;
  plans.forEach(plan=>plan.querySelectorAll('.consulting-platform-links a').forEach(destination=>{
   const title=plan.querySelector('h3').textContent+' · '+destination.textContent.replace('→','').trim();
   if(!normalize(title).includes(q))return;
   const link=document.createElement('a');link.textContent=title;link.href=destination.href;results.append(link);
  }));
  if(!results.childElementCount){const text=document.createElement('p');text.textContent='Chưa có gói phù hợp. Hãy thử từ khóa khác.';results.append(text);}
 });
 input.closest('form').addEventListener('submit',e=>{e.preventDefault();const first=results.querySelector('a');if(first)first.click();else document.getElementById('consulting-services').scrollIntoView({behavior:'smooth'});});
 document.addEventListener('click',e=>{if(!input.closest('form').contains(e.target))results.hidden=true;});
 document.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='k'){e.preventDefault();input.focus();}if(e.key==='Escape')results.hidden=true;});
})();

(() => {
 'use strict';
 const service=document.querySelector('.service-menu'),utility=document.querySelector('.utility-menu');
 if(!service||!utility)return;
 const menus=[service,utility],input=document.getElementById('mega-search');
 const tabs=[...document.querySelectorAll('[data-platform]')];
 const normalize=s=>s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d');
 let selected='tiktok';
 function render(){
  const q=normalize(input.value.trim());let total=0;
  tabs.forEach(tab=>{
   const panel=document.getElementById(tab.getAttribute('aria-controls'));
   let count=0;
   panel.querySelectorAll('.mega-service').forEach(link=>{link.hidden=!!q&&!normalize(link.textContent+' '+tab.textContent).includes(q);if(!link.hidden)count++;});
   panel.querySelector('.mega-count').textContent=count+' dịch vụ';
   panel.hidden=q?count===0:tab.dataset.platform!==selected;
   tab.setAttribute('aria-selected',String(tab.dataset.platform===selected));tab.tabIndex=tab.dataset.platform===selected?0:-1;
   if(!panel.hidden)total+=count;
  });
  document.querySelector('.mega-no-results').hidden=total>0;
 }
 tabs.forEach((tab,index)=>{tab.addEventListener('click',()=>{selected=tab.dataset.platform;input.value='';render();});tab.addEventListener('keydown',e=>{let n;if(['ArrowDown','ArrowRight'].includes(e.key))n=(index+1)%tabs.length;else if(['ArrowUp','ArrowLeft'].includes(e.key))n=(index+tabs.length-1)%tabs.length;else if(e.key==='Home')n=0;else if(e.key==='End')n=tabs.length-1;else return;e.preventDefault();tabs[n].click();tabs[n].focus();});});
 input.addEventListener('input',render);
 menus.forEach(menu=>{menu.querySelector('.panel-close').addEventListener('click',()=>{menu.open=false;menu.querySelector('summary').focus();});menu.addEventListener('toggle',()=>{if(menu.open)menus.forEach(other=>{if(other!==menu)other.open=false;});});menu.querySelectorAll('a').forEach(a=>a.addEventListener('click',()=>menu.open=false));});
 document.addEventListener('click',e=>menus.forEach(menu=>{if(!menu.contains(e.target))menu.open=false;}));
 document.addEventListener('keydown',e=>{if(e.key==='Escape'){const open=menus.find(m=>m.open);if(open){open.open=false;open.querySelector('summary').focus();}}});
 document.querySelector('[data-open-services]').addEventListener('click',e=>{e.stopPropagation();utility.open=false;service.open=true;input.focus();});
 document.querySelector('[data-focus-search]').addEventListener('click',()=>{utility.open=false;document.getElementById('service-search').focus();});
 const choices=[...document.querySelectorAll('[data-theme-choice]')],system=window.matchMedia('(prefers-color-scheme: light)');let theme='system';
 try{const saved=localStorage.getItem('growlab-theme');if(['system','light','dark'].includes(saved))theme=saved;}catch(e){}
 function apply(){document.documentElement.dataset.growlabTheme=theme==='system'?(system.matches?'light':'dark'):theme;choices.forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.themeChoice===theme)));}
 choices.forEach(b=>b.addEventListener('click',()=>{theme=b.dataset.themeChoice;try{localStorage.setItem('growlab-theme',theme);}catch(e){}apply();}));system.addEventListener('change',apply);
 render();apply();
})();
