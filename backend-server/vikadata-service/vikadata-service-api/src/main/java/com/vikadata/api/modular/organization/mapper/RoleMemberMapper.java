package com.vikadata.api.modular.organization.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import com.vikadata.api.modular.mapper.ExpandBaseMapper;
import com.vikadata.api.modular.organization.model.RoleMemberInfoDTO;
import com.vikadata.entity.RoleMemberEntity;

public interface RoleMemberMapper extends ExpandBaseMapper<RoleMemberEntity> {

    /**
     * get role members' id by role's id.
     *
     * @param roleId    the role's id
     * @return the role members' id
     */
    Set<Long> selectUnitRefIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * delete rows by the role's id and the role member's id.
     *
     * @param roleId    the role's id
     * @param unitRefIds the role members' id
     * @return deleted rows number
     */
    Integer deleteByRoleIdAndUnitRefIds(@Param("roleId") Long roleId, @Param("unitRefIds") List<Long> unitRefIds);

    /**
     * delete rows by the role's id.
     *
     * @param roleId    the role's id
     * @return deleted rows number
     */
    Integer deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * get roles' role members' information.
     *
     * @param roleIds the roles' id
     * @return the role members' information.
     */
    List<RoleMemberInfoDTO> selectRoleMembersByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * page query roles' role members' information.
     *
     * @param roleId the role's id
     * @param page   page parameter
     * @return role members' information
     */
    IPage<RoleMemberInfoDTO> selectRoleMembersByRoleId(@Param("roleId") Long roleId, Page<Void> page);

    /**
     * delete rows by the role members' id.
     *
     * @param unitRefIds the role members' id
     * @return deleted rows number
     */
    Integer deleteByUnitRefIds(@Param("unitRefIds") Collection<Long> unitRefIds);

    /**
     * get roles' id by role member id.
     *
     * @param unitRefId the role member's id
     * @return the roles' id of the member's ref.
     */
    List<Long> selectRoleIdsByUnitRefId(@Param("unitRefId") Long unitRefId);

    /**
     * get role members' information by role's id and role members' id.
     *
     * @param roleId        the role's id
     * @param unitRefIds    the role members' id
     * @return role members information
     */
    List<RoleMemberInfoDTO> selectRoleMembersByRoleIdAndUnitRefIds(@Param("roleId") Long roleId, @Param("unitRefIds") List<Long> unitRefIds);

    /**
     * count rows by role's id.
     *
     * @param roleId    the roles' id
     * @return number of rows
     */
    Integer selectCountByRoleId(@Param("roleId") Long roleId);
}
