package com.example.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val fullName: String = "",
    val bio: String = "",
    val link: String = "",
    val avatarUrl: String = "",
    val verified: Boolean = false,
    val postsCount: Long = 0L,
    val followersCount: String = "0",
    val followingCount: String = "0",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreUserPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val location: String? = null,
    val caption: String = "",
    val imageUrl: String = "",
    val hashtags: List<String> = emptyList(),
    val likesCount: Long = 0L,
    val liked: Boolean = false,
    val saved: Boolean = false,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestorePostState(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val liked: Boolean = false,
    val saved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreUserComment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val likesCount: Long = 0L,
    val liked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
