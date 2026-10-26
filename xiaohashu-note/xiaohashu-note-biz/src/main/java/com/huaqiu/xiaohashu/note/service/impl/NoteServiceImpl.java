package com.huaqiu.xiaohashu.note.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.nacos.shaded.com.google.common.base.Preconditions;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huaqiu.framework.common.exception.BizException;
import com.huaqiu.framework.common.response.Response;
import com.huaqiu.framework.common.util.JsonUtils;
import com.huaqiu.xiaohashu.framework.biz.context.holder.LoginUserContextHolder;
import com.huaqiu.xiaohashu.note.contants.MQConstants;
import com.huaqiu.xiaohashu.note.domain.dto.GetNoteDetailReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.GetNoteDetailRspDto;
import com.huaqiu.xiaohashu.note.domain.dto.NoteListItemDto;
import com.huaqiu.xiaohashu.note.domain.dto.NoteListRspDto;
import com.huaqiu.xiaohashu.note.domain.dto.PublishNoteReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.UpdateNoteReqDto;
import com.huaqiu.xiaohashu.note.domain.po.Note;
import com.huaqiu.xiaohashu.note.domain.po.Topic;
import com.huaqiu.xiaohashu.note.enums.NoteStatusEnum;
import com.huaqiu.xiaohashu.note.enums.NoteTypeEnum;
import com.huaqiu.xiaohashu.note.enums.NoteVisibleEnum;
import com.huaqiu.xiaohashu.note.enums.ResponseCodeEnum;
import com.huaqiu.xiaohashu.note.helper.NoteBloomFilterHelper;
import com.huaqiu.xiaohashu.note.helper.NoteCacheHelper;
import com.huaqiu.xiaohashu.note.mapper.NoteMapper;
import com.huaqiu.xiaohashu.note.mapper.TopicMapper;
import com.huaqiu.xiaohashu.note.rpc.DistributedIdGeneratorRpcService;
import com.huaqiu.xiaohashu.note.rpc.UserRpcService;
import com.huaqiu.xiaohashu.note.domain.dto.SearchNoteReqDto;
import com.huaqiu.xiaohashu.note.domain.dto.SearchNoteRspDto;
import com.huaqiu.xiaohashu.note.service.NoteSearchService;
import com.huaqiu.xiaohashu.note.service.NoteService;
import com.huaqiu.xiaohashu.user.domain.po.FindUserInfoRspDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.sql.SQLOutput;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("SpellCheckingInspection")
@Service
@Slf4j
public class NoteServiceImpl implements NoteService {

    @Resource
    private NoteMapper noteMapper;


    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Resource
    private TopicMapper topicMapper;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private NoteBloomFilterHelper noteBloomFilterHelper;


    @Resource
    private NoteCacheHelper noteCacheHelper;


    @Resource(name="taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RDelayedQueue<String> redisCacheDelayedQueue;

    @Resource
    private NoteSearchService noteSearchService;
    /**
     * 用户信息本地缓存
     */
    private static final Cache<Long, GetNoteDetailRspDto> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(30, TimeUnit.SECONDS) // 设置缓存条目在写入后 30 秒过期
            .build();


    @Override
    public Response<?> publishNote(PublishNoteReqDto publishNoteReqDto) {

        Integer type = publishNoteReqDto.getType();
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);
        if(Objects.isNull(noteTypeEnum)){
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }


        // 发布者用户 ID
        Long creatorId = LoginUserContextHolder.getUserId();
        // 话题
        Long topicId = publishNoteReqDto.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            try {
                // 获取话题名称
                Topic topic = topicMapper.selectByPrimaryKey(topicId);
                if (Objects.nonNull(topic)) {
                    topicName = topic.getName();
                    log.info("成功获取话题名称，topicId: {}, topicName: {}", topicId, topicName);
                } else {
                    log.warn("话题不存在，topicId: {}", topicId);
                    // 根据业务决定是否设置默认值或抛出异常
                    topicName = "默认话题";
                }
            } catch (Exception e) {
                log.error("获取话题名称失败，topicId: {}", topicId, e);
                // 异常处理
                topicName = "未知话题";
            }
        }

        // 获得图像或者视频的uri
        String imgUris = null;
        String videoUri = null;

        Boolean isContentEmpty = true;

        switch (noteTypeEnum){
            case IMAGE_TEXT:
                List<String> imgUriList = publishNoteReqDto.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");
                // 将图片链接拼接，以逗号分隔
                imgUris = StringUtils.join(imgUriList, ",");

                break;
            case VIDEO: // 视频笔记
                videoUri = publishNoteReqDto.getVideoUris();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
                break;
        }

        // RPC: 调用分布式 ID 生成服务，生成号段模式笔记 ID
        Long noteId = Long.valueOf(distributedIdGeneratorRpcService.getSegmentId());
        String content = publishNoteReqDto.getContent();
        // 正文直接存 MySQL，不再使用 KV/Cassandra
        if (StringUtils.isNotBlank(content)) {
            isContentEmpty = false;
        }


        Note note = Note.builder()
                .id(noteId)
                .creatorId(creatorId)
                .title(publishNoteReqDto.getTitle())
                .topicId(topicId)
                .topicName(topicName)
                .type(type)
                .imgUris(imgUris)
                .videoUri(videoUri)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .isTop(Boolean.FALSE)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(NoteStatusEnum.NORMAL.getCode())
                .content(content)
                .isContentEmpty(isContentEmpty)
                .build();

         try {
             noteMapper.insert(note);
             // ✅ 写入 BloomFilter（生产建议走 MQ，这里先同步）
             noteBloomFilterHelper.add(note.getId());
             // 同步到 Meilisearch 搜索索引
             noteSearchService.indexNote(note);

         }catch (Exception e){
             log.error("插入失败", e);

         }

        System.out.println("返回成功");

        return Response.success();
    }

    @Override
    public Response<?> getNoteDetail(GetNoteDetailReqDto req) {

        Long noteId = req.getNoteId();

        // ==============================
        // 1️⃣ BloomFilter 拦截
        // ==============================
        if (!noteBloomFilterHelper.contains(noteId)) {
            return Response.fail("布隆过滤器拦截 笔记不存在");
        }

        // ==============================
        // 2️⃣ 本地缓存
        // ==============================
        GetNoteDetailRspDto localCache = LOCAL_CACHE.getIfPresent(noteId);
        if (Objects.nonNull(localCache)) {
            log.info("==> 命中了本地缓存；{}", localCache);
            return Response.success(localCache);
        }

        // ==============================
        // 3️⃣ Redis 缓存
        // ==============================
        String cacheStr = noteCacheHelper.get(noteId);

        if (StringUtils.isNotBlank(cacheStr)) {

            if ("NULL".equals(cacheStr)) {
                return Response.fail("笔记不存在");
            }

            try {
                GetNoteDetailRspDto cacheObj =
                        JsonUtils.parseObject(cacheStr, GetNoteDetailRspDto.class);
                System.out.println("命中redis缓存");
                taskExecutor.submit(() -> LOCAL_CACHE.put(noteId, cacheObj));

                return Response.success(cacheObj);
            } catch (Exception e) {
                log.error("缓存反序列化失败", e);
            }
        }

        // ==============================
        // 4️⃣ 查 DB
        // ==============================
        Note note = noteMapper.selectByPrimaryKey(noteId);

        if (Objects.isNull(note)) {
            noteCacheHelper.set(noteId, "NULL", 300);
            return Response.fail("笔记不存在");
        }

        // ==============================
        // 5️⃣ 正文与用户信息（正文已随 t_note 查出，不再调 KV）

        // 正文直接从 note 中获取
        String content = note.getContent();

        // 查用户信息
        FindUserInfoRspDto userInfo = userRpcService.findUserInfo(note.getCreatorId());

        // ==============================
        // 6️⃣ 组装返回
        // ==============================
        GetNoteDetailRspDto rsp = GetNoteDetailRspDto.builder()
                .noteId(noteId)
                .title(note.getTitle())
                .content(content)
                .updateTime(note.getUpdateTime())
                .visible(note.getVisible())
                .type(note.getType())
                .imgUris(note.getImgUris())
                .videoUri(note.getVideoUri())
                .topicId(note.getTopicId())
                .topicName(note.getTopicName())
                .creatorId(userInfo.getUserId())
                .creatorNicakname(userInfo.getNickname())
                .creatorAvatar(userInfo.getAvatar())
                .build();

        // ==============================
        // 7️⃣ 异步写缓存
        // ==============================
        CompletableFuture.runAsync(() -> {
            try {
                long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                String json = JsonUtils.toJsonString(rsp);
                noteCacheHelper.set(noteId, json, expireSeconds);
            } catch (Exception e) {
                log.error("缓存写入失败", e);
            }
        }, taskExecutor);

        return Response.success(rsp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<?> updateNote(UpdateNoteReqDto updateNoteReqDto) {
        // 笔记 ID
        Long noteId = updateNoteReqDto.getId();
        // 笔记类型
        Integer type = updateNoteReqDto.getType();

        // 获取对应类型的枚举
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        // 若非图文、视频，抛出业务业务异常
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT: // 图文笔记
                List<String> imgUriList = updateNoteReqDto.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");

                imgUris = StringUtils.join(imgUriList, ",");
                break;
            case VIDEO: // 视频笔记
                videoUri = updateNoteReqDto.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
                break;
            default:
                break;
        }

        // 话题
        Long topicId = updateNoteReqDto.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            try {
                // 获取话题名称
                Topic topic = topicMapper.selectByPrimaryKey(topicId);
                if (Objects.nonNull(topic)) {
                    topicName = topic.getName();
                    log.info("成功获取话题名称，topicId: {}, topicName: {}", topicId, topicName);
                } else {
                    log.warn("话题不存在，topicId: {}", topicId);
                    // 根据业务决定是否设置默认值或抛出异常
                    topicName = "默认话题";
                }
            } catch (Exception e) {
                log.error("获取话题名称失败，topicId: {}", topicId, e);
                // 异常处理
                topicName = "未知话题";
            }
        }

        noteCacheHelper.delete(noteId);


        // 更新笔记元数据表 t_note
        String content = updateNoteReqDto.getContent();
        Note noteDO = Note.builder()
                .id(noteId)
                .isContentEmpty(StringUtils.isBlank(content))
                .content(content)
                .imgUris(imgUris)
                .title(updateNoteReqDto.getTitle())
                .topicId(updateNoteReqDto.getTopicId())
                .topicName(topicName)
                .type(type)
                .updateTime(LocalDateTime.now())
                .videoUri(videoUri)
                .build();

        noteMapper.updateByPrimaryKeySelective(noteDO);

        // Redisson 广播：通知所有实例删除本地 Caffeine 缓存（替代 RocketMQ 广播消息）
        RTopic topic = redissonClient.getTopic(MQConstants.TOPIC_DELETE_NOTE_LOCAL_CACHE);
        topic.publish(noteId);
        log.info("====> Redisson 广播：删除笔记本地缓存已发布...");

        // 一致性保证：延迟双删策略（Redisson 延时队列替代 RocketMQ 延时消息）
        // 1 秒后由 RedisCacheDelayedDeleteConsumer 删除 Redis 缓存
        redisCacheDelayedQueue.offer(String.valueOf(noteId), 1, TimeUnit.SECONDS);

        // 正文已随上面的 updateByPrimaryKeySelective 一起更新，无需再调 KV

        // 同步 Meilisearch 索引（重新读取完整笔记，保证过滤字段完整）
        Note updatedNote = noteMapper.selectByPrimaryKey(noteId);
        noteSearchService.indexNote(updatedNote);

        return Response.success();
    }

    /**
     * 删除本地笔记缓存
     * @param noteId
     */
    public void deleteNoteLocalCache(Long noteId) {
        LOCAL_CACHE.invalidate(noteId);
    }

    @Override
    public Response<List<SearchNoteRspDto>> searchNote(SearchNoteReqDto req) {
        List<Long> ids = noteSearchService.searchNoteIds(req.getKeyword(), req.getPage(), req.getSize(), req.getType());
        List<SearchNoteRspDto> list = new ArrayList<>();
        if (CollUtil.isEmpty(ids)) {
            return Response.success(list);
        }
        for (Long id : ids) {
            Note note = noteMapper.selectByPrimaryKey(id);
            if (note == null) {
                continue;
            }
            SearchNoteRspDto dto = SearchNoteRspDto.builder()
                    .noteId(id)
                    .title(note.getTitle())
                    .imgUris(note.getImgUris())
                    .creatorId(note.getCreatorId())
                    .updateTime(note.getUpdateTime() == null ? null : note.getUpdateTime().toString())
                    .build();
            list.add(dto);
        }
        return Response.success(list);
    }

    @Override
    public Response<NoteListRspDto> listNotes(int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 10 : size;
        int offset = (safePage - 1) * safeSize;

        // 分页查公开正常笔记 id（按发布时间倒序）
        List<Long> ids = noteMapper.selectPublicIdsByPage(offset, safeSize);
        Long total = noteMapper.countPublic();

        List<NoteListItemDto> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(ids)) {
            for (Long id : ids) {
                Note note = noteMapper.selectByPrimaryKey(id);
                if (note == null) {
                    continue;
                }
                NoteListItemDto item = NoteListItemDto.builder()
                        .noteId(id)
                        .title(note.getTitle())
                        .imgUris(note.getImgUris())
                        .creatorId(note.getCreatorId())
                        .createTime(note.getCreateTime() == null ? null : note.getCreateTime().toString())
                        .build();
                list.add(item);
            }
        }

        return Response.success(NoteListRspDto.builder()
                .total(total == null ? 0L : total)
                .list(list)
                .build());
    }


    @Override
    public Response<NoteListRspDto> listMyNotes(int page, int size) {
        Long creatorId = LoginUserContextHolder.getUserId();
        if (creatorId == null) {
            return Response.fail("未登录");
        }
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 10 : size;
        int offset = (safePage - 1) * safeSize;

        // 分页查当前用户的笔记 id（按发布时间倒序）
        List<Long> ids = noteMapper.selectMyIdsByPage(creatorId, offset, safeSize);
        Long total = noteMapper.countMy(creatorId);

        List<NoteListItemDto> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(ids)) {
            for (Long id : ids) {
                Note note = noteMapper.selectByPrimaryKey(id);
                if (note == null) {
                    continue;
                }
                list.add(NoteListItemDto.builder()
                        .noteId(id)
                        .title(note.getTitle())
                        .imgUris(note.getImgUris())
                        .creatorId(note.getCreatorId())
                        .createTime(note.getCreateTime() == null ? null : note.getCreateTime().toString())
                        .build());
            }
        }

        return Response.success(NoteListRspDto.builder()
                .total(total == null ? 0L : total)
                .list(list)
                .build());
    }
}
