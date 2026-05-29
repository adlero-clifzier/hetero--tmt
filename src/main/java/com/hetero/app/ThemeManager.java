package com.hetero.app;

import javafx.scene.Scene;

/** Manages dark / light theme switching across the application. */
public final class ThemeManager {
    public enum Theme { DARK, LIGHT }

    private static Theme current = Theme.DARK;
    private static Scene scene;

    private ThemeManager() {}

    public static void init(Scene s) { scene = s; apply(); }

    public static void toggle() {
        current = (current == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        apply();
    }

    public static Theme getCurrent() { return current; }

    private static void apply() {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = (current == Theme.DARK)
                ? "/com/hetero/css/hetero-dark.css"
                : "/com/hetero/css/hetero-light.css";
        scene.getStylesheets().add(ThemeManager.class.getResource(css).toExternalForm());
    }
}
