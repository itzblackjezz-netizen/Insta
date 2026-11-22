package com.example.data

import com.example.model.*

object SampleData {

    val currentUser = User(
        id = "user_me",
        username = "alex.creates",
        fullName = "Alex Mercer",
        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80",
        isVerified = true,
        isFollowing = false,
        bio = "Visual Designer & Photographer 📷\nExploring geometric shapes, neon nightscapes & quiet architecture.\n📍 San Francisco, CA",
        link = "alexmercer.design",
        postsCount = 42,
        followersCount = "18.4K",
        followingCount = "482",
        mutualFollowersSummary = "Followed by elena_v, marco_lens, and 24 others"
    )

    val sampleUsers = listOf(
        User(
            id = "user_1",
            username = "elena_v",
            fullName = "Elena Vance",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80",
            isVerified = true,
            isFollowing = true,
            bio = "Minimalist Architecture & Light Studies 🏛️\nCapturing concrete geometry across Europe.",
            link = "elenavance.studio",
            postsCount = 128,
            followersCount = "64.2K",
            followingCount = "312",
            mutualFollowersSummary = "Followed by marco_lens and 18 others"
        ),
        User(
            id = "user_2",
            username = "marco_lens",
            fullName = "Marco Rossi",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
            isVerified = true,
            isFollowing = true,
            bio = "Tokyo Nights & Street Photography 🏮\n35mm stories after midnight.",
            link = "marcorossi.photo",
            postsCount = 214,
            followersCount = "112K",
            followingCount = "419",
            mutualFollowersSummary = "Followed by elena_v, maya_style, and 31 others"
        ),
        User(
            id = "user_3",
            username = "maya_style",
            fullName = "Maya Chen",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
            isVerified = false,
            isFollowing = true,
            bio = "Sustainable Fashion & Editorial Styling ✨\nNYC • Creative Direction",
            link = "mayachen.co",
            postsCount = 96,
            followersCount = "29.8K",
            followingCount = "540",
            mutualFollowersSummary = "Followed by elena_v and 9 others"
        ),
        User(
            id = "user_4",
            username = "nordic_spaces",
            fullName = "Lukas Lindqvist",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
            isVerified = true,
            isFollowing = false,
            bio = "Scandinavian Interiors & Warm Minimalist Living ☕\nCopenhagen • Architecture & Oak",
            link = "nordicspaces.dk",
            postsCount = 84,
            followersCount = "45.1K",
            followingCount = "220",
            mutualFollowersSummary = "Followed by elena_v"
        ),
        User(
            id = "user_5",
            username = "chef_clara",
            fullName = "Clara Dupont",
            avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300&auto=format&fit=crop&q=80",
            isVerified = true,
            isFollowing = false,
            bio = "Pastry & Artisanal Coffee in Paris 🥐\nSourdough, croissants & slow mornings.",
            link = "maisonclara.fr",
            postsCount = 152,
            followersCount = "73.6K",
            followingCount = "198",
            mutualFollowersSummary = "Followed by marco_lens and 12 others"
        )
    )

    val sampleStories = listOf(
        Story(
            id = "story_me",
            userId = currentUser.id,
            username = "Your story",
            userAvatar = currentUser.avatarUrl,
            isYourStory = true,
            hasUnseenStory = false,
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&auto=format&fit=crop&q=80"
            ),
            timestamp = "Just now",
            caption = "Weekend getaway sunrise ☀️"
        ),
        Story(
            id = "story_1",
            userId = "user_1",
            username = "elena_v",
            userAvatar = sampleUsers[0].avatarUrl,
            isYourStory = false,
            hasUnseenStory = true,
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=800&auto=format&fit=crop&q=80"
            ),
            timestamp = "2h",
            caption = "Morning light at the pavilion ✨"
        ),
        Story(
            id = "story_2",
            userId = "user_2",
            username = "marco_lens",
            userAvatar = sampleUsers[1].avatarUrl,
            isYourStory = false,
            hasUnseenStory = true,
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=800&auto=format&fit=crop&q=80"
            ),
            timestamp = "4h",
            caption = "Rainy Shibuya neon reflections 🌧️"
        ),
        Story(
            id = "story_3",
            userId = "user_3",
            username = "maya_style",
            userAvatar = sampleUsers[2].avatarUrl,
            isYourStory = false,
            hasUnseenStory = true,
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=800&auto=format&fit=crop&q=80"
            ),
            timestamp = "5h",
            caption = "Autumn palette shoot preview 🍂"
        ),
        Story(
            id = "story_4",
            userId = "user_4",
            username = "nordic_spaces",
            userAvatar = sampleUsers[3].avatarUrl,
            isYourStory = false,
            hasUnseenStory = false,
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=800&auto=format&fit=crop&q=80"
            ),
            timestamp = "7h",
            caption = "Cozy reading corner essentials"
        ),
        Story(
            id = "story_5",
            userId = "user_5",
            username = "chef_clara",
            userAvatar = sampleUsers[4].avatarUrl,
            isYourStory = false,
            hasUnseenStory = true,
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=800&auto=format&fit=crop&q=80"
            ),
            timestamp = "9h",
            caption = "Fresh sourdough straight from the oven 🍞"
        )
    )

    val samplePosts = listOf(
        Post(
            id = "post_1",
            user = sampleUsers[0],
            location = "Valencia, Spain",
            musicTrack = "Ludovico Einaudi • Nuvole Bianche",
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=1080&auto=format&fit=crop&q=85",
                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=1080&auto=format&fit=crop&q=85"
            ),
            isVideo = false,
            likesCount = 3842,
            isLiked = false,
            isSaved = false,
            caption = "The convergence of clean geometry and afternoon shadows at Ciudad de las Artes y las Ciencias. Architecture that breathes.\n\nSwipe to see the second angle 👉",
            hashtags = listOf("#minimalarchitecture", "#valencia", "#geometricart", "#designinspo"),
            commentsCount = 84,
            comments = listOf(
                Comment(
                    id = "c1_1",
                    username = "marco_lens",
                    userAvatar = sampleUsers[1].avatarUrl,
                    text = "That shadow falloff is immaculate Elena! What lens did you shoot this on?",
                    timestamp = "1h",
                    likesCount = 12,
                    isLiked = true
                ),
                Comment(
                    id = "c1_2",
                    username = "nordic_spaces",
                    userAvatar = sampleUsers[3].avatarUrl,
                    text = "Pure poetry in concrete and glass. Absolutely stunning.",
                    timestamp = "45m",
                    likesCount = 5,
                    isLiked = false
                ),
                Comment(
                    id = "c1_3",
                    username = "alex.creates",
                    userAvatar = currentUser.avatarUrl,
                    text = "The second slide framing is so powerful 🔥",
                    timestamp = "20m",
                    likesCount = 2,
                    isLiked = false
                )
            ),
            timestamp = "2 hours ago",
            aspectRatio = 1.1f
        ),
        Post(
            id = "post_2",
            user = sampleUsers[1],
            location = "Shibuya, Tokyo",
            musicTrack = "Kavinsky • Nightcall (Lo-Fi Remix)",
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=1080&auto=format&fit=crop&q=85"
            ),
            isVideo = true,
            videoDuration = "0:15",
            likesCount = 8910,
            isLiked = true,
            isSaved = true,
            caption = "Rain falls over the scramble crossing, turning every puddle into an ethereal neon mirror. There is nowhere else on earth with this energy.",
            hashtags = listOf("#tokyo", "#shibuya", "#cyberpunk", "#streetphotography", "#rainynights"),
            commentsCount = 192,
            comments = listOf(
                Comment(
                    id = "c2_1",
                    username = "maya_style",
                    userAvatar = sampleUsers[2].avatarUrl,
                    text = "This looks like a still from a high-budget sci-fi film! 🌆",
                    timestamp = "3h",
                    likesCount = 28,
                    isLiked = false
                ),
                Comment(
                    id = "c2_2",
                    username = "chef_clara",
                    userAvatar = sampleUsers[4].avatarUrl,
                    text = "I miss Tokyo so much! Need ramen right now.",
                    timestamp = "2h",
                    likesCount = 9,
                    isLiked = false
                )
            ),
            timestamp = "5 hours ago",
            aspectRatio = 1.0f
        ),
        Post(
            id = "post_3",
            user = sampleUsers[2],
            location = "SoHo, New York",
            musicTrack = "Leon Bridges • Texas Sun",
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=1080&auto=format&fit=crop&q=85",
                "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=1080&auto=format&fit=crop&q=85"
            ),
            isVideo = false,
            likesCount = 5214,
            isLiked = false,
            isSaved = false,
            caption = "Editorial preview for @voguemagazine autumn collection. Earthy tones, oversized tailored coats, and natural daylight.",
            hashtags = listOf("#editorial", "#soho", "#fashionstyle", "#fallpalette", "#ootd"),
            commentsCount = 63,
            comments = listOf(
                Comment(
                    id = "c3_1",
                    username = "elena_v",
                    userAvatar = sampleUsers[0].avatarUrl,
                    text = "Love the silhouette on that camel trench coat! 🧡",
                    timestamp = "4h",
                    likesCount = 14,
                    isLiked = false
                )
            ),
            timestamp = "8 hours ago",
            aspectRatio = 1.25f
        ),
        Post(
            id = "post_4",
            user = sampleUsers[3],
            location = "Copenhagen, Denmark",
            musicTrack = "Novo Amor • Anchor",
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=1080&auto=format&fit=crop&q=85"
            ),
            isVideo = false,
            likesCount = 2740,
            isLiked = false,
            isSaved = false,
            caption = "Sunday slow mornings. Warm oak, natural linen, and filter coffee. Making space to disconnect before another busy week.",
            hashtags = listOf("#nordichome", "#copenhagen", "#hygge", "#japandi", "#interiorinspo"),
            commentsCount = 37,
            comments = listOf(
                Comment(
                    id = "c4_1",
                    username = "alex.creates",
                    userAvatar = currentUser.avatarUrl,
                    text = "The lighting here is so calm. Goals!",
                    timestamp = "6h",
                    likesCount = 4,
                    isLiked = false
                )
            ),
            timestamp = "12 hours ago",
            aspectRatio = 1.0f
        ),
        Post(
            id = "post_5",
            user = sampleUsers[4],
            location = "Le Marais, Paris",
            musicTrack = "Édith Piaf • La Vie en Rose",
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=1080&auto=format&fit=crop&q=85"
            ),
            isVideo = false,
            likesCount = 4120,
            isLiked = true,
            isSaved = false,
            caption = "Golden crust, airy crumb, and 36 hours of fermentation. The art of French baking never gets old 🥖✨",
            hashtags = listOf("#artisanbread", "#paris", "#lemarais", "#bakerylife", "#frenchpastry"),
            commentsCount = 76,
            comments = listOf(
                Comment(
                    id = "c5_1",
                    username = "marco_lens",
                    userAvatar = sampleUsers[1].avatarUrl,
                    text = "Can practically smell the crust through the screen!",
                    timestamp = "10h",
                    likesCount = 8,
                    isLiked = true
                )
            ),
            timestamp = "Yesterday",
            aspectRatio = 1.15f
        )
    )

    val sampleReels = listOf(
        Reel(
            id = "reel_1",
            user = sampleUsers[1],
            caption = "Chasing bullet trains into twilight in Kyoto 🚄✨ Watch until the end for the mountain view! #kyoto #travelreels",
            audioTitle = "Original Audio • marco_lens (Kyoto Ambience)",
            mediaUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            likesCount = 42800,
            commentsCount = 1320,
            sharesCount = 5400,
            isLiked = true,
            isSaved = true,
            comments = listOf(
                Comment(
                    id = "rc1_1",
                    username = "elena_v",
                    userAvatar = sampleUsers[0].avatarUrl,
                    text = "The colors right after sunset in Kyoto are unbeatable!",
                    timestamp = "18m",
                    likesCount = 24
                ),
                Comment(
                    id = "rc1_2",
                    username = "maya_style",
                    userAvatar = sampleUsers[2].avatarUrl,
                    text = "Adding this spot to my Japan itinerary immediately 🙌",
                    timestamp = "9m",
                    likesCount = 7
                )
            )
        ),
        Reel(
            id = "reel_2",
            user = sampleUsers[0],
            caption = "How light transforms this museum hallway over 6 hours. Architectural timelapse 🏛️ #architecture #lightstudy",
            audioTitle = "Nils Frahm • Says",
            mediaUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=800&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            likesCount = 61900,
            commentsCount = 840,
            sharesCount = 9800,
            isLiked = false,
            isSaved = false,
            comments = listOf(
                Comment(
                    id = "rc2_1",
                    username = "nordic_spaces",
                    userAvatar = sampleUsers[3].avatarUrl,
                    text = "That shadow movement across the concrete is mesmerizing.",
                    timestamp = "42m",
                    likesCount = 15
                )
            )
        ),
        Reel(
            id = "reel_3",
            user = sampleUsers[2],
            caption = "3 ways to style an oversized vintage trench this season 🧥 Which look was your favorite? 1, 2, or 3? #style #ootd",
            audioTitle = "Peggy Gou • (It Goes Like) Nanana",
            mediaUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            likesCount = 88400,
            commentsCount = 2940,
            sharesCount = 14200,
            isLiked = false,
            isSaved = false,
            comments = listOf(
                Comment(
                    id = "rc3_1",
                    username = "chef_clara",
                    userAvatar = sampleUsers[4].avatarUrl,
                    text = "Look #2 with the knit scarf is pure autumn perfection!",
                    timestamp = "1h",
                    likesCount = 31
                )
            )
        ),
        Reel(
            id = "reel_4",
            user = sampleUsers[4],
            caption = "Morning espresso pull & warm croissant layers straight from the deck oven ☕🥐 #paris #artisanbakery",
            audioTitle = "French Café Quartet • Le Matin",
            mediaUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=800&auto=format&fit=crop&q=80",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
            likesCount = 34500,
            commentsCount = 615,
            sharesCount = 4120,
            isLiked = false,
            isSaved = false,
            comments = listOf(
                Comment(
                    id = "rc4_1",
                    username = "marco_lens",
                    userAvatar = sampleUsers[1].avatarUrl,
                    text = "I need that flaky crust right now 😭",
                    timestamp = "2h",
                    likesCount = 11
                )
            )
        )
    )

    val sampleHighlights = listOf(
        StoryHighlight(
            id = "hl_1",
            title = "San Fran 🌁",
            coverUrl = "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=300&auto=format&fit=crop&q=80"
        ),
        StoryHighlight(
            id = "hl_2",
            title = "Designs 📐",
            coverUrl = "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=300&auto=format&fit=crop&q=80"
        ),
        StoryHighlight(
            id = "hl_3",
            title = "Japan ⛩️",
            coverUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=300&auto=format&fit=crop&q=80"
        ),
        StoryHighlight(
            id = "hl_4",
            title = "Coffee ☕",
            coverUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=300&auto=format&fit=crop&q=80"
        ),
        StoryHighlight(
            id = "hl_5",
            title = "Studio 💡",
            coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=300&auto=format&fit=crop&q=80"
        )
    )

    val exploreCategories = listOf(
        "For You",
        "Architecture",
        "Travel",
        "Style",
        "Street Art",
        "Interior",
        "Food & Coffee",
        "Film Photo"
    )

    val sampleExploreItems = listOf(
        "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=600&auto=format&fit=crop&q=80"
    )

    val sampleNotifications = listOf(
        NotificationItem(
            id = "notif_1",
            user = sampleUsers[0],
            type = NotificationType.LIKE,
            text = "liked your post.",
            postThumbnailUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=150&auto=format&fit=crop&q=80",
            postId = "post_1",
            timestamp = "15m",
            isRead = false
        ),
        NotificationItem(
            id = "notif_2",
            user = sampleUsers[2],
            type = NotificationType.FOLLOW,
            text = "started following you.",
            timestamp = "42m",
            isFollowingBack = true,
            isRead = false
        ),
        NotificationItem(
            id = "notif_3",
            user = sampleUsers[1],
            type = NotificationType.COMMENT,
            text = "commented: \"Love this color grading!\"",
            postThumbnailUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=150&auto=format&fit=crop&q=80",
            postId = "post_2",
            timestamp = "2h",
            isRead = true
        ),
        NotificationItem(
            id = "notif_4",
            user = sampleUsers[3],
            type = NotificationType.MENTION,
            text = "mentioned you in a comment.",
            postThumbnailUrl = "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=150&auto=format&fit=crop&q=80",
            postId = "post_4",
            timestamp = "6h",
            isRead = true
        ),
        NotificationItem(
            id = "notif_5",
            user = sampleUsers[4],
            type = NotificationType.LIKE,
            text = "and 48 others liked your reel.",
            postThumbnailUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=150&auto=format&fit=crop&q=80",
            postId = "post_5",
            timestamp = "1d",
            isRead = true
        )
    )

    data class SampleChatMedia(
        val id: String,
        val title: String,
        val mediaUri: String,
        val thumbnailUrl: String,
        val mediaType: ChatMediaType
    )

    val sampleChatMedia = listOf(
        SampleChatMedia(
            id = "cm_1",
            title = "Sunset Architecture",
            mediaUri = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=900&auto=format&fit=crop&q=85",
            thumbnailUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=400&auto=format&fit=crop&q=80",
            mediaType = ChatMediaType.IMAGE
        ),
        SampleChatMedia(
            id = "cm_2",
            title = "Neon Tokyo Street",
            mediaUri = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=900&auto=format&fit=crop&q=85",
            thumbnailUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=400&auto=format&fit=crop&q=80",
            mediaType = ChatMediaType.IMAGE
        ),
        SampleChatMedia(
            id = "cm_3",
            title = "Kyoto Escape Clip",
            mediaUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=600&auto=format&fit=crop&q=80",
            mediaType = ChatMediaType.VIDEO
        ),
        SampleChatMedia(
            id = "cm_4",
            title = "Studio Timelapse Clip",
            mediaUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600&auto=format&fit=crop&q=80",
            mediaType = ChatMediaType.VIDEO
        ),
        SampleChatMedia(
            id = "cm_5",
            title = "Morning Espresso",
            mediaUri = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=900&auto=format&fit=crop&q=85",
            thumbnailUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=400&auto=format&fit=crop&q=80",
            mediaType = ChatMediaType.IMAGE
        )
    )

    val sampleDirectMessages = listOf(
        DirectMessage(
            id = "dm_1",
            user = sampleUsers[0],
            lastMessage = "Check out the new museum light study clip! 🏛️",
            timestamp = "15m",
            unreadCount = 2,
            isOnline = true,
            lastSeenText = "Active now",
            messages = listOf(
                ChatMessage(
                    id = "m1_1",
                    senderId = sampleUsers[0].id,
                    text = "Hey Alex! Did you get a chance to check out the Valencia pavilion shots?",
                    timestamp = "10:14 AM",
                    reactions = mapOf(currentUser.id to "🔥"),
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = false
                ),
                ChatMessage(
                    id = "m1_2",
                    senderId = currentUser.id,
                    text = "Yes! The geometry on that second angle is unreal.",
                    timestamp = "10:18 AM",
                    reactions = mapOf(sampleUsers[0].id to "❤️"),
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = true
                ),
                ChatMessage(
                    id = "m1_3",
                    senderId = sampleUsers[0].id,
                    text = "Sent a photo from the courtyard",
                    mediaUri = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=900&auto=format&fit=crop&q=85",
                    mediaType = ChatMediaType.IMAGE,
                    thumbnailUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=500&auto=format&fit=crop&q=80",
                    timestamp = "10:21 AM",
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = false
                ),
                ChatMessage(
                    id = "m1_4",
                    senderId = sampleUsers[0].id,
                    text = "Check out the new museum light study clip! 🏛️",
                    mediaUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    mediaType = ChatMediaType.VIDEO,
                    thumbnailUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600&auto=format&fit=crop&q=80",
                    timestamp = "10:24 AM",
                    status = MessageDeliveryStatus.DELIVERED,
                    isFromMe = false
                )
            )
        ),
        DirectMessage(
            id = "dm_2",
            user = sampleUsers[1],
            lastMessage = "You: Count me in! Bringing the 35mm lens.",
            timestamp = "2h",
            unreadCount = 0,
            isOnline = true,
            lastSeenText = "Active now",
            messages = listOf(
                ChatMessage(
                    id = "m2_1",
                    senderId = sampleUsers[1].id,
                    text = "Are you coming to the photowalk tomorrow?",
                    timestamp = "9:05 AM",
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = false
                ),
                ChatMessage(
                    id = "m2_2",
                    senderId = currentUser.id,
                    text = "Count me in! Bringing the 35mm lens.",
                    timestamp = "9:12 AM",
                    reactions = mapOf(sampleUsers[1].id to "🙌"),
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = true
                )
            )
        ),
        DirectMessage(
            id = "dm_3",
            user = sampleUsers[2],
            lastMessage = "Loved the editorial concepts you shared!",
            timestamp = "Yesterday",
            unreadCount = 0,
            isOnline = false,
            lastSeenText = "Active 3h ago",
            messages = listOf(
                ChatMessage(
                    id = "m3_1",
                    senderId = currentUser.id,
                    text = "Here is the moodboard shot from SoHo",
                    mediaUri = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=900&auto=format&fit=crop&q=85",
                    mediaType = ChatMediaType.IMAGE,
                    thumbnailUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=500&auto=format&fit=crop&q=80",
                    timestamp = "Yesterday",
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = true
                ),
                ChatMessage(
                    id = "m3_2",
                    senderId = sampleUsers[2].id,
                    text = "Loved the editorial concepts you shared!",
                    timestamp = "Yesterday",
                    reactions = mapOf(currentUser.id to "❤️"),
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = false
                )
            )
        ),
        DirectMessage(
            id = "dm_4",
            user = sampleUsers[3],
            lastMessage = "Thanks for the feedback!",
            timestamp = "2d",
            unreadCount = 0,
            isOnline = false,
            lastSeenText = "Active 1d ago",
            messages = listOf(
                ChatMessage(
                    id = "m4_1",
                    senderId = currentUser.id,
                    text = "The oak shelving lighting looks super clean.",
                    timestamp = "2d ago",
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = true
                ),
                ChatMessage(
                    id = "m4_2",
                    senderId = sampleUsers[3].id,
                    text = "Thanks for the feedback!",
                    timestamp = "2d ago",
                    status = MessageDeliveryStatus.SEEN,
                    isFromMe = false
                )
            )
        )
    )
}
