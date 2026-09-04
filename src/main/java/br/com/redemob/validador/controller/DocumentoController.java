package br.com.redemob.validador.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.redemob.validador.model.DadosInformados;
import br.com.redemob.validador.model.ResultadoValidacao;
import br.com.redemob.validador.service.DocumentoAiValidationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class DocumentoController {

	private static final String INDEX = "index";

	private final DocumentoAiValidationService validationService;

	public DocumentoController(DocumentoAiValidationService validationService) {
		this.validationService = validationService;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("dados", DadosInformados.vazio());
		return INDEX;
	}

	@PostMapping("/validar")
	public String validar(@RequestParam String nome, @RequestParam String dataNascimento, @RequestParam String cpf,
			@RequestParam MultipartFile documento, @RequestParam MultipartFile foto,
			@RequestParam(defaultValue = "false") boolean consentimentoBiometria, Model model) {

		DadosInformados dados = new DadosInformados(nome, parseData(dataNascimento), cpf);
		List<String> erros = validarEntrada(dados, documento, foto, consentimentoBiometria);

		model.addAttribute("dados", dados);
		model.addAttribute("nomeInput", nome);
		model.addAttribute("dataNascimentoInput", dataNascimento);
		model.addAttribute("cpfInput", cpf);
		model.addAttribute("consentimentoBiometria", consentimentoBiometria);

		if (!erros.isEmpty()) {
			model.addAttribute("erros", erros);
			return INDEX;
		}

		ResultadoValidacao resultado = this.validationService.validar(dados, documento, foto);
		model.addAttribute("resultado", resultado);
		return INDEX;
	}

	private List<String> validarEntrada(DadosInformados dados, MultipartFile documento, MultipartFile foto,
			boolean consentimentoBiometria) {
		List<String> erros = new ArrayList<>();

		if (!StringUtils.hasText(dados.nome())) {
			erros.add("Informe o nome completo.");
		}
		if (dados.dataNascimento() == null) {
			erros.add("Informe uma data de nascimento valida.");
		}
		if (!temCpfValido(dados.cpf())) {
			erros.add("Informe um CPF com 11 digitos.");
		}
		if (documento == null || documento.isEmpty()) {
			erros.add("Anexe uma foto ou PDF do RG, CNH ou CIN.");
		}
		else if (!tipoAceito(documento)) {
			erros.add("Use apenas PDF, PNG, JPG ou JPEG para o documento.");
		}
		if (foto == null || foto.isEmpty()) {
			erros.add("Anexe uma foto do usuario para a validacao biometrica.");
		}
		else if (!imagemAceita(foto)) {
			erros.add("Use apenas PNG, JPG ou JPEG para a foto biometrica.");
		}
		if (!consentimentoBiometria) {
			erros.add("Confirme o consentimento para a validacao biometrica facial.");
		}

		return erros;
	}

	private LocalDate parseData(String valor) {
		if (!StringUtils.hasText(valor)) {
			return null;
		}
		try {
			return LocalDate.parse(valor);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	private boolean temCpfValido(String cpf) {
		return cpf != null && cpf.replaceAll("\\D", "").length() == 11;
	}

	private boolean tipoAceito(MultipartFile documento) {
		String contentType = documento.getContentType();
		if (contentType == null) {
			return false;
		}
		return switch (contentType.toLowerCase()) {
			case "application/pdf", "image/png", "image/jpeg", "image/jpg" -> true;
			default -> false;
		};
	}

	private boolean imagemAceita(MultipartFile foto) {
		String contentType = foto.getContentType();
		if (contentType == null) {
			return false;
		}
		return switch (contentType.toLowerCase()) {
			case "image/png", "image/jpeg", "image/jpg" -> true;
			default -> false;
		};
	}

}
