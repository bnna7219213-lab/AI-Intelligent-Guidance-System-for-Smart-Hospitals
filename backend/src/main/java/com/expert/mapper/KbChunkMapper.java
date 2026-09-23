package com.expert.mapper;

import com.expert.entity.KbChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 知识库文本块表 Mapper
 */
@Mapper
public interface KbChunkMapper {

    @Select("SELECT id, group_id, document_id, content, embedding, chunk_index, create_time " +
            "FROM kb_chunk WHERE group_id = #{group_id} ORDER BY chunk_index ASC")
    List<KbChunk> findByGroupId(@Param("group_id") Long groupId);

    @Select("SELECT id, group_id, document_id, content, embedding, chunk_index, create_time " +
            "FROM kb_chunk WHERE document_id = #{documentId} ORDER BY chunk_index ASC")
    List<KbChunk> findByDocumentId(@Param("documentId") Long documentId);

    @Select("SELECT id, group_id, document_id, content, embedding, chunk_index, create_time " +
            "FROM kb_chunk ORDER BY document_id ASC, chunk_index ASC")
    List<KbChunk> findAll();

    @Insert("INSERT INTO kb_chunk (group_id, document_id, content, embedding, chunk_index, create_time) " +
            "VALUES (#{groupId}, #{documentId}, #{content}, #{embedding}, #{chunkIndex}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KbChunk chunk);

    @Delete("DELETE FROM kb_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
