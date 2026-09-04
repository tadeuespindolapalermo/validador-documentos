package br.com.redemob.validador.service.biometria;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import br.com.redemob.validador.model.BiometriaDocumento;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CompreFaceBiometriaService implements BiometriaService {

	private static final Logger log = LoggerFactory.getLogger(CompreFaceBiometriaService.class);

	private static final List<Integer> ROTACOES = List.of(0, 90, 180, 270);

	private static final int TAMANHO_MAXIMO_COMPREFACE = 5 * 1024 * 1024;

	private static final int MAIOR_DIMENSAO = 1800;

	private final RestClient restClient;

	private final JsonMapper jsonMapper;

	private final String apiKey;

	private final double threshold;

	private final double detProbThreshold;

	private final int limit;

	private final boolean testarRotacoes;

	public CompreFaceBiometriaService(RestClient.Builder restClientBuilder,
			ObjectProvider<JsonMapper> jsonMapperProvider,
			@Value("${app.biometria.compreface.base-url:http://localhost:8000}") String baseUrl,
			@Value("${app.biometria.compreface.api-key:}") String apiKey,
			@Value("${app.biometria.compreface.threshold:0.70}") double threshold,
			@Value("${app.biometria.compreface.det-prob-threshold:0.80}") double detProbThreshold,
			@Value("${app.biometria.compreface.limit:1}") int limit,
			@Value("${app.biometria.compreface.test-rotations:true}") boolean testarRotacoes) {
		String url = StringUtils.hasText(baseUrl) ? baseUrl : "http://localhost:8000";
		this.restClient = restClientBuilder.baseUrl(url).build();
		this.jsonMapper = jsonMapperProvider.getIfAvailable(() -> JsonMapper.builder().build());
		this.apiKey = apiKey;
		this.threshold = normalizarThreshold(threshold);
		this.detProbThreshold = detProbThreshold;
		this.limit = Math.max(1, limit);
		this.testarRotacoes = testarRotacoes;
	}

	@Override
	public BiometriaDocumento comparar(MultipartFile documento, MultipartFile foto) {
		if (!StringUtils.hasText(this.apiKey)) {
			return BiometriaDocumento.inconclusiva(
					"Configure COMPREFACE_API_KEY para habilitar a comparacao facial pelo CompreFace.");
		}
		if (documento == null || documento.isEmpty() || foto == null || foto.isEmpty()) {
			return BiometriaDocumento.inconclusiva("Envie o documento e a foto para comparar as faces.");
		}

		try {
			List<ImagemBiometria> fotosUsuario = prepararVariacoes(foto, "foto-usuario");
			List<ImagemBiometria> documentos = prepararVariacoes(documento, "documento");
			Optional<ComparacaoFace> melhor = compararVariacoes(fotosUsuario, documentos);

			if (melhor.isEmpty()) {
				return BiometriaDocumento.inconclusiva(
						"O CompreFace nao localizou faces comparaveis nos arquivos enviados.");
			}

			double similaridade = melhor.get().similaridadeNormalizada();
			int percentual = Math.toIntExact(Math.round(similaridade * 100));
			if (similaridade >= this.threshold) {
				return BiometriaDocumento.match(percentual,
						"CompreFace confirmou match facial com similaridade de " + percentual + "%.");
			}
			return BiometriaDocumento.divergente(percentual,
					"CompreFace encontrou similaridade facial de " + percentual
							+ "%, abaixo do minimo configurado de " + percentualThreshold() + "%.");
		}
		catch (RestClientResponseException ex) {
			log.warn("CompreFace recusou a comparacao facial. Status: {}. Corpo: {}", ex.getStatusCode(),
					limitarParaLog(ex.getResponseBodyAsString()));
			return BiometriaDocumento.inconclusiva(
					"O CompreFace recusou as imagens. Verifique a API key e se ha uma face nitida em cada arquivo.");
		}
		catch (RestClientException ex) {
			log.warn("Falha ao chamar CompreFace", ex);
			return BiometriaDocumento.inconclusiva(
					"Nao foi possivel conectar ao CompreFace. Verifique se o servico esta rodando localmente.");
		}
		catch (IOException | RuntimeException ex) {
			log.warn("Falha ao preparar ou analisar imagens para biometria", ex);
			return BiometriaDocumento.inconclusiva(
					"Nao foi possivel preparar os arquivos para a comparacao facial.");
		}
	}

	private Optional<ComparacaoFace> compararVariacoes(List<ImagemBiometria> fotosUsuario,
			List<ImagemBiometria> documentos) throws IOException {
		List<ComparacaoFace> comparacoes = new ArrayList<>();
		for (ImagemBiometria fotoUsuario : fotosUsuario) {
			for (ImagemBiometria documento : documentos) {
				try {
					OptionalDouble similaridade = chamarCompreFace(fotoUsuario, documento);
					if (similaridade.isPresent()) {
						comparacoes.add(new ComparacaoFace(similaridade.getAsDouble()));
					}
				}
				catch (RestClientResponseException ex) {
					if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
						throw ex;
					}
					log.debug("CompreFace nao comparou uma variacao de imagem. Status: {}", ex.getStatusCode());
				}
			}
		}
		return comparacoes.stream().max(Comparator.comparingDouble(ComparacaoFace::similaridadeNormalizada));
	}

	private OptionalDouble chamarCompreFace(ImagemBiometria fotoUsuario, ImagemBiometria documento)
			throws IOException {
		MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
		corpo.add("source_image", arquivoMultipart(fotoUsuario));
		corpo.add("target_image", arquivoMultipart(documento));

		String resposta = this.restClient.post()
			.uri(uriBuilder -> uriBuilder.path("/api/v1/verification/verify")
				.queryParam("limit", this.limit)
				.queryParam("prediction_count", 1)
				.queryParam("det_prob_threshold", this.detProbThreshold)
				.queryParam("status", true)
				.build())
			.header("x-api-key", this.apiKey)
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(corpo)
			.retrieve()
			.body(String.class);

		if (!StringUtils.hasText(resposta)) {
			return OptionalDouble.empty();
		}
		return maiorSimilaridade(this.jsonMapper.readTree(resposta));
	}

	private HttpEntity<ByteArrayResource> arquivoMultipart(ImagemBiometria imagem) {
		ByteArrayResource resource = new ByteArrayResource(imagem.bytes()) {
			@Override
			public String getFilename() {
				return imagem.nomeArquivo();
			}
		};

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_JPEG);
		return new HttpEntity<>(resource, headers);
	}

	private List<ImagemBiometria> prepararVariacoes(MultipartFile arquivo, String prefixo) throws IOException {
		BufferedImage imagem = imagemBase(arquivo);
		List<Integer> rotacoes = this.testarRotacoes ? ROTACOES : List.of(0);
		List<ImagemBiometria> variacoes = new ArrayList<>();
		for (Integer rotacao : rotacoes) {
			BufferedImage preparada = redimensionarSeNecessario(converterParaRgb(rotacionar(imagem, rotacao)));
			byte[] bytes = escreverJpegAteLimite(preparada);
			variacoes.add(new ImagemBiometria(prefixo + "-rot" + rotacao + ".jpg", bytes));
		}
		return variacoes;
	}

	private BufferedImage imagemBase(MultipartFile arquivo) throws IOException {
		String contentType = Optional.ofNullable(arquivo.getContentType()).orElse("").toLowerCase();
		if ("application/pdf".equals(contentType)) {
			return primeiraPaginaPdf(arquivo);
		}

		try (InputStream input = arquivo.getInputStream()) {
			BufferedImage imagem = ImageIO.read(input);
			if (imagem == null) {
				throw new IOException("Formato de imagem nao suportado para biometria.");
			}
			return imagem;
		}
	}

	private BufferedImage primeiraPaginaPdf(MultipartFile arquivo) throws IOException {
		try (PDDocument pdf = Loader.loadPDF(arquivo.getBytes())) {
			if (pdf.getNumberOfPages() == 0) {
				throw new IOException("PDF sem paginas.");
			}
			PDFRenderer renderer = new PDFRenderer(pdf);
			return renderer.renderImageWithDPI(0, 220, ImageType.RGB);
		}
	}

	private BufferedImage rotacionar(BufferedImage imagem, int graus) {
		if (graus == 0) {
			return imagem;
		}

		int largura = imagem.getWidth();
		int altura = imagem.getHeight();
		int novaLargura = graus == 90 || graus == 270 ? altura : largura;
		int novaAltura = graus == 90 || graus == 270 ? largura : altura;

		BufferedImage rotacionada = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rotacionada.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, novaLargura, novaAltura);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setTransform(transformacaoRotacao(graus, largura, altura));
		g.drawImage(imagem, 0, 0, null);
		g.dispose();
		return rotacionada;
	}

	private AffineTransform transformacaoRotacao(int graus, int largura, int altura) {
		AffineTransform transform = new AffineTransform();
		switch (graus) {
			case 90 -> {
				transform.translate(altura, 0);
				transform.rotate(Math.toRadians(90));
			}
			case 180 -> {
				transform.translate(largura, altura);
				transform.rotate(Math.toRadians(180));
			}
			case 270 -> {
				transform.translate(0, largura);
				transform.rotate(Math.toRadians(270));
			}
			default -> {
			}
		}
		return transform;
	}

	private BufferedImage converterParaRgb(BufferedImage imagem) {
		if (imagem.getType() == BufferedImage.TYPE_INT_RGB) {
			return imagem;
		}
		BufferedImage rgb = new BufferedImage(imagem.getWidth(), imagem.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgb.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
		g.drawImage(imagem, 0, 0, null);
		g.dispose();
		return rgb;
	}

	private BufferedImage redimensionarSeNecessario(BufferedImage imagem) {
		int maiorDimensaoAtual = Math.max(imagem.getWidth(), imagem.getHeight());
		if (maiorDimensaoAtual <= MAIOR_DIMENSAO) {
			return imagem;
		}

		double escala = (double) MAIOR_DIMENSAO / maiorDimensaoAtual;
		int novaLargura = Math.max(1, (int) Math.round(imagem.getWidth() * escala));
		int novaAltura = Math.max(1, (int) Math.round(imagem.getHeight() * escala));
		return redimensionar(imagem, novaLargura, novaAltura);
	}

	private BufferedImage redimensionar(BufferedImage imagem, int largura, int altura) {
		BufferedImage redimensionada = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = redimensionada.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, largura, altura);
		g.drawImage(imagem, 0, 0, largura, altura, null);
		g.dispose();
		return redimensionada;
	}

	private byte[] escreverJpegAteLimite(BufferedImage imagem) throws IOException {
		BufferedImage atual = imagem;
		float qualidade = 0.88f;
		byte[] bytes = escreverJpeg(atual, qualidade);

		for (int tentativa = 0; tentativa < 8 && bytes.length > TAMANHO_MAXIMO_COMPREFACE; tentativa++) {
			if (qualidade > 0.56f) {
				qualidade -= 0.1f;
			}
			else {
				int largura = Math.max(320, (int) Math.round(atual.getWidth() * 0.84));
				int altura = Math.max(320, (int) Math.round(atual.getHeight() * 0.84));
				atual = redimensionar(atual, largura, altura);
			}
			bytes = escreverJpeg(atual, qualidade);
		}
		return bytes;
	}

	private byte[] escreverJpeg(BufferedImage imagem, float qualidade) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
			writer.setOutput(imageOutput);
			ImageWriteParam params = writer.getDefaultWriteParam();
			params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			params.setCompressionQuality(Math.max(0.35f, Math.min(0.95f, qualidade)));
			writer.write(null, new IIOImage(imagem, null, null), params);
			return output.toByteArray();
		}
		finally {
			writer.dispose();
		}
	}

	private OptionalDouble maiorSimilaridade(JsonNode node) {
		List<Double> valores = new ArrayList<>();
		coletarSimilaridades(node, valores);
		return valores.stream().mapToDouble(Double::doubleValue).max();
	}

	private void coletarSimilaridades(JsonNode node, List<Double> valores) {
		if (node == null || node.isNull()) {
			return;
		}
		if (node.isObject()) {
			for (Map.Entry<String, JsonNode> entry : node.properties()) {
				if ("similarity".equalsIgnoreCase(entry.getKey()) && entry.getValue().isNumber()) {
					valores.add(normalizarSimilaridade(entry.getValue().asDouble()));
				}
				coletarSimilaridades(entry.getValue(), valores);
			}
		}
		else if (node.isArray()) {
			for (JsonNode child : node) {
				coletarSimilaridades(child, valores);
			}
		}
	}

	private double normalizarSimilaridade(double valor) {
		if (valor > 1.0) {
			return valor / 100.0;
		}
		return Math.max(0.0, Math.min(1.0, valor));
	}

	private double normalizarThreshold(double valor) {
		if (valor > 1.0) {
			return valor / 100.0;
		}
		if (valor <= 0.0) {
			return 0.70;
		}
		return Math.min(1.0, valor);
	}

	private int percentualThreshold() {
		return Math.toIntExact(Math.round(this.threshold * 100));
	}

	private String limitarParaLog(String valor) {
		if (!StringUtils.hasText(valor)) {
			return "";
		}
		String limpo = valor.replaceAll("\\s+", " ").trim();
		return limpo.length() <= 500 ? limpo : limpo.substring(0, 500) + "...";
	}

	private record ImagemBiometria(String nomeArquivo, byte[] bytes) {
	}

	private record ComparacaoFace(double similaridadeNormalizada) {
	}

}
