package com.expert.service;

import com.expert.entity.PromptTemplate;

import java.util.List;

/**
 * 提示词模板服务接口
 */
public interface PromptTemplateService {

    /**
     * 根据模板键获取模板内容
     *
     * @param key 模板键
     * @return 模板内容
     */
    String getByKey(String key);

    /**
     * 新增模板
     *
     * @param template 模板信息
     */
    void save(PromptTemplate template);

    /**
     * 更新模板
     *
     * @param template 模板信息
     */
    void update(PromptTemplate template);

    /**
     * 查询所有模板
     *
     * @return 模板列表
     */
    List<PromptTemplate> listAll();
}
