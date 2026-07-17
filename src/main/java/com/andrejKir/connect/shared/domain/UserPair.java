package com.andrejKir.connect.shared.domain;

import java.util.UUID;

public record UserPair(UUID low, UUID high) {

    public static UserPair of(UUID a, UUID b){
        return a.compareTo(b) < 0 ? new UserPair(a,b) : new UserPair(b,a);
    }
}
