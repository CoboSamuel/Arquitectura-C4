# Consulta ciudadana UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (native) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a clear start page, improve the citizen-facing visual language, and show field validation feedback while the person is entering data without changing the existing consultation APIs.

**Architecture:** Keep the existing `App.jsx` workflow and reducer as the source of truth. Add a local `started` UI state for the landing-to-form transition, a local touched-field set for live feedback, and a small presentational `LandingPage` component. Keep validation rules in `consulta/validacion.js`; only the timing and display of validation change.

**Tech Stack:** React 19, Vite 7, plain CSS, Node built-in test runner.

**Spec:** `docs/superpowers/specs/2026-11-17-consulta-ciudadana-ux-design.md`

## Global Constraints

- Preserve the existing three-step consultation flow and backend contracts.
- Do not add authentication, persistence, or new services.
- Do not present this academic project as an official SRI portal.
- Use the existing validation functions from `frontend/src/consulta/validacion.js`.
- Keep keyboard focus, `aria-invalid`, `aria-describedby`, and live status announcements functional.

## Review Focus

- Landing state: first render shows purpose and does not force the user into the form; covered by `App.test.js`.
- Start action: clicking **Iniciar consulta** reveals the first step and focuses its heading; covered by the App interaction smoke test if supported and by the rendered structure assertion.
- Partial values: a field should not show an error before it has been touched; covered by the field-validation tests.
- Invalid edits after blur: an error should update as the value changes and clear when valid; covered by the field-validation tests.
- Existing server errors/results: the new UI must not remove the current retry, summaries, license result, or loading behavior; covered by the full frontend test and build commands.

### Task 1: Add explicit field-validation state and tests

**Files:**
- Modify: `frontend/src/App.jsx` (`FormField`, App state, `changeField`, validation effects/handlers)
- Test: `frontend/src/App.test.js`

**Interfaces:**
- Consumes: `validarCorreo`, `validarRuc`, `validarPlaca`, and `validarCedula`.
- Produces: a `getFieldError(campo)` behavior that returns no message for untouched fields, then returns the existing validator message once a field is touched or submitted.

- [ ] **Step 1: Write failing render assertions**

Add assertions that the first render contains the landing copy and `Iniciar consulta`, while the form labels are not rendered until the start action state is active. Add assertions for the form's validation contract: field errors render only with `aria-invalid="true"` and the existing helper remains the description when there is no error.

- [ ] **Step 2: Run the focused frontend test**

Run: `npm test -- --test-name-pattern "App renderiza"` from `frontend`.

Expected: FAIL because the current App renders the form immediately and has no touched-field validation state.

- [ ] **Step 3: Implement touched-field validation**

Add `const [touchedFields, setTouchedFields] = useState(new Set())` or an equivalent immutable object. Mark the relevant field touched on change and blur. Compute per-field messages with the existing validators, but only pass an error to `FormField` if that field is touched or the current submit attempt has exposed it. Keep the existing submit-time validation and focus-to-first-error behavior. When restarting or moving to a completed step, clear touched state for the fields that are no longer visible.

- [ ] **Step 4: Run focused tests again**

Run: `npm test -- --test-name-pattern "App renderiza"` from `frontend`.

Expected: PASS for the render contract, with no regression in the current server-side error/result assertions.

### Task 2: Add the landing page and start transition

**Files:**
- Modify: `frontend/src/App.jsx` (new `LandingPage` component and App render branch)
- Modify: `frontend/src/App.test.js`

**Interfaces:**
- Consumes: existing consultation state and `headingRef`.
- Produces: `LandingPage({ onStart })`, which calls `onStart` from a primary button labeled `Iniciar consulta`.

- [ ] **Step 1: Add a failing landing-to-form behavior check**

Extend the App test's server-render assertions to require the public-facing introduction, the three preparation items, and the absence of the form's `Correo electrónico` label before the user starts.

- [ ] **Step 2: Implement `LandingPage` and local start state**

Add `const [started, setStarted] = useState(false)`. Render a landing section when false, with concise copy explaining that the page brings together RUC, vehicle, and license information in three steps; list the required data; include a primary `Iniciar consulta` button and a small academic-project disclaimer. When the button is activated, set `started` to true and focus the step heading after the form appears. Keep the existing consultation section intact when `started` is true.

- [ ] **Step 3: Run the App test**

Run: `npm test -- --test-name-pattern "App renderiza"` from `frontend`.

Expected: PASS with the landing page present in SSR output and no form rendered in the initial state.

### Task 3: Refine citizen-facing presentation and responsive states

**Files:**
- Modify: `frontend/src/styles.css`

**Interfaces:**
- Consumes: new landing classes and existing workflow classes.
- Produces: responsive, accessible visual states for landing, form, validation, focus, loading, error, and results.

- [ ] **Step 1: Add the landing and form-state styles**

Append a focused section to the stylesheet that establishes a restrained institutional palette, a clear landing composition, visible primary action, preparation cards, and consistent field success/error styling. Avoid gradients and decorative labels that imply an AI-generated dashboard. Preserve existing media-query behavior and ensure a 320px minimum viewport remains usable.

- [ ] **Step 2: Add responsive overrides**

At mobile widths, stack landing content, make the start and submit buttons full width, keep helper/error text readable, and retain visible focus outlines.

- [ ] **Step 3: Run the frontend build**

Run: `npm run build` from `frontend`.

Expected: Vite exits with code 0 and emits the production bundle.

### Task 4: Full verification and diff review

**Files:**
- Verify: `frontend/src/App.jsx`, `frontend/src/styles.css`, `frontend/src/App.test.js`, `docs/superpowers/specs/2026-11-17-consulta-ciudadana-ux-design.md`, `docs/superpowers/plans/2026-11-17-consulta-ciudadana-ux.md`

- [ ] **Step 1: Run all frontend tests**

Run: `npm test` from `frontend`.

Expected: all tests pass with zero failures.

- [ ] **Step 2: Inspect the final diff**

Run: `git diff --check` and `git status --short` from the repository root.

Expected: no whitespace errors; only the intended frontend and UX documentation files are changed by this task, alongside pre-existing user changes.

- [ ] **Step 3: Re-read requirements against the implementation**

Confirm the landing page precedes the form, the start button reveals the flow, invalid data feedback appears before submit after touch, and the backend/API code is unchanged.
