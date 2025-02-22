/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package zowe.client.sdk.utility;

import zowe.client.sdk.core.ZOSConnection;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility Class contains helper methods for encoding processing
 *
 * @author Frank Giordano
 * @version 2.0
 */
public final class EncodeUtils {

    /**
     * Private constructor defined to avoid instantiation of class
     */
    private EncodeUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Encodes the passed String as UTF-8 using an algorithm that's compatible
     * with JavaScript's encodeURIComponent function. Returns incoming string un-encoded if exception occurs.
     *
     * @param str string to be encoded
     * @return encoded String or original string
     * @author Frank Giordano
     */
    public static String encodeURIComponent(String str) {
        ValidateUtils.checkNullParameter(str == null, "str is null");
        ValidateUtils.checkIllegalParameter(str.isEmpty(), "str not specified");
        return URLEncoder.encode(str, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20")
                .replaceAll("\\%21", "!")
                .replaceAll("\\%27", "'")
                .replaceAll("\\%28", "(")
                .replaceAll("\\%29", ")")
                .replaceAll("\\%7E", "~");
    }

    /**
     * Encodes the passed connection String as UTF-8 for usage of the AUTHORIZATION http header.
     *
     * @param connection connection information, see ZOSConnection object
     * @return encoded String
     * @author Frank Giordano
     */
    public static String getAuthEncoding(ZOSConnection connection) {
        ValidateUtils.checkConnection(connection);
        return Base64.getEncoder().encodeToString((connection.getUser() + ":" + connection.getPassword())
                .getBytes(StandardCharsets.UTF_8));
    }

}
