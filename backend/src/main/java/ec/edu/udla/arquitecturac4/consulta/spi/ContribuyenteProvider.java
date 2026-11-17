package ec.edu.udla.arquitecturac4.consulta.spi;

import ec.edu.udla.arquitecturac4.consulta.sri.dto.SriContribuyenteResponse;
import java.util.List;

public interface ContribuyenteProvider {

    boolean existe(String ruc);

    List<SriContribuyenteResponse> obtener(String ruc);
}
