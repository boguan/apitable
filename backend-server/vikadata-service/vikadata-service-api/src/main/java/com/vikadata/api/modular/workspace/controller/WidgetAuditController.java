package com.vikadata.api.modular.workspace.controller;

import javax.annotation.Resource;
import javax.validation.Valid;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import com.vikadata.api.annotation.ApiResource;
import com.vikadata.api.annotation.PostResource;
import com.vikadata.api.context.SessionContext;
import com.vikadata.api.control.ControlTemplate;
import com.vikadata.api.control.permission.FieldPermission;
import com.vikadata.api.model.ro.widget.WidgetAuditGlobalIdRo;
import com.vikadata.api.model.ro.widget.WidgetAuditSubmitDataRo;
import com.vikadata.api.model.vo.widget.WidgetIssuedGlobalIdVo;
import com.vikadata.api.modular.organization.service.IMemberService;
import com.vikadata.api.modular.workspace.service.INodeService;
import com.vikadata.api.modular.workspace.service.IWidgetAuditService;
import com.vikadata.core.support.ResponseData;
import com.vikadata.core.util.ExceptionUtil;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.vikadata.api.enums.exception.PermissionException.NODE_OPERATION_DENIED;

@RestController
@Api(tags = "Widget SDK - Widget Audit Api")
@ApiResource(path = "/widget/audit")
public class WidgetAuditController {

    @Resource
    private IWidgetAuditService iWidgetAuditService;

    @Resource
    private INodeService iNodeService;

    @Resource
    private IMemberService iMemberService;

    @Resource
    private ControlTemplate controlTemplate;

    @PostResource(path = "/issued/globalId", requiredPermission = false)
    @ApiOperation(value = "Issue global id")
    public ResponseData<WidgetIssuedGlobalIdVo> issuedGlobalId(@RequestBody @Valid WidgetAuditGlobalIdRo body) {
        Long userId = SessionContext.getUserId();
        String dstId = body.getDstId();
        String fieldId = body.getFieldId();

        // The method includes determining whether the template exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(dstId);
        // verify whether the user is in this space
        Long memberId = iMemberService.getMemberIdByUserIdAndSpaceId(userId, spaceId);
        controlTemplate.checkFieldPermission(memberId, dstId, fieldId, FieldPermission.EDIT_FIELD_DATA,
                status -> ExceptionUtil.isTrue(status, NODE_OPERATION_DENIED));

        WidgetIssuedGlobalIdVo result = new WidgetIssuedGlobalIdVo();
        result.setIssuedGlobalId(iWidgetAuditService.issuedGlobalId(userId, body));
        return ResponseData.success(result);
    }

    @PostResource(path = "/submit/data", requiredPermission = false)
    @ApiOperation(value = "Audit global widget submit data")
    public ResponseData<Void> auditSubmitData(@RequestBody @Valid WidgetAuditSubmitDataRo body) {
        Long userId = SessionContext.getUserId();
        String dstId = body.getDstId();
        String fieldId = body.getFieldId();

        // The method includes determining whether the template exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(dstId);
        // verify whether the user is in this space
        Long memberId = iMemberService.getMemberIdByUserIdAndSpaceId(userId, spaceId);
        controlTemplate.checkFieldPermission(memberId, dstId, fieldId, FieldPermission.EDIT_FIELD_DATA,
                status -> ExceptionUtil.isTrue(status, NODE_OPERATION_DENIED));

        iWidgetAuditService.auditSubmitData(userId, body);
        return ResponseData.success();
    }
}
