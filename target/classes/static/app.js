const state = {
  events: [],
  filteredEvents: [],
  credentials: sessionStorage.getItem('symposiumCredentials') || '',
  user: sessionStorage.getItem('symposiumUser') || '',
  role: sessionStorage.getItem('symposiumRole') || 'PARTICIPANT'
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#039;'
}[char]));

function showToast(message, type = 'success') {
  const toast = $('#toast');
  if (!toast) return;
  toast.textContent = message;
  toast.className = `toast visible ${type}`;
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.className = 'toast', 2600);
}

async function api(path, options = {}) {
  const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.headers || {}) };
  if (state.credentials) headers.Authorization = `Basic ${state.credentials}`;

  const response = await fetch(path, { ...options, headers });
  if (!response.ok) {
    const body = await response.text();
    let details;
    try {
      details = body ? JSON.parse(body) : null;
    } catch {
      details = null;
    }
    if (response.status === 401 || response.status === 403) {
      sessionStorage.clear();
      if (window.location.pathname !== '/login.html') {
        window.location.href = '/login.html';
      }
      return null;
    }
    const error = new Error(details?.message || details?.detail || body || `${response.status} ${response.statusText}`);
    error.code = details?.code;
    throw error;
  }

  return response.status === 204 ? null : response.json();
}

async function loadCurrentUser() {
  if (!state.credentials) {
    if (window.location.pathname.endsWith('/dashboard.html')) {
      window.location.href = '/login.html';
    }
    return null;
  }

  const user = await api('/api/me');
  if (!user) {
    return null;
  }

  state.user = user.username || state.user;
  state.role = user.role || state.role;
  sessionStorage.setItem('symposiumUser', state.user);
  sessionStorage.setItem('symposiumRole', state.role);
  return user;
}

function formatRoleLabel(role) {
  return String(role || 'PARTICIPANT').toLowerCase().replace(/(^|_)([a-z])/g, (_, sep, ch) => sep + ch.toUpperCase());
}

function escapeRoleBadge(role) {
  return role === 'ADMIN' ? 'Admin' : role === 'ORGANIZER' ? 'Organizer' : 'Participant';
}

function formatDate(dateString) {
  if (!dateString) return 'Date TBD';
  return new Date(`${dateString}T00:00:00`).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });
}

function currency(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0
  }).format(amount);
}

function populateSelects(events) {
  const registrationEvents = events.filter(event => event.registrationOpen);
  const registrationOptions = registrationEvents.length
    ? registrationEvents.map(event => `<option value="${event.id}">${escapeHtml(event.title)}</option>`).join('')
    : '<option value="">No events accepting registrations</option>';
  const submissionOptions = events.length
    ? events.map(event => `<option value="${event.id}">${escapeHtml(event.title)}</option>`).join('')
    : '<option value="">No events available</option>';
  const registration = $('#registrationEvent');
  const submission = $('#submissionEvent');

  if (registration) {
    registration.innerHTML = registrationOptions;
    registration.disabled = registrationEvents.length === 0;
  }
  if (submission) submission.innerHTML = submissionOptions;
  updateRegistrationAvailability();
}

function registrationLabel(event) {
  if (event.registrationOpen) return 'Register Now';
  switch (event.registrationStatus) {
    case 'EVENT_FULL': return 'Event Full';
    case 'EVENT_ENDED': return 'Event Ended';
    case 'CANCELLED': return 'Cancelled';
    case 'REGISTRATION_DEADLINE_PASSED': return 'Registration Closed';
    default: return 'Registration Closed';
  }
}

function updateRegistrationAvailability() {
  const select = $('#registrationEvent');
  const message = $('#registrationAvailability');
  const submit = $('#registrationForm button[type="submit"]');
  if (!select) return;

  const selected = state.events.find(item => String(item.id) === select.value);
  if (message) {
    message.textContent = selected
      ? selected.registrationMessage
      : 'No events are currently accepting registrations.';
  }
  if (submit) submit.disabled = !selected?.registrationOpen;
}

function renderFeaturedEvent(events) {
  const featured = events[0];
  if (!featured) return;

  const title = $('#featuredTitle');
  if (title) title.textContent = featured.title;
  const date = $('#featuredDate');
  if (date) date.textContent = `📅 ${formatDate(featured.startDate)}`;
  const venue = $('#featuredVenue');
  if (venue) venue.textContent = `📍 ${featured.venue || 'Venue TBD'}`;

  const registrationStatus = $('#featuredRegistrationStatus');
  if (registrationStatus) {
    registrationStatus.textContent = featured.registrationOpen ? 'Open' : registrationLabel(featured);
    registrationStatus.classList.toggle('closed', !featured.registrationOpen);
  }

  const capacity = Number(featured.maxParticipants || 0);
  const capacityLabel = $('#featuredCapacity');
  if (capacityLabel) {
    capacityLabel.textContent = capacity
      ? `${featured.availableSeats} available / ${capacity} total`
      : 'Not specified';
  }
  const capacityBar = $('#featuredCapacityBar');
  if (capacityBar) {
    const occupiedPercent = capacity
      ? Math.min(100, (Number(featured.registeredCount || 0) / capacity) * 100)
      : 0;
    capacityBar.style.width = `${occupiedPercent}%`;
  }

  const registerButton = $('#featuredRegisterButton');
  if (registerButton) {
    registerButton.dataset.openRegister = String(featured.id);
    registerButton.disabled = !featured.registrationOpen;
    registerButton.textContent = featured.registrationOpen ? 'Reserve Your Spot' : registrationLabel(featured);
  }
}

function renderEvents() {
  const list = $('#eventList');
  if (!list) return;

  if (!state.filteredEvents.length) {
    list.innerHTML = '<div class="empty-state">No events match your search yet.</div>';
    return;
  }

  list.innerHTML = state.filteredEvents.map((event) => `
    <article class="event-card">
      <div class="event-image-wrap">
        <img src="${event.imageUrl || 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80'}" alt="${escapeHtml(event.title)}" />
      </div>
      <div class="event-content">
        <div class="event-topline">
          <span class="pill">${escapeHtml(event.category || 'General')}</span>
          <span class="event-status">${escapeHtml(event.status || 'Published')}</span>
          <span class="event-registration-status">${escapeHtml(registrationLabel(event))}</span>
        </div>
        <h3>${escapeHtml(event.title)}</h3>
        <p>${escapeHtml(event.description || 'A professional symposium designed for future-ready learning and collaboration.')}</p>
        <div class="meta-grid">
          <span>📅 ${formatDate(event.startDate)}</span>
          <span>🕒 ${event.startDate ? '09:00 AM' : 'TBD'}</span>
          <span>📍 ${escapeHtml(event.venue || 'TBA')}</span>
          <span>👥 ${escapeHtml(event.organizerName || 'Symposium Team')}</span>
        </div>
        <div class="event-footer">
          <div class="price-block">
            <span>Registration fee</span>
            <strong>${currency(event.registrationFee)}</strong>
          </div>
          <div class="seat-block">
            <span>Available seats</span>
            <strong>${event.availableSeats ?? 'Not limited'}</strong>
            <span>Total capacity</span>
            <strong>${event.maxParticipants > 0 ? event.maxParticipants : 'Not limited'}</strong>
          </div>
        </div>
        <div class="card-actions">
          <button class="btn btn-ghost" type="button" data-event-detail="${event.id}">View Details</button>
          <button class="btn btn-primary" type="button" data-open-register="${event.id}" ${event.registrationOpen ? '' : 'disabled'}>${escapeHtml(registrationLabel(event))}</button>
        </div>
      </div>
    </article>
  `).join('');
}

function applyEventFilters() {
  const search = $('#eventSearch')?.value?.toLowerCase() || '';
  const category = $('#eventCategoryFilter')?.value || 'all';
  const sort = $('#eventSort')?.value || 'newest';

  const filtered = [...state.events].filter((event) => {
    const matchesSearch = !search || [event.title, event.category, event.venue, event.organizerName, event.tracks].join(' ').toLowerCase().includes(search);
    const matchesCategory = category === 'all' || event.category === category;
    return matchesSearch && matchesCategory;
  });

  filtered.sort((a, b) => {
    if (sort === 'price-low') return Number(a.registrationFee || 0) - Number(b.registrationFee || 0);
    if (sort === 'price-high') return Number(b.registrationFee || 0) - Number(a.registrationFee || 0);
    return new Date(b.startDate || 0) - new Date(a.startDate || 0);
  });

  state.filteredEvents = filtered;
  renderEvents();
}

async function loadEvents() {
  try {
    const response = await api('/api/events?status=PUBLISHED');
    state.events = response || [];
    state.filteredEvents = [...state.events];
    renderEvents();
    populateSelects(state.events);
    renderFeaturedEvent(state.events);
    updateDashboardStats();
  } catch (error) {
    const list = $('#eventList');
    if (list) {
      list.innerHTML = '<div class="empty-state">Unable to load events. Please try again later.</div>';
    }
  }
}

function openModal(id) {
  const modal = document.getElementById(id);
  if (!modal) return;
  modal.classList.add('open');
  modal.setAttribute('aria-hidden', 'false');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (!modal) return;
  modal.classList.remove('open');
  modal.setAttribute('aria-hidden', 'true');
}

function updateDashboardStats() {
  const categories = new Set(state.events.map(event => event.category).filter(Boolean));
  const onlineEvents = state.events.filter(event => event.online).length;
  const totalCapacity = state.events.reduce((total, event) => total + Number(event.maxParticipants || 0), 0);
  const upcomingEvents = state.events.filter(event => event.registrationStatus !== 'EVENT_ENDED');
  const metrics = {
    statUpcoming: upcomingEvents.length,
    statCategories: categories.size,
    statOnline: onlineEvents,
    statCapacity: totalCapacity
  };

  for (const [id, value] of Object.entries(metrics)) {
    const element = document.getElementById(id);
    if (element) element.textContent = String(value);
  }

  const heroMetrics = {
    heroEventCount: state.events.length,
    heroCategoryCount: categories.size,
    heroOnlineCount: onlineEvents
  };
  for (const [id, value] of Object.entries(heroMetrics)) {
    const element = document.getElementById(id);
    if (element) element.textContent = String(value);
  }
}

function renderEventDetails(event) {
  const details = $('#eventDetailContent');
  if (!details) return;

  details.innerHTML = `
    <div class="event-detail-banner" style="background-image: url('${escapeHtml(event.imageUrl || 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80')}');"></div>
    <div class="detail-copy">
      <span class="eyebrow">${escapeHtml(event.category || 'Conference')}</span>
      <h2>${escapeHtml(event.title)}</h2>
      <p>${escapeHtml(event.description || 'Event description coming soon.')}</p>

      <div class="detail-grid">
        <span>📅 ${formatDate(event.startDate)}</span>
        <span>⏰ 09:00 AM</span>
        <span>📍 ${escapeHtml(event.venue || 'TBA')}</span>
        <span>🧑‍🏫 ${escapeHtml(event.organizerName || 'Symposium Team')}</span>
        <span>💸 ${currency(event.registrationFee)}</span>
        <span>📋 Status: ${escapeHtml(registrationLabel(event))}</span>
        <span>⏳ Registration deadline: ${formatDate(event.registrationDeadline)}</span>
        <span>👥 Available seats: ${event.availableSeats ?? 'Not limited'} / ${event.maxParticipants > 0 ? event.maxParticipants : 'Not limited'} total</span>
        <span>🎯 ${escapeHtml(event.tracks || 'AI, Research, Innovation')}</span>
      </div>
      <p class="registration-message">${escapeHtml(event.registrationMessage || '')}</p>

      <div class="detail-actions">
        <button class="btn btn-primary" type="button" data-open-register="${event.id}" ${event.registrationOpen ? '' : 'disabled'}>${escapeHtml(registrationLabel(event))}</button>
        <button class="btn btn-ghost" type="button" data-download-brochure="${event.id}">Download Event Details</button>
      </div>
    </div>
  `;
}

async function handleRegistrationSubmit(event) {
  event.preventDefault();
  const formData = Object.fromEntries(new FormData(event.target).entries());
  const conferenceId = formData.conferenceId;

  if (!conferenceId) {
    showToast('Please choose an event to register.', 'error');
    return;
  }

  try {
    const currentEvent = await api(`/api/events/${conferenceId}`);
    if (!currentEvent?.registrationOpen) {
      showToast(currentEvent?.registrationMessage || 'Registration is closed for this event.', 'error');
      await loadEvents();
      return;
    }

    await api(`/api/registrations?conferenceId=${conferenceId}`, {
      method: 'POST',
      body: JSON.stringify({
        fullName: formData.fullName,
        email: formData.email,
        phone: formData.phone,
        college: formData.college,
        department: formData.department,
        participantType: formData.participantType,
        status: 'PENDING',
        registrationId: `REG-${Date.now()}`
      })
    });

    showToast('Registration successful.');
    event.target.reset();
    closeModal('registerModal');
    await loadEvents();
  } catch (error) {
    showToast(error.message || 'Registration failed.', 'error');
    if (['EVENT_FULL', 'EVENT_ENDED', 'REGISTRATION_CLOSED', 'REGISTRATION_DEADLINE_PASSED', 'CANCELLED'].includes(error.code)) {
      await loadEvents();
    }
  }
}

async function handlePaperSubmit(event) {
  event.preventDefault();
  const formData = Object.fromEntries(new FormData(event.target).entries());
  const conferenceId = formData.conferenceId;

  try {
    await api(`/api/submissions?conferenceId=${conferenceId}`, {
      method: 'POST',
      body: JSON.stringify({
        title: formData.title,
        authorName: formData.authorName,
        authorEmail: 'author@example.com',
        track: formData.track,
        status: 'SUBMITTED'
      })
    });

    showToast('Paper submitted successfully.');
    event.target.reset();
    closeModal('paperModal');
  } catch (error) {
    showToast(error.message || 'Paper submission failed.', 'error');
  }
}

document.addEventListener('click', async (event) => {
  if (event.target.closest('[data-open-paper]')) {
    openModal('paperModal');
    return;
  }

  const openRegister = event.target.closest('[data-open-register]');
  if (openRegister) {
    const id = openRegister.dataset.openRegister;
    const eventId = id === 'default'
      ? state.events.find(item => item.registrationOpen)?.id
      : Number(id);
    if (!eventId) {
      const reason = state.events[0]?.registrationMessage || 'No events are currently accepting registrations.';
      showToast(reason, 'error');
      return;
    }

    const selectedEvent = state.events.find(item => String(item.id) === String(eventId));
    if (!selectedEvent?.registrationOpen) {
      showToast(selectedEvent?.registrationMessage || 'Registration is closed for this event.', 'error');
      return;
    }

    const select = $('#registrationEvent');
    if (select) select.value = String(eventId);
    updateRegistrationAvailability();
    openModal('registerModal');
    return;
  }

  const detailButton = event.target.closest('[data-event-detail]');
  if (detailButton) {
    const eventItem = state.events.find((item) => String(item.id) === String(detailButton.dataset.eventDetail));
    if (eventItem) {
      renderEventDetails(eventItem);
      openModal('eventModal');
    }
    return;
  }

  const modalClose = event.target.closest('[data-close-modal]');
  if (modalClose) {
    closeModal(modalClose.dataset.closeModal);
    return;
  }

  const downloadButton = event.target.closest('[data-download-brochure]');
  if (downloadButton) {
    const eventItem = state.events.find(item => String(item.id) === downloadButton.dataset.downloadBrochure);
    if (eventItem) downloadEventDetails(eventItem);
    return;
  }

  if (event.target.closest('#announcementsButton')) {
    showToast('You are all caught up on announcements.');
  }
});

function downloadEventDetails(event) {
  const details = [
    event.title,
    '',
    `Category: ${event.category || 'General'}`,
    `Date: ${formatDate(event.startDate)}${event.endDate ? ` to ${formatDate(event.endDate)}` : ''}`,
    `Venue: ${event.venue || 'To be announced'}`,
    `Organizer: ${event.organizerName || 'Symposium Team'}`,
    `Registration fee: ${currency(event.registrationFee)}`,
    '',
    event.description || ''
  ].join('\n');
  const file = new Blob([details], { type: 'text/plain;charset=utf-8' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(file);
  link.download = `${event.title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}-details.txt`;
  link.click();
  URL.revokeObjectURL(link.href);
}

const searchInput = $('#eventSearch');
if (searchInput) {
  searchInput.addEventListener('input', applyEventFilters);
}

const categoryFilter = $('#eventCategoryFilter');
if (categoryFilter) {
  categoryFilter.addEventListener('change', applyEventFilters);
}

const sortSelect = $('#eventSort');
if (sortSelect) {
  sortSelect.addEventListener('change', applyEventFilters);
}

const registrationForm = $('#registrationForm');
if (registrationForm) registrationForm.addEventListener('submit', handleRegistrationSubmit);
const registrationEventSelect = $('#registrationEvent');
if (registrationEventSelect) registrationEventSelect.addEventListener('change', updateRegistrationAvailability);

const paperForm = $('#paperForm');
if (paperForm) paperForm.addEventListener('submit', handlePaperSubmit);

async function initializeDashboardApp() {
  const user = await loadCurrentUser();
  if (!user) return;

  const summary = await api('/api/dashboard');
  const events = await api('/api/events');
  const dashboardTitle = $('#dashboardTitle');
  const dashboardSubtitle = $('#dashboardSubtitle');
  const userBadge = $('#userBadge');

  if (dashboardTitle) {
    dashboardTitle.textContent = `${escapeRoleBadge(state.role)} workspace`;
  }
  if (dashboardSubtitle) {
    dashboardSubtitle.textContent = `Welcome back, ${state.user}.`;
  }
  if (userBadge) {
    userBadge.textContent = escapeRoleBadge(state.role);
  }

  const summaryValues = {
    totalEvents: summary?.conferences || 0,
    totalRegistrations: summary?.registrations || 0,
    totalPapers: summary?.papers || 0,
    acceptedPapers: summary?.acceptedPapers || 0
  };

  for (const [key, value] of Object.entries(summaryValues)) {
    const element = document.getElementById(key);
    if (element) element.textContent = String(value);
  }

  const eventList = $('#dashboardEventList');
  if (eventList) {
    const items = (events || []).slice(0, 6).map((event) => `
      <tr>
        <td>${escapeHtml(event.title)}</td>
        <td>${escapeHtml(event.category || 'General')}</td>
        <td>${escapeHtml(event.status || 'DRAFT')}</td>
        <td>${formatDate(event.startDate)}</td>
        <td>
          <button class="table-button" type="button" data-event-detail="${event.id}">View</button>
          <button class="table-button muted" type="button" data-dashboard-publish="${event.id}">Publish</button>
        </td>
      </tr>
    `).join('');
    eventList.innerHTML = items || '<tr><td colspan="5">No events available yet.</td></tr>';
  }

  const registrationList = $('#dashboardRegistrationList');
  if (registrationList) {
    const registrations = await api('/api/registrations/me');
    const rows = (registrations || []).slice(0, 6).map((item) => `
      <tr>
        <td>${escapeHtml(item.fullName || 'Participant')}</td>
        <td>${escapeHtml(item.email || 'n/a')}</td>
        <td>${escapeHtml(item.status || 'PENDING')}</td>
        <td>${escapeHtml(item.conference?.title || 'Conference')}</td>
      </tr>
    `).join('');
    registrationList.innerHTML = rows || '<tr><td colspan="4">No registrations yet.</td></tr>';
  }

  const createEventForm = $('#createEventForm');
  if (createEventForm) {
    createEventForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const formData = Object.fromEntries(new FormData(event.target).entries());
      try {
        await api('/api/events', {
          method: 'POST',
          body: JSON.stringify({
            title: formData.title,
            category: formData.category || 'General',
            organizerName: formData.organizerName || 'Symposium Team',
            department: formData.department || 'Technology',
            venue: formData.venue || 'TBA',
            description: formData.description || 'New symposium event.',
            startDate: formData.startDate,
            endDate: formData.endDate,
            registrationDeadline: formData.registrationDeadline,
            paperDeadline: formData.paperDeadline,
            maxParticipants: Number(formData.maxParticipants || 100),
            registrationFee: Number(formData.registrationFee || 0),
            status: 'DRAFT'
          })
        });
        showToast('Event created successfully.');
        event.target.reset();
        initializeDashboardApp();
      } catch (error) {
        showToast(error.message || 'Event creation failed.', 'error');
      }
    });
  }

  const publishButtons = $$('#dashboardEventList [data-dashboard-publish]');
  publishButtons.forEach((button) => {
    button.addEventListener('click', async () => {
      const eventId = button.dataset.dashboardPublish;
      const current = (events || []).find((item) => String(item.id) === String(eventId));
      if (!current) return;
      try {
        await api(`/api/events/${eventId}`, {
          method: 'PUT',
          body: JSON.stringify({ ...current, status: 'PUBLISHED' })
        });
        showToast('Event published.');
        initializeDashboardApp();
      } catch (error) {
        showToast(error.message || 'Unable to publish event.', 'error');
      }
    });
  });

  const detailButtons = $$('#dashboardEventList [data-event-detail]');
  detailButtons.forEach((button) => {
    button.addEventListener('click', () => {
      const eventId = button.dataset.eventDetail;
      const eventItem = (events || []).find((item) => String(item.id) === String(eventId));
      if (eventItem) {
        renderEventDetails(eventItem);
        openModal('eventModal');
      }
    });
  });

  const logoutButton = $('#logoutButton');
  if (logoutButton) {
    logoutButton.addEventListener('click', () => {
      sessionStorage.clear();
      window.location.href = '/login.html';
    });
  }
}

if (window.location.pathname.endsWith('/dashboard.html')) {
  initializeDashboardApp();
} else {
  loadEvents();
}
