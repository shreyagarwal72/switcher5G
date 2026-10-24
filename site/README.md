# Switcher 5G website

Static site. HTML, CSS and vanilla JS. No framework, no build step.

## Deploy to Vercel (works from a phone)

1. Put this whole folder in a GitHub repo. `vercel.json` must sit at the repo root, next to the `site/` folder.
2. In Vercel, tap **Add New > Project** and import the repo.
3. Framework Preset: **Other**. Leave Build Command and Output Directory empty. Tap **Deploy**.
4. Add your domain in **Settings > Domains**.

`vercel.json` turns on clean URLs (`/privacy` instead of `/privacy.html`) and rewrites `/` to `site/index.html`, `/assets/*` to `site/assets/*`, and every page and root file to its place inside `site/`.

## Files

```
vercel.json
site/
  index.html download.html changelog.html faq.html support.html
  privacy.html terms.html licenses.html design.html architecture.html code-of-conduct.html
  styles.css   shell: tokens, themes, header, dock, buttons, loader
  pages.css    sections and components
  app.js       theme, header collapse, carousel, accordion, live release card
  page.js      injects the shared header, mobile dock, menus and footer
  changelog.js renders the changelog as release cards
  robots.txt sitemap.xml site.webmanifest favicon.ico
  assets/  CHANGELOG.md  banner.png  hero.svg  fonts/  icons/  favicons/  badges/  screens/
```

## Placeholders you need to fill in

| What | Where | How |
| --- | --- | --- |
| **Domain** | Every page's `<head>` (canonical, Open Graph, Twitter), `sitemap.xml`, `robots.txt`. Currently `https://switcher-5g.vercel.app` | Find and replace that text with your real domain across the `site/` folder. |
| **APK SHA-256** | `download.html`, shown in a yellow box | Fills in by itself when GitHub's release API returns the file digest. If it does not, replace the yellow text with the hash. |
| **Verification tokens** | Commented-out `<meta>` tags in each page's `<head>` | Uncomment and paste your Google and Bing tokens. |
| **Screenshots** | `site/assets/screens/` (`dashboard.webp`, `setup.webp`, `settings.webp`) | They are the three screenshots from your repo. Replace with new ones at 540x1196, keep the names. |
| **Petal license** | `licenses.html`, credits table | Confirm and write Petal's license. |
| **Contact email** | `privacy.html`, `code-of-conduct.html` | Add one if you want one. Telegram is used now. |
| **Legal review** | `terms.html` | Have a lawyer check it before you rely on it. |

Every yellow box on the live site marks a placeholder.

## Updating the changelog

`changelog.js` fetches `/assets/CHANGELOG.md` and draws a card per release. To update, replace the contents of `site/assets/CHANGELOG.md` with your repo's `CHANGELOG.md`. Use headings like `## [2.0.0] - 2026-10-24` and `###` for groups. A copy is also embedded in `changelog.js` as a fallback if the fetch fails.

## Releases and the download card

The home and download pages call `https://api.github.com/repos/shreyagarwal72/switcher5G/releases/latest` in the browser. If it fails or is rate limited, they show version 2.0.0 and link to the GitHub releases page. To change the repo, edit `REPO` in `app.js` and `page.js`.

## Theming

Theme state lives on `<html>` as `data-theme-mode` (light or dark), `data-palette` (tide, zen, ember, forest) and `data-amoled`. Choices save to `localStorage`. Palette values come from the app's `Color.kt`.

## Notes

- Text and surface colors pass WCAG AA in every palette and mode.
- Fonts: Outfit and Nunito, Latin subsets, SIL Open Font License.
- GitHub links are used only for downloads, source and issues. All reading content is on this site.
