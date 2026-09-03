<template>
  <div>
    <n-alert type="info" show-icon :title="$t('i18n_db4b998fbd')">
      <template #description>
        <ul>
          <li>{{ $t('i18n_66238e0917') }}</li>
          <li>{{ $t('i18n_d301fdfc20') }}</li>
        </ul>
      </template>
    </n-alert>
    <n-tabs>
      <n-tab-pane name="dingtalk" :tab="$t('i18n_9e4ae8a24f')">
        <n-form ref="editForm" :model="dingtalk" :rules="rules">
          <n-form-item :label="$t('i18n_780afeac65')" path="enabled">
            <n-switch
              v-model:value="dingtalk.enabled"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_99593f7623')" path="clientId">
            <n-input v-model:value="dingtalk.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_e0ec07be7d')" path="clientSecret">
            <n-input v-model:value="dingtalk.clientSecret" type="password" :placeholder="$t('i18n_52c6af8174')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_51d47ddc69')" path="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-dingtalk` }}</template>
            <n-input v-model:value="dingtalk.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_953357d914')" path="ignoreCheckState">
            <n-switch
              v-model:value="dingtalk.ignoreCheckState"
              :checked-label="$t('i18n_c0d5d68f5f')"
              :unchecked-label="$t('i18n_b7579706a3')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_2e1f215c5d')" path="autoCreteUser">
            <n-switch
              v-model:value="dingtalk.autoCreteUser"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item v-if="dingtalk.autoCreteUser" :label="$t('i18n_f49dfdace4')" path="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
            <n-select
              v-model:value="dingtalk.permissionGroup"
              filterable
              :placeholder="$t('i18n_72d14a3890')"
              multiple
              :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
            />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" class="btn" @click="onSubmit('dingtalk')">{{ $t('i18n_939d5345ad') }}</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="feishu" :tab="$t('i18n_a436c94494')">
        <n-form ref="editForm" :model="feishu" :rules="rules">
          <n-form-item :label="$t('i18n_780afeac65')" path="enabled">
            <n-switch
              v-model:value="feishu.enabled"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_99593f7623')" path="clientId">
            <n-input v-model:value="feishu.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_e0ec07be7d')" path="clientSecret">
            <n-input v-model:value="feishu.clientSecret" type="password" :placeholder="$t('i18n_52c6af8174')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_51d47ddc69')" path="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-feishu` }}</template>
            <n-input v-model:value="feishu.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_953357d914')" path="ignoreCheckState">
            <n-switch
              v-model:value="feishu.ignoreCheckState"
              :checked-label="$t('i18n_c0d5d68f5f')"
              :unchecked-label="$t('i18n_b7579706a3')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_2e1f215c5d')" path="autoCreteUser">
            <n-switch
              v-model:value="feishu.autoCreteUser"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item v-if="feishu.autoCreteUser" :label="$t('i18n_f49dfdace4')" path="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
            <n-select
              v-model:value="feishu.permissionGroup"
              filterable
              :placeholder="$t('i18n_72d14a3890')"
              multiple
              :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
            />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" class="btn" @click="onSubmit('feishu')">{{ $t('i18n_939d5345ad') }}</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="wechat_enterprise" :tab="$t('i18n_9282b1e5da')">
        <n-form ref="editForm" :model="wechat_enterprise" :rules="rules">
          <n-form-item :label="$t('i18n_780afeac65')" path="enabled">
            <n-switch
              v-model:value="wechat_enterprise.enabled"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_f66847edb4')" path="agentId">
            <n-input v-model:value="wechat_enterprise.agentId" type="text" :placeholder="$t('i18n_68c55772ca')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_99593f7623')" path="clientId">
            <n-input v-model:value="wechat_enterprise.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_e0ec07be7d')" path="clientSecret">
            <n-input
              v-model:value="wechat_enterprise.clientSecret"
              type="password"
              :placeholder="$t('i18n_52c6af8174')"
            />
          </n-form-item>

          <n-form-item :label="$t('i18n_51d47ddc69')" path="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-wechat_enterprise` }}</template>
            <n-input v-model:value="wechat_enterprise.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_953357d914')" path="ignoreCheckState">
            <n-switch
              v-model:value="wechat_enterprise.ignoreCheckState"
              :checked-label="$t('i18n_c0d5d68f5f')"
              :unchecked-label="$t('i18n_b7579706a3')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_2e1f215c5d')" path="autoCreteUser">
            <n-switch
              v-model:value="wechat_enterprise.autoCreteUser"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item v-if="wechat_enterprise.autoCreteUser" :label="$t('i18n_f49dfdace4')" path="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
            <n-select
              v-model:value="wechat_enterprise.permissionGroup"
              filterable
              :placeholder="$t('i18n_72d14a3890')"
              multiple
              :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
            />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" class="btn" @click="onSubmit('wechat_enterprise')">{{
              $t('i18n_939d5345ad')
            }}</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="maxkey" tab="MaxKey">
        <n-form ref="editForm" :model="maxkey" :rules="rules">
          <n-form-item :label="$t('i18n_780afeac65')" path="enabled">
            <n-switch
              v-model:value="maxkey.enabled"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_99593f7623')" path="clientId">
            <n-input v-model:value="maxkey.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_e0ec07be7d')" path="clientSecret">
            <n-input v-model:value="maxkey.clientSecret" type="password" :placeholder="$t('i18n_52c6af8174')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_bcf48bf7a8')" path="authorizationUri">
            <n-input v-model:value="maxkey.authorizationUri" type="text" :placeholder="$t('i18n_543296e005')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_79a7072ee1')" path="accessTokenUri">
            <n-input v-model:value="maxkey.accessTokenUri" type="text" :placeholder="$t('i18n_8704e7bdb7')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_2527efedcd')" path="userInfoUri">
            <n-input v-model:value="maxkey.userInfoUri" type="text" :placeholder="$t('i18n_ce84c416f9')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_51d47ddc69')" path="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-maxkey` }}</template>
            <n-input v-model:value="maxkey.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_953357d914')" path="ignoreCheckState">
            <n-switch
              v-model:value="maxkey.ignoreCheckState"
              :checked-label="$t('i18n_c0d5d68f5f')"
              :unchecked-label="$t('i18n_b7579706a3')"
            />
          </n-form-item>

          <n-form-item :label="$t('i18n_2e1f215c5d')" path="autoCreteUser">
            <n-switch
              v-model:value="maxkey.autoCreteUser"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item v-if="maxkey.autoCreteUser" :label="$t('i18n_f49dfdace4')" path="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
            <n-select
              v-model:value="maxkey.permissionGroup"
              filterable
              :placeholder="$t('i18n_72d14a3890')"
              multiple
              :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
            />
          </n-form-item>

          <n-form-item>
            <n-button type="primary" class="btn" @click="onSubmit('maxkey')">{{ $t('i18n_939d5345ad') }}</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="topiam" tab="TOPIAM">
        <n-form ref="editForm" :model="topiam" :rules="rules">
          <n-form-item :label="$t('i18n_780afeac65')" path="enabled">
            <n-switch
              v-model:value="topiam.enabled"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_99593f7623')" path="clientId">
            <n-input v-model:value="topiam.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_e0ec07be7d')" path="clientSecret">
            <n-input v-model:value="topiam.clientSecret" type="password" :placeholder="$t('i18n_52c6af8174')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_bcf48bf7a8')" path="authorizationUri">
            <n-input v-model:value="topiam.authorizationUri" type="text" :placeholder="$t('i18n_543296e005')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_79a7072ee1')" path="accessTokenUri">
            <n-input v-model:value="topiam.accessTokenUri" type="text" :placeholder="$t('i18n_8704e7bdb7')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_2527efedcd')" path="userInfoUri">
            <n-input v-model:value="topiam.userInfoUri" type="text" :placeholder="$t('i18n_ce84c416f9')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_51d47ddc69')" path="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-topiam` }}</template>
            <n-input v-model:value="topiam.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_953357d914')" path="ignoreCheckState">
            <n-switch
              v-model:value="topiam.ignoreCheckState"
              :checked-label="$t('i18n_c0d5d68f5f')"
              :unchecked-label="$t('i18n_b7579706a3')"
            />
          </n-form-item>

          <n-form-item :label="$t('i18n_2e1f215c5d')" path="autoCreteUser">
            <n-switch
              v-model:value="topiam.autoCreteUser"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item v-if="topiam.autoCreteUser" :label="$t('i18n_f49dfdace4')" path="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
            <n-select
              v-model:value="topiam.permissionGroup"
              filterable
              :placeholder="$t('i18n_72d14a3890')"
              multiple
              :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
            />
          </n-form-item>

          <n-form-item>
            <n-button type="primary" class="btn" @click="onSubmit('topiam')">{{ $t('i18n_939d5345ad') }}</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="gitee" tab="Gitee">
        <n-form ref="editForm" :model="gitee" :rules="rules">
          <n-form-item :label="$t('i18n_780afeac65')" path="enabled">
            <n-switch
              v-model:value="gitee.enabled"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_99593f7623')" path="clientId">
            <n-input v-model:value="gitee.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_e0ec07be7d')" path="clientSecret">
            <n-input v-model:value="gitee.clientSecret" type="password" :placeholder="$t('i18n_52c6af8174')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_51d47ddc69')" path="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-gitee` }}</template>
            <n-input v-model:value="gitee.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_953357d914')" path="ignoreCheckState">
            <n-switch
              v-model:value="gitee.ignoreCheckState"
              :checked-label="$t('i18n_c0d5d68f5f')"
              :unchecked-label="$t('i18n_b7579706a3')"
            />
          </n-form-item>

          <n-form-item :label="$t('i18n_2e1f215c5d')" path="autoCreteUser">
            <n-switch
              v-model:value="gitee.autoCreteUser"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item v-if="gitee.autoCreteUser" :label="$t('i18n_f49dfdace4')" path="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
            <n-select
              v-model:value="gitee.permissionGroup"
              filterable
              :placeholder="$t('i18n_72d14a3890')"
              multiple
              :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
            />
          </n-form-item>

          <n-form-item>
            <n-button type="primary" class="btn" @click="onSubmit('gitee')">{{ $t('i18n_939d5345ad') }}</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>
      <n-tab-pane name="mygitlab" :tab="$t('i18n_dc2c61a605')">
        <n-form ref="editForm" :model="mygitlab" :rules="rules">
          <n-form-item :label="$t('i18n_780afeac65')" path="enabled">
            <n-switch
              v-model:value="mygitlab.enabled"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_f562f75c64')" path="host">
            <template #help>{{ $t('i18n_5a42ea648d') }}</template>
            <n-input v-model:value="mygitlab.host" type="text" :placeholder="$t('i18n_0d48f8e881')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_99593f7623')" path="clientId">
            <n-input v-model:value="mygitlab.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_e0ec07be7d')" path="clientSecret">
            <n-input v-model:value="mygitlab.clientSecret" type="password" :placeholder="$t('i18n_52c6af8174')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_51d47ddc69')" path="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-mygitlab` }}</template>
            <n-input v-model:value="mygitlab.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_953357d914')" path="ignoreCheckState">
            <n-switch
              v-model:value="mygitlab.ignoreCheckState"
              :checked-label="$t('i18n_c0d5d68f5f')"
              :unchecked-label="$t('i18n_b7579706a3')"
            />
          </n-form-item>

          <n-form-item :label="$t('i18n_2e1f215c5d')" path="autoCreteUser">
            <n-switch
              v-model:value="mygitlab.autoCreteUser"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item v-if="mygitlab.autoCreteUser" :label="$t('i18n_f49dfdace4')" path="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
            <n-select
              v-model:value="mygitlab.permissionGroup"
              filterable
              :placeholder="$t('i18n_72d14a3890')"
              multiple
              :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
            />
          </n-form-item>

          <n-form-item>
            <n-button type="primary" class="btn" @click="onSubmit('mygitlab')">{{ $t('i18n_939d5345ad') }}</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>

      <n-tab-pane name="github" tab="Github">
        <n-form ref="editForm" :model="github" :rules="rules">
          <n-form-item :label="$t('i18n_780afeac65')" path="enabled">
            <n-switch
              v-model:value="github.enabled"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_99593f7623')" path="clientId">
            <n-input v-model:value="github.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </n-form-item>
          <n-form-item :label="$t('i18n_e0ec07be7d')" path="clientSecret">
            <n-input v-model:value="github.clientSecret" type="password" :placeholder="$t('i18n_52c6af8174')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_51d47ddc69')" path="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-github` }}</template>
            <n-input v-model:value="github.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </n-form-item>

          <n-form-item :label="$t('i18n_953357d914')" path="ignoreCheckState">
            <n-switch
              v-model:value="github.ignoreCheckState"
              :checked-label="$t('i18n_c0d5d68f5f')"
              :unchecked-label="$t('i18n_b7579706a3')"
            />
          </n-form-item>

          <n-form-item :label="$t('i18n_2e1f215c5d')" path="autoCreteUser">
            <n-switch
              v-model:value="github.autoCreteUser"
              :checked-label="$t('i18n_7854b52a88')"
              :unchecked-label="$t('i18n_5c56a88945')"
            />
          </n-form-item>
          <n-form-item v-if="github.autoCreteUser" :label="$t('i18n_f49dfdace4')" path="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
            <n-select
              v-model:value="github.permissionGroup"
              filterable
              :placeholder="$t('i18n_72d14a3890')"
              multiple
              :options="permissionGroup.map((item) => ({ label: item.name, value: item.id }))"
            />
          </n-form-item>

          <n-form-item>
            <n-button type="primary" class="btn" @click="onSubmit('github')">{{ $t('i18n_939d5345ad') }}</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>
<script>
import { oauthConfigOauth2, oauthConfigOauth2Save } from '@/api/system'
import { getUserPermissionListAll } from '@/api/user/user-permission'
export default {
  data() {
    return {
      maxkey: {},
      gitee: {},
      github: {},
      dingtalk: {},
      feishu: {},
      mygitlab: {},
      wechat_enterprise: {},
      topiam: {},
      rules: {},
      provides: ['gitee', 'maxkey', 'github', 'dingtalk', 'feishu', 'mygitlab', 'wechat_enterprise', 'topiam'],
      host: '',
      permissionGroup: []
    }
  },
  mounted() {
    this.host = `${location.protocol}//${location.host}`
    this.loadData()
  },
  methods: {
    // load data
    loadData() {
      this.provides.forEach((item) => {
        oauthConfigOauth2({
          provide: item
        }).then((res) => {
          if (res.code === 200) {
            const permissionGroup = res.data?.permissionGroup?.split('@') || []
            this[item] = Object.assign(res.data || {}, { provide: item, permissionGroup: permissionGroup })
          }
        })
      })
      this.listUserPermissionListAll()
    },
    // submit
    onSubmit(key) {
      let data = this[key]
      data = { ...data, permissionGroup: data.permissionGroup.join('@') }
      oauthConfigOauth2Save(data).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    listUserPermissionListAll() {
      getUserPermissionListAll().then((res) => {
        if (res.code === 200 && res.data) {
          this.permissionGroup = res.data
        }
      })
    }
  }
}
</script>
