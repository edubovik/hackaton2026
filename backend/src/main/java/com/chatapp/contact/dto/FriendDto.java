package com.chatapp.contact.dto;

public record FriendDto(
        Long userId,
        String username,
        String presence
) {}
