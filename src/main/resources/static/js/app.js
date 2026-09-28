/**
 * MentorConnect — Frontend Application Script
 * Communicates with Spring Boot REST Controllers & handles UI rendering
 */

const API = {
  tags: '/api/tags',
  alumni: '/api/alumni',
  students: '/api/students',
  matching: '/api/matching',
  pairs: '/api/mentorship-pairs',
  sessions: '/api/sessions',
  reports: '/api/reports',
  audit: '/api/audit-logs'
};

let allAlumni = [];
let allStudents = [];
let allTags = [];
let allPairs = [];
let allAuditLogs = [];
let selectedAlumniTags = new Set();
let selectedStudentTags = new Set();

// Utility for making API requests with robust error extraction
async function apiCall(url, method = 'GET', body = null) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' }
  };
  if (body) {
    options.body = JSON.stringify(body);
  }

  try {
    const res = await fetch(url, options);
    const contentType = res.headers.get('content-type');
    let data = null;
    if (contentType && contentType.includes('application/json')) {
      data = await res.json();
    }

    if (!res.ok) {
      let errMsg = 'Request failed';
      if (data && data.message) {
        errMsg = data.message;
      } else if (data && data.error) {
        errMsg = data.error;
      } else {
        errMsg = `Server responded with status ${res.status}`;
      }
      throw new Error(errMsg);
    }

    return data;
  } catch (err) {
    console.error('API Error:', err);
    throw err;
  }
}

// Toast notification display
function showToast(title, message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <div class="toast-content">
      <div class="toast-title">${escapeHtml(title)}</div>
      <div class="toast-message">${escapeHtml(message)}</div>
    </div>
    <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
  `;
  container.appendChild(toast);
  setTimeout(() => {
    if (toast.parentElement) toast.remove();
  }, 4500);
}

function escapeHtml(text) {
  if (!text) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Tab navigation
function switchTab(tabName) {
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.tab === tabName);
  });
  document.querySelectorAll('.tab-content').forEach(section => {
    section.classList.toggle('active', section.id === `tab-${tabName}`);
  });

  // Refresh tab-specific data
  if (tabName === 'dashboard') loadDashboard();
  if (tabName === 'alumni') fetchAlumni();
  if (tabName === 'students') fetchStudents();
  if (tabName === 'pairs') fetchPairs();
  if (tabName === 'sessions') fetchSessions();
  if (tabName === 'reports') fetchEngagementReport();
  if (tabName === 'audit') fetchAuditLogs();
  if (tabName === 'matcher') populateMatcherDropdown();
}

// Initialize Application
document.addEventListener('DOMContentLoaded', async () => {
  await fetchTags();
  await loadDashboard();
  setupDatetimeDefault();
});

function setupDatetimeDefault() {
  const localDate = new Date(Date.now() + 86400000 - (new Date().getTimezoneOffset() * 60000));
  localDate.setMinutes(0);
  const isoStr = localDate.toISOString().slice(0, 16);
  const dateInput = document.getElementById('session-date');
  if (dateInput) dateInput.value = isoStr;
}

// -------------------------------------------------------------
// Tags
// -------------------------------------------------------------
async function fetchTags() {
  try {
    allTags = await apiCall(API.tags);
    populateTagFilters();
    renderTagSelectors();
  } catch (err) {
    showToast('Failed to load tags', err.message, 'error');
  }
}

function populateTagFilters() {
  const select = document.getElementById('alumni-tag-filter');
  if (!select) return;
  select.innerHTML = '<option value="">All Expertise Tags</option>';
  allTags.forEach(tag => {
    const opt = document.createElement('option');
    opt.value = tag.name;
    opt.textContent = tag.name;
    select.appendChild(opt);
  });
}

function renderTagSelectors() {
  const alumniCont = document.getElementById('alumni-tag-selector');
  const studentCont = document.getElementById('student-tag-selector');

  if (alumniCont) {
    alumniCont.innerHTML = allTags.map(t => `
      <div class="tag-select-chip ${selectedAlumniTags.has(t.name) ? 'selected' : ''}"
           onclick="toggleTagSelection('${escapeHtml(t.name)}', 'alumni', this)">
        ${escapeHtml(t.name)}
      </div>
    `).join('');
  }

  if (studentCont) {
    studentCont.innerHTML = allTags.map(t => `
      <div class="tag-select-chip ${selectedStudentTags.has(t.name) ? 'selected' : ''}"
           onclick="toggleTagSelection('${escapeHtml(t.name)}', 'student', this)">
        ${escapeHtml(t.name)}
      </div>
    `).join('');
  }
}

function toggleTagSelection(tagName, type, element) {
  const set = type === 'alumni' ? selectedAlumniTags : selectedStudentTags;
  if (set.has(tagName)) {
    set.delete(tagName);
    element.classList.remove('selected');
  } else {
    set.add(tagName);
    element.classList.add('selected');
  }
}

// -------------------------------------------------------------
// Dashboard & Metrics
// -------------------------------------------------------------
async function loadDashboard() {
  try {
    const report = await apiCall(`${API.reports}/engagement`);
    document.getElementById('stat-alumni').textContent = report.totalAlumni;
    document.getElementById('stat-students').textContent = report.totalStudents;
    document.getElementById('stat-active-pairs').textContent = report.activePairs;
    document.getElementById('stat-sessions').textContent = `${report.completedSessions} / ${report.totalSessions}`;
    document.getElementById('stat-hours').textContent = report.totalCompletedHours.toFixed(1) + ' hrs';

    // Populate quick student select
    allStudents = await apiCall(API.students);
    const select = document.getElementById('quick-student-select');
    if (select) {
      select.innerHTML = '<option value="">-- Choose Student --</option>';
      allStudents.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.id;
        opt.textContent = `${s.name} (${s.department} '${s.batchYear})`;
        select.appendChild(opt);
      });
    }
  } catch (err) {
    console.warn('Dashboard stats error:', err);
  }
}

function runQuickMatch() {
  const select = document.getElementById('quick-student-select');
  if (!select.value) {
    showToast('Select Student', 'Please pick a student to calculate matching mentors', 'warning');
    return;
  }
  const studentId = select.value;
  switchTab('matcher');
  setMatcherMode('student');
  const matcherSelect = document.getElementById('matcher-student-select');
  matcherSelect.value = studentId;
  loadStudentSuggestions();
}

// -------------------------------------------------------------
// Auto-Suggest & Matching (Core Features 2 & 3)
// -------------------------------------------------------------
let selectedCustomMatcherTags = new Set();
let currentMatcherMode = 'student';

function setMatcherMode(mode) {
  currentMatcherMode = mode;
  const isStudent = mode === 'student';
  const studentModeEl = document.getElementById('matcher-student-mode');
  const tagsModeEl = document.getElementById('matcher-tags-mode');
  if (studentModeEl) studentModeEl.style.display = isStudent ? 'flex' : 'none';
  if (tagsModeEl) tagsModeEl.style.display = isStudent ? 'none' : 'flex';

  const btnStudent = document.getElementById('btn-mode-student');
  const btnTags = document.getElementById('btn-mode-tags');
  if (btnStudent) btnStudent.className = isStudent ? 'btn btn-primary btn-sm' : 'btn btn-secondary btn-sm';
  if (btnTags) btnTags.className = isStudent ? 'btn btn-secondary btn-sm' : 'btn btn-primary btn-sm';

  const studentInfoBanner = document.getElementById('selected-student-info');
  if (!isStudent) {
    if (studentInfoBanner) studentInfoBanner.style.display = 'none';
    renderCustomMatcherTags();
  } else {
    const studentId = document.getElementById('matcher-student-select')?.value;
    if (studentId) {
      loadStudentSuggestions();
    }
  }
}

function renderCustomMatcherTags() {
  const container = document.getElementById('matcher-custom-tags-container');
  if (!container) return;
  container.innerHTML = allTags.map(t => `
    <div class="tag-select-chip ${selectedCustomMatcherTags.has(t.name) ? 'selected' : ''}"
         onclick="toggleCustomMatcherTag('${escapeHtml(t.name)}', this)">
      ${escapeHtml(t.name)}
    </div>
  `).join('');
}

function toggleCustomMatcherTag(tagName, el) {
  if (selectedCustomMatcherTags.has(tagName)) {
    selectedCustomMatcherTags.delete(tagName);
    el.classList.remove('selected');
  } else {
    selectedCustomMatcherTags.add(tagName);
    el.classList.add('selected');
  }
}

async function loadCustomTagSuggestions() {
  if (selectedCustomMatcherTags.size === 0) {
    showToast('Select Tags', 'Please select at least one interest tag to search mentors', 'warning');
    return;
  }
  const container = document.getElementById('matcher-results');
  container.innerHTML = '<div style="grid-column: 1 / -1; text-align: center; padding: 2rem;">Calculating overlap ranking for custom tags...</div>';

  try {
    const tagsArray = Array.from(selectedCustomMatcherTags);
    const suggestions = await apiCall(API.matching + '/suggest-custom', 'POST', tagsArray);
    renderSuggestions(suggestions, null);
  } catch (err) {
    container.innerHTML = `<div class="empty-state" style="grid-column: 1 / -1;"><div class="empty-title">Error</div><div class="empty-desc">${escapeHtml(err.message)}</div></div>`;
    showToast('Match Error', err.message, 'error');
  }
}

async function populateMatcherDropdown() {
  if (allStudents.length === 0) {
    allStudents = await apiCall(API.students);
  }
  const select = document.getElementById('matcher-student-select');
  if (!select) return;
  select.innerHTML = '<option value="">-- Select Student --</option>';
  allStudents.forEach(s => {
    const opt = document.createElement('option');
    opt.value = s.id;
    opt.textContent = `${s.name} — ${s.department} (${s.interestTags.length} tags)`;
    select.appendChild(opt);
  });
}

async function loadStudentSuggestions() {
  const studentId = document.getElementById('matcher-student-select').value;
  if (!studentId) return;

  const container = document.getElementById('matcher-results');
  container.innerHTML = '<div style="grid-column: 1 / -1; text-align: center; padding: 2rem;">Calculating overlap ranking...</div>';

  try {
    const student = await apiCall(`${API.students}/${studentId}`);
    const suggestions = await apiCall(`${API.matching}/suggest/${studentId}`);

    // Update banner
    const infoBanner = document.getElementById('selected-student-info');
    infoBanner.style.display = 'block';
    document.getElementById('match-student-name').textContent = student.name;
    document.getElementById('match-student-dept').textContent = `${student.department} (Batch of ${student.batchYear})`;
    document.getElementById('match-student-tags').innerHTML = student.interestTags.map(t => `
      <span class="tag-badge" style="background:#e0e7ff; color:#3730a3;">${escapeHtml(t.name)}</span>
    `).join('');

    renderSuggestions(suggestions, studentId);
  } catch (err) {
    container.innerHTML = `<div class="empty-state" style="grid-column: 1 / -1;"><div class="empty-title">Error</div><div class="empty-desc">${escapeHtml(err.message)}</div></div>`;
    showToast('Match Error', err.message, 'error');
  }
}

function renderSuggestions(suggestions, studentId) {
  const container = document.getElementById('matcher-results');
  if (!suggestions || suggestions.length === 0) {
    container.innerHTML = '<div class="empty-state" style="grid-column: 1 / -1;"><div class="empty-icon">🔍</div><div class="empty-title">No Mentors Found</div></div>';
    return;
  }

  container.innerHTML = suggestions.map((item, index) => {
    const isTop = index === 0 && item.matchCount > 0;
    const isFull = item.availableSlots <= 0;
    const isAlready = item.alreadyMatched === true;
    const matchScoreClass = item.matchCount >= 3 ? 'high' : (item.matchCount >= 1 ? 'medium' : 'low');
    const slotClass = item.availableSlots > 1 ? 'available' : (item.availableSlots === 1 ? 'warning' : 'full');
    const usedPct = ((item.activeMentees / item.maxConcurrentMentees) * 100).toFixed(0);

    let actionBtnHtml = '';
    if (isAlready) {
      actionBtnHtml = `<button class="btn btn-secondary btn-sm" disabled style="opacity:0.8; cursor:not-allowed;">🤝 Already Mentoring</button>`;
    } else if (isFull) {
      actionBtnHtml = `<button class="btn btn-secondary btn-sm" disabled>Capacity Full</button>`;
    } else if (studentId) {
      actionBtnHtml = `<button class="btn btn-success btn-sm" onclick="handleCreatePair(${item.alumniId}, ${studentId}, '${escapeHtml(item.alumniName)}')">🤝 Match Student</button>`;
    } else {
      actionBtnHtml = `<button class="btn btn-primary btn-sm" onclick="promptStudentToPair(${item.alumniId}, '${escapeHtml(item.alumniName)}')">🤝 Match with Student...</button>`;
    }

    return `
      <div class="entity-card match-card ${isTop ? 'top-rank' : ''}">
        <div>
          <div class="entity-header">
            <div>
              <div class="entity-name">${escapeHtml(item.alumniName)}</div>
              <div class="entity-subtitle">${escapeHtml(item.jobTitle)} @ <strong>${escapeHtml(item.company)}</strong></div>
              <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.15rem;">Class of ${item.graduationYear} &bull; ${escapeHtml(item.email)}</div>
            </div>
            <div>
              <span class="match-score-badge ${matchScoreClass}">
                🎯 ${item.matchCount} Overlap ${isTop ? '• #1 Match' : ''} ${isAlready ? '• Current Mentor' : ''}
              </span>
            </div>
          </div>

          <!-- Capacity Bar -->
          <div class="capacity-box">
            <div class="capacity-header">
              <span>Concurrent Mentee Limit: <strong>${item.maxConcurrentMentees}</strong></span>
              <span style="color: ${isFull ? 'var(--danger)' : 'var(--success)'};">
                ${isFull ? '🔴 Capacity Full' : `🟢 ${item.availableSlots} slot(s) available`}
              </span>
            </div>
            <div class="capacity-bar-bg">
              <div class="capacity-bar-fill ${slotClass}" style="width: ${usedPct}%;"></div>
            </div>
            <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.3rem;">
              Currently mentoring ${item.activeMentees} of max ${item.maxConcurrentMentees} students
            </div>
          </div>

          <!-- Matching Tags Highlighting -->
          <div style="margin: 0.5rem 0;">
            <div style="font-size: 0.8rem; font-weight: 600; color: var(--text-muted); margin-bottom: 0.3rem;">Expertise Tags:</div>
            <div class="tag-list">
              ${item.allExpertiseTags.map(tag => {
                const isOverlapping = item.matchingTags.includes(tag);
                return `<span class="tag-badge ${isOverlapping ? 'match' : ''}">
                  ${isOverlapping ? '✓ ' : ''}${escapeHtml(tag)}
                </span>`;
              }).join('')}
            </div>
          </div>
        </div>

        <div style="margin-top: 1rem; border-top: 1px solid var(--border); padding-top: 0.85rem; display: flex; justify-content: space-between; align-items: center;">
          <span style="font-size: 0.8rem; font-weight: 600; color: ${isAlready ? 'var(--primary)' : (isFull ? 'var(--danger)' : 'var(--text-muted)')};">
            ${isAlready ? 'ℹ️ Active mentorship in progress' : (isFull ? '⚠️ Cannot accept more mentees' : '✅ Eligible for matching')}
          </span>
          ${actionBtnHtml}
        </div>
      </div>
    `;
  }).join('');
}

async function promptStudentToPair(alumniId, alumniName) {
  if (allStudents.length === 0) {
    allStudents = await apiCall(API.students);
  }
  const studentListStr = allStudents.map(s => `${s.id}: ${s.name} (${s.department})`).join('\n');
  const input = prompt(`Enter Student ID to match with Mentor ${alumniName}:\n\nAvailable Students:\n${studentListStr}`);
  if (!input) return;
  const studentId = parseInt(input.trim());
  if (isNaN(studentId) || studentId <= 0) {
    showToast('Invalid ID', 'Please enter a valid numeric student ID', 'warning');
    return;
  }
  handleCreatePair(alumniId, studentId, alumniName);
}

async function handleCreatePair(alumniId, studentId, alumniName) {
  const notes = prompt(`Confirm matching student with mentor ${alumniName}:\nEnter any mentorship goals/notes:`, 'Matched via tag overlap algorithm');
  if (notes === null) return; // cancelled by user

  try {
    const pair = await apiCall(API.matching + '/pair', 'POST', {
      alumniId: Number(alumniId),
      studentId: Number(studentId),
      notes: notes || 'Matched via algorithm'
    });

    showToast('Match Successful!', `Student matched with ${alumniName}. Mentorship pair is now ACTIVE.`, 'success');
    if (currentMatcherMode === 'student') {
      loadStudentSuggestions();
    } else {
      loadCustomTagSuggestions();
    }
    loadDashboard();
  } catch (err) {
    // Show exact business rule error from backend!
    showToast('Matching Rule Violation', err.message, 'error');
  }
}

// -------------------------------------------------------------
// Alumni Directory
// -------------------------------------------------------------
async function fetchAlumni() {
  const container = document.getElementById('alumni-grid');
  container.innerHTML = '<div style="grid-column: 1 / -1; text-align: center; padding: 2rem;">Loading alumni...</div>';
  try {
    allAlumni = await apiCall(API.alumni);
    renderAlumni(allAlumni);
  } catch (err) {
    container.innerHTML = `<div class="empty-state" style="grid-column: 1 / -1;"><div class="empty-title">Error</div><div class="empty-desc">${escapeHtml(err.message)}</div></div>`;
  }
}

function filterAlumni() {
  const search = document.getElementById('alumni-search').value.toLowerCase().trim();
  const tag = document.getElementById('alumni-tag-filter').value.toLowerCase().trim();

  const filtered = allAlumni.filter(a => {
    const matchSearch = !search ||
      a.name.toLowerCase().includes(search) ||
      a.company.toLowerCase().includes(search) ||
      a.jobTitle.toLowerCase().includes(search) ||
      a.email.toLowerCase().includes(search);

    const matchTag = !tag ||
      a.expertiseTags.some(t => t.name.toLowerCase() === tag);

    return matchSearch && matchTag;
  });

  renderAlumni(filtered);
}

function renderAlumni(list) {
  const container = document.getElementById('alumni-grid');
  if (!list || list.length === 0) {
    container.innerHTML = '<div class="empty-state" style="grid-column: 1 / -1;"><div class="empty-icon">👨‍💼</div><div class="empty-title">No Alumni Found</div><div class="empty-desc">Try clearing your search or register a new alumni mentor.</div></div>';
    return;
  }

  container.innerHTML = list.map(a => {
    const isFull = a.availableSlots <= 0;
    const usedPct = ((a.activeMenteeCount / a.maxConcurrentMentees) * 100).toFixed(0);
    const slotClass = a.availableSlots > 1 ? 'available' : (a.availableSlots === 1 ? 'warning' : 'full');

    return `
      <div class="entity-card">
        <div>
          <div class="entity-header">
            <div>
              <div class="entity-name">${escapeHtml(a.name)}</div>
              <div class="entity-subtitle">${escapeHtml(a.jobTitle)} @ <strong>${escapeHtml(a.company)}</strong></div>
            </div>
            <span class="badge ${isFull ? 'badge-cancelled' : 'badge-active'}">
              ${isFull ? 'FULL' : `${a.availableSlots} SLOTS`}
            </span>
          </div>

          <div style="font-size: 0.825rem; color: var(--text-muted); margin-bottom: 0.5rem;">
            Graduated: ${a.graduationYear} &bull; ${escapeHtml(a.email)}
          </div>

          ${a.bio ? `<div style="font-size: 0.85rem; color: #475569; margin-bottom: 0.75rem; font-style: italic;">"${escapeHtml(a.bio)}"</div>` : ''}

          <!-- Capacity Bar -->
          <div class="capacity-box">
            <div class="capacity-header">
              <span>Active Mentees</span>
              <span><strong>${a.activeMenteeCount}</strong> / ${a.maxConcurrentMentees} max</span>
            </div>
            <div class="capacity-bar-bg">
              <div class="capacity-bar-fill ${slotClass}" style="width: ${usedPct}%;"></div>
            </div>
          </div>

          <!-- Expertise Tags -->
          <div class="tag-list">
            ${a.expertiseTags.map(t => `<span class="tag-badge">${escapeHtml(t.name)}</span>`).join('')}
          </div>
        </div>

        <div style="margin-top: 1rem; border-top: 1px solid var(--border); padding-top: 0.75rem; display: flex; justify-content: flex-end; gap: 0.5rem;">
          <button class="btn btn-secondary btn-sm" onclick="switchTab('matcher')">Find Mentees</button>
        </div>
      </div>
    `;
  }).join('');
}

function openRegisterAlumniModal() {
  selectedAlumniTags.clear();
  renderTagSelectors();
  document.getElementById('form-register-alumni').reset();
  document.getElementById('modal-register-alumni').classList.add('active');
}

async function handleRegisterAlumni(e) {
  e.preventDefault();
  if (selectedAlumniTags.size === 0) {
    showToast('Select Tags', 'Please select at least one expertise tag', 'warning');
    return;
  }

  const payload = {
    name: document.getElementById('alumni-name').value.trim(),
    email: document.getElementById('alumni-email').value.trim(),
    company: document.getElementById('alumni-company').value.trim(),
    jobTitle: document.getElementById('alumni-job-title').value.trim(),
    graduationYear: parseInt(document.getElementById('alumni-grad-year').value),
    maxConcurrentMentees: parseInt(document.getElementById('alumni-max-slots').value),
    bio: document.getElementById('alumni-bio').value.trim(),
    expertiseTags: Array.from(selectedAlumniTags)
  };

  try {
    await apiCall(API.alumni, 'POST', payload);
    showToast('Success', 'Alumni registered successfully', 'success');
    closeModal('modal-register-alumni');
    fetchAlumni();
    loadDashboard();
  } catch (err) {
    showToast('Registration Error', err.message, 'error');
  }
}

// -------------------------------------------------------------
// Students Directory
// -------------------------------------------------------------
async function fetchStudents() {
  const container = document.getElementById('student-grid');
  container.innerHTML = '<div style="grid-column: 1 / -1; text-align: center; padding: 2rem;">Loading students...</div>';
  try {
    allStudents = await apiCall(API.students);
    renderStudents(allStudents);
  } catch (err) {
    container.innerHTML = `<div class="empty-state" style="grid-column: 1 / -1;"><div class="empty-title">Error</div><div class="empty-desc">${escapeHtml(err.message)}</div></div>`;
  }
}

function filterStudents() {
  const search = document.getElementById('student-search').value.toLowerCase().trim();
  const filtered = allStudents.filter(s => {
    return !search ||
      s.name.toLowerCase().includes(search) ||
      s.department.toLowerCase().includes(search) ||
      (s.rollNumber && s.rollNumber.toLowerCase().includes(search)) ||
      s.email.toLowerCase().includes(search);
  });
  renderStudents(filtered);
}

function renderStudents(list) {
  const container = document.getElementById('student-grid');
  if (!list || list.length === 0) {
    container.innerHTML = '<div class="empty-state" style="grid-column: 1 / -1;"><div class="empty-icon">👩‍🎓</div><div class="empty-title">No Students Found</div></div>';
    return;
  }

  container.innerHTML = list.map(s => `
    <div class="entity-card">
      <div>
        <div class="entity-header">
          <div>
            <div class="entity-name">${escapeHtml(s.name)}</div>
            <div class="entity-subtitle">${escapeHtml(s.department)} &bull; Batch ${s.batchYear}</div>
          </div>
          <span class="badge ${s.activeMentorshipCount > 0 ? 'badge-active' : 'badge-completed'}">
            ${s.activeMentorshipCount > 0 ? `${s.activeMentorshipCount} Active Mentor` : 'Seeking Mentor'}
          </span>
        </div>

        <div style="font-size: 0.825rem; color: var(--text-muted); margin-bottom: 0.5rem;">
          Roll: ${escapeHtml(s.rollNumber || 'N/A')} &bull; ${escapeHtml(s.email)}
        </div>

        <div style="font-size: 0.8rem; font-weight: 600; color: var(--text-muted); margin-top: 0.5rem;">Target Learning Interests:</div>
        <div class="tag-list">
          ${s.interestTags.map(t => `<span class="tag-badge" style="background:#e0f2fe; color:#0369a1;">${escapeHtml(t.name)}</span>`).join('')}
        </div>
      </div>

      <div style="margin-top: 1rem; border-top: 1px solid var(--border); padding-top: 0.75rem; display: flex; justify-content: flex-end; gap: 0.5rem;">
        <button class="btn btn-primary btn-sm" onclick="matchDirectForStudent(${s.id})">🎯 Find Best Mentors</button>
      </div>
    </div>
  `).join('');
}

function matchDirectForStudent(studentId) {
  switchTab('matcher');
  const select = document.getElementById('matcher-student-select');
  select.value = studentId;
  loadStudentSuggestions();
}

function openRegisterStudentModal() {
  selectedStudentTags.clear();
  renderTagSelectors();
  document.getElementById('form-register-student').reset();
  document.getElementById('modal-register-student').classList.add('active');
}

async function handleRegisterStudent(e) {
  e.preventDefault();
  if (selectedStudentTags.size === 0) {
    showToast('Select Tags', 'Please select at least one learning interest tag', 'warning');
    return;
  }

  const payload = {
    name: document.getElementById('student-name').value.trim(),
    email: document.getElementById('student-email').value.trim(),
    department: document.getElementById('student-dept').value.trim(),
    batchYear: parseInt(document.getElementById('student-batch').value),
    rollNumber: document.getElementById('student-roll').value.trim(),
    interestTags: Array.from(selectedStudentTags)
  };

  try {
    await apiCall(API.students, 'POST', payload);
    showToast('Success', 'Student registered successfully', 'success');
    closeModal('modal-register-student');
    fetchStudents();
    loadDashboard();
  } catch (err) {
    showToast('Registration Error', err.message, 'error');
  }
}

// -------------------------------------------------------------
// Mentorship Pairs
// -------------------------------------------------------------
async function fetchPairs() {
  const status = document.getElementById('pair-status-filter').value;
  let url = API.pairs;
  if (status) url += `?status=${status}`;

  try {
    allPairs = await apiCall(url);
    renderPairs(allPairs);
  } catch (err) {
    showToast('Error', err.message, 'error');
  }
}

function renderPairs(list) {
  const tbody = document.getElementById('pairs-table-body');
  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">No mentorship pairs found.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(p => `
    <tr>
      <td><strong>#${p.id}</strong></td>
      <td>
        <div style="font-weight: 600;">${escapeHtml(p.alumniName)}</div>
        <div style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(p.alumniCompany)} &bull; ${escapeHtml(p.alumniEmail)}</div>
      </td>
      <td>
        <div style="font-weight: 600;">${escapeHtml(p.studentName)}</div>
        <div style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(p.studentDepartment)}</div>
      </td>
      <td>
        <span class="badge badge-${p.status.toLowerCase()}">${p.status}</span>
      </td>
      <td>${p.startDate || 'N/A'}</td>
      <td>
        <span style="font-weight: 600;">${p.completedSessions}</span> / ${p.totalSessions} completed
      </td>
      <td style="max-width: 200px; font-size: 0.85rem; color: var(--text-muted);">${escapeHtml(p.notes || '-')}</td>
      <td>
        <div style="display: flex; gap: 0.35rem; flex-wrap: wrap;">
          ${p.status === 'ACTIVE' ? `
            <button class="btn btn-primary btn-sm" onclick="openScheduleSessionModal(${p.id})">+ Session</button>
            <button class="btn btn-secondary btn-sm" onclick="changePairStatus(${p.id}, 'COMPLETED')">Complete</button>
            <button class="btn btn-danger btn-sm" onclick="changePairStatus(${p.id}, 'CANCELLED')">Cancel</button>
          ` : `
            <button class="btn btn-secondary btn-sm" onclick="changePairStatus(${p.id}, 'ACTIVE')">Re-activate</button>
          `}
        </div>
      </td>
    </tr>
  `).join('');
}

async function changePairStatus(pairId, newStatus) {
  if (!confirm(`Are you sure you want to change status of pair #${pairId} to ${newStatus}?`)) return;

  try {
    await apiCall(`${API.pairs}/${pairId}/status?status=${newStatus}`, 'PUT');
    showToast('Pair Updated', `Pair #${pairId} is now ${newStatus}`, 'success');
    fetchPairs();
    loadDashboard();
  } catch (err) {
    showToast('Operation Failed', err.message, 'error');
  }
}

// -------------------------------------------------------------
// Sessions Tracker (Core Feature 4)
// -------------------------------------------------------------
async function fetchSessions() {
  const status = document.getElementById('session-status-filter').value;
  let url = API.sessions;
  if (status) url += `?status=${status}`;

  try {
    const sessions = await apiCall(url);
    renderSessions(sessions);
  } catch (err) {
    showToast('Error', err.message, 'error');
  }
}

function renderSessions(list) {
  const tbody = document.getElementById('sessions-table-body');
  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">No mentoring sessions found.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(s => {
    const formattedDate = new Date(s.sessionDate).toLocaleString();
    return `
      <tr>
        <td><strong>#${s.id}</strong></td>
        <td>
          <div style="font-weight: 600;">Mentor: ${escapeHtml(s.alumniName)}</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">Student: ${escapeHtml(s.studentName)}</div>
        </td>
        <td>${formattedDate}</td>
        <td><strong>${s.durationMinutes}</strong> mins</td>
        <td style="font-weight: 600;">${escapeHtml(s.topic)}</td>
        <td>
          <span class="badge badge-${s.status.toLowerCase()}">${s.status}</span>
        </td>
        <td style="max-width: 250px;">
          ${s.meetingLink ? `<a href="${escapeHtml(s.meetingLink)}" target="_blank" style="color: var(--primary); text-decoration: underline; font-size: 0.8rem;">Join Video Call</a><br>` : ''}
          ${s.feedbackNotes ? `<span style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(s.feedbackNotes)}</span>` : '<span style="font-size: 0.75rem; color: var(--text-light);">(No feedback yet)</span>'}
        </td>
        <td>
          ${s.status === 'SCHEDULED' ? `
            <button class="btn btn-success btn-sm" onclick="openCompleteSessionModal(${s.id}, '${escapeHtml(s.topic)}')">Complete</button>
            <button class="btn btn-danger btn-sm" onclick="cancelSession(${s.id})">Cancel</button>
          ` : `
            <span style="font-size: 0.8rem; color: var(--text-muted); font-weight: 600;">Finished</span>
          `}
        </td>
      </tr>
    `;
  }).join('');
}

async function openScheduleSessionModal(preselectedPairId = null) {
  const select = document.getElementById('session-pair-select');
  select.innerHTML = '<option value="">Loading active pairs...</option>';

  document.getElementById('modal-schedule-session').classList.add('active');

  try {
    const activePairs = await apiCall(`${API.pairs}?status=ACTIVE`);
    if (activePairs.length === 0) {
      select.innerHTML = '<option value="">No ACTIVE pairs available. Create a match first!</option>';
      return;
    }

    select.innerHTML = activePairs.map(p => `
      <option value="${p.id}" ${preselectedPairId && p.id === preselectedPairId ? 'selected' : ''}>
        Pair #${p.id} &bull; Mentor: ${escapeHtml(p.alumniName)} ⟷ Student: ${escapeHtml(p.studentName)}
      </option>
    `).join('');
  } catch (err) {
    showToast('Failed to load pairs', err.message, 'error');
  }
}

async function handleScheduleSession(e) {
  e.preventDefault();
  const pairId = document.getElementById('session-pair-select').value;
  if (!pairId) {
    showToast('Select Pair', 'Please select an active mentorship pair', 'warning');
    return;
  }

  const payload = {
    mentorshipPairId: Number(pairId),
    sessionDate: document.getElementById('session-date').value,
    durationMinutes: parseInt(document.getElementById('session-duration').value),
    topic: document.getElementById('session-topic').value.trim(),
    meetingLink: document.getElementById('session-meeting-link').value.trim()
  };

  try {
    await apiCall(API.sessions, 'POST', payload);
    showToast('Session Scheduled', 'Mentoring session added successfully', 'success');
    closeModal('modal-schedule-session');
    fetchSessions();
    loadDashboard();
  } catch (err) {
    // Shows exact backend rejection rule
    showToast('Scheduling Rejected', err.message, 'error');
  }
}

function openCompleteSessionModal(id, topic) {
  document.getElementById('complete-session-id').value = id;
  document.getElementById('complete-session-info').textContent = `Topic: "${topic}"`;
  document.getElementById('complete-session-notes').value = '';
  document.getElementById('modal-complete-session').classList.add('active');
}

async function handleCompleteSession(e) {
  e.preventDefault();
  const id = document.getElementById('complete-session-id').value;
  const notes = document.getElementById('complete-session-notes').value.trim();

  try {
    const url = `${API.sessions}/${id}/status?status=COMPLETED&feedbackNotes=${encodeURIComponent(notes)}`;
    await apiCall(url, 'PUT');
    showToast('Session Completed', 'Recorded discussion feedback and updated status to COMPLETED.', 'success');
    closeModal('modal-complete-session');
    fetchSessions();
    loadDashboard();
  } catch (err) {
    showToast('Error', err.message, 'error');
  }
}

async function cancelSession(id) {
  if (!confirm(`Cancel session #${id}?`)) return;
  try {
    await apiCall(`${API.sessions}/${id}/status?status=CANCELLED`, 'PUT');
    showToast('Session Cancelled', `Session #${id} marked as CANCELLED`, 'info');
    fetchSessions();
    loadDashboard();
  } catch (err) {
    showToast('Error', err.message, 'error');
  }
}

// -------------------------------------------------------------
// Engagement Reports (Core Feature 5)
// -------------------------------------------------------------
async function fetchEngagementReport() {
  try {
    const report = await apiCall(`${API.reports}/engagement`);

    // Render report summary cards
    const summaryCards = document.getElementById('report-summary-cards');
    summaryCards.innerHTML = `
      <div class="stat-card">
        <div class="stat-icon blue">🤝</div>
        <div class="stat-info">
          <div class="stat-value">${report.totalPairs}</div>
          <div class="stat-label">Total Mentorship Pairs</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">🟢</div>
        <div class="stat-info">
          <div class="stat-value">${report.activePairs}</div>
          <div class="stat-label">Active Pairs</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon amber">✅</div>
        <div class="stat-info">
          <div class="stat-value">${report.completedSessions}</div>
          <div class="stat-label">Completed Sessions</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">⏱️</div>
        <div class="stat-info">
          <div class="stat-value">${report.totalCompletedHours.toFixed(1)}</div>
          <div class="stat-label">Total Mentoring Hours</div>
        </div>
      </div>
    `;

    // Render pair-by-pair table
    const tbody = document.getElementById('report-table-body');
    if (!report.pairReports || report.pairReports.length === 0) {
      tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 2rem;">No pair reports recorded.</td></tr>';
      return;
    }

    tbody.innerHTML = report.pairReports.map(pr => {
      const completionRate = pr.totalSessionsScheduled > 0
        ? Math.round((pr.completedSessions / pr.totalSessionsScheduled) * 100)
        : 0;

      return `
        <tr>
          <td><strong>Pair #${pr.pairId}</strong></td>
          <td>
            <div style="font-weight: 600;">${escapeHtml(pr.alumniName)}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(pr.alumniCompany)} &bull; ${escapeHtml(pr.alumniEmail)}</div>
          </td>
          <td>
            <div style="font-weight: 600;">${escapeHtml(pr.studentName)}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">${escapeHtml(pr.studentDepartment)}</div>
          </td>
          <td>
            <span class="badge badge-${pr.status.toLowerCase()}">${pr.status}</span>
          </td>
          <td style="font-size: 0.8rem; color: var(--text-muted);">
            Started: ${pr.startDate || 'N/A'}<br>
            ${pr.endDate ? `Ended: ${pr.endDate}` : 'Ongoing'}
          </td>
          <td style="text-align: center; font-weight: 600;">${pr.totalSessionsScheduled}</td>
          <td style="text-align: center; font-weight: 700; color: var(--success);">${pr.completedSessions} (${completionRate}%)</td>
          <td style="text-align: center; color: var(--danger);">${pr.cancelledSessions}</td>
          <td>
            <div style="font-weight: 700; color: #4338ca;">${pr.totalCompletedHours.toFixed(1)} hrs</div>
            <div style="font-size: 0.75rem; color: var(--text-muted);">(${pr.totalCompletedMinutes} mins)</div>
          </td>
        </tr>
      `;
    }).join('');

  } catch (err) {
    showToast('Report Error', err.message, 'error');
  }
}

// -------------------------------------------------------------
// Audit Logs (Specification Requirement)
// -------------------------------------------------------------
async function fetchAuditLogs() {
  try {
    allAuditLogs = await apiCall(API.audit);
    renderAuditLogs(allAuditLogs);
  } catch (err) {
    showToast('Failed to load audit logs', err.message, 'error');
  }
}

function renderAuditLogs(logs) {
  const tbody = document.getElementById('audit-table-body');
  if (!tbody) return;

  if (!logs || logs.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-muted);">No audit entries found.</td></tr>';
    return;
  }

  tbody.innerHTML = logs.map(log => {
    let badgeClass = 'badge-secondary';
    if (log.action.includes('REGISTERED')) badgeClass = 'badge-info';
    else if (log.action.includes('CREATED')) badgeClass = 'badge-success';
    else if (log.action.includes('COMPLETED')) badgeClass = 'badge-completed';
    else if (log.action.includes('CANCELLED')) badgeClass = 'badge-cancelled';
    else if (log.action.includes('SCHEDULED')) badgeClass = 'badge-scheduled';
    else if (log.action.includes('STATUS')) badgeClass = 'badge-warning';

    const formattedTime = log.timestamp ? new Date(log.timestamp).toLocaleString() : 'N/A';

    return `
      <tr>
        <td><strong>#${log.id}</strong></td>
        <td><span class="badge ${badgeClass}">${escapeHtml(log.action)}</span></td>
        <td><strong>${escapeHtml(log.entityType)}</strong></td>
        <td>${log.entityId !== null && log.entityId !== undefined ? `#${log.entityId}` : '—'}</td>
        <td style="font-size: 0.8rem; color: var(--text-muted); white-space: nowrap;">${formattedTime}</td>
        <td style="font-size: 0.875rem;">${escapeHtml(log.details)}</td>
      </tr>
    `;
  }).join('');
}

function filterAuditLogs() {
  const search = (document.getElementById('audit-search')?.value || '').toLowerCase().trim();
  const actionFilter = document.getElementById('audit-action-filter')?.value || '';

  const filtered = allAuditLogs.filter(log => {
    const matchesAction = !actionFilter || log.action === actionFilter;
    const matchesSearch = !search ||
      log.action.toLowerCase().includes(search) ||
      (log.entityType && log.entityType.toLowerCase().includes(search)) ||
      (log.details && log.details.toLowerCase().includes(search)) ||
      (log.entityId && String(log.entityId).includes(search));
    return matchesAction && matchesSearch;
  });

  renderAuditLogs(filtered);
}

// -------------------------------------------------------------
// Modal Utilities
// -------------------------------------------------------------
function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('active');
}

window.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.remove('active');
  }
});
