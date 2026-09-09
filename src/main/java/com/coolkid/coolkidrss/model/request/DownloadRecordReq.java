package com.coolkid.coolkidrss.model.request;

import lombok.Data;

@Data
public class DownloadRecordReq{
	private String downUrl;
	private String ruleSavePath;
	private int ruleSaveParam;
	private String dlId;
	private Long recordId;
	private String recordTitle;
}