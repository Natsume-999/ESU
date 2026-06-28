// 玩家 Wiki 自定义主题：扩展 VitePress 默认主题
// 注册 RPG 风格首页组件，叠加游戏化配色
import DefaultTheme from 'vitepress/theme'
import RpgHome from './components/RpgHome.vue'
import './style.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('RpgHome', RpgHome)
  }
}
