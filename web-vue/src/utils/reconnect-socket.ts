/**
 * 带自动重连的 WebSocket 封装
 *
 * 用法与原生 WebSocket 一致（赋值式 onopen/onmessage/onerror/onclose + send/close），
 * 非主动关闭时按 retryDelay 间隔自动重连，重连成功后重新触发 onopen（页面心跳/订阅随之恢复）。
 * 重试次数耗尽后才回调 onclose（此时页面再提示“连接已关闭”）。
 */
export class ReconnectSocket {
  onopen: ((ev: Event) => void) | null = null
  onmessage: ((ev: MessageEvent) => void) | null = null
  onerror: ((ev: Event) => void) | null = null
  onclose: ((ev: CloseEvent) => void) | null = null

  private readonly url: string
  private socket: WebSocket | null = null
  private manualClose = false
  private retryCount = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private readonly maxRetries: number
  private readonly retryDelay: number

  constructor(url: string, options: { maxRetries?: number; retryDelay?: number } = {}) {
    this.url = url
    this.maxRetries = options.maxRetries ?? 5
    this.retryDelay = options.retryDelay ?? 3000
    this.connect()
  }

  private connect() {
    this.socket = new WebSocket(this.url)
    this.socket.onopen = (ev) => {
      this.retryCount = 0
      this.onopen?.(ev)
    }
    this.socket.onmessage = (ev) => {
      this.onmessage?.(ev)
    }
    this.socket.onerror = (ev) => {
      this.onerror?.(ev)
    }
    this.socket.onclose = (ev) => {
      if (this.manualClose) {
        return
      }
      if (this.retryCount < this.maxRetries) {
        // 静默重连，成功前不打搅页面
        this.reconnectTimer = setTimeout(() => {
          this.retryCount++
          this.connect()
        }, this.retryDelay)
      } else {
        // 重试耗尽，通知页面走“连接已关闭”逻辑
        this.onclose?.(ev)
      }
    }
  }

  get readyState(): number {
    return this.socket?.readyState ?? WebSocket.CLOSED
  }

  send(data: string | ArrayBufferLike | Blob | ArrayBufferView) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(data)
    }
  }

  /** 主动关闭（组件卸载/页面离开时调用，不再自动重连） */
  close() {
    this.manualClose = true
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.socket?.close()
    this.socket = null
  }
}
