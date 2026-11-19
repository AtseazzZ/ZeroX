<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowRightOutlined,
  ArrowUpOutlined,
  ApartmentOutlined,
  AppstoreOutlined,
  CodeOutlined,
  GlobalOutlined,
  LoadingOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController'
import { getDeployUrl } from '@/config/env'
import AppCard from '@/components/AppCard.vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const userPrompt = ref('')
type CreationMode = 'standard' | 'workflow'
const creationMode = ref<CreationMode>('standard')
const promptInput = ref<HTMLTextAreaElement>()
const creating = ref(false)
const activeTab = ref<'featured' | 'mine'>('featured')
const activeType = ref('')
const searchInput = ref('')
const searchQuery = ref('')
const currentPage = ref(1)
const total = ref(0)
const apps = ref<API.AppVO[]>([])
const loading = ref(false)
const loadError = ref(false)
const pageSize = 6
let latestRequest = 0

const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))
const canCreate = computed(() => Boolean(userPrompt.value.trim()) && !creating.value)
const hasFilter = computed(() => Boolean(activeType.value || searchQuery.value))
const isWorkflowMode = computed(() => creationMode.value === 'workflow')

const modeOptions = [
  {
    value: 'standard' as const,
    label: '标准模式',
    hint: '自然对话生成',
    icon: CodeOutlined,
  },
  {
    value: 'workflow' as const,
    label: '工作流模式',
    hint: '分步执行复杂流程',
    icon: ApartmentOutlined,
  },
]

const promptExamples = [
  {
    label: '个人作品集',
    icon: AppstoreOutlined,
    prompt:
      '创建一个极简风格的设计师个人作品集网站，包含自我介绍、精选作品、项目详情和联系方式。使用大幅图片、清晰的排版和柔和的动效，适配手机和电脑。',
  },
  {
    label: '品牌官网',
    icon: GlobalOutlined,
    prompt:
      '设计一个现代、精致的咖啡品牌官网，包含品牌故事、招牌咖啡、门店介绍和联系我们。采用奶油白与深棕配色，使用大图展示产品，并适配手机浏览。',
  },
  {
    label: '电商展示页',
    icon: AppstoreOutlined,
    prompt:
      '创建一个有设计感的生活方式电商展示网站，包含首页、商品卡片、分类筛选、商品详情和购物车交互，使用模拟商品数据，采用简约清新的风格。',
  },
  {
    label: '效率小工具',
    icon: CodeOutlined,
    prompt:
      '制作一个简洁好用的番茄钟与待办事项应用，支持计时、暂停、完成任务和查看今日进度，使用浏览器本地存储保存数据，界面清爽并支持响应式布局。',
  },
]
const typeFilters = [
  { label: '全部', value: '' },
  { label: '单页网站', value: 'html' },
  { label: '多页网站', value: 'multi_file' },
  { label: '交互应用', value: 'vue_project' },
]

const focusPrompt = async () => {
  document.getElementById('create')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  await nextTick()
  promptInput.value?.focus({ preventScroll: true })
}
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
  focusPrompt()
}

const createApp = async () => {
  if (!canCreate.value) return
  if (!isLoggedIn.value) {
    sessionStorage.setItem('zerox:prompt', userPrompt.value)
    sessionStorage.setItem('zerox:agent', String(isWorkflowMode.value))
    message.info('登录后，即可开始创作')
    await router.push({ path: '/user/login', query: { redirect: '/' } })
    return
  }
  creating.value = true
  try {
    const res = await addApp({ initPrompt: userPrompt.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      sessionStorage.removeItem('zerox:prompt')
      sessionStorage.removeItem('zerox:agent')
      await router.push({
        path: `/app/chat/${String(res.data.data)}`,
        query: isWorkflowMode.value ? { agent: 'true' } : {},
      })
    } else {
      message.error(res.data.message || '创建失败，请重试')
    }
  } catch {
    message.error('暂时无法创建应用，请稍后重试')
  } finally {
    creating.value = false
  }
}
const handlePromptKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && (event.ctrlKey || event.metaKey) && !event.isComposing) {
    event.preventDefault()
    createApp()
  }
}

const loadApps = async () => {
  const requestId = ++latestRequest
  if (activeTab.value === 'mine' && !isLoggedIn.value) {
    apps.value = []
    total.value = 0
    loading.value = false
    loadError.value = false
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const query: API.AppQueryRequest = {
      pageNum: currentPage.value,
      pageSize,
      sortField: 'createTime',
      sortOrder: 'descend',
      codeGenType: activeType.value || undefined,
      appName: searchQuery.value || undefined,
    }
    const load = activeTab.value === 'featured' ? listGoodAppVoByPage : listMyAppVoByPage
    const res = await load(query)
    if (requestId !== latestRequest) return
    if (res.data.code !== 0 || !res.data.data) throw new Error('Unable to load applications')
    apps.value = res.data.data.records || []
    total.value = res.data.data.totalRow || 0
  } catch {
    if (requestId !== latestRequest) return
    apps.value = []
    total.value = 0
    loadError.value = true
  } finally {
    if (requestId === latestRequest) loading.value = false
  }
}
const applySearch = () => {
  searchQuery.value = searchInput.value.trim()
}
const resetFilters = () => {
  activeType.value = ''
  searchInput.value = ''
  searchQuery.value = ''
}
const viewChat = (appId: string | number | undefined) => {
  if (appId) router.push({ path: `/app/chat/${appId}`, query: { view: '1' } })
}
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) window.open(getDeployUrl(app.deployKey), '_blank', 'noopener,noreferrer')
}
watch([activeTab, activeType, searchQuery, () => loginUserStore.loginUser.id], () => {
  currentPage.value = 1
  loadApps()
})
onMounted(() => {
  userPrompt.value = sessionStorage.getItem('zerox:prompt') || ''
  creationMode.value = sessionStorage.getItem('zerox:agent') === 'true' ? 'workflow' : 'standard'
  loadApps()
})
onUnmounted(() => {
  latestRequest++
})
</script>

<template>
  <div id="homePage">
    <section id="create" class="hero" aria-labelledby="hero-title">
      <div class="hero-content">
        <div class="hero-eyebrow"><span class="status-dot"></span> YOUR IDEAS. ZERO LIMITS.</div>
        <h1 id="hero-title" class="hero-title">
          一句话，<span class="hero-mark"><img src="/zerox-mark.svg" alt="" /></span>即刻成真。
        </h1>
        <p class="hero-description">与 ZeroX 对话，让每一个想法都有自己的模样</p>
        <form
          class="prompt-composer"
          :class="{ 'is-creating': creating }"
          @submit.prevent="createApp"
        >
          <label for="app-prompt" class="sr-only">描述你想创建的应用</label>
          <textarea
            id="app-prompt"
            ref="promptInput"
            v-model="userPrompt"
            maxlength="1000"
            :disabled="creating"
            placeholder="描述你的想法，例如：一个极简风格的个人作品集网站…"
            @keydown="handlePromptKeydown"
          ></textarea>
          <div class="composer-toolbar">
            <div class="composer-mode-switch" role="group" aria-label="创建模式">
              <button
                v-for="option in modeOptions"
                :key="option.value"
                type="button"
                class="composer-mode-option"
                :class="{ active: creationMode === option.value }"
                :aria-pressed="creationMode === option.value"
                :title="option.hint"
                :disabled="creating"
                @click="creationMode = option.value"
              >
                <component :is="option.icon" aria-hidden="true" />
                <span>{{ option.label }}</span>
              </button>
            </div>
            <span v-if="creating" class="composer-creating-label" aria-live="polite">
              正在为你创建应用
            </span>
            <div class="composer-submit">
              <span class="prompt-counter">{{ userPrompt.length }}<span> / 1000</span></span>
              <button
                type="submit"
                class="create-button"
                :disabled="!canCreate"
                :aria-label="creating ? '正在创建应用' : '创建应用'"
                title="创建应用（Ctrl / ⌘ + Enter）"
              >
                <LoadingOutlined v-if="creating" spin /><ArrowUpOutlined v-else />
              </button>
            </div>
          </div>
        </form>
        <div class="prompt-examples" aria-label="试试这些灵感">
          <span class="examples-label">试试这些灵感</span>
          <button
            v-for="example in promptExamples"
            :key="example.label"
            type="button"
            :disabled="creating"
            @click="setPrompt(example.prompt)"
          >
            <component :is="example.icon" /> {{ example.label }}
          </button>
        </div>
        <p class="hero-note">从灵感到上线，只差一次对话 <span>✦</span></p>
      </div>
    </section>

    <section id="showcase" class="showcase" aria-labelledby="showcase-title">
      <div class="showcase-heading">
        <div>
          <span class="section-eyebrow">BUILT WITH ZEROX</span>
          <h2 id="showcase-title">发现下一份灵感<span class="heading-dot">.</span></h2>
          <p>探索 AI 创造的可能，也让你的想法在这里生长。</p>
        </div>
        <button type="button" class="text-action" @click="focusPrompt">
          开始我的创作 <ArrowRightOutlined />
        </button>
      </div>
      <div class="gallery-toolbar">
        <div class="gallery-tabs" aria-label="作品范围">
          <button
            type="button"
            :class="{ active: activeTab === 'featured' }"
            :aria-pressed="activeTab === 'featured'"
            @click="activeTab = 'featured'"
          >
            精选案例
          </button>
          <button
            type="button"
            :class="{ active: activeTab === 'mine' }"
            :aria-pressed="activeTab === 'mine'"
            @click="activeTab = 'mine'"
          >
            我的作品
          </button>
        </div>
        <form class="gallery-search" role="search" @submit.prevent="applySearch">
          <SearchOutlined aria-hidden="true" /><input
            v-model="searchInput"
            type="search"
            aria-label="搜索应用名称"
            placeholder="搜索应用名称"
            @input="!searchInput && applySearch()"
          /><button type="submit" aria-label="搜索"><ArrowRightOutlined /></button>
        </form>
      </div>
      <div class="gallery-filters" aria-label="应用类型">
        <button
          v-for="filter in typeFilters"
          :key="filter.value"
          type="button"
          :class="{ active: activeType === filter.value }"
          :aria-pressed="activeType === filter.value"
          @click="activeType = filter.value"
        >
          {{ filter.label }}
        </button>
        <span
          v-if="!loading && !loadError && (activeTab === 'featured' || isLoggedIn)"
          class="gallery-count"
          >{{ total }} 个{{ activeTab === 'featured' ? '灵感' : '作品' }}</span
        >
      </div>
      <div class="gallery-content" aria-live="polite" :aria-busy="loading">
        <div v-if="loading" class="app-grid" aria-label="正在加载作品">
          <div v-for="index in 3" :key="index" class="skeleton-card" aria-hidden="true">
            <div class="skeleton-preview"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
        <div v-else-if="activeTab === 'mine' && !isLoggedIn" class="gallery-empty">
          <div class="empty-symbol"><AppstoreOutlined /></div>
          <h3>你的创作空间，等你开启</h3>
          <p>登录后，随时回来继续打磨每一个想法。</p>
          <RouterLink
            :to="{ path: '/user/login', query: { redirect: '/#showcase' } }"
            class="dark-button"
            >登录，开始创作 <ArrowRightOutlined
          /></RouterLink>
        </div>
        <div v-else-if="loadError" class="gallery-empty">
          <div class="empty-symbol"><ReloadOutlined /></div>
          <h3>作品暂时没有加载出来</h3>
          <p>稍后再试一下，灵感不会走远。</p>
          <button type="button" class="outline-button" @click="loadApps">
            <ReloadOutlined /> 重新加载
          </button>
        </div>
        <div v-else-if="!apps.length" class="gallery-empty">
          <div class="empty-symbol"><AppstoreOutlined /></div>
          <h3>
            {{
              hasFilter
                ? '还没有找到匹配的作品'
                : activeTab === 'mine'
                  ? '第一个想法，就从现在开始'
                  : '下一个精彩，等你创造'
            }}
          </h3>
          <p>
            {{
              hasFilter
                ? '换个关键词，或看看其他类型的应用。'
                : '把脑海里的灵感写下来，剩下的交给 ZeroX。'
            }}
          </p>
          <button v-if="hasFilter" type="button" class="outline-button" @click="resetFilters">
            查看全部作品 <ArrowRightOutlined />
          </button>
          <button v-else type="button" class="dark-button" @click="focusPrompt">
            创建我的应用 <ArrowRightOutlined />
          </button>
        </div>
        <div v-else class="app-grid">
          <AppCard
            v-for="app in apps"
            :key="app.id"
            :app="app"
            :featured="activeTab === 'featured'"
            @view-chat="viewChat"
            @view-work="viewWork"
          />
        </div>
      </div>
      <div v-if="total > pageSize && !loading && !loadError" class="pagination-wrapper">
        <a-pagination
          v-model:current="currentPage"
          :page-size="pageSize"
          :total="total"
          :show-size-changer="false"
          @change="loadApps"
        />
      </div>
      <div class="showcase-bottom">
        <span class="status-dot"></span> 一个想法，无限可能
        <span class="bottom-wordmark">ZeroX</span>
      </div>
    </section>
  </div>
</template>

<style scoped>
#homePage {
  padding: 0 32px 36px;
  background: #f8f9f6;
  position: relative;
  isolation: isolate;
}
#homePage::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 950px;
  z-index: -1;
  background:
    radial-gradient(ellipse at 91% 73%, #a4e0e9 0, transparent 42%),
    radial-gradient(ellipse at 70% 100%, #9eaff4 0, transparent 53%),
    radial-gradient(ellipse at 12% 88%, #a8f3dc 0, transparent 51%),
    linear-gradient(180deg, #f8f9f6 8%, #f0faf3 50%, #d8f5ec 100%);
}
#homePage::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  opacity: 0.16;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Cpath fill='%23000' opacity='.3' filter='url(%23n)' d='M0 0h180v180H0z'/%3E%3C/svg%3E");
}
.hero {
  max-width: 1200px;
  margin: auto;
  padding: 64px 16px 56px;
  text-align: center;
}
.hero-content {
  max-width: 850px;
  margin: auto;
}
.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: #5d7169;
  font-size: 11px;
  letter-spacing: 2.1px;
  font-weight: 600;
  margin-bottom: 23px;
}
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #21b990;
  border-radius: 50%;
  flex-shrink: 0;
}
.hero-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 0;
  color: #14201e;
  font-size: clamp(36px, 4.2vw, 58px);
  line-height: 1.35;
  font-weight: 650;
  letter-spacing: -2.3px;
}
.hero-mark {
  display: inline-flex;
  align-items: center;
  transform: rotate(-9deg);
}
.hero-mark img {
  width: 53px;
  height: 53px;
  border-radius: 16px;
  box-shadow: 0 4px 12px #21d5aa1a;
}
.hero-description {
  margin: 22px 0 40px;
  color: #68746f;
  font-size: 16px;
  letter-spacing: 0.6px;
}
.prompt-composer {
  max-width: 780px;
  margin: auto;
  background: #fff;
  border: 1px solid #dce3dd;
  border-radius: 24px;
  padding: 23px 24px 17px;
  box-shadow:
    0 5px 8px #183d2510,
    0 14px 42px #1a725508;
  text-align: left;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}
.prompt-composer:focus-within {
  border-color: #5cc8a6;
  box-shadow:
    0 0 0 4px #65d7b516,
    0 12px 32px #1a72550a;
}
.prompt-composer textarea {
  width: 100%;
  height: 108px;
  padding: 0;
  resize: none;
  border: none;
  outline: none;
  background: transparent;
  color: #243b33;
  font-size: 17px;
  line-height: 1.8;
  letter-spacing: 0.1px;
}
.prompt-composer textarea::placeholder {
  color: #9ba6a0;
}
.composer-toolbar,
.composer-submit {
  display: flex;
  align-items: center;
}
.composer-toolbar {
  justify-content: space-between;
  gap: 16px;
  margin-top: 17px;
}
.composer-mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px;
  border-radius: 10px;
  background: #f4f7f4;
}
.composer-mode-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 4px 9px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #7b8b82;
  font-size: 11px;
  cursor: pointer;
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s;
}
.composer-mode-option :deep(svg) {
  width: 13px;
  height: 13px;
}
.composer-mode-option:hover:enabled {
  color: #346b54;
}
.composer-mode-option.active {
  border-color: #dce8df;
  background: #fff;
  color: #2e6a50;
  box-shadow: 0 1px 3px #183d250d;
}
.composer-mode-option:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}
.composer-creating-label {
  color: #8a9890;
  font-size: 11px;
  white-space: nowrap;
}
.composer-submit {
  gap: 16px;
}
.prompt-counter {
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: #6b7a72;
}
.prompt-counter span {
  color: #a7afa9;
}
.create-button {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  border: none;
  color: #fff;
  background: #172c24;
  font-size: 21px;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition:
    transform 0.2s,
    background 0.2s;
}
.create-button:hover:enabled {
  background: #275543;
  transform: translateY(-2px);
}
.create-button:disabled {
  cursor: not-allowed;
  background: #e5eae6;
  color: #99a79e;
}
.is-creating .create-button {
  background: #172c24;
  color: #fff;
}
.prompt-examples {
  margin-top: 21px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 9px;
}
.examples-label {
  font-size: 11px;
  color: #73877b;
  margin-right: 3px;
}
.prompt-examples button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #54675d;
  background: #ffffffbd;
  border: 1px solid #ffffffc7;
  border-radius: 9px;
  padding: 8px 12px;
  cursor: pointer;
  transition:
    background 0.2s,
    transform 0.2s;
}
.prompt-examples button:hover:enabled {
  background: #fff;
  transform: translateY(-2px);
}
.prompt-examples button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.hero-note {
  margin: 25px 0 0;
  color: #799489;
  font-size: 11px;
  letter-spacing: 1px;
}
.hero-note span {
  display: inline-block;
  color: #5fac93;
  margin-left: 5px;
}
.showcase {
  position: relative;
  max-width: 1240px;
  min-height: 480px;
  margin: 0 auto;
  padding: 38px 40px 0;
  border: 1px solid #ffffffc9;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 8px 40px #274a3a05;
  scroll-margin-top: 100px;
}
.showcase-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  margin-bottom: 29px;
}
.section-eyebrow {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 1.9px;
  color: #8a9891;
}
.showcase-heading h2 {
  font-size: 27px;
  line-height: 1.5;
  margin: 8px 0 7px;
  letter-spacing: -0.7px;
  color: #172b23;
  font-weight: 650;
}
.heading-dot {
  color: #27b989;
  margin-left: 4px;
}
.showcase-heading p {
  color: #89958e;
  font-size: 12px;
  margin: 0;
}
.text-action {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #566b5f;
  font-size: 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 10px 0;
  white-space: nowrap;
}
.text-action:hover {
  color: #16815e;
}
.gallery-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  border-bottom: 1px solid #eef0eb;
}
.gallery-tabs {
  display: flex;
  gap: 27px;
}
.gallery-tabs button {
  border: none;
  background: transparent;
  font-size: 14px;
  color: #8d9690;
  padding: 0 0 17px;
  position: relative;
  cursor: pointer;
}
.gallery-tabs button.active {
  color: #203c2e;
  font-weight: 600;
}
.gallery-tabs button.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: #263e31;
  border-radius: 2px;
}
.gallery-search {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #edf0eb;
  background: #f9faf8;
  border-radius: 9px;
  padding: 7px 10px;
  width: 224px;
  margin-bottom: 12px;
  color: #929c95;
}
.gallery-search:focus-within {
  border-color: #6cbfa0;
}
.gallery-search input {
  min-width: 0;
  width: 100%;
  outline: none;
  border: none;
  background: transparent;
  color: #31493c;
  font-size: 12px;
}
.gallery-search input::placeholder {
  color: #9aa29c;
}
.gallery-search button {
  display: grid;
  place-items: center;
  border: none;
  padding: 2px;
  background: transparent;
  color: #6e8074;
  cursor: pointer;
}
.gallery-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 20px 0 24px;
}
.gallery-filters button {
  background: #f7f8f5;
  border: 1px solid transparent;
  color: #819085;
  border-radius: 7px;
  font-size: 11px;
  padding: 6px 14px;
  cursor: pointer;
  white-space: nowrap;
}
.gallery-filters button.active {
  background: #eef6ee;
  color: #436d51;
  border-color: #e4eee1;
}
.gallery-filters button:hover {
  color: #294f38;
}
.gallery-count {
  margin-left: auto;
  color: #9ca69e;
  font-size: 10px;
  white-space: nowrap;
}
.app-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}
.gallery-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-height: 280px;
  justify-content: center;
  padding: 30px 10px 50px;
}
.empty-symbol {
  width: 52px;
  height: 52px;
  border: 1px solid #e8eee5;
  background: #f5f8f0;
  color: #859c7b;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 22px;
  margin-bottom: 17px;
}
.gallery-empty h3 {
  margin: 0 0 9px;
  font-size: 17px;
  font-weight: 550;
  color: #465b4c;
}
.gallery-empty p {
  margin: 0 0 21px;
  font-size: 12px;
  line-height: 1.8;
  color: #96a097;
}
.dark-button,
.outline-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  border-radius: 9px;
  padding: 10px 17px;
  font-size: 12px;
  cursor: pointer;
  text-decoration: none;
}
.dark-button {
  background: #203b2c;
  border: 1px solid #203b2c;
  color: #fff;
}
.dark-button:hover {
  color: #fff;
  background: #335a43;
}
.outline-button {
  background: #fff;
  border: 1px solid #dbe5da;
  color: #5e7562;
}
.outline-button:hover {
  background: #f6faf3;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 32px 0 8px;
}
.showcase-bottom {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 32px;
  border-top: 1px solid #f0f2ec;
  padding: 18px 0;
  font-size: 10px;
  letter-spacing: 0.8px;
  color: #a1ada2;
}
.showcase-bottom .status-dot {
  width: 4px;
  height: 4px;
  background: #a5c5a3;
}
.bottom-wordmark {
  margin-left: auto;
  font-weight: 650;
  letter-spacing: -0.5px;
  font-size: 15px;
  color: #bcc8bd;
}
.skeleton-card {
  border: 1px solid #eef0eb;
  border-radius: 15px;
  overflow: hidden;
  padding-bottom: 18px;
}
.skeleton-preview,
.skeleton-line {
  background: linear-gradient(90deg, #f2f5ee 25%, #e7ede4 50%, #f2f5ee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite linear;
}
.skeleton-preview {
  height: 190px;
  margin-bottom: 20px;
}
.skeleton-line {
  height: 12px;
  width: 62%;
  margin: 12px 18px;
  border-radius: 4px;
}
.skeleton-line.short {
  width: 38%;
  height: 9px;
}
@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
@media (min-width: 1600px) {
  .hero {
    padding-top: 125px;
    padding-bottom: 110px;
  }
}
@media (max-width: 1000px) {
  .hero {
    padding-top: 80px;
    padding-bottom: 72px;
  }
  .hero-title {
    font-size: 44px;
  }
  .hero-mark img {
    width: 45px;
    height: 45px;
  }
  .showcase {
    padding: 30px 28px 0;
  }
  .app-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
  }
}
@media (max-width: 640px) {
  #homePage {
    padding: 0 14px 20px;
  }
  #homePage::before {
    height: 800px;
  }
  .hero {
    padding: 64px 2px 54px;
  }
  .hero-eyebrow {
    font-size: 9px;
    letter-spacing: 1.7px;
    margin-bottom: 19px;
  }
  .hero-title {
    font-size: clamp(26px, 6.5vw, 38px);
    gap: 7px;
    letter-spacing: -1.2px;
  }
  .hero-mark img {
    width: 34px;
    height: 34px;
    border-radius: 10px;
  }
  .hero-description {
    font-size: 12px;
    letter-spacing: 0;
    margin: 17px 0 30px;
  }
  .prompt-composer {
    border-radius: 18px;
    padding: 18px 17px 14px;
  }
  .prompt-composer textarea {
    font-size: 14px;
    height: 111px;
  }
  .composer-toolbar {
    margin-top: 12px;
  }
  .composer-mode-switch {
    gap: 2px;
  }
  .composer-mode-option {
    min-height: 26px;
    padding: 4px 7px;
    font-size: 10px;
    gap: 4px;
  }
  .composer-mode-option :deep(svg) {
    width: 12px;
    height: 12px;
  }
  .composer-creating-label {
    display: none;
  }
  .composer-submit {
    gap: 9px;
  }
  .prompt-counter {
    font-size: 10px;
  }
  .create-button {
    width: 36px;
    height: 36px;
    border-radius: 11px;
  }
  .examples-label {
    display: none;
  }
  .prompt-examples {
    gap: 7px;
    margin-top: 17px;
  }
  .prompt-examples button {
    font-size: 10px;
    padding: 7px 9px;
    gap: 5px;
  }
  .hero-note {
    font-size: 10px;
    margin-top: 20px;
  }
  .showcase {
    padding: 26px 18px 0;
    border-radius: 22px;
    scroll-margin-top: 85px;
  }
  .showcase-heading {
    gap: 12px;
    align-items: flex-start;
    margin-bottom: 25px;
  }
  .section-eyebrow {
    font-size: 8px;
  }
  .showcase-heading h2 {
    font-size: 23px;
  }
  .showcase-heading p {
    font-size: 10px;
    max-width: 225px;
    line-height: 1.8;
  }
  .text-action {
    font-size: 10px;
    gap: 5px;
    padding-top: 28px;
  }
  .gallery-toolbar {
    flex-wrap: wrap;
    gap: 14px;
  }
  .gallery-tabs {
    gap: 24px;
    width: 100%;
  }
  .gallery-tabs button {
    font-size: 13px;
    padding-bottom: 12px;
  }
  .gallery-tabs button.active::after {
    bottom: 0;
  }
  .gallery-search {
    width: 100%;
    margin-bottom: 13px;
  }
  .gallery-filters {
    gap: 5px;
    margin: 17px 0 20px;
    flex-wrap: wrap;
  }
  .gallery-filters button {
    font-size: 10px;
    padding: 6px 10px;
  }
  .gallery-count {
    font-size: 9px;
  }
  .app-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .showcase-bottom {
    margin-top: 25px;
  }
}
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation: none !important;
    transition: none !important;
    scroll-behavior: auto !important;
  }
}
</style>
