import axios from './config'

/**
 *
 * @param data
 */
export function cronTools(data) {
  return axios({
    url: '/tools/cron',
    method: 'get',
    params: data
  })
}

export function ipList(data) {
  return axios({
    url: '/tools/ip-list',
    method: 'get',
    params: data
  })
}

export function netPing(data) {
  return axios({
    url: '/tools/net-ping',
    method: 'get',
    params: data
  })
}

export function netTelnet(data) {
  return axios({
    url: '/tools/net-telnet',
    method: 'get',
    params: data
  })
}
