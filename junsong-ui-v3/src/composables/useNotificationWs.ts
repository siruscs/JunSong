import { ref, onBeforeUnmount } from 'vue'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getToken } from '@/utils/auth'

export interface WsNotification {
  id: number
  title: string
  content: string
  type: string
  linkUrl: string | null
  bizId: string | null
  createTime: string
}

/**
 * WebSocket 通知 composable
 *
 * 通过 STOMP over WebSocket 订阅实时通知推送。
 * 连接失败时静默降级（前端继续使用 30s 轮询兜底）。
 *
 * 用法：
 *   const { connected, lastNotification, connect, disconnect } = useNotificationWs()
 *   connect()
 *   watch(lastNotification, (n) => { ... })
 */
export function useNotificationWs() {
  const connected = ref(false)
  const lastNotification = ref<WsNotification | null>(null)
  let client: Client | null = null

  function connect() {
    if (client?.connected) return

    const baseApi = import.meta.env.VITE_APP_BASE_API || ''
    const wsUrl = `${baseApi}/system/ws/notification`

    client = new Client({
      webSocketFactory: () =>
        new SockJS(wsUrl, undefined, { transports: ['xhr-streaming', 'xhr-polling'] }) as WebSocket,
      connectHeaders: {
        Authorization: `Bearer ${getToken() || ''}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
      onConnect: () => {
        connected.value = true
        // 订阅个人通知队列
        client?.subscribe('/user/queue/notifications', (message: IMessage) => {
          try {
            const payload = JSON.parse(message.body) as WsNotification
            lastNotification.value = payload
          } catch {
            // 忽略解析错误
          }
        })
        // 订阅全站广播
        client?.subscribe('/topic/notification-broadcast', (message: IMessage) => {
          try {
            const payload = JSON.parse(message.body) as WsNotification
            lastNotification.value = payload
          } catch {
            // 忽略解析错误
          }
        })
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: () => {
        connected.value = false
      },
      onWebSocketError: () => {
        connected.value = false
      },
      onWebSocketClose: () => {
        connected.value = false
      },
    })

    client.activate()
  }

  function disconnect() {
    if (client) {
      client.deactivate()
      client = null
    }
    connected.value = false
  }

  onBeforeUnmount(() => {
    disconnect()
  })

  return { connected, lastNotification, connect, disconnect }
}
