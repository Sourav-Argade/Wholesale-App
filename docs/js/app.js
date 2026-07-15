/* ====================================
   Wholesale App - GitHub Pages Edition
   ====================================
   - Detects backend availability on load
   - Backend online  → full app (login → dashboard)
   - Backend offline → landing/preview page
   - Also checks remote Railway backend for cloud-deployed version
   ==================================== */

const API_BASE = '/api';
// Remote backend URL — update this after deploying to Railway/Render
// Format: 'https://your-app-name.railway.app'
const API_REMOTE_BASE = ''; // ← Set this after deployment!
let authToken = localStorage.getItem('authToken');
let currentUser = null;
let currentPage = 'dashboard';
let mapInstance = null;
let mapMarkers = [];
let backendOnline = false;
let usingRemoteBackend = false;

// ====== DOM References ======
const $ = (id) => document.getElementById(id);
const landingScreen = $('landingScreen');
const loginScreen = $('loginScreen');
const appScreen = $('appScreen');
const mainContent = $('mainContent');
const sideNav = $('sideNav');
const navOverlay = $('navOverlay');
const menuToggle = $('menuToggle');
const modalOverlay = $('modalOverlay');
const modalTitle = $('modalTitle');
const modalBody = $('modalBody');
const modalClose = $('modalClose');
const pageTitle = $('pageTitle');
const loginForm = $('loginForm');
const loginError = $('loginError');
const backendBanner = $('backendBanner');

// ====== Backend Detection ======
async function checkBackend() {
    const bannerIcon = $('bannerIcon');
    const bannerText = $('bannerText');
    const statusText = $('statusText');
    const statusBadge = document.querySelector('.status-badge');
    const retryBtn = document.querySelector('.landing-btn-retry');
    const setupSection = document.getElementById('setupGuide');

    // First, try the local backend (same origin - for when running locally)
    try {
        const res = await fetch(`${API_BASE}/customers`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            signal: AbortSignal.timeout(4000)
        });

        if (res.ok || res.status === 401) {
            // Backend is alive (401 means auth required, which is expected)
            backendOnline = true;
            usingRemoteBackend = false;
            if (backendBanner) {
                backendBanner.className = 'backend-banner online';
                backendBanner.style.display = 'flex';
                bannerIcon.textContent = '✅';
                bannerText.innerHTML = 'Local backend is running! <a href="#" onclick="showApp()">Go to App →</a>';
            }
            if (statusText) {
                statusText.textContent = '✅ Local Backend Connected';
            }
            if (statusBadge) {
                statusBadge.className = 'status-badge online';
                const dot = statusBadge.querySelector('.status-dot');
                if (dot) dot.className = 'status-dot online';
            }
            if (retryBtn) retryBtn.style.display = 'none';
            if (setupSection) setupSection.style.display = 'none';
            return true;
        }
    } catch (e) {
        // Local backend unavailable
    }

    // Then, try the remote backend (if configured - for Railway/Render deployment)
    if (API_REMOTE_BASE) {
        try {
            const res = await fetch(`${API_REMOTE_BASE}${API_BASE}/customers`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                signal: AbortSignal.timeout(4000)
            });

            if (res.ok || res.status === 401) {
                backendOnline = true;
                usingRemoteBackend = true;
                if (backendBanner) {
                    backendBanner.className = 'backend-banner online';
                    backendBanner.style.display = 'flex';
                    bannerIcon.textContent = '☁️';
                    bannerText.innerHTML = 'Cloud backend is live! <a href="#" onclick="showApp()">Go to App →</a>';
                }
                if (statusText) {
                    statusText.textContent = '☁️ Cloud Backend Connected';
                }
                if (statusBadge) {
                    statusBadge.className = 'status-badge online';
                    const dot = statusBadge.querySelector('.status-dot');
                    if (dot) dot.className = 'status-dot online';
                }
                if (retryBtn) retryBtn.style.display = 'none';
                if (setupSection) setupSection.style.display = 'block';
                return true;
            }
        } catch (e) {
            // Remote backend unavailable
        }
    }

    backendOnline = false;
    usingRemoteBackend = false;
    if (backendBanner) {
        backendBanner.className = 'backend-banner offline';
        backendBanner.style.display = 'flex';
        bannerIcon.textContent = '🔌';
        if (API_REMOTE_BASE) {
            bannerText.textContent = 'Backend unreachable. Run locally or check your cloud deployment.';
        } else {
            bannerText.textContent = 'Backend offline — showing app preview. Run the app locally to use all features.';
        }
    }
    if (statusText) {
        statusText.textContent = 'Backend Offline — Preview Mode';
    }
    if (retryBtn) retryBtn.style.display = 'inline-flex';
    if (setupSection) setupSection.style.display = 'block';
    return false;
}

function showApp() {
    landingScreen.classList.remove('active');
    loginScreen.classList.add('active', 'screen');
    appScreen.classList.remove('active');
    // Re-init the app
    initApp();
}

// ====== Toast ======
function showToast(msg, isError = false) {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();
    const toast = document.createElement('div');
    toast.className = `toast${isError ? ' toast-error' : ''}`;
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

// ====== Screen Management ======
function showScreen(screen) {
    landingScreen.classList.remove('active');
    loginScreen.classList.toggle('active', screen === 'login');
    appScreen.classList.toggle('active', screen === 'app');
    if (screen === 'login') {
        authToken = null;
        localStorage.removeItem('authToken');
    }
}

// ====== Navigation ======
if (menuToggle) {
    menuToggle.onclick = () => {
        sideNav.classList.add('open');
        navOverlay.classList.remove('hide');
    };
}

if (navOverlay) {
    navOverlay.onclick = () => {
        sideNav.classList.remove('open');
        navOverlay.classList.add('hide');
    };
}

// ====== Modal ======
if (modalClose) {
    modalClose.onclick = () => modalOverlay.classList.add('hide');
}
if (modalOverlay) {
    modalOverlay.onclick = (e) => {
        if (e.target === modalOverlay) modalOverlay.classList.add('hide');
    };
}

function showModal(title, bodyHtml) {
    if (!modalTitle || !modalBody) return;
    modalTitle.textContent = title;
    modalBody.innerHTML = bodyHtml;
    modalOverlay.classList.remove('hide');
}

function closeModal() {
    modalOverlay.classList.add('hide');
}

// ====== Page Routing ======
const navMenu = document.querySelector('.side-nav-menu');
if (navMenu) {
    navMenu.addEventListener('click', (e) => {
        const link = e.target.closest('a');
        if (!link) return;
        e.preventDefault();
        const page = link.dataset.page;
        navigateTo(page);
        sideNav.classList.remove('open');
        navOverlay.classList.add('hide');
    });
}

function navigateTo(page) {
    currentPage = page;
    document.querySelectorAll('.side-nav-menu a').forEach(a => {
        a.classList.toggle('nav-active', a.dataset.page === page);
    });
    const titles = {
        dashboard: '📊 Dashboard',
        customers: '👥 Customers',
        products: '📦 Products',
        routes: '🛣️ Routes',
        mapview: '🗺️ Map View',
        visits: '✅ Check-in',
        prices: '💰 Price History',
        excel: '📋 Backup / Excel'
    };
    if (pageTitle) pageTitle.textContent = titles[page] || 'Wholesale App';
    renderPage(page);
}

async function renderPage(page) {
    if (!mainContent) return;
    mainContent.innerHTML = '<div class="loading">Loading...</div>';
    switch (page) {
        case 'dashboard': await renderDashboard(); break;
        case 'customers': await renderCustomers(); break;
        case 'products': await renderProducts(); break;
        case 'routes': await renderRoutes(); break;
        case 'mapview': await renderMapView(); break;
        case 'visits': await renderVisits(); break;
        case 'prices': await renderPriceHistory(); break;
        case 'excel': await renderExcel(); break;
        default: renderDashboard();
    }
}

// ====== API Helper ======
function getApiBase() {
    return usingRemoteBackend ? API_REMOTE_BASE + API_BASE : API_BASE;
}

async function api(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    if (currentUser) headers['X-User-Id'] = currentUser.userId;

    const base = getApiBase();
    const res = await fetch(`${base}${path}`, { ...options, headers });
    if (res.status === 401) {
        authToken = null;
        localStorage.removeItem('authToken');
        showScreen('login');
        return null;
    }
    if (res.status === 204) return null;
    const text = await res.text();
    try { return JSON.parse(text); } catch { return text; }
}

// ====== Retry backend check from landing page ======
window.handleLoginClick = function() {
    if (backendOnline) {
        showApp();
    } else {
        showToast('Backend is offline. Start the app locally or deploy to the cloud to sign in.', true);
        document.getElementById('setupGuide')?.scrollIntoView({behavior: 'smooth'});
    }
};

window.retryBackendCheck = async function() {
    const btn = document.querySelector('.landing-btn-retry');
    if (btn) {
        btn.disabled = true;
        btn.textContent = '⏳ Checking...';
    }
    const result = await checkBackend();
    if (result) {
        showApp();
    } else {
        showToast('Backend still unreachable. Make sure the Spring Boot app is running.', true);
    }
    if (btn) {
        btn.disabled = false;
        btn.textContent = '🔄 Try Backend Connection';
    }
};

// ====== Login ======
if (loginForm) {
    loginForm.onsubmit = async (e) => {
        e.preventDefault();
        const username = $('username')?.value.trim();
        const password = $('password')?.value.trim();

        if (loginError) loginError.classList.add('hide');
        const btn = loginForm.querySelector('button');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Signing in...';
        }

        try {
            const res = await fetch(`${getApiBase()}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            const data = await res.json();
            if (data.success) {
                authToken = data.token;
                currentUser = { userId: data.userId, role: data.role, displayName: data.displayName };
                localStorage.setItem('authToken', authToken);
                localStorage.setItem('currentUser', JSON.stringify(currentUser));
                showScreen('app');
                const navUser = $('navUserName');
                const navRole = $('navUserRole');
                if (navUser) navUser.textContent = data.displayName;
                if (navRole) navRole.textContent = data.role === 'ADMIN' ? 'Admin' : 'Partner';
                navigateTo('dashboard');
            } else {
                if (loginError) {
                    loginError.textContent = data.message || 'Login failed';
                    loginError.classList.remove('hide');
                }
            }
        } catch (err) {
            if (loginError) {
                loginError.textContent = 'Cannot connect to server. Is the backend running?';
                loginError.classList.remove('hide');
            }
        }
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Sign In';
        }
    };
}

// ====== Logout ======
const logoutBtn = $('logoutBtn');
if (logoutBtn) {
    logoutBtn.onclick = async () => {
        await api('/auth/logout', { method: 'POST' });
        showScreen('login');
    };
}

// ====== Init ======
async function initApp() {
    if (authToken) {
        try {
            const res = await fetch(`${getApiBase()}/customers`, {
                headers: { 'Authorization': `Bearer ${authToken}` }
            });
            if (res.ok) {
                const storedUser = localStorage.getItem('currentUser');
                if (storedUser) {
                    currentUser = JSON.parse(storedUser);
                    showScreen('app');
                    const navUser = $('navUserName');
                    const navRole = $('navUserRole');
                    if (navUser) navUser.textContent = currentUser.displayName;
                    if (navRole) navRole.textContent = currentUser.role === 'ADMIN' ? 'Admin' : 'Partner';
                    navigateTo('dashboard');
                    return;
                }
            }
        } catch {}
        authToken = null;
        localStorage.removeItem('authToken');
    }
    showScreen('login');
}

(async function init() {
    const isBackendUp = await checkBackend();

    if (isBackendUp) {
        // Backend is online — show login/app
        landingScreen.classList.remove('active');
        loginScreen.classList.add('active', 'screen');
        await initApp();
    } else {
        // Backend offline — show the landing/preview page
        landingScreen.classList.add('active');
        loginScreen.classList.remove('active');
        appScreen.classList.remove('active');
    }
})();

// ====== Helper Functions ======
function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function getLocation() {
    return new Promise((resolve) => {
        if (!navigator.geolocation) { resolve(null); return; }
        navigator.geolocation.getCurrentPosition(
            (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
            () => resolve(null),
            { enableHighAccuracy: true, timeout: 10000 }
        );
    });
}

// ==========================================
//  DASHBOARD
// ==========================================
async function renderDashboard() {
    try {
        const [customers, products, routes, todayVisits] = await Promise.all([
            api('/customers'),
            api('/products'),
            api('/routes'),
            api('/visits/today')
        ]);

        mainContent.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-value">${customers?.length || 0}</div>
                    <div class="stat-label">Customers</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${products?.length || 0}</div>
                    <div class="stat-label">Products</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${routes?.length || 0}</div>
                    <div class="stat-label">Routes</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${todayVisits?.length || 0}</div>
                    <div class="stat-label">Visits Today</div>
                </div>
            </div>
            <div class="card">
                <div class="card-header">
                    <h3>Today's Visits</h3>
                    <button class="btn btn-sm btn-primary" onclick="navigateTo('visits')">Check In</button>
                </div>
                ${todayVisits?.length ? `
                <div class="visit-timeline">
                    ${todayVisits.map(v => `
                        <div class="visit-item">
                            <strong>${escapeHtml(v.customerName)}</strong>
                            <span class="text-sm text-muted">${v.visitedDate}</span>
                        </div>
                    `).join('')}
                </div>
                ` : '<p class="text-muted text-sm">No visits logged today yet.</p>'}
            </div>
            <div class="card">
                <div class="card-header">
                    <h3>Quick Actions</h3>
                </div>
                <div class="flex gap-2" style="flex-wrap:wrap">
                    <button class="btn btn-sm btn-primary" onclick="navigateTo('customers')">➕ Add Customer</button>
                    <button class="btn btn-sm btn-success" onclick="navigateTo('products')">📦 Add Product</button>
                    <button class="btn btn-sm btn-warning" onclick="navigateTo('routes')">🛣️ View Routes</button>
                    <button class="btn btn-sm btn-secondary" onclick="navigateTo('excel')">📋 Export Backup</button>
                </div>
            </div>
        `;
    } catch (e) {
        mainContent.innerHTML = '<div class="empty-state"><p>Error loading dashboard.</p></div>';
    }
}

// ==========================================
//  CUSTOMERS
// ==========================================
async function renderCustomers() {
    mainContent.innerHTML = `
        <div class="search-bar"><input type="text" id="customerSearch" placeholder="Search customers..." oninput="window.searchCustomers(this.value)"></div>
        <button class="btn btn-primary btn-full" onclick="showAddCustomer()">➕ Add Customer</button>
        <div id="customerList" class="mt-4"></div>
    `;
    await loadCustomers();
}

async function loadCustomers(query) {
    const container = $('customerList');
    if (!container) return;
    try {
        const data = query ? await api(`/customers/search?q=${encodeURIComponent(query)}`) : await api('/customers');
        if (!data) return;

        if (!data.length) {
            container.innerHTML = '<div class="empty-state"><div class="empty-icon">👥</div><p>No customers yet. Add your first customer!</p></div>';
            return;
        }

        container.innerHTML = data.map(c => `
            <div class="card" onclick="showCustomerDetail(${c.id})" style="cursor:pointer">
                <div class="flex-between">
                    <div>
                        <strong>${escapeHtml(c.name)}</strong>
                        ${c.shopName ? `<br><span class="text-sm text-muted">${escapeHtml(c.shopName)}</span>` : ''}
                    </div>
                    <div class="text-right text-sm">
                        ${c.lastVisitDate ? `<span class="badge badge-visited">✅ ${c.lastVisitDate}</span>` : '<span class="badge badge-not-visited">Not visited</span>'}
                    </div>
                </div>
                ${c.phone ? `<div class="text-sm text-muted mt-4">📞 ${escapeHtml(c.phone)}</div>` : ''}
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<div class="empty-state"><p>Error loading customers.</p></div>';
    }
}

window.searchCustomers = debounce(async (q) => {
    await loadCustomers(q);
}, 300);

function debounce(fn, delay) {
    let timer;
    return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), delay); };
}

async function showAddCustomer(prefill = {}) {
    showModal('Add Customer', `
        <form id="customerForm" onsubmit="saveCustomer(event)">
            <div class="form-group">
                <label>Name *</label>
                <input name="name" value="${escapeHtml(prefill.name || '')}" required>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Shop Name</label>
                    <input name="shopName" value="${escapeHtml(prefill.shopName || '')}">
                </div>
                <div class="form-group">
                    <label>Phone</label>
                    <input name="phone" value="${escapeHtml(prefill.phone || '')}">
                </div>
            </div>
            <div class="form-group">
                <label>Address</label>
                <textarea name="address">${escapeHtml(prefill.address || '')}</textarea>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Latitude</label>
                    <input name="latitude" type="number" step="any" value="${prefill.latitude || ''}">
                </div>
                <div class="form-group">
                    <label>Longitude</label>
                    <input name="longitude" type="number" step="any" value="${prefill.longitude || ''}">
                </div>
            </div>
            <button type="button" class="btn btn-sm btn-secondary" onclick="pinCurrentLocation()">📍 Pin Current Location</button>
            <div class="form-group mt-4">
                <label>Notes</label>
                <textarea name="notes">${escapeHtml(prefill.notes || '')}</textarea>
            </div>
            <div class="action-bar" style="position:static;padding:16px 0 0;border:none;gap:8px;display:flex">
                <button type="submit" class="btn btn-primary" style="flex:1">Save</button>
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        </form>
    `);

    if (prefill.id) {
        const form = $('customerForm');
        if (form) form.dataset.editId = prefill.id;
    }
}

window.pinCurrentLocation = async () => {
    const loc = await getLocation();
    if (loc) {
        const form = $('customerForm');
        if (form) {
            form.latitude.value = loc.lat;
            form.longitude.value = loc.lng;
            showToast('📍 Location pinned!');
        }
    } else {
        showToast('Could not get location. Make sure GPS is enabled.', true);
    }
};

window.saveCustomer = async (e) => {
    e.preventDefault();
    const form = e.target;
    const data = Object.fromEntries(new FormData(form).entries());
    data.latitude = data.latitude ? parseFloat(data.latitude) : null;
    data.longitude = data.longitude ? parseFloat(data.longitude) : null;

    try {
        if (form.dataset.editId) {
            await api(`/customers/${form.dataset.editId}`, {
                method: 'PUT', body: JSON.stringify(data)
            });
            showToast('Customer updated!');
        } else {
            await api('/customers', {
                method: 'POST', body: JSON.stringify(data)
            });
            showToast('Customer added!');
        }
        closeModal();
        await loadCustomers();
    } catch (e) {
        showToast('Error saving customer', true);
    }
};

async function showCustomerDetail(id) {
    try {
        const c = await api(`/customers/${id}`);
        const prices = await api(`/prices?customerId=${id}`);
        if (!c) return;

        const priceByProduct = {};
        prices?.forEach(p => {
            if (!priceByProduct[p.productId]) priceByProduct[p.productId] = [];
            priceByProduct[p.productId].push(p);
        });

        showModal(escapeHtml(c.name), `
            <div class="flex-between mb-4">
                ${c.lastVisitDate ? `<span class="badge badge-visited">Last visit: ${c.lastVisitDate}</span>` : '<span class="badge badge-not-visited">Not visited</span>'}
                ${currentUser?.role === 'ADMIN' ? `
                    <div class="flex gap-2">
                        <button class="btn btn-sm btn-secondary" onclick="editCustomer(${c.id})">✏️ Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteCustomer(${c.id})">🗑️</button>
                    </div>
                ` : ''}
            </div>
            <div class="form-group">
                <label>Shop Name</label>
                <div>${escapeHtml(c.shopName) || '-'}</div>
            </div>
            <div class="form-group">
                <label>Phone</label>
                <div>${escapeHtml(c.phone) || '-'}</div>
            </div>
            <div class="form-group">
                <label>Address</label>
                <div>${escapeHtml(c.address) || '-'}</div>
            </div>
            ${c.latitude ? `<div class="form-group"><label>Location</label><div>${c.latitude}, ${c.longitude}</div></div>` : ''}
            <div class="form-group">
                <label>Notes</label>
                <div>${escapeHtml(c.notes) || '-'}</div>
            </div>
            <hr style="margin:12px 0;border:none;border-top:1px solid var(--gray-200)">
            <h4 style="margin-bottom:8px">💰 Latest Rates</h4>
            ${c.latestPrices && Object.keys(c.latestPrices).length ? `
                ${Object.values(c.latestPrices).map(p => `
                    <div class="price-item">
                        <div>
                            <span class="price-product">${escapeHtml(p.productName)}</span>
                            <span class="price-date"> (since ${p.effectiveDate})</span>
                        </div>
                        <span class="price-amount">₹${p.price}</span>
                    </div>
                `).join('')}
            ` : '<p class="text-sm text-muted">No prices set yet.</p>'}

            <hr style="margin:12px 0;border:none;border-top:1px solid var(--gray-200)">
            <div class="flex gap-2">
                <button class="btn btn-sm btn-success" onclick="checkInCustomer(${c.id})">✅ Check In Today</button>
                <button class="btn btn-sm btn-primary" onclick="showSetPrice(${c.id}, '${escapeHtml(c.name)}')">💰 Set Price</button>
            </div>

            <h4 class="mt-4 mb-4">📋 Visit History</h4>
            <div id="visitHistory${c.id}">
                <div class="loading">Loading visits...</div>
            </div>
        `);

        try {
            const visits = await api(`/visits/customer/${c.id}`);
            const container = $(`visitHistory${c.id}`);
            if (container) {
                if (visits?.length) {
                    container.innerHTML = `<div class="visit-timeline">${visits.slice(0, 10).map(v => `
                        <div class="visit-item">
                            <strong>${v.visitedDate}</strong>
                            ${v.notes ? `<div class="text-sm text-muted">${escapeHtml(v.notes)}</div>` : ''}
                            <div class="text-sm text-muted">by ${escapeHtml(v.userName || 'Unknown')}</div>
                        </div>
                    `).join('')}</div>`;
                } else {
                    container.innerHTML = '<p class="text-sm text-muted">No visits recorded.</p>';
                }
            }
        } catch {}
    } catch (e) {
        showToast('Error loading customer details', true);
    }
}

window.editCustomer = async (id) => {
    try {
        const c = await api(`/customers/${id}`);
        if (c) {
            closeModal();
            showAddCustomer(c);
        }
    } catch {
        showToast('Error loading customer', true);
    }
};

window.deleteCustomer = async (id) => {
    if (!confirm('Are you sure you want to delete this customer?')) return;
    try {
        await api(`/customers/${id}`, { method: 'DELETE' });
        showToast('Customer deleted');
        closeModal();
        await loadCustomers();
    } catch {
        showToast('Error deleting customer', true);
    }
};

window.checkInCustomer = async (id) => {
    try {
        const loc = await getLocation();
        await api('/visits/checkin', {
            method: 'POST',
            body: JSON.stringify({
                customerId: id,
                notes: '',
                latitude: loc?.lat || null,
                longitude: loc?.lng || null
            })
        });
        showToast('✅ Checked in!');
        closeModal();
    } catch {
        showToast('Error checking in', true);
    }
};

window.showSetPrice = async (customerId, customerName) => {
    try {
        const products = await api('/products');
        if (!products?.length) {
            showToast('Add products first!', true);
            return;
        }

        showModal(`Set Price - ${customerName}`, `
            <form id="priceForm">
                <div class="form-group">
                    <label>Product *</label>
                    <select name="productId" required>
                        <option value="">Select product...</option>
                        ${products.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (${escapeHtml(p.unit || 'unit')})</option>`).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>Price (₹) *</label>
                    <input name="price" type="number" step="0.01" required>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Date</label>
                        <input name="effectiveDate" type="date" value="${new Date().toISOString().slice(0, 10)}">
                    </div>
                </div>
                <div class="form-group">
                    <label>Notes</label>
                    <input name="notes">
                </div>
                <input type="hidden" name="customerId" value="${customerId}">
                <div class="flex gap-2 mt-4">
                    <button type="submit" class="btn btn-primary" style="flex:1">Save Price</button>
                    <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                </div>
            </form>
        `);

        const priceForm = $('priceForm');
        if (priceForm) {
            priceForm.onsubmit = async (e) => {
                e.preventDefault();
                const data = Object.fromEntries(new FormData(e.target).entries());
                data.price = parseFloat(data.price);
                data.customerId = parseInt(data.customerId);
                data.productId = parseInt(data.productId);

                try {
                    await api('/prices', { method: 'POST', body: JSON.stringify(data) });
                    showToast('Price saved!');
                    closeModal();
                } catch {
                    showToast('Error saving price', true);
                }
            };
        }
    } catch {
        showToast('Error loading products', true);
    }
};

// ==========================================
//  PRODUCTS
// ==========================================
async function renderProducts() {
    mainContent.innerHTML = `
        <button class="btn btn-primary btn-full" onclick="showAddProduct()">📦 Add Product</button>
        <div id="productList" class="mt-4"></div>
    `;
    await loadProducts();
}

async function loadProducts() {
    const container = $('productList');
    if (!container) return;
    try {
        const data = await api('/products');
        if (!data?.length) {
            container.innerHTML = '<div class="empty-state"><div class="empty-icon">📦</div><p>No products yet.</p></div>';
            return;
        }
        container.innerHTML = data.map(p => `
            <div class="card">
                <div class="flex-between">
                    <div>
                        <strong>${escapeHtml(p.name)}</strong>
                        ${p.unit ? `<span class="text-sm text-muted">(${escapeHtml(p.unit)})</span>` : ''}
                    </div>
                    <div class="flex gap-2">
                        <span class="price-amount">₹${p.defaultPrice || 0}</span>
                        ${currentUser?.role === 'ADMIN' ? `
                            <button class="btn btn-sm btn-secondary" onclick="editProduct(${p.id})">✏️</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteProduct(${p.id})">🗑️</button>
                        ` : ''}
                    </div>
                </div>
            </div>
        `).join('');
    } catch {
        container.innerHTML = '<div class="empty-state"><p>Error loading products.</p></div>';
    }
}

window.showAddProduct = () => {
    showModal('Add Product', `
        <form id="productForm" onsubmit="saveProduct(event)">
            <div class="form-group">
                <label>Product Name *</label>
                <input name="name" required>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Unit (kg, pcs, etc.)</label>
                    <input name="unit" placeholder="e.g. kg, pcs, box">
                </div>
                <div class="form-group">
                    <label>Default Price (₹)</label>
                    <input name="defaultPrice" type="number" step="0.01">
                </div>
            </div>
            <div class="flex gap-2 mt-4">
                <button type="submit" class="btn btn-primary" style="flex:1">Save</button>
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        </form>
    `);
};

window.saveProduct = async (e) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target).entries());
    data.defaultPrice = data.defaultPrice ? parseFloat(data.defaultPrice) : null;

    try {
        await api('/products', { method: 'POST', body: JSON.stringify(data) });
        showToast('Product added!');
        closeModal();
        await loadProducts();
    } catch {
        showToast('Error saving product', true);
    }
};

window.editProduct = async (id) => {
    try {
        const p = await api(`/products/${id}`);
        if (p) {
            showModal('Edit Product', `
                <form id="productForm" onsubmit="updateProduct(event, ${id})">
                    <div class="form-group">
                        <label>Product Name *</label>
                        <input name="name" value="${escapeHtml(p.name)}" required>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label>Unit</label>
                            <input name="unit" value="${escapeHtml(p.unit || '')}">
                        </div>
                        <div class="form-group">
                            <label>Default Price (₹)</label>
                            <input name="defaultPrice" type="number" step="0.01" value="${p.defaultPrice || ''}">
                        </div>
                    </div>
                    <div class="flex gap-2 mt-4">
                        <button type="submit" class="btn btn-primary" style="flex:1">Update</button>
                        <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    </div>
                </form>
            `);
        }
    } catch {}
};

window.updateProduct = async (e, id) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target).entries());
    data.defaultPrice = data.defaultPrice ? parseFloat(data.defaultPrice) : null;
    try {
        await api(`/products/${id}`, { method: 'PUT', body: JSON.stringify(data) });
        showToast('Product updated!');
        closeModal();
        await loadProducts();
    } catch {
        showToast('Error updating product', true);
    }
};

window.deleteProduct = async (id) => {
    if (!confirm('Delete this product?')) return;
    try {
        await api(`/products/${id}`, { method: 'DELETE' });
        showToast('Product deleted');
        await loadProducts();
    } catch {
        showToast('Error deleting product', true);
    }
};

// ==========================================
//  ROUTES
// ==========================================
async function renderRoutes() {
    mainContent.innerHTML = `
        <button class="btn btn-primary btn-full" onclick="showAddRoute()">🛣️ Create Route</button>
        <div id="routeList" class="mt-4"></div>
    `;
    await loadRoutes();
}

async function loadRoutes() {
    const container = $('routeList');
    if (!container) return;
    try {
        const data = await api('/routes');
        if (!data?.length) {
            container.innerHTML = '<div class="empty-state"><div class="empty-icon">🛣️</div><p>No routes yet. Create your first route!</p></div>';
            return;
        }
        container.innerHTML = data.map(r => `
            <div class="card" onclick="showRouteDetail(${r.id})" style="cursor:pointer">
                <div class="flex-between">
                    <div>
                        <strong>${escapeHtml(r.name)}</strong>
                        ${r.description ? `<br><span class="text-sm text-muted">${escapeHtml(r.description)}</span>` : ''}
                    </div>
                    <div class="text-sm text-muted">${r.customers?.length || 0} customers</div>
                </div>
            </div>
        `).join('');
    } catch {
        container.innerHTML = '<div class="empty-state"><p>Error loading routes.</p></div>';
    }
}

window.showAddRoute = () => {
    showModal('Create Route', `
        <form id="routeForm" onsubmit="saveRoute(event)">
            <div class="form-group">
                <label>Route Name *</label>
                <input name="name" placeholder="e.g. Monday Route - Sector 5" required>
            </div>
            <div class="form-group">
                <label>Description</label>
                <textarea name="description" placeholder="Optional notes about this route"></textarea>
            </div>
            <div class="flex gap-2 mt-4">
                <button type="submit" class="btn btn-primary" style="flex:1">Create</button>
                <button type="button" class="btn btn-secondary" onclick="closeModal()">Cancel</button>
            </div>
        </form>
    `);
};

window.saveRoute = async (e) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target).entries());
    try {
        await api('/routes', { method: 'POST', body: JSON.stringify(data) });
        showToast('Route created!');
        closeModal();
        await loadRoutes();
    } catch {
        showToast('Error creating route', true);
    }
};

async function showRouteDetail(routeId) {
    try {
        const r = await api(`/routes/${routeId}`);
        if (!r) return;

        showModal(escapeHtml(r.name), `
            ${r.description ? `<p class="text-muted text-sm mb-4">${escapeHtml(r.description)}</p>` : ''}

            <div class="flex gap-2 mb-4">
                <button class="btn btn-sm btn-primary" onclick="addCustomerToRoute(${routeId})">➕ Add Customer</button>
                ${currentUser?.role === 'ADMIN' ? `
                    <button class="btn btn-sm btn-danger" onclick="deleteRoute(${routeId})">🗑️ Delete Route</button>
                ` : ''}
            </div>

            <h4 class="mb-4">Customers (${r.customers?.length || 0})</h4>
            <div id="routeCustomers">
                ${r.customers?.length ? r.customers.map((c, i) => `
                    <div class="card" style="padding:10px 12px;margin-bottom:6px">
                        <div class="flex-between">
                            <div>
                                <strong>${i + 1}.</strong> ${escapeHtml(c.name)}
                                ${c.shopName ? `<span class="text-sm text-muted">- ${escapeHtml(c.shopName)}</span>` : ''}
                            </div>
                            <div class="flex gap-2">
                                ${c.latestPrices && Object.keys(c.latestPrices).length ? 
                                    `<span class="text-sm">₹${Object.values(c.latestPrices)[0]?.price || '-'}</span>` : ''}
                                <button class="btn btn-sm btn-danger" onclick="removeCustomerFromRoute(${routeId}, ${c.id})">✕</button>
                            </div>
                        </div>
                    </div>
                `).join('') : '<p class="text-muted text-sm">No customers in this route.</p>'}
            </div>
        `);
    } catch {
        showToast('Error loading route', true);
    }
}

window.addCustomerToRoute = async (routeId) => {
    try {
        const customers = await api('/customers');
        const r = await api(`/routes/${routeId}`);
        const existingIds = new Set(r?.customers?.map(c => c.id) || []);

        showModal('Add Customer to Route', `
            <div class="search-bar"><input type="text" id="addCustSearch" placeholder="Search..." oninput="filterCustomerList(this.value)"></div>
            <div id="addCustomerList">
                ${customers?.filter(c => !existingIds.has(c.id)).map(c => `
                    <div class="card" style="padding:10px 12px;margin-bottom:6px;cursor:pointer" 
                         onclick="addCustomerToRouteAction(${routeId}, ${c.id})">
                        <strong>${escapeHtml(c.name)}</strong>
                        ${c.shopName ? `<span class="text-sm text-muted">- ${escapeHtml(c.shopName)}</span>` : ''}
                    </div>
                `).join('') || '<p class="text-muted">All customers already in route.</p>'}
            </div>
        `);

        window.filterCustomerList = (q) => {
            document.querySelectorAll('#addCustomerList .card').forEach(el => {
                el.style.display = el.textContent.toLowerCase().includes(q.toLowerCase()) ? 'block' : 'none';
            });
        };
    } catch {
        showToast('Error loading customers', true);
    }
};

window.addCustomerToRouteAction = async (routeId, customerId) => {
    try {
        await api(`/routes/${routeId}/customers`, {
            method: 'POST',
            body: JSON.stringify({ customerId, visitOrder: 999 })
        });
        showToast('Customer added to route!');
        closeModal();
        await showRouteDetail(routeId);
    } catch {
        showToast('Error adding customer', true);
    }
};

window.removeCustomerFromRoute = async (routeId, customerId) => {
    try {
        await api(`/routes/${routeId}/customers/${customerId}`, { method: 'DELETE' });
        showToast('Customer removed from route');
        await showRouteDetail(routeId);
    } catch {
        showToast('Error removing customer', true);
    }
};

window.deleteRoute = async (id) => {
    if (!confirm('Delete this route and all its customer mappings?')) return;
    try {
        await api(`/routes/${id}`, { method: 'DELETE' });
        showToast('Route deleted');
        closeModal();
        await loadRoutes();
    } catch {
        showToast('Error deleting route', true);
    }
};

// ==========================================
//  MAP VIEW
// ==========================================
async function renderMapView() {
    mainContent.innerHTML = `
        <div class="flex gap-2 mb-4" style="flex-wrap:wrap">
            <button class="btn btn-sm btn-primary" onclick="refreshMap()">🔄 Refresh</button>
            <select id="mapRouteFilter" onchange="refreshMap()" style="flex:1;padding:6px 10px;border:1px solid var(--gray-300);border-radius:6px;font-size:14px">
                <option value="">All Routes</option>
            </select>
        </div>
        <div id="map" style="height:500px;border-radius:8px"></div>
        <div id="mapLegend" class="mt-4"></div>
    `;

    try {
        const routes = await api('/routes');
        const select = $('mapRouteFilter');
        routes?.forEach(r => {
            const opt = document.createElement('option');
            opt.value = r.id;
            opt.textContent = r.name;
            if (select) select.appendChild(opt);
        });
    } catch {}

    await initMap();
}

async function initMap() {
    if (mapInstance) {
        mapInstance.remove();
        mapInstance = null;
    }
    if (typeof L === 'undefined') {
        if (mainContent) mainContent.innerHTML = '<div class="empty-state"><p>Map library not loaded.</p></div>';
        return;
    }

    mapInstance = L.map('map').setView([20.5937, 78.9629], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(mapInstance);

    await refreshMap();
}

async function refreshMap() {
    if (!mapInstance) return;

    mapMarkers.forEach(m => mapInstance.removeLayer(m));
    mapMarkers = [];

    try {
        const filterRouteId = $('mapRouteFilter')?.value;
        let customers = [];

        if (filterRouteId) {
            const route = await api(`/routes/${filterRouteId}`);
            customers = route?.customers || [];
        } else {
            customers = await api('/customers') || [];
        }

        const colors = ['#2563eb', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'];

        const routeColorMap = {};
        try {
            const routes = await api('/routes');
            routes?.forEach((r, i) => {
                r.customers?.forEach(c => {
                    routeColorMap[c.id] = colors[i % colors.length];
                });
            });
        } catch {}

        const hasLocation = customers.filter(c => c.latitude && c.longitude);
        const legendEl = document.querySelector('#mapLegend');
        if (!hasLocation.length) {
            if (legendEl) legendEl.innerHTML = '<p class="text-muted text-sm">No customers with GPS locations yet.</p>';
            return;
        }

        const bounds = L.latLngBounds();
        hasLocation.forEach(c => {
            const color = routeColorMap[c.id] || '#64748b';
            const marker = L.circleMarker([c.latitude, c.longitude], {
                radius: 10,
                fillColor: color,
                color: '#ffffff',
                weight: 2,
                opacity: 1,
                fillOpacity: 0.8
            }).addTo(mapInstance);

            const latestPrice = c.latestPrices && Object.keys(c.latestPrices).length
                ? `<br>💰 Latest: ₹${Object.values(c.latestPrices)[0]?.price || '-'}`
                : '';

            marker.bindPopup(`
                <strong>${escapeHtml(c.name)}</strong>
                ${c.shopName ? `<br><em>${escapeHtml(c.shopName)}</em>` : ''}
                ${c.phone ? `<br>📞 ${escapeHtml(c.phone)}` : ''}
                ${latestPrice}
            `);
            mapMarkers.push(marker);
            bounds.extend([c.latitude, c.longitude]);
        });

        if (hasLocation.length > 1) {
            mapInstance.fitBounds(bounds, { padding: [30, 30] });
        } else {
            mapInstance.setView([hasLocation[0].latitude, hasLocation[0].longitude], 15);
        }

        const legendHtml = [];
        try {
            const routes = await api('/routes');
            routes?.forEach((r, i) => {
                const color = colors[i % colors.length];
                const count = r.customers?.filter(c => c.latitude).length || 0;
                if (count > 0) {
                    legendHtml.push(`<span style="display:inline-flex;align-items:center;gap:4px;margin-right:12px">
                        <span style="width:12px;height:12px;border-radius:50%;background:${color};display:inline-block"></span>
                        ${escapeHtml(r.name)} (${count})
                    </span>`);
                }
            });
        } catch {}
        if (legendEl) legendEl.innerHTML = legendHtml.length ? legendHtml.join('') : '';

    } catch {
        showToast('Error loading map data', true);
    }
}

// ==========================================
//  VISITS / CHECK-IN
// ==========================================
async function renderVisits() {
    mainContent.innerHTML = `
        <div class="card mb-4">
            <h3>✅ Today's Check-in</h3>
            <p class="text-sm text-muted">Select a customer to mark as visited today</p>
        </div>
        <div class="search-bar"><input type="text" id="visitSearch" placeholder="Search customers..." oninput="filterVisitList(this.value)"></div>
        <div id="visitList"></div>
    `;
    await loadVisitList();
}

async function loadVisitList() {
    const container = $('visitList');
    if (!container) return;
    try {
        const [customers, todayVisits] = await Promise.all([
            api('/customers'),
            api('/visits/today')
        ]);

        const visitedToday = new Set(todayVisits?.map(v => v.customerId) || []);

        if (!customers?.length) {
            container.innerHTML = '<div class="empty-state"><p>No customers yet.</p></div>';
            return;
        }

        container.innerHTML = customers.map(c => `
            <div class="card visit-card" data-name="${(c.name + ' ' + (c.shopName || '')).toLowerCase()}"
                 style="cursor:pointer;${visitedToday.has(c.id) ? 'opacity:0.6' : ''}">
                <div class="flex-between">
                    <div>
                        <strong>${escapeHtml(c.name)}</strong>
                        ${c.shopName ? `<br><span class="text-sm text-muted">${escapeHtml(c.shopName)}</span>` : ''}
                    </div>
                    <div>
                        ${visitedToday.has(c.id)
                            ? '<span class="badge badge-visited">✅ Visited</span>'
                            : `<button class="btn btn-sm btn-success" onclick="doCheckIn(${c.id}, this)">✅ Check In</button>`
                        }
                    </div>
                </div>
            </div>
        `).join('');
    } catch {
        container.innerHTML = '<div class="empty-state"><p>Error loading data.</p></div>';
    }
}

window.filterVisitList = (q) => {
    document.querySelectorAll('.visit-card').forEach(el => {
        el.style.display = el.dataset.name.includes(q.toLowerCase()) ? 'block' : 'none';
    });
};

window.doCheckIn = async (customerId, btn) => {
    btn.disabled = true;
    btn.textContent = '✓ Checking...';
    try {
        const loc = await getLocation();
        await api('/visits/checkin', {
            method: 'POST',
            body: JSON.stringify({
                customerId,
                notes: '',
                latitude: loc?.lat || null,
                longitude: loc?.lng || null
            })
        });
        showToast('✅ Checked in successfully!');
        await loadVisitList();
    } catch {
        showToast('Error checking in', true);
        btn.disabled = false;
        btn.textContent = '✅ Check In';
    }
};

// ==========================================
//  PRICE HISTORY
// ==========================================
async function renderPriceHistory() {
    mainContent.innerHTML = `
        <div class="card mb-4">
            <h3>💰 Price History</h3>
            <p class="text-sm text-muted">View price history for a customer-product pair</p>
        </div>
        <div class="form-row mb-4">
            <div class="form-group">
                <label>Customer</label>
                <select id="priceCustomer" onchange="loadPriceHistoryView()">
                    <option value="">Select customer...</option>
                </select>
            </div>
            <div class="form-group">
                <label>Product</label>
                <select id="priceProduct" onchange="loadPriceHistoryView()">
                    <option value="">All products</option>
                </select>
            </div>
        </div>
        <div id="priceHistoryList"></div>
    `;

    try {
        const [customers, products] = await Promise.all([
            api('/customers'),
            api('/products')
        ]);

        const custSelect = $('priceCustomer');
        customers?.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.id;
            opt.textContent = `${c.name}${c.shopName ? ' (' + c.shopName + ')' : ''}`;
            if (custSelect) custSelect.appendChild(opt);
        });

        const prodSelect = $('priceProduct');
        products?.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.id;
            opt.textContent = p.name;
            if (prodSelect) prodSelect.appendChild(opt);
        });
    } catch {}
}

window.loadPriceHistoryView = async () => {
    const container = $('priceHistoryList');
    if (!container) return;

    const customerId = $('priceCustomer')?.value;
    const productId = $('priceProduct')?.value;

    if (!customerId) {
        container.innerHTML = '<p class="text-muted text-sm">Select a customer to view price history.</p>';
        return;
    }

    try {
        let url = `/prices?customerId=${customerId}`;
        if (productId) url += `&productId=${productId}`;
        const prices = await api(url);

        if (!prices?.length) {
            container.innerHTML = '<div class="empty-state"><p>No price history for this selection.</p></div>';
            return;
        }

        container.innerHTML = `
            <h4 class="mb-4">Price History (${prices.length} records)</h4>
            ${prices.map(p => `
                <div class="price-item">
                    <div>
                        <span class="price-product">${escapeHtml(p.productName || 'Product #' + p.productId)}</span>
                        <span class="price-date"> — ${p.effectiveDate}</span>
                    </div>
                    <div class="text-right">
                        <span class="price-amount">₹${p.price}</span>
                        ${p.notes ? `<br><span class="text-sm text-muted">${escapeHtml(p.notes)}</span>` : ''}
                    </div>
                </div>
            `).join('')}
        `;
    } catch {
        container.innerHTML = '<div class="empty-state"><p>Error loading price history.</p></div>';
    }
};

// ==========================================
//  EXCEL / BACKUP
// ==========================================
function renderExcel() {
    mainContent.innerHTML = `
        <div class="card mb-4">
            <h3>📋 Backup & Export</h3>
            <p class="text-sm text-muted mb-4">Export all your data to an Excel file for backup or offline access.</p>
            <button class="btn btn-primary btn-full" onclick="exportExcel()">📥 Export to Excel</button>
        </div>
        <div class="card mb-4">
            <h3>📥 Import Customers from Excel</h3>
            <p class="text-sm text-muted mb-4">Upload an Excel file with a "Customers" sheet to bulk-import customer data.</p>
            <form id="importForm" onsubmit="importExcel(event)">
                <div class="form-group">
                    <input type="file" name="file" accept=".xlsx,.xls" required>
                </div>
                <button type="submit" class="btn btn-secondary btn-full">📤 Import</button>
            </form>
        </div>
        <div class="card">
            <h3>📖 Why Excel is Not the Live Database</h3>
            <p class="text-sm text-muted" style="line-height:1.7">
                Excel files work well as a <strong>backup/export format</strong> because you can open them on any device,
                share via email/WhatsApp, and keep offline copies. However, using an Excel file as the <em>live</em> database
                causes problems when multiple people (you + partners) try to write to it at the same time — the file can
                corrupt, edits get lost, and there's no proper querying for "show me all customers on this route with their
                latest prices." This app uses an H2 database (file-based, no server install needed) that handles
                concurrent writes, relationships, and queries properly, while letting you export to Excel anytime for backup.
            </p>
        </div>
    `;
}

window.exportExcel = async () => {
    try {
        const res = await fetch(`${getApiBase()}/excel/export`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        if (!res.ok) { showToast('Export failed', true); return; }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `wholesale_backup_${new Date().toISOString().slice(0,10)}.xlsx`;
        a.click();
        URL.revokeObjectURL(url);
        showToast('📥 Exported successfully!');
    } catch {
        showToast('Export failed', true);
    }
};

window.importExcel = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    try {
        const res = await fetch(`${getApiBase()}/excel/import`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${authToken}` },
            body: formData
        });
        const msg = await res.text();
        if (res.ok) {
            showToast('✅ ' + msg);
        } else {
            showToast('Import failed: ' + msg, true);
        }
    } catch {
        showToast('Import failed', true);
    }
};
