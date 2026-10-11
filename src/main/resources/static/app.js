let page=0,token=localStorage.getItem('cw_token')||'',registerMode=false,compareIds=[];
const el=id=>document.getElementById(id);
const esc=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const money=value=>value==null?'Not listed':`₹${Number(value).toLocaleString('en-IN')}`;

async function loadColleges(){
  const list=el('collegeList'); list.innerHTML='<p class="text-secondary" role="status">Loading colleges…</p>';
  const params=new URLSearchParams({page,size:'9',sort:el('sort').value});
  for(const [id,key] of [['query','q'],['state','state'],['city','city'],['maxFees','maxFees'],['hostel','hostel']])if(el(id).value)params.set(key,el(id).value);
  try{
    const response=await fetch('/api/colleges?'+params); if(!response.ok)throw new Error('Could not load colleges. Please retry.');
    const data=await response.json();
    list.innerHTML=data.content.length?data.content.map(college=>`<div class="col-md-6 col-lg-4"><article class="college-card"><p class="eyebrow mb-2">${esc(college.collegeType||'COLLEGE')}</p><h3>${esc(college.name)}</h3><p class="college-meta">${esc(college.city)}, ${esc(college.state)}</p><p>${esc((college.description||'Explore courses, campus life, and admissions at this institution.').slice(0,130))}</p><div class="d-flex justify-content-between small"><span>Fees: ${money(college.fees)}</span><span>${college.hostelAvailable?'Hostel':'No hostel'}</span></div><a class="btn btn-sm btn-outline-secondary mt-3 me-2" href="/college.html?id=${college.id}">Details</a><button class="btn btn-sm btn-outline-dark mt-3" data-compare-id="${college.id}" onclick="compareCollege(${college.id})">${compareIds.some(item=>item.id===college.id)?'Remove from compare':'Add to compare'}</button></article></div>`).join(''):'<div class="empty-state"><p class="mb-2">No colleges match these filters.</p><button class="btn btn-sm btn-outline-dark" id="clearFilters">Clear filters</button></div>';
    const clear=el('clearFilters'); if(clear)clear.onclick=()=>{['query','state','city','maxFees'].forEach(id=>el(id).value='');el('hostel').value='';page=0;loadColleges()};
    el('pageLabel').textContent=`Page ${data.number+1} · ${data.totalElements} colleges`;el('prev').disabled=data.first;el('next').disabled=data.last;
  }catch(error){list.innerHTML=`<div class="alert alert-danger" role="alert">${esc(error.message)} <button class="btn btn-sm btn-outline-danger ms-2" id="retryColleges">Retry</button></div>`;el('retryColleges').onclick=loadColleges}
}

function updateCompareTray(){
  const tray=el('compareTray');tray.classList.toggle('d-none',compareIds.length===0);
  el('compareCount').textContent=`${compareIds.length} of 3 colleges selected`;
  el('showCompare').disabled=compareIds.length<2;
  el('compareChips').replaceChildren(...compareIds.map(item=>{const chip=document.createElement('span');chip.className='compare-chip';chip.textContent=item.name;return chip}));
  document.querySelectorAll('[data-compare-id]').forEach(button=>{const id=Number(button.dataset.compareId);button.textContent=compareIds.some(x=>x.id===id)?'Remove from compare':'Add to compare'});
}

window.compareCollege=id=>{
  if(!token){setMode(false);el('authError').textContent='Sign in to compare colleges.';bootstrap.Modal.getOrCreateInstance(el('authModal')).show();return}
  const index=compareIds.findIndex(item=>item.id===id);
  if(index>=0)compareIds.splice(index,1);
  else if(compareIds.length>=3){el('compareCount').textContent='Remove a college before adding another (maximum 3).';return}
  else {const button=document.querySelector(`[data-compare-id="${id}"]`);const name=button?.closest('article')?.querySelector('h3')?.textContent||`College ${id}`;compareIds.push({id,name})}
  updateCompareTray();
};

function setMode(register){registerMode=register;el('nameField').classList.toggle('d-none',!register);el('authTitle').textContent=register?'Create your account':'Welcome back';el('loginTab').classList.toggle('active',!register);el('registerTab').classList.toggle('active',register);el('authError').textContent=''}
el('loginTab').onclick=()=>setMode(false);el('registerTab').onclick=()=>setMode(true);
document.querySelectorAll('[data-register]').forEach(button=>button.addEventListener('click',()=>setMode(true)));
el('authForm').onsubmit=async event=>{
  event.preventDefault();const data=Object.fromEntries(new FormData(event.currentTarget));
  try{const response=await fetch(registerMode?'/api/auth/register':'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});const result=await response.json();if(!response.ok)throw new Error(result.error||'Authentication failed. Check your details and try again.');
    token=result.token;localStorage.setItem('cw_token',token);el('welcome').textContent=`Hi, ${result.name}`;el('authButton').textContent='Sign out';bootstrap.Modal.getOrCreateInstance(el('authModal')).hide();
  }catch(error){el('authError').textContent=error.message}
};
el('authButton').onclick=async event=>{if(!token)return;try{await fetch('/api/auth/logout',{method:'POST',headers:{Authorization:`Bearer ${token}`}})}finally{localStorage.removeItem('cw_token');token='';el('welcome').textContent='';event.currentTarget.textContent='Sign in';compareIds=[];updateCompareTray()}};
['query','state','city','maxFees'].forEach(id=>el(id).addEventListener('input',()=>{page=0;loadColleges()}));
['hostel','sort'].forEach(id=>el(id).addEventListener('change',()=>{page=0;loadColleges()}));
el('prev').onclick=()=>{page=Math.max(0,page-1);loadColleges()};el('next').onclick=()=>{page++;loadColleges()};
el('clearCompare').onclick=()=>{compareIds=[];updateCompareTray()};
el('showCompare').onclick=showComparison;

function makeCompareTable(colleges){
  const metrics=[
    ['Location',college=>`${college.college.city}, ${college.college.state}`],
    ['College type',college=>college.college.collegeType],
    ['Fees',college=>money(college.college.fees)],
    ['NIRF Engineering rank',college=>college.college.ranking],
    ['Hostel',college=>college.college.hostelAvailable?'Available':'Not listed'],
    ['Courses',college=>college.courses.map(row=>row.name).join(', ')],
    ['Branches',college=>college.branches.map(row=>row.name).join(', ')],
    ['Latest average package',college=>{const placement=college.placements.at(-1);return placement?money(placement.averagePackage):'Not listed'}],
    ['Latest highest package',college=>{const placement=college.placements.at(-1);return placement?money(placement.highestPackage):'Not listed'}],
    ['Latest placement rate',college=>{const placement=college.placements.at(-1);return placement?.studentsEligible?`${Math.round(placement.studentsPlaced/placement.studentsEligible*100)}%`:'Not listed'}],
    ['Admission exams / closing ranks',college=>college.cutoffs.map(row=>`${row.entranceExam} ${row.examYear}: ${row.closingRank??`${row.closingPercentile}%`}`).join('; ')],
    ['Established',college=>college.college.establishedYear],
    ['Website',college=>college.college.website]
  ];
  const table=document.createElement('table');table.className='table table-bordered align-middle comparison-table';table.setAttribute('aria-label','College comparison details');
  const head=document.createElement('thead'),header=document.createElement('tr'),blank=document.createElement('th');blank.scope='col';blank.textContent='Compare';header.append(blank);
  colleges.forEach(college=>{const th=document.createElement('th');th.scope='col';th.textContent=college.college.name;header.append(th)});head.append(header);const body=document.createElement('tbody');
  metrics.forEach(([label,value])=>{const tr=document.createElement('tr'),th=document.createElement('th');th.scope='row';th.textContent=label;tr.append(th);colleges.forEach(college=>{const td=document.createElement('td');td.textContent=value(college)||'Not listed';tr.append(td)});body.append(tr)});
  table.append(head,body);el('comparisonResult').replaceChildren(table);
}
async function showComparison(){
  if(compareIds.length<2)return;el('comparisonResult').replaceChildren();el('comparisonLoading').textContent='Loading comparison…';
  bootstrap.Modal.getOrCreateInstance(el('compareModal')).show();
  try{const details=await Promise.all(compareIds.map(async item=>{const response=await fetch(`/api/colleges/${item.id}/details`,{headers:{Authorization:`Bearer ${token}`}});if(!response.ok)throw new Error('Unable to load comparison details. Please retry.');return response.json()}));makeCompareTable(details);el('comparisonLoading').textContent='Demo fees, placements and cutoff values are illustrative estimates.'}
  catch(error){el('comparisonLoading').textContent=error.message}
}

if(token){el('welcome').textContent='Signed in';el('authButton').textContent='Sign out'}
loadColleges();
