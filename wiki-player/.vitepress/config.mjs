import { defineConfig } from 'vitepress'

// 玩家 Wiki —— 公开，面向玩家，简体 + 繁体双语
// 繁体内容由 scripts/convert-tw.mjs 自动生成到 zh-tw/ 目录

const zhNav = [
  { text: '新手入门', link: '/start/welcome' },
  { text: '职业玩法', link: '/play/weapon-job' },
  { text: '武器技能', link: '/combat/weapons' },
  { text: '进阶攻略', link: '/advanced/tips' }
]

const zhSidebar = {
  '/start/': [{
    text: '新手入门',
    items: [
      { text: '欢迎来到 ESU', link: '/start/welcome' },
      { text: '第一次游玩', link: '/start/first-steps' },
      { text: '常见问题', link: '/start/faq' }
    ]
  }],
  '/play/': [{
    text: '职业玩法',
    items: [
      { text: '武器即职业', link: '/play/weapon-job' },
      { text: '切换玩法', link: '/play/switching' },
      { text: '熟练度成长', link: '/play/mastery' }
    ]
  }],
  '/combat/': [{
    text: '武器与技能',
    items: [
      { text: '武器流派一览', link: '/combat/weapons' },
      { text: '技能与释放', link: '/combat/skills' }
    ]
  }],
  '/advanced/': [{
    text: '进阶攻略',
    items: [
      { text: '成长技巧', link: '/advanced/tips' },
      { text: '搭配与 Combo', link: '/advanced/combos' }
    ]
  }]
}

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
    button: { buttonText: '搜索', buttonAriaLabel: '搜索' },
    modal: {
      noResultsText: '没有找到相关内容',
      resetButtonTitle: '清除搜索',
      footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' }
    }
  }
}

export default defineConfig({
  title: 'ESU 玩家 Wiki',
  description: 'ESU RPG 服务器玩家指南 · 武器即职业',
  lang: 'zh-Hans',
  lastUpdated: true,
  cleanUrls: true,
  // 部署在 GitHub Pages 子路径 natsume-999.github.io/ESU/
  base: '/ESU/',

  head: [['meta', { name: 'theme-color', content: '#3c8772' }]],

  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-Hans',
      themeConfig: { nav: zhNav, sidebar: zhSidebar }
    },
    'zh-tw': {
      label: '繁體中文',
      lang: 'zh-Hant',
      themeConfig: { nav: twNav, sidebar: twSidebar(zhSidebar) }
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
      message: 'ESU RPG 服务器玩家 Wiki',
      copyright: '武器即职业 · 自由无锁定'
    }
  }
})
