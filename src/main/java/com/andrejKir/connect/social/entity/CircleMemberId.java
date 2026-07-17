package com.andrejKir.connect.social.entity;

import jakarta .persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CircleMemberId implements Serializable {

    private UUID circleId;
    private UUID appUserId;

    public  CircleMemberId(UUID circleId, UUID appUserId) {
        this.circleId = circleId;
        this.appUserId = appUserId;

    }

    protected CircleMemberId() {

    }

    public UUID getCircleId()  { return circleId; }
    public UUID getAppUserId() { return appUserId; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CircleMemberId that)) return false;
        return Objects.equals(circleId, that.circleId)
                && Objects.equals(appUserId, that.appUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(circleId, appUserId);
    }
}
