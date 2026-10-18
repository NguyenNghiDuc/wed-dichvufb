(() => {
 'use strict';
 const country=document.getElementById('order-country');
 const server=document.getElementById('order-server');
 const quantity=document.getElementById('order-quantity');
 if(!country||!server||!quantity)return;
 const money=new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0});
 const options=[...server.options].filter(o=>o.value);
 const update=()=>{
  const selected=server.selectedOptions[0];
  const valid=selected?.value&&selected.dataset.country===country.value;
  const price=valid?Number(selected.dataset.price):0;
  document.getElementById('unit-price').textContent=valid?money.format(price):'— đ';
  const count=Number(quantity.value);
  const allowed=valid&&Number.isInteger(count)&&count>=Number(selected.dataset.min)&&count<=Number(selected.dataset.max);
  document.getElementById('total-price').textContent=allowed?money.format(price*count):'— đ';
  document.getElementById('service-detail').textContent=valid?(selected.dataset.description||'Yêu cầu được xét duyệt thủ công. Chưa bao gồm thanh toán.'):'Chọn server để xem mô tả gói.';
  document.getElementById('quantity-hint').textContent=valid?`Tối thiểu ${Number(selected.dataset.min).toLocaleString('vi-VN')} · Tối đa ${Number(selected.dataset.max).toLocaleString('vi-VN')}`:'Chọn server để xem giới hạn số lượng.';
  quantity.min=valid?selected.dataset.min:'1';quantity.max=valid?selected.dataset.max:'1000000';
  document.getElementById('server-version').value=valid?selected.dataset.version:'0';
  const link=document.getElementById('channel-url');
  const step=!link.value?0:!country.value?1:!valid?2:3;
  document.querySelectorAll('.order-progress li').forEach((item,index)=>item.classList.toggle('current',index===step));
 };
 const filter=()=>{
  options.forEach(option=>{const match=option.dataset.country===country.value;option.hidden=!match;option.disabled=!match;});
  if(server.value&&server.selectedOptions[0].dataset.country!==country.value)server.value='';
  server.disabled=!country.value;
  server.options[0].textContent=country.value?'Chọn server':'Chọn quốc gia trước';
  update();
 };
 country.addEventListener('change',filter);
 server.addEventListener('change',()=>{
  const selected=server.selectedOptions[0];
  if(selected?.value){quantity.value=String(Math.min(Number(selected.dataset.max),Math.max(Number(selected.dataset.min),Number(quantity.value)||Number(selected.dataset.min))));}
  update();
 });
 quantity.addEventListener('input',update);
 document.getElementById('channel-url').addEventListener('input',update);
 const search=document.getElementById('service-search');
 const results=document.getElementById('search-results');
 const normalize=value=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').toLowerCase();
 search.addEventListener('input',()=>{
  results.replaceChildren();const q=normalize(search.value.trim());results.hidden=!q;if(!q)return;
  const matches=options.filter(o=>normalize(o.textContent+' '+o.dataset.description).includes(q));
  matches.forEach(option=>{const button=document.createElement('button');button.type='button';button.textContent=option.textContent;button.addEventListener('click',()=>{country.value=option.dataset.country;filter();server.value=option.value;server.dispatchEvent(new Event('change'));results.hidden=true;server.focus();});results.append(button);});
  if(!matches.length){const p=document.createElement('p');p.textContent='Chưa có server phù hợp. Hãy thử từ khóa khác.';results.append(p);}
 });
 search.closest('form').addEventListener('submit',e=>{e.preventDefault();results.querySelector('button')?.click();});
 document.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='k'){e.preventDefault();search.focus();}if(e.key==='Escape')results.hidden=true;});
 document.addEventListener('click',e=>{if(!search.closest('form').contains(e.target))results.hidden=true;});
 document.getElementById('tiktok-order-form').addEventListener('submit',()=>{const b=document.querySelector('.boost-submit');b.disabled=true;});
 window.addEventListener('pageshow',()=>{document.querySelector('.boost-submit').disabled=!options.length;filter();});
 filter();
})();
