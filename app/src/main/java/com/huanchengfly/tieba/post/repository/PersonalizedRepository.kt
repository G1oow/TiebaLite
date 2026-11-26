package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import kotlinx.coroutines.flow.map

object PersonalizedRepository {
    /**
     * 个性推荐
     *
     * @param loadType 加载类型（1 - 下拉刷新 2 - 加载更多）
     * @param page 分页页码
     */
    fun personalizedFlow(loadType: Int, page: Int = 1) =
        TiebaApi.getInstance()
            .personalizedProtoFlow(loadType, page)
            .map { response ->
                val liveThreadIds = response.data_?.thread_list
                    ?.mapNotNull { if (it.ala_info != null) it.id else null }
                    ?.toHashSet()
                    ?: emptySet()
                if (liveThreadIds.isEmpty()) return@map response
                response.copy(
                    data_ = response.data_?.copy(
                        thread_list = response.data_.thread_list.filter { it.id !in liveThreadIds },
                        thread_personalized = response.data_.thread_personalized.filter {
                            it.tid !in liveThreadIds
                        }
                    )
                )
            }
}