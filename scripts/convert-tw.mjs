// 多语言生成脚本
// 单一源：各 wiki 的 _content/（简体，链接照常写 /xxx）
// 生成两份：
//   - 根目录 /      = 繁体（默认语言，台湾正体），链接保持 /xxx
//   - 子目录 /zh-hans/ = 简体，链接改写为 /zh-hans/xxx
// 作者只维护 _content 的简体，两份输出都自动生成、不入 git

import * as OpenCC from 'opencc-js'
import { readdir, readFile, writeFile, mkdir, rm } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { join, dirname, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = join(__dirname, '..')

// 简体 → 繁体（台湾正体，含用词转换）
const toTW = OpenCC.Converter({ from: 'cn', to: 'tw' })

const wikis = ['wiki-player', 'wiki-planning']

// 递归收集 _content 下所有 .md（相对 _content 的路径）
async function collectMarkdown(dir, base) {
  const result = []
  const entries = await readdir(dir, { withFileTypes: true })
  for (const entry of entries) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) {
      result.push(...await collectMarkdown(full, base))
    } else if (entry.isFile() && entry.name.endsWith('.md')) {
      result.push(relative(base, full))
    }
  }
  return result
}

// 给内部绝对链接加前缀（用于简体 /zh-hans/）
// 处理：Markdown 链接 ](/x) 与 frontmatter 的 link: /x
function prefixLinks(content, prefix) {
  let out = content.replace(/\]\((\/[^)]*)\)/g, (m, link) => {
    if (link.startsWith(prefix + '/') || link === prefix) return m
    return `](${prefix}${link})`
  })
  out = out.replace(/^(\s*link:\s*)(\/[^\s]*)$/gm, (m, p, link) => {
    if (link.startsWith(prefix + '/') || link === prefix) return m
    return `${p}${prefix}${link}`
  })
  return out
}

// 处理单个 wiki
async function buildWiki(wiki) {
  const wikiDir = join(root, wiki)
  const srcDir = join(wikiDir, '_content')
  if (!existsSync(srcDir)) return { wiki, tw: 0, hans: 0 }

  const files = await collectMarkdown(srcDir, srcDir)

  // 先清理上次生成的输出：根目录下与 _content 同名的条目 + zh-hans/
  const topDirs = new Set(files.map((f) => f.split(/[\/]/)[0]))
  for (const d of topDirs) {
    await rm(join(wikiDir, d), { recursive: true, force: true })
  }
  await rm(join(wikiDir, 'zh-hans'), { recursive: true, force: true })

  let tw = 0, hans = 0
  for (const rel of files) {
    const content = await readFile(join(srcDir, rel), 'utf-8')

    // 繁体 → 根目录，链接不变（默认语言）
    const twDest = join(wikiDir, rel)
    await mkdir(dirname(twDest), { recursive: true })
    await writeFile(twDest, toTW(content), 'utf-8')
    tw++

    // 简体 → zh-hans/，链接加 /zh-hans 前缀
    const hansDest = join(wikiDir, 'zh-hans', rel)
    await mkdir(dirname(hansDest), { recursive: true })
    await writeFile(hansDest, prefixLinks(content, '/zh-hans'), 'utf-8')
    hans++
  }
  return { wiki, tw, hans }
}

async function main() {
  for (const wiki of wikis) {
    const { tw, hans } = await buildWiki(wiki)
    console.log(`[convert] ${wiki}: 繁体(默认) ${tw} 篇, 简体 ${hans} 篇`)
  }
}

main().catch((e) => {
  console.error('[convert] 生成失败：', e)
  process.exit(1)
})
