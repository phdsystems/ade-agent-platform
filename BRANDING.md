# ade Agent Platform - Logo & Branding Guidelines

**Version:** 1.0
**Date:** 2025-10-20
**Status:** Active

---

## Logo Files

### SVG Logos (Vector Graphics)

| File | Size | Dimensions | Use Case |
|------|------|------------|----------|
| `logo-full.svg` | 3.3 KB | 1200×300 | **Primary** - Websites, documentation, presentations |
| `logo-compact.svg` | 2.4 KB | 600×120 | Headers, navigation bars, email signatures |
| `logo-icon.svg` | 1.6 KB | 200×200 | App icons, favicons, avatars (light mode) |
| `logo-dark.svg` | 3.0 KB | 1200×300 | Dark mode websites and documentation |
| `logo-icon-dark.svg` | 1.8 KB | 200×200 | App icons, favicons (dark mode) |
| `logo.svg` | 1.7 KB | 800×200 | Legacy - use `logo-full.svg` instead |

### ASCII Art Logos (Terminal)

| File | Size | Use Case |
|------|------|----------|
| `logo.txt` | 1.8 KB | Terminal output, CLI tools, README headers (decorative) |
| `logo-simple.txt` | 137 bytes | Compact terminal display, logs, CLI banners |

---

## Logo Concept

### Design Elements

**Multi-Agent Network:**
- Central node (●) representing the platform core
- Connected peripheral nodes representing agents
- Lines showing collaboration and communication

**Typography:**
- **"ade"** in lowercase monospace font
- **"Agent Platform"** in clean sans-serif
- Tagline: "Domain-Agnostic Multi-Agent AI System"

**Colors:**
- Primary: `#1a1a1a` (near-black)
- Secondary: `#666666` (gray)
- Background: `#ffffff` (white) or transparent

---

## Brand Identity

### Product Name

**Correct:**
```
ade Agent Platform
```

**Incorrect:**
```
ADE Agent Platform  ❌ (uppercase)
Ade Agent Platform  ❌ (title case)
ade agent platform  ❌ (all lowercase)
```

### Acronym

**Full Form:**
```
ade = agent development environment
```

**Alternative:**
```
ade = advanced domain engine
```

### Capitalization Rules

1. **Product names:** `ade Agent Platform`, `ade Agent SDK`
2. **In sentences:** "ade is PHD Systems' agent framework"
3. **Artifact IDs:** `ade-agent-platform` (kebab-case)
4. **Never:** `ADE`, `Ade`, `AdE`

---

## Logo Usage

### SVG Logos

#### Full Logo (`logo-full.svg`) - **PRIMARY**

**Recommended for:**
- Website hero sections
- Documentation landing pages
- Presentations (PowerPoint, Keynote, Google Slides)
- Marketing materials
- Social media covers

**Usage in Markdown:**
```markdown
![ade Agent Platform](logo-full.svg)
```

**Usage in HTML:**
```html
<img src="logo-full.svg" alt="ade Agent Platform" width="600">
```

**Minimum size:** 400px width

---

#### Compact Logo (`logo-compact.svg`)

**Recommended for:**
- Website navigation bars
- Email signatures
- README badges
- Documentation headers
- Mobile displays

**Usage in Markdown:**
```markdown
![ade Agent Platform](logo-compact.svg)
```

**Usage in HTML:**
```html
<img src="logo-compact.svg" alt="ade Agent Platform" width="300">
```

**Minimum size:** 200px width

---

#### Icon Only (`logo-icon.svg`)

**Recommended for:**
- App icons (512×512px)
- Favicons
- Avatar images
- Profile pictures
- iOS/Android icons

**Usage in HTML:**
```html
<link rel="icon" type="image/svg+xml" href="logo-icon.svg">
```

**Minimum size:** 64×64px

---

#### Dark Mode Variants

**`logo-dark.svg`** - Full logo for dark backgrounds
**`logo-icon-dark.svg`** - Icon for dark backgrounds

**Usage with CSS:**
```css
/* Automatic theme switching */
@media (prefers-color-scheme: dark) {
  .logo-light { display: none; }
  .logo-dark { display: block; }
}

@media (prefers-color-scheme: light) {
  .logo-light { display: block; }
  .logo-dark { display: none; }
}
```

**Usage in Markdown (GitHub dark mode):**
```markdown
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="logo-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="logo-full.svg">
  <img alt="ade Agent Platform" src="logo-full.svg">
</picture>
```

---

### ASCII Art Logo (`logo.txt`)

**Recommended for:**
- README.md headers
- CLI welcome screens
- Terminal banners
- ASCII documentation

**Usage:**
```bash
cat logo.txt
```

**Example in README:**
```markdown
```
(ASCII art from logo.txt)
```
```

---

### Simple ASCII Logo (`logo-simple.txt`)

**Recommended for:**
- Compact terminal displays
- Log file headers
- CLI `--version` output
- Status messages

**Usage:**
```bash
cat logo-simple.txt
```

**Example output:**
```
   ●───●
    ╲ ╱
     ●      ade Agent Platform
    ╱ ╲
   ●───●   Domain-Agnostic Multi-Agent AI System
```

---

## Color Palette

### Primary Colors

| Color Name | Hex | RGB | Use Case |
|------------|-----|-----|----------|
| **ade Black** | `#1a1a1a` | `rgb(26, 26, 26)` | Primary text, logo, headings |
| **ade Gray** | `#666666` | `rgb(102, 102, 102)` | Secondary text, subtitles |
| **ade Light** | `#f5f5f5` | `rgb(245, 245, 245)` | Backgrounds, containers |
| **ade White** | `#ffffff` | `rgb(255, 255, 255)` | White backgrounds |

### Accent Colors (Optional)

| Color Name | Hex | RGB | Use Case |
|------------|-----|-----|----------|
| **ade Blue** | `#0066cc` | `rgb(0, 102, 204)` | Links, CTAs, interactive elements |
| **ade Green** | `#00aa44` | `rgb(0, 170, 68)` | Success states, domain plugins |
| **ade Red** | `#cc0000` | `rgb(204, 0, 0)` | Errors, warnings, deprecated features |

---

## Typography

### Primary Font (Code/Technical)

**Monospace fonts for "ade":**
- SF Mono (macOS)
- Monaco (macOS)
- Consolas (Windows)
- Fira Code (cross-platform)
- JetBrains Mono (cross-platform)

**Fallback:**
```css
font-family: 'SF Mono', 'Monaco', 'Consolas', 'Fira Code', monospace;
```

### Secondary Font (UI/Body)

**Sans-serif fonts for "Agent Platform":**
- Inter (modern, clean)
- SF Pro (macOS native)
- Segoe UI (Windows native)
- System UI

**Fallback:**
```css
font-family: 'Inter', 'SF Pro', -apple-system, system-ui, sans-serif;
```

---

## Logo Variations

### Full Logo (Horizontal)

```
    ●───●
     ╲ ╱
      ●      ade Agent Platform
     ╱ ╲    Domain-Agnostic Multi-Agent AI System
    ●───●
```

**Use when:** Space allows, primary branding

### Compact Logo

```
   ●───●
    ╲ ╱
     ●    ade Agent Platform
    ╱ ╲
   ●───●
```

**Use when:** Limited vertical space

### Icon Only

```
   ●───●
    ╲ ╱
     ●
    ╱ ╲
   ●───●
```

**Use when:** App icons, favicons, avatars

### Text Only

```
ade Agent Platform
```

**Use when:** Inline mentions, body text

---

## Clear Space

**Minimum clear space around logo:** 20px on all sides

**Avoid:**
- ❌ Placing logo too close to edges
- ❌ Crowding with other elements
- ❌ Overlapping text or graphics

---

## Don'ts

### ❌ Incorrect Usage

1. **Don't change colors arbitrarily**
   - Use defined color palette only

2. **Don't distort the logo**
   - Maintain aspect ratio

3. **Don't rotate the logo**
   - Keep horizontal orientation

4. **Don't use uppercase "ADE"**
   - Brand is "ade" (lowercase)

5. **Don't add effects**
   - No shadows, gradients, or 3D effects

6. **Don't modify the node structure**
   - Keep multi-agent network intact

7. **Don't use poor contrast**
   - Ensure readability on backgrounds

---

## Examples

### ✅ Correct Usage

```markdown
# ade Agent Platform

![ade Agent Platform](logo.svg)

The **ade Agent Platform** is a domain-agnostic multi-agent system.
```

### ❌ Incorrect Usage

```markdown
# ADE AGENT PLATFORM  ❌ (uppercase)

![ADE Agent Platform](logo.svg)  ❌ (uppercase in alt text)

The **ADE Agent Platform** is...  ❌ (uppercase)
```

---

## File Locations

### In Project Repository

```
ade-agent-platform/
├── logo.svg              # SVG logo (scalable)
├── logo.txt              # Full ASCII art
├── logo-simple.txt       # Compact ASCII art
├── BRANDING.md          # This file
└── README.md            # Uses logo
```

### In Documentation

```
doc/
└── 3-design/
    └── ade-branding-alignment.md  # Branding guidelines
```

---

## Logo in Different Contexts

### GitHub README

```markdown
<div align="center">
  <img src="logo.svg" alt="ade Agent Platform" width="500">
  <h1>ade Agent Platform</h1>
  <p>Domain-Agnostic Multi-Agent AI System</p>
</div>
```

### CLI Tool Output

```bash
#!/bin/bash
cat logo-simple.txt
echo ""
echo "ade Agent Platform v0.2.0"
echo "Copyright © 2025 PHD Systems"
```

### Documentation Header

```markdown
![ade Agent Platform Logo](logo.svg)

# ade Agent Platform Documentation
```

---

## Social Media Guidelines

### Profile Images

- Use **icon only** version (multi-agent network)
- Minimum resolution: 512x512px
- Square format
- Transparent or white background

### Cover Images

- Use **full horizontal logo**
- Recommended resolution: 1500x500px
- White or light gray background

### Posts

- Always use lowercase "ade"
- Include logo.svg when announcing features
- Maintain brand consistency

---

## Trademark Notice

**ade Agent Platform** is a product of PHD Systems.

All logos, brand names, and trademarks are property of PHD Systems and may not be used without permission.

---

## Questions?

For branding questions or logo requests:
- **GitHub Issues:** https://github.com/phdsystems/software-engineer/issues
- **Documentation:** `/home/developer/software-engineer/doc/3-design/ade-branding-alignment.md`

---

**Last Updated:** 2025-10-20
**Version:** 1.0
**Status:** Active
