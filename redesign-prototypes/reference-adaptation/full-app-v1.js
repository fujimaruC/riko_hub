/**
 * RIKO HUB · FULL APP PREVIEW RUNTIME (M-CHEF ADAPTATION)
 * State management, screen navigation, interactive deck switching & contextual sheets.
 */

(function () {
  'use strict';

  // Application State
  const state = {
    currentScreen: 'home',
    theme: 'dark',
    activeSpace: 'academics',
    activeSheetTab: 'task'
  };

  // DOM Elements Cache
  const screens = document.querySelectorAll('.screen');
  const routeButtons = document.querySelectorAll('.route-list button');
  const dockButtons = document.querySelectorAll('.dock-nav-item');
  const toastEl = document.getElementById('toast');
  const sheetBackdrop = document.getElementById('sheet-backdrop');
  const contextSheet = document.getElementById('context-bottom-sheet');
  const sheetTitle = document.getElementById('sheet-title');
  const sheetKicker = document.getElementById('sheet-kicker');
  const sheetTabs = document.querySelectorAll('.sheet-tab');
  const sheetForms = document.querySelectorAll('.sheet-inner-form');

  // Spaces Hero Data Configurations (Interactive Deck)
  const SPACES_DATA = {
    academics: {
      kicker: '01 / AKADEMIK',
      title: 'KAMPUS<br>BERGERAK',
      badges: [
        { label: '12 SKS Aktif', dot: 'dot-red-full' },
        { label: '4 Mata Kuliah', dot: 'dot-blue-full' }
      ],
      bg: 'var(--butter)',
      color: 'var(--ink)',
      btnBg: '#1A1B18',
      btnColor: '#FFFFFF',
      buttonText: 'MASUK RUANG AKADEMIK',
      route: 'academics'
    },
    projects: {
      kicker: '02 / PROYEK',
      title: 'KERJA<br>BERGERAK',
      badges: [
        { label: 'Sprint 42 Aktif', dot: 'dot-red-full' },
        { label: '3 Deliverables', dot: 'dot-blue-full' }
      ],
      bg: 'var(--coral)',
      color: '#FFFFFF',
      btnBg: '#FFFFFF',
      btnColor: 'var(--ink)',
      buttonText: 'MASUK RUANG PROYEK',
      route: 'projects'
    },
    personal: {
      kicker: '03 / PERSONAL',
      title: 'JAGA<br>RITMEMU',
      badges: [
        { label: '2 Tugas Kecil', dot: 'dot-red-full' },
        { label: 'Jurnal Hari Ini', dot: 'dot-blue-full' }
      ],
      bg: '#1A1B18',
      color: '#FFFFFF',
      btnBg: 'var(--lime)',
      btnColor: 'var(--ink)',
      buttonText: 'MASUK RUANG PERSONAL',
      route: 'personal'
    },
    devices: {
      kicker: '04 / PERANGKAT',
      title: 'JEMBATAN<br>KERJA',
      badges: [
        { label: 'LAN Siaga', dot: 'dot-red-full' },
        { label: 'ThinkPad Terhubung', dot: 'dot-blue-full' }
      ],
      bg: 'var(--cyan-mint)',
      color: 'var(--ink)',
      btnBg: '#1A1B18',
      btnColor: '#FFFFFF',
      buttonText: 'MASUK RUANG PERANGKAT',
      route: 'devices'
    }
  };

  /**
   * Toast helper notification
   */
  let toastTimer = null;
  function showToast(message) {
    if (!toastEl) return;
    toastEl.textContent = message;
    toastEl.classList.add('is-visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toastEl.classList.remove('is-visible');
    }, 2400);
  }

  /**
   * Navigate to screen by ID
   */
  function navigateTo(targetId) {
    if (!targetId) return;
    const targetScreen = document.getElementById(targetId);
    if (!targetScreen) return;

    state.currentScreen = targetId;

    // Toggle screen visibility
    screens.forEach(screen => {
      if (screen.id === targetId) {
        screen.classList.add('is-active');
        screen.removeAttribute('hidden');
      } else {
        screen.classList.remove('is-active');
        screen.setAttribute('hidden', '');
      }
    });

    // Update left sidebar buttons
    routeButtons.forEach(btn => {
      if (btn.dataset.go === targetId) {
        btn.setAttribute('aria-current', 'page');
      } else {
        btn.removeAttribute('aria-current');
      }
    });

    // Update bottom dock buttons
    dockButtons.forEach(btn => {
      if (btn.dataset.go === targetId) {
        btn.classList.add('is-selected');
        btn.setAttribute('aria-current', 'page');
      } else {
        btn.classList.remove('is-selected');
        btn.removeAttribute('aria-current');
      }
    });

    // Scroll phone stage to top
    const stack = document.getElementById('screen-stack');
    if (stack) stack.scrollTop = 0;

    // Update location hash without pushing browser history
    if (window.location.hash.slice(1) !== targetId) {
      history.replaceState(null, '', '#' + targetId);
    }
  }

  /**
   * Switch Space in Stepped Deck (#spaces)
   */
  function switchSpace(spaceKey) {
    const data = SPACES_DATA[spaceKey];
    if (!data) return;

    state.activeSpace = spaceKey;
    const heroCard = document.getElementById('spaces-hero-card');
    if (!heroCard) return;

    // Apply color and layout changes
    heroCard.style.background = data.bg;
    heroCard.style.color = data.color;

    // Update Head
    const kicker = document.getElementById('hero-kicker');
    if (kicker) kicker.textContent = data.kicker;
    const title = document.getElementById('hero-title');
    if (title) title.innerHTML = data.title;

    // Update Action button destination
    const actionCircle = document.getElementById('hero-action-circle');
    if (actionCircle) actionCircle.dataset.go = data.route;

    // Update Badges
    const badgeContainer = document.getElementById('hero-badges-container');
    if (badgeContainer) {
      badgeContainer.innerHTML = data.badges.map(b => `
        <span class="cat-badge-pill">
          <i class="${b.dot}"></i> ${b.label}
        </span>
      `).join('');
    }

    // Update Main Action Button
    const enterBtn = document.getElementById('hero-enter-btn');
    if (enterBtn) {
      enterBtn.dataset.go = data.route;
      enterBtn.style.background = data.btnBg;
      enterBtn.style.color = data.btnColor;
      const textSpan = enterBtn.querySelector('span:first-child');
      if (textSpan) textSpan.textContent = data.buttonText;
    }

    // Update quick tabs
    document.querySelectorAll('.space-quick-btn').forEach(btn => {
      if (btn.dataset.switchSpace === spaceKey) {
        btn.classList.add('is-active');
      } else {
        btn.classList.remove('is-active');
      }
    });

    showToast(`Beralih ke: ${data.kicker}`);
  }

  /**
   * Contextual Bottom Sheet Drawer Handling
   */
  function openContextSheet(initialContext = 'task') {
    if (!sheetBackdrop || !contextSheet) return;

    state.activeSheetTab = initialContext;
    switchSheetTab(initialContext);

    sheetBackdrop.removeAttribute('hidden');
    contextSheet.removeAttribute('hidden');

    // Auto-focus first input of active form
    const activeForm = document.querySelector(`.sheet-inner-form[data-context="${initialContext}"]`);
    if (activeForm) {
      const input = activeForm.querySelector('input:not([type="radio"]), textarea');
      if (input) setTimeout(() => input.focus(), 100);
    }
  }

  function closeContextSheet() {
    if (!sheetBackdrop || !contextSheet) return;
    sheetBackdrop.setAttribute('hidden', '');
    contextSheet.setAttribute('hidden', '');
  }

  function switchSheetTab(tabKey) {
    state.activeSheetTab = tabKey;

    // Update Tab UI
    sheetTabs.forEach(tab => {
      const isMatch = tab.dataset.sheetTab === tabKey;
      tab.classList.toggle('is-active', isMatch);
      tab.setAttribute('aria-selected', isMatch ? 'true' : 'false');
    });

    // Update Titles
    const TITLES = {
      task: { kicker: 'ENTRI BARU', title: 'TAMBAH TUGAS' },
      event: { kicker: 'BLOK WAKTU', title: 'JADWALKAN AGENDA' },
      course: { kicker: 'MATA KULIAH', title: 'TAMBAH KULIAH' },
      note: { kicker: 'CATATAN CEPAT', title: 'TULIS GAGASAN' }
    };
    if (TITLES[tabKey]) {
      if (sheetKicker) sheetKicker.textContent = TITLES[tabKey].kicker;
      if (sheetTitle) sheetTitle.textContent = TITLES[tabKey].title;
    }

    // Show Matching Form
    sheetForms.forEach(form => {
      if (form.dataset.context === tabKey) {
        form.removeAttribute('hidden');
      } else {
        form.setAttribute('hidden', '');
      }
    });
  }

  // Priority Radio pill button clicks
  document.querySelectorAll('.priority-btn').forEach(btn => {
    btn.addEventListener('click', function () {
      const parent = this.closest('.priority-pill-buttons');
      if (!parent) return;
      parent.querySelectorAll('.priority-btn').forEach(b => b.classList.remove('is-selected'));
      this.classList.add('is-selected');
      const radio = this.querySelector('input[type="radio"]');
      if (radio) radio.checked = true;
    });
  });

  // Sheet Tab click handlers
  sheetTabs.forEach(tab => {
    tab.addEventListener('click', function () {
      switchSheetTab(this.dataset.sheetTab);
    });
  });

  // Sheet backdrop & close handlers
  if (sheetBackdrop) {
    sheetBackdrop.addEventListener('click', closeContextSheet);
  }

  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && contextSheet && !contextSheet.hasAttribute('hidden')) {
      closeContextSheet();
    }
  });

  // Form Submission Handlers
  const formTask = document.getElementById('form-task');
  if (formTask) {
    formTask.addEventListener('submit', function (e) {
      e.preventDefault();
      const title = this.title.value.trim();
      const priority = this.priority.value;
      if (!title) return;

      closeContextSheet();
      this.reset();
      showToast(`Tugas disimpan: "${title}" (${priority})`);
    });
  }

  const formEvent = document.getElementById('form-event');
  if (formEvent) {
    formEvent.addEventListener('submit', function (e) {
      e.preventDefault();
      const title = this.title.value.trim() || 'Agenda Baru';
      const start = this.startTime.value;
      closeContextSheet();
      this.reset();
      showToast(`Agenda dijadwalkan: ${title} (${start})`);
    });
  }

  const formCourse = document.getElementById('form-course');
  if (formCourse) {
    formCourse.addEventListener('submit', function (e) {
      e.preventDefault();
      const title = this.title.value.trim() || 'Mata Kuliah';
      const sks = this.sks.value;
      closeContextSheet();
      this.reset();
      showToast(`Mata kuliah disimpan: ${title} (${sks} SKS)`);
    });
  }

  const formNote = document.getElementById('form-note');
  if (formNote) {
    formNote.addEventListener('submit', function (e) {
      e.preventDefault();
      const title = this.title.value.trim() || 'Catatan Baru';
      closeContextSheet();
      this.reset();
      showToast(`Catatan disimpan: ${title}`);
    });
  }

  /**
   * Global Click Delegation
   */
  document.addEventListener('click', function (e) {
    // Navigation Targets
    const goBtn = e.target.closest('[data-go]');
    if (goBtn) {
      e.preventDefault();
      navigateTo(goBtn.dataset.go);
      return;
    }

    // Space Switcher in Deck
    const spaceSwitchBtn = e.target.closest('[data-switch-space]');
    if (spaceSwitchBtn) {
      e.preventDefault();
      switchSpace(spaceSwitchBtn.dataset.switchSpace);
      return;
    }

    // Context Sheet Triggers
    const openSheetBtn = e.target.closest('[data-action="open-context-sheet"]');
    if (openSheetBtn) {
      e.preventDefault();
      const context = openSheetBtn.dataset.sheet || 'task';
      openContextSheet(context);
      return;
    }

    const closeSheetBtn = e.target.closest('[data-action="close-sheet"]');
    if (closeSheetBtn) {
      e.preventDefault();
      closeContextSheet();
      return;
    }

    // Complete Task Action
    const completeBtn = e.target.closest('[data-action="complete-task"]');
    if (completeBtn) {
      e.stopPropagation();
      const isChecked = completeBtn.classList.toggle('checked');
      completeBtn.textContent = isChecked ? '✓' : '';
      const taskRow = completeBtn.closest('.task-row-card, .task-card-item');
      if (taskRow) {
        taskRow.classList.toggle('is-done', isChecked);
      }
      showToast(isChecked ? 'Tugas ditandai selesai!' : 'Tugas dikembalikan aktif');
      return;
    }

    // Filter Chips Toggle
    const filterBtn = e.target.closest('[data-action="toggle-filter"]');
    if (filterBtn) {
      const parent = filterBtn.closest('.filter-chips-list');
      if (parent) {
        parent.querySelectorAll('.pill-chip').forEach(b => b.classList.remove('is-selected'));
        filterBtn.classList.add('is-selected');
        const filterVal = filterBtn.dataset.filter || 'semua';
        showToast(`Filter: ${filterVal.toUpperCase()}`);
      }
      return;
    }

    // Suggest Prompt Chip in Capture
    const promptChip = e.target.closest('[data-action="suggest-prompt"]');
    if (promptChip) {
      const text = promptChip.dataset.text;
      const input = document.getElementById('quick-prompt-input');
      if (input && text) {
        input.value = text;
        input.focus();
        showToast('Ide disematkan ke input prompt');
      }
      return;
    }

    // Save Quick Prompt
    const savePromptBtn = e.target.closest('[data-action="save-quick-prompt"]');
    if (savePromptBtn) {
      const input = document.getElementById('quick-prompt-input');
      if (input && input.value.trim()) {
        const val = input.value.trim();
        input.value = '';
        showToast(`Catatan cepat ditangkap: "${val}"`);
      } else {
        showToast('Tulis ide sebelum menyimpan');
      }
      return;
    }

    // Personal Journal Draft & History
    const saveJournal = e.target.closest('[data-action="save-journal"]');
    if (saveJournal) {
      const txt = document.getElementById('personal-journal-input');
      if (txt && txt.value.trim()) {
        showToast('Draf jurnal hari ini disimpan secara lokal');
      } else {
        showToast('Tulis jurnal terlebih dahulu');
      }
      return;
    }

    const toggleHistory = e.target.closest('[data-action="toggle-journal-history"]');
    if (toggleHistory) {
      const historyList = document.getElementById('personal-journal-history');
      if (historyList) {
        const isHidden = historyList.hasAttribute('hidden');
        if (isHidden) {
          historyList.removeAttribute('hidden');
        } else {
          historyList.setAttribute('hidden', '');
        }
      }
      return;
    }

    // Theme Switch
    const themeBtn = e.target.closest('[data-action="theme"]');
    if (themeBtn) {
      state.theme = state.theme === 'dark' ? 'light' : 'dark';
      document.body.setAttribute('data-theme', state.theme);
      showToast(`Tema: ${state.theme === 'dark' ? 'Arang Gelap' : 'Gading Terang'}`);
      return;
    }

    // Generic feedback
    const feedbackBtn = e.target.closest('[data-action="feedback"]');
    if (feedbackBtn) {
      showToast('Pratinjau antarmuka aktif');
      return;
    }
  });

  // Search filter typing handler on tasks
  const taskSearchInput = document.getElementById('task-search-input');
  if (taskSearchInput) {
    taskSearchInput.addEventListener('input', function () {
      const q = this.value.toLowerCase().trim();
      const taskCards = document.querySelectorAll('#tasks .task-card-item');
      taskCards.forEach(card => {
        const text = card.textContent.toLowerCase();
        card.style.display = (!q || text.includes(q)) ? 'flex' : 'none';
      });
    });
  }

  // Handle Initial Hash on Load
  window.addEventListener('DOMContentLoaded', () => {
    const hash = window.location.hash.slice(1);
    if (hash && document.getElementById(hash)) {
      navigateTo(hash);
    } else {
      navigateTo('home');
    }
  });

  window.addEventListener('hashchange', () => {
    const hash = window.location.hash.slice(1);
    if (hash && document.getElementById(hash) && hash !== state.currentScreen) {
      navigateTo(hash);
    }
  });

})();
