<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="certificate-list"
      :empty-description="$t('i18n_8c2da7cce9')"
      :data="list"
      size="medium"
      :loading="loading"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
      @refresh="loadData"
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-space>
            <n-input
              v-model:value="listQuery['%issuerDnName%']"
              clearable
              class="search-input-item"
              :placeholder="$t('i18n_f0aba63ae7')"
              @press-enter="loadData"
            />
            <n-input
              v-model:value="listQuery['%subjectDnName%']"
              clearable
              class="search-input-item"
              :placeholder="$t('i18n_9970ad0746')"
              @press-enter="loadData"
            />
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
                </span>
              </template>
              $t('i18n_4838a3bd20')
            </n-tooltip>
            <n-button type="primary" @click="handleAdd">{{ $t('i18n_c1690fcca5') }}</n-button>
          </n-space>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
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
        <template v-else-if="column.dataIndex === 'serialNumberStr'">
          <n-popover>
            <template #trigger>
              <span class="tw">
                <!-- {{ text }} -->
                <n-button text style="padding: 0" size="small" @click="handleEdit(record)">{{ text }}</n-button>
              </span>
            </template>
            <template #header>{{ $t('i18n_5dc1f36a27') }}</template>

            <p>{{ $t('i18n_4a4e3b5ae4') }}{{ record.description }}</p>
          </n-popover>
        </template>
        <template v-else-if="column.dataIndex === 'fileExists'">
          <n-tag v-if="text" color="green">{{ $t('i18n_df9497ea98') }}</n-tag>
          <n-tag v-else color="red">{{ $t('i18n_162e219f6d') }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <n-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</n-tag>
          <n-tag v-else>{{ $t('i18n_98d69f8b62') }}</n-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleDeployFile(record)">{{
              $t('i18n_a9f94dcd57')
            }}</n-button>
            <n-button size="small" type="primary" @click="handleDownload(record)">{{ $t('i18n_55405ea6ff') }}</n-button>
            <n-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</n-button>
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- 导入 -->
    <CustomModal
      v-if="editCertVisible"
      v-model:open="editCertVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="700px"
      :title="$t('i18n_c1690fcca5')"
      :mask-closable="false"
      @ok="handleEditCertOk"
    >
      <n-form ref="importCertForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_929e857766')" path="type">
          <n-radio-group v-model:value="temp.type">
            <n-radio value="pkcs12"> pkcs12(pfx) </n-radio>
            <n-radio value="JKS"> JKS </n-radio>
            <n-radio value="X.509"> X.509(pem、key、crt、cer) </n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item :label="$t('i18n_94aa195397')" path="file">
          <n-upload
            v-if="temp.type"
            v-model:file-list="uploadFileList"
            :custom-request="
              (file) => {
                uploadFileList = [file]
                return false
              }
            "
            :accept="typeAccept[temp.type]"
            @remove="
              () => {
                uploadFileList = []
                return true
              }
            "
          >
            <n-button><UploadOutlined />{{ $t('i18n_fd7e0c997d') }}</n-button>
          </n-upload>
          <template v-else>{{ $t('i18n_c3512a3d09') }}</template>
        </n-form-item>
        <n-form-item
          v-if="temp.type && temp.type !== 'X.509'"
          :label="$t('i18n_45028ad61d')"
          path="password"
          :help="$t('i18n_e8f07c2186')"
        >
          <n-input v-model:value="temp.password" :placeholder="$t('i18n_45028ad61d')" />
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 编辑证书 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_d47ea92b3a')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <n-form ref="editForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_e475e0c655')" path="global">
          <n-radio-group v-model:value="temp.global">
            <n-radio :value="true"> {{ $t('i18n_2be75b1044') }} </n-radio>
            <n-radio :value="false"> {{ $t('i18n_691b11e443') }} </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item :label="$t('i18n_5dc1f36a27')" path="description">
          <n-input v-model:value="temp.description" type="textarea" :placeholder="$t('i18n_066431a665')" />
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 发布文件 -->
    <CustomModal
      v-if="releaseFileVisible"
      v-model:open="releaseFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_9ae40638d2')"
      width="70%"
      :mask-closable="false"
      @ok="releaseFileOk()"
    >
      <n-alert :title="$t('i18n_a62fa322b4')" type="info" show-icon style="margin-bottom: 10px" />
      <releaseFile v-if="releaseFileVisible" ref="releaseFile" @commit="handleCommitTask"></releaseFile>
    </CustomModal>
  </div>
</template>
<script>
import { UploadOutlined } from '@ant-design/icons-vue'

import {
  certificateImportFile,
  certList,
  deleteCert,
  downloadCert,
  certificateEdit,
  certificateDeploy,
  certListAll
} from '@/api/tools/certificate'
import { parseTime, CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY } from '@/utils/const'
import releaseFile from '@/pages/file-manager/fileStorage/releaseFile.vue'
export default {
  components: {
    releaseFile
  },
  props: {
    showAll: {
      type: Boolean,
      default: false
    }
  },
  emits: ['confirm'],
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      list: [],
      uploadFileList: [],
      typeAccept: {
        pkcs12: '.pfx,.zip',
        JKS: '.jks,.zip',
        'X.509': '.zip'
      },
      temp: {},
      editCertVisible: false,

      columns: [
        {
          title: this.$t('i18n_30aaa13963'),
          key: 'serialNumberStr',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_929e857766'),
          key: 'keyType',
          ellipsis: true,
          width: '80px'
        },
        {
          title: this.$t('i18n_a3d0154996'),
          key: 'fileExists',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$t('i18n_fffd3ce745'),
          key: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_f0aba63ae7'),
          key: 'issuerDnName',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_9970ad0746'),
          key: 'subjectDnName',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_3a6c2962e1'),
          key: 'sigAlgName',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_4f08d1ad9f'),
          key: 'sigAlgOid',
          ellipsis: true,
          width: 150
        },

        {
          title: this.$t('i18n_fc92e93523'),
          key: 'effectiveTime',
          render: (row) => parseTime(row['effectiveTime']),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('i18n_22e888c2df'),
          key: 'expirationTime',
          sorter: true,
          render: (row) => parseTime(row['expirationTime']),
          width: '170px'
        },
        {
          title: this.$t('i18n_d0b2958432'),
          key: 'certVersion',
          ellipsis: true,
          width: '80px'
        },
        {
          title: this.$t('i18n_95a43eaa59'),
          key: 'createUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',
          align: 'center',
          fixed: 'right',

          width: '180px'
        }
      ],

      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        // name: [{ required: true, message: "Please input name", trigger: "blur" }],
        // path: [{ required: true, message: "Please select path", trigger: "blur" }],
        type: [{ required: true, message: this.$t('i18n_ac408e4b03'), trigger: 'blur' }]
      },
      releaseFileVisible: false,
      editVisible: false,
      confirmLoading: false,
      tableSelections: []
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: 'radio'
      }
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    CHANGE_PAGE,

    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      const api = this.showAll ? certListAll : certList
      api(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },

    // 新增
    handleAdd() {
      this.temp = {}
      this.editCertVisible = true
      this.uploadFileList = []
      this.$refs['importCertForm']?.restoreValidation()
    },
    // // 修改
    // handleEdit(record) {
    //   this.temp = Object.assign({}, record);
    //   this.uploadFileList = [];
    //   this.editCertVisible = true;
    // },

    // 提交 Cert 数据
    handleEditCertOk() {
      // 检验表单
      this.$refs['importCertForm'].validate().then(() => {
        if (this.uploadFileList.length === 0) {
          $notification.error({
            message: this.$t('i18n_4244830033')
          })
          return false
        }
        const formData = new FormData()
        formData.append('file', this.uploadFileList[0])
        formData.append('type', this.temp.type)
        formData.append('password', this.temp.password || '')

        // 提交数据
        this.confirmLoading = true
        certificateImportFile(formData)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editCertVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_efe9d26148'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteCert({
            id: record.id
          }).then((res) => {
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
    // 下载证书文件
    handleDownload(record) {
      // 请求参数
      const params = {
        id: record.id
      }
      // 请求接口拿到 blob
      window.open(downloadCert(params), '_blank')
    },
    // 编辑
    handleEdit(item) {
      this.temp = {
        ...item,
        global: item.workspaceId === 'GLOBAL',
        workspaceId: ''
      }
      this.editVisible = true
      this.$refs['editForm']?.restoreValidation()
    },
    // 编辑确认
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        this.confirmLoading = true
        certificateEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    handleDeployFile(record) {
      this.releaseFileVisible = true
      this.temp = { id: record.id }
    },

    handleCommitTask(data) {
      this.confirmLoading = true
      certificateDeploy({ ...data, id: this.temp.id })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })

            this.releaseFileVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },

    releaseFileOk() {
      this.$refs.releaseFile?.tryCommit()
    },
    // 确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.error({
          message: this.$t('i18n_94ca71ae7b')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })[0]

      this.$emit('confirm', `${selectData.serialNumberStr}:${selectData.keyType}`)
    }
  }
}
</script>
