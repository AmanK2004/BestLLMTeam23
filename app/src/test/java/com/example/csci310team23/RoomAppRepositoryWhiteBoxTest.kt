package com.example.csci310team23

import com.example.csci310team23.data.local.CommentDao
import com.example.csci310team23.data.local.CommentEntity
import com.example.csci310team23.data.local.CommentVoteDao
import com.example.csci310team23.data.local.CommentVoteEntity
import com.example.csci310team23.data.local.PostDao
import com.example.csci310team23.data.local.PostEntity
import com.example.csci310team23.data.local.PostVoteDao
import com.example.csci310team23.data.local.PostVoteEntity
import com.example.csci310team23.data.local.PromptDao
import com.example.csci310team23.data.local.PromptEntity
import com.example.csci310team23.data.local.TagWatchHistoryDao
import com.example.csci310team23.data.local.TagWatchHistoryEntity
import com.example.csci310team23.data.local.UserDao
import com.example.csci310team23.data.local.UserEntity
import com.example.csci310team23.data.local.UserWatchHistoryDao
import com.example.csci310team23.data.local.UserWatchHistoryEntity
import com.example.csci310team23.data.repository.RoomAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class RoomAppRepositoryWhiteBoxTest {

    @Test
    fun registerUser_normalizesEmailAndHashesPassword() = runBlocking<Unit> {
        val env = testEnv()
        val profile = env.repository.registerUser("Alice Smith", "ALICE@USC.EDU", "1234567890", "Secret123!")
        val stored = env.userDao.getById(profile.id)!!
        assertEquals("alice@usc.edu", stored.email)
        assertNotEquals("Secret123!", stored.passwordHash)
        assertNotNull(env.repository.authenticate("alice@usc.edu", "Secret123!"))
    }

    @Test
    fun registerUser_rejectsNonUscEmail() = runBlocking<Unit> {
        val env = testEnv()
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.registerUser("Bob", "bob@gmail.com", "1234567890", "Secret123")
        }
    }

    @Test
    fun registerUser_rejectsDuplicateEmail() = runBlocking<Unit> {
        val env = testEnv()
        env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        expectSuspendThrows<IllegalStateException> {
            env.repository.registerUser("Other", "BOB@usc.edu", "0987654321", "Secret321")
        }
    }

    @Test
    fun registerUser_rejectsInvalidStudentId() = runBlocking<Unit> {
        val env = testEnv()
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.registerUser("Bob", "bob@usc.edu", "12345", "Secret123")
        }
    }

    @Test
    fun registerUser_rejectsBlankFields() = runBlocking<Unit> {
        val env = testEnv()
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.registerUser("", "user@usc.edu", "1234567890", "Secret123")
        }
    }

    @Test
    fun authenticate_returnsNullOnWrongPassword() = runBlocking<Unit> {
        val env = testEnv()
        env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        assertNull(env.repository.authenticate("bob@usc.edu", "wrong"))
    }

    @Test
    fun completeProfile_requiresDepartmentAndSchool() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.completeProfile(user.id, "", "Viterbi", null, "Bio")
        }
    }

    @Test
    fun completeProfile_rejectsLongBio() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val longBio = "a".repeat(RoomAppRepository.MAX_BIO_LENGTH + 1)
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.completeProfile(user.id, "CS", "Viterbi", null, longBio)
        }
    }

    @Test
    fun completeProfile_rejectsUnderageBirthDate() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val underage = LocalDate.now().minusYears((RoomAppRepository.MIN_AGE_YEARS - 1).toLong())
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.completeProfile(user.id, "CS", "Viterbi", underage, "Bio")
        }
    }

    @Test
    fun completeProfile_preventsChangingAffiliation() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        env.repository.completeProfile(user.id, "CS", "Viterbi", null, "Bio")
        expectSuspendThrows<IllegalStateException> {
            env.repository.completeProfile(user.id, "Art", "Dornsife", null, "Bio")
        }
    }

    @Test
    fun updateProfile_rejectsUnderageBirthDate() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val underage = LocalDate.now().minusYears((RoomAppRepository.MIN_AGE_YEARS - 1).toLong())
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.updateProfile(user.id, underage, "Bio")
        }
    }

    @Test
    fun updateProfile_trimsBioAndStoresBirthDate() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val birthDate = LocalDate.of(1995, 3, 10)
        val updated = env.repository.updateProfile(user.id, birthDate, "  Focused on AI  ")
        assertEquals("Focused on AI", updated.bio)
        assertEquals(birthDate, updated.birthDate)
    }

    @Test
    fun createPost_rejectsBlankTitle() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.createPost(user.id, "   ", "Body", "gpt-4", true)
        }
    }

    @Test
    fun createPost_rejectsBlankTag() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.createPost(user.id, "Title", "Body", "   ", true)
        }
    }

    @Test
    fun updatePost_setsEditedFlagAndTrimsFields() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val postId = env.repository.createPost(user.id, "First", "Body", "gpt-4", true)
        env.repository.updatePost(postId, "  Second  ", "  Revised body  ", "  claude  ")
        val stored = env.postDao.getById(postId)!!
        assertTrue(stored.isEdited)
        assertEquals("Second", stored.title)
        assertEquals("Revised body", stored.body)
        assertEquals("claude", stored.tag)
    }

    @Test
    fun publishDraft_marksPostPublished() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val postId = env.repository.createPost(user.id, "Draft", "Body", "gpt-4", false)
        env.repository.publishDraft(postId)
        val stored = env.postDao.getById(postId)!!
        assertTrue(stored.isPublished)
    }

    @Test
    fun createComment_rejectsBlankBody() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val postId = env.repository.createPost(user.id, "Title", "Body", "gpt-4", true)
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.createComment(postId, user.id, "Title", "   ")
        }
    }

    @Test
    fun updateComment_trimsTitleAndMarksEdited() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val postId = env.repository.createPost(user.id, "Title", "Body", "gpt-4", true)
        val commentId = env.repository.createComment(postId, user.id, "Original", "body")
        env.repository.updateComment(commentId, "   ", " next ")
        val stored = env.commentDao.getById(commentId)!!
        assertTrue(stored.isEdited)
        assertNull(stored.title)
        assertEquals("next", stored.body)
    }

    @Test
    fun voteOnPost_zeroValueRemovesVote() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val postId = env.repository.createPost(user.id, "Title", "Body", "gpt-4", true)
        env.repository.voteOnPost(postId, user.id, 1)
        assertEquals(1, env.postVoteDao.entries.size)
        env.repository.voteOnPost(postId, user.id, 0)
        assertTrue(env.postVoteDao.entries.isEmpty())
    }

    @Test
    fun voteOnPost_invalidValueThrows() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val postId = env.repository.createPost(user.id, "Title", "Body", "gpt-4", true)
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.voteOnPost(postId, user.id, 2)
        }
    }

    @Test
    fun voteOnComment_invalidValueThrows() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val postId = env.repository.createPost(user.id, "Title", "Body", "gpt-4", true)
        val commentId = env.repository.createComment(postId, user.id, null, "body")
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.voteOnComment(commentId, user.id, 3)
        }
    }

    @Test
    fun createPrompt_rejectsBlankContent() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        expectSuspendThrows<IllegalArgumentException> {
            env.repository.createPrompt(user.id, "Title", "Desc", "   ", "gpt-4", null, null, null, false)
        }
    }

    @Test
    fun updatePrompt_trimsOptionalFields() = runBlocking<Unit> {
        val env = testEnv()
        val user = env.repository.registerUser("Bob", "bob@usc.edu", "1234567890", "Secret123")
        val promptId = env.repository.createPrompt(
            user.id,
            "Title",
            "Desc",
            "Body",
            "gpt-4",
            "0.7",
            "context",
            "512",
            false
        )
        env.repository.updatePrompt(
            promptId,
            "  New ",
            "  Updated ",
            "  Body2 ",
            " claude ",
            "   ",
            "   ",
            " ",
            true
        )
        val stored = env.promptDao.getById(promptId)!!
        assertEquals("New", stored.title)
        assertEquals("Updated", stored.description)
        assertEquals("Body2", stored.content)
        assertEquals("claude", stored.tag)
        assertNull(stored.temperature)
        assertNull(stored.context)
        assertNull(stored.memoryTokens)
        assertTrue(stored.isPrivate)
    }

    @Test
    fun saveTagWatchHistory_updatesExistingEntry() = runBlocking<Unit> {
        val env = testEnv()
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = start.plusSeconds(90)
        env.repository.saveTagWatchHistory(1, "gpt4", start, null)
        env.repository.saveTagWatchHistory(1, "gpt4", start, end)
        val rows = env.tagWatchHistoryDao.getByUserId(1)
        assertEquals(1, rows.size)
        assertEquals(end.toEpochMilli(), rows.first().endTime)
    }

    @Test
    fun saveUserWatchHistory_updatesExistingEntry() = runBlocking<Unit> {
        val env = testEnv()
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = start.plusSeconds(120)
        env.repository.saveUserWatchHistory(2, "peer@usc.edu", start, null)
        env.repository.saveUserWatchHistory(2, "peer@usc.edu", start, end)
        val rows = env.userWatchHistoryDao.getByUserId(2)
        assertEquals(1, rows.size)
        assertEquals(end.toEpochMilli(), rows.first().endTime)
    }

    @Test
    fun getTagWatchHistory_returnsDomainModels() = runBlocking<Unit> {
        val env = testEnv()
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = start.plusSeconds(60)
        env.tagWatchHistoryDao.insert(
            TagWatchHistoryEntity(userId = 3, tag = "claude", startTime = start.toEpochMilli(), endTime = end.toEpochMilli())
        )
        val history = env.repository.getTagWatchHistory(3)
        assertEquals(1, history.size)
        assertEquals("claude", history.first().tag)
        assertEquals(start, history.first().startTime)
        assertEquals(end, history.first().endTime)
    }

    @Test
    fun getUserWatchHistory_returnsDomainModels() = runBlocking<Unit> {
        val env = testEnv()
        val start = Instant.parse("2024-02-01T00:00:00Z")
        val end = start.plusSeconds(30)
        env.userWatchHistoryDao.insert(
            UserWatchHistoryEntity(userId = 4, watchedUserEmail = "peer@usc.edu", startTime = start.toEpochMilli(), endTime = end.toEpochMilli())
        )
        val history = env.repository.getUserWatchHistory(4)
        assertEquals(1, history.size)
        assertEquals("peer@usc.edu", history.first().email)
        assertEquals(start, history.first().startTime)
        assertEquals(end, history.first().endTime)
    }

    private fun testEnv(): RepositoryTestEnv {
        val userDao = FakeUserDao()
        val postDao = FakePostDao()
        val commentDao = FakeCommentDao()
        val promptDao = FakePromptDao()
        val postVoteDao = FakePostVoteDao()
        val commentVoteDao = FakeCommentVoteDao()
        val tagWatchHistoryDao = FakeTagWatchHistoryDao()
        val userWatchHistoryDao = FakeUserWatchHistoryDao()
        val repository = RoomAppRepository(
            userDao,
            postDao,
            commentDao,
            promptDao,
            postVoteDao,
            commentVoteDao,
            tagWatchHistoryDao,
            userWatchHistoryDao
        )
        return RepositoryTestEnv(
            repository,
            userDao,
            postDao,
            commentDao,
            promptDao,
            postVoteDao,
            commentVoteDao,
            tagWatchHistoryDao,
            userWatchHistoryDao
        )
    }
}

private data class RepositoryTestEnv(
    val repository: RoomAppRepository,
    val userDao: FakeUserDao,
    val postDao: FakePostDao,
    val commentDao: FakeCommentDao,
    val promptDao: FakePromptDao,
    val postVoteDao: FakePostVoteDao,
    val commentVoteDao: FakeCommentVoteDao,
    val tagWatchHistoryDao: FakeTagWatchHistoryDao,
    val userWatchHistoryDao: FakeUserWatchHistoryDao
)

private class FakeUserDao : UserDao {
    private val users = mutableListOf<UserEntity>()
    private var idCounter = 1L
    private val flow = MutableStateFlow<List<UserEntity>>(emptyList())

    private fun emit() {
        flow.value = users.map { it.copy() }
    }

    override suspend fun insert(user: UserEntity): Long {
        val id = if (user.id == 0L) idCounter++ else user.id
        val entity = user.copy(id = id)
        users.removeAll { it.id == id }
        users.add(entity)
        emit()
        return id
    }

    override suspend fun update(user: UserEntity) {
        val index = users.indexOfFirst { it.id == user.id }
        if (index >= 0) {
            users[index] = user
            emit()
        }
    }

    override suspend fun getByEmail(email: String): UserEntity? =
        users.firstOrNull { it.email == email }

    override suspend fun getById(id: Long): UserEntity? =
        users.firstOrNull { it.id == id }

    override fun observeById(id: Long): Flow<UserEntity?> =
        flow.map { list -> list.firstOrNull { it.id == id } }

    override fun observeAll(): Flow<List<UserEntity>> = flow

    override suspend fun getByIds(ids: List<Long>): List<UserEntity> =
        users.filter { ids.contains(it.id) }

    override suspend fun updatePassword(userId: Long, passwordHash: String) {
        val index = users.indexOfFirst { it.id == userId }
        if (index >= 0) {
            users[index] = users[index].copy(passwordHash = passwordHash)
            emit()
        }
    }
}

private class FakePostDao : PostDao {
    private val posts = mutableListOf<PostEntity>()
    private var idCounter = 1L
    private val flow = MutableStateFlow<List<PostEntity>>(emptyList())

    private fun emit() {
        flow.value = posts.map { it.copy() }
    }

    override suspend fun insert(post: PostEntity): Long {
        val id = if (post.id == 0L) idCounter++ else post.id
        val entity = post.copy(id = id)
        posts.removeAll { it.id == id }
        posts.add(entity)
        emit()
        return id
    }

    override suspend fun update(post: PostEntity) {
        val index = posts.indexOfFirst { it.id == post.id }
        if (index >= 0) {
            posts[index] = post
            emit()
        }
    }

    override suspend fun delete(postId: Long) {
        posts.removeAll { it.id == postId }
        emit()
    }

    override suspend fun getById(postId: Long): PostEntity? =
        posts.firstOrNull { it.id == postId }

    override fun observeAll(): Flow<List<PostEntity>> = flow
}

private class FakeCommentDao : CommentDao {
    private val comments = mutableListOf<CommentEntity>()
    private var idCounter = 1L
    private val flow = MutableStateFlow<List<CommentEntity>>(emptyList())

    private fun emit() {
        flow.value = comments.map { it.copy() }
    }

    override suspend fun insert(comment: CommentEntity): Long {
        val id = if (comment.id == 0L) idCounter++ else comment.id
        val entity = comment.copy(id = id)
        comments.removeAll { it.id == id }
        comments.add(entity)
        emit()
        return id
    }

    override suspend fun update(comment: CommentEntity) {
        val index = comments.indexOfFirst { it.id == comment.id }
        if (index >= 0) {
            comments[index] = comment
            emit()
        }
    }

    override suspend fun getById(commentId: Long): CommentEntity? =
        comments.firstOrNull { it.id == commentId }

    override fun observeForPost(postId: Long): Flow<List<CommentEntity>> =
        flow.map { list -> list.filter { it.postId == postId } }

    override fun observeAll(): Flow<List<CommentEntity>> = flow
}

private class FakePromptDao : PromptDao {
    private val prompts = mutableListOf<PromptEntity>()
    private var idCounter = 1L
    private val flow = MutableStateFlow<List<PromptEntity>>(emptyList())

    private fun emit() {
        flow.value = prompts.map { it.copy() }
    }

    override suspend fun insert(prompt: PromptEntity): Long {
        val id = if (prompt.id == 0L) idCounter++ else prompt.id
        val entity = prompt.copy(id = id)
        prompts.removeAll { it.id == id }
        prompts.add(entity)
        emit()
        return id
    }

    override suspend fun update(prompt: PromptEntity) {
        val index = prompts.indexOfFirst { it.id == prompt.id }
        if (index >= 0) {
            prompts[index] = prompt
            emit()
        }
    }

    override suspend fun delete(promptId: Long) {
        prompts.removeAll { it.id == promptId }
        emit()
    }

    override suspend fun getById(promptId: Long): PromptEntity? =
        prompts.firstOrNull { it.id == promptId }

    override fun observeAll(): Flow<List<PromptEntity>> = flow
}

private class FakePostVoteDao : PostVoteDao {
    private val votes = mutableListOf<PostVoteEntity>()
    private var idCounter = 1L
    private val flow = MutableStateFlow<List<PostVoteEntity>>(emptyList())

    val entries: List<PostVoteEntity>
        get() = votes.toList()

    private fun emit() {
        flow.value = votes.map { it.copy() }
    }

    override suspend fun insert(vote: PostVoteEntity): Long {
        val index = votes.indexOfFirst { it.postId == vote.postId && it.userId == vote.userId }
        return if (index >= 0) {
            votes[index] = vote.copy(id = votes[index].id)
            emit()
            votes[index].id
        } else {
            val entity = vote.copy(id = idCounter++)
            votes.add(entity)
            emit()
            entity.id
        }
    }

    override suspend fun delete(postId: Long, userId: Long) {
        votes.removeAll { it.postId == postId && it.userId == userId }
        emit()
    }

    override suspend fun getVote(postId: Long, userId: Long): PostVoteEntity? =
        votes.firstOrNull { it.postId == postId && it.userId == userId }

    override fun observeAll(): Flow<List<PostVoteEntity>> = flow
}

private class FakeCommentVoteDao : CommentVoteDao {
    private val votes = mutableListOf<CommentVoteEntity>()
    private var idCounter = 1L
    private val flow = MutableStateFlow<List<CommentVoteEntity>>(emptyList())

    private fun emit() {
        flow.value = votes.map { it.copy() }
    }

    override suspend fun insert(vote: CommentVoteEntity): Long {
        val index = votes.indexOfFirst { it.commentId == vote.commentId && it.userId == vote.userId }
        return if (index >= 0) {
            votes[index] = vote.copy(id = votes[index].id)
            emit()
            votes[index].id
        } else {
            val entity = vote.copy(id = idCounter++)
            votes.add(entity)
            emit()
            entity.id
        }
    }

    override suspend fun delete(commentId: Long, userId: Long) {
        votes.removeAll { it.commentId == commentId && it.userId == userId }
        emit()
    }

    override suspend fun getVote(commentId: Long, userId: Long): CommentVoteEntity? =
        votes.firstOrNull { it.commentId == commentId && it.userId == userId }

    override fun observeAll(): Flow<List<CommentVoteEntity>> = flow
}

private class FakeTagWatchHistoryDao : TagWatchHistoryDao {
    private val histories = mutableListOf<TagWatchHistoryEntity>()
    private var idCounter = 1L

    override suspend fun getByUserId(userId: Long): List<TagWatchHistoryEntity> =
        histories.filter { it.userId == userId }.map { it.copy() }

    override suspend fun insert(entity: TagWatchHistoryEntity): Long {
        val stored = entity.copy(id = idCounter++)
        histories.add(stored)
        return stored.id
    }

    override suspend fun update(entity: TagWatchHistoryEntity) {
        val index = histories.indexOfFirst { it.id == entity.id }
        if (index >= 0) {
            histories[index] = entity
        }
    }

    override suspend fun delete(userId: Long, tag: String) {
        histories.removeAll { it.userId == userId && it.tag == tag }
    }
}

private class FakeUserWatchHistoryDao : UserWatchHistoryDao {
    private val histories = mutableListOf<UserWatchHistoryEntity>()
    private var idCounter = 1L

    override suspend fun getByUserId(userId: Long): List<UserWatchHistoryEntity> =
        histories.filter { it.userId == userId }.map { it.copy() }

    override suspend fun insert(entity: UserWatchHistoryEntity): Long {
        val stored = entity.copy(id = idCounter++)
        histories.add(stored)
        return stored.id
    }

    override suspend fun update(entity: UserWatchHistoryEntity) {
        val index = histories.indexOfFirst { it.id == entity.id }
        if (index >= 0) {
            histories[index] = entity
        }
    }

    override suspend fun delete(userId: Long, email: String) {
        histories.removeAll { it.userId == userId && it.watchedUserEmail == email }
    }
}

private suspend inline fun <reified T : Throwable> expectSuspendThrows(block: suspend () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) return throwable
        throw AssertionError("Expected ${T::class.java.simpleName} but was ${throwable::class.java.simpleName}", throwable)
    }
    throw AssertionError("Expected ${T::class.java.simpleName} to be thrown")
}