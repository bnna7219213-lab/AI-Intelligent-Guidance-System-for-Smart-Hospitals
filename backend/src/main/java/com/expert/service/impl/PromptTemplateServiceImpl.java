package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.PromptTemplate;
import com.expert.mapper.PromptTemplateMapper;
import com.expert.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 提示词模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final PromptTemplateMapper promptTemplateMapper;

    @Override
    public String getByKey(String key) {
        PromptTemplate template = promptTemplateMapper.findByTemplateKey(key);
        if (template == null) {
            throw new BizException("模板不存在: " + key);
        }
        return template.getTemplateContent();
    }

    @Override
    public void save(PromptTemplate template) {
        PromptTemplate existing = promptTemplateMapper.findByTemplateKey(template.getTemplateKey());
        if (existing != null) {
            throw new BizException("模板键已存在: " + template.getTemplateKey());
        }
        promptTemplateMapper.insert(template);
        log.info("模板已新增: key={}", template.getTemplateKey());
    }

    @Override
    public void update(PromptTemplate template) {
        PromptTemplate existing = promptTemplateMapper.findByTemplateKey(template.getTemplateKey());
        if (existing == null) {
            throw new BizException("模板不存在: " + template.getTemplateKey());
        }
        promptTemplateMapper.update(template);
        log.info("模板已更新: key={}", template.getTemplateKey());
    }

    @Override
    public List<PromptTemplate> listAll() {
        List<PromptTemplate> list = promptTemplateMapper.findAll();
        return list != null ? list : Collections.emptyList();
    }
}
