/**
 * - Белый Дом — client script
 * - pages: home · catalog · product · cart · checkout · success · auth
 * - cart / fav → localStorage · orders → POST /api/orders
 */
(() => {
    'use strict';

    /* ========================= helpers ========================= */

    const $ = (id) => document.getElementById(id);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const esc = (s) =>
        String(s ?? '').replace(/[&<>"']/g, (c) =>
            ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])
        );

    const rub = (n) => Number(n).toLocaleString('ru-RU') + ' ₽';
    const fmt = (n) => Number(n).toLocaleString('ru-RU');

    const plural = (n, one, few, many) => {
        const a = n % 10;
        const b = n % 100;
        if (a === 1 && b !== 11) return one;
        if (a >= 2 && a <= 4 && (b < 10 || b >= 20)) return few;
        return many;
    };

    const storage = {
        get(key, fallback) {
            try {
                const v = JSON.parse(localStorage.getItem(key) || 'null');
                return v == null ? fallback : v;
            } catch {
                return fallback;
            }
        },
        set(key, value) {
            try {
                localStorage.setItem(key, JSON.stringify(value));
            } catch { /* private mode / quota */ }
        },
    };

    const notifyStorage = () => window.dispatchEvent(new Event('storage'));

    /* ========================= cart ========================= */

    function readCart() {
        const raw = storage.get('cart', []);
        if (!Array.isArray(raw)) return [];
        const cart = [];
        for (const it of raw) {
            const ex = cart.find(
                (c) =>
                    String(c.id) === String(it.id) &&
                    c.width === it.width &&
                    c.finish === it.finish
            );
            const qty = Number(it.qty) || 1;
            if (ex) ex.qty += qty;
            else cart.push({ ...it, qty });
        }
        return cart;
    }

    function writeCart(cart) {
        storage.set('cart', cart);
        notifyStorage();
    }

    function cartQty(cart = readCart()) {
        return cart.reduce((s, c) => s + (Number(c.qty) || 1), 0);
    }

    /* ========================= 1. chrome ========================= */

    function initChrome() {
        const burger = $('burger');
        const nav = $('mainNav');

        if (burger && nav) {
            const isOpen = () =>
                nav.classList.contains('is-open') || nav.classList.contains('open');
            const setOpen = (open) => {
                nav.classList.toggle('is-open', open);
                nav.classList.toggle('open', open);
                burger.setAttribute('aria-expanded', String(open));
                burger.setAttribute('aria-label', open ? 'Закрыть меню' : 'Открыть меню');
            };

            burger.addEventListener('click', () => setOpen(!isOpen()));
            $$('a', nav).forEach((a) => a.addEventListener('click', () => setOpen(false)));
            document.addEventListener('click', (e) => {
                if (isOpen() && !nav.contains(e.target) && !burger.contains(e.target)) {
                    setOpen(false);
                }
            });
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') setOpen(false);
            });
            window.addEventListener('resize', () => {
                if (window.innerWidth > 900) setOpen(false);
            });
        }

        const toTop = $('toTop');
        if (toTop) {
            toTop.addEventListener('click', (e) => {
                e.preventDefault();
                window.scrollTo({ top: 0, behavior: 'smooth' });
            });
        }

        const badges = $$('[data-cart-count]');
        if (!badges.length) return;

        const updateBadge = () => {
            const n = cartQty();
            badges.forEach((b) => {
                b.textContent = n > 99 ? '99+' : String(n);
                b.hidden = !n;
            });
        };

        updateBadge();
        window.addEventListener('pageshow', updateBadge);
        window.addEventListener('storage', updateBadge);
    }

    /* ========================= 2. global search ========================= */

    function initGlobalSearch() {
        if ($('grid') && $('filters')) return;

        $$('form[role="search"]').forEach((form) => {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                const input = form.querySelector('input[type="search"], input[name="q"]');
                const q = input ? input.value.trim() : '';
                location.href = '/catalog' + (q ? '?q=' + encodeURIComponent(q) : '');
            });
        });
    }

    /* ========================= 3. catalog ========================= */

    function initCatalog() {
        const gridEl = $('grid');
        const filtersEl = $('filters');
        if (!gridEl || !filtersEl) return;

        const pageTitleEl = $('pageTitle');
        const crumbCatEl = $('crumbCat');
        const searchInputEl = $('searchInput');
        const foundEl = $('found');
        const fApplyEl = $('fApply');
        const favLinkEl = $('favLink');
        const pagerEl = $('pager');
        const rMin = $('rMin');
        const rMax = $('rMax');
        const pMin = $('pMin');
        const pMax = $('pMax');
        const sortEl = $('sort');
        const searchForm = $('searchForm');
        const fOpen = $('fOpen');
        const fClose = $('fClose');
        const viewButtons = $$('.vt');
        const filterCheckboxes = $$('.f-group input[type=checkbox]');

        const params = new URLSearchParams(location.search);
        const category = params.get('category');
        const initialQ = (params.get('q') || '').trim();
        const categoryNav = document.querySelector('.cnav-inner');

        if (categoryNav) {
            categoryNav.addEventListener('wheel', (event) => {
                const maxScroll = categoryNav.scrollWidth - categoryNav.clientWidth;
                if (maxScroll <= 0) return;
                const delta = Math.abs(event.deltaY) > Math.abs(event.deltaX)
                    ? event.deltaY
                    : event.deltaX;
                const next = Math.max(0, Math.min(maxScroll, categoryNav.scrollLeft + delta));
                if (next !== categoryNav.scrollLeft) {
                    categoryNav.scrollLeft = next;
                    event.preventDefault();
                }
            }, { passive: false });
        }

        const BGS = [
            '#d9d0c3', '#c5b09a', '#dfd8cc', '#a8a39b', '#d6d4ce',
            '#928f89', '#cdb9a3', '#7c7f72', '#ddd4cb',
        ];

        $$('.cnav a[data-cat]').forEach((a) => {
            a.classList.toggle('on', (a.dataset.cat || '') === (category || ''));
        });

        let all = [];
        let loaded = false;
        const st = {
            page: 1,
            per: 9,
            sort: 'pop',
            q: initialQ.toLowerCase(),
            qRaw: initialQ,
        };
        const rawFavorites = storage.get('fav', []);
        let favorites = Array.isArray(rawFavorites) ? rawFavorites : [];

        if (searchInputEl) {
            searchInputEl.value = initialQ;
            searchInputEl.name = 'q';
        }

        const setHeading = () => {
            const title = category || (st.q ? 'Поиск: «' + st.q + '»' : 'Каталог');
            if (pageTitleEl) pageTitleEl.textContent = title;
            if (crumbCatEl) {
                crumbCatEl.textContent = category || (st.q ? 'Поиск' : 'Все товары');
            }
            document.title = title + ' — Белый Дом';
        };
        setHeading();

        const syncFavLink = () => {
            if (favLinkEl) {
                favLinkEl.textContent = favorites.length
                    ? 'Избранное (' + favorites.length + ')'
                    : 'Избранное';
            }
        };

        const mapProduct = (p, i) => {
            const rawImg = p['imageUrl'];
            const imgUrl = typeof rawImg === 'string' ? rawImg : '';
            return {
                id: Number(p.id),
                title: p.title,
                price: p.price != null ? Number(p.price) : null,
                stock: p.stock != null ? Number(p.stock) : 0,
                article: p.article || '',
                barcode: p.barcode || '',
                color: p.color || '',
                old: p.oldPrice != null ? Number(p.oldPrice) : null,
                img: imgUrl,
                meta: p.category,
                w: p.width != null ? Number(p.width) : null,
                kit: p.kit,
                style: p.style,
                badge: p.badge,
                bg: BGS[i % BGS.length],
            };
        };

        fetch('/api/products')
            .then((r) => (r.ok ? r.json() : Promise.reject()))
            .then((d) => {
                if (!Array.isArray(d)) return;
                const catLower = (category || '').toLowerCase();
                all = d
                    .filter((p) => !category || (p.category || '').toLowerCase() === catLower)
                    .map(mapProduct);
                loaded = true;
            })
            .catch(() => {
                loaded = false;
            })
            .finally(render);

        const checked = (g) =>
            $$(`[data-group="${g}"] input:checked`).map((i) => i.value);

        const num = (v) => parseInt(String(v).replace(/\D/g, ''), 10) || 0;

        function filtered() {
            const min = num(pMin && pMin.value) || 0;
            const max = num(pMax && pMax.value) || Infinity;
            const w = checked('w');
            const kit = checked('kit');
            const sty = checked('style');

            let list = all.filter((p) => {
                if (p.price != null && (p.price < min || p.price > max)) return false;
                if (
                    w.length &&
                    !(
                        p.w != null &&
                        w.some((v) => (v === '120' ? p.w >= 120 : Number(v) === p.w))
                    )
                ) {
                    return false;
                }
                if (kit.length && !(p.kit && kit.includes(p.kit))) return false;
                if (sty.length && !(p.style && sty.includes(p.style))) return false;
                return !(
                    st.q &&
                    !((p.title || '') + ' ' + (p.meta || '') + ' ' + (p.article || '') + ' ' + (p.barcode || '')).toLowerCase().includes(st.q)
                );
            });

            if (st.sort === 'asc') list = [...list].sort((a, b) => (a.price ?? Infinity) - (b.price ?? Infinity));
            if (st.sort === 'desc') list = [...list].sort((a, b) => {
                if (a.price == null) return 1;
                if (b.price == null) return -1;
                return b.price - a.price;
            });
            return list;
        }

        function card(p) {
            const canBuy = p.price != null && p.price > 0 && p.stock > 0;
            const sale = Boolean(p.old && p.old > p.price);
            const badge =
                p.badge === 'new'
                    ? '<span class="badge">Новинка</span>'
                    : p.badge === 'sale' || sale
                        ? '<span class="badge sale">Акция</span>'
                        : '';
            const pic = p.img
                ? `<img src="${esc(p.img)}" alt="${esc(p.title)}" class="pc-pic">`
                : '<i class="ph"><b></b></i>';
            const on = favorites.includes(p.id);

            return `<article class="pc">
        <div class="pc-img" style="background:${p.bg || '#d9d0c3'}">${pic}${badge}
          <a class="pc-link" href="/product/${esc(p.id)}" aria-label="${esc(p.title)}"></a>
          <button type="button" class="fav${on ? ' on' : ''}" data-id="${p.id}" aria-label="В избранное">${on ? '♥' : '♡'}</button>
        </div>
        <div class="pc-body">
          <span class="pc-meta">${esc(p.meta || '')}</span>
          <h3 class="pc-title"><a href="/product/${esc(p.id)}">${esc(p.title)}</a></h3>
          <p class="pc-w">${[p.article ? 'Артикул: ' + esc(p.article) : '', p.barcode ? 'Штрихкод: ' + esc(p.barcode) : '', p.color ? 'Цвет: ' + esc(p.color) : '', p.w ? 'Ширина: ' + p.w + ' см' : ''].filter(Boolean).join(' · ') || '&nbsp;'}</p>
          <div class="pc-foot">
            <div class="pc-price${sale ? ' sale' : ''}">
              <span>${canBuy ? fmt(p.price) + ' ₽' : 'Цена уточняется'}</span>${sale ? `<s>${fmt(p.old)} ₽</s>` : ''}
            </div>
            <a class="pc-go" href="/product/${esc(p.id)}" aria-label="Подробнее">→</a>
          </div>
        </div>
      </article>`;
        }

        function bindBrokenImageFallbacks() {
            gridEl.querySelectorAll('img.pc-pic').forEach((img) => {
                if (img.dataset.boundError) return;
                img.dataset.boundError = '1';
                img.addEventListener('error', () => {
                    const ph = document.createElement('i');
                    ph.className = 'ph';
                    ph.innerHTML = '<b></b>';
                    img.replaceWith(ph);
                }, { once: true });
            });
        }

        function fillCounts() {
            $$('[data-group]').forEach((group) => {
                const g = group.dataset.group;
                group.hidden = !all.some((p) => (g === 'w' ? p.w != null : Boolean(p[g])));

                group.querySelectorAll('input[type=checkbox]').forEach((i) => {
                    const v = i.value;
                    const n = all.filter((p) =>
                        g === 'w'
                            ? v === '120'
                                ? p.w >= 120
                                : p.w === Number(v)
                            : p[g] === v
                    ).length;
                    const sp = i.nextElementSibling;
                    if (!sp) return;
                    if (sp.dataset.base === undefined) sp.dataset.base = sp.textContent;
                    sp.textContent = sp.dataset.base + ' (' + n + ')';
                });
            });
        }

        function renderPager(pages) {
            if (!pagerEl) return;
            if (pages < 2) {
                pagerEl.innerHTML = '';
                return;
            }
            const set = new Set([1, pages, st.page - 1, st.page, st.page + 1]);
            if (pages <= 6) {
                for (let i = 1; i <= pages; i++) set.add(i);
            } else if (st.page <= 3) {
                [2, 3, 4, 5].forEach((i) => set.add(i));
            }
            const nums = [...set].filter((n) => n >= 1 && n <= pages).sort((a, b) => a - b);

            let html =
                `<button type="button" data-p="${st.page - 1}" ${st.page === 1 ? 'disabled' : ''} aria-label="Назад">‹</button>`;
            nums.forEach((n, i) => {
                if (i && n - nums[i - 1] > 1) html += '<button type="button" disabled>…</button>';
                html +=
                    `<button type="button" data-p="${n}" class="${n === st.page ? 'active' : ''}">${n}</button>`;
            });
            html +=
                `<button type="button" data-p="${st.page + 1}" ${st.page === pages ? 'disabled' : ''} aria-label="Вперёд">›</button>`;
            pagerEl.innerHTML = html;
        }

        function render() {
            fillCounts();
            const list = filtered();
            const pages = Math.max(1, Math.ceil(list.length / st.per));
            st.page = Math.min(st.page, pages);
            const slice = list.slice((st.page - 1) * st.per, st.page * st.per);

            if (foundEl) {
                foundEl.textContent = loaded
                    ? 'Найдено: ' + list.length + ' ' + plural(list.length, 'товар', 'товара', 'товаров')
                    : 'Не удалось загрузить товары';
            }
            if (fApplyEl) {
                fApplyEl.textContent =
                    'Показать ' + list.length + ' ' + plural(list.length, 'товар', 'товара', 'товаров');
            }

            gridEl.innerHTML = !loaded
                ? '<p class="empty">Не удалось загрузить каталог. Обновите страницу чуть позже.</p>'
                : slice.length
                    ? slice.map(card).join('')
                    : '<p class="empty">По вашему запросу ничего не найдено</p>';

            bindBrokenImageFallbacks();
            renderPager(pages);
            syncFavLink();
        }

        if (pagerEl) {
            pagerEl.addEventListener('click', (e) => {
                const b = e.target.closest('button[data-p]');
                if (!b || b.disabled) return;
                st.page = Number(b.dataset.p);
                render();
                window.scrollTo({ top: 0, behavior: 'smooth' });
            });
        }

        gridEl.addEventListener('click', (e) => {
            const f = e.target.closest('.fav');
            if (!f) return;
            const id = Number(f.dataset.id);
            favorites = favorites.includes(id)
                ? favorites.filter((x) => x !== id)
                : [...favorites, id];
            storage.set('fav', favorites);
            const isFav = favorites.includes(id);
            f.classList.toggle('on', isFav);
            f.textContent = isFav ? '♥' : '♡';
            syncFavLink();
        });

        if (sortEl) {
            sortEl.addEventListener('change', (e) => {
                st.sort = e.target.value;
                st.page = 1;
                render();
            });
        }

        viewButtons.forEach((b) => {
            b.addEventListener('click', () => {
                viewButtons.forEach((x) => x.classList.toggle('active', x === b));
                gridEl.classList.toggle('list', b.dataset.view === 'list');
            });
        });

        filterCheckboxes.forEach((i) => {
            i.addEventListener('change', () => {
                st.page = 1;
                render();
            });
        });

        if (rMin && rMax && pMin && pMax) {
            const sliderToInputs = (changed) => {
                if (Number(rMin.value) > Number(rMax.value)) {
                    if (changed === rMin) rMin.value = rMax.value;
                    else rMax.value = rMin.value;
                }
                pMin.value =
                    Number(rMin.value) > 20000
                        ? Number(rMin.value).toLocaleString('ru-RU')
                        : '';
                pMax.value =
                    Number(rMax.value) < 150000
                        ? Number(rMax.value).toLocaleString('ru-RU')
                        : '';
                st.page = 1;
                render();
            };
            rMin.addEventListener('input', () => sliderToInputs(rMin));
            rMax.addEventListener('input', () => sliderToInputs(rMax));
            [pMin, pMax].forEach((inp) => {
                inp.addEventListener('change', () => {
                    const a = num(pMin.value);
                    const b = num(pMax.value);
                    rMin.value = a || 20000;
                    rMax.value = b || 150000;
                    st.page = 1;
                    render();
                });
            });
        }

        const applySearch = () => {
            if (!searchInputEl) return;
            st.qRaw = searchInputEl.value.trim();
            st.q = st.qRaw.toLowerCase();
            st.page = 1;
            const u = new URLSearchParams();
            if (category) u.set('category', category);
            if (st.qRaw) u.set('q', st.qRaw);
            const qs = u.toString();
            try {
                history.replaceState(null, '', location.pathname + (qs ? '?' + qs : ''));
            } catch { /* ignore */ }
            setHeading();
            render();
        };

        if (searchForm) {
            searchForm.addEventListener('submit', (e) => {
                e.preventDefault();
                applySearch();
            });
        }
        if (searchInputEl) {
            searchInputEl.addEventListener('search', applySearch);
        }

        const setFilters = (open) => {
            filtersEl.classList.toggle('open', open);
            document.body.classList.toggle('lock', open);
        };
        if (fOpen) fOpen.addEventListener('click', () => setFilters(true));
        if (fClose) fClose.addEventListener('click', () => setFilters(false));
        if (fApplyEl) {
            fApplyEl.addEventListener('click', () => {
                setFilters(false);
                window.scrollTo({ top: 0 });
            });
        }
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') setFilters(false);
        });
        window.addEventListener('resize', () => {
            if (innerWidth > 900) setFilters(false);
        });
    }

    /* ========================= 4. product ========================= */

    function initProduct() {
        const pdTitleEl = $('pdTitle');
        if (!pdTitleEl) return;

        const KIT = {
            tumba: 'С тумбой и раковиной',
            mirror: 'С зеркалом',
            penal: 'С пеналом',
            full: 'Полный комплект',
        };
        const STYLE = {
            modern: 'Современный',
            classic: 'Классический',
            loft: 'Лофт',
            minimal: 'Минимализм',
        };

        const m = location.pathname.match(/\/product\/([^/?#]+)/);
        const id = m
            ? decodeURIComponent(m[1])
            : new URLSearchParams(location.search).get('id');

        const hide = (el) => {
            if (el) el.style.display = 'none';
        };

        fetch('/api/products')
            .then((r) => (r.ok ? r.json() : Promise.reject()))
            .then((d) => (Array.isArray(d) ? d : []))
            .then((list) => {
                const p = list.find((x) => String(x.id) === String(id));
                if (p) init(p);
                else notFound();
            })
            .catch(() =>
                notFound('Не удалось загрузить товар. Обновите страницу чуть позже.')
            );

        function notFound(text) {
            document.title = 'Товар не найден — Белый Дом';
            const root = document.querySelector('.pd');
            if (root) {
                root.innerHTML =
                    '<div class="pd-empty"><p>' +
                    esc(text || 'Такого товара нет в каталоге') +
                    '</p><a href="/catalog">Вернуться в каталог</a></div>';
            }
        }

        function init(p) {
            const rawImg = p['imageUrl'];
            const primaryImg = typeof rawImg === 'string' ? rawImg : '';
            const images = [{ src: primaryImg, label: 'Фото' }];
            let cur = 0;

            const pdCrumbEl = $('pdCrumb');
            const pdCatEl = $('pdCat');
            const pdKickerEl = $('pdKicker');
            const pdDescEl = $('pdDesc');
            const pdPriceEl = $('pdPrice');
            const pdTableEl = $('pdTable');
            const pdImgEl = $('pdImg');
            const pdCountEl = $('pdCount');
            const pdThumbsEl = $('pdThumbs');
            const pdZoom = $('pdZoom');
            const pdWidthsEl = $('pdWidths');
            const pdFinishEl = $('pdFinish');
            const planW = $('planW');
            const pdAccBtnEl = $('pdAccBtn');
            const pdAccBodyEl = $('pdAccBody');
            const cartLabelEl = $('cartLabel');
            const pdAddTextEl = $('pdAddText');
            const pdAdd = $('pdAdd');
            const canBuy = p.price != null && Number(p.price) > 0 && Number(p.stock) > 0;

            document.title = (p.title || 'Товар') + ' — Белый Дом';
            if (pdCrumbEl) pdCrumbEl.textContent = p.title || '';
            const cat = p.category || 'Каталог';
            if (pdCatEl) {
                pdCatEl.textContent = cat;
                pdCatEl.href = '/catalog?category=' + encodeURIComponent(cat);
            }
            if (pdKickerEl) pdKickerEl.textContent = cat;
            pdTitleEl.textContent = p.title || '';
            if (pdDescEl) pdDescEl.textContent = p.description || '';
            if (pdPriceEl) pdPriceEl.textContent = canBuy ? fmt(p.price) + ' ₽' : 'Цена уточняется';
            if (!primaryImg && pdZoom) pdZoom.style.display = 'none';
            if (pdAdd && !canBuy) {
                pdAdd.disabled = true;
                pdAdd.setAttribute('aria-disabled', 'true');
                if (pdAddTextEl) pdAddTextEl.textContent = 'Пока недоступен';
            }

            const showImg = (i) => {
                cur = i;
                if (pdImgEl) {
                    if (images[i].src) {
                        pdImgEl.src = images[i].src;
                        pdImgEl.style.display = '';
                        pdImgEl.alt = p.title || '';
                    } else {
                        pdImgEl.removeAttribute('src');
                        pdImgEl.style.display = 'none';
                        const container = pdImgEl.parentElement;
                        let placeholder = container && container.querySelector('.pd-no-image');
                        if (container && !placeholder) {
                            placeholder = document.createElement('div');
                            placeholder.className = 'pd-no-image';
                            placeholder.textContent = 'Фото добавим позже';
                            container.appendChild(placeholder);
                        }
                    }
                }
                if (pdCountEl) {
                    pdCountEl.textContent =
                        images.length > 1 ? i + 1 + ' / ' + images.length : '';
                }
                $$('.pd-thumb').forEach((b, k) => b.classList.toggle('active', k === i));
            };

            if (pdThumbsEl) {
                pdThumbsEl.innerHTML =
                    images.length > 1
                        ? images
                            .map(
                                (im, i) =>
                                    `<button type="button" class="pd-thumb" data-i="${i}"><img src="${esc(im.src)}" alt=""><span>${esc(im.label)}</span></button>`
                            )
                            .join('')
                        : '';
                pdThumbsEl.addEventListener('click', (e) => {
                    const b = e.target.closest('.pd-thumb');
                    if (b) showImg(Number(b.dataset.i));
                });
            }
            showImg(0);

            if (pdZoom) {
                pdZoom.addEventListener('click', () => {
                    const lb = document.createElement('div');
                    lb.className = 'pd-lb';
                    lb.innerHTML = `<img src="${esc(images[cur].src)}" alt="">`;
                    const close = () => {
                        lb.remove();
                        document.removeEventListener('keydown', onKey);
                    };
                    const onKey = (e) => {
                        if (e.key === 'Escape') close();
                    };
                    lb.addEventListener('click', close);
                    document.addEventListener('keydown', onKey);
                    document.body.appendChild(lb);
                });
            }

            const w = p.width != null ? Number(p.width) : null;

            const hideWithLabel = (el) => {
                if (!el) return;
                const prev = el.previousElementSibling;
                if (prev && prev.classList.contains('pd-label')) hide(prev);
                hide(el);
            };

            hideWithLabel(pdWidthsEl);
            hideWithLabel(pdFinishEl);

            if (w && pdWidthsEl) {
                const label = pdWidthsEl.previousElementSibling;
                if (label) label.style.display = '';
                pdWidthsEl.style.display = '';
                pdWidthsEl.innerHTML =
                    `<button type="button" class="pd-opt pd-opt--w active" data-w="${w}">${w} см</button>`;
            }

            const renderSpecs = () => {
                const rows = [['Категория', cat]];
                if (w) rows.push(['Ширина', w + ' см']);
                if (p.kit && KIT[p.kit]) rows.push(['Тип комплекта', KIT[p.kit]]);
                if (p.style && STYLE[p.style]) rows.push(['Стиль', STYLE[p.style]]);
                if (pdTableEl) {
                    pdTableEl.innerHTML = rows
                        .map(
                            ([k, v]) =>
                                `<div><dt>${esc(k)}</dt><dd>${esc(v)}</dd></div>`
                        )
                        .join('');
                }
            };

            const plan = planW && (planW.closest('.pd-plan') || planW.closest('svg'));
            hide(plan);
            const grid = document.querySelector('.pd-specs-grid');
            if (grid) grid.style.gridTemplateColumns = '1fr';

            renderSpecs();

            if (pdAccBtnEl && pdAccBodyEl) {
                pdAccBtnEl.addEventListener('click', () => {
                    const open = pdAccBodyEl.hidden;
                    pdAccBodyEl.hidden = !open;
                    pdAccBtnEl.setAttribute('aria-expanded', String(open));
                    const icon = pdAccBtnEl.querySelector('i');
                    if (icon) icon.textContent = open ? '−' : '+';
                });
            }

            let cart = readCart();

            const showCart = () => {
                if (!cartLabelEl) return;
                const n = cartQty(cart);
                cartLabelEl.textContent = n ? 'Корзина (' + n + ')' : 'Корзина';
            };
            showCart();

            if (pdAdd) {
                pdAdd.addEventListener('click', () => {
                    if (!canBuy) return;
                    const width = w || undefined;
                    const ex = cart.find(
                        (c) =>
                            String(c.id) === String(p.id) &&
                            c.width === width &&
                            c.finish === undefined
                    );
                    if (ex) {
                        ex.qty = (ex.qty || 1) + 1;
                    } else {
                        cart.push({
                            id: p.id,
                            title: p.title,
                            price: Number(p.price),
                            width,
                            finish: undefined,
                            qty: 1,
                            img: images[0].src,
                            category: cat,
                        });
                    }
                    writeCart(cart);
                    showCart();
                    if (pdAddTextEl) {
                        pdAddTextEl.textContent = 'Добавлено ✓';
                        setTimeout(() => {
                            pdAddTextEl.textContent = 'В корзину';
                        }, 1200);
                    }
                });
            }
        }
    }

    /* ========================= 5. cart ========================= */

    function initCart() {
        const ctItemsEl = $('ctItems');
        if (!ctItemsEl) return;

        const ctCountEl = $('ctCount');
        const ctGoodsLabelEl = $('ctGoodsLabel');
        const ctGoodsEl = $('ctGoods');
        const ctTotalEl = $('ctTotal');
        const ctCheckoutEl = $('ctCheckout');

        let cart = readCart();
        const info = {};

        const save = () => writeCart(cart);

        fetch('/api/products')
            .then((r) => (r.ok ? r.json() : Promise.reject()))
            .then((d) => {
                if (!Array.isArray(d)) return;
                d.forEach((p) => {
                    info[String(p.id)] = p;
                });
            })
            .catch(() => {})
            .finally(render);

        function row(it, i) {
            const p = info[String(it.id)] || {};
            const img = it.img || p['imageUrl'] || '';
            const cat = it.category || p.category || '';
            const fin = it.finish ? String(it.finish).toLowerCase() : '';

            return `<article class="ct-item">
        <a class="ct-img" href="/product/${esc(it.id)}">${
                img ? `<img src="${esc(img)}" alt="${esc(it.title)}">` : ''
            }</a>
        <div class="ct-info">
          ${cat ? `<span class="ct-kicker">${esc(cat)}</span>` : ''}
          <h3 class="ct-name"><a href="/product/${esc(it.id)}">${esc(it.title)}</a></h3>
          ${it.width ? `<p class="ct-meta">Ширина: ${esc(it.width)} см</p>` : ''}
          ${fin ? `<p class="ct-meta">Отделка: ${esc(fin)}</p>` : ''}
          <button type="button" class="ct-del" data-act="del" data-i="${i}">Удалить</button>
        </div>
        <div class="ct-buy">
          <div class="ct-qty">
            <button type="button" data-act="dec" data-i="${i}" aria-label="Меньше">−</button>
            <span>${it.qty}</span>
            <button type="button" data-act="inc" data-i="${i}" aria-label="Больше">+</button>
          </div>
          <div class="ct-sum">${rub(it.price * it.qty)}</div>
        </div>
      </article>`;
        }

        function render() {
            const n = cartQty(cart);
            const sum = cart.reduce((s, c) => s + c.price * c.qty, 0);

            if (ctCountEl) {
                ctCountEl.textContent = n
                    ? n + ' ' + plural(n, 'товар', 'товара', 'товаров') + ' в корзине'
                    : 'В корзине пока пусто';
            }
            ctItemsEl.innerHTML = cart.length
                ? cart.map(row).join('')
                : '<p class="ct-empty">Добавьте товары из каталога, и они появятся здесь.</p>';
            if (ctGoodsLabelEl) ctGoodsLabelEl.textContent = 'Товары (' + n + ')';
            if (ctGoodsEl) ctGoodsEl.textContent = rub(sum);
            if (ctTotalEl) ctTotalEl.textContent = rub(sum);
            if (ctCheckoutEl) {
                ctCheckoutEl.classList.toggle('is-disabled', !n);
                ctCheckoutEl.setAttribute('aria-disabled', String(!n));
            }
        }

        ctItemsEl.addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-act]');
            if (!btn) return;
            const idx = Number(btn.dataset.i);
            if (Number.isNaN(idx) || !cart[idx]) return;
            const act = btn.dataset.act;
            if (act === 'del') {
                cart.splice(idx, 1);
            } else if (act === 'inc') {
                cart[idx].qty = (Number(cart[idx].qty) || 1) + 1;
            } else if (act === 'dec') {
                const nextQty = (Number(cart[idx].qty) || 1) - 1;
                if (nextQty <= 0) cart.splice(idx, 1);
                else cart[idx].qty = nextQty;
            }
            save();
            render();
        });
    }

    /* ========================= 6. checkout ========================= */

    function initCheckout() {
        const form = $('checkoutForm');
        if (!form) return;

        const itemsEl = $('coItems');
        const goodsLabelEl = $('coGoodsLabel');
        const goodsEl = $('coGoods');
        const deliveryEl = $('coDelivery');
        const totalEl = $('coTotal');
        const submitBtn = $('coSubmit');
        const consentEl = $('coConsent');
        const addressEl = $('coAddress');
        const commentEl = $('coComment');
        const firstEl = $('coFirst');
        const lastEl = $('coLast');
        const phoneEl = $('coPhone');
        const emailEl = $('coEmail');

        let errEl = $('coError');
        if (!errEl && submitBtn) {
            errEl = document.createElement('p');
            errEl.id = 'coError';
            errEl.className = 'sum-note';
            errEl.style.color = '#b42318';
            errEl.style.display = 'none';
            submitBtn.insertAdjacentElement('beforebegin', errEl);
        }

        const showErr = (msg) => {
            if (!errEl) return;
            errEl.textContent = msg;
            errEl.style.display = msg ? '' : 'none';
        };

        const getDelivery = () => {
            const checked = form.querySelector('input[name="delivery"]:checked');
            return checked ? checked.value : 'pickup';
        };

        const getPayment = () => {
            const checked = form.querySelector('input[name="payment"]:checked');
            return checked ? checked.value : 'card_on_delivery';
        };

        const syncDeliveryState = () => {
            const isPickup = getDelivery() === 'pickup';
            if (addressEl) {
                addressEl.disabled = isPickup;
                addressEl.placeholder = isPickup
                    ? 'Самовывоз из шоурума'
                    : 'Город, улица, дом, квартира';
                if (isPickup) addressEl.value = '';
            }
            const sum = readCart().reduce((s, c) => s + (Number(c.price) || 0) * (Number(c.qty) || 1), 0);
            const price = Math.max(0, Number(form.dataset.deliveryPrice) || 0);
            const threshold = Math.max(0, Number(form.dataset.freeThreshold) || 0);
            const deliveryCost = isPickup || (threshold > 0 && sum >= threshold) ? 0 : price;
            if (deliveryEl) deliveryEl.textContent = deliveryCost ? rub(deliveryCost) : '0 ₽';
            const courierPrice = $('coCourierPrice');
            if (courierPrice) courierPrice.textContent = threshold > 0 && sum >= threshold ? 'Бесплатно' : rub(price);
            if (totalEl) totalEl.textContent = rub(sum + deliveryCost);
        };

        const renderSummary = () => {
            const cart = readCart();
            const n = cartQty(cart);
            const sum = cart.reduce((s, c) => s + (Number(c.price) || 0) * (Number(c.qty) || 1), 0);

            if (itemsEl) {
                itemsEl.innerHTML = cart.length
                    ? cart
                        .map(
                            (it) => `<div class="sum-row">
                    <span>${esc(it.title)} × ${Number(it.qty) || 1}</span>
                    <span>${rub((Number(it.price) || 0) * (Number(it.qty) || 1))}</span>
                </div>`
                        )
                        .join('')
                    : '<p class="ct-empty">Корзина пуста.</p>';
            }

            if (goodsLabelEl) goodsLabelEl.textContent = 'Товары (' + n + ')';
            if (goodsEl) goodsEl.textContent = rub(sum);
            const deliveryPrice = Math.max(0, Number(form.dataset.deliveryPrice) || 0);
            const freeThreshold = Math.max(0, Number(form.dataset.freeThreshold) || 0);
            const isCourier = getDelivery() === 'courier';
            const deliveryCost = isCourier && !(freeThreshold > 0 && sum >= freeThreshold) ? deliveryPrice : 0;
            if (deliveryEl) deliveryEl.textContent = deliveryCost ? rub(deliveryCost) : '0 ₽';
            if (totalEl) totalEl.textContent = rub(sum + deliveryCost);
            if (submitBtn) submitBtn.disabled = !n;
        };

        $$('input[name="delivery"]', form).forEach((r) => {
            r.addEventListener('change', syncDeliveryState);
        });

        renderSummary();
        syncDeliveryState();

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            showErr('');

            const cart = readCart();
            if (!cart.length) {
                showErr('Корзина пуста');
                return;
            }

            const first = (firstEl ? firstEl.value : '').trim();
            const last = (lastEl ? lastEl.value : '').trim();
            const customerName = [first, last].filter(Boolean).join(' ').trim();
            const phone = (phoneEl ? phoneEl.value : '').trim();
            const email = (emailEl ? emailEl.value : '').trim();
            const delivery = getDelivery();
            const payment = getPayment();
            const address = (addressEl ? addressEl.value : '').trim();
            const rawComment = (commentEl ? commentEl.value : '').trim();

            if (!customerName) {
                showErr('Укажите имя и фамилию');
                return;
            }
            if (!phone) {
                showErr('Укажите телефон');
                return;
            }
            if (delivery === 'courier' && !address) {
                showErr('Укажите адрес доставки');
                return;
            }
            if (consentEl && !consentEl.checked) {
                showErr('Подтвердите согласие на обработку персональных данных');
                return;
            }

            const payLabel = payment === 'online' ? 'Онлайн картой' : 'Картой при получении';
            const delLabel = delivery === 'courier' ? 'Курьерская доставка' : 'Самовывоз';
            const comment = [rawComment, `Доставка: ${delLabel}`, `Оплата: ${payLabel}`]
                .filter(Boolean)
                .join(' | ');

            const payload = {
                customerName,
                phone,
                email: email || null,
                delivery,
                paymentMethod: payment,
                address: delivery === 'courier' ? address : null,
                comment,
                items: cart.map((c) => ({
                    productId: Number(c.id),
                    quantity: Number(c.qty) || 1,
                })),
            };

            if (submitBtn) submitBtn.disabled = true;

            try {
                const res = await fetch('/api/orders', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload),
                });

                const data = await res.json().catch(() => ({}));
                if (!res.ok) {
                    showErr(data.message || 'Не удалось оформить заказ');
                    if (submitBtn) submitBtn.disabled = false;
                    return;
                }

                const localSum = cart.reduce((s, c) => s + (Number(c.price) || 0) * (Number(c.qty) || 1), 0);
                storage.set('lastOrder', {
                    ...data,
                    total: data.total != null ? data.total : localSum,
                    items: cart.map((c) => ({
                        title: c.title,
                        width: c.width,
                        finish: c.finish,
                        qty: Number(c.qty) || 1,
                        price: Number(c.price) || 0,
                    })),
                });
                writeCart([]);
                location.href = '/order-success';
            } catch {
                showErr('Ошибка сети. Попробуйте ещё раз.');
                if (submitBtn) submitBtn.disabled = false;
            }
        });
    }

    /* ========================= 7. order success ========================= */

    function initOrderSuccess() {
        const numEl = $('odNumber');
        const itemsEl = $('odItems');
        if (!numEl && !itemsEl) return;

        const order = storage.get('lastOrder', null);
        if (numEl) {
            numEl.textContent = order && order.id != null ? String(order.id) : '—';
        }

        if (itemsEl) {
            const items = order && Array.isArray(order.items) ? order.items : [];
            if (!items.length) {
                itemsEl.innerHTML = '<p class="od-row-meta">Данные о заказе не найдены.</p>';
                return;
            }

            const rowsHtml = items
                .map((it) => {
                    const qty = Number(it.qty) || 1;
                    const price = Number(it.price) || 0;
                    const metaParts = [];
                    if (it.width) metaParts.push(it.width + ' см');
                    if (it.finish) metaParts.push(String(it.finish).toLowerCase());
                    metaParts.push(qty + ' шт.');

                    return `<div class="od-row">
                        <div>
                            <p class="od-row-title">${esc(it.title || 'Товар')}</p>
                            <p class="od-row-meta">${esc(metaParts.join(' · '))}</p>
                        </div>
                        <div class="od-row-sum">${rub(price * qty)}</div>
                    </div>`;
                })
                .join('');

            const totalVal = order && order.total != null
                ? Number(order.total)
                : items.reduce((s, it) => s + (Number(it.price) || 0) * (Number(it.qty) || 1), 0);
            const deliveryVal = order ? Number(order.deliveryCost) || 0 : 0;

            itemsEl.innerHTML = `${rowsHtml}${deliveryVal > 0 ? `<div class="od-row"><span class="od-row-meta">Доставка</span><span class="od-row-sum">${rub(deliveryVal)}</span></div>` : ''}
                <div class="od-total-foot">
                    <span>Итого</span>
                    <span class="od-total-val">${rub(totalVal)}</span>
                </div>`;
        }
    }

    initChrome();
    initGlobalSearch();
    initCatalog();
    initProduct();
    initCart();
    initCheckout();
    initOrderSuccess();
})();
