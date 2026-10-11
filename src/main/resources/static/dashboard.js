const token=localStorage.getItem('cw_token');
const $=id=>document.getElementById(id);
const headers={'Authorization':`Bearer ${token}`,'Content-Type':'application/json'};
const message=$('message');
document.getElementById('logout').onclick=async()=>{try{await fetch('/api/auth/logout',{method:'POST',headers:{Authorization:`Bearer ${token}`}})}finally{localStorage.removeItem('cw_token');location.href='/'}};
const safe=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

if(!token)message.innerHTML='Please <a href="/">sign in</a> to view your student dashboard.';
else start();

async function api(path,options={}){
  const response=await fetch('/api'+path,{...options,headers:{...headers,...options.headers}});
  if(!response.ok){const body=await response.json().catch(()=>({}));throw new Error(body.error||`Request failed (${response.status})`)}
  return response.status===204?null:response.json();
}

async function start(){
  try{
    const profile=await api('/students/profile');$('app').classList.remove('d-none');
    message.textContent=`Welcome, ${profile.name}. Add or update your preferences to improve your matches.`;
    const form=$('profileForm');
    for(const [key,value]of Object.entries(profile)){if(value==null)continue;const input=form.elements[key];if(input){if(input.type==='checkbox')input.checked=value;else input.value=value}}
    await Promise.all([loadRecommendations(),loadFavorites(),loadCareer()]);await loadHistory();
  }catch(error){message.textContent=error.message}
}

function card(college,extra=''){
  const reasons=college.reasons?.length?`<h4 class="h6 mt-3 mb-1">Why this match</h4><ul class="small mb-2">${college.reasons.map(reason=>`<li>${safe(reason)}</li>`).join('')}</ul>`:'';
  const concerns=college.concerns?.length?`<h4 class="h6 mt-2 mb-1">Things to check</h4><ul class="small text-secondary mb-2">${college.concerns.map(reason=>`<li>${safe(reason)}</li>`).join('')}</ul>`:'';
  const cutoff=college.admissionCompatibility?`<span class="badge text-bg-light">Admission: ${safe(college.admissionCompatibility.replaceAll('_',' '))}</span>`:'';
  const match=college.score!=null?`<span class="match-score">${college.score}% match</span>`:'';
  return `<div class="col-12"><article class="college-card"><div class="d-flex justify-content-between gap-3"><div><h3>${safe(college.collegeName||college.name)}</h3><p class="college-meta mb-1">${match} ${cutoff}</p><a class="small" href="/college.html?id=${Number(college.collegeId||college.id)}">View college details</a>${college.explanation?`<p class="small mt-2 mb-1">${safe(college.explanation)}</p>`:''}${reasons}${concerns}${college.fees!=null?`<small>Fees: ₹${Number(college.fees).toLocaleString('en-IN')}</small>`:''}</div>${extra}</div></article></div>`;
}

async function loadRecommendations(){
  $('recommendations').innerHTML='<p class="text-secondary" role="status">Loading recommendations…</p>';
  try{const rows=await api('/students/recommendations');$('recommendations').innerHTML=rows.length?rows.map(row=>card(row,`<button class="btn btn-sm btn-outline-dark align-self-start" onclick="saveFavorite(${row.collegeId})">Save</button>`)).join(''):'<p class="text-secondary">No college matches are available yet. Complete your profile and try again.</p>'}
  catch(error){$('recommendations').innerHTML=`<p class="text-danger" role="alert">${safe(error.message)}</p>`}
}

async function loadFavorites(){
  $('favorites').innerHTML='<p class="text-secondary" role="status">Loading saved colleges…</p>';
  try{const rows=await api('/students/favorites');$('favorites').innerHTML=rows.length?rows.map(college=>card(college,`<button class="btn btn-sm btn-outline-danger align-self-start" onclick="removeFavorite(${college.id})">Remove</button>`)).join(''):'<p class="text-secondary">No saved colleges yet. Save a college from your recommendations.</p>'}
  catch(error){$('favorites').innerHTML=`<p class="text-danger" role="alert">${safe(error.message)}</p>`}
}

async function loadCareer(){try{const result=await api('/students/career-guidance');$('career').textContent=result.guidance}catch(error){$('career').textContent=error.message}}

async function loadHistory(){
  $('history').innerHTML='<p class="text-secondary" role="status">Loading recommendation history…</p>';
  try{const rows=await api('/students/recommendations/history');$('history').innerHTML=rows.length?rows.map(row=>`<div class="col-12 col-md-6"><article class="college-card"><h3 class="h6">${safe(row.collegeName)}</h3><p class="college-meta mb-1">${row.score}% match · ${new Date(row.createdAt).toLocaleString()}</p><a class="small" href="/college.html?id=${row.collegeId}">View college details</a></article></div>`).join(''):'<p class="text-secondary">Your recommendation history will appear here after you refresh your matches.</p>'}
  catch(error){$('history').innerHTML=`<p class="text-danger" role="alert">${safe(error.message)}</p>`}
}

window.saveFavorite=async id=>{try{await api(`/students/favorites/${id}`,{method:'POST'});$('profileMessage').textContent='College added to your saved list.';await loadFavorites()}catch(error){$('profileMessage').textContent=error.message}};
window.removeFavorite=async id=>{try{await api(`/students/favorites/${id}`,{method:'DELETE'});$('profileMessage').textContent='College removed from your saved list.';await loadFavorites()}catch(error){$('profileMessage').textContent=error.message}};

$('profileForm').onsubmit=async event=>{
  event.preventDefault();const data=Object.fromEntries(new FormData(event.currentTarget));
  for(const key of ['tenthPercentage','twelfthPercentage','examYear','rank','percentile','maximumBudget'])data[key]=data[key]?Number(data[key]):null;
  data.hostelRequired=event.currentTarget.elements.hostelRequired.checked;
  $('profileMessage').textContent='Saving profile…';
  try{await api('/students/profile',{method:'PUT',body:JSON.stringify(data)});$('profileMessage').textContent='Profile saved.';await Promise.all([loadRecommendations(),loadCareer()]);await loadHistory()}
  catch(error){$('profileMessage').textContent=error.message}
};

$('refresh').onclick=async()=>{await loadRecommendations();await loadHistory()};
$('compare').onclick=async()=>{
  const ids=[...new Set($('compareIds').value.split(',').map(value=>Number(value.trim())).filter(Number.isInteger))];
  const output=$('comparison');
  if(ids.length<2||ids.length>3){output.innerHTML='<p class="text-danger" role="alert">Enter 2 or 3 distinct college IDs.</p>';return}
  output.innerHTML='<p class="text-secondary" role="status">Loading comparison…</p>';
  try{
    const response=await api('/students/compare?'+ids.map(id=>'ids='+encodeURIComponent(id)).join('&'));
    const details=await Promise.all(response.map(async college=>api(`/colleges/${college.id}/details`)));
    renderComparison(details,output);
  }catch(error){output.innerHTML=`<p class="text-danger" role="alert">${safe(error.message)}</p>`}
};

function renderComparison(colleges,output){
  const rows=[
    ['Location',item=>`${item.college.city}, ${item.college.state}`],
    ['Fees',item=>item.college.fees==null?'Not listed':`₹${Number(item.college.fees).toLocaleString('en-IN')}`],
    ['Ranking',item=>item.college.ranking??'Not listed'],
    ['Hostel',item=>item.college.hostelAvailable?'Available':'Not listed'],
    ['Courses',item=>item.courses.map(row=>row.name).join(', ')||'Not listed'],
    ['Branches',item=>item.branches.map(row=>row.name).join(', ')||'Not listed'],
    ['Average placement',item=>item.placements.length?`₹${Number(item.placements.at(-1).averagePackage).toLocaleString('en-IN')}`:'Not listed'],
    ['Highest placement',item=>item.placements.length?`₹${Number(item.placements.at(-1).highestPackage).toLocaleString('en-IN')}`:'Not listed'],
    ['Admission cutoffs',item=>item.cutoffs.map(row=>`${row.entranceExam} ${row.examYear}: ${row.closingRank??row.closingPercentile}`).join('; ')||'Not listed']
  ];
  const table=document.createElement('table');table.className='table table-bordered align-middle comparison-table';table.setAttribute('aria-label','College comparison');
  const thead=document.createElement('thead'),head=document.createElement('tr'),first=document.createElement('th');first.scope='col';first.textContent='Compare';head.append(first);
  colleges.forEach(item=>{const th=document.createElement('th');th.scope='col';th.textContent=item.college.name;head.append(th)});thead.append(head);
  const tbody=document.createElement('tbody');rows.forEach(([label,value])=>{const tr=document.createElement('tr'),th=document.createElement('th');th.scope='row';th.textContent=label;tr.append(th);colleges.forEach(item=>{const td=document.createElement('td');td.textContent=value(item);tr.append(td)});tbody.append(tr)});
  table.append(thead,tbody);output.replaceChildren(table);
}
