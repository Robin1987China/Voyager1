import { t } from '@/i18n'
import axios from '../config'

export function userLoginLgin(params) {
  return axios({
    url: '/user/login-log/list-data',
    method: 'post',
    data: params
  })
}

export const operateCodeMap = {
  0: t('i18n_dd95bf2d45'),
  1: t('i18n_5a5368cf9b'),
  2: t('i18n_18d49918f5'),
  3: t('i18n_a093ae6a6e'),
  4: t('i18n_8b63640eee'),
  6: 'oauth2'
}
