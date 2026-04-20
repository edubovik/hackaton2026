package com.chatapp.message.dto;

public record SendMessageRequest(String content, Long replyToId) {}
