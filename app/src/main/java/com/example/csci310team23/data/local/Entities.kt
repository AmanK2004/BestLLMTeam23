package com.example.csci310team23.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true), Index(value = ["studentId"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val studentId: String,
    val passwordHash: String,
    val department: String,
    val school: String,
    val birthDateEpochDay: Long?,
    val bio: String,
    val isProfileComplete: Boolean = false
)

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["authorId"]), Index(value = ["tag"])]
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: Long,
    val title: String,
    val body: String,
    val tag: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPublished: Boolean = true,
    val isEdited: Boolean = false
)

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["postId"]), Index(value = ["authorId"])]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val authorId: Long,
    val title: String?,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isEdited: Boolean = false
)

@Entity(
    tableName = "prompts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["authorId"]), Index(value = ["tag"])]
)
data class PromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: Long,
    val title: String,
    val description: String,
    val content: String,
    val tag: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPrivate: Boolean = false
)

@Entity(
    tableName = "post_votes",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["postId", "userId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class PostVoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val userId: Long,
    val value: Int
)

@Entity(
    tableName = "comment_votes",
    foreignKeys = [
        ForeignKey(
            entity = CommentEntity::class,
            parentColumns = ["id"],
            childColumns = ["commentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["commentId", "userId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class CommentVoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commentId: Long,
    val userId: Long,
    val value: Int
)