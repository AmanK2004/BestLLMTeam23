package com.example.csci310team23.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csci310team23.AppGraph
import com.example.csci310team23.data.model.Comment
import com.example.csci310team23.data.model.Post
import com.example.csci310team23.data.model.PostSearchType
import com.example.csci310team23.data.model.Prompt
import com.example.csci310team23.data.model.UserProfile
import com.example.csci310team23.data.model.formatRelative
import java.time.LocalDate

@Composable
fun AppRoot(
    viewModel: AppViewModel = viewModel(factory = AppViewModel.provideFactory(AppGraph.repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser = uiState.currentUser

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    when {
        currentUser == null -> {
            AuthScreen(
                authState = authState,
                onModeChange = viewModel::setAuthMode,
                onSignIn = { email, password -> viewModel.signIn(email, password) },
                onRegister = { name, email, studentId, password ->
                    viewModel.register(name, email, studentId, password)
                }
            )
        }

        uiState.requiresProfileSetup -> {
            currentUser?.let { user ->
                ProfileSetupScreen(
                    user = user,
                    onComplete = { department, school, birthDate, bio ->
                        viewModel.completeProfile(department, school, birthDate, bio)
                    },
                    onLogout = viewModel::logout
                )
            }
        }

        else -> {
            currentUser?.let { user ->
                MainScreen(
                    currentUser = user,
                    uiState = uiState,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScreen(
    authState: AuthUiState,
    onModeChange: (AuthMode) -> Unit,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var studentId by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "USC LLM Community",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = authState.mode == AuthMode.SIGN_IN,
                onClick = {
                    localError = null
                    onModeChange(AuthMode.SIGN_IN)
                },
                label = { Text("Sign In") }
            )
            FilterChip(
                selected = authState.mode == AuthMode.REGISTER,
                onClick = {
                    localError = null
                    onModeChange(AuthMode.REGISTER)
                },
                label = { Text("Register") }
            )
        }

        Spacer(modifier = Modifier.size(24.dp))

        if (authState.mode == AuthMode.REGISTER) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(Modifier.size(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("USC Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.size(12.dp))

        if (authState.mode == AuthMode.REGISTER) {
            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it.filter { ch -> ch.isDigit() }.take(10) },
                label = { Text("Student ID (10 digits)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(Modifier.size(12.dp))
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.9f),
            visualTransformation = PasswordVisualTransformation()
        )

        if (authState.mode == AuthMode.REGISTER) {
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.9f),
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(modifier = Modifier.size(24.dp))

        val errorMessage = localError ?: authState.errorMessage
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            enabled = !authState.isProcessing,
            onClick = {
                if (authState.mode == AuthMode.SIGN_IN) {
                    if (email.isBlank() || password.isBlank()) {
                        localError = "Email and password are required"
                        return@Button
                    }
                    localError = null
                    onSignIn(email.trim(), password)
                } else {
                    if (name.isBlank() || email.isBlank() || studentId.length != 10 || password.isBlank()) {
                        localError = "Please fill in all registration fields with valid values"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        localError = "Passwords do not match"
                        return@Button
                    }
                    localError = null
                    onRegister(name.trim(), email.trim(), studentId.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Text(if (authState.mode == AuthMode.SIGN_IN) "Sign In" else "Register")
        }
    }
}

@Composable
private fun ProfileSetupScreen(
    user: UserProfile,
    onComplete: (String, String, LocalDate?, String) -> Unit,
    onLogout: () -> Unit
) {
    var department by rememberSaveable { mutableStateOf(user.department) }
    var school by rememberSaveable { mutableStateOf(user.school) }
    var birthDateText by rememberSaveable { mutableStateOf(user.birthDate?.toString().orEmpty()) }
    var bio by rememberSaveable { mutableStateOf(user.bio) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Complete Your Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = school,
            onValueChange = { school = it },
            label = { Text("School") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = birthDateText,
            onValueChange = { birthDateText = it },
            label = { Text("Birth Date (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(onClick = {
            val birthDate = birthDateText.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it.trim()) }.getOrElse {
                    error = "Birth date must follow YYYY-MM-DD"
                    return@Button
                }
            }
            if (department.isBlank() || school.isBlank()) {
                error = "Affiliation information is required"
                return@Button
            }
            onComplete(department.trim(), school.trim(), birthDate, bio.trim())
        }) {
            Text("Save Profile")
        }
        OutlinedButton(onClick = onLogout) {
            Text("Sign Out")
        }
    }
}

private enum class MainSection(val title: String) {
    FEED("Feed"),
    TRENDING("Trending"),
    PROMPTS("Prompts"),
    SEARCH("Search"),
    PROFILE("Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    currentUser: UserProfile,
    uiState: AppUiState,
    viewModel: AppViewModel,
    snackbarHostState: SnackbarHostState
) {
    var selectedSection by rememberSaveable { mutableStateOf(MainSection.FEED) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USC LLM Community") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                MainSection.values().forEach { section ->
                    val icon = when (section) {
                        MainSection.FEED -> Icons.Filled.Home
                        MainSection.TRENDING -> Icons.Outlined.Whatshot
                        MainSection.PROMPTS -> Icons.Outlined.Article
                        MainSection.SEARCH -> Icons.Filled.Search
                        MainSection.PROFILE -> Icons.Filled.Person
                    }
                    NavigationBarItem(
                        selected = section == selectedSection,
                        onClick = { selectedSection = section },
                        icon = { Icon(icon, contentDescription = section.title) },
                        label = { Text(section.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedSection) {
            MainSection.FEED -> FeedSection(
                modifier = Modifier.padding(innerPadding),
                currentUser = currentUser,
                posts = uiState.posts,
                onCreatePost = viewModel::createPost,
                onUpdatePost = viewModel::updatePost,
                onCreateComment = viewModel::createComment,
                onUpdateComment = viewModel::updateComment,
                onVotePost = viewModel::voteOnPost,
                onVoteComment = viewModel::voteOnComment
            )

            MainSection.TRENDING -> TrendingSection(
                modifier = Modifier.padding(innerPadding),
                posts = uiState.trending,
                onVotePost = viewModel::voteOnPost,
                onVoteComment = viewModel::voteOnComment
            )

            MainSection.PROMPTS -> PromptSection(
                modifier = Modifier.padding(innerPadding),
                currentUser = currentUser,
                prompts = uiState.prompts,
                onCreatePrompt = viewModel::createPrompt,
                onUpdatePrompt = viewModel::updatePrompt,
                onDeletePrompt = viewModel::deletePrompt
            )

            MainSection.SEARCH -> SearchSection(
                modifier = Modifier.padding(innerPadding),
                posts = uiState.searchState.postResults,
                prompts = uiState.searchState.promptResults,
                onSearchPosts = viewModel::searchPosts,
                onSearchPrompts = viewModel::searchPrompts
            )

            MainSection.PROFILE -> ProfileSection(
                modifier = Modifier.padding(innerPadding),
                user = currentUser,
                onUpdateProfile = viewModel::updateProfile,
                onResetPassword = viewModel::resetPassword,
                onLogout = viewModel::logout
            )
        }
    }
}

@Composable
private fun FeedSection(
    modifier: Modifier = Modifier,
    currentUser: UserProfile?,
    posts: List<Post>,
    onCreatePost: (String, String, String) -> Unit,
    onUpdatePost: (Long, String, String, String) -> Unit,
    onCreateComment: (Long, String?, String) -> Unit,
    onUpdateComment: (Long, String?, String) -> Unit,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (currentUser != null) {
                CreatePostCard(onCreatePost = onCreatePost)
            }
        }
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                currentUser = currentUser,
                onUpdatePost = onUpdatePost,
                onCreateComment = onCreateComment,
                onUpdateComment = onUpdateComment,
                onVotePost = onVotePost,
                onVoteComment = onVoteComment,
                enableCommentComposer = true
            )
        }
        if (posts.isEmpty()) {
            item {
                Text(
                    text = "No posts yet. Be the first to share your LLM experience!",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun TrendingSection(
    modifier: Modifier = Modifier,
    posts: List<Post>,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                currentUser = null,
                onUpdatePost = { _, _, _, _ -> },
                onCreateComment = { _, _, _ -> },
                onUpdateComment = { _, _, _ -> },
                onVotePost = onVotePost,
                onVoteComment = onVoteComment,
                enableCommentComposer = false
            )
        }
        if (posts.isEmpty()) {
            item {
                Text(
                    text = "No trending posts yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun PromptSection(
    modifier: Modifier = Modifier,
    currentUser: UserProfile?,
    prompts: List<Prompt>,
    onCreatePrompt: (String, String, String, String) -> Unit,
    onUpdatePrompt: (Long, String, String, String, String) -> Unit,
    onDeletePrompt: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (currentUser != null) {
                CreatePromptCard(onCreatePrompt = onCreatePrompt)
            }
        }
        items(prompts, key = { it.id }) { prompt ->
            PromptCard(
                prompt = prompt,
                canEdit = currentUser?.id == prompt.author.id,
                onUpdatePrompt = onUpdatePrompt,
                onDeletePrompt = onDeletePrompt
            )
        }
        if (prompts.isEmpty()) {
            item {
                Text(
                    text = "No prompts shared yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSection(
    modifier: Modifier = Modifier,
    posts: List<Post>,
    prompts: List<Prompt>,
    onSearchPosts: (PostSearchType, String) -> Unit,
    onSearchPrompts: (String) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(PostSearchType.TAG) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var promptTag by rememberSaveable { mutableStateOf("") }
    var postSearched by rememberSaveable { mutableStateOf(false) }
    var promptSearched by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Search Posts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PostSearchType.values().forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.name.replace("_", " ")) }
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("Keyword") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(8.dp))
            Button(onClick = {
                postSearched = true
                onSearchPosts(selectedType, keyword)
            }) {
                Text("Search Posts")
            }
        }

        items(posts, key = { it.id }) { post ->
            PostSummaryCard(post = post)
        }
        if (posts.isEmpty()) {
            item {
                Text(
                    text = if (postSearched) "No post results." else "Results will appear here after you search."
                )
            }
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Search Prompts by Tag",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = promptTag,
                onValueChange = { promptTag = it },
                label = { Text("LLM Tag") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(8.dp))
            Button(onClick = {
                promptSearched = true
                onSearchPrompts(promptTag)
            }) {
                Text("Search Prompts")
            }
        }

        items(prompts, key = { it.id }) { prompt ->
            PromptSummaryCard(prompt)
        }
        if (prompts.isEmpty()) {
            item {
                Text(
                    text = if (promptSearched) "No prompt results." else "Prompt matches will show up here."
                )
            }
        }
    }
}

@Composable
private fun ProfileSection(
    modifier: Modifier = Modifier,
    user: UserProfile,
    onUpdateProfile: (LocalDate?, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onLogout: () -> Unit
) {
    var birthDate by rememberSaveable { mutableStateOf(user.birthDate?.toString().orEmpty()) }
    var bio by rememberSaveable { mutableStateOf(user.bio) }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text("Name: ${user.name}")
            Text("Email: ${user.email}")
            Text("Student ID: ${user.studentId}")
            Text("Affiliation: ${user.department} - ${user.school}")
        }
        item {
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("Birth Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )
        }
        item {
            Button(onClick = {
                val parsedBirthDate = birthDate.takeIf { it.isNotBlank() }?.let {
                    runCatching { LocalDate.parse(it.trim()) }.getOrElse {
                        message = "Birth date must follow YYYY-MM-DD"
                        return@Button
                    }
                }
                onUpdateProfile(parsedBirthDate, bio.trim())
                message = "Profile updated"
            }) {
                Text("Save Changes")
            }
        }
        item {
            Divider()
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
        }
        item {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
        }
        item {
            Button(onClick = {
                if (newPassword != confirmPassword) {
                    message = "Passwords do not match"
                    return@Button
                }
                if (newPassword.isBlank()) {
                    message = "Password cannot be blank"
                    return@Button
                }
                onResetPassword(newPassword)
                newPassword = ""
                confirmPassword = ""
                message = "Password updated"
            }) {
                Text("Update Password")
            }
        }
        item {
            Divider()
            Button(onClick = onLogout) { Text("Log Out") }
        }
        message?.let { msg ->
            item { Text(msg) }
        }
    }
}

@Composable
private fun CreatePostCard(
    onCreatePost: (String, String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Share your experience",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text("LLM Tag") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Your experience") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
            )
            Button(onClick = {
                if (title.isBlank() || tag.isBlank() || body.isBlank()) return@Button
                onCreatePost(title.trim(), body.trim(), tag.trim())
                title = ""
                tag = ""
                body = ""
            }) {
                Text("Publish")
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    currentUser: UserProfile?,
    onUpdatePost: (Long, String, String, String) -> Unit,
    onCreateComment: (Long, String?, String) -> Unit,
    onUpdateComment: (Long, String?, String) -> Unit,
    onVotePost: (Long, Int) -> Unit,
    onVoteComment: (Long, Int) -> Unit,
    enableCommentComposer: Boolean
) {
    var commentBody by rememberSaveable(post.id) { mutableStateOf("") }
    var commentTitle by rememberSaveable(post.id) { mutableStateOf("") }
    var showEditPost by remember { mutableStateOf(false) }
    var editPostTitle by remember { mutableStateOf(post.title) }
    var editPostTag by remember { mutableStateOf(post.tag) }
    var editPostBody by remember { mutableStateOf(post.body) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${post.author.name} - ${post.createdAt.formatRelative()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "#${post.tag}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium
            )

            VoteRow(
                score = post.voteSummary.score,
                currentVote = post.currentUserVote,
                onUpvote = {
                    val newValue = if (post.currentUserVote == 1) 0 else 1
                    onVotePost(post.id, newValue)
                },
                onDownvote = {
                    val newValue = if (post.currentUserVote == -1) 0 else -1
                    onVotePost(post.id, newValue)
                }
            )

            if (currentUser?.id == post.author.id) {
                TextButton(onClick = {
                    editPostTitle = post.title
                    editPostTag = post.tag
                    editPostBody = post.body
                    showEditPost = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Edit Post")
                }
            }

            Text(
                text = "Comments (${post.comments.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            post.comments.forEach { comment ->
                CommentCard(
                    comment = comment,
                    currentUser = currentUser,
                    onUpdateComment = onUpdateComment,
                    onVoteComment = onVoteComment
                )
            }

            if (enableCommentComposer && currentUser != null) {
                OutlinedTextField(
                    value = commentTitle,
                    onValueChange = { commentTitle = it },
                    label = { Text("Comment Title (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commentBody,
                    onValueChange = { commentBody = it },
                    label = { Text("Your comment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                )
                Button(onClick = {
                    if (commentBody.isBlank()) return@Button
                    onCreateComment(post.id, commentTitle.takeIf { it.isNotBlank() }?.trim(), commentBody.trim())
                    commentTitle = ""
                    commentBody = ""
                }) {
                    Text("Add Comment")
                }
            }
        }
    }

    if (showEditPost) {
        AlertDialog(
            onDismissRequest = { showEditPost = false },
            confirmButton = {
                TextButton(onClick = {
                    onUpdatePost(post.id, editPostTitle.trim(), editPostBody.trim(), editPostTag.trim())
                    showEditPost = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPost = false }) { Text("Cancel") }
            },
            title = { Text("Edit Post") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editPostTitle,
                        onValueChange = { editPostTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPostTag,
                        onValueChange = { editPostTag = it },
                        label = { Text("Tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPostBody,
                        onValueChange = { editPostBody = it },
                        label = { Text("Body") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun CommentCard(
    comment: Comment,
    currentUser: UserProfile?,
    onUpdateComment: (Long, String?, String) -> Unit,
    onVoteComment: (Long, Int) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(comment.title.orEmpty()) }
    var editBody by remember { mutableStateOf(comment.body) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = comment.title ?: "Comment",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${comment.author.name} - ${comment.createdAt.formatRelative()}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(comment.body)
            VoteRow(
                score = comment.voteSummary.score,
                currentVote = comment.currentUserVote,
                onUpvote = {
                    val newValue = if (comment.currentUserVote == 1) 0 else 1
                    onVoteComment(comment.id, newValue)
                },
                onDownvote = {
                    val newValue = if (comment.currentUserVote == -1) 0 else -1
                    onVoteComment(comment.id, newValue)
                }
            )
            if (currentUser?.id == comment.author.id) {
                TextButton(onClick = {
                    editTitle = comment.title.orEmpty()
                    editBody = comment.body
                    showEdit = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Edit Comment")
                }
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateComment(comment.id, editTitle.takeIf { it.isNotBlank() }?.trim(), editBody.trim())
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("Cancel") }
            },
            title = { Text("Edit Comment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { editBody = it },
                        label = { Text("Comment") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun VoteRow(
    score: Int,
    currentVote: Int?,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onUpvote) {
            Icon(
                imageVector = Icons.Filled.ThumbUp,
                contentDescription = "Upvote",
                tint = if (currentVote == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(score.toString(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onDownvote) {
            Icon(
                imageVector = Icons.Filled.ThumbDown,
                contentDescription = "Downvote",
                tint = if (currentVote == -1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CreatePromptCard(
    onCreatePrompt: (String, String, String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Share a Prompt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Prompt Text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
            )
            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text("LLM Tag") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                if (title.isBlank() || content.isBlank() || tag.isBlank()) return@Button
                onCreatePrompt(title.trim(), description.trim(), content.trim(), tag.trim())
                title = ""
                description = ""
                content = ""
                tag = ""
            }) {
                Text("Share Prompt")
            }
        }
    }
}

@Composable
private fun PromptCard(
    prompt: Prompt,
    canEdit: Boolean,
    onUpdatePrompt: (Long, String, String, String, String) -> Unit,
    onDeletePrompt: (Long) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(prompt.title) }
    var editDescription by remember { mutableStateOf(prompt.description) }
    var editContent by remember { mutableStateOf(prompt.content) }
    var editTag by remember { mutableStateOf(prompt.tag) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${prompt.author.name} - ${prompt.createdAt.formatRelative()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "#${prompt.tag}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (prompt.description.isNotBlank()) {
                Text(
                    text = prompt.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(prompt.content)
            if (canEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = {
                        editTitle = prompt.title
                        editDescription = prompt.description
                        editContent = prompt.content
                        editTag = prompt.tag
                        showEdit = true
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Edit")
                    }
                    TextButton(onClick = { onDeletePrompt(prompt.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            confirmButton = {
                TextButton(onClick = {
                    onUpdatePrompt(prompt.id, editTitle.trim(), editDescription.trim(), editContent.trim(), editTag.trim())
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("Cancel") }
            },
            title = { Text("Edit Prompt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Prompt Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp)
                    )
                    OutlinedTextField(
                        value = editTag,
                        onValueChange = { editTag = it },
                        label = { Text("Tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}

@Composable
private fun PostSummaryCard(post: Post) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${post.author.name} - ${post.createdAt.formatRelative()} - #${post.tag}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = post.body,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text("Score: ${post.voteSummary.score}")
        }
    }
}

@Composable
private fun PromptSummaryCard(prompt: Prompt) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = prompt.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${prompt.author.name} - #${prompt.tag}",
                style = MaterialTheme.typography.bodySmall
            )
            if (prompt.description.isNotBlank()) {
                Text(prompt.description, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = prompt.content,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

