package ec.edu.udla.arquitecturac4.consulta.dto;

public record ConsultaVehiculoResponse(
        String placa,
        String marca,
        String modelo,
        String anioModelo,
        String claseVehiculo,
        String color,
        String cilindraje) {
}
