import axios from 'axios'

const api = axios.create({
  baseURL:  '/api',
  timeout: 60_000, // 60 s — RAG calls can be slow while Ollama generates
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => Promise.reject(error),
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message: string =
      error.response?.data?.message ??
      error.response?.data?.error ??
      error.message ??
      'An unexpected error occurred'

    return Promise.reject({ message, status: error.response?.status })
  },
)

export default api
