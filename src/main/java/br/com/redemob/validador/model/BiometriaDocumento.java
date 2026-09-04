package br.com.redemob.validador.model;

import org.springframework.util.StringUtils;

public record BiometriaDocumento(String resultado, Integer confianca, String observacoes) {

	private static final String MATCH = "MATCH";

	private static final String DIVERGENTE = "DIVERGENTE";

	private static final String INCONCLUSIVO = "INCONCLUSIVO";

	public static BiometriaDocumento match(int confianca, String observacoes) {
		return new BiometriaDocumento(MATCH, confianca, observacoes);
	}

	public static BiometriaDocumento divergente(int confianca, String observacoes) {
		return new BiometriaDocumento(DIVERGENTE, confianca, observacoes);
	}

	public static BiometriaDocumento inconclusiva(String observacoes) {
		return new BiometriaDocumento(INCONCLUSIVO, 0,
				StringUtils.hasText(observacoes) ? observacoes : "Validacao biometrica nao realizada.");
	}

	public static BiometriaDocumento inconclusiva() {
		return inconclusiva("Validacao biometrica nao realizada.");
	}

	public boolean match() {
		return MATCH.equalsIgnoreCase(this.resultado);
	}

	public boolean divergente() {
		return DIVERGENTE.equalsIgnoreCase(this.resultado);
	}

	public int confiancaSegura() {
		return this.confianca == null ? 0 : this.confianca;
	}

}
