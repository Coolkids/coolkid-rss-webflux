package com.coolkid.coolkidrss.task.impl;

import com.coolkid.coolkidrss.aop.DistributedLock;
import com.coolkid.coolkidrss.service.RssLittleJobService;
import com.coolkid.coolkidrss.task.ScheduledTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@Data
public class CleanOldDataTask implements ScheduledTask {
    private final RssLittleJobService rssLittleJobService;
    private static final String TASK_NAME = "rss-clean-old-data";

    @Value("${coolkidrss.clean.data}")
    private boolean taskEnable;

    @Value("${coolkidrss.devmode}")
    private boolean devMode;

    @Override
    @Scheduled(fixedRate= 2 * 1000 * 3600)
    @DistributedLock(name = "ScheduledJob_"+TASK_NAME)
    public void mainJob() {
        if (devMode) {
            return;
        }
        if (taskEnable){
            rssLittleJobService.cleanRssOldData()
                    .subscribe(ignored -> { }, error -> log.error("清理旧数据失败", error));
        }
    }

    @Override
    public String taskName() {
        return TASK_NAME;
    }
}
