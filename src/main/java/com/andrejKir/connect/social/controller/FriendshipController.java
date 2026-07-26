package com.andrejKir.connect.social.controller;


import com.andrejKir.connect.accounts.security.SecurityUser;
import com.andrejKir.connect.shared.web.ApiPaths;
import com.andrejKir.connect.social.dto.FriendshipRequest;
import com.andrejKir.connect.social.dto.FriendshipResponse;
import com.andrejKir.connect.social.enums.RequestDirection;
import com.andrejKir.connect.social.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.V1 + "/friendship")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController (FriendshipService friendshipService){
        this.friendshipService = friendshipService;
    }


    @PostMapping
    public ResponseEntity<FriendshipResponse> createFriendshipRequest(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @RequestBody FriendshipRequest friendshipRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(friendshipService.createFriendshipRequest(principal.getId(),friendshipRequest));
    }

    @PostMapping(path = "{friendshipRequestId}/accept")
    public ResponseEntity<FriendshipResponse> confirmFriendshipRequest(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID friendshipRequestId){
        return ResponseEntity.status(HttpStatus.OK).body(friendshipService.confirmFriendshipRequest(principal.getId(),friendshipRequestId));
    }

    @PostMapping(path = "{friendshipRequestId}/decline")
    public ResponseEntity<FriendshipResponse> declineFriendshipRequest(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID friendshipRequestId){
        return ResponseEntity.status(HttpStatus.OK).body(friendshipService.declineFriendshipRequest(principal.getId(),friendshipRequestId));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendshipResponse>> listRequests(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam String direction){
        return ResponseEntity.ok(friendshipService.listRequests(principal.getId(), RequestDirection.from(direction)));
    }

    @GetMapping("/friends")
    public ResponseEntity<List<FriendshipResponse>> listFriends(
            @AuthenticationPrincipal SecurityUser principal){
        return ResponseEntity.ok(friendshipService.listFriends(principal.getId()));
    }


}
