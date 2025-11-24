package com.example.photosmanager.data.model

data class PhotoSelection(
    val photoInfo: PhotoInfo,
    var action: PhotoAction = PhotoAction.KEEP
)


