'use client';

export default function GlobalError({ error, reset }) {
    return (
        <html>
            <body style={{ margin: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', background: '#fff1f2', fontFamily: 'system-ui, sans-serif' }}>
                <div style={{ textAlign: 'center', padding: '2rem', background: 'white', borderRadius: '16px', boxShadow: '0 4px 20px rgba(0,0,0,0.1)', maxWidth: 400 }}>
                    <div style={{ fontSize: 48, marginBottom: '1rem' }}>💔</div>
                    <h2 style={{ fontSize: 24, fontWeight: 800, color: '#be123c', marginBottom: '0.5rem' }}>Something went wrong!</h2>
                    <p style={{ color: '#4b5563', marginBottom: '1.5rem', lineHeight: 1.5 }}>
                        We encountered an unexpected error. Please try refreshing the page.
                    </p>
                    <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                        <button
                            onClick={() => reset()}
                            style={{ padding: '10px 24px', background: '#be123c', color: 'white', border: 'none', borderRadius: 99, fontWeight: 700, cursor: 'pointer' }}
                        >
                            Try Again
                        </button>
                        <button
                            onClick={() => window.location.href = '/'}
                            style={{ padding: '10px 24px', background: '#f1f5f9', color: '#334155', border: 'none', borderRadius: 99, fontWeight: 700, cursor: 'pointer' }}
                        >
                            Go Home
                        </button>
                    </div>
                </div>
            </body>
        </html>
    );
}
