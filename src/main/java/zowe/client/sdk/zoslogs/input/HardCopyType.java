/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 */
package zowe.client.sdk.zoslogs.input;

/**
 * Enum class representing z/OS Log type to gather log data from.
 *
 * @author Frank Giordano
 * @version 2.0
 */
public enum HardCopyType {

    /**
     * OPERLOG type
     */
    OPERLOG("operlog"),
    /**
     * SYSLOG type
     */
    SYSLOG("syslog");

    private final String value;

    HardCopyType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}