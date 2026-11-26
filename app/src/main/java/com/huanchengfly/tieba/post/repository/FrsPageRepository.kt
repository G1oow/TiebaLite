package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponse
import com.huanchengfly.tieba.post.api.models.protos.threadList.ThreadListResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.AppCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

object FrsPageRepository {
    var lastHash: String = ""
    var lastResponse: FrsPageResponse? = null

    fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int? = null,
        forceNew: Boolean = false,
    ): Flow<FrsPageResponse> {
        val hash = "${forumName}_${page}_${loadType}_${sortType}_${goodClassifyId}"
        if (!forceNew && lastResponse != null && lastHash == hash) {
            return flowOf(lastResponse!!)
        }
        lastHash = hash
        return TiebaApi.getInstance().frsPage(forumName, page, loadType, sortType, goodClassifyId)
            .map { response ->
                if (response.data_ == null) throw TiebaUnknownException
                val userMap = response.data_.user_list.associateBy { it.id }
                val blockVideo = App.INSTANCE.appPreferences.blockVideo
                val threadList = response.data_.thread_list
                    .mapNotNull { threadInfo ->
                        if (threadInfo.ala_info != null) return@mapNotNull null
                        if (blockVideo && threadInfo.videoInfo != null) return@mapNotNull null
                        threadInfo.copy(author = userMap[threadInfo.authorId])
                    }
                response.copy(data_ = response.data_.copy(thread_list = threadList))
            }
            .onEach { response ->
                lastResponse = response
            }
            .catch {
                lastResponse?.let { emit(it) } ?: throw it
            }
    }

    fun threadList(
        forumId: Long,
        forumName: String,
        page: Int,
        sortType: Int,
        threadIds: String = "",
    ): Flow<ThreadListResponse> =
        TiebaApi.getInstance()
            .threadList(forumId, forumName, page, sortType, threadIds)
            .map { response ->
                if (response.data_ == null) throw TiebaUnknownException
                val userMap = response.data_.user_list.associateBy { it.id }
                val blockVideo = App.INSTANCE.appPreferences.blockVideo
                val threadList = response.data_.thread_list
                    .mapNotNull { threadInfo ->
                        if (threadInfo.ala_info != null) return@mapNotNull null
                        if (blockVideo && threadInfo.videoInfo != null) return@mapNotNull null
                        threadInfo.copy(author = userMap[threadInfo.authorId])
                    }
                response.copy(data_ = response.data_.copy(thread_list = threadList))
            }
}