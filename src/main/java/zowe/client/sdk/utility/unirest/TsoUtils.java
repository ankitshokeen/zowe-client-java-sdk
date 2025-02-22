/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package zowe.client.sdk.utility.unirest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import zowe.client.sdk.rest.Response;
import zowe.client.sdk.utility.RestUtils;
import zowe.client.sdk.utility.ValidateUtils;
import zowe.client.sdk.zostso.TsoConstants;
import zowe.client.sdk.zostso.message.*;
import zowe.client.sdk.zostso.response.StartStopResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility Class for Tso command related static helper methods.
 *
 * @author Frank Giordano
 * @version 2.0
 */
public final class TsoUtils {

    /**
     * Private constructor defined to avoid instantiation of class
     */
    private TsoUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieve Tso response
     *
     * @param response Response object
     * @return ZosmfTsoResponse object
     * @throws Exception error processing response
     * @author Frank Giordano
     */
    public static ZosmfTsoResponse getZosmfTsoResponse(Response response) throws Exception {
        ValidateUtils.checkNullParameter(response == null, "response is null");
        ZosmfTsoResponse result;
        final int statusCode = response.getStatusCode().get();
        if (response.getStatusCode().isPresent() && RestUtils.isHttpError(statusCode)) {
            final ZosmfMessages zosmfMsg = new ZosmfMessages(
                    (String) response.getResponsePhrase().orElseThrow(() -> new Exception("no response phrase")),
                    null, null);
            final List<ZosmfMessages> zosmfMessages = new ArrayList<>();
            zosmfMessages.add(zosmfMsg);
            result = new ZosmfTsoResponse.Builder().msgData(zosmfMessages).build();
        } else {
            result = TsoUtils.parseJsonTsoResponse(
                    (JSONObject) new JSONParser().parse(response.getResponsePhrase()
                            .orElseThrow(() -> new Exception("no response phrase")).toString()));
        }

        return result;
    }

    /*
    following json parsing is being constructed to conform to the following format:
    https://www.ibm.com/docs/en/zos/2.1.0?topic=services-tsoe-address-space
    */

    /**
     * Retrieve parsed Json Tso Stop Response
     *
     * @param obj JSONObject object
     * @return populated console response, see ZosmfTsoResponse object
     * @author Frank Giordano
     */
    public static ZosmfTsoResponse parseJsonStopResponse(JSONObject obj) {
        ValidateUtils.checkNullParameter(obj == null, "no obj to parse");
        return new ZosmfTsoResponse.Builder().ver((String) obj.get("ver")).servletKey((String) obj.get("servletKey"))
                .reused((boolean) obj.get("reused")).timeout((boolean) obj.get("timeout")).build();
    }

    @SuppressWarnings("unchecked")
    private static void parseJsonTsoMessage(List<TsoMessages> tsoMessagesLst, JSONObject obj, TsoMessages tsoMessages) {
        final Map<String, String> tsoMessageMap = ((Map<String, String>) obj.get(TsoConstants.TSO_MESSAGE));
        if (tsoMessageMap != null) {
            final TsoMessage tsoMessage = new TsoMessage();
            tsoMessageMap.forEach((key, value) -> {
                if ("DATA".equals(key)) {
                    tsoMessage.setData(value);
                }
                if ("VERSION".equals(key)) {
                    tsoMessage.setVersion(value);
                }
            });
            tsoMessages.setTsoMessage(tsoMessage);
            tsoMessagesLst.add(tsoMessages);
        }
    }

    @SuppressWarnings("unchecked")
    private static void parseJsonTsoPrompt(List<TsoMessages> tsoMessagesLst, JSONObject obj, TsoMessages tsoMessages) {
        final Map<String, String> tsoPromptMap = ((Map<String, String>) obj.get(TsoConstants.TSO_PROMPT));
        if (tsoPromptMap != null) {
            TsoPromptMessage tsoPromptMessage = new TsoPromptMessage();
            tsoPromptMap.forEach((key, value) -> {
                if ("VERSION".equals(key)) {
                    tsoPromptMessage.setVersion(value);
                }
                if ("HIDDEN".equals(key)) {
                    tsoPromptMessage.setHidden(value);
                }
            });
            tsoMessages.setTsoPrompt(tsoPromptMessage);
            tsoMessagesLst.add(tsoMessages);
        }
    }

    @SuppressWarnings("unchecked")
    private static ZosmfTsoResponse parseJsonTsoResponse(JSONObject result) throws Exception {
        ValidateUtils.checkNullParameter(result == null, "no results to parse");
        ZosmfTsoResponse response;
        try {
            response = new ZosmfTsoResponse.Builder().queueId((String) result.get("queueID"))
                    .ver((String) result.get("ver")).servletKey((String) result.get("servletKey"))
                    .reused((boolean) result.get("reused")).timeout((boolean) result.get("timeout")).build();
        } catch (Exception e) {
            throw new Exception("missing one of the following json field values: queueID, ver, servletKey, " +
                    "reused and timeout");
        }

        List<TsoMessages> tsoMessagesLst = new ArrayList<>();
        final Optional<JSONArray> tsoData = Optional.ofNullable((JSONArray) result.get("tsoData"));

        tsoData.ifPresent(data -> {
            data.forEach(item -> {
                final JSONObject obj = (JSONObject) item;
                final TsoMessages tsoMessages = new TsoMessages();
                parseJsonTsoMessage(tsoMessagesLst, obj, tsoMessages);
                parseJsonTsoPrompt(tsoMessagesLst, obj, tsoMessages);
            });
            response.setTsoData(tsoMessagesLst);
        });

        return response;
    }

    /**
     * Populate either a Tso start or stop command phase
     *
     * @param zosmfResponse zosmf response info, see zosmfResponse
     * @return StartStopResponse object
     * @author Frank Giordano
     */
    public static StartStopResponse populateStartAndStop(ZosmfTsoResponse zosmfResponse) {
        ValidateUtils.checkNullParameter(zosmfResponse == null, "zosmfResponse is null");
        final StartStopResponse startStopResponse = new StartStopResponse(false, zosmfResponse,
                zosmfResponse.getServletKey().orElse(""));

        startStopResponse.setSuccess(zosmfResponse.getServletKey().isPresent());
        if (!zosmfResponse.getMsgData().isEmpty()) {
            final ZosmfMessages zosmfMsg = zosmfResponse.getMsgData().get(0);
            final String msgText = zosmfMsg.getMessageText().orElse(TsoConstants.ZOSMF_UNKNOWN_ERROR);
            startStopResponse.setFailureResponse(msgText);
        }

        return startStopResponse;
    }

}
