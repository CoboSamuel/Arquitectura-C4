package ec.edu.udla.arquitecturac4.consulta;

import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaContribuyenteRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaLicenciaRequest;
import ec.edu.udla.arquitecturac4.consulta.dto.ConsultaVehiculoRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/consultas", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @PostMapping(path = "/contribuyentes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void consultarContribuyente(@Valid @RequestBody ConsultaContribuyenteRequest request) {
        consultaService.consultarContribuyente(request);
    }

    @PostMapping(path = "/vehiculos", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void consultarVehiculo(@Valid @RequestBody ConsultaVehiculoRequest request) {
        consultaService.consultarVehiculo(request);
    }

    @PostMapping(path = "/licencias", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void consultarLicencia(@Valid @RequestBody ConsultaLicenciaRequest request) {
        consultaService.consultarLicencia(request);
    }
}
