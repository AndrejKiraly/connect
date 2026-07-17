package com.andrejKir.connect.social.controller;


import com.andrejKir.connect.shared.web.ApiPaths;
import com.andrejKir.connect.social.dto.FriendshipRequest;
import com.andrejKir.connect.social.dto.FriendshipResponse;
import com.andrejKir.connect.social.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.V1 + "/friendship")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController (FriendshipService friendshipService){
        this.friendshipService = friendshipService;
    }


    @PostMapping
    public ResponseEntity<FriendshipResponse> createFriendshipRequest(@Valid @RequestBody FriendshipRequest friendshipRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(friendshipService.createFriendshipRequest(friendshipRequest));
    }

    @PostMapping(path = "{friendshipRequestId}/accept")
    public ResponseEntity<FriendshipResponse> confirmFriendshipRequest(@PathVariable UUID friendshipRequestId){
        return ResponseEntity.status(HttpStatus.OK).body(friendshipService.confirmFriendshipRequest(friendshipRequestId));
    }


}
