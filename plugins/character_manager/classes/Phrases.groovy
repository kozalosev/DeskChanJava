package classes

import enums.PhraseAction

// Класс для хранения наборов фраз для каждого действия.
class Phrases {
    Set<String> welcomeMessages = new HashSet<>()
    Set<String> clickMessages = new HashSet<>()
    Set<String> feedMessages = new HashSet<>()
    Set<String> naughtyMessages = new HashSet<>()
    Set<String> walkMessages = new HashSet<>()
    Set<String> playMessages = new HashSet<>()
    Set<String> watchMessages = new HashSet<>()
    Set<String> petMessages = new HashSet<>()
    Set<String> phrases = new HashSet<>()

    // Геттер.
    Set<String> get(PhraseAction action) {
        switch (action) {
            case PhraseAction.MESSAGE:
                return phrases
            case PhraseAction.WELCOME:
                return welcomeMessages
            case PhraseAction.CLICK:
                return clickMessages
            case PhraseAction.FEED:
                return feedMessages
            case PhraseAction.NAUGHTY:
                return naughtyMessages
            case PhraseAction.WALK:
                return walkMessages
            case PhraseAction.PLAY:
                return playMessages
            case PhraseAction.WATCH:
                return watchMessages
            case PhraseAction.PET:
                return petMessages
            default:
                return null
        }
    }

    // Метод для объединения фраз с фразами из другого набора.
    void concat(Phrases another) {
        welcomeMessages.addAll(another.welcomeMessages)
        clickMessages.addAll(another.clickMessages)
        feedMessages.addAll(another.feedMessages)
        naughtyMessages.addAll(another.naughtyMessages)
        walkMessages.addAll(another.walkMessages)
        playMessages.addAll(another.playMessages)
        watchMessages.addAll(another.watchMessages)
        petMessages.addAll(another.petMessages)
        phrases.addAll(another.phrases)
    }

    // Добавляет новую фразу для указанного действия.
    void add(PhraseAction action, String phrase) {
        switch (action) {
            case PhraseAction.MESSAGE:
                phrases.add(phrase)
                break
            case PhraseAction.WELCOME:
                welcomeMessages.add(phrase)
                break
            case PhraseAction.CLICK:
                clickMessages.add(phrase)
                break
            case PhraseAction.FEED:
                feedMessages.add(phrase)
                break
            case PhraseAction.NAUGHTY:
                naughtyMessages.add(phrase)
                break
            case PhraseAction.WALK:
                walkMessages.add(phrase)
                break
            case PhraseAction.PLAY:
                playMessages.add(phrase)
                break
            case PhraseAction.WATCH:
                watchMessages.add(phrase)
                break
            case PhraseAction.PET:
                petMessages.add(phrase)
                break
        }
    }
}