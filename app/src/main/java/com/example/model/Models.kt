package com.example.model

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val isVerified: Boolean = false,
    val isFollowing: Boolean = false,
    val bio: String = "",
    val link: String = "",
    val postsCount: Int = 0,
    val followersCount: String = "0",
    val followingCount: String = "0",
    val mutualFollowersSummary: String = ""
)

data class Story(
    val id: String,
    val userId: String,
    val username: String,
    val userAvatar: String,
    val isYourStory: Boolean = false,
    val hasUnseenStory: Boolean = true,
    val mediaUrls: List<String> = emptyList(),
    val timestamp: String = "1h",
    val caption: String = "",
    val isLiked: Boolean = false,
    val lastReaction: String? = null
)

data class Comment(
    val id: String,
    val username: String,
    val userAvatar: String,
    val text: String,
    val timestamp: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val replyToUsername: String? = null
)

data class Post(
    val id: String,
    val user: User,
    val location: String? = null,
    val musicTrack: String? = null,
    val mediaUrls: List<String>,
    val isVideo: Boolean = false,
    val videoDuration: String? = null,
    val likesCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val caption: String,
    val hashtags: List<String> = emptyList(),
    val commentsCount: Int,
    val comments: List<Comment> = emptyList(),
    val timestamp: String,
    val aspectRatio: Float = 1.0f
)

data class Reel(
    val id: String,
    val user: User,
    val caption: String,
    val audioTitle: String,
    val mediaUrl: String,
    val videoUrl: String = "",
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val comments: List<Comment> = emptyList()
)

enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    MENTION,
    STORY_INTERACTION,
    FOLLOW_REQUEST
}

data class NotificationItem(
    val id: String,
    val user: User,
    val type: NotificationType,
    val text: String,
    val postThumbnailUrl: String? = null,
    val postId: String? = null,
    val storyId: String? = null,
    val timestamp: String,
    val isFollowingBack: Boolean = false,
    val isRead: Boolean = false
)

enum class MessageDeliveryStatus {
    SENT,
    DELIVERED,
    SEEN
}

enum class ChatMediaType {
    IMAGE,
    VIDEO
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String = "",
    val mediaUri: String? = null,
    val mediaType: ChatMediaType? = null,
    val thumbnailUrl: String? = null,
    val timestamp: String,
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val status: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val isFromMe: Boolean = false
)

data class DirectMessage(
    val id: String,
    val user: User,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeenText: String = if (isOnline) "Active now" else "Active recently",
    val isTyping: Boolean = false,
    val messages: List<ChatMessage> = emptyList()
)

data class StoryHighlight(
    val id: String,
    val title: String,
    val coverUrl: String
)

enum class NavigationTab(val title: String) {
    HOME("Home"),
    EXPLORE("Explore"),
    CREATE("Create"),
    REELS("Reels"),
    PROFILE("Profile")
}
