package com.andrejKir.connect.messaging.controller;

import com.andrejKir.connect.accounts.security.SecurityUser;
import com.andrejKir.connect.messaging.dto.response.ConversationDetailResponse;
import com.andrejKir.connect.messaging.dto.response.ConversationSummaryResponse;
import com.andrejKir.connect.messaging.service.ConversationService;
import com.andrejKir.connect.shared.web.ApiPaths;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.V1 + "/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<ConversationSummaryResponse> listConversations(
            @AuthenticationPrincipal SecurityUser principal){
        return conversationService.listConversations(principal.getId());
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse getConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal SecurityUser principal) {
        return conversationService.getConversation(conversationId, principal.getId());
    }
}