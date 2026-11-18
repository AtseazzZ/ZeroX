<template>
  <main id="userLoginPage" class="auth-page">
    <section class="auth-card" aria-labelledby="login-title">
      <RouterLink to="/" class="auth-brand">
        <img src="/zerox-mark.svg" alt="" /><span>ZeroX</span>
      </RouterLink>
      <h1 id="login-title" class="title">欢迎回来</h1>
      <p class="desc">登录 ZeroX，让下一个想法成为现实。</p>
      <a-form :model="formState" layout="vertical" name="login" @finish="handleSubmit">
        <a-form-item
          label="账号"
          name="userAccount"
          :rules="[{ required: true, message: '请输入账号' }]"
        >
          <a-input
            v-model:value="formState.userAccount"
            autocomplete="username"
            placeholder="输入你的账号"
          />
        </a-form-item>
        <a-form-item
          label="密码"
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码长度不能小于 8 位' },
          ]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            autocomplete="current-password"
            placeholder="输入你的密码"
          />
        </a-form-item>
        <a-form-item class="submit-item">
          <a-button class="submit-button" type="primary" html-type="submit" :loading="submitting"
            >登录 ZeroX</a-button
          >
        </a-form-item>
        <p class="tips">
          还没有账号？<RouterLink :to="{ path: '/user/register', query: redirectQuery }"
            >立即注册 ↗</RouterLink
          >
        </p>
      </a-form>
      <p class="auth-note">从零开始，创造无限可能。</p>
    </section>
  </main>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue'
import { userLogin } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})
const submitting = ref(false)
const route = useRoute()
const redirectQuery = computed(() =>
  typeof route.query.redirect === 'string' ? { redirect: route.query.redirect } : {},
)
const router = useRouter()
const loginUserStore = useLoginUserStore()

const handleSubmit = async (values: API.UserLoginRequest) => {
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await userLogin(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
      let destination = '/'
      if (redirect) {
        try {
          const target = new URL(redirect, window.location.origin)
          if (target.origin === window.location.origin && target.pathname !== '/user/login') {
            destination = target.pathname + target.search + target.hash
          }
        } catch {
          destination = '/'
        }
      }
      await router.replace(destination)
    } else {
      message.error('登录失败，' + res.data.message)
    }
  } catch {
    message.error('登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: grid;
  place-items: center;
  min-height: calc(100vh - 180px);
  padding: 56px 24px 72px;
  background:
    radial-gradient(ellipse at 15% 40%, rgba(160, 243, 211, 0.22), transparent 50%),
    radial-gradient(ellipse at 90% 70%, rgba(177, 208, 255, 0.2), transparent 50%), #fafbf8;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 36px;
  border: 1px solid #e8ece7;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 64px rgba(28, 58, 44, 0.045);
}

.auth-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  margin-bottom: 30px;
  color: #15251f;
  font-size: 23px;
  font-weight: 750;
  letter-spacing: -0.8px;
  text-decoration: none;
}

.auth-brand img {
  width: 34px;
  height: 34px;
}
.title {
  margin: 0 0 10px;
  text-align: center;
  color: #17251f;
  font-size: 25px;
  font-weight: 650;
}
.desc {
  margin: 0 0 32px;
  text-align: center;
  color: #89918d;
  font-size: 13px;
}
.auth-card :deep(.ant-form-item-label > label) {
  color: #4b5952;
  font-size: 13px;
}
.auth-card :deep(.ant-input),
.auth-card :deep(.ant-input-affix-wrapper) {
  min-height: 44px;
  border-color: #e2e8e3;
  border-radius: 10px;
  font-size: 14px;
}
.auth-card :deep(.ant-input-affix-wrapper .ant-input) {
  min-height: unset;
  border-radius: 0;
}
.auth-card :deep(.ant-input:hover),
.auth-card :deep(.ant-input-affix-wrapper:hover) {
  border-color: #9bcdb9;
}
.auth-card :deep(.ant-input:focus),
.auth-card :deep(.ant-input-affix-wrapper-focused) {
  border-color: #2cb88a;
  box-shadow: 0 0 0 3px rgba(46, 230, 175, 0.09);
}
.submit-item {
  margin: 28px 0 0;
}
.submit-button {
  width: 100%;
  height: 44px;
  border: 0;
  border-radius: 10px;
  background: #162029;
  box-shadow: none;
  font-size: 14px;
}
.submit-button:hover,
.submit-button:focus {
  background: #2b3c42 !important;
}
.tips {
  margin: 24px 0 0;
  color: #8a938e;
  font-size: 13px;
  text-align: center;
}
.tips a {
  margin-left: 6px;
  color: #1e8a65;
  font-weight: 500;
}
.auth-note {
  margin: 26px 0 0;
  color: #a0a8a3;
  text-align: center;
  font-size: 11px;
  letter-spacing: 0.2px;
}

@media (max-width: 480px) {
  .auth-page {
    min-height: calc(100vh - 170px);
    padding: 28px 20px 48px;
  }
  .auth-card {
    padding: 28px 24px;
    border-radius: 20px;
  }
}
</style>
