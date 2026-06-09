package cl.duoc.cloud.gestion_guias.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Value("${app.efs.temp-folder}")
    private String efsTempFolder;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    
    public String procesarYSubirGuia(MultipartFile archivo, String fecha, String transportista) throws IOException {
        Path directorioEfs = Paths.get(efsTempFolder);
        if (!Files.exists(directorioEfs)) {
            Files.createDirectories(directorioEfs);
        }
        
        String nombreOriginal = archivo.getOriginalFilename();
        String nombreArchivo = org.springframework.util.StringUtils.cleanPath(nombreOriginal);
        if (nombreArchivo.contains("..")) {
            throw new SecurityException("¡Alerta de seguridad! El archivo contiene una ruta inválida.");
        }
        Path rutaLocal = directorioEfs.resolve(nombreArchivo);
        archivo.transferTo(rutaLocal.toFile());

        String s3Key = fecha + "/" + transportista + "/" + nombreArchivo;
        
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(rutaLocal.toFile()));
        
        // Limpiamos el EFS temporal
        Files.deleteIfExists(rutaLocal);

        return "Operación exitosa en S3: " + s3Key;
    }

    // 2. DESCARGAR
    public byte[] descargarGuia(String fecha, String transportista, String nombreArchivo) {
        String s3Key = fecha + "/" + transportista + "/" + nombreArchivo;
        
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(request);
        return objectBytes.asByteArray();
    }

    // 3. ELIMINAR
    public String eliminarGuia(String fecha, String transportista, String nombreArchivo) {
        String s3Key = fecha + "/" + transportista + "/" + nombreArchivo;
        
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(request);
        return "Archivo eliminado: " + s3Key;
    }

    // 4. LISTAR HISTORIAL POR FECHA Y TRANSPORTISTA
    public List<String> listarGuias(String fecha, String transportista) {
        String prefix = fecha + "/" + transportista + "/";
        
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }
}