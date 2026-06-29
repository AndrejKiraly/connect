package com.andrejKir.connect.accounts.enums;

import java.util.Locale;

public enum SupportedLanguage {
    EN(Locale.ENGLISH),
    SK(Locale.of("sk")),
    DE(Locale.GERMAN);


    public final Locale locale;
    SupportedLanguage(Locale locale) {
        this.locale = locale;
    }
    public Locale locale(){return locale;}
}
