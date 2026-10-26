package com.huaqiu.xiaohashu.oss.mapper;

import com.huaqiu.xiaohashu.oss.domain.po.OssUploadSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 上传会话 Mapper。
 */
@Mapper
public interface OssUploadSessionMapper {

    @Insert("""
            INSERT INTO t_oss_upload_session
            (upload_id, user_id, bucket_name, object_name, file_name, file_size,
             content_type, file_fingerprint, part_size, total_parts, status,
             expires_at, created_at, updated_at)
            VALUES
            (#{uploadId}, #{userId}, #{bucketName}, #{objectName}, #{fileName}, #{fileSize},
             #{contentType}, #{fileFingerprint}, #{partSize}, #{totalParts}, #{status},
             #{expiresAt}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OssUploadSession session);

    @Select("SELECT * FROM t_oss_upload_session WHERE upload_id = #{uploadId}")
    OssUploadSession selectByUploadId(@Param("uploadId") String uploadId);

    @Select("""
            SELECT * FROM t_oss_upload_session
            WHERE user_id = #{userId}
              AND file_fingerprint = #{fileFingerprint}
              AND file_size = #{fileSize}
              AND status IN (0, 1)
            ORDER BY id DESC
            LIMIT 1
            """)
    OssUploadSession selectActiveByUserAndFingerprint(@Param("userId") Long userId,
                                                       @Param("fileFingerprint") String fileFingerprint,
                                                       @Param("fileSize") Long fileSize);

    @Update("UPDATE t_oss_upload_session SET status = #{status}, updated_at = NOW() WHERE upload_id = #{uploadId}")
    int updateStatus(@Param("uploadId") String uploadId, @Param("status") Integer status);
}
