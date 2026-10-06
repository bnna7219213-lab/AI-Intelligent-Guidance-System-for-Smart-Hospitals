package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.SymptomTag;
import com.expert.mapper.SymptomTagMapper;
import com.expert.service.SymptomTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 症状标签服务实现
 *
 * 缓存策略（通过 RedisCacheConfig 透明切换 Redis / 内存 / 空缓存）：
 * - listAll / searchByName / findRedFlagSymptoms 读取走缓存（缓存名 symptomTags）
 * - save / update / deleteById 写入后主动失效缓存
 * 症状标签是分诊 Agent 的核心依据，几乎不变，缓存命中率高。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomTagServiceImpl implements SymptomTagService {

    private final SymptomTagMapper symptomTagMapper;

    @Override
    @Cacheable(cacheNames = "symptomTags", key = "'listAll'")
    public List<SymptomTag> listAll() {
        List<SymptomTag> list = symptomTagMapper.findAll();
        return list != null ? list : Collections.emptyList();
    }

    @Override
    @Cacheable(cacheNames = "symptomTags", key = "'search:' + (#keyword == null ? '' : #keyword)")
    public List<SymptomTag> searchByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return listAll();
        }
        List<SymptomTag> list = symptomTagMapper.findByNameLike(keyword);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    @CacheEvict(cacheNames = "symptomTags", allEntries = true)
    public void save(SymptomTag tag) {
        tag.setDeleted(0);
        symptomTagMapper.insert(tag);
        log.info("症状标签已新增: {}", tag.getName());
    }

    @Override
    @CacheEvict(cacheNames = "symptomTags", allEntries = true)
    public void update(SymptomTag tag) {
        SymptomTag existing = symptomTagMapper.findById(tag.getId());
        if (existing == null) {
            throw new BizException("症状标签不存在");
        }
        symptomTagMapper.update(tag);
        log.info("症状标签已更新: id={}", tag.getId());
    }

    @Override
    @CacheEvict(cacheNames = "symptomTags", allEntries = true)
    public void deleteById(Long id) {
        SymptomTag existing = symptomTagMapper.findById(id);
        if (existing == null) {
            throw new BizException("症状标签不存在");
        }
        symptomTagMapper.deleteById(id);
        log.info("症状标签已删除: id={}", id);
    }

    @Override
    @Cacheable(cacheNames = "symptomTags", key = "'redFlag'")
    public List<SymptomTag> findRedFlagSymptoms() {
        List<SymptomTag> list = symptomTagMapper.findByIsRedFlag("是");
        return list != null ? list : Collections.emptyList();
    }
}