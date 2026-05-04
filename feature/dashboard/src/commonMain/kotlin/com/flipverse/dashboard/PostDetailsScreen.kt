package com.flipverse.dashboard


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.flipverse.dashboard.component.CommentReplySheetContent
import com.flipverse.dashboard.component.CommentSheetContent
import com.flipverse.data.util.generateUserId
import com.flipverse.data.util.normalizeUsername
import com.flipverse.shared.Alpha
import com.flipverse.shared.PreferencesRepository.getThumbnail
import com.flipverse.shared.Red
import com.flipverse.shared.Resources
import com.flipverse.shared.Resources.Icon.Delete
import com.flipverse.shared.domain.Comment
import com.flipverse.shared.domain.ImageItem
import com.flipverse.shared.domain.Post
import com.flipverse.shared.presentation.component.FlipExpandableText
import com.flipverse.shared.util.createShareManager
import com.mohamedrejeb.calf.io.getName
import com.mohamedrejeb.calf.io.readByteArray
import com.mohamedrejeb.calf.permissions.ExperimentalPermissionsApi
import com.mohamedrejeb.calf.permissions.Permission
import com.mohamedrejeb.calf.permissions.isGranted
import com.mohamedrejeb.calf.permissions.rememberPermissionState
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import rememberMessageBarState
import com.flipverse.shared.Strings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PostDetailsScreen(
    navigateBack: () -> Unit,
    postId: String,
) {

    val viewModel = koinViewModel<DashboardViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    val shareManager = remember { createShareManager() }

    LaunchedEffect(postId) {
        viewModel.loadPostWithComments(postId)
    }

    var showMenu by remember { mutableStateOf(false) }
    val messageBarState = rememberMessageBarState()
    var showCommentSheet by remember { mutableStateOf(false) }
    var commentPost by remember { mutableStateOf<Post?>(null) }
    var showCommentReplySheet by remember { mutableStateOf(false) }
    var commentReplyPost by remember { mutableStateOf<Comment?>(null) }

    val context = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    var screenState = viewModel.recommendationScreenState

    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }


    val scope = rememberCoroutineScope()

    // Permission states
    val cameraPermission = rememberPermissionState(Permission.Camera)
    val storagePermission = rememberPermissionState(Permission.ReadStorage)

    // Image picker launcher
    val imagePickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Image,
        selectionMode = FilePickerSelectionMode.Multiple,
        onResult = { files ->
            scope.launch {
                val imageItems = files.map { file ->
                    ImageItem(
                        id = generateUserId(),
                        fileName = file.getName(context).toString(),
                        size = file.readByteArray(context).size.toLong(),
                        data = file.readByteArray(context)
                    )
                }

                onAction(DashboardAction.OnSelectImages(imageItems))
            }
        }
    )


    @OptIn(ExperimentalMaterial3Api::class)
    if (showCommentSheet && commentPost != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            // Sheet content -make a CommentBox composable
            CommentSheetContent(
                post = commentPost!!,
                onDismiss = { showCommentSheet = false },
                onPost = { replyText ->
                    viewModel.onPostComment(
                        postId = commentPost!!.id,
                        replyText = replyText,
                        onSuccess = {
                            messageBarState.addSuccess("Your comment has been posted.")
                            showCommentSheet = false
                            viewModel.loadPostWithComments(postId)
                        },
                        onError = { error -> messageBarState.addSuccess("Something went wrong...$error") }
                    )

                },
                isPosting = uiState.isPostingComment

            )
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    if (showCommentReplySheet && commentReplyPost != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showCommentReplySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            // Sheet content -make a CommentBox composable
            CommentReplySheetContent(
                comment = commentReplyPost!!,
                onDismiss = { showCommentReplySheet = false },
                onPost = { replyText ->
                    viewModel.onCommentReply(
                        postId = postId,
                        replyText = replyText,
                        onSuccess = {
                            messageBarState.addSuccess("Your comment has been posted.")
                            showCommentSheet = false
                        },
                        onError = { error -> messageBarState.addSuccess("Something went wrong...$error") },
                        parentCommentId = commentReplyPost!!.id
                    )

                },
                onClickSelectImages = {
                    if (storagePermission.status.isGranted) {
                        imagePickerLauncher.launch()
                    } else {
                        pendingAction = { imagePickerLauncher.launch() }
                        showPermissionDialog = true
                        storagePermission.launchPermissionRequest()
                    }
                },
                screenState = screenState,
                onRemoveImage = { item ->
                    screenState = screenState.copy(
                        images = screenState.images.filter { it.id != item.id }
                    )
                },
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        Strings.post_title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navigateBack() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(
                            vectorResource(Resources.Icon.BackArrow),
                            contentDescription = Strings.back
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = vectorResource(Resources.Icon.VerticalMenu),
                                contentDescription = Strings.more_options,
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = Alpha.DISABLED
                                )
                            ) // Set background for the menu
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = vectorResource(
                                            when (uiState.postWithComments.post.postType.name) {
                                                "RECOMMENDATION" -> Resources.Icon.Recommendation
                                                "REVIEW" -> Resources.Icon.Review
                                                else -> Resources.Icon.Quote
                                            }
                                        ),
                                        contentDescription = Strings.post_type,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                text = {
                                    Text(
                                        text = uiState.postWithComments.post.postType.name,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                onClick = {
                                    // Handle Pin action
                                    showMenu = false
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = Color.Gray.copy(alpha = Alpha.DISABLED)
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = vectorResource(Resources.Icon.Pin),
                                        contentDescription = Strings.pin_post,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                text = {
                                    Text(
                                        text = if (uiState.postWithComments.post.isPinned) Strings.unpin_post else Strings.pin_post,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                onClick = {
                                    // Handle Pin action
                                    showMenu = false
                                }
                            )
//                            DropdownMenuItem(
//                                leadingIcon = {
//                                    Icon(
//                                        imageVector = vectorResource(Mute),
//                                        contentDescription = "Mute",
//                                        tint = MaterialTheme.colorScheme.onPrimary
//                                    )
//                                },
//                                text = {
//                                    Text(
//                                        "Mute Author",
//                                        color = MaterialTheme.colorScheme.onPrimary
//                                    )
//                                },
//                                onClick = {
//                                    // Handle Mute action
//                                    showMenu = false
//                                }
//                            )


                            Spacer(modifier = Modifier.height(8.dp))
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = vectorResource(Delete),
                                        contentDescription = Strings.delete_label,
                                        tint = Red
                                    )
                                },
                                text = {
                                    Text(
                                        Strings.delete_label,
                                        color = Red
                                    )
                                }, // Typically delete is red
                                onClick = {
                                    // Handle Delete action
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ReplyBottomBar(
                onCommentClick = {
                    commentPost = uiState.postWithComments.post
                    showCommentSheet = true
                }
            )
        }
    ) { paddingValues ->
        // Show loading state while post details are being fetched
        if (uiState.isPostDetailsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = Strings.loading_post,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = Strings.fetching_post_details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    MainPostItem(
                        post = uiState.postWithComments.post,
                        onLikeClick = { id ->
                            println("Like clicked::${id}")
                            onAction(
                                DashboardAction.OnPostDetailsLikeClick(
                                    id
                                )
                            )
//                        viewModel.loadPostWithComments(postId)
                        },
                        onCommentClick = {
                            commentPost = uiState.postWithComments.post
                            showCommentSheet = true
                        },
                        onShareClick = {
                            val content =
                                uiState.postWithComments.post.whatsNew + "\n" + "\n" + "Source: " + uiState.postWithComments.post.source + "\n" + "Tags:" + uiState.postWithComments.post.tags.joinToString(
                                    ", "
                                )
                            shareManager.shareInviteLink(
                                inviteLink = content,
                                title = Strings.share_your_post
                            )
                            onAction(
                                DashboardAction.OnPostShareClick(
                                    uiState.postWithComments.post.id
                                )
                            )
                        },
                        isPostLiked = uiState.postWithComments.post.engagement.isLikedByCurrentUser
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 2.dp),
                        color = Color.DarkGray.copy(alpha = 0.3f)
                    )
                }
                val commentList = uiState.postWithComments.comments.sortedByDescending {
                    it.timestamp
                }
                items(commentList.size) { index ->
                    CommentItem(
                        comment = commentList[index],
                        post = uiState.postWithComments.post,
                        onClickCommentLike = { commentId ->
                            onAction(
                                DashboardAction.OnCommentLikeClick(
                                    commentId,
                                    postId
                                )
                            )
                        },
                        onCommentReply = { id ->
                            commentReplyPost = commentList[index]
                            showCommentReplySheet = true
                        },
                        onShareClick = {
                            shareManager.shareInviteLink(
                                inviteLink = commentList[index].commentText,
                                title = Strings.share_your_reply
                            )
                            onAction(
                                DashboardAction.OnPostShareClick(
                                    commentList[index].id
                                )
                            )
                        }
                    )
                    if (index < commentList.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color.DarkGray.copy(alpha = 0.15f)
                        )
                    }
                }
                item {
                    // Handle permission results
                    LaunchedEffect(cameraPermission.status, storagePermission.status) {
                        if (showPermissionDialog &&
                            (cameraPermission.status.isGranted || storagePermission.status.isGranted)
                        ) {
                            showPermissionDialog = false
                            pendingAction?.invoke()
                            pendingAction = null
                        }
                    }
                }


            }
        }
    }
}



@Composable
fun MainPostItem(
    post: Post,
    onLikeClick: (String) -> Unit,
    isPostLiked: Boolean,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(modifier = Modifier.padding(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatarContent = post.thumbnailUrl, name = post.authorName)
            Spacer(Modifier.width(12.dp))
            Column {
                Row {
                    Text(
                        normalizeUsername(post.authorName),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(4.dp))

                    if (post.authorIsVerified) {
                        Box(
                            contentAlignment = Alignment.BottomStart,
                            modifier = Modifier.size(32.dp).padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = vectorResource(Resources.Icon.VerifiedAccount),
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                    }
                }
                Text(text = post.timestamp, color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(Modifier.weight(1f))
//            TextButton(onClick = { /*TODO*/ }) {
//                Text(
//                    "Subscribe",
//                    color = MaterialTheme.colorScheme.onPrimary,
//                    fontWeight = FontWeight.Bold
//                )
//            }
        }
        Spacer(Modifier.height(8.dp))

        val normalizedTags = post.tags.map { normalizeUsername(it) }
        val content =
            post.whatsNew + "\n" + "\n" + "Source: " + post.source + "\n" + "Tags:" + normalizedTags.joinToString(
                ", "
            )

        Text(
            content
        )
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isPostLiked) vectorResource(Resources.Icon.LikeSelected) else vectorResource(
                    Resources.Icon.Like
                ),
                tint = if (isPostLiked) Color.Red else MaterialTheme.colorScheme.onSecondary,
                contentDescription = Strings.likes_cd,
                modifier = Modifier.size(20.dp).clickable { onLikeClick(post.id) }
            )
            Spacer(Modifier.width(20.dp))

            Icon(
                vectorResource(Resources.Icon.Comment),
                contentDescription = Strings.comments_cd,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp).clickable { onCommentClick() })

            Spacer(Modifier.width(20.dp))
            Icon(
                vectorResource(Resources.Icon.Share),
                contentDescription = Strings.share_cd,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp).clickable { onShareClick() }
            )
//            Spacer(Modifier.width(20.dp))
//            Icon(
//                imageVector = vectorResource(
//                    when (post.postType.name) {
//                        "RECOMMENDATION" -> Resources.Icon.Recommendation
//                        "REVIEW" -> Resources.Icon.Review
//                        else -> Resources.Icon.Quote
//                    }
//                ),
//                contentDescription = Strings.post_type,
//                tint = Color.Gray,
//                modifier = Modifier.size(20.dp)
//            )
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = Color.DarkGray.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                post.engagement.likesCount.toString() + Strings.likes_suffix,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                "•",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                post.engagement.commentsCount.toString() + Strings.comments_suffix,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                "•",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                post.engagement.sharesCount.toString() + Strings.shares_suffix,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    post: Post,
    onClickCommentLike: (String) -> Unit,
    onCommentReply: (String) -> Unit,
    onShareClick: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                avatarContent = comment.profileThumbnail,
                name = comment.fullname.toString()
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    comment.fullname.toString().ifEmpty { "" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(comment.timestamp, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
//            TextButton(onClick = { /*TODO*/ }) {
//                Text(
//                    "Subscribe",
//                    color = MaterialTheme.colorScheme.onPrimary,
//                    fontWeight = FontWeight.Bold
//                )
//            }
        }
        Spacer(Modifier.height(8.dp))
        FlipExpandableText(modifier = Modifier.padding(start = 28.dp), text = comment.commentText)
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            println("Comment like Count: ${comment.likesCount}")
            ActionIconWithText(
                icon = vectorResource(Resources.Icon.Like),
                count = comment.likesCount,
                isCommentLiked = comment.likesCount > 0,
                onClickIcon = { onClickCommentLike(comment.id) },
            )
            ActionIconWithText(
                icon = vectorResource(Resources.Icon.Comment),
                count = comment.repliesCount,
                isCommentLiked = false,
                onClickIcon = { onCommentReply(comment.id) },
            )
            Icon(
                vectorResource(Resources.Icon.Share),
                contentDescription = Strings.share_cd,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp).clickable { onShareClick() }
            )
            Icon(
                vectorResource(
                    when (post.postType.name) {
                        "RECOMMENDATION" -> Resources.Icon.Recommendation
                        "REVIEW" -> Resources.Icon.Review
                        else -> Resources.Icon.Quote
                    }
                ),
                contentDescription = Strings.post_type,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ReplyBottomBar(onCommentClick: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                avatarContent = getThumbnail(),
                name = "User"
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primaryContainer),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.clickable { onCommentClick() },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(Strings.leave_a_reply_placeholder, color = Color.Gray)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

// --- Helper Composables ---

@Composable
fun Avatar(avatarContent: String?, name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (avatarContent?.isNotEmpty() == true) {
            AsyncImage(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                model = ImageRequest.Builder(
                    LocalPlatformContext.current
                ).data(avatarContent)
                    .crossfade(enable = true)
                    .build(),
                contentDescription = "Author Profile URL",
                contentScale = ContentScale.Crop
            )

        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Alpha.HALF)) //  color for the circle
                    .border(2.dp, Color.Transparent, CircleShape), //  border color
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif
                )

            }
        }
    }
}

@Composable
fun ActionIconWithText(
    icon: ImageVector,
    count: Int,
    isCommentLiked: Boolean,
    onClickIcon: () -> Unit
) {
    Row(
        modifier = Modifier.clickable { onClickIcon() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isCommentLiked) vectorResource(Resources.Icon.LikeSelected) else icon,
            tint = if (isCommentLiked) Color.Red else MaterialTheme.colorScheme.onSecondary,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        if (count > 0) {
            Text(count.toString(), color = Color.Gray, fontSize = 14.sp)
        }
    }
}

