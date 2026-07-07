/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ['class'],
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // FinTech dark slate palette
        surface: {
          DEFAULT: '#0F1117',   // page background
          card:    '#171B26',   // card / sidebar background
          raised:  '#1E2330',   // elevated surface (inputs, hover)
          border:  '#2A3045',   // subtle dividers
        },
        primary: {
          DEFAULT: '#3B82F6',   // electric blue – primary actions
          hover:   '#2563EB',
          muted:   '#1D3A6E',   // low-key backgrounds
        },
        accent: {
          DEFAULT: '#06B6D4',   // cyan – AI/assistant identity
          muted:   '#0E4F5C',
        },
        success: '#10B981',
        warning: '#F59E0B',
        danger:  '#EF4444',
        text: {
          primary:   '#F1F5F9',
          secondary: '#94A3B8',
          muted:     '#475569',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      borderRadius: {
        lg: '0.75rem',
        xl: '1rem',
        '2xl': '1.25rem',
      },
      boxShadow: {
        card:  '0 1px 3px rgba(0,0,0,0.4), 0 1px 2px rgba(0,0,0,0.3)',
        panel: '0 4px 24px rgba(0,0,0,0.5)',
        glow:  '0 0 20px rgba(59,130,246,0.15)',
      },
      animation: {
        'fade-in':    'fadeIn 0.2s ease-out',
        'slide-up':   'slideUp 0.3s ease-out',
        'pulse-slow': 'pulse 2s cubic-bezier(0.4,0,0.6,1) infinite',
      },
      keyframes: {
        fadeIn:  { from: { opacity: '0' }, to: { opacity: '1' } },
        slideUp: { from: { opacity: '0', transform: 'translateY(8px)' }, to: { opacity: '1', transform: 'translateY(0)' } },
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
}
