package logic.soft.fel.services;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;

public class FacturaFel {

    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String DB_USER = "tu_usuario";
    private static final String DB_PASSWORD = "tu_contraseña";

    private Connection dbConn;

    // public FacturaFel(Connection dbConn) {
    // this.dbConn = dbConn;
    // }

    public void conectarBaseDatos() {
        try {
            dbConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Conexión a la base de datos establecida exitosamente.");
        } catch (SQLException e) {
            System.out.println("Error al conectar a la base de datos: " + e.getMessage());
        }
    }

    public void cerrarConexion() {
        try {
            if (dbConn != null && !dbConn.isClosed()) {
                dbConn.close();
                System.out.println("Conexión a la base de datos cerrada.");
            }
        } catch (SQLException e) {
            System.out.println("Error al cerrar la conexión a la base de datos: " + e.getMessage());
        }
    }

    public static void procesarDocumentos() {
        System.out.println("Procesando documentos...");
    }

    public List<Map<String, Object>> getListDocsCondominios(int empresa) {
        List<Map<String, Object>> salida = new ArrayList<>();
        String sql = "SELECT * FROM sqladmin.V_DOC_SAT_PENDIENTES_CONDO WHERE empresa = ?";

        try (PreparedStatement stm = dbConn.prepareStatement(sql)) {
            stm.setInt(1, empresa);

            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> trmp = new HashMap<>();
                    int columnCount = rs.getMetaData().getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i).toLowerCase();
                        trmp.put(columnName, rs.getObject(i));
                    }
                    salida.add(trmp);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener documentos de condominios: " + e.getMessage());
        }

        return salida;
    }

    public List<Map<String, Object>> getListDocs() {
        List<Map<String, Object>> salida = new ArrayList<>();
        String sql = "SELECT tipo, empresa, agencia, id, version_endpoint FROM sqladmin.v_doc_sat_pendientes";

        try (PreparedStatement stm = dbConn.prepareStatement(sql);
                ResultSet rs = stm.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> trmp = new HashMap<>();
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i).toLowerCase();
                    trmp.put(columnName, rs.getObject(i));
                }
                salida.add(trmp);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener documentos: " + e.getMessage());
        }

        return salida;
    }

    public Map<String, Object> getEmpresaInfoCondominios(int empresa) {
        String sql = "SELECT " +
                "fel_token, " +
                "lpad(replace(nit_empresa, '-', ''), 12, '0') AS NIT, " +
                "replace(nit_empresa, '-') AS nit2, " +
                "'condominios/' || folder || '/' AS PDF_destino " +
                "FROM sqladmin.empresa " +
                "WHERE empresa = ?";
        Map<String, Object> result = new HashMap<>();

        try (PreparedStatement stm = dbConn.prepareStatement(sql)) {
            stm.setInt(1, empresa);

            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    result.put("fel_token", rs.getString("fel_token"));
                    result.put("NIT", rs.getString("NIT"));
                    result.put("nit2", rs.getString("nit2"));
                    result.put("PDF_destino", rs.getString("PDF_destino"));
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener la información de la empresa (condominios): " + e.getMessage());
        }

        return result;
    }

    private final String url = "https://ingestor.totaldoc.io/v1.0/";

    public Map<String, Object> getEmpresaInfo(int empresa) {
        String sql = "SELECT " +
                "fel_token, " +
                "lpad(replace(nit_empresa, '-', ''), 12, '0') AS NIT, " +
                "replace(nit_empresa, '-') AS nit2, " +
                "folder || '/' AS PDF_destino " +
                "FROM sqladmin.empresa " +
                "WHERE empresa = ?";
        Map<String, Object> result = new HashMap<>();

        try (PreparedStatement stm = dbConn.prepareStatement(sql)) {
            stm.setInt(1, empresa);

            try (ResultSet rs = stm.executeQuery()) {
                if (rs.next()) {
                    result.put("fel_token", rs.getString("fel_token"));
                    result.put("NIT", rs.getString("NIT"));
                    result.put("nit2", rs.getString("nit2"));
                    result.put("PDF_destino", rs.getString("PDF_destino"));
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener la información de la empresa: " + e.getMessage());
        }

        return result;
    }

    /**
     * @param empresa
     * @param xml
     * @param tipo
     * @param anulacion
     * @return
     * @throws Exception
     */
    public String firmarXML(int empresa, String xml, String tipo, boolean anulacion) throws Exception {

        Map<String, Object> empVars;
        if ("condominios".equals(tipo)) {
            empVars = getEmpresaInfoCondominios(empresa);
        } else {
            empVars = getEmpresaInfo(empresa);
        }

        if (empVars == null) {
            throw new Exception("Error al obtener datos de la empresa");
        }

        String tempURL = url + (anulacion ? "signanulacion" : "signature");

        String encodedXml = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
        JSONObject payload = new JSONObject();
        JSONObject dteObject = new JSONObject();
        dteObject.put("nit_transmitter", empVars.get("nit2"));
        dteObject.put("xml_dte", encodedXml);
        payload.put("dte", dteObject);

        HttpURLConnection connection = (HttpURLConnection) new URL(tempURL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("apiKey", (String) empVars.get("fel_token"));
        connection.setDoOutput(true);
        // connection.setSSLSocketFactory((SSLSocketFactory)
        // SSLSocketFactory.getDefault());

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream is = connection.getInputStream()) {
                String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject jsonResponse = new JSONObject(response);

                if (jsonResponse.has("xmlSigned")) {
                    return jsonResponse.getString("xmlSigned");
                }
            }
        } else {
            throw new Exception("Error en la conexión HTTP: Código de respuesta " + responseCode);
        }

        return null;
    }

    public String subirSAT(int empresa, int agencia, int id, String xml, String tipo, int casa, boolean anulacion) {
        try {

            xml = firmarXML(empresa, xml, tipo, anulacion);
            if (xml == null) {
                throw new Exception("No se pudo Firmar el DTE");
            }

            String sql = getQueryForTipo(tipo);
            if (sql == null) {
                throw new Exception("Tipo de documento desconocido");
            }

            // ResultSet rs2 = null;
            try (PreparedStatement stm = dbConn.prepareStatement(sql)) {
                stm.setInt(1, empresa);
                stm.setInt(2, agencia);
                stm.setInt(3, id);
                if ("condominios".equals(tipo)) {
                    stm.setInt(4, casa);
                }

                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next() && rs.getInt("CERTIFICADO_SAT") == 1 && !anulacion) {
                        return "{\"success\": false, \"message\": \"Este Documento ya fue registrado ante la SAT.\"}";
                    }
                }
            }

            Map<String, Object> empVars = "condominios".equals(tipo) ? getEmpresaInfoCondominios(empresa)
                    : getEmpresaInfo(empresa);
            if (empVars == null) {
                throw new Exception("Error al obtener datos de la empresa");
            }

            String tempURL = url + (anulacion ? "dte-anulacion" : "dte");

            JSONObject payload = new JSONObject();
            JSONObject dteObject = new JSONObject();
            dteObject.put("nit_transmitter", empVars.get("nit2"));
            // dteObject.put("serie", rs.getString("serie"));
            // dteObject.put("number", rs.getString("num_doc"));
            dteObject.put("xml_dte", xml);
            payload.put("dte", dteObject);

            HttpURLConnection connection = (HttpURLConnection) new URL(tempURL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("apiKey", (String) empVars.get("fel_token"));
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            String response = processHttpResponse(connection);
            JSONObject jsonResponse = new JSONObject(response);

            if (!jsonResponse.has("code")) {
                return processSATSuccessResponse(jsonResponse, empVars, tipo, agencia, empresa, id, casa, anulacion);
            } else {

                updateErrorStatusInDatabase(tipo, empresa, agencia, id, casa, jsonResponse.getString("message"));
                return jsonResponse.toString();
            }

        } catch (Exception e) {
            return "{\"success\": false, \"error\": {\"msj\": \"Connection error: " + e.getMessage() + "\"}}";
        }
    }

    public String subirSAT2(int empresa, int agencia, int id, String xml, String tipo, int casa, boolean anulacion) {
        try {
            // Firma del XML
            xml = firmarXML(empresa, xml, tipo, anulacion);
            if (xml == null) {
                throw new Exception("No se pudo Firmar el DTE");
            }

            // Obtener consulta SQL según el tipo de documento
            String sql = getQueryForTipo(tipo);
            if (sql == null) {
                throw new Exception("Tipo de documento desconocido");
            }

            // Preparar y ejecutar la consulta para obtener estado del certificado
            String serie = null;
            String numDoc = null;

            try (PreparedStatement stm = dbConn.prepareStatement(sql)) {
                stm.setInt(1, empresa);
                stm.setInt(2, agencia);
                stm.setInt(3, id);
                if ("condominios".equals(tipo)) {
                    stm.setInt(4, casa);
                }

                try (ResultSet rs = stm.executeQuery()) {
                    if (rs.next()) {
                        // Si el documento ya está certificado, retornar sin hacer nada más
                        if (rs.getInt("CERTIFICADO_SAT") == 1 && !anulacion) {
                            return "{\"success\": false, \"message\": \"Este Documento ya fue registrado ante la SAT.\"}";
                        }
                        serie = rs.getString("serie");
                        numDoc = rs.getString("num_doc");
                    }
                }
            }

            // Obtener información de la empresa y construir la URL
            Map<String, Object> empVars = "condominios".equals(tipo) ? getEmpresaInfoCondominios(empresa)
                    : getEmpresaInfo(empresa);
            if (empVars == null) {
                throw new Exception("Error al obtener datos de la empresa");
            }

            String tempURL = url + (anulacion ? "dte-anulacion" : "dte");

            // Crear el JSON payload
            JSONObject payload = new JSONObject();
            JSONObject dteObject = new JSONObject();
            dteObject.put("nit_transmitter", empVars.get("nit2"));
            dteObject.put("serie", serie);
            dteObject.put("number", numDoc);
            dteObject.put("xml_dte", xml);
            payload.put("dte", dteObject);

            // Configurar y enviar la solicitud HTTP
            HttpURLConnection connection = (HttpURLConnection) new URL(tempURL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("apiKey", (String) empVars.get("fel_token"));
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            // Procesar la respuesta de la solicitud HTTP
            String response = processHttpResponse(connection);
            JSONObject jsonResponse = new JSONObject(response);

            // Manejar respuesta exitosa
            if (!jsonResponse.has("code")) {
                return processSATSuccessResponse(jsonResponse, empVars, tipo, agencia, empresa, id, casa, anulacion);
            } else {
                // Actualizar el estado de error en la base de datos
                updateErrorStatusInDatabase(tipo, empresa, agencia, id, casa, jsonResponse.getString("message"));
                return jsonResponse.toString();
            }

        } catch (Exception e) {
            return "{\"success\": false, \"error\": {\"msj\": \"Connection error: " + e.getMessage() + "\"}}";
        }
    }

    /**
     * @param tipo
     * @return
     */
    private String getQueryForTipo(String tipo) {
        switch (tipo) {
            case "condominios":
                return "SELECT fel_estado AS CERTIFICADO_SAT, num_documento AS num_doc, serie FROM sqladmin.c_abono WHERE empresa = ? AND agencia = ? AND casa = ? AND id_abono = ?";
            case "ventas":
                return "SELECT CERTIFICADO_SAT, Serie, num_documento FROM SQLCONTA.DOC_VENTA WHERE empresa = ? AND agencia = ? AND id_venta = ?";
            case "compras":
                return "SELECT CERTIFICADO_SAT FROM SQLCONTA.DOC_COMPRA WHERE empresa = ? AND agencia = ? AND id_compra = ?";
            default:
                return null;
        }
    }

    private String processHttpResponse(HttpURLConnection connection) throws Exception {
        try (InputStream is = connection.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String processSATSuccessResponse(JSONObject jsonResponse, Map<String, Object> empVars, String tipo,
            int agencia, int empresa, int id, int casa, boolean anulacion) throws Exception {
        String uuid = jsonResponse.getString("uuid");

        String pdfPath = "./pdf_FEL/" + empVars.get("pdf_destino");
        new File(pdfPath).mkdirs();
        String xmlSigned = jsonResponse.getString("xmlSigned");

        try (FileOutputStream fos = new FileOutputStream(pdfPath + uuid + ".xml")) {
            fos.write(Base64.getDecoder().decode(xmlSigned));
        }

        updateDatabaseWithUUID(uuid, tipo, empresa, agencia, id, casa, anulacion);
        return "{\"success\": true, \"uuid\": \"" + uuid + "\"}";
    }

    private void updateDatabaseWithUUID(String uuid, String tipo, int empresa, int agencia, int id, int casa,
            boolean anulacion) throws SQLException {
        String sql;
        switch (tipo) {
            case "condominios":
                sql = "UPDATE sqladmin.c_abono SET uuid_dte = ?, fel_estado = ? WHERE empresa = ? AND agencia = ? AND casa = ? AND id_abono = ?";
                break;
            case "ventas":
                sql = "UPDATE SQLCONTA.DOC_VENTA SET DTE_CERT_FULL = ?, CERTIFICADO_SAT = ? WHERE empresa = ? AND agencia = ? AND id_venta = ?";
                break;
            case "compras":
                sql = "UPDATE SQLCONTA.DOC_COMPRA SET DTE_CERT_FULL = ?, CERTIFICADO_SAT = ? WHERE empresa = ? AND agencia = ? AND id_compra = ?";
                break;
            default:
                throw new SQLException("Tipo desconocido: " + tipo);
        }

        try (PreparedStatement stm = dbConn.prepareStatement(sql)) {
            stm.setString(1, uuid);
            stm.setInt(2, anulacion ? 2 : 1);
            stm.setInt(3, empresa);
            stm.setInt(4, agencia);
            stm.setInt(5, id);
            if ("condominios".equals(tipo)) {
                stm.setInt(6, casa);
            }
            stm.executeUpdate();
            dbConn.commit();
        }
    }

    private void updateErrorStatusInDatabase(String tipo, int empresa, int agencia, int id, int casa,
            String errorMessage) throws SQLException {
        String sql;
        switch (tipo) {
            case "condominios":
                sql = "UPDATE sqladmin.c_abono SET fel_estado = 3, fel_errores = nvl(fel_errores, 0) + 1, resultado_fel = ? WHERE empresa = ? AND agencia = ? AND casa = ? AND id_abono = ?";
                break;
            case "ventas":
                sql = "UPDATE SQLCONTA.DOC_VENTA SET CERTIFICADO_SAT = 3, fel_errores = nvl(fel_errores, 0) + 1, resultado_fel = ? WHERE empresa = ? AND agencia = ? AND id_venta = ?";
                break;
            case "compras":
                sql = "UPDATE SQLCONTA.DOC_COMPRA SET CERTIFICADO_SAT = 3, fel_errores = nvl(fel_errores, 0) + 1, resultado_fel = ? WHERE empresa = ? AND agencia = ? AND id_compra = ?";
                break;
            default:
                throw new SQLException("Tipo desconocido: " + tipo);
        }

        try (PreparedStatement stm = dbConn.prepareStatement(sql)) {
            stm.setString(1, errorMessage);
            stm.setInt(2, empresa);
            stm.setInt(3, agencia);
            stm.setInt(4, id);
            if ("condominios".equals(tipo)) {
                stm.setInt(5, casa);
            }
            stm.executeUpdate();
            dbConn.commit();
        }
    }

}
