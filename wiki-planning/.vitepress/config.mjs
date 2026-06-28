import { defineConfig } from 'vitepress'

// 策划 Wiki —— 繁体中文为默认语言，简体为次要
// 单一源在 _content/（简体），由 scripts/convert-tw.mjs 生成：
//   根目录 = 繁体（默认）  /zh-hans/ = 简体

// 繁体导航与侧边栏（默认，链接无前缀）
const twNav = [
  { text: '概述', link: '/overview/vision' },
  { text: '遊玩流程', link: '/gameplay/loop' },
  { text: '核心系統', link: '/systems/profession' },
  { text: '武器與技能', link: '/combat/weapon-classes' },
  { text: '數值與平衡', link: '/balance/curves' }
]

const twSidebar = {
  '/overview/': [{
    text: '專案概述',
    items: [
      { text: '設計願景', link: '/overview/vision' },
      { text: '核心支柱', link: '/overview/pillars' },
      { text: '目標玩家', link: '/overview/audience' }
    ]
  }],
  '/gameplay/': [{
    text: '遊玩流程',
    items: [
      { text: '核心循環', link: '/gameplay/loop' },
      { text: '新手旅程', link: '/gameplay/onboarding' },
      { text: '進程設計', link: '/gameplay/progression' },
      { text: '終局內容', link: '/gameplay/endgame' }
    ]
  }],
  '/systems/': [{
    text: '核心系統',
    items: [
      { text: '職業系統', link: '/systems/profession' },
      { text: '主武器機制', link: '/systems/main-weapon' },
      { text: '熟練度成長', link: '/systems/mastery' },
      { text: '角色檔案', link: '/systems/profile' }
    ]
  }],
  '/combat/': [{
    text: '武器與技能',
    items: [
      { text: '武器流派', link: '/combat/weapon-classes' },
      { text: '武器效果設計', link: '/combat/weapon-effects' },
      { text: '技能系統', link: '/combat/skills' },
      { text: '技能圖鑑', link: '/combat/skill-list' }
    ]
  }],
  '/balance/': [{
    text: '數值與平衡',
    items: [
      { text: '成長曲線', link: '/balance/curves' },
      { text: '屬性體系', link: '/balance/attributes' },
      { text: '平衡原則', link: '/balance/principles' }
    ]
  }]
}

// 简体文字映射（繁→简）
const hansText = {
  '概述': '概述', '遊玩流程': '游玩流程', '核心系統': '核心系统',
  '武器與技能': '武器与技能', '數值與平衡': '数值与平衡', '專案概述': '项目概述',
  '設計願景': '设计愿景', '核心支柱': '核心支柱', '目標玩家': '目标玩家',
  '核心循環': '核心循环', '新手旅程': '新手旅程', '進程設計': '进程设计',
  '終局內容': '终局内容', '職業系統': '职业系统', '主武器機制': '主武器机制',
  '熟練度成長': '熟练度成长', '角色檔案': '角色档案', '武器流派': '武器流派',
  '武器效果設計': '武器效果设计', '技能系統': '技能系统', '技能圖鑑': '技能图鉴',
  '成長曲線': '成长曲线', '屬性體系': '属性体系', '平衡原則': '平衡原则'
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
    button: { buttonText: '搜尋文件', buttonAriaLabel: '搜尋文件' },
    modal: { noResultsText: '無法找到相關結果', resetButtonTitle: '清除查詢條件',
      footer: { selectText: '選擇', navigateText: '切換', closeText: '關閉' } }
  }
}
const searchHans = {
  translations: {
    button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
    modal: { noResultsText: '无法找到相关结果', resetButtonTitle: '清除查询条件',
      footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' } }
  }
}

export default defineConfig({
  title: '界限 策劃文件',
  description: 'RPG 伺服器策劃設計文件 · 內部資料',
  lang: 'zh-Hant',
  lastUpdated: true,
  cleanUrls: true,
  base: '/',
  srcExclude: ['_content/**'],
  head: [['meta', { name: 'theme-color', content: '#bd5b38' }]],

  locales: {
    root: {
      label: '繁體中文', lang: 'zh-Hant',
      themeConfig: {
        nav: twNav, sidebar: twSidebar,
        outline: { level: [2, 3], label: '本頁目錄' },
        docFooter: { prev: '上一頁', next: '下一頁' },
        search: { provider: 'local', options: searchTW },
        footer: { message: '界限 RPG 策劃文件 · 內部資料', copyright: '武器即職業 · 自由無鎖定' }
      }
    },
    'zh-hans': {
      label: '简体中文', lang: 'zh-Hans',
      themeConfig: {
        nav: hansNav, sidebar: hansSidebar,
        outline: { level: [2, 3], label: '本页目录' },
        docFooter: { prev: '上一页', next: '下一页' },
        search: { provider: 'local', options: searchHans },
        footer: { message: '界限 RPG 策划文档 · 内部资料', copyright: '武器即职业 · 自由无锁定' }
      }
    }
  },

  themeConfig: {
    outline: { level: [2, 3], label: '本頁目錄' },
    darkModeSwitchLabel: '主題', sidebarMenuLabel: '選單', returnToTopLabel: '回到頂部',
    search: { provider: 'local' }
  }
})
