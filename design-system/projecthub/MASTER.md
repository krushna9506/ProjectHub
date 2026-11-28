# ProjectHub Design System — MASTER

## 1. Visual Direction
- **Palette Name:** Indigo Slate + Emerald Accent (Modern Productivity & Developer Workspace)
- **Typography:** `Plus Jakarta Sans` (Web headings/body), system geometric sans with refined weight hierarchy (Android), `JetBrains Mono` / `monospace` for Join Codes, Task Keys, and Code Snippets.
- **Spacing Rhythm:** `4dp` / `8dp` grid (`8dp` compact gap, `12dp` inner card margin, `16dp` card padding & screen gutter, `24dp` auth/section spacing).
- **Shape Tokens:** Cards `16dp`, Inputs & Primary Buttons `12dp`, Badges/Pills `8dp`, Chat Bubbles `18dp` (`4dp` tail corner).

---

## 2. Semantic Color Tokens (WCAG AA >= 4.5:1)

| Token | Light Mode | Dark Mode | Role |
| :--- | :--- | :--- | :--- |
| `primary` | `#4F46E5` | `#818CF8` | Primary brand actions, active indicators, links |
| `on_primary` | `#FFFFFF` | `#0F172A` | Text/icons on `primary` fills |
| `primary_dark` | `#3730A3` | `#6366F1` | Pressed states, emphasis accents |
| `primary_light` | `#EEF2FF` | `#1E1B4B` | Soft containers, tonal pills, reply banners |
| `on_primary_container` | `#312E81` | `#E0E7FF` | Text/icons inside `primary_light` containers |
| `accent` | `#059669` | `#34D399` | Positive outcomes, Campus Verified, Explore |
| `accent_light` | `#ECFDF5` | `#064E3B` | Soft emerald badge surfaces |
| `on_accent_container` | `#065F46` | `#A7F3D0` | Text/icons inside `accent_light` |
| `background` | `#F6F7FB` | `#0B1120` | App canvas behind cards |
| `surface` | `#FFFFFF` | `#131C31` | Cards, headers, dialogs, sheets |
| `surface_variant` | `#EEF0F8` | `#1B2640` | Secondary containers, Kanban columns, tracks |
| `text_primary` | `#0F172A` | `#F8FAFC` | Primary headings & body copy |
| `text_secondary` | `#475569` | `#94A3B8` | Metadata, subtitles, timestamps |
| `divider` | `#E2E8F0` | `#24324E` | 1dp borders, hairline dividers |

### Workflow Status Tokens

| Status Family | Light Base | Light `_bg` | Light `_text` | Dark Base | Dark `_bg` (16% Alpha) | Dark `_text` |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `status_todo` | `#64748B` | `#F1F5F9` | `#334155` | `#94A3B8` | `#2994A3B8` | `#CBD5E1` |
| `status_progress` | `#F59E0B` | `#FEF3C7` | `#92400E` | `#FBBF24` | `#29F59E0B` | `#FCD34D` |
| `status_review` | `#0EA5E9` | `#E0F2FE` | `#075985` | `#38BDF8` | `#290EA5E9` | `#7DD3FC` |
| `status_done` | `#10B981` | `#D1FAE5` | `#065F46` | `#34D399` | `#2910B981` | `#6EE7B7` |
| `status_blocked` | `#EF4444` | `#FEE2E2` | `#991B1B` | `#F87171` | `#29EF4444` | `#FCA5A5` |

---

## 3. Component & Accessibility Rules
1. **No Emoji as Structural Icons:** Buttons, headers, search fields, filters, and navigation use crisp 24dp vector drawables (`ic_*.xml` on Android, inline SVGs on Web).
2. **No Material 3 Default Purple:** All `colorPrimaryContainer`, `colorSecondaryContainer`, `colorSurfaceContainer*`, and `colorOutline*` theme attributes map explicitly to ProjectHub Indigo Slate tokens.
3. **Touch Targets:** Interactive icon buttons are at least `48dp × 48dp` on Android and `36px+` with clear focus rings on Web.
4. **Full Light/Dark Parity:** Every color, shape drawable, bubble, and card stroke resolves through semantic tokens across `values/` and `values-night/`.
