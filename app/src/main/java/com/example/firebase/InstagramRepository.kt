package com.example.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.SampleData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.OnProgressListener
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface SocialDataSource {
    fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?>
    fun observeUserPosts(userId: String): Flow<List<FirestoreUserPost>>
    suspend fun fetchLatestUserPosts(userId: String): Result<List<FirestoreUserPost>>
    fun observePostStates(userId: String): Flow<List<FirestorePostState>>
    fun observeUserComments(userId: String): Flow<List<FirestoreUserComment>>
    suspend fun ensureUserProfile(userId: String, displayName: String?, photoUrl: String?): Result<Unit>
    suspend fun updateProfile(userId: String, username: String, fullName: String, bio: String, link: String): Result<Unit>
    suspend fun uploadPostMedia(
        userId: String,
        mediaUriString: String,
        isVideo: Boolean,
        onProgress: (Float) -> Unit = {}
    ): Result<String>
    suspend fun createPost(
        userId: String,
        username: String,
        avatarUrl: String,
        caption: String,
        imageUrl: String,
        location: String?,
        hashtags: List<String>
    ): Result<String>
    suspend fun deletePost(userId: String, postId: String): Result<Unit>
    suspend fun savePostInteraction(
        userId: String,
        postId: String,
        isUserPost: Boolean,
        liked: Boolean,
        likesCount: Int,
        saved: Boolean
    ): Result<Unit>
    suspend fun addUserComment(
        userId: String,
        postId: String,
        isUserPost: Boolean,
        username: String,
        avatarUrl: String,
        text: String
    ): Result<Unit>
}

class InstagramRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val storage: FirebaseStorage = Firebase.storage
) : SocialDataSource {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    override fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FirestoreUserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    override fun observeUserPosts(userId: String): Flow<List<FirestoreUserPost>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/posts"
        emitAll(
            db.collection("users").document(uid).collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreUserPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    override suspend fun fetchLatestUserPosts(userId: String): Result<List<FirestoreUserPost>> {
        val path = "users/$userId/posts"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val query = db.collection("users").document(uid).collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
            val snapshot = try {
                query.get(Source.SERVER).await()
            } catch (serverError: Exception) {
                if (serverError is CancellationException) throw serverError
                // Fallback to default source if offline while preserving server-first semantics
                query.get().await()
            }
            val posts = snapshot.toObjects(
                FirestoreUserPost::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(posts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    override fun observePostStates(userId: String): Flow<List<FirestorePostState>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/post_states"
        emitAll(
            db.collection("users").document(uid).collection("post_states")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestorePostState::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    override fun observeUserComments(userId: String): Flow<List<FirestoreUserComment>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/comments"
        emitAll(
            db.collection("users").document(uid).collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreUserComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    override suspend fun ensureUserProfile(
        userId: String,
        displayName: String?,
        photoUrl: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeFullName = displayName?.takeIf { it.isNotBlank() }?.take(60)
                    ?: SampleData.currentUser.fullName
                val baseHandle = safeFullName.lowercase()
                    .replace(Regex("[^a-z0-9._]"), "")
                    .let { if (it.length < 2) "creator" else it }
                    .take(24)
                val safeAvatar = photoUrl?.takeIf { it.isNotBlank() }?.take(500)
                    ?: SampleData.currentUser.avatarUrl

                val payload = mapOf(
                    "userId" to uid,
                    "username" to baseHandle,
                    "fullName" to safeFullName,
                    "bio" to SampleData.currentUser.bio.take(250),
                    "link" to SampleData.currentUser.link.take(120),
                    "avatarUrl" to safeAvatar,
                    "verified" to true,
                    "postsCount" to SampleData.currentUser.postsCount.toLong(),
                    "followersCount" to SampleData.currentUser.followersCount,
                    "followingCount" to SampleData.currentUser.followingCount,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        userId: String,
        username: String,
        fullName: String,
        bio: String,
        link: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanUsername = username.trim().take(30)
            val cleanFullName = fullName.trim().take(60)
            require(cleanUsername.length >= 2) { "Username must be at least 2 characters" }
            require(cleanFullName.isNotEmpty()) { "Name cannot be empty" }

            val updates = mapOf(
                "username" to cleanUsername,
                "fullName" to cleanFullName,
                "bio" to bio.trim().take(250),
                "link" to link.trim().take(120),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun uploadPostMedia(
        userId: String,
        mediaUriString: String,
        isVideo: Boolean,
        onProgress: (Float) -> Unit
    ): Result<String> {
        if (mediaUriString.startsWith("http://") || mediaUriString.startsWith("https://")) {
            onProgress(1f)
            return Result.success(mediaUriString)
        }
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val localUri = Uri.parse(mediaUriString)
            val ext = if (isVideo) "mp4" else "jpg"
            val objectPath = "users/$uid/posts/${System.currentTimeMillis()}_${UUID.randomUUID()}.$ext"
            val ref = storage.reference.child(objectPath)
            val metadata = StorageMetadata.Builder()
                .setContentType(if (isVideo) "video/mp4" else "image/jpeg")
                .build()

            val uploadTask = ref.putFile(localUri, metadata)
            val listener = OnProgressListener<UploadTask.TaskSnapshot> { snapshot ->
                val total = snapshot.totalByteCount
                if (total > 0L) {
                    val fraction = (snapshot.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0f, 0.99f)
                    onProgress(fraction)
                }
            }
            uploadTask.addOnProgressListener(listener)
            try {
                uploadTask.await()
            } finally {
                uploadTask.removeOnProgressListener(listener)
            }
            val downloadUrl = ref.downloadUrl.await().toString()
            onProgress(1f)
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("InstagramRepository", "Firebase Storage upload failed", e)
            Result.failure(e)
        }
    }

    override suspend fun createPost(
        userId: String,
        username: String,
        avatarUrl: String,
        caption: String,
        imageUrl: String,
        location: String?,
        hashtags: List<String>
    ): Result<String> {
        var docPath = "users/$userId/posts"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid).collection("posts").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "authorUsername" to username.take(30).ifBlank { "creator" },
                "authorAvatarUrl" to avatarUrl.take(500),
                "location" to location?.trim()?.takeIf { it.isNotEmpty() }?.take(100),
                "caption" to caption.trim().take(2200),
                "imageUrl" to imageUrl.take(500),
                "hashtags" to hashtags.take(20),
                "likesCount" to 1L,
                "liked" to true,
                "saved" to false,
                "commentsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()

            // Increment user profile postsCount safely
            db.collection("users").document(uid).update(
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun deletePost(userId: String, postId: String): Result<Unit> {
        val safePostId = postId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val docPath = "users/$userId/posts/$safePostId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(safePostId.isNotEmpty()) { "Post ID cannot be blank" }

            db.collection("users").document(uid).collection("posts").document(safePostId)
                .delete()
                .await()

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                val currentCount = userSnap.getLong("postsCount") ?: 0L
                if (currentCount > 0L) {
                    userRef.update(
                        mapOf(
                            "postsCount" to maxOf(0L, currentCount - 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun savePostInteraction(
        userId: String,
        postId: String,
        isUserPost: Boolean,
        liked: Boolean,
        likesCount: Int,
        saved: Boolean
    ): Result<Unit> {
        val safePostId = postId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val path = if (isUserPost) "users/$userId/posts/$safePostId" else "users/$userId/post_states/$safePostId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(safePostId.isNotEmpty()) { "Post ID cannot be blank" }

            if (isUserPost) {
                db.collection("users").document(uid).collection("posts").document(safePostId)
                    .update(
                        mapOf(
                            "liked" to liked,
                            "likesCount" to maxOf(0, likesCount).toLong(),
                            "saved" to saved,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            } else {
                val stateRef = db.collection("users").document(uid).collection("post_states").document(safePostId)
                val existing = stateRef.get().await()
                if (existing.exists()) {
                    stateRef.update(
                        mapOf(
                            "liked" to liked,
                            "saved" to saved,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    stateRef.set(
                        mapOf(
                            "userId" to uid,
                            "postId" to postId.take(128),
                            "liked" to liked,
                            "saved" to saved,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun addUserComment(
        userId: String,
        postId: String,
        isUserPost: Boolean,
        username: String,
        avatarUrl: String,
        text: String
    ): Result<Unit> {
        val trimmedText = text.trim().take(500)
        if (trimmedText.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty"))

        var docPath = "users/$userId/comments"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val commentRef = db.collection("users").document(uid).collection("comments").document()
            docPath = commentRef.path

            val payload = mapOf(
                "userId" to uid,
                "postId" to postId.take(128),
                "authorUsername" to username.take(30).ifBlank { "creator" },
                "authorAvatarUrl" to avatarUrl.take(500),
                "text" to trimmedText,
                "likesCount" to 0L,
                "liked" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            commentRef.set(payload).await()

            if (isUserPost) {
                val safePostId = postId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
                db.collection("users").document(uid).collection("posts").document(safePostId)
                    .update(
                        mapOf(
                            "commentsCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
