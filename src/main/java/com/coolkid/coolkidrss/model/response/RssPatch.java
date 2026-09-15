package com.coolkid.coolkidrss.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 按需加载的代码 patch，避免列表接口携带大文本。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RssPatch {
    private String patch;
    private String patchUrl;
    private Long size;
    private Boolean truncated;
}
