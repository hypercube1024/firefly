package com.firefly.codec.http2.model.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * The Authentication state of a request.
 * <p>
 * The Authentication state can be one of several sub-types that
 * reflects where the request is in the many different authentication
 * cycles. Authentication might not yet be checked or it might be checked
 * and failed, checked and deferred or succeeded.
 */
public interface Authentication {
    /* ------------------------------------------------------------ */
    class Failed extends QuietServletException {
        public Failed(String message) {
            super(message);
        }
    }

    /* ------------------------------------------------------------ */

    /**
     * A successful Authentication with User information.
     */
    interface User extends Authentication {
        String getAuthMethod();

        UserIdentity getUserIdentity();

        boolean isUserInRole(UserIdentity.Scope scope, String role);

        void logout();
    }

    /* ------------------------------------------------------------ */

    /**
     * A wrapped authentication with methods provide the
     * wrapped request/response for use by the application
     */
    interface Wrapped extends Authentication {
        HttpServletRequest getHttpServletRequest();

        HttpServletResponse getHttpServletResponse();
    }

    /* ------------------------------------------------------------ */

    /**
     * A deferred authentication with methods to progress
     * the authentication process.
     */
    interface Deferred extends Authentication {
        /* ------------------------------------------------------------ */

        /**
         * Authenticate if possible without sending a challenge.
         * This is used to check credentials that have been sent for
         * non-manditory authentication.
         *
         * @param request the request
         * @return The new Authentication state.
         */
        Authentication authenticate(ServletRequest request);

        /* ------------------------------------------------------------ */

        /**
         * Authenticate and possibly send a challenge.
         * This is used to initiate authentication for previously
         * non-manditory authentication.
         *
         * @param request  the request
         * @param response the response
         * @return The new Authentication state.
         */
        Authentication authenticate(ServletRequest request, ServletResponse response);


        /* ------------------------------------------------------------ */

        /**
         * Login with the LOGIN authenticator
         *
         * @param username the username
         * @param password the password
         * @param request  the request
         * @return The new Authentication state
         */
        Authentication login(String username, Object password, ServletRequest request);
    }


    /* ------------------------------------------------------------ */

    /**
     * Authentication Response sent state.
     * Responses are sent by authenticators either to issue an
     * authentication challenge or on successful authentication in
     * order to redirect the user to the original URL.
     */
    interface ResponseSent extends Authentication {
    }

    /* ------------------------------------------------------------ */

    /**
     * An Authentication Challenge has been sent.
     */
    interface Challenge extends ResponseSent {
    }

    /* ------------------------------------------------------------ */

    /**
     * An Authentication Failure has been sent.
     */
    interface Failure extends ResponseSent {
    }

    interface SendSuccess extends ResponseSent {
    }

    /* ------------------------------------------------------------ */
    /**
     * Unauthenticated state.
     * <p>
     * This convenience instance is for non mandatory authentication where credentials
     * have been presented and checked, but failed authentication.
     */
    Authentication UNAUTHENTICATED = new Authentication() {
        @Override
        public String toString() {
            return "UNAUTHENTICATED";
        }
    };

    /* ------------------------------------------------------------ */
    /**
     * Authentication not checked
     * <p>
     * This convenience instance us for non mandatory authentication when no
     * credentials are present to be checked.
     */
    Authentication NOT_CHECKED = new Authentication() {
        @Override
        public String toString() {
            return "NOT CHECKED";
        }
    };

    /* ------------------------------------------------------------ */
    /**
     * Authentication challenge sent.
     * <p>
     * This convenience instance is for when an authentication challenge has been sent.
     */
    Authentication SEND_CONTINUE = new Authentication.Challenge() {
        @Override
        public String toString() {
            return "CHALLENGE";
        }
    };

    /* ------------------------------------------------------------ */
    /**
     * Authentication failure sent.
     * <p>
     * This convenience instance is for when an authentication failure has been sent.
     */
    Authentication SEND_FAILURE = new Authentication.Failure() {
        @Override
        public String toString() {
            return "FAILURE";
        }
    };
    Authentication SEND_SUCCESS = new SendSuccess() {
        @Override
        public String toString() {
            return "SEND_SUCCESS";
        }
    };
}
