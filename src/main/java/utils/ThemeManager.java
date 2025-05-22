package utils;

import javafx.scene.Scene;

import java.net.URL;
import java.util.logging.Logger;

public class ThemeManager {

    private static final Logger logger = Logger.getLogger(ThemeManager.class.getName());

    private static final String LIGHT_THEME_PATH = "/css/light-theme.css";
    private static final String DARK_THEME_PATH = "/css/dark-theme.css";

    public enum Theme {
        LIGHT, DARK
    }

    private static Theme currentTheme = Theme.LIGHT;

    // Applique le thème spécifié à une scène
    public static void applyTheme(Scene scene, Theme theme) {
        String cssPath = (theme == Theme.DARK) ? DARK_THEME_PATH : LIGHT_THEME_PATH;
        URL themeUrl = ThemeManager.class.getResource(cssPath);

        if (themeUrl == null) {
            logger.severe("[ThemeManager] Impossible de charger le fichier : " + cssPath);
            return;
        }

        scene.getStylesheets().clear();
        scene.getStylesheets().add(themeUrl.toExternalForm());
        currentTheme = theme;

        logger.info("[ThemeManager] Thème appliqué : " + theme.name());
    }

    // Applique le thème actuellement actif
    public static void applyCurrentTheme(Scene scene) {
        applyTheme(scene, currentTheme);
    }

    // Bascule entre clair et sombre
    public static void toggleTheme(Scene scene) {
        Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        applyTheme(scene, newTheme);
    }

    // Retourne le thème actuel
    public static Theme getCurrentTheme() {
        return currentTheme;
    }
}
