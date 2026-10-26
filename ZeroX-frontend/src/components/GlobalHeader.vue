<script setup lang="ts">
import { computed, ref, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'

/**
 * 菜单项配置：新增页面时在此追加即可
 * key 与路由 path 保持一致，用于高亮与跳转
 */
const menuItems = ref<MenuProps['items']>([
  { key: '/', label: '首页' },
  { key: '/about', label: '关于' },
])

const route = useRoute()
const router = useRouter()

/** 当前选中的菜单（与路由联动） */
const selectedKeys = computed<string[]>(() => [route.path])

const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (typeof key === 'string' && key !== route.path) {
    router.push(key)
  }
}

/** 登录占位（后续接入真实登录后替换为头像 + 昵称） */
const handleLogin = () => {
  // TODO: 接入登录逻辑
}
</script>

<template>
  <div class="global-header">
    <div class="header-inner">
      <!-- 左侧：Logo + 标题 -->
      <router-link to="/" class="brand">
        <img src="/logo.png" alt="ZeroX" class="brand-logo" />
        <span class="brand-title">ZeroX</span>
      </router-link>

      <!-- 中间：菜单 -->
      <a-menu
        class="header-menu"
        mode="horizontal"
        :items="menuItems"
        :selected-keys="selectedKeys"
        @click="handleMenuClick"
      />

      <!-- 右侧：登录按钮（占位，后续替换为头像 + 昵称） -->
      <div class="header-actions">
        <a-button type="primary" shape="round" class="login-btn" @click="handleLogin">
          登录
        </a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.global-header {
  width: 100%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #f0f0f0;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  text-decoration: none;
}

.brand-logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: cover;
}

.brand-title {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: -0.5px;
}

.header-menu {
  flex: 1;
  min-width: 0;
  background: transparent;
  border-bottom: none;
  line-height: 62px;
}

.header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 美团 NoCode 风格：黑色胶囊登录按钮 */
.login-btn {
  background: #1a1a1a;
  border-color: #1a1a1a;
  font-weight: 500;
  padding: 0 22px;
}

.login-btn:hover,
.login-btn:focus {
  background: #000 !important;
  border-color: #000 !important;
}

/* 响应式：小屏隐藏菜单项文字间距收缩 */
@media (max-width: 768px) {
  .header-inner {
    padding: 0 16px;
    gap: 16px;
  }

  .brand-title {
    font-size: 17px;
  }
}
</style>
