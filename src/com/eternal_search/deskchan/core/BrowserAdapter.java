package com.eternal_search.deskchan.core;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;

// Based on: http://stackoverflow.com/a/10967469
public class BrowserAdapter {

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}