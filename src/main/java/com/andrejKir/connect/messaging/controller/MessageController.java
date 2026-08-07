package com.andrejKir.connect.messaging.controller;

import com.andrejKir.connect.accounts.security.SecurityUser;
import com.andrejKir.connect.messaging.dto.request.MessageRequest;
import com.andrejKir.connect.messaging.dto.response.MessagePageResponse;
import com.andrejKir.connect.messaging.dto.response.MessageResponse;
import com.andrejKir.connect.messaging.service.MessageService;
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
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MessageRequest messageRequest,
            @AuthenticationPrincipal SecurityUser principal) {

        MessageResponse response = messageService.createMessage(conversationId, principal.getId(), messageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{conversationId}/messages")
    public MessagePageResponse listMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID beforeId,
            @AuthenticationPrincipal SecurityUser principal) {

        return messageService.listMessages(conversationId, principal.getId(), beforeId);
    }
}