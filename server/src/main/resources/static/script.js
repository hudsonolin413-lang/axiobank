// Smooth scrolling for navigation links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Navbar scroll effect
let lastScroll = 0;
const navbar = document.querySelector('.navbar');

window.addEventListener('scroll', () => {
    const currentScroll = window.pageYOffset;

    if (currentScroll > 100) {
        navbar.style.boxShadow = '0 4px 16px rgba(0, 0, 0, 0.15)';
    } else {
        navbar.style.boxShadow = '0 2px 8px rgba(0, 0, 0, 0.1)';
    }

    lastScroll = currentScroll;
});

// Animate elements on scroll
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.style.opacity = '1';
            entry.target.style.transform = 'translateY(0)';
        }
    });
}, observerOptions);

// Observe all cards
document.querySelectorAll('.feature-card, .service-card, .benefit-item').forEach(el => {
    el.style.opacity = '0';
    el.style.transform = 'translateY(20px)';
    el.style.transition = 'opacity 0.6s ease-out, transform 0.6s ease-out';
    observer.observe(el);
});

// Add loading indicator for buttons
const buttons = document.querySelectorAll('button');
buttons.forEach(button => {
    button.addEventListener('click', function(e) {
        if (this.getAttribute('onclick')) {
            this.style.opacity = '0.7';
            this.innerHTML = 'Loading...';
        }
    });
});

// Service card interactions
document.querySelectorAll('.service-card').forEach(card => {
    card.addEventListener('mouseenter', function() {
        this.style.transform = 'translateY(-12px) scale(1.02)';
    });

    card.addEventListener('mouseleave', function() {
        this.style.transform = 'translateY(0) scale(1)';
    });
});

// Animated Statistics Counter
class StatCounter {
    constructor(element, index) {
        this.element = element;
        this.numberElement = this.element.querySelector('.number-animation');
        this.lineElement = this.element.querySelector('.stat-line');
        this.target = parseInt(this.numberElement.getAttribute('data-target'));
        this.current = 0;
        this.animationDelay = index * 200;
        this.createObserver();
    }

    createObserver() {
        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                setTimeout(() => {
                    this.startAnimation();
                }, this.animationDelay);
                observer.disconnect();
            }
        }, { threshold: 0.2 });

        observer.observe(this.element);
    }

    startAnimation() {
        // Animate line
        this.element.classList.add('animate');

        // Animate number
        setTimeout(() => {
            const duration = 2000;
            const stepTime = 30;
            const steps = duration / stepTime;
            const increment = this.target / steps;

            const timer = setInterval(() => {
                this.current += increment;
                if (this.current >= this.target) {
                    this.current = this.target;
                    clearInterval(timer);
                }
                this.numberElement.textContent = Math.round(this.current);
            }, stepTime);
        }, 800);
    }
}

// Initialize stat counters
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.stat-card').forEach((card, index) => {
        new StatCounter(card, index);
    });
});

// Console welcome message
console.log('%c🏦 Welcome to Axio Bank!', 'color: #C41E3A; font-size: 24px; font-weight: bold;');
console.log('%cEmpowering communities through banking', 'color: #666666; font-size: 14px;');
console.log('%cAPI Base URL: http://localhost:8081', 'color: #4CAF50; font-size: 12px;');

// Check API connectivity
fetch('/api/health')
    .then(response => response.json())
    .then(data => {
        console.log('%c✅ Server is online', 'color: #4CAF50; font-weight: bold;');
    })
    .catch(error => {
        console.log('%c⚠️  Server may be offline', 'color: #FF9800; font-weight: bold;');
    });

// Testimonials Slider
class TestimonialsSlider {
    constructor() {
        this.slider = document.getElementById('testimonials-slider');
        this.prevBtn = document.getElementById('prev-testimonial');
        this.nextBtn = document.getElementById('next-testimonial');
        this.scrollbarThumb = document.getElementById('scrollbar-thumb');
        this.currentIndex = 0;
        this.cardsPerView = 3;
        this.totalCards = 4;

        this.init();
    }

    init() {
        if (!this.slider) return;

        this.updateCardsPerView();
        window.addEventListener('resize', () => this.updateCardsPerView());

        this.prevBtn.addEventListener('click', () => this.prev());
        this.nextBtn.addEventListener('click', () => this.next());

        this.updateSlider();
    }

    updateCardsPerView() {
        const width = window.innerWidth;
        if (width <= 768) {
            this.cardsPerView = 1;
        } else if (width <= 1024) {
            this.cardsPerView = 2;
        } else {
            this.cardsPerView = 3;
        }
        this.updateSlider();
    }

    prev() {
        if (this.currentIndex > 0) {
            this.currentIndex--;
            this.updateSlider();
        }
    }

    next() {
        const maxIndex = this.totalCards - this.cardsPerView;
        if (this.currentIndex < maxIndex) {
            this.currentIndex++;
            this.updateSlider();
        }
    }

    updateSlider() {
        const card = this.slider.querySelector('.testimonial-card');
        if (!card) return;

        const cardWidth = card.offsetWidth;
        const gap = 40;
        const offset = -(this.currentIndex * (cardWidth + gap));

        this.slider.style.transform = `translateX(${offset}px)`;

        // Update buttons state
        this.prevBtn.disabled = this.currentIndex === 0;
        this.nextBtn.disabled = this.currentIndex >= this.totalCards - this.cardsPerView;

        // Update scrollbar
        const scrollPercentage = (this.currentIndex / (this.totalCards - this.cardsPerView)) * 100;
        const thumbWidth = (this.cardsPerView / this.totalCards) * 100;
        this.scrollbarThumb.style.width = `${thumbWidth}%`;
        this.scrollbarThumb.style.transform = `translateX(${scrollPercentage}%)`;
    }
}

// Initialize testimonials slider
document.addEventListener('DOMContentLoaded', () => {
    new TestimonialsSlider();
});

// Cookie Consent Banner
class CookieBanner {
    constructor() {
        this.banner = document.getElementById('cookie-banner');
        this.acceptBtn = document.getElementById('accept-cookies');
        this.cookieName = 'axio_bank_cookie_consent';
        this.init();
    }

    init() {
        if (!this.banner || !this.acceptBtn) return;

        // Check if user has already accepted cookies
        if (!this.hasAcceptedCookies()) {
            // Show banner after a short delay for better UX
            setTimeout(() => {
                this.showBanner();
            }, 1000);
        }

        // Handle accept button click
        this.acceptBtn.addEventListener('click', () => {
            this.acceptCookies();
        });
    }

    hasAcceptedCookies() {
        return document.cookie.split('; ').some(cookie => cookie.startsWith(`${this.cookieName}=`));
    }

    showBanner() {
        this.banner.classList.add('show');
    }

    hideBanner() {
        this.banner.classList.remove('show');
        this.banner.classList.add('hide');
    }

    acceptCookies() {
        // Set cookie that expires in 1 year
        const expiryDate = new Date();
        expiryDate.setFullYear(expiryDate.getFullYear() + 1);
        document.cookie = `${this.cookieName}=accepted; expires=${expiryDate.toUTCString()}; path=/; SameSite=Lax`;

        // Hide the banner
        this.hideBanner();

        // Optional: Log analytics event
        console.log('%c✅ Cookie consent accepted', 'color: #4CAF50; font-weight: bold;');
    }
}

// Initialize cookie banner
document.addEventListener('DOMContentLoaded', () => {
    new CookieBanner();
});

// Currency Converter
class CurrencyConverter {
    constructor() {
        this.modal = document.getElementById('currency-converter-modal');
        this.openBtn = document.getElementById('open-converter');
        this.closeBtn = document.getElementById('close-converter');
        this.fromCurrency = document.getElementById('from-currency');
        this.toCurrency = document.getElementById('to-currency');
        this.fromAmount = document.getElementById('from-amount');
        this.toAmount = document.getElementById('to-amount');
        this.swapBtn = document.getElementById('swap-currencies');
        this.rateDisplay = document.getElementById('rate-display');

        // Exchange rates (relative to KES)
        this.rates = {
            'USD': { buying: 125.5, selling: 130.8 },
            'GBP': { buying: 168.1, selling: 178.46 },
            'EUR': { buying: 145.66, selling: 155.92 },
            'ZAR': { buying: 6.42, selling: 9.44 },
            'JPY': { buying: 78.29, selling: 88.42 },
            'UGX': { buying: 22.85, selling: 29.42 },
            'TZS': { buying: 17.3, selling: 24.9 },
            'CNY': { buying: 15.51, selling: 21.76 },
            'RWF': { buying: 9.73, selling: 12.82 },
            'KES': { buying: 1, selling: 1 }
        };

        this.init();
    }

    init() {
        if (!this.modal || !this.openBtn) return;

        // Open modal
        this.openBtn.addEventListener('click', () => this.openModal());

        // Close modal
        this.closeBtn.addEventListener('click', () => this.closeModal());

        // Close when clicking outside
        this.modal.addEventListener('click', (e) => {
            if (e.target === this.modal) {
                this.closeModal();
            }
        });

        // Convert on input change
        this.fromAmount.addEventListener('input', () => this.convert());
        this.fromCurrency.addEventListener('change', () => this.convert());
        this.toCurrency.addEventListener('change', () => this.convert());

        // Swap currencies
        this.swapBtn.addEventListener('click', () => this.swapCurrencies());

        // Initial conversion
        this.convert();
    }

    openModal() {
        this.modal.classList.add('show');
        document.body.style.overflow = 'hidden';
    }

    closeModal() {
        this.modal.classList.remove('show');
        document.body.style.overflow = '';
    }

    getExchangeRate(from, to) {
        if (from === to) return 1;

        // Convert from source currency to KES
        const fromRate = this.rates[from];
        const toRate = this.rates[to];

        if (!fromRate || !toRate) return 1;

        // Use buying rate for converting TO KES, selling rate for converting FROM KES
        if (to === 'KES') {
            return fromRate.buying;
        } else if (from === 'KES') {
            return 1 / toRate.selling;
        } else {
            // Convert through KES: from -> KES -> to
            const toKES = fromRate.buying;
            const fromKES = 1 / toRate.selling;
            return toKES * fromKES;
        }
    }

    convert() {
        const from = this.fromCurrency.value;
        const to = this.toCurrency.value;
        const amount = parseFloat(this.fromAmount.value) || 0;

        const rate = this.getExchangeRate(from, to);
        const result = amount * rate;

        this.toAmount.value = result.toFixed(2);
        this.rateDisplay.textContent = `1 ${from} = ${rate.toFixed(4)} ${to}`;
    }

    swapCurrencies() {
        // Swap currency selections
        const tempCurrency = this.fromCurrency.value;
        this.fromCurrency.value = this.toCurrency.value;
        this.toCurrency.value = tempCurrency;

        // Swap amounts
        const tempAmount = this.fromAmount.value;
        this.fromAmount.value = this.toAmount.value;

        // Recalculate
        this.convert();
    }
}

// Initialize currency converter
document.addEventListener('DOMContentLoaded', () => {
    new CurrencyConverter();
});

// Product Carousel for Personal Banking
class ProductCarousel {
    constructor(trackId, prevBtnId, nextBtnId, indicatorsId) {
        this.track = document.getElementById(trackId);
        this.prevBtn = document.getElementById(prevBtnId);
        this.nextBtn = document.getElementById(nextBtnId);
        this.indicatorsContainer = document.getElementById(indicatorsId);
        this.currentIndex = 0;
        this.cardsPerView = 3;

        if (!this.track) return;

        this.cards = this.track.querySelectorAll('.product-card');
        this.totalCards = this.cards.length;

        this.init();
    }

    init() {
        this.updateCardsPerView();
        this.createIndicators();
        window.addEventListener('resize', () => this.updateCardsPerView());

        if (this.prevBtn) {
            this.prevBtn.addEventListener('click', () => this.prev());
        }
        if (this.nextBtn) {
            this.nextBtn.addEventListener('click', () => this.next());
        }

        this.updateCarousel();
    }

    updateCardsPerView() {
        const width = window.innerWidth;
        if (width <= 768) {
            this.cardsPerView = 1;
        } else if (width <= 1200) {
            this.cardsPerView = 2;
        } else {
            this.cardsPerView = 3;
        }
        this.updateCarousel();
    }

    prev() {
        if (this.currentIndex > 0) {
            this.currentIndex--;
            this.updateCarousel();
        }
    }

    next() {
        const maxIndex = this.totalCards - this.cardsPerView;
        if (this.currentIndex < maxIndex) {
            this.currentIndex++;
            this.updateCarousel();
        }
    }

    updateCarousel() {
        if (!this.cards.length) return;

        const cardWidth = this.cards[0].offsetWidth;
        const gap = 32;
        const offset = -(this.currentIndex * (cardWidth + gap));

        this.track.style.transform = `translateX(${offset}px)`;

        // Update buttons state
        if (this.prevBtn) {
            this.prevBtn.disabled = this.currentIndex === 0;
        }
        if (this.nextBtn) {
            this.nextBtn.disabled = this.currentIndex >= this.totalCards - this.cardsPerView;
        }

        // Update indicators
        this.updateIndicators();
    }

    createIndicators() {
        if (!this.indicatorsContainer) return;

        this.indicatorsContainer.innerHTML = '';
        const maxIndex = this.totalCards - this.cardsPerView;

        for (let i = 0; i <= maxIndex; i++) {
            const indicator = document.createElement('div');
            indicator.classList.add('indicator');
            if (i === 0) indicator.classList.add('active');
            indicator.addEventListener('click', () => {
                this.currentIndex = i;
                this.updateCarousel();
            });
            this.indicatorsContainer.appendChild(indicator);
        }
    }

    updateIndicators() {
        if (!this.indicatorsContainer) return;

        const indicators = this.indicatorsContainer.querySelectorAll('.indicator');
        indicators.forEach((indicator, index) => {
            if (index === this.currentIndex) {
                indicator.classList.add('active');
            } else {
                indicator.classList.remove('active');
            }
        });
    }
}

// Initialize product carousels
document.addEventListener('DOMContentLoaded', () => {
    new ProductCarousel(
        'personal-products-track',
        'prev-personal-product',
        'next-personal-product',
        'personal-indicators'
    );

    new ProductCarousel(
        'business-products-track',
        'prev-business-product',
        'next-business-product',
        'business-indicators'
    );
});
