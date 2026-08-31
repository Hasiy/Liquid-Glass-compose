# Liquid Glass Compose SDK

`liquidglass-compose` is a reusable Android Library containing Liquid Glass styled Jetpack Compose and Material3 components. It does not depend on the demo app or its resources.

## Requirements

- Android minSdk 26
- compileSdk 35
- Kotlin 2.0.21
- Compose BOM 2024.10.01 / Material3 1.3.1

## Add to another project

For a composite/local project:

```kotlin
dependencies {
    implementation(project(":liquidglass"))
}
```

Build a reusable AAR:

```powershell
.\gradlew.bat :liquidglass:assembleRelease
```

The artifact is generated at `liquidglass/build/outputs/aar/liquidglass-release.aar`.

When copying the raw AAR instead of consuming the module/Maven publication, also add its runtime blur engine explicitly:

```kotlin
implementation("dev.chrisbanes.haze:haze:1.4.0")
```

The module also configures Maven publication coordinates:

```text
top.hasiyliquidglass:liquidglass-compose:1.0.0
```

Run `publishReleasePublicationToMavenLocal` when local Maven consumption is preferred.

## Theme and touch behavior

Use `GlassPresets.Drop`, `Neutral`, `Dark`, or `Native`, or create a custom `GlassConfig`. At rest the glass highlight sits at `highlightCenterX` / `highlightCenterY` (upper-left by default). While the user presses a component the highlight follows the pointer, brightens, tightens, and gains an extra fingertip hot spot; on release it fades back and springs to its resting position. That is the default (`followTouchHighlight = true`, `hideHighlightOnTouch = false`). Set `followTouchHighlight = false` to keep the press brightening without the pointer tracking, or `hideHighlightOnTouch = true` to fade the highlight out while pressed instead.

`glassSurface` clips its content with `clip(shape)`. Any click modifier that draws a Material indication (`clickable`, `selectable`, `toggleable`, ...) must therefore be chained **after** `glassSurface`, otherwise the ripple is drawn before the clip and spills out of the rounded corners as a rectangle.

## Public component coverage

| Category | APIs |
| --- | --- |
| Foundation | `glassSurface`, `glassBlur`, `GlassCard`, `GlassButton`, `GlassIconButton` |
| Dialog / Popup | `GlassBackdropHost`, `GlassDialogBlurHost`, `GlassDialog`, `GlassPopupBlurBox`, `GlassPopup` |
| Selection | `GlassCheckbox`, `GlassRadioButton`, `GlassSlider`, `GlassVolumeSlider`, `GlassSwitch` |
| Navigation | `GlassSegmentedTabBar`, `GlassNavigationBar`, `GlassNavigationRail`, `GlassModalNavigationDrawer` |
| Bottom sheets | `GlassBottomSheet`, `GlassBottomSheetScaffold`, `GlassModalBottomSheet` |
| Actions | `GlassFloatingActionButton`, `GlassExtendedFloatingActionButton` |
| Information | `GlassSnackbar`, `GlassBadge`, `GlassAssistChip`, `GlassSuggestionChip`, `GlassFilterChip`, `GlassInputChip`, `GlassTooltipBox` |
| Menus | `GlassDropdownMenu`, `GlassDropdownMenuItem`, `GlassExposedDropdownMenuBox`, `GlassContextMenuArea` |
| Input | `GlassTextField`, `GlassSearchBar`, `GlassDockedSearchBar`, `GlassDatePickerDialog`, `GlassTimePickerDialog` |
| Feedback | `GlassProgressBar`, `GlassPullToRefreshBox` |
| Layout / list | `GlassTopBar`, `GlassListItem`, `GlassThemeSelector` |

`GlassBottomSheetScaffold` is a screen/root-layout component. Like Material3's original scaffold, it must receive a finite height and must not be placed directly inside `verticalScroll` or another parent that measures children with infinite height.

## Blur contracts

The rendering contract has four layers:

```text
L0  Page background and decoration
L1  Real page content recorded once by GlassBackdropHost
L2  Glass surface: backdrop blur/fallback tint, highlight, border, and shadow
L3  Foreground text, icons, and controls; never blurred
```

- Wrap each screen once with `GlassBackdropHost`. It records the page as a blur source but never blurs the page itself.
- A nested `GlassBackdropHost` reuses the outer source instead of creating another capture source.
- Every SDK overlay applies backdrop blur only inside its own rounded surface: Dialog, Popup, Dropdown/ExposedDropdown, ContextMenu, docked-search results, Tooltip, DatePicker, TimePicker, Drawer, BottomSheet/ModalBottomSheet, and Snackbar.
- Content outside the overlay stays sharp. Glass modal components also use a transparent Material scrim; the invisible scrim still handles outside-tap dismissal.
- Pass the same `Shape` to `glassBackdrop`, `glassSurface`, and the Material container so blur, fill, border, and clipping remain aligned.
- `overlayBlurRadius` in `GlassConfig` controls the default radius. `GlassBackdropHost(blurRadius = ...)`, legacy `GlassDialogBlurHost`, or legacy `GlassPopupBlurBox` can override it for a subtree.
- API 26–30, a missing host, or another unsupported backdrop path uses `overlayFallbackAlpha` as a denser frosted tint; foreground content remains readable and interactive.
- `GlassDialogBlurHost` and `GlassPopupBlurBox` remain source-compatible, but their old whole-page/anchor-blur behavior has intentionally been removed.

```kotlin
GlassBackdropHost(Modifier.fillMaxSize()) {
    ScreenContent()
    if (showDialog) {
        GlassDialog(
            onDismissRequest = { showDialog = false },
            title = "Title",
            message = "Only this card blurs its backdrop",
            onConfirm = { showDialog = false },
        )
    }
}
```
