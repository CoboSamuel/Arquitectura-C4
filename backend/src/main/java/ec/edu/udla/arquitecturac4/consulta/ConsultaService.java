package ec.edu.udla.arquitecturac4.consulta;

import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaLicenciaRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoResponse;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoRequest;
import ec.edu.udla.arquitecturac4.consulta.sri.SRIContribuyenteClient;
import ec.edu.udla.arquitecturac4.consulta.sri.SRIVehiculoClient;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriVehiculoData;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    private final SRIContribuyenteClient sriContribuyenteClient;
    private final SRIVehiculoClient sriVehiculoClient;

    public ConsultaService(SRIContribuyenteClient sriContribuyenteClient, SRIVehiculoClient sriVehiculoClient) {
        this.sriContribuyenteClient = sriContribuyenteClient;
        this.sriVehiculoClient = sriVehiculoClient;
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

        SriContribuyenteResponse contributor = contributors.stream()
                .filter(candidate -> ruc.equals(candidate.numeroRuc()))
                .findFirst()
                .orElseThrow(SriRespuestaInvalidaException::new);

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
                || isBlank(vehicle.anioModelo())
                || isBlank(vehicle.claseVehiculo())) {
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

    public void consultarLicencia(ConsultaLicenciaRequest request) {
        throw new IntegracionPendienteException("La consulta de licencias todavía no está disponible.");
    }
}
