package com.eternal_search.deskchan.core;

import com.eternal_search.deskchan.exceptions.WrongCharacterException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;


public class Character {
    private enum TimeOfDay {
        DAY, NIGHT, MORNING, EVENING
    }

    private String name;
    private SkinInfo defaultSkin;
    private SkinInfo nightSkin;
    private SkinInfo morningSkin;
    private SkinInfo eveningSkin;
    private TimeOfDay currentTimeOfDay;

    Character(String name) throws WrongCharacterException
    {
        this.name = name;

        SkinInfo[] skins = readSkins(name);
        for (SkinInfo info : skins) {
            switch (info.name.substring(0, info.name.length() - 4)) {
                case "default":
                    defaultSkin = info;
                    break;
                case "night":
                    nightSkin = info;
                    break;
                case "morning":
                    morningSkin = info;
                    break;
                case "evening":
                    eveningSkin = info;
                    break;
            }
        }

        if (defaultSkin == null)
            throw new WrongCharacterException("No default skin!");
    }

    public String getName() { return name; }

    public Path getSkin() {
        TimeOfDay timeOfDay = getTimeOfDay();

        switch (timeOfDay) {
            case MORNING:
                currentTimeOfDay = TimeOfDay.MORNING;
                return (morningSkin != null) ? morningSkin.path : defaultSkin.path;
            case NIGHT:
                currentTimeOfDay = TimeOfDay.NIGHT;
                return (nightSkin != null) ? nightSkin.path : defaultSkin.path;
            case EVENING:
                currentTimeOfDay = TimeOfDay.EVENING;
                return (eveningSkin != null) ? eveningSkin.path : defaultSkin.path;
            default:
                currentTimeOfDay = TimeOfDay.DAY;
                return defaultSkin.path;
        }
    }

    public boolean skinReloadRequired() {
        return currentTimeOfDay != getTimeOfDay();
    }

    public String getWelcomePhrase() {
        switch (currentTimeOfDay) {
            case MORNING:
                return "Доброе утро, Хозяин!";
            case NIGHT:
                return "Доброй ночи, Хозяин!";
            case EVENING:
                return "Добрый вечер, Хозяин!";
            default:
                return "Добрый день, Хозяин!";
        }
    }

    private SkinInfo[] readSkins(String path) throws WrongCharacterException
    {
        ArrayList<SkinInfo> list = new ArrayList<>();
        Path directoryPath = Utils.getResourcePath("characters/" + path);
        if (directoryPath != null) {
            try {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath);
                for (Path skinPath : directoryStream) {
                    if (!Files.isDirectory(skinPath)) {
                        list.add(new SkinInfo(skinPath));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
            throw new WrongCharacterException("Character not found!");

        SkinInfo[] resultArray = new SkinInfo[list.size()];
        resultArray = list.toArray(resultArray);
        return resultArray;
    }

    private TimeOfDay getTimeOfDay() {
        Calendar currentTime = Calendar.getInstance();
        int currentHour = currentTime.get(Calendar.HOUR_OF_DAY);

        if (currentHour < 12 && currentHour > 6)
            return TimeOfDay.MORNING;
        else if (currentHour < 17 && currentHour > 6)
            return TimeOfDay.DAY;
        else if (currentHour < 23 && currentHour > 6)
            return TimeOfDay.EVENING;
        else
            return TimeOfDay.NIGHT;
    }
}

class SkinInfo implements Comparable<SkinInfo> {

    String name;
    Path path;

    SkinInfo(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    SkinInfo(Path path) {
        this(path.getFileName().toString(), path);
    }

    @Override
    public int compareTo(SkinInfo o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name;
    }

}