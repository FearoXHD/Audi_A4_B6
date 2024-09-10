import retrofit2.Call
import retrofit2.http.GET

interface LocationService {
    @GET("location")  // Ersetze dies durch den Pfad zu deiner API
    fun getLocation(): Call<LocationData>
}

data class LocationData(
    val latitude: Double,
    val longitude: Double
)
