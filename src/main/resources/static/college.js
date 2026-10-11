const root=document.getElementById('college');
const id=new URLSearchParams(location.search).get('id');
const money=value=>value==null?'Not listed':`₹${Number(value).toLocaleString('en-IN')}`;
const cell=value=>value==null||value===''?'Not listed':String(value);

function section(title){
  const wrapper=document.createElement('section');
  wrapper.className='college-detail-section';
  const heading=document.createElement('h2');
  heading.className='h4'; heading.textContent=title;
  wrapper.append(heading);
  return wrapper;
}

function emptyState(message){
  const text=document.createElement('p'); text.className='text-secondary mb-0'; text.textContent=message; return text;
}

function table(headers,rows,label){
  if(!rows.length)return emptyState('No published demo records are available for this college.');
  const wrapper=document.createElement('div'); wrapper.className='table-responsive';
  const element=document.createElement('table'); element.className='table table-sm table-striped align-middle'; element.setAttribute('aria-label',label);
  const thead=document.createElement('thead'),headRow=document.createElement('tr');
  headers.forEach(value=>{const th=document.createElement('th');th.scope='col';th.textContent=value;headRow.append(th)});
  thead.append(headRow); const body=document.createElement('tbody');
  rows.forEach(row=>{const tr=document.createElement('tr');row.forEach(value=>{const td=document.createElement('td');td.textContent=cell(value);tr.append(td)});body.append(tr)});
  element.append(thead,body); wrapper.append(element); return wrapper;
}

function render(data){
  const c=data.college;
  root.replaceChildren();
  const card=document.createElement('article'); card.className='college-card college-detail-card';
  const title=document.createElement('h1'); title.textContent=cell(c.name);
  const location=document.createElement('p'); location.className='college-meta'; location.textContent=[c.city,c.state].filter(Boolean).join(', ');
  const description=document.createElement('p'); description.textContent=c.description||'No description provided.';
  const basics=document.createElement('dl'); basics.className='row mt-4';
  const fields=[['College type',c.collegeType],['Fees',money(c.fees)],['NIRF engineering rank',c.ranking],['Established',c.establishedYear],['Hostel',c.hostelAvailable?'Available':'Not listed'],['Accreditation',c.accreditation],['Address',c.address]];
  fields.forEach(([label,value])=>{const dt=document.createElement('dt');dt.className='col-sm-4';dt.textContent=label;const dd=document.createElement('dd');dd.className='col-sm-8';dd.textContent=cell(value);basics.append(dt,dd)});
  card.append(title,location,description,basics);
  if(c.website){try{const url=new URL(c.website);if(['https:','http:'].includes(url.protocol)){const link=document.createElement('a');link.href=url.href;link.rel='noopener noreferrer';link.target='_blank';link.textContent='Visit official website';link.className='btn btn-outline-dark';card.append(link)}}catch{}}
  root.append(card);

  const programmes=section('Courses');
  programmes.append(data.courses.length?createList(data.courses.map(x=>x.name)):emptyState('No course records are available.'));
  root.append(programmes);
  const branchSection=section('Branches');
  branchSection.append(data.branches.length?createList(data.branches.map(x=>x.name)):emptyState('No branch records are available.'));
  root.append(branchSection);

  const cutoffSection=section('Admission cutoffs');
  cutoffSection.append(table(['Exam','Year','Category','Course','Branch','Closing rank','Percentile'],data.cutoffs.map(x=>[
    x.entranceExam,x.examYear,x.category,data.courses.find(c=>c.id===x.courseId)?.name,
    data.branches.find(b=>b.id===x.branchId)?.name,x.closingRank,x.closingPercentile==null?null:`${x.closingPercentile}%`
  ]),'Illustrative admission cutoff data'));
  root.append(cutoffSection);

  const placementSection=section('Placement snapshot');
  placementSection.append(table(['Year','Course','Placed','Eligible','Placement rate','Average package','Highest package'],data.placements.map(x=>[
    x.year,data.courses.find(c=>c.id===x.courseId)?.name,x.studentsPlaced,x.studentsEligible,
    x.studentsPlaced!=null&&x.studentsEligible?`${Math.round(x.studentsPlaced/x.studentsEligible*100)}%`:null,
    money(x.averagePackage),money(x.highestPackage)
  ]),'Illustrative placement statistics'));
  root.append(placementSection);
}

function createList(values){const list=document.createElement('ul');list.className='detail-list';values.forEach(value=>{const item=document.createElement('li');item.textContent=cell(value);list.append(item)});return list;}

async function load(){
  if(!id||!/^[0-9]+$/.test(id)){root.replaceChildren(emptyState('A valid college ID is required.'));return}
  try{const response=await fetch(`/api/colleges/${encodeURIComponent(id)}/details`);if(!response.ok)throw new Error(response.status===404?'College not found.':'Unable to load college details.');render(await response.json())}
  catch(error){root.replaceChildren(emptyState(error.message||'Unable to load college details. Please try again.'))}
}
load();
