<script setup>
// 界限 玩家 Wiki 首页 —— 游戏官网风格
// 双语：根据当前 locale（lang）切换繁简文案
import { useData, withBase } from 'vitepress'
import { computed, ref, onMounted } from 'vue'

// ============================================================
// 配置区：把下面的值改成你的真实信息即可
// ============================================================
const SERVER_IP = 'minecraft.natsume.net'   // 服务器 IP（Java 版）
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

const features = computed(() => [
  {
    icon: '⚔️',
    title: t('武器即職業', '武器即职业'),
    text: t('拿起什麼武器，就是什麼職業。拿劍是劍士，換弓是遊俠，換武器即換玩法。',
            '拿起什么武器，就是什么职业。拿剑是剑士，换弓是游侠，换武器即换玩法。')
  },
  {
    icon: '🔓',
    title: t('自由無鎖定', '自由无锁定'),
    text: t('沒有轉職門檻，沒有不可逆選擇。任何武器隨時可用，零代價切換。',
            '没有转职门槛，没有不可逆选择。任何武器随时可用，零代价切换。')
  },
  {
    icon: '📈',
    title: t('用進廢退', '用进废退'),
    text: t('用什麼武器戰鬥，就練什麼武器。越練越強，各流派獨立成長。',
            '用什么武器战斗，就练什么武器。越练越强，各流派独立成长。')
  },
  {
    icon: '🎭',
    title: t('一個號玩出花', '一个号玩出花'),
    text: t('不用開小號。同一角色，劍、弓、法杖、匕首，想練幾個練幾個。',
            '不用开小号。同一角色，剑、弓、法杖、匕首，想练几个练几个。')
  }
])

const classes = computed(() => [
  { icon: '⚔️', name: t('劍士', '剑士'), role: t('平衡近戰 · 連擊流暢', '平衡近战 · 连击流畅') },
  { icon: '🪓', name: t('狂戰', '狂战'), role: t('重型爆發 · 破甲高傷', '重型爆发 · 破甲高伤') },
  { icon: '🏹', name: t('遊俠', '游侠'), role: t('遠程輸出 · 蓄力精準', '远程输出 · 蓄力精准') },
  { icon: '🔮', name: t('法師', '法师'), role: t('範圍法術 · 元素控制', '范围法术 · 元素控制') },
  { icon: '🗡️', name: t('刺客', '刺客'), role: t('機動暗殺 · 背刺斬殺', '机动暗杀 · 背刺斩杀') },
  { icon: '👊', name: t('武者', '武者'), role: t('徒手格鬥 · 連段機動', '徒手格斗 · 连段机动') }
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
  <!-- 英雄横幅 -->
  <section class="rpg-hero">
    <div class="rpg-hero__tagline">{{ t('Songs of Freedom', 'Songs of Freedom') }}</div>
    <h1 class="rpg-hero__title">界限</h1>
    <p class="rpg-hero__subtitle">
      {{ t('武器即職業 · 拿起武器就是你的戰鬥方式', '武器即职业 · 拿起武器就是你的战斗方式') }}
    </p>
    <div class="rpg-hero__actions">
      <a class="rpg-btn rpg-btn--primary" :href="p('/start/welcome')">
        {{ t('新手入門', '新手入门') }}
      </a>
      <a class="rpg-btn rpg-btn--ghost" :href="p('/play/weapon-job')">
        {{ t('職業玩法', '职业玩法') }}
      </a>
      <a class="rpg-btn rpg-btn--ghost" :href="p('/combat/weapons')">
        {{ t('武器流派', '武器流派') }}
      </a>
    </div>
  </section>

  <!-- 核心特色 -->
  <section class="rpg-section">
    <h2 class="rpg-section__title">{{ t('為什麼選擇界限', '为什么选择界限') }}</h2>
    <p class="rpg-section__desc">
      {{ t('拋棄傳統職業鎖定，一套自由的成長系統。', '抛弃传统职业锁定，一套自由的成长系统。') }}
    </p>
    <div class="rpg-grid">
      <div class="rpg-card" v-for="f in features" :key="f.title">
        <div class="rpg-card__icon">{{ f.icon }}</div>
        <h3 class="rpg-card__title">{{ f.title }}</h3>
        <p class="rpg-card__text">{{ f.text }}</p>
      </div>
    </div>
  </section>

  <!-- 六大流派 -->
  <section class="rpg-section--alt">
    <div class="rpg-section__inner rpg-section">
      <h2 class="rpg-section__title">{{ t('六大武器流派', '六大武器流派') }}</h2>
      <p class="rpg-section__desc">
        {{ t('每種武器一種玩法，沒有最強，只有最愛。', '每种武器一种玩法，没有最强，只有最爱。') }}
      </p>
      <div class="rpg-grid">
        <a class="rpg-class" v-for="c in classes" :key="c.name" :href="p('/combat/weapons')">
          <span class="rpg-class__icon">{{ c.icon }}</span>
          <span>
            <span class="rpg-class__name">{{ c.name }}</span><br />
            <span class="rpg-class__role">{{ c.role }}</span>
          </span>
        </a>
      </div>
    </div>
  </section>

  <!-- 加入我们：服务器 IP + 实时在线人数 -->
  <section class="rpg-join">
    <h2 class="rpg-section__title">{{ t('立即加入', '立即加入') }}</h2>
    <p class="rpg-section__desc">
      {{ t('Java 版 · 複製 IP 即可進入', 'Java 版 · 复制 IP 即可进入') }}
    </p>

    <div class="rpg-ip" @click="copyIP">
      <span class="rpg-ip__addr">{{ config.SERVER_IP }}</span>
      <button class="rpg-ip__copy">
        {{ copied ? t('已複製 ✓', '已复制 ✓') : t('複製', '复制') }}
      </button>
    </div>

    <div class="rpg-status">
      <span class="rpg-status__dot" :class="{ on: status.online }"></span>
      <template v-if="status.loading">{{ t('查詢中…', '查询中…') }}</template>
      <template v-else-if="status.online">
        {{ t('線上', '在线') }} ·
        <strong>{{ status.players }}</strong> / {{ status.max }}
        {{ t('名玩家', '名玩家') }}
      </template>
      <template v-else>{{ t('伺服器離線', '服务器离线') }}</template>
    </div>
  </section>

  <!-- 社区：YouTube / Discord / 商店 / 官网 -->
  <section class="rpg-section--alt">
    <div class="rpg-section__inner rpg-section">
      <h2 class="rpg-section__title">{{ t('加入社群', '加入社区') }}</h2>
      <p class="rpg-section__desc">
        {{ t('關注我們，獲取最新動態與福利。', '关注我们，获取最新动态与福利。') }}
      </p>
      <div class="rpg-social">
        <a class="rpg-social__btn yt" :href="config.YOUTUBE_URL" target="_blank" rel="noopener">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="currentColor"><path d="M23.5 6.2a3 3 0 0 0-2.1-2.1C19.5 3.6 12 3.6 12 3.6s-7.5 0-9.4.5A3 3 0 0 0 .5 6.2 31 31 0 0 0 0 12a31 31 0 0 0 .5 5.8 3 3 0 0 0 2.1 2.1c1.9.5 9.4.5 9.4.5s7.5 0 9.4-.5a3 3 0 0 0 2.1-2.1A31 31 0 0 0 24 12a31 31 0 0 0-.5-5.8zM9.6 15.6V8.4l6.2 3.6-6.2 3.6z"/></svg>
          <span>YouTube</span>
        </a>
        <a class="rpg-social__btn dc" :href="config.DISCORD_URL" target="_blank" rel="noopener">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="currentColor"><path d="M20.3 4.4A19.8 19.8 0 0 0 15.4 3l-.3.5a14.6 14.6 0 0 0-6.3 0L8.6 3a19.8 19.8 0 0 0-4.9 1.4C.6 9 0 13.4.3 17.8a19.9 19.9 0 0 0 6 3l.7-1.1c-.7-.3-1.4-.6-2-1l.5-.4a14.2 14.2 0 0 0 12.2 0l.5.4c-.6.4-1.3.7-2 1l.7 1.1a19.9 19.9 0 0 0 6-3c.4-5.1-.6-9.5-2.5-13.4zM8.4 15.3c-1.2 0-2.1-1.1-2.1-2.4S7.2 10.5 8.4 10.5s2.2 1.1 2.1 2.4-.9 2.4-2.1 2.4zm7.2 0c-1.2 0-2.1-1.1-2.1-2.4s.9-2.4 2.1-2.4 2.2 1.1 2.1 2.4-.9 2.4-2.1 2.4z"/></svg>
          <span>Discord</span>
        </a>
        <a class="rpg-social__btn shop" :href="config.SHOP_URL" target="_blank" rel="noopener">
          <span class="rpg-social__emoji">🛒</span>
          <span>{{ t('商店', '商店') }}</span>
        </a>
        <a class="rpg-social__btn web" :href="config.WEBSITE_URL" target="_blank" rel="noopener">
          <span class="rpg-social__emoji">🌐</span>
          <span>{{ t('官網', '官网') }}</span>
        </a>
      </div>
    </div>
  </section>

  <!-- 底部 CTA -->
  <section class="rpg-cta">
    <h2 class="rpg-cta__title">
      {{ t('準備好踏入界限了嗎？', '准备好踏入界限了吗？') }}
    </h2>
    <div class="rpg-hero__actions">
      <a class="rpg-btn rpg-btn--primary" :href="p('/start/first-steps')">
        {{ t('立即開始', '立即开始') }}
      </a>
      <a class="rpg-btn rpg-btn--ghost" :href="p('/start/faq')">
        {{ t('常見問題', '常见问题') }}
      </a>
    </div>
  </section>
</template>
