version = 2

cloudstream {
    description = "Xtream IPTV Provider with Live TV, Movies & Series support"
    authors = listOf("anhdaden")
    status = 3  // 0=Down, 1=Ok, 2=Slow, 3=Beta
    tvTypes = listOf("Live", "Movie", "TvSeries")
    language = "vi"
    iconUrl = "https://gitlab.com/tearrs/cloudstream-vietnamese/-/raw/master/no-image.webp"
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
}

android {
    namespace = "com.anhdaden"
}
