import { ElMessage } from 'element-plus'

/**
 * Creates an SSE connection using native fetch with streaming support.
 * Supports POST requests with streaming responses.
 *
 * @param {string} url - The API endpoint URL
 * @param {object} options - Configuration options
 * @param {object} options.body - POST request body
 * @param {function} options.onMessage - Callback for each message chunk
 * @param {function} options.onError - Callback for errors
 * @param {function} options.onComplete - Callback when stream completes
 * @param {AbortSignal} options.signal - AbortSignal for cancellation
 */
export function createSseConnection(url, options = {}) {
  const { body, onMessage, onError, onComplete, signal } = options

  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  fetch(url, {
    method: 'POST',
    headers,
    body: body ? JSON.stringify(body) : undefined,
    signal
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          onComplete && onComplete()
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n\n')
        buffer = lines.pop()

        for (const line of lines) {
          if (line.trim()) {
            const dataLine = line.split('\n').find((l) => l.startsWith('data:'))
            if (dataLine) {
              const data = dataLine.substring(5).trim()
              if (data && onMessage) {
                onMessage(data)
              }
            }
          }
        }
      }
    })
    .catch((error) => {
      if (error.name === 'AbortError') return
      if (onError) {
        onError(error)
      } else {
        ElMessage({
          message: `SSE连接错误: ${error.message}`,
          type: 'error',
          duration: 3000
        })
      }
    })
}

/**
 * Streams AI consultation messages via SSE.
 *
 * @param {string} message - The user's message
 * @param {string} sessionId - The consultation session ID
 * @param {function} onMessage - Callback receiving incremental text chunks
 * @param {function} onError - Callback for stream errors
 * @param {function} onComplete - Callback when stream ends
 * @returns {AbortController} Controller to cancel the stream
 */
export function streamChat(message, sessionId, onMessage, onError, onComplete) {
  const controller = new AbortController()

  const url = '/api/patient/consultations/sse'
  const body = {
    message,
    sessionId
  }

  createSseConnection(url, {
    body,
    onMessage: (data) => {
      try {
        const parsed = JSON.parse(data)
        if (parsed.content) {
          onMessage(parsed.content)
        }
      } catch {
        onMessage(data)
      }
    },
    onError,
    onComplete,
    signal: controller.signal
  })

  return controller
}
