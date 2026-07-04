package com.example.firenotes.data.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.example.firenotes.domain.repository.AddressDetails
import com.example.firenotes.domain.repository.LocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private fun isRunningOnEmulator(): Boolean {
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val model = android.os.Build.MODEL
        val hardware = android.os.Build.HARDWARE
        return (brand.startsWith("generic") && device.startsWith("generic"))
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Pair<Double, Double>> = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                continuation.resume(Result.success(Pair(location.latitude, location.longitude)))
            } else {
                val errorMsg = if (isRunningOnEmulator()) {
                    "Configure uma localização no Android Emulator."
                } else {
                    "GPS retornou nulo. Verifique se o GPS está ativado."
                }
                continuation.resume(Result.failure(Exception(errorMsg)))
            }
        }.addOnFailureListener { exception ->
            val errorMsg = if (isRunningOnEmulator()) {
                "Configure uma localização no Android Emulator."
            } else {
                exception.localizedMessage ?: "Erro desconhecido ao obter GPS."
            }
            continuation.resume(Result.failure(Exception(errorMsg)))
        }
    }

    override suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Result<AddressDetails> = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses.isNullOrEmpty()) {
                throw Exception("Nenhum endereço encontrado para essas coordenadas.")
            }
            val address = addresses[0]
            val rawNum = address.subThoroughfare
            val num = if (rawNum == null || rawNum == "null" || rawNum == "0") "" else rawNum
            
            val cidade = address.locality?.takeIf { it.isNotBlank() }
                ?: address.subAdminArea?.takeIf { it.isNotBlank() }
                ?: address.adminArea?.takeIf { it.isNotBlank() }
                ?: address.subLocality?.takeIf { it.isNotBlank() }
                ?: ""
                
            val ufSigla = getUfAbbreviation(address.adminArea)
            
            android.util.Log.d(
                "FireNotes",
                "ReverseGeocoder: Latitude=$latitude, Longitude=$longitude, Rua=${address.thoroughfare ?: ""}, Número=$num, Bairro=${address.subLocality ?: ""}, Cidade=$cidade, UF=$ufSigla, Precisão=N/A, Timestamp=${System.currentTimeMillis()}"
            )

            AddressDetails(
                rua = address.thoroughfare ?: "",
                numero = num,
                bairro = address.subLocality ?: "",
                cidade = cidade,
                uf = ufSigla
            )
        }
    }

    private fun getUfAbbreviation(stateName: String?): String {
        if (stateName == null) return ""
        val name = stateName.lowercase().trim()
        return when {
            name.contains("são paulo") || name == "sp" -> "SP"
            name.contains("rio de janeiro") || name == "rj" -> "RJ"
            name.contains("minas gerais") || name == "mg" -> "MG"
            name.contains("espírito santo") || name == "es" -> "ES"
            name.contains("paraná") || name == "pr" -> "PR"
            name.contains("santa catarina") || name == "sc" -> "SC"
            name.contains("rio grande do sul") || name == "rs" -> "RS"
            name.contains("bahia") || name == "ba" -> "BA"
            name.contains("pernambuco") || name == "pe" -> "PE"
            name.contains("ceará") || name == "ce" -> "CE"
            name.contains("distrito federal") || name == "df" -> "DF"
            name.contains("goiás") || name == "go" -> "GO"
            name.contains("mato grosso") || name == "mt" -> "MT"
            name.contains("mato grosso do sul") || name == "ms" -> "MS"
            name.contains("alagoas") || name == "al" -> "AL"
            name.contains("sergipe") || name == "se" -> "SE"
            name.contains("paraíba") || name == "pb" -> "PB"
            name.contains("rio grande do norte") || name == "rn" -> "RN"
            name.contains("maranhão") || name == "ma" -> "MA"
            name.contains("piauí") || name == "pi" -> "PI"
            name.contains("pará") || name == "pa" -> "PA"
            name.contains("amazonas") || name == "am" -> "AM"
            name.contains("acre") || name == "ac" -> "AC"
            name.contains("rondônia") || name == "ro" -> "RO"
            name.contains("roraima") || name == "rr" -> "RR"
            name.contains("amapá") || name == "ap" -> "AP"
            name.contains("tocantins") || name == "to" -> "TO"
            else -> if (stateName.length >= 2) stateName.take(2).uppercase() else stateName.uppercase()
        }
    }
}
