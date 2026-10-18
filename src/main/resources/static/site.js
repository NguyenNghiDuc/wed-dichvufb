(() => {
 'use strict';
 // Keep the security decision on the server; these controls only improve usability.
 document.querySelectorAll('input[type=password]').forEach((input,index)=>{
  if(input.closest('.password-wrap'))return;
  const wrap=document.createElement('div');wrap.className='password-wrap';input.before(wrap);wrap.append(input);
  if(!input.id)input.id='password-field-'+index;
  const toggle=document.createElement('button');toggle.type='button';toggle.className='password-toggle';toggle.textContent='Hiện';
  toggle.setAttribute('aria-controls',input.id);toggle.setAttribute('aria-pressed','false');toggle.setAttribute('aria-label','Hiện mật khẩu');
  toggle.addEventListener('click',()=>{const show=input.type==='password';input.type=show?'text':'password';toggle.textContent=show?'Ẩn':'Hiện';toggle.setAttribute('aria-label',show?'Ẩn mật khẩu':'Hiện mật khẩu');toggle.setAttribute('aria-pressed',String(show));});wrap.append(toggle);
  if(input.name==='password'||input.name==='newPassword')input.addEventListener('input',()=>{
   input.setCustomValidity(new TextEncoder().encode(input.value).length>72?'Mật khẩu tối đa 72 byte UTF-8.':'');
  });
 });
 document.querySelectorAll('.field textarea[maxlength]').forEach((textarea,index)=>{
  if(textarea.id==='brief'&&document.getElementById('brief-count'))return;
  const counter=document.createElement('small');counter.className='field-counter';counter.id='field-counter-'+index;
  textarea.setAttribute('aria-describedby',[textarea.getAttribute('aria-describedby'),counter.id].filter(Boolean).join(' '));
  const update=()=>counter.textContent=`${textarea.value.length} / ${textarea.maxLength} ký tự`;
  textarea.after(counter);textarea.addEventListener('input',update);update();
 });
 const menus=[...document.querySelectorAll('.app-menu')];
 menus.forEach(menu=>menu.addEventListener('toggle',()=>{if(menu.open)menus.forEach(other=>{if(other!==menu)other.open=false;});}));
 document.addEventListener('click',event=>menus.forEach(menu=>{if(!menu.contains(event.target))menu.open=false;}));
 document.addEventListener('keydown',event=>{if(event.key==='Escape')menus.forEach(menu=>menu.open=false);});
 document.querySelectorAll('.app-topbar a[href]').forEach(link=>{
  if(link.pathname===location.pathname)link.setAttribute('aria-current','page');
 });
 const dialog=document.createElement('dialog');dialog.className='confirm-dialog';dialog.setAttribute('aria-labelledby','site-confirm-title');
 const title=document.createElement('h2');title.id='site-confirm-title';title.textContent='Xác nhận thao tác';
 const message=document.createElement('p');message.className='muted';const actions=document.createElement('div');actions.className='actions';
 const cancel=document.createElement('button');cancel.type='button';cancel.className='btn btn-outline';cancel.textContent='Quay lại';
 const proceed=document.createElement('button');proceed.type='button';proceed.className='btn';proceed.textContent='Xác nhận';
 actions.append(cancel,proceed);dialog.append(title,message,actions);document.body.append(dialog);
 let pending=null;const confirmed=new WeakSet();
 cancel.addEventListener('click',()=>dialog.close());
 proceed.addEventListener('click',()=>{if(pending){confirmed.add(pending.form);dialog.close();pending.form.requestSubmit(pending.submitter||undefined);}});
 const busy=new Map();
 document.addEventListener('submit',event=>{
  if(event.defaultPrevented)return;
  const form=event.target;if(form.method.toLowerCase()!=='post')return;
  // Let the browser own authentication, cookies and redirects, including through
  // Codespaces proxies. Never leave auth controls disabled during navigation.
  if(form.closest('.auth-page'))return;
  if(form.dataset.confirm&&!confirmed.has(form)){
   event.preventDefault();message.textContent=form.dataset.confirm;pending={form,submitter:event.submitter};
   if(typeof dialog.showModal==='function')dialog.showModal();
   else if(window.confirm(message.textContent)){confirmed.add(form);form.requestSubmit(event.submitter||undefined);}return;
  }
  confirmed.delete(form);
  const button=event.submitter;if(!button||button.name||button.disabled)return;
  busy.set(button,{disabled:button.disabled,text:button.textContent});
  button.disabled=true;button.textContent='Đang xử lý…';
 });
 window.addEventListener('pageshow',()=>{busy.forEach((state,button)=>{button.disabled=state.disabled;button.textContent=state.text;});busy.clear();});
})();

(() => {
 const amount=document.getElementById('deposit-amount');if(!amount)return;
 const buttons=[...document.querySelectorAll('[data-deposit-amount]')],preview=document.getElementById('deposit-preview');
 const money=new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0});
 const update=()=>{const value=Number(amount.value);preview.textContent=amount.value&&amount.validity.valid?money.format(value):'— đ';buttons.forEach(b=>b.setAttribute('aria-pressed',String(Number(b.dataset.depositAmount)===value)));};
 buttons.forEach(b=>b.addEventListener('click',()=>{amount.value=b.dataset.depositAmount;amount.dispatchEvent(new Event('input',{bubbles:true}));}));amount.addEventListener('input',update);window.addEventListener('pageshow',update);update();
})();
