package com.chatapp.message.dto;

import java.util.List;

public record MessagePage(List<MessageDto> messages, boolean hasMore) {}
