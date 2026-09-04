package br.com.redemob.validador.model;

public record DadosDocumentoExtraidoIa(String tipoDocumento, String nome, String dataNascimento, String cpf,
		Integer confianca, String observacoes) {

	public static DadosDocumentoExtraidoIa vazio() {
		return new DadosDocumentoExtraidoIa("Nao identificado", "", "", "", 0,
				"Nao foi possivel extrair dados do documento.");
	}

	public DadosExtraidosDocumento comBiometria(BiometriaDocumento biometria) {
		return new DadosExtraidosDocumento(this.tipoDocumento, this.nome, this.dataNascimento, this.cpf,
				this.confianca, this.observacoes, biometria == null ? BiometriaDocumento.inconclusiva() : biometria);
	}

}
