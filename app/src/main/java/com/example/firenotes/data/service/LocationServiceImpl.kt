package com.example.firenotes.data.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.example.firenotes.domain.repository.AddressInfo
import com.example.firenotes.domain.repository.LocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geocoder: Geocoder
) : LocationService {

    companion object {
        private const val TAG = "FireLocation"
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<Pair<Double, Double>> {
        return withContext(Dispatchers.IO) {
            try {
                // Verificar permissões
                if (!checkPermissions()) {
                    return@withContext Result.failure(SecurityException("Permissão de localização negada"))
                }

                // Tentar última localização conhecida
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    logD("✅ Última localização: ${lastLocation.latitude}, ${lastLocation.longitude}")
                    return@withContext Result.success(lastLocation.latitude to lastLocation.longitude)
                }

                // Se não houver última localização, solicitar atualização
                val location = fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .await()
                
                if (location != null) {
                    logD("✅ Localização atual: ${location.latitude}, ${location.longitude}")
                    return@withContext Result.success(location.latitude to location.longitude)
                }

                return@withContext Result.failure(Exception("Não foi possível obter localização"))
            } catch (e: Exception) {
                logE("❌ Erro ao obter localização: ${e.message}")
                return@withContext Result.failure(e)
            }
        }
    }

    override suspend fun getAddressFromLocation(lat: Double, lon: Double): Result<AddressInfo> {
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val address = addresses?.firstOrNull()
                
                if (address != null) {
                    val info = AddressInfo(
                        rua = address.thoroughfare ?: address.featureName ?: "",
                        numero = address.subThoroughfare ?: "",
                        bairro = address.subLocality ?: address.locality ?: "",
                        cidade = address.locality ?: address.subAdminArea ?: "",
                        uf = address.adminArea ?: "",
                        cep = address.postalCode ?: "",
                        pais = address.countryName ?: "Brasil"
                    )
                    logD("✅ Endereço obtido: ${info.cidade}/${info.uf}")
                    return@withContext Result.success(info)
                }
                
                return@withContext Result.failure(Exception("Endereço não encontrado"))
            } catch (e: Exception) {
                logE("❌ Erro no geocoding: ${e.message}")
                return@withContext Result.failure(e)
            }
        }
    }

    override suspend fun getCoordsFromCityName(cityName: String): Result<Pair<Double, Double>> {
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(cityName, 1)
                val address = addresses?.firstOrNull()
                
                if (address != null) {
                    logD("✅ Coordenadas obtidas: ${address.latitude}, ${address.longitude}")
                    return@withContext Result.success(address.latitude to address.longitude)
                }
                
                return@withContext Result.failure(Exception("Cidade não encontrada: $cityName"))
            } catch (e: Exception) {
                logE("❌ Erro ao buscar cidade: ${e.message}")
                return@withContext Result.failure(e)
            }
        }
    }

    override suspend fun checkPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fine || coarse
    }

    override suspend fun requestPermissions(): Boolean {
        return withContext(Dispatchers.Main) {
            // Implementar com ActivityResultLauncher
            // Ou usar Flow para observar mudanças
            true
        }
    }

    private fun logD(message: String) = android.util.Log.d(TAG, message)
    private fun logE(message: String, throwable: Throwable? = null) = 
        android.util.Log.e(TAG, message, throwable)
}
