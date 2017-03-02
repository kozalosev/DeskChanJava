package classes

import enums.PhraseAction
import enums.PhraseCondition

// Класс для хранения набора фраз под каждое условие для текущего времени суток.
class PhrasesSet {
    private Phrases defaultPhrases = new Phrases()

    private Phrases fedPhrases = new Phrases()
    private Phrases hungryPhrases = new Phrases()

    private Phrases sexuallySatisfiedPhrases = new Phrases()
    private Phrases sexuallyHungryPhrases = new Phrases()

    // Геттеры, которые при отсутствии фраз в каком-либо наборе, возвращают фразы по умолчанию.
    Set<String> getDefaultPhrases(PhraseAction action) {
        return defaultPhrases.get(action)
    }

    Set<String> getFullPhrases(PhraseAction action) {
        return baseGetter(fedPhrases, action)
    }

    Set<String> getHungryPhrases(PhraseAction action) {
        return baseGetter(hungryPhrases, action)
    }

    Set<String> getSexuallySatisfiedPhrases(PhraseAction action) {
        return baseGetter(sexuallySatisfiedPhrases, action)
    }

    Set<String> getSexuallyHungryPhrases(PhraseAction action) {
        return baseGetter(sexuallyHungryPhrases, action)
    }

    // Метод для объединения фраз из двух объектов.
    void concat(PhrasesSet another) {
        defaultPhrases.concat(another.defaultPhrases)
        fedPhrases.concat(another.fedPhrases)
        hungryPhrases.concat(another.hungryPhrases)
        sexuallySatisfiedPhrases.concat(another.sexuallySatisfiedPhrases)
        sexuallyHungryPhrases.concat(another.sexuallyHungryPhrases)
    }

    // Добавляет новые фразы под указанное условие.
    void add(PhraseCondition condition, Phrases phrases) {
        switch (condition) {
            case PhraseCondition.DEFAULT:
                defaultPhrases.concat(phrases)
                break
            case PhraseCondition.FED:
                fedPhrases.concat(phrases)
                break
            case PhraseCondition.HUNGRY:
                hungryPhrases.concat(phrases)
                break
            case PhraseCondition.SEXUALLY_SATISFIED:
                sexuallySatisfiedPhrases.concat(phrases)
                break
            case PhraseCondition.SEXUALLY_HUNGRY:
                sexuallyHungryPhrases.concat(phrases)
                break
        }
    }

    // Добавляет новую фразу под указанное условие и действие.
    void add(PhraseAction action, PhraseCondition condition, String phrase) {
        switch (condition) {
            case PhraseCondition.DEFAULT:
                defaultPhrases.add(action, phrase)
                break
            case PhraseCondition.FED:
                fedPhrases.add(action, phrase)
                break
            case PhraseCondition.HUNGRY:
                hungryPhrases.add(action, phrase)
                break
            case PhraseCondition.SEXUALLY_SATISFIED:
                sexuallySatisfiedPhrases.add(action, phrase)
                break
            case PhraseCondition.SEXUALLY_HUNGRY:
                sexuallyHungryPhrases.add(action, phrase)
                break
        }
    }

    private baseGetter(Phrases source, PhraseAction action) {
        Set<String> set = source.get(action)
        return (!set.isEmpty()) ? set : getDefaultPhrases(action)
    }
}