package com.firefly.codec.oauth2.validator;

import com.firefly.codec.oauth2.exception.OAuthProblemException;

public interface OAuthValidator<T> {

    void validateMethod(T ctx) throws OAuthProblemException;

    void validateContentType(T ctx) throws OAuthProblemException;

    void validateRequiredParameters(T ctx) throws OAuthProblemException;

    void validateOptionalParameters(T ctx) throws OAuthProblemException;

    void validateNotAllowedParameters(T ctx) throws OAuthProblemException;

    void validateClientAuthenticationCredentials(T ctx) throws OAuthProblemException;

    void performAllValidations(T ctx) throws OAuthProblemException;

}
