package ec.edu.udla.arquitecturac4.consulta;

import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaLicenciaRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoRequest;
import org.springframework.stereotype.Service;

@Service
public class ConsultaService {

    public void consultarContribuyente(ConsultaContribuyenteRequest request) {
        throw new IntegracionPendienteException("La consulta de contribuyentes todavía no está disponible.");
    }

    public void consultarVehiculo(ConsultaVehiculoRequest request) {
        throw new IntegracionPendienteException("La consulta de vehículos todavía no está disponible.");
    }

    public void consultarLicencia(ConsultaLicenciaRequest request) {
        throw new IntegracionPendienteException("La consulta de licencias todavía no está disponible.");
    }
}
