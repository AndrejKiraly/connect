package com.andrejKir.connect.messaging.dto.response;


import com.andrejKir.connect.messaging.enums.MessageReactionType;

import java.util.List;
import java.util.UUID;

public record MessageReactionResponse(
        MessageReactionType reactionType,
        List<UUID> userIds
) {
}
