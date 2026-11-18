<script setup lang="ts">
import { computed, ref, watch } from 'vue'

interface Props {
  app: API.AppVO
  featured?: boolean
}

const props = withDefaults(defineProps<Props>(), { featured: false })
const emit = defineEmits<{
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
}>()

const coverFailed = ref(false)
const appName = computed(() => props.app.appName || '未命名应用')
const typeLabel = computed(() => {
  const labels: Record<string, string> = {
    html: '单页网站',
    multi_file: '多页网站',
    vue_project: '交互应用',
  }
  return labels[props.app.codeGenType || ''] || '应用'
})

const openPreview = () => {
  if (props.app.deployKey) {
    emit('view-work', props.app)
  } else {
    emit('view-chat', props.app.id)
  }
}

watch(
  () => props.app.cover,
  () => {
    coverFailed.value = false
  },
)
</script>

<template>
  <article class="app-card">
    <button
      type="button"
      class="app-preview"
      :class="`preview-${app.codeGenType || 'html'}`"
      :aria-label="app.deployKey ? `访问${appName}的网站` : `查看${appName}的对话`"
      @click="openPreview"
    >
      <img
        v-if="app.cover && !coverFailed"
        class="cover-image"
        :src="app.cover"
        :alt="`${appName}的网站预览`"
        loading="lazy"
        @error="coverFailed = true"
      />
      <div v-else class="preview-placeholder" aria-hidden="true">
        <div class="mini-browser">
          <div class="mini-toolbar"><i></i><i></i><i></i><span>zerox.app</span></div>
          <div class="mini-content">
            <span class="mini-eyebrow">MADE WITH ZEROX</span>
            <strong>{{ appName }}</strong>
            <span class="mini-line"></span>
            <span class="mini-line mini-line-short"></span>
            <div class="mini-blocks"><span></span><span></span><span></span></div>
          </div>
        </div>
      </div>
      <span v-if="featured" class="featured-label">精选</span>
      <span class="preview-overlay"
        ><span
          >{{ app.deployKey ? '访问网站' : '查看对话' }}
          <span aria-hidden="true">{{ app.deployKey ? '↗' : '→' }}</span></span
        ></span
      >
    </button>

    <div class="app-details">
      <div class="title-row">
        <button type="button" class="app-title" :title="appName" @click="emit('view-chat', app.id)">
          {{ appName }}
        </button>
        <span class="type-label">{{ typeLabel }}</span>
      </div>
      <div class="app-meta">
        <div class="app-author">
          <a-avatar :src="app.user?.userAvatar" :size="20">{{
            app.user?.userName?.charAt(0) || 'Z'
          }}</a-avatar>
          <span>{{ app.user?.userName || (featured ? 'ZeroX 创作者' : '我的应用') }}</span>
        </div>
        <span class="app-status" :class="{ 'is-published': app.deployKey }"
          ><i></i>{{ app.deployKey ? '已发布' : '创作中' }}</span
        >
      </div>
      <div class="card-actions">
        <button type="button" @click="emit('view-chat', app.id)">
          查看对话 <span aria-hidden="true">→</span>
        </button>
        <button
          v-if="app.deployKey"
          type="button"
          class="visit-button"
          @click="emit('view-work', app)"
        >
          访问网站 <span aria-hidden="true">↗</span>
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.app-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e9ede5;
  border-radius: 15px;
  background: #fff;
  transition:
    transform 180ms,
    box-shadow 180ms;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(32, 59, 45, 0.07);
}

.app-preview {
  position: relative;
  display: block;
  width: 100%;
  height: 200px;
  overflow: hidden;
  padding: 0;
  border: 0;
  background: #f2f0e8;
  cursor: pointer;
  text-align: left;
}

.preview-multi_file {
  background: #eeeaf6;
}
.preview-vue_project {
  background: #e6f1e9;
}
.cover-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 250ms;
}
.app-card:hover .cover-image {
  transform: scale(1.025);
}
.preview-placeholder {
  display: grid;
  place-items: center;
  height: 100%;
  padding: 24px 34px 0;
}

.mini-browser {
  width: 100%;
  max-width: 260px;
  height: 176px;
  overflow: hidden;
  border: 1px solid rgba(68, 88, 70, 0.11);
  border-bottom: 0;
  border-radius: 9px 9px 0 0;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 8px 20px rgba(43, 61, 47, 0.04);
  transform: rotate(-2deg) translateY(5px);
}

.preview-multi_file .mini-browser {
  transform: rotate(2deg) translateY(5px);
}
.mini-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 23px;
  padding: 0 10px;
  border-bottom: 1px solid #f0f2ed;
}
.mini-toolbar i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #ced7cf;
}
.mini-toolbar span {
  margin-left: auto;
  color: #a3afa6;
  font-size: 7px;
  letter-spacing: 0.3px;
}
.mini-content {
  padding: 19px 19px 0;
}
.mini-eyebrow {
  display: block;
  margin-bottom: 10px;
  color: #709584;
  font-size: 6px;
  font-weight: 600;
  letter-spacing: 1.5px;
}
.mini-content strong {
  display: block;
  overflow: hidden;
  margin-bottom: 12px;
  color: #35463e;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mini-line {
  display: block;
  width: 80%;
  height: 4px;
  margin-bottom: 5px;
  border-radius: 3px;
  background: #e4eae5;
}
.mini-line-short {
  width: 58%;
}
.mini-blocks {
  display: flex;
  gap: 7px;
  margin-top: 17px;
}
.mini-blocks span {
  width: 33%;
  height: 32px;
  border-radius: 4px;
  background: #dcebe0;
}
.mini-blocks span:nth-child(2) {
  background: #ecede1;
}
.mini-blocks span:nth-child(3) {
  background: #e3e8f0;
}
.preview-multi_file .mini-blocks span:first-child {
  background: #e4dff2;
}

.featured-label {
  position: absolute;
  z-index: 1;
  top: 13px;
  left: 13px;
  padding: 4px 9px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.93);
  color: #4b5b50;
  font-size: 10px;
  line-height: 1.5;
}
.preview-overlay {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(20, 36, 30, 0.15);
  opacity: 0;
  transition: opacity 160ms;
}
.preview-overlay > span {
  padding: 10px 17px;
  border-radius: 9px;
  background: #fff;
  color: #243b30;
  font-size: 12px;
  box-shadow: 0 4px 16px rgba(26, 44, 35, 0.08);
}
.preview-overlay > span span {
  margin-left: 9px;
}
.app-preview:hover .preview-overlay,
.app-preview:focus-visible .preview-overlay {
  opacity: 1;
}
.app-details {
  padding: 18px 18px 0;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.app-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  padding: 0;
  border: 0;
  background: transparent;
  color: #28372e;
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.6;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}
.type-label {
  flex-shrink: 0;
  padding: 3px 6px;
  border-radius: 5px;
  background: #f5f7f3;
  color: #889285;
  font-size: 9px;
  line-height: 1.6;
}
.app-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin: 12px 0 16px;
}
.app-author {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}
.app-author :deep(.ant-avatar) {
  flex-shrink: 0;
  background: #f0f3ed;
  color: #81917e;
  font-size: 9px;
}
.app-author > span:last-child {
  overflow: hidden;
  color: #8a948b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-status {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 4px;
  color: #a0a69e;
  font-size: 10px;
}
.app-status i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #bbc0b6;
}
.app-status.is-published {
  color: #679b7b;
}
.app-status.is-published i {
  background: #69b384;
}
.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  border-top: 1px solid #f0f3ed;
}
.card-actions button {
  padding: 8px 0;
  border: 0;
  background: transparent;
  color: #879184;
  font: inherit;
  font-size: 11px;
  cursor: pointer;
  transition: color 160ms;
}
.card-actions button span {
  margin-left: 4px;
}
.card-actions button:hover,
.app-title:hover {
  color: #208b66;
}
.card-actions .visit-button {
  color: #3f7059;
}
.app-preview:focus-visible {
  outline: 3px solid #38cfa5;
  outline-offset: -3px;
}
.app-title:focus-visible,
.card-actions button:focus-visible {
  outline: 2px solid #38cfa5;
  outline-offset: 3px;
  border-radius: 3px;
}

@media (max-width: 480px) {
  .app-preview {
    height: 200px;
  }
  .app-details {
    padding: 16px 16px 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-card,
  .cover-image,
  .preview-overlay {
    transition: none;
  }
  .app-card:hover,
  .app-card:hover .cover-image {
    transform: none;
  }
}
</style>
