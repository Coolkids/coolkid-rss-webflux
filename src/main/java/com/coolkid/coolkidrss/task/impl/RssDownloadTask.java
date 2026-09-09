package com.coolkid.coolkidrss.task.impl;

import com.coolkid.coolkidrss.aop.DistributedLock;
import com.coolkid.coolkidrss.service.DownloadService;
import com.coolkid.coolkidrss.task.ScheduledTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Data
public class RssDownloadTask implements ScheduledTask {

    private final DownloadService downloadService;
    private static final String TASK_NAME = "RssDownloadTask";

    @Value("${coolkidrss.devmode}")
    private boolean devMode;

    @Override
    @Scheduled(fixedRate= 5, timeUnit= TimeUnit.MINUTES)
    @DistributedLock(name = "ScheduledJob_"+TASK_NAME)
    public void mainJob() {
        if (devMode) {
            return;
        }
        downloadService.download()
                .subscribe(ignored -> { }, error -> log.error("执行 RSS 下载任务失败", error));
    }

    @Override
    public String taskName() {
        return TASK_NAME;
    }
}
