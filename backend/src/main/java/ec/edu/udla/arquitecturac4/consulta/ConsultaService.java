package ec.edu.udla.arquitecturac4.consulta;

import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaLicenciaRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaLicenciaResponse;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoResponse;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoRequest;
import ec.edu.udla.arquitecturac4.consulta.spi.ContribuyenteProvider;
import ec.edu.udla.arquitecturac4.consulta.spi.VehiculoProvider;
import ec.edu.udla.arquitecturac4.consulta.cache.ConsultaLicenciaResult;
import ec.edu.udla.arquitecturac4.consulta.cache.LicenciaCacheService;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriVehiculoData;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    private final ContribuyenteProvider sriContribuyenteClient;
    private final VehiculoProvider sriVehiculoClient;
    private final LicenciaCacheService licenciaCacheService;

    public ConsultaService(
            ContribuyenteProvider sriContribuyenteClient,
            VehiculoProvider sriVehiculoClient,
            LicenciaCacheService licenciaCacheService) {
        this.sriContribuyenteClient = sriContribuyenteClient;
        this.sriVehiculoClient = sriVehiculoClient;
        this.licenciaCacheService = licenciaCacheService;
    }

    public ConsultaContribuyenteResponse consultarContribuyente(ConsultaContribuyenteRequest request) {
        String ruc = request.ruc();
        if (!sriContribuyenteClient.existe(ruc)) {
            throw new ContribuyenteNoEncontradoException();
        }

        List<SriContribuyenteResponse> contributors = sriContribuyenteClient.obtener(ruc);
        if (contributors.isEmpty()) {
            throw new ContribuyenteNoEncontradoException();
        }

        List<SriContribuyenteResponse> matches = contributors.stream()
                .filter(candidate -> ruc.equals(candidate.numeroRuc()))
                .toList();
        if (matches.size() != 1) {
            throw new SriRespuestaInvalidaException();
        }
        SriContribuyenteResponse contributor = matches.getFirst();

        if (contributor.tipoContribuyente() == null
                || contributor.tipoContribuyente().isBlank()) {
            throw new SriRespuestaInvalidaException();
        }
        if (!"PERSONA NATURAL".equalsIgnoreCase(contributor.tipoContribuyente().trim())) {
            throw new NoEsPersonaNaturalException();
        }

        if (isBlank(contributor.razonSocial())
                || isBlank(contributor.estadoContribuyenteRuc())
                || isBlank(contributor.actividadEconomicaPrincipal())
                || isBlank(contributor.regimen())) {
            throw new SriRespuestaInvalidaException();
        }

        String startDate = contributor.informacionFechasContribuyente() == null
                ? null
                : contributor.informacionFechasContribuyente().fechaInicioActividades();

        return new ConsultaContribuyenteResponse(
                contributor.numeroRuc(),
                contributor.razonSocial(),
                "PERSONA NATURAL",
                contributor.estadoContribuyenteRuc(),
                contributor.actividadEconomicaPrincipal(),
                contributor.regimen(),
                startDate);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public ConsultaVehiculoResponse consultarVehiculo(ConsultaVehiculoRequest request) {
        Optional<SriVehiculoData> result = sriVehiculoClient.buscar(request.placa());
        SriVehiculoData vehicle = result.orElseThrow(VehiculoNoEncontradoException::new);
        if (isBlank(vehicle.marca())
                || isBlank(vehicle.modelo())
                || isBlank(vehicle.anioModelo())) {
            throw new SriRespuestaInvalidaException();
        }
        return new ConsultaVehiculoResponse(
                request.placa(),
                vehicle.marca(),
                vehicle.modelo(),
                vehicle.anioModelo(),
                vehicle.claseVehiculo(),
                vehicle.color(),
                vehicle.cilindraje());
    }

    public ConsultaLicenciaResponse consultarLicencia(ConsultaLicenciaRequest request) {
        ConsultaLicenciaResult result = licenciaCacheService.consultar(request.cedula(), request.placa());
        return new ConsultaLicenciaResponse(
                result.puntosDisponibles(), result.actualizadoEn(), result.origen(), result.desactualizado());
    }
}
