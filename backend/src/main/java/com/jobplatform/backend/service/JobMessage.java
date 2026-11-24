package com.jobplatform.backend.service;

public record JobMessage(long jobId, int run, int attempt) {}
