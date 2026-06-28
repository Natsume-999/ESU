<script setup>
// 界限 玩家 Wiki 首页 —— 游戏官网风格
// 双语：根据当前 locale（lang）切换繁简文案
import { useData, withBase } from 'vitepress'
import { computed, ref, onMounted } from 'vue'

// ============================================================
// 配置区：把下面的值改成你的真实信息即可
// ============================================================
const SERVER_IP = 'mc.swordmagic.online'   // 服务器 IP（Java 版）
const DISCORD_URL = 'https://discord.gg/xxxx'  // ← 换成真实 Discord 邀请链接
const YOUTUBE_URL = 'https://aaa'              // ← 换成真实 YouTube 链接
const SHOP_URL = 'https://shop.example.com'    // ← 换成真实商店链接
const WEBSITE_URL = 'https://ardonia.xyz'      // ← 换成真实官网链接
// ============================================================

const { lang } = useData()

// 是否简体（zh-Hans）；默认繁体
const isHans = computed(() => lang.value === 'zh-Hans')

// 简体路径加 /zh-hans 前缀，繁体不加
const p = (path) => withBase(isHans.value ? '/zh-hans' + path : path)

const t = (tw, hans) => (isHans.value ? hans : tw)

// 首屏下方的快速入口（精简，仅核心几个）
const links = computed(() => [
  { label: t('新手入門', '新手入门'), to: '/start/welcome' },
  { label: t('職業玩法', '职业玩法'), to: '/play/weapon-job' },
  { label: t('武器流派', '武器流派'), to: '/combat/weapons' }
])

// ---- 实时服务器状态（mcsrvstat.us 公开 API）----
const status = ref({ loading: true, online: false, players: 0, max: 0 })

onMounted(async () => {
  try {
    const res = await fetch(`https://api.mcsrvstat.us/3/${SERVER_IP}`)
    const data = await res.json()
    status.value = {
      loading: false,
      online: !!data.online,
      players: data.players?.online ?? 0,
      max: data.players?.max ?? 0
    }
  } catch (e) {
    status.value = { loading: false, online: false, players: 0, max: 0 }
  }
})

// ---- 复制服务器 IP ----
const copied = ref(false)
function copyIP() {
  navigator.clipboard?.writeText(SERVER_IP).then(() => {
    copied.value = true
    setTimeout(() => (copied.value = false), 1800)
  })
}

const config = { DISCORD_URL, YOUTUBE_URL, SHOP_URL, WEBSITE_URL, SERVER_IP }
</script>

<template>
  <!-- 英雄横幅：核心信息集中首屏 -->
  <section class="rpg-hero">
    <div class="rpg-hero__tagline">{{ t('Songs of Freedom', 'Songs of Freedom') }}</div>
    <h1 class="rpg-hero__title">界限</h1>
    <p class="rpg-hero__subtitle">
      {{ t('武器即職業 · 拿起武器就是你的戰鬥方式', '武器即职业 · 拿起武器就是你的战斗方式') }}
    </p>

    <!-- 服务器 IP + 一键复制 -->
    <div class="rpg-ip" @click="copyIP">
      <span class="rpg-ip__addr">{{ config.SERVER_IP }}</span>
      <button class="rpg-ip__copy">
        {{ copied ? t('已複製 ✓', '已复制 ✓') : t('複製 IP', '复制 IP') }}
      </button>
    </div>

    <!-- 实时在线人数 -->
    <div class="rpg-status">
      <span class="rpg-status__dot" :class="{ on: status.online }"></span>
      <template v-if="status.loading">{{ t('查詢中…', '查询中…') }}</template>
      <template v-else-if="status.online">
        {{ t('線上', '在线') }} <strong>{{ status.players }}</strong> / {{ status.max }}
      </template>
      <template v-else>{{ t('伺服器離線', '服务器离线') }}</template>
    </div>

    <!-- 主行动按钮 -->
    <div class="rpg-hero__actions">
      <a class="rpg-btn rpg-btn--primary" :href="p('/start/welcome')">
        {{ t('開始遊玩', '开始游玩') }}
      </a>
    </div>

    <!-- 快速入口 -->
    <nav class="rpg-quicklinks">
      <a v-for="l in links" :key="l.to" :href="p(l.to)">{{ l.label }}</a>
    </nav>
  </section>

  <!-- 社区：紧凑一排 -->
  <section class="rpg-social-bar">
    <a class="rpg-social__btn yt" :href="config.YOUTUBE_URL" target="_blank" rel="noopener">
      <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 0 0 .5 6.2 31 31 0 0 0 0 12a31 31 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 0 0 2.1-2.1A31 31 0 0 0 24 12a31 31 0 0 0-.5-5.8zM9.6 15.6V8.4l6.2 3.6-6.2 3.6z"/></svg>
      <span>YouTube</span>
    </a>
    <a class="rpg-social__btn dc" :href="config.DISCORD_URL" target="_blank" rel="noopener">
      <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M20.3 4.4A19.8 19.8 0 0 0 15.4 3l-.3.5a14.6 14.6 0 0 0-6.3 0L8.6 3a19.8 19.8 0 0 0-4.9 1.4C.6 9 0 13.4.3 17.8a19.9 19.9 0 0 0 6 3l.7-1.1c-.7-.3-1.4-.6-2-1l.5-.4a14.2 14.2 0 0 0 12.2 0l.5.4c-.6.4-1.3.7-2 1l.7 1.1a19.9 19.9 0 0 0 6-3c.4-5.1-.6-9.5-2.5-13.4zM8.4 15.3c-1.2 0-2.1-1.1-2.1-2.4S7.2 10.5 8.4 10.5s2.2 1.1 2.1 2.4-.9 2.4-2.1 2.4zm7.2 0c-1.2 0-2.1-1.1-2.1-2.4s.9-2.4 2.1-2.4 2.2 1.1 2.1 2.4-.9 2.4-2.1 2.4z"/></svg>
      <span>Discord</span>
    </a>
    <a class="rpg-social__btn shop" :href="config.SHOP_URL" target="_blank" rel="noopener">
      <span class="rpg-social__emoji">🛒</span><span>{{ t('商店', '商店') }}</span>
    </a>
    <a class="rpg-social__btn web" :href="config.WEBSITE_URL" target="_blank" rel="noopener">
      <span class="rpg-social__emoji">🌐</span><span>{{ t('官網', '官网') }}</span>
    </a>
  </section>
</template>
