# Design System: Kairos
**Project ID:** 14010359681060407838

## 1. Visual Theme & Atmosphere

The Creative North Star for this design system is **"The Mindful Anchor."**

In an ADHD-focused context, traditional productivity interfaces are often "loud"—relying on urgent reds, rigid grids, and high-density information that triggers executive dysfunction. This system rejects the "assembly line" aesthetic in favor of **Organic Editorialism**. We utilize intentional asymmetry, overlapping tonal layers, and expansive negative space to create a digital environment that feels like a premium, tactile journal rather than a high-stress dashboard.

The goal is to reduce cognitive load by guiding the eye through a clear hierarchy of "event-based triggers" while maintaining a sense of calm, even when tasks are left incomplete. We embrace **Sustainable Imperfection**—where the interface feels human, forgiving, and deeply intentional.

The overall atmosphere is **calm, expansive, and deliberately imperfect**. It prioritizes the "Hero" experience of Dark Mode with deep teal-navy backgrounds that feel more like a quiet sanctuary than a task command center. The density is intentionally lower than typical productivity apps, allowing the eye to rest and breathe.

## 2. Color Palette & Roles

### Core Backgrounds & Surfaces
- **Deep Muted Teal-Navy** (#0c0e10) — Base background, the foundational "sanctuary" layer of the entire interface
- **Shadowed Teal-Base** (#111416) — Secondary surface layer for tonal separation
- **Obsidian Deep** (#000000) — Deepest surface for absolute contrast in nested cards

### Surface Hierarchy (Tonal Layering)
- **Surface Container** (#171a1d) — Secondary content zones, carved out through tonal shift
- **Surface Container High** (#1d2023) — Interactive elements and active states
- **Surface Container Highest** (#22262a) — Elevated contexts and foreground elements

### Primary Action Colors
- **Soft Bright Teal** (#8ef4e9) — Primary action color, used for CTAs, active states, and celebration of wins
- **Teal Container** (#7fe6db) — Primary container backgrounds, softer variant for surfaces
- **Muted Teal Glow** (#71d7cd) — Primary dim state, used for "Now" triggers to create a cooling effect

### Secondary & Supporting Colors
- **Muted Indigo-Lavender** (#bac3ff) — Secondary color for "done" states, historical data, and less urgent information
- **Deep Navy Container** (#041970) — Secondary container backgrounds, provides tonal depth
- **Softened Indigo** (#96a5ff) — Secondary dim state, used for "Later" triggers

### Tertiary (Celebration) Colors
- **Warm Sunset Amber** (#ffc87f) — Celebrates small wins, used for positive reinforcement without alarm
- **Amber Glow Container** (#feb64c) — Tertiary container for celebration states
- **Muted Amber** (#eea840) — Tertiary dim state, softer presence of celebration

### Text & On-Surface Colors
- **Soft Off-White** (#e3e6ea) — Primary text color, never uses pure white (#FFFFFF) to reduce eye strain
- **Deep Forest Green** (#005c56) — On-primary text, high contrast for teal backgrounds
- **Muted Teal-Sage** (#a8abb0) — On-surface-variant for secondary text and labels
- **Warm Brown-Auburn** (#644000) — On-tertiary text for amber backgrounds

### Error & Alert Colors
- **Soft Coral Red** (#ff716c) — Error state, deliberately softer than typical alert reds to avoid triggering stress
- **Ghost Gray** (#44484c) — Outline variant, used at 15% opacity for accessible ghost borders

## 3. Typography Rules

### Font Families
The type system balances the technical precision of **Inter** for utility with the sophisticated, wide-set nature of **Manrope** for headlines.

- **Display & Headlines (Manrope):** Large, expressive, and airy. Used for daily focus titles to give them a sense of "ceremony." The wide apertures of Manrope convey a sense of modern openness and editorial authority.
- **Body & Labels (Inter):** All body text uses Inter for high legibility and functional clarity.

### Typography Hierarchy
- **Display Scale (3.5rem):** Used for the single most important element on screen—the daily focus or greeting. Bold and unapologetic.
- **Headline Scale (1.75rem):** Section headers with generous margin-bottom (Spacing 8) to prevent visual crowding.
- **Body Scale (1rem):** Default body text for all general information to ensure high legibility on mobile displays.
- **Label Scale:** For secondary metadata, labels, and supporting information.

### Character & Spacing
- **Display/Headlines:** Use wider letter-spacing and generous line heights to create "breathing room"
- **Body/Labels:** Standard spacing for functional text, slightly increased line height (1.5+) for readability
- **Intentional Hierarchy:** Use tertiary (warm amber) for labels requiring attention without causing alarm. Use secondary (muted indigo) for "done" states or historical data.

## 4. Component Stylings

### Buttons & Touch Targets
- **Minimum Target:** All interactive elements must be at least **48dp x 48dp** to ensure comfortable touch interaction.
- **Primary Action:** A pill-shaped (ROUND_FULL roundedness) button using the primary gradient from Soft Bright Teal (#8ef4e9) to Teal Container (#7fe6db). No border.
- **Gradient Effect:** Subtle linear gradients on primary CTAs to add a "lithographic" soul to the interface
- **Tertiary Action:** title-sm text with a secondary color, no container, placed in generous whitespace.

### Habit "Orbs" (Cards)
- **Shape:** Instead of rectangular lists, use large, rounded cards (rounded-lg to rounded-full) that feel like tactile orbs or stones.
- **No Dividers:** Forbid the use of horizontal lines. Use spacing-4 (1.4rem) of vertical whitespace to separate tasks.
- **Background:** Surface Container High (#1d2023) for active states, transitioning to surface colors for inactive
- **The Imperfection State:** When a habit is missed, the card background shifts from Surface Container High to Tertiary Container (#feb64c) at 10% opacity. It glows with a soft amber hue rather than turning red.
- **Elevation:** Soft tonal lift from surface background, never using hard shadows

### Input Fields
- **Form:** Use "Understated Inputs." No box containers.
- **Style:** A thick surface-variant (#22262a) bottom bar (2px) that transforms into a primary glow when focused.
- **Focus State:** Apply a soft backdrop-blur to the rest of the UI when a user is typing to minimize peripheral distraction.

### Event-Based Triggers
- **Visual Priority:** Use Primary Dim (#71d7cd) for "Now" triggers and Secondary Dim (#96a5ff) for "Later." This creates a cooling effect on the brain.
- **Contextual Coloring:** Triggers are color-coded by urgency but never use high-stress reds.

### Floating Elements (Modals, Pickers)
- **Glass Effect:** Combine surface-variant at 60% opacity with a backdrop-blur of 20px.
- **Shadows:** Extra-diffused shadow: box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4). Never pure black—tinted with deep indigo.
- **The "Ghost Border" Fallback:** If accessibility requires a stroke, use outline-variant (#44484c) at **15% opacity**. Never use 100% opaque borders.

## 5. Layout Principles

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to define sections or cards. Boundaries must be established solely through background color shifts or subtle tonal transitions.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers—stacked sheets of fine, semi-translucent paper.
- **Base Layer:** Surface (#0c0e10)
- **Secondary Content:** Surface Container (#171a1d)
- **Interactive Elements:** Surface Container High (#1d2023) or Highest (#22262a)

### Whitespace Strategy
- **Expansive Negative Space:** Use generous spacing between elements to create "breathing room"
- **Asymmetrical Margins:** (e.g., 10% left, 15% right) on editorial pages to create a "human" feel
- **Vertical Separation:** Use spacing-4 (1.4rem) of vertical whitespace to separate tasks instead of dividers
- **Progressive Disclosure:** If there are more than three primary tasks in a view, use tonal layering to hide complexity by default

### Grid & Alignment
- **Intentional Asymmetry:** Move away from rigid grids toward organic, editorial layouts
- **Single-Column Focus:** Primary views prioritize one main focus element with supporting content below

## 6. Do's and Don'ts

### Do
- **Do** use asymmetrical margins (e.g., 10% left, 15% right) on editorial pages to create a "human" feel.
- **Do** prioritize the Dark Mode (background: #0c0e10) as the "Hero" experience.
- **Do** use the tertiary amber (#ffc87f) to celebrate small wins—it should feel like a warm sunset.
- **Do** use tonal shifts for section boundaries instead of borders
- **Do** give content room to breathe—more whitespace than typical productivity apps
- **Do** ensure all text-to-background contrast ratios meet WCAG AA standards, even with soft tones

### Don't
- **Don't** use "Alert Red." If a task is overdue, it is "Awaiting Your Care," styled in muted amber.
- **Don't** cram more than three primary tasks into a single view. Use progressive disclosure through tonal layering.
- **Don't** use 100% white (#FFFFFF). Use on-background (#e3e6ea) to keep the contrast "soft" and reduce eye strain.
- **Don't** use 1px solid borders or dividers
- **Don't** use sharp corners—all roundedness should be at least md (1.5rem)
- **Don't** create visual noise with too many competing elements

## 7. Accessibility Note

While we use soft tones to reduce cognitive load, all text-to-background contrast ratios must meet WCAG AA standards. If a soft teal lacks contrast, use the primary-fixed-dim variant to darken the foreground without losing the hue's calming intent. Touch targets must remain at least 48dp regardless of visual styling.

---

*This design system was synthesized from the Kairos Stitch project (ID: 14010359681060407838) and reflects the ADHD-optimized "Sustainable Imperfection" philosophy of habit building.*
