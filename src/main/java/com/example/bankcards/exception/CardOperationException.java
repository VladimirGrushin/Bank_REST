package com.example.bankcards.exception;

public class CardOperationException extends RuntimeException {
    public CardOperationException(String message) {
        super(message);
    }
}
