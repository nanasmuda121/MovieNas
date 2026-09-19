/**
 * STREAMBOX - Core Shared App Script
 * Handles Navigation, Watchlist, Continue Watching, Toasts & Utilities
 */

const StreamBoxApp = {
  // 1. Initialize Global UI
  init() {
    this.initNavbar();
    this.initSearchInput();
    this.initMobileMenu();
    this.updateWatchlistBadge();
  },

  // Navbar scroll background transition
  initNavbar() {
    const navbar = document.querySelector('.navbar');
    if (!navbar) return;

    window.addEventListener('scroll', () => {
      if (window.scrollY > 40) {
        navbar.classList.add('scrolled');
      } else {
        navbar.classList.remove('scrolled');
      }
    });

    if (window.scrollY > 40) {
      navbar.classList.add('scrolled');
    }
  },

  // Navbar & Global Search
  initSearchInput() {
    const navSearch = document.getElementById('navSearchInput');
    if (!navSearch) return;

    navSearch.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const query = navSearch.value.trim();
        if (query) {
          window.location.href = `/search.html?q=${encodeURIComponent(query)}`;
        }
      }
    });

    // Keyboard shortcut '/' to quickly focus search
    window.addEventListener('keydown', (e) => {
      if (e.key === '/' && document.activeElement !== navSearch && document.activeElement.tagName !== 'INPUT') {
        e.preventDefault();
        navSearch.focus();
      }
    });
  },

  // Mobile Menu Toggle
  initMobileMenu() {
    const toggleBtn = document.getElementById('menuToggleBtn');
    const navLinks = document.getElementById('navLinks');
    if (!toggleBtn || !navLinks) return;

    toggleBtn.addEventListener('click', () => {
      navLinks.classList.toggle('mobile-open');
    });
  },

  // ==========================================
  // WATCHLIST / FAVORIT (localStorage)
  // ==========================================
  getWatchlist() {
    try {
      return JSON.parse(localStorage.getItem('movienas_watchlist') || '[]');
    } catch {
      return [];
    }
  },

  isWatchlisted(detailPath) {
    if (!detailPath) return false;
    const list = this.getWatchlist();
    return list.some((item) => item.detailPath === detailPath);
  },

  toggleWatchlist(item) {
    let list = this.getWatchlist();
    const index = list.findIndex((i) => i.detailPath === item.detailPath);

    if (index > -1) {
      list.splice(index, 1);
      this.showToast(`Dihapus dari Watchlist: ${item.title}`);
    } else {
      list.unshift({
        detailPath: item.detailPath,
        subjectId: item.subjectId,
        title: item.title,
        coverUrl: item.coverUrl,
        typeLabel: item.typeLabel,
        imdbRating: item.imdbRating,
        year: item.year,
        addedAt: Date.now(),
      });
      this.showToast(`Ditambahkan ke Watchlist: ${item.title}`);
    }

    localStorage.setItem('movienas_watchlist', JSON.stringify(list));
    this.updateWatchlistBadge();
    window.dispatchEvent(new CustomEvent('watchlist-updated'));
    return index === -1; // true if added
  },

  updateWatchlistBadge() {
    const badges = document.querySelectorAll('.badge-count');
    const count = this.getWatchlist().length;
    badges.forEach((b) => {
      b.textContent = count;
      b.style.display = count > 0 ? 'inline-block' : 'none';
    });
  },

  // ==========================================
  // CONTINUE WATCHING / RIWAYAT (localStorage)
  // ==========================================
  getHistory() {
    try {
      return JSON.parse(localStorage.getItem('movienas_history') || '[]');
    } catch {
      return [];
    }
  },

  saveHistory(record) {
    if (!record || !record.detailPath) return;
    let list = this.getHistory();
    list = list.filter((i) => i.detailPath !== record.detailPath);

    list.unshift({
      detailPath: record.detailPath,
      subjectId: record.subjectId || '',
      title: record.title || '',
      coverUrl: record.coverUrl || '',
      isMovie: Boolean(record.isMovie),
      season: Number(record.season) || 0,
      episode: Number(record.episode) || 0,
      currentTime: Number(record.currentTime) || 0,
      duration: Number(record.duration) || 0,
      percent: Math.min(100, Math.round(((record.currentTime || 0) / (record.duration || 1)) * 100)),
      updatedAt: Date.now(),
    });

    // Keep max 30 history items
    if (list.length > 30) list = list.slice(0, 30);
    localStorage.setItem('movienas_history', JSON.stringify(list));
    window.dispatchEvent(new CustomEvent('history-updated'));
  },

  getHistoryItem(detailPath) {
    const list = this.getHistory();
    return list.find((i) => i.detailPath === detailPath) || null;
  },

  clearHistory() {
    localStorage.removeItem('movienas_history');
    window.dispatchEvent(new CustomEvent('history-updated'));
  },

  // ==========================================
  // UI UTILITIES & TOAST
  // ==========================================
  showToast(message, icon = 'fa-check') {
    let container = document.getElementById('toastContainer');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toastContainer';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `<i class="fa-solid ${icon} text-red-500"></i><span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  },

  // Format seconds to mm:ss or hh:mm:ss
  formatTime(seconds) {
    const s = Math.floor(Number(seconds) || 0);
    const hrs = Math.floor(s / 3600);
    const mins = Math.floor((s % 3600) / 60);
    const secs = s % 60;

    const pad = (n) => String(n).padStart(2, '0');
    if (hrs > 0) {
      return `${pad(hrs)}:${pad(mins)}:${pad(secs)}`;
    }
    return `${pad(mins)}:${pad(secs)}`;
  },

  // Create card element for a Movie/Series item
  createPosterCard(item) {
    const isSeries = item.typeLabel === 'Series' || item.subjectType === 2;
    const badgeTypeClass = isSeries ? 'badge-type-series' : 'badge-type-movie';
    const detailUrl = `/detail.html?path=${encodeURIComponent(item.detailPath)}`;
    const playUrl = `/player.html?path=${encodeURIComponent(item.detailPath)}`;

    const fallbackImg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='300' viewBox='0 0 200 300'%3E%3Crect width='200' height='300' fill='%23171722'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%2364748b' font-family='sans-serif' font-size='14'%3ENo Poster%3C/text%3E%3C/svg%3E";

    return `
      <div class="card-poster" onclick="window.location.href='${detailUrl}'">
        <div class="card-image-wrap">
          <img src="${item.coverUrl || fallbackImg}" alt="${item.title}" loading="lazy" onerror="this.src='${fallbackImg}'" />
          
          <div class="card-badge-top">
            <span class="badge ${badgeTypeClass}">${isSeries ? 'Series' : 'Movie'}</span>
          </div>

          ${item.imdbRating && item.imdbRating !== '0.0' ? `
            <div class="card-rating-badge">
              <i class="fa-solid fa-star"></i>
              <span>${item.imdbRating}</span>
            </div>
          ` : ''}

          <div class="card-overlay-hover">
            <div class="card-hover-play" onclick="event.stopPropagation(); window.location.href='${playUrl}'" title="Putar Langsung">
              <i class="fa-solid fa-play"></i>
            </div>
          </div>
        </div>

        <div class="card-info">
          <h3 class="card-title" title="${item.title}">${item.title}</h3>
          <div class="card-meta">
            <span>${item.year || (item.releaseDate ? item.releaseDate.split('-')[0] : '') || 'N/A'}</span>
            <span>${item.durationFormatted || (isSeries ? 'Episodes' : '')}</span>
          </div>
        </div>
      </div>
    `;
  },
};

document.addEventListener('DOMContentLoaded', () => {
  StreamBoxApp.init();
});
