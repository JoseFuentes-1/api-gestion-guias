package cl.duoc.cloud.gestion_guias.controller;

import cl.duoc.cloud.gestion_guias.service.S3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final S3Service s3Service;

    public GuiaController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

// Endpoint: Crear o Subir Guía
    @PostMapping("/subir")
    public ResponseEntity<String> subirGuia(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("fecha") String fecha,
            @RequestParam("transportista") String transportista) {
        try {
            String resultado = s3Service.procesarYSubirGuia(archivo, fecha, transportista);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            e.printStackTrace(); // Esto forzará que ./mvnw spring-boot:runel error salga en rojo en la consola de VS Code
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error real de AWS o Servidor: " + e.getMessage());
        }
    }

    // Endpoint: Actualizar Guía (En AWS S3 funciona igual que subir, sobreescribe el archivo)
    @PutMapping("/actualizar")
    public ResponseEntity<String> actualizarGuia(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("fecha") String fecha,
            @RequestParam("transportista") String transportista) {
        try {
            String resultado = s3Service.procesarYSubirGuia(archivo, fecha, transportista);
            return ResponseEntity.ok("Guía actualizada: " + resultado);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al actualizar");
        }
    }

    // Endpoint: Descargar Guía con Validación de Permisos Simulada
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarGuia(
            @RequestParam("fecha") String fecha,
            @RequestParam("transportista") String transportista,
            @RequestParam("nombreArchivo") String nombreArchivo,
            @RequestHeader(value = "Authorization", required = false) String token) {

        // Validación simple de permisos requerida por la pauta
        if (token == null || !token.equals("Bearer mi-token-secreto")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        byte[] archivo = s3Service.descargarGuia(fecha, transportista, nombreArchivo);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", nombreArchivo);

        return new ResponseEntity<>(archivo, headers, HttpStatus.OK);
    }

    // Endpoint: Eliminar
    @DeleteMapping("/eliminar")
    public ResponseEntity<String> eliminarGuia(
            @RequestParam("fecha") String fecha,
            @RequestParam("transportista") String transportista,
            @RequestParam("nombreArchivo") String nombreArchivo) {
        String resultado = s3Service.eliminarGuia(fecha, transportista, nombreArchivo);
        return ResponseEntity.ok(resultado);
    }

    // Endpoint: Consultar por transportista y fecha
    @GetMapping("/listar")
    public ResponseEntity<List<String>> listarGuias(
            @RequestParam("fecha") String fecha,
            @RequestParam("transportista") String transportista) {
        List<String> guias = s3Service.listarGuias(fecha, transportista);
        return ResponseEntity.ok(guias);
    }
}