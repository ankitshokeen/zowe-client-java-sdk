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
package zowe.client.sdk.zosjobs.methods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zowe.client.sdk.core.ZOSConnection;
import zowe.client.sdk.utility.ValidateUtils;
import zowe.client.sdk.utility.timer.WaitUtil;
import zowe.client.sdk.zosjobs.input.CommonJobParams;
import zowe.client.sdk.zosjobs.input.GetJobParams;
import zowe.client.sdk.zosjobs.input.JobFile;
import zowe.client.sdk.zosjobs.input.MonitorJobWaitForParams;
import zowe.client.sdk.zosjobs.response.CheckJobStatus;
import zowe.client.sdk.zosjobs.response.Job;
import zowe.client.sdk.zosjobs.types.JobStatus;

import java.util.List;

/**
 * APIs for monitoring the status of a job. Use these APIs to wait for a job to enter the specified status. All APIs
 * in MonitorJobs invoke z/OSMF jobs REST endpoints to obtain job status information.
 *
 * @author Frank Giordano
 * @version 2.0
 */
public class JobMonitor {

    /**
     * Default number of poll attempts to check for the specified job status.
     */
    public static final int DEFAULT_ATTEMPTS = 1000;
    /**
     * The default amount of lines to check from job output.
     */
    public static final int DEFAULT_LINE_LIMIT = 1000;
    /**
     * Default expected job status ("OUTPUT")
     */
    public static final JobStatus.Type DEFAULT_STATUS = JobStatus.Type.OUTPUT;
    /**
     * The default amount of time (in 3000 milliseconds is 3 seconds) to wait until the next job status poll.
     */
    public static final int DEFAULT_WATCH_DELAY = 3000;
    private static final Logger LOG = LoggerFactory.getLogger(JobMonitor.class);
    private final ZOSConnection connection;
    // double settings from DEFAULTS variables to allow constructor to control them also
    private int attempts = DEFAULT_ATTEMPTS;
    private int watchDelay = DEFAULT_WATCH_DELAY;
    private int lineLimit = DEFAULT_LINE_LIMIT;

    /**
     * MonitorJobs constructor.
     *
     * @param connection connection information, see ZOSConnection object
     * @author Frank Giordano
     */
    public JobMonitor(ZOSConnection connection) {
        ValidateUtils.checkConnection(connection);
        this.connection = connection;
    }

    /**
     * MonitorJobs constructor.
     *
     * @param connection connection information, see ZOSConnection object
     * @param attempts   number of attempts to get status
     * @author Frank Giordano
     */
    public JobMonitor(ZOSConnection connection, int attempts) {
        ValidateUtils.checkConnection(connection);
        this.connection = connection;
        this.attempts = attempts;
    }

    /**
     * MonitorJobs constructor.
     *
     * @param connection connection information, see ZOSConnection object
     * @param attempts   number of attempts to get status
     * @param watchDelay delay time in milliseconds to wait each time requesting status
     * @author Frank Giordano
     */
    public JobMonitor(ZOSConnection connection, int attempts, int watchDelay) {
        ValidateUtils.checkConnection(connection);
        this.connection = connection;
        this.attempts = attempts;
        this.watchDelay = watchDelay;
    }

    /**
     * MonitorJobs constructor.
     *
     * @param connection connection information, see ZOSConnection object
     * @param attempts   number of attempts to get status
     * @param watchDelay delay time in milliseconds to wait each time requesting status
     * @param lineLimit  number of line to inspect job output
     * @author Frank Giordano
     */
    public JobMonitor(ZOSConnection connection, int attempts, int watchDelay, int lineLimit) {
        ValidateUtils.checkConnection(connection);
        this.connection = connection;
        this.attempts = attempts;
        this.watchDelay = watchDelay;
        this.lineLimit = lineLimit;
    }

    /**
     * Checks if the given message is within the job output within line limit.
     *
     * @param params  monitor jobs params, see MonitorJobWaitForParams
     * @param message message string
     * @return boolean message found status
     * @throws Exception error processing check request
     * @author Frank Giordano
     */
    private boolean checkMessage(MonitorJobWaitForParams params, String message) throws Exception {
        final JobGet getJobs = new JobGet(connection);
        final GetJobParams filter = new GetJobParams.Builder("*")
                .jobId(params.getJobId().orElseThrow(() -> new Exception("job id not specified")))
                .prefix(params.getJobName().orElseThrow(() -> new Exception("job name not specified"))).build();
        final List<Job> jobs = getJobs.getCommon(filter);
        if (jobs.isEmpty()) {
            throw new Exception("job does not exist");
        }
        final List<JobFile> files = getJobs.getSpoolFilesByJob(jobs.get(0));
        final String[] output = getJobs.getSpoolContent(files.get(0)).split("\n");

        final int lineLimit = params.getLineLimit().orElse(DEFAULT_LINE_LIMIT);
        final int size = output.length, start;

        if (size < lineLimit) {
            start = 0;
        } else {
            start = size - lineLimit;
        }

        for (int i = start; i < size; i++) {
            LOG.debug(output[i]);
            if (output[i].contains(message)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the status of the job for the expected status (OR that the job has progressed passed the expected status).
     *
     * @param params monitor jobs params, see MonitorJobWaitForParams
     * @return boolean true when the job status is obtained
     * @throws Exception error processing check request
     * @author Frank Giordano
     */
    private CheckJobStatus checkStatus(MonitorJobWaitForParams params) throws Exception {
        return checkStatus(params, false);
    }

    /**
     * Checks the status of the job for the expected status (OR that the job has progressed passed the expected status).
     *
     * @param params monitor jobs params, see MonitorJobWaitForParams
     * @return boolean true when the job status is obtained
     * @throws Exception error processing check request
     * @author Frank Giordano
     */
    private CheckJobStatus checkStatus(MonitorJobWaitForParams params, boolean getStepData) throws Exception {
        final JobGet getJobs = new JobGet(connection);
        final String statusNameCheck = params.getJobStatus().orElse(DEFAULT_STATUS).toString();

        final Job job = getJobs.getStatusCommon(
                new CommonJobParams(params.getJobId().orElseThrow(() -> new Exception("job id not specified")),
                        params.getJobName().orElseThrow(() -> new Exception("job name not specified")),
                        getStepData));

        if (statusNameCheck.equals(job.getStatus().orElse(DEFAULT_STATUS.toString()))) {
            return new CheckJobStatus(true, job);
        }

        final String invalidStatusMsg = "Invalid status when checking for status ordering.";
        final int orderIndexOfDesiredJobStatus = getOrderIndexOfStatus(statusNameCheck);
        if (orderIndexOfDesiredJobStatus == -1) { // this should never happen but let's check for it.
            throw new Exception(invalidStatusMsg);
        }

        final int orderIndexOfCurrRunningJobStatus =
                getOrderIndexOfStatus(job.getStatus().orElseThrow(() -> new Exception("job status not specified")));
        if (orderIndexOfCurrRunningJobStatus == -1) {  // this should never happen but let's check for it.
            throw new Exception(invalidStatusMsg);
        }

        if (orderIndexOfCurrRunningJobStatus > orderIndexOfDesiredJobStatus) {
            return new CheckJobStatus(true, job);
        }

        return new CheckJobStatus(false, job);
    }

    /**
     * Checks the status order of the given status name
     *
     * @param statusName status name
     * @return int index of status order or -1 if none found
     * @author Frank Giordano
     */
    private int getOrderIndexOfStatus(String statusName) {
        for (int i = 0; i < JobStatus.Order.length; i++) {
            if (statusName.equals(JobStatus.Order[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determines if a given job is in a running state or not.
     *
     * @param params monitor jobs params, see MonitorJobWaitForParams
     * @return true if in running state
     * @throws Exception error processing running status check
     * @author Frank Giordano
     */
    public boolean isRunning(MonitorJobWaitForParams params) throws Exception {
        ValidateUtils.checkNullParameter(params == null, "params is null");
        final JobGet getJobs = new JobGet(connection);
        final String jobName = params.getJobName().orElseThrow(() -> new Exception("job name not specified"));
        final String jobId = params.getJobId().orElseThrow(() -> new Exception("job id not specified"));
        final String status = getJobs.getStatusValue(jobName, jobId);
        return !JobStatus.Type.INPUT.toString().equals(status) && !JobStatus.Type.OUTPUT.toString().equals(status);
    }

    /**
     * "Polls" (sets timeouts and continuously checks) for the given message within the job output.
     *
     * @param params  monitor jobs params, see MonitorJobWaitForParams
     * @param message message string
     * @return boolean message found status
     * @throws Exception error processing poll check request
     * @author Frank Giordano
     */
    private boolean pollByMessage(MonitorJobWaitForParams params, String message) throws Exception {
        final int timeoutVal = params.getWatchDelay().orElse(DEFAULT_WATCH_DELAY);
        boolean messageFound;  // no assigment boolean means by default it is false
        boolean shouldContinue;
        int numOfAttempts = 0;
        final int maxAttempts = params.getAttempts().orElse(DEFAULT_ATTEMPTS);

        LOG.info("Waiting for message \"{}\"", message);

        do {
            numOfAttempts++;

            messageFound = checkMessage(params, message);

            shouldContinue = !messageFound && (maxAttempts > 0 && numOfAttempts < maxAttempts);

            if (shouldContinue) {
                WaitUtil.wait(timeoutVal);
                if (!isRunning(params)) {
                    return false;
                }
                LOG.info("Waiting for message \"{}\"", message);
            }
        } while (shouldContinue);

        return numOfAttempts != maxAttempts;
    }

    /**
     * "Polls" (sets timeouts and continuously checks) for the status of the job to match the desired status.
     *
     * @param params monitor jobs params, see MonitorJobWaitForParams
     * @return job document
     * @throws Exception error processing poll check request
     * @author Frank Giordano
     */
    private Job pollByStatus(MonitorJobWaitForParams params) throws Exception {
        final int timeoutVal = params.getWatchDelay().orElse(DEFAULT_WATCH_DELAY);
        boolean expectedStatus;  // no assigment boolean means by default it is false
        boolean shouldContinue;
        int numOfAttempts = 0;
        final int maxAttempts = params.getAttempts().orElse(DEFAULT_ATTEMPTS);

        String statusName = params.getJobStatus().orElse(DEFAULT_STATUS).toString();
        LOG.info("Waiting for status \"{}\"", statusName);

        CheckJobStatus checkJobStatus;
        do {
            numOfAttempts++;

            checkJobStatus = checkStatus(params);
            expectedStatus = checkJobStatus.isStatusFound();

            shouldContinue = !expectedStatus && (maxAttempts > 0 && numOfAttempts < maxAttempts);

            if (shouldContinue) {
                WaitUtil.wait(timeoutVal);
                LOG.info("Waiting for status \"{}\"", statusName);
            } else {
                // Get the stepData
                try {
                    checkJobStatus = checkStatus(params, true);
                } catch (Exception ignore) {
                    // JCL error, return without stepData
                }
            }
        } while (shouldContinue);

        if (numOfAttempts == maxAttempts) {
            throw new Exception("Desired status not seen. The number of maximum attempts reached.");
        }

        return checkJobStatus.getJob();
    }

    /**
     * Given a Job document (has jobname/jobid), waits for the given message from the job. This API will poll for
     * the given message once every 3 seconds for at least 1000 times. If the polling interval/duration is NOT
     * sufficient, use "waitForMessageCommon" method to adjust.
     * <p>
     * See JavaDoc for "waitForMessageCommon" for full details on polling and other logic.
     *
     * @param job     document of the z/OS job to wait for (see z/OSMF Jobs APIs for details)
     * @param message message string
     * @return job document
     * @throws Exception error processing wait check request
     * @author Frank Giordano
     */
    public boolean waitByMessage(Job job, String message) throws Exception {
        ValidateUtils.checkNullParameter(job == null, "job is null");
        ValidateUtils.checkIllegalParameter(job.getJobName().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(job.getJobName().get().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(job.getJobId().isEmpty(), "job id not specified");
        ValidateUtils.checkIllegalParameter(job.getJobId().get().isEmpty(), "job id not specified");
        return waitMessageCommon(new MonitorJobWaitForParams.Builder(job.getJobName().get(), job.getJobId().get())
                .jobStatus(JobStatus.Type.OUTPUT).attempts(attempts).watchDelay(watchDelay).build(), message);
    }

    /**
     * Given the jobname/jobid, waits for the given message from the job. This API will poll for
     * the given message once every 3 seconds for at least 1000 times. If the polling interval/duration is NOT
     * sufficient, use "waitForMessageCommon" method to adjust.
     * <p>
     * See JavaDoc for "waitForMessageCommon" for full details on polling and other logic.
     *
     * @param jobName the z/OS jobname of the job to wait for output status (see z/OSMF Jobs APIs for details)
     * @param jobId   the z/OS jobid of the job to wait for output status (see z/OSMF Jobs APIS for details)
     * @param message message string
     * @return job document
     * @throws Exception error processing wait check request
     * @author Frank Giordano
     */
    public boolean waitByMessage(String jobName, String jobId, String message) throws Exception {
        return waitMessageCommon(new MonitorJobWaitForParams.Builder(jobName, jobId).jobStatus(JobStatus.Type.OUTPUT)
                .attempts(attempts).watchDelay(watchDelay).build(), message);
    }

    /**
     * Given a Job document (has jobname/jobid), waits for the status of the job to be "OUTPUT". This API will poll for
     * the OUTPUT status once every 3 seconds indefinitely. If the polling interval/duration is NOT sufficient, use
     * "waitForStatusCommon" to adjust.
     * <p>
     * See JSDoc for "waitForStatusCommon" for full details on polling and other logic.
     *
     * @param job document of the z/OS job to wait for (see z/OSMF Jobs APIs for details)
     * @return job document
     * @throws Exception error processing wait check request
     * @author Frank Giordano
     */
    public Job waitByOutputStatus(Job job) throws Exception {
        ValidateUtils.checkNullParameter(job == null, "job is null");
        ValidateUtils.checkIllegalParameter(job.getJobName().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(job.getJobName().get().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(job.getJobId().isEmpty(), "job id not specified");
        ValidateUtils.checkIllegalParameter(job.getJobId().get().isEmpty(), "job id not specified");
        return waitStatusCommon(new MonitorJobWaitForParams.Builder(job.getJobName().get(), job.getJobId().get())
                .jobStatus(JobStatus.Type.OUTPUT).attempts(attempts).watchDelay(watchDelay).build());
    }

    /**
     * Given the jobname/jobid, waits for the status of the job to be "OUTPUT". This API will poll for the OUTPUT status
     * once every 3 seconds indefinitely. If the polling interval/duration is NOT sufficient, use
     * "waitForStatusCommon" to adjust.
     * <p>
     * See JavaDoc for "waitForStatusCommon" for full details on polling and other logic.
     *
     * @param jobName the z/OS jobname of the job to wait for output status (see z/OSMF Jobs APIs for details)
     * @param jobId   the z/OS jobid of the job to wait for output status (see z/OSMF Jobs APIS for details)
     * @return job document
     * @throws Exception error processing wait check request
     * @author Frank Giordano
     */
    public Job waitByOutputStatus(String jobName, String jobId) throws Exception {
        return waitStatusCommon(new MonitorJobWaitForParams.Builder(jobName, jobId).jobStatus(JobStatus.Type.OUTPUT).
                attempts(attempts).watchDelay(watchDelay).build());
    }

    /**
     * Given a Job document (has jobname/jobid), waits for the given status of the job. This API will poll for
     * the given status once every 3 seconds for at least 1000 times. If the polling interval/duration is NOT
     * sufficient, use "waitForStatusCommon" method to adjust.
     * <p>
     * See JavaDoc for "waitForStatusCommon" for full details on polling and other logic.
     *
     * @param job        document of the z/OS job to wait for (see z/OSMF Jobs APIs for details)
     * @param statusType status type, see JobStatus.Type object
     * @return job document
     * @throws Exception error processing wait check request
     * @author Frank Giordano
     */
    public Job waitByStatus(Job job, JobStatus.Type statusType) throws Exception {
        ValidateUtils.checkNullParameter(job == null, "job is null");
        ValidateUtils.checkIllegalParameter(job.getJobName().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(job.getJobName().get().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(job.getJobId().isEmpty(), "job id not specified");
        ValidateUtils.checkIllegalParameter(job.getJobId().get().isEmpty(), "job id not specified");
        return waitStatusCommon(new MonitorJobWaitForParams.Builder(job.getJobName().get(), job.getJobId().get())
                .jobStatus(statusType).attempts(attempts).watchDelay(watchDelay).build());
    }

    /**
     * Given the jobname/jobid, waits for the given status of the job. This API will poll for the given status once
     * every 3 seconds for at least 1000 times. If the polling interval/duration is NOT sufficient, use
     * "waitForStatusCommon" method to adjust.
     * <p>
     * See JavaDoc for "waitForStatusCommon" for full details on polling and other logic.
     *
     * @param jobName    the z/OS jobname of the job to wait for output status (see z/OSMF Jobs APIs for details)
     * @param jobId      the z/OS jobid of the job to wait for output status (see z/OSMF Jobs APIS for details)
     * @param statusType status type, see JobStatus.Type object
     * @return job document
     * @throws Exception error processing wait check request
     * @author Frank Giordano
     */
    public Job waitByStatus(String jobName, String jobId, JobStatus.Type statusType) throws Exception {
        return waitStatusCommon(new MonitorJobWaitForParams.Builder(jobName, jobId).jobStatus(statusType)
                .attempts(attempts).watchDelay(watchDelay).build());
    }

    /**
     * Given jobname/jobid, checks for the desired message continuously (based on the interval and attempts specified).
     *
     * @param params  monitor jobs parameters, see MonitorJobWaitForParams object
     * @param message message string
     * @return job document
     * @throws Exception error processing wait check request
     * @author Frank Giordano
     */
    public boolean waitMessageCommon(MonitorJobWaitForParams params, String message) throws Exception {
        ValidateUtils.checkNullParameter(params == null, "params is null");
        ValidateUtils.checkIllegalParameter(params.getJobName().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(params.getJobName().get().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(params.getJobId().isEmpty(), "job id not specified");
        ValidateUtils.checkIllegalParameter(params.getJobId().get().isEmpty(), "job id not specified");
        if (params.getAttempts().isEmpty()) {
            params.setAttempts(attempts);
        }

        if (params.getWatchDelay().isEmpty()) {
            params.setWatchDelay(watchDelay);
        }
        if (params.getLineLimit().isEmpty()) {
            params.setLineLimit(lineLimit);
        }
        return pollByMessage(params, message);
    }

    /**
     * Given jobname/jobid, checks for the desired "status" (default is "OUTPUT") continuously (based on the interval
     * and attempts specified).
     * <p>
     * The "order" of natural job status is INPUT ACTIVE OUTPUT. If the requested status is earlier in the sequence
     * than the current status of the job, then the method returns immediately (since the job will never enter the
     * requested status) with the current status of the job.
     *
     * @param params monitor jobs parameters, see MonitorJobWaitForParams object
     * @return job document
     * @throws Exception error processing wait check request
     * @author Frank Giordano
     */
    public Job waitStatusCommon(MonitorJobWaitForParams params) throws Exception {
        ValidateUtils.checkNullParameter(params == null, "params is null");
        ValidateUtils.checkIllegalParameter(params.getJobName().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(params.getJobName().get().isEmpty(), "job name not specified");
        ValidateUtils.checkIllegalParameter(params.getJobId().isEmpty(), "job id not specified");
        ValidateUtils.checkIllegalParameter(params.getJobId().get().isEmpty(), "job id not specified");
        if (params.getJobStatus().isEmpty()) {
            params.setJobStatus(DEFAULT_STATUS);
        }

        if (params.getAttempts().isEmpty()) {
            params.setAttempts(attempts);
        }
        if (params.getWatchDelay().isEmpty()) {
            params.setWatchDelay(watchDelay);
        }
        return pollByStatus(params);
    }

}