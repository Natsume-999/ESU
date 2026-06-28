import { defineConfig } from 'vitepress'

// 策划团队 Wiki —— 内部资料，简体 + 繁体双语
// 繁体内容由 scripts/convert-tw.mjs 自动生成到 zh-tw/ 目录

const zhNav = [
  { text: '概述', link: '/overview/vision' },
  { text: '游玩流程', link: '/gameplay/loop' },
  { text: '核心系统', link: '/systems/profession' },
  { text: '武器与技能', link: '/combat/weapon-classes' },
  { text: '数值与平衡', link: '/balance/curves' }
]

const zhSidebar = {
  '/overview/': [{
    text: '项目概述',
    items: [
      { text: '设计愿景', link: '/overview/vision' },
      { text: '核心支柱', link: '/overview/pillars' },
      { text: '目标玩家', link: '/overview/audience' }
    ]
  }],
  '/gameplay/': [{
    text: '游玩流程',
    items: [
      { text: '核心循环', link: '/gameplay/loop' },
      { text: '新手旅程', link: '/gameplay/onboarding' },
      { text: '进程设计', link: '/gameplay/progression' },
      { text: '终局内容', link: '/gameplay/endgame' }
    ]
  }],
  '/systems/': [{
    text: '核心系统',
    items: [
      { text: '职业系统', link: '/systems/profession' },
      { text: '主武器机制', link: '/systems/main-weapon' },
      { text: '熟练度成长', link: '/systems/mastery' },
      { text: '角色档案', link: '/systems/profile' }
    ]
  }],
  '/combat/': [{
    text: '武器与技能',
    items: [
      { text: '武器流派', link: '/combat/weapon-classes' },
      { text: '武器效果设计', link: '/combat/weapon-effects' },
      { text: '技能系统', link: '/combat/skills' },
      { text: '技能图鉴', link: '/combat/skill-list' }
    ]
  }],
  '/balance/': [{
    text: '数值与平衡',
    items: [
      { text: '成长曲线', link: '/balance/curves' },
      { text: '属性体系', link: '/balance/attributes' },
      { text: '平衡原则', link: '/balance/principles' }
    ]
  }]
}

// 把简体侧边栏的 link 加上 /zh-tw 前缀，键名也加前缀
function twSidebar(sidebar) {
  const result = {}
  for (const [key, groups] of Object.entries(sidebar)) {
    result['/zh-tw' + key] = groups.map((g) => ({
      text: g.text,
      items: g.items.map((i) => ({ text: i.text, link: '/zh-tw' + i.link }))
    }))
  }
  return result
}

const twNav = zhNav.map((n) => ({ text: n.text, link: '/zh-tw' + n.link }))

const searchZh = {
  translations: {
    button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
    modal: {
      noResultsText: '无法找到相关结果',
      resetButtonTitle: '清除查询条件',
      footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' }
    }
  }
}

export default defineConfig({
  title: 'ESU 策划文档',
  description: 'RPG 服务器策划设计文档 · 内部资料',
  lang: 'zh-Hans',
  lastUpdated: true,
  cleanUrls: true,
  base: '/',

  head: [['meta', { name: 'theme-color', content: '#bd5b38' }]],

  // 简体为默认（root），繁体在 /zh-tw/
  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-Hans',
      themeConfig: {
        nav: zhNav,
        sidebar: zhSidebar
      }
    },
    'zh-tw': {
      label: '繁體中文',
      lang: 'zh-Hant',
      themeConfig: {
        nav: twNav,
        sidebar: twSidebar(zhSidebar)
      }
    }
  },

  themeConfig: {
    outline: { level: [2, 3], label: '本页目录' },
    docFooter: { prev: '上一页', next: '下一页' },
    darkModeSwitchLabel: '主题',
    lightModeSwitchTitle: '切换到浅色模式',
    darkModeSwitchTitle: '切换到深色模式',
    sidebarMenuLabel: '菜单',
    returnToTopLabel: '回到顶部',
    search: { provider: 'local', options: searchZh },
    footer: {
      message: 'ESU RPG 策划文档 · 内部资料',
      copyright: '武器即职业 · 自由无锁定'
    }
  }
})
