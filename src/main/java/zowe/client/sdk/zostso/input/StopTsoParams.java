/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package zowe.client.sdk.zostso.input;

import java.util.Optional;

/**
 * TSO stop command z/OSMF parameters
 *
 * @author Frank Giordano
 * @version 2.0
 */
public class StopTsoParams {

    /**
     * Servlet key of an active address space
     */
    private final Optional<String> servletKey;

    /**
     * SendTsoParams constructor
     *
     * @param servletKey key of an active tso address space
     * @author Frank Giordano
     */
    public StopTsoParams(String servletKey) {
        this.servletKey = Optional.ofNullable(servletKey);
    }

    /**
     * Retrieve servletKey specified
     *
     * @return servletKey key value of an active address space
     * @author Frank Giordano
     */
    public Optional<String> getServletKey() {
        return servletKey;
    }

    @Override
    public String toString() {
        return "StopTsoParams{" +
                "servletKey=" + servletKey +
                '}';
    }

}
