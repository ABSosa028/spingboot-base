package logic.soft.fel.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/factura")
public class CertificacionController {

    @GetMapping("/servicio-prueba")
    public String saludo() {
        return "Aplicacion corriendo de manera correcta";
    }

    @GetMapping("/ver")
    public ResponseEntity<String> doGet() {
        return ResponseEntity.ok("en funcionamiento");
    }

    @GetMapping("/send")
    public ResponseEntity<String> sendFactura() {
        return ResponseEntity.ok("en funcionamiento enviar");
    }

    @GetMapping("/load")
    public ResponseEntity<String> loadPdf() {
        return ResponseEntity.ok("en funcionamiento pdf");
    }

    @GetMapping("/loadXml")
    public ResponseEntity<String> loadXml() {
        return ResponseEntity.ok("en funcionamiento pdf");
    }

}
