package com.vikadata.api.modular.workspace.service;

import com.vikadata.define.enums.NodeType;
import com.vikadata.schema.NodeRecentlyBrowsedSchema;
public interface INodeRecentlyBrowsedService {
    /**
     * update or insert a new record
     *
     * @param memberId member id
     * @param spaceId space id
     * @param nodeId node id
     * @param nodeType node type
     * 
     * 
     */
    void saveOrUpdate(Long memberId, String spaceId, String nodeId, NodeType nodeType);

    /**
     * get mongo document by member id and node type
     *
     * @param memberId member id
     * @param nodeType node type
     * @return NodeRecentlyBrowsedEntity
     * 
     * 
     */
    NodeRecentlyBrowsedSchema getByMemberIdAndNodeType(Long memberId, NodeType nodeType);

    /**
     * create a node recently browsed record
     *
     * @param memberId member id
     * @param spaceId space id
     * @param nodeId node id
     * @param nodeType node type
     * 
     * 
     */
    void saveMemberBrowsedNodeId(Long memberId, String spaceId, String nodeId, NodeType nodeType);

}