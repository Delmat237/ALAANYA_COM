package com.alaanya.utils;


public class AuthenticationResult {
    private final boolean authenticated;
    private final String errorMessage;

    public AuthenticationResult(boolean authenticated, String errorMessage) {
        this.authenticated = authenticated;
        this.errorMessage = errorMessage;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}

