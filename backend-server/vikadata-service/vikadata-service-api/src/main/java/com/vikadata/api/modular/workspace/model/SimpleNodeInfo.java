package com.vikadata.api.modular.workspace.model;

import lombok.Data;

@Data
public class SimpleNodeInfo {

    private String nodeId;

    private String parentId;

    private Boolean extend;
}
