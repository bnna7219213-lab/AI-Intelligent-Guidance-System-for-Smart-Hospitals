package com.expert.service.impl;

import com.expert.common.BizException;
import com.expert.entity.SymptomTag;
import com.expert.mapper.SymptomTagMapper;
import com.expert.service.SymptomTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 症状标签服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SymptomTagServiceImpl implements SymptomTagService {

    private final SymptomTagMapper symptomTagMapper;

    @Override
    public List<SymptomTag> listAll() {
        List<SymptomTag> list = symptomTagMapper.findAll();
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public List<SymptomTag> searchByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return listAll();
        }
        List<SymptomTag> list = symptomTagMapper.findByNameLike(keyword);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public void save(SymptomTag tag) {
        tag.setDeleted(0);
        symptomTagMapper.insert(tag);
        log.info("症状标签已新增: {}", tag.getName());
    }

    @Override
    public void update(SymptomTag tag) {
        SymptomTag existing = symptomTagMapper.findById(tag.getId());
        if (existing == null) {
            throw new BizException("症状标签不存在");
        }
        symptomTagMapper.update(tag);
        log.info("症状标签已更新: id={}", tag.getId());
    }

    @Override
    public void deleteById(Long id) {
        SymptomTag existing = symptomTagMapper.findById(id);
        if (existing == null) {
            throw new BizException("症状标签不存在");
        }
        symptomTagMapper.deleteById(id);
        log.info("症状标签已删除: id={}", id);
    }

    @Override
    public List<SymptomTag> findRedFlagSymptoms() {
        List<SymptomTag> list = symptomTagMapper.findByIsRedFlag("是");
        return list != null ? list : Collections.emptyList();
    }
}
