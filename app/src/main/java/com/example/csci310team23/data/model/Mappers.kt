package com.example.csci310team23.data.model

import com.example.csci310team23.data.local.CommentEntity
import com.example.csci310team23.data.local.PostEntity
import com.example.csci310team23.data.local.PostVersionEntity
import com.example.csci310team23.data.local.PromptEntity
import com.example.csci310team23.data.local.PromptVersionEntity
import com.example.csci310team23.data.local.UserEntity

fun UserEntity.toProfile(): UserProfile = UserProfile(
    id = id,
    name = name,
    email = email,
    studentId = studentId,
    department = department,
    school = school,
    birthDate = birthDateEpochDay.toLocalDate(),
    bio = bio,
    isProfileComplete = isProfileComplete
)

fun PostEntity.toDomain(
    author: UserSummary,
    comments: List<Comment>,
    voteSummary: VoteSummary,
    currentUserVote: Int?,
    isBookmarked: Boolean = false
): Post = Post(
    id = id,
    author = author,
    isAnonymous = isAnonymous,
    title = title,
    body = body,
    tag = tag,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    commentCount = comments.size,
    comments = comments,
    voteSummary = voteSummary,
    currentUserVote = currentUserVote,
    isBookmarked = isBookmarked,
    isPublished = isPublished,
    isEdited = isEdited
)

fun CommentEntity.toDomain(
    author: UserSummary,
    voteSummary: VoteSummary,
    currentUserVote: Int?
): Comment = Comment(
    id = id,
    postId = postId,
    author = author,
    title = title,
    body = body,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    voteSummary = voteSummary,
    currentUserVote = currentUserVote,
    isEdited = isEdited
)

fun PromptEntity.toDomain(author: UserSummary): Prompt = Prompt(
    id = id,
    author = author,
    title = title,
    description = description,
    content = content,
    tag = tag,
    temperature = temperature,
    context = context,
    memoryTokens = memoryTokens,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    isPrivate = isPrivate,
    isBookmarked = false,
    isEdited = isEdited
)

fun PromptEntity.toDomain(author: UserSummary, isBookmarked: Boolean): Prompt =
    toDomain(author).copy(isBookmarked = isBookmarked)

fun PostVersionEntity.toDomain(): PostVersion = PostVersion(
    id = id,
    postId = postId,
    title = title,
    body = body,
    tag = tag,
    createdAt = createdAt.toInstant()
)

fun PromptVersionEntity.toDomain(): PromptVersion = PromptVersion(
    id = id,
    promptId = promptId,
    title = title,
    description = description,
    content = content,
    tag = tag,
    temperature = temperature,
    context = context,
    memoryTokens = memoryTokens,
    createdAt = createdAt.toInstant()
)
