package com.vikadata.api.modular.workspace.model;

import lombok.Data;

@Data
public class NodeWidgetDto {

    /**
     * widget name
     */
    private String widgetName;

    /**
     * widget reference datasheet
     */
    private String dstId;

    /**
     * node id
     */
    private String nodeId;
}
