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

import zowe.client.sdk.core.ZOSConnection;
import zowe.client.sdk.rest.type.ZoweRequestType;

/**
 * Zowe request factory that generates the desire CRUD operation
 *
 * @author Frank Giordano
 * @version 2.0
 */
public final class ZoweRequestFactory {

    /**
     * Private constructor defined to avoid instantiation of class
     */
    private ZoweRequestFactory() {
        throw new IllegalStateException("Factory class");
    }

    /**
     * Assign the request to the Http verb type request object
     *
     * @param connection connection information, see ZOSConnection object
     * @param type       request http type, see ZoweRequestType object
     * @return ZoweRequest value
     * @throws Exception error with type not found
     * @author Frank Giordano
     */
    public static ZoweRequest buildRequest(ZOSConnection connection, ZoweRequestType type) throws Exception {
        ZoweRequest request;
        switch (type) {
            case GET_JSON:
                request = new JsonGetRequest(connection);
                break;
            case PUT_JSON:
                request = new JsonPutRequest(connection);
                break;
            case POST_JSON:
                request = new JsonPostRequest(connection);
                break;
            case DELETE_JSON:
                request = new JsonDeleteRequest(connection);
                break;
            case GET_TEXT:
                request = new TextGetRequest(connection);
                break;
            case PUT_TEXT:
                request = new TextPutRequest(connection);
                break;
            case GET_STREAM:
                request = new StreamGetRequest(connection);
                break;
            default:
                throw new Exception("no valid type specified");
        }
        return request;
    }

}
