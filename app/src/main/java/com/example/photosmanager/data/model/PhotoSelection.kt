package com.example.photosmanager.data.model

data class PhotoSelection(
    val photoInfo: PhotoInfo,
    var action: PhotoAction = PhotoAction.KEEP,
    /** 由「附近」插入的照片，不允许再对它点「附近」，否则队列会无限膨胀 */
    val fromNearby: Boolean = false
)
