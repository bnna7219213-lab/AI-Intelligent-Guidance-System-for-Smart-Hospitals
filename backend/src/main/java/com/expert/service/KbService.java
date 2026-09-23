package com.expert.service;

import com.expert.entity.KbChunk;
import com.expert.entity.KbDocument;
import com.expert.entity.KbGroup;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KbService {

    /**
     * 上传文档并解析文本
     *
     * @param file    上传文件
     * @param groupId 分组ID
     * @return 文档ID
     */
    Long docUpload(MultipartFile file, Long groupId);

    /**
     * 对文档进行分块并向量化存储
     *
     * @param documentId 文档ID
     */
    void chunkDocument(Long documentId);

    /**
     * 语义搜索（余弦相似度，纯内存计算）
     *
     * @param query 查询文本
     * @param topK  返回条数
     * @return 匹配的文本块列表
     */
    List<KbChunk> semanticSearch(String query, int topK);

    /**
     * 查询所有分组
     *
     * @return 分组列表
     */
    List<KbGroup> listGroups();

    /**
     * 新增分组
     *
     * @param group 分组信息
     */
    void saveGroup(KbGroup group);

    /**
     * 查询分组下的文档
     *
     * @param groupId 分组ID
     * @return 文档列表
     */
    List<KbDocument> listDocuments(Long groupId);

    /**
     * 查询文档的所有分块
     *
     * @param documentId 文档ID
     * @return 分块列表
     */
    List<KbChunk> listChunks(Long documentId);
}
