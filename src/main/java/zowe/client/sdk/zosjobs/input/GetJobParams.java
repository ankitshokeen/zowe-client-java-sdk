/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package zowe.client.sdk.zosjobs.input;

import zowe.client.sdk.zosjobs.JobsConstants;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Interface for various GetJobs APIs
 *
 * @author Frank Giordano
 * @version 2.0
 */
public class GetJobParams {

    /**
     * Owner for which to obtain jobs for.
     */
    private final Optional<String> owner;

    /**
     * Prefix to filter when obtaining jobs.
     * Default: *
     */
    private final Optional<String> prefix;

    /**
     * Max jobs to return in a list
     * Default: JobsConstants.DEFAULT_MAX_JOBS
     */
    private final OptionalInt maxJobs;

    /**
     * job id for a job
     */
    private final Optional<String> jobId;

    private GetJobParams(Builder builder) {
        this.owner = Optional.ofNullable(builder.owner);
        this.prefix = Optional.ofNullable(builder.prefix);
        if (builder.maxJobs == null) {
            this.maxJobs = OptionalInt.empty();
        } else {
            this.maxJobs = OptionalInt.of(builder.maxJobs);
        }
        this.jobId = Optional.ofNullable(builder.jobId);
    }

    /**
     * Retrieve jobId specified
     *
     * @return jobId value
     * @author Frank Giordano
     */
    public Optional<String> getJobId() {
        return jobId;
    }

    /**
     * Retrieve maxJobs specified
     *
     * @return maxJobs value
     * @author Frank Giordano
     */
    public OptionalInt getMaxJobs() {
        return maxJobs;
    }

    /**
     * Retrieve owner specified
     *
     * @return owner value
     * @author Frank Giordano
     */
    public Optional<String> getOwner() {
        return owner;
    }

    /**
     * Retrieve prefix specified
     *
     * @return prefix value
     * @author Frank Giordano
     */
    public Optional<String> getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return "GetJobParams{" +
                "owner=" + owner +
                ", prefix=" + prefix +
                ", maxJobs=" + maxJobs +
                ", jobId=" + jobId +
                '}';
    }

    public static class Builder {

        private String owner = "*";
        private String prefix = "*";
        private Integer maxJobs = JobsConstants.DEFAULT_MAX_JOBS;
        private String jobId;

        public Builder() {
        }

        public Builder(String owner) {
            this.owner = owner;
        }

        public GetJobParams build() {
            return new GetJobParams(this);
        }

        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder maxJobs(Integer maxJobs) {
            this.maxJobs = maxJobs;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

    }

}
