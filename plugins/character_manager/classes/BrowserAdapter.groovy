package classes

import java.awt.Desktop

// Вспомогательный класс для открытия ссылок в браузере.
// Код основан на следующем ответе: http://stackoverflow.com/a/10967469
class BrowserAdapter {

    static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri)
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
    }

    static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI())
        } catch (URISyntaxException e) {
            e.printStackTrace()
        }
    }

    static void openWebpage(String url) {
        try {
            openWebpage(new URL(url))
        } catch (MalformedURLException e) {
            e.printStackTrace()
        }
    }

}
