package br.com.redemob.validador.model;

public record DadosExtraidosDocumento(String tipoDocumento, String nome, String dataNascimento, String cpf,
		Integer confianca, String observacoes, BiometriaDocumento biometria) {

	public static DadosExtraidosDocumento vazio() {
		return new DadosExtraidosDocumento("Nao identificado", "", "", "", 0,
				"Nao foi possivel extrair dados do documento.", BiometriaDocumento.inconclusiva());
	}

	public DadosExtraidosDocumento comBiometria(BiometriaDocumento biometria) {
		return new DadosExtraidosDocumento(this.tipoDocumento, this.nome, this.dataNascimento, this.cpf,
				this.confianca, this.observacoes, biometria == null ? BiometriaDocumento.inconclusiva() : biometria);
	}

}
