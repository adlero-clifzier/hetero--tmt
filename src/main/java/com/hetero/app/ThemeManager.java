package com.hetero.app;

import javafx.scene.Scene;
import java.util.logging.Logger;

/**
 * ThemeManager switches the app between dark mode and light mode.
 *
 * It works by swapping the CSS stylesheet attached to the current Scene.
 * Dark mode loads hetero-dark.css and light mode loads hetero-light.css.
 *
 * init() must be called every time we show a new screen so the correct
 * theme is applied to that screen's Scene object.
 *
 * The nested Theme enum holds the two possible values: DARK and LIGHT.
 */
public final class ThemeManager {

    /** The two supported themes. */
    public enum Theme {
        DARK,
        LIGHT
    }

    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());

    // Which theme is currently active. Starts on dark mode.
    private static Theme currentTheme = Theme.DARK;

    // The Scene that is currently on screen — we apply the stylesheet to this
    private static Scene activeScene;

    // Prevent instantiation
    private ThemeManager() { }

    /**
     * Registers a Scene and immediately applies the current theme to it.
     * Call this every time a new screen is loaded.
     *
     * @param scene the Scene to apply the theme to
     */
    public static void init(Scene scene) {
        activeScene = scene;
        applyCurrentTheme();
    }

    /**
     * Switches between dark and light mode.
     * The change is visible instantly on the current screen.
     */
    public static void toggle() {
        currentTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        applyCurrentTheme();
        LOGGER.info("[Theme] Switched to: " + currentTheme);
    }

    /**
     * Returns which theme is currently active.
     *
     * @return Theme.DARK or Theme.LIGHT
     */
    public static Theme getCurrent() {
        return currentTheme;
    }

    /**
     * Clears the old stylesheet and loads the one matching the current theme.
     */
    private static void applyCurrentTheme() {
        if (activeScene == null) {
            return; // Nothing to apply to yet
        }

        String cssPath = (currentTheme == Theme.DARK)
                ? "/com/hetero/css/hetero-dark.css"
                : "/com/hetero/css/hetero-light.css";

        activeScene.getStylesheets().clear();
        activeScene.getStylesheets().add(
            ThemeManager.class.getResource(cssPath).toExternalForm());
    }
}
