package classes

import java.awt.Desktop
import java.awt.EventQueue

// Вспомогательный класс для открытия ссылок в браузере.
// Код основан на следующем ответе: http://stackoverflow.com/a/10967469
class BrowserAdapter {

    static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null
        if (desktop?.isSupported(Desktop.Action.BROWSE)) {
            EventQueue.invokeLater({ ->
                try {
                    desktop.browse(uri)
                } catch (Exception e) {
                    e.printStackTrace()
                }
            })
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
            openWebpage(new URI(url))
        } catch (URISyntaxException e) {
            e.printStackTrace()
        }
    }

}
