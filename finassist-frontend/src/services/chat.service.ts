import api from './api'
import type { ChatRequest, ChatResponse } from '@/types'

export const chatService = {

  async sendQuestion(request: ChatRequest): Promise<ChatResponse> {
    const { data } = await api.post<ChatResponse>('/chat', request)
    return data
  },
}
