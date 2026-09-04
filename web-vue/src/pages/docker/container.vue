<template>
  <div>
    <template v-if="type === 'container'">
            <n-card size="small" :body-style="{ padding: '12px' }" style="margin-bottom: 12px">

          <n-space>
            <n-input
              v-model:value="listQuery['name']"
              :placeholder="$t('i18n_d7ec2d3fea')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['containerId']"
              :placeholder="$t('i18n_74dc77d4f7')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['imageId']"
              :placeholder="$t('i18n_72e7a5d105')"
              class="search-input-item"
              @keyup.enter="loadData"
            />
            <div>
              {{ $t('i18n_607e7a4f37') }}
              <n-switch
                v-model:value="listQuery['showAll']"
                :checked-label="$t('i18n_9a7b52fc86')"
                :unchecked-label="$t('i18n_d679aea3aa')"
              />
            </div>
            <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
            <n-statistic format="s" :title="$t('i18n_0f8403d07e') + ' '" :value="countdownTime" @finish="autoUpdate">
              <template #suffix>
                <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
              </template>
            </n-statistic>
          </n-space>
        
      </n-card>
<n-data-table
        :data="list"
        size="medium"
        :columns="columns"
        :pagination="false"
        bordered
        :row-key="(row) => row.id"
        >
        
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.key === 'names'">
            <n-popover>
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ (text || []).join(',') }}</span>
                  </span>
                </span>
              </template>
              <template #header>{{ `${$t('i18n_0c256f73b8')}${(text || []).join(',')}` }}</template>

              <p>{{ $t('i18n_abee751418') }}{{ record.id }}</p>
              <p>{{ $t('i18n_26eccfaad1') }}{{ record.image }}</p>
              <p>{{ $t('i18n_c87bd94cd7') }}{{ record.imageId }}</p>
            </n-popover>
          </template>

          <template v-else-if="column.key === 'labels'">
            <n-popover>
              <template #trigger>
                <template v-if="record.labels && Object.keys(record.labels).length">
                  <span>{{ (record.labels && Object.keys(record.labels).length) || 0 }} <TagsOutlined /></span>
                </template>
                <template v-else>-</template>
              </template>
              <template #header>{{ `${$t('i18n_89cfb655e0')}` }}</template>

              <template v-if="record.labels">
                <p v-for="(value, key) in record.labels" :key="key">
                  {{ key }}
                  <ArrowRightOutlined />

                  {{ value }}
                </p>
              </template>
            </n-popover>
          </template>
          <template v-else-if="column.key === 'mounts'">
            <n-popover>
              <template #trigger>
                <template v-if="record.mounts && Object.keys(record.mounts).length">
                  <span>{{ (record.mounts && Object.keys(record.mounts).length) || 0 }} <ApiOutlined /></span>
                </template>
                <template v-else>-</template>
              </template>
              <template #header>{{ `${$t('i18n_9964d6ed3f')}` }}</template>

              <template v-if="record.mounts">
                <div v-for="(item, index) in record.mounts" :key="index">
                  <p>
                    {{ $t('i18n_5b47861521') }}{{ item.name }}
                    <n-tag>{{ item.rw ? $t('i18n_2300ad28b8') : $t('i18n_75769d1ac8') }}</n-tag>
                  </p>
                  <p>{{ $t('i18n_e362bc0e8a') }}</p>
                  <n-divider></n-divider>
                </div>
              </template>
            </n-popover>
          </template>

          <template v-else-if="column.tooltip">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{ text }}</span>
                  </span>
                </span>
              </template>
              text
            </n-tooltip>
          </template>

          <template v-else-if="column.showid">
            <n-tooltip placement="topLeft">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span style="display: none"> {{ array = text.split(':') }}</span>
                    <span>{{ array[array.length - 1].slice(0, 12) }}</span>
                  </span>
                </span>
              </template>
              text
            </n-tooltip>
          </template>

          <template v-else-if="column.key === 'ports'">
            <n-popover placement="top-start">
              <template #trigger>
                <span class="tw">
                  <span class="tw">
                    <span>{{
                      (text || [])
                        .slice(0, 2)
                        .map((item) => {
                          return item.type + ' ' + (item.publicPort || '') + ':' + item.privatePort
                        })
                        .join('/')
                    }}</span>
                  </span>
                </span>
              </template>
              <template #header>
                {{ $t('i18n_d94167ab19') }}
                <ul>
                  <li v-for="(item, index) in text || []" :key="index">
                    {{ item.type + ' ' + (item.ip || '') + ':' + (item.publicPort || '') + ':' + item.privatePort }}
                  </li>
                </ul>
              </template>

              <template v-if="record.networkSettings">
                <template v-if="record.networkSettings.networks">
                  <template v-if="record.networkSettings.networks.bridge">
                    {{ $t('i18n_71ee088528') }}
                    <p v-if="record.networkSettings.networks.bridge.ipAddress">
                      IP:
                      <n-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</n-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.bridge.macAddress">
                      MAC:
                      <n-tag>{{ record.networkSettings.networks.bridge.macAddress }}</n-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.bridge.gateway">
                      {{ $t('i18n_f332f2c8df') }}:
                      <n-tag>{{ record.networkSettings.networks.bridge.gateway }}</n-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.bridge.networkID">
                      networkID:
                      <n-tag>{{ record.networkSettings.networks.bridge.networkID }}</n-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.bridge.endpointId">
                      endpointId:
                      <n-tag>{{ record.networkSettings.networks.bridge.endpointId }}</n-tag>
                    </p>
                  </template>
                  <template v-if="record.networkSettings.networks.ingress">
                    <p v-if="record.networkSettings.networks.ingress.ipAddress">
                      IP:
                      <n-tag>{{ record.networkSettings.networks.ingress.ipAddress }}</n-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.ingress.macAddress">
                      MAC:
                      <n-tag>{{ record.networkSettings.networks.ingress.macAddress }}</n-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.ingress.gateway">
                      {{ $t('i18n_f332f2c8df') }}:
                      <n-tag>{{ record.networkSettings.networks.ingress.gateway }}</n-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.ingress.networkID">
                      networkID:
                      <n-tag>{{ record.networkSettings.networks.ingress.networkID }}</n-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.ingress.endpointId">
                      endpointId:
                      <n-tag>{{ record.networkSettings.networks.ingress.endpointId }}</n-tag>
                    </p>
                  </template>
                </template>
              </template>
            </n-popover>
          </template>

          <template v-else-if="column.key === 'state'">
            <n-tooltip @click="viewLog(record)">
              <template #trigger>
                <n-switch :value="text === 'running'" :disabled="true">
                  <template #checked>
                    <CheckCircleOutlined />
                  </template>
                  <template #unchecked>
                    <WarningOutlined />
                  </template>
                </n-switch>
              </template>
              {{ (record.status || '') + $t('i18n_aac62bc255') }}
            </n-tooltip>
          </template>
          <template v-else-if="column.key === 'operation'">
            <n-space>
              <template v-if="record.state === 'running'">
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" text :disabled="record.state !== 'running'" @click="handleTerminal(record)"
                        ><CodeOutlined
                      /></n-button>
                    </span>
                  </template>
                  $t('i18n_4fb2400af7')
                </n-tooltip>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" text @click="doAction(record, 'stop')"><StopOutlined /></n-button>
                    </span>
                  </template>
                  $t('i18n_095e938e2a')
                </n-tooltip>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" text @click="doAction(record, 'restart')"><ReloadOutlined /></n-button>
                    </span>
                  </template>
                  $t('i18n_01b4e06f39')
                </n-tooltip>
              </template>
              <template v-else>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" text @click="doAction(record, 'start')">
                        <PlayCircleOutlined />
                      </n-button>
                    </span>
                  </template>
                  $t('i18n_8e54ddfe24')
                </n-tooltip>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" text :disabled="true"><StopOutlined /></n-button>
                    </span>
                  </template>
                  $t('i18n_095e938e2a')
                </n-tooltip>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-button size="small" text :disabled="true"><ReloadOutlined /></n-button>
                    </span>
                  </template>
                  $t('i18n_01b4e06f39')
                </n-tooltip>
              </template>

              <n-dropdown
                :options="[
                  {
                    label: $t('i18n_9e09315960'),
                    key: '0',
                    icon: () => h(NIcon, null, { default: () => h(RedoOutlined) }),
                    props: { onClick: () => rebuild(record) }
                  },
                  {
                    label: $t('i18n_95b351c862'),
                    key: '1',
                    icon: () => h(NIcon, null, { default: () => h(EditOutlined) }),
                    disabled: record.state !== 'running',
                    props: { onClick: () => editContainer(record) }
                  },
                  {
                    label: $t('i18n_456d29ef8b'),
                    key: '2',
                    icon: () => h(NIcon, null, { default: () => h(MessageOutlined) }),
                    props: { onClick: () => viewLog(record) }
                  },
                  {
                    label: $t('i18n_2f4aaddde3'),
                    key: '3',
                    icon: () => h(NIcon, null, { default: () => h(DeleteOutlined) }),
                    props: { onClick: () => doAction(record, 'remove') }
                  }
                ]"
              >
                <a @click="(e) => e.preventDefault()">
                  <MoreOutlined />
                </a>
              </n-dropdown>
            </n-space>
          </template>
        </template>
      </n-data-table>
    </template>
    <template v-else-if="type === 'compose'">
      <n-card>
        <template #title>
          <n-space>
            <n-input
              v-model:value="listQuery['name']"
              :placeholder="$t('i18n_d7ec2d3fea')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['containerId']"
              :placeholder="$t('i18n_74dc77d4f7')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['imageId']"
              :placeholder="$t('i18n_72e7a5d105')"
              class="search-input-item"
              @keyup.enter="loadData"
            />
            <div>
              {{ $t('i18n_607e7a4f37') }}
              <n-switch
                v-model:value="listQuery['showAll']"
                :checked-label="$t('i18n_9a7b52fc86')"
                :unchecked-label="$t('i18n_d679aea3aa')"
              />
            </div>
            <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
            <n-statistic format="s" :title="$t('i18n_0f8403d07e') + ' '" :value="countdownTime" @finish="autoUpdate">
              <template #suffix>
                <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
              </template>
            </n-statistic>
          </n-space>
        </template>
        <n-collapse v-if="list && list.length">
          <n-collapse-item v-for="(item2, index) in list" :key="index">
            <template #header>
              <n-space>
                <span>{{ item2.name }}</span>
                <span>
                  <span style="display: none">
                    {{
                      array = (item2.child || []).map((item) => {
                        return item.state
                      })
                    }}
                    {{
                      runningCount = array
                        .map((item) => {
                          return item === 'running' ? 1 : 0
                        })
                        .reduce((prev, curr) => {
                          return prev + curr
                        }, 0)
                    }}</span
                  >
                  <span v-if="runningCount">Running({{ runningCount }}/{{ array.length }})</span>
                  <span v-else>Exited</span>
                </span>
              </n-space>
            </template>
            <n-data-table
              :data="item2.child"
              size="medium"
              :columns="columns"
              :pagination="false"
              bordered
              :row-key="(row) => row.id"
              >
              <template #bodyCell="{ column, text, record }">
                <template v-if="column.key === 'names'">
                  <n-popover>
                    <template #trigger>
                      <span class="tw">
                        <span class="tw">
                          <span>{{ (text || []).join(',') }}</span>
                        </span>
                      </span>
                    </template>
                    <template #header>{{ `${$t('i18n_0c256f73b8')}${(text || []).join(',')}` }}</template>

                    <p>{{ $t('i18n_abee751418') }}{{ record.id }}</p>
                    <p>{{ $t('i18n_26eccfaad1') }}{{ record.image }}</p>
                    <p>{{ $t('i18n_c87bd94cd7') }}{{ record.imageId }}</p>
                  </n-popover>
                </template>

                <template v-else-if="column.key === 'labels'">
                  <n-popover>
                    <template #trigger>
                      <template v-if="record.labels && Object.keys(record.labels).length">
                        <span>{{ (record.labels && Object.keys(record.labels).length) || 0 }} <TagsOutlined /></span>
                      </template>
                      <template v-else>-</template>
                    </template>
                    <template #header>{{ `${$t('i18n_89cfb655e0')}` }}</template>

                    <template v-if="record.labels">
                      <p v-for="(value, key) in record.labels" :key="key">
                        {{ key }}

                        <ArrowRightOutlined />
                        {{ value }}
                      </p>
                    </template>
                  </n-popover>
                </template>
                <template v-else-if="column.key === 'mounts'">
                  <n-popover>
                    <template #trigger>
                      <template v-if="record.mounts && Object.keys(record.mounts).length">
                        <span>{{ (record.mounts && Object.keys(record.mounts).length) || 0 }} <ApiOutlined /></span>
                      </template>
                      <template v-else>-</template>
                    </template>
                    <template #header>{{ `${$t('i18n_9964d6ed3f')}` }}</template>

                    <template v-if="record.mounts">
                      <div v-for="(item, idx) in record.mounts" :key="idx">
                        <p>
                          {{ $t('i18n_5b47861521') }}{{ item.name }}
                          <n-tag>{{ item.rw ? $t('i18n_2300ad28b8') : $t('i18n_75769d1ac8') }}</n-tag>
                        </p>
                        <p>
                          {{ $t('i18n_e362bc0e8a', { source: item.source, destination: item.destination }) }}
                        </p>
                        <n-divider></n-divider>
                      </div>
                    </template>
                  </n-popover>
                </template>

                <template v-else-if="column.tooltip">
                  <n-tooltip placement="topLeft">
                    <template #trigger>
                      <span class="tw">
                        <span class="tw">
                          <span>{{ text }}</span>
                        </span>
                      </span>
                    </template>
                    text
                  </n-tooltip>
                </template>

                <template v-else-if="column.showid">
                  <n-tooltip placement="topLeft">
                    <template #trigger>
                      <span class="tw">
                        <span class="tw">
                          <span style="display: none"> {{ array = text.split(':') }}</span>
                          <span>{{ array[array.length - 1].slice(0, 12) }}</span>
                        </span>
                      </span>
                    </template>
                    text
                  </n-tooltip>
                </template>

                <template v-else-if="column.key === 'ports'">
                  <n-popover placement="top-start">
                    <template #trigger>
                      <span class="tw">
                        <span class="tw">
                          <span>{{
                            (text || [])
                              .map((item) => {
                                return item.type + ' ' + (item.publicPort || '') + ':' + item.privatePort
                              })
                              .join('/')
                          }}</span>
                        </span>
                      </span>
                    </template>
                    <template #header>
                      {{ $t('i18n_d94167ab19') }}
                      <ul>
                        <li v-for="(item, idx) in text || []" :key="idx">
                          {{
                            item.type + ' ' + (item.ip || '') + ':' + (item.publicPort || '') + ':' + item.privatePort
                          }}
                        </li>
                      </ul>
                    </template>

                    <template v-if="record.networkSettings">
                      <template v-if="record.networkSettings.networks">
                        <template v-if="record.networkSettings.networks.bridge">
                          {{ $t('i18n_71ee088528') }}
                          <p v-if="record.networkSettings.networks.bridge.ipAddress">
                            IP:
                            <n-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</n-tag>
                          </p>
                          <p v-if="record.networkSettings.networks.bridge.macAddress">
                            MAC:
                            <n-tag>{{ record.networkSettings.networks.bridge.macAddress }}</n-tag>
                          </p>
                          <p v-if="record.networkSettings.networks.bridge.gateway">
                            {{ $t('i18n_f332f2c8df') }}:
                            <n-tag>{{ record.networkSettings.networks.bridge.gateway }}</n-tag>
                          </p>
                          <p v-if="record.networkSettings.networks.bridge.networkID">
                            networkID:
                            <n-tag>{{ record.networkSettings.networks.bridge.networkID }}</n-tag>
                          </p>
                          <p v-if="record.networkSettings.networks.bridge.endpointId">
                            endpointId:
                            <n-tag>{{ record.networkSettings.networks.bridge.endpointId }}</n-tag>
                          </p>
                        </template>
                        <template v-if="record.networkSettings.networks.ingress">
                          <p v-if="record.networkSettings.networks.ingress.ipAddress">
                            IP:
                            <n-tag>{{ record.networkSettings.networks.ingress.ipAddress }}</n-tag>
                          </p>
                          <p v-if="record.networkSettings.networks.ingress.macAddress">
                            MAC:
                            <n-tag>{{ record.networkSettings.networks.ingress.macAddress }}</n-tag>
                          </p>
                          <p v-if="record.networkSettings.networks.ingress.gateway">
                            {{ $t('i18n_f332f2c8df') }}:
                            <n-tag>{{ record.networkSettings.networks.ingress.gateway }}</n-tag>
                          </p>
                          <p v-if="record.networkSettings.networks.ingress.networkID">
                            networkID:
                            <n-tag>{{ record.networkSettings.networks.ingress.networkID }}</n-tag>
                          </p>
                          <p v-if="record.networkSettings.networks.ingress.endpointId">
                            endpointId:
                            <n-tag>{{ record.networkSettings.networks.ingress.endpointId }}</n-tag>
                          </p>
                        </template>
                      </template>
                    </template>
                  </n-popover>
                </template>

                <template v-else-if="column.key === 'state'">
                  <n-tooltip @click="viewLog(record)">
                    <template #trigger>
                      <n-switch :value="record.state === 'running'" :disabled="true">
                        <template #checked>
                          <CheckCircleOutlined />
                        </template>
                        <template #unchecked>
                          <WarningOutlined />
                        </template>
                      </n-switch>
                    </template>
                    {{ (record.status || '') + $t('i18n_aac62bc255') }}
                  </n-tooltip>
                </template>
                <template v-else-if="column.key === 'operation'">
                  <n-space>
                    <template v-if="record.state === 'running'">
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            <n-button size="small" text @click="handleTerminal(record)"><CodeOutlined /></n-button>
                          </span>
                        </template>
                        $t('i18n_4fb2400af7')
                      </n-tooltip>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            <n-button size="small" text @click="doAction(record, 'stop')"><StopOutlined /></n-button>
                          </span>
                        </template>
                        $t('i18n_095e938e2a')
                      </n-tooltip>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            <n-button size="small" text @click="doAction(record, 'restart')">
                              <ReloadOutlined />
                            </n-button>
                          </span>
                        </template>
                        $t('i18n_01b4e06f39')
                      </n-tooltip>
                    </template>
                    <template v-else>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            <n-button size="small" text @click="doAction(record, 'start')">
                              <PlayCircleOutlined />
                            </n-button>
                          </span>
                        </template>
                        $t('i18n_8e54ddfe24')
                      </n-tooltip>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            <n-button size="small" text :disabled="true"><StopOutlined /></n-button>
                          </span>
                        </template>
                        $t('i18n_095e938e2a')
                      </n-tooltip>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            <n-button size="small" text :disabled="true"><ReloadOutlined /></n-button>
                          </span>
                        </template>
                        $t('i18n_01b4e06f39')
                      </n-tooltip>
                    </template>

                    <n-dropdown
                      :options="[
                        {
                          label: $t('i18n_9e09315960'),
                          key: '0',
                          icon: () => h(NIcon, null, { default: () => h(RedoOutlined) }),
                          props: { onClick: () => rebuild(record) }
                        },
                        {
                          label: $t('i18n_95b351c862'),
                          key: '1',
                          icon: () => h(NIcon, null, { default: () => h(EditOutlined) }),
                          disabled: record.state !== 'running',
                          props: { onClick: () => editContainer(record) }
                        },
                        {
                          label: $t('i18n_456d29ef8b'),
                          key: '2',
                          icon: () => h(NIcon, null, { default: () => h(MessageOutlined) }),
                          props: { onClick: () => viewLog(record) }
                        },
                        {
                          label: $t('i18n_2f4aaddde3'),
                          key: '3',
                          icon: () => h(NIcon, null, { default: () => h(DeleteOutlined) }),
                          props: { onClick: () => doAction(record, 'remove') }
                        }
                      ]"
                    >
                      <a @click="(e) => e.preventDefault()">
                        <MoreOutlined />
                      </a>
                    </n-dropdown>
                  </n-space>
                </template>
              </template>
            </n-data-table>
          </n-collapse-item>
        </n-collapse>
        <n-empty v-else>
          <template #description>{{ $t('i18n_21efd88b67') }}</template>
        </n-empty>
      </n-card>

      <!-- <n-data-table
        :data="list"
        size="medium"
        :columns="parentColumns"
        :pagination="false"
        bordered
        >
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.tooltip">
            <n-tooltip placement="topLeft">
<template #trigger>
            <span class="tw">

            <span class="tw">


              <span>{{ text }}</span>
            

            </span>
          
            </span>
          </template>
text
</n-tooltip>
          </template>
          <template v-else-if="column.key === 'state'"> </template>
        </template>
        <template #expandedRowRender="{ record }"> </template>
      </n-data-table> -->
    </template>
    <!-- 日志 -->

    <log-view2
      v-if="logVisible > 0"
      :id="id"
      :show="logVisible != 0"
      :url-prefix="urlPrefix"
      :machine-docker-id="machineDockerId"
      :container-id="temp.id"
      @close="
        () => {
          logVisible = 0
        }
      "
    />

    <!-- Terminal -->
    <CustomModal
      v-if="terminalVisible"
      v-model:open="terminalVisible"
      width="80vw"
      :body-style="{
        padding: '0 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `70vh`
      }"
      :title="`docker cli ${(temp.names || []).join(',')}`"
      :footer="null"
      :mask-closable="false"
    >
      <terminal2 v-if="terminalVisible" :id="id" :machine-docker-id="machineDockerId" :container-id="temp.id" />
    </CustomModal>
    <!-- 编辑容器配置 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="$t('i18n_1ba584c974')"
      :mask-closable="false"
      @ok="
        () => {
          $refs.editContainer
            .handleEditOk()
            .then(() => {
              editVisible = false
              loadData()
            })
            .finally(() => {
              confirmLoading = false
            })
        }
      "
    >
      <editContainer
        :id="id"
        ref="editContainer"
        :machine-docker-id="machineDockerId"
        :url-prefix="urlPrefix"
        :container-id="temp.id"
      ></editContainer>
    </CustomModal>
    <!-- rebuild container -->

    <BuildContainer
      v-if="buildVisible"
      :id="id"
      :image-id="temp.imageId"
      :machine-docker-id="machineDockerId"
      :url-prefix="urlPrefix"
      :container-id="temp.id"
      :container-data="temp"
      @cancel-btn-click="
        () => {
          buildVisible = false
        }
      "
      @confirm-btn-click="
        () => {
          buildVisible = false
          loadData()
        }
      "
    />
  </div>
</template>
<script>
import {
  ApiOutlined,
  ArrowRightOutlined,
  CheckCircleOutlined,
  CodeOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  StopOutlined,
  TagsOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'
import { DeleteOutlined, EditOutlined, MessageOutlined, MoreOutlined, RedoOutlined } from '@ant-design/icons-vue'
import { parseTime } from '@/utils/const'
import {
  dockerContainerList,
  dockerContainerRemove,
  dockerContainerRestart,
  dockerContainerStart,
  dockerContainerStop,
  dockerContainerListCompose
} from '@/api/docker-api'
import LogView2 from '@/pages/docker/log-view'
import Terminal2 from '@/pages/docker/terminal'
import editContainer from './editContainer.vue'
import BuildContainer from './buildContainer.vue'
import { NEmpty as Empty } from 'naive-ui'
export default {
  name: 'Container',
  components: {
    LogView2,
    Terminal2,
    editContainer,
    BuildContainer
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      // container  or compose
      default: 'container'
    }
  },
  setup() {
    // 模板内联 dropdown options 的 icon 函数需访问模块作用域的 h/NIcon/图标组件
    return { h, NIcon, RedoOutlined, EditOutlined, MessageOutlined, DeleteOutlined }
  },
  data() {
    return {
      Empty,
      list: [],
      loading: false,
      listQuery: {
        showAll: true
      },
      terminalVisible: false,
      logVisible: 0,
      temp: {},
      confirmLoading: false,
      columns: [
        {
          title: this.$t('i18n_faaadc447b'),
          width: '60px',
          // ellipsis: true,
          align: 'center',
          render: (row, index) => `${index + 1}`
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'names',
          ellipsis: true
          // width: 150
        },
        {
          title: this.$t('i18n_87d50f8e03'),
          key: 'id',
          ellipsis: true,
          width: '10px',
          showid: true
        },
        {
          title: this.$t('i18n_40aff14380'),
          key: 'imageId',
          ellipsis: true,
          width: '130px',
          showid: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'state',
          // ellipsis: true,
          align: 'center',
          width: '80px'
        },

        {
          title: this.$t('i18n_c76cfefe72'),
          key: 'ports',
          ellipsis: true,
          width: '100px'
        },

        {
          title: this.$t('i18n_14d342362f'),
          key: 'labels',
          ellipsis: true,
          width: '50px'
        },
        {
          title: this.$t('i18n_9964d6ed3f'),
          key: 'mounts',
          ellipsis: true,
          width: '50px'
        },
        {
          title: this.$t('i18n_ddf7d2a5ce'),
          key: 'command',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'created',
          ellipsis: true,
          sorter: (a, b) => Number(a.created) - new Number(b.created),
          defaultSortOrder: 'descend',
          render: (row) => parseTime(row['created']),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          fixed: 'right',

          width: '160px'
        }
      ],

      // parentColumns: [
      //   {
      //     title: '序号',
      //     width: '80px',
      //     ellipsis: true,
      //     align: 'center',
      //     render: (row, index) => `${index + 1}`
      //   },
      //   {
      //     title: '名称',
      //     width: 200,
      //     key: 'name',
      //     ellipsis: true,
      //     tooltip: true
      //   },
      //   {
      //     title: '状态',
      //     key: 'state',
      //     width: '150px',
      //     ellipsis: true
      //   },
      //   {
      //     title: '操作',
      //     width: '80px',
      //     ellipsis: true
      //   }
      // ],
      action: {
        remove: {
          msg: this.$t('i18n_c469afafe0'),
          api: dockerContainerRemove
        },
        stop: {
          msg: this.$t('i18n_60b4c08f5c'),
          api: dockerContainerStop
        },
        restart: {
          msg: this.$t('i18n_bf77165638'),
          api: dockerContainerRestart
        },
        start: {
          msg: this.$t('i18n_2b0aa77353'),
          api: dockerContainerStart
        }
      },
      editVisible: false,

      countdownTime: Date.now(),

      buildVisible: false
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  beforeUnmount() {},
  mounted() {
    this.autoUpdate()
  },
  methods: {
    autoUpdate() {
      this.loadData()
    },
    // 加载数据
    loadData() {
      if (!this.visible) {
        return
      }
      this.loading = true
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.reqDataId
      ;(this.type === 'container'
        ? dockerContainerList(this.urlPrefix, this.listQuery)
        : dockerContainerListCompose(this.urlPrefix, this.listQuery)
      ).then((res) => {
        if (res.code === 200) {
          this.list = this.sortPort(res.data || []).map((item) => {
            let child = item.child
            if (child) {
              child = this.sortPort(child)
            }
            return { ...item, child: child }
          })
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    sortPort(list) {
      return list.map((item) => {
        let ports = item.ports
        if (ports) {
          try {
            ports = ports.sort(
              (a, b) =>
                a.privatePort - b.privatePort ||
                (a.type || '').toLowerCase().localeCompare((b.type || '').toLowerCase())
            )
          } catch (e) {
            console.error(e)
          }
        }

        return { ...item, ports: ports }
      })
    },
    doAction(record, actionKey) {
      const action = this.action[actionKey]
      if (!action) {
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: action.msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return action
            .api(this.urlPrefix, {
              id: this.reqDataId,
              containerId: record.id
            })
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                this.loadData()
              }
            })
        }
      })
    },
    viewLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    },
    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
    },
    editContainer(record) {
      this.temp = Object.assign({}, record)
      this.editVisible = true
      // console.log(this.temp);
    },
    // click rebuild button
    rebuild(record) {
      this.temp = Object.assign({}, record)
      this.buildVisible = true
    }
  }
}
</script>
<style scoped>
:deep(.n-statistic .n-statistic-value__content),
:deep(.n-statistic .n-statistic-value__prefix),
:deep(.n-statistic .n-statistic-value__suffix) {
  font-size: 16px;
}
</style>
