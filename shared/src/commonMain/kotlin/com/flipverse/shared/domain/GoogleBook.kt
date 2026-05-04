package com.flipverse.shared.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleBooksResponse(
    val kind: String,
    val totalItems: Int,
    val items: List<GoogleBookItem>? = null
)

@Serializable
data class GoogleBookItem(
    val kind: String,
    val id: String,
    val etag: String? = null,
    val selfLink: String? = null,
    @SerialName("volumeInfo") val volumeInfo: VolumeInfo,
    @SerialName("saleInfo") val saleInfo: SaleInfo? = null,
    @SerialName("accessInfo") val accessInfo: AccessInfo? = null
)

@Serializable
data class VolumeInfo(
    val title: String,
    val subtitle: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null,
    val averageRating: Double? = null,
    val ratingsCount: Int? = null,
    val imageLinks: ImageLinks? = null,
    val language: String? = null,
    val previewLink: String? = null,
    val infoLink: String? = null,
    val canonicalVolumeLink: String? = null
)

@Serializable
data class ImageLinks(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val extraLarge: String? = null
)

@Serializable
data class SaleInfo(
    val country: String? = null,
    val saleability: String? = null,
    val isEbook: Boolean? = null,
    val listPrice: Price? = null,
    val retailPrice: Price? = null,
    val buyLink: String? = null,
    val offers: List<Offer>? = null
)

@Serializable
data class Price(
    val amount: Double? = null,
    val currencyCode: String? = null
)

@Serializable
data class Offer(
    val finskyOfferType: Int? = null,
    val listPrice: PriceMicros? = null,
    val retailPrice: PriceMicros? = null
)

@Serializable
data class PriceMicros(
    val amountInMicros: Long? = null,
    val currencyCode: String? = null
)

@Serializable
data class AccessInfo(
    val country: String? = null,
    val viewability: String? = null,
    val embeddable: Boolean? = null,
    val publicDomain: Boolean? = null,
    val textToSpeechPermission: String? = null,
    val epub: EpubAvailability? = null,
    val pdf: PdfAvailability? = null,
    val webReaderLink: String? = null,
    val accessViewStatus: String? = null
)

@Serializable
data class EpubAvailability(
    val isAvailable: Boolean? = null,
    val acsTokenLink: String? = null
)

@Serializable
data class PdfAvailability(
    val isAvailable: Boolean? = null,
    val acsTokenLink: String? = null
)

// Local cart item
@Serializable
data class CartItem(
    val bookId: String,
    val title: String,
    val authors: String,
    val price: Double,
    val currencyCode: String,
    val imageUrl: String?,
    val buyLink: String?,
    val quantity: Int = 1
)
