import { defineConfig } from 'vitepress'

// 玩家 Wiki —— 公开，繁体中文为默认语言（台湾），简体为次要
// 单一源在 _content/（简体），由 scripts/convert-tw.mjs 生成：
//   根目录 = 繁体（默认）  /zh-hans/ = 简体

// 繁体导航与侧边栏（默认，链接无前缀）
const twNav = [
  { text: '新手入門', link: '/start/welcome' },
  { text: '職業玩法', link: '/play/weapon-job' },
  { text: '武器技能', link: '/combat/weapons' },
  { text: '進階攻略', link: '/advanced/tips' }
]

const twSidebar = {
  '/start/': [{
    text: '新手入門',
    items: [
      { text: '歡迎來到 界限', link: '/start/welcome' },
      { text: '第一次遊玩', link: '/start/first-steps' },
      { text: '常見問題', link: '/start/faq' }
    ]
  }],
  '/play/': [{
    text: '職業玩法',
    items: [
      { text: '武器即職業', link: '/play/weapon-job' },
      { text: '切換玩法', link: '/play/switching' },
      { text: '熟練度成長', link: '/play/mastery' }
    ]
  }],
  '/combat/': [{
    text: '武器與技能',
    items: [
      { text: '武器流派一覽', link: '/combat/weapons' },
      { text: '技能與釋放', link: '/combat/skills' }
    ]
  }],
  '/advanced/': [{
    text: '進階攻略',
    items: [
      { text: '成長技巧', link: '/advanced/tips' },
      { text: '搭配與 Combo', link: '/advanced/combos' }
    ]
  }]
}

// 简体导航/侧边栏：由繁体结构机械加 /zh-hans 前缀并转回简体文字
const hansText = {
  '新手入門': '新手入门', '職業玩法': '职业玩法', '武器技能': '武器技能',
  '進階攻略': '进阶攻略', '歡迎來到 界限': '欢迎来到 界限',
  '第一次遊玩': '第一次游玩', '常見問題': '常见问题', '武器即職業': '武器即职业',
  '切換玩法': '切换玩法', '熟練度成長': '熟练度成长', '武器與技能': '武器与技能',
  '武器流派一覽': '武器流派一览', '技能與釋放': '技能与释放', '成長技巧': '成长技巧',
  '搭配與 Combo': '搭配与 Combo'
}
const t = (s) => hansText[s] || s

const hansNav = twNav.map((n) => ({ text: t(n.text), link: '/zh-hans' + n.link }))
const hansSidebar = (() => {
  const r = {}
  for (const [k, groups] of Object.entries(twSidebar)) {
    r['/zh-hans' + k] = groups.map((g) => ({
      text: t(g.text),
      items: g.items.map((i) => ({ text: t(i.text), link: '/zh-hans' + i.link }))
    }))
  }
  return r
})()

const searchTW = {
  translations: {
    button: { buttonText: '搜尋', buttonAriaLabel: '搜尋' },
    modal: {
      noResultsText: '沒有找到相關內容', resetButtonTitle: '清除搜尋',
      footer: { selectText: '選擇', navigateText: '切換', closeText: '關閉' }
    }
  }
}
const searchHans = {
  translations: {
    button: { buttonText: '搜索', buttonAriaLabel: '搜索' },
    modal: {
      noResultsText: '没有找到相关内容', resetButtonTitle: '清除搜索',
      footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' }
    }
  }
}

export default defineConfig({
  title: '界限 玩家 Wiki',
  description: '界限 RPG 伺服器玩家指南 · 武器即職業',
  lang: 'zh-Hant',
  lastUpdated: true,
  cleanUrls: true,
  base: '/ESU/',

  // _content 是源目录，不参与构建（构建用生成出来的根/zh-hans）
  srcExclude: ['_content/**'],

  head: [['meta', { name: 'theme-color', content: '#3c8772' }]],

  themeConfig: {
    outline: { level: [2, 3], label: '本頁目錄' },
    docFooter: { prev: '上一頁', next: '下一頁' },
    darkModeSwitchLabel: '主題',
    sidebarMenuLabel: '選單',
    returnToTopLabel: '回到頂部',
    search: { provider: 'local' }
  },

  locales: {
    // 根 = 繁体（默认语言）
    root: {
      label: '繁體中文',
      lang: 'zh-Hant',
      themeConfig: {
        nav: twNav,
        sidebar: twSidebar,
        outline: { level: [2, 3], label: '本頁目錄' },
        docFooter: { prev: '上一頁', next: '下一頁' },
        search: { provider: 'local', options: searchTW },
        footer: { message: '界限 RPG 伺服器玩家 Wiki', copyright: '武器即職業 · 自由無鎖定' }
      }
    },
    // zh-hans = 简体
    'zh-hans': {
      label: '简体中文',
      lang: 'zh-Hans',
      themeConfig: {
        nav: hansNav,
        sidebar: hansSidebar,
        outline: { level: [2, 3], label: '本页目录' },
        docFooter: { prev: '上一页', next: '下一页' },
        search: { provider: 'local', options: searchHans },
        footer: { message: '界限 RPG 服务器玩家 Wiki', copyright: '武器即职业 · 自由无锁定' }
      }
    }
  }
})
