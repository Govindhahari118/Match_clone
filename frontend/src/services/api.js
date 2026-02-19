import axios from 'axios';

const api = axios.create({
    baseURL: "http://localhost:4000/api",
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add a request interceptor to include the auth token
api.interceptors.request.use(
    (config) => {
        if (typeof window !== 'undefined') {
            const token = localStorage.getItem('access_token');
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Add a response interceptor to handle token expiration
api.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        if (error.response && error.response.status === 401) {
            // Handle unauthorized access (e.g., redirect to login)
            // For now, just clear token and reject
            if (typeof window !== 'undefined') {
                localStorage.removeItem('access_token');
                localStorage.removeItem('user');
                // window.location.href = '/login'; // Optional: force redirect
            }
        }
        return Promise.reject(error);
    }
);

export default api;
