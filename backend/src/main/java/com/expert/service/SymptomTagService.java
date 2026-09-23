package com.expert.service;

import com.expert.entity.SymptomTag;

import java.util.List;

/**
 * 症状标签服务接口
 */
public interface SymptomTagService {

    /**
     * 查询所有症状标签
     *
     * @return 标签列表
     */
    List<SymptomTag> listAll();

    /**
     * 按名称搜索症状标签
     *
     * @param keyword 关键字
     * @return 标签列表
     */
    List<SymptomTag> searchByName(String keyword);

    /**
     * 新增症状标签
     *
     * @param tag 标签信息
     */
    void save(SymptomTag tag);

    /**
     * 更新症状标签
     *
     * @param tag 标签信息
     */
    void update(SymptomTag tag);

    /**
     * 删除症状标签
     *
     * @param id 标签ID
     */
    void deleteById(Long id);

    /**
     * 查询红旗症状（危重标识）
     *
     * @return 红旗症状列表
     */
    List<SymptomTag> findRedFlagSymptoms();
}
