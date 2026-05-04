package com.flipverse.data.util

data class ContentVettingResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Vets the story continuation for quality, relevance, and spam detection
 * @param continuation The story continuation text to validate
 * @param existingStory The existing story content for context checking
 * @return ContentVettingResult indicating if content is valid and any error message
 */
fun vetStoryContinuation(continuation: String, existingStory: String = ""): ContentVettingResult {
    val trimmedContent = continuation.trim()
    
    // Basic length and empty checks
    if (trimmedContent.isEmpty()) {
        return ContentVettingResult(false, "Story continuation cannot be empty")
    }
    
    if (trimmedContent.length < 10) {
        return ContentVettingResult(false, "Story continuation is too short. Please write at least 10 characters.")
    }
    
    // Check for gibberish patterns
    if (isGibberish(trimmedContent)) {
        return ContentVettingResult(false, "Content appears to be gibberish or meaningless text")
    }
    
    // Check for spam patterns
    if (isSpam(trimmedContent)) {
        return ContentVettingResult(false, "Content appears to be spam or promotional material")
    }
    
    // Check for repetitive content
    if (isRepetitive(trimmedContent)) {
        return ContentVettingResult(false, "Content is too repetitive. Please provide more varied text.")
    }
    
    // Check for excessive all caps
    if (isExcessiveAllCaps(trimmedContent)) {
        return ContentVettingResult(false, "Please avoid writing in all caps")
    }
    
    // Check for basic coherence (sentence structure)
    if (!hasBasicCoherence(trimmedContent)) {
        return ContentVettingResult(false, "Content lacks proper sentence structure. Please write in complete sentences.")
    }

    // Only check for story relevance if there is existing story context
    // Skip this check for initial paragraphs (when existingStory is empty)
    if (existingStory.isNotEmpty() && !isRelevantToStory(trimmedContent, existingStory)) {
        return ContentVettingResult(false, "Content doesn't seem to continue the story naturally")
    }
    
    return ContentVettingResult(true)
}

/**
 * Detects gibberish by looking for patterns that suggest random typing
 */
private fun isGibberish(text: String): Boolean {
    val lowercaseText = text.lowercase()
    
    // Check for excessive consecutive consonants (more than 4)
    val consecutiveConsonants = Regex("[bcdfghjklmnpqrstvwxyz]{5,}")
    if (consecutiveConsonants.containsMatchIn(lowercaseText)) return true
    
    // Check for random character patterns
    val randomPatterns = listOf(
        Regex("[qwerty]{6,}"), // Keyboard mashing
        Regex("[asdf]{5,}"),   // More keyboard patterns
        Regex("[zxcv]{4,}"),   // Bottom row patterns
        Regex("(.)\\1{4,}"),   // Same character repeated 5+ times
    )
    
    if (randomPatterns.any { it.containsMatchIn(lowercaseText) }) return true
    
    // Check vowel to consonant ratio (too few vowels might indicate gibberish)
    val vowels = lowercaseText.count { it in "aeiou" }
    val consonants = lowercaseText.count { it in "bcdfghjklmnpqrstvwxyz" }
    val totalLetters = vowels + consonants
    
    if (totalLetters > 10 && vowels.toDouble() / totalLetters < 0.15) return true
    
    return false
}

/**
 * Detects spam patterns like promotional content, URLs, etc.
 */
private fun isSpam(text: String): Boolean {
    val lowercaseText = text.lowercase()
    
    // Common spam keywords
    val spamKeywords = listOf(
        "click here", "buy now", "limited time", "act now", "free money",
        "make money", "work from home", "get rich", "guaranteed income",
        "no investment", "risk free", "100% guaranteed", "visit our website",
        "call now", "don't miss out", "exclusive offer", "special deal"
    )
    
    if (spamKeywords.any { lowercaseText.contains(it) }) return true
    
    // Check for URLs
    val urlPattern = Regex("https?://[\\w.-]+|www\\.[\\w.-]+|[\\w.-]+\\.(com|org|net|edu|gov)")
    if (urlPattern.containsMatchIn(lowercaseText)) return true
    
    // Check for excessive special characters that might indicate spam
    val specialCharCount = text.count { !it.isLetterOrDigit() && !it.isWhitespace() }
    if (specialCharCount > text.length * 0.3) return true
    
    return false
}

/**
 * Detects overly repetitive content
 */
private fun isRepetitive(text: String): Boolean {
    val words = text.trim().split(Regex("\\s+"))
    if (words.size < 5) return false
    
    // Check if the same word appears too frequently
    val wordCounts = words.groupBy { it.lowercase() }.mapValues { it.value.size }
    val maxWordCount = wordCounts.maxOfOrNull { it.value } ?: 0
    val uniqueWords = wordCounts.size
    
    // If any single word makes up more than 40% of the text, it's repetitive
    if (maxWordCount > words.size * 0.4) return true
    
    // Check for repeated phrases (3+ words)
    val phrases = mutableListOf<String>()
    for (i in 0..words.size - 3) {
        val phrase = words.subList(i, i + 3).joinToString(" ").lowercase()
        phrases.add(phrase)
    }
    
    val phraseCounts = phrases.groupBy { it }.mapValues { it.value.size }
    val maxPhraseCount = phraseCounts.maxOfOrNull { it.value } ?: 0
    
    // If any phrase appears more than twice, it might be repetitive
    if (maxPhraseCount > 2) return true
    
    return false
}

/**
 * Checks for excessive use of capital letters
 */
private fun isExcessiveAllCaps(text: String): Boolean {
    val letters = text.filter { it.isLetter() }
    if (letters.isEmpty()) return false
    
    val uppercaseCount = letters.count { it.isUpperCase() }
    val uppercaseRatio = uppercaseCount.toDouble() / letters.length
    
    // If more than 70% of letters are uppercase and text is longer than 20 chars, it's excessive
    return uppercaseRatio > 0.7 && text.length > 20
}

/**
 * Checks for basic sentence coherence
 */
private fun hasBasicCoherence(text: String): Boolean {
    val trimmedText = text.trim()
    
    // Should have some punctuation for longer texts
    if (trimmedText.length > 50 && !trimmedText.any { it in ".!?;:" }) {
        return false
    }
    
    // Should have reasonable word count
    val words = trimmedText.split(Regex("\\s+"))
    if (words.size < 3) return false
    
    // Should have some variation in word length
    val avgWordLength = words.map { it.length }.average()
    if (avgWordLength < 2 || avgWordLength > 15) return false
    
    return true
}

/**
 * Checks if the continuation is relevant to the existing story context
 */
private fun isRelevantToStory(continuation: String, existingStory: String): Boolean {
    val continuationWords = continuation.lowercase().split(Regex("\\W+")).filter { it.length > 3 }
    val storyWords = existingStory.lowercase().split(Regex("\\W+")).filter { it.length > 3 }
    
    if (storyWords.isEmpty()) return true // No context to compare against
    
    // Check for some word overlap or thematic consistency
    val commonWords = continuationWords.intersect(storyWords.toSet())
    val overlapRatio = commonWords.size.toDouble() / minOf(continuationWords.size, 10)
    
    // Allow continuation if there's some thematic overlap or if it's narrative prose
    val narrativeWords = listOf("the", "and", "but", "then", "when", "after", "before", "while", "since")
    val hasNarrativeStructure = narrativeWords.any { continuation.lowercase().contains(it) }
    
    return overlapRatio > 0.1 || hasNarrativeStructure || continuation.length > 100
}