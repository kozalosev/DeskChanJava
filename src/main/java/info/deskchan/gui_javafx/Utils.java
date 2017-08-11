package info.deskchan.gui_javafx;

import info.deskchan.core.LocalizedManifestStrings;


abstract class Utils {

    static final LocalizedManifestStrings manifestLabels = new LocalizedManifestStrings(
            Main.getString("manifest.name"),
            Main.getString("manifest.version"),
            Main.getString("manifest.description"),
            Main.getString("manifest.authors"),
            Main.getString("manifest.license")
    );

}
