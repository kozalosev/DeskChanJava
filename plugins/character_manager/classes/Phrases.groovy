package classes

import enums.PhraseAction

class Phrases {
    Set<String> welcomeMessages = new HashSet<String>()
    Set<String> clickMessages = new HashSet<String>()
    Set<String> feedMessages = new HashSet<String>()
    Set<String> naughtyMessages = new HashSet<String>()
    Set<String> phrases = new HashSet<String>()

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
        }
    }

    void concat(Phrases another) {
        welcomeMessages.addAll(another.welcomeMessages)
        clickMessages.addAll(another.clickMessages)
        feedMessages.addAll(another.feedMessages)
        naughtyMessages.addAll(another.naughtyMessages)
        phrases.addAll(another.phrases)
    }

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
        }
    }
}