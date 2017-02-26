package classes

class Phrases {
    String welcomeMessage = null
    String clickMessage = null
    List<String> phrases = new ArrayList<String>()

    void concat(Phrases another) {
        if (welcomeMessage == null)
            welcomeMessage = another.welcomeMessage
        if (clickMessage == null)
            clickMessage = another.clickMessage

        phrases.addAll(another.phrases)
    }
}