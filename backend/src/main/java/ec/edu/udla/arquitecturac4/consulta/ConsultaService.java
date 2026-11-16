package ec.edu.udla.arquitecturac4.consulta;

import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteResponse;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaLicenciaRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoRequest;
import ec.edu.udla.arquitecturac4.consulta.sri.SRIContribuyenteClient;
import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriContribuyenteResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    private final SRIContribuyenteClient sriContribuyenteClient;

    public ConsultaService(SRIContribuyenteClient sriContribuyenteClient) {
        this.sriContribuyenteClient = sriContribuyenteClient;
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

    public void consultarVehiculo(ConsultaVehiculoRequest request) {
        throw new IntegracionPendienteException("La consulta de vehículos todavía no está disponible.");
    }

    public void consultarLicencia(ConsultaLicenciaRequest request) {
        throw new IntegracionPendienteException("La consulta de licencias todavía no está disponible.");
    }
}
