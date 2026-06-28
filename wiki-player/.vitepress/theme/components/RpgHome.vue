<script setup>
// 界限 玩家 Wiki 首页 —— 游戏官网风格
// 双语：根据当前 locale（lang）切换繁简文案
import { useData, withBase } from 'vitepress'
import { computed } from 'vue'

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
