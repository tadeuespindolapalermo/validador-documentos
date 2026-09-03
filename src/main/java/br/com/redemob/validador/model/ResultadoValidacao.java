package br.com.redemob.validador.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ResultadoValidacao(ResultadoStatus status, DadosInformados informado,
		DadosExtraidosDocumento extraido, List<CampoComparado> campos, String resumo, LocalDateTime analisadoEm) {

	private static final DateTimeFormatter DATA_HORA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	public static ResultadoValidacao erro(DadosInformados informado, String mensagem) {
		return new ResultadoValidacao(ResultadoStatus.ERRO, informado, DadosExtraidosDocumento.vazio(), List.of(),
				mensagem, LocalDateTime.now());
	}

	public String analisadoEmFormatado() {
		return this.analisadoEm == null ? "" : this.analisadoEm.format(DATA_HORA_BR);
	}

	public int camposConfirmados() {
		return (int) this.campos.stream().filter(CampoComparado::confirmado).count();
	}

	public int camposDivergentes() {
		return (int) this.campos.stream().filter(CampoComparado::divergente).count();
	}

}
