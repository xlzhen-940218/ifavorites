// DOM 元素引用
const authScreen = document.getElementById('auth-screen');
const appScreen = document.getElementById('app-screen');
const authForm = document.getElementById('auth-form');
const mainFoldersList = document.getElementById('main-folders-list');
const subFoldersList = document.getElementById('sub-folders-list');
const bookmarksContainer = document.getElementById('bookmarks-container');
const currentFolderName = document.getElementById('current-folder-name');

// 移动端相关
const mobileMenu = document.getElementById('mobile-menu');
const mobileMenuWrapper = document.getElementById('mobile-menu-wrapper');
const mobileMenuBtn = document.getElementById('mobile-menu-btn');
const mobileMenuOverlay = document.getElementById('mobile-menu-overlay');
const backToMainFoldersBtn = document.getElementById('back-to-main-folders-btn');

// 功能相关
const addFolderBtn = document.getElementById('add-folder-btn');
const addFolderForm = document.getElementById('add-folder-form');
const addBookmarkBtn = document.getElementById('add-bookmark-btn');
const addBookmarkForm = document.getElementById('add-bookmark-form');
const coverPreview = document.getElementById('cover-preview');
const newBookmarkCoverInput = document.getElementById('new-bookmark-cover');
const crawlUrlBtn = document.getElementById('crawl-url-btn');
const crawlUrlForm = document.getElementById('crawl-url-form');
const playerModal = document.getElementById('player-modal');
const playerModalTitle = document.getElementById('player-modal-title');
const playerContainer = document.getElementById('player-container');
const playerModalCloseBtn = document.getElementById('player-modal-close-btn');

// 状态变量
let currentUserId = null;
let currentMainFolderId = null;
let currentSubFolderId = null;

// --- 核心逻辑 ---
function init() {
    const storedUserId = localStorage.getItem('user_id');
    if (storedUserId) {
        currentUserId = storedUserId;
        showScreen('app-screen');
        fetchMainFolders();
    } else {
        showScreen('auth-screen');
    }
}

// --- 移动端菜单交互 (优化后) ---
function toggleMobileMenu(show) {
    if (show) {
        mobileMenu.classList.add('open');
        mobileMenuOverlay.classList.remove('hidden');
    } else {
        mobileMenu.classList.remove('open');
        mobileMenuOverlay.classList.add('hidden');
        setTimeout(() => mobileMenuWrapper.classList.remove('show-sub'), 300); // 关闭时重置
    }
}

function showSubFoldersView() { mobileMenuWrapper.classList.add('show-sub'); }
function showMainFoldersView() { mobileMenuWrapper.classList.remove('show-sub'); }

mobileMenuBtn.addEventListener('click', (e) => { e.stopPropagation(); toggleMobileMenu(true); });
backToMainFoldersBtn.addEventListener('click', showMainFoldersView);

// --- 数据获取与渲染 ---
async function fetchData(url, options = {}) {
    const token = localStorage.getItem('user_id');
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const response = await fetch(url, { ...options, headers });
    if (response.status === 401) { showModal('登录已过期，请重新登录。'); showScreen('auth-screen'); return null; }
    return response.json();
}
// (恢复) 获取需授权的文件 (封面/播放文件)
async function fetchAuthenticatedFile(url) {
    const token = localStorage.getItem('user_id');
    if (!token) return null;
    try {
        const response = await fetch(url, { headers: { 'Authorization': `Bearer ${token}` } });
        if (response.ok) {
            const blob = await response.blob();
            return URL.createObjectURL(blob);
        }
        return null;
    } catch (error) { console.error('Error fetching authenticated file:', error); return null; }
}

// (fetchMainFolders, fetchSubFolders 无变化)
async function fetchMainFolders() {
    mainFoldersList.innerHTML = `<li class="p-2 text-center text-gray-500"><div class="loader mx-auto"></div></li>`;
    const data = await fetchData('/get_main_folders');
    if (data && data.success) {
        mainFoldersList.innerHTML = '';
        data.folders.forEach(folder => {
            const li = document.createElement('li');
            li.innerHTML = `<button data-id="${folder.id}" class="main-folder-item flex items-center w-full p-3 rounded-lg hover:bg-gray-200"><svg class="h-5 w-5 text-gray-500 mr-3 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/></svg><span class="text-sm font-medium truncate">${folder.name}</span></button>`;
            mainFoldersList.appendChild(li);
        });
    } else { mainFoldersList.innerHTML = `<li class="p-2 text-red-500">加载失败</li>`; }
}
async function fetchSubFolders(parentId) {
    subFoldersList.innerHTML = `<li class="p-2 text-center text-gray-500"><div class="loader mx-auto"></div></li>`;
    const data = await fetchData(`/get_sub_folders/${parentId}`);
    if (data && data.success) {
        subFoldersList.innerHTML = '';
        data.folders.forEach(folder => {
            const li = document.createElement('li');
            li.innerHTML = `<button data-id="${folder.id}" class="sub-folder-item flex items-center w-full p-3 rounded-lg hover:bg-gray-200"><svg class="h-5 w-5 text-gray-500 mr-3 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/></svg><span class="text-sm font-medium truncate">${folder.name}</span></button>`;
            subFoldersList.appendChild(li);
        });
    } else { subFoldersList.innerHTML = `<li class="p-2 text-red-500">加载失败</li>`; }
}

async function fetchBookmarks(folderId) {
    bookmarksContainer.innerHTML = `<div class="w-full text-center py-8"><div class="loader mx-auto"></div></div>`;
    const data = await fetchData(`/get_bookmarks/${folderId}`);
    if (data && data.success) {
        bookmarksContainer.innerHTML = '';
        if (data.bookmarks.length === 0) {
            bookmarksContainer.innerHTML = `<div class="w-full text-center text-gray-500 mt-8">该文件夹下没有内容</div>`;
            return;
        }
        const fragment = document.createDocumentFragment();
        for (const bookmark of data.bookmarks) {
            const cardWrapper = document.createElement('div');
            cardWrapper.innerHTML = `
                <div class="bookmark-card bg-white rounded-xl shadow-lg hover:shadow-xl transition-shadow duration-300 overflow-hidden cursor-pointer h-full flex flex-col"
                    data-link="${bookmark.link}" data-title="${bookmark.title}" ${bookmark.filepath ? `data-filepath="${bookmark.filepath}"` : ''}>
                    <img src="${bookmark.cover}" alt="Cover" class="w-full h-32 object-cover pointer-events-none">
                    <div class="p-4 flex-grow flex flex-col pointer-events-none">
                        <h4 class="text-md font-semibold text-gray-800 truncate">${bookmark.title}</h4>
                        <p class="text-xs text-gray-500 mt-1 flex-grow">${bookmark.description || '无描述'}</p>
                        <p class="text-xs text-blue-500 mt-2 truncate">${bookmark.link}</p>
                    </div>
                </div>`;
            fragment.appendChild(cardWrapper);
        }
        bookmarksContainer.appendChild(fragment);
    } else {
        bookmarksContainer.innerHTML = `<div class="w-full text-center text-red-500 mt-8">加载失败</div>`;
    }
}

// --- 事件委托与处理 (更新) ---
document.addEventListener('click', (e) => {
    const mainFolder = e.target.closest('.main-folder-item');
    if (mainFolder) {
        currentMainFolderId = mainFolder.dataset.id;
        fetchSubFolders(currentMainFolderId);
        showSubFoldersView(); // 移动端切换视图
        document.querySelectorAll('.main-folder-item').forEach(el => el.classList.remove('bg-gray-200'));
        mainFolder.classList.add('bg-gray-200');
        return;
    }
    const subFolder = e.target.closest('.sub-folder-item');
    if (subFolder) {
        currentSubFolderId = subFolder.dataset.id;
        currentFolderName.textContent = subFolder.querySelector('span').textContent;
        fetchBookmarks(currentSubFolderId);
        recoveryTasks(currentSubFolderId);
        toggleMobileMenu(false); // 移动端选择后关闭菜单
        document.querySelectorAll('.sub-folder-item').forEach(el => el.classList.remove('bg-gray-200'));
        subFolder.classList.add('bg-gray-200');
        return;
    }
    const bookmarkCard = e.target.closest('.bookmark-card');
    if (bookmarkCard) {
        const { link, title, filepath } = bookmarkCard.dataset;
        if (filepath) { // (恢复) 优先处理播放
            showPlayerModal(title, filepath);
        } else if (link) {
            window.open(link, '_blank');
        }
    }
});

var currentSubFolderIdGlobal = ''; // 全局变量存储当前子文件夹ID

async function get_progress(taskIds) {
    if(currentSubFolderIdGlobal.length === 0){
        currentSubFolderIdGlobal = currentSubFolderId;
    }
    var task_success_count = 0;
    if(taskIds.length === 0){
        return;
    }
    var taskId = taskIds[task_success_count];
    const progressInterval = setInterval(async () => {
        const progressRes = await fetchData(`/get_progress/${taskId}`, {
            method: 'GET'
        });
        if(currentSubFolderIdGlobal !== currentSubFolderId){
            currentSubFolderIdGlobal = currentSubFolderId;
            clearInterval(progressInterval);
            return;
        }
        const progressData = await progressRes;
        if (progressData.success) {
            const { status, progress, message } = progressData;
            // 3. 任务完成或失败时停止轮询
            if (status === 'COMPLETED' || status === 'FAILED') {
                task_success_count++;
                taskId = taskIds[task_success_count];
                if (task_success_count == taskIds.length) {
                    clearInterval(progressInterval);
                }
                fetchBookmarks(currentSubFolderId);
            }
            if (taskId === taskIds[task_success_count] && task_success_count < taskIds.length - 1) {
                taskId = taskIds[task_success_count + 1];//下次查询第二个任务
            } else if (taskId === taskIds[task_success_count + 1] && task_success_count < taskIds.length - 2) {
                taskId = taskIds[task_success_count + 2];//再下次查询第三个任务
            } else if (taskId === taskIds[task_success_count + 2] && task_success_count < taskIds.length - 3) {
                taskId = taskIds[task_success_count + 3];//再下次查询第三个任务
            } else if (taskId === taskIds[task_success_count + 3] && task_success_count < taskIds.length - 4) {
                taskId = taskIds[task_success_count + 4];//再下次查询第三个任务
            } else if (taskId === taskIds[task_success_count + 4] && task_success_count < taskIds.length - 5) {
                taskId = taskIds[task_success_count + 5];//再下次查询第三个任务
            }else {
                taskId = taskIds[task_success_count];//最后查询第一个任务。直到有完成的任务。记录++
            }
            document.getElementById('download-message').innerHTML = message ? message.length > 20 ? message.substring(0, 20) + '...' : message : '';
            document.getElementById('download-progress').innerHTML = `(${task_success_count}/${taskIds.length})`;
        } else {
            if (task_success_count >= taskIds.length) {
                clearInterval(progressInterval);
                fetchBookmarks(currentSubFolderId);
            }else{
                task_success_count++;
                taskId = taskIds[task_success_count];
            }
            
            
        }
    }, 5000); // 每5秒轮询一次
}

async function recoveryTasks(currentSubFolderId) {
    const result = await fetchData('/recover_tasks', {
        method: 'POST',
        body: JSON.stringify({
            folder_id: currentSubFolderId
        })
    });
    if (result && result.success && result.task_ids.length > 0) {
        document.getElementById('download-status-panel').style.visibility = 'visible';
        get_progress(result.task_ids);
    }
}

// --- 新增功能逻辑 ---
function openModal(modalId) { document.getElementById(modalId).classList.replace('hidden', 'flex'); }
function closeModal(modalId) { document.getElementById(modalId).classList.replace('flex', 'hidden'); }
// (addFolder, addBookmark, crawUrl 函数无变化)
addFolderBtn.addEventListener('click', () => { if (!currentMainFolderId) { showModal('请先选择一个主文件夹。'); return; } addFolderForm.reset(); openModal('add-folder-modal'); });
addFolderForm.addEventListener('submit', async (e) => { e.preventDefault(); const folderName = document.getElementById('new-folder-name').value; const data = await fetchData('/create_folder', { method: 'POST', body: JSON.stringify({ name: folderName, parent_id: currentMainFolderId, user_id: currentUserId }) }); if (data && data.success) { closeModal('add-folder-modal'); fetchSubFolders(currentMainFolderId); } else { showModal(data.message || '创建失败'); } });
addBookmarkBtn.addEventListener('click', () => { if (!currentSubFolderId) { showModal('请先选择一个子文件夹。'); return; } addBookmarkForm.reset(); coverPreview.classList.add('hidden'); document.getElementById('add-bookmark-message').textContent = ''; openModal('add-bookmark-modal'); });
newBookmarkCoverInput.addEventListener('change', () => { const file = newBookmarkCoverInput.files[0]; if (file) { const reader = new FileReader(); reader.onload = e => { coverPreview.src = e.target.result; coverPreview.classList.remove('hidden'); }; reader.readAsDataURL(file); } });
async function uploadFile(url, file) { const formData = new FormData(); formData.append('file', file); const token = localStorage.getItem('user_id'); const response = await fetch(url, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: formData }); return response.json(); }
addBookmarkForm.addEventListener('submit', async (e) => { e.preventDefault(); const submitBtn = document.getElementById('add-bookmark-submit-btn'); const messageEl = document.getElementById('add-bookmark-message'); submitBtn.disabled = true; submitBtn.innerHTML = `<div class="loader !w-5 !h-5 !border-2 mr-2"></div> 处理中...`; messageEl.textContent = ''; try { let coverId = null; const coverFile = document.getElementById('new-bookmark-cover').files[0]; if (coverFile) { const coverResult = await uploadFile('/upload_cover', coverFile); if (!coverResult.success) throw new Error(coverResult.message || '封面上传失败'); coverId = coverResult.file_id; } let fileId = null; const contentFile = document.getElementById('new-bookmark-file').files[0]; if (contentFile) { const fileResult = await uploadFile('/upload_file', contentFile); if (!fileResult.success) throw new Error(fileResult.message || '文件上传失败'); fileId = fileResult.file_id; } const bookmarkData = { title: document.getElementById('new-bookmark-title').value, description: document.getElementById('new-bookmark-desc').value, link: document.getElementById('new-bookmark-link').value, folder_id: currentSubFolderId, cover: coverId, file_id: fileId, user_id: currentUserId }; const addResult = await fetchData('/add_bookmark', { method: 'POST', body: JSON.stringify(bookmarkData) }); if (addResult && addResult.success) { closeModal('add-bookmark-modal'); fetchBookmarks(currentSubFolderId); } else { throw new Error(addResult.message || '创建内容失败'); } } catch (error) { messageEl.textContent = error.message; } finally { submitBtn.disabled = false; submitBtn.innerHTML = '创建'; } });
crawlUrlBtn.addEventListener('click', () => { if (!currentSubFolderId) { showModal('请先选择一个子文件夹。'); return; } crawlUrlForm.reset(); document.getElementById('crawl-url-message').textContent = ''; openModal('crawl-url-modal'); });

crawlUrlForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = document.getElementById('crawl-url-submit-btn');
    const messageEl = document.getElementById('crawl-url-message');
    const link = document.getElementById('crawl-url-link').value;
    submitBtn.disabled = true;
    submitBtn.innerHTML = `<div class="loader !w-5 !h-5 !border-2 mr-2"></div> 抓取中...`;
    messageEl.textContent = '';
    try {
        const result = await fetchData('/craw_url', {
            method: 'POST',
            body: JSON.stringify({
                folder_id: currentSubFolderId,
                link: link
            })
        });
        if (result && result.success) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '抓取';
            closeModal('crawl-url-modal');
            document.getElementById('download-status-panel').style.visibility = 'visible';

            get_progress(result.task_ids ? result.task_ids : [result.task_id]);
        } else {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '抓取';
            messageEl.textContent = result.message || '抓取失败';
        }
    } catch (error) {
        messageEl.textContent = error.message;
    }
});

// (恢复) 播放器逻辑
function showPlayerModal(title, filepath) {
    playerModalTitle.textContent = title;
    playerContainer.innerHTML = `<div class="loader mx-auto my-8"></div>`;
    openModal('player-modal');
    const extension = filepath.split('.').pop().toLowerCase();
    let playerElement = '';
    if (['mp4', 'webm', 'ogg'].includes(extension)) { playerElement = `<video src="${filepath}" controls autoplay class="w-full max-h-[70vh] rounded-lg"></video>`; }
    else if (['mp3', 'wav', 'flac'].includes(extension)) { playerElement = `<audio src="${filepath}" controls autoplay class="w-full"></audio>`; }
    else { playerElement = `<p class="text-center text-red-500">不支持的文件类型。</p>`; URL.revokeObjectURL(filepath); }
    playerContainer.innerHTML = playerElement;
}
function hidePlayerModal() {
    const mediaElement = playerContainer.querySelector('video, audio');
    if (mediaElement && mediaElement.src.startsWith('blob:')) { URL.revokeObjectURL(mediaElement.src); }
    playerContainer.innerHTML = '';
    closeModal('player-modal');
}
playerModalCloseBtn.addEventListener('click', hidePlayerModal);

// --- 其他辅助函数 (登录、视图切换等) ---
const toggleAuthBtn = document.getElementById('toggle-auth'); const authTitle = document.getElementById('auth-title'); const authButton = document.getElementById('auth-button'); const authMessage = document.getElementById('auth-message'); const logoutBtn = document.getElementById('logout-btn'); const gridViewBtn = document.getElementById('grid-view-btn'); const listViewBtn = document.getElementById('list-view-btn'); let isLoginMode = true;
function showModal(message) { document.getElementById('modal-message').textContent = message; openModal('message-modal'); }
document.getElementById('modal-close-btn').addEventListener('click', () => closeModal('message-modal'));
toggleAuthBtn.addEventListener('click', (e) => { e.preventDefault(); isLoginMode = !isLoginMode; authTitle.textContent = isLoginMode ? '登录' : '注册'; authButton.textContent = isLoginMode ? '登录' : '注册'; toggleAuthBtn.textContent = isLoginMode ? '没有账户？去注册' : '已有账户？去登录'; authMessage.textContent = ''; });
function showScreen(screenId) { if (screenId === 'auth-screen') { authScreen.style.display = 'block'; appScreen.classList.add('hidden'); } else { authScreen.style.display = 'none'; appScreen.classList.remove('hidden'); } }
authForm.addEventListener('submit', async (e) => { e.preventDefault(); const email = document.getElementById('email').value; const password = document.getElementById('password').value; const url = isLoginMode ? '/login' : '/register'; authButton.innerHTML = `<div class="loader mx-auto"></div>`; authButton.disabled = true; try { const response = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) }); const result = await response.json(); authMessage.textContent = ''; if (result.success) { currentUserId = result.user_id; localStorage.setItem('user_id', currentUserId); showScreen('app-screen'); fetchMainFolders(); } else { authMessage.textContent = result.message || '操作失败'; authMessage.classList.add('text-red-500'); } } catch (error) { authMessage.textContent = '网络错误，请稍后再试。'; authMessage.classList.add('text-red-500'); } finally { authButton.textContent = isLoginMode ? '登录' : '注册'; authButton.disabled = false; } });
logoutBtn.addEventListener('click', () => { localStorage.removeItem('user_id'); currentUserId = null; showScreen('auth-screen'); });
gridViewBtn.addEventListener('click', () => { bookmarksContainer.classList.replace('flex-col', 'grid'); bookmarksContainer.classList.add('sm:grid-cols-2', 'lg:grid-cols-3'); });
listViewBtn.addEventListener('click', () => { bookmarksContainer.classList.replace('grid', 'flex-col'); bookmarksContainer.classList.remove('sm:grid-cols-2', 'lg:grid-cols-3'); });

init(); // 启动应用