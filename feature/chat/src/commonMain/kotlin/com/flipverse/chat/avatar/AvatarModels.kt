package com.flipverse.chat.avatar

import kotlinx.serialization.Serializable

@Serializable
data class Avatar(
    val id: String,
    val url: String,
    val style: String,
    val seed: String,
    val customizations: AvatarCustomizations = AvatarCustomizations()
)

@Serializable
data class AvatarCustomizations(
    val accessories: String? = null,
    val accessoriesColor: String? = null,
    val top: String? = null,
    val hairColor: String? = null,
    val facialHair: String? = null,
    val facialHairColor: String? = null,
    val eyes: String? = null,
    val eyebrows: String? = null,
    val mouth: String? = null,
    val skinColor: String? = null,
    val clothing: String? = null,
    val clothesColor: String? = null,
    val clothingGraphic: String? = null,
    val nose: String? = null,
    val hatColor: String? = null
) {
    fun toQueryParams(style: AvatarStyle): String {
        val params = mutableListOf<String>()
        val customizationMap = style.customizationOptions.associateBy { it.key }

        accessories?.let { value ->
            customizationMap["accessories"]?.values?.find { it.value == value }?.let {
                params.add("accessories=${it.valueCode}")
            } ?: params.add("accessories=$value")
        }
        accessoriesColor?.let { params.add("accessoriesColor=$it") }
        top?.let { value ->
            customizationMap["top"]?.values?.find { it.value == value }?.let {
                params.add("top=${it.valueCode}")
            } ?: params.add("top=$value")
        }
        hairColor?.let { value ->
            customizationMap["hairColor"]?.values?.find { it.value == value }?.let {
                params.add("hairColor=${it.valueCode}")
            } ?: params.add("hairColor=$value")
        }
        facialHair?.let { value ->
            customizationMap["facialHair"]?.values?.find { it.value == value }?.let {
                params.add("facialHair=${it.valueCode}")
            } ?: params.add("facialHair=$value")
        }
        facialHairColor?.let { params.add("facialHairColor=$it") }
        eyes?.let { value ->
            customizationMap["eyes"]?.values?.find { it.value == value }?.let {
                params.add("eyes=${it.valueCode}")
            } ?: params.add("eyes=$value")
        }
        eyebrows?.let { params.add("eyebrows=$it") }
        mouth?.let { value ->
            customizationMap["mouth"]?.values?.find { it.value == value }?.let {
                params.add("mouth=${it.valueCode}")
            } ?: params.add("mouth=$value")
        }
        skinColor?.let { value ->
            customizationMap["skinColor"]?.values?.find { it.value == value }?.let {
                params.add("skinColor=${it.valueCode}")
            } ?: params.add("skinColor=$value")
        }
        clothing?.let { params.add("clothing=$it") }
        clothesColor?.let { params.add("clothesColor=$it") }
        clothingGraphic?.let { params.add("clothingGraphic=$it") }
        nose?.let { params.add("nose=$it") }
        hatColor?.let { params.add("hatColor=$it") }

        return if (params.isNotEmpty()) "&${params.joinToString("&")}" else ""
    }
}

@Serializable
data class AvatarCustomizationOption(
    val key: String,
    val displayName: String,
    val values: List<CustomizationValue>
)

@Serializable
data class CustomizationValue(
    val value: String,
    val displayName: String,
    val previewUrl: String? = null,
    val valueCode: String = value
)

@Serializable
data class AvatarStyle(
    val name: String,
    val displayName: String,
    val previewUrl: String,
    val description: String? = null,
    val customizationOptions: List<AvatarCustomizationOption> = emptyList()
) {
    companion object {
        val DEFAULT_STYLES = listOf(
            AvatarStyle(
                name = "avataaars",
                displayName = "Avataaars",
                previewUrl = "https://api.dicebear.com/8.x/avataaars/png?seed=preview1&size=100",
                customizationOptions = listOf(
                    AvatarCustomizationOption(
                        key = "accessories",
                        displayName = "Accessories",
                        values = listOf(
                            CustomizationValue("blank", "None", valueCode = "blank"),
                            CustomizationValue("kurt", "Kurt", valueCode = "kurt"),
                            CustomizationValue(
                                "prescription01",
                                "Glasses",
                                valueCode = "prescription01"
                            ),
                            CustomizationValue(
                                "prescription02",
                                "Glasses 2",
                                valueCode = "prescription02"
                            ),
                            CustomizationValue("round", "Round Glasses", valueCode = "round"),
                            CustomizationValue(
                                "sunglasses",
                                "Sunglasses",
                                valueCode = "sunglasses"
                            ),
                            CustomizationValue("wayfarers", "Wayfarers", valueCode = "wayfarers")
                        )
                    ),
                    AvatarCustomizationOption(
                        key = "top",
                        displayName = "Hair",
                        values = listOf(
                            CustomizationValue("noHair", "Bald", valueCode = "noHair"),
                            CustomizationValue("eyepatch", "Eyepatch", valueCode = "eyepatch"),
                            CustomizationValue("hat", "Hat", valueCode = "hat"),
                            CustomizationValue("hijab", "Hijab", valueCode = "hijab"),
                            CustomizationValue("turban", "Turban", valueCode = "turban"),
                            CustomizationValue(
                                "winterHat1",
                                "Winter Hat",
                                valueCode = "winterHat1"
                            ),
                            CustomizationValue(
                                "winterHat2",
                                "Winter Hat 2",
                                valueCode = "winterHat02"
                            ),
                            CustomizationValue(
                                "winterHat3",
                                "Winter Hat 3",
                                valueCode = "winterHat03"
                            ),
                            CustomizationValue(
                                "winterHat4",
                                "Winter Hat 4",
                                valueCode = "winterHat04"
                            ),
                            CustomizationValue(
                                "longHairBigHair",
                                "Long Big Hair",
                                valueCode = "bigHair"
                            ),
                            CustomizationValue("longHairBob", "Bob Cut", valueCode = "bob"),
                            CustomizationValue(
                                "longHairBun",
                                "Hair Bun",
                                valueCode = "bun"
                            ),
                            CustomizationValue(
                                "longHairCurly",
                                "Curly Hair",
                                valueCode = "curly"
                            ),
                            CustomizationValue(
                                "longHairCurvy",
                                "Curvy Hair",
                                valueCode = "curvy"
                            ),
                            CustomizationValue(
                                "longHairDreads",
                                "Dreadlocks",
                                valueCode = "dreads"
                            ),
                            CustomizationValue(
                                "longHairFrida",
                                "Frida Style",
                                valueCode = "frida"
                            ),
                            CustomizationValue("longHairFro", "Afro", valueCode = "fro"),
                            CustomizationValue(
                                "longHairFroBand",
                                "Afro with Band",
                                valueCode = "froBand"
                            ),
                            CustomizationValue(
                                "longHairNotTooLong",
                                "Medium Hair",
                                valueCode = "longButNotTooLong"
                            ),
                            CustomizationValue(
                                "longHairShavedSides",
                                "Shaved Sides",
                                valueCode = "shavedSides"
                            ),
                            CustomizationValue(
                                "longHairMiaWallace",
                                "Mia Wallace",
                                valueCode = "miaWallace"
                            ),
                            CustomizationValue(
                                "longHairStraight",
                                "Straight Hair",
                                valueCode = "straight01"
                            ),
                            CustomizationValue(
                                "longHairStraight2",
                                "Straight Hair 2",
                                valueCode = "straight02"
                            ),
                            CustomizationValue(
                                "longHairStraightStrand",
                                "Hair Strand",
                                valueCode = "straightAndStrand"
                            ),
                            CustomizationValue(
                                "shortHairDreads01",
                                "Short Dreads",
                                valueCode = "dreads01"
                            ),
                            CustomizationValue(
                                "shortHairDreads02",
                                "Short Dreads 2",
                                valueCode = "dreads02"
                            ),
                            CustomizationValue(
                                "shortHairFrizzle",
                                "Frizzled",
                                valueCode = "frizzle"
                            ),
                            CustomizationValue(
                                "shortHairShaggyMullet",
                                "Shaggy Mullet",
                                valueCode = "shaggyMullet"
                            ),
                            CustomizationValue(
                                "shortHairShortCurly",
                                "Short Curly",
                                valueCode = "shortCurly"
                            ),
                            CustomizationValue(
                                "shortHairShortFlat",
                                "Short Flat",
                                valueCode = "shortFlat"
                            ),
                            CustomizationValue(
                                "shortHairShortRound",
                                "Short Round",
                                valueCode = "shortRound"
                            ),
                            CustomizationValue(
                                "shortHairShortWaved",
                                "Short Waved",
                                valueCode = "shortWaved"
                            ),
                            CustomizationValue(
                                "shortHairSides",
                                "Short Sides",
                                valueCode = "sides"
                            ),
                            CustomizationValue(
                                "shortHairTheCaesar",
                                "Caesar Cut",
                                valueCode = "theCaesar"
                            ),
                            CustomizationValue(
                                "shortHairTheCaesarSidePart",
                                "Caesar Side Part",
                                valueCode = "theCaesarAndSidePart"
                            )
                        )
                    ),
                    AvatarCustomizationOption(
                        key = "hairColor",
                        displayName = "Hair Color",
                        values = listOf(
                            CustomizationValue("auburn", "Auburn", valueCode = "a55728"),
                            CustomizationValue("black", "Black", valueCode = "2c1b18"),
                            CustomizationValue("blonde", "Blonde", valueCode = "d6b370"),
                            CustomizationValue(
                                "blondeGolden",
                                "Golden Blonde",
                                valueCode = "b58143"
                            ),
                            CustomizationValue("brown", "Brown", valueCode = "724133"),
                            CustomizationValue("brownDark", "Dark Brown", valueCode = "4a312c"),
                            CustomizationValue(
                                "pastelPink",
                                "Pastel Pink",
                                valueCode = "f59797"
                            ),
                            CustomizationValue("blue", "Blue", valueCode = "65c9ff"),
                            CustomizationValue("platinum", "Platinum", valueCode = "ecdcbf"),
                            CustomizationValue("red", "Red", valueCode = "c93305"),
                            CustomizationValue(
                                "silverGray",
                                "Silver Gray",
                                valueCode = "e8e1e1"
                            )
                        )
                    ),
                    AvatarCustomizationOption(
                        key = "facialHair",
                        displayName = "Facial Hair",
                        values = listOf(
                            CustomizationValue("blank", "None", valueCode = "blank"),
                            CustomizationValue(
                                "beardMedium",
                                "Medium Beard",
                                valueCode = "beardMedium"
                            ),
                            CustomizationValue(
                                "beardLight",
                                "Light Beard",
                                valueCode = "beardLight"
                            ),
                            CustomizationValue(
                                "beardMagestic",
                                "Majestic Beard",
                                valueCode = "beardMagestic"
                            ),
                            CustomizationValue(
                                "moustacheFancy",
                                "Fancy Moustache",
                                valueCode = "moustacheFancy"
                            ),
                            CustomizationValue(
                                "moustacheMagnum",
                                "Magnum Moustache",
                                valueCode = "moustacheMagnum"
                            )
                        )
                    ),
                    AvatarCustomizationOption(
                        key = "eyes",
                        displayName = "Eyes",
                        values = listOf(
                            CustomizationValue("close", "Closed", valueCode = "close"),
                            CustomizationValue("cry", "Crying", valueCode = "cry"),
                            CustomizationValue("default", "Default", valueCode = "default"),
                            CustomizationValue("dizzy", "Dizzy", valueCode = "dizzy"),
                            CustomizationValue("eyeRoll", "Eye Roll", valueCode = "eyeRoll"),
                            CustomizationValue("happy", "Happy", valueCode = "happy"),
                            CustomizationValue("hearts", "Hearts", valueCode = "hearts"),
                            CustomizationValue("side", "Side", valueCode = "side"),
                            CustomizationValue("squint", "Squint", valueCode = "squint"),
                            CustomizationValue("surprised", "Surprised", valueCode = "surprised"),
                            CustomizationValue("wink", "Wink", valueCode = "wink"),
                            CustomizationValue("winkWacky", "Wacky Wink", valueCode = "winkWacky")
                        )
                    )
                )
            ),
            AvatarStyle(
                name = "adventurer",
                displayName = "Adventurer",
                previewUrl = "https://api.dicebear.com/8.x/adventurer/png?seed=preview2&size=100",
                customizationOptions = listOf(
                    AvatarCustomizationOption(
                        key = "hair",
                        displayName = "Hair",
                        values = listOf(
                            CustomizationValue("short01", "Short 1", valueCode = "short01"),
                            CustomizationValue("short02", "Short 2", valueCode = "short02"),
                            CustomizationValue("short03", "Short 3", valueCode = "short03"),
                            CustomizationValue("short04", "Short 4", valueCode = "short04"),
                            CustomizationValue("short05", "Short 5", valueCode = "short05"),
                            CustomizationValue("short06", "Short 6", valueCode = "short06"),
                            CustomizationValue("short07", "Short 7", valueCode = "short07"),
                            CustomizationValue("short08", "Short 8", valueCode = "short08"),
                            CustomizationValue("short09", "Short 9", valueCode = "short09"),
                            CustomizationValue("short10", "Short 10", valueCode = "short10"),
                            CustomizationValue("short11", "Short 11", valueCode = "short11"),
                            CustomizationValue("short12", "Short 12", valueCode = "short12"),
                            CustomizationValue("short13", "Short 13", valueCode = "short13"),
                            CustomizationValue("short14", "Short 14", valueCode = "short14"),
                            CustomizationValue("short15", "Short 15", valueCode = "short15"),
                            CustomizationValue("short16", "Short 16", valueCode = "short16"),
                            CustomizationValue("short17", "Short 17", valueCode = "short17"),
                            CustomizationValue("short18", "Short 18", valueCode = "short18"),
                            CustomizationValue("long01", "Long 1", valueCode = "long01"),
                            CustomizationValue("long02", "Long 2", valueCode = "long02"),
                            CustomizationValue("long03", "Long 3", valueCode = "long03"),
                            CustomizationValue("long04", "Long 4", valueCode = "long04"),
                            CustomizationValue("long05", "Long 5", valueCode = "long05"),
                            CustomizationValue("long06", "Long 6", valueCode = "long06"),
                            CustomizationValue("long07", "Long 7", valueCode = "long07"),
                            CustomizationValue("long08", "Long 8", valueCode = "long08"),
                            CustomizationValue("long09", "Long 9", valueCode = "long09"),
                            CustomizationValue("long10", "Long 10", valueCode = "long10")
                        )
                    ),
                    AvatarCustomizationOption(
                        key = "hairColor",
                        displayName = "Hair Color",
                        values = listOf(
                            CustomizationValue("0e0e0e", "Black", valueCode = "0e0e0e"),
                            CustomizationValue("3eac2c", "Green", valueCode = "3eac2c"),
                            CustomizationValue("6a4e35", "Brown", valueCode = "6a4e35"),
                            CustomizationValue("81583e", "Light Brown", valueCode = "81583e"),
                            CustomizationValue("a56b43", "Auburn", valueCode = "a56b43"),
                            CustomizationValue("b58143", "Golden Brown", valueCode = "b58143"),
                            CustomizationValue("d6b370", "Blonde", valueCode = "d6b370"),
                            CustomizationValue("e6dba7", "Light Blonde", valueCode = "e6dba7"),
                            CustomizationValue("c93305", "Red", valueCode = "c93305"),
                            CustomizationValue("2c1b18", "Dark Brown", valueCode = "2c1b18"),
                            CustomizationValue("b89778", "Light Auburn", valueCode = "b89778")
                        )
                    ),
                    AvatarCustomizationOption(
                        key = "eyes",
                        displayName = "Eyes",
                        values = listOf(
                            CustomizationValue("variant01", "Variant 1", valueCode = "variant01"),
                            CustomizationValue("variant02", "Variant 2", valueCode = "variant02"),
                            CustomizationValue("variant03", "Variant 3", valueCode = "variant03"),
                            CustomizationValue("variant04", "Variant 4", valueCode = "variant04"),
                            CustomizationValue("variant05", "Variant 5", valueCode = "variant05"),
                            CustomizationValue("variant06", "Variant 6", valueCode = "variant06"),
                            CustomizationValue("variant07", "Variant 7", valueCode = "variant07"),
                            CustomizationValue("variant08", "Variant 8", valueCode = "variant08"),
                            CustomizationValue("variant09", "Variant 9", valueCode = "variant09"),
                            CustomizationValue("variant10", "Variant 10", valueCode = "variant10"),
                            CustomizationValue("variant11", "Variant 11", valueCode = "variant11"),
                            CustomizationValue("variant12", "Variant 12", valueCode = "variant12"),
                            CustomizationValue("variant13", "Variant 13", valueCode = "variant13"),
                            CustomizationValue("variant14", "Variant 14", valueCode = "variant14"),
                            CustomizationValue("variant15", "Variant 15", valueCode = "variant15"),
                            CustomizationValue("variant16", "Variant 16", valueCode = "variant16"),
                            CustomizationValue("variant17", "Variant 17", valueCode = "variant17"),
                            CustomizationValue("variant18", "Variant 18", valueCode = "variant18"),
                            CustomizationValue("variant19", "Variant 19", valueCode = "variant19"),
                            CustomizationValue("variant20", "Variant 20", valueCode = "variant20"),
                            CustomizationValue("variant21", "Variant 21", valueCode = "variant21"),
                            CustomizationValue("variant22", "Variant 22", valueCode = "variant22"),
                            CustomizationValue("variant23", "Variant 23", valueCode = "variant23"),
                            CustomizationValue("variant24", "Variant 24", valueCode = "variant24"),
                            CustomizationValue("variant25", "Variant 25", valueCode = "variant25"),
                            CustomizationValue("variant26", "Variant 26", valueCode = "variant26")
                        )
                    ),
                    AvatarCustomizationOption(
                        key = "mouth",
                        displayName = "Mouth",
                        values = listOf(
                            CustomizationValue("variant01", "Variant 1", valueCode = "variant01"),
                            CustomizationValue("variant02", "Variant 2", valueCode = "variant02"),
                            CustomizationValue("variant03", "Variant 3", valueCode = "variant03"),
                            CustomizationValue("variant04", "Variant 4", valueCode = "variant04"),
                            CustomizationValue("variant05", "Variant 5", valueCode = "variant05"),
                            CustomizationValue("variant06", "Variant 6", valueCode = "variant06"),
                            CustomizationValue("variant07", "Variant 7", valueCode = "variant07"),
                            CustomizationValue("variant08", "Variant 8", valueCode = "variant08"),
                            CustomizationValue("variant09", "Variant 9", valueCode = "variant09"),
                            CustomizationValue("variant10", "Variant 10", valueCode = "variant10"),
                            CustomizationValue("variant11", "Variant 11", valueCode = "variant11"),
                            CustomizationValue("variant12", "Variant 12", valueCode = "variant12"),
                            CustomizationValue("variant13", "Variant 13", valueCode = "variant13"),
                            CustomizationValue("variant14", "Variant 14", valueCode = "variant14"),
                            CustomizationValue("variant15", "Variant 15", valueCode = "variant15"),
                            CustomizationValue("variant16", "Variant 16", valueCode = "variant16"),
                            CustomizationValue("variant17", "Variant 17", valueCode = "variant17"),
                            CustomizationValue("variant18", "Variant 18", valueCode = "variant18"),
                            CustomizationValue("variant19", "Variant 19", valueCode = "variant19"),
                            CustomizationValue("variant20", "Variant 20", valueCode = "variant20"),
                            CustomizationValue("variant21", "Variant 21", valueCode = "variant21"),
                            CustomizationValue("variant22", "Variant 22", valueCode = "variant22"),
                            CustomizationValue("variant23", "Variant 23", valueCode = "variant23"),
                            CustomizationValue("variant24", "Variant 24", valueCode = "variant24"),
                            CustomizationValue("variant25", "Variant 25", valueCode = "variant25"),
                            CustomizationValue("variant26", "Variant 26", valueCode = "variant26"),
                            CustomizationValue("variant27", "Variant 27", valueCode = "variant27"),
                            CustomizationValue("variant28", "Variant 28", valueCode = "variant28"),
                            CustomizationValue("variant29", "Variant 29", valueCode = "variant29"),
                            CustomizationValue("variant30", "Variant 30", valueCode = "variant30")
                        )
                    ),
                    AvatarCustomizationOption(
                        key = "skinColor",
                        displayName = "Skin Color",
                        values = listOf(
                            CustomizationValue("f3d7a7", "Light", valueCode = "f3d7a7"),
                            CustomizationValue("e4a853", "Medium Light", valueCode = "e4a853"),
                            CustomizationValue("ba7f5c", "Medium", valueCode = "ba7f5c"),
                            CustomizationValue("a0583a", "Medium Dark", valueCode = "a0583a"),
                            CustomizationValue("6b3e2c", "Dark", valueCode = "6b3e2c")
                        )
                    )
                )
            ),
            AvatarStyle(
                name = "big-smile",
                displayName = "Big Smile",
                previewUrl = "https://api.dicebear.com/8.x/big-smile/png?seed=preview3&size=100"
            ),
            AvatarStyle(
                name = "bottts",
                displayName = "Bottts",
                previewUrl = "https://api.dicebear.com/8.x/bottts/png?seed=preview4&size=100"
            ),
            AvatarStyle(
                name = "croodles",
                displayName = "Croodles",
                previewUrl = "https://api.dicebear.com/8.x/croodles/png?seed=preview5&size=100"
            ),
            AvatarStyle(
                name = "fun-emoji",
                displayName = "Fun Emoji",
                previewUrl = "https://api.dicebear.com/8.x/fun-emoji/png?seed=preview6&size=100"
            ),
            AvatarStyle(
                name = "lorelei",
                displayName = "Lorelei",
                previewUrl = "https://api.dicebear.com/8.x/lorelei/png?seed=preview7&size=100"
            ),
            AvatarStyle(
                name = "micah",
                displayName = "Micah",
                previewUrl = "https://api.dicebear.com/8.x/micah/png?seed=preview8&size=100"
            ),
            AvatarStyle(
                name = "open-peeps",
                displayName = "Open Peeps",
                previewUrl = "https://api.dicebear.com/8.x/open-peeps/png?seed=preview9&size=100"
            )
        )
    }
}

data class AvatarSelectionState(
    val avatars: List<Avatar> = emptyList(),
    val availableStyles: List<AvatarStyle> = AvatarStyle.DEFAULT_STYLES,
    val selectedStyle: AvatarStyle = AvatarStyle.DEFAULT_STYLES.first(),
    val selectedAvatarUrl: String = "",
    val selectedAvatar: Avatar? = null,
    val customizations: AvatarCustomizations = AvatarCustomizations(),
    val showCustomization: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)