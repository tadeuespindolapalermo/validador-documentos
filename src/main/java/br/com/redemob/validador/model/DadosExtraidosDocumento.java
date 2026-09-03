package br.com.redemob.validador.model;

public record DadosExtraidosDocumento(String tipoDocumento, String nome, String dataNascimento, String cpf,
		Integer confianca, String observacoes) {

	public static DadosExtraidosDocumento vazio() {
		return new DadosExtraidosDocumento("Nao identificado", "", "", "", 0,
				"Nao foi possivel extrair dados do documento.");
	}

}
