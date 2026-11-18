<template>
  <header class="global-header">
    <div class="header-inner">
      <RouterLink to="/" class="brand" aria-label="ZeroX 首页">
        <img class="brand-mark" src="/zerox-mark.svg" alt="" />
        <span>ZeroX</span>
      </RouterLink>

      <nav class="navigation" aria-label="主导航">
        <RouterLink to="/" :class="{ 'is-active': route.path === '/' && !route.hash }"
          >首页</RouterLink
        >
        <RouterLink
          :to="{ path: '/', hash: '#showcase' }"
          :class="{ 'is-active': route.path === '/' && route.hash === '#showcase' }"
          >案例广场</RouterLink
        >
        <a
          class="github-link"
          href="https://github.com/AtseazzZ"
          target="_blank"
          rel="noopener noreferrer"
        >
          GitHub <span aria-hidden="true">↗</span>
        </a>
      </nav>

      <div class="header-actions">
        <a-dropdown v-if="isAdmin" :trigger="['click']" placement="bottomRight">
          <button class="admin-button" type="button" aria-label="打开管理菜单">
            管理 <DownOutlined />
          </button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="users"
                ><RouterLink to="/admin/userManage">用户管理</RouterLink></a-menu-item
              >
              <a-menu-item key="apps"
                ><RouterLink to="/admin/appManage">应用管理</RouterLink></a-menu-item
              >
              <a-menu-item key="chats"
                ><RouterLink to="/admin/chatManage">对话管理</RouterLink></a-menu-item
              >
            </a-menu>
          </template>
        </a-dropdown>

        <a-dropdown v-if="loginUserStore.loginUser.id" :trigger="['click']" placement="bottomRight">
          <button class="user-button" type="button" aria-label="打开用户菜单">
            <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="32">
              {{ loginUserStore.loginUser.userName?.charAt(0) || 'Z' }}
            </a-avatar>
            <span class="user-name">{{ loginUserStore.loginUser.userName || 'ZeroX 用户' }}</span>
            <DownOutlined class="user-chevron" />
          </button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="logout" @click="doLogout"><LogoutOutlined /> 退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <RouterLink v-else to="/user/login" class="login-button"
          >登录 <span aria-hidden="true">↗</span></RouterLink
        >
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')

const doLogout = async () => {
  try {
    const res = await userLogout()
    if (res.data.code === 0) {
      loginUserStore.setLoginUser({ userName: '未登录' })
      message.success('退出登录成功')
      await router.push('/user/login')
    } else {
      message.error('退出登录失败，' + res.data.message)
    }
  } catch {
    message.error('退出登录失败，请稍后重试')
  }
}
</script>

<style scoped>
.global-header {
  position: relative;
  z-index: 10;
  flex-shrink: 0;
  height: 80px;
  background: #fafbf8;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  max-width: 1360px;
  margin: 0 auto;
  padding: 0 40px;
  gap: 44px;
}

.brand {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
  color: #131c23;
  font-size: 28px;
  font-weight: 750;
  line-height: 1;
  letter-spacing: -1.1px;
  text-decoration: none;
}

.brand-mark {
  width: 38px;
  height: 38px;
}

.navigation {
  display: flex;
  align-items: center;
  gap: 30px;
  white-space: nowrap;
}

.navigation a {
  color: #777e80;
  font-size: 14px;
  text-decoration: none;
  transition: color 160ms;
}

.navigation a:hover,
.navigation a.is-active {
  color: #111b23;
}
.github-link span {
  margin-left: 3px;
  font-size: 15px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-left: auto;
}

.login-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  min-width: 96px;
  height: 40px;
  padding: 0 20px;
  border-radius: 12px;
  background: #162029;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition:
    background 160ms,
    transform 160ms;
}

.login-button:hover {
  transform: translateY(-1px);
  background: #2b3c42;
  color: #fff;
}

.admin-button,
.user-button {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 5px 0;
  border: 0;
  background: transparent;
  color: #354044;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.admin-button .anticon,
.user-chevron {
  font-size: 10px;
  color: #858d8f;
}
.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-button :deep(.ant-avatar) {
  background: #ddf5eb;
  color: #216551;
}

.brand:focus-visible,
.navigation a:focus-visible,
.login-button:focus-visible,
.admin-button:focus-visible,
.user-button:focus-visible {
  outline: 3px solid #38cfa5;
  outline-offset: 5px;
}

@media (max-width: 760px) {
  .header-inner {
    padding: 0 24px;
    gap: 24px;
  }
  .navigation {
    gap: 20px;
  }
  .user-name,
  .user-chevron {
    display: none;
  }
  .header-actions {
    gap: 12px;
  }
}

@media (max-width: 600px) {
  .global-header {
    height: 72px;
  }
  .header-inner {
    padding: 0 20px;
    gap: 20px;
  }
  .brand {
    gap: 7px;
    font-size: 23px;
  }
  .brand-mark {
    width: 31px;
    height: 31px;
  }
  .navigation {
    gap: 16px;
    margin-left: auto;
  }
  .navigation a {
    font-size: 12px;
  }
  .navigation .github-link {
    display: none;
  }
  .header-actions {
    margin-left: 0;
  }
  .login-button {
    min-width: 62px;
    height: 35px;
    padding: 0 14px;
    font-size: 12px;
  }
  .login-button span {
    display: none;
  }
}

@media (max-width: 390px) {
  .header-inner {
    padding: 0 16px;
    gap: 14px;
  }
  .navigation {
    gap: 12px;
  }
  .navigation a:first-child {
    display: none;
  }
}
</style>
