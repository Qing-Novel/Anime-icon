package com.sakura.anime.domain.model

import com.sakura.anime.data.local.entity.DownloadEntity

data class Download(
    val title: String,
    val detailUrl: String,
    val imgUrl: String,
    val source: Int = 0,
    val totalSize: Long = 0, /* 已下载完成的全部剧集大小，单位字节 */
    val downloadDetails: List<DownloadDetail>
) {
    fun toDownloadEntity(): DownloadEntity {
        return DownloadEntity(
            title = title,
            detailUrl = detailUrl,
            imgUrl = imgUrl,
            source = source,
        )
    }

}
