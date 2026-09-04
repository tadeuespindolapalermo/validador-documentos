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

	private final DocumentoAiValidationService validationService;

	public DocumentoController(DocumentoAiValidationService validationService) {
		this.validationService = validationService;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("dados", DadosInformados.vazio());
		return "index";
	}

	@PostMapping("/validar")
	public String validar(@RequestParam String nome, @RequestParam String dataNascimento, @RequestParam String cpf,
			@RequestParam MultipartFile documento, Model model) {

		DadosInformados dados = new DadosInformados(nome, parseData(dataNascimento), cpf);
		List<String> erros = validarEntrada(dados, documento);

		model.addAttribute("dados", dados);
		model.addAttribute("nomeInput", nome);
		model.addAttribute("dataNascimentoInput", dataNascimento);
		model.addAttribute("cpfInput", cpf);

		if (!erros.isEmpty()) {
			model.addAttribute("erros", erros);
			return "index";
		}

		ResultadoValidacao resultado = this.validationService.validar(dados, documento);
		model.addAttribute("resultado", resultado);
		return "index";
	}

	private List<String> validarEntrada(DadosInformados dados, MultipartFile documento) {
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
			erros.add("Use apenas PDF, PNG, JPG, JPEG ou WEBP.");
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
			case "application/pdf", "image/png", "image/jpeg", "image/jpg", "image/webp" -> true;
			default -> false;
		};
	}

}
