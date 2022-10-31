package com.vikadata.api.modular.workspace.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.extern.slf4j.Slf4j;

import com.vikadata.api.component.TaskManager;
import com.vikadata.api.config.properties.LimitProperties;
import com.vikadata.api.control.ControlRoleDict;
import com.vikadata.api.control.ControlTemplate;
import com.vikadata.api.control.ControlType;
import com.vikadata.api.control.role.ControlRoleManager;
import com.vikadata.api.control.role.RoleConstants.Node;
import com.vikadata.api.enums.exception.DatabaseException;
import com.vikadata.api.enums.exception.PermissionException;
import com.vikadata.api.model.dto.node.NodeBaseInfoDTO;
import com.vikadata.api.model.vo.node.BaseNodeInfo;
import com.vikadata.api.model.vo.node.RubbishNodeVo;
import com.vikadata.api.modular.control.service.IControlService;
import com.vikadata.api.modular.finance.service.ISpaceSubscriptionService;
import com.vikadata.api.modular.space.service.ISpaceAssetService;
import com.vikadata.api.modular.workspace.mapper.NodeMapper;
import com.vikadata.api.modular.workspace.service.IDatasheetService;
import com.vikadata.api.modular.workspace.service.INodeRoleService;
import com.vikadata.api.modular.workspace.service.INodeRubbishService;
import com.vikadata.api.modular.workspace.service.INodeService;
import com.vikadata.core.util.ExceptionUtil;
import com.vikadata.define.enums.NodeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.vikadata.api.enums.exception.BillingException.PLAN_FEATURE_OVER_LIMIT;
import static com.vikadata.api.enums.exception.NodeException.RUBBISH_NODE_NOT_EXIST;

@Slf4j
@Service
public class NodeRubbishServiceImpl implements INodeRubbishService {

    @Resource
    private INodeService iNodeService;

    @Resource
    private NodeMapper nodeMapper;

    @Resource
    private IDatasheetService iDatasheetService;

    @Resource
    private ISpaceAssetService iSpaceAssetService;

    @Resource
    private ControlTemplate controlTemplate;

    @Resource
    private IControlService iControlService;

    @Resource
    private INodeRoleService iNodeRoleService;

    @Resource
    private ISpaceSubscriptionService iSpaceSubscriptionService;

    @Resource
    private LimitProperties limitProperties;

    @Override
    public List<RubbishNodeVo> getRubbishNodeList(String spaceId, Long memberId, Integer size, String lastNodeId, Boolean isOverLimit) {
        log.info("The member [{}] of the space [{}] obtains the node list of the rubbish, and the ID of the last node in the loaded list:[{}]", memberId, spaceId, lastNodeId);

        boolean allowOverLimit = isOverLimit && Boolean.TRUE.equals(limitProperties.getIsAllowOverLimit());

        // Obtain the maximum storage days of the rubbish corresponding to the space subscription plan.
        long subscriptionRemainDays = iSpaceSubscriptionService.getPlanTrashRemainDays(spaceId);
        long retainDay = allowOverLimit ? limitProperties.getRubbishMaxRetainDay() : subscriptionRemainDays;
        // Push back start time (not included)
        LocalDateTime beginTime = LocalDateTime.of(LocalDate.now().minusDays(retainDay), LocalTime.MAX);
        LocalDateTime endTime = null;

        // If it is not loaded for the first time, determine whether the ID of the last node in the loaded list exceeds the number of days to save the subscription plan.
        if (StrUtil.isNotBlank(lastNodeId)) {
            LocalDateTime rubbishUpdatedAt = nodeMapper.selectRubbishUpdatedAtByNodeId(lastNodeId);
            // If the last node is not in the rubbish (recovered or completely deleted), the location fails and an abnormal service status code is returned. The client can request the last node again.
            ExceptionUtil.isNotNull(rubbishUpdatedAt, RUBBISH_NODE_NOT_EXIST);
            // It cannot be loaded for more than the number of days saved in the reading plan.
            if (rubbishUpdatedAt.compareTo(beginTime) <= 0) {
                return new ArrayList<>();
            }
            endTime = rubbishUpdatedAt;
        }
        while (true) {
            // Query the rubbish node ID (modify the reverse time sequence)
            List<String> rubbishNodeIds = nodeMapper.selectRubbishNodeIds(spaceId, size, beginTime, endTime);
            if (rubbishNodeIds.isEmpty()) {
                return new ArrayList<>();
            }
            // obtain node permissions
            ControlRoleDict roleDict = controlTemplate.fetchRubbishNodeRole(memberId, rubbishNodeIds);
            if (!roleDict.isEmpty()) {
                // filter nodes with inconsistent permissions
                List<String> rubbishNodeIdsAfterFilter = roleDict.entrySet().stream()
                        .filter(entry -> entry.getValue().isGreaterThanOrEqualTo(ControlRoleManager.parseNodeRole(Node.MANAGER)))
                        .map(Map.Entry::getKey).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(rubbishNodeIdsAfterFilter)) {
                    return nodeMapper.selectRubbishNodeInfo(spaceId, rubbishNodeIdsAfterFilter, subscriptionRemainDays);
                }
            }
            // There is no permission.
            // If the number of nodes is less than the expected number of loads, it indicates that the load has been completed and the end is returned.
            // Otherwise, the modification time of the new last node is taken as the end time, and the load is loaded forward (timeline) again.
            int count = rubbishNodeIds.size();
            if (count < size) {
                return new ArrayList<>();
            }
            endTime = nodeMapper.selectRubbishUpdatedAtByNodeId(rubbishNodeIds.get(count - 1));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverRubbishNode(Long userId, String nodeId, String parentId) {
        log.info("The user [{}] restores the node [{}] of the rubbish to the parent node [{}]", userId, nodeId, parentId);
        // Obtain the node ID of the node and its child descendants.
        List<String> subNodeIds = nodeMapper.selectBatchAllSubNodeIds(Collections.singletonList(nodeId), true);
        if (CollUtil.isNotEmpty(subNodeIds)) {
            // recovery datasheet
            iDatasheetService.updateIsDeletedStatus(userId, subNodeIds, false);
            // Restore the spatial attachment resources of the node
            iSpaceAssetService.updateIsDeletedByNodeIds(subNodeIds, false);
            // Only child nodes are restored, and the original node is handed over to the method of restoring node information.
            if (subNodeIds.size() > 1) {
                subNodeIds.remove(nodeId);
                boolean flag = SqlHelper.retBool(nodeMapper.updateIsRubbishByNodeIdIn(userId, subNodeIds, false));
                ExceptionUtil.isTrue(flag, DatabaseException.EDIT_ERROR);
            }
        }
        // Modify the pre-node ID of the first node under the original parent node
        nodeMapper.updatePreNodeIdBySelf(nodeId, null, parentId);
        // duplicate name modification at the same level
        BaseNodeInfo nodeInfo = nodeMapper.selectBaseNodeInfoByNodeId(nodeId);
        String name = iNodeService.duplicateNameModify(parentId, nodeInfo.getType(), nodeInfo.getNodeName(), null);
        // modify the information of the recovery node
        boolean flag = SqlHelper.retBool(nodeMapper.updateInfoByNodeId(nodeId, parentId, null, name));
        ExceptionUtil.isTrue(flag, DatabaseException.EDIT_ERROR);
    }

    @Override
    public void delRubbishNode(Long userId, String nodeId) {
        log.info("User [{}] completely delete the node of the rubbish [{}]", userId, nodeId);
        // Obtain the node ID of the node and its child descendants.
        List<String> subNodeIds = nodeMapper.selectBatchAllSubNodeIds(Collections.singletonList(nodeId), true);
        // logical delete node
        boolean flag = SqlHelper.retBool(nodeMapper.updateIsDeletedByNodeId(userId, nodeId));
        ExceptionUtil.isTrue(flag, DatabaseException.DELETE_ERROR);
        if (CollUtil.isEmpty(subNodeIds)) {
            return;
        }
        TaskManager.me().execute(() -> {
            // delete all roles of the node
            iNodeRoleService.deleteByNodeId(userId, subNodeIds);
            // Clear the field permissions of the deleted grid datasheet
            List<NodeBaseInfoDTO> baseNodeInfos = nodeMapper.selectBaseNodeInfoByNodeIdsIncludeDelete(subNodeIds);
            baseNodeInfos.stream().filter(info -> NodeType.toEnum(info.getType()).equals(NodeType.DATASHEET))
                    .forEach(info -> {
                        List<String> controlIds = iControlService.getControlIdByControlIdPrefixAndType(info.getNodeId(),
                                ControlType.DATASHEET_FIELD.getVal());
                        if (CollUtil.isNotEmpty(controlIds)) {
                            // turn off field permissions
                            iControlService.removeControl(userId, controlIds, true);
                        }
                    });
        });
    }

    @Override
    public void checkRubbishNode(String spaceId, Long memberId, String nodeId) {
        log.info("Check whether the rubbish node [{}] exists and whether the member [{}] has permission", nodeId, memberId);
        // Query the modification time of the rubbish node to determine whether it exists.
        LocalDateTime rubbishUpdatedAt = nodeMapper.selectRubbishUpdatedAtByNodeId(nodeId);
        ExceptionUtil.isNotNull(rubbishUpdatedAt, RUBBISH_NODE_NOT_EXIST);

        // Obtain the maximum storage days of the rubbish corresponding to the space subscription plan.
        long retainDay = Boolean.TRUE.equals(limitProperties.getIsAllowOverLimit()) ?
                limitProperties.getRubbishMaxRetainDay() : iSpaceSubscriptionService.getPlanTrashRemainDays(spaceId);
        // Subscription function restriction check
        ExceptionUtil.isTrue(rubbishUpdatedAt.isAfter(LocalDateTime.of(LocalDate.now().minusDays(retainDay), LocalTime.MAX)), PLAN_FEATURE_OVER_LIMIT);

        // Check node permissions
        ControlRoleDict roleDict = controlTemplate.fetchRubbishNodeRole(memberId, Collections.singletonList(nodeId));
        ExceptionUtil.isFalse(roleDict.isEmpty() ||
                roleDict.get(nodeId).isLessThan(ControlRoleManager.parseNodeRole(Node.MANAGER)), PermissionException.NODE_OPERATION_DENIED);
    }
}
