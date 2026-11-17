# Ecuadorian Frontend Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (native) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the complete frontend with an Ecuadorian visual identity, add a Quito illustration, and prevent common email-domain typos from passing silently.

**Architecture:** Keep the existing consultation reducer, API clients, and three-step flow. Add one pure email-domain suggestion helper, integrate it into the existing field validation path, and replace the current visual composition with a single cohesive editorial layout using a local/generated image asset and new CSS tokens.

**Tech Stack:** React 19, Vite 7, plain CSS, Node built-in test runner, generated raster image asset.

**Spec:** `docs/superpowers/specs/2026-11-17-frontend-ecuador-visual-design.md`

## Global Constraints

- Do not change APIs, backend contracts, or consultation business logic.
- Do not use official SRI, ANT, Quito municipality, or Ecuador government logos.
- The image is illustrative and must not be presented as an official or documentary source.
- Do not block legitimate custom or institutional email domains.
- The existing empty-field and format-validation messages remain understandable and accessible.
- The user will create commits and push manually; the agent must not run `git commit` or `git push`.

## Review Focus

- Known typo `samuelcobo29@gmiol.com`: show a correction suggestion and block submit until corrected; covered by domain-helper tests and App render/validation assertions.
- Valid custom domain `persona@universidad.edu.ec`: keep it accepted; covered by domain-helper tests.
- Landing image unavailable or slow: keep the text and CTA usable with a stable fallback color; covered by build and CSS inspection.
- Mobile viewport: preserve readable copy, CTA visibility, and form order; covered by responsive CSS review.
- Existing API/result flow: no request payload or result component changes; covered by the full frontend test suite.

### Task 1: Detect common email-domain typos

**Files:**
- Create: `frontend/src/consulta/dominioCorreo.js`
- Test: `frontend/src/consulta/dominioCorreo.test.js`
- Modify: `frontend/src/consulta/validacion.js`
- Modify: `frontend/src/consulta/validacionEnVivo.js`

**Interfaces:**
- Produces: `sugerirCorreccionDominio(value: string) -> { dominio: string, sugerencia: string } | null`.
- Consumes: existing `validarCorreo` and live-field validation behavior.

- [ ] **Step 1: Write failing tests**

Cover `gmiol.com -> gmail.com`, another common-provider typo, acceptance of `persona@universidad.edu.ec` with no suggestion, and no suggestion for an unknown custom domain.

- [ ] **Step 2: Run the focused tests**

Run: `npm test -- --test-name-pattern "dominio|gmiol|universidad"` from `frontend`.

Expected: FAIL because the helper does not exist.

- [ ] **Step 3: Implement the pure helper and integrate it**

Implement a small map/distance check limited to common public providers (`gmail.com`, `hotmail.com`, `outlook.com`, `yahoo.com`). Return a correction object only when the entered domain is one edit away from a known provider and is not the exact provider. Extend the live validation result so the field can expose the suggestion as an error-like blocking message, while leaving custom domains valid.

- [ ] **Step 4: Run focused and full frontend tests**

Run the focused command above and then `npm test`.

Expected: focused tests and the full suite pass.

### Task 2: Add the Ecuador/Quito visual asset

**Files:**
- Create: `frontend/public/quito-hero.png` (generated illustrative asset)
- Modify: `frontend/index.html` only if metadata needs a visual description.

**Interfaces:**
- Produces: a stable public asset path `/quito-hero.png` for the landing page.

- [ ] **Step 1: Generate the asset**

Use the image generation skill to create a wide editorial illustration of Quito at golden hour: layered Andean hills, historic-center silhouettes, restrained navy/terracotta/gold palette, no text, no logos, no identifiable private people. Preserve enough quiet sky/negative space for overlay or adjacent copy.

- [ ] **Step 2: Verify the asset contract**

Confirm the file exists at `frontend/public/quito-hero.png`, is referenced by the frontend, and has no text/logo content that could be mistaken for official branding.

### Task 3: Redesign the landing and consultation composition

**Files:**
- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/styles.css`
- Modify: `frontend/src/App.test.js`

**Interfaces:**
- Consumes: `/quito-hero.png`, `sugerirCorreccionDominio`, and existing `started`/step state.
- Produces: a responsive editorial landing section, a clearer consultation header, and a field message that blocks known bad-domain submissions.

- [ ] **Step 1: Write failing render assertions**

Require the landing markup to include the Quito image with descriptive alt text, the Ecuadorian visual copy, and the consultation card classes; require the initial render to keep the form hidden.

- [ ] **Step 2: Run the focused App test**

Run: `npm test -- --test-name-pattern "portada|Quito|Ecuador"` from `frontend`.

Expected: FAIL because the current landing has no image and retains the previous visual structure.

- [ ] **Step 3: Implement the composition and field message**

Add the image to `LandingPage`, update copy and landmarks without official claims, refine the consultation shell to feel like one service page, and pass the domain suggestion through `FormField` with `aria-invalid`, `aria-describedby`, and `role="alert"`. Do not modify API calls or result rendering.

- [ ] **Step 4: Replace contradictory CSS overrides**

Consolidate the final palette and layout at the end of `styles.css`, remove or override conflicting purple/gradient rules, set image fallbacks, and add mobile breakpoints for the editorial layout.

- [ ] **Step 5: Run the focused App test**

Run: `npm test -- --test-name-pattern "portada|Quito|Ecuador"`.

Expected: PASS.

### Task 4: Full verification and handoff

**Files:**
- Verify: all files above plus the existing API/result modules.

- [ ] **Step 1: Run all frontend tests**

Run: `npm test` from `frontend`.

Expected: all tests pass with zero failures.

- [ ] **Step 2: Run the production build**

Run: `npm run build` from `frontend`.

Expected: Vite exits with code 0 and includes the Quito asset.

- [ ] **Step 3: Review the working tree without committing**

Run: `git status --short` and `git diff --check` from the repository root.

Expected: no whitespace errors; no commit or push is created by the agent. Report the exact manual `git add`, `git commit`, and `git push` commands to the user.
