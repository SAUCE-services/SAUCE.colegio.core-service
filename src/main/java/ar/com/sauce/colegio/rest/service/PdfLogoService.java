package ar.com.sauce.colegio.rest.service;

import org.openpdf.text.Document;
import org.openpdf.text.Image;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.InputStream;

// 🌟 Servicio compartido: carga y posiciona el logo de Jardín/Colegio en cualquier PDF
// de la app. Se armó como un service aparte (en vez de repetir la lógica en cada
// service que genera PDFs) para no duplicar código entre FacturaService, AlumnoService, etc.
@Service
public class PdfLogoService {

    // Carga el logo correcto (Jardín o Colegio) según el nombre del establecimiento.
    // Si no matchea ninguno de los dos, o si el archivo no se puede leer, devuelve null
    // (nunca rompe la generación del PDF por un logo faltante).
    public Image cargarLogo(String nombreEstablecimiento) {
        if (nombreEstablecimiento == null) return null;

        String archivo;
        String nombreUpper = nombreEstablecimiento.toUpperCase();
        if (nombreUpper.contains("JARDIN") || nombreUpper.contains("JARDÍN")) {
            archivo = "/images/jardin.jpg";
        } else if (nombreUpper.contains("COLEGIO")) {
            archivo = "/images/colegio.jpg";
        } else {
            return null;
        }

        try (InputStream is = getClass().getResourceAsStream(archivo)) {
            if (is == null) return null;
            byte[] bytes = is.readAllBytes();
            return Image.getInstance(bytes);
        } catch (Exception e) {
            return null; // Si el logo falla, el PDF se sigue generando igual, sin logo
        }
    }

    // Coloca el logo arriba a la derecha de la página actual, sin desplazar el resto
    // del contenido (usa coordenadas absolutas sobre el PdfContentByte)
    public void colocarArribaDerecha(Document document, PdfWriter writer, String nombreEstablecimiento) {
        Image logo = cargarLogo(nombreEstablecimiento);
        if (logo == null) return;

        try {
            float maxAncho = 70f;
            float maxAlto = 70f;
            float escala = Math.min(maxAncho / logo.getWidth(), maxAlto / logo.getHeight());
            logo.scaleToFit(logo.getWidth() * escala, logo.getHeight() * escala);

            float x = document.getPageSize().getWidth() - document.rightMargin() - logo.getScaledWidth();
            float y = document.getPageSize().getHeight() - document.topMargin() - logo.getScaledHeight() + 10f;
            logo.setAbsolutePosition(x, y);

            writer.getDirectContent().addImage(logo);
        } catch (Exception e) {
            // Si algo falla al posicionarlo, seguimos sin el logo antes que romper el PDF
        }
    }
}