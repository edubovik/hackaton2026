package com.chatapp.message.dto;

public record UnreadCountDto(Long roomId, Long partnerId, int count) {}
