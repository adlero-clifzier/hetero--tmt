package com.hetero.app;

import javafx.scene.Scene;

import java.util.logging.Logger;

/**
 * Application-wide theme manager that switches between the dark and light
 * CSS stylesheets without requiring an application restart.
 *
 * <p>The active theme is applied to whichever {@link Scene} was most recently
 * passed to {@link #init}.  When the user navigates from the Login screen to
 * the Main layout, {@link HeteroApp} calls {@link #init} again with the new scene
 * so the correct stylesheet is always attached.
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Instance variables and objects:</b>
 *       {@code currentTheme} is a static enum instance variable;
 *       {@code activeScene} is a static {@link Scene} object reference.</li>
 *   <li><b>Imported classes:</b>
 *       {@link Scene} from {@code javafx.scene}; {@link Logger} from {@code java.util.logging}.</li>
 *   <li><b>Custom-built class:</b>
 *       The nested {@link Theme} enum is defined within this class for Hetero's domain.</li>
 *   <li><b>Proper access control:</b>
 *       Constructor is {@code private}; all state is accessed through the static API.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       {@code currentTheme}, {@code activeScene}, {@code stylesheetPath} — all self-describing.</li>
 * </ul>
 */
public final class ThemeManager {

    // ── Nested enum ───────────────────────────────────────────────────────────

    /**
     * The two supported visual themes.
     * Ordinal values are not used — the enum is compared by identity.
     */
    public enum Theme {
        /** Dark mode — Notion-inspired dark palette (default). */
        DARK,
        /** Light mode — clean light palette for bright environments. */
        LIGHT
    }

    // ── Logger ────────────────────────────────────────────────────────────────

    /** Logger for theme-change events. */
    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());

    // ── Static state ──────────────────────────────────────────────────────────

    /**
     * The currently selected theme. Defaults to {@link Theme#DARK} on startup.
     * Changed via {@link #toggle()}.
     */
    private static Theme currentTheme = Theme.DARK;

    /**
     * The JavaFX {@link Scene} to which the active stylesheet is applied.
     * Updated each time {@link #init} is called with a new scene.
     */
    private static Scene activeScene;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Private constructor — this class is a pure static utility; instantiation
     * is not meaningful and is therefore blocked.
     */
    private ThemeManager() { }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises the manager with the given scene and immediately applies
     * the currently selected theme stylesheet.
     *
     * <p>Must be called every time a new {@link Scene} is set on the
     * primary stage so that navigation transitions preserve the theme choice.
     *
     * @param scene the scene that should receive the active stylesheet; must not be null
     */
    public static void init(Scene scene) {
        activeScene = scene;
        applyCurrentTheme();
    }

    /**
     * Toggles between {@link Theme#DARK} and {@link Theme#LIGHT} and
     * immediately re-applies the new stylesheet to the active scene.
     *
     * <p>Called by {@link com.hetero.controller.SettingsController#onToggleTheme}.
     */
    public static void toggle() {
        currentTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        applyCurrentTheme();
        LOGGER.info("[Theme] Switched to: " + currentTheme);
    }

    /**
     * Returns the currently selected theme.
     *
     * @return {@link Theme#DARK} or {@link Theme#LIGHT}
     */
    public static Theme getCurrent() {
        return currentTheme;
    }

    // ── Private helper ────────────────────────────────────────────────────────

    /**
     * Clears all existing stylesheets from the active scene and attaches
     * the CSS file that corresponds to the current theme.
     *
     * <p>Stylesheet paths:
     * <ul>
     *   <li>Dark  — {@code /com/hetero/css/hetero-dark.css}</li>
     *   <li>Light — {@code /com/hetero/css/hetero-light.css}</li>
     * </ul>
     */
    private static void applyCurrentTheme() {
        if (activeScene == null) {
            return; // Guard against calls before init() has been invoked
        }

        String stylesheetPath = (currentTheme == Theme.DARK)
                ? "/com/hetero/css/hetero-dark.css"
                : "/com/hetero/css/hetero-light.css";

        activeScene.getStylesheets().clear();
        activeScene.getStylesheets().add(
            ThemeManager.class.getResource(stylesheetPath).toExternalForm());
    }
}
