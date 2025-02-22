/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package zowe.client.sdk.zosfiles.dsn.methods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zowe.client.sdk.core.ZOSConnection;
import zowe.client.sdk.rest.JsonDeleteRequest;
import zowe.client.sdk.rest.Response;
import zowe.client.sdk.rest.ZoweRequest;
import zowe.client.sdk.rest.ZoweRequestFactory;
import zowe.client.sdk.rest.type.ZoweRequestType;
import zowe.client.sdk.utility.EncodeUtils;
import zowe.client.sdk.utility.RestUtils;
import zowe.client.sdk.utility.ValidateUtils;
import zowe.client.sdk.utility.unirest.UniRestUtils;
import zowe.client.sdk.zosfiles.ZosFilesConstants;

/**
 * Provides delete dataset and member functionality
 *
 * @author Leonid Baranov
 * @author Frank Giordano
 * @version 2.0
 */
public class DsnDelete {

    private static final Logger LOG = LoggerFactory.getLogger(DsnDelete.class);
    private final ZOSConnection connection;
    private ZoweRequest request;

    /**
     * DsnDelete Constructor
     *
     * @param connection connection information, see ZOSConnection object
     * @author Leonid Baranov
     */
    public DsnDelete(ZOSConnection connection) {
        ValidateUtils.checkConnection(connection);
        this.connection = connection;
    }

    /**
     * Alternative DsnDelete constructor with ZoweRequest object. This is mainly used for internal code unit testing
     * with mockito, and it is not recommended to be used by the larger community.
     *
     * @param connection connection information, see ZOSConnection object
     * @param request    any compatible ZoweRequest Interface type object
     * @author Frank Giordano
     */
    public DsnDelete(ZOSConnection connection, ZoweRequest request) {
        ValidateUtils.checkConnection(connection);
        this.connection = connection;
        this.request = request;
    }

    /**
     * Delete a dataset member
     *
     * @param dataSetName name of a dataset (e.g. 'DATASET.LIB')
     * @param memberName  name of member to delete
     * @return http response object
     * @throws Exception error processing request
     * @author Frank Giordano
     */
    public Response delete(String dataSetName, String memberName) throws Exception {
        ValidateUtils.checkNullParameter(dataSetName == null, "dataSetName is null");
        ValidateUtils.checkIllegalParameter(dataSetName.isEmpty(), "dataSetName not specified");
        ValidateUtils.checkNullParameter(memberName == null, "memberName is null");
        ValidateUtils.checkIllegalParameter(memberName.isEmpty(), "memberName not specified");
        return delete(String.format("%s(%s)", dataSetName, memberName));
    }

    /**
     * Delete a dataset
     *
     * @param dataSetName name of a dataset (e.g. 'DATASET.LIB')
     * @return http response object
     * @throws Exception error processing request
     * @author Leonid Baranov
     */
    public Response delete(String dataSetName) throws Exception {
        ValidateUtils.checkNullParameter(dataSetName == null, "dataSetName is null");
        ValidateUtils.checkIllegalParameter(dataSetName.isEmpty(), "dataSetName not specified");

        final String url = "https://" + connection.getHost() + ":" + connection.getZosmfPort() + ZosFilesConstants.RESOURCE +
                ZosFilesConstants.RES_DS_FILES + "/" + EncodeUtils.encodeURIComponent(dataSetName);

        LOG.debug(url);

        if (request == null || !(request instanceof JsonDeleteRequest)) {
            request = ZoweRequestFactory.buildRequest(connection, ZoweRequestType.DELETE_JSON);
        }
        request.setUrl(url);

        final Response response = UniRestUtils.getResponse(request);
        if (RestUtils.isHttpError(response.getStatusCode().get())) {
            throw new Exception(response.getResponsePhrase().get().toString());
        }

        return response;
    }

}
