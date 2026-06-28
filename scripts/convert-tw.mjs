// 简体 → 繁体（台湾用词）自动转换脚本
// 扫描各 wiki 的简体 Markdown，生成 zh-tw/ 镜像目录
// 作者只维护简体，繁体自动生成，不手工维护、不入 git

import * as OpenCC from 'opencc-js'
import { readdir, readFile, writeFile, mkdir, rm } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { join, dirname, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = join(__dirname, '..')

// 简体中文 → 繁体中文（台湾正体，含用词转换）
const convert = OpenCC.Converter({ from: 'cn', to: 'tw' })

// 要处理的两个 wiki
const wikis = ['wiki-player', 'wiki-planning']

// 跳过的目录名
const SKIP_DIRS = new Set(['.vitepress', 'zh-tw', 'public', 'node_modules'])

// 递归收集某目录下的所有 .md 文件（排除 SKIP_DIRS）
async function collectMarkdown(dir, base) {
  const result = []
  const entries = await readdir(dir, { withFileTypes: true })
  for (const entry of entries) {
    if (entry.isDirectory()) {
      if (SKIP_DIRS.has(entry.name)) continue
      result.push(...await collectMarkdown(join(dir, entry.name), base))
    } else if (entry.isFile() && entry.name.endsWith('.md')) {
      result.push(relative(base, join(dir, entry.name)))
    }
  }
  return result
}

// 改写内部链接，加上 /zh-tw 前缀，使繁体页互相跳转时停留在繁体
// 1. Markdown 链接 ](/xxx)
// 2. frontmatter 中 home 页按钮的 link: /xxx
function rewriteLinks(content) {
  let out = content.replace(/\]\((\/[^)]*)\)/g, (match, link) => {
    if (link.startsWith('/zh-tw/') || link === '/zh-tw') return match
    return `](/zh-tw${link})`
  })
  // frontmatter / home actions 的 link 字段（YAML），如 "link: /overview/vision"
  out = out.replace(/^(\s*link:\s*)(\/[^\s]*)$/gm, (match, prefix, link) => {
    if (link.startsWith('/zh-tw/') || link === '/zh-tw') return match
    return `${prefix}/zh-tw${link}`
  })
  return out
}

// 转换单个 wiki
async function convertWiki(wiki) {
  const wikiDir = join(root, wiki)
  if (!existsSync(wikiDir)) return { wiki, count: 0 }

  const twDir = join(wikiDir, 'zh-tw')
  // 清空旧的繁体目录，确保删除的简体文件不残留
  if (existsSync(twDir)) {
    await rm(twDir, { recursive: true, force: true })
  }

  const files = await collectMarkdown(wikiDir, wikiDir)
  let count = 0
  for (const rel of files) {
    const src = join(wikiDir, rel)
    const dest = join(twDir, rel)
    const content = await readFile(src, 'utf-8')
    const converted = rewriteLinks(convert(content))
    await mkdir(dirname(dest), { recursive: true })
    await writeFile(dest, converted, 'utf-8')
    count++
  }
  return { wiki, count }
}

async function main() {
  for (const wiki of wikis) {
    const { count } = await convertWiki(wiki)
    console.log(`[convert] ${wiki}: 生成繁体 ${count} 篇`)
  }
}

main().catch((e) => {
  console.error('[convert] 转换失败：', e)
  process.exit(1)
})
