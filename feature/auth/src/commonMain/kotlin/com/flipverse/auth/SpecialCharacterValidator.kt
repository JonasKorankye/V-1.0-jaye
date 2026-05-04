package com.flipverse.auth

data class SpecialCharacterValidator(val input: String) {

    fun containsSpecialCharacters(): Boolean {
       val specialCharacters = "[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~]"
        val regex = Regex(specialCharacters)
        return regex.containsMatchIn(input)
    }
}

fun <T> getElementPosition(list: MutableList<T>, element: T): Int {
    return list.indexOf(element)
}