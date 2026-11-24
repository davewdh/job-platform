package com.jobplatform.backend.model;

public enum JobScenario {
    SUCCESS,
    FAIL_ONCE,
    EXHAUST_RETRIES;

    public boolean succeeds(int attempt) {
        return switch (this) {
            case SUCCESS -> true;
            case FAIL_ONCE -> attempt > 1;
            case EXHAUST_RETRIES -> false;
        };
    }
}
