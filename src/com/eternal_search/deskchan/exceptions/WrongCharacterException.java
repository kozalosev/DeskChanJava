package com.eternal_search.deskchan.exceptions;

public class WrongCharacterException extends RuntimeException {
    public WrongCharacterException(String string) {
        super(string);
    }

    public WrongCharacterException() {
        super();
    }
}
