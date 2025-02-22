/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package zowe.client.sdk.teamconfig.types;

/**
 * SectionType class provides type representation of Zowe Global Team Configuration sections.
 *
 * @author Frank Giordano
 * @version 2.0
 */
public enum SectionType {

    $SCHEMA("$schema"),
    PROFILES("profiles"),
    DEFAULTS("defaults"),
    AUTOSTORE("autoStore");

    private final String value;

    SectionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}


