package com.jobagent.worker;

import com.jobagent.model.Job;
import com.jobagent.model.JobSource;

import java.util.List;

public interface JobSourceConnector {

    String getSourceType();

    List<Job> fetchJobs(JobSource source, int maxResults);
}
