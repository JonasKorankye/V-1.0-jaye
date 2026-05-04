package com.flipverse.shared

import flipverse.shared.generated.resources.Res
import flipverse.shared.generated.resources.add_image_icon
import flipverse.shared.generated.resources.app_icon_dark
import flipverse.shared.generated.resources.app_icon_full_dark
import flipverse.shared.generated.resources.app_icon_full_outline_dark
import flipverse.shared.generated.resources.app_icon_full_outline_white
import flipverse.shared.generated.resources.app_icon_full_white
import flipverse.shared.generated.resources.app_icon_outline_dark
import flipverse.shared.generated.resources.app_icon_outline_white
import flipverse.shared.generated.resources.app_icon_white
import flipverse.shared.generated.resources.app_logo_white
import flipverse.shared.generated.resources.appearance
import flipverse.shared.generated.resources.apple_black
import flipverse.shared.generated.resources.apple_logo_black
import flipverse.shared.generated.resources.apple_logo_white
import flipverse.shared.generated.resources.apple_white
import flipverse.shared.generated.resources.arrow
import flipverse.shared.generated.resources.arrow_down
import flipverse.shared.generated.resources.arrow_right
import flipverse.shared.generated.resources.auth_screen_image
import flipverse.shared.generated.resources.back_arrow
import flipverse.shared.generated.resources.bg_message_dark
import flipverse.shared.generated.resources.bg_message_light
import flipverse.shared.generated.resources.book
import flipverse.shared.generated.resources.book_closed
import flipverse.shared.generated.resources.bookmark
import flipverse.shared.generated.resources.bulb
import flipverse.shared.generated.resources.chat_icon_outlined
import flipverse.shared.generated.resources.check
import flipverse.shared.generated.resources.check_circle
import flipverse.shared.generated.resources.close
import flipverse.shared.generated.resources.contact
import flipverse.shared.generated.resources.contemporary
import flipverse.shared.generated.resources.contribute
import flipverse.shared.generated.resources.creative_non_fiction
import flipverse.shared.generated.resources.crowd_favorite
import flipverse.shared.generated.resources.delete
import flipverse.shared.generated.resources.edit
import flipverse.shared.generated.resources.edit_document
import flipverse.shared.generated.resources.email
import flipverse.shared.generated.resources.fantasy_supernatural
import flipverse.shared.generated.resources.fast_fingers
import flipverse.shared.generated.resources.feedback
import flipverse.shared.generated.resources.filled_bookmark
import flipverse.shared.generated.resources.flip_chat
import flipverse.shared.generated.resources.flip_explore
import flipverse.shared.generated.resources.flip_home
import flipverse.shared.generated.resources.flip_livebook
import flipverse.shared.generated.resources.flip_notify
import flipverse.shared.generated.resources.flipverse
import flipverse.shared.generated.resources.folklore_mythic_fiction
import flipverse.shared.generated.resources.genre_jumper
import flipverse.shared.generated.resources.google_logo
import flipverse.shared.generated.resources.grid
import flipverse.shared.generated.resources.historical_fiction
import flipverse.shared.generated.resources.holy_bible
import flipverse.shared.generated.resources.home
import flipverse.shared.generated.resources.horizontal_menu
import flipverse.shared.generated.resources.humor
import flipverse.shared.generated.resources.ic_add
import flipverse.shared.generated.resources.ic_bell
import flipverse.shared.generated.resources.ic_camera
import flipverse.shared.generated.resources.ic_filled_contacts
import flipverse.shared.generated.resources.ic_filled_person
import flipverse.shared.generated.resources.ic_filled_person_dark
import flipverse.shared.generated.resources.ic_notification
import flipverse.shared.generated.resources.ic_notifications
import flipverse.shared.generated.resources.ic_outline_cancel
import flipverse.shared.generated.resources.ic_person
import flipverse.shared.generated.resources.ic_privacy
import flipverse.shared.generated.resources.ic_refresh
import flipverse.shared.generated.resources.ic_send_message
import flipverse.shared.generated.resources.ic_send_message_rotated
import flipverse.shared.generated.resources.indie_author
import flipverse.shared.generated.resources.invite
import flipverse.shared.generated.resources.latest_post
import flipverse.shared.generated.resources.like
import flipverse.shared.generated.resources.like_icon_filled
import flipverse.shared.generated.resources.like_icon_outlined
import flipverse.shared.generated.resources.literary_fiction
import flipverse.shared.generated.resources.log_in
import flipverse.shared.generated.resources.log_out
import flipverse.shared.generated.resources.login_image
import flipverse.shared.generated.resources.lv_slide_four
import flipverse.shared.generated.resources.lv_slide_one
import flipverse.shared.generated.resources.lv_slide_three
import flipverse.shared.generated.resources.lv_slide_two
import flipverse.shared.generated.resources.map_pin
import flipverse.shared.generated.resources.menu
import flipverse.shared.generated.resources.mute
import flipverse.shared.generated.resources.next
import flipverse.shared.generated.resources.outline_bookmark
import flipverse.shared.generated.resources.paypal_logo
import flipverse.shared.generated.resources.pen_slinger
import flipverse.shared.generated.resources.person_circle_icon
import flipverse.shared.generated.resources.pin
import flipverse.shared.generated.resources.post_a
import flipverse.shared.generated.resources.post_b
import flipverse.shared.generated.resources.post_c
import flipverse.shared.generated.resources.quote
import flipverse.shared.generated.resources.quotes
import flipverse.shared.generated.resources.recommend
import flipverse.shared.generated.resources.recommendation
import flipverse.shared.generated.resources.remove
import flipverse.shared.generated.resources.repost
import flipverse.shared.generated.resources.review
import flipverse.shared.generated.resources.ribbon_badge
import flipverse.shared.generated.resources.right_arrow
import flipverse.shared.generated.resources.romance
import flipverse.shared.generated.resources.school
import flipverse.shared.generated.resources.science_fiction
import flipverse.shared.generated.resources.search
import flipverse.shared.generated.resources.settings
import flipverse.shared.generated.resources.share
import flipverse.shared.generated.resources.shopping_cart
import flipverse.shared.generated.resources.slide_four
import flipverse.shared.generated.resources.slide_one
import flipverse.shared.generated.resources.slide_three
import flipverse.shared.generated.resources.slide_two
import flipverse.shared.generated.resources.stars
import flipverse.shared.generated.resources.story_master
import flipverse.shared.generated.resources.support
import flipverse.shared.generated.resources.thriller_suspense
import flipverse.shared.generated.resources.trophy_badge
import flipverse.shared.generated.resources.unlock
import flipverse.shared.generated.resources.verified_account
import flipverse.shared.generated.resources.verified_user
import flipverse.shared.generated.resources.vertical_menu
import flipverse.shared.generated.resources.virtual_assistant
import flipverse.shared.generated.resources.visibility_off
import flipverse.shared.generated.resources.visibility_on
import flipverse.shared.generated.resources.warning
import flipverse.shared.generated.resources.welcome_image
import flipverse.shared.generated.resources.wp_background
import flipverse.shared.generated.resources.wp_background_light
import flipverse.shared.generated.resources.wp_dark
import flipverse.shared.generated.resources.young_adult
import io.ktor.http.CacheControl

object Resources {

    object Icon {
        val SignIn = Res.drawable.log_in
        val SignOut = Res.drawable.log_out
        val Unlock = Res.drawable.unlock
        val ShoppingCart = Res.drawable.shopping_cart
        val Remove = Res.drawable.remove
        val Search = Res.drawable.search
        val Person = Res.drawable.ic_filled_person
        val Invite = Res.drawable.invite
        val Contact = Res.drawable.contact
        val PersonDark = Res.drawable.ic_filled_person_dark
        val Account = Res.drawable.ic_person
        val Privacy = Res.drawable.ic_privacy
        val Checkmark = Res.drawable.check
        val Notification = Res.drawable.ic_notifications
        val BookClosed = Res.drawable.book_closed
        val VisibilityOn = Res.drawable.visibility_on
        val VisibilityOff = Res.drawable.visibility_off
        val Bookmark = Res.drawable.bookmark
        val EditDocument = Res.drawable.edit_document
        val Bible = Res.drawable.holy_bible
        val School = Res.drawable.school
        val Edit = Res.drawable.edit
        val Menu = Res.drawable.menu
        val FlipChat = Res.drawable.flip_chat
        val FlipExplore = Res.drawable.flip_explore
        val FlipHome = Res.drawable.flip_home
        val FlipLiveBook = Res.drawable.flip_livebook
        val FlipNotify = Res.drawable.flip_notify
        val Add = Res.drawable.ic_add
        val BackArrow = Res.drawable.back_arrow
        val RightArrow = Res.drawable.right_arrow
        val Home = Res.drawable.home
        val Categories = Res.drawable.grid
        val MapPin = Res.drawable.map_pin
        val Close = Res.drawable.close
        val Settings = Res.drawable.settings
        val Check = Res.drawable.check
        val Book = Res.drawable.book
        val VerticalMenu = Res.drawable.vertical_menu
        val Delete = Res.drawable.delete
        val Warning = Res.drawable.warning
        val Like = Res.drawable.like_icon_outlined
        val LikeSelected = Res.drawable.like_icon_filled
        val Comment = Res.drawable.chat_icon_outlined
        val Repost = Res.drawable.repost
        val Share = Res.drawable.share
        val Recommendation = Res.drawable.recommend
        val Quote = Res.drawable.quotes
        val Review = Res.drawable.stars
        val ArrowRight = Res.drawable.arrow_right
        val Feedback = Res.drawable.feedback
        val Support = Res.drawable.support
        val Theme = Res.drawable.appearance
        val VerifiedUser = Res.drawable.verified_user
        val Arrow = Res.drawable.arrow
        val Pin = Res.drawable.pin
        val Mute = Res.drawable.mute
        val NotificationBell = Res.drawable.ic_bell
        val SendMessageFlat = Res.drawable.ic_send_message
        val SendMessageTilted = Res.drawable.ic_send_message_rotated
        val LatestPost = Res.drawable.latest_post
        val VerifiedAccount = Res.drawable.verified_account
        val HorizontalMenu = Res.drawable.horizontal_menu
        val BookmarkOutline = Res.drawable.outline_bookmark
        val BookmarkFilled = Res.drawable.filled_bookmark
        val CreatePostImage = Res.drawable.add_image_icon
        val Camera = Res.drawable.ic_camera
        val Refresh = Res.drawable.ic_refresh
        val Email = Res.drawable.email
        val HelpCenter = Res.drawable.virtual_assistant
        val sent = Res.drawable.check_circle
        val CancelOutline = Res.drawable.ic_outline_cancel
        val ArrowDown = Res.drawable.arrow_down
        val TrophyBadge = Res.drawable.trophy_badge
        val RibbonBadge = Res.drawable.ribbon_badge
        val YoungAdult = Res.drawable.young_adult
        val ThrillerSuspense = Res.drawable.thriller_suspense
        val StoryMaster = Res.drawable.story_master
        val ScienceFiction = Res.drawable.science_fiction
        val Romance = Res.drawable.romance
        val PenSlinger = Res.drawable.pen_slinger
        val LiteraryFiction = Res.drawable.literary_fiction
        val IndieAuthor = Res.drawable.indie_author
        val Humor = Res.drawable.humor
        val HistoricalFiction = Res.drawable.historical_fiction
        val GenreJumper = Res.drawable.genre_jumper
        val FolkloreMythicFiction = Res.drawable.folklore_mythic_fiction
        val FastFingers = Res.drawable.fast_fingers
        val FantasySupernatural = Res.drawable.fantasy_supernatural
        val CrowdFavorite = Res.drawable.crowd_favorite
        val CreativeNonFiction = Res.drawable.creative_non_fiction
        val Contemporary = Res.drawable.contemporary
        val Contribute = Res.drawable.contribute
        val Next = Res.drawable.next
        val ToolTip = Res.drawable.bulb
        val SlideOne = Res.drawable.slide_one
        val SlideTwo = Res.drawable.slide_two
        val SlideThree = Res.drawable.slide_three
        val SLideFour = Res.drawable.slide_four
    }

    object Image {
        val Google = Res.drawable.google_logo
        val AppleBlack = Res.drawable.apple_black
        val AppleWhite = Res.drawable.apple_white
        val PayPal = Res.drawable.paypal_logo
        val MessageBackgroundDark = Res.drawable.bg_message_dark
        val ChatBackgroundDark = Res.drawable.wp_dark
        val ChatBackground = Res.drawable.wp_background
        val MessageBackgroundLight = Res.drawable.bg_message_light
        val FlipVerseLogoOfficial = Res.drawable.app_logo_white
        val AppLogoFullWhite = Res.drawable.app_icon_full_white
        val AppLogoFullDark = Res.drawable.app_icon_full_dark
        val AppLogoFullOutlineWhite = Res.drawable.app_icon_full_outline_white
        val AppLogoFullOutlineDark = Res.drawable.app_icon_full_outline_dark
        val AppLogoWhite = Res.drawable.app_icon_white
        val AppLogoDark = Res.drawable.app_icon_dark
        val AppLogoOutlineWhite = Res.drawable.app_icon_outline_white
        val AppLogoOutlineDark = Res.drawable.app_icon_outline_dark
        val WelcomeImage = Res.drawable.welcome_image
        val LoginImage = Res.drawable.auth_screen_image
        val PostA = Res.drawable.post_a
        val PostB = Res.drawable.post_b
        val PostC = Res.drawable.post_c
    }
}