package com.andrejKir.connect.shared.exception;

/**
 * Base for exceptions whose user-facing message is resolved via Spring MessageSource
 * (i18n). Carries a message key + arguments; the actual localized text is looked up
 * in the GlobalExceptionHandler based on the request Locale. The {@code super(message)}
 * is a developer/English message for logs only.
 */
public abstract class LocalizedException extends RuntimeException {

    private final String messageKey;
    private final transient Object[] args;

    protected LocalizedException(String messageKey, String developerMessage, Object... args) {
        super(developerMessage);
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}