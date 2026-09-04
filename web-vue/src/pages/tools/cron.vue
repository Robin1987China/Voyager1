<template>
  <div>
    <n-grid justify="center" type="flex">
      <n-grid-item :span="18">
        <n-space direction="vertical" style="width: 100%">
          <n-alert :title="$t('i18n_9880bd3ba1')" type="info" />
          <n-collapse>
            <n-collapse-item key="1" :header="$t('i18n_ef734bf850')"
              >{{ $t('i18n_71c6871780') }}<br />{{ $t('i18n_e930e7890f') }}
              <ol>
                <li>
                  <strong>{{ $t('i18n_daf783c8cd') }}</strong
                  >{{ $t('i18n_d57796d6ac') }}
                </li>
                <li>
                  <strong>{{ $t('i18n_609b5f0a08') }}</strong
                  >{{ $t('i18n_867cc1aac4') }}
                </li>
                <li>
                  <strong>{{ $t('i18n_3edddd85ac') }}</strong
                  >{{ $t('i18n_9b7ada2613') }}<strong>"L"</strong>{{ $t('i18n_7b961e05d0') }}
                </li>
                <li>
                  <strong>{{ $t('i18n_e42b99d599') }}</strong
                  >{{ $t('i18n_ffd67549cf') }}
                </li>
                <li>
                  <strong>{{ $t('i18n_a657f46f5b') }}</strong
                  >{{ $t('i18n_312e044529') }}<strong>"L"</strong>{{ $t('i18n_207d9580c1') }}
                </li>
              </ol>
              <p>{{ $t('i18n_f6d96c1c8c') }}<br /></p>

              <pre>{{$t('i18n_0c4eef1b88')}}<strong >{{$t('i18n_0c1fec657f')}}</strong>{{$t('i18n_55da97b631')}}<strong >{{$t('i18n_465260fe80')}}</strong>{{$t('i18n_9443399e7d')}}</pre>
              <p>{{ $t('i18n_3ae4c953fe') }}<br />{{ $t('i18n_ba8d1dca4a') }}</p>

              <pre>{{ $t('i18n_674a284936') }}</pre>
              <p>{{ $t('i18n_01226f48fc') }}</p>

              <ul>
                <li><strong>*</strong>{{ $t('i18n_0ccaa1c8b2') }}</li>
                <li><strong>?</strong>{{ $t('i18n_6470685fcd') }}</li>
                <li><strong>*&#47;2</strong>{{ $t('i18n_d0be2fcd05') }}</li>
                <li><strong>2-8</strong>{{ $t('i18n_8c0283435b') }}</li>
                <li><strong>2,3,5,8</strong>{{ $t('i18n_61341628ab') }}</li>
                <li><strong>cronA | cronB</strong>{{ $t('i18n_ed6a8ee039') }}</li>
              </ul>
              {{ $t('i18n_932b4b7f79') }}
              <pre>{{ $t('i18n_8724641ba8') }}</pre>
              <p>
                {{ $t('i18n_3c99ea4ec2') }}<br />
                <br />
              </p>

              <p>{{ $t('i18n_c2add44a1d') }}</p>

              <ul>
                <li><strong>5 * * * *</strong>{{ $t('i18n_4a6f3aa451') }}</li>
                <li><strong>* * * * *</strong>{{ $t('i18n_1f0c93d776') }}</li>
                <li><strong>*&#47;2 * * * *</strong>{{ $t('i18n_e97a16a6d7') }}</li>
                <li><strong>* 12 * * *</strong>{{ $t('i18n_a3751dc408') }}</li>
                <li><strong>59 11 * * 1,2</strong>{{ $t('i18n_c0996d0a94') }}</li>
                <li><strong>3-18&#47;5 * * * *</strong>{{ $t('i18n_b3f9beb536') }}</li>
              </ul>
            </n-collapse-item>
          </n-collapse>
          <n-form ref="form" :model="temp" :rules="rules" @submit.prevent="onSubmit">
            <n-form-item :label="$t('i18n_3c6fa6f667')" path="cron">
              <n-input v-model:value="temp.cron" :placeholder="$t('i18n_cfa72dd73a')" />
            </n-form-item>
            <n-form-item :label="$t('i18n_d87940854f')" path="count">
              <n-input-number
                v-model:value="temp.count"
                :min="1"
                :placeholder="$t('i18n_25c6bd712c')"
                style="width: 100%"
              />
            </n-form-item>
            <n-form-item :label="$t('i18n_481ffce5a9')">
              <n-switch
                v-model:value="temp.isMatchSecond"
                :checked-label="$t('i18n_0a60ac8f02')"
                :unchecked-label="$t('i18n_c9744f45e7')"
              />
            </n-form-item>
            <n-form-item :label="$t('i18n_cd649f76d4')" path="date" :help="$t('i18n_07d2261f82')">
              <n-date-picker
                v-model:formatted-value="temp.date"
                type="daterange"
                format="yyyy-MM-dd"
                value-format="yyyy-MM-dd"
                :separator="$t('i18n_981cbe312b')"
                style="width: 100%"
                clearable
              />
            </n-form-item>

            <n-form-item>
              <n-button type="primary" attr-type="submit">{{ $t('i18n_e26dcacfb1') }}</n-button>
            </n-form-item>
          </n-form>
        </n-space>
      </n-grid-item>

      <n-grid-item :span="10">
        <n-list bordered>
          <template #header>
            <div>{{ $t('i18n_5ad7f5a8b2') }}</div>
          </template>
          <n-list-item v-for="(item, index) in resultList" :key="index">
            {{ parseTime(item, 'YYYY-MM-DD HH:mm:ss') }}
          </n-list-item>
          <template #footer v-if="!resultList || !resultList.length">
            <div class="empty-text">{{ locale.emptyText }}</div>
          </template>
        </n-list>
      </n-grid-item>
    </n-grid>
  </div>
</template>
<script>
import { cronTools } from '@/api/tools'
import { parseTime } from '@/utils/const'
export default {
  data() {
    return {
      temp: {
        count: 10
      },
      locale: {
        emptyText: this.$t('i18n_21efd88b67')
      },
      resultList: [],
      // 表单校验规则
      rules: {
        cron: [
          {
            required: true,
            message: this.$t('i18n_cfa72dd73a'),
            trigger: 'blur'
          }
        ],

        count: [
          {
            required: true,
            type: 'number',
            message: this.$t('i18n_25c6bd712c'),
            trigger: ['blur', 'input']
          }
        ]
      }
    }
  },
  mounted() {
    const cron = this.$route.query.cron
    if (cron) {
      this.temp = { ...this.temp, cron: cron }
      this.$nextTick(() => {
        this.onSubmit()
      })
    }
  },
  methods: {
    parseTime,
    onSubmit() {
      // Naive 的 n-form 无 @finish 事件（Ant 写法迁移遗漏），提交前需手动校验
      this.$refs['form'] &&
        this.$refs['form']
          .validate()
          .then(() => {
            this.submitCron()
          })
          .catch(() => {})
    },
    submitCron() {
      this.resultList = []
      this.locale = {
        emptyText: this.$t('i18n_21efd88b67')
      }
      const temp = {
        ...this.temp,
        date: this.temp.date && this.temp.date[0] + ' ~ ' + this.temp.date[1]
      }

      cronTools(temp).then((res) => {
        //   console.log(res);
        this.resultList = res.data || []
        this.locale = {
          emptyText: res.msg
        }
      })
    }
  }
}
</script>
<style scoped>
.empty-text {
  padding: 12px 16px;
  color: rgba(0, 0, 0, 0.45);
  text-align: center;
}
</style>
