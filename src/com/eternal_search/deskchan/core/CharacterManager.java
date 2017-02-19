package com.eternal_search.deskchan.core;

import com.eternal_search.deskchan.exceptions.WrongCharacterException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

public abstract class CharacterManager {
    public static String[] getCharacterList() {
        ArrayList<String> list = new ArrayList<>();
        Path directoryPath = Utils.getResourcePath("characters");
        if (directoryPath != null) {
            try {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath);
                for (Path characterPath : directoryStream) {
                    if (Files.isDirectory(characterPath)) {
                        list.add(characterPath.getFileName().toString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
            throw new WrongCharacterException("Directory for characters not found at all!");

        String[] resultArray = new String[list.size()];
        resultArray = list.toArray(resultArray);
        return resultArray;
    }

    public static Character getRandomCharacter() {
        String[] listOfCharacters = getCharacterList();

        Random random = new Random();
        int maxNumber = listOfCharacters.length;
        String randomName = listOfCharacters[random.nextInt(maxNumber)];

        return new Character(randomName);
    }
}
