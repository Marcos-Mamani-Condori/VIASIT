package com.oficial.viasit

import android.app.Application
import com.oficial.viasit.data.local.AppDatabase
import com.oficial.viasit.data.remote.PocketBaseRealtimeClient
import com.oficial.viasit.data.repository.AdminRepository
import com.oficial.viasit.data.repository.AutoRepository
import com.oficial.viasit.data.repository.AuthRepository
import com.oficial.viasit.data.repository.ReportesRepository
import com.oficial.viasit.domain.repository.IAdminRepository
import com.oficial.viasit.domain.usecases.*

class AutosAplicacion : Application() {

    private val database by lazy { AppDatabase.getDatabase(this) }

    val pocketBaseClient by lazy {
        PocketBaseRealtimeClient(database.autoData())
    }

    val repository: AutoRepository by lazy {
        AutoRepository(
            pocketBaseClient = pocketBaseClient,
            dao = database.autoData(),
            authRepository = authRepository
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(applicationContext)
    }

    val loginUseCase by lazy { LoginUseCase(authRepository) }
    val registerUseCase by lazy { RegisterUseCase(authRepository, adminRepository) }
    val logoutUseCase by lazy { LogoutUseCase(authRepository) }
    val enterAsGuestUseCase by lazy { EnterAsGuestUseCase(authRepository) }
    val setDriverInServiceUseCase by lazy { SetDriverInServiceUseCase(authRepository) }
    val getAuthStateUseCase by lazy { GetAuthStateUseCase(authRepository) }
    val getIsInServiceUseCase by lazy { GetIsInServiceUseCase(authRepository) }
    val restoreSessionUseCase by lazy { RestoreSessionUseCase(authRepository) }

    // UseCases de Autos
    val getAutosUseCase by lazy { GetAutosUseCase(repository) }
    val registerVehicleUseCase by lazy { RegisterVehicleUseCase(repository) }
    val getAutoByUserIdUseCase by lazy { GetAutoByUserIdUseCase(repository) }
    val startRealtimeAutosUseCase by lazy { StartRealtimeAutosUseCase(repository) }
    val stopRealtimeAutosUseCase by lazy { StopRealtimeAutosUseCase(repository) }
    val refreshAutosUseCase by lazy { RefreshAutosUseCase(repository) }
    val syncAutosUseCase by lazy { SyncAutosUseCase(repository) }

    // UseCases de Reportes
    val submitReporteUseCase by lazy { SubmitReporteUseCase(reportesRepository) }
    val getReportesUseCase by lazy { GetReportesUseCase(reportesRepository) }
    val responderReporteUseCase by lazy { ResponderReporteUseCase(reportesRepository) }

    // UseCases de Admin - Lineas
    val getLineasUseCase by lazy { GetLineasUseCase(adminRepository) }
    val createLineaUseCase by lazy { CreateLineaUseCase(adminRepository) }
    val updateLineaUseCase by lazy { UpdateLineaUseCase(adminRepository) }
    val deleteLineaUseCase by lazy { DeleteLineaUseCase(adminRepository) }

    // UseCases de Admin - Invitaciones
    val getInvitationCodesUseCase by lazy { GetInvitationCodesUseCase(adminRepository) }
    val generateInvitationCodeUseCase by lazy { GenerateInvitationCodeUseCase(adminRepository) }
    val deleteInvitationCodeUseCase by lazy { DeleteInvitationCodeUseCase(adminRepository) }
    val validateInvitationCodeUseCase by lazy { ValidateInvitationCodeUseCase(adminRepository) }
    val useInvitationCodeUseCase by lazy { UseInvitationCodeUseCase(adminRepository) }

    // UseCases de Admin - Rutas
    val getRutasUseCase by lazy { GetRutasUseCase(adminRepository) }
    val createRutaUseCase by lazy { CreateRutaUseCase(adminRepository) }
    val deleteRutaUseCase by lazy { DeleteRutaUseCase(adminRepository) }
    val assignRutaToLineaUseCase by lazy { AssignRutaToLineaUseCase(adminRepository) }

    // UseCases de Admin - Logs
    val getLogsUseCase by lazy { GetLogsUseCase(adminRepository) }
    val createLogUseCase by lazy { CreateLogUseCase(adminRepository) }

    // UseCases de Admin - Users
    val getUsersByLineaUseCase by lazy { GetUsersByLineaUseCase(adminRepository) }
    val setUserActiveStatusUseCase by lazy { SetUserActiveStatusUseCase(adminRepository) }
    val deleteUserUseCase by lazy { DeleteUserUseCase(adminRepository) }

    // UseCases de Admin - Extra (Appeals & All Reports)
    val getAppealsUseCase by lazy { GetAppealsUseCase(adminRepository) }
    val resolveAppealUseCase by lazy { ResolveAppealUseCase(adminRepository) }
    val getAllReportesUseCase by lazy { GetAllReportesUseCase(reportesRepository) }

    // UseCases de Admin - Vehicle Codes
    val getVehicleInvitationCodesUseCase by lazy { GetVehicleInvitationCodesUseCase(adminRepository) }
    val generateVehicleInvitationCodeUseCase by lazy { GenerateVehicleInvitationCodeUseCase(adminRepository) }
    val deleteVehicleInvitationCodeUseCase by lazy { DeleteVehicleInvitationCodeUseCase(adminRepository) }

    // UseCases de Pasajero
    val searchLineasByDestinationUseCase by lazy { SearchLineasByDestinationUseCase() }

    val reportesRepository: ReportesRepository by lazy {
        ReportesRepository(pocketBaseClient, authRepository)
    }

    val adminRepository: IAdminRepository by lazy {
        AdminRepository(pocketBaseClient, authRepository)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        pocketBaseClient.release()
    }

    companion object {
        lateinit var instance: AutosAplicacion
            private set
    }
}
