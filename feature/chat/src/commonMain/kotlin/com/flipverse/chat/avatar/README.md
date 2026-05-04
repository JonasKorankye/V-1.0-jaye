# Avatar Selection Feature

This package provides a complete avatar selection system using DiceBear avatars, allowing users to
browse and customize their preferred avatars from various styles.

## Features

- ✅ **Multiple Avatar Styles**: Support for 9 different DiceBear avatar styles including Avataaars,
  Adventurer, Big Smile, Bottts, Croodles, Fun Emoji, Lorelei, Micah, and Open Peeps
- ✅ **Dynamic Avatar Generation**: Generates 12 unique avatars per style using random seeds
- ✅ **Real-time Preview**: Shows current avatar and selected avatar in real-time
- ✅ **Style Switching**: Easy switching between different avatar styles
- ✅ **Regeneration**: Ability to regenerate new avatars for the current style
- ✅ **Clean UI**: Modern Material 3 design with smooth animations
- ✅ **Repository Pattern**: Clean architecture with repository pattern for data management
- ✅ **Dependency Injection**: Fully integrated with Koin DI system

## Components

### Data Classes

#### `Avatar`

```kotlin
@Serializable
data class Avatar(
    val id: String,
    val url: String,
    val style: String,
    val seed: String
)
```

Represents a single avatar with its unique ID, DiceBear URL, style name, and seed.

#### `AvatarStyle`

```kotlin
@Serializable  
data class AvatarStyle(
    val name: String,
    val displayName: String,
    val previewUrl: String,
    val description: String? = null
)
```

Represents an avatar style with default styles provided in the companion object.

#### `AvatarSelectionState`

```kotlin
data class AvatarSelectionState(
    val avatars: List<Avatar> = emptyList(),
    val availableStyles: List<AvatarStyle> = AvatarStyle.DEFAULT_STYLES,
    val selectedStyle: AvatarStyle = AvatarStyle.DEFAULT_STYLES.first(),
    val selectedAvatarUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
```

Manages the UI state for the avatar selection screen.

### Repository

#### `AvatarRepository`

Interface defining avatar-related operations:

- `generateAvatars()`: Generate avatars for a specific style
- `getAvatarStyles()`: Get available avatar styles
- `saveUserAvatar()`: Save user's selected avatar
- `getUserAvatarFlow()`: Get user's avatar as a Flow

#### `AvatarRepositoryImpl`

Implementation that generates DiceBear avatar URLs using the pattern:

```
https://api.dicebear.com/8.x/{style}/svg?seed={seed}&size=200
```

### ViewModel

#### `AvatarSelectionViewModel`

Manages the business logic and state for avatar selection:

- Loads available avatar styles
- Generates avatars for selected style
- Handles style changes and avatar selection
- Manages loading and error states

### UI Components

#### `AvatarSelectionScreen`

Main composable screen with:

- **Top Bar**: Back navigation and regenerate action
- **Current Avatar Preview**: Shows selected or existing avatar
- **Style Selection Grid**: 3-column grid of available styles
- **Avatar Grid**: 3-column grid of generated avatars
- **Confirm Button**: Appears when avatar is selected

#### `AvatarStyleCard`

Displays individual avatar style with preview and name.

#### `AvatarItem`

Displays individual avatar option with selection state.

## Usage

### 1. Basic Implementation

```kotlin
@Composable
fun MyScreen() {
    AvatarSelectionScreen(
        onBackPressed = { /* Handle back navigation */ },
        onAvatarSelected = { avatarUrl ->
            // Handle avatar selection
            println("Selected avatar: $avatarUrl")
        },
        currentAvatarUrl = userCurrentAvatar // Optional
    )
}
```

### 2. Navigation Integration

Add to your navigation routes:

```kotlin
// In ChatRoutes.kt
@Serializable
data class ChatAvatarSelection(val currentAvatarUrl: String? = null) : ChatRoutes
```

### 3. Dependency Injection

Already configured in `KoinModule.kt`:

```kotlin
single<AvatarRepository> { AvatarRepositoryImpl() }
viewModelOf(::AvatarSelectionViewModel)
```

### 4. Custom Avatar Styles

Extend the default styles by modifying `AvatarStyle.DEFAULT_STYLES`:

```kotlin
val CUSTOM_STYLES = AvatarStyle.DEFAULT_STYLES + listOf(
    AvatarStyle(
        name = "pixel-art",
        displayName = "Pixel Art", 
        previewUrl = "https://api.dicebear.com/8.x/pixel-art/svg?seed=custom"
    )
)
```

## Available Avatar Styles

1. **Avataaars** - Popular avatar style by Pablo Stanley
2. **Adventurer** - Adventure-themed characters by Lisa Wischofsky
3. **Big Smile** - Happy, smiling faces by Ashley Seo
4. **Bottts** - Robot avatars by Pablo Stanley
5. **Croodles** - Doodle-style avatars by vijay verma
6. **Fun Emoji** - Emoji-style avatars by Davis Uche
7. **Lorelei** - Feminine avatars by Lisa Wischofsky
8. **Micah** - Diverse human avatars by Micah Lanier
9. **Open Peeps** - Hand-drawn style by Pablo Stanley

## Error Handling

The system includes comprehensive error handling:

- Network errors when loading avatars
- Invalid avatar URLs
- Style loading failures
- Graceful fallbacks to default states

## Customization Options

### Colors and Theming

The UI automatically adapts to Material 3 theming. Key colors used:

- `MaterialTheme.colorScheme.primary` - Selection indicators
- `MaterialTheme.colorScheme.primaryContainer` - Selected style background
- `MaterialTheme.colorScheme.surfaceVariant` - Default backgrounds

### Avatar Generation

Customize avatar generation by:

- Modifying seed generation logic in `AvatarRepositoryImpl`
- Changing avatar count (default: 12 per style)
- Adding URL parameters for avatar customization

### Repository Implementation

Replace `AvatarRepositoryImpl` with your own implementation for:

- Different avatar providers
- Local avatar storage
- Custom avatar generation logic
- Backend integration

## Integration with User Profiles

To integrate with user profiles:

1. **Save Avatar**: Call `viewModel.saveAvatar(userId, avatarUrl)` after selection
2. **Load Current Avatar**: Pass current avatar URL to the screen
3. **Update User Model**: Extend User data class if needed

```kotlin
// Example integration
fun onAvatarSelected(avatarUrl: String) {
    // Save to user repository
    userRepository.updateUserAvatar(userId, avatarUrl)
    
    // Navigate back or show success
    navigateBack()
}
```

## Testing

The modular architecture makes testing straightforward:

```kotlin
class AvatarSelectionViewModelTest {
    @Test
    fun `should generate avatars on init`() {
        // Test avatar generation
    }
    
    @Test
    fun `should handle style changes`() {
        // Test style switching
    }
}
```

## Future Enhancements

Potential improvements:

- Avatar favoriting/bookmarking
- Custom avatar upload
- Avatar history
- Social avatar sharing
- Advanced filtering options
- Offline avatar caching
- Avatar animation previews

## Dependencies

- **Coil3**: Image loading and caching
- **Koin**: Dependency injection
- **Kotlinx Serialization**: Data serialization
- **Material 3**: UI components
- **Compose Navigation**: Screen navigation

## License

This implementation uses the DiceBear API which provides free avatar generation.
Check [DiceBear's terms](https://dicebear.com/) for usage guidelines.