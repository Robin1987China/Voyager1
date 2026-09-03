import { t } from '@/i18n'

/**
 * 项目 DSL 示例
 */
export const PROJECT_DSL_DEFATUL =
  t('i18n_3f5af13b4b') +
  '\n' +
  t('i18n_13c76c38b7') +
  '\n' +
  'description: ' +
  t('i18n_db06c78d1e') +
  '\n' +
  'run:\r\n' +
  '  start:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: start\r\n' +
  '    scriptEnv:\r\n' +
  '      "boot_active": test\r\n' +
  '  status:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: status\r\n' +
  '  stop:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: stop\r\n' +
  '#  restart:\r\n' +
  '##    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '#    scriptId: \r\n' +
  '#    scriptArgs: restart\r\n' +
  '#    scriptEnv:\r\n' +
  '#      "boot_active": test\r\n' +
  '#  reload:\r\n' +
  '##    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '#    scriptId: \r\n' +
  '#    scriptArgs: reload\r\n' +
  '#    scriptEnv:\r\n' +
  '#      "boot_active": test\r\n' +
  '#  fileChangeReload: true\r\n' +
  t('i18n_8d6d47fbed') +
  '\n#  execPath: ./\r\n' +
  'file:\r\n' +
  t('i18n_0eccc9451d') +
  '\n#  backupCount: 5\r\n' +
  t('i18n_8ba977b4b7') +
  "\n#  backupSuffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]\r\n" +
  t('i18n_7b61408779') +
  '\n#  backupPath: /data/voyager1_backup\r\n' +
  t('i18n_96972aa0df') +
  '\n#  diffBackup: true\r\n' +
  'config:\r\n' +
  t('i18n_0d467f7889') +
  '\n#  autoBackToFile: true\r\n' +
  '\r\n'

/**
 * 定时 cron 默认提示
 *
 * https://www.npmjs.com/package/cron-parser
 */
export const CRON_DATA_SOURCE = [
  {
    title: t('i18n_6948363f65'),
    options: [
      {
        title: '',
        value: ''
      }
    ]
  },
  {
    title: t('i18n_d5d46dd79b'),
    options: [
      {
        title: t('i18n_76ebb2be96'),
        value: '0 0/1 * * * ?'
      },
      {
        title: t('i18n_b2f296d76a'),
        value: '0 0/5 * * * ?'
      },
      {
        title: t('i18n_3bdab2c607'),
        value: '0 0/10 * * * ?'
      },
      {
        title: t('i18n_751a79afde'),
        value: '0 0/30 * * * ?'
      }
    ]
  },
  {
    title: t('i18n_99b3c97515'),
    options: [
      {
        title: t('i18n_860c00f4f7'),
        value: '0 0 0/1 * * ?'
      }
    ]
  },
  {
    title: t('i18n_15fa91e3ab'),
    options: [
      {
        title: t('i18n_616879745d'),
        value: '0 0 0,12 * * ?'
      },
      {
        title: t('i18n_8844085e15'),
        value: '0 0 0 * * ?'
      }
    ]
  },
  {
    title: t('i18n_8da42dd738'),
    options: [
      {
        title: t('i18n_6334eec584'),
        value: '0/5 * * * * ?'
      },
      {
        title: t('i18n_14a25beebb'),
        value: '0/10 * * * * ?'
      },
      {
        title: t('i18n_354a3dcdbd'),
        value: '0/30 * * * * ?'
      }
    ]
  }
]
