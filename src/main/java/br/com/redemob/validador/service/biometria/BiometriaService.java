package br.com.redemob.validador.service.biometria;

import br.com.redemob.validador.model.BiometriaDocumento;
import org.springframework.web.multipart.MultipartFile;

public interface BiometriaService {

	BiometriaDocumento comparar(MultipartFile documento, MultipartFile foto);

}
