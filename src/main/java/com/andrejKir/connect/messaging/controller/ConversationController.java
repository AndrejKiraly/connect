package com.andrejKir.connect.messaging.controller;

import com.andrejKir.connect.accounts.security.SecurityUser;
import com.andrejKir.connect.messaging.dto.request.CreateConversationRequest;
import com.andrejKir.connect.messaging.dto.request.MarkReadRequest;
import com.andrejKir.connect.messaging.dto.response.ConversationDetailResponse;
import com.andrejKir.connect.messaging.dto.response.ConversationPageResponse;
import com.andrejKir.connect.messaging.service.ConversationService;
import com.andrejKir.connect.messaging.service.ConversationService.OpenedConversation;
import com.andrejKir.connect.shared.web.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.V1 + "/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ConversationPageResponse listConversations(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(required = false) UUID cursor,
            @AuthenticationPrincipal SecurityUser principal) {
        return conversationService.listConversations(principal.getId(), q, unread, cursor);
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse getConversation(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal SecurityUser principal) {
        return conversationService.getConversation(conversationId, principal.getId());
    }

    @PostMapping
    public ResponseEntity<ConversationDetailResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest conversationRequest,
            @AuthenticationPrincipal SecurityUser principal) {

        OpenedConversation opened =
                conversationService.findOrCreateDirect(principal.getId(), conversationRequest.counterpartId());

        return ResponseEntity.status(opened.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(opened.conversation());
    }

    @PostMapping("/{conversationId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MarkReadRequest markReadRequest,
            @AuthenticationPrincipal SecurityUser principal) {

        conversationService.markRead(conversationId, principal.getId(), markReadRequest.lastReadMessageId());
        return ResponseEntity.noContent().build();
    }
}
