/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package zowe.client.sdk.rest;

import java.util.Optional;

/**
 * Holds http response information
 *
 * @author Frank Giordano
 * @version 2.0
 */
public class Response {

    /**
     * Holds http response information
     */
    private final Optional<Object> responsePhrase;

    /**
     * Holds http response status code
     */
    private final Optional<Integer> statusCode;

    /**
     * Holds http response status text
     */
    private final Optional<String> statusText;

    /**
     * Response constructor
     *
     * @param responsePhrase http response information
     * @param statusCode     http response status code
     * @param statusText     http response status text
     * @author Frank Giordano
     */
    public Response(Object responsePhrase, Integer statusCode, String statusText) {
        this.responsePhrase = Optional.ofNullable(responsePhrase);
        this.statusCode = Optional.ofNullable(statusCode);
        this.statusText = Optional.ofNullable(statusText);
    }

    /**
     * Retrieve responsePhrase value
     *
     * @return responsePhrase value
     * @author Frank Giordano
     */
    public Optional<Object> getResponsePhrase() {
        return responsePhrase;
    }

    /**
     * Retrieve statusCode value
     *
     * @return statusCode value
     * @author Frank Giordano
     */
    public Optional<Integer> getStatusCode() {
        return statusCode;
    }

    /**
     * Retrieve statusText value
     *
     * @return statusText value
     * @author Frank Giordano
     */
    public Optional<String> getStatusText() {
        return statusText;
    }

    @Override
    public String toString() {
        return "Response{" +
                "responsePhrase=" + responsePhrase +
                ", statusCode=" + statusCode +
                ", statusText=" + statusText +
                '}';
    }

}
