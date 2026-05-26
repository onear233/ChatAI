package com.onear.chatai.web.dto;

public record SkillUploadRequest(
    String name,
    String description,
    String version
) {}
