/**
 * @file Main application logic for the bookmark manager.
 * @description This script handles user authentication, folder/bookmark management,
 * file uploads, background task polling, and media playback.
 */

// A simple query selector helper for convenience.
const $ = (selector) => document.querySelector(selector);

// Centralized object for all DOM element references for better organization.
const UI = {
    // Screens
    authScreen: $('#auth-screen'),
    appScreen: $('#app-screen'),
    // Auth
    authForm: $('#auth-form'),
    authTitle: $('#auth-title'),
    authButton: $('#auth-button'),
    authMessage: $('#auth-message'),
    toggleAuthBtn: $('#toggle-auth'),
    // Main App & Lists
    mainFoldersList: $('#main-folders-list'),
    subFoldersList: $('#sub-folders-list'),
    bookmarksContainer: $('#bookmarks-container'),
    currentFolderName: $('#current-folder-name'),
    // Mobile Menu
    mobileMenu: $('#mobile-menu'),
    mobileMenuWrapper: $('#mobile-menu-wrapper'),
    // Modals
    addFolderModal: $('#add-folder-modal'),
    addBookmarkModal: $('#add-bookmark-modal'),
    crawlUrlModal: $('#crawl-url-modal'),
    playerModal: $('#player-modal'),
    messageModal: $('#message-modal'),
    modalMessage: $('#modal-message'),
    // Forms & Inputs
    addFolderForm: $('#add-folder-form'),
    addBookmarkForm: $('#add-bookmark-form'),
    newBookmarkCoverInput: $('#new-bookmark-cover'),
    coverPreview: $('#cover-preview'),
    crawlUrlForm: $('#crawl-url-form'),
    // Buttons
    logoutBtn: $('#logout-btn'),
    addFolderBtn: $('#add-folder-btn'),
    addBookmarkBtn: $('#add-bookmark-btn'),
    crawlUrlBtn: $('#crawl-url-btn'),
    mobileMenuBtn: $('#mobile-menu-btn'),
    backToMainFoldersBtn: $('#back-to-main-folders-btn'),
    // Player
    playerModalTitle: $('#player-modal-title'),
    playerContainer: $('#player-container'),
    // Task Progress
    downloadStatusPanel: $('#download-status-panel'),
    downloadMessage: $('#download-message'),
    downloadProgress: $('#download-progress'),
};

// Centralized application state.
const state = {
    currentUserId: localStorage.getItem('user_id') || null,
    currentMainFolderId: null,
    currentSubFolderId: null,
    isLoginMode: true,
    taskPollingInterval: null,
};

// --- API & Helper Functions ---

/**
 * Generic fetch wrapper to handle API requests.
 * Automatically adds auth token and handles 401 unauthorized errors.
 * @param {string} url - The API endpoint.
 * @param {object} options - Standard fetch options.
 * @returns {Promise<object|null>} The JSON response or null on error.
 */
async function fetchData(url, options = {}) {
    const token = localStorage.getItem('user_id');
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    try {
        const response = await fetch(url, { ...options, headers });
        if (response.status === 401) {
            showModal('登录已过期，请重新登录。');
            logout();
            return null;
        }
        // For file uploads, response might not be JSON if it fails early.
        if (options.body instanceof FormData) return response.json();
        return response.json();
    } catch (error) {
        console.error('Fetch error:', error);
        showModal('网络连接错误。');
        return null;
    }
}

/**
 * Handles the UI state for form submissions (disables button, shows loader, etc.).
 * @param {HTMLButtonElement} submitBtn - The form's submit button.
 * @param {HTMLElement} messageEl - The element to display error messages.
 * @param {Function} submitLogic - An async function containing the submission logic.
 */
async function handleFormSubmit(submitBtn, messageEl, submitLogic) {
    const originalBtnText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = `<div class="loader !w-5 !h-5 !border-2 mr-2"></div> 处理中...`;
    if (messageEl) messageEl.textContent = '';

    try {
        const success = await submitLogic();
        if (!success) throw new Error('操作失败，请重试。');
    } catch (error) {
        if (messageEl) messageEl.textContent = error.message;
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnText;
    }
}

// --- UI Toggling Functions ---

const showScreen = (screenId) => {
    UI.authScreen.style.display = screenId === 'auth-screen' ? 'block' : 'none';
    UI.appScreen.classList.toggle('hidden', screenId === 'auth-screen');
};

const toggleModal = (modal, show) => modal.classList.toggle('hidden', !show).classList.toggle('flex', show);
const showModal = (message) => { UI.modalMessage.textContent = message; toggleModal(UI.messageModal, true); };

const toggleMobileMenu = (show) => {
    UI.mobileMenu.classList.toggle('open', show);
    UI.mobileMenuOverlay.classList.toggle('hidden', !show);
    if (!show) setTimeout(() => UI.mobileMenuWrapper.classList.remove('show-sub'), 300);
};

// --- Data Rendering ---


const createFolderItemHTML = (folder, type) => `
    <li>
        <button data-id="${folder.id}" class="${type}-item flex items-center w-full p-3 rounded-lg hover:bg-gray-200">
            <svg class="h-5 w-5 text-gray-500 mr-3 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/></svg>
            <span class="text-sm font-medium truncate">${folder.name}</span>
        </button>
    </li>`;

/**
 * Generic function to fetch and render items into a list container.
 * @param {HTMLElement} listEl - The container element (e.g., a UL).
 * @param {string} url - The API endpoint to fetch data from.
 * @param {Function} itemTemplate - Function to generate HTML for one item.
 */
async function renderList(listEl, url, itemTemplate) {
    listEl.innerHTML = `<li class="p-2 text-center text-gray-500"><div class="loader mx-auto"></div></li>`;
    const data = await fetchData(url);
    if (data?.success && Array.isArray(data.folders)) {
        listEl.innerHTML = data.folders.length > 0
            ? data.folders.map(itemTemplate).join('')
            : `<li class="p-2 text-gray-500 text-center">空空如也</li>`;
    } else {
        listEl.innerHTML = `<li class="p-2 text-red-500 text-center">加载失败</li>`;
    }
}

const fetchMainFolders = () => renderList(UI.mainFoldersList, '/get_main_folders', (f) => createFolderItemHTML(f, 'main-folder'));
const fetchSubFolders = (parentId) => renderList(UI.subFoldersList, `/get_sub_folders/${parentId}`, (f) => createFolderItemHTML(f, 'sub-folder'));

/**
 * Fetches and renders bookmarks for a given folder.
 * @param {string} folderId - The ID of the folder.
 */
async function fetchBookmarks(folderId) {
    UI.bookmarksContainer.innerHTML = `<div class="w-full text-center py-8"><div class="loader mx-auto"></div></div>`;
    const data = await fetchData(`/get_bookmarks/${folderId}`);
    if (data?.success) {
        if (data.bookmarks.length === 0) {
            UI.bookmarksContainer.innerHTML = `<div class="w-full text-center text-gray-500 mt-8">该文件夹下没有内容</div>`;
            return;
        }
        UI.bookmarksContainer.innerHTML = data.bookmarks.map(bookmark => `
            <div class="bookmark-card bg-white rounded-xl shadow-lg hover:shadow-xl transition-shadow duration-300 overflow-hidden cursor-pointer h-full flex flex-col"
                 data-link="${bookmark.link}" data-title="${bookmark.title}" ${bookmark.filepath ? `data-filepath="${bookmark.filepath}"` : ''}>
                <img src="${bookmark.cover}" alt="Cover" class="w-full h-32 object-cover pointer-events-none">
                <div class="p-4 flex-grow flex flex-col pointer-events-none">
                    <h4 class="text-md font-semibold text-gray-800 truncate">${bookmark.title}</h4>
                    <p class="text-xs text-gray-500 mt-1 flex-grow">${bookmark.description || '无描述'}</p>
                    <p class="text-xs text-blue-500 mt-2 truncate">${bookmark.link}</p>
                </div>
            </div>`
        ).join('');
    } else {
        UI.bookmarksContainer.innerHTML = `<div class="w-full text-center text-red-500 mt-8">加载失败</div>`;
    }
}


// --- Task Polling ---

/**
 * Polls the progress of tasks until all are completed or failed.
 * @param {string[]} taskIds - An array of task IDs to monitor.
 */
function pollTaskProgress(taskIds) {
    if (state.taskPollingInterval) clearInterval(state.taskPollingInterval);

    let activeTaskIds = [...taskIds];
    const totalTasks = taskIds.length;
    let completedCount = 0;

    UI.downloadStatusPanel.style.visibility = 'visible';

    state.taskPollingInterval = setInterval(async () => {
        if (activeTaskIds.length === 0) {
            clearInterval(state.taskPollingInterval);
            state.taskPollingInterval = null;
            UI.downloadStatusPanel.style.visibility = 'hidden';
            if (state.currentSubFolderId) fetchBookmarks(state.currentSubFolderId); // Final refresh
            return;
        }

        const currentTaskId = activeTaskIds[0];
        const result = await fetchData(`/get_progress/${currentTaskId}`);

        if (result?.success) {
            UI.downloadMessage.textContent = result.message || '处理中...';
            if (['COMPLETED', 'FAILED'].includes(result.status)) {
                activeTaskIds.shift(); // Task finished, remove from queue
                completedCount++;
                if (state.currentSubFolderId) fetchBookmarks(state.currentSubFolderId);
            }
        } else {
            activeTaskIds.shift(); // API error, remove to prevent infinite loop
            completedCount++;
        }
        UI.downloadProgress.textContent = `(${completedCount}/${totalTasks})`;
    }, 5000);
}


// --- Event Handlers ---

const handleMainFolderClick = (element) => {
    state.currentMainFolderId = element.dataset.id;
    fetchSubFolders(state.currentMainFolderId);
    UI.mobileMenuWrapper.classList.add('show-sub'); // For mobile view
    document.querySelectorAll('.main-folder-item').forEach(el => el.classList.remove('bg-gray-200'));
    element.classList.add('bg-gray-200');
};

const handleSubFolderClick = async (element) => {
    state.currentSubFolderId = element.dataset.id;
    UI.currentFolderName.textContent = element.querySelector('span').textContent;
    await fetchBookmarks(state.currentSubFolderId);
    
    // Check for any unfinished tasks in this folder
    const result = await fetchData('/recover_tasks', {
        method: 'POST',
        body: JSON.stringify({ folder_id: state.currentSubFolderId })
    });
    if (result?.success && result.task_ids?.length > 0) {
        pollTaskProgress(result.task_ids);
    }
    
    toggleMobileMenu(false); // Close mobile menu after selection
    document.querySelectorAll('.sub-folder-item').forEach(el => el.classList.remove('bg-gray-200'));
    element.classList.add('bg-gray-200');
};

const handleBookmarkClick = (element) => {
    const { link, title, filepath } = element.dataset;
    if (filepath) {
        // ... Logic to show player modal ...
        console.log(`Playing ${filepath}`);
    } else if (link) {
        window.open(link, '_blank');
    }
};

const logout = () => {
    localStorage.removeItem('user_id');
    state.currentUserId = null;
    showScreen('auth-screen');
};

/**
 * Initializes all application event listeners.
 */
function initializeEventListeners() {
    // Main event delegation for dynamically created elements
    document.addEventListener('click', (e) => {
        const target = e.target;
        const mainFolder = target.closest('.main-folder-item');
        if (mainFolder) return handleMainFolderClick(mainFolder);

        const subFolder = target.closest('.sub-folder-item');
        if (subFolder) return handleSubFolderClick(subFolder);

        const bookmarkCard = target.closest('.bookmark-card');
        if (bookmarkCard) return handleBookmarkClick(bookmarkCard);
        
        if (target.closest('#modal-close-btn')) return toggleModal(UI.messageModal, false);
        if (target.closest('#player-modal-close-btn')) return toggleModal(UI.playerModal, false);
    });
    
    // Form Submissions
    UI.authForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await handleFormSubmit(UI.authButton, UI.authMessage, async () => {
            const email = $('#email').value;
            const password = $('#password').value;
            const url = state.isLoginMode ? '/login' : '/register';
            const result = await fetchData(url, { method: 'POST', body: JSON.stringify({ email, password }) });
            if (result?.success) {
                state.currentUserId = result.user_id;
                localStorage.setItem('user_id', state.currentUserId);
                showScreen('app-screen');
                fetchMainFolders();
                return true;
            }
            throw new Error(result?.message || '操作失败');
        });
    });

    // Other direct event listeners
    UI.logoutBtn.addEventListener('click', logout);
    UI.mobileMenuBtn.addEventListener('click', (e) => { e.stopPropagation(); toggleMobileMenu(true); });
    UI.backToMainFoldersBtn.addEventListener('click', () => UI.mobileMenuWrapper.classList.remove('show-sub'));

    UI.addFolderBtn.addEventListener('click', () => {
        if (!state.currentMainFolderId) return showModal('请先选择一个主文件夹。');
        UI.addFolderForm.reset();
        toggleModal(UI.addFolderModal, true);
    });

    UI.addBookmarkBtn.addEventListener('click', () => {
        if (!state.currentSubFolderId) return showModal('请先选择一个子文件夹。');
        UI.addBookmarkForm.reset();
        UI.coverPreview.classList.add('hidden');
        toggleModal(UI.addBookmarkModal, true);
    });
}

// --- Initialization ---

/**
 * Initializes the application.
 */
function init() {
    initializeEventListeners();
    if (state.currentUserId) {
        showScreen('app-screen');
        fetchMainFolders();
    } else {
        showScreen('auth-screen');
    }
}

init(); // Start the application