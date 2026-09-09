<template>
  <div>
    <n-card size="small" :title="$t('i18n_7f9f6f69')">
      <n-form layout="inline">
        <n-form-item :label="$t('i18n_f29c543f')">
          <n-select
            v-model:value="analyzeForm.groupBy"
            style="width: 150px"
            :options="[
              { label: $t('i18n_47d68cd0'), value: 'serviceName' },
              { label: $t('i18n_d3ce40d8'), value: 'region' },
              { label: $t('i18n_70aefc6a'), value: 'vendor' },
              { label: $t('i18n_7035c62f'), value: 'accountId' },
              { label: $t('i18n_14d34236'), value: 'tagKey' }
            ]"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_b44c0f33')"
          ><n-date-picker
            v-model:formatted-value="analyzeForm.startDate"
            type="date"
            value-format="yyyy-MM-dd"
            clearable
            style="width: 150px"
        /></n-form-item>
        <n-form-item :label="$t('i18n_1d468be9')"
          ><n-date-picker
            v-model:formatted-value="analyzeForm.endDate"
            type="date"
            value-format="yyyy-MM-dd"
            clearable
            style="width: 150px"
        /></n-form-item>
        <n-form-item
          ><n-button type="primary" @click="loadAnalyze">{{ $t('i18n_72fa7c88') }}</n-button></n-form-item
        >
        <n-form-item
          ><span
            >{{ $t('i18n_e529e7bc35') }}<b>¥{{ totalAmount }}</b></span
          ></n-form-item
        >
      </n-form>
      <n-data-table
        :data="analyzeResult"
        :pagination="false"
        size="small"
        :row-key="(row) => row.groupKey"
        style="margin-top: 12px"
        :columns="[
          { title: $t('i18n_4d9f9c0f'), key: 'groupKey' },
          { title: $t('i18n_95ffadb8c1'), key: 'totalAmount' }
        ]"
      />
    </n-card>

    <n-card size="small" :title="$t('i18n_775350e886')" style="margin-top: 12px">
      <n-form layout="inline">
        <n-form-item :label="$t('i18n_ff7c6ad4')">
          <n-select
            v-model:value="importForm.accountId"
            style="width: 200px"
            :options="accounts.map((acc) => ({ label: acc.name, value: acc.id }))"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_99b7dc82')"
          ><n-input v-model:value="importForm.billingCycle" style="width: 110px" placeholder="2026-08"
        /></n-form-item>
        <n-form-item><n-button type="primary" @click="doImport">{{ $t('i18n_a7e731e1b3') }}</n-button></n-form-item>
        <n-form-item><n-button @click="doSyncBill">{{ $t('i18n_a84f2445c3') }}</n-button></n-form-item>
      </n-form>
      <n-input
        v-model:value="importForm.csvContent"
        type="textarea"
        :rows="4"
        style="margin-top: 8px"
        placeholder="billDate,serviceName,resourceId,region,tagKey,tagValue,amount,currency&#10;2026-08-31,ECS,i-xxx,cn-hangzhou,env,prod,100.5,CNY"
      />
    </n-card>

    <n-card size="small" :title="$t('i18n_58073aaf')" style="margin-top: 12px">
      <n-form layout="inline">
        <n-form-item :label="$t('i18n_70aefc6a')"
          ><n-input v-model:value="tagRuleForm.vendor" style="width: 100px" :placeholder="$t('i18n_1430ea9bf4')"
        /></n-form-item>
        <n-form-item :label="$t('i18n_ceb7097235')"><n-input v-model:value="tagRuleForm.tagKey" style="width: 120px" /></n-form-item>
        <n-form-item :label="$t('i18n_d3889a097a')"
          ><n-input v-model:value="tagRuleForm.tagValue" style="width: 120px"
        /></n-form-item>
        <n-form-item :label="$t('i18n_33c9e2388e')"><n-input v-model:value="tagRuleForm.projectId" style="width: 120px" /></n-form-item>
        <n-form-item :label="$t('i18n_0848477e')"
          ><n-input v-model:value="tagRuleForm.projectName" style="width: 120px"
        /></n-form-item>
        <n-form-item
          ><n-button type="primary" @click="saveTagRule">{{ $t('i18n_af1bc110') }}</n-button></n-form-item
        >
      </n-form>
      <n-data-table
        :data="tagRules"
        :columns="tagRuleColumns"
        :pagination="false"
        size="small"
        :row-key="(row) => row.id"
        style="margin-top: 12px"
      />
    </n-card>

    <n-card size="small" :title="$t('i18n_8e5df983')" style="margin-top: 12px">
      <n-form layout="inline">
        <n-form-item :label="$t('i18n_d7ec2d3f')"
          ><n-input v-model:value="budgetForm.name" style="width: 120px"
        /></n-form-item>
        <n-form-item :label="$t('i18n_eaa13ec3')">
          <n-select
            v-model:value="budgetForm.scopeType"
            style="width: 110px"
            :options="[
              { label: $t('i18n_7035c62f'), value: 'account' },
              { label: $t('i18n_31ecc0e6'), value: 'project' },
              { label: $t('i18n_14d34236'), value: 'tag' },
              { label: $t('i18n_2be75b10'), value: 'global' }
            ]"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_76d932fd')"
          ><n-input v-model:value="budgetForm.scopeValue" style="width: 120px" :placeholder="$t('i18n_9f48b8e6d8')"
        /></n-form-item>
        <n-form-item :label="$t('i18n_f7aac33040')"
          ><n-input-number v-model:value="budgetForm.monthlyLimit" :min="0" style="width: 120px"
        /></n-form-item>
        <n-form-item
          ><n-button type="primary" @click="saveBudget">{{ $t('i18n_60c9ac57') }}</n-button></n-form-item
        >
        <n-form-item
          ><n-button @click="checkBudget">{{ $t('i18n_aab979b2') }}</n-button></n-form-item
        >
      </n-form>
      <n-data-table
        :data="budgets"
        :columns="budgetColumns"
        :pagination="false"
        size="small"
        :row-key="(row) => row.id"
        style="margin-top: 12px"
      />
      <n-alert v-if="overBudget.length" type="error" style="margin-top: 12px">
        <template #message>
          <div v-for="item in overBudget" :key="item.budgetId">
            {{ $t('i18n_059a9932a8', { name: item.name, monthlyLimit: item.monthlyLimit, currentAmount: item.currentAmount, overAmount: item.overAmount }) }}
          </div>
        </template>
      </n-alert>
    </n-card>

    <n-card size="small" :title="$t('i18n_e03e3661b4')" style="margin-top: 12px">
      <n-button size="small" @click="loadIdle">{{ $t('i18n_b844d6eb') }}</n-button>
      <n-data-table
        :data="idleResources"
        :pagination="false"
        size="small"
        :row-key="(row) => row.instanceId"
        style="margin-top: 8px"
        :columns="[
          { title: $t('i18n_1782d6afac'), key: 'instanceId' },
          { title: $t('i18n_d7ec2d3f'), key: 'name' },
          { title: $t('i18n_d3ce40d8'), key: 'regionId' },
          { title: $t('i18n_19444e70'), key: 'suggestion' }
        ]"
      />
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, h } from 'vue'
import { NButton } from 'naive-ui'
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
import {
  analyzeCostBill,
  totalCostBill,
  importCostBill,
  syncCostBill,
  saveCostTagRule,
  listCostTagRules,
  deleteCostTagRule,
  saveCostBudget,
  listCostBudgets,
  deleteCostBudget,
  checkCostBudget,
  listIdleResources
} from '@/api/finops'
import { listCloudAccounts } from '@/api/cloud'

const accounts = ref<any[]>([])
// 日期初值必须为 null（空串会让 n-date-picker 抛 Invalid time value）
const analyzeForm = reactive({ groupBy: 'serviceName', startDate: null as string | null, endDate: null as string | null })
const analyzeResult = ref<any[]>([])
const totalAmount = ref(0)

const loadAccounts = async () => {
  const res: any = await listCloudAccounts()
  if (res.code === 200) accounts.value = res.data || []
}
const loadAnalyze = async () => {
  const res: any = await analyzeCostBill(analyzeForm)
  if (res.code === 200) analyzeResult.value = res.data || []
  const total: any = await totalCostBill({ startDate: analyzeForm.startDate, endDate: analyzeForm.endDate })
  if (total.code === 200) totalAmount.value = total.data || 0
}

const importForm = reactive({ accountId: '', billingCycle: '', csvContent: '' })
const doImport = async () => {
  if (!importForm.accountId) {
    $message.warning(t('i18n_248d9352'))
    return
  }
  if (!importForm.csvContent) {
    $message.warning(t('i18n_4d13594f'))
    return
  }
  const res: any = await importCostBill(importForm)
  if (res.code === 200) {
    $message.success(t('i18n_490e8b4c', { n: res.data }))
    importForm.csvContent = ''
    loadAnalyze()
  }
}
const doSyncBill = async () => {
  if (!importForm.accountId) {
    $message.warning(t('i18n_248d9352'))
    return
  }
  if (!importForm.billingCycle) {
    $message.warning(t('i18n_a518a6e435'))
    return
  }
  const res: any = await syncCostBill({ accountId: importForm.accountId, billingCycle: importForm.billingCycle })
  if (res.code === 200) {
    $message.success(t('i18n_7022db32', { n: res.data }))
    loadAnalyze()
  }
}

const tagRuleForm = reactive({ vendor: '', tagKey: '', tagValue: '', projectId: '', projectName: '' })
const tagRules = ref<any[]>([])
const loadTagRules = async () => {
  const res: any = await listCostTagRules()
  if (res.code === 200) tagRules.value = res.data || []
}
const saveTagRule = async () => {
  if (!tagRuleForm.tagKey || !tagRuleForm.tagKey.trim()) {
    $message.warning(t('i18n_6c962bbc20'))
    return
  }
  if (!tagRuleForm.tagValue || !tagRuleForm.tagValue.trim()) {
    $message.warning(t('i18n_17a1eb5620'))
    return
  }
  const res: any = await saveCostTagRule(tagRuleForm)
  if (res.code === 200) {
    $message.success(t('i18n_199f2806'))
    tagRuleForm.vendor =
      tagRuleForm.tagKey =
      tagRuleForm.tagValue =
      tagRuleForm.projectId =
      tagRuleForm.projectName =
        ''
    loadTagRules()
  }
}
const removeTagRule = (record) => {
  $confirm({
    title: t('i18n_aec937fb'),
    content: t('i18n_f5b8870892', { tagKey: record.tagKey, tagValue: record.tagValue }),
    okText: t('i18n_38cf16f2'),
    cancelText: t('i18n_625fb26b'),
    onOk: async () => {
      const res: any = await deleteCostTagRule({ id: record.id })
      if (res.code === 200) {
        $message.success(t('i18n_0007d170'))
        loadTagRules()
      }
    }
  })
}

const budgetForm = reactive({ name: '', scopeType: 'account', scopeValue: '', monthlyLimit: 0 })
const budgets = ref<any[]>([])
const overBudget = ref<any[]>([])
const loadBudgets = async () => {
  const res: any = await listCostBudgets()
  if (res.code === 200) budgets.value = res.data || []
}
const saveBudget = async () => {
  if (!budgetForm.name || !budgetForm.name.trim()) {
    $message.warning(t('i18n_e331691ea6'))
    return
  }
  if (!budgetForm.monthlyLimit || budgetForm.monthlyLimit <= 0) {
    $message.warning(t('i18n_d8440a4101'))
    return
  }
  const res: any = await saveCostBudget(budgetForm)
  if (res.code === 200) {
    $message.success(t('i18n_5b4d38ca'))
    budgetForm.name = budgetForm.scopeValue = ''
    budgetForm.monthlyLimit = 0
    loadBudgets()
  }
}
const removeBudget = (record) => {
  $confirm({
    title: t('i18n_7c9790c3'),
    content: t('i18n_9073c9d39a', { name: record.name }),
    okText: t('i18n_38cf16f2'),
    cancelText: t('i18n_625fb26b'),
    onOk: async () => {
      const res: any = await deleteCostBudget({ id: record.id })
      if (res.code === 200) {
        $message.success(t('i18n_0007d170'))
        loadBudgets()
      }
    }
  })
}
const checkBudget = async () => {
  // 用本地时区取月份（toISOString 是 UTC，UTC+8 每月 1 日上午会查成上个月）
  const now = new Date()
  const month = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const res: any = await checkCostBudget({ month })
  if (res.code === 200) {
    overBudget.value = res.data || []
    if (!overBudget.value.length) $message.success(t('i18n_4681735f'))
  }
}

const idleResources = ref<any[]>([])
const loadIdle = async () => {
  const res: any = await listIdleResources()
  if (res.code === 200) idleResources.value = res.data || []
}

const tagRuleColumns = [
  { title: t('i18n_70aefc6a'), key: 'vendor', width: 80 },
  { title: t('i18n_14d34236'), key: 'tagRule', width: 200, render: (row) => `${row.tagKey}=${row.tagValue}` },
  { title: t('i18n_31ecc0e6'), key: 'project', width: 200, render: (row) => `${row.projectName}(${row.projectId})` },
  {
    title: t('i18n_2b6bc0f2'),
    key: 'actions',
    width: 90,
    render: (row) =>
      h(
        NButton,
        { size: 'small', danger: true, onClick: () => removeTagRule(row) },
        { default: () => t('i18n_2f4aaddd') }
      )
  }
]

const budgetColumns = [
  { title: t('i18n_d7ec2d3f'), key: 'name' },
  { title: t('i18n_df011658'), key: 'scope', width: 180, render: (row) => `${row.scopeType}:${row.scopeValue || '-'}` },
  { title: t('i18n_f7aac33040'), key: 'monthlyLimit', width: 110 },
  {
    title: t('i18n_2b6bc0f2'),
    key: 'actions',
    width: 90,
    render: (row) =>
      h(
        NButton,
        { size: 'small', danger: true, onClick: () => removeBudget(row) },
        { default: () => t('i18n_2f4aaddd') }
      )
  }
]

onMounted(() => {
  loadAccounts()
  loadAnalyze()
  loadTagRules()
  loadBudgets()
})
</script>
