package com.vikadata.api.modular.space.model;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("space capacity info")
public class SpaceCapacityUsedInfo {

    /**
     * subscription package capacity used sizes
     */
    private Long currentBundleCapacityUsedSizes;

    /**
     * gift capacity used sizes
     */
    private Long giftCapacityUsedSizes;
}
