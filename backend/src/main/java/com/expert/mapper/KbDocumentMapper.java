package com.expert.mapper;

import com.expert.entity.KbDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 知识库文档表 Mapper
 */
@Mapper
public interface KbDocumentMapper {

    @Select("SELECT id, group_id, title, file_name, file_content, status, chunk_count, create_time, update_time " +
            "FROM kb_document WHERE group_id = #{groupId} ORDER BY id ASC")
    List<KbDocument> findByGroupId(@Param("groupId") Long groupId);

    @Select("SELECT id, group_id, title, file_name, file_content, status, chunk_count, create_time, update_time " +
            "FROM kb_document WHERE id = #{id}")
    KbDocument findById(@Param("id") Long id);

    @Insert("INSERT INTO kb_document (group_id, title, file_name, file_content, status, chunk_count, create_time, update_time) " +
            "VALUES (#{groupId}, #{title}, #{fileName}, #{fileContent}, #{status}, #{chunkCount}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KbDocument document);

    @Update("UPDATE kb_document SET group_id = #{groupId}, title = #{title}, file_name = #{fileName}, " +
            "file_content = #{fileContent}, status = #{status}, chunk_count = #{chunkCount}, " +
            "update_time = NOW() WHERE id = #{id}")
    int update(KbDocument document);

    @Delete("DELETE FROM kb_document WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
