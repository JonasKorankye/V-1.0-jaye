package com.flipverse.shared.location


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationInfo(
    @SerialName("asn")
    val asn: String = "",
    @SerialName("city")
    val city: String="",
    @SerialName("continent_code")
    val continentCode: String = "",
    @SerialName("country")
    val country: String = "",
    @SerialName("country_area")
    val countryArea: Double = 0.0,
    @SerialName("country_calling_code")
    val countryCallingCode: String = "",
    @SerialName("country_capital")
    val countryCapital: String = "",
    @SerialName("country_code")
    val countryCode: String = "",
    @SerialName("country_code_iso3")
    val countryCodeIso3: String = "",
    @SerialName("country_name")
    val countryName: String = "",
    @SerialName("country_population")
    val countryPopulation: Int = 0,
    @SerialName("country_tld")
    val countryTld: String = "",
    @SerialName("currency")
    val currency: String = "",
    @SerialName("currency_name")
    val currencyName: String = "",
    @SerialName("in_eu")
    val inEu: Boolean = false,
    @SerialName("ip")
    val ip: String = "",
    @SerialName("languages")
    val languages: String = "",
    @SerialName("latitude")
    val latitude: Double = 0.0,
    @SerialName("longitude")
    val longitude: Double = 0.0,
    @SerialName("network")
    val network: String = "",
    @SerialName("org")
    val org: String = "",
    @SerialName("postal")
    val postal: String = "",
    @SerialName("region")
    val region: String = "",
    @SerialName("region_code")
    val regionCode: String = "",
    @SerialName("timezone")
    val timezone: String = "",
    @SerialName("utc_offset")
    val utcOffset: String = "",
    @SerialName("version")
    val version: String = ""
)