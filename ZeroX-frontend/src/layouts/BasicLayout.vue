<script setup lang="ts">
import { RouterView } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航 -->
    <a-layout-header class="layout-header-fixed">
      <GlobalHeader />
    </a-layout-header>

    <!-- 内容区域 -->
    <a-layout-content class="layout-content">
      <RouterView />
    </a-layout-content>

    <!-- 底部版权（始终固定在底部） -->
    <a-layout-footer class="layout-footer-fixed">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<style scoped>
/* 全屏自适应布局 */
.basic-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 固定顶部导航 */
.layout-header-fixed {
  padding: 0;
  height: 64px;
  line-height: 64px;
  background: transparent;
  position: sticky;
  top: 0;
  z-index: 100;
}

/* 中间内容：自动撑满剩余空间 */
.layout-content {
  flex: 1;
  padding: 24px;
}

/* 底部：随内容撑到底，若内容不足也贴底 */
.layout-footer-fixed {
  padding: 0;
  background: transparent;
}
</style>
