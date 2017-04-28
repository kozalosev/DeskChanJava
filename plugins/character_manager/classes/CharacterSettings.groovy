package classes

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern


class CharacterSettings {
    private Set<String> animeWebsiteURLs = new HashSet<>()
    private Set<Integer> gameSteamIds = new HashSet<>()

    CharacterSettings(Path filepath) {
        if (!Files.isRegularFile(filepath)) {
            animeWebsiteURLs.add("http://animespirit.ru")
            animeWebsiteURLs.add("http://animeonline.su")
            animeWebsiteURLs.add("http://yummyanime.com")
            return
        }

        String currentSection = ""

        Files.lines(filepath).forEach({ line ->
            if (line.trim() == "" || line[0] == '#')
                return

            Pattern p = Pattern.compile("\\[([A-Za-z_]+)]")
            Matcher m = p.matcher(line)
            while (m.find()) {
                currentSection = m.group(1)
                return
            }

            if (currentSection == "GAME_STEAM_IDS") {
                int id = Integer.parseInt(line)
                gameSteamIds.add(id)
            }
            else if (currentSection == "ANIME_WEBSITE_URLS") {
                animeWebsiteURLs.add(line)
            }
        })
    }

    URL getRandomAnimeWebsite() {
        if (animeWebsiteURLs.size() == 0)
            return null

        Random random = new Random()
        int randI = random.nextInt(animeWebsiteURLs.size())

        URL url = null
        try {
            url = new URL(animeWebsiteURLs[randI])
        } catch (MalformedURLException e) {
            e.printStackTrace()
        }

        return url
    }

    URI getRandomSteamId() {
        if (gameSteamIds.size() == 0)
            return null

        Random random = new Random()
        int randId = random.nextInt(gameSteamIds.size())
        String link = "steam://rungameid/" + gameSteamIds[randId]
        URI uri = new URI(link)
        return uri
    }
}
